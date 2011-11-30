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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import com.sun.labs.minion.indexer.Closeable;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.entry.DuplicateKeyException;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.EntryMapper;
import com.sun.labs.minion.indexer.partition.io.DiskPartitionOutput;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.util.FileLock;
import com.sun.labs.minion.util.NanoWatch;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static Logger logger = Logger.getLogger(
            DiskPartition.class.getName());

    /**
     * The dictionary file.
     */
    protected RandomAccessFile dictFile;

    /**
     * The postings file.
     */
    protected RandomAccessFile[] postFiles;

    /**
     * The document dictionary.
     */
    protected DiskDictionary<String> docDict;

    /**
     * The deleted documents file.
     */
    protected File delFile;

    /**
     * A lock for the deleted documents file.
     */
    protected FileLock delFileLock;

    /**
     * The deletion map for this partition.
     */
    protected DelMap deletions;

    /**
     * A <code>File</code> indicating that this partition is no longer
     * active.
     */
    protected File removedFile;

    /**
     * Whether or not this partition has been closed.
     */
    private boolean closed;

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
     * @param documentPostingsType the type of postings to use in the document
     * dictionary
     * @throws java.io.IOException If there is an error opening or reading
     * any of the files making up a partition.
     *
     * @see Partition
     * @see com.sun.labs.minion.indexer.dictionary.Dictionary
     *
     */
    public DiskPartition(int partNumber,
                         PartitionManager manager,
                         Postings.Type documentPostingsType)
            throws java.io.IOException {
        this.partNumber = partNumber;
        this.manager = manager;

        //
        // Open the dictionary and postings files.
        File[] files = getMainFiles();
        dictFile = new RandomAccessFile(files[0], "r");
        postFiles = new RandomAccessFile[files.length - 1];
        for(int i = 1; i < files.length; i++) {
            postFiles[i - 1] = new RandomAccessFile(files[i], "r");
        }

        //
        // Jump to where the partition header is and read it.  We don't need
        // to jump back because the header contains all of the offsets for the
        // stuff that we want.
        long headerOffset = dictFile.readLong();
        dictFile.seek(headerOffset);
        header = new PartitionHeader(dictFile);

        dictFile.seek(header.getDocDictOffset());
        docDict = new DiskDictionary<String>(
                new EntryFactory(documentPostingsType),
                new StringNameHandler(),
                dictFile, postFiles);
        docDict.setName("doc");

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
    }

    /**
     * Gets the document dictionary this partition.  Note that an iterator for
     * this dictionary will return entries for documents that might have been
     * deleted.
     * @return the document dictionary for this partition
     */
    public DiskDictionary getDocumentDictionary() {
        return docDict;
    }

    /**
     * Gets an iterator for the document keys in this partition.  All
     * documents, including those that have been deleted will be returned.
     *
     * @return an iterator for the entries in the document dictionary,
     * which have the document keys as their names.
     */
    public Iterator getDocumentIterator() {
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
        return docDict.iterator(begin, end);
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
        closed = true;
        try {

            syncDeletedMap();
            if(dictFile != null) {
                dictFile.close();
                for(RandomAccessFile pf : postFiles) {
                    pf.close();
                }
            }
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error closing partition", ioe);
        }
        return true;
    }

    public void setClosed() {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    /**
     * Deletes the files associated with this partition.
     */
    public void delete() {
        close();

        File[] files = getMainFiles();
        boolean fail = false;
        for(int i = 0; i < files.length; i++) {
            if(!files[i].delete()) {
                fail = true;
            }
        }

        if(fail) {
            logger.warning("Unable to remove all files for partition "
                    + partNumber);
        }

        File remFile = manager.makeRemovedPartitionFile(partNumber);
        if(remFile.exists() && !remFile.delete()) {

            //
            // NON-FATAL ERROR:
            logger.warning("Unable to remove deleted partition file: " + remFile);
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
        File[] files = getMainFiles(m, n);
        for(File f : files) {
            if((!f.delete()) && (f.exists())) {
                logger.warning("Failed to delete: " + f);
            }
        }

        //
        // Remove the deletion bitmap and the removed partition files.
        if(!m.makeDeletedDocsFile(n).delete()) {
            logger.severe("Failed to reap partition " + n);
        }
        if(!m.makeRemovedPartitionFile(n).delete()) {
            logger.severe("Failed to reap partition " + n);
        }
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
     * the given dictionary.
     *
     * @param keys a set of keys to delete.  The string representation of the
     * elements of the set will be the keys to delete.
     * @return <code>true</code> if any documents were deleted,
     * <code>false</code> otherwise.
     */
    protected boolean updatePartition(Set<String> keys) {

        //
        // First, update our deleted document bitmap.
        syncDeletedMap();

        //
        // Now, loop through the keys, deleting any that are from this
        // partition.
        boolean someDeleted = false;
        for(String key : keys) {
            if(deleteDocument(key)) {
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
        return header.getnDocs() - deletions.getNDeleted();
    }

    /**
     * Get the maximum document ID.  Note that
     * this value can be larger than the number of documents in the partition, due
     * to the presence of deleted documents.
     * @return the maximum ID assigned to a document in this partition.
     */
    public int getMaxDocumentID() {
        return docDict.getMaxID();
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
     * Gets an array of buffers to use for buffering postings during
     * merges.
     *
     * @param size The size of the input buffers to use.
     * @return An array of buffers large enough to handle any dictionary
     * merge.
     */
    private ByteBuffer[] getInputBuffers(int size) {
        ByteBuffer[] ret = new ByteBuffer[postFiles.length];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = ByteBuffer.allocateDirect(size);
        }
        return ret;
    }

    /**
     * Merges a number of <code>DiskPartition</code>s into a single
     * partition.
     *
     * @param partitions the partitions to merge
     * @param delMaps the state of the deletion maps for the partitions to 
     * merge before the merge started.  We need these to be the same as the
     * ones at the place where the merge was called for (see {@link PartitionManager.Merger}), 
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
     * ones at the place where the merge was called for (see {@link PartitionManager.Merger}), 
     * otherwise we might get some skew in the maps between when they are recorded
     * there and recorded here!
     * @param calculateDVL if <code>true</code>, then calculate the document
     * vector lengths for the documents in the merged partition after the merge
     * is finished.
     * @param depth how many times we've retried the merge, aimed at getting
     * rid of duplicate documents.
     * @return the newly-merged partition.
     * @throws Exception If there is any error during the merge.
     */
    public DiskPartition merge(List<DiskPartition> partitions,
                               List<DelMap> delMaps,
                               boolean calculateDVL, int depth)
            throws Exception {

        //
        // A copy of the partition list and a place to put delmaps.
        List<DiskPartition> partCopy = new ArrayList<DiskPartition>(partitions);

        //
        // We need to make sure that the list is sorted by partition number
        // so that the merge will work correctly.  We can't guarantee that
        // it happened outside, so we'll do it here.
        Collections.sort(partCopy);

        //
        // A quick sanity check:  Any partitions whose documents have all
        // been deleted can be removed from the list.
        Iterator<DelMap> dmi = delMaps.iterator();
        Iterator<DiskPartition> dpi = partCopy.iterator();
        while(dpi.hasNext() && dmi.hasNext()) {
            DiskPartition p = dpi.next();
            DelMap d = dmi.next();
            p.ignored = false;
            if(p.getNDocs() == 0) {
                p.ignored = true;
                dpi.remove();
                dmi.remove();
            }
        }

        //
        // If we're left with no partitions, we're done.
        if(partCopy.isEmpty()) {
            return null;
        }
        
        //
        // A place to store state and pass it around while we're merging.
        MergeState mergeState = new MergeState(manager, new DiskPartitionOutput(manager));

        //
        // A honkin' big try, so that we can get rid of any failed merges.
        try {
            NanoWatch mw = new NanoWatch();
            mw.start();
            logger.info(String.format("Merging %s into DP: %d", partCopy,
                                      mergeState.partOut.getPartitionNumber()));

            //
            // Get an array of partitions, and a similar sized array of
            // dictionaries, term mappers, and starting points for the new
            // partition's document IDs.
            mergeState.partitions = partCopy.toArray(new DiskPartition[0]);
            DiskDictionary[] dicts = new DiskDictionary[mergeState.partitions.length];
            ByteBuffer[][] buffs = new ByteBuffer[mergeState.partitions.length][];
            EntryMapper[] mappers = new EntryMapper[dicts.length];

            //
            // The new starting document IDs for the merged partition.
            mergeState.docIDStarts = new int[dicts.length];
            mergeState.fakeStarts = new int[dicts.length];
            mergeState.docIDStarts[0] = 1;

            //
            // The faked starts for merging things other than document IDs.
            int[] fakeStart = new int[dicts.length];

            //
            // We need a set of maps from the document IDs in the separate
            // partitions to the document IDs in the new partition.  This will
            // allow us to remove data for documents that have been deleted.
            mergeState.docIDMaps = new int[mergeState.partitions.length][];

            //
            // The number of documents in each of the partitions, excluding
            // documents that have been deleted.
            mergeState.nUndel = new int[mergeState.partitions.length];

            //
            // We'll quickly build some stats that we need for pretty much
            // everything.
            for(int i = 0; i < mergeState.partitions.length; i++) {

                DiskPartition d = mergeState.partitions[i];

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
                mergeState.docIDMaps[i] =
                        d.getDocIDMap((ReadableBuffer) delMaps.get(i).delMap);
                mergeState.nUndel[i] =
                        mergeState.docIDMaps[i] == null ? d.header.getnDocs()
                        : mergeState.docIDMaps[i][0];
                fakeStart[i] = 1;

                mergeState.partOut.setNDocs(mergeState.partOut.getNDocs() + mergeState.nUndel[i]);

                //
                // We need to figure out the new starting sequence numbers for
                // each partition.
                if(i > 0) {
                    mergeState.docIDStarts[i] = mergeState.docIDStarts[i - 1] + mergeState.nUndel[i - 1];
                }
                mergeState.maxDocID += mergeState.nUndel[i];
            }
            
            //
            // Write a placeholder for the offset of the partition header.
            
            DictionaryOutput fieldDictOut = mergeState.partOut.getPartitionDictionaryOutput();
            int phoffsetpos = fieldDictOut.position();
            fieldDictOut.byteEncode(0L, 8);

            //
            // Pick up the document dictionaries for the merge.
            for(int i = 0; i < dicts.length; i++) {
                dicts[i] = mergeState.partitions[i].docDict;
                mappers[i] = new DocEntryMapper(mergeState.docIDStarts[i], mergeState.docIDMaps[i]);
            }
            
            int[][] docDictIDMaps = new int[dicts.length][1];
            docDictIDMaps[0][0] = mergeState.maxDocID;

            //
            // Merge the document dictionaries.  We'll need to remap the
            logger.fine("Merge document dictionary");
            mergeState.partOut.getPartitionHeader().setDocDictOffset(fieldDictOut.position());
            DiskDictionary.merge(manager.getIndexDir(),
                    new StringNameHandler(),
                           dicts,
                           mappers,
                           fakeStart, 
                           docDictIDMaps,
                           fieldDictOut, 
                           mergeState.partOut.getPostingsOutput(), true);


            mergeCustom(mergeState);

            int phoffset = fieldDictOut.position();
            
            mergeState.partOut.getPartitionHeader().write(fieldDictOut);
            int pos = fieldDictOut.position();
            fieldDictOut.position(phoffsetpos);
            fieldDictOut.byteEncode((long) phoffset, 8);
            fieldDictOut.position(pos);
            
            mergeState.partOut.flush(null);
            mergeState.partOut.close();

            
            DiskPartition ndp = manager.newDiskPartition(mergeState.partOut.getPartitionNumber(), manager);
            mw.stop();
            logger.info(String.format("Merge took %.3fms", mw.getTimeMillis()));
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
                throw (dke);
            }
            DiskPartition p = (DiskPartition) dke.getEntry().getPartition();
            int origID = dke.getEntry().getID();

            logger.warning(String.format("%s when merging. Deleting old document %s %d", 
                                         dke.getMessage(), p, origID));

            //
            // OK, just delete the key in our delmaps and go on, right?
            dpi = partitions.iterator();
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
            DiskPartition.reap(manager, mergeState.partOut.getPartitionNumber());
            return merge(partitions, delMaps, calculateDVL, depth + 1);

        } catch(Exception e) {

            logger.log(Level.SEVERE, "Exception merging partitions", e);


            //
            // Clean up the unfinished partition.
            DiskPartition.reap(manager, mergeState.partOut.getPartitionNumber());
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
    protected void mergeCustom(MergeState mergeState)
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

    @Override
    public String toString() {
        return String.format("DP: %d", partNumber);
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
            logger.log(Level.SEVERE, "Error creating remove file: "
                    + removedFile,
                       ex);
        }
    }
}
