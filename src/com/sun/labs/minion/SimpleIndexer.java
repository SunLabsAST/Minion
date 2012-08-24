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

import java.util.Collection;
import java.util.Date;

/**
 * An interface that allows one fairly straightforward access to the
 * indexing API.  An implementer of this class is meant to be used <em>by a
 * single thread only</em>.  Using an implementation of this class with multiple
 * threads will lead to unexpected results and will likely produce lots of
 * exceptions.
 *
 * <p>
 *
 * A typical use of a simple indexer will be something like:
 *
 * <pre>
 * SimpleIndexer indexer = engine.getSimpleIndexer();
 * while(!done) {
 *    indexer.startDocument(key);
 *    indexer.addField(name, val);
 *    indexer.addTerm(t1, c1);
 *    indexer.addField(name2, val2);
 *    indexer.addTerm(t2, c2);
 *    ...
 *    indexer.endDocument();
 * }
 * indexer.finish();
 * </pre>
 *
 * <p>
 *
 * Note that you <em>must</em> call <code>finish</code> when you are done
 * using the simple indexer.  If you do not, then some of your data may not
 * be saved into the index.
 *
 * <p>
 *
 * Once you have called <code>finish</code>, you cannot use the simple
 * indexer to do any more indexing.  Any attempt to do so will result in an
 * <code>IllegalStateException</code> being thrown.
 *
 * @see SearchEngine
 * @see Document
 *    
 */
public interface SimpleIndexer {
    
    /**
     * Indexes a whole document at once.  You must not do this in the middle of 
     * another document!
     *
     * @param doc a document to index.
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * indexing the document
     * @see SearchEngine#index(Indexable)
     */
    public void indexDocument(Indexable doc) throws SearchEngineException;

    /**
     * Indexes a whole document at once.  You must not do this in the middle of 
     * another document!
     *
     * @param doc a document to index.
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * indexing the document
     * @see Document
     * @see SearchEngine#index(Document)
     */
    public void indexDocument(Document doc) throws SearchEngineException;

    /**
     * Starts the indexing of a document.
     *
     * @param key the document key for the document.  If this is a
     * duplicate, then any old data associated with the document will be
     * removed.
     * @throws NullPointerException if the document key is null
     * @throws IllegalStateException if this method is called while we are
     * already indexing another document.
     */
    public void startDocument(String key);

    /**
     * Adds a string field value to the current document.
     *
     * @param name the name of the field to which we want to add a value.
     * If the named field is not currently defined, then the way that the
     * field is treated depends on the engine configuration.  By default,
     * the field will be indexed, tokenized, and vectored.  If the field
     * name is <code>null</code>, then the value will be treated as part of
     * the implicit document body and will therefore be indexed, tokenized,
     * and vectored.
     * @param value the value of the field.  The value will be
     * appropriately tokenized, indexed and vectored if the named field has
     * those attributes.  If the field is a saved field, then the engine
     * will attempt to process the field according to the type of the saved
     * field.  If the field is a string saved field, this is
     * straightforward.  If the field is an integer or float saved field,
     * then the engine will attempt to parse a numeric value of the
     * appropriate type from the string value.  If the field is a date
     * saved field, then the engine will attempt to parse a date out of the
     * string value using a number of different date formats.
     * @throws IllegalStateException if this method is called without having
     * called <code>startDocument</code>
     * @see IndexConfig
     * @see #startDocument
     * 
     */
    public void addField(String name, String value);

    /**
     * Adds a date field value to the current document.
     *
     * @param name the name of the field to which we want to add a value.
     * If the named field is not currently defined, then the way that the
     * field is treated depends on the engine configuration.  By default,
     * the field will be indexed, tokenized, and vectored.  If the field
     * name is <code>null</code>, then the value will be treated as part of
     * the implicit document body and will therefore be indexed, tokenized,
     * and vectored.
     * @param value the value of the field.  If the named field is not a
     * date saved field, then a warning will be issued.  Note that this
     * warning may not be seen unless you have set the logging level high
     * enough for your indexer. If the named field is a date saved field,
     * then this value will be stored so that it may be retrieved later.
     * @throws IllegalStateException if this method is called without having
     * called <code>startDocument</code>
     * @see IndexConfig
     * @see #startDocument
     * 
     */
    public void addField(String name, Date value);
    
