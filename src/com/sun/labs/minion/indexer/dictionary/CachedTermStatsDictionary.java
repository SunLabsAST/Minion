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

/**
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class CachedTermStatsDictionary extends CachedDiskDictionary implements TermStatsDictionary {

    private File df;

    private long closeTime;

    private int refCount;

    public static int BUFFER_SIZE = 8 * 1024;

    public static final String logTag = "UTSD";

    /**
     * Creates a term statistics dictinary
     * @param df the file from which the terms statistics dictionary will
     * be read
     * @throws java.io.IOException if there is any error opening the dictionary
     */
    public CachedTermStatsDictionary(File df) throws java.io.IOException {
        super(TermStatsEntry.class,
                new StringNameHandler(),
                new RandomAccessFile(df, "r"),
                new RandomAccessFile[0],
                DiskDictionary.CHANNEL_FULL_POST,
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

    public void setCloseTime(long closeTime) {
        this.closeTime = closeTime;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public void createRemoveFile() {
        File closeFile = new File(df + ".rem");
        try {
            closeFile.createNewFile();
        } catch(IOException ex) {
            log.error(logTag, 1,
                    "Error creating term stats remove file:" + closeFile);
        }
    }

    public boolean close(long currTime) {
        try {
            //
            // Check if enough time has passed for this close
            // to succeed.
            if(currTime < closeTime || refCount > 0) {
                return false;
            }
            dictFile.close();
        } catch(IOException ex) {
            log.error(logTag, 1, "Error closing term stats dictionary:" + df);
        }
        return true;
    }

    /**
     * Creates an empty term stats dictionary.
     * @param indexDir the directory where the term statistics dictionary
     * should be created
     * @param df the file that should be created for the dictionary
     */
    public static void create(String indexDir, File df) {
        try {
            log.log(logTag, 3, "Making term stats dictionary: " + df);
            RandomAccessFile raf = new RandomAccessFile(df, "rw");
            MemoryDictionary tts =
                    new MemoryDictionary(TermStatsEntry.class);
            tts.dump(indexDir, new StringNameHandler(), raf,
                    new PostingsOutput[0],
                    MemoryDictionary.Renumber.RENUMBER,
                    MemoryDictionary.IDMap.NONE, null);
            raf.close();
        } catch(java.io.IOException ioe) {
            log.error(logTag, 1, "Error creating term stats dictionary", ioe);
        }
    }

    /**
     * Gets an iterator.
     *
     * @param ref if <code>true</code> then we will keep a reference count for
     * this dictionary so that we don't close the file while it's in use.  If
     * <code>false</code> this dictionary may be closed during iteration.
     * @return an iterator for this dictionary
     */
    public DictionaryIterator iterator(boolean ref) {
        if(ref) {
            return iterator();
        }
        return super.iterator();
    }

    /**
     * Gets an iterator and keeps a count of extant references to the dictionary,
     * so that we know when a dictionary is no longer in use.
     */
    public DictionaryIterator iterator() {
        refCount++;
        return super.iterator();
    }

    public void iterationDone() {
        if(refCount > 0) {
            refCount--;
        }
    }
}