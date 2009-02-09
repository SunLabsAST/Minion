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
package com.sun.labs.minion.test.regression.deletion;

import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.util.Getopt;
import java.util.logging.Logger;

/**
 * A class that randomly deletes documents from an index, keeping a record of
 * the deletions that can be replayed at a later time.
 * @author Stephen Green <stephen.green@sun.com>
 */
public class RandomDeletions {

    /**
     * Creates a RandomDeletions
     */
    public RandomDeletions() {
    }

    /**
     * Prints usage message.
     */
    public static void usage() {
        System.err.println("Usage: RandomDeletions -d indexDir -n number");
        return;
    }

    /**
     * Main program.
     * @param args the arguments.
     */
    public static void main(String[] args) throws SearchEngineException, IOException {
        String replayFile = null;
        String flags = "d:n:o:pr:";

        Getopt gopt = new Getopt(args, flags);
        int c;
        String indexDir = null;
        String outputFile = "del.out";
        boolean doOpt = false;
        int nDel = 50;

        if(args.length == 0) {
            usage();
            return;
        }

        Thread.currentThread().setName("MergeTest");

        //
        // Handle the options.
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'n':
                    try {
                        nDel = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                    }
                    break;

                case 'o':
                    outputFile = gopt.optArg;
                    break;
                case 'p':
                    doOpt = true;
                    break;
                case 'r':
                    replayFile = gopt.optArg;
                    break;

            }
        }

        int remaining = nDel;
        int td = 0;
        Random rand = new Random();
        SearchEngineImpl engine = (SearchEngineImpl) SearchEngineFactory.
                getSearchEngine(indexDir);
        int startnd = engine.getNDocs();
        System.out.println("nDocs: " + startnd);
        List l = engine.getManager().getActivePartitions();

        if(replayFile != null) {
            BufferedReader r = new BufferedReader(new FileReader(replayFile));
            String s;
            while((s = r.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(s);
                int partNum = Integer.parseInt(st.nextToken());
                int nd = Integer.parseInt(st.nextToken());
                for(Iterator i = l.iterator(); i.hasNext();) {
                    DiskPartition p = (DiskPartition) i.next();
                    if(p.getPartitionNumber() == partNum) {
                        System.out.println(p + " " + nd);
                        for(int j = 0; j < nd; j++) {
                            int did = Integer.parseInt(r.readLine());
                            p.deleteDocument(did);
                        }
                    }
                }
            }
        } else {
            PrintWriter pw = new PrintWriter(new FileWriter(outputFile));
            for(Iterator i = l.iterator(); i.hasNext();) {
                DiskPartition p = (DiskPartition) i.next();
                int nd = i.hasNext() ? rand.nextInt(remaining) : remaining;
                pw.println(p.getPartitionNumber() + " " + nd);
                System.out.println(p + " " + nd + " deletions");
                for(int j = 0; j < nd; j++) {
                    int did = rand.nextInt(p.getMaxDocumentID()) + 1;
                    p.deleteDocument(did);
                    pw.println(did);
                }
                remaining -= nd;
                td += nd;
            }
            pw.close();
        }

        System.out.println(td + " deletions done");
        System.out.println("nDocs: " + engine.getNDocs() + " " + (startnd - td));
        if(doOpt) {
            engine.optimize();
        }
        engine.close();
    }
}
