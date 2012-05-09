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
import com.sun.labs.minion.HLPipeline;
import com.sun.labs.minion.PassageBuilder;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultAccessor;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.WeightedField;
import com.sun.labs.minion.engine.DocumentImpl;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
     * The query that generated this result.
     */
    protected QueryElement el;

    /**
     * The ID of the document containing the hit.
     */
    protected int doc;

    /**
     * The score the docuemnt received.
     */
    protected float score;

    /**
     * The sorting specification for this result.
     */
    protected SortSpec sortSpec;

    /**
     * A map from field names to the passage stores for those fields.  Used
     * for passage highlighting.
     */
    protected Map passages;

    /**
     * The field values used to sort this document.
     */
    protected Object[] fields;

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
     * @param doc The document ID for the hit.
     * @param score The score the document received.
     */
    public ResultImpl(ResultSet set,
            ArrayGroup ag,
            SortSpec sortSpec,
            int doc,
            float score) {
        init(set, ag, sortSpec, doc, score);
    }

    /**
     * Initializes the members of this result.  Needed so that we can
     * refill results rather than generating new ones when processing large
     * document sets.
     *
     * @param set The set of results to which this result belongs.
     * @param ag the array group to which this result belongs
     * @param sortSpec the sort order being applied to the result set
     * @param doc The document ID for the hit.
     * @param score The score the document received.
     */
    protected void init(ResultSet set,
            ArrayGroup ag,
            SortSpec sortSpec,
            int doc,
            float score) {
        this.set = set;
        this.ag = ag;
        this.doc = doc;
        this.score = score;
        this.sortSpec = sortSpec;

        if(sortSpec != null && ag != null && ag.part instanceof InvFileDiskPartition) {

            //
            // Fill in the field values that we're sorting on.
            if(fields == null) {
                fields = new Object[sortSpec.size];
            } else {
                for(int i = 0; i < fields.length; i++) {
                    fields[i] = null;
                }
            }
        }
    }

    /**
     * Gets the document key entry associated with this document.
     * @return the doc key entry
     */
    public QueryEntry getKeyEntry() {
        return ag.part.getDocumentDictionary().getByID(doc);
    }

    /**
     * Gets the document key associated with the document represented by
     * this result.
     */
    @Override
    public String getKey() {
        return (String) ag.part.getDocumentDictionary().getByID(doc).getName();
    }

    /**
     * Gets the value of the saved field from the field store.
     *
     * @return an <code>Object</code> of the appropriate type.
     */
    @Override
    public List getField(String name) {
        return (List) ((InvFileDiskPartition) ag.part).getSavedFieldData(name,
                doc, true);
    }

    @Override
    public Object getSingleFieldValue(String name) {
        return ((InvFileDiskPartition) ag.part).getSavedFieldData(name, doc,
                false);
    }

    @Override
    public Document getDocument() {
        QueryEntry dke = ag.part.getDocumentDictionary().getByID(doc);

        //
        // If there's no such key, then return
        // null, since we don't have this document.
        if(dke == null) {
            logger.severe("No document term for result: " + ag.part + " " + doc);

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
        return new SingleFieldDocumentVector(this, null);
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
        return ag.part.getPartitionNumber();
    }

    public DiskPartition getPart() {
        return ag.part;
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
        pass.setPartition(ag.part);
        pass.setStorePassages(true);

        //
        // We'll modify the penalties in order to get hits on all terms.
        pass.setGap(0.001f);
        pass.setOOO(0.0025f);
        pass.setWindow(750);

        //
        // Go ahead and eval it.
        ArrayGroup pa = pass.eval(doc);
        pa.part = ag.part;
        List queryTerms = pass.getQueryTerms();

        //
        // Fetch out the query terms.
        String[] qt = new String[queryTerms.size()];
        int p = 0;
        for(Iterator i = queryTerms.iterator(); i.hasNext();) {
            qt[p++] = ((DictTerm) i.next()).val;
        }

        //
        // Get a pipeline that can highlight these passages.
        HLPipeline hlp = ((ResultSetImpl) set).getHLPipeline();
        hlp.reset(pa, doc, qt);
        return hlp;
    }

    /**
     * Gets a single field value, suitable for use in sorting results.
     *
     * @param i The index of the field in our sort specification whose
     * value we want to retrieve.
     */
    protected void getFieldValue(int i) {

        //
        // A field with a zero ID indicates that we're sorting by score.
        if(sortSpec.fields[i] != null && sortSpec.fields[i].getID() == 0) {
            fields[i] = new Float(score);
            return;
        }

        //
        // Get a single field value to use for the sort.
        if(sortSpec.fetchers[i] != null) {
            fields[i] = sortSpec.fetchers[i].fetchOne(doc);
        } else {
            fields[i] = null;
        }

        //
        // Get the default value for the field.
        if(fields[i] == null) {
            fields[i] = sortSpec.fields[i].getDefaultSavedValue();
        }
    }

    protected void setFields() {
        for(int i = 0; i < fields.length; i++) {
            getFieldValue(i);
        }
    }

    @Override
    public int compareTo(Result o) {

        ResultImpl r = (ResultImpl) o;
        if(fields == null) {
            if(score < r.score) {
                return 1;
            }
            if(score > r.score) {
                return -1;
            }
            return 0;
        }

        //
        // Do the sort based on the fields.
        for(int i = 0; i < fields.length; i++) {

            //
            // Make sure we have this field value in both results.
            if(fields[i] == null) {
                getFieldValue(i);
            }

            if(r.fields[i] == null) {
                r.getFieldValue(i);
            }

            //
            // Compare the field values.
            int cmp = ((Comparable) fields[i]).compareTo(r.fields[i]);

            //
            // No decision...
            if(cmp == 0) {
                continue;
            }

            //
            // If this field is increasing, we can just use the comparison
            // that we just got.
            return sortSpec.directions[i] ? -cmp : cmp;
        }

        return 0;
    }

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
        return ag.part == other.ag.part && doc == other.doc;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(ag.part).append(", ").append(doc).append(", ").append(score).append(", [");
        for(int i = 0; i < fields.length; i++) {
            if(i > 0) {
                sb.append(", ");
            }
            sb.append(fields[i].toString());
        }
        sb.append("]");
        return sb.toString();
    }

    public SearchEngine getEngine() {
        return set.getEngine();
    }

    public SortSpec getSortSpec() {
        return sortSpec;
    }

    public boolean[] getDirections() {
        return sortSpec.directions;
    }

    public Object[] getSortVals() {
        return fields;
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

