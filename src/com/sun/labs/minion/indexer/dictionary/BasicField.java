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
package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.FieldValue;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.CDateParser;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.FileReadableBuffer;
import com.sun.labs.minion.util.buffer.NIOBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.CasedIDEntry;
import com.sun.labs.minion.indexer.entry.IDEntry;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.Occurrence;
import com.sun.labs.minion.indexer.postings.OccurrenceImpl;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.ArrayGroup.DocIterator;
import com.sun.labs.minion.retrieval.ScoredGroup;
import com.sun.labs.minion.retrieval.ScoredQuickOr;
import com.sun.labs.minion.util.buffer.FileWriteableBuffer;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to hold the data for a saved field during indexing.
 *
 * @see FieldInfo
 * @see MemoryFieldStore
 */
public class BasicField implements SavedField {

    /**
     * The field info object for this field.  Used during indexing.
     */
    protected FieldInfo field;

    /**
     * A dictionary to use for the saved field data.
     */
    protected Dictionary values;

    /**
     * An array of the sets of entries stored per document at indexing time.
     */
    protected List[] dv;

    /**
     * The current postition in the dtv array, that is, where the next
     * document ID will be added.
     */
    protected int dvPos;

    /**
     * A buffer containing the dtv offsets at query time.
     */
    protected ReadableBuffer dtvOffsets;

    /**
     * A buffer containing the actual dtv data at query time.
     */
    protected ReadableBuffer dtvData;

    /**
     * A bigram dictionary that we can use for character fields.
     */
    protected DiskBiGramDictionary bigrams;

    /**
     * The number of bytes we're using to store data.
     */
    protected int nBytes;

    /**
     * The header for this field.
     */
    protected SavedFieldHeader header;

    /**
     * A date parser for date fields.
     */
    protected CDateParser dp;

    /**
     * The log.
     */
    Logger logger = Logger.getLogger(getClass().getName());

    /**
     * The log tag.
     */
    protected static String logTag = "BF";

    /**
     * Default constructor for subclasses.
     */
    protected BasicField() {
    }

    /**
     * Constructs a saved field that will be used to store data during
     * indexing.
     *
     * @param field The <code>FieldInfo</code> for this saved field.
     */
    public BasicField(FieldInfo field) {
        this.field = field.clone();
        values = new MemoryDictionary(getEntryClass(field));
        dv = new List[1024];
        if(field.getType() == FieldInfo.Type.DATE) {
            dp = new CDateParser();
        }
    }

    /**
     * Constructs a saved field that will be used to retrieve data during
     * querying.
     *
     * @param field The <code>FieldInfo</code> for this saved field.
     * @param dictFile The file containing the dictionary for this field.
     * @param postFiles The files containing the postings for this field.
     * @param part The disk partition that this field is associated with.
     *
     * @throws java.io.IOException if there is any error loading the field
     * data.
     */
    public BasicField(FieldInfo field, RandomAccessFile dictFile,
            RandomAccessFile[] postFiles,
            DictionaryFactory fieldStoreDictFactory,
            DictionaryFactory bigramDictFactory, DiskPartition part) throws java.io.IOException {
        this.field = (FieldInfo) field.clone();

        header = new SavedFieldHeader(dictFile);

        logger.finer("Loading dictionary for field: " + field.getName());
        dictFile.seek(header.valOffset);
        values =
                fieldStoreDictFactory.getDiskDictionary(
                BasicField.getEntryClass(field),
                BasicField.getNameDecoder(field), dictFile, postFiles, part);
        ((DiskDictionary) values).setName(String.format("%s-values", field.getName()));

        //
        // If we're a character field, load our bigrams.
        if(field.getType() == FieldInfo.Type.STRING) {
            dictFile.seek(header.bgOffset);
            bigrams =
                    bigramDictFactory.getBiGramDictionary(
                    (DiskDictionary) values,
                    dictFile, postFiles[0], part);
            bigrams.setName(String.format("%s-bigrams", field.getName()));
        }

        logger.finer("Loading docsToValues for field: " + field.getName());

        //
        // Load the docs to vals data, using a file backed buffer.
        dtvData = new FileReadableBuffer(postFiles[0], header.dtvOffset, 8192);

        //
        // Load the docs to vals offset data, using a file backed buffer.
        dtvOffsets =
                new FileReadableBuffer(postFiles[0], header.dtvOffsetOffset,
                8192);
    }

