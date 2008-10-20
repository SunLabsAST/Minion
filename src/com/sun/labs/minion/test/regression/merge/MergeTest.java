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

package com.sun.labs.minion.test.regression.merge;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.test.IndexTest;
import com.sun.labs.minion.test.SEMain;
import com.sun.labs.minion.test.regression.IndexInverter;
import com.sun.labs.minion.test.util.FileComparer;

import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.Log;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import java.net.MalformedURLException;

/**
 * This class is responsible for testing the merging feature of the search
 * engine.<br>
 * It repeatedly indexes a random selection of (a fixed size of) documents from
 * a document directory, then "de-indexes" them compares them with the
 * originals. It relies on class IndexInverter to achieve the
 * "de-indexification".<br>
 * In the <code>main()</code> method the user should provide a config file, a
 * document directory and an output directory. The user can also provide the
 * number of iterations (using the <code>-i</code> flag and the number of
 * documents to be indexed at each interation (using the <code>-s</code>
 * flag).<br>
 * A log of the results of the program is written to a file <code>/tmp/MergeTestReport.xml</code> that
 * can later be used by {@link com.sun.labs.minion.test.regression.merge.MergeTestReplayer}.
 * 
 * @author Bernard Horan
 * 
 */
public class MergeTest {
    /**
     * Class MergeTestReporter is a simple inner class responsible for reporting the
     * results of the test.<br>
     * The class outputs an XML file (stream) that contains the results of the
     * test.
     * 
     */
    class MergeTestReporter implements TestReporter {
        
        /**
         * If true, report failures to syserr
         */
        private final boolean REPORT_FAILURES = true;
        
        /**
         * Format for printing timestamps
         */
        private final DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        //2001-07-04T12:08:56.235-0700
               
        /**
         * The stream on which the results are written.
         */
        private Writer out;

        /**
         * The arguments that invoked the virtual machine
         */
        private String[] args;

        /**
         * Create a new instance of me.<br>
         * Instantiates a file on which to output the results, by default this
         * is <code>filename</code>
         * @param args 
         */
        MergeTestReporter(String[] args) {
            out = new BufferedWriter(new PrintWriter(System.out));
            this.args = args;           
        }

        /**
         * Report the memory, including:
         * <ul>
         * <li>max memory, as possibly indicated by -Xmx</li>
         * <li>total memory</li>
         * <li>free memory</li>
         * </ul>
         * @throws IOException
         */
        private void reportMemory() throws IOException {
            out.write("<Memory>");
            out.write('\n');
            
            out.write("<entry key=\"max\">");
            out.write(new Long(Runtime.getRuntime().maxMemory()).toString());
            out.write("</entry>");
            out.write('\n');
            
            out.write("<entry key=\"total\">");
            out.write(new Long(Runtime.getRuntime().totalMemory()).toString());
            out.write("</entry>");
            out.write('\n');
            
            out.write("<entry key=\"free\">");
            out.write(new Long(Runtime.getRuntime().freeMemory()).toString());
            out.write("</entry>");
            out.write('\n');
            
            out.write("</Memory>");
            out.write('\n');
            
        }

        private String printTimeStamp() {
            return dateFormatter.format(new Date());
        }

        /**
         * Report the environment, as provided by <code>System.getEnv()</code>.
         * @throws IOException
         */
        private void reportEnvironment() throws IOException {
            try {
                out.write("<Environment>");
                out.write('\n');
                writeMap(System.getenv());
            } finally {
                out.write("</Environment>");
                out.write('\n');
                out.flush();
            }
        }

        /**
         * Report the system properties.
         * @throws IOException
         */
        private void reportProperties() throws IOException {
            try {
                out.write("<Properties>");
                out.write('\n');
                writeMap(System.getProperties());
            } finally {
                out.write("</Properties>");
                out.write('\n');
                out.flush();
            }
        }

        /**
         * Private utility method to write a map as a sequence of XML
         * &lt;entry&gt; elements, with an attribute corresponding to a key and a content
         * corresponding to the value.
         * @param aMap
         * @throws IOException
         */
        private void writeMap(Map aMap) throws IOException {
            Iterator keyIterator = aMap.keySet().iterator();
            while (keyIterator.hasNext()) {
                String key = (String) keyIterator.next();
                out.write("<entry key=\"" + key + "\">");
                out.write((String)aMap.get(key));
                out.write("</entry>");
                out.write('\n');
            }
        }

