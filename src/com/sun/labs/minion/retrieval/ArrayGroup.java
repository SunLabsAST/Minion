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

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.ResultAccessor;
import com.sun.labs.minion.ScoreModifier;
import com.sun.labs.minion.indexer.dictionary.DiskDictionaryBundle.Fetcher;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;

import com.sun.labs.minion.util.Util;

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A container for the sets of arrays that are generated during document
 * retrieval operations.  This class implements a strict boolean set of
 * documents, i.e., documents in such a set do not have any scores
 * associated with them.
 */
public class ArrayGroup implements Cloneable {

    private static final Logger logger = Logger.getLogger(ArrayGroup.class.
            getName());

    /**
     * The documents in the set. Elements of this array may be set to a
     * value less than 0, indicating a position where a document was
     * removed during processing.
     */
    protected int[] docs;

    /**
     * The width of the original query, in terms. Used to compute where the
     * passages are in the <code>pass</code> array.
     */
    protected int width;

    /**
     * The size of the current set, that is the number of documents that it
     * contains.
     */
    protected int size;
    
    /**
     * The fields that contributed documents to this group.
     */
    protected Set<FieldInfo> fields;

    /**
     * Passages for each field.
     */
    protected PassageStore[] pass;

    /**
     * The partition that generated this set.
     */
    protected DiskPartition part;

    /**
     * The query terms that generated this set.
     */
    protected List queryTerms;

    protected ScoreModifier sm;

    protected ArrayGroup() {
    }

    /**
     * Creates a group that can hold the given number of documents.
     *
     * @param n The number of documents that the group must be able to
     * hold.
     */
    public ArrayGroup(int n) {
        docs = new int[n];
    }

    /**
     * Creates an array group with the specified array of document IDs.
     */
    public ArrayGroup(int[] docs, int l) {
        this(null, docs, l);
    }

    /**
     * Creates an array group with the specified array of document IDs.
     */
    public ArrayGroup(DiskPartition part, int[] docs, int l) {
        this.part = part;
        this.docs = docs;
        this.size = l;
    }

    /**
     * Creates an array group from the given postings iterator.
     */
    public ArrayGroup(PostingsIterator pi) {
        init(pi);
    }

    /**
     * Creates a group that shares a set of documents with another group.
     *
     * @param ag The group to share data with.
     */
    protected ArrayGroup(ArrayGroup ag) {
        init(ag);
    }

    /**
     * Initializes this group with data from another group.  The
     * data will be shared as much as possible in order to avoid
     * copying.
     *
     * @param ag The group to share data with.
     */
    protected void init(ArrayGroup ag) {
        docs = ag.docs;
        pass = ag.pass;
        size = ag.size;
    }

    /**
     * Re-initializes the group with the contents of the given postings
     * iterator.  To be used by others only in extremis (e.g., when
     * unioning thousands of saved field terms), since this is destructive.
     *
     * @param pi The iterator to use.
     */
    protected void init(PostingsIterator pi) {
        if(pi == null) {
            if(docs == null) {
                docs = new int[0];
            } else {
                size = 0;
            }
            return;
        }

        if(docs == null || docs.length < pi.getN()) {
            docs = new int[pi.getN()];
        }
        while(pi.next()) {
            docs[size++] = pi.getID();
        }
    }

    /**
     * Resizes the group so that it can hold the given number of
     * documents.
     *
     * @param n The number of documents that the group must be able to
     * hold.
     */
    protected void resize(int n) {
        if(n > docs.length) {
            docs = Arrays.copyOf(docs, n);
        }
    }

    /**
     * Returns a negative version of this array group.  The data will not
     * be copied into the new group, rather it will share the data with the
     * old group.  This is so that we don't need to clone large arrays.
     *
     * @return A negative group containing the same documents as this
     * group.
     */
    public ArrayGroup getNegative() {
        return new NegativeGroup(this);
    }

    /**
     * Gets a strict version of this array group, which just returns this
     * group.
     *
     * @return A strict group containing the same documents as this group.
     */
    public ArrayGroup getStrict() {
        return this;
    }

