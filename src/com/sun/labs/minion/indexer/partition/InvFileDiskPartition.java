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

import java.io.IOException;
import java.util.List;
import java.io.File;
import java.io.RandomAccessFile;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.DiskDictionaryBundle.Fetcher;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    public InvFileDiskPartition(int partNumber, PartitionManager manager, 
                                                EntryFactory mainDictEntryFactory)
            throws java.io.IOException {
        super(partNumber, manager, Postings.Type.NONE);

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
                   
            dictFile.seek(offset.getOffset());
            FieldInfo info = manager.getMetaFile().getFieldInfo(offset.getId());
            fields[offset.getId()] =
                    new DiskField(this, info, dictFile, vectorLengths,
                    postFiles, mainDictEntryFactory);
        }
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
            fields = new HashSet<FieldInfo>(manager.engine.getQueryConfig().getDefaultFields());
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

    protected void mergeCustom(int newPartNumber,
            PartitionHeader mHeader,
            DiskPartition[] sortedParts,
            int[][] idMaps,
            int newMaxDocID, int[] docIDStart, int[] nUndel,
            int[][] docIDMaps,
            RandomAccessFile mDictFile,
            PostingsOutput[] mPostOut)
            throws Exception {

        File vlf = manager.makeVectorLengthFile(partNumber);
        RandomAccessFile mVectorLengths = new RandomAccessFile(vlf, "rw");

        for (Iterator<FieldInfo> i = manager.getMetaFile().fieldIterator(); i.
                hasNext();) {
            FieldInfo fi = i.next();
            DiskField[] fields = new DiskField[sortedParts.length];
            DiskField merger = null;
            for (int j = 0; j < sortedParts.length; j++) {
                fields[j] = ((InvFileDiskPartition) sortedParts[j]).getDF(fi);
                if (fields[j] != null) {
                    merger = fields[j];
                }
            }
            merger.merge(manager.getIndexDir(),
                    fields, docIDStart, docIDMaps, nUndel, mDictFile,
                    mPostOut,
                    mVectorLengths);
        }

        mVectorLengths.close();
    }

    /**
     * Close the files associated with this partition.
     */
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
        File[] files = getMainFiles(m, n);
        for (File f : files) {
            if ((!f.delete()) && (f.exists())) {
                logger.warning("Failed to delete: " + f);
            }
        }

        File vlf = m.makeVectorLengthFile(n);
        if (!vlf.delete()) {
            logger.severe(String.format(
                    "Failed to delete vector lengths file %s", vlf));
        }

        //
        // Remove the deletion bitmap and the removed partition files.
        if (!m.makeDeletedDocsFile(n).delete()) {
            logger.severe("Failed to reap partition " + n);
        }
        if (!m.makeRemovedPartitionFile(n).delete()) {
            logger.severe("Failed to reap partition " + n);
        }
    }

}
