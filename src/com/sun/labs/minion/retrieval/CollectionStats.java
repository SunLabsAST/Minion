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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.entry.TermStatsEntry;

import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.partition.PartitionStats;

/**
 * A container for collection level statistics that are coelesced out of
 * the statistics associated with a number of partitons.
 */
public class CollectionStats {
    
    /**
     * A partition manager that will allow us to fetch collection-wide term
     * statistics.
     */
    protected PartitionManager pm;
    
    protected DictionaryIterator di;
    
    /**
     * A local cache of term stats.
     */
    protected Map<String,TermStatsImpl> termStats;
    
    /**
     * The total number of documents in the collection.
     */
    protected int nDocs;
    
    /**
     * The number of tokens in the collection.
     */
    protected long nTokens;
    
    /**
     * The maximum term frequency in the collection.  For all terms
     * <em>t</em> in the collection and all documents <em>d</em> in
     * the partition, this is the maximum value of
     * f<sub><em>d,t</em></sub>, the frequency of term <em>t</em> in
     * document <em>d</em>.
     */
    public int maxfdt;
    
    /**
     * The maximum document frequency in the collection.  This is given by
     * the term that has the largest number of documents associated with
     * it, across all dictionaries in the collection.  This will most
     * likely be an underestimate, as it most likely will not take into
     * account the fact that the same term occurs in more than one
     * partition!
     */
    public int maxft;
    
    /**
     * The number of distinct terms in the collection.  This is very likely
     * an overestimate, as many terms will be shared in the various
     * partitions' main dictionaries.
     */
    public int nd;
    
    /**
     * The average document length in the collection, in words.
     */
    public float avgDocLen;
    
    public CollectionStats(PartitionManager pm) {
        this.pm = pm;
        termStats = new HashMap<String,TermStatsImpl>();
        for(Iterator i = pm.getActivePartitions().iterator(); i.hasNext();) {
            Partition p = (Partition) i.next();
            nDocs            += p.getNDocs();
            PartitionStats s  = p.getStats();
            nTokens          += s.nTokens;
            maxfdt            = Math.max(maxfdt, s.maxfdt);
            maxft             = Math.max(maxft, s.maxft);
            nd               += s.nd;
        }
        avgDocLen = (float) nTokens / nDocs;
        di = pm.getTermStatsDict().iterator(false);
    }
    
    /**
     * Gets the collection-wide statistics for a given term name.
     */
    public synchronized TermStatsImpl getTermStats(String s) {
        TermStatsEntry tse = (TermStatsEntry) di.get(s);
        if(tse == null) {
            return null;
        }
        return tse.getTermStats();
    }
    
    public int getNDocs() {
        return nDocs;
    }
    
    public long getNTokens() {
        return nTokens;
    }
    
    public float getAvgerageDocumentLength() {
        return avgDocLen;
    }
    
    public int getNDistinct() {
        return nd;
    }
    
    void setTermStats(String term, TermStatsImpl ts) {
        termStats.put(term, ts);
    }
    
    public String toString() {
        return di.toString();
    }
} // CollectionStats
