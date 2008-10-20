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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.test.SEMain;
import com.sun.labs.minion.test.util.FileComparer;

import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.Log;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;

/**
 * Multi-threaded index and test.
 * Indexes a directory of documents, using a config file with multiple pipleines,
 * then inverts the index into an output directory and compares against the 
 * original files.
 * @author Bernard Horan
 *
 */
public class MPIndexTest {

    /**
     * The character encoding
     */
    protected static String charEnc = "8859_1";
    /**
     * Tag for the log
     */
    protected static String logTag = "MPIndexTest";
    /**
     * The log onto which messages <b>should</b> be written
     */
    protected static Log log;
    /**
     * Flags used to describe options permitted in the command line invocation
     */
    static String flags = "c:d:f:l:o:x:";

    /**
     * Create a new instance of the test class
     * @param indexDir the location of the index directory
     * @param cmFile the file name for the configuration file
     * @param dDir the name of the document directory
     * @param oDir the name of the output directory
     * @throws FileNotFoundException
     */
    public MPIndexTest(String indexDir, String cmFile, String dDir, String oDir) throws FileNotFoundException {
        this();
        this.indexDir = indexDir;
        this.cmFile = cmFile;
        documentDirectory = new File(dDir);
        if(!documentDirectory.exists()) {
            throw new FileNotFoundException("Document directory " + dDir + " does not exist");
        }
        outputDirectory = new File(oDir);
        if (!outputDirectory.exists()) {
            throw new FileNotFoundException("Output directory " + oDir + " does not exist");
        }
        createSearchEngine(cmFile, indexDir);
        
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        String documentDir = null;
        Getopt gopt = new Getopt(args, flags);
        int c;
        String indexDir = null;
        String cmFile = null;
        String outputDir = "/tmp";

        if (args.length == 0) {
            usage();
            return;
        }

        Thread.currentThread().setName("MergeTest");

        int logLevel = 3;

        //
        // Set up the logging for the search engine. We'll send everything
        // to the standard output, except for errors, which will go to
        // standard error. We'll set the level at 3, which is pretty
        // verbose.
        Log log = Log.getLog();
        log.setStream(System.out);
        log.setStream(Log.ERROR, System.err);
        log.setLevel(logLevel);

        //
        // Handle the options.
        while ((c = gopt.getopt()) != -1) {
            switch (c) {

            case 'c':
                charEnc = gopt.optArg;
                break;

            case 'd':
                indexDir = gopt.optArg;
                break;

            case 'f':
                documentDir = gopt.optArg;
                break;

            case 'l':
                try {
                    logLevel = Integer.parseInt(gopt.optArg);
                } catch (NumberFormatException nfe) {
                }
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
        // We may have gotten a larger log level.
        log.setLevel(logLevel);

        if (indexDir == null && cmFile == null) {
            log.warn("logTag", 0, "You must specify a configuration.");
            usage();
            return;
        }

        if (documentDir == null) {
            log.warn("logTag", 0, "You must specify a document directory.");
            usage();
            return;
        }
        
        
        

        MPIndexTest test = null;
        try {
            test = new MPIndexTest(indexDir, cmFile, documentDir, outputDir);
            
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
        try {
            test.test();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * The directory containing the documents to be indexed
     */
    protected File documentDirectory;
    /**
     * A search engine
     */
    protected SearchEngineImpl engine;
    /**
     * The directory into which the de-indexed files should be written
     */
    protected File outputDirectory;
    /**
     * The directory to contain the index
     */
    protected String indexDir;
    /**
     * The filename of the configuration file if provided on the command line args
     */
    protected String cmFile;
    /**
     * The name of the configuration file preferred for use. This enables us to dump synchronously and avoid any multithreaded nastiness when re-reading the log
     */
    private final String CONFIG_FILE = "MPIndexTest-config.xml";
    
    /**
     * Run the test
     * @throws IOException
     */
    void test() throws IOException {
        //
        // Get the test files
        String[] testFiles = getFileList();
        //
        // Index the test files
        index(testFiles);
        invert();
        correlateInvertedFiles(testFiles);
        diff();
    }

    /**
     * Return the list of filenames from the document directory
     */
    private String[] getFileList() {
        return documentDirectory.list();
    }

    /**
     * Indexes the test files
     * @param testFiles the files to be indexed
     * @throws IOException
     */
    private void index(String[] testFiles) throws IOException {
        for (int i = 0; i < testFiles.length; i++) {
            String file = documentDirectory + File.separator + testFiles[i];
    
            IndexableFile f = new IndexableFile(file, charEnc);
            Indexable document = SEMain.makeDocument(f);
    
            try {
                long len = f.length();
                boolean longFile = len > 400000;
                if (longFile) {
                    log.debug(logTag, 0, "Long: " + f.length());
                    engine.flush();
                }
                engine.index(document);
                if (longFile) {
                    engine.flush();
                }
    
            } catch (SearchEngineException se) {
                log.error(logTag, 1, "Error indexing document " + f, se);
            } catch (Exception e) {
                log.error(logTag, 0, "Error indexing", e);
            }
        }
        try {
            engine.close();
        } catch (SearchEngineException e) {
            e.printStackTrace();
        }
    
    }

    /**
     * Takes the index and reproduces the documents that produced it.<br>
     * The documents are reproduced into the output directory, with any files already there
     * being deleted.
     * @throws IOException
     */
    protected void invert() throws IOException  {
        //
        // Delete the old files in the output directory
        File[] files = outputDirectory.listFiles();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (!file.delete()) {
                log.error(logTag, 2, "Failed to delete old file " + file.getName());
            }
        }
        //
        // Create a new index inverter and output the inverted documents into
        // the output directory
        IndexInverter inverter = new IndexInverter(cmFile, indexDir, outputDirectory);
        log.log(logTag, MinionLog.LOG, "Thread count: " + Thread.activeCount());
        log.log(logTag, MinionLog.LOG, "Starting inversion");
        try {
            inverter.outputInvertedIndex();
        } catch (Exception e) {
            System.err.println(e);
        }
        try {
            inverter.close();
        } catch (SearchEngineException e) {
            System.err.println(e);
        }
        log.log(logTag, MinionLog.LOG, "Finished inversion");
    }

    /**
     * Check to see that the files that were indexed are in the
     * output directory
     * @param testFiles
     * @throws IOException 
     */
    private void correlateInvertedFiles(String[] testFiles) throws IOException {
        Set<String> invertedSet = new HashSet<String>();
        //
        //Put the names of the test files into a set
        for (int i = 0; i < testFiles.length; i++) {
            invertedSet.add(testFiles[i]);
        }
        //
        //Get the names of the files in the output directory
        String[] invertedFiles = outputDirectory.list();
        //
        //Remove the name of every file in the output directory from the
        //set of files that have been indexed. This should result in an empty set
        for (int i = 0; i < invertedFiles.length; i++) {
            invertedSet.remove(invertedFiles[i]);
        }
        if (invertedSet.isEmpty()) {
            log.log(logTag, MinionLog.LOG, "No missing files");
        } else {
            //
            //There are some files that have been indexed that are  
            //missing from the output directory
            for (Iterator iter = invertedSet.iterator(); iter.hasNext();) {
                String filename = (String) iter.next();
                System.err.println("Missing file: " + filename);
            }
        }
    }

    /**
     * Compare one file with another and return a result indicating if they are the same.
     * @param filename the name of the file to compare
     * @return a boolean, true if the files are equal
     * @throws IOException
     */
    protected boolean diff(String filename) throws IOException {
        String outputPathname = outputDirectory.getAbsolutePath() + File.separator + filename;
        String originalPathname = documentDirectory.getAbsolutePath() + File.separator + filename;
        FileComparer differ = new FileComparer(outputPathname, originalPathname);
        return differ.areLinesEqual();
    }

    /**
     * Compares the files in the output directory to the document directory.<br>
     * @throws IOException
     */
    protected void diff() throws IOException {
        log.log(logTag, MinionLog.LOG, "Starting diff");
        
        String[] outputDocuments = outputDirectory.list();
        for (int i = 0; i < outputDocuments.length; i++) {
            String filename = outputDocuments[i];
            boolean success = diff(filename);
            if (!success) {
                System.err.println("Diff failed: " + filename);
            }
        }
        log.log(logTag, MinionLog.LOG, "Finsihed diff");
    }

    /**
     * Creates a new instance of a search engines and assigns it to the field
     */
    protected void createSearchEngine(String cmFile, String indexDir) {
        if (cmFile != null) {
            try {
                engine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(cmFile, indexDir);
            } catch (SearchEngineException se) {
                log.error(logTag, 1, "Error opening collection", se);
                return;
            }
        } else {
            URL configURL = getClass().getResource(CONFIG_FILE);
            if (configURL == null) {
                throw new RuntimeException("Missing config file: " + CONFIG_FILE);
            }
            try {
                engine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(indexDir, configURL);
            } catch (SearchEngineException se) {
                log.error(logTag, 1, "Error opening collection", se);
                return;
            }
        }
    }

    /**
     * Default contructor. Sets up the index inverter
     */
    public MPIndexTest() {
        //
        // Set up the index inverter
        IndexInverter.DEBUG = false;
    }

    /**
     * Help!
     */
    static void usage() {
        System.out
                .println("Usage: java com.sun.labs.minion.test.regression.MPIndexTest -c <character_encoding> " +
                        "-d <index_directory> -f <document_directory> " +
                        "-l <log_level> " +
                        "-o <output_directory> " +
                        "[-x <config_file>]");
    }

}
