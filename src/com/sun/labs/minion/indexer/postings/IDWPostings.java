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

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class IDWPostings extends IDPostings {
    
    protected float currWeight;
    
    /**
     * Creates a IDWPostings
     */
    public IDWPostings() {
        super();
    }
    
    public IDWPostings(ReadableBuffer b) {
        super(b, 0, 0);
    }
    
    protected int encode(int id) {
        int nb = super.encode(id);
        ((WriteableBuffer) post).encode(currWeight);
        currWeight = 0;
        return nb + 4;
    }
    
    public void add(Occurrence o) {
        super.add(o);
        currWeight += ((WeightedOccurrence) o).getWeight();
    }
    
    /**
     * Re-encodes the data from another postings onto this one.
     *
     * @param currID The current ID
     * @param lastID The last ID.
     * @param pi the iterator for another postings.
     */
    protected void recodeID(int currID, int lastID, PostingsIterator pi) {
        super.recodeID(currID, lastID, pi);
        ((WriteableBuffer) post).encode(pi.getWeight());
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

        //
        // We only support the cases where there are no features at all or
        // only weighting related features.
        if(features == null ||
            (features.getFields() == null &&
                  features.getMult() == null &&
                  !features.getPositions())) {
            return new IDWIterator(features);
        } else {
            log.warn(logTag, 3, "Requested unsupported features for " +
                     "IDFreqPostings");
            return null;
        }
    }

    public class IDWIterator extends IDIterator {

        /**
         * The current weight.
         */
        protected float weight;

        /**
         * Creates a postings iterator for this postings type.
         */
        public IDWIterator(PostingsIteratorFeatures features) {
            super(features);
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

            weight = rp.decodeFloat();
            
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
            return weight;
        }

    }
    

}