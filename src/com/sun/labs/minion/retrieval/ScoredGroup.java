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
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A scored array group is one for which the documents have scores
 * associated with them.  When combined with another group via one of the
 * combination operators, the scores for documents in both groups will be
 * combined according to the dictates of the operator.
 */
public class ScoredGroup extends ArrayGroup {
    
    private static final Logger logger = Logger.getLogger(ScoredGroup.class.getName());
    
    /**
     * The scores for the documents in the set.
     */
    protected float[] scores;
    
    /**
     * The weights to be applied (eventually) to the scores.
     */
    protected float[] weights;
    
    /**
     * The sum of the squared query weights for this group.
     */
    protected float sqw;
    
    /**
     * A flag indicating whether normalization has been performed on this
     * group or not.
     */
    protected boolean normalized;
    
    public ScoredGroup(int n) {
        docs = new int[n];
        scores = new float[n];
    }
    
    public ScoredGroup(DiskPartition part, float[] scores) {
        this.part = part;
        int n = 0;
        for(int i = 0; i < scores.length; i++) {
            if(scores[i] > 0) {
                n++;
            }
        }
        docs = new int[n];
        this.scores = new float[n];
        for(int i = 0; i < scores.length; i++) {
            if(scores[i] > 0) {
                docs[size] = i;
                this.scores[size++] = scores[i];
            }
        }
        sqw = 1;
        normalized = true;
    }
    
    /**
     * Creates a scored group from the given group.
     */
    protected ScoredGroup(ArrayGroup ag) {
        init(ag);
        if(ag instanceof ScoredGroup) {
            scores = ((ScoredGroup) ag).scores;
        } else {
            
            //
            // We'll leave the scores as 0.
            scores = new float[docs.length];
        }
    }
    
    /**
     * Creates an array group from the given postings iterator.
     */
    public ScoredGroup(PostingsIterator pi) {
        this(pi, 1);
    }
    
    /**
     * Creates an array group from the given postings iterator.  This
     * constructor is implicitly performing a dot product of the query term
     * and the documents in the iterator.
     *
     * @param pi the postings iterator from which we will get the documents
     * and scores for the group.
     * @param qw the weight associated with the query term that produced
     * these postings.
     *
     */
    public ScoredGroup(PostingsIterator pi, float qw) {
        if(pi == null) {
            this.docs = new int[0];
            this.scores = new float[0];
            return;
        }
        
        docs = new int[pi.getN()];
        scores = new float[docs.length];
        sqw += qw*qw;
        while(pi.next()) {
            docs[size]     = pi.getID();
            scores[size++] = pi.getWeight() * qw;
        }
    }
    
    /**
     * Creates a scored group from the given arrays.  This should be used
     * only for testing purposes!
     */
    public ScoredGroup(int[] docs, float[] scores, int l) {
        this(null, docs, scores, l, 0);
    }
    
    /**
     * Creates a scored group from the given arrays.  This should be used
     * only for testing purposes!
     */
    public ScoredGroup(DiskPartition part,
            int[] docs, float[] scores, int l) {
        this(part, docs, scores, l, 0);
    }
    
    /**
     * Creates a scored group from the given arrays.  This should be used
     * only for testing purposes!
     */
    public ScoredGroup(DiskPartition part,
            int[] docs, float[] scores,
            int l, float sqw) {
        super(docs, l);
        this.part   = part;
        this.scores = scores;
        this.sqw    = sqw;
    }
    
    /**
     * Creates a scored group from a set of euclidean distances between documents.
     * The score for a document is the distance, which means that you will need
     * to sort by <em>increasing</em> score to get the documents in the order that
     * you would like!
     *
     * <p>
     *
     * We will leave it up to the application to normalize such scores.
     *
     * @param part the partition from which the distances were calculated.
     * @param dist an array of doubles containing the distances between documents.
     *
     */
    public ScoredGroup(DiskPartition part, double[] dist) {
        
        this.part = part;
        docs = new int[dist.length];
        scores = new float[dist.length];
        for(int i = 1; i < dist.length; i++) {
            docs[i-1] = i;
            if(dist[i] != Double.POSITIVE_INFINITY) {
                scores[i-1] = (float) dist[i];
            }
        }
        size = dist.length-1;
    }

    @Override
    public ArrayGroup getStrict() {
        return new ArrayGroup(docs, size);
    }
    
    /**
     * Gets a scored group from this group, which just returns this group.
     */
    @Override
    public ScoredGroup getScored() {
        return this;
    }
    
