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

import com.sun.labs.minion.indexer.entry.QueryEntry;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import com.sun.labs.minion.indexer.entry.TermStatsEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A term statistics dictionary that will live mostly on disk.
 */
public class UncachedTermStatsDictionary extends DiskDictionary implements
        TermStatsDictionary {

    private File df;

    private long closeTime;

    public static int BUFFER_SIZE = 8 * 1024;

    private boolean closed;

    public UncachedTermStatsDictionary() {
    }

    /**
     * Creates a term statistics dictinary
     * @param df the file from which the terms statistics dictionary will
     * be read
     * @throws java.io.IOException if there is any error opening the dictionary
     */
    public UncachedTermStatsDictionary(File df) throws java.io.IOException {
        super(TermStatsEntry.class,
                new StringNameHandler(), new RandomAccessFile(df, "r"),
                new RandomAccessFile[0],
                DiskDictionary.PostingsInputType.CHANNEL_FULL_POST,
                DiskDictionary.BufferType.NIOFILEBUFFER,
                32768, BUFFER_SIZE,
                BUFFER_SIZE, BUFFER_SIZE, BUFFER_SIZE, null);
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
            logger.severe("Error creating term stats remove file:" + closeFile);
        }
    }

    /**
     * Creates a term stats dictionary from a number of partitions.  This can
     * be used to re-create a term statistics dictionary when things have gone
     * wonky.
     * @param df the file where the term stats dictionary will be written
     * @param parts the partitions whose dictionaries will be included
     */
    public void recalculateTermStats(File df, List<DiskPartition> parts) throws java.io.IOException {
        PriorityQueue<HE> h = new PriorityQueue<HE>();
        for(DiskPartition p : parts) {
            HE el = new HE(p.getMainDictionaryIterator());
            if(el.next()) {
                h.offer(el);
            }
        }

        //
        // A writer for our output dictionary.
        DictionaryWriter tsw = new DictionaryWriter(df.getParent(),
                new StringNameHandler(), null, 0,
                MemoryDictionary.Renumber.RENUMBER);
        int nMerged = 0;
        while(h.size() > 0) {
            HE top = h.peek();
            TermStatsEntry tse = new TermStatsEntry((String) top.curr.getName());
            TermStatsImpl ts = tse.getTermStats();
            while(top != null && top.curr.getName().equals(tse.getName())) {
                top = h.poll();
                ts.add(top.curr);
                if(top.next()) {
                    h.offer(top);
                }
                top = h.peek();
            }
            tsw.write(tse);
            nMerged++;
            if(nMerged % 100000 == 0) {
                Logger.getLogger("").info("Generated " + nMerged);
            }
        }
        if(nMerged % 100000 != 0) {
            Logger.getLogger("").info("Generated " + nMerged);
        }
        RandomAccessFile raf = new RandomAccessFile(df, "rw");
        tsw.finish(raf);
        raf.close();
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
            Logger.getLogger(UncachedTermStatsDictionary.class.getName())
                    .fine("Making term stats dictionary: " + df);
            RandomAccessFile raf = new RandomAccessFile(df, "rw");
            MemoryDictionary tts =
                    new MemoryDictionary(TermStatsEntry.class);
            tts.dump(indexDir, new StringNameHandler(), raf,
                    new PostingsOutput[0],
                    MemoryDictionary.Renumber.RENUMBER,
                    MemoryDictionary.IDMap.NONE, null);
            raf.close();
        } catch(java.io.IOException ioe) {
            Logger.getLogger(UncachedTermStatsDictionary.class.getName())
                    .log(Level.SEVERE, "Error creating term stats dictionary", ioe);
        }
    }

    /**
     * An inner class to use when building a new term stats dictionary out of a 
     * number of partitions.
     */
    private class HE implements Comparable<HE> {

        DictionaryIterator di;

        QueryEntry curr;

        public HE(DictionaryIterator di) {
            this.di = di;
        }

        public boolean next() {
            if(di.hasNext()) {
                curr = di.next();
                return true;
            }
            return false;
        }

        public int compareTo(HE o) {
            return ((Comparable) curr.getName()).compareTo(o.curr.getName());
        }
    }
}
