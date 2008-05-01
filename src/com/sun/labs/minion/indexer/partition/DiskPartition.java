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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.sun.labs.minion.indexer.Closeable;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DictionaryFactory;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.DuplicateKeyException;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryMapper;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.FileLock;
import com.sun.labs.minion.util.buffer.ReadableBuffer;

/**
 * A partition of the index which is resident on the disk and suitable for
 * querying.
 *
 * <p>
 *
 * A disk partition consists of four things:
 *
 * <ul>
 * <li> A main dictionary and associated postings.  This is the dictionary where
 * we will look up most query terms
 * <li> A document dictionary and associated postings.  This dictionary contains
 * the keys for the documents in this partition.
 * <li> A set of document vector lengths for this partition.
 * <li> A map of the documents that have been deleted from this partition.
 * </ul>
 *
 * @see com.sun.labs.minion.indexer.dictionary.DiskDictionary
 * @see DocumentVectorLengths
 *
 */
public class DiskPartition extends Partition implements Closeable {

    /**
     * The files containing the main data.
     */
    protected File[] mainFiles;

    /**
     * A factory for the document dictionary.
     */
    protected DictionaryFactory documentDictFactory;

    /**
     * The main dictionary.
     */
    protected DiskDictionary mainDict;

    /**
     * The document dictionary.
     */
    protected DiskDictionary docDict;

    /**
     * The stream for the document dictionary.
     */
    protected RandomAccessFile docDictFile;

    /**
     * The postings stream for the document dictionary.
     */
    protected RandomAccessFile docPostFile;

    /**
     * The deleted documents file.
     */
    protected File delFile;

    /**
     * A lock for the deleted documents file.
     */
    protected FileLock delFileLock;

    /**
     * A <code>File</code> indicating that this partition is no longer
     * active.
     */
    protected File removedFile;

    /**
     * The deletion map for this partition.
     */
    protected DelMap deletions;

    /**
     * The lengths of the document vectors for this partition.
     */
    protected DocumentVectorLengths dvl;

    /**
     * Whether this partition was ignored during a merge, due to it being
     * empty.
     */
    protected boolean ignored;

    /**
     * The time at which it is safe to close this partition.
     */
    private long closeTime;

    /**
     * The tag for this module.
     */
    protected static String logTag = "DP";

    /**
     * Buffer size for merging.
     */
    protected static int BUFF_SIZE = 1024 * 1024;

    /**
     * Minimum length of a stem.
     */
    protected static int MIN_LEN = 3;

    /**
     * The limit for variant entries relationship to a stemmed entry.
     */
    protected static float MATCH_CUT_OFF = (float) 0.65;

    /**
     * Opens a partition with a given number
     *
     * @param partNumber the number of this partition.
     * @param manager the manager for this partition.
     * @param mainDictFactory the dictionary factory that we will use to create
     * the main dictionary
     * @param documentDictFactory the dictionary factory that we will use to
     * create the document dictionary
     * @throws java.io.IOException If there is an error opening or reading
     * any of the files making up a partition.
     *
     * @see Partition
     * @see Dictionary
     *
     */
    public DiskPartition(int partNumber,
            PartitionManager manager,
            DictionaryFactory mainDictFactory,
            DictionaryFactory documentDictFactory)
            throws java.io.IOException {
        this.partNumber = partNumber;
        this.manager = manager;
        this.mainDictFactory = mainDictFactory;
        this.documentDictFactory = documentDictFactory;

        mainFiles = getMainFiles();

        //
        // Get the deletion file and the removed file, as well as the lock
        // for the deleted docs file.
        delFile = this.manager.makeDeletedDocsFile(this.partNumber);
        removedFile = this.manager.makeRemovedPartitionFile(this.partNumber);
        delFileLock = new FileLock(manager.lockDirFile, delFile);

        //
        // Read the deletion bitmap.
        deletions = new DelMap(delFile, delFileLock);
        deletions.setPartition(this);

    //
    // We're done for now.  See the various init methods for the code
    // that actually instantiates things.
    }

    /**
     * Initializes the main dictionary and the document dictionary.
     *
     * @throws java.io.IOException if there is any error initializing the
     * dictionaries.
     */
    protected void initAll()
            throws java.io.IOException {
        initMainDict();
        initDocDict();
    }

