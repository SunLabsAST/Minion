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

import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;


import com.sun.labs.minion.IndexConfig;
import java.util.ArrayList;
import java.util.List;
import com.sun.labs.minion.engine.SearchEngineImpl;

import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.postings.DocOccurrence;

import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
import com.sun.labs.minion.util.StopWatch;

/**
 * A class for holding a partition in memory while it is under
 * construction.  A partition consists of four files:
 *
 * <ol>
 * <li> The dictionary, which contains a mapping from terms to various
 * offset information.
 * <li> The postings data for the files in the partition.
 * <li> The "document dictionary", which contains information about the
 * documents indexed in this partition (e.g., the title)
 * <li> The taxonomy for the terms indexed in the partition.
 * </ol>
 *
 * Such a partition cannot be used for searching.
 *
 * @see DiskPartition
 * @see com.sun.labs.minion.indexer.dictionary.MemoryDictionary
 * @see com.sun.labs.minion.indexer.dictionary.MemoryFieldStore
 */
public abstract class MemoryPartition extends Partition {
    
    /**
     * The main dictionary.
     */
    protected MemoryDictionary mainDict;
    
    /**
     * The document dictionary.
     */
    protected MemoryDictionary docDict;
    
    /**
     * The key for the document that we're currently processing, from the
     * document dictionary.
     */
    protected DocKeyEntry dockey;
    
    /**
     * An occurrence that we can use to add data to postings for the
     * document dictionary entries.
     */
    protected DocOccurrence ddo;
    
    /**
     * A deleted map to use when the same document comes along in the same
     * partition.
     */
    protected DelMap del;
    
    /**
     * The number of bytes of postings data we've encoded so far.
     */
    protected long postBytes;
    
    /**
     * The number of words in the current document.
     */
    protected int nWords;
    
    /**
     * The tag for this module.
     */
    protected static String logTag = "MP";
    
    protected String name;
    
    public MemoryPartition() {
    }
    
    /**
     * Dumps the current partition.
     *
     * @return The partition number for the dumped partition.
     * @throws java.io.IOException if there is any error writing the partition
     * data to disk
     */
    protected int dump() throws java.io.IOException {
        
        //
        // Do nothing if we have no data.
        if(docDict.size() == 0) {
            return -1;
        }
        
        long start = System.currentTimeMillis();
        
        partNumber = manager.getNextPartitionNumber();
        
        File[] files = getMainFiles();
        
        //
        // Get a channel for the main dictionaries.
        RandomAccessFile dictFile = new RandomAccessFile(files[0], "rw");
        
        //
        // Get channels for the postings.
        OutputStream[] postStream = new OutputStream[files.length-1];
        PostingsOutput[] postOut = new PostingsOutput[files.length-1];
        for(int i = 1; i < files.length; i++) {
            postStream[i-1] =
                    new BufferedOutputStream(new FileOutputStream(files[i]),
                    32768);
            postOut[i-1] = new StreamPostingsOutput(postStream[i-1]);
        }
        
        String indexDir = manager.getIndexDir();
        
        //
        // Dump the main dictionary
        StopWatch sw = new StopWatch();
        sw.start();
        IndexEntry[] sorted = mainDict.dump(indexDir,
                new StringNameHandler(),
                stats,
                dictFile, postOut,
                MemoryDictionary.Renumber.RENUMBER, 
                MemoryDictionary.IDMap.NEWTOOLD,
                null);
        
        sw.stop();
        log.log(logTag, 4, "Main dictionary dump: " + sw.getTime());
        sw.reset();
        
        //
        // Close off those files.
        dictFile.close();
        for(int i = 0; i < postOut.length; i++) {
            postStream[i].close();
        }
        
        //
        // Get channels for the document key dictionary.
        files = getDocFiles();
        dictFile = new RandomAccessFile(files[0], "rw");
        BufferedOutputStream dictPostStream =
                new BufferedOutputStream(new FileOutputStream(files[1]),
                8196);
        
        //
        // Set the number of documents.
        stats.nDocs = docDict.size();
        
        //
        // Write the partition statistics to the document dictionary
        // channel.
        stats.write(dictFile);
        
        //
        // Dump the document dictionary and postings.  We won't remap the
        // IDs assigned to the keys, but we will need to remap the IDs in
        // the postings list using the ID map from the case insensitive
        // dictionary.
        sw.start();
        docDict.dump(indexDir,
                new StringNameHandler(),
                dictFile,
                new PostingsOutput[] {
            new StreamPostingsOutput(dictPostStream)
        },
                MemoryDictionary.Renumber.NONE,
                MemoryDictionary.IDMap.NONE,
                mainDict.getIdMap());
        dictFile.close();
        dictPostStream.close();
        sw.stop();
        log.log(logTag, 4, "Document dictionary dump: " + sw.getTime());
        sw.reset();
        
        //
        // If we deleted some documents along the way, then dump that data
        // now.
        if(del.getNDeleted() > 0) {
            log.log(logTag, 4, "Dump deleted documents");
            del.write(manager.makeDeletedDocsFile(partNumber));
        }
        
        //
        // Dump any custom data -- to be filled in by subclasses.
        dumpCustom(sorted);
        
        //
        // Reset the per-partition data.
        postBytes = 0;
        del = new DelMap();
        
        //
        // If we're calculating vector lengths and we're not in the middle of a
        // long indexing run, then initialize the document vectors for the new
        // partition and (while we're there) build new term statistics for the 
        // index.
        DiskPartition ndp = manager.newDiskPartition(partNumber, manager);
        if(manager.getCalculateDVL() && 
                !((SearchEngineImpl) manager.getEngine()).getLongIndexingRun()) {
            sw.start();
            ndp.initDVL(true);
            sw.stop();
            log.log(logTag, 4, "Init DVL: " + sw.getTime());
        }
        
        //
        // Log the dump.
//        log.log(logTag, 2, String.format("%d Dump: %d %s%d docs, %d terms, %dms",
//                manager.getRandID(),
//                partNumber,
//                deleted.size() > 0 ? (deleted + " deleted docs ") : "", 
//                docDict.size(),
//                mainDict.size(),
//                (System.currentTimeMillis() - start)));
        log.log(logTag, 2, String.format("Dump: %d %d docs, %d terms, %dms",
                partNumber,
                docDict.size(),
                mainDict.size(),
                (System.currentTimeMillis() - start)));
        
        manager.addNewPartition(ndp, docDict.getKeys());
        
        //
        // Some stats for our partition.
        stats = new PartitionStats();
        
        //
        // Clear the dictionaries for the next chunk.
        mainDict.clear();
        docDict.clear();
        
        return partNumber;
    }
    
