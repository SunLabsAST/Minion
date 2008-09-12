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
import java.util.Comparator;
import java.util.List;

import com.sun.labs.minion.QueryConfig;

import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;

import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.Util;

/**
 * An abstract base class for all of the term and operator classes that
 * take part in the evaluation of a query.
 *
 */
public abstract class QueryElement implements Comparable {

    /**
     * A partition upon which retrieval will be performed.
     */
    protected DiskPartition part;

    /**
     * A set of query statistics for this query.
     */
    protected QueryStats qs;

    /**
     * The query configuration for this particular query.
     */
    protected QueryConfig qc;

    /**
     * The weighting function to use for this query.
     */
    protected WeightingFunction wf;

    /**
     * A set of weighting components to use for this query.
     */
    protected WeightingComponents wc;

    /**
     * The estimated size of the result set for this element, for the
     * current partition. If this value is &lt; 0, then no size has been
     * estimated yet.
     */
    protected int estSize;

    /**
     * The original order of this element in the enclosing query. Order is
     * numbered from 0.
     */
    protected int order;

    /**
     * Whether proximity operators require the terms to be in order.
     */
    public boolean inOrder;

    /**
     * Whether we need to keep the positions of terms that we find.
     */
    public boolean keepPositions;

    /**
     * Whether we're evaluating in a strict boolean context.
     */
    public boolean strictEval;

    /**
     * The names of the fields to which search should be restricted.
     */
    protected String[] searchFieldNames;

    /**
     * An array of fields that can be used for getting postings iterators
     * that are field based.  This will be set per-partition.
     */
    protected int[] searchFields;

    /**
     * An array of field multipliers that can be used for getting postings
     * iterators that will apply multipliers.  This will be set
     * per-partition.
     */
    protected float[] fieldMultipliers;

    protected static MinionLog log = MinionLog.getLog();

    /**
     * Sets the current partition, and makes sure that we have valid search
     * field and multiplier arrays for this partition.  This should be
     * propagated to any components of the query element!
     */
    protected void setPartition(DiskPartition part) {

        this.part = part;
        estSize = calculateEstimatedSize();

        //
        // We only need to set up the search fields and multipliers once.
        if(searchFields == null) {
            if (part instanceof InvFileDiskPartition) {
                InvFileDiskPartition ifpart = (InvFileDiskPartition) part;
                if (searchFieldNames == null) {
                    //
                    // Set up any default fields if there are none specified in
                    // the query.
                    List<FieldInfo> df = qc.getDefaultFields();
                    if (df.size() > 0) {
                        searchFieldNames = new String[df.size()];
                        for (int i = 0; i < df.size(); i++) {
                            searchFieldNames[i] = df.get(i).getName();
                        }
                    }
                }
                searchFields = ifpart.getFieldStore().getFieldArray(searchFieldNames);
                fieldMultipliers = ifpart.getFieldStore().getMultArray(qc.getMultFields(),
                        qc.getMultValues());
            } else {
                searchFields = new int[0];
                fieldMultipliers = new float[0];
            }
        }
    }

    /**
     * Sets the current query configuration.  This should be propagated to
     * any components of the query element!
     */
    public void setQueryConfig(QueryConfig qc) {
        this.qc = qc;
    }

    /**
     * Sets the query statistics.
     */
    public void setQueryStats(QueryStats qs) {
        this.qs = qs;
    }

    /**
     * Gets the set of query stats for this element.
     */
    public QueryStats getQueryStats() {
        return qs;
    }

    /**
     * Sets the current weighting function.  This should be propagated to
     * any components of the query element.
     */
    protected void setWeightingFunction(WeightingFunction wf) {
        this.wf = wf;
    }

    /**
     * Sets the current weighting components.  This should be propagated to
     * any components of the query element.
     */
    protected void setWeightingComponents(WeightingComponents wc) {
        this.wc = wc;
        this.wc.setCollection(qc.getCollectionStats());
    }

    /**
     * Estimates the size of the results set.  This method simply returns
     * the pre-calculated estimated size.
     *
     * @return The estimated size of the results set.  This is meant to be
     * an as-tight-as-possible estimate of the results set size.  It is
     * acceptable to overestimate, but not to underestimate the results set
     * size.
     */
    public int estimateSize() {
        return estSize;
    }

    /**
     * Calculates the estimated return set size.
     *
     * @return The estimated size of the results set for this element.
     * This is meant to be an as-tight-as-possible estimate of the results
     * set size.  It is acceptable to overestimate, but not to
     * underestimate the size of the result set.
     */
    protected abstract int calculateEstimatedSize();

    /**
     * Gets the order of this element.
     *
     * @return The order of this element in the query.
     */
    public int getOrder() {
        return order;
    }

    /**
     * Sets the order of this element.
     *
     * @param order The order of this element in the query.
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * Determines whether this element must have operands in the correct
     * sequence.
     */
    public boolean getInOrder() {
        return inOrder;
    }

    /**
     * Sets the in-order requirement for operands.
     */
    public void setInOrder(boolean inOrder) {
        this.inOrder = inOrder;
    }


    /** 
     * Returns an array of field names to which search should be
     * restricted.
     * @return a clone of the internal array of field names
     */
    public String[] getSearchFieldNames() {
        if (searchFieldNames == null) {
            return new String[0];
        }
        return searchFieldNames.clone();
    }


    /** 
     * Adds a field name to which the search should should be restricted
     *
     * @param fieldName the name of the field
     */
    public void addSearchFieldName(String fieldName) {
        if (searchFieldNames == null) {
            searchFieldNames = new String[1];
            searchFieldNames[0] = fieldName;
        } else {
            String[] str = new String[searchFieldNames.length + 1];
            for (int i = 0; i < searchFieldNames.length; i++) {
                str[i] = searchFieldNames[i];
            }
            str[str.length - 1] = fieldName;
            searchFieldNames = str;
        }
    }


    /** 
     * Returns a list of all the regular terms in this query in the order
     * that they appeared in the query.  Field Terms and terms within a
     * not or a hide expression will not be returned.
     */
    public List getQueryTerms() {
        return getQueryTerms(new TermOrderer());
    }

    /** 
     * Returns a list of all the regular terms in this query in the order
     * that they appeared in the query.  Field Terms and terms within a
     * not or a hide expression will not be returned.
     *
     * @param c A comparator used to order the terms.
     */
    protected abstract List getQueryTerms(Comparator c);
    
    /**
     * Compares a query element to another, based on its estimated size.
     * This can be used to sort elements for more efficient processing.
     * Note that 
     */
    public int compareTo(Object o) {
        return estimateSize() - ((QueryElement) o).estimateSize();
    }

    /**
     * Evaluates this query element.
     */
    public ArrayGroup eval(ArrayGroup ag) {
        return new ArrayGroup(0);
    }

    public String toString() {
        return toString("");
    }

    public String toString(String prefix) {
        String name = getClass().getName();
        String fields = "";
        if (searchFieldNames != null) {
            for (int i=0; i < searchFieldNames.length; i++) {
                fields = fields + searchFieldNames[i] + (i != searchFieldNames.length - 1 ? ", " : "");
            }
        }
        return prefix + name.substring(name.lastIndexOf('.') + 1) + (!fields.equals("")?" (fields: " + fields + ") ":"");
    }

    public void dump(String prefix) {
        System.out.println(toString(prefix));
    }
    
} // QueryElement
