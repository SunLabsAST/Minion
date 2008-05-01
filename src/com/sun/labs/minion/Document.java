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
import java.util.Map;

/**
 * A document abstraction for the search engine.  This interface is meant to 
 * be used as a way for an application to retrieve (some) of the contents of a 
 * document out of the index, make some modifications to the index and then
 * resubmit the document to the index.
 * 
 * <p>
 * 
 * Note that this representation of a document will contain only the fields that
 * have the <code>SAVED</code> and <code>VECTORED</code> attributes set.  Data
 * that was indexed, but not vectored cannot be recovered for such a representation.
 * 
 * @see SearchEngine#getDocument
 * @see FieldInfo
 */
public interface Document {
    
    /**
     * Gets the key associated with this document.
     *
     * @return the key for this document.
     */
    public String getKey();
    
    /**
     * Sets the key for this document.  Note that this change does not affect
     * the engine from which this document was drawn.  If you change the key
     * for a document you will need to resubmit the document for indexing.  Note
     * that if you change the key and reindex the document, you are responsible for
     * deleting the document under it's old key!  Note that the other methods for
     * this class will continue to function, even if the document's key is 
     * changed using this method.
     *
     * @param key the key for the document
     */
    public void setKey(String key);
    
    /**
     * Gets the values of a given saved field from the index.
     * @param field the name of the saved field whose values should be retrieved.
     * @return a list of the values for the saved field.  If there are no
     * values for the given saved field in this document, an empty list is returned.  
     * @throws IllegalArgumentException if the given field is not the name of a
     * saved field.
     */
    public List<Object> getSavedField(String field);
    
    /**
     * Gets an iterator for all of the saved field values in a document.
     * @return an iterator for all of the saved fields that occur in this document.
     * 
     */
    public Iterator<Map.Entry<String,List>> getSavedFields();
    
    /**
     * Sets the values of a saved field.  Note that this will not affect the 
     * underlying index.  If modifications are made to a document, the modified
     * document must be re-submitted to the search engine for indexing.
     *
     * @param field the name of the saved field whose values should be set.
     * @param values a list of values to assign to the saved field.  These values
     * should be of a type appropriate to the saved field.
     * @throws IllegalArgumentException if the named field is not a saved field.
     * @see SearchEngine#index(Document)
     * 
     */
    public void setSavedField(String field, List values);
    
    /**
     * Gets the list of postings associated with a given field.
     * @param field the name of a field that was indexed and vectored.  If this
     * value is <code>null</code> then the postings returned are for terms that
     * were not added to any explicit field.
     * @return the list of postings associated with the given field in this
     * document
     * @throws IllegalArgumentException if the named field is not a vectored
     * field.
     */
    public List<Posting> getPostings(String field);
    
    /**
     * Gets an iterator for the vectored fields in this document.
     *
     * @return an iterator for the vectored fields in this document.
     */
    public Iterator<Map.Entry<String,List<Posting>>> getPostings();
    
    /**
     * Sets the list of postings associated with a given field.  Note that this will not affect the 
     * underlying index.  If modifications are made to a document, the modified
     * document must be re-submitted to the search engine for indexing.
     *
     * @param field the name of the field whose postings should be modified.  
     *  If this
     * value is <code>null</code> then the postings will be used for the default
     * unnamed field.
     * @param postings a list of postings to associate with the field.  If the
     * named field has no postings for this field, then an empty list is returned.
     * @throws IllegalArgumentException if the named field is not a 
     * vectored field.
     */
    public void setPostings(String field, List<Posting> postings);
}
