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
import com.sun.labs.minion.IndexableString;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.Date;
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
import com.sun.labs.minion.MetaDataStore;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.QueryException;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.Searcher;
import com.sun.labs.minion.SimpleIndexer;
import com.sun.labs.minion.TermStats;
import com.sun.labs.minion.WeightedField;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.retrieval.CollectionStats;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.QueryElement;
import com.sun.labs.minion.retrieval.QueryOptimizer;
import com.sun.labs.minion.retrieval.parser.LuceneTransformer;
import com.sun.labs.minion.retrieval.parser.Parser;
import com.sun.labs.minion.retrieval.parser.SimpleNode;
import com.sun.labs.minion.retrieval.parser.StrictTransformer;
import com.sun.labs.minion.retrieval.parser.Transformer;
import com.sun.labs.minion.retrieval.parser.WebTransformer;
import com.sun.labs.util.props.ConfigBoolean;
import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.ConfigDouble;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.ConfigString;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.ConfigurationManager;
import java.io.File;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.concurrent.ArrayBlockingQueue;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.DocumentIterator;
import com.sun.labs.minion.indexer.partition.Marshaller;
import com.sun.labs.minion.indexer.partition.InvFileMemoryPartition;
import com.sun.labs.minion.knowledge.KnowledgeSource;
import com.sun.labs.minion.pipeline.PipelineFactory;
import com.sun.labs.minion.query.And;
import com.sun.labs.minion.query.Element;
import com.sun.labs.minion.query.Or;
import com.sun.labs.minion.query.Range;
import com.sun.labs.minion.query.Relation;
import com.sun.labs.minion.query.StringRelation;
import com.sun.labs.minion.query.Term;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.DocumentVectorImpl;
import com.sun.labs.minion.retrieval.ScoredGroup;
import com.sun.labs.minion.retrieval.parser.LuceneParser;
import com.sun.labs.minion.retrieval.parser.StrictParser;
import com.sun.labs.minion.retrieval.parser.TokenMgrError;
import com.sun.labs.minion.retrieval.parser.WebParser;
import com.sun.labs.minion.util.CDateParser;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public class SearchEngineImpl implements SearchEngine, Configurable {

    /**
     * The log.
     */
    static final Logger logger = Logger.getLogger(
            SearchEngineImpl.class.getName());

    @ConfigComponent(type = com.sun.labs.minion.IndexConfig.class)
    public static final String PROP_INDEX_CONFIG = "index_config";

    @ConfigComponent(type = com.sun.labs.minion.QueryConfig.class)
    public static final String PROP_QUERY_CONFIG = "query_config";

    @ConfigComponent(type = com.sun.labs.minion.pipeline.PipelineFactory.class)
    public static final String PROP_PIPELINE_FACTORY = "pipeline_factory";

    private PipelineFactory pipelineFactory;

    @ConfigComponent(type =
    com.sun.labs.minion.indexer.partition.PartitionManager.class)
    public static final String PROP_INV_FILE_PARTITION_MANAGER =
            "inv_file_partition_manager";

    /**
     * A property indicating whether we should build classifiers while indexing
     * or not.
     */
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_BUILD_CLASSIFIERS = "build_classifiers";

    private boolean buildClassifiers;

    @ConfigDouble(defaultValue = 0.30)
    public static final String PROP_MIN_MEMORY_PERCENT = "min_memory_percent";

    private double minMemoryPercent;

    @ConfigComponent(type = com.sun.labs.minion.indexer.partition.Marshaller.class)
    public static final String PROP_MARSHALLER = "marshaller";

    private Marshaller marshaller;
    
    @ConfigInteger(defaultValue=-1)
    public static final String PROP_DOCS_PER_PARTITION = "docs_per_partition";
    
    private int docsPerPartition;

    @ConfigInteger(defaultValue = 1)
    public static final String PROP_NUM_INDEXING_THREADS = "num_indexing_threads";

    private int numIndexingThreads;

    @ConfigInteger(defaultValue = 256)
    public static final String PROP_INDEXING_QUEUE_LENGTH = "indexing_queue_length";

    private int indexingQueueLength;

    @ConfigString(defaultValue = "com.sun.labs.minion.classification.Rocchio")
    public static final String PROP_CLASSIFIER_CLASS_NAME =
            "classifier_class_name";

    private String classifierClassName;

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
     * The pipelines to use for indexing.
     */
    protected Indexer[] indexers;

    /**
     * Threads to hold run our pipelines.
     */
    protected Thread[] indexingThreads;
    
    /**
     * Memory partitions to use during indexing.
     */
    protected BlockingQueue<InvFileMemoryPartition> mpPool;

    /**
     * The configuration name for this search engine.
     */
    private String name;

    private MemoryMXBean memBean;

    long hwMark;
    
    private Indexable flushDoc = new IndexableMap("SearchEngineImpl-flush");
    
    private Indexable closeDoc = new IndexableMap("SearchEngineImpl-close");

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

    public Set<String> getTermVariations(String term) {
        Set<String> ret = null;
        KnowledgeSource ks = queryConfig.getKnowledgeSource();
        if(ks != null) {
            ret = queryConfig.getKnowledgeSource().variantsOf(term);
        } else {
            ret = new HashSet<String>();
        }
        ret.add(term);
        return ret;
    }

    public TermStats getTermStats(String term, String field) {
        return invFilePartitionManager.getTermStats(term, field);
    }

    public TermStats getTermStats(String term, FieldInfo field) {
        return invFilePartitionManager.getTermStats(term, field);
    }

    public Document getDocument(String key) {
        QueryEntry dke =
                (QueryEntry) invFilePartitionManager.getDocumentTerm(key);

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
        List<String> remaining = new ArrayList<String>(keys);
        List<Document> docs = new ArrayList<Document>();
        for(DiskPartition p : invFilePartitionManager.getActivePartitions()) {

            if(p.isClosed()) {
                continue;
            }
            for(Iterator<String> j = remaining.iterator(); j.hasNext();) {

                //
                // If there's an entry for this key, and it hasn't been
                // deleted, then add it to the group we're building.
                QueryEntry dke =
                        (QueryEntry) p.getDocumentDictionary().get(j.next());
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
        QueryEntry dke =
                (QueryEntry) invFilePartitionManager.getDocumentTerm(key);

        //
        // If there's an existing, un-deleted document, then return null.
        if(dke != null && !((DiskPartition) dke.getPartition()).isDeleted(dke.
                getID())) {
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
    public void index(String key, Map document) throws SearchEngineException {
        index(new IndexableMap(key, document));
    }

    public void index(Indexable doc) throws SearchEngineException {
        int hash = doc.getKey().hashCode();
        //
        // Docs with the same key hash always go to the same indexer, so that 
        // we get updates in the right order to the underlying index.
        indexers[hash % indexers.length].index(doc);
        checkDump();
    }

    public void index(Document document) throws SearchEngineException {
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
    protected void dump() throws SearchEngineException {
        for(int i = 0; i < indexers.length; i++) {
            indexers[i].marshall();
        }
    }

    /**
     * Flushes the indexed material currently held in memory to the disk,
     * making it available for searching.
     * @throws com.sun.labs.minion.SearchEngineException If there is any error
     * flushing the in-memory data.
     */
    public synchronized void flush() throws SearchEngineException {

        if(invFilePartitionManager == null) {
            return;
        }

        for(Indexer indexer : indexers) {
            indexer.index(flushDoc);
        }

        if(metaDataStore != null) {
            try {
                metaDataStore.store();
            } catch(IOException e) {
                logger.log(Level.WARNING, "MetaData failed in store()", e);
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
                Searcher.Operator.AND, Searcher.Grammar.STRICT);
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
                Searcher.Operator.AND, Searcher.Grammar.STRICT);
    }

    @Override
    public ResultSet search(String query, String sortOrder, int defaultOperator,
                            int grammar) throws SearchEngineException {
        return search(query, sortOrder,
                Searcher.Operator.values()[defaultOperator],
                Searcher.Grammar.values()[grammar]);
    }



    @Override
    public ResultSet search(String query, String sortOrder,
            Searcher.Operator defaultOperator, Searcher.Grammar grammar)
            throws SearchEngineException {

        QueryElement qe = null;
        SimpleNode parseTree = null;

        Parser p = null;
        Transformer xer = null;
        switch(grammar) {
            case WEB:
                p = new WebParser(new StringReader(query));
                xer = new WebTransformer();
                break;
            case STRICT:
                p = new StrictParser(new StringReader(query));
                xer = new StrictTransformer();
                break;
            case LUCENE:
                p = new LuceneParser(new StringReader(query));
                xer = new LuceneTransformer();
                break;
            default:
                throw new SearchEngineException("Unknown grammar specified: " +
                        grammar);
        }

        try {
            parseTree = (SimpleNode) p.doParse();
        } catch(com.sun.labs.minion.retrieval.parser.ParseException ex) {
            logger.log(Level.FINE, "Error parsing query", ex);

            com.sun.labs.minion.retrieval.parser.Token badToken =
                    ex.currentToken;
            while (badToken.next != null) {
                badToken = badToken.next;
            }
            
            throw new com.sun.labs.minion.ParseException(
                    badToken.image,
                    badToken.beginColumn);
        } catch(TokenMgrError tme) {
            logger.log(Level.SEVERE, "Error parsing query", tme);
            throw new SearchEngineException("Error parsing query: " + tme);
        }

        try {
            qe = xer.transformTree(parseTree, defaultOperator);
        } catch(java.text.ParseException ex) {
            logger.log(Level.SEVERE, "Error transforming query", ex);
            throw new SearchEngineException("Error transforming query", ex);
        }

        if(qe == null) {
            //
            // The query construction code returned an empty query
            throw new SearchEngineException(
                    "Error evaluating query: Query is empty");
        }

        //
        // Try to optimize the query some.
        QueryOptimizer opti = new QueryOptimizer();
        qe = opti.optimize(qe);

        return search(qe, sortOrder);
    }

    private ResultSet search(QueryElement qe, String sortOrder) throws
            SearchEngineException {

        try {
            CollectionStats cs =
                    new CollectionStats(invFilePartitionManager);
            Collection<DiskPartition> parts = invFilePartitionManager.
                    getActivePartitions();
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
            logger.log(Level.SEVERE, "Error evaluating query: " + qe, e);
            throw new SearchEngineException("Error evaluating query", e);
        } catch(Throwable t) {
            logger.log(Level.SEVERE, "Caught throwable", t);
            throw new SearchEngineException("Throwable error evaluating query",
                                            null);
        }
    }

    public ResultSet search(Element el) throws SearchEngineException {
        return search(el, "-score");
    }

    public ResultSet search(Element el, String sortOrder) throws
            SearchEngineException {
        checkQuery(null, el);
        return search(el.getQueryElement(), sortOrder);
    }

    /**
     * Checks to make sure that the given query makes sense for this engine.
     * @param el the query element to check.
     */
    private void checkQuery(CDateParser dp, Element el) throws QueryException {
        if(el instanceof Relation && !(el instanceof StringRelation)) {
            Relation r = (Relation) el;
            String field = r.getField();
            String val = r.getValue();

            //
            // Check if the field exists.
            FieldInfo fi = getFieldInfo(field);
            if(fi == null) {
                throw new QueryException(String.format(
                        "Field %s is not defined for relation %s",
                        field, r));
            }

            //
            // The field exists, lets make sure that the type makes sense for the
            // types where we need to parsing.
            checkType(dp, fi, r.getOperator(), val);
        } else if(el instanceof Range) {
            Range range = (Range) el;
            String field = range.getField();

            //
            // Check if the field exists.
            FieldInfo fi = getFieldInfo(field);
            if(fi == null) {
                throw new QueryException(String.format(
                        "Field %s is not defined for range operator %s",
                        field, range));
            }

            checkType(dp, fi, range.getLeftOperator(), range.getLeftValue());
            checkType(dp, fi, range.getRightOperator(), range.getRightValue());
        } else if(el instanceof Term) {
        }

    }

    private void checkType(CDateParser dp,
                           FieldInfo fi,
                           Relation.Operator op,
                           String val) throws QueryException {
        switch(fi.getType()) {
            case DATE:
                if(dp == null) {
                    dp = new CDateParser();
                }
                try {
                    dp.parse(val);
                } catch(java.text.ParseException pe) {
                    throw new QueryException(String.format(
                            "Value in relation %s %s %s is not parseable as a date, "
                            + "where %s is a date field",
                            fi.getName(),
                            op.getRep(),
                            val, fi.getName()));
                }
                break;
            case INTEGER:
                try {
                    Long.parseLong(val);
                } catch(NumberFormatException nfe) {
                    throw new QueryException(String.format(
                            "Value in relation %s %s %s is not parseable as a "
                            + "64 bit integer, where %s is an integer field",
                            fi.getName(),
                            op.getRep(),
                            val, fi.getName()));

                }
                break;
            case FLOAT:
                try {
                    Double.parseDouble(val);
                } catch(NumberFormatException nfe) {
                    throw new QueryException(String.format(
                            "Value in relation %s %s %s is not parseable as a "
                            + "64 bit float, where %s is a float field",
                            fi.getName(),
                            op.getRep(),
                            val,
                            fi.getName()));

                }
                break;
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
        Collection<DiskPartition> parts = invFilePartitionManager.
                getActivePartitions();
        List<String> remaining = new ArrayList<String>(keys);
        List sets = new ArrayList();
        for(DiskPartition p : parts) {
            if(p.isClosed()) {
                continue;
            }

            //
            // We'll make an array group for this partition.
            ArrayGroup ag = new ArrayGroup(remaining.size());
            ag.setPartition(p);

            for(Iterator j = remaining.iterator(); j.hasNext();) {
                String key = (String) j.next();

                //
                // If there's an entry for this key, and it hasn't been
                // deleted, then add it to the group we're building.
                QueryEntry dke =
                        (QueryEntry) p.getDocumentDictionary().get(key);
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
        Collection<DiskPartition> parts = invFilePartitionManager.
                getActivePartitions();
        Map<String, Float> remaining = new LinkedHashMap<String, Float>(keys);
        List sets = new ArrayList();
        for(DiskPartition p : parts) {
            if(p.isClosed()) {
                continue;
            }

            ScoredGroup ag = new ScoredGroup(remaining.size());
            ag.setPartition(p);

            for(Iterator<Map.Entry<String, Float>> j =
                    remaining.entrySet().iterator(); j.hasNext();) {
                Map.Entry<String, Float> e = j.next();

                //
                // If there's an entry for this key, and it hasn't been
                // deleted, then add it to the group we're building.
                QueryEntry dke =
                        p.getDocumentDictionary().get(e.getKey());
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
    public ResultSet anyTerms(Collection<String> terms,
                              Collection<String> fields) throws
            SearchEngineException {
        return anyTerms(terms, fields, false);
    }

    /**
     * Builds a result set of the documents containing any of the given terms
     * in any of the given fields.
     * @param terms the terms to look for
     * @param fields the fields to look for the terms in
     * @param scored if <code>true</code> then return a scored set, otherwise
     * return a strict boolean set.
     * @return the set of documents that contain any of the given terms in any
     * of the given fields.
     */
    public ResultSet anyTerms(Collection<String> terms,
                              Collection<String> fields,
                              boolean scored) throws SearchEngineException {
        Or or = new Or();
        for(String t : terms) {
            Term term = new Term(t, null);
            term.setFields(fields);
            term.setStrict(!scored);
            or.add(term);
        }
        or.setStrict(!scored);
        return search(or, "-score");
    }

    /**
     * Builds a result set containing all of the given terms in any of the given
     * fields.
     * @param terms the terms that we want to find
     * @param fields the fields that we must find the terms in
     * @throws SearchEngineException if there is an error during the search.
     */
    public ResultSet allTerms(Collection<String> terms,
                              Collection<String> fields) throws
            SearchEngineException {
        return allTerms(terms, fields, true);
    }

    /**
     * Builds a result set containing all of the given terms in any of the given
     * fields.
     * @param terms the terms that we want to find
     * @param fields the fields that we must find the terms in
     * @throws SearchEngineException if there is an error during the search.
     */
    public ResultSet allTerms(Collection<String> terms,
                              Collection<String> fields,
                              boolean scored) throws SearchEngineException {
        And and = new And();
        for(String t : terms) {
            Term term = new Term(t, null);
            term.setFields(fields);
            term.setStrict(!scored);
            and.add(term);
        }
        and.setStrict(!scored);
        return search(and, "-score");
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
//        IndexEntry dke =
//                ((MemoryPartition) ((PipelineImpl) si).getIndexer()).
//                getDocumentDictionary().get(doc.getKey());

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
        DocumentVectorImpl dvi = null;
//                new DocumentVectorImpl(this,
//                ((FieldedDocKeyEntry) dke).getWeightedFeatures(fid,
//                queryConfig.getWeightingFunction(),
//                queryConfig.getWeightingComponents()));
//        dvi.setField(field);
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
//        IndexEntry dke =
//                ((MemoryPartition) ((PipelineImpl) si).getIndexer()).
//                getDocumentDictionary().get(doc.getKey());

        return null;
    }

    public QueryEntry getDocumentTerm(String key) {
        if(invFilePartitionManager == null) {
            return null;
        }
        return invFilePartitionManager.getDocumentTerm(key);
    }

    /**
     * Deletes all of the data in the index.
     */
    public void purge() {

        if(invFilePartitionManager != null) {
            invFilePartitionManager.purge();
            for(int i = 0; i < indexers.length; i++) {
                indexers[i].purge();
            }
        }
        try {
            MetaDataStoreImpl mds = (MetaDataStoreImpl) getMetaDataStore();
            mds.purge();
        } catch(SearchEngineException e) {
            logger.log(Level.INFO, "Failed to purge meta data store", e);
        } catch(IOException e) {
            logger.log(Level.INFO, "Failed to purge meta data store", e);
        }

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
                invFilePartitionManager.getMerger(invFilePartitionManager.
                mergeGeometric());
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
    public void optimize() throws SearchEngineException {
        if(invFilePartitionManager == null) {
            return;
        }
        try {
            invFilePartitionManager.mergeAll();
            invFilePartitionManager.recalculateTermStats();
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

        //
        // Tell the indexers to close when they get to the end of their queues.
        for(Indexer indexer : indexers) {
            indexer.index(closeDoc);
        }
        
        //
        // Wait for the threads to finish.
        for(int i = 0; i < indexers.length; i++) {
            try {
                indexingThreads[i].join();
            } catch(InterruptedException ex) {
                logger.warning(String.format("Interrupted during join for indexer %d", i));
            }
        }

        //
        // If we have a classifier manager, then we need to dump any data
        // in the classifier memory partition.

        //
        // Shutdown the dumper for our partitions.
        marshaller.finish();
        
        //
        // If we didn't do it while we were indexing, then calculate the document
        // vectors and term stats now.
        if(longIndexingRun) {
            try {
                logger.info(String.format("Optimizing after indexing run"));
                invFilePartitionManager.setLongIndexingRun(false);
                optimize();
            } catch(Exception ex) {
                logger.log(Level.SEVERE, String.format("Error optimizing after long indexing run"), ex);
            }
        }
        
        try {
            invFilePartitionManager.close();

            //
            // Dump the configuration into the index directory.
            cm.save(new File(indexConfig.getIndexDirectory()
                    + File.separatorChar + "config.xml"));
        } catch(java.io.IOException ioe) {
            throw new SearchEngineException("Error closing engine", ioe);
        }

        if(metaDataStore != null) {
            try {
                metaDataStore.store();
            } catch(IOException e) {
                logger.log(Level.WARNING, "MetaData failed in store()", e);
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
        return new Indexer();
    }

    /**
     * Gets a pipeline that can be used for highlighting.
     * @return An instance of <code>Pipeline</code> that can be used to highlight passages in
     * documents returned by a search.
     */
    public HLPipeline getHLPipeline() {
        return pipelineFactory.getHLPipeline();
    }

    /**
     * Gets a string description of the search engine.
     * @return a string description of the search engine.
     */
    @Override
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
            logger.log(Level.WARNING, "Failed to load MetaData store", e);
            throw new SearchEngineException("Failed to load MetaData store", e);
        }
        return metaDataStore;
    }

    
    private long bigGigLimit = (long) (4.5 * 1000L * 1000L * 1000L);
    private long afterGC = 0;
    private int state = 0;
    
    /**
     * Determines if available memory is low.  Currently, this is defined
     * by minMemoryPercent.
     *
     * @return true if memory is low
     */
    public boolean checkLowMemory() {
//        MemoryUsage mu = memBean.getHeapMemoryUsage();
//
//        if(state == 1 && mu.getUsed() > afterGC) {
//            logger.info(String.format("Big heap at %,d", mu.getUsed()));
//            HeapDumper.dumpHeap("bigheap", false);
//        } else if(mu.getUsed() > bigGigLimit) {
//            if(state == 0) {
//                System.gc();
//                mu = memBean.getHeapMemoryUsage();
//                afterGC = mu.getUsed();
//                logger.info(String.format("Baseline at %,d", afterGC));
//                afterGC += (long) (0.7 * 1000L * 1000L * 1000L);
//                HeapDumper.dumpHeap("baseline", false);
//                state++;
//            }
//        }
        
        //
        // If we have less than 15% of all our memory free, then
        // memory is low.
//        double freePercent = (mu.getMax() - mu.getUsed()) / (double) mu.getMax();
//        if(freePercent < 0.01) {
//            logger.info(String.format(
//                    "Memory is low %.1fMB used %.1fMB max %.1f%% free",
//                    toMB(mu.getUsed()), toMB(mu.getMax()),
//                    freePercent * 100));
//            return true;
//        }
        return false;
    }

    protected double toMB(long x) {
        return x / 1024.0 / 1024.0;
    }

    public ConfigurationManager getConfigurationManager() {
        return cm;
    }

    public void newProperties(PropertySheet ps)
            throws PropertyException {
        cm = ps.getConfigurationManager();
        invFilePartitionManager =
                (PartitionManager) ps.getComponent(
                PROP_INV_FILE_PARTITION_MANAGER);
        invFilePartitionManager.setEngine(this);

        setQueryConfig((QueryConfig) ps.getComponent(PROP_QUERY_CONFIG));

        pipelineFactory =
                (PipelineFactory) ps.getComponent(PROP_PIPELINE_FACTORY);

        //
        // The indexing queue length for the threads.
        indexingQueueLength =
                ps.getInt(PROP_INDEXING_QUEUE_LENGTH);

        //
        // Make our indexing pipelines.
        numIndexingThreads =
                ps.getInt(PROP_NUM_INDEXING_THREADS);
        docsPerPartition = ps.getInt(PROP_DOCS_PER_PARTITION);

        //
        // Make a pool of memory partitions that can be re-used over time, two
        // per indexing thread (one for indexing, one for dumping.)
        mpPool = new ArrayBlockingQueue<InvFileMemoryPartition>(numIndexingThreads * 2);
        for(int i = 0; i < numIndexingThreads * 2; i++) {
            InvFileMemoryPartition mp = new InvFileMemoryPartition(invFilePartitionManager);
            mp.setPartitionName("IF-" + i);
            mpPool.add(mp);
        }
        
        indexers = new Indexer[numIndexingThreads];
        //
        // Start threads for the indexers.
        indexingThreads = new Thread[indexers.length];
        for(int i = 0; i < indexers.length; i++) {
            indexers[i] = new Indexer(docsPerPartition, indexingQueueLength);
            indexingThreads[i] = new Thread(indexers[i]);
            indexingThreads[i].setName("Indexer-" + i);
            indexingThreads[i].start();
        }
        
        //
        // Define all of our fields.
        indexConfig = (IndexConfig) ps.getComponent(PROP_INDEX_CONFIG);
        for(FieldInfo fi : indexConfig.getFieldInfo().values()) {
            try {
                defineField(fi);
            } catch(SearchEngineException ex) {
                logger.log(Level.SEVERE, "Error defining field: " + fi.getName(),
                           ex);
            }
        }

        buildClassifiers = ps.getBoolean(PROP_BUILD_CLASSIFIERS);
        minMemoryPercent = ps.getDouble(PROP_MIN_MEMORY_PERCENT);
        marshaller = (Marshaller) ps.getComponent(PROP_MARSHALLER);
        marshaller.setMemoryPartitionQueue(mpPool);
    }

    /**
     * Indicates whether this search engine is being used for a long indexing
     * run.
     * 
     * @return <code>true</code> if this engine is being use for a long indexing
     * run, in which case term statistics dictionaries and document vector lengths
     * will not be calculated until the engine is shutdown.
     */
    public boolean isLongIndexingRun() {
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
        marshaller.setLongIndexingRun(longIndexingRun);
        invFilePartitionManager.setLongIndexingRun(longIndexingRun);
    }
    
    public void setDocsPerPartition(int docsPerPartition) {
        for(Indexer indexer : indexers) {
            indexer.setDocsPerPart(docsPerPartition);
        }
    }

    public void setQueryConfig(QueryConfig queryConfig) {
        this.queryConfig = queryConfig;
        queryConfig.setEngine(this);
    }

    /**
     * A class to index documents, either as part of a thread or used directly.
     */
    private class Indexer implements Runnable, SimpleIndexer {

        private InvFileMemoryPartition part;
        
        private BlockingQueue<Indexable> indexingQueue;

        private boolean finished;

        private boolean flushRequested;
        
        private String key;
        
        private int nIndexed;
        
        private int docsPerPart;
        
        public Indexer() {
            this(-1, 2);
        }
        
        public Indexer(int docsPerPart, int queueSize) {
            this.docsPerPart = docsPerPart;
            indexingQueue = new ArrayBlockingQueue<Indexable>(queueSize);
            try {
                part = mpPool.take();
                part.start();
            } catch(InterruptedException ex) {
                throw new IllegalStateException("Error getting memory partition");
            }
        }
        
        public int getDocsPerPart() {
            return docsPerPart;
        }

        public void setDocsPerPart(int docsPerPart) {
            this.docsPerPart = docsPerPart;
        }

        public void run() {
            while(!finished) {
                try {
                    Indexable doc = indexingQueue.poll(100, TimeUnit.MILLISECONDS);
                    
                    //
                    // Process a flush request.
                    if(doc == flushDoc) {
                        marshall();
                        continue;
                    }
                    
                    //
                    // We're done.  Flush and return.
                    if(doc == closeDoc) {
                        marshall();
                        break;
                    }
                    
                    //
                    // Regular kind of document.
                    if(doc != null) {
                        indexInternal(doc);
                        if(docsPerPart > 0 && nIndexed == docsPerPart) {
                            marshall();
                        }
                    }
                } catch(InterruptedException ex) {
                    return;
                }
            }
        }
        
        public void index(Indexable doc) {
            try {
                indexingQueue.put(doc);
//                if(!indexingQueue.offer(doc, 5, TimeUnit.SECONDS)) {
//                    logger.log(Level.SEVERE, String.format("Unable to put %s on indexing queue for %s", 
//                            doc.getKey(),
//                            Thread.currentThread().getName()));
//                }
            } catch(InterruptedException ex) {
                Logger.getLogger(SearchEngineImpl.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public void indexInternal(Indexable doc) {
            if(part == null) {
                throw new IllegalStateException("Can't index without a partition");
            }
            part.index(doc);
            nIndexed++;
        }
        
        public void flush() {
            flushRequested = true;
        }

        public void purge() {
            part = new InvFileMemoryPartition(invFilePartitionManager);
        }

        public void marshall() {
            marshaller.marshall(part);
            try {
                part = mpPool.take();
                part.start();
            } catch(InterruptedException ex) {
                logger.log(Level.SEVERE, String.format("Error getting memory partition"), ex);
            }
            nIndexed = 0;
        }

        public void finish() {
            finished = true;
        }

        public void indexDocument(Indexable doc) throws SearchEngineException {
            index(doc);
        }

        public void indexDocument(Document doc) throws SearchEngineException {
        }

        public void startDocument(String key) {
            this.key = key;
            part.startDocument(key);
        }

        public void addField(String name, String value) {
            FieldInfo fi = getFieldInfo(name);
            if(fi == null) {
                logger.warning(String.format("Unknown field %s for %s", name,
                                             key));
                return;
            }
            part.addField(fi, value);
        }

        public void addField(String name, IndexableString value) {
            FieldInfo fi = getFieldInfo(name);
            if(fi == null) {
                logger.warning(String.format("Unknown field %s for %s", name,
                                             key));
                return;
            }
            part.addField(fi, value);
        }

        public void addField(String name, Date value) {
            FieldInfo fi = getFieldInfo(name);
            if(fi == null) {
                logger.warning(String.format("Unknown field %s for %s", name,
                                             key));
                return;
            }
            part.addField(fi, value);
        }

        public void addField(String name, Long value) {
            FieldInfo fi = getFieldInfo(name);
            if(fi == null) {
                logger.warning(String.format("Unknown field %s for %s", name,
                                             key));
                return;
            }
            part.addField(fi, value);
        }

        public void addField(String name, Integer value) {
            FieldInfo fi = getFieldInfo(name);
            if(fi == null) {
                logger.warning(String.format("Unknown field %s for %s", name,
                                             key));
                return;
            }
            part.addField(fi, value);
        }

        public void addField(String name, Double value) {
            FieldInfo fi = getFieldInfo(name);
            if(fi == null) {
                logger.warning(String.format("Unknown field %s for %s", name,
                                             key));
                return;
            }
            part.addField(fi, value);
        }

        public void addField(String name, Float value) {
            FieldInfo fi = getFieldInfo(name);
            if(fi == null) {
                logger.warning(String.format("Unknown field %s for %s", name,
                                             key));
                return;
            }
            part.addField(fi, value);
        }

        public void addField(String name, Object[] values) {
            FieldInfo fi = getFieldInfo(name);
            if(fi == null) {
                logger.warning(String.format("Unknown field %s for %s", name,
                                             key));
                return;
            }
            for(Object value : values) {
                part.addField(fi, value);
            }
        }

        public void addField(String name,
                             Collection<Object> values) {
            FieldInfo fi = getFieldInfo(name);
            if(fi == null) {
                logger.warning(String.format("Unknown field %s for %s", name,
                                             key));
            }
            for(Object value : values) {
                part.addField(fi, value);
            }
        }

        public void addTerm(String term) {
            addTerm(null, term, 1);
        }

        public void addTerm(String term, int count) {
            addTerm(null, term, count);
        }

        public void addTerm(String field, String term, int count) {
            part.addTerm(field, term, count);
        }

        public void endDocument() {
        }

        public boolean isIndexed(String key) {
            return part.isIndexed(key) || invFilePartitionManager.isIndexed(key);
        }
    }
}