    /**
     * Initializes the files used for the main dictionary and the associated
     * postings.
     * @throws java.io.IOException if there is any error opening the files
     */
    protected void initMainFiles()
            throws java.io.IOException {

        if(mainDictFile != null) {
            return;
        }

        try {

            //
            // Get a channel for the main dictionaries.
            mainDictFile = new RandomAccessFile(mainFiles[0], "r");

            //
            // Get channels for the postings.
            mainPostFiles = new RandomAccessFile[mainFiles.length - 1];
            for(int i = 1; i < mainFiles.length;
                    i++) {
                mainPostFiles[i - 1] = new RandomAccessFile(mainFiles[i], "r");
            }
        } catch(java.io.IOException ioe) {
            log.error(logTag, 1, "Error opening partition " + partNumber, ioe);
            mainDictFile = null;
            mainPostFiles = null;
            throw ioe;
        }
    }

    /**
     * Initializes the main dictionary, if necessary.
     */
    protected synchronized void initMainDict() {

        if(mainDict != null) {
            return;
        }

        try {
            initMainFiles();

            //
            // We'll give the main dictionary a larger cache.
            mainDict = mainDictFactory.getDiskDictionary(new StringNameHandler(),
                    mainDictFile,
                    mainPostFiles, this);
        } catch(java.io.IOException ioe) {
            log.error(logTag, 1,
                    "Error opening main dictionary for " + partNumber, ioe);
            mainDict = null;
        }
    }

    /**
     * Initializes the document dictionary, if necessary.
     */
    protected synchronized void initDocDict() {

        if(docDict != null) {
            return;
        }

        try {
            File[] files = getDocFiles();
            docDictFile = new RandomAccessFile(files[0], "r");
            docPostFile = new RandomAccessFile(files[1], "r");

            //
            // Get the number of docs, the total length and the individual
            // lengths.
            stats = new PartitionStats(docDictFile);

            //
            // Instantiate the dictionary.
            docDict =
                    documentDictFactory.getDiskDictionary(new StringNameHandler(),
                    docDictFile,
                    new RandomAccessFile[]{docPostFile},
                    this);
        } catch(java.io.IOException ioe) {
            log.error(logTag, 1,
                    "Error opening document dictionary for " + partNumber);
            docDict = null;
        }
    }

    /**
     * Initializes the document vector lengths.
     * @param adjustStats if it's necessary to compute the document vector lengths,
     * should we adjust the term statistics while we're at it?
     */
    protected synchronized void initDVL(boolean adjustStats) {
        if(dvl != null) {
            return;
        }
        try {
            dvl = new DocumentVectorLengths(this, adjustStats);
        } catch(java.io.IOException ioe) {
            log.error(logTag, 1,
                    "Error reading document vector lengths for " + this, ioe);
        }
    }

    /**
     * Gets the document vector lengths associated with this partition.
     * @return the document vector lengths associated with this partition
     */
    public DocumentVectorLengths getDVL() {
        initDVL(false);
        return dvl;
    }

    /**
     * Gets an iterator for the document keys in this partition.  All
     * documents, including those that have been deleted will be returned.
     *
     * @return an iterator for the entries in the document dictionary,
     * which have the document keys as their names.
     */
    public Iterator getDocumentIterator() {
        initDocDict();
        return docDict.iterator();
    }

    /**
     * Gets an iterator for some of the document keys in this partition.
     * All documents in the given range, including those that have been
     * deleted will be returned.
     *
     * @param begin the ID (inclusive) of the document at which we wish to begin
     * iteration
     * @param end the ID (exclusive) of the document at which we wish to end
     * iteration
     * @return an iterator for the entries in the document dictionary,
     * which have the document keys as their names.
     */
    protected Iterator getDocumentIterator(int begin, int end) {
        initDocDict();
        return docDict.iterator(begin, end);
    }

    /**
     * Gets an iterator for the entries in the main dictionary.
     * @return an iterator for the entries in the main dictionary.
     */
    public DictionaryIterator getMainDictionaryIterator() {
        initMainDict();
        return mainDict.iterator();
    }

    /**
     * Gets an iterator for the entries in the main dictionary.
     * @param start the name of the entry (inclusive) at which to start the
     * iteration
     * @param end the name of the entry (exclusive) at which to stop the iteration
     * @return an iterator that will return entries from the main dictionary
     * between the provided start and end names
     */
    public Iterator getMainDictionaryIterator(String start, String end) {
        initMainDict();
        return mainDict.iterator(start, true, end, true);
    }

    /**
     * Gets the entry from the document dictionary corresponding to a given
     * document key
     * @param key the document key
     * @return the entry in the document dictionary for this key or <code>null</code>
     * if this key does not occur in the document dictionary.
     */
    public DocKeyEntry getDocumentTerm(String key) {
        initDocDict();
        return (DocKeyEntry) docDict.get(key);
    }

