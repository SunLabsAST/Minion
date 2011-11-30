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

import java.io.IOException;
import com.sun.labs.minion.indexer.postings.Occurrence;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * An entry for the global term statistics dictionary.
 * These entries will not have any postings.
 */
public class TermStatsIndexEntry extends IndexEntry {
    
    private TermStatsImpl termStats;

    public TermStatsIndexEntry(TermStatsQueryEntry qe) {
        super(qe.name, 0, null);
        termStats = new TermStatsImpl(qe.getTermStats());
    }
    
    public TermStatsIndexEntry(String name, int id) {
        super(name, id, null);
        termStats = new TermStatsImpl(name);
    }

    public TermStatsImpl getTermStats() {
        return termStats;
    }

    public void setTermStats(TermStatsImpl termStats) {
        this.termStats = termStats;
    }
    
    @Override
    public void add(Occurrence o) {
    }

    @Override
    public boolean writePostings(PostingsOutput[] out, int[] idMap) throws IOException {
        return true;
    }

    @Override
    public void append(QueryEntry qe, int start, int[] idMap) {
    }

    @Override
    public void encodeEntryInfo(WriteableBuffer b) {
        super.encodeEntryInfo(b);
        termStats.encode(b);
    }
}