    /**
     * Gets an entry class appropriate to the type of the given field.
     */
    public static Class getEntryClass(FieldInfo field) {

        //
        // We'll use a cased IDEntry for string fields that we want to do
        // case sensitive querying on.
        if(field.getType() == FieldInfo.Type.STRING && !field.isCaseSensitive()) {
            return CasedIDEntry.class;
        }

        //
        // Otherwise, we just use a plain old IDEntry..
        return IDEntry.class;
    }

    /**
     * Adds data to a saved field.
     *
     * @param docID the document ID for the document containing the saved
     * data
     * @param data The actual field data.
     */
    public void add(int docID, Object data) {

        Object name = getEntryName(data);

        //
        // If we had a failure, then just return.
        if(name == null) {
            return;
        }

        IndexEntry e = (IndexEntry) values.get(name);

        if(e == null) {
            e = ((MemoryDictionary) values).newEntry(name);
            values.put(name, e);
        }

        //
        // The astute will notice that we don't add any postings data to the
        // entry that we just pulled out.  This is because we may be adding 
        // data in non-document ID order, for example, when doing classification
        // on new documents.  We'll add the postings data in the dump method
        // once we've got everything sorted out and can add the data in order.
        // o.setID(docID);
        // e.add(o);

        //
        // Make sure that we have a list for this document.
        if(docID >= dv.length) {
            List[] temp = new List[(docID + 1) * 2];
            System.arraycopy(dv, 0, temp, 0, dv.length);
            dv = temp;
        }
        if(dv[docID] == null) {
            dv[docID] = new ArrayList<IndexEntry>();
        }
        dv[docID].add(e);
    }

    /**
     * Gets a name decoder of the appropriate type for the given field.
     *
     * @param field The field for which we want a name decoder.
     */
    protected static NameDecoder getNameDecoder(FieldInfo field) {
        switch(field.getType()) {
            case INTEGER:
                return new LongNameHandler();
            case FLOAT:
                return new DoubleNameHandler();
            case DATE:
                return new DateNameHandler();
            case STRING:
                return new StringNameHandler();
            default:
                Logger.getLogger(BasicField.class.getName()).warning("Field: " + field.getName() + " " +
                        "has unknown SAVED type: " + field.getType() +
                        ", using VARCHAR.");
                return new StringNameHandler();
        }
    }

    /**
     * Gets a name encoder of the appropriate type for the given field.
     *
     * @param field The field for which we want a name encoder.
     */
    protected static NameEncoder getNameEncoder(FieldInfo field) {
        return (NameEncoder) getNameDecoder(field);
    }

    /**
     * Gets a name for a given saved value, parsing as necessary.
     *
     * @param val The value that we were passed.
     * @return A name appropriate for the given value and field type.
     */
    protected Object getEntryName(Object val) {
        if(val == null) {
            return null;
        }

        switch(field.getType()) {
            case INTEGER:
                try {
                    if(val instanceof Integer) {
                        return new Long(((Integer) val).intValue());
                    } else if(val instanceof Long) {
                        return val;
                    } else {
                        return new Long(val.toString());
                    }
                } catch(NumberFormatException nfe) {
                    logger.warning("Non integer value: " + val +
                            " for integer saved field " + field.getName() +
                            ", ignoring " + val.getClass());
                    return null;
                }
            case FLOAT:
                try {
                    if(val instanceof Double) {
                        return val;
                    } else if(val instanceof Float) {
                        return new Double(((Float) val).floatValue());
                    } else {
                        return new Double(val.toString());
                    }
                } catch(NumberFormatException nfe) {
                    logger.warning("Non float value: " + val +
                            " for float saved field " + field.getName() +
                            ", ignoring");
                    return null;
                }
            case DATE:
                try {
                    if(val instanceof Date) {
                        return val;
                    } else if(val instanceof Long) {
                        return new Date(((Long) val).longValue());
                    } else if(val instanceof Integer) {
                        return new Date(((long) ((Integer) val).intValue()) *
                                1000);
                    } else {
                        return dp.parse(val.toString());
                    }
                } catch(java.text.ParseException pe) {
                    logger.warning("Non-parseable date: " + val +
                            " for date saved field " + field.getName() +
                            ", ignoring");
                    return null;
                }
            case STRING:
                return val.toString();
            default:
                logger.warning("Field: " + field.getName() + " " +
                        "has unknown SAVED type: " + field.getType() +
                        ", using VARCHAR.");
                return val.toString();
        }
    }