    /**
     * Gets the entry from the document dictionary corresponding to a given
     * document ID
     * @param docID the document ID
     * @return the entry in the document dictionary for this key or <code>null</code>
     * if this key does not occur in the document dictionary.
     */
    public DocKeyEntry getDocumentTerm(int docID) {
        initDocDict();
        return (DocKeyEntry) docDict.get(docID);
    }

    /**
     * Gets the length of a document vector for a given document.  Note
     * that this may cause all of the document vector lengths for this
     * partition to be calculated!
     *
     * @param docID the ID of the document for whose vector we want the length
     * @return the length of the document.  If there are any errors getting
     * the length, a value of 1 is returned.
     */
    public float getDocumentVectorLength(int docID) {
        initDVL(false);
        return dvl.getVectorLength(docID);
    }

    /**
     * Gets the length of a document vector for a given document.  Note
     * that this may cause all of the document vector lengths for this
     * partition to be calculated!
     *
     * @param docID the ID of the document for whose vector we want the length
     * @param field the vectored field for which we we want the document vector
     * length.  If this value is <code>null</code> the length for all vectored fields
     * is returned.  If this value is the empty string, the length for the default
     * body field is returned.  If this value does not name a vectored field, a
     * default value of 1 will be returned.
     * @return the length of the document.  If there are any errors getting
     * the length, a value of 1 is returned.
     */
    public float getDocumentVectorLength(int docID, String field) {
        initDVL(false);
        return dvl.getVectorLength(docID, manager.getMetaFile().getVectoredFieldID(field));
    }

    /**
     * Gets the length of a document vector for a given document.  Note
     * that this may cause all of the document vector lengths for this
     * partition to be calculated!
     *
     * @param docID the ID of the document for whose vector we want the length
     * @param fieldID the ID of the field for which we want the length if this
     * value is less than 0, the length for all vectored fields
     * is returned.  If this value is 0, the length for the default
     * body field is returned.  Other wise, the length for the corresponding field
     * is returned.
     * @return the length of the document.  If there are any errors getting
     * the length, a value of 1 is returned.
     */
    public float getDocumentVectorLength(int docID, int fieldID) {
        initDVL(false);
        if(fieldID == -1) {
            return dvl.getVectorLength(docID);
        }
        return dvl.getVectorLength(docID, fieldID);
    }

    /**
     * Synchronizes the deletion map in memory with the one on disk.
     */
    protected void syncDeletedMap() {
        deletions.sync();
    }

    /**
     * Close the files associated with this partition.
     * @return <code>true</code> if the files were successfully closed.
     */
    public synchronized boolean close() {
        return close(Long.MAX_VALUE);
    }

    /**
     * Close the files associated with this partition, if enough time has
     * passed.  We normally want to delay the close of the dictionaries in
     * order to make sure that any queries in flight have completed.
     *
     * @param currTime the current time
     */
    public synchronized boolean close(long currTime) {

        //
        // Check if enough time has passed for this close
        // to succeed.
        if(closeTime > currTime) {
            return false;
        }
        try {

            syncDeletedMap();

            //
            // Close the main dictionary and postings.
            if(mainDictFile != null) {
                mainDictFile.close();
                for(int i = 0; i < mainPostFiles.length;
                        i++) {
                    mainPostFiles[i].close();
                }
            }

            if(docDict != null) {
                docDictFile.close();
                docPostFile.close();
            }

            if(dvl != null) {
                dvl.close();
            }
        } catch(java.io.IOException ioe) {
            log.error(logTag, 1, "Error closing partition", ioe);
        }
        return true;
    }

    /**
     * Deletes the files associated with this partition.
     */
    public void delete() {
        close();

        File[] files = getAllFiles();
        boolean fail = false;
        for(int i = 0; i < files.length;
                i++) {
            if(!files[i].delete()) {
                fail = true;
            }
        }

        if(fail) {
            log.warn(logTag, 2,
                    "Unable to remove all files for partition " + partNumber);
        }

        File remFile = manager.makeRemovedPartitionFile(partNumber);
        if(remFile.exists() && !remFile.delete()) {

            //
            // NON-FATAL ERROR:
            log.error(logTag, 1,
                    "Unable to remove deleted partition file: " + remFile);
        }
    }

    /**
     * Reaps the given partition.  If the postings file cannot be removed,
     * then we return control immediately.
     *
     * @param m The manager associated with the partition.
     * @param n The partition number to reap.
     */
    protected static void reap(PartitionManager m, int n) {

        //
        // Remove the data files.
        File[] files = getAllFiles(m, n);
        for(int i = 0; i < files.length;
                i++) {
            if((!files[i].delete()) && (files[i].exists())) {
                log.warn(logTag, 4, "Failed to delete: " + files[i]);
            }
        }

        //
        // Remove the deletion bitmap and the removed partition files.
        m.makeDeletedDocsFile(n).delete();
        m.makeRemovedPartitionFile(n).delete();
    }

