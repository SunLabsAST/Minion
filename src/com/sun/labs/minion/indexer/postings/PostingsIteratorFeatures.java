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

import com.sun.labs.minion.QueryStats;


import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;

/**
 * A set of features that can be used to configure various aspects of a
 * postings iterator.
 */
public class PostingsIteratorFeatures implements Cloneable {

    protected QueryStats qs;
    
    /**
     * The weighting function that the iterator will use to calculate
     * weights.
     */
    protected WeightingFunction wf;

    /**
     * A set of weighting components that can be used when calculating term
     * weights.
     */
    protected WeightingComponents wc;

    /**
     * The multiplier that we wish to apply to scores.
     */
    protected float multiplier;

    /**
     * Whether the iterator must be able to return positions.  If
     * <code>true</code>, then the iterator must be capable of returning
     * positions.
     */
    protected boolean positions;

    /**
     * Whether the iterator will return postings for case-sensitive or case
     * insensitive variations.
     */
    protected boolean caseSensitive;

    /**
     * Creates a default set of features.
     */
    public PostingsIteratorFeatures() {
    }

    /**
     * Creates a set of features.
     *
     * @param wf A weighting function to use for these postings.  If
     * <code>null</code>, no weighting function will be used.
     * @param wc a collection of components that can be used when
     * calculating weighting functions.
     *
     */
    public PostingsIteratorFeatures(WeightingFunction wf,
                                    WeightingComponents wc) {
        this(wf, wc, 1.0f, false);
    } // PostingsIteratorFeatures constructor


    /**
     * Creates a set of features.
     *
     * @param wf A weighting function to use for these postings.  If
     * <code>null</code>, no weighting function will be used.
     * @param wc a collection of components that can be used when
     * calculating weighting functions.
     * @param fields If the <em>i<sup>th</sup></em> element of this field
     * is greater than zero, then the iterator will return postings for the
     * field whose ID is <em>i</em>.  If element 0 of the array is greater
     * than zero, then postings that are not in any field will be
     * returned.
     * @param mult An array of floats indicating the field IDs for which we
     * want to apply multipliers.  The value in the <em>i<sup>th</sup></em>
     * position indicates the weight that will be used to multiply the
     * score of a term that has occurrences in the field whose ID is
     * <em>i</em>.  If this array is <code>null</code>, then no multipliers
     * will be applied.
     * @param positions if <code>true</code>, then iterators created using
     * this set of features must be able to return positions.
     * @param caseSensitive if <code>true</code> then the iterator that is
     * returned will be for case sensitive postings.  If <code>false</code>
     * then the iterator that is returned will be for case insensitive
     * postings.
     */
    public PostingsIteratorFeatures(WeightingFunction wf,
                                    WeightingComponents wc,
                                    float mult,
                                    boolean positions) {
        
        this.wf            = wf;
        this.wc            = wc;
        this.multiplier          = mult;
        this.positions     = positions;
    }

    /**
     * Sets the weighting function to be used by iterators.
     */
    public void setWeightingFunction(WeightingFunction wf) {
        this.wf = wf;
    }

    /**
     * Gets the weighting function to be used by iterators.
     */
    public WeightingFunction getWeightingFunction() {
        return wf;
    }

    /**
     * Sets the weighting components to be used by iterators.
     */
    public void setWeightingComponents(WeightingComponents wc) {
        this.wc = wc;
    }

    /**
     * Gets the weighting components to be used by iterators.
     */
    public WeightingComponents getWeightingComponents() {
        return wc;
    }

    public void setQueryStats(QueryStats qs) {
        this.qs = qs;
    }

    public QueryStats getQueryStats() {
        return qs;
    }

    /**
     * Sets the value of the positions.
     *
     * @param positions if <code>true</code>, then iterators created using
     * this set of features must be able to return positions.
     */
    public void setPositions(boolean positions) {
        this.positions = positions;
    }

    /**
     * Gets whether this set of features requires that iterators be able to
     * return positions.
     */
    public boolean getPositions() {
        return positions;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Sets the fied multipliers that we should be using.
     */
    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }

    public float getMultiplier() {
        return multiplier;
    }

    /**
     * Clones this set of features.
     */
    public Object clone() {
        PostingsIteratorFeatures result = null;
        try {
            result = (PostingsIteratorFeatures) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        return result;
    }

} // PostingsIteratorFeatures
