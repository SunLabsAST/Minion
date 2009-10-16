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
import com.sun.labs.minion.IndexListener;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.FieldFrequency;
import com.sun.labs.minion.FieldValue;
import com.sun.labs.minion.IndexConfig;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.WeightedField;
import com.sun.labs.util.props.ConfigBoolean;
import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.ConfigString;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.Closeable;
import com.sun.labs.minion.indexer.MetaFile;
import com.sun.labs.minion.indexer.dictionary.UncachedTermStatsDictionary;
import com.sun.labs.minion.retrieval.CollectionStats;
import com.sun.labs.minion.retrieval.DocumentVectorImpl;
import com.sun.labs.minion.retrieval.ScoredGroup;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.util.FileLock;
import com.sun.labs.minion.util.FileLockException;
import com.sun.labs.minion.util.SimpleFilter;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.dictionary.FeatureVector;
import com.sun.labs.minion.indexer.dictionary.TermStatsDictionary;
import com.sun.labs.minion.indexer.dictionary.TermStatsFactory;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.FieldedDocKeyEntry;
import com.sun.labs.minion.indexer.entry.TermStatsEntry;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.retrieval.CompositeDocumentVectorImpl;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.util.DirCopier;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * For any particular collection that we will index, there can be multiple
 * entry types and for each entry type there can be multiple partitions.
 * An <code>PartitionManager</code> is used to manage all of the partitions
 * in a collection that have the same entry type.
 *
 * <p>
 *
 * The static <code>getManager</code> method can be used to retrieve the
 * partition manager for a particular entry type.
 *
 * <p>
 *
 * The <code>PartitionManager</code> maintains a static HashMap that maps
 * index directories and entry names to the actual manager instance for
 * entries of that type.  The key for the hash is
 * &lt;<code>indexDir</code>&gt;/&lt;<code>EntryType</code>&gt;, which will
 * allow us to open multiple collections in the same VM.
 *
 * <p>
 *
 * The <code>PartitionManager</code> for a particular entry type provides
 * access to the set of <code>DiskPartitions</code> that contain entries of
 * that type.
 *
 * <p>
 *
 * The <code>PartitionManager</code> is also responsible for providing two
 * kinds of data for partition use.  First, it hands out the numbers that
 * are used for the partitions.  These numbers are local to the
 * <code>PartitionManager</code> for a given entry type.
 *
 * <p>
 *
 * A partition manager can be given a name via the <code>setName</code>
 * method.  This name can be used to find the corresponding paritition
 * manager object.  The name defaults to the name of the index directory.
 */
public class PartitionManager implements com.sun.labs.util.props.Configurable {

    /**
     * The search engine that is using us.
     */
    protected SearchEngineImpl engine;

    /**
     * The index configuration for the index we'll be managing.
     */
    protected IndexConfig indexConfig;

    /**
     * A dictionary containing global term statistics.
     */
    private TermStatsDictionary termStatsDict;

    private File currTSF;

    /**
     * A timer that can be used during querying to time tasks.
     */
    protected Timer queryTimer;

    /**
     * The configuration name for this partition manager.
     */
    protected String name;

    /**
     * The list of index listeners for this index.
     */
    private final List<IndexListener> listeners = Collections.synchronizedList(
            new ArrayList<IndexListener>());

    /**
     * The last time that a purge was called.  If new partitions start dumping
     * before this time, they shouldn't be added to the active list.
     */
    protected Date lastPurgeTime = new Date(0);

    /**
     * Whether we're shutting down or not.
     */
    private boolean noMoreMerges;

    /**
     * Instantiates a <code>PartitionManager</code> with the given index
     * configuration.
     */
    public PartitionManager() {
        subDir = "index";
    }

    public void addIndexListener(IndexListener il) {
        listeners.add(il);
    }

    public void removeIndexListener(IndexListener il) {
        listeners.remove(il);
    }

    /**
     * Initializes the PartitionManager.  Allows the directory to be
     * parameterized so that a subclass can use a different directory.
     */
    protected void init() {

        //
        // Pull stuff from the configuration.
        indexDir = indexConfig.getIndexDirectory() + File.separatorChar + subDir;
        indexDirFile = new File(indexDir);
        lockDirFile = new File(lockDir);
        randID = (int) (Math.random() * Integer.MAX_VALUE);

        collectionStats = new CollectionStats(this);

        //
        // Set up the query timer.
        queryTimer = new Timer(true);

        //
        // Log message.
        logger.fine("Init " + indexDir + " " + randID);

        //
        // Create the index directory if necessary.
        File iDFile = new File(indexDir);
        if(!iDFile.exists()) {
            logger.fine("Making index directory: " + iDFile);
            if(!iDFile.mkdirs()) {
                logger.severe("Failed to create index directory");
            }

            //
            // If there's data that we need to copy, then do it now.
            if(startingDataDir != null) {
                if(startingDataDir.exists()) {
                    try {
                        DirCopier dc = new DirCopier(startingDataDir, iDFile);
                        dc.copy();
                    } catch(java.io.IOException ioe) {
                        logger.log(Level.SEVERE,
                                   "Error copying starting directory",
                                   ioe);
                    }
                } else {
                    logger.warning("Starting data directory " +
                            startingDataDir +
                            " does not exist.");
                }
            }
        }

        //
        // The name of our active file.
        activeFile = new ActiveFile(indexDirFile, lockDirFile, logTag);

        //
        // Set up the lock for the merge.  We make this a single try, no
        // pause lock because we don't want to wait until the lock is
        // ready, we simply want to see whether we can get it.
        mergeLock = new FileLock(lockDirFile, new File("merge." + logTag), 0,
                                 TimeUnit.SECONDS);

        //
        // Read the active file.
        try {
            activeFile.lock();

            //
            // Make the meta file.  We'll write it if it doesn't exist.
            metaFile = new MetaFile(lockDirFile, makeMetaFile());
            try {
                metaFile.lock();
                if(!metaFile.exists()) {
                    logger.fine("Making meta file");
                    metaFile.write();
                } else {
                    metaFile.read();
                }
                metaFile.unlock();
            } catch(com.sun.labs.minion.util.FileLockException fle) {
                logger.severe("Error locking meta file: " + fle);
                return;
            } catch(java.io.IOException ioe) {
                logger.severe("Error reading meta file: " + ioe);
                try {
                    metaFile.unlock();
                } catch(Exception e) {
                    //
                    // Gah!
                    logger.severe("Unable to unlock meta file after " +
                            "meta file I/O exception.");
                }
                return;
            }

            //
            // Open our dictionary of term stats if we're the sort of partition
            // manager that's going to need term stats.
            if(calculateDVL) {
                try {
                    currTSF = makeTermStatsFile(metaFile.getTermStatsNumber());
                    if(!currTSF.exists()) {
                        UncachedTermStatsDictionary.create(indexDir, currTSF);
                    }
                    termStatsDict = termstatsDictFactory.getDictionary(currTSF);
                } catch(java.io.IOException ioe) {
                    logger.log(Level.SEVERE,
                               "Error opening term stats dictionary",
                               ioe);
                } catch(FileLockException fle) {
                    logger.log(Level.SEVERE,
                               "Error opening term stats dictionary",
                               fle);
                }
            }

            updateActiveParts(true);
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "I/O error updating active partition list",
                       ioe);
            activeParts.clear();
        } catch(FileLockException fle) {
            logger.log(Level.SEVERE,
                       "Error locking active file for active file read", fle);
            activeParts.clear();
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Other exception during updateActive", e);
            activeParts.clear();
        } finally {
            try {
                activeFile.unlock();
            } catch(FileLockException fle) {
                logger.log(Level.SEVERE,
                           "Unable to release active lock after update", fle);
            }

        }