    /**
     * Writes the data to the provided stream.
     *
     * @param path The path of the index directory.
     * @param dictFile The file where the dictionary will be written.
     * @param postOut A place to write the postings associated with the
     * values.
     * @param maxID The maximum document ID for this partition.
     * @throws java.io.IOException if there is an error during the
     * writing.
     */
    public void dump(String path, RandomAccessFile dictFile,
            PostingsOutput[] postOut, int maxID) throws java.io.IOException {

        long headerPos = dictFile.getFilePointer();
        header = new SavedFieldHeader();
        header.write(dictFile);

        logger.finer("Dump values dictionary for field: " +
                field.getName());

        header.nDocs = maxID;
        header.valOffset = dictFile.getFilePointer();

        //
        // We didn't add any occurrence data while we were adding values, since
        // we might have added data in non-document ID order.  We can do that now
        // since we can process things in document ID order.
        Occurrence o = new OccurrenceImpl();
        for(int i = 0; i < dv.length; i++) {
            if(dv[i] != null) {
                o.setID(i);
                for(IndexEntry e : (List<IndexEntry>) dv[i]) {
                    e.add(o);
                }
            }
        }

        Entry[] vals = ((MemoryDictionary) values).dump(path,
                BasicField.getNameEncoder(field), dictFile, postOut,
                MemoryDictionary.Renumber.RENUMBER,
                MemoryDictionary.IDMap.OLDTONEW,
                null);

        //
        // If we're a character saved field, then we need to dump some
        // bigrams.
        if(field.getType() == FieldInfo.Type.STRING) {
            MemoryBiGramDictionary bg = new MemoryBiGramDictionary(vals);
            header.bgOffset = dictFile.getFilePointer();
            bg.dump(path, new StringNameHandler(), dictFile, postOut,
                    MemoryDictionary.Renumber.RENUMBER,
                    MemoryDictionary.IDMap.NONE, null);
            bg.clear();
        }

        //
        // Dump the map from document IDs to field value IDs, remapping
        // them with the map we got when we dumped the values.
        int[] temp = new int[maxID];
        WriteableBuffer dtvBuff = new NIOBuffer(32768, true);
        for(int i = 1; i <= maxID; i++) {
            temp[i - 1] = dtvBuff.position();

            //
            // If there's no set here, or we're past the end of the array,
            // encode 0 for the count of items for this document ID.
            if(i >= dv.length || dv[i] == null) {
                dtvBuff.byteEncode(0);
                continue;
            }
            List<IndexEntry> dvs = (List<IndexEntry>) dv[i];
            dtvBuff.byteEncode(dvs.size());
            for(IndexEntry e : dvs) {
                dtvBuff.byteEncode(e.getID());
            }
        }

        //
        // Now encode the offsets into the dtv buffer using the smallest
        // number of bytes.
        header.offsetBytes = NIOBuffer.bytesRequired(dtvBuff.position());
        WriteableBuffer dtvOffsetsOut = new NIOBuffer(32768, true);
        for(int i = 0; i < temp.length; i++) {
            dtvOffsetsOut.byteEncode(temp[i], header.offsetBytes);
        }
        temp = null;

        //
        // Write the maps, recording the offset and size data in our
        // header.
        header.dtvOffset = postOut[0].position();
        header.dtvSize = dtvBuff.position();
        postOut[0].write(dtvBuff);

        header.dtvOffsetOffset = postOut[0].position();
        header.dtvOffsetSize = dtvOffsetsOut.position();
        postOut[0].write(dtvOffsetsOut);

        //
        // Now zip back and write the header.
        long end = dictFile.getFilePointer();
        dictFile.seek(headerPos);
        header.write(dictFile);
        dictFile.seek(end);
    }

