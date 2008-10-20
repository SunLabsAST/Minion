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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Date;
import java.util.concurrent.LinkedBlockingQueue;

import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.StopWatch;

import com.sun.labs.minion.Log;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.Searcher;

/**
 * Class QueryRunner runs queries against a search engine.<br>
 * Arguments to the <code>main()</code> method of QueryRunner specify how many instances of
 * QueryRunner to create, the location of the file containing a search log, the size of the queue
 * to be used by each runner, and the location of the index directory. See {@link #usage()}.<br>
 * 
 * 
 * @author Bernard Horan
 *
 */
public class QueryRunner implements Runnable {

   /**
    * The queue to contain the queries for this runner
    */
   private LinkedBlockingQueue<String> queryQueue;

   /**
    * The thread in which a QueryRunner runs
    */
   private Thread queryThread;
   
   /**
    * An identifier for the queryrunner (for debugging purposes only)
    */
   private int id;
    
    /**
     * To keep track of how long the queryrunner takes to run all its queries
     * (for reporting)
     */
    private long totalTime;
    
    /**
     * A counter for the number of queries (for reporting)
     */
    private int n;
    
    /**
     * Where to write log messages
     */
    private Writer logger;
    
    /**
     * The file on which to write log messages (logger is a writer on this file)
     */
    private File logFile;
    
    /**
     * Constant to indicate that the query runner should stop.
     * When a queryrunner encounters this as a query, it forces itself to stop
     */
    private static final String STOP = "!!STOP!!";
    
    /**
     * Default size for a queryrunner's queue. May be overridden with 
     * command line argument <code>-q &lt;queue size&gt;</code>.
     */
    private static final int DEFAULT_QUEUE_SIZE = 100;
    
    /**
     * Default number of threads to create (and hence number of runners
     * to create).<br>
     * May be overridden with <code>-t &lt;thread count&gt;</code>.
     */
    private static final int DEFAULT_THREAD_COUNT = 1;
    
    /**
     * The search engine used by all query runners
     */
    private static SearchEngine ENGINE;
    
    /**
     * Tag for the log
     */
    private static final String logTag = "QueryRunner";
    
    /**
     * Log for use by the <code>main()</code> method.
     */
    protected static Log log;

    /** 
     * True if file input is just the query on each line,
     * false if the file is from the server log and has a date, etc
     */
    protected static boolean simpleFormat = false;

