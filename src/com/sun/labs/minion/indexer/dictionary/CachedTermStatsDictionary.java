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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import com.sun.labs.minion.indexer.entry.TermStatsEntry;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A term statistics dictionary that will be cached entirely in memory.
 */
public class CachedTermStatsDictionary extends CachedDiskDictionary implements TermStatsDictionary {

    private File df;

    private long closeTime;

    public static int BUFFER_SIZE = 8 * 1024;

    private boolean closed;

    /**
     * Creates a term statistics dictionary
     * @param df the file from which the terms statistics dictionary will
     * be read
     * @throws java.io.IOException if there is any error opening the dictionary
     */
    public CachedTermStatsDictionary(File df) throws java.io.IOException {
        super(TermStatsEntry.class,
                new StringNameHandler(),
                new RandomAccessFile(df, "r"),
                new RandomAccessFile[0],
                DiskDictionary.PostingsInputType.CHANNEL_FULL_POST,
                DiskDictionary.BufferType.NIOFILEBUFFER,
                BUFFER_SIZE,    // Names buffer size
                BUFFER_SIZE,    // Name offset buffer size
                BUFFER_SIZE,    // Info buffer size
                BUFFER_SIZE,    // Info offset buffer size
                null);
        this.df = df;
    }

    public TermStatsEntry getTermStats(String term) {
        return (TermStatsEntry) get(term);
    }

    @Override
    public void setCloseTime(long closeTime) {
        this.closeTime = closeTime;
    }

    @Override
    public long getCloseTime() {
        return closeTime;
    }

    @Override
    public void createRemoveFile() {
        logger.log(Level.INFO, String.format("createRemoveFile"), new Exception("here"));
        File closeFile = new File(df + ".rem");
        try {
            closeFile.createNewFile();
        } catch(IOException ex) {
            logger.severe("Error creating term stats remove file:" + closeFile);
        }
    }

    @Override
    public boolean close(long currTime) {
        try {
            //
            // Check if enough time has passed for this close
            // to succeed.
            if(currTime < closeTime) {
                return false;
            }
            closed = true;
            dictFile.close();
        } catch(IOException ex) {
            logger.severe("Error closing term stats dictionary:" + df);
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
     * Creates an empty term stats dictionary.
     * @param indexDir the directory where the term statistics dictionary
     * should be created
     * @param df the file that should be created for the dictionary
     */
    public static void create(String indexDir, File df) {
        try {
            logger.info("Making term stats dictionary: " + df);
            RandomAccessFile raf = new RandomAccessFile(df, "rw");
            MemoryDictionary tts =
                    new MemoryDictionary(TermStatsEntry.class);
            tts.dump(indexDir, new StringNameHandler(), raf,
                    new PostingsOutput[0],
                    MemoryDictionary.Renumber.RENUMBER,
                    MemoryDictionary.IDMap.NONE, null);
            raf.close();
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error creating term stats dictionary", ioe);
        }
    }

}
