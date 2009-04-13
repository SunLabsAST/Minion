/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.samples;

import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.NanoWatch;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A multi-threaded query class.
 */
public class MTQuery implements Runnable {

    protected SearchEngine engine;

    protected List<String> queries;

    protected int duration;

    protected int index;

    protected long stop;

    protected Logger logger;

    protected NanoWatch nw;

    public MTQuery(List<String> queries, SearchEngine engine, int duration,
            int index) {
        this.queries = new ArrayList<String>(queries);
        Collections.shuffle(this.queries);
        this.engine = engine;
        this.duration = duration;
        this.index = index;
        stop = System.currentTimeMillis() + duration * 1000;
        logger = Logger.getLogger(getClass().getName());
        nw = new NanoWatch();
    }

    public void run() {
        int iter = 0;
        while(System.currentTimeMillis() < stop) {
            int qn = 1;
            for(String q : queries) {
                try {
                    nw.start();
                    ResultSet rs = engine.search(q);
                    nw.stop();
                    if(logger.isLoggable(Level.FINER)) {
                        logger.finer(String.format(
                                "thread: %d %d/%d/%d query: %s %.2fms %d results",
                                index,
                                iter, qn, queries.size(),
                                q, nw.getLastTimeMillis(), rs.size()));
                    }
                } catch(SearchEngineException see) {
                    logger.severe("Exception during search: " + see);
                }
                if(qn % 50 == 0) {
                    logger.fine(String.format(
                            "thread: %d queries: %d average query time: %.2fms",
                            index, nw.getClicks(), nw.getAvgTimeMillis()));
                    if(System.currentTimeMillis() >= stop) {
                        break;
                    }
                }
                qn++;
            }
            logger.fine(String.format(
                    "thread: %d iter: %d average query time: %.2fms", index,
                    iter, nw.getAvgTimeMillis()));
            iter++;
        }
    }

    public static void usage() {
        System.err.println(
                String.format(
                "Usage: MTQuery -d <index dir> -q <query file> [-n <num threads>] [-t <dur in seconds>]"));
        System.err.println(String.format(
                " num threads defaults to 4, duration to 300 seconds"));
    }

    /**
     * @param args the command line arguments
     */
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
        String flags = "d:q:n:t:l:";
        Getopt gopt = new Getopt(args, flags);

        int nThreads = 4;
        int duration = 300;
        String indexDir = null;
        String queriesFile = null;
        int c;

        //
        // Handle the options.
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'q':
                    queriesFile = gopt.optArg;
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
            }
        }

        if(indexDir == null || queriesFile == null) {
            usage();
            return;
        }

        List<String> queries = new ArrayList<String>();
        BufferedReader in = new BufferedReader(new FileReader(queriesFile));
        String q;
        while((q = in.readLine()) != null) {
            queries.add(q);
        }
        in.close();

        System.out.println(String.format("Running %d queries in %d threads",
                queries.size(), nThreads));

        SearchEngine engine = SearchEngineFactory.getSearchEngine(indexDir);

        ResultSet s = engine.search("the");
        s.getResults(0, 10);

        try {
            NanoWatch total;
            if(nThreads == 1) {
                MTQuery single = new MTQuery(queries, engine, duration, 0);
                single.run();
                total = single.nw;
            } else {
                MTQuery[] mtqs = new MTQuery[nThreads];
                Thread[] threads = new Thread[nThreads];
                for(int i = 0; i < nThreads; i++) {
                    mtqs[i] = new MTQuery(queries, engine, duration, i);
                    threads[i] = new Thread(mtqs[i]);
                    threads[i].setName("mtquery-" + i);
                    threads[i].start();
                }

                total = new NanoWatch();
                for(int i = 0; i < nThreads; i++) {
                    try {
                        threads[i].join();
                        total.accumulate(mtqs[i].nw);
                    } catch(InterruptedException ie) {
                    }
                }
            }
            logger.info(String.format(
                    "threads: %d queries: %d average query time: %.2fms",
                    nThreads, total.getClicks(),
                    total.getAvgTimeMillis()));
            logger.info(engine.getQueryStats().dump());
        } finally {
            engine.close();
        }
    }
}
