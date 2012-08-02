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

import com.sun.labs.minion.indexer.HighlightDocumentProcessor;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.query.Element;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;


/**
 * This is the main interface for the search engine, which handles both indexing
 * and retrieval operations.  An implementation of this interface can be created
 * using the {@link SearchEngineFactory}.
 *
 *
 * <h2>Indexing Documents</h2>
 *
 * <p> Each document is expected to have a unique <em>key</em> associated
 * with it.  The key can be any non-<code>null</code> Java
 * <code>String</code>.  It is up to the application to create these unique
 * keys for the documents that are to be indexed.  If you re-use a key (for
 * example, if you're re-indexing a document whose contents have changed),
 * then the search engine will index the new document and mark the old one
 * as deleted.
 *
 * <h3>{@link Indexable}</h3> A search engine provides three approaches for
 * indexing documents.  The first is provided by the {@link
 * #index(Indexable)} method.  {@link Indexable} is an interface that can
 * be implemented by objects that you want to be indexed by the search
 * engine.  Objects that implement {@link Indexable} are indexed using the
 * second approach for indexing.
 *
 * <h3>Indexing a <code>Map</code></h3> In the second approach, a document
 * key and a map from field names to field values are provided by the
 * application.  The search engine will iterate through the map and process
 * each of the fields represented by a key/value pair.  It is expected that
 * the names of the fields provided as the keys in the map will be the same
 * as the names of the fields defined in the configuration for the indexer.
 * See {@link #defineField} to see how you can define fields
 * programatically.
 *
 * <p> How the values in the map are handled depends on the attributes and
 * types of the fields being indexed.  The values in the maps can be a
 * variety of types then engine will recognize <code>String</code>,
 * <code>java.util.Date</code>, <code>Integer</code>, <code>Long</code>,
 * <code>Float</code>, <code>Double</code>, as well as
 * <code>java.util.Collection</code>s and arrays of these types.
 *
 * <p> The engine will do some type conversion as necessary.  For example,
 * if the application defines a field of type <code>INTEGER</code> and a
 * string is passed as the value for that field, the string will be parsed
 * as an integer.
 *
 * <h3>Indexing using {@link SimpleIndexer}</h3>
 *
 * The final indexing approach is the {@link SimpleIndexer}.  The
 * application can request an instance of a {@link SimpleIndexer} using the
 * {@link #getSimpleIndexer} method.
 *
 * <p>
 *
 * The application can use the simple indexer to add fields to a document one
 * at a time, rather than having to have them all at the same time.
 *
 *
 *
 * </ol>
 *
 */
public interface SearchEngine extends Searcher {
    
    /**
     * Sets the default field information to use when unknown fields are
     * encountered during indexing.
     * 
     * @param field an exemplar field information object that has the
     * attributes and type that should be used when an unknows field is
     * encountered during indexing.  Note that any name associated with
     * this particular object will be ignored, we are only interested in
     * the attributes and type associated with this field.
     * @see #defineField for how to define a field to use during indexing
     */
    public void setDefaultFieldInfo(FieldInfo field);

    /**
     * Sets the indicator that this is a long indexing run, in which case 
     * term statistics dictionaries and document vector lengths
     * will not be calculated until the engine is shutdown.  This should be 
     * done before any indexing begins for best results.
     * 
     * @param longIndexingRun true if this is a long indexing run
     */
    public void setLongIndexingRun(boolean longIndexingRun);
    
    /**
     * Adds a listener for events in the index backing this search engine.
     * 
     * @param il the listener to add.
     */
    public void addIndexListener(IndexListener il);
    
    /**
     * Removes an index listener from the listeners.
     * @param il the index listener to remove.
     */
    public void removeIndexListener(IndexListener il);
    
