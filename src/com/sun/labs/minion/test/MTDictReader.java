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

import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.samples.MTQuery;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.util.NanoWatch;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generates a set of terms, evenly space through a dictionary, that are used
 * to "get" each term from the dictionary.  Each thread shuffles the collection
 * of terms to generate more randomness in the access order.
 */
public class MTDictReader implements Runnable {
    protected DiskDictionary dict = null;
    protected List<String> terms = null;
    protected int threadNum = 0;
    protected long stopTime = 0;
    protected Logger logger = null;
    protected NanoWatch nw = null;
    protected DiskDictionary.LookupState lus;

    public MTDictReader(List<String> terms, DiskDictionary dict, int duration, int threadNum) {
        this.terms = new ArrayList<String>(terms);
        this.dict = dict;
        this.threadNum = threadNum;
        stopTime = System.currentTimeMillis() + duration * 1000;
        logger = Logger.getLogger(getClass().getName());
        nw = new NanoWatch();
        Collections.shuffle(this.terms);
    }

    @Override
    public void run() {
        int iter = 0;
        while(System.currentTimeMillis() < stopTime) {
            for(String term : terms) {
                nw.start();
                QueryEntry entry = dict.get(term);
                nw.stop();
                if(!term.equals(entry.getName())) {
                    logger.severe(String.format("Uh oh! thread: %d term: %s entry: %s", threadNum, term, entry.getName()));
                }
                if(logger.isLoggable(Level.FINER)) {
                    logger.finer(String.format(
                            "thread: %d %d/%d/%d term: %s %.2fms",
                            threadNum,
                            iter, nw.getClicks(), terms.size(),
                            term, nw.getLastTimeMillis()));
                }
                if(nw.getClicks() % 500 == 0) {
                    logger.fine(String.format(
                            "thread: %d queries: %d average lookup time: %.2fms",
                            threadNum, nw.getClicks(), nw.getAvgTimeMillis()));
                    if(System.currentTimeMillis() >= stopTime) {
                        break;
                    }
                }
            }
            logger.fine(String.format(
                    "thread: %d iter: %d average lookup time: %.2fms", threadNum,
                    iter, nw.getAvgTimeMillis()));
            iter++;
        }
    }

    public static void usage() {
        System.err.print(
                "Usage: MTDictReader \n" +
                "      -d <index dir> \n" +
                "      [-n <num threads; default 4>]\n" +
                "      [-t <duration in seconds; default 300>]\n" +
                "      [-x <config file>]\n" +
                "      [-m <max number of terms to read; default 10000>\n");
    }

    public static void main(String[] args) throws Exception {
        if(args.length == 0) {
            usage();
            return;
        }

        Logger rl = Logger.getLogger("");
        for(Handler h : rl.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new SimpleLabsLogFormatter());
            try {
                h.setEncoding("utf-8");
            } catch(Exception ex) {
                rl.severe("Error setting output encoding");
            }
        }

        Logger logger = Logger.getLogger(MTQuery.class.getName());
        String flags = "d:n:t:l:x:m:";
        Getopt gopt = new Getopt(args, flags);

        int nThreads = 4;
        int duration = 300;
        String indexDir = null;
        String configFile = null;
        int maxTerms = 10000;
        int c;

        //
        // Handle the options.
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'l':
                    logger.setLevel(Level.parse(gopt.optArg));
                    break;

                case 'n':
                    nThreads = Integer.parseInt(gopt.optArg);
                    break;

                case 't':
                    duration = Integer.parseInt(gopt.optArg);
                    break;

                case 'x':
                    configFile = gopt.optArg;
                    break;

                case 'm':
                    maxTerms = Integer.parseInt(gopt.optArg);
                    break;
                    
                default:
                    usage();
                    System.exit(-1);
            }
        }

        if(indexDir == null) {
            usage();
            return;
        }

        SearchEngine engine =
                SearchEngineFactory.getSearchEngine(indexDir, configFile);
        SearchEngineImpl engineImpl = (SearchEngineImpl)engine;

        //
        // Find the largest main dictionary
        List<DiskPartition> partitions =
                engineImpl.getPM().getActivePartitions();
        DiskDictionary selectedDict = null;
        int numTerms = 0;
        for(DiskPartition part : partitions) {
            for(DiskField df : ((InvFileDiskPartition) part).getDiskFields()) {
                DiskDictionary main = (DiskDictionary) df.getTermDictionary();
                if(main.size() > numTerms) {
                    numTerms = main.size();
                    selectedDict = main;
                }
            }
        }

        //
        // Collect all the terms, spreading out if we have more terms than
        // the max.
        int mod = 1;
        if (numTerms > maxTerms) {
            mod = numTerms / maxTerms;
            if (mod == 0) {
                mod = 1;
            }
        }
        logger.info("Dictionary contains " + numTerms + " terms, taking every " +
                (mod == 1 ? "term " : mod + " terms ") +
                "up to " + maxTerms);

        int curr = 0;
        List<String> terms = new ArrayList<String>();
        Iterator dit = selectedDict.iterator();
        while (dit.hasNext() && terms.size() < maxTerms) {
            QueryEntry e = (QueryEntry) dit.next();
            if (curr++ % mod == 0) {
                terms.add((String)e.getName());
            }
        }

        //
        // Kick off our test code
        try {
            NanoWatch total;
            QueryStats qs;
            if(nThreads == 1) {
                MTDictReader single = new MTDictReader(terms, selectedDict, duration, 0);
                single.run();
                total = single.nw;
                qs = single.lus.getQueryStats();
            } else {
                MTDictReader[] mtqs = new MTDictReader[nThreads];
                Thread[] threads = new Thread[nThreads];
                for(int i = 0; i < nThreads; i++) {
                    mtqs[i] = new MTDictReader(terms, selectedDict, duration, i);
                    threads[i] = new Thread(mtqs[i]);
                    threads[i].setName("mtdictreader-" + i);
                    threads[i].start();
                }

                total = new NanoWatch();
                qs = new QueryStats();
                for(int i = 0; i < nThreads; i++) {
                    try {
                        threads[i].join();
                        total.accumulate(mtqs[i].nw);
                        qs.accumulate(mtqs[i].lus.getQueryStats());
                    } catch(InterruptedException ie) {
                    }
                }
            }
            logger.info(String.format(
                    "threads: %d lookups: %d %d avg lookup: %.4fms",
                    nThreads,
                    total.getClicks(),
                    qs.dictLookups,
                    total.getAvgTimeMillis()));

//            logger.info(engine.getQueryStats().dump());
        } finally {
            engine.close();
        }
    }
}
