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

package com.sun.labs.minion.test.regression;

import com.sun.labs.minion.SearchEngineFactory;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.entry.CasedDFOEntry;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.util.Getopt;

import com.sun.labs.minion.IndexConfig;
import com.sun.labs.minion.Log;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.util.props.ConfigurationManager;
import com.sun.labs.util.props.PropertyException;

/**
 * Class DVPostingsTester compares the DocumentVectorPostings in the index
 * with the frequencies of terms in the main dictionary.<br>
 * This only seems to work for lowercase terms.
 * @author Bernard Horan
 *
 */
public class DVPostingsTester {
    /**
     * The inner class DocumentPartition is an in-memory lightweight simulation of
     * a DiskPartition. It provides the ability to maintain a collection of documents.
     *
     */
    public class DocumentPartition {
        /**
         * The documents maintained by this partition
         */
        private Document[] documents;
        
        /**
         * Create the empty collection of documents to be maintained by this DocumentPartition
         * @param partition a DiskPartition, which can be asked for the documents it contains
         */
        public void createDocuments(DiskPartition partition) {
            //
            //Document ids start at 1
            documents = new Document[partition.getNDocs() + 1];
            Iterator di = partition.getDocumentIterator();
            while (di.hasNext()) {
                /* get the first document in the partition */
                DocKeyEntry dke = (DocKeyEntry) di.next();
                int id = dke.getID();
                if (!partition.isDeleted(dke.getID())) {
                    Document document = new Document(dke.getName());
                    //message("Created document: " + document + " with id: " + id);
                    if (id >= documents.length) {
                        Document[] newDocuments = new Document[id + 1];
                        System.arraycopy(documents, 0, newDocuments, 0,
                                documents.length);
                        documents = newDocuments;
                    }
                    documents[id] = document;
                }
                
            }
        }
        
        /**
         * Returns the document whose identifier is documentId
         * @param documentId the integer identifier for the document (starts at 1)
         * @return a Document
         */
        public Document getDocument(int documentId) {
            return documents[documentId];
        }
        
        /**
         * Accumulate the frequencies from the main dictionary
         * @param mainDictionaryIterator an iterator over the main dictionary
         * @param partition the partition that contains the documents whose terms are in the main dictionary
         */
        public void createFrequenciesFromMainDictionaryIterator( DictionaryIterator mainDictionaryIterator, DiskPartition partition) {
            //
            // Create a features object to get the appropriate postings from an
            // entry
            PostingsIteratorFeatures features = new PostingsIteratorFeatures();
            features.setCaseSensitive(false);
            
            while (mainDictionaryIterator.hasNext()) {
                CasedDFOEntry entry = (CasedDFOEntry) mainDictionaryIterator.next();
                PostingsIterator pIterator = entry.iterator(features);
                if (pIterator == null) {
                    continue;
                }
                
                while (pIterator.next()) {
                    int docId = pIterator.getID();
                    if (!partition.isDeleted(docId)) {
                        Document document = getDocument(docId);
                        int frequency = document .getFrequencyFromDictionary(entry.getName());
                        document.setFrequencyFromDictionary(entry.getName(), frequency + pIterator.getFreq());
                    }
                    
                }
            }
        }
        
        /**
         * Compare the Frequencies of the document vector postings with those
         * accumulated from the main dictionary
         * @param documentIterator an iterator that contains all the documents in a partition
         * @param partition the partition containing the documents available from the documentIterator
         */
        public void compareFrequenciesFromDocumentIterator( Iterator documentIterator, DiskPartition partition) {
            int failed = 0;
            int succeeded = 0;
            //
            // Create a features object to get the appropriate postings from an
            // entry
            PostingsIteratorFeatures features = new PostingsIteratorFeatures();
            features.setCaseSensitive(false);
            
            while (documentIterator.hasNext()) {
                DocKeyEntry dkEntry = (DocKeyEntry) documentIterator.next();
                int docId = dkEntry.getID();
                if (!partition.isDeleted(docId)) {
                    PostingsIterator pIterator = dkEntry.iterator(features);
                    if (pIterator == null) {
                        continue;
                    }
                    Document document = getDocument(docId);
                    
                    while (pIterator.next()) {
                        int termId = pIterator.getID();
                        Object term = partition.getTerm(termId).getName();
                        int frequency = document.getFrequencyFromDictionary(term);
                        
                        if (pIterator.getFreq() == frequency) {
                            //System.out.println("count of " + term + " in " + document.name + " is same");
                            succeeded++;
                        } else {
                            //System.out.println("count of " + term + " in " + document.name + " is different");
                            failed++;
                        }
                        
                    }
                    System.out.println("Document " + document.name + " failures: " + failed + " successes: " + succeeded);
                }
            }
            
        }
    }
    
    /**
     * A Document has a name and maintains a collection of ordered words.
     *
     */
    class Document {
        /**
         * The name of the document
         */
        private Object name;
        
        /**
         * Maps to contain the frequencies retrieved from the index and
         * recreated from the documents.
         */
        private SortedMap frequenciesFromDictionary;
        /**
         * Creates a new instance of Document with a specified name
         * @param name an Object that identifies this Document
         */
        public Document(Object name) {
            this.name = name;
            //message("Creating " + this);
            frequenciesFromDictionary = new TreeMap();
        }
        
            /* (non-Javadoc)
             * @see java.lang.Object#toString()
             */
        public String toString() {
            return super.toString() + " name: " + name;
        }
        
        /**
         * Set the frequency of the term
         * @param aName the term
         * @param i its frequency
         */
        private void setFrequencyFromDictionary(Object aName, int i) {
            frequenciesFromDictionary.put(aName, i);
            
        }
        