   /**
    * @param args
    */
    public static void main(String[] args) {
        if (args.length == 0) {
            usage();
            return;
        }
        // get args
        String flags = "d:f:q:t:x:ls";
        Getopt gopt = new Getopt(args, flags);
        int threadCount = DEFAULT_THREAD_COUNT;
        String queriesFile = null;
        String configFile = null;
        String indexDir = null;
        boolean lowerCase = false;
        int c;
        int queue_size = DEFAULT_QUEUE_SIZE;
        Thread.currentThread().setName("QueryRunner-main");
        
        //
        // Handle the options.
        while ((c = gopt.getopt()) != -1) {
            switch (c) {
                
                case 'd':
                    indexDir = gopt.optArg;
                    break;
                    
                case 'f':
                    queriesFile = gopt.optArg;
                    break;
                    
                case 'q':
                    queue_size = Integer.parseInt(gopt.optArg);
                    break;
                    
                case 't':
                    threadCount = Integer.parseInt(gopt.optArg);
                    break;
                    
                case 'x':
                    configFile = gopt.optArg;
                    break;

                case 'l':
                    lowerCase = true;
                    break;

                case 's':
                    // Log files are simple -- just queries, not server log
                    simpleFormat = true;
                    break;
            }
        }
        
        //
        // Setup logging.
        log = Log.getLog();
        log.setStream(System.out);
        log.setLevel(3);
        
        
        //
        //Create the ENGINE
        try {
            ENGINE = SearchEngineFactory.getSearchEngine(configFile, indexDir);
        } catch (SearchEngineException e) {
            e.printStackTrace();
            return;
        }
        StopWatch wall = new StopWatch();
        wall.start();
        //
        // Create the query runners, and set them running
        QueryRunner[] runners = new QueryRunner[threadCount];
        for (int i = 0; i < runners.length; i++) {
            runners[i] = new QueryRunner(i, queue_size);
         
        }

        //
        // Open the file to read the queries
        try {
            BufferedReader inputFile = new BufferedReader(new InputStreamReader(
                                                                                new FileInputStream(queriesFile), "UTF-8"));

            //
            // Read the queries and send them off the the runners' queues
            int runIndex = 0;
            String query = null;
            int nq = 0;
            while (true) {
                if (query == null) {
                    query = inputFile.readLine();
                    //log.log(logTag, MinionLog.LOG, "line: " + query);
                }
                
                
                if (query == null || query.length() == -1) {
                    break;
                }
                
                if (lowerCase) {
                    query = query.toLowerCase();
                }

                try {
                    if (runners[runIndex++].queueQuery(query)) {
                        query = null;
                        nq++;
                        if(nq % 500 == 0) {
                            log.log(logTag, 3, "Enqueued " + nq + " queries");
                        }
                    }
                    runIndex = runIndex % threadCount;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            
            }
            inputFile.close();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //
        //Stop the runners
        log.log(logTag, MinionLog.LOG, "Main: stopping runners");
        for (int i = 0; i < runners.length; i++) {
            try {
                runners[i].stop();
            } catch (InterruptedException e) {
                e.printStackTrace();
                
            }
         
        }
        //
        //Wait for them to stop
        float avgTime = 0;
        int nQs = 0;
        log.log(logTag, MinionLog.LOG, "Main: waiting for runners to stop");
        for (int i = 0; i < runners.length; i++) {
            try {
                runners[i].waitToComplete();
                avgTime += runners[i].getTime();
                nQs += runners[i].getNQueries();
            } catch (InterruptedException e) {
                e.printStackTrace();
                
            }
            
        }
        wall.stop();
        avgTime /= (double)nQs;
        //
        //Close the ENGINE
        log.log(logTag, MinionLog.LOG, "Main: closing the engine");
        log.log(logTag, MinionLog.LOG,
                String.format("avg speed %.1f ms/q, total: %d ms",
                              avgTime, wall.getTime()));
        try {
            ENGINE.close();
        } catch (SearchEngineException e) {
            e.printStackTrace();
        }
        log.log(logTag, MinionLog.LOG, "Main: finished");
    }
   
    private void stop() throws InterruptedException {
        log.log(logTag, MinionLog.LOG, "Stopping " + this);
        message("stopping");
        queryQueue.put(STOP);
    }

    /**
     * Wait for the runner's thread to die
    * @throws InterruptedException
    */
   private void waitToComplete() throws InterruptedException {
        if (queryThread != null) {
            queryThread.join();
        }
        
    }

    /**
     * Help!
     */
    private static void usage() {
        System.out
        .println("Usage: java com.sun.labs.minion.test.QueryRunner -d <index_directory> " +
                "-f <query log> " +
                "-q <queue size> -t <thread count> " +
                "[-x <config_file>]");
        
    }

    /**
     * Log a message from an identifier
     * @param identifier the object to which this message is related
     * @param message  the message to log
     */
    private void message(String identifier, String message) {
        try {
            logger.write("[" + new Date() + "] " + identifier + ": " + message);
            logger.write('\n');
            logger.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
    }

    /**
     * Create a new QueryRunner. 
     * @param id an identifier for the runner (to aid debugging)
     * @param queue_size the maximum size of the queue for this runner
     */
    QueryRunner(int id, int queue_size) {
        this.id = id;
        
        //
        // Setup logging.
        try {
            logger = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(getLogFile()), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        
        queryQueue = new LinkedBlockingQueue<String>(queue_size);
      queryThread = new Thread(this);
      queryThread.setName(getClass().getSimpleName() + id);
      queryThread.start();
   }

   /**
     * Gets the log file
    * @return the File to be used as a log for this query runner
    * @throws IOException
    */
   private File getLogFile() throws IOException {
        if (logFile != null) {
            return logFile;
        }
        logFile = new File("/tmp/" + toString());
        if (!logFile.createNewFile()) {
            throw new IOException("Failed to create log file");
        }
        return logFile;
    }

    /**
     * Put a Query on the queue
    * @param query a String query to be evaluated against the search engine
    * @return true if the query was added to the queue, false otherwise
    * @throws InterruptedException
    */
   private boolean queueQuery(String query) throws InterruptedException {
       
      boolean success = queryQueue.offer(query);
        if (success) {
            message("Queuing " + query);
        }
        return success;
   }

    public long getTime() {
        return totalTime;
    }

    public long getNQueries() {
        return n;
    }
    
   /**
     * While the runner's thread isn't interrupted, get the next query from the
     * queue, parse it and evaluate it against the search engine.
    * @see java.lang.Runnable#run()
    */
   public void run() {
        String query = null;
        try {
            while (!queryThread.isInterrupted()) {
                try {
                    query = getNextRawQuery();
//                    message("rawQuery: " + query);
                    if (query.equals(STOP)) {
//                        message(query);
                        break;
                    }
                    query = parseQuery(query);
//                    message(query);
                    runQuery(query);
                } catch (InterruptedException e) {
                    message("interrupted", e.getLocalizedMessage());
                    e.printStackTrace();
                    break;
                }
            }
        } finally {
            message("Trying to close down");
            message(String.format("Ran %d queries in %d ms %.1f ms/query", n, totalTime, totalTime / (double) n));
            queryThread.interrupt();
            try {
                logger.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            synchronized (this) {
                queryThread = null;
                
            }
        }

    }

   /**
     * Evaluate the query against the engine
    * @param query the query string
    */
   private void runQuery(String query) {
        try {
            ResultSet r = ENGINE.search(query, "-score", Searcher.OP_AND,
                                        Searcher.GRAMMAR_STRICT);
            message(query + " took " + r.getQueryTime() + "ms and returned " + r.size() + " documents");
            totalTime += r.getQueryTime();
            n++;
        } catch (SearchEngineException se) {
            Throwable cause = se.getCause();
            if (cause == null) {
                message("Error running search " + se);
                System.err.println(this + " search error for query: " + query);
                se.printStackTrace(System.err);
            } else {
                message("Error running search " + cause.getClass());
                System.err.println(this + " search error for query: " + query);
                System.err.println("cause: " + cause.getClass());
            }
        }
        
    }

    /**
     * Logging message
     * @param message the message to be logged 
     */
    private void message(String message) {
        message(this.toString(), message);
        
    }

    /**
     * Gets the next query from the queue, blocks till there is one.
     * @return a query string from the query log
     * @throws InterruptedException
     */
    private String getNextRawQuery() throws InterruptedException {
        if (queryQueue.peek() == null) {
            message("waiting for query queue");
        }
      return queryQueue.take();
   }
    
    /**
     * Returns a query that can be evaluated against the search engine.
     * <EM>NOTE:</EM> This method has a hardcoded parsing mechanism for use 
     * with the mailfinder search log.
     * @param rawQuery a line from the search log
     * @return a substring of rawQuery that represents a search engine query
     */
    private String parseQuery(String rawQuery) {
        int index = 0;
        if (!simpleFormat) {
            for (int i = 1; i <=6; i++) {
                index = rawQuery.indexOf(' ', index + 1);
            }
            return rawQuery.substring(index + 1);
        } else {
            return rawQuery;
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "MQR-" + id ;
    }
   
}
