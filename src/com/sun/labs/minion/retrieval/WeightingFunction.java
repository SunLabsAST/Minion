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

/**
 * An interface for a term weighting function that can be used during
 * retrieval, classification, and profiling operations.  Classes that
 * implement this interface will be used in two distinct ways:
 *
 * <ol>
 *
 * <li> During standard query processing, terms will be retrieved from the
 * dictionary and their postings lists will be processed.  In this case,
 * when each term is retrieved from the dictionary, the
 * <code>initTerm</code> method will be called with the term statistics for
 * the term.  This should result in the calculation and caching of any
 * collection-level weight for the term.  Once initTerm has been called,
 * the {@link #termWeight(WeightingComponents)} method will be called repeatedly for
 * each element in the postings list associated with the term.</li>
 *
 * <li> During classification operatons and during the calculation of
 * document vector lengths at document dictionary dump and merge time, we
 * will call the {@link #termWeight(WeightingComponents)} method
 * repeatedly to calculate the weights associated with each term in the
 * document.
 *
 * </ol>
 *
 *
 * @see TFIDF
 * @see Okapi
 */
public interface WeightingFunction {

    /**
     * Initializes the weighting function for a particular term.  It is
     * expected that this method will use the weighting components to
     * calculate some collection level weight for the term that will be
     * used repeatedly during the processing of a postings list associated
     * with the term.
     *
     * <p>
     *
     * If a collection level weight is calculated as part of a weighting
     * function, it <em>must</em> be placed into the
     * <code>WeightingComponents.wt</code> member.  During query processing
     * any calls to <code>termWeight</code> that follow the call to
     * <code>initTerm</code> for a given term are guaranteed to pass in the
     * same <code>WeightingComponents</code> object, so it can be used to
     * safely cache the collection-level weights.  Additionally, it will be
     * safe to subclass <code>WeightingComponents</code> if necessary.
     *
     * <p>
     *
     * Note that the term weight computed by this method may be used as the
     * weight of the terms in a query, so if no such weight needs to be
     * calculated for a given implementation, a value of 1 should be
     * used.
     *
     * @param wc a set of weighting components.
     * @return the collection-level weight associated with this term.
     * 
     * @see WeightingComponents#wt
     */
    public float initTerm(WeightingComponents wc);

    /**
     * Calculates the weight for a particular term in a particular
     * document, given a set of weighting components.
     *
     * @param wc a set of weighting components.
     * @return the weight of the given term in the given document.
     */
    public float termWeight(WeightingComponents wc);


}// WeightingFunction