    /**
     * Get the score at a particular index
     *
     */
    public float getScore(int i) {
        return scores[i];
    }
    
    /**
     * Adds a document to the group.
     */
    public void addDoc(int docID, float score) {
        if(size+1 >= docs.length) {
            docs = Arrays.copyOf(docs, docs.length*2);
            scores = Arrays.copyOf(scores, scores.length*2);
        }
        docs[size] = docID;
        scores[size++] = score;
    }
    
    /**
     * Sets the score at a particular index
     *
     */
    public void setScore(int i, float score) {
        scores[i] = score;
    }
    
    
    @Override
    public ArrayGroup intersect(QueryTerm t) {
        return t.eval(this);
    }
    
    @Override
    public void removeDeleted(ReadableBuffer del) {
        if(del == null) {
            return;
        }
        
        int rp = 0;
        for(int i = 0; i < size; i++) {
            if(!del.test(docs[i])) {
                docs[rp] = docs[i];
                scores[rp++] = scores[i];
            }
        }
        size = rp;
    }
    
    @Override
    public void retain(int ids[]) {
        Arrays.sort(ids);
        
        int rp = 0;
        for (int i = 0; i < size; i++) {
            if (Arrays.binarySearch(ids, docs[i]) >= 0) {
                docs[rp] = docs[i];
                scores[rp++] = scores[i];
            }
        }
        size = rp;
    }
    
    @Override
    public ArrayGroup normalize() {
        if(!normalized) {
            sqw = (float) Math.sqrt(sqw);
            if(sqw == 0) {
                sqw = 1;
            }
            ((InvFileDiskPartition) part).normalize(fields, docs, scores, size, sqw);
            normalized = true;
        }
        return this;
    }
    
    /**
     * Normalizes the scores by the document length.
     *
     * @param field the ID of a vectored field whose length we should use.  If 
     * this is -1, then the length computed across all vectored fields is used.
     * If this is 0, then the length for the data not in any explicit field is 
     * used.  If this is not 0, then the length of that field is used.  Note if
     * this ID is not for a vectored field, then a default length of 1 will be
     * used!
     * @return this group with normalized scores
     */
    public ArrayGroup normalize(int field) {
        
        if(!normalized) {
            sqw = (float) Math.sqrt(sqw);
            if(sqw == 0) {
                sqw = 1;
            }
            ((InvFileDiskPartition) part).getDF(field).normalize(docs, scores, size, sqw);
            normalized = true;
        }
        return this;
    }
    
    public void setNormalized() {
        normalized = true;
    }
    
    @Override
    public ArrayGroup destructiveIntersect(PostingsIterator pi) {
        if(pi == null) {
            size = 0;
            return this;
        }
        
        if(size == 0) {
            return this;
        }
        
        int rp = 0;
        
        if(pi.getN() < 10 * size) {
            pi.next();
            for(int i = 0; i < size; i++) {
                while(pi.getID() < docs[i] && pi.next());
                if(pi.getID() == docs[i]) {
                    docs[rp] = docs[i];
                    scores[rp++] = scores[i];
                }
            }
        } else {
            for(int i = 0; i < size; i++) {
                if(pi.findID(docs[i])) {
                    docs[rp] = docs[i];
                    scores[rp++] = scores[i];
                }
            }
        }
        size = rp;
        return this;
    }

    /**
     * Discards any entries in this scored group that are a score less
     * than the value supplied
     *
     * @param score the score below which to discard values
     * @return this group
     */
    public ScoredGroup discardBelow(float score) {
        // Scores are positive, so ignore this request if a negative
        // score is supplied
        if (score <= 0) {
            return this;
        }
        
        int pos = 0;
        for (int i = 0; i < size; i++) {
            if (scores[i] >= score) {
                docs[pos] = docs[i];
                scores[pos] = scores[i];
                pos++;
            }
        }
        size = pos;
        return this;
    }
    
    /**
     * Unions a statically type array group with this group.  We'll use the
     * method from <code>ArrayGroup</code> to do this.
     */
    @Override
    protected ArrayGroup agUnion(ArrayGroup ag) {
        return ag.union(this);
    }
    
