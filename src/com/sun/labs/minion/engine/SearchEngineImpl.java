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

package com.sun.labs.minion.engine;

import com.sun.labs.minion.Document;
import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.FieldFrequency;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.io.StringReader;
import java.util.concurrent.BlockingQueue;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.FieldValue;
import com.sun.labs.minion.HLPipeline;
import com.sun.labs.minion.IndexConfig;
import com.sun.labs.minion.IndexListener;
import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.IndexableMap;
import com.sun.labs.minion.Log;
import com.sun.labs.minion.MetaDataStore;
import com.sun.labs.minion.Pipeline;
import com.sun.labs.minion.Progress;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.Searcher;
import com.sun.labs.minion.SimpleIndexer;
import com.sun.labs.minion.TermStats;
import com.sun.labs.minion.WeightedField;
import com.sun.labs.minion.indexer.entry.FieldedDocKeyEntry;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.retrieval.CollectionStats;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.QueryElement;
import com.sun.labs.minion.retrieval.QueryOptimizer;
import com.sun.labs.minion.classification.ClassifierMemoryPartition;
import com.sun.labs.minion.classification.ClassifierManager;
import com.sun.labs.minion.classification.ClusterManager;
import com.sun.labs.minion.classification.ClusterMemoryPartition;
import com.sun.labs.minion.retrieval.parser.LuceneTransformer;
import com.sun.labs.minion.retrieval.parser.ParseException;
import com.sun.labs.minion.retrieval.parser.Parser;
import com.sun.labs.minion.retrieval.parser.SimpleNode;
import com.sun.labs.minion.retrieval.parser.StrictTransformer;
import com.sun.labs.minion.retrieval.parser.Transformer;
import com.sun.labs.minion.retrieval.parser.WebTransformer;
import com.sun.labs.util.props.ConfigBoolean;
import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.ConfigComponentList;
import com.sun.labs.util.props.ConfigDouble;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.ConfigString;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.ConfigurationManager;
import java.io.File;
import java.lang.management.MemoryMXBean;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.concurrent.ArrayBlockingQueue;
import com.sun.labs.minion.classification.ClassifierModel;
import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.DocumentIterator;
import com.sun.labs.minion.indexer.partition.Dumper;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.pipeline.AbstractPipelineImpl;
import com.sun.labs.minion.pipeline.AsyncPipelineImpl;
import com.sun.labs.minion.pipeline.PipelineFactory;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.CompositeDocumentVectorImpl;
import com.sun.labs.minion.retrieval.DocumentVectorImpl;
import com.sun.labs.minion.retrieval.QuickOr;
import com.sun.labs.minion.retrieval.ScoredGroup;
import com.sun.labs.minion.retrieval.parser.LuceneParser;
import com.sun.labs.minion.retrieval.parser.StrictParser;
import com.sun.labs.minion.retrieval.parser.TokenMgrError;
import com.sun.labs.minion.retrieval.parser.WebParser;
import java.util.Set;

/**
 * This is the main class for handling a search engine, both for indexing
 * and retrieval operations.  The engine is configured by two sets of
 * properties:  indexing properties and query properties.  The valid set
 * properties for indexing can be found in the documentation for the {@link
 * IndexConfig} class. The valid set of query properties can be found in
 * the documentation for the {@link QueryConfig} class.
 *
 *
 * <h2>Indexing Documents</h2>
 *
 * Indexing is done by <em>pipelines</em>.  In the index configuration you
 * can specify the number of pipelines that the engine should create.  By
 * creating more than one pipeline, you can index documents in parallel.
 * There are two kinds of pipeline: <em>synchronous</em> and
 * <em>asynchronous</em>.
 *
 * <p>
 *
 * A synchronous pipeline is one which blocks the caller until indexing of
 * the document has been completed.  An asynchronous pipeline contains a
 * queue of documents to index, and the caller will not be blocked during
 * the indexing of a document unless the queue is full.  Once a document
 * has been added to the indexing queue, control returns to the caller.
 * Note that in the case of asynchronous indexing, the map containing your
 * document may sit on the indexing queue for some time, so you <em>should
 * not</em> attempt to change or re-use that map!
 *
 * <p>
 *
 * To index documents, you set up the engine using a set of index configuration
 * properties and then simply call the {@link SearchEngine#index} method.
 * This will route the document to a pipeline that is ready to index.  Once
 * you've indexed all of your documents, you can call the
 * {@link SearchEngine#flush} method to make sure that all of your indexed data
 * is written to the disk.
 *
 *
 */
