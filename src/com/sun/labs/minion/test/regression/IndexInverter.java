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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.entry.DictionaryEntry;
import com.sun.labs.minion.indexer.entry.CasedDFOEntry;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.PosPostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.util.Getopt;

import com.sun.labs.minion.IndexConfig;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;

/**
 * Class IndexInverter recreates the list of documents from the index.
 * see the main() method for more details.
 * To compare the resulting files, use
 * <code>diff -iEbBw</code> (on BSD Unix, such as a Mac)
 * @author Bernard Horan
 *
 */
public class IndexInverter {

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
            documents = new Document[partition.getMaxDocumentID() + 1];
            Iterator di = partition.getDocumentIterator();
            while(di.hasNext()) {
                /* get the first document in the partition */
                DocKeyEntry dke = (DocKeyEntry) di.next();
                int id = dke.getID();
                if(!partition.isDeleted(dke.getID())) {
                    Document document = new Document(dke.getName());
                    //message("Created document: " + document + " with id: " + id);
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
         * Write out the documents into the output directory
         * @param outputDir a String identifying the directory into which the recreated documents should be
         * written
         */
        public void outputDocuments(String outputDir) {
            for(int i = 0; i < documents.length; i++) {
                Document d = documents[i];
                if(d != null) {
                    d.outputContents(outputDir);
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
         * Simple record-srtle structure that represents a word and its position in
         * a document
         *
         */
        class WordPosition implements Comparable<WordPosition> {

            /**
             * The word whose position I manage
             */
            private Object word;

            /**
             * The position of the word in the containing document
             */
            private int position;

            /**
             * Creata a new instance of WordPasition, recording the word and its position
             * @param word the word in a document
             * @param position the position of the word in the document
             */
            WordPosition(Object word, int position) {
                this.word = word;
                this.position = position;
            }

            /* (non-Javadoc)
             * @see java.lang.Comparable#compareTo(<T>)
             */
            public int compareTo(WordPosition w) {
                return position - w.position;
            }

            /**
             * Gets the word from the word position
             * @return the word
             */
            Object getWord() {
                return word;
            }
        }
        /**
         * An ordered set of words, ordered by position
         */
        private SortedSet<WordPosition> entries = new TreeSet<WordPosition>();

        /**
         * The name of the document
         */
        private Object name;

        /**
         * Creates a new instance of Document with a specified name
         * @param name an Object that identifies this Document
         */
        public Document(Object name) {
            this.name = name;
        //message("Creating " + this);
        }

        /**
         * Sets the position of an entry's name in this document
         * @param entry a BaseEntry that encapsulates an ID and a name
         * @param position the position of this entry in the document
         */
        public void setPosition(DictionaryEntry entry, int position) {
            //message(this + " setPosition(" + entry + ", " + position + ")");
            entries.add(new WordPosition(entry.getName(), position));
        }

        /**
         * Output the contents of this Document into a specified directory
         * @param outputDir the directory into which this document should be written
         */
        public void outputContents(String outputDir) {
            message("Outputting: " + this + " to " + outputDir);
            FileWriter writer = null;
            ;
            try {
                writer = createFileWriter(outputDir);

                for(Iterator iter = entries.iterator(); iter.hasNext();) {
                    WordPosition wp = (WordPosition) iter.next();
                    writer.write((String) wp.getWord());
                    writer.write('\n');
                }
                writer.write('\n');
                writer.flush();
                writer.close();
            } catch(IOException e) {
                System.err.println(e);
                try {
                    if(writer != null) {
                        writer.close();
                    }
                } catch(IOException e1) {
                    System.err.println("Failed to close writer!");
                    e1.printStackTrace();

                }
            }

        }

        /**
         * Create a FileWriter for this document in the specified directory
         * @param outputDir the directory into which the document is to be written
         * @return a FileWriter to which the contents of the document may be written
         * @throws IOException
         */
        private FileWriter createFileWriter(String outputDir) throws IOException {
            String filename = (String) name;
            File file = new File(filename);
            if(file.isAbsolute()) {
                filename = file.getName();
            }
            file = new File(outputDir + File.separatorChar + filename);
            if(file.exists()) {
                throw new RuntimeException("File already exists: " + file.
                        getAbsolutePath());
            }
            //message("Created: " + file);
            return new FileWriter(file);


        }

        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return super.toString() + " name: " + name;
        }
    }
    /**
     * For tracing
     */
    public static boolean DEBUG = true;

    /**
     * Tag for the log
     */
    protected static final String logTag = "IndexInverter";

    /**
     * The configuration for the IndexInverter
     */
    public static IndexConfig indexConfig = new IndexConfig();

    /**
     * A SearchEngine
     */
    private SearchEngineImpl engine;

    /**
     * The name of the directory into which the contents of the recreated documents should be written
     */
    private File outputDir;

    /**
     * The DocumentPartitions that represent the partitions in the index
     */
    private DocumentPartition[] partitions;

    /**
     * This method recreates the documents from the index
     * @param activePartitions the list of active partitions in the index
     */
    private void recreateDocuments(List activePartitions) {
        //
        // Create a features object to get the appropriate postings from an
        // entry
        PostingsIteratorFeatures features = new PostingsIteratorFeatures();
        features.setPositions(true);
        features.setCaseSensitive(true);

        Iterator partitionIterator = activePartitions.iterator();
        int partitionNumber = 0;
        while(partitionIterator.hasNext()) {
            DiskPartition partition = (DiskPartition) partitionIterator.next();
            // message("partition: " + partition);
            DocumentPartition documentPartition = partitions[partitionNumber++];
            DictionaryIterator mainDictionaryIterator =
                    (DictionaryIterator) partition.getMainDictionaryIterator();
            while(mainDictionaryIterator.hasNext()) {
                CasedDFOEntry entry = (CasedDFOEntry) mainDictionaryIterator.
                        next();
                PosPostingsIterator pIterator = (PosPostingsIterator) entry.
                        iterator(features);
                if(pIterator == null) {
                    continue;
                }
                while(pIterator.next()) {
                    //
                    // The id identifies the document, per partition
                    int documentId = pIterator.getID();
                    if(!partition.isDeleted(documentId)) {

                        //
                        // Positions encapsulates an array of positions for this
                        // entry, in the document identified by id
                        int[][] positions = pIterator.getPositions();
                        //
                        // For the time being we only care about the non-field
                        // positions, i.e. only the zeroth index
                        /**
                         * {@link com.sun.labs.minion.indexer.postings.PosPostingsIterator#getPositions()}
                         */
                        Document document = documentPartition.getDocument(
                                documentId);
                        //
                        // The zeroth element idenfies the count of following
                        // elements
                        int count = positions[0][0];
                        int position = 1;
                        for(int i = 0; i < count; i++) {
                            document.setPosition(entry,
                                    positions[0][position++]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a list of documents that will be used to represent the recreated
     * documents
     *
     * @param activePartitions
     *            a list of active partitions in the index
     */
    private void createDocumentList(List activePartitions) {
        partitions = new DocumentPartition[activePartitions.size()];
        Iterator partitionIterator = activePartitions.iterator();
        int p = 0;
        /* iterate through all the partitions */
        while(partitionIterator.hasNext()) {
            DiskPartition diskPartition =
                    (DiskPartition) partitionIterator.next();
            createDocumentPartition(diskPartition, p++);
        }

    }

    /**
     * Create a DocumentPartition to mirror the partition on the disk, and in turn create its documents
     * @param diskPartition a partition managed by the partition manager
     */
    private void createDocumentPartition(DiskPartition diskPartition,
            int partitionNumber) {
        DocumentPartition documentPartition = new DocumentPartition();
        partitions[partitionNumber] = documentPartition;
        documentPartition.createDocuments(diskPartition);

    }

    /**
     * DEBUG information
     * @param string string to be output
     */
    void message(String string) {
        if(DEBUG) {
            System.out.println(string);
        }

    }

    /**
     * Create the list of documents, recreate their contents then write them into an output directory
     */
    public void outputInvertedIndex() {
        PartitionManager manager = engine.getPM();
        List activePartitions = manager.getActivePartitions();
        createDocumentList(activePartitions);
        recreateDocuments(activePartitions);
        outputDocuments();
    }

    /**
     * Output the recreated documents into the output directory
     */
    private void outputDocuments() {
        for(int i = 0; i < partitions.length; i++) {
            DocumentPartition dp = partitions[i];
            if(dp != null) {
                dp.outputDocuments(outputDir.getAbsolutePath());
            }
        }

    }

    /**
     * Help!
     */
    public static void usage() {
        System.out.println(
                "Usage: java com.sun.labs.minion.test.regression.IndexInverter -d <index_dir> -o <output_dir> -x <config_file>");
    }

    /**
     * Main method to kick off the indexinverter
     * @param args
     * @throws java.io.IOException
     * @throws NumberFormatException
     * @throws SearchEngineException
     */
    public static void main(String[] args) throws java.io.IOException,
            NumberFormatException, SearchEngineException {

        if(args.length == 0) {
            usage();
            return;
        }

        String flags = "d:o:x:";
        Getopt gopt = new Getopt(args, flags);
        int logLevel = 3;
        int c;
        String indexDir = null;
        String cmFile = null;
        String outputDir = "/tmp";


        if(args.length == 0) {
            usage();
            return;
        }

        Thread.currentThread().setName("IndexInverter");

        //
        // Handle the options.
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'o':
                    outputDir = gopt.optArg;
                    break;

                case 'x':
                    cmFile = gopt.optArg;
                    break;
            }
        }

        //
        // Setup logging.
        IndexInverter inverter = new IndexInverter(cmFile, indexDir, outputDir);
        inverter.outputInvertedIndex();
        inverter.close();
        System.out.println("Completed inversion");


    }

    public void close() throws SearchEngineException {
        engine.close();
    }

    /**
     * Creata a new indexinverter to output recreated documents into the specified output directory
     * @param outputDirectory the identifier for the output directory
     */
    public IndexInverter(String cmFile, String indexDir, File outputDirectory) {
        this.outputDir = outputDirectory;
        if(!outputDir.exists()) {
            System.err.println("Output directory (" + outputDirectory.
                    getAbsolutePath() + ") does not exist, please create it");
            System.exit(0);
        }
        //
        // Open our engine for use.  We give it the properties that we read
        // and no query properties.
        try {
            engine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(
                    cmFile, indexDir);

        } catch(SearchEngineException se) {
            System.err.println("Error opening collection: " + se);
            System.exit(0);
        }
    }

    public IndexInverter(String cmFile, String indexDir, String outputDirectory) {
        this(cmFile, indexDir, new File(outputDirectory));
    }
}