    /**
     * Indicates whether a given document has saved data for this field.
     *
     * @param docID the document ID for the document that we wish to check.
     * @return <code>true</code> if this document ID has saved values,
     * <code>false</code> otherwise.
     */
    public boolean hasSavedValues(int docID) {

        //
        // Get the offset where we need to look in the dtv data.
        int pos = header.offsetBytes * (docID - 1);

        //
        // Check if this is within bounds if not, then there's no data.
        if(pos >= header.dtvOffsetSize) {
            return false;
        }

        //
        // Check the number of stored values.  If it's 0, then there's no saved
        // data.
        synchronized(dtvOffsets) {
            int off = dtvOffsets.byteDecode(pos, header.offsetBytes);
            dtvData.position(off);
            return dtvData.byteDecode() != 0;
        }
    }

    /**
     * Retrieve data from a saved field.
     *
     * @param docID the document ID that we want data for.
     * @param all If <code>true</code>, return all known values for the
     * field in the given document.  If <code>false</code> return only one
     * value.
     * @return If <code>all</code> is <code>true</code>, then return a
     * <code>List</code> of the values stored in the given field in the
     * given document.  If <code>all</code> is <code>false</code>, a single
     * value of the appropriate type will be returned.
     *
     * <P>
     *
     * If the given name is not the name of a saved field, or the document
     * ID is invalid, <code>null</code> will be returned.
     */
    public Object getSavedData(int docID, boolean all) {

        //
        // Get the offset where we need to look in the dtv data.
        int pos = header.offsetBytes * (docID - 1);

        //
        // Check if this is within bounds.
        if(pos >= header.dtvOffsetSize) {
            return null;
        }

        //
        // Pull the IDs of the values that we want.
        int[] vals = null;
        synchronized(dtvOffsets) {
            int off = dtvOffsets.byteDecode(pos, header.offsetBytes);
            dtvData.position(off);
            int n = dtvData.byteDecode();
            vals = new int[n];
            for(int i = 0; i < n; i++) {
                vals[i] = dtvData.byteDecode();
            }
        }

        //
        // Return all values?
        if(all) {
            ArrayList ret = new ArrayList(vals.length);
            for(int i = 0; i < vals.length; i++) {

                //
                // Get the term with the ID from the postings from our main
                // dictionary, and add its name to the return List.
                Entry val = ((DiskDictionary) values).getByID(vals[i]);
                if(val != null) {
                    ret.add(val.getName());
                }
            }
            return ret;
        }

        //
        // Return just the first value.
        if(vals.length != 0) {
            Entry val = ((DiskDictionary) values).getByID(vals[0]);
            if(val != null) {
                return val.getName();
            }
        }

        return null;
    }

    /**
     * Gets a particular value from the field.
     *
     * @param v The value to get.
     * @param caseSensitive If true, case should be taken into account when
     * iterating through the values.  This value will only be observed for
     * character fields!
     * @return The term associated with that name, or <code>null</code> if
     * that term doesn't occur in the indexed material.
     */
    public QueryEntry get(Object v, boolean caseSensitive) {

        Object name = getEntryName(v);

        //
        // If this is a character field and we're supposed to do a case
        // insensitive lookup, do so now.
        if(field.getType() == FieldInfo.Type.STRING && !caseSensitive) {
            name = CharUtils.toLowerCase((String) name);
        }

        return (QueryEntry) values.get(name);
    }

    /**
     * Gets a group of all the documents that do not have any values saved for
     * this field.
     *
     * @param ag a set of documents to which we should restrict the search for
     * documents with undefined field values.  If this is <code>null</code> then
     * there is no such restriction.
     * @return a set of documents that have no defined values for this field.
     * This set may be restricted to documents occurring in the group that was
     * passed in.
     */
    public ArrayGroup getUndefined(ArrayGroup ag) {

        ArrayGroup ret = new ArrayGroup(ag == null ? 2048 : ag.getSize());

        //
        // Get local copies of the buffers.
        ReadableBuffer ldtvo = dtvOffsets.duplicate();
        ReadableBuffer ldtv = dtvData.duplicate();

        if(ag == null) {

            //
            // If there's no restriction set, then do all documents.
            ldtvo.position(0);
            for(int i = 0; i < header.nDocs; i++) {

                //
                // Jump to each offset and decode the number of saved values.  If
                // that's 0, then we have a document that has no defined values, so
                // we put it in the return set.
                ldtv.position(ldtvo.byteDecode(header.offsetBytes));
                if(ldtv.byteDecode() == 0) {
                    ret.addDoc(i + 1);
                }
            }
        } else {

            //
            // Just check for the documents in the set.
            for(DocIterator i = ag.iterator(); i.next();) {
                ldtvo.position((i.getDoc() - 1) * header.offsetBytes);
                ldtv.position(ldtvo.byteDecode(header.offsetBytes));
                if(ldtv.byteDecode() == 0) {
                    ret.addDoc(i.getDoc());
                }
            }
        }

        return ret;
    }