        /** 
         * Open the XML Element &lt;MergeTest&gt;.<br>
         * Report the properties, environment and current memory.
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startReport()
         */
        public void startReport() throws IOException {
            out.write("<MergeTest timestamp=\"" + printTimeStamp() + "\">");
            out.write('\n');
            try {
                reportConfiguration();
            } catch (IOException e) {
                System.err.println(e);
            }
        }

        /**
         * Report the configuration, i.e.
         * <ul>
         * <li>the command line arguments</li>
         * <li>the system properties</li>
         * <li>the memory</li>
         * </ul>
         * @throws IOException
         */
        private void reportConfiguration() throws IOException {
            try {
                out.write("<Configuration>");
                out.write('\n');
                reportCLArgs();
                reportProperties();
                reportEnvironment();
                reportMemory();
            } finally {
                out.write("</Configuration>");
                out.write('\n');
            }
        }

        

        /**
         * Report the command line arguments, each within an &lt;entry&gt; element.
         * @throws IOException
         */
        private void reportCLArgs() throws IOException {
            Getopt gopt = new Getopt(args, flags);
            int c;
            out.write("<Arguments>");
            out.write('\n');
            while ((c = gopt.getopt()) != -1) {
                out.write("<entry key=\"-" + (char)c + "\">");
                out.write(gopt.optArg);
                out.write("</entry>");
                out.write('\n');
            }
            out.write("</Arguments>");
            out.write('\n');
        }

        /* (non-Javadoc)
         * @see com.sun.labs.minion.test.regression.merge.MergeReporter#reportTestFiles(java.lang.String[])
         */
        public void reportTestFiles(String[] testFiles) throws IOException {
            out.write("<Files timestamp=\"" + printTimeStamp() + "\">");
            out.write('\n');
            for (int i = 0; i < testFiles.length; i++) {
                String filename = testFiles[i];
                out.write("<File name=\"");
                out.write(filename);
                out.write("\"/>");
                out.write('\n');
            }
            out.write("<Files>");
            out.write('\n');
        }

        /** 
         * Close the XML Element &lt;MergeTest&gt;.
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endReport()
         */
        public void endReport() throws IOException {
            out.write("</MergeTest>");
            out.write('\n');
            out.close();
        }

        /** 
         * Open XML Element &lt;Iteration&gt; with an
         * attribute <code>count</code> indicating the count of the iteration.
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startIteration(int)
         */
        public void startIteration(int i) throws IOException {
            out.write("<Iteration timestamp=\"" + printTimeStamp() + "\" count=\"" + i + "\">");
            out.write('\n');
        }

        /** 
         * End the &lt;Iteration&gt; element.
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endIteration()
         */
        public void endIteration() throws IOException {
            out.write("</Iteration>");
            out.write('\n');
            out.flush();
        }

        /** 
         * Open the &lt;Indexing&gt; XML element.
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startIndexing()
         */
        public void startIndexing() throws IOException {
            out.write("<Indexing timestamp=\"" + printTimeStamp() + "\">");
            out.write('\n');
        }

        /** 
         * Create an XML element &lt;Index&gt; with two attributes:
         * <ul>
         * <li><code>file</code> corresponding to the name of the file</li>
         * <li><code>success</code> corrresponding to the success of it being indexed</li>
         * </ul>
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#index(java.lang.String, boolean)
         */
        public void index(String file, boolean success) throws IOException {
            if (REPORT_FAILURES && !success) {
                System.err.println("Index of file " + file + " failed");
            }
            out.write("<Index timestamp=\"" + printTimeStamp() + "\" file=\"");
            out.write(file);
            out.write("\" success=\"" + success + "\"/>");
            out.write('\n');
        }

        /** 
         * Close the &lt;Indexing&gt; element
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endIndexing()
         */
        public void endIndexing() throws IOException {
            out.write("</Indexing>");
            out.write('\n');
            out.flush();
        }

        /** 
         * Open the &lt;Inverting&gt; element.
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startInverting()
         */
        public void startInverting() throws IOException {
            out.write("<Inverting>");
            out.write('\n');
        }

        /** 
         * Close the &lt;Inverting&gt; element
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endInverting()
         */
        public void endInverting() throws IOException {
            out.write("</Inverting>");
            out.write('\n');
            out.flush();
        }