        /**
         * Get he frequency for a given term
         * @param aName the term
         * @return the frequency for <code>aName</code>
         */
        private int getFrequencyFromDictionary(Object aName) {
            Integer freq = (Integer) frequenciesFromDictionary.get(aName);
            if (freq == null) {
                return 0;
            } else {
                return freq.intValue();
            }
        }
        
    }
    
    /**
     * For tracing
     */
    protected final static boolean DEBUG = true;
    /**
     * Tag for the log
     */
    protected static final String logTag = "DVPostingsTester";
    /**
     * The configuration for the IndexInverter
     */
    private static IndexConfig indexConfig = new IndexConfig();
    /**
     * The log onto which messages <b>should</b> be written
     */
    protected static Log log;
    /**
     * A SearchEngine
     */
    private SearchEngineImpl engine;
    
    /**
     * The DocumentPartitions that represent the partitions in the index
     */
    private DocumentPartition[] partitions;
    /**
     * This method creates the frequencies from the main dictionary of the index
     * @param manager the partition manager that manages the partitions in the index
     */
    private void createFrequenciesFromMainDictionary(PartitionManager manager) {
        
        
        Iterator partitionIterator = manager.getActivePartitions().iterator();
        int partitionNumber = 0;
        while (partitionIterator.hasNext()) {
            DiskPartition partition = (DiskPartition) partitionIterator.next();
            //message("partition: " + partition);
            DocumentPartition documentPartition = partitions[partitionNumber++];
            
            DictionaryIterator mainDictionaryIterator = (DictionaryIterator) partition.getMainDictionaryIterator();
            documentPartition.createFrequenciesFromMainDictionaryIterator(mainDictionaryIterator, partition);
            
        }
    }
    
    /**
     * DEBUG information
     * @param string string to be output
     */
    void message(String string) {
        if (DEBUG) {
            System.out.println(string);
        }
        
    }
    
    /**
     * Run the test
     */
    public void test() {
        PartitionManager manager = engine.getPM();
        createDocumentList(manager);
        createFrequenciesFromMainDictionary(manager);
        compareFrequenciesFromPostings(manager);
    }
    
    /**
     * Compare the frequencies from the postings with those that we should
     * have already accumulated from the main dictionary.
     * @param manager the Partition Manager
     */
    private void compareFrequenciesFromPostings(PartitionManager manager) {
        Iterator partitionIterator = manager.getActivePartitions().iterator();
        int partitionNumber = 0;
        while (partitionIterator.hasNext()) {
            DiskPartition partition = (DiskPartition) partitionIterator.next();
            //message("partition: " + partition);
            DocumentPartition documentPartition = partitions[partitionNumber++];
            
            Iterator documentIterator = partition.getDocumentIterator();
            documentPartition.compareFrequenciesFromDocumentIterator(documentIterator, partition);
            
        }
        
    }
    
    
    /**
     * Create a list of documents that will be used to represent the recreated documents
     * @param manager a partition manager that represents the partitions in the index
     */
    private void createDocumentList(PartitionManager manager) {
        partitions = new DocumentPartition[manager.getActivePartitions().size()];
        Iterator partitionIterator = manager.getActivePartitions().iterator();
        int p = 0;
        /* iterate through all the partitions */
        while (partitionIterator.hasNext()) {
            DiskPartition diskPartition = (DiskPartition) partitionIterator.next();
            createDocumentPartition(diskPartition, p++);
        }
        
    }
    
    /**
     * Create a DocumentPartition to mirror the partition on the disk, and in turn create its documents
     * @param diskPartition a partition managed by the partition manager
     */
    private void createDocumentPartition(DiskPartition diskPartition, int partitionNumber) {
        DocumentPartition documentPartition = new DocumentPartition();
        partitions[partitionNumber] = documentPartition;
        documentPartition.createDocuments(diskPartition);
        
    }
    
    /**
     * Help!
     */
    public static void usage() {
        System.out
                .println("Usage: java DVPostingsTester <properties> {<properties>...}");
    }
    
    /**
     * Main method to kick off the DVPostingsTester
     * @param args
     * @throws java.io.IOException
     * @throws NumberFormatException
     * @throws SearchEngineException
     */
    public static void main(String[] args) throws java.io.IOException,
            NumberFormatException, SearchEngineException {
        
        if (args.length == 0) {
            usage();
            return;
        }
        
        String flags = "d:e:k:x:";
        Getopt gopt = new Getopt(args, flags);
        int logLevel = 3;
        int c;
        String mainDictEntry = null;
        String indexDir = null;
        String lockDir = null;
        String cmFile = null;
        
        
        if (args.length == 0) {
            usage();
            return;
        }
        
        Thread.currentThread().setName("DVPostingsTester");
        
        //
        // Handle the options.
        while ((c = gopt.getopt()) != -1) {
            switch (c) {
                
                case 'd':
                    indexDir = gopt.optArg;
                    break;
                    
                case 'e':
                    mainDictEntry = gopt.optArg;
                    break;
                    
                    
                case 'k':
                    lockDir = gopt.optArg;
                    break;
                    
                case 'x':
                    cmFile = gopt.optArg;
                    break;
            }
        }
        
        //
        // Setup logging.
        log = Log.getLog();
        log.setStream(System.out);
        log.setLevel(logLevel);
        
        DVPostingsTester tester = new DVPostingsTester(cmFile, indexDir);
        tester.test();
        System.exit(-1);
        
        
    }
    
    /**
     * Create a new DVPostingsTester
     */
    DVPostingsTester(String cmFile, String indexDir) {
        
        //
        // Open our engine for use.  We give it the properties that we read
        // and no query properties.
        try {
            engine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(cmFile, indexDir);
        } catch (SearchEngineException se) {
            log.error("Indexer", 1, "Error opening collection", se);
            System.exit(0);
        }
    }
    
}