    /**
     * Adds a long field value to the current document.
     *
     * @param name the name of the field to which we want to add a value.
     * If the named field is not currently defined, then the way that the
     * field is treated depends on the engine configuration.  By default,
     * the field will be indexed, tokenized, and vectored.  If the field
     * name is <code>null</code>, then the value will be treated as part of
     * the implicit document body and will therefore be indexed, tokenized,
     * and vectored.
     * @param value the value of the field.  If the named field is an
     * integer saved field then the value will be stored for later
     * retrieval.  If the named field is a date saved field, then this
     * value will be treated as a number of milliseconds since the epoch.
     * If the named field is a string saved field, then a string
     * representation of the number (the representation provided by
     * <code>Long.toString()</code>) will be saved in the index for later
     * retrieval.  Note that such a saved value will most likely
     * <em>not</em> be suitable for sorting by numerical order!
     * @throws IllegalStateException if this method is called without having
     * called <code>startDocument</code>
     * @see IndexConfig
     * @see #startDocument
     * 
     */
    public void addField(String name, Long value);
    
    /**
     * Adds an integer field value to the current document.
     *
     * @param name the name of the field to which we want to add a value.
     * If the named field is not currently defined, then the way that the
     * field is treated depends on the engine configuration.  By default,
     * the field will be indexed, tokenized, and vectored.  If the field
     * name is <code>null</code>, then the value will be treated as part of
     * the implicit document body and will therefore be indexed, tokenized,
     * and vectored.
     * @param value the value of the field.  If the named field is an
     * integer saved field then the value will be stored for later
     * retrieval.  If the named field is a date saved field, then this
     * value will be treated as the number of seconds since the epoch.
     * If the named field is a string saved field, then a string
     * representation of the number (the representation provided by
     * <code>Integer.toString()</code>) will be saved in the index for later
     * retrieval.  Note that such a saved value will most likely
     * <em>not</em> be suitable for sorting by numerical order!
     * @throws IllegalStateException if this method is called without having
     * called <code>startDocument</code>
     * @see IndexConfig
     * @see #startDocument
     * 
     */
    public void addField(String name, Integer value);
    
    /**
     * Adds a double field value to the current document.
     *
     * @param name the name of the field to which we want to add a value.
     * If the named field is not currently defined, then the way that the
     * field is treated depends on the engine configuration.  By default,
     * the field will be indexed, tokenized, and vectored.  If the field
     * name is <code>null</code>, then the value will be treated as part of
     * the implicit document body and will therefore be indexed, tokenized,
     * and vectored.
     * @param value the value of the field.  If the named field is an float
     * saved field then the value will be stored for later retrieval.  If
     * the named field is a string saved field, then a string
     * representation of the number (the representation provided by
     * <code>Double.toString()</code>) will be saved in the index for later
     * retrieval.  Note that such a saved value will most likely
     * <em>not</em> be suitable for sorting by numerical order!
     * @throws IllegalStateException if this method is called without having
     * called <code>startDocument</code>
     * @see IndexConfig
     * @see #startDocument
     * 
     */
    public void addField(String name, Double value);

    /**
     * Adds a float field value to the current document.
     *
     * @param name the name of the field to which we want to add a value.
     * If the named field is not currently defined, then the way that the
     * field is treated depends on the engine configuration.  By default,
     * the field will be indexed, tokenized, and vectored.  If the field
     * name is <code>null</code>, then the value will be treated as part of
     * the implicit document body and will therefore be indexed, tokenized,
     * and vectored.
     * @param value the value of the field.  If the named field is an float
     * saved field then the value will be stored for later retrieval.  If
     * the named field is a string saved field, then a string
     * representation of the number (the representation provided by
     * <code>Double.toString()</code>) will be saved in the index for later
     * retrieval.  Note that such a saved value will most likely
     * <em>not</em> be suitable for sorting by numerical order!
     * @throws IllegalStateException if this method is called without
     * having called <code>startDocument</code>
     * @see IndexConfig
     * @see #startDocument
     * 
     */
    public void addField(String name, Float value);

