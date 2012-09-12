/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */
package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.Field;
import com.sun.labs.minion.indexer.MetaFile;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DiskDictionaryBundle.Fetcher;
import com.sun.labs.minion.indexer.dictionary.TermStatsHeader;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.indexer.postings.Postings;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A disk partition that holds data that is specific to the
 * implementation of an inverted file.  It extends the disk
 * partition to add bigrams and a field store to the main
 * and document dictionaries already present in the superclass.
 *
 */
public class InvFileDiskPartition extends DiskPartition {

    private static final Logger logger = Logger.getLogger(InvFileDiskPartition.class.
            getName());

    private DiskField[] fields;

    private RandomAccessFile vectorLengths;

    /**
     * Opens a partition with a given number
     *
     * @param partNumber the number of this partition.
     * @param manager the manager for this partition.
     * @throws java.io.IOException If there is an error opening or reading
     * any of the files making up a partition.
     *
     * @see com.sun.labs.minion.indexer.partition.Partition
     * @see com.sun.labs.minion.indexer.dictionary.Dictionary
     *
     */
    public InvFileDiskPartition(int partNumber,
            PartitionManager manager,
            EntryFactory mainDictEntryFactory)
            throws java.io.IOException {
        super(partNumber, manager, Postings.Type.NONE);

        this.partNumber = partNumber;
        File vlf = manager.makeVectorLengthFile(partNumber);
        vectorLengths = new RandomAccessFile(vlf, "rw");

        //
        // Get the field offsets and use the ID of the last field to size our
        // array of fields.
        List<PartitionHeader.FieldOffset> offsets = header.getFieldOffsets();
        fields = new DiskField[offsets.get(offsets.size() - 1).getId() + 1];

        for (PartitionHeader.FieldOffset offset : header.getFieldOffsets()) {
            if(offset.getOffset() == -1) {
                continue;
            }
                   
            dictRAF.seek(offset.getOffset());
            FieldInfo info = manager.getMetaFile().getFieldInfo(offset.getId());
            fields[offset.getId()] =
                    new DiskField(this, info, dictRAF, vectorLengths,
                    postRAFs);
        }
    }

    @Override
    public String[] getPostingsChannelNames() {
        String[] max = null;
        for(DiskField field : fields) {
            if(field != null) {
                String[] t = field.getPostingsChannelNames();
                if(max == null || (t != null && t.length > max.length)) {
                    max = t;
                }
            }
        }
        return max;
    }
    
    public QueryEntry get(String field, String term, boolean caseSensitive) {
        DiskField df = getDF(field);
        if (df == null) {
            return null;
        }
        return df.getTerm(term, caseSensitive);
    }

    public QueryEntry get(String field, int termID, boolean caseSensitive) {
        DiskField df = getDF(field);
        if (df == null) {
            return null;
        }
        return df.getTerm(termID, caseSensitive);
    }

    public Map<String, List> getSavedValues(int docID) {
        Map<String, List> ret = new HashMap<String, List>();
        for (DiskField df : fields) {
            if (df != null) {
                List l = df.getFetcher().fetch(docID);
                if (l != null && l.size() > 0) {
                    ret.put(df.getInfo().getName(), l);
                }
            }
        }
        return ret;
    }

    /**
     * Gets the field for a given name.
     * @param name the name of the field
     * @return the field for that name, or <code>null</code> if there is no such field.
     */
    public DiskField getDF(String name) {
        return getDF(manager.getFieldInfo(name));
    }

    /**
     * Gets the field associated with a given field information object.
     * @param fi the field info object for the field to fetch
     * @return the field for this object or <code>null</code> if there is no
     * such field in this partition.
     */
    public DiskField getDF(FieldInfo fi) {
        if (fi == null) {
            return null;
        }
        return getDF(fi.getID());
    }

    public DiskField getDF(int fieldID) {
        if (fieldID < 0 || fieldID >= fields.length) {
            return null;
        }
        return fields[fieldID];
    }

    public List<DiskField> getDiskFields() {
        List<DiskField> ret = new ArrayList<DiskField>();
        for(DiskField field : fields) {
            if(field != null) {
                ret.add(field);
            }
        }
        return ret;
    }
    
    public void normalize(Set<FieldInfo> fields, int[] docs, float[] scores,
                          int size, float sqw) {
        if(fields == null || fields.isEmpty()) {
            fields = manager.engine.getDefaultFields();
        }
        List<DocumentVectorLengths> dvls = new ArrayList<DocumentVectorLengths>();
        for(FieldInfo field : fields) {
            DiskField df = getDF(field);
            if(df != null) {
                dvls.add(df.getDocumentVectorLengths());
            }
        }
        DocumentVectorLengths.normalize(dvls, docs, scores, size, sqw);
    }

