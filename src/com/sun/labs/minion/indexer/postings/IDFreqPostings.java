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
import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger(IDFreqPostings.class.getName());

    /**
     * The position where we're collecting data.
     */
    protected int pos = -1;

    /**
     * The frequencies for these postings.
     */
    protected int[] freqs;

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
        freqs = new int[ids.length];
    }

    /**
     * Makes a postings entry that is useful during querying.
     *
     * @param b the data read from a postings file.
     */
    public IDFreqPostings(PostingsInput[] in, long[] offset, int[] size) throws IOException {
        super(in, offset, size);
    }

    /**
     * Makes a postings entry that is useful during querying.
     *
     * @param b the data read from a postings file.
     * @param offset The offset in the buffer from which we should start
     * reading.  If this value is greater than 0, then we need to share the
     * bit buffer, since we may be part of a larger postings entry that
     * will need multiple readers.
     * @param size the size of the postings in bytes
     */
    public IDFreqPostings(ReadableBuffer b, int offset, int size) {
        super(b, offset, size);
    }

    @Override
    public Type getType() {
        return Type.ID_FREQ;
    }

    @Override
    public void add(Occurrence o) {
        int oid = o.getID();
        if(oid != currentID) {
            nIDs++;
            pos++;
            if(ids == null || nIDs >= ids.length) {
                int nl = nIDs + 128;
                ids = Arrays.copyOf(ids, nl);
                freqs = Arrays.copyOf(freqs, nl);
            }
            ids[pos] = oid;
            freqs[pos] = o.getCount();
            currentID = oid;
            lastID = currentID;
        } else {
            freqs[pos] += o.getCount();
        }
    }

    @Override
    protected void encodeOtherPostingsData(WriteableBuffer b, int i) {
        b.byteEncode(freqs[i]);
    }

    @Override
    public void append(Postings p, int start, int[] idMap) {

        if(idBuff == null) {
            idBuff = new ArrayBuffer(p.getN() * 2);
        }

        //
        // If there's no id mapping to be done, then do a simple append.
        if(idMap == null) {
            append(p, start);
            return;
        }

        //
        // We'll iterate through the postings.
        PostingsIterator pi = p.iterator(null);
        WriteableBuffer wpost = (WriteableBuffer) idBuff;
        while(pi.next()) {
            int origID = pi.getID();
            int mapID = idMap[origID];

            //
            // Skip deleted documents.
            if(mapID < 0) {
                continue;
            }

            int cID = mapID + start - 1;

            //
            // Increment our ID count, and see if we need to add a skip.
            nIDs++;
            if(nIDs % skipSize == 0) {
                addSkip(cID, (int) idBuff.position());
            }
            
            try {
            wpost.byteEncode(cID - lastID);
            } catch (ArithmeticException ex) {
                logger.log(Level.SEVERE, String.format(
                        "Error appending start: %s origID: %d mapID: %d cID: %d", 
                        start, origID, mapID, cID));
                throw(ex);
            }
            to += pi.getFreq();
            maxfdt = Math.max(pi.getFreq(), maxfdt);
            wpost.byteEncode(pi.getFreq());

            //
            // Set the new last document for our entry.
            lastID = cID;
        }
    }
    
    @Override
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
                tid = Util.addExpand(tid, np, pid);
                tf = Util.addExpand(tf, np++, freqs[p2++] + pi.getFreq());
                p1++;
                pi.next();
            }
        }

        while(p1 < n) {
            int pid = map[pi.getID()];
            if(pid > 0) {
                tid = Util.addExpand(tid, np, pid);
                tf = Util.addExpand(tf, np++, pi.getFreq());
            }
            pi.next();
            p1++;
        }

        if(p2 < nIDs) {
            int toadd = (nIDs - p2);
            if(np + toadd >= tid.length) {
                tid = Arrays.copyOf(tid, Math.max(np + toadd, tid.length * 2));
                tf = Arrays.copyOf(tf, Math.max(np + toadd, tf.length * 2));
            }
            System.arraycopy(ids, p2, tid, np, toadd);
            System.arraycopy(freqs, p2, tf, np, toadd);
            np += toadd;
        }

        if(np > 0) {
            nIDs = np;
            ids = tid;
            lastID = ids[np - 1];
            freqs = tf;
        }
    }

    /**
     * Gets the maximum frequency in the postings list.
     */
    @Override
    public int getMaxFDT() {
        return maxfdt;
    }

    /**
     * Gets the total number of occurrences in this postings list.
     */
    @Override
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
    @Override
    public PostingsIterator iterator(PostingsIteratorFeatures features) {

        if(ids != null) {
            return new UncompressedIDFreqIterator(features);
        }
        return new CompressedIDFreqIterator(features);
    }

    @Override
    public void clear() {
        super.clear();
        pos = -1;
    }
    
    @Override
    public String describe(boolean verbose) {
        StringBuilder b = new StringBuilder();
        b.append(getType()).append(' ').append("N: ").append(nIDs);
        if(verbose) {
            PostingsIterator pi = iterator(null);
            boolean first = true;
            while(pi.next()) {
                if(!first) {
                    b.append(' ');
                }
                first = false;
                b.append('<').append(pi.getID()).append(',').append(pi.getFreq()).append('>').append(',');
            }
        }
        return b.toString();
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
            return freqs[currPos];
        }

        @Override
        public float getWeight() {
            if(wf == null) {
                return getFreq();
            }
            wc.fdt = getFreq();
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

        @Override
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

        @Override
        public float getWeight() {
            if(wf == null) {
                return freq;
            }
            wc.fdt = freq;
            return wf.termWeight(wc);
        }

        @Override
        public int getFreq() {
            return freq;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(nIDs * 6);
        sb.append("nIDS: ").append(nIDs).append(" [");
        for(int i = 0; i < nIDs; i++) {
            if(i > 0) {
                sb.append(", ");
            }
            sb.append('(').append(ids[i]).append(',').append(freqs[i]).append(')');
        }
        sb.append(']');
        return sb.toString();
    }
} // IDFreqPostings