    /**
     * Gets the entry in the main dictionary associated with a given name.  This is a
     * case-insensitive lookup.
     *
     * @param name The name of the term, as a string.
     * @return The term associated with that name.
     * @see #getTerm(String,boolean)
     */
    public QueryEntry getTerm(String name) {
        return getTerm(name, false);
    }

    /**
     * Gets the entry from the main dictionary that has a given ID.
     *
     * @param id The ID of the term that we want to get.
     * @return the entry associated with that ID, or <code>null</code> if the
     * ID is not in the main dictionary.
     */
    public QueryEntry getTerm(int id) {
        initMainDict();
        return (QueryEntry) mainDict.get(id);
    }

    /**
     * Gets the term associated with a given name.
     *
     * @param name The name of the term.
     * @param caseSensitive If <code>true</code> then the term should be
     * looked up in the case that it is given.
     * @return the entry from the main dicitionary associated with the given
     * name.
     */
    public QueryEntry getTerm(String name, boolean caseSensitive) {
        initMainDict();

        if(caseSensitive) {
            return (QueryEntry) mainDict.get(name);
        }

        return (QueryEntry) mainDict.get(CharUtils.toLowerCase(name));
    }

    /**
     * Checks to see whether a given document is indexed.  A document is
     * indexed if the provided key appears in the index and the associated
     * document ID has not been deleted.
     * @param key the key for the document that we want to check
     * @return <code>true</code> if this key occurs in this partition and the
     * document has not been deleted.
     */
    public boolean isIndexed(String key) {
        initDocDict();
        Entry d = docDict.get(key);
        return d != null && !isDeleted(d.getID());
    }

    /**
     * Deletes a document specified by the given ID.
     *
     * @param docID The ID of the file to delete.
     * @return true if the document is in this partition and was deleted,
     * false otherwise.
     */
    public boolean deleteDocument(int docID) {
        initDocDict();

        if(docID < 1 || docID > docDict.getMaxID()) {
            return false;
        }
        return deletions.delete(docID);
    }

    /**
     * Deletes a document specified by the given key, if it occurs in this
     * partition.
     *
     * @param key The document key to be deleted.
     * @return true if the document occurs in this partition and was
     * deleted, false otherwise.
     */
    public boolean deleteDocument(String key) {

        initDocDict();
        Entry d = docDict.get(key);

        if(d != null) {
            return deleteDocument(d.getID());
        }
        return false;
    }

    /**
     * Tells us whether a given document ID has been deleted.
     * @param docID the ID of the document that we want to check
     * @return <code>true</code> if the document has been deleted, <code>false</code>
     * otherwise.
     */
    public boolean isDeleted(int docID) {
        return deletions.isDeleted(docID);
    }

    /**
     * Updates the partition by deleting any documents whose keys are in
     * the given dictionary.  Available only to package mates.
     *
     * @param keys a set of keys to delete.  The string representation of the
     * elements of the set will be the keys to delete.
     * @return <code>true</code> if any documents were deleted,
     * <code>false</code> otherwise.
     */
    protected boolean updatePartition(Set<Object> keys) {

        //
        // First, update our deleted document bitmap.
        syncDeletedMap();

        //
        // Now, loop through the keys, deleting any that are from this
        // partition.
        boolean someDeleted = false;
        for(Object key : keys) {
            if(deleteDocument((String) key)) {
                someDeleted = true;
            }
        }
        if(someDeleted) {
            syncDeletedMap();
        }
        return someDeleted;
    }

    /**
     * Gets the number of documents in this partition.  This excludes
     * deleted documents.
     * @return the number of documents in this partition, not including
     * deleted documents
     */
    public int getNDocs() {
        initDocDict();
        return stats.nDocs - deletions.getNDeleted();
    }

    /**
     * Get the maximum document ID.  Note that
     * this value can be larger than the number of documents in the partition, due
     * to the presence of deleted documents.
     * @return the maximum ID assigned to a document in this partition.
     */
    public int getMaxDocumentID() {
        initDocDict();
        return docDict.getMaxID();
    }

    /**
     * Gets the maximum term ID from the main dictionary.
     * @return the maximum term ID in the dictionary
     */
    public int getMaxTermID() {
        initMainDict();
        return mainDict.getMaxID();
    }