    public ArrayGroup getSimilar(ArrayGroup ag, String value, boolean matchCase) {
        int[] var = bigrams.getAllVariants(value, true);
        int maxd = Integer.MIN_VALUE;
        PriorityQueue<IntEntry> h = new PriorityQueue<IntEntry>();
        if(!matchCase) {
            value = CharUtils.toLowerCase(value);
        }
        for(int i = 0; i < var.length; i++) {
            QueryEntry qe = ((DiskDictionary) values).getByID(var[i]);
            String name = qe.getName().toString();
            if(!matchCase) {
                name = CharUtils.toLowerCase(name);
            }
            int d = Util.levenshteinDistance(value, name);
            maxd = Math.max(d, maxd);
            h.offer(new IntEntry(d, qe));
        }

        ScoredQuickOr qor =
                new ScoredQuickOr((DiskPartition) values.getPartition(),
                h.size());
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
        feat.setCaseSensitive(true);
        while(h.size() > 0) {
            IntEntry e = h.poll();
            PostingsIterator pi = e.e.iterator(feat);
            if(pi == null) {
                continue;
            }
            qor.add(pi, (maxd - e.i) / (float) maxd, 1);
        }
        ScoredGroup sg = (ScoredGroup) qor.getGroup();
        sg.setNormalized();
        return sg;
    }

    public SortedSet<FieldValue> getMatching(String pattern) {
        SortedSet<FieldValue> ret = new TreeSet<FieldValue>();
        //
        // Check for all asterisks, indicating that we should return everything.
        // We could be fancier here and catch question marks and only return things
        // of the appropriate length.
        if(pattern.matches("\\*+")) {
            DictionaryIterator di = ((DiskDictionary) values).iterator();
            while(di.hasNext()) {
                ret.add(new FieldValue(((QueryEntry) di.next()).getName().
                        toString(), 1));
            }
        } else {
            QueryEntry[] m = ((DiskDictionary) values).getMatching(bigrams,
                    pattern, false, -1, -1);
            float l = pattern.length();
            for(QueryEntry qe : m) {
                String n = qe.getName().toString();
                ret.add(new FieldValue(n, l / n.length()));
            }
        }
        return ret;
    }

    /**
     * Gets an iterator for the values in this field.  If this is a string field
     * only the values that actually occurred will be returned.
     *
     * @param lowerBound the name of the entry that will be the lower bound of
     * the iterator, or <code>null</code> if there is no such bound
     * @param includeLower whether the lower bound should be included in the
     * results of the iterator
     * @param upperBound the name of the entry that will be the upper bound of
     * the iterator, or <code>null</code> if there is no such bound
     * @param includeUpper whether the upper bound should be included in the
     * results of the iterator
     * @return an iterator for the entries in the dictionary
     */
    public DictionaryIterator iterator(Object lowerBound, boolean includeLower,
            Object upperBound, boolean includeUpper) {
        DictionaryIterator di =
                ((DiskDictionary) values).iterator(getEntryName(lowerBound),
                includeLower, getEntryName(upperBound), includeUpper);
        di.setActualOnly(true);
        return di;
    }

