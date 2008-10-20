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
import java.util.Random;
import java.util.Set;

import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.test.SEMain;

import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.Log;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;

/**
 * Regression test to check that files are deleted from the index.
 * @author Bernard Horan
 *
 */
public class DeletionTest {
	/**
	 * Random number generator
	 */
	private Random numberGen = new Random();
	/**
	 * Set of files to be indexed. Keeps track of the files that have been removed.
	 */
	private Set<String> testFiles;

	/**
	 * Tag for the log
	 */
	protected static String logTag = "DeletionTest";
	/**
	 * The log onto which messages <b>should</b> be written
	 */
	protected static Log log;
	/**
	 * The character encoding
	 */
	protected static String charEnc = "8859_1";
	/**
	 * Flags used to describe options permitted in the command line invocation
	 */
	static String flags = "c:d:f:i:l:o:x:";

	/**
	 * @param args
	 * @throws SearchEngineException 
	 */
	public static void main(String[] args) throws SearchEngineException {

        String documentDir = null;
        Getopt gopt = new Getopt(args, flags);
        int c;
        String indexDir = null;
        String cmFile = null;
        int iterations = 5;
        String outputDir = "/tmp";

        if (args.length == 0) {
            usage();
            return;
        }

        Thread.currentThread().setName("DeletionTest");

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

            case 'i':
                try {
                    iterations = Integer.parseInt(gopt.optArg);
                } catch (NumberFormatException nfe) {
                }
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
        
        
        

        DeletionTest test = null;
        try {
            test = new DeletionTest(indexDir, iterations, cmFile, documentDir, outputDir);
            
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
	 * The directory into which the de-indexed files should be written
	 */
	protected File outputDirectory;
	/**
	 * The directory to contain the index
	 */
	protected String indexDir;
	/**
	 * A search engine
	 */
	protected SearchEngineImpl engine;
	/**
	 * The directory containing the documents to be indexed
	 */
	protected File documentDirectory;

	/**
	 * Number of random files to remove
	 */
	private int iterations;
	/**
	 * The filename of the configuration file if provided on the command line args
	 */
	protected String cmFile;
	/**
	 * The name of the configuration file preferred for use. This enables us to dump synchronously and avoid any multithreaded nastiness when re-reading the log
	 */
	private final String CONFIG_FILE = "DeletionTest-config.xml";
	
	/**
	 * Run the test.
	 * Indexes the files in the document directory, then deletes files as follows:
	 * <ol>
	 * <li>Deletes random files (number of random files is determined by <code>iterations</code></li>
	 * <li>Deletes first and last documents of each partition</li>
	 * <li>Deletes all the remaining files</li>
	 * </ol>
	 * After each deletion invert the files to ensure that the files really have been removed.
	 * @throws IOException
	 * @throws SearchEngineException
	 */
	private void test() throws IOException, SearchEngineException {
		//
        // Get the test files
        getFileList();
        //
        // Index the test files
        index();
        
        //
        //Recreate the search engine, it was closed after indexing 
        //to ensure that the index was fully written
        createSearchEngine(cmFile, indexDir);
		
        for (int i = 0; i < iterations; i++) {
        	if (testFiles.size() > 0) {
        		String randomDocument = getRandomDocument();
        		deleteDocument(randomDocument);
        		invert();
        		checkForAbsences();
        	}
        }
		if (testFiles.size() > 0) {
			deleteFirstLast();
		}
		deleteAll();
		engine.close();
		log.log(logTag, MinionLog.LOG, "Test complete");
	}

	/**
	 * Delete all the files in the index.
	 * Check to ensure that all the files have been deleted by inverting the index into
	 * the output directory.
	 * @throws IOException
	 */
	private void deleteAll() throws IOException {
		Set<String> copyOfTestFiles = new HashSet<String>();
		copyOfTestFiles.addAll(testFiles);
		for (Iterator iter = copyOfTestFiles.iterator(); iter.hasNext();) {
			String filename = (String) iter.next();
			deleteDocument(filename);
		}
		invert();
		String[] invertedFileList = outputDirectory.list();
		if (invertedFileList.length > 0) {
			System.err.println("Files still in index");
			for (int i = 0; i < invertedFileList.length; i++) {
				System.err.println(invertedFileList[i]);
			}
		} else {
			Log.log(logTag, MinionLog.LOG, "All files deleted");
		}
	}

	/**
	 * Delete the first and last documents from each partition.
	 * Check to ensure that the documents have been deleted by inverting the
	 * index into the output directory.
	 * @throws IOException
	 */
	private void deleteFirstLast() throws IOException {
		Iterator activeIterator = engine.getManager().getActivePartitions().iterator();
        
        /* iterate through all the partitions */
        while (activeIterator.hasNext()) {
        	DiskPartition partition = (DiskPartition) activeIterator.next();
            Iterator di = partition.getDocumentIterator();
            if (di.hasNext()) {

                /* get the first document in the partition */
            	DocKeyEntry firstDocEntry = (DocKeyEntry) di.next();
                
                deleteFilename((String) firstDocEntry.getName());

                /* get the last document in the partition */
                
                DocKeyEntry lastDocEntry = null;
                if (di.hasNext()) {
                	lastDocEntry = (DocKeyEntry) di.next();
                    while (di.hasNext()) {
                        lastDocEntry = (DocKeyEntry) di.next();
                    }
                }

                if (lastDocEntry != null) {
                    deleteFilename((String) lastDocEntry.getName());
                    }
                }
            invert();
            checkForAbsences();
            }
        }
		
	

	/**
	 * Delete a filename.
	 * This deletes a document whose name is the last part of file name.
	 * @param string an absolute path for a file
	 */
	private void deleteFilename(String string) {
		File f = new File(string);
		deleteDocument(f.getName());
		
	}

	/**
	 * Return a random document from the set of files
	 * that have been indexed
	 * @return a String that indicates the finall name of the file (not the absolute oath)
	 */
	private String getRandomDocument() {
		String[] files = testFiles.toArray(new String[testFiles.size()]);
		int randomIndex = numberGen.nextInt(files.length);
		return files[randomIndex];	
	}

	/**
	 * Delete a document from the set of files and from the index
	 * @param filename a short name for the file to be removed (not the absolute filename)
	 */
	private void deleteDocument(String filename) {
		log.log(logTag, Log.LOG, "Deleting: " + filename);
		testFiles.remove(filename);
        engine.delete(getAbsoluteFilename(filename));
	}

	/**
	 * Fill the set of filenames from the document directory
	 */
	private void getFileList() {
	    String[] files = documentDirectory.list();
	    testFiles = new HashSet<String>();
	    for (int i = 0; i < files.length; i++) {
			testFiles.add(files[i]);
		}
	}

	/**
	 * Indexes the test files
	 * @throws IOException
	 */
	private void index() throws IOException {
		for (Iterator iter = testFiles.iterator(); iter.hasNext();) {
			String testFile = (String) iter.next();
			String file = getAbsoluteFilename(testFile);
	
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
	 * Return the full absolute path for a given filename
	 * @param localFilename the last part of a filename (not the absolute filename)
	 * @return
	 */
	private String getAbsoluteFilename(String localFilename) {
		String file = documentDirectory + File.separator + localFilename;
		return file;
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
	 * Check to ensure that the files that have been produced 
	 * do not contain the files that should have been deleted.
	 * @throws IOException 
	 */
	private void checkForAbsences() throws IOException {
	    //Get the names of the files in the output directory
	    String[] invertedFileList = outputDirectory.list();
	    Set<String> invertedFiles = new HashSet<String>();
	    //
	    //Create a set from the list of files
	    for (int i = 0; i < invertedFileList.length; i++) {
	        invertedFiles.add(invertedFileList[i]);
	    }
	    //
	    //Remove the testFiles from the set of files
	    invertedFiles.removeAll(testFiles);
	    if (invertedFiles.size() > 0) {
	    	System.err.println("Failed to delete:");
	    	for (String filename : invertedFiles) {
				System.err.println(filename);
			}
	    } else {
	    	log.log(logTag, Log.LOG, "File(s) deleted");
	    }
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
	public DeletionTest() {
	    //
	    // Set up the index inverter
	    IndexInverter.DEBUG = false;
	}

	/**
	 * Create a new instance of the test class
	 * @param indexDir the location of the index directory
	 * @param iterations the number of random files to be deleted
	 * @param cmFile the file name for the configuration file
	 * @param dDir the name of the document directory
	 * @param oDir the name of the output directory
	 * @throws FileNotFoundException
	 */
	public DeletionTest(String indexDir, int iterations, String cmFile, String dDir, String oDir) throws FileNotFoundException {
	    this();
	    this.indexDir = indexDir;
	    this.cmFile = cmFile;
	    this.iterations = iterations;
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
	 * Help!
	 */
	static void usage() {
	    System.out
	            .println("Usage: java com.sun.labs.minion.test.regression.DeletionTest -c <character_encoding> " +
	                    "-d <index_directory> -f <document_directory> " +
	                    "-i <iterations> -l <log_level> " +
	                    "-o <output_directory> " +
	                    "[-x <config_file>]");
	}

}
