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

import java.io.RandomAccessFile;

import com.sun.labs.minion.indexer.entry.IndexEntry;

import java.util.logging.Logger;

/**
 * A class that holds a variety of statistics for a partition.  The
 * statistics collected are useful for calculating term weighting
 * information.
 */
public class PartitionStats {

    Logger logger = Logger.getLogger(getClass().getName());

    protected static String logTag = "PS";

    /**
     * The file containing the stats.
     */
    public RandomAccessFile raf;

    /**
     * The number of documents in the partition.
     */
    public int nDocs;

    /**
     * The number of tokens in the partition.
     */
    public long nTokens;

    /**
     * The maximum term frequency in the partition.  For all terms
     * <em>t</em> in the main dictionary and all documents <em>d</em> in
     * the partition, this is the maximum value of
     * f<sub><em>d,t</em></sub>, the frequency of term <em>t</em> in
     * document <em>d</em>.
     */
    public int maxfdt;

    /**
     * The maximum document frequency in the partition.  This is given by
     * the entry in the main dictionary that has the largest number of
     * documents associated with it.
     */
    public int maxft;

    /**
     * The number of distinct terms in the partition.
     */
    public int nd;

    /**
     * The average document length, in words.
     */
    public float avgDocLen;

    /**
     * Creates a set of partition statistics that can be used during
     * indexing.
     */
    public PartitionStats() {
    }

    /**
     * Processes an entry from a dictionary, modifying the statistics as
     * necessary.  This method should be called when dumping or merging the
     * main dictionary entries in a partition.
     */
    public void processEntry(IndexEntry e) {
        maxfdt = Math.max(e.getMaxFDT(), maxfdt);
        maxft = Math.max(e.getN(), maxft);
        nd++;
    }

    /**
     * Creates a set of partition statistics by reading them from the
     * provided channel.
     */
    public PartitionStats(RandomAccessFile raf)
            throws java.io.IOException {

        //
        // Read the data from the file.
        this.raf = raf;
        nDocs = raf.readInt();
        nTokens = raf.readLong();
        maxfdt = raf.readInt();
        maxft = raf.readInt();
        nd = raf.readInt();

        //
        // Derived info.
        avgDocLen = (float) nTokens / (float) nDocs;
    }

    /**
     * Writes the stats to a file.
     *
     * @param raf the file to which we're going to write the stats.
     */
    protected void write(RandomAccessFile raf)
            throws java.io.IOException {
        raf.writeInt(nDocs);
        raf.writeLong(nTokens);
        raf.writeInt(maxfdt);
        raf.writeInt(maxft);
        raf.writeInt(nd);
    }

    public String toString() {
        return nDocs + " " + nTokens + " " + maxfdt + " " + maxft + " " + nd;
    }
} // PartitionStats
