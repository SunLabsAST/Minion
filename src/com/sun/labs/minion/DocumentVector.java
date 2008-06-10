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

package com.sun.labs.minion;

import java.util.Map;
import java.util.Set;

/**
 * An interface defining the behavior of document vectors.  These are the
 * basis for all classification, clustering, and profiling activity.  A
 * document vector can be obtained from a search result using the {@link
 * Result#getDocumentVector} method.
 *
 * <p>
 *
 * The name is a bit misleading:  an instance of this class can be used to
 * represent a set of documents as easily as it can a single document.
 */
public interface DocumentVector extends Cloneable {

    /**
     * Creates a copy of the current document vector and returns it.
     * @return a copy of the current document vector
     */
    public DocumentVector copy();
    
    /**
     * Sets the search engine to use with this document vector.
     * @param e the engine
     */
    public void setEngine(SearchEngine e);

    /** 
     * Determines of two document vectors are equal.  Document vectors are
     * equal each of their terms is equal in both name and weight.
     * 
     * @param o the document vector to which this vector is compared
     * @return true if the two document vectors are equal, false otherwise
     */
    public boolean equals(Object o);

    /** 
     * Computes the similarity between this document vector and the
     * supplied vector.  Similarity is as how small or large the angle
     * between the two vectors is.  The measurement returned is the cosine
     * of the angle between the vectors.
     * 
     * @param vector the vector representing the document to compare this vector to
     * @return the cosine of the angle between the two vectors
     */
    public float getSimilarity(DocumentVector vector);

    /** 
     * Finds documents that are similar to this one.
     * 
     * @return documents similar to the one this vector represents
     */
    public ResultSet findSimilar();

    /** 
     * Finds documents that are similar to this one.
     *
     * @param sortOrder a string describing the order in which to sort the results
     * @return documents similar to the one this vector represents
     */
    public ResultSet findSimilar(String sortOrder);
    
    /** 
     * Finds documents that are similar to this one.
     *
     * @param sortOrder a string describing the order in which to sort the results
     * @param skimPercent a number between 0 and 1 representing what percent of the features should be used to perform findSimilar
     * @return documents similar to the one this vector represents
     */
    public ResultSet findSimilar(String sortOrder, double skimPercent);
    
    /**
     * Gets the key for the document associated with this vector.
     *
     * @return the key for this document.
     */
    public String getKey();
    
    
    /**
     * Gets the set of terms in the document represented by this vector.
     * 
     * @return a set of the terms in the document.
     */
    public Set<String> getTerms();

    /** 
     * Gets the n terms that have the highest document weight in this
     * document vector.  The results are expressed as a HashMap from
     * String term names to Float term weights.  The term weights are
     * normalized.
     * 
     * @param nTerms the number of terms to return
     * @return a HashMap from Strings to Floats with at most nTerms terms (fewer if the document contains fewer than nTerms terms)
     */
    public Map<String,Float> getTopWeightedTerms(int nTerms);

    /** 
     * Gets a HashMap of term names to weights, where the weights
     * represent the amount the term contributed to the similarity
     * of the two documents.  Only terms that occur in both documents
     * are returned, as all other terms have weight zero.  The keys
     * in the HashMap are sorted according to the natural ordering
     * of their values.  That is, the first string returned from
     * an iterator over the key set will be the term with the
     * highest weight.
     * 
     * @param vector the document vector to compare this one to
     * @return a sorted hash map of String names to Float weights
     */
    public Map<String,Float> getSimilarityTerms(DocumentVector vector);
    
}// DocumentVector
