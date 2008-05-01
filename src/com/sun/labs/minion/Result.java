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

import java.util.Iterator;
import java.util.List;

/**
 * A class that holds a single search result.
 */
public interface Result extends Comparable<Result> {
    
    /**
     * Gets the document key associated with the document represented by
     * this result.
     * @return the key for the document represented by this result
     */
    public String getKey();
    
    /**
     * Gets the values of a saved field for the document.
     *
     * @param name The name of the field for which we want the saved
     * values.
     * @return A list containing objects of the appropriate type for the
     * named field.  If the field is not defined as a saved field, or if
     * there was no value stored for this result, then an empty list is
     * returned.
     */
    public List getField(String name);
    
    /**
     * Gets a single value of a saved field for the document.
     *
     * @param name The name of the field for which we want the saved
     * values.
     * @return An object of the appropriate type for the named field.  If multiple
     * values are stored for the named field, no guarantee is made about which of
     * the multiple values are returned.  If the given name does not name a saved
     * field or if there was no data stored for that field, then <code>null</code>
     * is returned.
     */
    public Object getSingleFieldValue(String name);
    
    /**
     * Gets a document abstraction for the document in this result.
     *
     * @return the document abstraction corresponding to this result.
     */
    public Document getDocument();
    
    /**
     * Gets a document vector corresponding to the document in this result.
     * @return the document vector for the document represented by this result
     */
    public DocumentVector getDocumentVector();
    
    /**
     * Gets a document vector corresponding to a particluar vectored field in
     * the document in this result.
     * @param field the name of a vectored field upon which the clustering
     * should be based.  A value of <code>null</code> indicates that all vectored
     * fields should be considered, while an empty string indicates that data
     * in no explicit field should be considered.
     * @return the document vector for this document where the terms in the vector are those
     * found only in the given field.  Note that the document vector returned cannot
     * be used in conjunction with a composite document vector.
     * 
     * @see #getDocumentVector(WeightedField[])
     */
    public DocumentVector getDocumentVector(String field);
    
    /**
     * Gets a composite document vector that corresponds to a linear combination of a number
     * of vectored fields in the document in this result.  The linear combination
     * of the components of the document vector is described by the weighted fields
     * provided.  While not an absolute necessity, if the weights of the fields 
     * do not sum to 1, you may get a document vector with weights greater than
     * 1.
     * @param fields the fields that we will be using to build the document
     * vector
     * @return the composite document vector for this document where the terms in the vector
     * are those found in any of the provided fields.  Note that the document vector
     * returned cannot be used in conjunction with a non-composite document vector!
     * 
     * @see #getDocumentVector(String)
     */
    public DocumentVector getDocumentVector(WeightedField[] fields);
    
    /**
     * Returns an iterator for all of the field values in a result.
     * @return an iterator for all of the field values in a result
     */
    public Iterator getFieldIterator();
    
    /**
     * Gets the score associated with this result.
     * @return the score associated with this result
     */
    public float getScore();
    
    /**
     * Gets the distance between this result and another based on a named
     * feature vector value.
     *
     * @param r the other result
     * @param name the name of the feature vector field
     * @return the euclidean distance between the two results based on the
     * give field.
     */
    public double getDistance(Result r, String name);
    
    /**
     * Gets the number of passages associated with this result.
     * @return the number of passages associated with this result.
     */
    public int getNPassages();
    
    /**
     * Gets the name of the index that this result was drawn from.  This is a
     * configuration parameter that is passed to the
     * <code>SearchEngine</code> at startup time.
     * @return the name of the index from which this result was drawn
     */
    public String getIndexName();
    
    /**
     * Gets a passage builder that can be used to construct passages for
     * display from this result.
     * @return a passage builder that we can use to construct passages for this
     * result
     */
    public PassageBuilder getPassageBuilder();
    
} // Result