    /**
     * Defines a given field.  Once a field has been defined, its
     * attributes and type cannot be changed, although it can be redefined
     * with the same attributes and types.
     *
     * @param field the field to define
     * @return the defined field information object, including an ID
     * assigned by the engine.
     * @throws SearchEngineException if the field is already defined and
     * there is a mismatch in the attributes or type of the given field or
     * if there is an error adding the field to the index
     */
    public FieldInfo defineField(FieldInfo field)
            throws SearchEngineException;

    /**
     * Gets the information for a field.
     *
     * @param name the name of the field for which we want information
     * @return the information associated with this field, or <code>null</code>
     * if this name is not the name of a defined field.
     */
    public FieldInfo getFieldInfo(String name);

    /**
     * Gets the set of variations on a term that will be generated by default
     * when searching for the term.  The composition of the set depends on the
     * configuration of the engine and will be returned in no particular order.
     * @param term the term for which we want variants
     * @return the set of variants for the term, which will always include the
     * term itself.  The case of the variants will match (as much as possible) the
     * case of the provided term.
     */
    public Set<String> getTermVariations(String term);

    /**
     * Gets the collection level term statistics for the given term.
     * @param term the term for which we want the statistics
     * @return the statistics associated with the given term, or <code>null</code>
     * if the term does not occur in the collection.
     */
    public TermStats getTermStats(String term, String field);

    /**
     * Gets the collection level term statistics for the given term.
     * @param term the term for which we want the statistics
     * @return the statistics associated with the given term, or <code>null</code>
     * if the term does not occur in the collection.
     */
    public TermStats getTermStats(String term, FieldInfo field);
    
    /**
     * Gets a document with a given key.
     *
     * @param key the key for the document to retrieve.
     * @return a document with the given key.  If the given key does not occur
     * in the index, then <code>null</code> is returned.
     * @see #index(Document)
     * @see #createDocument
     * @see SimpleIndexer#indexDocument(Document)
     */
    public Document getDocument(String key);

    /**
     * Gets a list of documents with the given keys.
     *
     * @param keys the list of keys for which we want documents
     * @return a list of the documents corresponding to the keys in the
     * list.  Note that this list will not include documents that have been
     * deleted and no documents will be returned for keys that do not exist
     * in the index, so there may not be a one-to-one correspondence
     * between the keys in <code>keys</code> and the documents in the
     * returned list.
     */
    public List<Document> getDocuments(List<String> keys);

    /**
     * Creates a new document with a given key.
     *
     * @param key the key for the new document
     * @return a new document with the given key.  If the given key is already
     * in the index, then <code>null</code> is returned.
     * @see #index(Document)
     * @see #getDocument
     * @see SimpleIndexer#indexDocument(Document)
     */
    public Document createDocument(String key);

    /**
     * Indexes a document into the database.  If the document already
     * exists in the database, the new information will replace the old.
     *
     * <p>
     *
     * Note that simply calling <code>index</code> will not make a document
     * available for searching.  Documents are not available until they are
     * flushed to disk.  This can be accomplished using the
     * <code>flush</code> method.
     *
     * @param key The document key for this document.  The key should be
     * unique in the index.  If the key passed in matches a document that
     * is already in the index, the information for this document will
     * replace the existing one.
     * @param document A map from field names to the value for that field.
     * If a particular field has a type or attributes associated with it,
     * they will be respected during indexing.  If a field has no
     * attributes associated with it, the field will be tokenized and
     * indexed.  If you desire consistent treatment of documents for both
     * indexing and highlighting, then we strongly suggest that you use a
     * <code>LinkedHashMap</code> for this parameter.
     *
     * @throws SearchEngineException if there are any errors during the
     * indexing.
     *
     * @see java.util.LinkedHashMap
     */
    public void index(String key, Map document)
            throws SearchEngineException;

    /**
     * Indexes a document into the database.  If the document already
     * exists in the database, the new information will replace the old.
     *
     * <p>
     *
     * Note that simply calling <code>index</code> will not make a document
     * available for searching.  Documents are not available until they are
     * flushed to disk.  This can be accomplished using the
     * <code>flush</code> method.
     *
     * @param document the document to index.
     *
     * @throws SearchEngineException if there are any errors during the
     * indexing.
     *
     */
    public void index(Indexable document)
            throws SearchEngineException;

