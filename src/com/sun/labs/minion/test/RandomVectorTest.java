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

import com.sun.labs.minion.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.pipeline.SyncPipelineImpl;
import com.sun.labs.minion.retrieval.ResultImpl;

import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.StopWatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class RandomVectorTest {

    protected static DecimalFormat form = new DecimalFormat("###0.0000");

    /** Creates a new instance of VectorTest */
    public RandomVectorTest() {
    }

    public static int[] getPairs(int n) {
        Random rand = new Random();
        int[] pairs = new int[n * 2];
        String[] keys = new String[pairs.length];
        for(int i = 0; i < pairs.length; i++) {
            pairs[i] = rand.nextInt(n) + 1;
        }
        return pairs;
    }

    public static void runKeyPairs(int n, PartitionManager manager) {
        StopWatch sw = new StopWatch();
        sw.start();
        int[] pairs = getPairs(n);
        for(int i = 0; i < n; i += 2) {
            double x = manager.getDistance("document-" + pairs[i],
                    "document-" + pairs[i + 1],
                    "features");
        }
        sw.stop();
        System.out.println(form.format(sw.getTime() * 1000.0 / n) +
                "us per paired distance calc");
    }

    public static void runResultPairs(int n, PartitionManager manager) throws SearchEngineException {
        StopWatch sw = new StopWatch();
        int[] pairs = getPairs(n);

        //
        // Get a result set containing all documents.
        StopWatch simw = new StopWatch();
        simw.start();
        ResultSet rs = manager.getSimilar("document-1", "features");
        List l = rs.getAllResults(false);
        simw.stop();

        System.out.println("Sim took: " + simw.getTime());

        //
        // Random pair distance computation.
        sw.start();
        for(int i = 0; i < n; i += 2) {
            double x = ((Result) l.get(pairs[i] - 1)).getDistance(
                    (Result) l.get(pairs[i + 1] - 1), "features");
        }
        sw.stop();
        System.out.println(form.format(sw.getTime() * 1000.0 / n) +
                "us per paired distance calc");
    }

    public static void runResultLists(int n, PartitionManager manager) throws SearchEngineException {
        int[] lists = new int[(n + 1) * 11];
        Random rand = new Random();
        for(int i = 0; i < lists.length; i++) {
            lists[i] = rand.nextInt(n);
        }

        //
        // Get a result set containing all documents.
        StopWatch simw = new StopWatch();
        simw.start();
        ResultSet rs = manager.getSimilar("document-1", "features");
        List l = rs.getAllResults(false);
        simw.stop();

        System.out.println("Sim took: " + simw.getTime());

        //
        // Random pair distance computation.
        StopWatch sw = new StopWatch();
        sw.start();
        List s = new ArrayList();
        int calcs = 0;
        for(int i = 0; i < lists.length; i += 11) {
            s.clear();
            for(int j = 0; j < 10; j++) {
                s.add(l.get(lists[i + 1 + j]));
            }
            double[] x = ((ResultImpl) l.get(lists[i])).getDistance(s,
                    "features");
            calcs += s.size();
        }
        sw.stop();
        System.out.println(form.format(sw.getTime() * 1000.0 / calcs) +
                "us per paired distance calc");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {

        //
        // Set up the logging for the search engine.  We'll send everything
        // to the standard output, except for errors, which will go to
        // standard error.  We'll set the level at 3, which is pretty
        // verbose.
        Logger logger = Logger.getLogger(RandomVectorTest.class.getName());

        //
        // Handle the options.
        int c;
        int vecLen = 10;
        int nDocs = 100;
        boolean doIndex = false;
        boolean doTest = false;
        boolean doSort = false;
        boolean doPairs = false;
        boolean doKeys = false;
        String indexDir = null;
        String cmFile = null;

        Getopt gopt = new Getopt(args, "d:f:kin:pstx:");

        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;
                case 'f':
                    vecLen = Integer.parseInt(gopt.optArg);
                    break;
                case 'i':
                    doIndex = true;
                    break;
                case 'k':
                    doKeys = true;
                    break;
                case 'n':
                    nDocs = Integer.parseInt(gopt.optArg);
                    break;
                case 'p':
                    doPairs = true;
                    break;
                case 's':
                    doSort = true;
                    break;
                case 't':
                    doTest = true;
                    break;
                case 'x':
                    cmFile = gopt.optArg;
                    break;
            }
        }

        //
        // Open our engine for use.  We give it the properties that we read
        // and no query properties.
        SearchEngineImpl engine;
        try {
            engine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(
                    cmFile, indexDir);
        } catch(SearchEngineException se) {
            logger.log(Level.SEVERE, "Error opening collection", se);
            return;
        }

        if(doIndex) {
            //
            // Index nDocs random documents with feature vectors.
            SimpleIndexer si = engine.getSimpleIndexer();
            Random rand = new Random();
            for(int i = 1; i <= nDocs; i++) {
                si.startDocument("document-" + i);
                si.addField("dn", new Integer(i));
                si.addField("title", "This is document number " + i);

                //
                // Generate a random feature vector.
                double[] v = new double[vecLen];
                for(int j = 0; j < v.length; j++) {
                    v[j] = rand.nextDouble();
                }
                ((SyncPipelineImpl) si).addFieldInternal("features", v);
                si.endDocument();

                if(i % 1000 == 0) {
                    System.out.println("Indexed " + i + " docs");
                }
            }
            si.finish();
        }

        //
        // The number of documents indexed in the engine.
        int n = engine.getNDocs();
        int p = n / 1000;

        if(doPairs) {
            if(doKeys) {
                runKeyPairs(n, engine.getManager());
                runKeyPairs(n, engine.getManager());
            } else {
                runResultPairs(n, engine.getManager());
                runResultPairs(n, engine.getManager());
            }
        }

        if(doTest) {

            StopWatch sw = new StopWatch();
            sw.start();
            int x = 0;
            for(Iterator i =
                    engine.getManager().getActivePartitions().iterator(); i.
                    hasNext();) {
                DiskPartition dp = (DiskPartition) i.next();
                for(Iterator d = dp.getDocumentIterator(); d.hasNext();) {
                    DocKeyEntry dke = (DocKeyEntry) d.next();

                    //
                    // Get the feature vector for this document.
                    double[] feat = (double[]) engine.getFieldValue("features",
                            dke.getName().toString());
                    int dn = (int) ((Long) engine.getFieldValue("dn", dke.
                            getName().toString())).longValue();

                    //
                    // We'll check for null, no matter what.
                    if(feat == null) {
                        System.out.println("null features for: " + dke.getName() +
                                " " +
                                dn);
                        continue;
                    }

                    //
                    // Run the similarity computation for this document.
                    ResultSet rs = engine.getSimilar((String) dke.getName(),
                            "features");
                    List l = rs.getAllResults(doSort);
                    x++;
                    if(x % p == 0) {
                        System.out.println(x + "/" + n);
                    }
                }
            }

            sw.stop();
            System.out.println("Calc took: " + sw.getTime());
            System.out.println(form.format((double) sw.getTime() / x) +
                    "ms per similarity calc");
            System.out.println(form.format((n * n) / (double) sw.getTime()) +
                    " calcs per ms");
        }

        engine.close();
    }
}