    /**
     * Gets the length of a document (in words) qthat's in this partition.
     * @param docID the ID of the document for which we want the length
     * @return the length of the document
     * @see #getDocumentVectorLength(int) for a way to get the length of the
     * vector associated with this document
     */
    public int getDocumentLength(int docID) {
        initDocDict();
        QueryEntry de = getDocumentTerm(docID);
        if(de == null || !(de instanceof DocKeyEntry)) {
            return 0;
        }

        return ((DocKeyEntry) de).getDocumentLength();
    }

    /**
     * Get the average document length in this partition.
     * @return the average length (in words) of the documents in this partition
     */
    public float getAverageDocumentLength() {
        initDocDict();
        return stats.avgDocLen;
    }

    /**
     * Gets the total number of tokens indexed in this partition.
     * @return the total number of tokens
     */
    public long getNTokens() {
        initDocDict();
        return stats.nTokens;
    }

    /**
     * Gets the total number of distinct terms in this partition.
     * @return the number of distinct terms in the partition
     */
    public int getNEntries() {
        initDocDict();
        return stats.nd;
    }

    /**
     * Gets the map of deleted documents for this partition.
     *
     * @return the bitmap of deleted documents.
     */
    public ReadableBuffer getDeletedDocumentsMap() {
        return deletions.getDelMap();
    }

    public DelMap getDelMap() {
        return deletions;
    }
    /**
     * Returns a map from the document IDs in this partition to IDs in a
     * partition that has no deleted documents.
     *
     * @param del a buffer of deleted documents
     * @return An array of <code>int</code> containing the mapping, where
     * deleted documents map to < 0, or <code>null</code> if there are no
     * deleted documents.  The 0th element of the returned array contains
     * the number of undeleted documents.
     * @see DelMap#getDelMap
     */
    protected int[] getDocIDMap(ReadableBuffer del) {
        if(del == null) {
            return null;
        }

        int maxDocID = docDict.getMaxID();
        int[] map = new int[maxDocID + 1];

        //
        // Make the map from old to new document IDs.
        int nd = 1;
        for(int od = 1; od <= maxDocID; od++) {
            if(del.test(od)) {
                map[od] = -1;
            } else {
                map[od] = nd++;
            }
        }
        map[0] = nd - 1;
        return map;
    }

    /**
     * Gets an iterator for the entries in the main dictionary.
     *
     * @return an iterator for the entries in the main dictionary.
     */
    public Iterator getMainIterator() {
        return getMainDictionaryIterator(null, null);
    }

    /**
     * Gets an array of buffers to use for buffering postings during
     * merges.
     *
     * @param size The size of the input buffers to use.
     * @return An array of buffers large enough to handle any dictionary
     * merge.
     */
    protected ByteBuffer[] getInputBuffers(int size) {
        ByteBuffer[] ret = new ByteBuffer[mainPostFiles.length];
        for(int i = 0; i < ret.length;
                i++) {
            ret[i] = ByteBuffer.allocateDirect(size);
        }
        return ret;
    }

    /**
     * Returns the main dictionary, to be used by subclasses.
     *
     * @return the main dictionary
     */
    public DiskDictionary getMainDictionary() {
        initMainDict();
        return mainDict;
    }

    /**
     * Merges a number of <code>DiskPartition</code>s into a single
     * partition.
     *
     * @param partitions the partitions to merge
     * @param delMaps the state of the deletion maps for the partitions to 
     * merge before the merge started.  We need these to be the same as the
     * ones at the place where the merge was called for (see {@link PartitionManager#Merger}), 
     * otherwise we might get some skew in the maps between when they are recorded
     * there and recorded here!
     * @param calculateDVL if <code>true</code>, then calculate the document
     * vector lengths for the documents in the merged partition after the merge
     * is finished.
     * @return the newly-merged partition.
     * @throws Exception If there is any error during the merge.
     */
    public DiskPartition merge(List<DiskPartition> partitions,
            List<DelMap> delMaps,
            boolean calculateDVL)
            throws Exception {
        return merge(partitions, delMaps, calculateDVL, 0);
    }
    
