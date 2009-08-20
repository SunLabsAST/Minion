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

import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.indexer.entry.QueryEntry;

import com.sun.labs.minion.indexer.partition.DiskPartition;

import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.retrieval.ScoredGroup;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.Util;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * An element in a term cache.  Stores the weighted postings for a single
 * term in a single partition.
 */
public class TermCacheElement {

    protected List<String> terms;
    
    protected DiskPartition part;

    protected PostingsIteratorFeatures feat;

    protected int[] ids;

    protected int[] counts;

    protected float[] weights;

    protected int n;

    protected TermStatsImpl ts;

    private float sqw;

    WeightingFunction wf;

    WeightingComponents wc;

    /**
     * Creates a cache element.
     *
     * @param terms the terms to cache in this element.
     * @param feat the features that we'll use when adding terms to this cache element.
     * @param part the partition from which the term has been selected.
     */
    protected TermCacheElement(List<String> terms, PostingsIteratorFeatures feat, DiskPartition part) {
        this.terms = new ArrayList<String>(terms);
        this.feat = feat;
        this.part = part;
        wf = feat.getWeightingFunction();
        wc = feat.getWeightingComponents();
        for(String term : terms) {
            add(part.getTerm(term));
        }
    }

    protected PostingsIterator preAdd(QueryEntry e) {
        if(e == null) {
            return null;
        }

        TermStatsImpl ets = part.getManager().getTermStats(
                e.getName().toString());
        wc.setTerm(ets);
        float qw = wf.initTerm(wc);
        sqw += qw * qw;
        if(ts == null) {
            ts = ets;
        } else {
            ts.add(ets);
        }

        return e.iterator(feat);
    }

    public TermStatsImpl getTermStats() {
        return ts;
    }

    public float getQueryWeight() {
        return sqw;
    }
    
    /**
     * Adds a dictionary entry to this cache element.
     * 
     * @param e the entry to add.
     */
    protected void add(QueryEntry e) {
        PostingsIterator pi = preAdd(e);
        if(pi == null) {
            return;
        }
        
        QueryStats qs = feat == null ? null : feat.getQueryStats();

        if(qs != null) {
            qs.termCacheW.start();
        }

        if(ids == null) {
            ids = new int[e.getN()];
            counts = new int[e.getN()];
            while(pi.next()) {
                ids[n] = pi.getID();
                counts[n] = pi.getFreq();
                n++;
            }
        } else {
            merge(pi);
        }

        if(qs != null) {
            qs.termCacheW.stop();
        }
    }

    protected void merge(PostingsIterator pi) {
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
            while(iterLeft) {
                ti[np] = pi.getID();
                tc[np++] = pi.getFreq();
                iterLeft = pi.next();
            }
        }

        ids = ti;
        counts = tc;
        n = np;
    }

    public String getName() {
        return terms.get(0);
    }

    /**
     * Computes the weights for the documents in this group, given a
     * weighting function and set of weighting components.
     */
    protected void computeWeights() {
        if(weights == null) {
            weights = new float[n];
            WeightingComponents wc = feat.getWeightingComponents();
            WeightingFunction wf = feat.getWeightingFunction();
            sqw = wf.initTerm(wc.setTerm(ts));
            for(int i = 0; i < n; i++) {
                wc.fdt = counts[i];
                weights[i] = wf.termWeight(wc);
            }
        }
    }

    public ScoredGroup getGroup() {
        computeWeights();
        ScoredGroup ret = new ScoredGroup(part, ids.clone(), weights.clone(), n);
        ret.setQueryWeight(sqw);
        return ret;
    }

    public PostingsIterator iterator() {
        return new TCEIterator();
    }

    /**
     * An iterator for this element of the term cache.
     */
    public class TCEIterator implements PostingsIterator {

        int p;

        public TCEIterator() {
            computeWeights();
            p = -1;
        }

        public PostingsIteratorFeatures getFeatures() {
            return feat;
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
            int pos = Util.binarySearch(ids, start, n - 1, id);

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
