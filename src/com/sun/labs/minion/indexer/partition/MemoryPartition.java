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
import java.io.RandomAccessFile;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.util.StopWatch;
import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger(MemoryPartition.class.
            getName());

    /**
     * A partition-wide document dictionary, which we'll use to assign doc IDs.
     */
    protected MemoryDictionary docDict;

    /**
     * A deletion map.
     */
    protected DelMap deletions;
    
    public MemoryPartition() {
        
    }

    public MemoryPartition(PartitionManager manager, Postings.Type type) {
        this.manager = manager;
        docDict = new MemoryDictionary<String>(new EntryFactory(type));
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
        
        StopWatch sw = new StopWatch();
        sw.start();
        
        File[] files = getMainFiles();

        DumpState dumpState = new DumpState(manager, files);

        //
        // Get a file for the dictionaries.
        RandomAccessFile dictFile = new RandomAccessFile(files[0], "rw");

        dumpState.partHeader = new PartitionHeader();

        //
        // A spot for the partition header offset to be written.
        long phoffsetpos = dictFile.getFilePointer();
        dictFile.writeLong(0);

        //
        // Dump the document dictionary.
        dumpState.partHeader.setDocDictOffset(dictFile.getFilePointer());
        dumpState.renumber = MemoryDictionary.Renumber.NONE;
        dumpState.idMap = MemoryDictionary.IDMap.NONE;
        dumpState.postIDMap = null;
        docDict.dump(dumpState);

        //
        // Set the number of documents.
        dumpState.partHeader.setnDocs(docDict.size());
        dumpState.partHeader.setMaxDocID(maxDocumentID);

        //
        // If we deleted some documents along the way, then dump that data
        // now.
        if(deletions != null && deletions.getNDeleted() > 0) {
            logger.fine("Dump deleted documents");
            deletions.write(manager.makeDeletedDocsFile(partNumber));
        }

        //
        // Dump any custom data -- to be filled in by subclasses.
        dumpCustom(dumpState);
        
        //
        // Flush the dictionary output to our dictionary file.
        dumpState.fieldDictOut.flush(dictFile);

        //
        // Write the partition header, then return to the top of the file to
        // say where it is.
        long phoffset = dictFile.getFilePointer();
        dumpState.partHeader.write(dictFile);
        long pos = dictFile.getFilePointer();
        dictFile.seek(phoffsetpos);
        dictFile.writeLong(phoffset);
        dictFile.seek(pos);

        dictFile.close();
        
        dumpState.close();
        sw.stop();
        logger.info(String.format("Dumped %d, %d docs took %dms", partNumber, 
                docDict.size(), sw.getTime()));

        manager.addNewPartition(partNumber, docDict.getKeys());
        return partNumber;
    }

    public MemoryDictionary getDocumentDictionary() {
        return docDict;
    }

    /**
     * Performs any custom data dump required in a subclass.  This method
     * exists to be overridden in a subclass and provides no functionality
     * at this level.
     *
     * @param dumpState a state holder for dumping
     * @throws java.io.IOException if there is any error writing the data
     * to disk
     */
    protected abstract void dumpCustom(DumpState dumpState) throws java.io.IOException;
} // MemoryPartition

