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

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.Log;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs find similars in multiple threads.
 *
 * @author stgreen
 */
public class FindSimilar implements Runnable {

    private SearchEngine engine;

    private List<String> keys;

    private int reps;

    private String field;

    private Logger logger;

    public FindSimilar(SearchEngine engine, List<String> keys, String field, int reps) {
        this.engine = engine;
        this.keys = new ArrayList<String>(keys);
        this.field = field;
        this.reps = reps;
        Collections.shuffle(keys);
        logger = Logger.getLogger(getClass().getName());
    }

    public void run() {
        for(int i = 0; i < reps; i++) {
            for (String key : keys) {
                DocumentVector dv = engine.getDocumentVector(key, field);
                ResultSet rs = dv.findSimilar();
                try {
                    for (Result r : rs.getResults(0, 10)) {
                        r.getKey();
                    }
                    logger.fine("Worked: " + key);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed: " + key, e);
                }
            }
            logger.info(Thread.currentThread().getName() + " finished rep " + i);
        }

    }

    public static void usage() {
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }

        String logTag = "FS";
        String flags = "d:f:n:q:r:";
        Getopt gopt = new Getopt(args, flags);
        String indexDir = null;
        int n = 4;
        int reps = 10;
        String query = "aura-type = artist <and> socialtags <contains> canadian";
        String field = "socialtags";
        int c;


        //
        // Set up the logging for the search engine.  We'll send everything
        // to the standard output, except for errors, which will go to
        // standard error.  We'll set the level at 3, which is pretty
        // verbose.
        //
        // Use the labs format logging.
        Logger rl = Logger.getLogger("");
        for (Handler h : rl.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new SimpleLabsLogFormatter());
            try {
                h.setEncoding("utf-8");
            } catch (Exception ex) {
                rl.severe("Error setting output encoding");
            }
        }

        Log.setLogger(rl);
        Log.setLevel(3);

        //
        // Handle the options.
        while ((c = gopt.getopt()) != -1) {
            switch (c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'f':
                    field = gopt.optArg;
                    break;

                case 'n':
                    n = Integer.parseInt(gopt.optArg);
                    break;

                case 'r':
                    reps = Integer.parseInt(gopt.optArg);
                    break;

                case '1':
                    query = gopt.optArg;
                    break;
            }
        }

        if (indexDir == null) {
            System.err.println(String.format("You must specify an index directory"));
            usage();
            return;
        }

        //
        // Open our engine for use. 
        SearchEngine engine;
        try {
            engine = SearchEngineFactory.getSearchEngine(indexDir);
        } catch (SearchEngineException se) {
            System.err.println("Error opening collection: " + se);
            return;
        }

        FindSimilar[] fs = new FindSimilar[n];
        Thread[] threads = new Thread[n];
        List<String> keys = new ArrayList<String>();

        for(Result r : engine.search(query).getResults(0, 100)) {
            keys.add(r.getKey());
        }

        System.out.println(String.format("Found %d keys", keys.size()));

        for(int i = 0; i < n; i++) {
            fs[i] = new FindSimilar(engine, keys, field, reps);
            threads[i] = new Thread(fs[i]);
            threads[i].setName("FS-" + i);
            threads[i].start();
        }

        for(int i = 0; i < n; i++) {
            threads[i].join();
        }

        engine.close();
    }

}
