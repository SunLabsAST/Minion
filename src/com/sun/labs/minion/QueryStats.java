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
package com.sun.labs.minion;

import com.sun.labs.minion.util.NanoWatch;
import java.io.Serializable;

/**
 *  A class to hold statistics generated during a single or many queries.
 */
public class QueryStats implements Serializable {

    /**
     * The number of main dictionary lookups done during the query.
     */
    public int dictLookups;
    
    /**
     * The number of main dictionary cache hits during the query.
     */
    public int dictCacheHits;
    
    /**
     * The number of main dictionary cache misses during the query.
     */
    public int dictCacheMisses;

    /**
     * The number of term cache hits.
     */
    public int termCacheHits;

    /**
     * The number of term cache misses.
     */
    public int termCacheMisses;
    
    /**
     * The total size (in bytes) of postings read during the query.
     */
    public long postingsSize;

    /**
     * A stopwatch that accumulates the total query time.
     */
    public NanoWatch queryW = new NanoWatch();
    
    /**
     * A stopwatch that accumulates the time required for dictionary lookups
     * during the query.
     */
    public NanoWatch dictLookupW = new NanoWatch();

    /**
     * A stopwatch that accumulates the time spent getting entries for the
     * term cache.
     */
    public NanoWatch termCacheW = new NanoWatch();
    
    /**
     * A stopwatch that accumulates the time required for reading postings
     * during the query.
     */
    public NanoWatch postReadW = new NanoWatch();
    /**
     * A stopwatch that accumulates the time required for unioning postings
     * during the query.  This will accumulate the time for single term queries.
     */
    public NanoWatch unionW = new NanoWatch();

    /**
     * A stopwatch that accumulates the time required for intersecting postings
     * during the query.
     */
    public NanoWatch intersectW = new NanoWatch();

    /**
     * A stopwatch that accumulates the time spent iterating through postings,
     * no matter what the reason.
     */
    public NanoWatch piW = new NanoWatch();

    /**
     * A stopwatch that accumulates the time spent sorting postings.
     */
    public NanoWatch postSortW = new NanoWatch();

    /**
     * A stopwatch that accumulates the time spent combining document scores
     * when generating results.
     */
    public NanoWatch docCombW = new NanoWatch();

    /**
     * A stopwatch that accumulates the time spent normalizing document scores.
     */
    public NanoWatch normW = new NanoWatch();

    /**
     * Accumulates one set of query statistics into this one, so that we can
     * gather stats across a number of queries.
     * @param qs a set of query statistics to add to this one.
     */
    public void accumulate(QueryStats qs) {
       dictLookups += qs.dictLookups;
       dictCacheHits += qs.dictCacheHits;
       dictCacheMisses += qs.dictCacheMisses;
       postingsSize += qs.postingsSize;
       termCacheHits += qs.termCacheHits;
       termCacheMisses += qs.termCacheMisses;
       queryW.accumulate(qs.queryW);
       dictLookupW.accumulate(qs.dictLookupW);
       termCacheW.accumulate(qs.termCacheW);
       postReadW.accumulate(qs.postReadW);
       unionW.accumulate(qs.unionW);
       intersectW.accumulate(qs.intersectW);
       piW.accumulate(qs.piW);
       postSortW.accumulate(qs.postSortW);
       docCombW.accumulate(qs.docCombW);
       normW.accumulate(qs.normW);
    }

    public String dump() {
        double tqt = queryW.getTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d queries in %.2fms, %.2fms per query\n",
                queryW.getClicks(),
                queryW.getTimeMillis(),
                queryW.getAvgTimeMillis()));
        sb.append("Dictionary activity:\n");
        sb.append(String.format(" %d lookups in %.2fms (%.2f%% of total), %.2fms per lookup\n",
                dictLookupW.getClicks(),
                dictLookupW.getTimeMillis(),
                dictLookupW.getTimeMillis() * 100 / tqt,
                dictLookupW.getAvgTimeMillis()));
        sb.append(String.format(" %-30s %10d\n", "Cache Hits:", dictCacheHits));
        sb.append(String.format(" %-30s %10d\n", "Cache Misses:", dictCacheMisses));
        sb.append("Postings Activity\n");
        sb.append(String.format(" %d postings lists read in %.2fms (%.2f%% of total), %.2fms per read\n",
                postReadW.getClicks(),
                postReadW.getTimeMillis(),
                postReadW.getTimeMillis() * 100 / tqt,
                postReadW.getAvgTimeMillis()));
        sb.append(String.format(" %-30s %10.1fKB\n", "Average Postings size:",
                postingsSize / (double) postReadW.getClicks() / 1024));
        sb.append(String.format(" %-30s %10d\n", "Term Cache Hits:", termCacheHits));
        sb.append(String.format(" %-30s %10d\n", "Term Cache Misses:", termCacheMisses));
        sb.append(String.format(" %-30s %10.1fms (%.2f%% of total)\n", "Generating term cache entries:",
                termCacheW.getTimeMillis(),
                termCacheW.getTimeMillis() * 100 / tqt));
        sb.append(String.format(" %-30s %10.1fms (%.2f%% of total)\n", "Postings iteration:",
                piW.getTimeMillis(),
                piW.getTimeMillis() * 100 / tqt));
        sb.append("Query Activity\n");
        sb.append(String.format(" %-30s %10.1fms (%.2f%% of total)\n", "Union processing:",
                unionW.getTimeMillis(),
                unionW.getTimeMillis() * 100 / tqt));
        sb.append(String.format(" %-30s %10.1fms (%.2f%% of total)\n", "Intersect processing:",
                intersectW.getTimeMillis(),
                intersectW.getTimeMillis() * 100 / tqt));
        sb.append(String.format(" %-30s %10.1fms (%.2f%% of total)\n", "Sorting postings:",
                postSortW.getTimeMillis(),
                postSortW.getTimeMillis() * 100 / tqt));
        sb.append(String.format(" %-30s %10.1fms (%.2f%% of total)\n",
                "Combining document scores:",
                docCombW.getTimeMillis(),
                docCombW.getTimeMillis() * 100 / tqt));
        sb.append(String.format(" %-30s %10.1fms (%.2f%% of total)\n", "Normalizing scores:",
                normW.getTimeMillis(),
                normW.getTimeMillis() * 100 / tqt));
        return sb.toString();
        
    }
}