    /**
     * Adds a number of values to a particular field in the current
     * document.
     * 
     * @param name the name of the field to which we want to add values.
     * If the named field is not currently defined, then the way that the
     * field is treated depends on the engine configuration.  By default,
     * the field will be indexed, tokenized, and vectored.  If the field
     * name is <code>null</code>, then the value will be treated as part of
     * the implicit document body and will therefore be indexed, tokenized,
     * and vectored.
     * @param values the values to add to the field.  The disposition of
     * the elements of the array is in accordance with the single-value
     * instances of the <code>addField</code> method.  If the array
     * contains a type that is not specified in one those methods, then we
     * will call the <code>toString</code> method on the object and index
     * the results.
     * @throws IllegalStateException if this method is called without
     * having called <code>startDocument</code>
     * @see IndexConfig
     * @see #startDocument
     *
     */
    public void addField(String name, Object[] values);
    
    /**
     * Adds a number of values to a particular field in the current
     * document.
     * 
     * @param name the name of the field to which we want to add values.
     * If the named field is not currently defined, then the way that the
     * field is treated depends on the engine configuration.  By default,
     * the field will be indexed, tokenized, and vectored.  If the field
     * name is <code>null</code>, then the value will be treated as part of
     * the implicit document body and will therefore be indexed, tokenized,
     * and vectored.
     * @param values the values to add to the field.  The disposition of
     * the elements of the collection is in accordance with the
     * single-value instances of the <code>addField</code> method.  If the
     * array contains a type that is not specified in one those methods,
     * then we will call the <code>toString</code> method on the object and
     * index the results.
     * @throws IllegalStateException if this method is called without
     * having called <code>startDocument</code>
     * @see IndexConfig
     * @see #startDocument
     *
     */
    public void addField(String name, Collection<Object> values);

    /**
     * Adds a term to the current document.  The term is added to the
     * implicit body field of the document.
     *
     * @param term the term
     * @throws IllegalStateException if this method is called without having
     * called <code>startDocument</code>
     * @see #addTerm(String,int)
     * @see #startDocument
     */
    public void addTerm(String term);

    /**
     * Adds a term to the current document.  The term is added to the
     * implicit body field of the document.
     *
     * @param term the term
     * @param count the number of times that the term occurs in the
     * body of the document
     * @throws IllegalStateException if this method is called without having
     * called <code>startDocument</code>
     * @see #addTerm(String)
     * @see #startDocument
     */
    public void addTerm(String term, int count);

    /**
     * Adds a term to the given field in the current document.  This field <em>must</em>
     * have the indexed attribute and may specify the vectored attribute, if you would
     * like to use this field to do document similarity calculations.
     * 
     * <p>
     * Note that if the named field is a saved field, this method will not
     * cause the data to be saved into the index.
     * </p>
     * 
     * <p>
     * If the given field names an undefined field, a field will be defined
     * using the default attributes and type.
     * </p>
     *
     * @param field the field to which we want to add the term. 
     * @param term the term
     * @param count the number of times that the term occurs in the
     * document
     * @throws IllegalStateException if this method is called without having
     * called <code>startDocument</code>
     * @throws IllegalArgumentException if an attempt is made to add terms
     * to a field that does not have the indexed attribute
     * @see #startDocument
     */
    public void addTerm(String field, String term, int count);

    /**
     * Ends the current document.  This may cause some index data to be
     * written to disk.
     *
     * @throws IllegalStateException if this method is called without having
     * called <code>startDocument</code>
     * @see #startDocument
     */
    public void endDocument();

    /**
     * Finishes indexing.  You <em>must</em> call this method when you have
     * finished any indexing that you wish to do.  If you do not, some of
     * your data may not be saved to the index.  Once this method has been
     * called, any further attempt to index data using the simple indexer
     * will result in an <code>IllegalStateException</code>.
     */
    public void finish();
            
    /**
     * Finishes indexing and throws away the indexed data.
     */
    public void clear();
    
    /**
     * Indicates whether a document has been indexed or not.  A document
     * has been indexed if its document key is in the index and the
     * document has not been deleted.
     *
     * @param key the document key for the document that we wish to check.
     * @return <code>true</code> if the document is in the index,
     * <code>false</code> otherwise.  A document has been indexed if its
     * document key is in the index and the document has not been deleted.
     */
    public boolean isIndexed(String key);

}// SimpleIndexer