public class SearchEngineImpl implements SearchEngine,
        Configurable {

    /**
     * The configuration for the index and the indexing engine.
     */
    protected IndexConfig indexConfig;

    /**
     * The configuration for the query engine.
     */
    protected QueryConfig queryConfig;

    /**
     * The meta data storage for this engine/index
     */
    protected MetaDataStoreImpl metaDataStore;

    /**
     * The configuration manager for this engine.
     */
    protected ConfigurationManager cm;

    /**
     * The manager for the partitions in this index.
     */
    protected PartitionManager invFilePartitionManager;

    /**
     * The manager for the classifier partitions in this index.
     */
    protected ClassifierManager classManager;

    /**
     * The manager for the cluster partitions in this index.
     */
    protected ClusterManager clusterManager;

    /**
     * The memory partition for building classifiers
     */
    protected ClassifierMemoryPartition classMemoryPartition;

    /**
     * The memory partition for building feature clusters
     */
    protected ClusterMemoryPartition clusterMemoryPartition;

    /**
     * A blocking queue upon which we can put indexable things.
     */
    protected BlockingQueue indexingQueue;

    /**
     * The pipelines to use for indexing.
     */
    protected Pipeline[] pipes;

    /**
     * Threads to hold run our pipelines.
     */
    protected Thread[] pipeThreads;

    /**
     * A format object for formatting the output.
     */
    protected static DecimalFormat form =
            new DecimalFormat("#####00.00");

    /**
     * The log.
     */
    protected static Log log = Log.getLog();

    /**
     * Our log tag.
     */
    protected static String logTag = "SE";

    /**
     * The configuration name for this search engine.
     */
    private String name;

    private MemoryMXBean memBean;

    /**
     * Gets a search engine implementation.
     */
    public SearchEngineImpl() {
        memBean = ManagementFactory.getMemoryMXBean();
        qs = new QueryStats();
    }

    public FieldInfo defineField(FieldInfo field)
            throws SearchEngineException {
        return invFilePartitionManager.getMetaFile().defineField(field);
    }

    public void setDefaultFieldInfo(FieldInfo field) {
        indexConfig.setDefaultFieldInfo(field);
    }

    /**
     * Gets the information for a field.
     *
     * @param name the name of the field for which we want information
     * @return the information associated with this field, or <code>null</code>
     * if this name is not the name of a defined field.
     */
    public FieldInfo getFieldInfo(String name) {
        FieldInfo fi = invFilePartitionManager.getMetaFile().getFieldInfo(name);
        if(fi != null) {
            fi = fi.clone();
        }
        return fi;
    }

    public TermStats getTermStats(String term) {
        return invFilePartitionManager.getTermStats(term);
    }

    public Document getDocument(String key) {
        DocKeyEntry dke =
                (DocKeyEntry) invFilePartitionManager.getDocumentTerm(key);

        //
        // If there's no such key, then return
        // null, since we don't have this document.
        if(dke == null) {
            return null;
        }

        //
        // Back the existing document with the index.
        return new DocumentImpl(dke);
    }

    public List<Document> getDocuments(List<String> keys) {

        //
        // We'll get a list of the active partitions and the keys that we're
        // looking for.
        List parts = invFilePartitionManager.getActivePartitions();
        List<String> remaining = new ArrayList<String>(keys);
        List<Document> docs =
                new ArrayList<Document>();
        for(Iterator i = parts.iterator(); i.hasNext();) {

            DiskPartition p = (DiskPartition) i.next();
            for(Iterator<String> j = remaining.iterator(); j.hasNext();) {

                //
                // If there's an entry for this key, and it hasn't been
                // deleted, then add it to the group we're building.
                DocKeyEntry dke =
                        (DocKeyEntry) p.getDocumentTerm(j.next());
                if(dke != null) {
                    if(!p.isDeleted(dke.getID())) {
                        docs.add(new DocumentImpl(dke));
                        j.remove();
                    }
                }
            }
        }
        return docs;
    }

    public Document createDocument(String key) {
        DocKeyEntry dke =
                (DocKeyEntry) invFilePartitionManager.getDocumentTerm(key);

        //
        // If there's an existing, un-deleted document, then return null.
        if(dke != null &&
                !((DiskPartition) dke.getPartition()).isDeleted(dke.getID())) {
            return null;
        }

        //
        // Send back an empty document.
        return new DocumentImpl(this, key);
    }

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
     * indexed.
     *
     * @throws SearchEngineException if there are any errors during the
     * indexing.
     *
     * @see com.sun.labs.minion.IndexConfig#IndexConfig
     */
    public void index(String key, Map document)
            throws SearchEngineException {
        index(new IndexableMap(key, document));
    }

    public void index(Indexable doc)
            throws SearchEngineException {
        try {
            if(pipes.length > 1) {
                indexingQueue.put(doc);
            } else {
                pipes[0].index(doc);
            }

            checkDump();
        } catch(InterruptedException ex) {
            log.error(logTag, 1, "Interrupted during index", ex);
        }
    }

    public void index(Document document)
            throws SearchEngineException {
        SimpleIndexer si = getSimpleIndexer();
        ((DocumentImpl) document).index(si);
        si.finish();
    }

    public void addIndexListener(IndexListener il) {
        invFilePartitionManager.addIndexListener(il);
    }
    
    public void removeIndexListener(IndexListener il) {
        invFilePartitionManager.removeIndexListener(il);
    }

    public void checkDump()
            throws SearchEngineException {
        if(checkLowMemory()) {
            dump();
        }
    }

    /**
     * Dumps any data currently held in memory to the disk via our configured
     * dumper.
     */
    protected void dump()
            throws SearchEngineException {
        for(int i = 0; i < pipes.length;
                i++) {
            pipes[i].dump();
        }
    }

    /**
     * Flushes the indexed material currently held in memory to the disk,
     * making it available for searching.
     * @throws com.sun.labs.minion.SearchEngineException If there is any error flushing the in-memory data.
     */
    public synchronized void flush()
            throws SearchEngineException {

        if(invFilePartitionManager == null) {
            return;
        }

        for(int i = 0; i < pipes.length; i++) {
            pipes[i].flush();
        }

        if(classManager != null) {
            classMemoryPartition.dump(indexConfig);
            if(clusterManager != null) {
                clusterMemoryPartition.dump(indexConfig);
            }
        }

        if(metaDataStore != null) {
            try {
                metaDataStore.store();
            } catch(IOException e) {
                log.warn(logTag, 3, "MetaData failed in store()", e);
                throw new SearchEngineException("MetaData could not be stored",
                        e);
            }
        }
    }

    /**
     * Checks to see if a document is in the index.
     *
     * @param key the key for the document that we wish to check.
     * @return <code>true</code> if the document is in the index.  A
     * document is considered to be in the index if a document with the
     * given key appears in the index and has not been deleted.
     */
    public boolean isIndexed(String key) {
        return invFilePartitionManager.isIndexed(key);
    }

    /**
     * Deletes a document from the index.
     *
     * @param key The key for the document to delete.
     */
    public void delete(String key) {
        if(invFilePartitionManager == null) {
            return;
        }
        invFilePartitionManager.deleteDocument(key);
    }

    /**
     * Deletes a number of documents from the index.
     * @param docs The keys of the documents to delete
     * @throws com.sun.labs.minion.SearchEngineException If there is any error deleting the documents.
     */
    public void delete(List<String> docs)
            throws SearchEngineException {
        if(invFilePartitionManager == null) {
            return;
        }
        invFilePartitionManager.deleteDocuments(docs);
    }

    /**
     * Runs a query against the index, returning a set of results.
     * @param query The query to run, in our query syntax.
     * @throws com.sun.labs.minion.SearchEngineException If there is any error during the search.
     * @return An instance of <CODE>ResultSet</CODE> containing the results of the query.
     * @see ResultSet
     */
    public ResultSet search(String query)
            throws SearchEngineException {
        return search(query, "-score",
                Searcher.OP_AND, Searcher.GRAMMAR_STRICT);
    }

    /**
     * Runs a query against the index, returning a set of results.
     * @param query The query to run, in our query syntax.
     * @param sortOrder How the results should be sorted.  This is a set of
     * comma-separated field names, each preceeded by a <code>+</code> (for
     * increasing order) or by a <code>-</code> (for decreasing order).
     * @throws com.sun.labs.minion.SearchEngineException If there is any error during the search.
     * @return An instance of <CODE>ResultSet</CODE> containing the results of the query.
     * @see ResultSet
     */
    public ResultSet search(String query, String sortOrder)
            throws SearchEngineException {
        return search(query, sortOrder,
                Searcher.OP_AND, Searcher.GRAMMAR_STRICT);
    }

    /**
     * Runs a query against the index, returning a set of results.
     * @param query The query to run, in our query syntax.
     * @param sortOrder How the results should be sorted.  This is a set of
     * comma-separated field names, each preceeded by a <code>+</code> (for
     * increasing order) or by a <code>-</code> (for decreasing order).
     * @param defaultOperator specifies the default operator to use when no
     * other operator is provided between terms in the query.  Valid values are
     * defined in the {@link com.sun.labs.minion.Searcher} interface
     * @param grammar specifies the grammar to use to parse the query.  Valid values
     * ar edefined in the {@link com.sun.labs.minion.Searcher} interface
     * @throws com.sun.labs.minion.SearchEngineException If there is any error during the search.
     * @return An instance of <code>ResultSet</code> containing the results of the query.
     */
    public ResultSet search(String query, String sortOrder,
            int defaultOperator, int grammar)
            throws SearchEngineException {

        QueryElement qe = null;
        SimpleNode parseTree = null;

        Parser p = null;
        Transformer xer = null;
        switch(grammar) {
            case Searcher.GRAMMAR_WEB:
                p = new WebParser(new StringReader(query));
                xer = new WebTransformer();
                break;
            case Searcher.GRAMMAR_STRICT:
                p = new StrictParser(new StringReader(query));
                xer = new StrictTransformer();
                break;
            case Searcher.GRAMMAR_LUCENE:
                p = new LuceneParser(new StringReader(query));
                xer = new LuceneTransformer();
                break;
            default:
                throw new SearchEngineException("Unknown grammar specified: " +
                        grammar);
        }

        try {
            parseTree = (SimpleNode) p.doParse();
        } catch(ParseException ex) {
//            log.error(logTag, 1, "Error parsing query", ex);
            log.debug(logTag, 4, "Parse error", ex);
            String msg = ex.getMessage().substring(0, ex.getMessage().indexOf('\n'));
            throw new SearchEngineException("Error parsing query: " + msg);
        } catch(TokenMgrError tme) {
            log.debug(logTag, 4, "Parse error", tme);
            throw new SearchEngineException("Error parsing query: " + tme);
        }

        try {
            qe = xer.transformTree(parseTree, defaultOperator);
        } catch(java.text.ParseException ex) {
            log.error(logTag, 1, "Error transforming query", ex);
            throw new SearchEngineException("Error transforming query", ex);
        }

        if(qe == null) {
            //
            // The query construction code returned an empty query
            throw new SearchEngineException("Error evaluating query: Query is empty");
        }

        //
        // Try to optimize the query some.
        QueryOptimizer opti = new QueryOptimizer();
        qe = opti.optimize(qe);

        log.debug(logTag, 5, "\n" + qe.toString(""));

        try {
            CollectionStats cs =
                    new CollectionStats(invFilePartitionManager);
            List parts = invFilePartitionManager.getActivePartitions();
            QueryConfig cqc = (QueryConfig) queryConfig.clone();
            qe.setQueryConfig(cqc);
            cqc.setCollectionStats(cs);
            cqc.setSortSpec(sortOrder);
            QueryStats lqs = new QueryStats();
            qe.setQueryStats(lqs);
            ResultSetImpl rsi = new ResultSetImpl(qe, cqc, lqs, parts, this);
            qs.accumulate(lqs);
            return rsi;
        } catch(Exception e) {
            log.error(logTag, 1, "Error evaluating query", e);
            throw new SearchEngineException("Error evaluating query", e);
        } catch(Throwable t) {
            log.error(logTag, 1, "Caught throwable", t);
            throw new SearchEngineException("Throwable error evaluating query",
                    null);
        }
    }

    public QueryStats getQueryStats() {
        return qs;
    }

    public void resetQueryStats() {
        qs = new QueryStats();
    }

    public void addQueryStats(QueryStats qs) {
        this.qs.accumulate(qs);
    }

    /**
     * Gets a set of results corresponding to the document keys passed in.
     * This is a convenience method to go from document keys to something upon which
     * more complicated computations can be done.
     *
     * @param keys a collection of document keys for which we want results.
     * @return a result set that includes the documents whose keys occur in the
     * list.  All documents in the set will be assigned a score of 1.  Note that documents
     * that have been deleted will not appear in the result set.
     */
    public ResultSet getResults(Collection<String> keys) {

        //
        // We'll get a list of the active partitions and the keys that we're
        // looking for.
        List parts = invFilePartitionManager.getActivePartitions();
        List<String> remaining = new ArrayList<String>(keys);
        List sets = new ArrayList();
        for(Iterator i = parts.iterator(); i.hasNext();) {

            //
            // We'll make an array group for this partition.
            DiskPartition p = (DiskPartition) i.next();
            ArrayGroup ag = new ArrayGroup(remaining.size());
            ag.setPartition(p);

            int pos = 0;
            for(Iterator j = remaining.iterator(); j.hasNext();) {
                String key = (String) j.next();

                //
                // If there's an entry for this key, and it hasn't been
                // deleted, then add it to the group we're building.
                DocKeyEntry dke =
                        (DocKeyEntry) p.getDocumentTerm(key);
                if(dke != null) {
                    if(!p.isDeleted(dke.getID())) {
                        ag.addDoc(dke.getID());
                        j.remove();
                    }
                }
            }
            if(ag.getSize() > 0) {
                sets.add(ag);
            }
        }

        //
        // Return the result set.
        return new ResultSetImpl(this, "-score", sets);
    }

    /**
     * Gets a set of results corresponding to the document keys and scores
     * passed in.
     * This is a convenience method to go from document keys and scores to something upon which
     * more complicated computations can be done.
     *
     * @param keys a collection of document keys for which we want results.
     * @return a result set that includes the documents whose keys occur in the
     * list.  All documents in the set will be assigned a score of 1.  Note that documents
     * that have been deleted will not appear in the result set.
     */
    public ResultSet getResults(Map<String, Float> keys) {

        //
        // We'll get a list of the active partitions and the keys that we're
        // looking for.
        List parts = invFilePartitionManager.getActivePartitions();
        Map<String, Float> remaining =
                new LinkedHashMap<String, Float>(keys);
        List sets = new ArrayList();
        for(Iterator i = parts.iterator(); i.hasNext();) {

            //
            // We'll make an array group for this partition.
            DiskPartition p = (DiskPartition) i.next();
            ScoredGroup ag = new ScoredGroup(remaining.size());
            ag.setPartition(p);

            int pos = 0;
            for(Iterator<Map.Entry<String, Float>> j =
                    remaining.entrySet().iterator(); j.hasNext();) {
                Map.Entry<String, Float> e = j.next();

                //
                // If there's an entry for this key, and it hasn't been
                // deleted, then add it to the group we're building.
                DocKeyEntry dke =
                        p.getDocumentTerm(e.getKey());
                if(dke != null) {
                    if(!p.isDeleted(dke.getID())) {
                        ag.addDoc(dke.getID(), e.getValue());
                        j.remove();
                    }
                }
            }
            if(ag.getSize() > 0) {
                sets.add(ag);
            }
        }

        //
        // Return the result set.
        return new ResultSetImpl(this, "-score", sets);
    }
    
    /**
     * Builds a result set of the documents containing any of the given terms
     * in any of the given fields.
     * @param terms the terms to look for
     * @param fields the fields to look for the terms in
     * @return the set of documents that contain any of the given terms in any
     * of the given fields.
     */
    public ResultSet anyTerms(Set<String> terms, Set<String> fields) {
        List<ArrayGroup> ags = new ArrayList<ArrayGroup>();
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
        if(fields != null && fields.size() > 0) {
            feat.setFields(invFilePartitionManager.getMetaFile().getFieldArray(
                fields.toArray(new String[0])));
        }
        for(DiskPartition dp : invFilePartitionManager.getActivePartitions()) {
            QuickOr qor = new QuickOr(dp, 2048);
            for(String term : terms) {
                QueryEntry qe = dp.getTerm(term);
                if(qe == null) {
                    continue;
                }
                qor.add(qe.iterator(feat));
            }
            ags.add(qor.getGroup());
        }
        return new ResultSetImpl(this, invFilePartitionManager.getQueryConfig(), ags);
    }

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
            String pattern) {
        return invFilePartitionManager.getMatching(field, pattern);
    }

    /**
     * Gets an iterator for all the values in a field.  The values are
     * returned by the iterator in the order defined by the field type.
     *
     * @param field The name of the field who's values we need an iterator
     * for.
     * @return An iterator for the given field.  If the field is not a
     * saved field, then an iterator that will return no values will be
     * returned.
     */
    public Iterator getFieldIterator(String field) {
        return invFilePartitionManager.getFieldIterator(field);
    }

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
    public List getAllFieldValues(String field, String key) {
        return invFilePartitionManager.getAllFieldValues(field, key);
    }

    /**
     * Gets a list of the top n most frequent field values for a given
     * named field.  If n is < 1, all field values are returned, in order
     * of their frequency from most to least frequent.
     *
     * @param field the name of the field to rank
     * @param n the number of field values to return
     * @return a <code>List</code> containing field values of the appropriate
     *         type for the field, ordered by frequency
     */
    public List<FieldFrequency> getTopFieldValues(String field, int n,
            boolean ignoreCase) {
        return invFilePartitionManager.getTopFieldValues(field, n, ignoreCase);
    }
    
    public List<FieldValue> getSimilarClassifiers(String cname, int n) {
        if(classManager == null) {
            return new ArrayList<FieldValue>();
        }
        return classManager.findSimilar(cname, n);
    }
    
    public List<WeightedFeature> getSimilarClassifierTerms(String cname1, 
            String cname2, int n) {
        if(classManager == null) {
            return new ArrayList<WeightedFeature>();
        }
        return classManager.explain(cname1, cname2, n);
    }

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
    public Object getFieldValue(String field, String key) {
        return invFilePartitionManager.getFieldValue(field, key);
    }

    /**
     * Gets the names of all the fields known in the index
     *
     * @return a collection of String
     */
    public Collection getFieldNames() {
        return invFilePartitionManager.getFieldNames();
    }

    /**
     * Gets a document vector for the given key.
     * @param key The key for the document whose vector we are to retrieve.
     * @return An instance of <CODE>DocumentVector</CODE> containing the vector for this document.
     * @see DocumentVector
     */
    public DocumentVector getDocumentVector(String key) {
        return getDocumentVector(key, (String) null);
    }

    /**
     * Gets a document vector for the given key.
     * @param key The key for the document whose vector we are to retrieve.
     * @return An instance of <CODE>DocumentVector</CODE> containing the vector for this document.
     * @see DocumentVector
     */
    public DocumentVector getDocumentVector(String key, String field) {
        if(invFilePartitionManager == null) {
            return null;
        }
        return invFilePartitionManager.getDocumentVector(key, field);
    }

    public DocumentVector getDocumentVector(String key, WeightedField[] fields) {
        if(invFilePartitionManager == null) {
            return null;
        }
        return invFilePartitionManager.getDocumentVector(key, fields);
    }

    public DocumentVector getDocumentVector(Document doc, String field)
            throws SearchEngineException {

        //
        // Push the document through the indexer.
        SimpleIndexer si = getSimpleIndexer();
        si.indexDocument(doc);

        //
        // Get the document key for the indexed document.
        DocKeyEntry dke =
                ((MemoryPartition) ((AbstractPipelineImpl) si).getIndexer()).getDocumentTerm(doc.getKey());

        //
        // If this is an unfielded document key, then return the full vector,
        // no matter what field was asked for.
        if(!(dke instanceof FieldedDocKeyEntry)) {
            log.debug(logTag, 0, "here?");
            return new DocumentVectorImpl(this,
                    dke.getWeightedFeatures(queryConfig.getWeightingFunction(),
                    queryConfig.getWeightingComponents()));
        }

        //
        // Figure out the ID of the field being asked for.
        int fid = -1;
        if(field.equals("")) {
            fid = 0;
        } else {
            FieldInfo fi = invFilePartitionManager.getFieldInfo(field);
            if(fi == null) {
                fid = -1;
            } else {
                fid = fi.getID();
            }
        }

        //
        // Get a document vector for that field.
        DocumentVectorImpl dvi =
                new DocumentVectorImpl(this,
                ((FieldedDocKeyEntry) dke).getWeightedFeatures(fid,
                queryConfig.getWeightingFunction(),
                queryConfig.getWeightingComponents()));
        dvi.setField(field);
        return dvi;
    }

    public DocumentVector getDocumentVector(Document doc,
            WeightedField[] fields)
            throws SearchEngineException {

        //
        // Push the document through the indexer.
        SimpleIndexer si = getSimpleIndexer();
        si.indexDocument(doc);

        //
        // Get the document key for the indexed document.
        DocKeyEntry dke =
                ((MemoryPartition) ((AbstractPipelineImpl) si).getIndexer()).getDocumentTerm(doc.getKey());

        //
        // If this is an unfielded document key, then return the full vector,
        // no matter what field was asked for.
        if(!(dke instanceof FieldedDocKeyEntry)) {
            return new DocumentVectorImpl(this,
                    dke.getWeightedFeatures(queryConfig.getWeightingFunction(),
                    queryConfig.getWeightingComponents()));
        }

        //
        // Get a document vector for that field.
        CompositeDocumentVectorImpl dvi =
                new CompositeDocumentVectorImpl(this, dke, fields);
        return dvi;
    }

    public DocKeyEntry getDocumentTerm(String key) {
        if(invFilePartitionManager == null) {
            return null;
        }
        return invFilePartitionManager.getDocumentTerm(key);
    }

    /**
     * Gets a set of results ordered by similarity to the given document, calculated
     * by computing the euclidean distance based on the feature vector stored in the
     * given field.
     *
     * @param key the key of the document to which we'll compute similarity.
     * @param name the name of the field containing the feature vectors that
     * we'll use in the similarity computation.
     * @return a result set containing the distance between the given document
     * and all of the documents.  The scores assigned to the documents are the
     * distance scores, and so the returned set will be set to be sorted in increasing
     * order of the document score.  It is up to the application to handle the
     * scores in whatever way they deem appropriate.
     */
    public ResultSet getSimilar(String key, String name) {
        if(invFilePartitionManager == null) {
            return null;
        }

        return invFilePartitionManager.getSimilar(key, name);
    }

    /**
     * Gets the distance between two documents, based on the values stored in
     * in a given feature vector saved field.
     *
     * @param k1 the first key
     * @param k2 the second key
     * @param name the name of the feature vector field for which we want the
     * distance
     * @return the euclidean distance between the two documents' feature vectors.
     * If the field value is not defined for either of the two documents, <code>
     * Double.POSITIVE_INFINITY</code> is returned.
     */
    public double getDistance(String k1, String k2, String name) {
        if(invFilePartitionManager == null) {
            return Double.POSITIVE_INFINITY;
        }

        return invFilePartitionManager.getDistance(k1, k2, name);
    }

    /**
     * Deletes all of the data in the index.
     */
    public void purge() {
        if(invFilePartitionManager == null) {
            return;
        }
        invFilePartitionManager.purge();
    }

    /**
     * Performs a merge in the index, if one is necessary.  Returns control
     * to the caller when the merge is completed.  If you do not set the
     * <code>asyncMerges</code> property to <code>true</code>, you will
     * need to call this method periodically to cause merges to happen.  If
     * you do not, you may run out of file handles, leading to exceptions.
     *
     * @return <code>true</code> if a merge was performed,
     * <code>false</code> otherwise.
     */
    public boolean merge() {
        if(invFilePartitionManager == null) {
            return false;
        }

        PartitionManager.Merger m =
                invFilePartitionManager.getMerger(invFilePartitionManager.mergeGeometric());
        if(m == null) {
            return false;
        }
        m.merge();
        return true;
    }

    /**
     * Merges all of the partitions in the index into a single partition.
     * @throws com.sun.labs.minion.SearchEngineException If there is any error during the merge.
     */
    public void optimize()
            throws SearchEngineException {
        if(invFilePartitionManager == null) {
            return;
        }
        try {
            invFilePartitionManager.mergeAll();
        } catch(Exception e) {
            throw new SearchEngineException("Error optimizing index", e);
        }
    }

    /**
     * Attempts to recover the index after an unruly shutdown.  Makes
     * sure that lock files are removed.
     * @throws com.sun.labs.minion.SearchEngineException If there is any error during the recovery.
     */
    public void recover()
            throws SearchEngineException {
        if(invFilePartitionManager == null) {
            return;
        }
        try {
            invFilePartitionManager.recover(indexConfig.getIndexDirectory());
        } catch(java.io.IOException ioe) {
            throw new SearchEngineException("Error recovering index", ioe);
        }
    }

    /**
     * Gets an iterator for all of the non-deleted documents in the
     * collection.
     * @return An iterator that will return the keys of all
     * of the non-deleted documents in the index, as strings.  The iterators will be
     * returned in document ID order.
     */
    public Iterator<Document> getDocumentIterator() {
        return new DocumentIterator(invFilePartitionManager);
    }

    /**
     * Closes the engine.  If you wish to reuse a closed engine, you must
     * use the constructor to get a new engine!
     * @throws com.sun.labs.minion.SearchEngineException If there is any error closing the engine.
     */
    public synchronized void close()
            throws SearchEngineException {

        //
        // Tell the partition managers that it's not OK to do any more merges.
        invFilePartitionManager.noMoreMerges();
        if(classManager != null) {
            classManager.noMoreMerges();
        }
        if(clusterManager != null) {
            clusterManager.noMoreMerges();
        }

        if(pipes.length > 1) {
            for(int i = 0; i < pipes.length;
                    i++) {
                pipes[i].shutdown();
                try {
                    pipeThreads[i].join();
                } catch(InterruptedException ex) {
                    log.warn(logTag, 3,
                            "Interrupted during join for pipeline " + i);
                }
            }
        } else {
            pipes[0].shutdown();
        }

        //
        // If we have a classifier manager, then we need to dump any data
        // in the classifier memory partition.
        if(classManager != null) {
            classMemoryPartition.dump(indexConfig);
            if(clusterManager != null) {
                clusterMemoryPartition.dump(indexConfig);
            }
        }

        //
        // Shutdown the dumper for our partitions.
        dumper.finish();

        try {
            invFilePartitionManager.shutdown();
            if(classManager != null) {
                classManager.shutdown();
                if(clusterManager != null) {
                    clusterManager.shutdown();
                }
            }

            //
            // Dump the configuration into the index directory.
            cm.save(new File(indexConfig.getIndexDirectory() +
                    File.separatorChar + "config.xml"));
        } catch(java.io.IOException ioe) {
            throw new SearchEngineException("Error closing engine", ioe);
        }

        if(metaDataStore != null) {
            try {
                metaDataStore.store();
            } catch(IOException e) {
                log.warn(logTag, 3, "MetaData failed in store()", e);
                throw new SearchEngineException("MetaData could not be stored",
                        e);
            }
        }
    }

    /**
     * Gets the name of this engine, if one has been assigned by the
     * application.
     * @return The name of the engine assigned by the application, or
     * <code>null</code> if none has been assigned.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the number of documents that the index contains.
     * @return The number of documents in the index.  This number does not include documents
     * that have been deleted but whose data has not been garbage collected.
     */
    public int getNDocs() {
        return invFilePartitionManager.getNDocs();
    }

    /**
     * Gets the query configuration being used by this search engine.
     * @return The current query configuration in use by this engine.
     */
    public QueryConfig getQueryConfig() {
        return (QueryConfig) queryConfig.clone();
    }

    /**
     * Gets a simple indexer that can be used for simple indexing.
     *
     * @return a simple indexer that will index documents into this
     * engine.
     */
    public SimpleIndexer getSimpleIndexer() {
        return (SimpleIndexer) pipelineFactory.getSynchronousPipeline(this);
    }

    /**
     * Gets a pipeline that can be used for highlighting.
     * @return An instance of <code>Pipeline</code> that can be used to highlight passages in
     * documents returned by a search.
     */
    public HLPipeline getHLPipeline() {
        return pipelineFactory.getHLPipeline(this);
    }

    /**
     * Gets a string description of the search engine.
     * @return a string description of the search engine.
     */
    public String toString() {
        return "indexDir: " + indexConfig.getIndexDirectory();
    }

    /**
     * Gets the partition manager for this search engine.  This is for
     * testing purposes only and not for general consumption.
     *
     * @return The partition manager for this search engine.
     */
    public PartitionManager getPM() {
        return invFilePartitionManager;
    }

    /**
     * Gets the partition manager associated with this search engine.
     * @return The partition manager associated with this search engine.
     */
    public PartitionManager getManager() {
        return invFilePartitionManager;
    }

    /**
     * Gets the classifier manager for this search engine.
     *
     * @return the classifier manager
     */
    public ClassifierManager getClassifierManager() {
        return classManager;
    }

    /**
     * Gets the cluster manager for this search engine.
     *
     * @return the cluster manager
     */
    public ClusterManager getClusterManager() {
        return clusterManager;
    }

    /**
     * Dumps all the classifiers that have been traied since the
     * last dump, or since the searh engine started.  That is, all
     * the new classifiers that are currently only in memory will
     * be written out to disk.
     */
    public void flushClassifiers()
            throws SearchEngineException {
        if(classMemoryPartition != null) {
            classMemoryPartition.dump(indexConfig);
            if(clusterMemoryPartition != null) {
                clusterMemoryPartition.dump(indexConfig);
            }
        }
    }

    public void trainClass(ResultSet results, String className,
            String fieldName)
            throws SearchEngineException {
        trainClass(results, className, fieldName, null, null);
    }

    public void trainClass(ResultSet results, String className,
            String fieldName, String fromField)
            throws SearchEngineException {
        trainClass(results, className, fieldName, fromField, null);
    }

    public void trainClass(ResultSet results, String className,
            String fieldName, Progress p)
            throws SearchEngineException {
        trainClass(results, className, fieldName, null, p);
    }

    /**
     * Generates a classifier based on the documents in the provided
     * result set.  If the name provided is an existing class, then
     * the existing classifier will be replaced.  This method does not
     * affect any documents that have already been indexed.
     *
     * @param results the set of documents to use for training the classifier
     * @param className the name of the class to create or replace
     */
    public void trainClass(ResultSet results, String className,
            String fieldName, String fromField,
            Progress progress)
            throws SearchEngineException {
        if(classMemoryPartition != null) {
            if(!(results instanceof ResultSetImpl)) {
                //
                // Whaa?
                throw new SearchEngineException("Unsupported type of ResultSet");
            }
            classMemoryPartition.train(className, fieldName, fromField,
                    (ResultSetImpl) results, progress);
//            if(checkLowMemory()) {
            //
            // Dump after every partition.
            if(true) {
                synchronized(classMemoryPartition) {
                    classMemoryPartition.dump(indexConfig);
                    if(clusterManager != null) {
                        clusterMemoryPartition.dump(indexConfig);
                    }
                }
            }
        } else {
            throw new SearchEngineException("No classifier was specified");
        }
    }

    /**
     * Creates a manual assignment of a set of documents to a set of classes.
     * All of the documents will be assigned to all of the classes.  Manual
     * assignments are stored independently of the automatic assignment the
     * engine performs while indexing.  The documents will also automatically
     * be indexed and classified.
     *
     * @param docKeys the keys of the documents to classify
     * @param classNames the classes to assign the documents to
     */
    public void classify(String[] docKeys, String[] classNames)
            throws SearchEngineException {
        throw new SearchEngineException("SearchEngine.classify is currently unimplemented/unsupported");
    }

    /**
     * Causes the engine to reclassify all documents against the classifier
     * for the given class name.  Upon completion of the classification, a
     * short pause will occur while switching from the old set of classes
     * to the new set (the implementation of this will determine exactly
     * what the characteristics of the switch are).  This method is only needed
     * when there are existing indexed documents and there has been a change
     * to the set of classifiers.  Since reclassifying will likely be a
     * lengthy process, it is never implicit in any of the other methods.
     * (Side note: Should this be a blocking call?  If not, should there be
     * a simple event/callback mechanism to notify a user of progress?)
     *
     * @param className the class to reclassify all documents against
     */
    public void reclassifyIndex(String className)
            throws SearchEngineException {
    }

    /**
     * Returns the set of documents that was used to train the classifier
     * for the class with the provided class name.
     *
     * @param className the name of a class
     * @return the set of documents that defines the named class
     */
    public ResultSet getTrainingDocuments(String className)
            throws SearchEngineException {
        return null;
    }

    /**
     * Returns the names of the classes for which classifiers are defined.
     * If no classes are defined, an empty array is returned.
     *
     * @return an array of class names
     */
    public String[] getClasses() {
        return null;
    }
    
    public ClassifierModel getClassifier(String name) {
        if(classManager != null) {
            return classManager.getClassifier(name);
        }
        return null;
    }

    /**
     * Gets the index configuration in use by this search engine.
     * @return The index configuration in use by this search engine.
     */
    public IndexConfig getIndexConfig() {
        return indexConfig;
    }

    public QueryConfig getQC() {
        return queryConfig;
    }

    /**
     * Gets the MetaDataStore for this index.  This is a singleton
     * that stores index-related global variables.
     *
     * @return the MetaDataStore instance
     */
    public synchronized MetaDataStore getMetaDataStore()
            throws SearchEngineException {
        try {
            if(metaDataStore == null) {
                metaDataStore =
                        new MetaDataStoreImpl(indexConfig.getIndexDirectory());
            }
        } catch(IOException e) {
            log.warn(logTag, 3, "Failed to load MetaData store", e);
            throw new SearchEngineException("Failed to load MetaData store", e);
        }
        return metaDataStore;
    }

    /**
     * Determines if available memory is low.  Currently, this is defined
     * by minMemoryPercent.
     *
     * @return true if memory is low
     */
    public boolean checkLowMemory() {
        MemoryUsage mu = memBean.getHeapMemoryUsage();

        //
        // If we have less than 15% of all our memory free, then
        // memory is low.
        double freePercent = (mu.getMax() - mu.getUsed()) / (double) mu.getMax();
        if(freePercent < minMemoryPercent) {
//            log.debug(logTag, 3,
//                      String.format("Memory is low %sMB used %sMB max %.1f%% free",
//                                    toMB(mu.getUsed()), toMB(mu.getMax()),
//                                    freePercent * 100));
            return true;
        }
        return false;
    }

    protected String toMB(long x) {
        return form.format((float) x / (float) (1024 * 1024));
    }

    public ConfigurationManager getConfigurationManager() {
        return cm;
    }

    public void newProperties(PropertySheet ps)
            throws PropertyException {
        cm = ps.getConfigurationManager();
        invFilePartitionManager =
                (PartitionManager) ps.getComponent(PROP_INV_FILE_PARTITION_MANAGER);
        invFilePartitionManager.setEngine(this);

        setQueryConfig((QueryConfig) ps.getComponent(PROP_QUERY_CONFIG));

        pipelineFactory =
                (PipelineFactory) ps.getComponent(PROP_PIPELINE_FACTORY);

        //
        // Make a queue for indexing.
        indexingQueueLength =
                ps.getInt(PROP_INDEXING_QUEUE_LENGTH);
        indexingQueue =
                new ArrayBlockingQueue(indexingQueueLength);

        //
        // Make our indexing pipelines.
        numPipelines =
                ps.getInt(PROP_NUM_PIPELINES);
        pipes = new AbstractPipelineImpl[numPipelines];
        if(pipes.length == 1) {
            pipes[0] = pipelineFactory.getSynchronousPipeline(this);
        } else {
            for(int i = 0; i < pipes.length;
                    i++) {
                pipes[i] =
                        pipelineFactory.getAsynchronousPipeline(this,
                        indexingQueue);
            }
        }

        //
        // Start the threads for the pipelines.
        if(pipes.length > 1) {
            pipeThreads = new Thread[pipes.length];
            for(int i = 0; i < pipeThreads.length;
                    i++) {
                pipeThreads[i] =
                        new Thread((AsyncPipelineImpl) pipes[i], "pipeline-" + i);
                pipeThreads[i].start();
            }
        }

        //
        // Define all of our fields.
        indexConfig =
                (IndexConfig) ps.getComponent(PROP_INDEX_CONFIG);
        for(Iterator i = indexConfig.getFieldInfo().values().iterator();
                i.hasNext();) {
            FieldInfo fi = (FieldInfo) i.next();
            try {
                defineField(fi);
            } catch(SearchEngineException ex) {
                log.error(logTag, 1, "Error defining field: " + fi.getName(), ex);
            }
        }

        buildClassifiers =
                ps.getBoolean(PROP_BUILD_CLASSIFIERS);
        if(buildClassifiers) {
            classifierClassName =
                    ps.getString(PROP_CLASSIFIER_CLASS_NAME);
            try {
                Class.forName(classifierClassName);
            } catch(ClassNotFoundException ex) {
                throw new PropertyException(ps.getInstanceName(),
                        PROP_CLASSIFIER_CLASS_NAME,
                        "Cannot load class: " +
                        classifierClassName);
            }
            classManager =
                    (ClassifierManager) ps.getComponent(PROP_CLASS_MANAGER);
            classManager.setEngine(this);
            clusterManager =
                    (ClusterManager) ps.getComponent(PROP_CLUSTER_MANAGER);
            clusterManager.setEngine(this);
            classMemoryPartition =
                    (ClassifierMemoryPartition) ps.getComponent(PROP_CLASS_MEMORY_PARTITION);
            clusterMemoryPartition =
                    (ClusterMemoryPartition) ps.getComponent(PROP_CLUSTER_MEMORY_PARTITION);
        }
        minMemoryPercent =
                ps.getDouble(PROP_MIN_MEMORY_PERCENT);
        dumper = (Dumper) ps.getComponent(PROP_DUMPER);
        profilers =
                ps.getComponentList(PROP_PROFILERS);
        longIndexingRun = ps.getBoolean(PROP_LONG_INDEXING_RUN);

//        //
//        // Dump the configuration into the index directory.
//        try {
//            cm.save(new File(indexConfig.getIndexDirectory() +
//                    File.separatorChar + "config.xml"));
//        } catch(java.io.IOException ioe) {
//            log.error(logTag, 0, "Error saving config file", ioe);
//        }

    }

    /**
     * Indicates whether this search engine is being used for a long indexing
     * run.
     * 
     * @return <code>true</code> if this engine is being use for a long indexing
     * run, in which case term statistics dictionaries and document vector lengths
     * will not be calculated until the engine is shutdown.
     */
    public boolean getLongIndexingRun() {
        return longIndexingRun;
    }

    /**
     * Sets the indicator that this is a long indexing run, in which case 
     * term statistics dictionaries and document vector lengths
     * will not be calculated until the engine is shutdown.  This should be 
     * done before any indexing begins for best results.
     */
    public void setLongIndexingRun(boolean longIndexingRun) {
        this.longIndexingRun = longIndexingRun;
    }

    public void setQueryConfig(QueryConfig queryConfig) {
        this.queryConfig = queryConfig;
        queryConfig.setEngine(this);
    }

    public void export(PrintWriter o)
            throws java.io.IOException {
        o.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        o.println("<export>");

        //
        // Export the current configuration.
        cm.save(o);

        //
        // Export each partition.
        for(Iterator i = invFilePartitionManager.getActivePartitions().
                iterator(); i.hasNext();) {
            InvFileDiskPartition p =
                    (InvFileDiskPartition) i.next();
            log.debug(logTag, 0, "export: " + p);
            p.export(o);
        }
        o.println("</export>");
    }
    @ConfigComponent(type = com.sun.labs.minion.IndexConfig.class)
    public static final String PROP_INDEX_CONFIG = "index_config";

    @ConfigComponent(type = com.sun.labs.minion.QueryConfig.class)
    public static final String PROP_QUERY_CONFIG = "query_config";

    @ConfigComponent(type = com.sun.labs.minion.pipeline.PipelineFactory.class)
    public static final String PROP_PIPELINE_FACTORY = "pipeline_factory";

    private PipelineFactory pipelineFactory;

    @ConfigComponent(type = com.sun.labs.minion.indexer.partition.PartitionManager.class)
    public static final String PROP_INV_FILE_PARTITION_MANAGER =
            "inv_file_partition_manager";

    /**
     * A property indicating whether we should build classifiers while indexing
     * or not.
     */
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_BUILD_CLASSIFIERS = "build_classifiers";

    private boolean buildClassifiers;

    @ConfigComponent(type = com.sun.labs.minion.classification.ClassifierManager.class)
    public static final String PROP_CLASS_MANAGER = "class_manager";

    @ConfigComponent(type = com.sun.labs.minion.classification.ClusterManager.class)
    public static final String PROP_CLUSTER_MANAGER = "cluster_manager";

    @ConfigComponent(type = com.sun.labs.minion.indexer.partition.MemoryPartition.class)
    public static final String PROP_CLASS_MEMORY_PARTITION =
            "class_memory_partition";

    @ConfigComponent(type = com.sun.labs.minion.classification.ClusterMemoryPartition.class)
    public static final String PROP_CLUSTER_MEMORY_PARTITION =
            "cluster_memory_partition";

    @ConfigDouble(defaultValue = 0.25)
    public static final String PROP_MIN_MEMORY_PERCENT = "min_memory_percent";

    private double minMemoryPercent;

    @ConfigComponent(type = com.sun.labs.minion.indexer.partition.Dumper.class)
    public static final String PROP_DUMPER = "dumper";

    private Dumper dumper;

    @ConfigInteger(defaultValue = 1)
    public static final String PROP_NUM_PIPELINES = "num_pipelines";

    private int numPipelines;

    @ConfigInteger(defaultValue = 256)
    public static final String PROP_INDEXING_QUEUE_LENGTH =
            "indexing_queue_length";

    private int indexingQueueLength;

    @ConfigString(defaultValue = "com.sun.labs.minion.classification.Rocchio")
    public static final String PROP_CLASSIFIER_CLASS_NAME =
            "classifier_class_name";

    private String classifierClassName;

    @ConfigComponentList(type = com.sun.labs.minion.classification.Profiler.class)
    public static final String PROP_PROFILERS = "profilers";

    private List profilers;

    /**
     * A property that indicates that the search engine will be used for a long
     * indexing run with <em>no</em> querying going on during that time.  If this
     * property is set to <code>true</code> (the default is <code>false</code>),
     * then no term statistics dictionaries or document vector lengths will be
     * calculated during indexing or merging of partitions.  Additionally, at
     * shutdown, the extant partitions will be merged into a single partition and
     * then term statistics and document vector lengths will be calculated
     * for that single new partition.
     */
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_LONG_INDEXING_RUN = "long_indexing_run";

    private boolean longIndexingRun;

    private QueryStats qs;

    public List getProfilers() {
        return profilers;
    }

    public void setProfilers(List profilers) {
        this.profilers = profilers;
    }
}
