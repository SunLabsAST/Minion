package com.sun.labs.minion.test;

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.util.NanoWatch;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A mutating, find similar running thing.
 */
public class MFS {

    private static Logger logger = Logger.getLogger(MFS.class.getName());

    public static void usage() {
        System.out.println(
                "Usage: java Mutator\n" +
                " -i <file list>   " +
                "Directory or file list to index (Required)\n" +
                " -d <index dir>   " +
                "The index directory\n" +
                " -r <num>         " +
                "The number of times to update, < 0 means run continuously (default: 10)\n" +
                " -s <num>         " +
                "Maximum number of seconds between update runs\n" +
                " -t <num>         " +
                "The number of threads to run\n" +
                " -n <num>         " +
                "The number of lines to use from the input file (default: all)\n" +
                " -m <num>          " +
                "The maximum number of files to index/update\n" +
                " -f <field name>  " +
                "The name of the field to run fs against\n" +
                " -q <query>       " +
                "The query to run" +
                "-e <num>          " +
                "The number of find similar threads to run" +
                " -x <file>        " +
                "An XML configuration file specifying extra props for " +
                "the index");
        return;
    }

    public static void main(String[] args) throws java.io.IOException,
            SearchEngineException {

        String flags = "i:d:s:n:m:r:t:x:f:e:q:";
        String stuff = null;
        String indexDir = null;
        int updateInterval = 30000;
        int maxUpd = 100;
        int nThreads = 2;
        int fsThreads = 20;
        String query = "java";
        String field = null;
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

                case 'e':
                    fsThreads = Integer.parseInt(gopt.optArg);
                    break;

                case 'f':
                    field = gopt.optArg;
                    break;

                case 'q':
                    query = gopt.optArg;
                    break;
            }

        }

        if(stuff == null) {
            logger.warning("You must specify a list of files to index.");
            usage();
            return;
        }

        if(query == null) {
            logger.warning("You must specify a query to run");
            usage();
            return;
        }

        if(cmFile == null) {
            cmFile = Mutator.class.getResource("MFSConfig.xml");
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
        while((l = inFile.readLine()) != null) {
            il.add(l);
        }

        Thread[] indexThreads = new Thread[nThreads];
        Indexer[] indexers = new Indexer[nThreads];

        SearchEngine engine = SearchEngineFactory.getSearchEngine(indexDir,
                                                                  "mfs_search_engine", cmFile);

        try {

            Timer dt = new Timer("dump-timer");
            TimerTask dumpTask = new Dumper(engine);
            dt.scheduleAtFixedRate(dumpTask, 60000, 60000);

            for(int i = 0; i < nThreads; i++) {
                indexers[i] = new Indexer(il, maxUpd, updateInterval, engine);
                indexThreads[i] = new Thread(indexers[i]);
                indexThreads[i].setName("INDEXER-" + i);
                indexThreads[i].start();
            }

            while(engine.getNDocs() < 1000) {
                try {
                    Thread.sleep(1000);
                } catch(InterruptedException ex) {
                }
            }

            Thread[] fst = new Thread[fsThreads];
            FindSimilar[] fs = new FindSimilar[fsThreads];

            for(int i = 0; i < fsThreads; i++) {
                fs[i] = new FindSimilar(engine, query, field, 200);
                fst[i] = new Thread(fs[i]);
                fst[i].setName("FS-" + i);
                fst[i].start();
            }


            for(int i = 0; i < indexThreads.length; i++) {
                try {
                    indexThreads[i].join();
                } catch(InterruptedException ie) {
                }
            }

            for(int i = 0; i < fst.length; i++) {
                try {
                    fst[i].join();
                } catch(InterruptedException ex) {
                }
            }
        } finally {

            //
            // Close the search engines.
            try {
                engine.close();
            } catch(SearchEngineException se) {
                logger.log(Level.SEVERE, "Error closing engines", se);
            }
        }
    }

    private static class Indexer implements Runnable {

        List<String> stuff;

        int max;

        int delay;

        boolean finished;

        SearchEngine engine;

        public Indexer(List<String> stuff, int max, int delay,
                       SearchEngine engine) {
            this.stuff = new ArrayList<String>(stuff);
            Collections.shuffle(this.stuff);
            this.max = max;
            this.delay = delay;
            this.engine = engine;
        }

        private void index(String f) {
            try {
                engine.index(SEMain.makeDocument(new IndexableFile(f)));
            } catch(SearchEngineException ex) {
                logger.log(Level.SEVERE, "Error indexing document " + f,
                           ex);
            } catch(Exception ex) {
                logger.log(Level.SEVERE, "Error indexing", ex);
            }
        }

        @Override
        public void run() {
            Random rand = new Random();
            int start = 0;
            while(start < stuff.size()) {
                int end = start + rand.nextInt(max);
                for(int i = start; i < end && i < stuff.size(); i++) {
                    index(stuff.get(i));
                }
                logger.info(String.format("%s indexed %d", Thread.currentThread().getName(), end - start));
                try {
                    Thread.sleep(rand.nextInt(delay));
                } catch(InterruptedException ex) {
                    return;
                }
                start = end;
            }
        }
    }

    private static class FindSimilar implements Runnable {

        private SearchEngine engine;

        private String query;

        private String field;

        private int reps;

        private List<String> keys;

        public FindSimilar(SearchEngine engine, String query,
                           String field, int reps) {
            this.engine = engine;
            this.query = query;
            this.field = field;
            this.reps = reps;
            keys = new ArrayList<String>();
        }

        public void run() {
            for(int i = 0; i < reps; i++) {

                keys.clear();
                try {
                    for(Result r : engine.search(query).getAllResults(false)) {
                        keys.add(r.getKey());
                    }
                } catch(SearchEngineException ex) {
                    logger.log(Level.SEVERE, "Error running search", ex);
                    continue;
                }
                Collections.shuffle(keys);
                logger.info(String.format("%s got %d keys", Thread.currentThread().getName(), keys.size()));
                NanoWatch nw = new NanoWatch();
                for(String key : keys) {
                    DocumentVector dv = engine.getDocumentVector(key, field);
                    if(dv == null) {
                        continue;
                    }
                    try {
                        nw.start();
                        ResultSet rs = dv.findSimilar();
                        nw.stop();

                        for(Result r : rs.getResults(0, 10)) {
                            r.getKey();
                        }
                    } catch(Exception e) {
                        logger.log(Level.SEVERE, "Failed: " + key, e);
                    }
                    if(nw.getClicks() % 20 == 0) {
                        logger.info(String.format(
                                "%s %d fs avg: %.3f",
                                Thread.currentThread().getName(),
                                nw.getClicks(),
                                nw.getAvgTimeMillis()));
                    }
                }
                logger.info(String.format(
                        "%s rep %d average time for %d fses %.3f",
                        Thread.currentThread().getName(),
                        (i + 1),
                        nw.getClicks(), nw.getAvgTimeMillis()));
            }

        }
    }

    protected static class Dumper extends TimerTask {

        private SearchEngine engine;
        public Dumper(SearchEngine engine) {
            this.engine = engine;
        }

        @Override
        public void run() {
            try {
            engine.flush();
            } catch (SearchEngineException ex) {
                logger.log(Level.SEVERE, "Error dumping data", ex);
            }
        }
    }
}