    /**
     * Indexes a document into the database.  If the document already
     * exists in the database, the new information will replace the old.
     *
     * <p>
     *
     * In this case, the data for the document will be flushed to disk as
     * soon as the document is indexed.  For indexing a large number of
     * documents, you may wish to consider the {@link
     * SimpleIndexer#indexDocument(Document)} method, which will allow you
     * more control over when the data will be flushed to disk.
     *
     * @param document a document to be indexed
     *
     * @throws SearchEngineException if there are any errors during the
     * indexing.
     * @see #getDocument
     * @see SimpleIndexer#indexDocument(Document)
     *
     */
    public void index(Document document) throws SearchEngineException;

    /**
     * Flushes the indexed material currently held in memory to the disk,
     * making it available for searching. Control will not return to the caller
     * until the data is available for searching, which may take some time. If 
     * you wish to get control back immediately, see {@link #asyncFlush()}.
     * 
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * flushing the data to disk
     */
    public void flush() throws SearchEngineException;

    /**
     * Flushes the indexed material currently held in memory to the disk, making
     * it available for searching.
     *
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * flushing the data to disk
     */
    public void asyncFlush() throws SearchEngineException;

    /**
     * Checks to see if a document is in the index.
     *
     * @param key the key for the document that we wish to check.
     * @return <code>true</code> if the document is in the index.  A
     * document is considered to be in the index if a document with the
     * given key appears in the index and has not been deleted.
     */
    public boolean isIndexed(String key);

    /**
     * Deletes a document from the index.
     *
     * @param key The key for the document to delete.
     */
    public void delete(String key);

    /**
     * Deletes a number of documents from the index.
     *
     * @param keys The keys of the documents to delete
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * deleting the documents from the index
     */
    public void delete(List<String> keys)
            throws SearchEngineException;

    /**
     * Runs a query against the index, returning a set of results.  The result
     * set will be sorted in descending order of score, by default.
     *
     * @param query The query to run, in our query syntax.
     */
    @Override
    public ResultSet search(String query)
            throws SearchEngineException;
    
    /**
     * Runs a query against the index, returning a set of results.
     *
     * @param query The query to run, in our query syntax.
     * @param sortOrder How the results should be sorted.  This is a set of
     * comma-separated field names, each preceeded by a <code>+</code> (for
     * increasing order) or by a <code>-</code> (for decreasing order).
     * @throws com.sun.labs.minion.SearchEngineException if there are any errors
     * evaluating the query
     */
    @Override
    public ResultSet search(String query, String sortOrder)
            throws SearchEngineException;

    /**
     * Runs a query against the index, returning a set of results.
     *
     * @param query The query to run, in our query syntax.
     * @param sortOrder How the results should be sorted.  This is a set of
     * comma-separated field names, each preceeded by a <code>+</code> (for
     * increasing order) or by a <code>-</code> (for decreasing order).
     * @param defaultOperator specified the default operator to use when no
     * other operator is provided between terms in the query.  Valid values are
     * defined in the {@link Searcher} interface
     * @param grammar specifies the grammar to use to parse the query.  Valid values
     * ar edefined in the {@link com.sun.labs.minion.Searcher} interface
     * @return a set of results for this search.
     * @throws com.sun.labs.minion.SearchEngineException if there is any error during
     * the search.
     */
    @Override
    public ResultSet search(String query, String sortOrder,
                            Searcher.Operator defaultOperator,
                            Searcher.Grammar grammar)
            throws SearchEngineException;

    /**
     * Runs a query against the index, returning a set of results.
     * @param el the query, expressed using the programattic query API
     * @return the set of documents that match the query
     * @throws com.sun.labs.minion.SearchEngineException if there are any errors
     * evaluating the query
     */
    ResultSet search(Element el) throws SearchEngineException;

