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
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Logger;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.MinionLog;

/**
 * A class to test deletions in partitions while those partitions are
 * being merged.
 */
public class DelDuringMerge {

    public static void deleter(DiskPartition dp, List<Integer> docs) {
        for(Integer doc : docs) {
            dp.deleteDocument(doc);
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SearchEngineException, FileNotFoundException, IOException {
        String flags = "d:f:x:";
        String indexDir = null;
        String ddmFile = null;
        URL cmFile = null;
        Getopt gopt = new Getopt(args, flags);
        int c;
        if (args.length == 0) {
            usage();
            return;
        }

        List<Integer> partNums = new ArrayList<Integer>();

        //
        // Set up the log.
        Logger logger = Logger.getLogger("");
        for (Handler h : logger.getHandlers()) {
            h.setFormatter(new SimpleLabsLogFormatter());
        }

        MinionLog.setLogger(logger);
        MinionLog.setLevel(3);

        while ((c = gopt.getopt()) != -1) {
            switch (c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;
                case 'f':
                    ddmFile = gopt.optArg;
                    break;
                case 'x':
                    cmFile = (new File(gopt.optArg)).toURI().toURL();
                    break;
                }
        }

        if (indexDir == null || ddmFile == null) {
            usage();
            return;
        }
        
        //
        // Read the del-during merge file.
        Map<Integer,List<Integer>> preMergeDels = new HashMap<Integer,List<Integer>>();
        Map<Integer,List<Integer>> duringMergeDels = new HashMap<Integer,List<Integer>>();
        List<Integer> parts =new ArrayList<Integer>();
        
        BufferedReader r = new BufferedReader(new FileReader(ddmFile));
        String l = r.readLine();
        
        //
        // Get the partitions to merge.
        for(String p : l.split(" ")) {
            parts.add(new Integer(p));
        }
        
        //
        // Write the active file.
        WActive wa = new WActive(indexDir + File.separatorChar + "index" + File.separatorChar +
                "AL.PM");
        wa.writeActiveFile(parts);
        
        //
        // Remove the deleted docs files for these partitions.
        for(Integer p : parts) {
            File f = PartitionManager.makeDeletedDocsFile(indexDir + File.separatorChar + "index", p);
            logger.info("Deleting: " + f);
            if(f.exists()) {
                f.delete();
            }
        }
        
        //
        // Read the pre-merge deletions.
        for(Integer p : parts) {
            List<Integer> docs = new ArrayList<Integer>();
            String[] v = r.readLine().split(" ");
            for(int i = 1; i < v.length; i++) {
                docs.add(new Integer(v[i]));
            }
            preMergeDels.put(p, docs);
        }
        
        //
        // Read the during-merge deletions.
        for(Integer p : parts) {
            List<Integer> docs = new ArrayList<Integer>();
            String[] v = r.readLine().split(" ");
            for(int i = 1; i < v.length; i++) {
                docs.add(new Integer(v[i]));
            }
            duringMergeDels.put(p, docs);
        }
        
        r.close();

        //
        // Get our engine.
        SearchEngineImpl engine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(indexDir, cmFile);

        //
        // Get the partitions.
        PartitionManager pm = engine.getPM();
        List<DiskPartition> active = pm.getActivePartitions();
        
        logger.info("Partitions: " + active);
        
        //
        // Do the pre-merge deletions.
        logger.info("Doing pre-merge deletions");
        for(DiskPartition dp : active) {
            logger.info("Pre-delete for " + dp);
            deleter(dp, preMergeDels.get(dp.getPartitionNumber()));
        }
        
        //
        // Start the merge, which will store the pre-merge deletion state.
        PartitionManager.Merger merger = null;
        merger = pm.getMerger(active);
        if (merger == null) {
            logger.severe("Could not get merger for " + active);
            return;
        }

        //
        // Now we can delete the documents we wanted to during the merge.
        logger.info("Doing during-merge deletions");
        for(DiskPartition dp : active) {
            logger.info("During merge delete for " + dp);
            deleter(dp, duringMergeDels.get(dp.getPartitionNumber()));
        }
        
        //
        // Report the state of the deletion maps for the various partitions.
        DiskPartition mp = merger.merge();
        
        for(DiskPartition dp : active) {
            logger.info(dp + " del: " + dp.getDelMap());
        }
        
        logger.info(mp + " del: " + mp.getDelMap());

        engine.close();
    }

    private static void usage() {
        System.err.println("Usage: DelDuringMerge -d <indexDir> -f <del-during-merge file>");
    }
}
