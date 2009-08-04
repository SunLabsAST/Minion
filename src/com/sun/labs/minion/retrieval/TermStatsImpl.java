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

package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.TermStats;
import java.util.Iterator;
import java.util.List;

import com.sun.labs.minion.indexer.entry.Entry;

import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.partition.DiskPartition;

/**
 * A class that holds collection-wide statistics for a term.
 */
public class TermStatsImpl implements TermStats, Comparable<TermStatsImpl> {
    
    /**
     * The name of the term for which we're collecting stats.
     */
    protected String name;
    
    /**
     * The maximum frequency for the term.
     */
    protected int maxfdt;
    
    /**
     * The number of documents in which the term occurs.
     */
    protected int ft;
    
    /**
     * The total number of occurrences for the term.
     */
    protected long Ft;
    
    /**
     * Creates an empty set of term stats to which we can add terms.
     */
    public TermStatsImpl(String name) {
        this.name = name;
    }
    
    /**
     * Builds the collection wide statistics from the partitions in a
     * partition manager.
     *
     * @param name the name of the term we're building stats for
     * @param m a partition manager for the collection
     */
    public TermStatsImpl(String name, PartitionManager m) {
        this.name = name;
        init(m.getActivePartitions().iterator());
    } // TermStats constructor
    
    /**
     * Builds the collection wide statistics from the partitions in a
     * partition manager.
     *
     * @param name the name of the term we're building stats for
     * @param l a list of partitions for which we want to build the stats
     */
    public TermStatsImpl(String name, List l) {
        this.name = name;
        init(l.iterator());
    } // TermStats constructor
    
    /**
     * Initializes the stats from an iterator for a list of partitions.
     *
     * @param i the iterator
     */
    protected void init(Iterator<DiskPartition> i) {
        while(i.hasNext()) {
            DiskPartition p = i.next();
            if(p.isClosed()) {
                continue;
            }
            Entry e = p.getTerm(name);
            if(e != null) {
                add(e);
            }
        }
    }
    
    /**
     * Adds the stats for a term to these term stats.
     */
    public void add(Entry e) {
        ft += e.getN();
        Ft += e.getTotalOccurrences();
        maxfdt = Math.max(maxfdt, e.getMaxFDT());
    }
    
    /**
     * Combines another set of term statistics with this one.
     */
    public void add(TermStatsImpl ts) {
        ft += ts.ft;
        Ft += ts.Ft;
        maxfdt = Math.max(maxfdt, ts.maxfdt);
    }
    
    public void add(int ft, int Ft) {
        this.ft += ft;
        this.Ft += Ft;
    }
    
    /**
     * Gets the name of the term whose stats we're holding.
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the number of documents in which the term occurs.
     *
     * @return the number of documents in the collection that contain the term.
     */
    public int getDocFreq() {
        return ft;
    }
    
    public void setDocFreq(int ft) {
        this.ft = ft;
    }
    
    /**
     * Gets the total number of occurrences of the term in the collection.
     *
     * @return the total number of occurrences of the term in all documents
     * in the collection.
     */
    public int getTotalOccurrences() {
        return (int) Math.min(Ft, Integer.MAX_VALUE);
    }
    
    public void setTotalOccurrences(int Ft) {
        this.Ft = Ft;
    }
    
    public int getMaxFDT() {
        return maxfdt;
    }
    
    public void setMaxFDT(int maxfdt) {
        this.maxfdt = maxfdt;
    }
    
    public String toString() {
        return String.format("name: %s doc freq: %d total freq: %d max term freq: %d", name, ft, Ft, maxfdt);
    }

    public boolean equals(TermStatsImpl tsi) {
        return name.equals(tsi.name) &&
                ft == tsi.ft &&
                Ft == tsi.Ft &&
                maxfdt == tsi.maxfdt;
    }

    public int compareTo(TermStatsImpl o) {
       return name.compareTo(o.name);
    }
    
} // TermStats
