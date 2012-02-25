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

import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to test deletions in partitions and how the deletions affect merges.
 */
public class DelMergeTest {

    public static void deleter(DiskPartition dp, List<Integer> docs) {
        for(Integer doc : docs) {
            dp.deleteDocument(doc);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SearchEngineException, FileNotFoundException, IOException {
        String flags = "ad:p:r:x:";
        String indexDir = null;
        String ddmFile = null;
        boolean allParts = false;
        double proportion = 0.05;
        URL cmFile = null;
        Getopt gopt = new Getopt(args, flags);
        int c;
        if(args.length == 0) {
            usage();
            return;
        }

        List<Integer> partNums = new ArrayList<Integer>();

        //
        // Set up the log.
        Logger logger = Logger.getLogger("");
        for(Handler h : logger.getHandlers()) {
            h.setFormatter(new SimpleLabsLogFormatter());
        }

        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'a':
                    allParts = true;
                    break;

                case 'd':
                    indexDir = gopt.optArg;
                    break;
                case 'p':
                    try {
                        partNums.add(new Integer(gopt.optArg));
                    } catch(NumberFormatException nfe) {
                        logger.severe(String.format("Bad partition number %s",
                                                    gopt.optArg));
                        return;
                    }
                    break;
                    
                case 'r':
                    try {
                        proportion = Double.parseDouble(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        logger.severe(String.format("Bad proportion %s",
                                                    gopt.optArg));
                        return;
                    }
                    break;

                case 'x':
                    cmFile = (new File(gopt.optArg)).toURI().toURL();
                    break;
            }
        }

        if(indexDir == null || ddmFile == null) {
            usage();
            return;
        }

        //
        // Get our engine.
        SearchEngineImpl engine = (SearchEngineImpl) SearchEngineFactory.
                getSearchEngine(indexDir, cmFile);

        //
        // Get the partitions.
        PartitionManager pm = engine.getPM();

        //
        // The partitions.
        List<DiskPartition> allPartList = pm.getActivePartitions();
        List<DiskPartition> parts;
        if(allParts) {
            parts = allPartList;
        } else {
            parts = new ArrayList<DiskPartition>();
            for(DiskPartition part : allPartList) {
                int pn = part.getPartitionNumber();
                if(partNums.contains(pn)) {
                    parts.add(part);
                }
            }
        }
        logger.info("Partitions: " + parts);

        //
        // Delete some docs.
        Deletion[] deletions = new Deletion[parts.size()];
        for(int i = 0; i < deletions.length; i++) {
            deletions[i] = new Deletion(parts.get(i), proportion);
        }

        //
        // Merge them.
        PartitionManager.Merger merger = null;
        merger = pm.getMerger(parts);
        if(merger == null) {
            logger.severe("Could not get merger for " + parts);
            return;
        }

        DiskPartition mp = merger.merge();

        engine.close();
    }

    private static void usage() {
        System.err.println(
                "Usage: DelDuringMerge -d <indexDir> -f <del-during-merge file>");
    }
    
    private static class Deletion {
        
        DiskPartition part;
        
        int[] dels;
        
        /**
         * Do random deletions.
         */
        public Deletion(DiskPartition part, double proportion) {
            this.part = part;
            Random rand = new Random();
            int n = rand.nextInt((int) (proportion * part.getMaxDocumentID())) + 1;
            dels = new int[n];
            for(int i = 0; i < n; i++) {
                dels[i] = rand.nextInt(part.getMaxDocumentID()) + 1;
                part.deleteDocument(dels[i]);
            }
        }
    }
}
