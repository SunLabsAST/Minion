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
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.util.Util;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A class that will allow us to quickly or together a number of terms.
 * The class will attempt to optimize for a number of conditions, including
 * the case where the resulting document set can be stored in a single
 * array.
 */
public class ScoredQuickOr extends QuickOr {

    /**
     * The weights associated with the documents.
     */
    float[] weights;

    /**
     * The sum of the squared query weights
     */
    float sqw = 0;

    protected static final Logger logger = Logger.getLogger(ScoredQuickOr.class.getName());

    /**
     * Creates a quick or for a given partition.  The estimated size of the
     * result set is used for our initial calculation.
     */
    public ScoredQuickOr(DiskPartition part, int estSize) {
        this(part, estSize, false);
    }

    public ScoredQuickOr(DiskPartition part, int estSize, boolean shouldStoreAll) {
        super(part, estSize, shouldStoreAll);
        if(storeAll) {
            weights = new float[docs.length];
        } else {
            weights = new float[estSize];
        }
    } // ScoredQuickOr constructor

    @Override
    public void add(PostingsIterator pi) {
        add(pi, 1);
    }
    
    @Override
    public void add(PostingsIterator pi, float qw) {
        if(pi == null) {
            return;
        }

        added++;
        sqw += (qw * qw);

        qs.piW.start();
        qs.unionW.start();
        if(storeAll) {
            while(pi.next()) {
                weights[pi.getID()] += pi.getWeight() * qw;
            }
        } else {
            int s = pi.getN() + p;
            if(s >= docs.length) {
                docs = Arrays.copyOf(docs, s * 2);
                weights = Arrays.copyOf(weights, s * 2);
            }

            while(pi.next()) {
                docs[p] = pi.getID();
                weights[p++] = pi.getWeight() * qw;
            }
        }
        
        qs.piW.stop();
        qs.unionW.stop();
    }

    public void add(PostingsIterator pi, float dw, float qw) {
        if(pi == null) {
            return;
        }

        added++;
        sqw += (qw * qw);

        qs.piW.start();
        qs.unionW.start();
        if(storeAll) {
            while(pi.next()) {
                weights[pi.getID()] += dw * qw;
            }
        } else {
            int s = pi.getN() + p;
            if(s >= docs.length) {
                docs = Arrays.copyOf(docs, s * 2);
                weights = Arrays.copyOf(weights, s * 2);
            }

            while(pi.next()) {
                docs[p] = pi.getID();
                weights[p++] = dw * qw;
            }
        }
        qs.piW.stop();
        qs.unionW.stop();
    }

    @Override
    public void add(int[] d, float[] w, float qw) {

        added++;
        if(storeAll) {
            for(int i = 0; i < d.length; i++) {
                weights[d[i]] += w[i] * qw;
            }
        } else {

            int s = p + d.length;
            if(s >= docs.length) {
                docs = Arrays.copyOf(docs, s * 2);
                weights = Arrays.copyOf(weights, s * 2);
            }

            System.arraycopy(d, 0, docs, p, d.length);
            if(qw == 1) {
                System.arraycopy(w, 0, weights, p, w.length);
                p += w.length;
            } else {
                sqw += qw * qw;
                for(int i = 0; i < w.length; i++) {
                    weights[p++] = w[i] * qw;
                }
            }
        }
    }

    @Override
    public void addWeightOnly(float qw) {
        sqw += qw * qw;
    }

    @Override
    public ArrayGroup getGroup() {

        if(storeAll) {
            p = 0;
            qs.docCombW.start();
            for(int i = 0; i < weights.length; i++) {
                if(weights[i] > 0) {
                    docs[p] = i;
                    weights[p++] = weights[i];
                }
            }
            qs.docCombW.stop();
        } else {

            if(added > 1) {
                qs.postSortW.start();
                Util.sort(docs, weights, 0, p);
                qs.postSortW.stop();
                int s = -1;
                int prev = -1;

                qs.docCombW.start();
                for(int i = 0; i < p; i++) {
                    if(docs[i] != prev) {
                        docs[++s] = docs[i];
                        weights[s] = weights[i];
                    } else {
                        weights[s] += weights[i];
                    }
                    prev = docs[i];
                }
                qs.docCombW.stop();
                s++;
                p = s;
            }
        }
        ScoredGroup ret = new ScoredGroup(part, docs, weights, p, sqw);
        ret.setFields(fields);
        return ret;
    }
    @Override
    public String toString() {
        return String.format("part: %s storeAll: %s nDocs: %d size: %d", part,
                             storeAll, part.getNDocs(), p);
    }
} // ScoredQuickOr