    /**
     * Gets some or all of the data saved in a given field, in a given
     * document.
     *
     * @param name The name of the field.
     * @param key The document key of the document for which we want data.
     * @param all If <code>true</code>, all field values will be returned
     * as a list.  If <code>false</code> only the first value will be returned.
     * @return A list of the values saved in the given field in the given
     * document, or <code>null</code> if the given key is not in this
     * partition.
     */
    public Object getSavedFieldData(String name, String key, boolean all) {
        QueryEntry e = docDict.get(key);
        if (e == null) {
            return null;
        }

        return getSavedFieldData(name, e.getID(), all);
    }

    /**
     * Gets some or all of the data saved in a given field.
     *
     * @param name The name of the field.
     * @param docID The document ID for which we want the saved data.
     * @param all If <code>true</code>, return all known values for the
     * field in the given document.  If <code>false</code> return only one
     * value.
     * @return If <code>all</code> is true, then return a <code>List</code>
     * of field values, otherwise, return a single field value of the
     * appropriate type.  If <code>all</code> is <code>false</code>, a single
     * value of the appropriate type will be returned.
     *
     * <P>
     *
     * If the given name is not the name of a saved field, or the document
     * ID is invalid, then if <code>all</code> is true, an empty list will
     * be returned.  If <code>all</code> is <code>false</code>,
     * <code>null</code> will be returned.
     */
    public Object getSavedFieldData(String name, int docID, boolean all) {
        DiskField df = getDF(name);

        if (df == null) {
            return null;
        }

        Fetcher f = df.getFetcher();
        if (f == null) {
            return null;
        }

        if (all) {
            return f.fetch(docID);
        } else {
            return f.fetchOne(docID);
        }
    }
    
    /**
     * Gets an iterator for all of the values in a field.
     *
     * @param name The name of the field we need an iterator for.
     * @return An iterator for the values in the field.
     */
    public DictionaryIterator getFieldIterator(String name) {
        return getFieldIterator(name, true, null, true, null, true);
    }

    /**
     * Gets an iterator for the values in a given range in a field.
     *
     * @param name The name of the field we need an iterator for.
     * @param caseSensitive If true, case should be taken into account when
     * iterating through the values.  This value will only be observed for
     * character fields!
     * @param lowerBound The lower bound on the iterator.  If
     * <code>null</code>, only the upper bound is considered and the
     * iteration will commence with the first term in the dictionary.
     * @param includeLower If <code>true</code>, then the lower bound will
     * be included in the entries returned by the iterator, if it occurs in
     * the dictionary.
     * @param upperBound The upper bound on the iterator.  If
     * <code>null</code>, only the lower bound is considered and the
     * iteration will end at the last term in the dictionary.
     * @param includeUpper If <code>true</code>, then the upper bound will
     * be included in the entries returned by the iterator, if it occurs in
     * the dictionary.
     * @return An iterator for the dictionary entries contained in the
     * range, or <code>null</code> if there is no such range or the named
     * field is not a saved field.
     */
    public DictionaryIterator getFieldIterator(String name,
            boolean caseSensitive,
            Comparable lowerBound,
            boolean includeLower,
            Comparable upperBound,
            boolean includeUpper) {
        DiskField df = getDF(name);
        if (df == null) {
            return null;
        }
        return df.getSavedValuesIterator(
                caseSensitive, lowerBound,
                includeLower, upperBound, includeUpper);
    }

    /**
     * Gets an iterator for the character saved field values that match a
     * given wildcard pattern.
     *
     * @param name The name of the field whose values we wish to match
     * against.
     * @param val The wildcard value against which we will match.
     * @param caseSensitive If <code>true</code>, then case will be taken
     * into account during the match.
     */
    public DictionaryIterator getMatchingIterator(String name, String val,
            boolean caseSensitive) {
        DiskField df = getDF(name);
        if (df == null) {
            return null;
        }
        return df.getMatchingIterator(val, caseSensitive, -1, -1);
    }

    /**
     * Gets an iterator for the character saved field values that contain a
     * given substring.
     *
     * @param name The name of the field whose values we wish to match
     * against.
     * @param val The wildcard value against which we will match.
     * @param caseSensitive If <code>true</code>, then case will be taken
     * into account during the match.
     */
    public DictionaryIterator getSubstringIterator(String name, String val,
            boolean caseSensitive,
            boolean starts, boolean ends) {
        DiskField df = getDF(name);
        if (df == null) {
            return null;
        }
        return df.getSubstringIterator(val, caseSensitive, starts, ends,
                -1, -1);
    }

