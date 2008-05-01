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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Logger;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.MinionLog;

/**
 *
 * A class that will delete a set of documents from a list of partitions.
 */
public class Deleter {

    private DiskPartition[] parts;
    private List<DiskPartition> toMerge;

    public Deleter() {

    }

    public Deleter(PartitionManager pm, List<Integer> partNums) {
        List<DiskPartition> pmp = pm.getActivePartitions();
        parts = new DiskPartition[pmp.get(pmp.size() - 1).getPartitionNumber() + 1];
        for (DiskPartition p : pmp) {
            parts[p.getPartitionNumber()] = p;
        }

        //
        // Get a merger for the partitions of interest.
        toMerge = new ArrayList<DiskPartition>();
        for (Integer i : partNums) {
            toMerge.add(parts[i]);
        }

        //
        // Restrict our partitions to just these, to reduce clutter.
        for (int i = 0; i < parts.length; i++) {
            parts[i] = null;
        }

        for (DiskPartition p : toMerge) {
            parts[p.getPartitionNumber()] = p;
        }

    }

    public List<DiskPartition> getMerging() {
        return toMerge;
    }

    public void doDel(String delFile) throws IOException {
        BufferedReader r = new BufferedReader(new FileReader(delFile));
        String l;
        while ((l = r.readLine()) != null) {
            String[] vals = l.split("\\s+");
            int pn = Integer.parseInt(vals[0]);
            int docid = Integer.parseInt(vals[1]);
            if (pn < parts.length && parts[pn] != null) {
                parts[pn].deleteDocument(docid);
            }
        }
    }

    private static void usage() {
        System.err.println("Usage: Deleter -d <indexDir> -i <partNum> -f <delfile>");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SearchEngineException, FileNotFoundException, IOException {
        String flags = "d:i:f:";
        String indexDir = null;
        String delFile = null;
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
                case 'i':
                    partNums.add(Integer.parseInt(gopt.optArg));
                    break;
                case 'f':
                    delFile = gopt.optArg;
                    break;
            }
        }
        
        if (indexDir == null || delFile == null || partNums.size() == 0) {
            usage();
            return;
        }

        Collections.sort(partNums);

        SearchEngineImpl engine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(indexDir);

        //
        // Delete the documents.
        Deleter del = new Deleter(engine.getPM(), partNums);
        del.doDel(delFile);
        engine.close();
    }
}