    /**
     * Runs a query against the index, returning a set of results.
     * @param el the query, expressed using the programattic query API
     * @param sortOrder How the results should be sorted.  This is a set of
     * comma-separated field names, each preceeded by a <code>+</code> (for
     * increasing order) or by a <code>-</code> (for decreasing order).
     * @return the set of documents that match the query
     * @throws com.sun.labs.minion.SearchEngineException if there are any errors
     * evaluating the query
     */
    ResultSet search(Element el, String sortOrder) throws SearchEngineException;

    /**
     * Gets a set of results corresponding to the document keys passed in.
     * This is a convenience method to go from document keys to something upon which
     * more complicated computations can be done.
     *
     * @param keys a list of document keys for which we want results.
     * @return a result set that includes the documents whose keys occur in the
     * list.  All documents in the set will be assigned a score of 1.  Note that documents
     * that have been deleted will not appear in the result set.
     */
    public ResultSet getResults(Collection<String> keys);

    /**
     * Gets the values for the given field that match the given pattern.
     * @param field the saved, string field against whose values we will match.
     * If the named field is not saved or is not a string field, then the empty
     * set will be returned.
     * @param pattern the pattern for which we'll find matching field values.
     * @return a sorted set of field values.  This set will be ordered by the
     * proportion of the field value that is covered by the given pattern.
     */
    public SortedSet<FieldValue> getMatching(String field,
                                             String pattern);

    /**
     * Gets an iterator for all the values in a field.  The values are
     * returned by the iterator in the order defined by the field type.
     *
     * @param field The name of the field who's values we need an iterator
     * for.
     * @return An iterator for the given field.  If the field is not a
     * saved field, then an iterator that will return no values will be
     * returned.  Note that the application will need to know what type of
     * values to get from the list!
     *
     * @see FieldInfo#getType
     */
    public Iterator<Object> getFieldIterator(String field);

    /**
     * Gets all of the field values associated with a given field in a
     * given document.
     *
     * @param field The name of the field for which we want the values.
     * @param key The key of the document whose values we want.
     * @return A <code>List</code> containing values of the appropriate
     * type.  If the named field is not a saved field, or if the given
     * document key is not in the index, then an empty list is returned.
     */
    public List<Object> getAllFieldValues(String field, String key);

    /**
     * Gets a single field value associated with a given field in a given
     * document.
     *
     * @param field The name of the field for which we want the values.
     * @param key The key of the document whose values we want.
     * @return An <code>Object</code> of the appropriate type for the named
     * field. If the named field is not a saved field, or if the given
     * document key is not in the index, then <code>null</code> is
     * returned.
     * <p>
     * Note that if there are multiple values for the given field, there is
     * no guarantee which of the values will be returned by this method.
     *
     * @see #getAllFieldValues
     */
    public Object getFieldValue(String field, String key);

    /**
     * Gets a list of the top n most frequent field values for a given
     * named field.  If n is &lt; 1, all field values are returned, in order
     * of their frequency from most to least frequent.
     *
     * @param field the name of the field to rank
     * @param n the number of field values to return
     * @return a <code>List</code> containing field values of the appropriate
     * type for the field, ordered by frequency.  The scores associated with the
     * field values are their frequency in the collection.
     */
    public List<FieldFrequency> getTopFieldValues(String field, int n, boolean ignoreCase);

    /**
     * Gets a document vector for the given key. The vector will be composed of
     * the default, vectored fields, that is, fields that have the
     * {@link FieldInfo.Attribute#DEFAULT} and
     * {@link FieldInfo.Attribute#VECTORED} attributes.
     *
     * @param key the key of the document whose vector we will return
     * @return the vector for that document, or <code>null</code> if that key
     * does not appear in this index.
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * retrieving the document vector for the document associated with the
     * given key.
     */
    public DocumentVector getDocumentVector(String key)
            throws SearchEngineException;