        /** 
         * Creates an &lt;InvertException&gt; element, with the attribute:
         * <ul>
         * <li><code>exception</code> that identifies the exception
         * </ul>
         * The element encloses a <code>CDATA</code> element containing the stacktrace
         * of the exception.
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#invertException(java.lang.Exception)
         */
        public void invertException(Exception e) throws IOException {
            out.write("<InvertException timestamp=\"" + printTimeStamp() + "\" exception=\"" + e + "\">");
            out.write('\n');
            out.write("<![CDATA[");
            out.write('\n');
            PrintWriter pWriter = new PrintWriter(out);
            e.printStackTrace(pWriter);
            out.write('\n');
            out.write("]]>");
            out.write('\n');
            out.write("</InvertException>");
        }

        /** 
         * Opens the &lt;Diffing&gt; element.
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startDiff()
         */
        public void startDiff() throws IOException {
            out.write("<Diffing timestamp=\"" + printTimeStamp() + "\">");
            out.write('\n');
        }

        /** 
         * Creates a &lt;Diff&gt; element that encloses a <code>CDATA</code> element
         * containing the line to be reported.
         * @throws IOException 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#reportDiff(String, boolean)
         */
        public void reportDiff(String file, boolean success) throws IOException {
            if (REPORT_FAILURES && !success) {
                System.err.println("Diff of file " + file + " failed");
            }
            out.write("<Diff timestamp=\"" + printTimeStamp() + "\" file=\"");
            out.write(file);
            out.write("\" success=\"" + success + "\"/>");
            out.write('\n');
        }
        

        /**
         * Closes the &lt;Diffing&gt; element.<br>
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endDiff()
         */
        
        public void endDiff() throws IOException {
           out.write("</Diffing>");
            out.write('\n');
            out.flush();
        }

        /**
         * Opens the &lt;Optimizing&gt; element.
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startOptimizing()
         */
        public void startOptimizing() throws IOException {
            out.write("<Optimizing timestamp=\"" + printTimeStamp() + "\">");
            out.write('\n');
        }

        /**
         * Closes the &lt;Optmizing&gt; element
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endOptimizing()
         */
        public void endOptimizing() throws IOException {
            out.write("</Optimizing>");
            out.write('\n');
            out.flush();
        }

        /**
         * Creates an &lt;Optmize&gt; element with the following attribute:
         * <ul>
         * <li><code>exception</code> corresponding to the parameter <code>e</code></li>
         * </ul>
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#optimizeException(com.sun.labs.minion.SearchEngineException)
         */
        public void optimizeException(SearchEngineException e)
                throws IOException {
            out.write("<Optimize timestamp=\"" + printTimeStamp() + "\" exception=\"" + e + "\">");
            out.write('\n');
        }

