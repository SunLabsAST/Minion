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

package com.sun.labs.minion.retrieval.cache;

import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.indexer.entry.QueryEntry;

import com.sun.labs.minion.indexer.partition.DiskPartition;

import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.Util;

/**
 * An element in a term cache.  Stores the weighted postings for a single
 * term in a single partition.
 */
public class TermCacheElement {
    
    protected String name;
    protected DiskPartition part;
    protected int[] ids;
    protected int[] counts;
    protected float[] weights;
    int n;
    protected TermStatsImpl ts;
    protected static MinionLog log = MinionLog.getLog();
    protected static String logTag = "TCE";
    
    /**
     * Creates a term counter with a given name for a given partition.
     */
    public TermCacheElement(String name, DiskPartition part) {
        this.name = name;
        this.part = part;
    }
    
    /**
     * Adds the counts for the given term from this partition
     * to the counts that we're collecting.
     *
     * @param term the term whose counts we wish to collect.
     */
    public void add(String term) {
        QueryEntry e = part.getTerm(term);
        if(e == null) {
            return;
        }
        
        PostingsIterator pi = e.iterator(null);
        if(pi == null) {
            return;
        }
        
        if(ids == null) {
            ids = new int[e.getN()];
            counts = new int[e.getN()];
            while(pi.next()) {
                ids[n] = pi.getID();
                counts[n++] = pi.getFreq();
            }
        } else {
            merge(pi);
        }
    }
    
    private void merge(PostingsIterator pi) {
        int[] ti = new int[n + pi.getN()];
        int[] tc = new int[n + pi.getN()];
        int p = 0;
        int np = 0;
        pi.next();
        boolean iterLeft = true;
        while(p < n) {
            int d1 = ids[p];
            int d2 = pi.getID();
            if(d1 < d2) {
                ti[np] = d1;
                tc[np++] = counts[p++];
            } else if(d1 > d2) {
                ti[np] = d2;
                tc[np++] = pi.getFreq();
                if(!pi.next()) {
                    iterLeft = false;
                    break;
                }
            } else {
                ti[np] = d1;
                tc[np++] = pi.getFreq() + counts[p++];
                if(!pi.next()) {
                    iterLeft = false;
                    break;
                }
            }
        }
        
        if(p < n) {
            System.arraycopy(ids, p, ti, np, n - p);
            System.arraycopy(counts, p, tc, np, n - p);
            np += (n - p);
        } else if(iterLeft) {
            ti[np] = pi.getID();
            tc[np++] = pi.getFreq();
            pi.next();
        }
        
        ids = ti;
        counts = tc;
        n = np;
    }
    
    /**
     * Adds the counts for the given term from this partition
     * to the counts that we're collecting.
     *
     * @param feat the term whose counts we wish to collect.
     */
    public void add(WeightedFeature feat) {
        add(feat.getName());
    }
    
    /**
     * Gets the term statistics associated with the counts that we've collected.
     *
     * @return the computed term statistics
     */
    public TermStatsImpl getTermStats() {
        if(ts == null) {
            ts = new TermStatsImpl(name);
            if(counts == null) {
                return ts;
            }
            for(int i = 0; i < counts.length; i++) {
                int c = counts[i];
                if(c > 0) {
                    ts.add(1, c);
                    ts.setMaxFDT(Math.max(c, ts.getMaxFDT()));
                }
            }
        }
        return ts;
    }
    
    /**
     * Computes the weights for the documents in this group, given a
     * weighting function and set of weighting components.  We make the
     * assumption here that cross-partition term stats have been collected
     * (most likely from a number of <code>TermCounter</code>s) and used
     * to initialize the term level weights in the weighting components.
     */
    public float[] computeWeights(
            WeightingComponents wc,
            WeightingFunction wf) {
        if(weights == null) {
            weights = new float[n];
            wf.initTerm(wc.setTerm(getTermStats()));
            for(int i = 0; i < n; i++) {
                wc.fdt = counts[i];
                weights[i] = wf.termWeight(wc);
            }
        }

        return weights;
    }
    
    public PostingsIterator iterator(
            WeightingComponents wc,
            WeightingFunction wf) {
        return new TCEIterator(wc, wf);
    }
    
    /**
     * An iterator for this element of the term cache.
     */
    public class TCEIterator implements PostingsIterator {

        int p;
        public TCEIterator(
            WeightingComponents wc,
            WeightingFunction wf) {
            computeWeights(wc, wf);
            p = -1;
        }
        
        public PostingsIteratorFeatures getFeatures() {
            return null;
        }
        
        public int getN() {
            return n;
        }

        public int get(int[] ids) {
            int ret = Math.min(ids.length, n - p);
            System.arraycopy(TermCacheElement.this.ids, p, ids, 0, ret);
            p += ret;
            return ret;
        }

        public int get(int[] ids, int[] freq) {
            throw new UnsupportedOperationException("Not supported yet.");
       }

        public int get(int[] ids, float[] weights) {
            throw new UnsupportedOperationException("Not supported yet.");
       }

        public boolean next() {
            p++;
            return p < n;
        }

        public boolean findID(int id) {
            int start = 0; // p > 0 && id > ids[p] ? p : 0;
            int pos = Util.binarySearch(ids, start, n-1, id);
                    
            if(pos > 0) {
                p = pos;
                return true;
            } else {
                p = -pos - 1;
                return false;
            }
        }

        public void reset() {
            p = -1;
        }

        public int getID() {
            return ids[p];
        }

        public float getWeight() {
            return weights[p];
        }

        public int getFreq() {
            return counts[p];
        }

        public int compareTo(Object o) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
    
} // TermCacheElement