    /**
     * Union a scored group with this group.  The current group is modified
     * and returned.
     *
     * @param ag The scored group to union with this group.
     * @return The result of unioning the given group with this group.  An
     * instance of <code>ScoredGroup</code> is returned.
     */
    @Override
    public ArrayGroup union(ScoredGroup ag) {
        
        ScoredGroup ret = new ScoredGroup(size + ag.size);
        
        int i1 = 0, i2 = 0;
        
        //
        // We need to keep track of the squared query weights for the
        // groups that we add to this one.  How we calculate that weight
        // will depend on how things have been normalized
        ret.sqw = (ag.normalized ? 0 : ag.sqw) +
                (normalized ? 0 : sqw);
        
        while(i1 < size && i2 < ag.size) {
            int d1 = docs[i1];
            int d2 = ag.docs[i2];
            
            int diff = d1 - d2;
            ret.docs[ret.size] = diff < 0 ? d1 : d2;
            if (diff < 0) {
                ret.scores[ret.size] = scores[i1];
                i1++;
            } else if (diff > 0) {
                ret.scores[ret.size] = ag.scores[i2];
                i2++;
            } else {
                ret.scores[ret.size] = scores[i1] + ag.scores[i2];
                i1++;
                i2++;
            }
            ret.size++;
        }
        
        if(i1 < size) {
            int n = size - i1;
            System.arraycopy(docs, i1, ret.docs, ret.size, n);
            System.arraycopy(scores, i1, ret.scores, ret.size, n);
            ret.size += n;
        }
        
        if(i2 < ag.size) {
            int n = ag.size - i2;
            System.arraycopy(ag.docs, i2, ret.docs, ret.size, n);
            System.arraycopy(ag.scores, i2, ret.scores, ret.size, n);
            ret.size += n;
        }
        return ret;
    }
    
    /**
     * Intersect a strict boolean group with this group.
     *
     * @param ag The scored group to intersect with this group.
     * @return The result of intersecting the given group with this group.
     * An instance of <code>ScoredGroup</code> is returned.
     */
    @Override
    public ArrayGroup agIntersect(ArrayGroup ag) {
        return ag.intersect(this);
    }
    
    /**
     * Intersect a scored group with this group.
     *
     * @param ag The scored group to intersect with this group.
     * @return The result of intersecting the given group with this group.
     * An instance of <code>ScoredGroup</code> will be returned.
     */
    @Override
    public ArrayGroup intersect(ScoredGroup ag) {
        
        //
        // Get a scored group to return.
        ScoredGroup ret = new ScoredGroup(Math.min(size, ag.size));
        
        //
        // We need to keep track of the squared query weights for the
        // groups that we add to this one.  How we calculate that weight
        // will depend on how things have been normalized
        ret.sqw = (ag.normalized ? 0 : ag.sqw) +
                (normalized ? 0 : sqw);
        
        int i1 = 0, i2 = 0;
        while(i1 < size && i2 < ag.size) {
            int d1 = docs[i1];
            int d2 = ag.docs[i2];
            
            if(d1 < d2) {
                i1++;
            } else if(d1 > d2) {
                i2++;
            } else {
                ret.docs[ret.size] = d1;
                ret.scores[ret.size++] = scores[i1++] + ag.scores[i2++];
            }
        }
        return ret;
    }
    
    /**
     * Intersect a negative group with this group.  The current group is
     * modified and returned.
     *
     * @param ag The scored group to intersect with this group.
     * @return The result of intersecting the given group with this group.
     */
    @Override
    public ArrayGroup intersect(NegativeGroup ag)  {
        
        //
        // Get a scored group to return.
        ScoredGroup ret = new ScoredGroup(Math.max(size, ag.size));
        ret.sqw         = sqw;
        ret.normalized  = normalized;
        
        int i1 = 0, i2 = 0;
        while(i1 < size && i2 < ag.size) {
            int d1 = docs[i1];
            int d2 = ag.docs[i2];
            
            if(d1 < d2) {
                //
                // It's in our set, but not in the other, so it's in the result.
                ret.docs[ret.size] = d1;
                ret.scores[ret.size++] = scores[i1];
                i1++;
            } else {
                i2++;
                if(d1 == d2) {
                    i1++;
                }
            }
        }
        
        if(i1 < size) {
            int n = size - i1;
            System.arraycopy(docs, i1, ret.docs, ret.size, n);
            System.arraycopy(scores, i1, ret.scores, ret.size, n);
            ret.size += n;
        }
        return ret;
    }
    
    /**
     * Applies a multiplier to an array group, returning a new group.  This
     * will only affect scored groups, but a query may apply a multiplier
     * to any group.
     *
     * @param m The multiplier to apply.
     * @return A group where the multiplier has been applied.  The current
     * group is not affected.
     */
    @Override
    public ArrayGroup mult(float m) {
        ScoredGroup ret = (ScoredGroup) this.clone();
        return ret.destructiveMult(m);
    }
    
