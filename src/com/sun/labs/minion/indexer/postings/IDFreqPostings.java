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

package com.sun.labs.minion.indexer.postings;

import com.sun.labs.minion.indexer.postings.Postings.Type;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.Util;

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * A postings class for IDs that have frequencies associated with them.
 *
 * <p>
 *
 * The format is just like that for {@link IDPostings}, except that for
 * each ID:
 *
 * <ol>
 *
 * <li>The ID is byte encoded as a delta from the previous ID.</li>
 *
 * <li>The frequency is byte encoded as-is</li>
 *
 * </ol>
 */
public class IDFreqPostings extends IDPostings {

    /**
     * The frequencies for these postings.
     */
    protected int[] freqs;

    /**
     * The frequency of the current ID.
     */
    protected int freq;

    /**
     * The total number of occurrences in the postings list.  Note that this is
     * a long, even though the return value from getTotalOccurrences is an int.
     * This is because, while it doesn't make any sense to return a long's worth 
     * of counts, we may collect more than an int's worth.
     */
    protected long to;

    /**
     * The maximum frequency.
     */
    protected int maxfdt;

    /**
     * Makes a postings entry that is useful during indexing.
     */
    public IDFreqPostings() {
        super();
    }

    /**
     * Makes a postings entry that is useful during querying.
     *
     * @param b the data read from a postings file.
     */
    public IDFreqPostings(ReadableBuffer b) {
        super(b);
    }

    /**
     * Makes a postings entry that is useful during querying.
     *
     * @param b the data read from a postings file.
     * @param offset The offset in the buffer from which we should start
     * reading.  If this value is greater than 0, then we need to share the
     * bit buffer, since we may be part of a larger postings entry that
     * will need multiple readers.
     */
    public IDFreqPostings(ReadableBuffer b, int offset, int size) {
        super(b, offset, size);
    }

    @Override
    public Type getType() {
        return Type.ID_FREQ;
    }

    /**
     * Adds an occurrence to the postings list.
     *
     * @param o The occurrence to add.
     */
    public void add(Occurrence o) {

        if(o.getID() != curr) {
            if(curr != 0) {
                nIDs++;
            }
            if(ids == null || nIDs >= ids.length) {
                ids = Util.expandInt(ids, (nIDs+1) * 2);
                freqs = Util.expandInt(freqs, ids.length);
            }
            ids[nIDs] = o.getID();
            curr = o.getID();
        }
        freqs[nIDs] += o.getCount();
    }

    @Override
    protected void encodeOtherData(WriteableBuffer b, int i) {
        b.byteEncode(freqs[i]);
    }

    /**
     * Re-encodes the data from another postings onto this one.  A
     * PostingsIterator is passed in, adjusted to the current posting
     * being encoded.  This allows additional postings data about the
     * current ID to be retrieved.
     *
     * @param currID The current ID
     * @param lastID The last ID.
     * @param pi the iterator of another postings.
     */
    protected void recodeID(int currID, int lastID, PostingsIterator pi) {
        super.recodeID(currID, lastID, pi);
        to += pi.getFreq();
        maxfdt = Math.max(pi.getFreq(), maxfdt);
        ((WriteableBuffer) post).byteEncode(pi.getFreq());
    }

    public void merge(MergeablePostings mp, int[] map) {

        int n = ((Postings) mp).getN();
        int[] tid = new int[Math.max(n, nIDs)];
        int[] tf = new int[Math.max(n, nIDs)];

        PostingsIterator pi = ((Postings) mp).iterator(null);
        pi.next();
        int p1 = 0;
        int p2 = 0;
        int np = 0;
        while(p1 < n && p2 < nIDs) {
            int pid = map[pi.getID()];
            if(pid == -1) {
                //
                // There is no mapping for this ID, so continue to the next.
                p1++;
                pi.next();
                continue;
            }
            int diff = pid - ids[p2];
            if(diff < 0) {
                tid = Util.addExpand(tid, np, pid);
                tf = Util.addExpand(tf, np++, pi.getFreq());
                p1++;
                pi.next();
            } else if(diff > 0) {
                tid = Util.addExpand(tid, np, ids[p2]);
                tf = Util.addExpand(tf, np++, freqs[p2++]);
            } else {
                tid = Util.addExpand(tid, np, ids[p2]);
                tf = Util.addExpand(tf, np++, freqs[p2++] + pi.getFreq());
                p1++;
                pi.next();
            }
        }

        while(p1 < n) {
            int pid = map[pi.getID()];
            if(pid > 0) {
                tid = Util.addExpand(tid, np, map[pi.getID()]);
                tf = Util.addExpand(tf, np++, pi.getFreq());
            }
            pi.next();
            p1++;
        }

        if(p2 < nIDs) {
            int toadd = (nIDs - p2);
            if(np + toadd >= tid.length) {
                tid = Util.expandInt(tid, Math.max(np + toadd, tid.length * 2));
                tf = Util.expandInt(tf, Math.max(np + toadd, tf.length * 2));
            }
            System.arraycopy(ids, p2, tid, np, toadd);
            System.arraycopy(freqs, p2, tf, np, toadd);
            np += toadd;
        }

        nIDs = np;
        ids = tid;
        freqs = tf;
    }