    /**
     * Gets an entry from the in-memory document dictionary.  This can be
     * used to get a document vector for a document that has not been committed
     * to disk.
     * @param key the key of the document that we want the entry for
     * @return the entry for the given key, or <code>null</code> if this key 
     * doesn't occur in this partition.
     */
    public DocKeyEntry getDocumentTerm(String key) {
        return (DocKeyEntry) docDict.get(key);
    }
    
    /**
     * Performs any custom data dump required in a subclass.  This method
     * exists to be overridden in a subclass and provides no functionality
     * at this level.
     *
     * @param sorted the sorted array of main dictionary entries, which might
     *        be useful in a subclass
     * @throws java.io.IOException if there is any error writing the data
     * to disk
     */
    protected void dumpCustom(Entry[] sorted) throws java.io.IOException {
    }
    
    /**
     * Tells a stage that its data must be dumped to the index.
     *
     * @param iC The configuration for the index, which can be used to
     * retrieve things like the index directory.
     */
    public void dump(IndexConfig iC) {
        try {
            dump();
        } catch (java.io.IOException ioe) {
            log.error(logTag, 0,
                    "Error dumping partition", ioe);
        }
    }
    
protected List<Integer> deleted = new ArrayList<Integer>();
    /**
     * Shut down the indexing stage, dumping any collected data and
     * reporting on our final progress.
     *
     * @param iC The configuration for the index, which can be used to
     * retrieve things like the index directory.
     */
    public void shutdown(IndexConfig iC) {
//        try {
//            dump();
//        } catch (java.io.IOException ioe) {
//            log.error(logTag, 1, "IO exception writing partition");
//        }
    }
    
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        
        //
        // Our main dictionary.
        mainDict = mainDictFactory.getMemoryDictionary(this);
        
        //
        // An occurrences that we can reuse.
        ddo = new DocOccurrence();
        
        //
        // A dictionary for our document keys.
        docDict = docDictFactory.getMemoryDictionary(this);
        
        //
        // Our statistics.
        stats = new PartitionStats();
        
        //
        // Our deleted docs map.
        del = new DelMap();
        del.setPartition(this);
    }
    
} // MemoryPartition