        /* (non-Javadoc)
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#setReportFilename(java.lang.String)
         */
        public void setReportFilename(String reportFileName) {
            if (reportFileName.length() == 0) {
                return;
            }
            File outFile = new File(reportFileName);
            try {
                out = new BufferedWriter(new FileWriter(outFile));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* (non-Javadoc)
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endReportMissingFiles()
         */
        public void endReportMissingFiles() throws IOException {
            out.write("</MissingFiles>");
            out.write('\n');
            
        }

        /* (non-Javadoc)
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#missingFile(java.lang.String)
         */
        public void missingFile(String filename) throws IOException {
            if (REPORT_FAILURES) {
                System.err.println("Missing file: " + filename);
            }
            out.write("<Missing timestamp=\"" + printTimeStamp() + "\"");
            out.write(" filename=\"" + filename + "\">");
            out.write('\n');
            
        }

        /* (non-Javadoc)
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startReportMissingFiles(int)
         */
        public void startReportMissingFiles(int i) throws IOException {
            if (REPORT_FAILURES && (i != 0)) {
                System.err.println("Missing file count: " + i);
            }
            out.write("<MissingFiles timestamp=\"" + printTimeStamp() + "\"");
            out.write(" count=\"" + i + "\">");
            out.write('\n');
            
        }

        

        

    }

    /**
     * Tag for the log
     */
    protected static String logTag = "MergeTest";

    /**
     * static for debugging
     */
    static final boolean DEBUG = false;

    /**
     * The log onto which messages <b>should</b> be written
     */
    protected static Log log;
    
    /**
     * The number of documents to be indexed per iteration
     */
    protected int documentCount;

    /**
     * The number of iterations to run the test
     */
    protected int iterations;

    /**
     * The directory containing the documents to be indexed
     */
    protected File documentDirectory;

    /**
     * The names of the files in the document directory
     */
    private String[] files;

    /**
     * The character encoding
     */
    protected static String charEnc = "8859_1";

    /**
     * A search engine
     */
    protected SearchEngineImpl engine;

    /**
     * The directory into which the de-indexed files should be written
     */
    protected File outputDirectory;

    /**
     * The reporter for recording progress of the test
     */
    protected TestReporter reporter;
    
    /**
     * Flags used to describe options permitted in the command line invocation
     */
    static String flags = "c:d:f:i:l:o:r:s:x:";

    /**
     * The filename of the configuration file if provided on the command line args
     */
    protected String cmFile;

    /**
     * The directory to contain the index
     */
    protected String indexDir;

    /**
     * The name of the configuration file preferred for use. This enables us to dump
     * synchronously and avoid any multithreaded nastiness when re-reading the log
     */
    private final String CONFIG_FILE = "MergeTest-config.xml";


    /**
     * Help!
     */
    static void usage() {
        System.out
                .println("Usage: java com.sun.labs.minion.test.regression.merge.MergeTest -c <character_encoding> " +
                        "-d <index_directory> -f <document_directory> " +
                        "-i <number_of_iteratins> -l <log_level> " +
                        "-o <output_directory> [-r <report_filename> ] " +
                        "-s <number_of_documents_to_index_per_iteration> " +
                        "[-x <config_file>]");
    }
    
    
    /**
     * Main invocation of the test
     * @param args
     */
    public static void main(String[] args) {

        String documentDir = null;
        Getopt gopt = new Getopt(args, flags);
        int c;
        String indexDir = null;
        String cmFile = null;
        String reportFileName = null;
        String outputDir = "/tmp";
        int iterations = 5;
        int documentCount = 20;

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

            case 'r':
                reportFileName = gopt.optArg;
                break;       
            
            case 's':
                try {
                    documentCount = Integer.parseInt(gopt.optArg);
                } catch (NumberFormatException nfe) {
                }
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
        
        
        

        MergeTest test = null;
        try {
            test = new MergeTest(indexDir, 
                    cmFile, documentCount, iterations, documentDir,
                    outputDir, args);
            if (reportFileName != null) {
                test.setReportFilename(reportFileName);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.exit(1);
        }
        test.test();

    }

    /**
     * Set the name of the report file
     * @param reportFileName the filename of the report file
     */
    private void setReportFilename(String reportFileName) {
        reporter.setReportFilename(reportFileName);
        
    }

    /**
     * Creates a new instance of MergeTest, and sets up internal fields.
     * @param dc the number of documents to be indexed per iteration
     * @param i the number of iterations
     * @param dDir the document directory
     * @param oDir the output directory
     * @param args 
     * @throws FileNotFoundException 
     */
    MergeTest(String indexDir, String cmFile,
            int dc, int i, String dDir, String oDir, String[] args) throws FileNotFoundException {
        this();
        this.indexDir = indexDir;
        this.cmFile = cmFile;
        documentCount = dc;
        iterations = i;
        documentDirectory = new File(dDir);
        if(!documentDirectory.exists()) {
            throw new FileNotFoundException("Document directory " + dDir + " does not exist");
        }
        outputDirectory = new File(oDir);
        if (!outputDirectory.exists()) {
            throw new FileNotFoundException("Output directory " + oDir + " does not exist");
        }
        getFileList();
        createSearchEngine(cmFile, indexDir);
        openReporter(args);
        
    }

    /**
     * Default contructor. Sets up the index inverter
     */
    public MergeTest() {
        //
        // Set up the index inverter
        IndexInverter.DEBUG = false;
    }

    /**
     * Creates a new instance of a search engines and assigns it to the field
     */
    protected void createSearchEngine(String cmFile, String indexDir) {
        if (cmFile != null) {
            try {
                engine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(cmFile, (new File(cmFile).toURI().toURL()));
            } catch (SearchEngineException se) {
                log.error("Indexer", 1, "Error opening collection", se);
                return;
            } catch (MalformedURLException murl) {
                log.error("Indexer", 1, "Error reading configuration file", murl);
                return;
            }
        } else {
            URL configURL = getClass().getResource(CONFIG_FILE);
            try {
                engine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(indexDir, configURL);
            } catch (SearchEngineException se) {
                log.error("Indexer", 1, "Error opening collection", se);
                return;
            }
        }
    }

    /**
     * Creates the list of filenames from the document directory
     */
    private void getFileList() {
        files = documentDirectory.list();
    }

    /**
     * Returns a random list of filenames from the document directory
     * @return an array of string of size <code>documentCount</code>
     */
    private String[] getTestFiles() {
        String[] testFiles = new String[documentCount];
        Set randomIndices = getRandomIndices();
        int i = 0;
        for (Iterator iter = randomIndices.iterator(); iter.hasNext();) {
            Integer index = (Integer) iter.next();
            testFiles[i++] = files[index];
        }
        return testFiles;
    }

    /**
     * Returns a set of size <code>documentCount</code> of random integers between 0 and <code>documentCount</code>
     * @return
     */
    private Set getRandomIndices() {
        Set<Integer> randomIndices = new HashSet<Integer>();
        Random numberGen = new Random();
        while (randomIndices.size() < documentCount) {
            randomIndices.add(numberGen.nextInt(files.length));
        }
        return randomIndices;
    }

    /**
     * Runs the test, in the following order, for each iteration:
     * <ol>
     * <li>index the test files</li>
     * <li>invert the index to files in the output directory</li>
     * <li>diff the files in the output directory against those in the document directory</li>
     * <li>merge the partitions</li>
     * <li>invert the index to files in the output directory</li>
     * <li>diff the files in the output directory against those in the document directory</li>
     * </ol>
     * 
     */
    private void test() {
        int i = 0;
        try {
            
            reporter.startReport();
            while (i < iterations) {
                reporter.startIteration(i);
                //
                // Get the test files
                String[] testFiles = getTestFiles();
                //
                // Index the test files
                index(testFiles);
                //
                // Invert the index and diff against the originals
                invert();
                correlateInvertedFiles(testFiles);
                diff();
                //
                // Optimise the partitions, i.e. merge
                optimize();
                //
                // Invert the index and diff against the originals
                invert();
                correlateInvertedFiles(testFiles);
                diff();

                reporter.endIteration();
                i++;
            }
            
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            try {
                engine.close();
                reporter.endReport();
            } catch (IOException e) {
                System.err.println(e);
            } catch (SearchEngineException e) {
                System.err.println(e);
            }
        }
    }

    /**
     * Open the test reporter.
     * @param args the arguments that started the execution of the program
     */
    protected void openReporter(String[] args) {
        reporter = new MergeTestReporter(args);
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
        reporter.startInverting();
        try {
            inverter.outputInvertedIndex();
        } catch (Exception e) {
            System.err.println(e);
            reporter.invertException(e);
        }
        try {
            inverter.close();
        } catch (SearchEngineException e) {
            System.err.println(e);
            reporter.invertException(e);
        }
        reporter.endInverting();
    }

    /**
     * Merges the partitions of the index.
     * @throws IOException
     */
    protected void optimize() throws IOException {
        reporter.startOptimizing();
        try {
            engine.optimize();
        } catch (SearchEngineException e) {
            log.error(logTag, 0, "Error optimising", e);
            reporter.optimizeException(e);
        }
        reporter.endOptimizing();
    }

    /**
     * Compares the files in the output directory to the document directory.<br>
     * @throws IOException
     */
    protected void diff() throws IOException {
        reporter.startDiff();
        
        String[] outputDocuments = outputDirectory.list();
        for (int i = 0; i < outputDocuments.length; i++) {
            String filename = outputDocuments[i];
            boolean success = diff(filename);
            reporter.reportDiff(filename, success);
        }
        reporter.endDiff();
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
     * Indexes the test files
     * @param testFiles the files to be indexed
     * @throws IOException
     */
    private void index(String[] testFiles) throws IOException {
        long totalLen = 0;
        int nDocs = 0;
        long start = System.currentTimeMillis();
        reporter.startIndexing();
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
                reporter.index(file, true);
                if (longFile) {
                    engine.flush();
                }

                nDocs++;
                totalLen += len;

                if (nDocs % 10 == 0) {
                    IndexTest.reportProgress(start, totalLen, nDocs);
                }

            } catch (SearchEngineException se) {
                log.error("Indexer", 1, "Error indexing document " + f, se);
                reporter.index(file, false);
            } catch (Exception e) {
                log.error(logTag, 0, "Error indexing", e);
                reporter.index(file, false);
            }
        }
        try {
            engine.flush();
        } catch (SearchEngineException e) {
            e.printStackTrace();
        }
        reporter.endIndexing();

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
            reporter.startReportMissingFiles(0);
            reporter.endReportMissingFiles();
        } else {
            //
            //There are some files that have been indexed that are  
            //missing from the output directory
            reporter.startReportMissingFiles(invertedSet.size());
            for (Iterator iter = invertedSet.iterator(); iter.hasNext();) {
                String filename = (String) iter.next();
                reporter.missingFile(filename);
            }
           reporter.endReportMissingFiles();
        }
    }

    

}
