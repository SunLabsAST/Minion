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

import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;

import com.sun.labs.minion.util.Util;

public class QuickOr {

    /**
     * The partition for which we're holding documents.
     */
    protected DiskPartition part;

    QueryStats qs;

    /**
     * The documents that we're storing.
     */
    protected int[] docs;

    /**
     * The number of unique documents that we're storing.
     */
    protected int p;

    /**
     * The number of sets of postings that have been added.
     */
    protected int added;

    /**
     * A flag indicating that the weights array is as long as the number of
     * documents in our partition, so we don't need to store the document
     * IDs.
     */
    boolean storeAll;

    public QuickOr(DiskPartition part, int estSize) {
        this(part, estSize, false);
    }

    public QuickOr(DiskPartition part, int estSize, boolean shouldStoreAll) {
        this.part = part;
        qs = new QueryStats();
        if(shouldStoreAll) {
            storeAll = true;
        } else {
            storeAll = shouldStoreAll(part, estSize);
        }
        if(storeAll) {
            docs = new int[part.getMaxDocumentID() + 1];
        } else {
            docs = new int[estSize];
        }
    }

    /**
     * Decide whether we should store weights for all documents in a
     * partition.  We will do so if the number of documents in the
     * partition is less than 50,000 or if the estimated size of the
     * results set is more than 90% of the number of documents in the
     * partition.
     */
    protected boolean shouldStoreAll(DiskPartition part,
                                     int estSize) {

        if(part == null) {
            return false;
        }

        int N = part.getNDocs();
        return N < 5000 || estSize >= 0.9 * N;
    }

    public void setQueryStats(QueryStats qs) {
        this.qs = qs;
    }

    public void add(PostingsIterator pi) {

        if(pi == null) {
            return;
        }

        added++;
        if(storeAll) {
            while(pi.next()) {
                docs[pi.getID()] = 1;
            }
        } else {
            int s = pi.getN() + p;
            if(s >= docs.length) {
                docs = Util.expandInt(docs, s * 2);
            }

            while(pi.next()) {
                docs[p++] = pi.getID();
            }
        }
    }

    public void add(PostingsIterator pi, float fw) {
        add(pi);
    }

    /**
     * Adds an explicit set of documents and weights to this quick or.
     *
     * @param d the documents to add
     * @param w the weights associated with the documents
     * @param qw a weight associated with a query term, which will be
     * multiplied against the weights in the array.
     */
    public void add(int[] d, float[] w, float qw) {

        added++;
        if(storeAll) {
            for(int i = 0; i < d.length; i++) {
                docs[d[i]] = 1;
            }
        } else {

            int s = p + d.length;
            if(s >= docs.length) {
                docs = Util.expandInt(docs, s * 2);
            }
            System.arraycopy(d, 0, docs, p, d.length);
        }
    }

    /** 
     * Adds only a weight to this QuickOr.  This allows for document
     * representations where not all terms from a document appear
     * in the partition that this QuickOr corresponds to.  We still want
     * those terms' weights to be accounted for.
     * 
     * @param qw the query weight of a term to add to this QuickOr
     */
    public void addWeightOnly(float qw) {
        //
        // Do nothing.  We don't care about scores in the base class.
    }

    public ArrayGroup getGroup() {
        if(storeAll) {
            p = 0;
            for(int i = 0; i < docs.length; i++) {
                if(docs[i] == 1) {
                    docs[p++] = i;
                }
            }
        } else {

            if(added > 1) {
                java.util.Arrays.sort(docs, 0, p);
                int s = 0;
                int prev = -1;

                for(int i = 0; i < p; i++) {
                    if(docs[i] != prev) {
                        docs[s++] = docs[i];
                    }
                    prev = docs[i];
                }
                p = s;
            }
        }
        return new ArrayGroup(part, docs, p);
    }
} // QuickOr