    /**
     * Merges a number of saved fields.
     *
     * @param path The path to the index directory.
     * @param fields An array of fields to merge.
     * @param maxID The max doc ID in the new partition
     * @param starts The new starting document IDs for the partitions.
     * @param docIDMaps A map for each partition from old document IDs to
     * new document IDs.  IDs that map to a value less than 0 have been
     * deleted.  A null array means that the old IDs are the new IDs.
     * @param dictFile The file to which the merged dictionaries will be
     * written.
     * @param postOut The output to which the merged postings will be
     * written.
     * @throws java.io.IOException if there is an error during the merge.
     */
    public void merge(String path, SavedField[] fields, int maxID, int[] starts,
            int[] nUndel, int[][] docIDMaps, RandomAccessFile dictFile,
            PostingsOutput postOut) throws java.io.IOException {

        //
        // Write a placeholder for the header.
        long headerPos = dictFile.getFilePointer();
        SavedFieldHeader mHeader = new SavedFieldHeader();
        mHeader.write(dictFile);

        //
        // Figure out how many non-null dictionaries there are.
        int nonNull = 0;
        for(int i = 0; i < fields.length; i++) {
            if(fields[i] != null) {
                nonNull++;
            }
        }

        DiskDictionary[] dicts = new DiskDictionary[nonNull];
        DiskBiGramDictionary[] bgdicts = new DiskBiGramDictionary[nonNull];
        BasicField[] nnFields = new BasicField[nonNull];
        int[] nnStarts = new int[nonNull];
        int[][] nnDocIDMaps = new int[nonNull][];

        //
        // Collect the non-null dictionaries into our dicts array.
        for(int i = 0, n = 0; i < fields.length; i++) {
            if(fields[i] != null) {
                dicts[n] = (DiskDictionary) ((BasicField) fields[i]).values;
                bgdicts[n] = ((BasicField) fields[i]).bigrams;
                nnFields[n] = (BasicField) fields[i];
                nnStarts[n] = starts[i];
                nnDocIDMaps[n++] = docIDMaps[i];
            }
        }

        IndexEntry ef;
        try {
            ef = (IndexEntry) BasicField.getEntryClass(field).newInstance();
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error instantiating entry factory", e);
            ef = null;
        }

        //
        // Merge the field values.
        PostingsOutput[] mergeOut = new PostingsOutput[]{postOut};

        logger.fine("Merge dictionary for field: " + field.getName());
        mHeader.valOffset = dictFile.getFilePointer();
        int[][] idMap;

        idMap =
                dicts[0].merge(ef, BasicField.getNameEncoder(field), dicts, null,
                nnStarts, nnDocIDMaps, dictFile, mergeOut, true);

        //
        // Merge the bigram dictionaries, if there are any.
        if(bgdicts[0] != null) {
            mHeader.bgOffset = dictFile.getFilePointer();
            bgdicts[0].merge(bgdicts, nnStarts, idMap, dictFile, mergeOut[0]);
        }

        //
        // Merge the docs to vals data.
        int[] temp = new int[1024];
        File mdtvFile = Util.getTempFile(path, "mdtv", ".dtv");
        RandomAccessFile mdtvRAF = new RandomAccessFile(mdtvFile, "rw");
        WriteableBuffer mdtvData = new FileWriteableBuffer(mdtvRAF, 32768);

        //
        // Go through all the fields.  Some may be null, but that doesn't
        // mean that we didn't include documents from them in the merge above,
        // only that they didn't have any values for this field.  If this is the
        // case, we need to write out the appropriate number of zeros here.
        int currNonNullField = 0;
        for(int i = 0; i < fields.length; i++) {

            //
            // Add the number of undeleted documents for this partition.  Make sure that we have enough
            // room for the data that we're about to add.
            mHeader.nDocs += nUndel[i];
            if(mHeader.nDocs >= temp.length) {
                temp = Util.expandInt(temp, Math.max(mHeader.nDocs * 2,
                        temp.length * 2));
            }

            //
            // Special case here for null fields
            if(fields[i] == null) {

                //
                // No data so all positions are zero.
                for(int j = 0, p = starts[i] - 1; j < nUndel[i]; j++, p++) {
                    //
                    // Note the position we're about to write at
                    temp[p] = mdtvData.position();
                    mdtvData.byteEncode(0);
                }
                continue;
            }

            ReadableBuffer dtvDup = ((BasicField) fields[i]).dtvData.duplicate();
            int[] docIDMap = docIDMaps[i];
            int[] valIDMap = idMap[currNonNullField++];

            for(int j = 0, p = starts[i] - 1; j <
                    ((BasicField) fields[i]).header.nDocs; j++) {
                int n = dtvDup.byteDecode();

                if(docIDMap != null && docIDMap[j + 1] < 0) {
                    //
                    // Skip this document's data.
                    for(int k = 0; k < n; k++) {
                        dtvDup.byteDecode();
                    }
                } else {

                    //
                    // Note the position of this data.
                    temp[p++] = mdtvData.position();

                    //
                    // Re-encode the data, keeping in mind that the IDs for
                    // the values were remapped by the values merge above.
                    mdtvData.byteEncode(n);
                    for(int k = 0; k < n; k++) {
                        int id = valIDMap[dtvDup.byteDecode()];
                        mdtvData.byteEncode(id);
                    }
                }
            }
        }

        //
        // Now encode the offsets.
        mHeader.offsetBytes = NIOBuffer.bytesRequired(mdtvData.position());
        File mdtvOffFile = Util.getTempFile(path, "mdtv", ".dtv");
        RandomAccessFile mdtvOffRAF = new RandomAccessFile(mdtvOffFile, "rw");
        WriteableBuffer mdtvOffsets = new FileWriteableBuffer(mdtvOffRAF, 32768);
        for(int i = 0; i < mHeader.nDocs; i++) {
            mdtvOffsets.byteEncode(temp[i], mHeader.offsetBytes);
        }

        //
        // Write the data.
        mHeader.dtvOffset = postOut.position();
        mHeader.dtvSize = mdtvData.position();
        postOut.write(mdtvData);

        mHeader.dtvOffsetOffset = postOut.position();
        mHeader.dtvOffsetSize = mdtvOffsets.position();
        postOut.write(mdtvOffsets);

        mdtvRAF.close();
        if(!mdtvFile.delete()) {
            logger.severe(
                    "Failed to delete docs to values buffer file after merge");
        }
        mdtvOffRAF.close();
        if(!mdtvOffFile.delete()) {
            logger.severe(
                    "Failed to delete docs to values offset buffer file after merge");
        }

        long end = dictFile.getFilePointer();

        //
        // Go back and write our header.
        dictFile.seek(headerPos);
        mHeader.write(dictFile);
        dictFile.seek(end);
    }