    /**
     * Merges a number of <code>DiskPartition</code>s into a single
     * partition.
     *
     * @param partitions the partitions to merge
     * @param delMaps the state of the deletion maps for the partitions to 
     * merge before the merge started.  We need these to be the same as the
     * ones at the place where the merge was called for (see {@link PartitionManager#Merger}), 
     * otherwise we might get some skew in the maps between when they are recorded
     * there and recorded here!
     * @param calculateDVL if <code>true</code>, then calculate the document
     * vector lengths for the documents in the merged partition after the merge
     * is finished.
     * @return the newly-merged partition.
     * @throws Exception If there is any error during the merge.
     */
    public DiskPartition merge(List<DiskPartition> partitions,
            List<DelMap> delMaps,
            boolean calculateDVL, int depth)
            throws Exception {

        //
        // A copy of the partition list and a place to put delmaps.
        List<DiskPartition> partCopy =
                new ArrayList<DiskPartition>(partitions);

        //
        // We need to make sure that the list is sorted by partition number
        // so that the merge will work correctly.  We can't guarantee that
        // it happened outside, so we'll do it here.
        Collections.sort(partCopy);

        //
        // A quick sanity check:  Any partitions whose documents have all
        // been deleted can be removed from the list.
        Iterator<DelMap> dmi = delMaps.iterator();
        for(Iterator<DiskPartition> i = partCopy.iterator(); i.hasNext() && dmi.hasNext();) {
            DiskPartition p = i.next();
            p.initDocDict();

            //
            // We need a copy of the deletion map, because we don't want
            // people dicking with it before we're ready to use it.
            DelMap d = dmi.next();
            p.ignored = false;
            if(p.stats.nDocs == d.nDeleted) {
                log.debug(logTag, 10, "Ignoring: " + p);
                p.ignored = true;
                i.remove();
                dmi.remove();
            }
        }

        //
        // If we're left with no partitions, we're done.
        if(partCopy.size() == 0) {
            return null;
        }

        //
        // Set up to write the files for the merged entries.
        int newPartNumber = manager.getNextPartitionNumber();

        //
        // A honkin' big try, so that we can get rid of any failed merges.
        try {
            long start = System.currentTimeMillis();
            log.log(logTag, 2,
                    "Merging: " + partCopy + " into DP: " + newPartNumber);

            //
            // Get an array of partitions, and a similar sized array of
            // dictionaries, term mappers, and starting points for the new
            // partition's document IDs.
            DiskPartition[] sortedParts = partCopy.toArray(new DiskPartition[0]);
            DiskDictionary[] dicts = new DiskDictionary[sortedParts.length];
            ByteBuffer[][] buffs = new ByteBuffer[sortedParts.length][];
            EntryMapper[] mappers = new EntryMapper[dicts.length];

            //
            // The new starting document IDs for the merged partition.
            int[] docIDStart = new int[dicts.length];
            docIDStart[0] = 1;

            //
            // The faked starts for merging things other than document IDs.
            int[] fakeStart = new int[dicts.length];

            //
            // We need a set of maps from the document IDs in the separate
            // partitions to the document IDs in the new partition.  This will
            // allow us to remove data for documents that have been deleted.
            int[][] docIDMaps = new int[sortedParts.length][];

            //
            // The number of documents in each of the partitions, excluding
            // documents that have been deleted.
            int[] nUndel = new int[sortedParts.length];

            //
            // Some partition stats for our merged partition.  We'll be
            // filling in bits and pieces as we go.
            PartitionStats mStats = new PartitionStats();

            //
            // We'll quickly build some stats that we need for pretty much
            // everything.
            int newMaxDocID = 0;
            for(int i = 0; i < sortedParts.length; i++) {

                DiskPartition d = sortedParts[i];
                d.initDocDict();
                d.initMainFiles();

                //
                // Get buffers for the postings from this partition.
                buffs[i] = d.getInputBuffers(1024 * 1024);

                //
                // Make the map from old to new document IDs.  Note here
                // that we're going to garbage collect any partition in
                // which there is even a single deleted document.  This
                // could clearly be a more complicated decision, perhaps
                // even handled by a class implementing a GCPolicy
                // interface or something similar.
                docIDMaps[i] =
                        d.getDocIDMap((ReadableBuffer) delMaps.get(i).delMap);
                nUndel[i] = docIDMaps[i] == null ? d.stats.nDocs : docIDMaps[i][0];
                fakeStart[i] = 1;

                mStats.nDocs += nUndel[i];
                mStats.nTokens += d.stats.nTokens;

                //
                // We need to figure out the new starting sequence numbers for
                // each partition.
                if(i > 0) {
                    docIDStart[i] = docIDStart[i - 1] + nUndel[i - 1];
                }
                newMaxDocID += nUndel[i];
            }

            //
            // OK, here we go.  Get the files for the main dictionaries and
            // postings.
            File[] files = getMainFiles(manager, newPartNumber);

            //
            // Get a channel for the main dictionary.
            RandomAccessFile mDictFile = new RandomAccessFile(files[0], "rw");

            //
            // Get channels for the postings.
            OutputStream[] mPostStreams = new OutputStream[files.length - 1];
            PostingsOutput[] mPostOut = new PostingsOutput[files.length - 1];
            for(int i = 1; i < files.length; i++) {
                mPostStreams[i - 1] =
                        new BufferedOutputStream(new FileOutputStream(files[i]),
                        8192);
                mPostOut[i - 1] = new StreamPostingsOutput(mPostStreams[i - 1]);
            }

            log.log(logTag, 4, "Merge main dictionary");

            for(int i = 0; i < sortedParts.length; i++) {
                sortedParts[i].initMainDict();
                dicts[i] = sortedParts[i].mainDict;
            }

            //
            // Get an instance of the main dictionary class to pass in.
            IndexEntry mde;
            try {
                mde = (IndexEntry) mainDictFactory.getEntryClass().newInstance();
            } catch(Exception e) {
                log.error(logTag, 1,
                        "Error instantiating main dictionary entry before merge",
                        e);
                throw new java.io.IOException("Error merging main dictionary");
            }

            //
            // Merge the main dictionaries.
            int[][] idMap =
                    dicts[0].merge(mde, new StringNameHandler(), mStats, dicts,
                    null, docIDStart, docIDMaps, mDictFile,
                    mPostOut, true);

            log.log(logTag, 4, "Write merged bigram dictionary");

            //
            // We need some of the information from the merge of the main
            // dictionary to handle the document dictionary, so we'll merge
            // that now.
            files = getDocFiles(manager, newPartNumber);
            RandomAccessFile mDocDictFile =
                    new RandomAccessFile(files[0], "rw");
            BufferedOutputStream mDocDictPost =
                    new BufferedOutputStream(new FileOutputStream(files[1]),
                    8192);

            //
            // Write the merged partition statistics into the document
            // dictionary.
            mStats.write(mDocDictFile);

            //
            // Pick up the document dictionaries for the merge.
            for(int i = 0; i < dicts.length;
                    i++) {
                dicts[i] = sortedParts[i].docDict;
                mappers[i] = new DocEntryMapper(docIDStart[i], docIDMaps[i]);
            }

            //
            // Stow away the new max doc ID for the doc dict merge
            idMap[0][0] = newMaxDocID;

            //
            // Get an instance of the document dictionary class to pass in.
            IndexEntry dde;
            try {
                dde = (IndexEntry) documentDictFactory.getEntryClass().newInstance();
            } catch(Exception e) {
                log.error(logTag, 1,
                        "Error instantiating document dictionary " +
                        "entry before merge", e);
                throw new java.io.IOException("Error merging " +
                        "document dictionary");
            }


            //
            // Merge the document dictionaries.  We'll need to remap the
            log.log(logTag, 4, "Merge document dictionary");
            int[][] mergedDocIDMap =
                    dicts[0].merge(dde,
                    new StringNameHandler(),
                    dicts, mappers,
                    fakeStart, idMap, mDocDictFile,
                    new PostingsOutput[]{new StreamPostingsOutput(mDocDictPost)},
                    true);
            mDocDictFile.close();
            mDocDictPost.close();
            if(docsAreMerged()) {
                //
                // If docs may have been merged, we need to re-write
                // the main dict with new doc IDs.  This means opening
                // up the whole thing and going term by term, rewriting
                // all the postings.
                //
                // We'll open up a set of output files and pass them into
                // a disk dictionary method that can rewrite the dict
                // with new mapped postings ids.
                files = getMainFiles(manager, newPartNumber);

                //
                // First, instantiate our newly merged main dict
                mDictFile.seek(0);
                RandomAccessFile[] postIn =
                        new RandomAccessFile[files.length - 1];
                for(int i = 1; i < files.length;
                        i++) {
                    postIn[i - 1] = new RandomAccessFile(files[i], "rw");
                }

                DiskDictionary mergedMainDict =
                        mainDictFactory.getDiskDictionary(new StringNameHandler(),
                        mDictFile, postIn,
                        this);

                //
                // Get a channel to write the remapped main dict
                RandomAccessFile mappedDictFile =
                        new RandomAccessFile(files[0].getAbsolutePath() +
                        ".remap", "rw");
                StreamPostingsOutput[] mappedPostOut = new StreamPostingsOutput[files.length -
                        1];

                //
                // Get channels for the postings
                for(int i = 1; i < files.length;
                        i++) {
                    OutputStream outStr =
                            new BufferedOutputStream(new FileOutputStream(files[i].getAbsolutePath() +
                            ".remap"),
                            8192);
                    mappedPostOut[i - 1] = new StreamPostingsOutput(outStr);
                }

                log.log(logTag, 4, "Remap main dictionary");
                mergedMainDict.remapPostings(mde, new StringNameHandler(), stats,
                        mergedDocIDMap[0], mappedDictFile,
                        mappedPostOut);

                //
                // Close up
                for(int i = 0; i < postIn.length;
                        i++) {
                    postIn[i].close();
                }

                mappedDictFile.close();
                for(int i = 0; i < mappedPostOut.length;
                        i++) {
                    mappedPostOut[i].close();
                }

                //
                // Rename the files that were remapped
                for(int i = 0; i < files.length;
                        i++) {
                    files[i].delete();
                    File mappedFile =
                            new File(files[i].getAbsolutePath() + ".remap");
                    mappedFile.renameTo(files[i]);
                }
            }

            //
            // Close off the main dict files
            mDictFile.close();
            for(int i = 0; i < mPostStreams.length; i++) {
                mPostStreams[i].close();
            }

            mergeCustom(newPartNumber, sortedParts, idMap, newMaxDocID,
                    docIDStart, nUndel, docIDMaps);

            DiskPartition ndp =
                    manager.newDiskPartition(newPartNumber, manager);
            if(calculateDVL && manager.getCalculateDVL() &&
                    !((SearchEngineImpl) manager.getEngine()).getLongIndexingRun()) {
                //
                // Calculate the document vectors for the merged partition.
                ndp.initDVL(false);
            }

            log.log(logTag, 3,
                    "Merge took: " + (System.currentTimeMillis() - start) + "ms");

            return ndp;
        } catch(DuplicateKeyException dke) {
            //
            // What's going on here?  Well, if we hit a duplicate document
            // key during the merge, then the partition that we're merging is
            // no good.  So, we delete the document key that was duplicated 
            // and we re-try the merge.
            //
            // This is a stopgap until we can actually find the cause of the 
            // document not getting deleted, but we need to keep running in the
            // face of such problems.  Note, however, that we're keeping track
            // of how many times we've pulled this trick, so that we don't
            // keep trying forever.  This should be configurable, but then 
            // again we should be able to find the bug too.
            if(depth > 5) {
                throw(dke);
            }
            DiskPartition p = (DiskPartition) dke.getEntry().getPartition();
            int origID = dke.getEntry().getOrigID();
            
            log.warn(logTag, 1, dke.getMessage() + 
                    " when merging. Deleting old document " + 
                    p + " " + origID);
            //
            // OK, just delete the key in our delmaps and go on, right?
            Iterator<DiskPartition> dpi = partitions.iterator();
            dmi = delMaps.iterator();
            while(dpi.hasNext()) {
                DiskPartition dp = dpi.next();
                
                //
                // We'll delete it in the original partition and in the map
                // that we got before this merge started.  This will ensure that
                // the deletion will persist if we break out of this handling
                // due to the depth limitation above.
                dp.deleteDocument(origID);
                DelMap dm = dmi.next();
                if(dp == p) {
                    dm.delete(origID);
                    break;
                }
            }
            //
            // OK, try the merge again.
            DiskPartition.reap(manager, newPartNumber);
            return merge(partitions, delMaps, calculateDVL, depth+1);
            
        } catch(Exception e) {

            log.error(logTag, 1, "Exception merging partitions", e);


            //
            // Clean up the unfinished partition.
            partCopy.get(0).reap(manager, newPartNumber);
            throw e;
        }
    }

