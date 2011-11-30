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

import com.sun.labs.minion.indexer.entry.Entry;

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

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
    protected int maxfdt = 1;
    
    /**
     * The number of documents in which the term occurs.
     */
    protected int ft =1;
    
    /**
     * The total number of occurrences for the term.
     */
    protected long Ft = 1;
    
    /**
     * Creates an empty set of term stats to which we can add terms.
     * @param name the name of the term
     */
    public TermStatsImpl(String name) {
        this.name = name;
    }

    public TermStatsImpl(String name, ReadableBuffer b) {
        this.name = name;
        decode(b);
    }
    
    public void clear() {
        maxfdt = 0;
        ft = 0;
        Ft = 0;
    }

    public TermStatsImpl(TermStatsImpl tsi) {
        name = tsi.name;
        maxfdt = tsi.maxfdt;
        ft = tsi.ft;
        Ft = tsi.Ft;
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

    public void encode(WriteableBuffer b) {
        b.byteEncode(getDocFreq());
        b.byteEncode(getTotalOccurrences());
        b.byteEncode(getMaxFDT());
    }

    public void decode(ReadableBuffer b) {
        ft = b.byteDecode();
        Ft = b.byteDecode();
        maxfdt = b.byteDecode();
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