    protected Iterator valueIterator() {
        return values.iterator();
    }

    /**
     * Gets the number of saved terms that we're storing.
     */
    public int size() {
        return values.size();
    }

    /**
     * Clears a saved field, if it's open for indexing.
     */
    public void clear() {
        if(values instanceof MemoryDictionary) {
            ((MemoryDictionary) values).clear();
            for(List s : dv) {
                if(s != null) {
                    s.clear();
                }
            }
        }
    }

    /**
     * Compares saved fields according to the field ID.
     */
    public int compareTo(Object o) {
        return field.getID() - ((BasicField) o).field.getID();
    }

    /**
     * Get the field info object for this field.  Used during indexing.
     *
     * @return the FieldInfo
     */
    public FieldInfo getField() {
        return field;
    }

    public Fetcher getFetcher() {
        return new Fetcher();
    }

    /**
     * A class that can be used when you want to get a lot of field values for
     * a particular field, for example, when sorting or clustering results
     * by a particular field.
     */
    public class Fetcher {

        /**
         * A local copy of the docs-to-vals offset buffer.
         */
        private ReadableBuffer ldtvo;

        /**
         * A local copy of the docs-to-vals buffer.
         */
        private ReadableBuffer ldtv;

        public Fetcher() {
            ldtvo = dtvOffsets.duplicate();
            ldtv = dtvData.duplicate();
        }

        /**
         * Fetches one value for the given field.
         */
        public Object fetchOne(int docID) {
            ldtv.position(ldtvo.byteDecode(header.offsetBytes * (docID - 1),
                    header.offsetBytes));
            int n = ldtv.byteDecode();
            if(n == 0) {
                return field.getDefaultSavedValue();
            }
            return ((DiskDictionary) values).getByID(ldtv.byteDecode()).getName();
        }

        public List fetch(int docID) {
            return fetch(docID, new ArrayList());
        }

        public List fetch(int docID, List l) {
            ldtv.position(ldtvo.byteDecode(header.offsetBytes * (docID - 1),
                    header.offsetBytes));
            int n = ldtv.byteDecode();
            for(int i = 0; i < n; i++) {
                l.add(((DiskDictionary) values).getByID(ldtv.byteDecode()).getName());
            }
            return l;
        }
    }
} // BasicField