    /**
     * Gets a document vector for given vectored field for the given key.
     *
     * @param key the key of the document whose vector we will return
     * @param field the field for which we want a document vector.  If this
     * parameter is <code>null</code>, then a vector containing the terms from
     * all vectored fields in the document is returned. If this value is the empty string, then a
     * vector for the contents of the document that are not in any field are
     * returned.  If this value is the name of a field that was not vectored
     * during indexing, an empty vector will be returned.
     * @return the vector for that document, or <code>null</code> if that key
     * does not appear in this index.
     */
    public DocumentVector getDocumentVector(String key, String field);
    
    /**
     * Gets a document vector for the given document, combining the data from the
     * provided fields.
     * @param key the key for the document whose vector we will return.
     * @param fields the name of the fields whose data will be included in the 
     * returned vector.
     * @return A document vector for the given key. If the key is not found in the
     * index, then <code>null</code> will be returned.
     * @throws IllegalArgumentException if one of the named fields does not exist, or
     * if the named field is not vectored.
     */
    public DocumentVector getDocumentVector(String key, String[] fields);

    /**
     * Gets a document vector for the given linear combination of 
     * vectored fields for the given key.
     *
     * @param key the key of the document whose vector we will return
     * @param fields the fields from which the document vector will be composed.  
     * @return the vector for that document, or <code>null</code> if that key
     * does not appear in this index.
     */
    public DocumentVector getDocumentVector(String key, WeightedField[] fields);

    /**
     * Creates a document vector for the given document as though it occurred
     * in the index.
     *
     * @param doc a document for which we want a document vector.  This
     * document may be in the index or may be generated via the
     * {@link #createDocument} method.  The document will be processed as
     * though it were being indexed in order to extract the appropriate
     * document vector, but the data resulting from this processing <em>will
     * not</em> be added to the index.
     * @param field the field for which we want a document vector.  If this
     * parameter is <code>null</code>, then a vector containing the terms from
     * all vectored fields in the document is returned. If this value is the
     * empty string, then a vector for the contents of the document that are
     * not in any field are returned.  If this value is the name of a field
     * that was not vectored during indexing, an empty vector will be returned.
     * @return the vector for the given document, taking into account the 
     * restrictions in the <code>field</code> parameter
     */
    public DocumentVector getDocumentVector(Document doc, String field)
            throws SearchEngineException;

    /**
     * Creates a composite document vector for the given document as though it occurred
     * in the index.
     *
     * @param doc a document for which we want a document vector.  This
     * document may be in the index or may be generated via the
     * {@link #createDocument} method.  The document will be processed as
     * though it were being indexed in order to extract the appropriate
     * document vector, but the data resulting from this processing <em>will
     * not</em> be added to the index.
     * @param fields the fields for which we want a document vector.
     */
    public DocumentVector getDocumentVector(Document doc, WeightedField[] fields)
            throws SearchEngineException;
    
    /**
     * Gets a document vector that is created from the combination of the 
     * document vectors for the given document keys.
     * @param keys the keys for which we want a document vector.
     * @return the document vector.
     */
    public DocumentVector getDocumentVector(Collection<String> keys) throws SearchEngineException;

    public DocumentVector getDocumentVector(Collection<String> keys, String field) throws
            SearchEngineException;
    
    public DocumentVector getDocumentVector(Collection<String> keys, String[] fields)
            throws SearchEngineException;
    
    public DocumentVector getDocumentVector(Collection<String> keys, WeightedField[] fields) throws
            SearchEngineException;
    /**
     * Gets the combined query stats for any queries run by the engine.
     * @return the combined query statistics
     * @see #resetQueryStats
     */
    public QueryStats getQueryStats();

    /**
     * Resets the query stats for the engine.
     * @see #getQueryStats
     */
    public void resetQueryStats();

