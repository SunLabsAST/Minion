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

package com.sun.labs.minion.indexer.entry;

import com.sun.labs.minion.indexer.postings.Occurrence;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.IOException;

/**
 * An entry for the global term statistics dictionary.
 * These entries will not have any postings.
 */
public class TermStatsQueryEntry extends QueryEntry<String> {
    
    protected TermStatsImpl ts;

    public TermStatsQueryEntry(String name) {
        this.name = name;
        ts = new TermStatsImpl(name);
    }
    
    public TermStatsQueryEntry(String name, ReadableBuffer b) {
        this.name = name;
        ts = new TermStatsImpl(name);
        decode(b);
    }

    @Override
    public void decode(ReadableBuffer b) {
        super.decode(b);
        ts.decode(b);
    }

    @Override
    public void setName(String name) {
        super.setName(name);
        ts.setName(name);
    }
    
    public void setTermStats(TermStatsImpl ts) {
        this.ts = ts;
    }
    
    public void setTermStats(ReadableBuffer b) {
        this.ts.decode(b);
    }
    
    public TermStatsImpl getTermStats() {
        return ts;
    }

    public void add(Occurrence o) {
    }

    public boolean writePostings(PostingsOutput[] out, int[] idMap) throws IOException {
        return true;
    }

    public void append(QueryEntry qe, int start, int[] idMap) {
    }

    @Override
    public int getN() {
        return ts.getDocFreq();
    }

    @Override
    public long getTotalOccurrences() {
        return ts.getTotalOccurrences();
    }

    @Override
    public int getMaxFDT() {
        return ts.getMaxFDT();
    }

    @Override
    public void readPostings() throws IOException {
    }

    @Override
    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        return null;
    }
    
}