    /**
     * Gets a scored version of this array group.  The data will not be
     * copied into the new group, rather it will share the data with the
     * old group.  This is so that we don't need to clone large arrays.
     *
     * @return A scored group containing the same documents as this group.
     * If this group is a scored group, the documents in the new group will
     * have the same scores, otherwise all of the scores will be 1.
     */
    public ScoredGroup getScored() {
        return new ScoredGroup(this);
    }

    /**
     * Normalizes this group.
     */
    public ArrayGroup normalize() {
        return this;
    }

    /**
     * Returns the size of this set.
     */
    public int getSize() {
        return size;
    }

    /** 
     * Sets the size of this set.  Only do this if you know what
     * you're doing.
     * 
     * @param newSize the new size
     */
    public void setSize(int newSize) {
        size = newSize;
    }

    /**
     * Gets the documents making up this array group, as an array sized to
     * fit.
     */
    public int[] getDocs() {
        int[] ret = new int[size];
        System.arraycopy(docs, 0, ret, 0, size);
        return ret;
    }

    /** 
     * Gets the docId at a particular index
     *
     */
    public int getDoc(int i) {
        return docs[i];
    }

    /** 
     * Sets the docId at a particular index
     *
     */
    public void setDoc(int i, int docId) {
        docs[i] = docId;
    }

    /**
     * Adds a document to the group.
     */
    public void addDoc(int docID) {
        if(size + 1 >= docs.length) {
            docs = Arrays.copyOf(docs, docs.length * 2);
        }
        docs[size++] = docID;
    }

    public Set<FieldInfo> getFields() {
        return fields;
    }

    public void setFields(Set<FieldInfo> fields) {
        this.fields = fields;
    }
    
    /**
     * Unions the documents generated by the term with the ones in this
     * group.  Note that if the limit on the document set size has been
     * surpassed, we will only consider modifying the scores for documents
     * that are already in the set.
     *
     * @return The result of unioning the current term with this set.  The
     * static type of the result is determined by a combination of the
     * types of the groups being combined and the current query status.
     */
    public ArrayGroup union(QueryTerm t) {

        //
        // Union in the documents from the given term.
        return union(t.eval(null));
    }

    /**
     * Intersects the documents generated by the term with the ones in this
     * group. A new group is returned.
     *
     * @param t The term to intersect with the current set.
     * @return The result of intersecting the given term with this set.
     */
    public ArrayGroup intersect(QueryTerm t) {
        return t.eval(this);
    }

    /**
     * Removes deleted documents from the results set, modifying the set in
     * the process.
     */
    public void removeDeleted() {
        if(part != null) {
            removeDeleted(part.getDeletedDocumentsMap());
        }
    }

    /**
     * Removes deleted documents from the results set, modifying the set in
     * the process.
     */
    public void removeDeleted(ReadableBuffer del) {
        if(del == null) {
            return;
        }

        int rp = 0;
        for(int i = 0; i < size; i++) {
            if(!del.test(docs[i])) {
                docs[rp++] = docs[i];
            }
        }
        size = rp;
    }

    /** 
     * Retain only the documents with the given ids
     * 
     * @param ids document ids to keep
     */
    public void retain(int[] ids) {
        Arrays.sort(ids);

        int rp = 0;
        for(int i = 0; i < size; i++) {
            if(Arrays.binarySearch(ids, docs[i]) >= 0) {
                docs[rp++] = docs[i];
            }
        }
        size = rp;
    }