    /**
     * Purges all of the data in the index.  This operation is not reversible!
     * Any documents in the indexing pipeline will be flushed out and purged
     * during this process.  Any meta data in the MetaDataStore will also be
     * purged.
     * 
     * Indexing documents while purge is called isn't recommended.
     * If you are using a SimpleIndexer, you should call
     * {@link SimpleIndexer#finish()} before calling purge.
     */
    public void purge();

    /**
     * Performs a merge in the index, if one is necessary.  Returns control
     * to the caller when the merge is completed.
     *
     * @return <code>true</code> if a merge was performed,
     * <code>false</code> otherwise.
     */
    public boolean merge();

    /**
     * Merges all of the partitions in the index into a single partition.
     * Typically an index with a single partition will have better query
     * performance than one with many partitions.
     * 
     * @throws com.sun.labs.minion.SearchEngineException if there was any error
     * during the merge.
     */
    public void optimize()
            throws SearchEngineException;

    /**
     * Attempts to recover the index after an unruly shutdown.  Makes
     * sure that lock files are removed and that any partial files resulting from failed
     * partition dumps or merges are removed.  Note that this method should be
     * called <em>only</em> when you are sure that the current search engine
     * is the only one open for a given index.
     * 
     * @throws com.sun.labs.minion.SearchEngineException if there is any problem
     * recovering the index.
     */
    public void recover()
            throws SearchEngineException;

    /**
     * Gets an iterator for all of the non-deleted documents in the
     * collection.
     * @return an iterator for all of the non-deleted documents in the 
     * collection.
     */
    public Iterator<Document> getDocumentIterator();

    /**
     * Closes the engine.  If you wish to reuse a closed engine, you must
     * use the factory class to get a new instance of the engine!
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * closing the search engine
     */
    public void close()
            throws SearchEngineException;

    /**
     * Gets the name of this engine, if one has been assigned by the
     * application.
     *
     * @return The name of the engine assigned by the application, or
     * <code>null</code> if none has been assigned.
     */
    public String getName();

    /**
     * Gets the number of undeleted documents that the index contains.
     * @return the number of undeleted documents in the index
     */
    public int getNDocs();
    
    /**
     * Gets the default fields to search from this engine. These may have been
     * specified as part of a query configuration or when the fields were
     * defined. Any default field information that is part of the query configuration 
     * (see {@link QueryConfig#addDefaultField}) will take precedence over 
     * default fields in the field definitions themselves.
     */
    public Set<FieldInfo> getDefaultFields();
    
    /**
     * Sets the query configuration to use for subsequent queries.
     *
     * @param queryConfig a set of properties describing the query
     * configuration.
     *
     */
    public void setQueryConfig(QueryConfig queryConfig);

    /**
     * Gets the query configuration that the engine is currently using.
     *
     * @return the current query configuration.
     */
    public QueryConfig getQueryConfig();

    /**
     * Gets a simple indexer that can be used for simple indexing.
     *
     * @return a simple indexer that will index documents into this
     * engine.
     */
    public SimpleIndexer getSimpleIndexer();

    /**
     * Gets a processor that can be used for highlighting.
     * @return an processor that can be used for highlighting 
     * hit documents.
     */
    public HighlightDocumentProcessor getHighlightProcessor();

    /**
     * Gets the partition manager for this search engine.  This is for
     * testing purposes only and not for general consumption.
     *
     * @return The partition manager for this engine.
     */
    public PartitionManager getPM();

    /**
     * Gets the partition manager associated with this search engine.
     * @return The partition manager associated with this search engine.
     */
    public PartitionManager getManager();

    /**
     * Gets the index configuration in use by this search engine.
     * @return The index configuration in use by this search engine.
     */
    public IndexConfig getIndexConfig();

    /**
     * Gets the MetaDataStore for this index.  This is a singleton
     * that stores index-related global variables.
     *
     * @return the MetaDataStore instance
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * getting the metadata store
     */
    public MetaDataStore getMetaDataStore()
            throws SearchEngineException;

}