        //
        // Start our housekeeping thread.
        timer = new Timer("PM-timer");
        timer.scheduleAtFixedRate(new HouseKeeper(),
                                  activeCheckInterval,
                                  activeCheckInterval);
    }

    /**
     * Reads the active file and adds any new partitions to our active
     * list.
     *
     * @param addNew Whether newly activated partitions should be added to
     * the active list immediately.
     * @return The list of newly opened partitions.
     * @throws Exception if anything goes wrong.
     */
    public List<DiskPartition> updateActiveParts(boolean addNew) throws
            Exception {

        //
        // Re-read the meta file.
        try {
            metaFile.read();
        } catch(Exception e) {
            logger.warning("Error reading meta file in update: " + e);
        }

        boolean releaseNeeded = false;
        if(!activeFile.isLocked()) {
            activeFile.lock();
            releaseNeeded = true;
        }

        List<Integer> add = activeFile.read();
        List<Integer> currList = ActiveFile.getPartNumbers(activeParts);
        List<Integer> close = new ArrayList<Integer>(currList);

        //
        // Get the partitions that we have opened that are not in the
        // active file, and those that are in the active file, but we don't
        // have opened.
        close.removeAll(add);
        add.removeAll(currList);

        //
        // We need to close partitions that were merged and not add
        // them again.
        List<Integer> mp = getPartNumbers(mergedParts);
        close.addAll(mp);
        add.removeAll(mp);
        mergedParts.clear();

        //
        // Remove any partitions that we have to close.
        if(close.size() > 0) {
            for(Iterator<DiskPartition> i = activeParts.iterator(); i.hasNext();) {
                DiskPartition dp = i.next();
                if(close.contains(dp.getPartitionNumber())) {
                    dp.setCloseTime(System.currentTimeMillis() + partCloseDelay);
                    dp.setClosed();
                    thingsToClose.add(dp);
                    i.remove();
                }
            }
        }

        //
        // Add any partitions that we haven't opened.
        List<DiskPartition> newlyLoadedParts = new ArrayList<DiskPartition>();
        for(Integer pn : add) {
            try {
                newlyLoadedParts.add(newDiskPartition(pn, this));
            } catch(java.io.IOException ioe) {
                logger.log(Level.SEVERE, "Error activating partition: " + pn +
                        " in updateActivePartitions", ioe);
            } catch(Exception e) {
                logger.log(Level.SEVERE, "Exception loading partition: " + pn +
                        " in updateActivePartitions", e);
            }
        }

        if(addNew && newlyLoadedParts.size() > 0) {
            activeParts.addAll(newlyLoadedParts);

            //
            // Only update the collection stats when we get new partitions.
            collectionStats = new CollectionStats(this, activeParts);
        }


        //
        // Update our term statistics dictionary, if necessary.
        try {
            updateTermStats();
        } catch(java.io.IOException ex) {
            logger.log(Level.SEVERE,
                       "Error reading term stats dictionary during updateActiveParts");
            termStatsDict = null;
        } catch(FileLockException ex) {
            logger.log(Level.SEVERE,
                       "Error locking metafile during updateActiveParts");
            termStatsDict = null;
        }

        if(releaseNeeded) {
            activeFile.unlock();
        }

        return newlyLoadedParts;
    }

    /**
     * Adds a new partition to this manager.  We will remove the given keys
     * from older partitions.
     *
     * @param partNumber The number of the partition.
     * @param keys A list of <code>String</code>s representing the document
     * keys for the documents indexed into the new partition.  They will be
     * removed from the old partitions.
     */
    protected synchronized void addNewPartition(int partNumber,
                                                Set<Object> keys) {
        //
        // If we're asked to add a weird partition, don't do it.
        if(partNumber <= 0) {

            if(partNumber == -1) {
                logger.warning("Add new partition: " + partNumber +
                        " ignored");
            }

            //
            // A "partition" made up of empty partitions should cause an
            // update to happen.
            if(partNumber == -2) {

                //
                // Update the active list.
                try {
                    updateActiveParts(true);
                    activeFile.write(activeParts);
                } catch(Exception e) {
                    logger.severe("Error updating active partitions " +
                            "during add new partition for partition: " +
                            partNumber);
                }
            }
            return;
        }

        logger.finer("Started addNewPartition " + partNumber);
        try {
            DiskPartition ndp = newDiskPartition(partNumber, this);
            addNewPartition(ndp, keys);
        } catch(IOException ex) {
            logger.log(Level.SEVERE, "Error opening new partition: " +
                    partNumber,
                       ex);
        }
    }

    protected synchronized void addNewPartition(DiskPartition dp,
                                                Set<Object> keys) {

        //
        // Get the latest version of the active file, locking out
        // anyone else.
        try {
            activeFile.lock();
            updateActiveParts(true);

            //
            // OK, if we've crossed the high water mark, we need to take
            // remedial action now.
            if(!noMoreMerges && activeParts.size() >= openPartitionHighWaterMark) {
                handleTooManyPartitions();
            }

            //
            // Update our term statistics dictionary, if necessary.
            try {
                updateTermStats();
            } catch(Exception ex) {
                logger.log(Level.SEVERE,
                           String.format(
                        "Error reading term stats dictionary when adding %s", dp),
                           ex);
                termStatsDict = null;
            }

            //
            // Delete the keys we updated.
            if(keys != null && keys.size() > 0) {
                deleteKeys(keys);
            }

            //
            // OK, add the new partition to the list of active partitions and
            // make sure it's in order by partition number.
            activeParts.add(dp);

            //
            // Write the active file.
            activeFile.write(activeParts);
        } catch(Exception ex) {
            logger.log(Level.SEVERE, String.format("Exception adding %s", ex));
        } finally {
            try {
                activeFile.unlock();
            } catch(Exception ex) {
                logger.log(Level.SEVERE,
                           String.format(
                        "Error unlocking active file when adding %s", dp),
                           ex);
            }
        }

        //
        // Notify the listeners.  Note that we're doing this out of the
        // locked region, since they can't affect what's happening.
        if(keys != null) {
            synchronized(listeners) {
                for(IndexListener il : listeners) {
                    il.partitionAdded(engine, keys);
                }
            }
        }
    }

    /**
     * Returns the number of active partitions being managed.
     * @return the number of active partitions
     */
    public int getNActive() {
        return activeParts.size();
    }

    /**
     * Returns a list of the currently active partitions.  Note that these
     * partitions may be closed if you hang onto this list for a long time!
     * @return the active partitions
     */
    public List<DiskPartition> getActivePartitions() {
        return new ArrayList<DiskPartition>(activeParts);
    }

    /**
     * Instantiates a disk partition of the correct type for this manager
     *
     * @param partNum the partition number
     * @param m the manager
     * @return the new partition
     * @throws java.io.IOException if there is any error opening the partition
     */
    protected DiskPartition newDiskPartition(Integer partNum,
                                             PartitionManager m)
            throws java.io.IOException {
        return partitionFactory.getDiskPartition(partNum, m);
    }

    /**
     * Instantiates a base disk partition for this manager
     *
     * @param partNum the partition number
     * @param m the manager
     * @return the new partition
     * @throws java.io.IOException if there is any error opening the partition
     */
    protected DiskPartition newBaseDiskPartition(Integer partNum,
                                                 PartitionManager m)
            throws java.io.IOException {
        return partitionFactory.getBaseDiskPartition(partNum, m);
    }

    /**
     * Get a timer that can be used during querying to time tasks.
     *
     * @return the query timer
     */
    public Timer getQueryTimer() {
        return queryTimer;
    }

    /**
     * Get the directory where the index is.  Shared by all PartitionManagers
     * in a given directory.
     *
     * @return the index dir
     */
    public String getIndexDir() {
        return indexDir;
    }

    /**
     * Sets the rate of partition merges during indexing.
     *
     * @param rate Controls the rate of merging. Must be >= 2.  Lower
     * value leads to fewer partitions, faster searches, more merges,
     * slower indexing
     */
    public synchronized void setMergeRate(int rate) {
        mergeRate = rate;
    }

    /**
     * Checks to see if a document is in the index.
     *
     * @param key the key for the document that we wish to check.
     * @return <code>true</code> if the document is in the index.  A
     * document is considered to be in the index if a document with the
     * given key appears in the index and has not been deleted.
     */
    public boolean isIndexed(String key) {

        //
        // Try each partition, quitting when we find the one with the key.
        for(DiskPartition p : getActivePartitions()) {
            if(p.isIndexed(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes a single document from whatever partition that it is in.
     *
     * @param key The document key for the document to be deleted.
     */
    public void deleteDocument(String key) {

        //
        // Try each partition, quitting when we find the one with the key.
        for(DiskPartition p : activeParts) {
            if(p.deleteDocument(key)) {
                p.syncDeletedMap();
            }
        }
        activeFile.setLastModified(System.currentTimeMillis());
    }

    /**
     * Deletes a set of documents from whatever partition that they are
     * in.
     *
     * @param keys The list of keys of the documents to be deleted.
     */
    public void deleteDocuments(List<String> keys) {
        for(DiskPartition dp : activeParts) {
            boolean some = false;
            for(Iterator<String> i = keys.iterator(); i.hasNext();) {
                if(dp.deleteDocument(i.next())) {
                    i.remove();
                    some = true;
                }
            }
            if(some) {
                dp.syncDeletedMap();
            }
        }
        activeFile.setLastModified(System.currentTimeMillis());
    }

    /**
     * Delete the given keys from the given list of partitions.  This will
     * force the deletion bitmaps to be flushed to the disk.
     *
     * @param keys The list of document keys to delete.
     */
    protected void deleteKeys(Set<Object> keys) {

        for(DiskPartition p : activeParts) {
            p.updatePartition(keys);
        }
        if(!activeFile.setLastModified(System.currentTimeMillis())) {
            logger.severe("Failed to update active file mod time");
        }
    }

    /**
     * Gets a term from a document dictionary corresponding to the given key.
     * Terms associated with documents that have been deleted are ignored.
     * @param key the key for which we want a term from the document dictionary
     * @return the entry, or <code>null</code> if this key does not appear in
     * the index or if the associated document has been deleted
     */
    public DocKeyEntry getDocumentTerm(String key) {
        for(DiskPartition p : getActivePartitions()) {
            DocKeyEntry dt = p.getDocumentTerm(key);
            if(dt != null) {
                return dt;
            }
        }
        return null;
    }

    /**
     * Gets a document vector for the given document key.
     * @param key the key of the document for which we want a vector
     * @return the document vector for the document with the given key, or
     * <code>null</code> if this key does not appear in the index or if the
     * associated document has been deleted.
     */
    public DocumentVector getDocumentVector(String key) {
        return getDocumentVector(key, (String) null);
    }

    /**
     * Gets a document vector for the given document key.
     * @param key the key of the document for which we want a vector
     * @param field the field for which we want a document vector.  If this
     * parameter is <code>null</code>, then a vector containing the terms from
     * all vectored fields in the document is returned. If this value is the empty string, then a
     * vector for the contents of the document that are not in any field are
     * returned.  If this value is the name of a field that was not vectored
     * during indexing, an empty vector will be returned.
     * @return the document vector for the document with the given key, or
     * <code>null</code> if this key does not appear in the index or if the
     * associated document has been deleted.
     */
    public DocumentVector getDocumentVector(String key, String field) {
        DocKeyEntry dt = getDocumentTerm(key);
        if(dt != null) {
            return new DocumentVectorImpl(engine, dt, field);
        }
        return null;
    }

    /**
     * Gets a composite document vector for the given document key.
     * @param key the key of the document for which we want a vector
     * @param fields the fields for which we want a document vector.
     * @return the document vector for the document with the given key, or
     * <code>null</code> if this key does not appear in the index or if the
     * associated document has been deleted.
     */
    public DocumentVector getDocumentVector(String key, WeightedField[] fields) {
        DocKeyEntry dt = getDocumentTerm(key);
        if(dt != null) {
            return new CompositeDocumentVectorImpl(engine, dt, fields);
        }
        return null;
    }

    /**
     * Gets a set of results ordered by similarity to the given document, calculated
     * by computing the euclidean distance based on the feature vector stored in the
     * given field.
     *
     * @param key the key of the document to which we'll compute similarity.
     * @param name the name of the field containing the feature vectors that
     * we'll use in the similarity computation.
     * @return a result set containing the distance between the given document
     * and all of the documents.  The scores assigned to the documents are the
     * distance scores, and so the returned set will be sorted in increasing
     * order of the document score.  It is up to the application to handle the
     * scores in whatever way they deem appropriate.
     */
    public ResultSet getSimilar(String key, String name) {
        double[] vec = (double[]) getFieldValue(name, key);
        List results = new ArrayList();
        for(DiskPartition dp : activeParts) {
            results.add(new ScoredGroup(dp,
                                        ((InvFileDiskPartition) dp).
                    euclideanDistance(vec, name)));
        }

        //
        // Now go ahead and make a set of reults.  Note that we need to sort
        // these results in increasing order, since they are based on distance,
        // not similarity.
        return new ResultSetImpl(engine, "+score", results);
    }

    public double getDistance(int d1, int d2, String name) {
        InvFileDiskPartition p = (InvFileDiskPartition) activeParts.peek();
        FeatureVector v1 = (FeatureVector) p.getFieldStore().getSavedField(name);
        return v1.distance(d1,
                           (FeatureVector) p.getFieldStore().getSavedField(name),
                           d2);
    }

    /**
     * Gets the distance between two documents, based on the values stored in
     * in a given feature vector saved field.
     *
     * @param k1 the first key
     * @param k2 the second key
     * @param name the name of the feature vector field for which we want the
     * distance
     * @return the euclidean distance between the two documents' feature vectors.
     * If the field value is not defined for either of the two documents, <code>
     * Double.POSITIVE_INFINITY</code> is returned.
     */
    public double getDistance(String k1, String k2, String name) {
        QueryEntry d1 = null;
        QueryEntry d2 = null;

        for(DiskPartition dp : getActivePartitions()) {

            if(d1 == null) {
                d1 = dp.getDocumentTerm(k1);
            }

            if(d2 == null) {
                d2 = dp.getDocumentTerm(k2);
            }

            if(d1 != null && d2 != null) {
                FeatureVector v1 =
                        (FeatureVector) ((InvFileDiskPartition) d1.getPartition()).getFieldStore().
                        getSavedField(name);

                if(v1 == null) {
                    return Double.POSITIVE_INFINITY;
                }

                FeatureVector v2 =
                        (FeatureVector) ((InvFileDiskPartition) d2.getPartition()).getFieldStore().
                        getSavedField(name);

                if(v2 == null) {
                    return Double.POSITIVE_INFINITY;
                }

                return v1.distance(d1.getID(), v2, d2.getID());
            }
        }

        return Double.POSITIVE_INFINITY;
    }

    /**
     * Gets the values for the given field that match the given pattern.
     * @param field the saved, string field against whose values we will match.
     * If the named field is not saved or is not a string field, then the empty
     * set will be returned.
     * @param pattern the pattern for which we'll find matching field values.
     * @return a sorted set of field values.  This set will be ordered by the
     * proportion of the field value that is covered by the given pattern.
     */
    public SortedSet<FieldValue> getMatching(String field,
                                             String pattern) {
        SortedSet<FieldValue> ret =
                new TreeSet<FieldValue>();
        for(Iterator i = getActivePartitions().iterator(); i.hasNext();) {
            ret.addAll(((InvFileDiskPartition) i.next()).getFieldStore().
                    getMatching(field, pattern));
        }
        return ret;
    }

    /**
     * Gets an iterator for all the values in a field.  The values are
     * returned by the iterator in the order defined by the field type.
     *
     * @param field The name of the field who's values we need an iterator
     * for.
     * @return An iterator for the given field.  If the field is not a
     * saved field, then an iterator that will return no values will be
     * returned.
     */
    public FieldIterator getFieldIterator(String field) {
        return getFieldIterator(field, false);
    }

    /**
     * Gets an iterator for all the values in a field.  The values are
     * returned by the iterator in the order defined by the field type.
     *
     * @param field The name of the field who's values we need an iterator
     * for.
     * @param ignoreCase whether the iterator should ignore case when returing
     * results
     * @return An iterator for the given field.  If the field is not a
     * saved field, then an iterator that will return no values will be
     * returned.
     */
    public FieldIterator getFieldIterator(String field, boolean ignoreCase) {
        return new FieldIterator(getActivePartitions(), field, ignoreCase);
    }

    /**
     * Gets all of the field values associated with a given field in a
     * given document.
     *
     * @param field The name of the field for which we want the values.
     * @param key The key of the document whose values we want.
     * @return A <code>List</code> containing values of the appropriate
     * type.  If the named field is not a saved field, or if the given
     * document key is not in the index, then an empty list is returned.
     */
    public List getAllFieldValues(String field, String key) {
        List ret = null;
        for(DiskPartition dp : getActivePartitions()) {
            ret =
                    (List) ((InvFileDiskPartition) dp).getSavedFieldData(field,
                                                                         key,
                                                                         true);
            if(ret != null && ret.size() > 0) {
                return ret;
            }
        }
        return ret;
    }

    /**
     * Gets a list of the top n most frequent field values for a given
     * named field.  If n is &lt; 1, all field values are returned, in order
     * of their frequency from most to least frequent.
     *
     * @param field the name of the field to rank
     * @param n the number of field values to return
     * @return a <code>List</code> containing field values of the appropriate
     *         type for the field, ordered by frequency
     */
    public List<FieldFrequency> getTopFieldValues(String field, int n,
                                                  boolean ignoreCase) {
        //
        // Make a heap to store the top n values (ranked by freq), and
        // iterate across all values to fill in the heap
        PriorityQueue<FieldFrequency> sorter =
                new PriorityQueue<FieldFrequency>();
        for(FieldIterator it = getFieldIterator(field, ignoreCase); it.hasNext();) {
            Object val = it.next();
            int freq = it.getFreq();


            //
            // See if we should keep this field of throw it away. We'll only
            // generate an element for the queue when we need to!
            if(n < 0 || sorter.size() < n) {
                sorter.offer(new FieldFrequency(val, freq));
            } else {
                if(sorter.peek().getFreq() < freq) {
                    sorter.poll();
                    sorter.offer(new FieldFrequency(val, freq));
                }
            }
        }

        //
        // Now we have the heap of the top results (lowest freq of the top
        // frequencies at the top of the heap).
        List<FieldFrequency> res = new ArrayList<FieldFrequency>(sorter.size());
        while(sorter.size() > 0) {
            res.add(0, sorter.poll());
        }

        return res;
    }

    /**
     * Handles the case where we've exceeded the high water mark for partitions.
     * The resolution is to run a merge of the smaller partitions in the index,
     * resulting in an index with a number of partitions equal to the low water
     * mark for open partitions.
     */
    private boolean handleTooManyPartitions() {

        logger.warning("Too many open partitions: " + activeParts.size());

        //
        // If we're doing async merges, then we need to acquire the merge lock
        // before we can do anything else.  Acquiring the lock will mean that there
        // are no outstanding merges proceeding.  Note that we're not using the
        // merge lock defined in this class because that's a single try lock.
        // We need a lock that we can retry on to give the merge time to finish.
        // We're going to use a long timeout, because if we're in here, the indexer
        // is in trouble, and this needs to get taken care of!
        FileLock ourMergeLock =
                new FileLock(mergeLock, 60, TimeUnit.SECONDS);
        try {
            ourMergeLock.acquireLock();
        } catch(IOException ex) {
            logger.log(Level.SEVERE,
                       "Error getting merge lock when too many partitions", ex);
            return false;
        } catch(FileLockException fle) {
            logger.log(Level.SEVERE,
                       "Error getting merge lock when too many partitions", fle);
            return false;
        }

        //
        // Get a list of partitions sorted by increasing size:  smaller partitions
        // will merge faster.
        List<DiskPartition> parts = new ArrayList<DiskPartition>(activeParts);
        Collections.sort(parts,
                         new Comparator<DiskPartition>() {

            public int compare(DiskPartition o1, DiskPartition o2) {
                return o1.getMaxDocumentID() - o2.getMaxDocumentID();
            }
        });

        //
        // Take a sublist that will get us to the low water mark.
        parts = parts.subList(0, parts.size() - openPartitionLowWaterMark);
        Merger m = getMerger(parts, ourMergeLock);
        m.merge();

        //
        // Now force a close of the partitions on the toClose list to free
        // up those file handles.  This may break queries that are in flight,
        // but we're in trouble here!
        for(Closeable cl : thingsToClose) {
            cl.close(Long.MAX_VALUE);
            cl.createRemoveFile();
        }
        thingsToClose.clear();

        return true;
    }

    public int getNFields() {
        return metaFile.size();
    }

    /**
     * Gets a single field value associated with a given field in a given
     * document.
     *
     * @param field The name of the field for which we want the values.
     * @param key The key of the document whose values we want.
     * @return An <code>Object</code> of the appropriate type for the named
     * field. If the named field is not a saved field, or if the given
     * document key is not in the index, then <code>null</code> is
     * returned.
     * <p>
     * Note that if there are multiple values for the given field, there is
     * no guarantee which of the values will be returned by this method.
     *
     * @see #getAllFieldValues
     */
    public Object getFieldValue(String field, String key) {
        for(DiskPartition dp : getActivePartitions()) {
            Object o = ((InvFileDiskPartition) dp).getSavedFieldData(
                    field, key, false);
            if(o != null) {
                return o;
            }
        }
        return null;
    }
    private ExtFilter remFilter = new ExtFilter("rem");

    private ExtFilter delFilter = new ExtFilter("del");

    private RTSFilter rtsdFilter = new RTSFilter();

    /**
     * Reaps deleted partitions from the collection.  Walks the partition
     * files and deletes any that have had their removed files existing for
     * enough time.
     *
     */
    public void reap() {

        //
        // Sometimes we want to turn off reaping for debugging purposes.
        if(reapDoesNothing) {
            return;
        }

        //
        // Get the files in the directory.
        String[] dir = indexDirFile.list();
        if(dir == null) {
            logger.warning("Unable to list directory files in reap");
            return;
        }

        //
        // Walk through the files looking for ones that look like
        // remove files.
        long currTime = System.currentTimeMillis();
        for(String curr : dir) {

            File currF = new File(indexDirFile.toString(), curr);
            if(!currF.exists()) {
                continue;
            }

            //
            // Check if this is a remove file and enough time has passed.
            if(remFilter.accept(curr)) {

                if(currF.lastModified() + partReapDelay <= currTime) {
                    //
                    // Treat it as a partition.
                    reapPartition(remFilter.partNum);
                    logger.fine("Reaped DP: " + remFilter.partNum);
                }
            } else if(delFilter.accept(curr) &&
                    !makeDictionaryFile(delFilter.partNum,
                                        "main").exists()) {

                //
                // Remove an orphaned deletion map.
                currF.delete();
                logger.fine("Orphan: " + curr);
            } else if(rtsdFilter.accept(curr)) {

                //
                // Removed term stat dictionary.  See if we should get rid
                // of it.
                if(currF.lastModified() + partReapDelay <= currTime) {

                    (new File(indexDirFile.toString(),
                              curr.replace(".rem",
                                           ""))).delete();
                    currF.delete();
                    logger.fine(String.format("Removed term stats %d",
                                              rtsdFilter.getTermStatsNumber()));
                }
            }
        }
    }

    /**
     * A method to reap a single partition.  This can be overridden in a
     * subclass so that the reap method will work for the super and
     * subclass.
     * @param partNumber the number of the partition to reap.
     */
    protected void reapPartition(int partNumber) {
        InvFileDiskPartition.reap(this, partNumber);
    }
    /**
     * A pattern for partition files that gets the partition number and the
     * extension.
     */
    private static Pattern ppat = Pattern.compile("^p(\\d+).*\\.([^.]*)$");

    /**
     * A class that implements SimpleFilter so that we can find partition
     * files with various extensions.
     */
    protected class ExtFilter implements SimpleFilter {

        /**
         * The partition number for the last file parsed.
         */
        protected int partNum;

        /**
         * The extension for the last file parsed.
         */
        protected String ext;

        /**
         * The extensions that we're looking for.
         */
        protected Set<String> exts;

        public ExtFilter(String... exts) {
            this.exts = new HashSet<String>();
            for(String ext : exts) {
                this.exts.add(ext);
            }
            partNum = -1;
        }

        /**
         * Tests whether this file has our extension.
         * @param s the name of the file.
         * @return <code>true</code> if this file name matches our extension,
         * <code>false</code> otherwise.
         */
        @Override
        public boolean accept(String s) {
            Matcher m = ppat.matcher(s);
            if(m.matches()) {
                partNum = Integer.parseInt(m.group(1));
                ext = m.group(2);
                return exts.contains(ext);
            }
            return false;
        }

        public int getPartNumber() {
            return partNum;
        }

        public String getExtension() {
            return ext;
        }
    }
    private static Pattern dtsp = Pattern.compile(
            ".*termstats\\.(\\d+)\\.dict.rem");

    protected class RTSFilter implements SimpleFilter {

        private int tsn;

        @Override
        public boolean accept(String s) {
            Matcher m = dtsp.matcher(s);
            if(m.matches()) {
                tsn = Integer.parseInt(m.group(1));
                return true;
            }
            return false;
        }

        public int getTermStatsNumber() {
            return tsn;
        }
    }

    /**
     * Purges the collection.  Closes and removes all partitions from the
     * active list and writes the list.
     */
    public synchronized void purge() {
        lastPurgeTime = new Date();
        try {
            activeFile.lock();

            //
            // Mark the partitions as removed and close them.
            for(DiskPartition p : activeParts) {
                try {
                    p.removedFile.createNewFile();
                } catch(java.io.IOException ioe) {
                    logger.severe("Error making removed file during purge");
                }
                p.setCloseTime(System.currentTimeMillis() + partCloseDelay);
                thingsToClose.add(p);
            }
            activeParts.clear();

            //
            // Write an empty active file, but leave the meta file since we
            // want to keep field definitions (and it doesn't matter if the
            // partition number or termstats number isn't reset)
            try {
                activeFile.write(activeParts);
            } catch(FileLockException fle) {
                logger.log(Level.SEVERE, "Exception locking active file during purge: " +
                        fle);
            } catch(java.io.IOException ioe) {
                logger.log(Level.SEVERE, "Error writing active file during purge: " +
                        ioe);
            }
            logger.info("Purged index in: " + indexDir);
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Exception locking active file during purge: " +
                    ioe);
        } catch(FileLockException fle) {
            logger.log(Level.SEVERE, "Exception locking active file during purge: " +
                    fle);
        } finally {
            try {
                activeFile.unlock();
            } catch(FileLockException fle) {
                logger.log(Level.SEVERE, "Exception unlocking active file during purge: " +
                        fle);
            }
        }
    }

    /**
     * Gets the index configuration for this manager.
     * @return the configuration for this index
     */
    public IndexConfig getIndexConfig() {
        return indexConfig;
    }

    /**
     * Gets the query configuration for this manager.
     * @return the query configuration for this index
     */
    public QueryConfig getQueryConfig() {
        return engine.getQueryConfig();
    }

    public CollectionStats getCollectionStats() {
        return collectionStats;
    }

    /**
     * Gets the total number of documents managed.
     * @return the total number of documents
     */
    public int getNDocs() {
        return (new CollectionStats(this)).getNDocs();
    }

    /**
     * Gets the total number of tokens indexed.
     * @return the number of tokens represented by the index.
     */
    public long getNTokens() {
        return new CollectionStats(this).getNTokens();
    }

    /**
     * Gets the total number of terms indexed.
     * @return an the number of unique terms in the indexed material
     */
    public int getNTerms() {
        return termStatsDict.size();
    }

    /**
     * Shuts down the manager.  Mostly this consists of writing the list of
     * active partitions.  This requires the file to be locked.
     *
     * @throws java.io.IOException if there is an error writing the active
     * file of partitions or closing one of the partitions.
     */
    public synchronized void shutdown() throws java.io.IOException {

        //
        // If there is a thread merging, then wait for it to finish.
        while(mergeThread != null) {

            logger.info("Waiting for merge thread to finish");
            try {
                mergeThread.join(250);
            } catch(InterruptedException ie) {
            }
        }

        timer.cancel();

        //
        // Do a final reap.
        reap();

        //
        // If this was a long indexing run, and we're supposed to compute
        // document vector lengths, then do a final merge and take the
        // opportunity to compute vector lengths and a term stats dictionary.
        //
        // Once that's done, we need to re-write the active partitions list!
        if(calculateDVL && engine.getLongIndexingRun()) {
            try {
                DiskPartition mdp = mergeAll();
                DocumentVectorLengths.calculate(mdp, termStatsDict,
                                                true);
                activeParts.clear();
                activeParts.add(mdp);
                activeFile.write(activeParts);
            } catch(FileLockException ex) {
                logger.log(Level.SEVERE, "Error locking active file after " +
                        " long indexing run merge", ex);
            }
        }

        //
        // Close the open partitions and whatever else needs to be closed.
        for(DiskPartition p : activeParts) {
            p.close();
        }
        activeParts.clear();

        for(Closeable c : thingsToClose) {
            c.close(Long.MAX_VALUE);
            c.createRemoveFile();
        }
        thingsToClose.clear();

        logger.fine("Shutdown " + indexDir + " " + randID);
    }

    /**
     * Recovers an index directory.  This method should not be called
     * when other processes are modifying the index, or really bad stuff
     * will happen.
     *
     * @param iD The index directory to recover.
     * @throws java.io.IOException if there are any errors recovering the
     * directory
     */
    public static void recover(String iD) throws java.io.IOException {

        File idf = new File(iD);
        File af = new File(iD + File.separator + "AL." + "PM");

        //
        // Read the active file.
        RandomAccessFile raf = new RandomAccessFile(af, "rw");

        int nParts = raf.readInt();
        int[] parts = new int[nParts];
        for(int i = 0; i < parts.length;
                i++) {
            parts[i] = raf.readInt();
        }
        raf.close();

        //
        // Get the files in the directory.
        File[] dir = idf.listFiles();

        if(dir == null) {
            logger.severe("Recover unable to list directory files.");
            return;
        }

        //
        // Walk through the files, removing locks and checking for orphaned
        // partitions.
        for(int i = 0; i < dir.length;
                i++) {
            String n = dir[i].getName();
            if(n.endsWith(".lock")) {
                //
                // Remove lock file.
                if(!dir[i].delete()) {
                    logger.severe("Failed to delete lock file " +
                            dir[i].getName());
                }
            } else if(n.charAt(0) == 'p' && n.endsWith("post")) {

                //
                // Check if there's a partition number.
                int ind = n.indexOf('.');
                if(ind > 0) {
                    try {
                        int partNum = Integer.parseInt(n.substring(1, ind));

                        //
                        // If this partition is not in the active list, then
                        // we'll remove it.
                        for(int j = 0; j < parts.length;
                                j++) {
                            if(parts[j] == partNum) {
                                if(!makeRemovedPartitionFile(iD, partNum).
                                        createNewFile()) {
                                    logger.severe("Failed to create rem file for part " +
                                            partNum);
                                }
                                break;
                            }
                        }
                    } catch(NumberFormatException nfe) {
                        continue;
                    }
                }
            }
        }
    }

    /**
     * Makes a <code>File</code> for the active file.
     * @return a file for the active file for this index
     */
    protected File makeActiveFile() {
        return new File(indexDir + File.separator + "AL." + logTag);
    }

    /**
     * Makes a <code>File</code> for the global term stats.
     * @param tsn the number of the term statistics file to make
     * @return a file for the global term statistics for this index
     */
    protected File makeTermStatsFile(int tsn) {
        return new File(indexDir, "termstats." + tsn + ".dict");
    }

    /**
     * Makes a <code>File</code> for the meta file.
     * @return a file for the meta file file this index
     */
    protected File makeMetaFile() {
        return new File(indexDir + File.separator + "MF." + logTag);
    }

    /**
     * Makes a <code>File</code> for a dictionary.
     *
     * @param iD The index directory
     * @param partNumber The number of the partition for which we're making
     * a dictionary <code>File</code>.
     * @param type The dictionary type.
     * @return A <code>File<code> initialized with an appropriate path.
     * name.
     */
    public static File makeDictionaryFile(String iD, int partNumber,
                                          String type) {
        return new File(iD + File.separator + "p" + partNumber +
                (type != null ? ("." +
                type) : "") + ".dict");
    }

    /**
     * Makes a <code>File</code> for a dictionary.
     *
     * @param partNumber The number of the partition for which we're making
     * a dictionary <code>File</code>.
     * @param type the dictionary type
     * @return A <code>File<code> initialized with an appropriate path.
     * name.
     */
    public File makeDictionaryFile(int partNumber, String type) {
        return makeDictionaryFile(indexDir, partNumber, type);
    }

    /**
     * Makes a <code>File</code> for a postings file.
     *
     * @param iD The index directory
     * @param partNumber The number of the partition for which we're making
     * a postings file
     * @param type The type of postings file
     * @param number The number of the postings file.  If this is less than
     * 0, it will be ignored.
     * @return A <code>File</code> initialized with an appropriate path.
     * name.
     */
    public static File makePostingsFile(String iD, int partNumber,
                                        String type,
                                        int number) {
        return new File(iD + File.separator + "p" + partNumber +
                (type != null ? ("." +
                type) : "") +
                (number > 0 ? ("." + number) : "") + ".post");
    }

    /**
     * Makes a <code>File</code> for a postings file.
     *
     * @param partNumber The number of the partition for which we're making
     * a postings file
     * @param type The type of postings file
     * @param number The number of the postings file.  If this is less than
     * 0, it will be ignored.
     * @return A <code>File</code> initialized with an appropriate path.
     * name.
     */
    public File makePostingsFile(int partNumber, String type, int number) {
        return makePostingsFile(indexDir, partNumber, type, number);
    }

    /**
     * Makes a <code>File</code> for a postings file.
     *
     * @param partNumber The number of the partition for which we're making
     * a postings file
     * @param type The type of postings file
     * @return A <code>File</code> initialized with an appropriate path.
     * name.
     */
    public File makePostingsFile(int partNumber, String type) {
        return makePostingsFile(indexDir, partNumber, type, -1);
    }

    /**
     * Makes a <code>File</code> for the file containing the bitmap of
     * deleted documents.
     *
     * @param iD The index directory
     * @param partNumber The number of the partition for which we're making a
     * taxonomy <code>File</code>.
     * @return A <code>File</code> initialized with an appropriate path.
     * name.
     */
    public static File makeDeletedDocsFile(String iD, int partNumber) {
        return new File(iD + File.separator + "p" + partNumber + ".del");
    }

    /**
     * Makes a <code>File</code> for the file containing the bitmap of
     * deleted documents.
     *
     * @param partNumber The number of the partition for which we're making a
     * taxonomy <code>File</code>.
     * @return A <code>File</code> initialized with an appropriate path.
     * name.
     */
    public File makeDeletedDocsFile(int partNumber) {
        return makeDeletedDocsFile(indexDir, partNumber);
    }

    /**
     * Makes a <code>File</code> for the file containing the lengths of
     * document vectors.
     *
     * @param partNumber The number of the partition for which we're making a
     * taxonomy <code>File</code>.
     * @return A <code>File</code> initialized with an appropriate path.
     * name.
     */
    public File makeVectorLengthFile(int partNumber) {
        return makeVectorLengthFile(indexDir, partNumber);
    }

    /**
     * Makes a <code>File</code> for the file containing the lengths of
     * document vectors.
     *
     * @param iD The index directory
     * @param partNumber The number of the partition for which we're making a
     * removed <code>File</code>.
     * @return A <code>File</code> initialized with an appropriate path.
     * name.
     */
    public static File makeVectorLengthFile(String iD, int partNumber) {
        return new File(iD + File.separator + "p" + partNumber + ".vl");
    }

    /**
     * Makes a <code>File</code> that we'll use to indicate that this
     * partition has been merged away.
     *
     * @param iD The index directory
     * @param partNumber The number of the partition for which we're making a
     * removed <code>File</code>.
     * @return A <code>File</code> initialized with an appropriate path.
     * name.
     */
    public static File makeRemovedPartitionFile(String iD, int partNumber) {
        return new File(iD + File.separator + "p" + partNumber + ".rem");
    }

    /**
     * Makes a <code>File</code> that we'll use to indicate that this
     * partition has been merged away.
     *
     * @param partNumber The number of the partition for which we're making a
     * removed <code>File</code>.
     * @return A <code>File</code> initialized with an appropriate path.
     * name.
     */
    public File makeRemovedPartitionFile(int partNumber) {
        return makeRemovedPartitionFile(indexDir, partNumber);
    }

    public static File makeTaxonomyFile(String iD, int partNumber) {
        return new File(iD + File.separator + "p" + partNumber + ".tax");
    }

    public File makeTaxonomyFile(int partNumber) {
        return makeTaxonomyFile(indexDir, partNumber);
    }

    /**
     * Gets the search engine associated with this PartitionManager instance
     *
     * @return the search engine
     */
    public SearchEngine getEngine() {
        return engine;
    }

    /**
     * Sets the search engine associated with this partition manager.
     * @param engine the engine associated with this manager
     */
    public void setEngine(SearchEngineImpl engine) {
        this.engine = engine;
    }

    public void noMoreMerges() {
        noMoreMerges = true;
    }

    /**
     * Merges all partitions from the active list into a new partition.
     * The merged partitions are removed from the active list, and the new
     * partition is then placed there.
     *
     * @return the merged partition.  This may be an existing partition!
     */
    public synchronized DiskPartition mergeAll() {

        //
        // No need to merge one partition.
        if(activeParts.size() == 1) {
            return activeParts.peek();
        }

        //
        // Do the merge and reap any deleted partitions.
        Merger m;
        m = getMerger((List<DiskPartition>) getActivePartitions());
        return m.merge();
    }

    // XXX We're still not handling deleted docs > 20% of partition merge
    // trigger XXX Should try to remove the need for delteOld and remap
    /**
     * This is a geometric merge heuristic controlled by the mergeRate. It
     * determines which partitions on the active list can be merged.
     *
     * @return The list of partitions that should be merged, or
     * <code>null</code> if none should be merged.
     * @see #setMergeRate
     */
    public List<DiskPartition> mergeGeometric() {

        //
        // Get a list of the active partitions sorted by the number of
        // documents that they contain.
        List<DiskPartition> parts = (List<DiskPartition>) getActivePartitions();
        Collections.sort(parts,
                         new Comparator<DiskPartition>() {

            public int compare(DiskPartition o1, DiskPartition o2) {
                return o1.getMaxDocumentID() - o2.getMaxDocumentID();
            }
        });

        //
        // Check each partition in turn to see if it overflows the
        // index capacity for the geometric sequence level it is on.
        // We enforce mergeRate parts per level and a total
        // doc capacity for levels 1..n of mergeRate^n - 1.
        int numDocs = 0; // total number of docs so far
        int partDocs = 1; // partition capacity at this level
        int mergePart = -1; // the largest overflow partition
        int levelParts = 0; // number of parts seen on this level
        for(int p = 0; p < parts.size();
                p++) {
            DiskPartition dp = parts.get(p);

            while(dp.getMaxDocumentID() > partDocs) {
                //
                // Move to the next level.
                partDocs *= mergeRate;
                levelParts = 0;
            }
            //
            //  Total capacity below this level
            int capacity = partDocs * mergeRate;
            numDocs += dp.getMaxDocumentID();
            if(++levelParts >= mergeRate || numDocs >= capacity) {
                //
                // This partition overflows this level
                mergePart = p;
            }
        }

        //
        // If there are no partitions to merge, return null.
        if(mergePart == -1) {
            return null;
        }

        //
        // Merge all partitions up to and including mergePart.  Nb: This
        // will merge all partitions at the mergePart level and below.
        List<DiskPartition> toMerge =
                new ArrayList<DiskPartition>();
        for(int p = 0; p <= mergePart && p < maxMergeSize; p++) {
            toMerge.add(parts.get(p));
        }

        return toMerge;
    }

    /**
     * Merges together the partitions in the provided
     * list.  Merging is done in blocks of <code>mergeBlockSize</code>
     * partitions, ordered by the partition number.  We do this in order to
     * avoid problems with running out of file handles for the files making
     * up the partitions.
     *
     * @param parts a list of partitions to merge
     * @return the merged partition
     */
    protected DiskPartition merge(List<DiskPartition> parts) {

        //
        // Catch an empty list.
        if(parts.size() == 0) {
            return null;
        }

        //
        // Take a copy of the list.
        List<DiskPartition> local =
                new ArrayList<DiskPartition>(parts);

        //
        // Sort the copy.
        Collections.sort(local);

        //
        // If there's more than mergeBlockSize elements in the list, we
        // need to call the merge helper function.
        DiskPartition ndp;
        if(local.size() > maxMergeSize) {
            ndp = mergeInPieces(local);
        } else {
            ndp = realMerge(local, true);
        }

        return ndp;
    }

    /**
     * Breakss a list of partitions into blocks of
     * <code>mergeBlockSize</code>, and merges those.  This method will work
     * recursively if there are enough blocks to justify it.
     *
     * @param parts the partitions that we want to merge
     * @return the merged partition
     */
    protected DiskPartition mergeInPieces(List<DiskPartition> parts) {

        //
        // Compute the number of blocks that we'll need to merge.
        int nBlocks = parts.size() / maxMergeSize;
        if(parts.size() % maxMergeSize != 0) {
            nBlocks++;
        }
        logger.fine("Merging partitions in " + nBlocks + " blocks.");

        List<DiskPartition> newParts =
                new ArrayList<DiskPartition>();
        List<DiskPartition> temp =
                new ArrayList<DiskPartition>();

        for(int i = 0; i < nBlocks; i++) {
            int begin = i * maxMergeSize;
            int end;
            if(i < nBlocks - 1) {
                end = begin + maxMergeSize;
            } else {
                end = parts.size();
            }

            //
            // Make a list of the partitions in this block.
            temp.clear();
            for(int j = begin; j < end; j++) {
                temp.add(parts.get(j));
            }

            logger.fine("Block " + (i + 1) + ": ");

            //
            // Really merge this block and get back a new partition.
            DiskPartition tdp = realMerge(temp, false);

            if(tdp != null) {
                newParts.add(tdp);
            }
        }

        //
        // If there were no new partitions created, then we can just return
        // now, indicating that we didn't generate a partition.
        if(newParts.size() == 0) {
            return null;
        }

        //
        // If the merge resulted in too many partitions, then recurse.
        DiskPartition ndp;
        if(newParts.size() >= maxMergeSize) {
            ndp = mergeInPieces(newParts);
        } else {
            ndp = realMerge(newParts, true);
        }

        //
        // Reap the intermediate partitions.  Only we ever saw them so
        // there's no need for the close or reap delay.
        for(DiskPartition dp : newParts) {
            DiskPartition.reap(this, dp.getPartitionNumber());
        }

        return ndp;
    }

    /**
     * Merges a list of partitions.  This is the
     * base case for the recursive merge.
     *
     * @param diskParts the actual partitions to merge
     * @param calculateDVL if <code>true</code>, calculate the document vector
     * lengths for the documents in the merged partition
     * @return the partition resulting from the merge
     */
    protected DiskPartition realMerge(List<DiskPartition> diskParts,
                                      boolean calculateDVL) {
        //
        // If we don't have any partitions left, then we're done.
        if(diskParts.size() == 0) {
            return null;
        }

        List<DelMap> delMaps = new ArrayList<DelMap>();
        for(DiskPartition dp : diskParts) {
            delMaps.add((DelMap) dp.deletions.clone());
        }

        //
        // Run the merge, using the first element.
        DiskPartition ndp = null;
        try {
            ndp = diskParts.get(0).merge(diskParts, delMaps, calculateDVL);
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error merging partitions: " + diskParts, e);
        }

        //
        // If we were successful, then return the new partition number for
        // the merged partition.
        if(ndp != null) {
            logger.info("Partitions merged.  New partition: " + ndp);

            //
            // Set the merged partitions for closing and mark the partitions
            // to be removde.
            for(int i = 0; i < diskParts.size();
                    i++) {
                DiskPartition dp = diskParts.get(i);
                dp.setCloseTime(System.currentTimeMillis() + partCloseDelay);
                thingsToClose.add(dp);
                try {
                    dp.removedFile.createNewFile();
                } catch(java.io.IOException ioe) {
                    logger.log(Level.SEVERE, "Unable to create removed file " +
                            "after merge" +
                            ioe);
                }
            }
        }


        //
        // Return the new partition number.
        return ndp;
    }

    /**
     * Gets the next number to use for a partition.  This requires locking
     * the partition number file for our entry type.  Errors during the
     * reading, writing, or locking of this file will cause a random
     * partition number between 500,000 and 1,000,000 to be generated.
     *
     * @return the partition number for the next partition to write.
     */
    protected int getNextPartitionNumber() {
        try {
            return metaFile.getNextPartitionNumber();
        } catch(java.lang.Exception ex) {
            //
            // Exception ex could be IOException or FileLockException
            //
            // Make a random file number that isn't already taken.
            int nextPartitionNumber;
            Random rand = new java.util.Random();
            while(true) {
                nextPartitionNumber = rand.nextInt(1000000) + 500000;
                File f = makeDictionaryFile(nextPartitionNumber, "main");
                try {
                    if(f.createNewFile()) {
                        logger.warning("Error accessing partition number: " +
                                ex +
                                " using random partition number: " +
                                nextPartitionNumber);
                        break;
                    }
                } catch(java.io.IOException ioe) {
                }
            }
            return nextPartitionNumber;
        }
    }

    /**
     * Gets a list of the partition numbers for the given partitions.  This can
     * be used when we want to act on collections of partition numbers, rather
     * than the partitions themselves.
     *
     * @param parts the partitions for which we want the numbers
     * @return an list of the active partition numbers.
     */
    protected List<Integer> getPartNumbers(Collection<DiskPartition> parts) {
        List<Integer> ret = new ArrayList<Integer>();
        synchronized(parts) {
            for(DiskPartition dp : parts) {
                ret.add(dp.getPartitionNumber());
            }
        }
        return ret;
    }

    /**
     * Gets a list of partitions from the corresponding partition numbers.
     * Partitions will be loaded as necessary, and numbers for partitions that
     * do not exist will be logged and ignored.
     *
     * @param partNums the partition numbers for the partitions that we want.
     * @return a list of the partitions corresponding to the numbers given.
     */
    protected List<DiskPartition> getPartitions(List<Integer> partNums) {
        List<DiskPartition> ret = new ArrayList<DiskPartition>();
        synchronized(activeParts) {
            for(DiskPartition dp : activeParts) {
                if(partNums.remove(new Integer(dp.getPartitionNumber()))) {
                    ret.add(dp);
                }
            }
        }

        for(Integer pn : partNums) {
            try {
                ret.add(newDiskPartition(pn, this));
            } catch(IOException ex) {
                logger.warning("Error getting partition: " + pn);
            }
        }
        return ret;
    }

    /**
     * Get the meta file for this index.
     *
     * @return the meta file for this index.
     */
    public MetaFile getMetaFile() {
        return metaFile;
    }

    public List<String> getFieldNames() {
        MetaFile mf = getMetaFile();
        List<String> l =
                new ArrayList<String>(mf.size());
        for(Iterator it = mf.fieldIterator(); it.hasNext();) {
            l.add(((FieldInfo) it.next()).getName());
        }
        return l;
    }

    /**
     * Gets the information for a named field.
     * @param name the name of the field for which we want information
     * @return the field information for the named field, or <code>null</code>
     * if there is no field with the given name
     */
    public FieldInfo getFieldInfo(String name) {
        return metaFile.getFieldInfo(name);
    }

    /**
     * Gets the name of the index.
     * @return the configuration name of the index
     */
    public String getName() {
        return name;
    }

    public Date getLastPurgeTime() {
        return lastPurgeTime;
    }

    /**
     * Gets an instance of the merger class that can be used to merge any
     * partitions that require it.  If no merge is currently required, then
     * <code>null</code> is returned.
     *
     * @return An instance of <code>Merger</code> that can be used to merge
     * these partitions, or <code>null</code> if no merge is currently
     * possible.
     */
    public Merger getMerger() {
        return getMerger(mergeGeometric(), mergeLock);
    }

    /**
     * Gets an instance of the merger class in order to merge a list of
     * partitions.
     *
     * @param l A list of partitions to merge, such as that returned by
     * <code>mergeGeometric</code>.
     * @return An instance of <code>Merger</code> that can be used to merge
     * these partitions, or <code>null</code> if no merge is currently
     * possible.
     */
    public Merger getMerger(List<DiskPartition> l) {
        return getMerger(l, mergeLock);
    }

    /**
     * Gets an instance of the merger class in order to merge a list of
     * partitions.
     *
     * @param l A list of partitions to merge, such as that returned by
     * <code>mergeGeometric</code>.
     * @param localMergeLock the lock to use for running the merge.
     * @return An instance of <code>Merger</code> that can be used to merge
     * these partitions, or <code>null</code> if no merge is currently
     * possible.
     */
    public Merger getMerger(List<DiskPartition> l, FileLock localMergeLock) {

        //
        // They may not have checked the results of mergeGeo.
        if(l == null) {
            return null;
        }

        try {
            localMergeLock.acquireLock();
            Merger m = new Merger(l, localMergeLock);
            return m;
        } catch(Exception e) {

            //
            // We didn't get the merge lock.  Return null.
            return null;
        }
    }

    /**
     * Gets a merger that will merge the partitions represented by the given
     * list of partition numbers.
     * @param l a list of the numbers of some partitions that we would like to
     * merge
     * @return a merger for the partitions corresponding to the numbers in the
     * list
     */
    public Merger getMergerFromNumbers(List<Integer> l) {
        return getMerger(getPartitions(l));
    }

    /**
     * A threadable class used for merging a list of partitions.
     */
    public class Merger implements Runnable {

        /**
         * The new disk partition resulting from the merge.
         */
        protected DiskPartition newDP;

        /**
         * The lock that we'll need to acquire to merge.
         */
        private FileLock localMergeLock;

        /**
         * Instantiates a merger for the given list of partitions.  The
         * resulting partition will be added to the active list of the
         * given manager.
         *
         * @param l The list of partitions to merge.
         * @param localMergeLock the lock file to use when releasing the merge
         * lock.
         */
        public Merger(List<DiskPartition> l, FileLock localMergeLock) {

            parent = Thread.currentThread();
            newPart = -1;

            toMerge = new ArrayList<DiskPartition>(l);
            this.localMergeLock = localMergeLock;

            //
            // Sort the list by partition number.
            Collections.sort(toMerge);

            //
            // Also, we want to keep a copy of the deletion bitmaps before
            // we started the merge.  This will allow us to construct a
            // deletion bitmap for the merged partition when we're done.
            preDelMaps = new ArrayList<DelMap>();
            preMaps = new ReadableBuffer[toMerge.size()];
            for(int i = 0; i < toMerge.size(); i++) {
                preDelMaps.add((DelMap) toMerge.get(i).deletions.clone());
                preMaps[i] = preDelMaps.get(i).getDelMap();
//		log.debug(logTag, 0, toMerge.get(i) + " preMap:\n" +
//			  preMaps[i]);
            }
        }

        /**
         * Merges.
         */
        public DiskPartition merge() {
            run();
            return newDP;
        }

        /**
         * Does the merge of the partitions.
         */
        public void run() {

            try {
                localMergeLock.tradeLock(parent, Thread.currentThread());
            } catch(FileLockException fle) {
                logger.severe("Failed to trade merge lock");
                mergeThread = null;
                return;
            }

            newDP = null;

            //
            // Do the merge.
            try {
                newDP = toMerge.get(0).merge(toMerge, preDelMaps, true);
            } catch(Exception e) {
                logger.log(Level.SEVERE, "Exception merging partitions: " +
                        toMerge,
                           e);
            }

            if(newDP != null) {

                boolean releaseNeeded = false;

                try {
                    activeFile.lock();

                    //
                    // Fix up the deletion bitmap for the new
                    // partition, since deletions may have happened in
                    // the partitions that we were merging, while we
                    // were merging them.  We need to xor the bitmaps
                    // from before the merge with the ones from after,
                    // and set the bits for any new deletions in the
                    // new document ID space of the mapped partition.
                    // Clear?
//                    log.debug(logTag, 0, randID +
//                            " starting post-merge deletions");
                    for(int i = 0, incr = 0; i < toMerge.size(); i++) {

                        DiskPartition curr = toMerge.get(i);

                        //
                        // Synchronize the deletion bitmap for this
                        // partition, and get the result.
                        ReadableBuffer postMap = curr.deletions.syncGetMap();
//                        logger.finer(curr + " postMap:\n" + postMap);

                        //
                        // No deletions, means move on.
                        if(preMaps[i] == null && postMap == null) {
                            incr += curr.stats.nDocs;
                            continue;
                        }

                        int[] idMap = null;
                        ArrayBuffer xor = (ArrayBuffer) postMap;

                        //
                        // If the preMap wasn't null, we need to find all of the
                        // documents that were deleted during the merge and make
                        // sure to delete those.
                        if(preMaps[i] != null) {

                            //
                            // Get the ID map in the merged partition.
                            idMap = curr.getDocIDMap(preMaps[i]);

                            //
                            // Do the exclusive or.
                            xor.xor(preMaps[i]);
                        }

//                        logger.info(curr + " xor:\n" + xor);

                        //
                        // Now we have a buffer with bits set for only the
                        // newly deleted documents.  We need to find out
                        // what the document IDs are for those documents in
                        // the new partition, and delete them.
                        for(int origID = 1; origID <= curr.getMaxDocumentID();
                                origID++) {
                            if(xor.test(origID)) {
//                                logger.info("deleted during merge: " + origID);
                                int mapID;
                                if(idMap == null) {
                                    mapID = origID;
                                } else {
                                    mapID = idMap[origID];
                                }
                                mapID += incr;

                                //
                                // Set the bit for this document.
                                newDP.deleteDocument(mapID);
                            }
                        }

                        //
                        // Fix up the increment.
                        if(idMap == null) {
                            incr += curr.stats.nDocs;
                        } else {
                            incr += idMap[0];
                        }
                    }

//                    logger.info("Finished deleting documents");

                    //
                    // Write the deleted bitmap for our new partition.
                    newDP.syncDeletedMap();

                    //
                    // Put the partitions we merged onto the
                    // list of merged partitions.
                    mergedParts.addAll(toMerge);

                    updateActiveParts(true);
                    if(newDP != null) {
                        activeParts.add(newDP);
                    }
                    activeFile.write(activeParts);

                    //
                    // Some day we'll want notification when partitions have been
                    // merged.
//                    synchronized(listeners) {
//                        for(IndexListener il : listeners) {
//
//                        }
//                    }
                } catch(FileLockException fle) {
                    mergeThread = null;
                    logger.log(Level.SEVERE, "Error locking active file after merge: " +
                            activeFile, fle);
                    try {
                        localMergeLock.releaseLock();
                    } catch(FileLockException fle2) {
                        logger.log(Level.SEVERE,
                                   "Error releasing merge lock after error after merge.",
                                   fle2);
                    }
                } catch(java.io.IOException ioe) {
                    logger.log(Level.SEVERE, "I/O Error after merge.", ioe);
                } catch(Exception e) {
                    logger.log(Level.SEVERE, "Exception after merge", e);
                } finally {
                    //
                    // Release the locks we're holding.
                    try {
                        activeFile.unlock();
                        localMergeLock.releaseLock();
                    } catch(FileLockException fle) {
                        logger.log(Level.SEVERE, "Error unlocking after error after merge: " +
                                activeFile, fle);
                    }
                }
            }


            mergeThread = null;
        }

        public String toString() {
            return toMerge.toString();
        }
        /**
         * The list of partitions that we wish to merge.
         */
        protected List<DiskPartition> toMerge;

        /**
         * The list of delmaps for the partitions.
         */
        protected List<DelMap> preDelMaps;

        /**
         * The deletion bitmaps for the partitions at startup.
         */
        protected ReadableBuffer[] preMaps;

        /**
         * The thread that instantiated us.  We need to take the merge lock
         * from them.
         */
        protected Thread parent;

        /**
         * The new partition created in the merge.
         */
        protected int newPart;

    }

    /**
     * An inner class that does housekeeping duties during querying.  These
     * duties include keeping up with changes to the active file, closing
     * partitions that are no longer in use, and reaping partitions that
     * have been removed from use.
     */
    protected class HouseKeeper extends TimerTask {

        private int ignored = 0;

        /**
         * The maximum number of times to ignore our duties, to avoid over-locking
         * the active file when it hasn't been changed.
         */
        public static final int MAX_IGNORE = 5;

        /**
         * Creates a housekeeper.
         */
        public HouseKeeper() {
            done = false;
            lastMod = activeFile.lastModified();
        }

        /**
         * Tells the house keeper to finish up.
         */
        public void finish() {
            done = true;
        }

        /**
         * Closes the partitions on the close list.
         * @param time the current time, in milliseconds since the epoch
         */
        protected void closeThings(long time) {
            synchronized(thingsToClose) {
                for(Iterator<Closeable> i = thingsToClose.iterator();
                        i.hasNext();) {
                    Closeable clo = i.next();
                    if(clo.close(time)) {
                        clo.createRemoveFile();
                        i.remove();
                    }
                }
            }
        }

        /**
         * Performs the housekeeping duties.
         */
        public void run() {

            //
            // See if the file has changed.  This may not be perfect,
            // but it will keep us from over-locking.
            long stamp = activeFile.lastModified();
            if(stamp == lastMod && ignored < MAX_IGNORE) {
                ignored++;
                return;
            }

            ignored = 0;

            //
            // Get any newly added partitions, but don't add
            // them to the active list yet.
            try {
                activeFile.lock();
            } catch(Exception e) {

                //
                // If we didn't get the lock, we'll just try again later.
                return;
            }

            //
            // See whether it's time to close any of the
            // partitions.
            closeThings(System.currentTimeMillis());

            //
            // Do a reap.
            reap();


            //
            // OK, I think we're ready to load some partitions now.  Figure
            // out the updates to the active partition list, but don't put
            // them on the list yet!
            try {
                //
                // We have the list of updated partitions.  Get the
                // updated deletion bitmaps without changing the copies
                // in the partions.
                for(DiskPartition dp : activeParts) {
                    dp.syncDeletedMap();
                }
                
                updateActiveParts(true);
            } catch(Exception e) {

                //
                // Exceptions here need to be logged!
                logger.log(Level.SEVERE, "Exception in HouseKeeper", e);
            } finally {
                try {
                    activeFile.unlock();
                } catch(FileLockException ex) {
                    logger.log(Level.SEVERE,
                               "Exception unlocking active file", ex);
                }
            }

            lastMod = stamp;
        }
        /**
         * The last modified time for the active file.
         */
        protected long lastMod;

        /**
         * Whether we're done or not.
         */
        protected boolean done;

    }
    /**
     * The directory where locks will be put.
     */
    protected File lockDirFile;

    /**
     * The file containing the list of active partitions.
     */
    protected ActiveFile activeFile;

    /**
     * A lock for the collection so that only one merge may be ongoing
     * at any time.
     */
    protected FileLock mergeLock;

    /**
     * The list of partitions that we're managing.
     */
    protected final Queue<DiskPartition> activeParts =
            new ConcurrentLinkedQueue<DiskPartition>();

    /**
     * The current statistics for this collection.
     */
    private CollectionStats collectionStats;

    /**
     * The list of parts to close.
     */
    protected final Queue<Closeable> thingsToClose =
            new ConcurrentLinkedQueue<Closeable>();

    /**
     * A list of partitions that have been merged.
     */
    protected final Queue<DiskPartition> mergedParts =
            new ConcurrentLinkedQueue<DiskPartition>();

    /**
     * A thread to be used during merge operations.
     */
    protected Thread mergeThread;

    /**
     * A house keeper class.
     */
    protected HouseKeeper keeper;

    /**
     * A timer for periodic events.
     */
    private Timer timer;

    /**
     * The <code>MetaFile</code> containing the number for the next partition
     * to write and the field name maps.
     */
    protected MetaFile metaFile;

    /**
     * The directory where the index is.  Shared by all PartitionManagers
     * in a given directory.
     */
    protected String indexDir;

    /**
     * The <code>File</code> containing the directory where the index is
     * held.
     */
    protected File indexDirFile;

    /**
     * The amount of space (in bytes) that we're willing to devote to
     * buffering postings entries during merges.  The default is 5MB.
     */
    protected int mergeSpace = 5 * 1024 * 1024;

    /**
     * The rate of partition merges - bigger means less merges,
     * faster indexing, more parts, slower queries
     */
    protected int mergeRate = 5;

    /**
     * A random number that we can use to tell the difference between
     * various partition managers.
     */
    protected int randID;

    public int getRandID() {
        return randID;
    }
    /**
     * The log.
     */
    static Logger logger = Logger.getLogger(PartitionManager.class.getName());

    /**
     * The tag for this module.
     */
    protected String logTag = "PM";

    /**
     * The subdirectory of the main index directory where we will put our
     * partitions.
     */
    protected String subDir;

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {

        //
        // If we're re-called, then do a shutdown before initializing.
        if(activeParts != null && activeParts.size() > 0) {
            try {
                shutdown();
            } catch(IOException ex) {
                logger.log(Level.SEVERE,
                           "Error shutting down for re-initialization",
                           ex);
            }
        }
        partitionFactory = (DiskPartitionFactory) ps.getComponent(
                PROP_PARTITION_FACTORY);
        indexConfig = (IndexConfig) ps.getComponent(PROP_INDEX_CONFIG);
        mergeRate = ps.getInt(PROP_MERGE_RATE);
        maxMergeSize = ps.getInt(PROP_MAX_MERGE_SIZE);
        activeCheckInterval = ps.getInt(PROP_ACTIVE_CHECK_INTERVAL);
        partCloseDelay = ps.getInt(PROP_PART_CLOSE_DELAY);
        asyncMerges = ps.getBoolean(PROP_ASYNC_MERGES);
        partReapDelay = ps.getInt(PROP_PART_REAP_DELAY);
        calculateDVL = ps.getBoolean(PROP_CALCULATE_DVL);
        lockDir = ps.getString(PROP_LOCK_DIR);
        openPartitionHighWaterMark = ps.getInt(
                PROP_OPEN_PARTITION_HIGH_WATER_MARK);
        openPartitionLowWaterMark =
                ps.getInt(PROP_OPEN_PARTITION_LOW_WATER_MARK);
        reapDoesNothing = ps.getBoolean(PROP_REAP_DOES_NOTHING);
        String startingData = ps.getString(PROP_STARTING_DATA);
        if(!startingData.equals("")) {
            startingDataDir = new File(startingData);
        }

        if(calculateDVL) {
            termstatsDictFactory =
                    (TermStatsFactory) ps.getComponent(
                    PROP_TERMSTATS_DICT_FACTORY);
        }
        init();
    }

    int getNumPostingsChannels() {
        return partitionFactory.mainDictFactory.getNumPostingsChannels();
    }

    /**
     * Indicates whether this index uses a cased main dictionary.
     *
     * @return <code>true</code> if the index stores cased information,
     * <code>false</code> otherwise.
     *
     */
    public boolean isCasedIndex() {
        return partitionFactory.mainDictFactory.hasCasedEntry();
    }

    /**
     * Indicates whether this index uses fielded document vectors.
     * @return <code>true</code> if the document vectors for this index contain
     * field information, <code>false</code> otherwise.
     */
    public boolean hasFieldedVectors() {
        return partitionFactory.documentDictFactory.getEntryClass() ==
                FieldedDocKeyEntry.class;
    }
    @ConfigComponent(type = DiskPartitionFactory.class)
    public static final String PROP_PARTITION_FACTORY =
            "partition_factory";

    private DiskPartitionFactory partitionFactory;

    @ConfigComponent(type = com.sun.labs.minion.IndexConfig.class)
    public static final String PROP_INDEX_CONFIG = "index_config";

    @ConfigInteger(defaultValue = 5)
    public static final String PROP_MERGE_RATE = "merge_rate";

    /**
     * A property for the maximum number of open partitions that we'll allow.
     * Once this number of open partitions is crossed, no new partitions will
     * be allowed to be dumped until the number of partitions can be decreased.
     * Generally speaking, this is an exceptional condition and it will cause
     * indexing to slow down substantially.  We need to monitor this because
     * having too many open partitions can lead to running out of filehandles.
     */
    @ConfigInteger(defaultValue = 40)
    public static final String PROP_OPEN_PARTITION_HIGH_WATER_MARK =
            "open_partition_high_water_mark";

    private int openPartitionHighWaterMark;

    /**
     * A property for the "low water" number of open partitions that we'll allow.
     * Once the highwater mark for open partitions has been reached, no more
     * partitions may be added to the index until the number of open partitions can
     * be decreased below this value.
     */
    @ConfigInteger(defaultValue = 20)
    public static final String PROP_OPEN_PARTITION_LOW_WATER_MARK =
            "open_partition_low_water_mark";

    private int openPartitionLowWaterMark;

    @ConfigInteger(defaultValue = 20)
    public static final String PROP_MAX_MERGE_SIZE =
            "max_merge_size";

    private int maxMergeSize;

    @ConfigInteger(defaultValue = 1000)
    public static final String PROP_ACTIVE_CHECK_INTERVAL =
            "active_check_interval";

    private int activeCheckInterval;

    @ConfigInteger(defaultValue = 15000)
    public static final String PROP_PART_CLOSE_DELAY = "part_close_delay";

    private int partCloseDelay;

    public int getPartCloseDelay() {
        return partCloseDelay;
    }

    public void setPartCloseDelay(int partCloseDelay) {
        this.partCloseDelay = partCloseDelay;
    }

    public boolean getCalculateDVL() {
        return calculateDVL;
    }
    @ConfigBoolean(defaultValue = true)
    public static final String PROP_ASYNC_MERGES = "async_merges";

    private boolean asyncMerges;

    @ConfigInteger(defaultValue = 15000)
    public static final String PROP_PART_REAP_DELAY = "part_reap_delay";

    private int partReapDelay;

    @ConfigBoolean(defaultValue = true)
    public static final String PROP_CALCULATE_DVL = "calculate_dvl";

    private boolean calculateDVL;

    @ConfigString
    public static final String PROP_LOCK_DIR = "lock_dir";

    private String lockDir;

    public String getLockDir() {
        return lockDir;
    }

    public void setLockDir(String lockDir) {
        this.lockDir = lockDir;
    }
    @ConfigComponent(type =
    com.sun.labs.minion.indexer.dictionary.TermStatsFactory.class, mandatory =
    false)
    public static final String PROP_TERMSTATS_DICT_FACTORY =
            "termstats_dict_factory";

    private TermStatsFactory termstatsDictFactory;

    @ConfigBoolean(defaultValue = false)
    public static final String PROP_REAP_DOES_NOTHING = "reap_does_nothing";

    private boolean reapDoesNothing;

    /**
     * A configuration property that can be used to name an index directory
     * whose contents should be copied into the current directory when it is
     * created.  All data in that directory will be copied, whether it's index
     * data or not.
     */
    @ConfigString(defaultValue = "", mandatory = false)
    public static final String PROP_STARTING_DATA = "starting_data";

    private File startingDataDir;

    /**
     * Gets the term statisitics dictionary for this index
     *
     * @return the term statistics dictionary for this index
     */
    public TermStatsDictionary getTermStatsDict() {
        return termStatsDict;
    }

    /**
     * Gets the term statistics for a term
     * @param name the name of the term for which we want term statistics
     * @return the statistics associated with the given name, or an empty set
     * of term statistics if there are none for the given name
     */
    public TermStatsImpl getTermStats(String name) {
        if(termStatsDict == null) {
            return new TermStatsImpl(name);
        }
        TermStatsEntry tse = termStatsDict.getTermStats(name);
        return tse == null ? new TermStatsImpl(name) : tse.getTermStats();
    }

    /**
     * Regenerates the term stats for the currently active partitions.  This can
     * be used after modifications have been made to an index manually.
     * @throws java.io.IOException if there is any error writing the new term
     * stats.
     * @throws com.sun.labs.minion.util.FileLockException if there is an error
     * locking the meta file to get the number for the next term stats dictionary.
     */
    public void recalculateTermStats() throws java.io.IOException,
            FileLockException {
        int tsn = metaFile.getNextTermStatsNumber();
        File newTSF = makeTermStatsFile(tsn);
        (new UncachedTermStatsDictionary()).recalculateTermStats(newTSF,
                                                                 getActivePartitions());
        metaFile.setTermStatsNumber(tsn);
        updateTermStats();
    }

    protected void updateTermStats() throws java.io.IOException,
            FileLockException {
        File newTSF = makeTermStatsFile(metaFile.getTermStatsNumber());
        if(!newTSF.equals(currTSF) && newTSF.exists()) {

            TermStatsDictionary oldTSD = termStatsDict;
            termStatsDict = termstatsDictFactory.getDictionary(newTSF);

            //
            // Set up the new dictionary.
            currTSF = newTSF;

            //
            // Set up to (eventually) close and delete the term stats dictionary, which might be in
            // use by someone else.
            oldTSD.setCloseTime(System.currentTimeMillis() + partCloseDelay * 2);
            oldTSD.setClosed();
            thingsToClose.add(oldTSD);
        }
    }
} // PartitionManager

