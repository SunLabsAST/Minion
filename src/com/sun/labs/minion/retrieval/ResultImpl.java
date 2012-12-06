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

import com.sun.labs.minion.Document;
import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.PassageBuilder;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultAccessor;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.WeightedField;
import com.sun.labs.minion.engine.DocumentImpl;
import com.sun.labs.minion.indexer.HighlightDocumentProcessor;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class ResultImpl implements Result, Comparable<Result>, Cloneable,
        ResultAccessor {
    /**
     * The set of results to which this result belongs.
     */
    protected ResultSet set;

    /**
     * The array group containing this result.
     */
    protected ArrayGroup ag;
    
    /**
     * The partition backing this result.
     */
    protected DiskPartition partition;

    /**
     * The query that generated this result.
     */
    protected QueryElement el;

    /**
     * The ID of the document containing the hit.
     */
    protected int doc;

    /**
     * The score the document received.
     */
    protected float score;

    /**
     * The sorting specification for this result.
     */
    protected SortSpec sortSpec;
    
    /**
     * Whether we're sorting local to a partition or global to an index.
     */
    protected boolean localSort;

    /**
     * A map from field names to the passage stores for those fields.  Used
     * for passage highlighting.
     */
    protected Map passages;

    /**
     * The actual field values used to sort this document.
     */
    protected Object[] sortFieldValues;
    
    /**
     * The IDs of the field values used to sort.  These can be used to sort
     * more quickly at the local partition.
     */
    protected int[] sortFieldIDs;
    
    /**
     * A set of query statistics.
     */
    protected QueryStats qs;

    private static final Logger logger = Logger.getLogger(ResultImpl.class.getName());

    /**
     * Creates an empty search result.
     */
    public ResultImpl() {
    }

    /**
     * Creates a search result.
     *
     * @param set The set of results to which this result belongs.
     * @param ag the array group to which this result belongs
     * @param sortSpec the sort order being applied to the result set
     * @param localSort if true, we're computing sort criteria that are local to
     * a single partition so that sorting can be done on IDs.
     * @param doc The document ID for the hit.
     * @param score The score the document received.
     */
    public ResultImpl(ResultSet set,
            ArrayGroup ag,
            SortSpec sortSpec,
            boolean localSort,
            int doc,
            float score) {
        init(set, ag, sortSpec, localSort, doc, score);
    }

    /**
     * Initializes the members of this result.  Needed so that we can
     * refill results rather than generating new ones when processing large
     * document sets.
     *
     * @param set The set of results to which this result belongs.
     * @param ag the array group to which this result belongs
     * @param sortSpec the sort order being applied to the result set
     * @param localSort if <code>true</code> then we're doing a sort on a local
     * partition only, so we just have to consider the IDs of the saved values.
     * @param doc The document ID for the hit.
     * @param score The score the document received.
     */
    protected final void init(ResultSet set,
            ArrayGroup ag,
            SortSpec sortSpec,
            boolean localSort,
            int doc,
            float score) {
        this.set = set;
        this.ag = ag;
        if(ag != null) {
            partition = ag.getPartition();
        }
        this.sortSpec = sortSpec;
        this.localSort = localSort;
        this.doc = doc;
        this.score = score;
        

        if(sortSpec != null && ag != null && partition instanceof InvFileDiskPartition) {

            //
            // Fill in the field IDs or values that we're sorting on.
            if(localSort) {
                if(sortFieldIDs == null) {
                    sortFieldIDs = new int[sortSpec.size];
                }
                for(int i = 0; i < sortFieldIDs.length; i++) {
                    sortSpec.getSortFieldID(i, sortFieldIDs, doc);
                }
            } else {
                if(sortFieldValues == null) {
                    sortFieldValues = new Object[sortSpec.size];
                }
                for(int i = 0; i < sortFieldValues.length; i++) {
                    sortFieldValues[i] = null;
                }
            }
        }
    }

    public void setArrayGroup(ArrayGroup ag) {
        this.ag = ag;
    }

    /**
     * Sets the partition for this result.
     * @param partition the partition that generated this result.
     */
    public void setPartition(DiskPartition partition) {
        this.partition = partition;
    }

    /**
     * Gets the document key entry associated with this document.
     * @return the doc key entry
     */
    public QueryEntry getKeyEntry() {
        return partition.getDocumentDictionary().getByID(doc);
    }

    /**
     * Gets the document key associated with the document represented by
     * this result.
     */
    @Override
    public String getKey() {
        return (String) partition.getDocumentDictionary().getByID(doc).getName();
    }

    /**
     * Gets the value of the saved field from the field store.
     *
     * @return an <code>Object</code> of the appropriate type.
     */
    @Override
    public List getField(String field) {
        return (List) ((InvFileDiskPartition) partition).getSavedFieldData(field,
                doc, true);
    }

    @Override
    public Object getSingleFieldValue(String field) {
        return ((InvFileDiskPartition) partition).getSavedFieldData(field, doc,
                false);
    }

    @Override
    public boolean containsAny(String field, int[] ids) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsAll(String field, int[] ids) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Document getDocument() {
        QueryEntry dke = partition.getDocumentDictionary().getByID(doc);

        //
        // If there's no such key, then return
        // null, since we don't have this document.
        if(dke == null) {
            logger.severe(String.format("No document term for result: %s (docid: %d)", partition, doc));
            return null;
        }

        //
        // Back the existing document with the index.
        return new DocumentImpl(dke);
    }

    /**
     * Gets the document vector corresponding to this result.  The document
     * vector will be built from data for all the vectored fields in the document.
     * 
     */
    @Override
    public DocumentVector getDocumentVector() {
        Set<FieldInfo> defaultFields = set.getEngine().getDefaultFields();
        if(defaultFields.size() == 1) {
            return new SingleFieldDocumentVector(this, defaultFields.iterator().next());
        } else {
            WeightedField[] wf = new WeightedField[defaultFields.size()];
            int i = 0;
            for(FieldInfo fi : defaultFields) {
                wf[i] = new WeightedField(fi, 1f);
            }
            return new MultiFieldDocumentVector(this, wf);
        }
    }

    /**
     * Gets the document vector corresponding to this result.
     */
    @Override
    public DocumentVector getDocumentVector(FieldInfo field) {
        return new SingleFieldDocumentVector(this, field);
    }

    @Override
    public DocumentVector getDocumentVector(WeightedField[] fields) {
        if(fields.length == 1) {
            
            //
            // One weighted field? It's just a scaling, so ignore the weight.
            return new SingleFieldDocumentVector(this, fields[0].getField());
        } else {
            return new MultiFieldDocumentVector(this, fields);
        }
    }

    /**
     * Returns an iterator for all of the field values in a result.
     */
    @Override
    public Iterator getFieldIterator() {
        return null;
    }

    /**
     * Gets the score associated with this result.
     */
    @Override
    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public void setQueryStats(QueryStats qs) {
        this.qs = qs;
    }

    public int getDocID() {
        return doc;
    }

    public int getPartNum() {
        return partition.getPartitionNumber();
    }

    public DiskPartition getPart() {
        return (DiskPartition) partition;
    }

    /**
     * Gets the number of passages associated with this result.
     */
    @Override
    public int getNPassages() {
        return 0;
    }

    /**
     * Gets the name of the index that this hit was drawn from.  This is a
     * configuration parameter that is passed to the
     * <code>SearchEngine</code> at startup time.
     */
    @Override
    public String getIndexName() {
        return set.getEngine().getName();
    }

    /**
     * Gets a passage builder for this result.  This will find the actual.
     * passages.
     *
     * @return <code>null</code> if there are no query terms suitable for
     * passage highlighting.
     */
    @Override
    public PassageBuilder getPassageBuilder() {

        if(ag.queryTerms == null || ag.queryTerms.isEmpty()) {
            return null;
        }

        //
        // We'll make a passage operator with our query terms, set the
        // partition, then do a quick retrieval.
        Passage pass = new Passage(ag.queryTerms);
        pass.setQueryConfig(((ResultSetImpl) set).e.getQueryConfig());
        pass.setQueryStats(new QueryStats());
        pass.setPartition(partition);
        pass.setStorePassages(true);

        //
        // We'll modify the penalties in order to get hits on all terms.
        pass.setGap(0.001f);
        pass.setOOO(0.0025f);
        pass.setWindow(750);

        //
        // Go ahead and eval it.
        ArrayGroup pa = pass.eval(doc);
        pa.part = partition;
        List<QueryTerm> queryTerms = pass.getQueryTerms();

        //
        // Fetch out the query terms.
        String[] qts = new String[queryTerms.size()];
        int p = 0;
        for(QueryTerm qt : queryTerms) {
            qts[p++] = ((DictTerm) qt).getName();
        }

        //
        // Get a processor for highlighting these passages.
        HighlightDocumentProcessor hdp = ((ResultSetImpl) set).getHighlightProcessor();
        hdp.reset(pa, doc, qts);
        return hdp;
    }

    /**
     * Gets all of the sort field values, and sets the flag that says that's 
     * what we're comparing by.
     */
    protected void setSortFieldValues() {
        if(sortSpec != null) {
            localSort = false;
            sortFieldValues = new Object[sortSpec.size];
            sortSpec.getSortFieldValues(sortFieldValues, doc, score, null);
        }
    }

    @Override
    public int compareTo(Result o) {

        ResultImpl r = (ResultImpl) o;
        if(sortSpec == null || sortSpec.isJustScoreSort()) {
            return SortSpec.compareScore(score, r.score, SortSpec.Direction.DECREASING);
        }
        
        //
        // If we're local to a partition, sort based on the field IDs.
        if(localSort) {
            return sortSpec.compareIDs(sortFieldIDs, r.sortFieldIDs, score, r.score);
        } else {
            return sortSpec.compareValues(sortFieldValues, r.sortFieldValues, doc, score, r.doc, r.score, null, null);
        }
    }
    
    /**
     * A comparator that reverses the result of the comparison for a pair of 
     * results.  This can be used when we want to generate a min-heap of
     * results during results sorting.
     */
    public static final Comparator REVERSE_COMPARATOR =
            new Comparator<ResultImpl>() {
                @Override
                public int compare(ResultImpl o1,
                                   ResultImpl o2) {
                    return -o1.compareTo(o2);
                }
            };

    /** 
     * Two results are equal if they represent the same document.
     * 
     * @param o the object to test for equality
     * @return true if the receiver and the argument are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if(!(o instanceof ResultImpl)) {
            return false;
        }

        ResultImpl other = (ResultImpl) o;
        return partition == other.partition && doc == other.doc;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash +
                (this.partition != null ? this.partition.hashCode() : 0);
        hash = 61 * hash + this.doc;
        return hash;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(partition).append(", ").append(doc).append(", ").
                append(score).append(", [");
        if(localSort) {
            if(sortFieldIDs != null) {
                for(int i = 0; i < sortFieldIDs.length; i++) {
                    if(i > 0) {
                        sb.append(", ");
                    }
                    sb.append(sortFieldIDs[i]);
                }
            }
        } else {
            if(sortFieldValues != null) {
                for(int i = 0; i < sortFieldValues.length; i++) {
                    if(i > 0) {
                        sb.append(", ");
                    }
                    sb.append(sortFieldValues[i].toString());
                }
            }
        }
        sb.append("])");
        return sb.toString();
    }

    public SearchEngine getEngine() {
        return set.getEngine();
    }

    public SortSpec getSortSpec() {
        return sortSpec;
    }

    /**
     * Clones this group.
     *
     * @return a clone of this group.  We will clone the internal arrays.
     */
    @Override
    public Object clone() {
        ResultImpl result = null;
        try {
            result = (ResultImpl) super.clone();
        } catch(CloneNotSupportedException e) {
            throw new InternalError();
        }
        return result;
    }
} // ResultImpl