    /**
     * Intersects a postings iterator with this group, destructively.
     * 
     */
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
                    docs[rp++] = docs[i];
                }
            }
        } else {
            for(int i = 0; i < size; i++) {
                if(pi.findID(docs[i])) {
                    docs[rp++] = docs[i];
                }
            }
        }
        size = rp;
        return this;
    }

    /**
     * Unions another group with this group.
     *
     * @param ag The group to union with this group.
     * @return The result of unioning the given group with this group.  The
     * static type of the return value will depend on the type of
     * <code>ag</code>.
     */
    public ArrayGroup union(ArrayGroup ag) {

        //
        // Handle a scored group.
        if(ag instanceof ScoredGroup) {
            return union((ScoredGroup) ag);
        }

        if(ag instanceof NegativeGroup) {
            return union((NegativeGroup) ag);
        }

        return agUnion(ag);
    }

    /**
     * Unions a statically-typed array group with the current group.  This
     * should be used only for array groups that have the static type
     * <code>ArrayGroup</code>!
     */
    protected ArrayGroup agUnion(ArrayGroup ag) {

        //
        // Straightforward boolean union.  How beautiful!
        ArrayGroup ret = new ArrayGroup(size + ag.size);
        int i1 = 0, i2 = 0;

        while(i1 < size && i2 < ag.size) {
            int d1 = docs[i1];
            int d2 = ag.docs[i2];

            if(d1 < d2) {
                ret.docs[ret.size++] = d1;
                i1++;
            } else {
                ret.docs[ret.size++] = d2;
                i2++;
                if(d1 == d2) {
                    i1++;
                }
            }
        }

        if(i1 < size) {
            System.arraycopy(docs, i1, ret.docs, ret.size, size - i1);
            ret.size += size - i1;
        }

        if(i2 < ag.size) {
            System.arraycopy(ag.docs, i2, ret.docs, ret.size, ag.size - i2);
            ret.size += ag.size - i2;
        }

        return ret;
    }

    /**
     * Unions a scored group with this group, returning a new scored group.
     *
     * @param ag The group to union with this group.
     * @return an instance of <code>ScoredGroup</code> that contains scores
     * for the documents that have them.
     */
    public ArrayGroup union(ScoredGroup ag) {

        //
        // We'll keep the scores from the scored group, and use 1 for the
        // scores from this group.
        ScoredGroup ret = new ScoredGroup(size + ag.size);

        int i1 = 0, i2 = 0;
        while(i1 < size && i2 < ag.size) {
            int d1 = docs[i1];
            int d2 = ag.docs[i2];
            int diff = d1 - d2;

            ret.docs[ret.size] = diff < 0 ? d1 : d2;
            ret.scores[ret.size++] = diff < 0 ? 1 : ag.scores[i2++];

            if(diff <= 0) {
                i1++;
            }
        }

        if(i1 < size) {
            int n = size - i1;
            System.arraycopy(docs, i1, ret.docs, ret.size, n);

            //
            // Put in the 1s.
            for(int i = 0; i < n; i++) {
                ret.scores[ret.size++] = 1;
            }
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
     * Unions a postings iterator with this group, placing the results in
     * the given group.  This is to be used only in extremis, since it's
     * destructive to the given group.
     *
     * @param ag The group to put the results in.  May need resizing.
     * @param pi The postings iterator to union.
     * @return <code>ag</code>
     */
    protected ArrayGroup union(ArrayGroup ag, PostingsIterator pi) {

        //
        // If the iterator is null, we just return a copy of this set.
        if(pi == null) {
            ag.resize(size);
            System.arraycopy(docs, 0, ag.docs, 0, size);
            ag.size = size;
            return ag;
        }

        ag.resize(size + pi.getN());
        ag.size = 0;
        int i1 = 0;
        pi.next();
        int d2 = pi.getID();
        while(i1 < size && d2 > 0) {

            int d1 = docs[i1];

            if(d1 < d2) {
                ag.docs[ag.size++] = d1;
                i1++;
            } else {
                ag.docs[ag.size++] = d2;
                if(d1 == d2) {
                    i1++;
                }
                if(pi.next()) {
                    d2 = pi.getID();
                } else {
                    d2 = -1;
                }
            }
        }

        if(i1 < size) {
            System.arraycopy(docs, i1, ag.docs, ag.size, size - i1);
            ag.size += size - i1;
        }

        if(d2 > 0) {
            ag.docs[ag.size++] = d2;
            while(pi.next()) {
                ag.docs[ag.size++] = pi.getID();
            }
        }

        return ag;
    }

    /**
     * Unions a negative group with this group, returning a negative
     * group.
     *
     * @param ag The negative group to union with this group.
     */
    public ArrayGroup union(NegativeGroup ag) {

        //
        // DeMorgan's law: A union not(B) is the same as not(not(A)
        // intersect B), so we get a negative version of this set,
        // intersect the non-negative version of B, and then get a negative
        // version of the result.
        return getNegative().intersect(ag.getStrict()).getNegative();
    }

    /**
     * Dispatches the intersect operator to the appropriate method by
     * downcasting the <code>Object</code> to the proper subtype.  The
     * given object must be an instance of <code>ArrayGroup</code> or
     * <code>QueryTerm</code>.
     *
     * @return The result of intersecting the given element with the current
     * group. The static type of the result is determined by a combination
     * of the types of the groups being combined and the current query
     * status.
     * @throws ClassCastException if the given object is not an instance of
     * <code>ArrayGroup</code> or <code>QueryTerm</code>.
     */
    public ArrayGroup intersect(ArrayGroup ag) {

        //
        // For a scored group, we'll return a scored group.
        if(ag instanceof ScoredGroup) {
            return intersect((ScoredGroup) ag);
        }

        //
        // For a negative group, we return an ArrayGroup.
        if(ag instanceof NegativeGroup) {
            return intersect((NegativeGroup) ag);
        }

        //
        // Otherwise it's straight boolean intersection.
        return agIntersect(ag);
    }

    protected ArrayGroup agIntersect(ArrayGroup ag) {
        ArrayGroup ret = new ArrayGroup(Math.min(size, ag.size));
        int i1 = 0, i2 = 0;
        while(i1 < size && i2 < ag.size) {
            int d1 = docs[i1];
            int d2 = ag.docs[i2];

            if(d1 < d2) {
                i1++;
            } else if(d1 > d2) {
                i2++;
            } else {
                ret.docs[ret.size++] = d1;
                i1++;
                i2++;
            }
        }

        return ret;
    }

    protected ArrayGroup destructiveIntersect(ArrayGroup ag) {
        int i1 = 0, i2 = 0, s = 0;
        while(i1 < size && i2 < ag.size) {
            int d1 = docs[i1];
            int d2 = ag.docs[i2];

            if(d1 < d2) {
                i1++;
            } else if(d1 > d2) {
                i2++;
            } else {
                docs[s++] = d1;
                i1++;
                i2++;
            }
        }

        size = s;

        return this;
    }

    public ArrayGroup intersect(ScoredGroup ag) {
        ScoredGroup ret = new ScoredGroup(Math.min(size, ag.size));

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
                ret.scores[ret.size++] = ag.scores[i2++];
                i1++;
            }
        }
        ret.normalized = ag.normalized;
        return ret;
    }

    public ArrayGroup intersect(NegativeGroup ag) {
        ArrayGroup ret = new ArrayGroup(Math.max(size, ag.size));

        int i1 = 0, i2 = 0;
        while(i1 < size && i2 < ag.size) {
            int d1 = docs[i1];
            int d2 = ag.docs[i2];

            if(d1 < d2) {
                //
                // It's in our set, but not in the other, so it's in the result.
                ret.docs[ret.size++] = d1;
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
    public ArrayGroup mult(float m) {
        return this;
    }

    protected void setScoreModifier(ScoreModifier sm) {
        this.sm = sm;
    }

    /**
     * Adds a passage to the passages stored for the given field.  The
     * passage is added to the document currently at the end of the set.
     */
    protected void addPassage(int field, int[] newPass, float penalty) {
        addPassage(size, field, newPass, penalty);
    }

    /**
     * Adds a passage to the passages stored for the given field for the
     * document stored at the given position.
     */
    protected void addPassage(int pos, int field, int[] newPass, float penalty) {
        if(pass == null) {
            pass = new PassageStore[field + 1];
        } else if(pass.length <= field) {
            PassageStore[] temp = new PassageStore[field + 1];
            System.arraycopy(pass, 0, temp, 0, pass.length);
            pass = temp;
        }

        if(pass[field] == null) {
            pass[field] = new PassageStore(this);
        }
        pass[field].add(pos, newPass, penalty);
    }

    public Map getPassages(int doc) {
        Map ret = new HashMap();

        //
        // No passages!
        if(pass == null) {
            return ret;
        }

        int pos = Util.binarySearch(docs, 0, size, doc);

        //
        // The document isn't in the set!
        if(pos < 0) {
            return ret;
        }

        for(int i = 0; i < pass.length; i++) {
            if(pass[i] == null) {
                continue;
            }
            if(part instanceof InvFileDiskPartition) {
//                ret.put(((InvFileDiskPartition) part).getFieldStore().
//                        getFieldName(i), pass[i]);
            }
        }
        return ret;
    }

    /**
     * Gets the partition that generated this group.
     */
    public DiskPartition getPartition() {
        return part;
    }

    /**
     * Sets the partitoin for this group.
     */
    public void setPartition(DiskPartition part) {
        this.part = part;
    }

    /**
     * Gets an iterator that will return each document in the set.
     */
    public DocIterator iterator() {
        return new DocIterator();
    }

    /**
     * Clones this group.
     *
     * @return a clone of this group.  We will clone the internal arrays.
     */
    public Object clone() {
        ArrayGroup result = null;
        try {
            result = (ArrayGroup) super.clone();
        } catch(CloneNotSupportedException e) {
            throw new InternalError();
        }
        result.docs = docs.clone();
        if(pass != null) {
            result.pass = pass.clone();
        }
        return result;
    }

    /**
     * Tests two array groups for equality.  Two groups are equal if they
     * contain exactly the same set of documents.
     *
     * @return <code>true</code> if the groups contain the same documents,
     * <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof ArrayGroup)) {
            return false;
        }

        ArrayGroup ag = (ArrayGroup) o;
        if(size != ag.size) {
            return false;
        }

        for(int i = 0; i < size; i++) {
            if(docs[i] != ag.docs[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(size * 3);
        sb.append('[');
        for(int i = 0; i < size; i++) {
            sb.append(i == 0 ? "" : ", ").append(docs[i]);
        }

        sb.append("] ").append(size).append(" docs");
        return sb.toString();
    }

    /**
     * A class that provides an iterator for the documents in this group.
     */
    public class DocIterator implements Comparable<DocIterator>, ResultAccessor {

        /**
         * The current position in the set.
         */
        int pos = -1;

        public DocIterator() {
            fetchers = new HashMap<String, Fetcher>();
        }

        /**
         * Advance to the next position, skipping deleted documents.
         *
         * @return <code>true</code> if there is a next position,
         * <code>false</code> otherwise.
         */
        public boolean next() {
            while(pos < size - 1) {
                pos++;
                if(part == null || !part.isDeleted(docs[pos])) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Gets the document at the head of the iterator.
         */
        public int getDoc() {
            return docs[pos];
        }

        /**
         * Gets the score at the head of the iterator.  In this case,
         * always returns 1.
         */
        public float getScore() {
            return 1;
        }

        /**
         * Gets the partition associated with this array group.
         */
        public DiskPartition getPart() {
            return part;
        }

        /**
         * Compares this iterator to another.  The comparison is done by
         * document ID.
         */
        public int compareTo(DocIterator o) {
            return getDoc() - o.getDoc();
        }

        //
        // Implementation of ResultAccessor
        private Map<String, Fetcher> fetchers;

        public String getKey() {
            return part.getDocumentDictionary().getByID(docs[pos]).getName().toString();
        }

        private Fetcher getFetcher(String field) {
            Fetcher f = fetchers.get(field);
            if(f == null) {
                fetchers.put(field,
                        ((InvFileDiskPartition) part).getDF(field).getFetcher());
            }
            return f;
        }

        public List<Object> getField(String field) {
            Fetcher f = getFetcher(field);
            return f == null ? Collections.emptyList() : f.fetch(docs[pos]);
        }

        public List<Object> getField(String field, List<Object> l) {
            l.clear();
            Fetcher f = getFetcher(field);
            return f == null ? Collections.emptyList() : f.fetch(docs[pos], l);
        }

        public Object getSingleFieldValue(String field) {
            Fetcher f = getFetcher(field);
            return f == null ? null : f.fetchOne(docs[pos]);
        }
    }
} // ArrayGroup