    /**
     * Gets the maximum frequency in the postings list.
     */
    public int getMaxFDT() {
        return maxfdt;
    }

    /**
     * Gets the total number of occurrences in this postings list.
     */
    public long getTotalOccurrences() {
        return to;
    }

    /**
     * Gets an iterator for the postings.
     *
     * @param features A set of features that the iterator must support.
     * @return A postings iterator.  The iterators for these postings only
     * support the weighting function feature.  If any extra features are
     * requested, a warning will be logged and <code>null</code> will be
     * returned.
     */
    public PostingsIterator iterator(PostingsIteratorFeatures features) {

        if(ids != null) {
            return new UncompressedIDFreqIterator(features);
        }
        return new CompressedIDFreqIterator(features);
    }

    public class UncompressedIDFreqIterator extends UncompressedIDIterator {
        
        /**
         * The weighting function.
         */
        protected WeightingFunction wf;

        /**
         * A set of weighting components.
         */
        protected WeightingComponents wc;

        /**
         * Creates a postings iterator for this postings type.
         */
        public UncompressedIDFreqIterator(PostingsIteratorFeatures features) {
            super(features);
            if(features != null) {
                wf = features.getWeightingFunction();
                wc = features.getWeightingComponents();
            }
        }

        @Override
        public int getFreq() {
            return freqs[pos];
        }

        @Override
        public float getWeight() {
            if(wf == null) {
                return freq;
            }
            wc.fdt = freq;
            return wf.termWeight(wc);
        }


    }

    public class CompressedIDFreqIterator extends CompressedIDIterator {

        /**
         * The current frequency.
         */
        protected int freq;

        /**
         * The weighting function.
         */
        protected WeightingFunction wf;

        /**
         * A set of weighting components.
         */
        protected WeightingComponents wc;
        
        /**
         * Creates a postings iterator for this postings type.
         */
        public CompressedIDFreqIterator(PostingsIteratorFeatures features) {
            super(features);
            if(features != null) {
                wf = features.getWeightingFunction();
                wc = features.getWeightingComponents();
            }
        }

        /**
         * Advances to the next ID in the postings entry.
         *
         * @param id The ID to use for that document, if we've skipped to
         * this point. If id is less than 0, we will use the ID as it was
         * decoded.
         * @return <code>true</code> if there is a next ID,
         * <code>false</code> otherwise.
         */
        protected boolean next(int pos, int id) {
            if(done) {
                return false;
            }

            if(pos > 0) {
                rp.position(pos);
            }

            curr += rp.byteDecode();

            if(id > 0) {
                curr = id;
            }

            freq = rp.byteDecode();
            done = curr == lastID;
            return true;
        }

        /**
         * Gets the weight of the term for the current ID, as generated by
         * some weighting function.
         *
         * @return the term weight for this ID.  If the weighting function
         * is null, the raw frequency will be returned.
         */
        public float getWeight() {
            if(wf == null) {
                return freq;
            }
            wc.fdt = freq;
            return wf.termWeight(wc);
        }

        /**
         * Gets the frequency of the term in the current document.
         */
        public int getFreq() {
            return freq;
        }

        public int get(int[] ids) {
            int p;
            for(p = 0; p < ids.length && curr != lastID; p++) {
                curr += rp.byteDecode();
                rp.byteDecode();
            }
            return p;
        }

        public int get(int[] ids, int[] freq) {
            int p;
            for(p = 0; p < ids.length && curr != lastID; p++) {
                curr += rp.byteDecode();
                ids[p] = curr;
                freq[p] = rp.byteDecode();
            }
            return p;
        }

        public int get(int[] ids, float[] weights) {
            int p;
            for(p = 0; p < ids.length && curr != lastID; p++) {
                curr += rp.byteDecode();
                ids[p] = curr;
                freq = rp.byteDecode();
                if(wf == null) {
                    weights[p] = freq;
                } else {
                    wc.fdt = freq;
                    weights[p] = wf.termWeight(wc);
                }

            }
            return p;
        }
    }
} // IDFreqPostings

