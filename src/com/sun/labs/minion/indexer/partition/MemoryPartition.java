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
import java.io.OutputStream;
import java.io.RandomAccessFile;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
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

    private static Logger logger = Logger.getLogger(MemoryPartition.class.
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

        partNumber = manager.getNextPartitionNumber();

        File[] files = getMainFiles();

        //
        // Get a file for the dictionaries.
        RandomAccessFile dictFile = new RandomAccessFile(files[0], "rw");

        //
        // Get channels for the postings.
        OutputStream[] postStream = new OutputStream[files.length - 1];
        PostingsOutput[] postOut = new PostingsOutput[files.length - 1];
        for(int i = 1; i < files.length; i++) {
            postStream[i - 1] =
                    new BufferedOutputStream(new FileOutputStream(files[i]),
                                             32768);
            postOut[i - 1] = new StreamPostingsOutput(postStream[i - 1]);
        }

        File indexDir = manager.getIndexDir();

        header = new PartitionHeader();

        //
        // A spot for the partition header offset to be written.
        long phoffsetpos = dictFile.getFilePointer();
        dictFile.writeLong(0);

        //
        // Dump the document dictionary.
        header.setDocDictOffset(dictFile.getFilePointer());
        docDict.dump(indexDir, new StringNameHandler(),
                     dictFile, postOut,
                     MemoryDictionary.Renumber.NONE, MemoryDictionary.IDMap.NONE,
                     null);

        //
        // Set the number of documents.
        header.setnDocs(docDict.size());
        header.setMaxDocID(maxDocumentID);

        //
        // Write the partition header to the dictionary file.
        header.write(dictFile);

        //
        // If we deleted some documents along the way, then dump that data
        // now.
        if(deletions != null && deletions.getNDeleted() > 0) {
            logger.fine("Dump deleted documents");
            deletions.write(manager.makeDeletedDocsFile(partNumber));
        }

        //
        // Dump any custom data -- to be filled in by subclasses.
        dumpCustom(indexDir, partNumber, header, dictFile, postOut);

        //
        // Write the partition header, then return to the top of the file to
        // say where it is.
        long phoffset = dictFile.getFilePointer();
        header.write(dictFile);
        long pos = dictFile.getFilePointer();
        dictFile.seek(phoffsetpos);
        dictFile.writeLong(phoffset);
        dictFile.seek(pos);

        dictFile.close();
        for(int i = 0; i < postStream.length; i++) {
            postOut[i].flush();
            postStream[i].close();
        }
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
     * @param partNumber the number of the partition that we're dumping.
     * @param indexDir the directory where we're dumping the data
     * @param ph a header for this partition
     * @param dictFile a file where dictionaries can be dumped
     * @param postOut where postings can be dumped
     * @throws java.io.IOException if there is any error writing the data
     * to disk
     */
    protected abstract void dumpCustom(
            File indexDir,
            int partNumber,
            PartitionHeader ph,
            RandomAccessFile dictFile, PostingsOutput[] postOut) throws java.io.IOException;
} // MemoryPartition