    public ArrayGroup destructiveMult(float m) {
        for(int i = 0; i < size; i++) {
            scores[i] *= m;
        }
        return this;
    }
    
    
    /**
     * Gets an iterator for the documents in this group.
     */
    @Override
    public DocIterator iterator() {
        return new ScoredDocIterator();
    }
    
    /**
     * Sorts this group by score, returning a sorted version.  Note that
     * the returned group will be useless for any further retrieval
     * operations.
     */
    public ScoredGroup sort() {
        return sort(false);
    }
    
    /**
     * Sorts this group by score, returning a sorted version.  Note that
     * the returned group will be useless for any further retrieval
     * operations.
     *
     * @param destructive if <code>true</code>, this group is modified
     */
    public ScoredGroup sort(boolean destructive) {
        ScoredGroup ret;
        if(!destructive) {
            ret = (ScoredGroup) this.clone();
        } else {
            ret = this;
        }
        
        Util.sort(ret.scores, ret.docs, 0, ret.size);
        
        //
        // That sorts them into ascending order.  We want descending, so we
        // reverse the array.
        int stop = size / 2;
        for(int i = 0, j = size - 1; j > i; i++, j--) {
            int td = ret.docs[i];
            ret.docs[i] = ret.docs[j];
            ret.docs[j] = td;
            float ts = ret.scores[i];
            ret.scores[i] = ret.scores[j];
            ret.scores[j] = ts;
        }
        
        return ret;
    }
    
    /**
     * Clones this group.
     *
     * @return a clone of this group.  We will clone the internal arrays.
     */
    @Override
    public Object clone() {
        ScoredGroup result = null;
        result = (ScoredGroup) super.clone();
        result.scores = (float[]) scores.clone();
        if(weights != null) {
            result.weights = (float[]) weights.clone();
        }
        return result;
    }
    
    /**
     * Tests two scored groups for equality.  Two scored groups are equal
     * if they contain exactly the same set of documents.  A scored group
     * can only be equal to another scored group.
     *
     * @return <code>true</code> if the groups contain the same documents,
     * <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof ScoredGroup)) {
            return false;
        }
        
        ScoredGroup ag = (ScoredGroup) o;
        
        if(size != ag.size) {
            return false;
        }
        
        for(int i = 0; i < size; i++) {
            if((docs[i] != ag.docs[i]) ||
                    (scores[i] != ag.scores[i])) {
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return toString(0);
    }
    
    public String toString(int t) {
        StringBuilder sb = new StringBuilder(size*5);
        switch(t) {
            case 0:
                sb.append("[");
                for(int i = 0; i < size; i++) {
                    sb.append(i == 0 ? "" : ", ").append(String.format("(%d,%5.3f)", docs[i], scores[i]));
                }
                
                sb.append("] ").append(size).append(" docs ").append(sqw).append(" squared qw");
                break;
            case 1:
                sb.append(size).append(" docs ").append(sqw).append(" squared qw\n");
                for(int i = 0; i < size; i++) {
                    sb.append(String.format("%6d %10f\n", docs[i], scores[i]));
                }
                break;
        }
        
        return sb.toString();
    }

    public void setQueryWeight(float sqw) {
        this.sqw = sqw;
    }
    
    /**
     * A class for an iterator for this set.
     */
    public class ScoredDocIterator extends DocIterator {
        
        /**
         * Whether we need to re-run any score modifier for the score that
         * we're curently looking at.  We want callers to use getScore() with
         * impunity, rather than copying a score locally.
         */
        private boolean rescore = true;

        @Override
        public float getScore() {
            if(sm != null && rescore) {
                scores[pos] = sm.modifyScore(scores[pos], this);
                rescore = false;
            }
            return scores[pos];
        }
        
        @Override
        public int compareTo(DocIterator o) {
            
            float s1;
            float s2; 
            if(sm != null) {
                s1 = sm.modifyScore(scores[pos], this);
                s2 = sm.modifyScore(o.getScore(), this);
            } else {
                s1 = scores[pos];
                s2 = o.getScore();
            }

            if(s1 < s2) {
                return -1;
            }
            
            if(s1 > s2) {
                return 1;
            }
            
            return 0;
        }
        
        @Override
        public boolean next() {
            rescore = true;
            return super.next();
        }
    }
    
} // ScoredGroup
