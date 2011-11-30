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

import com.sun.labs.minion.indexer.partition.DiskPartition;

import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.partition.PartitionStats;
import java.util.Collection;

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
        this(pm, pm.getActivePartitions());
    }

    public CollectionStats(PartitionManager pm, Collection<DiskPartition> parts) {
        this.pm = pm;
        for(DiskPartition p : parts) {
            nDocs            += p.getNDocs();
            PartitionStats s  = p.getStats();
            nTokens          += s.nTokens;
            maxfdt            = Math.max(maxfdt, s.maxfdt);
            maxft             = Math.max(maxft, s.maxft);
            nd               += s.nd;
        }
        avgDocLen = (float) nTokens / nDocs;
    }
    
    /**
     * Gets the collection-wide statistics for a given term name.
     * @param term the term for which we want statistics.
     * @return the term statistics for the term, if it is found in the collection,
     * otherwise return <code>null</code>.
     */
    public TermStatsImpl getTermStats(String term) {
        return pm.getTermStats(term);
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
    
} // CollectionStats
