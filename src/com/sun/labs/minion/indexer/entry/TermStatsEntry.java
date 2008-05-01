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
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.retrieval.TermStats;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * An entry for the global term statistics dictionary.  These entries will not have any postings.
 * @author Stephen Green <stephen.green@sun.com>
 */
public class TermStatsEntry extends BaseEntry {
    
    private TermStats ts;
    
    public TermStatsEntry() {
        super();
        ts = new TermStats(null);
    }
    
    public TermStatsEntry(String name) {
        super(name);
        ts = new TermStats(name);
    }
    
    public void setTermStats(TermStats ts) {
        this.ts = ts;
    }
    
    public TermStats getTermStats() {
        return ts;
    }

    public void add(Occurrence o) {
    }

    public boolean writePostings(PostingsOutput[] out, int[] idMap) throws IOException {
        return true;
    }

    public void append(QueryEntry qe, int start, int[] idMap) {
    }

    public void encodePostingsInfo(WriteableBuffer b) {
        b.byteEncode(ts.getDocFreq());
        b.byteEncode(ts.getTotalOccurrences());
        b.byteEncode(ts.getMaxFDT());
    }

    public Entry getEntry(Object name) {
        return new TermStatsEntry((String) name);
    }

    public Entry getEntry() {
        TermStatsEntry ret = (TermStatsEntry) getEntry(name);
        ret.id = id;
        ret.ts = ts;
        return ret;
    }

    public int getN() {
        return ts.getDocFreq();
    }

    public int getTotalOccurrences() {
        return ts.getTotalOccurrences();
    }

    public int getMaxFDT() {
        return ts.getMaxFDT();
    }

    public int getNumChannels() {
        return 0;
    }

    public void decodePostingsInfo(ReadableBuffer b, int pos) {
        ts = new TermStats((String) name);
        b.position(pos);
        ts.setDocFreq(b.byteDecode());
        ts.setTotalOccurrences(b.byteDecode());
        ts.setMaxFDT(b.byteDecode());
    }

    public void readPostings() throws IOException {
    }

    public boolean hasPositionInformation() {
        return false;
    }

    public boolean hasFieldInformation() {
        return false;
    }

    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        return null;
    }
    
}
