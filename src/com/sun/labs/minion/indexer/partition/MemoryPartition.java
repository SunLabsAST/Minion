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

import com.sun.labs.minion.util.FileLockException;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.indexer.partition.io.RAMPartitionOutput;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.util.StopWatch;
import java.util.logging.Level;
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
    protected PartitionOutput dump(PartitionOutput partOut) throws java.io.IOException {

        //
        // Do nothing if we have no data.
        if(docDict.size() == 0) {
            return null;
        }
        
        StopWatch sw = new StopWatch();
        sw.start();
        
        partOut.startPartition();
        partOut.setKeys(docDict.getKeys());
        
        PartitionHeader partHeader = partOut.getPartitionHeader();
        DictionaryOutput partDictOut = partOut.getPartitionDictionaryOutput();

        //
        // A spot for the partition header offset to be written.
        int phoffsetpos = partDictOut.position();
        partDictOut.byteEncode(0, 8);

        //
        // Dump the document dictionary.
        partHeader.setDocDictOffset(partDictOut.position());
        partOut.setDictionaryRenumber(MemoryDictionary.Renumber.NONE);
        partOut.setDictionaryIDMap(MemoryDictionary.IDMap.NONE);
        partOut.setPostingsIDMap(null);
        partOut.setDictionaryEncoder(new StringNameHandler());
        docDict.dump(partOut);

        //
        // Set the number of documents.
        partOut.setMaxDocID(maxDocumentID);
        partOut.setNDocs(docDict.size());

        //
        // If we deleted some documents along the way, then dump that data
        // now.
        if(deletions != null && deletions.getNDeleted() > 0) {
            logger.fine("Dump deleted documents");
            deletions.write(manager.makeDeletedDocsFile(partNumber));
        }

        //
        // Dump any custom data -- to be filled in by subclasses.
        dumpCustom(partOut);
        
        //
        // Write the partition header, then return to the top of the file to
        // say where it is.
        int phoffset = partDictOut.position();
        partHeader.write(partDictOut);
        int pos = partDictOut.position();
        partDictOut.position(phoffsetpos);
        partDictOut.byteEncode(phoffset, 8);
        partDictOut.position(pos);
        
        sw.stop();
        logger.info(String.format("Dumped %d, %d docs took %dms",
                partOut.getPartitionNumber(),
                docDict.size(), sw.getTime()));

        return partOut;

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
    protected abstract void dumpCustom(PartitionOutput dumpState) throws java.io.IOException;
} // MemoryPartition

