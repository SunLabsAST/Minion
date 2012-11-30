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

import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.util.Util;
import com.sun.labs.util.NanoWatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class for holding a partition in memory while it is under
 * construction. 
 */
public abstract class MemoryPartition extends Partition {

    private static final Logger logger = Logger.getLogger(MemoryPartition.class.
            getName());

    /**
     * A partition-wide document dictionary, which we'll use to assign doc IDs.
     */
    protected MemoryDictionary<String> docDict;

    /**
     * A deletion map.
     */
    protected DelMap deletions;
    
    private long startIndexTime;
    
    public MemoryPartition() {
    }

    public MemoryPartition(PartitionManager manager, Postings.Type type) {
        this.manager = manager;
        docDict = new MemoryDictionary<String>(new EntryFactory(type));
    }
    
    public void start() {
        startIndexTime = System.currentTimeMillis();
    }

    /**
     * Marshals the data for the current partition so that it can be written to
     * disk for later use.
     *
     * @return The partition number for the dumped partition.
     * @throws java.io.IOException if there is any error writing the partition
     * data to disk
     */
    public PartitionOutput marshal(PartitionOutput partOut) throws java.io.IOException {
        
        //
        // Do nothing if we have no data.
        if(docDict.size() == 0) {
            return null;
        }
        
        NanoWatch mw = new NanoWatch();
        mw.start();
        
        partOut.startPartition(this);
        partOut.setKeys(docDict.getKeys());
        
        long dur = System.currentTimeMillis() - startIndexTime;
        if(logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("Indexing %s %d, %d docs took %s",
                    getPartitionName(),
                    partOut.getPartitionNumber(),
                    docDict.size(), 
                    Util.millisToTimeString(dur)));
        }
        
        PartitionHeader partHeader = partOut.getPartitionHeader();
        partHeader.setProvenance("marshaled");
        partHeader.setPostingsChannelNames(getPostingsChannelNames());
        DictionaryOutput partDictOut = partOut.getPartitionDictionaryOutput();

        //
        // A spot for the partition header offset to be written.
        long phoffsetpos = partDictOut.position();
        partDictOut.byteEncode(0, 8);

        //
        // Dump the document dictionary.
        partHeader.setDocDictOffset(partDictOut.position());
        partOut.setDictionaryRenumber(MemoryDictionary.Renumber.NONE);
        partOut.setDictionaryIDMap(MemoryDictionary.IDMap.NONE);
        partOut.setPostingsIDMap(null);
        partOut.setDictionaryEncoder(new StringNameHandler());
        docDict.marshal(partOut);

        //
        // Set the number of documents.
        partOut.setMaxDocID(maxDocumentID);
        partOut.setNDocs(docDict.size());

        //
        // If we deleted some documents along the way, then dump that data
        // now.
        if(deletions != null && deletions.getNDeleted() > 0) {
            if(logger.isLoggable(Level.FINE)) {
                logger.fine(String.format("Dump %d deleted documents", deletions.getNDeleted()));
            }
            deletions.write(manager.makeDeletedDocsFile(partOut.getPartitionNumber()));
        }

        //
        // Dump any custom data -- to be filled in by subclasses.
        customMarshal(partOut);
        
        //
        // Write the partition header, then return to the top of the file to
        // say where it is.
        long phoffset = partDictOut.position();
        partHeader.write(partDictOut);
        long pos = partDictOut.position();
        partDictOut.position(phoffsetpos);
        partDictOut.byteEncode(phoffset, 8);
        partDictOut.position(pos);
        
        mw.stop();
        if(logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("Marshaled %s %d into %s, %d docs took %s",
                    getPartitionName(),
                    partOut.getPartitionNumber(),
                    partOut.getName(),
                    partOut.getNDocs(), 
                    Util.millisToTimeString(mw.getTimeMillis())));
        }
        return partOut;

    }
    
    public void clear() {
        docDict.clear();
    }

    public MemoryDictionary getDocumentDictionary() {
        return docDict;
    }

    /**
     * Performs any custom data marshaling required by a subclass.  This method
     * exists to be overridden in a subclass and provides no functionality
     * at this level.
     *
     * @param dumpState a state holder for dumping
     * @throws java.io.IOException if there is any error writing the data
     * to disk
     */
    protected abstract void customMarshal(PartitionOutput dumpState) throws java.io.IOException;

    @Override
    public String toString() {
        return "MP: " + getPartitionName();
    }
    
    
} // MemoryPartition