    /**
     * Gets the number of defined fields.
     */
    public int getFieldCount() {
        return manager.metaFile.size();
    }

    @Override
    protected void mergeCustom(MergeState mergeState)
            throws Exception {

        for(FieldInfo fi : mergeState.partOut.getPartitionManager().getMetaFile()) {
            DiskField[] mFields = new DiskField[mergeState.partitions.length];
            DiskField merger = null;
            for (int j = 0; j < mergeState.partitions.length; j++) {
                mFields[j] = ((InvFileDiskPartition) mergeState.partitions[j]).getDF(fi);
                if (mFields[j] != null) {
                    merger = mFields[j];
                }
            }
            
            long fieldOffset = mergeState.partOut.getPartitionDictionaryOutput().position();
            
            if(merger == null) {
                logger.finer(String.format("No merger for %s", fi.getName()));
                fieldOffset = -1;
            } else {
                mergeState.info = fi;
                DiskField.merge(mergeState, mFields);
            }
            mergeState.partOut.getPartitionHeader().addOffset(fi.getID(), fieldOffset);
        }
    }
    
    public static void calculateTermStats(DiskPartition[] partitions,
            DictionaryOutput termStatsDictOut) throws java.io.IOException {

        //
        // The header and where it goes.
        TermStatsHeader termStatsHeader = new TermStatsHeader();
        termStatsDictOut.byteEncode(0, 8);
        
        MetaFile mf = partitions[0].manager.getMetaFile();

        for(FieldInfo fi : mf) {
            
            if(!fi.hasAttribute(FieldInfo.Attribute.INDEXED)) {
                //
                // Skip fields that don't have term information.
                for(Field.TermStatsType type : Field.TermStatsType.values()) {
                    termStatsHeader.addOffset(fi.getID(), type, -1);
                }
                continue;
            }
            
            DiskField[] fields = new DiskField[partitions.length];
            DiskField regen = null;
            for(int j = 0; j < partitions.length; j++) {
                fields[j] = ((InvFileDiskPartition) partitions[j]).getDF(fi);
                if(fields[j] != null) {
                    regen = fields[j];
                }
            }
            
            if(regen == null) {
                for(Field.TermStatsType type : Field.TermStatsType.values()) {
                    termStatsHeader.addOffset(fi.getID(), type, -1);
                }
                continue;
            }
            DiskField.calculateTermStats(fields, termStatsHeader, termStatsDictOut);
        }
        
        long tshpos = termStatsDictOut.position();
        termStatsHeader.write(termStatsDictOut);
        long end = termStatsDictOut.position();
        termStatsDictOut.position(0);
        termStatsDictOut.byteEncode(tshpos, 8);
        termStatsDictOut.position(end);
    }
    
    public void calculateVectorLengths(PartitionOutput partOut) throws IOException {
        
        partOut.setPartitionNumber(partNumber);
        for(DiskField df : fields) {

            if(df == null) {
                continue;
            }
            
            if(!df.getInfo().hasAttribute(FieldInfo.Attribute.INDEXED)) {
                //
                // Skip fields that don't have term information.
                continue;
            }
            
            df.calculateVectorLengths(partOut);
        }
        
        partOut.flushVectorLengths();
        
        //
        // Open our new vector lengths.
        vectorLengths.close();
        vectorLengths = new RandomAccessFile(manager.makeVectorLengthFile(partNumber), "rw");
    }

    /**
     * Close the files associated with this partition.
     */
    @Override
    public synchronized boolean close(long currTime) {
        if (!super.close(currTime)) {
            return false;
        } else {
            try {
                vectorLengths.close();
            } catch (java.io.IOException ioe) {
                logger.log(Level.SEVERE, "Error closing partition", ioe);
            }
        }

        return true;
    }

    /**
     * Reaps the given partition.  If the postings file cannot be removed,
     * then we return control immediately.
     *
     * @param m The manager associated with the partition.
     * @param n The partition number to reap.
     */
    protected static void reap(PartitionManager m, int n) {
        DiskPartition.reap(m, n);

        File vlf = m.makeVectorLengthFile(n);
        if (!vlf.delete()) {
            logger.warning(String.format(
                    "Failed to delete vector lengths file for %d", n));
        }
    }
}
