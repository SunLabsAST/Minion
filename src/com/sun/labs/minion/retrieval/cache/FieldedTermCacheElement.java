/*
 * Copyright 2007-2009 Sun Microsystems, Inc. All Rights Reserved.
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
import com.sun.labs.minion.indexer.postings.FieldedPostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.Util;
import java.util.List;

/**
 * An element of a term cache for terms that were fetched with fields.
 */
public class FieldedTermCacheElement extends TermCacheElement {

    /**
     * The counts for the fields we were generated with.
     */
    private int[][] fieldCounts;

    /**
     * The weights for the fields we were generated with.
     */
    protected float[][] fieldWeights;

    /**
     * Creates a cache element with a given name.
     * @param terms the terms that will be cached in this element.
     * @param feat the features to use when iterating through the postings for
     * terms added to this cache element.
     * @param part the partition from which the term has been selected.
     */
    public FieldedTermCacheElement(List<String> terms,
            PostingsIteratorFeatures feat,
            DiskPartition part) {
        super(terms, feat, part);
    }

    /**
     * Adds a dictionary entry to this cache.
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
            fieldCounts = new int[e.getN()][];
            while(pi.next()) {
                ids[n] = pi.getID();
                counts[n] = pi.getFreq();
                fieldCounts[n] = ((FieldedPostingsIterator) pi).getFieldFreq().clone();
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
        int[][] tfc = new int[n + pi.getN()][];
        int p = 0;
        int np = 0;
        pi.next();
        boolean iterLeft = true;
        while(p < n) {
            int d1 = ids[p];
            int d2 = pi.getID();
            if(d1 < d2) {
                ti[np] = d1;
                tc[np] = counts[p];
                tfc[np] = fieldCounts[p];
                np++; p++;
            } else if(d1 > d2) {
                ti[np] = d2;
                tc[np] = pi.getFreq();
                tfc[np] = ((FieldedPostingsIterator) pi).getFieldFreq().clone();
                np++;
                if(!pi.next()) {
                    iterLeft = false;
                    break;
                }
            } else {
                ti[np] = d1;
                tc[np] = pi.getFreq() + counts[p];
                tfc[np] = fieldCounts[p];
                int[] fc = ((FieldedPostingsIterator) pi).getFieldFreq();
                if(tfc[np].length <= fc.length) {
                    tfc[np] = Util.expandInt(tfc[np], tfc[np].length*2);
                }
                for(int i = 0; i < fc.length; i++) {
                    tfc[np][i] += fc[i];
                }
                np++; p++;

                if(!pi.next()) {
                    iterLeft = false;
                    break;
                }
            }
        }

        if(p < n) {
            System.arraycopy(ids, p, ti, np, n - p);
            System.arraycopy(counts, p, tc, np, n - p);
            System.arraycopy(fieldCounts, p, tfc, np, n - p);
            np += (n - p);
        } else if(iterLeft) {
            while(iterLeft) {
                ti[np] = pi.getID();
                tc[np] = pi.getFreq();
                tfc[np++] = ((FieldedPostingsIterator) pi).getFieldFreq().clone();
                iterLeft = pi.next();
            }
        }

        ids = ti;
        counts = tc;
        fieldCounts = tfc;
        n = np;
    }
    
    /**
     * Computes the weights for the documents in this group, given a
     * weighting function and set of weighting components.
     */
    protected void computeWeights() {
        if(weights == null && ts != null) {
            weights = new float[n];
            fieldWeights = new float[n][];
            wf.initTerm(wc.setTerm(ts));
            for(int i = 0; i < n; i++) {
                wc.fdt = counts[i];
                int dc = 0;
                int[] fc = fieldCounts[i];
                float[] fw = new float[fc.length];
                for(int j = 0; j < fc.length; j++) {
                    if(fc[j] > 0) {
                        wc.fdt = fc[j];
                        dc += fc[j];
                        fw[j] = wf.termWeight(wc);
                    }
                }
                wc.fdt = dc;
                weights[i] = wf.termWeight(wc);
                fieldCounts[i] = fc;
                fieldWeights[i] = fw;
            }
        }
    }

    public PostingsIterator iterator() {
        return new FTCEIterator();
    }

    public class FTCEIterator extends TCEIterator implements FieldedPostingsIterator {

        public FTCEIterator() {
            computeWeights();
        }

        public int[] getFieldFreq() {
            return fieldCounts[p];
        }

        public float[] getFieldWeights() {
            return fieldWeights[p];
        }
        
    }
}