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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.test.IndexTest;
import com.sun.labs.minion.test.SEMain;
import com.sun.labs.minion.test.regression.IndexInverter;

import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.util.props.PropertyException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MergeTestReplayer replays the log from MergeTest and recreates the run of
 * files indexed. It emits its results on the console.
 * 
 * @author Bernard Horan
 * 
 */
public class MergeTestReplayer extends MergeTest {
    /**
     * MergeTestReporter is a TestReporter that emits its report to the console.
     * Many of these methods are no-ops.
     * 
     */
    public class MergeReplayReporter implements TestReporter {
        private final boolean ONLY_ERRORS = true;

        /**
         * Creata a new instance of me
         */
        public MergeReplayReporter() {
            super();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startReport()
         */
        public void startReport() throws IOException {
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#reportTestFiles(java.lang.String[])
         */
        public void reportTestFiles(String[] testFiles) throws IOException {
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endReport()
         */
        public void endReport() throws IOException {
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startIteration(int)
         */
        public void startIteration(int i) throws IOException {
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endIteration()
         */
        public void endIteration() throws IOException {
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startIndexing()
         */
        public void startIndexing() throws IOException {
            System.out.println("Starting indexing");
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#index(java.lang.String,
         *      boolean)
         */
        public void index(String file, boolean success) throws IOException {
            if (!ONLY_ERRORS && success) {
                System.out.print("Indexed " + file + " with ");
                if (success) {
                    System.out.println("same result as log");
                } else {
                    System.out.println("different result from log");
                }
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endIndexing()
         */
        public void endIndexing() throws IOException {
            System.out.println("Finished indexing");
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startInverting()
         */
        public void startInverting() throws IOException {
            System.out.println("Start inverting");
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endInverting()
         */
        public void endInverting() throws IOException {
            System.out.println("End inverting");
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#invertException(java.lang.Exception)
         */
        public void invertException(Exception e) throws IOException {
            System.err.println("Invert Exception: " + e);
            e.printStackTrace();
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startDiff()
         */
        public void startDiff() throws IOException {
            System.out.println("Starting diff");
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#reportDiff(java.lang.String)
         */
        public void reportDiff(String outputPathname, boolean success)
                throws IOException {
            if (!ONLY_ERRORS && success) {
                System.out.print("Compared " + outputPathname + " with ");
                if (success) {
                    System.out.println("same result as log");
                } else {
                    System.out.println("different result from log");
                }
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#diffException(java.lang.Throwable)
         */
        public void diffException(Throwable t) throws IOException {
            System.err.println("Diff exception: " + t);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endDiff(int)
         */
        public void endDiff() throws IOException {
            System.out.println("Diff ended");
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startOptimizing()
         */
        public void startOptimizing() throws IOException {
            System.out.println("Starting optimize");
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endOptimizing()
         */
        public void endOptimizing() throws IOException {
            System.out.println("Ending optimize");
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#optimizeException(com.sun.labs.minion.SearchEngineException)
         */
        public void optimizeException(SearchEngineException e)
                throws IOException {
            System.err.println("Optimize exception: " + e);
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#setReportFilename(java.lang.String)
         */
        public void setReportFilename(String reportFileName) {
        }

        /* (non-Javadoc)
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#endReportMissingFiles()
         */
        public void endReportMissingFiles() throws IOException {
            
            
        }

        /* (non-Javadoc)
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#missingFile(java.lang.String)
         */
        public void missingFile(String filename) throws IOException {
            
            
        }

        /* (non-Javadoc)
         * @see com.sun.labs.minion.test.regression.merge.TestReporter#startReportMissingFiles(int)
         */
        public void startReportMissingFiles(int i) throws IOException {
            
            
        }

    }

    /**
     * Location of the xmlFile containing the output from the MergeTest
     */
    private String xmlFile;

    /**
     * The system environment at startup
     */
    private Map<String, String> environment;

    /**
     * Java system properties at startup
     */
    private Properties systemProperties;

    /**
     * The memory settings at startup
     */
    private Map<String, Long> memoryMap;

    /**
     * The handler used when parsing the XML
     */
    private MergeReportHandler handler;

    /**
     * The java command line arguments used to start MergeTest
     */
    private List<String> arguments;

    /**
     * Create a new instance of me
     * 
     * @param string
     *            the location of the XML file containing the log of a MergeTest
     */
    public MergeTestReplayer(String indexDir, String cmFile, String string) {
        super();
        this.indexDir = indexDir;
        this.cmFile = cmFile;
        xmlFile = string;
        environment = System.getenv();
        systemProperties = System.getProperties();
        memoryMap = new HashMap<String, Long>();
        memoryMap.put("free", Runtime.getRuntime().freeMemory());
        memoryMap.put("total", Runtime.getRuntime().totalMemory());
        memoryMap.put("max", Runtime.getRuntime().maxMemory());
        arguments = new ArrayList<String>();
        openReporter(null);
    }

    /**
     * Add an option to the table of arguments
     * 
     * @param entryKey
     *            the argument flag, e.g. '-x'
     * @param entryValue
     *            the value of the argument, e.g. '10'
     */
    public void addArgument(String entryKey, String entryValue) {
        arguments.add(entryKey);
        arguments.add(entryValue);
    }

    /**
     * Compare the value of an environment key with the environment at startup.<br>
     * If the values differ, writes an entry to system.err
     * 
     * @param entryKey
     *            the key for the environment value
     * @param entryValue
     *            the environment value
     */
    public void checkEnvironment(String entryKey, String entryValue) {
        String envValue = environment.get(entryKey);
        if (envValue == null) {
            System.err.println("No environment value for " + entryKey);
        } else {
            if (!envValue.equals(entryValue)) {
                System.err.println("Environment values differ for key "
                        + entryKey);
            }
        }
    }

    /**
     * Compare the value of a system property from the log with the same
     * property at startup.<br>
     * If the values differ, writes an entry to system.err
     * 
     * @param entryKey
     *            the key of the system property
     * @param entryValue
     *            the value of the system property
     */
    public void checkProperty(String entryKey, String entryValue) {
        String sysValue = (String) systemProperties.get(entryKey);
        if (sysValue == null) {
            System.err.println("No system property for " + entryKey);
        } else {
            if (!sysValue.equals(entryValue)) {
                System.err.println("Property values differ for property "
                        + entryKey);
            }
        }
    }

    /**
     * Compare the value for a memory setting from the log with the same setting
     * at startup.<br>
     * If the values differ, writes an entry to system.err
     * 
     * @param entryKey
     *            the key for the memory setting (currently <i>total</i>,
     *            <i>free</i> and <i>max</i>)
     * @param entryValue
     *            the value for a memory setting
     */
    public void checkMemory(String entryKey, String entryValue) {
        Long memValue = memoryMap.get(entryKey);
        if (!memValue.equals(new Long(entryValue))) {
            System.err.println("Memory values differ for " + entryKey);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.labs.minion.test.regression.merge.MergeTest#invert()
     */
    protected void invert() {
        try {
            super.invert();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.labs.minion.test.regression.merge.MergeTest#optimize()
     */
    protected void optimize() {
        try {
            super.optimize();
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    /**
     * Start the application. Provide the location of the log file using the
     * <code>-r</code> option.
     * 
     * @param args
     */
    public static void main(String[] args) {
        /**
         * Tag for the log
         */
        logTag = "MergeTestReplayer";
        String flags = "r:";
        Getopt gopt = new Getopt(args, flags);
        int c;
        String reportFileName = null;
        while ((c = gopt.getopt()) != -1) {
            switch (c) {

            case 'r':
                reportFileName = gopt.optArg;
                break;
            }
        }
        if (reportFileName == null) {
            usage();
            return;
        }
        MergeTestReplayer replayer = new MergeTestReplayer(null, null,
                reportFileName);
        try {
            replayer.replay();
            System.exit(0);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Replay the log file
     * 
     * @throws SAXException
     * @throws IOException
     */
    private void replay() throws SAXException, IOException {
        XMLReader parser = XMLReaderFactory.createXMLReader();
        handler = new MergeReportHandler(this);
        parser.setContentHandler(handler);

        parser.parse(xmlFile);
    }

    /**
     * Return the entry that's at the top of the handlerStack for the content
     * handler
     * 
     * @return the object at the top of the handler stack
     */
    public Object peekHandlerStack() {
        return handler.peekHandlerStack();
    }

    /**
     * All the information about the configuration has been read from the log,
     * so now we read the arguments, and create the search engine
     * 
     * @throws SearchEngineException
     * @throws PropertyException
     * @throws IOException
     */
    public void completeConfiguration() {
        String[] args = arguments.toArray(new String[0]);
        documentDirectory = null;
        Getopt gopt = new Getopt(args, flags);
        int c;
        String cmFile = null;
        String oDir = "/tmp";
        String docDirectory = null;
        iterations = 5;
        documentCount = 20;

        if (args.length == 0) {
            usage();
            return;
        }

        Thread.currentThread().setName("MergeTestReplayer");

        int logLevel = 3;

        //
        // Set up the logging for the search engine. We'll send everything
        // to the standard output, except for errors, which will go to
        // standard error. We'll set the level at 3, which is pretty
        // verbose.
        Logger logger = Logger.getLogger(MergeTestReplayer.class.getName());

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
                docDirectory = gopt.optArg;
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
                oDir = gopt.optArg;
                break;

            case 'r':
                String originalReportFile = gopt.optArg;
                if (!originalReportFile.equals(xmlFile)) {
                    System.err.println("Original report file name does not match current report file name");
                }
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

        if (indexDir == null && cmFile == null) {
            logger.warning("You must specify an index directory.");
            usage();
            return;
        }

        if (docDirectory == null) {
            logger.warning("You must specify a document directory.");
            usage();
            return;
        } else {
            documentDirectory = new File(docDirectory);
        }
        outputDirectory = new File(oDir);
        createSearchEngine(cmFile, indexDir);
        //
        // Set up the index inverter
        IndexInverter.DEBUG = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.labs.minion.test.regression.merge.MergeTest#openReporter(java.lang.String[])
     */
    protected void openReporter(String[] args) {
        reporter = new MergeReplayReporter();
    }

    public ElementHandler getElementHandler(String string) {
        return handler.getElementHandler(string);
    }

    public void index(Map<String, Boolean> fileTable) {
        long totalLen = 0;
        int nDocs = 0;
        long start = System.currentTimeMillis();
        try {
            reporter.startIndexing();
            for (Iterator iter = fileTable.keySet().iterator(); iter.hasNext();) {
                String file = (String) iter.next();
                ;

                IndexableFile f = new IndexableFile(file, charEnc);
                Indexable document = SEMain.makeDocument(f);

                try {
                    long len = f.length();
                    boolean longFile = len > 400000;
                    if (longFile) {
                        logger.info("Long: " + f.length());
                        engine.flush();
                    }
                    engine.index(document);
                    reporter.index(file, fileTable.get(file));
                    if (longFile) {
                        engine.flush();
                    }

                    nDocs++;
                    totalLen += len;

                    if (nDocs % 10 == 0) {
                        IndexTest.reportProgress(logger, start, totalLen, nDocs);
                    }

                } catch (SearchEngineException se) {
                    logger.log(Level.SEVERE, "Error indexing document " + f, se);
                    reporter.index(file, false == fileTable.get(file));
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error indexing", e);
                    reporter.index(file, false == fileTable.get(file));
                }
            }

            try {
                engine.flush();
            } catch (SearchEngineException e) {
                e.printStackTrace();
            }
            reporter.endIndexing();
        } catch (IOException e) {
            System.err.println(e);
        }

    }

    /**
     * Help!
     */
    static void usage() {
        System.out
                .println("Usage: java com.sun.labs.minion.test.merge.MergeTestReplayer -r <report_file_name>");
    }

    /**
     * Compare a file in the output directory with its original. Report the success of the 
     * comparison.
     * @param pathName the name of the file in the output directory
     * @param oldSuccess a boolean indicating the success as recorded in the log
     */
    void diff(String pathName, boolean oldSuccess) {
        File file = new File(pathName);
        String filename = file.getName();
        try {
            boolean success = diff(filename);
            reporter.reportDiff(filename, oldSuccess == success);
        } catch (IOException e) {
            System.err.println(e);
        }

    }

}