    /**
     * Provides a place to merge data that is specific to a subclass of disk
     * partition.  This method will be called after the disk partition data
     * is merged, but inside the try block for the whole merge.
     *
     * @param newPartNumber the number of the new partition
     * @param sortedParts the sorted list of partitions
     * @param idMaps a set of maps from old entry ids in the main dictionary
     * to new entry ids in the merged dictionary
     * @param newMaxDocID the new maximum document id
     * @param docIDStart the starting doc ids
     * @param nUndel the number of undeleted documents in each partition
     * @param docIDMaps doc id maps (see merge)
     */
    protected void mergeCustom(int newPartNumber,
            DiskPartition[] sortedParts, int[][] idMaps,
            int newMaxDocID, int[] docIDStart, int[] nUndel,
            int[][] docIDMaps)
            throws Exception {

    //
    // No customization at this level.
    }

    /**
     * Returns true if documents in this partition type can be merged -
     * that is, that the postings of two same-named docs in different
     * partitions will be combined.
     *
     * @return false by default, other classes may override
     */
    public boolean docsAreMerged() {
        return false;
    }

    public String toString() {
        return "DP: " + partNumber;
    }

    public void setCloseTime(long closeTime) {
        this.closeTime = closeTime;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public void createRemoveFile() {
        try {
            removedFile.createNewFile();
        } catch(IOException ex) {
            log.error(logTag, 1, "Error creating remove file: " + removedFile,
                    ex);
        }
    }
}