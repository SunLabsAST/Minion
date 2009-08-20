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
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.NanoWatch;
import com.sun.labs.util.LabsLogFormatter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs find similars against a set of pre-defined keys.
 *
 * @author stgreen
 */
public class SimpleFindSimilar {

    private SearchEngine engine;

    private List<String> keys;

    private String field;

    private static Logger logger = Logger.getLogger(SimpleFindSimilar.class.getName());

    public SimpleFindSimilar(SearchEngine engine, List<String> keys,
            String field, int reps) {
        this.engine = engine;
        this.keys = new ArrayList<String>(keys);
        this.field = field;
        Collections.shuffle(keys);
    }

    public void run() {
    }

    public static void usage() {
        System.out.println(String.format("SimpleFindSimilar -d <dir> -f <field> -i <keys file>"));
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        if(args.length == 0) {
            usage();
            return;
        }

        String flags = "d:f:i:n:";
        Getopt gopt = new Getopt(args, flags);
        String indexDir = null;
        String field = null;
        String input = null;
        int c;
        int n = 0;


        Logger rl = Logger.getLogger("");
        for(Handler h : rl.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
            try {
                h.setEncoding("utf-8");
            } catch(Exception ex) {
                rl.severe("Error setting output encoding");
            }
        }

        //
        // Handle the options.
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'f':
                    field = gopt.optArg;
                    break;

                case 'i':
                    input = gopt.optArg;
                    break;
                case 'n':
                    n = Integer.parseInt(gopt.optArg);
                    break;
            }
        }

        if(indexDir == null) {
            System.err.println(String.format(
                    "You must specify an index directory"));
            usage();
            return;
        }

        if(input == null) {
            System.err.println(String.format("You must specify a file of keys"));
            usage();
            return;
        }

        BufferedReader r = new BufferedReader(new FileReader(input));
        List<String> keys = new ArrayList<String>();
        String k;
        while((k = r.readLine()) != null) {
            keys.add(k);
        }
        r.close();


        //
        // Open our engine for use. 
        SearchEngine engine;
        try {
            engine = SearchEngineFactory.getSearchEngine(indexDir);
        } catch(SearchEngineException se) {
            System.err.println("Error opening collection: " + se);
            return;
        }
        
        NanoWatch nw = new NanoWatch();
        NanoWatch vw = new NanoWatch();
        for(String key : keys) {
            nw.start();
            vw.start();
            DocumentVector dv = engine.getDocumentVector(key, field);
            vw.stop();
            if(dv == null) {
                System.out.println(String.format("null vector for: " + key));
                continue;
            }
            ResultSet rs = dv.findSimilar();
            nw.stop();
            if(n > 0) {
                System.out.println(String.format("%s", key));
                System.out.println(String.format(" %d %.3fms", rs.size(), nw.getLastTimeMillis()));
                for(Result result : rs.getResults(0, n)) {
                    System.out.printf(" %.3f %s\n", result.getScore(), result.getKey());
                }
            }
            if(nw.getClicks() % 50 == 0) {
                System.out.println(String.format(
                        "%d fs computed, avg gdv: %.3fms avg fs: %.3fms",
                        nw.getClicks(),
                        vw.getAvgTimeMillis(),
                        nw.
                        getAvgTimeMillis()));
            }
            
        }
        System.out.println(String.format(
                "%d fs computed, avg gdv: %.3fms avg fs: %.3fms",
                nw.getClicks(),
                vw.getAvgTimeMillis(),
                nw.getAvgTimeMillis()));
        System.out.println(String.format("Stats:\n%s\n", engine.getQueryStats().dump()));

        engine.close();
    }
}
