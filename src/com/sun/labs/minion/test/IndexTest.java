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
package com.sun.labs.minion.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.File;

import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;

import com.sun.labs.minion.util.Getopt;
import com.sun.labs.util.LabsLogFormatter;
import java.util.logging.Handler;

public class IndexTest extends SEMain {

    private static final boolean DEBUG = false;

    public static void usage() {
        System.out.println(
                "Usage: java IndexTest [options] [file file....]\n" +
                " -i <file list>   " +
                "Directory or file list to index (Required)\n" +
                " -c <encoding>    " +
                "The default character encoding to use when reading files\n" +
                " -d <index dir>   " +
                "Directory containing index (Required)\n" +
                " -e <entry class> " +
                "The class for the entries in the main dictionary\n" +
                " -k <dir>         " +
                " The directory to use for lock files\n" +
                " -n <number>      " +
                "Number of pipelines to use.  Default: 1\n" +
                " -m <number>      " +
                "The number of MB that each pipeline will store " +
                "before dumping\n" +
                " -r <number>      " +
                "The merge rate for partitions.\n" +
                " -u <number>      " +
                "A dump indicator as a number of documents\n" +
                " -x <file>        " +
                "An XML configuration file specifying extra props for " +
                "the index\n" +
                " -t <engine type> " +
                "The type of engine (one of the names in the config file)\n");
        return;
    }

    public static void reportProgress(Logger logger,
            long start,
            long docsize,
            int n) {
        double secs = (System.currentTimeMillis() - start) / 1000.0;
        double MB = docsize / (1024.0 * 1024.0);

        logger.info(String.format(
                "%d documents, %.2fMB, %.2fs, %.2fMB/h, %.2fMB total mem",
                n, MB, secs, MB / (secs / 3600),
                Runtime.getRuntime().totalMemory() / (1024.0 * 1024.0)));
    }

    public static void main(String[] args) throws MalformedURLException {

        String flags = "i:c:d:e:k:n:m:l:pr:u:x:t:";
        String stuff = null;
        Getopt gopt = new Getopt(args, flags);
        String charEnc = "8859_1";
        int c;
        int mergeRate = 5;
        int dumpDocs = -1;
        long totalLen = 0;
        int nDocs = 0;
        String indexDir = null;
        URL cmFile = null;
        String engineType = null;
        long start = System.currentTimeMillis();


        if(args.length == 0) {
            usage();
            return;
        }

        Thread.currentThread().setName("IndexTest");

        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
        }
        boolean profiling = false;

        //
        // Set up the logging for the search engine.  We'll send everything
        // to the standard output, except for errors, which will go to
        // standard error.  We'll set the level at 3, which is pretty
        // verbose.
        Logger logger = Logger.getLogger(IndexTest.class.getName());

        //
        // Handle the options.
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'i':
                    stuff = gopt.optArg;
                    break;

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'c':
                    charEnc = gopt.optArg;
                    break;

                case 'l':
                    logger.setLevel(Level.parse(gopt.optArg));
                    break;

                case 'p':
                    profiling = true;
                    break;

                case 'r':
                    try {
                        mergeRate = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        logger.warning("Bad merge rate: " + gopt.optArg +
                                " using default: " +
                                mergeRate);
                    }
                    break;

                case 'u':
                    try {
                        dumpDocs = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        logger.warning("Bad document dump number: " +
                                gopt.optArg + ", ignoring");
                    }

                    if(dumpDocs <= 0) {
                        logger.warning("Dump documents must be greater than 0, using " +
                                "memory based dumping");
                        dumpDocs = -1;
                    }
                    break;

                case 'x':
                    cmFile = (new File(gopt.optArg)).toURI().toURL();
                    break;

                case 't':
                    engineType = gopt.optArg;
                    break;
            }
        }


        List<String> clFiles = new ArrayList<String>();

        for(int i = gopt.optInd; i < args.length; i++) {
            clFiles.add(args[i]);
        }

        //
        // We may have gotten a larger log level.
        if(indexDir == null && cmFile == null) {
            logger.warning("You must specify an index directory.");
            usage();
            return;
        }

        if(stuff == null && clFiles.size() == 0) {
            logger.warning("You must specify a directory or " +
                    "list of files to index with either the " +
                    "-i option or on the command line");
            usage();
            return;
        }

        //
        // Open our engine for use.  We give it the properties that we read
        // and no query properties.
        SearchEngine engine;
        try {
            if(engineType == null) {
                engine = SearchEngineFactory.getSearchEngine(indexDir, cmFile);
            } else {
                engine = SearchEngineFactory.getSearchEngine(indexDir,
                        engineType, cmFile);
            }

            //
            // Define the fields we know about
            defineFields(engine);
        } catch(SearchEngineException se) {
            logger.log(Level.SEVERE, "Error opening collection", se);
            return;
        }

//        ((SearchEngineImpl) engine).setLongIndexingRun(true);

        if(stuff != null) {
            BufferedReader r;
            try {
                r = new BufferedReader(new FileReader(stuff));
            } catch(java.io.IOException ioe) {
                logger.log(Level.SEVERE, "Error opening list of files", ioe);
                return;
            }

            String n;

            try {
                while((n = r.readLine()) != null) {

                    IndexableFile f = new IndexableFile(n, charEnc);
                    Indexable document = makeDocument(f);

                    try {
                        long len = f.length();
                        boolean longFile = len > 400000;
//                        if(longFile) {
//                        logger.info("Long: " + f.length());
//                            engine.flush();
//                        }
                        engine.index(document);
//                        if(longFile) {
//                            engine.flush();
//                        }

                        nDocs++;
                        totalLen += len;

                        if(nDocs % 1000 == 0) {
                            reportProgress(logger, start, totalLen, nDocs);
                        }

                    } catch(SearchEngineException se) {
                        logger.log(Level.SEVERE, "Error indexing document " + f,
                                se);
                    } catch(Exception e) {
                        logger.log(Level.SEVERE, "Error indexing", e);
                    }
                }
            } catch(java.io.IOException ioe) {
                logger.log(Level.SEVERE,
                        "Error reading from list of files, continuing", ioe);
            }
        }

        for(Iterator i = clFiles.iterator(); i.hasNext();) {

            IndexableFile f = new IndexableFile((String) i.next(), charEnc);
            Indexable document = makeDocument(f);
            try {
                engine.index(document);
            } catch(SearchEngineException se) {
                logger.severe("Error indexing: " + f);
            }
            nDocs++;
            totalLen += f.length();
        }

        reportProgress(logger, start, totalLen, nDocs);

        //
        // We're done, close down the engine.
        try {
            engine.close();
        } catch(SearchEngineException se) {
            logger.log(Level.SEVERE, "Error closing engine", se);
        }

        if(profiling) {
            logger.info("Finished.  Sleeping for a few minutes.");
            try {
                Thread.sleep(600000);
            } catch(Exception e) {
            }
        }

    }
} // IndexTest
