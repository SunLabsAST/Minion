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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;

import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.File;
import java.net.URL;
import java.util.logging.Handler;
import java.util.logging.Logger;
import com.sun.labs.minion.util.Getopt;
import java.util.logging.Level;

public class Mutator extends SEMain implements Runnable {

    protected SearchEngine engine;

    protected Thread indexingThread;

    protected String logTag;

    protected List<String> indexList;

    protected static String stuff;

    protected static int nLines;

    protected static int maxUpd;

    protected static int nUpdates;

    protected static int updateInterval;

    Logger logger = Logger.getLogger(getClass().getName());

    private static String indexDir;

    public Mutator(SearchEngine engine, List<String> l, int index)
            throws SearchEngineException {
        this.engine = engine;
        logTag = "M" + index;
        indexList = new ArrayList<String>(l);
    }

    public void run() {

        int nIter = 1;
        boolean done = false;
        Thread.currentThread().setName(logTag);
        Random rand = new Random();

        //
        // Loop until we're done.
        mainLoop:
        while(!done) {

            //
            // How many documents will we do this time around?  Note that
            // the answer can be 0!
            int toIndex = rand.nextInt(maxUpd);
            logger.info("Iteration: " + nIter + " " + toIndex + " docs");

            for(int i = 0; i < toIndex; i++) {

                //
                // Choose a random document from the list
                IndexableFile file =
                        new IndexableFile(
                        indexList.get(rand.nextInt(indexList.size())));

                //
                // Try to index it.
                try {
                    engine.index(makeDocument(file));
                } catch(SearchEngineException se) {
                    logger.log(Level.SEVERE, "Error indexing document " + file,
                            se);
                } catch(Exception e) {
                    logger.log(Level.SEVERE, "Error indexing", e);
                }
            }

            if(toIndex > 0) {
                try {
                    engine.flush();
                } catch(SearchEngineException se) {
                    logger.log(Level.SEVERE, "Error flushing", se);
                }
            }

            if(updateInterval > 0) {

                int ms = rand.nextInt(updateInterval * 1000);

                logger.info("Sleeping " + ms + "ms");

                try {
                    Thread.sleep(ms);
                } catch(InterruptedException ie) {
                }
            }

            //
            // See whether we're done.
            done = nUpdates >= 0 && nIter >= nUpdates;
            nIter++;
        }
    }

    public static void usage() {
        System.out.println(
                "Usage: java Mutator\n" +
                " -i <file list>   " +
                "Directory or file list to index (Required)\n" +
                " -d <index dir>   " +
                "Directory containing index (Required)\n" +
                " -w <dict type>   " +
                "For wildcard expansion (BG, RT).  Default: BG\n" +
                " -e <entry type>  " +
                "Postings entry type (Word, Doc).  Default: Word\n" +
                " -r <num>         " +
                "The number of times to update, < 0 means run continuously (default: 10)\n" +
                " -s <num>         " +
                "Maximum number of seconds between update runs\n" +
                " -t <num>         " +
                "The number of threads to run\n" +
                " -g <num>         " +
                "The number of concurrent merge threads to allow (Default: 1)\n" +
                " -n <num>         " +
                "The number of lines in the input file (required)\n" +
                " -m <num>          " +
                "The maximum number of files to index/update\n" +
                " -x <file>        " +
                "An XML configuration file specifying extra props for " +
                "the index");
        return;
    }

    public static void main(String[] args) throws java.io.IOException, SearchEngineException {

        String flags = "i:d:w:e:s:n:m:r:t:g:x:";
        stuff = null;
        updateInterval = 10;
        nUpdates = 10;
        nLines = Integer.MAX_VALUE;
        maxUpd = 100;
        int nThreads = 1;
        Getopt gopt = new Getopt(args, flags);
        int c;
        URL cmFile = null;

        if(args.length == 0) {
            usage();
            return;
        }

        //
        // Set up the log.
        Logger logger = Logger.getLogger("");
        for(Handler h : logger.getHandlers()) {
            h.setFormatter(new SimpleLabsLogFormatter());
        }

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

                case 's':
                    try {
                        updateInterval = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        //
                        // Unknown type.
                        logger.warning("Invalid update interval: " + gopt.optArg);
                        usage();
                        return;
                    }
                    break;

                case 'm':
                    try {
                        maxUpd = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        //
                        // Unknown type.
                        logger.warning("Invalid maximum update: " + gopt.optArg);
                        usage();
                        return;
                    }
                    break;

                case 'n':
                    try {
                        nLines = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        //
                        // Unknown type.
                        logger.warning("Invalid number of lines: " + gopt.optArg);
                        usage();
                        return;
                    }
                    break;

                case 'r':
                    try {
                        nUpdates = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        //
                        // Unknown type.
                        logger.warning("Invalid number of updates: " +
                                gopt.optArg);
                        usage();
                        return;
                    }
                    break;
                case 't':
                    try {
                        nThreads = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        //
                        // Unknown type.
                        logger.warning("Invalid number of threads: " +
                                gopt.optArg);
                        usage();
                        return;
                    }
                    break;
                case 'x':
                    cmFile = (new File(gopt.optArg)).toURI().toURL();
                    break;
            }

        }

        if(stuff == null) {
            logger.warning("You must specify a list of files to index.");
            usage();
            return;
        }

        if(cmFile == null) {
            cmFile = Mutator.class.getResource("Mutator-config.xml");
        }

        //
        // Open the file of file names.
        BufferedReader inFile = null;

        try {
            inFile = new BufferedReader(new FileReader(stuff));
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Unable to open file: " + stuff, ioe);
            usage();
            return;
        }

        String l;
        int n = 0;
        List<String> il = new ArrayList<String>();
        while((l = inFile.readLine()) != null && n < nLines) {
            il.add(l);
        }

        Thread[] threads = new Thread[nThreads];
        Mutator[] muts = new Mutator[nThreads];

        SearchEngine engine = SearchEngineFactory.getSearchEngine(indexDir,
                cmFile);

        try {
            for(int i = 0; i < nThreads; i++) {
                muts[i] = new Mutator(engine, il, i + 1);
                threads[i] = new Thread(muts[i]);
                threads[i].start();
            }
        } catch(SearchEngineException se) {
            logger.log(Level.SEVERE, "Error creating mutators", se);
            return;
        }

        for(int i = 0; i < nThreads; i++) {
            try {
                threads[i].join();
            } catch(InterruptedException ie) {
            }
        }

        //
        // Close the search engines.
        try {
            engine.close();
        } catch(SearchEngineException se) {
            logger.log(Level.SEVERE, "Error closing engines", se);
        }
    }
} // Mutator
