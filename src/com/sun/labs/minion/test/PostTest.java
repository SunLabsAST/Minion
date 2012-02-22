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

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.*;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.util.LabsLogFormatter;
import com.sun.labs.util.NanoWatch;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A test program that will exercise the postings iterators for a set of
 * partitions.
 */
public class PostTest {

    protected static void usage() {
        System.out.println(
                "Usage: PostTest [options] [term] [term] ...\n"
                + " Options:\n"
                + "  -d <index dir>   "
                + "Directory containing index (Required)\n"
                + "  -f <field name>  "
                + "Restrict iterators to this field\n"
                + "  -w               "
                + "Get words for the documents retrieved\n"
                + "  -p <number>      "
                + "Exercise postings from the given partition\n"
                + "  -a               "
                + "Do all partitions in the index\n"
                + "  -l               "
                + "Do all terms in a partition\n"
                + "  -q               Stay quiet");
        System.out.println("\n"
                + "The p and f options may be specified multiple times,"
                + "if so desired.");
    }

    public static void main(String[] args) throws java.io.IOException, SearchEngineException {

        if(args.length == 0) {
            usage();
            return;
        }

        String flags = "d:f:p:laqw";
        String indexDir = null;
        boolean fullDict = false;
        boolean allParts = false;
        boolean quiet = false;
        boolean getWords = false;
        boolean caseSensitive = false;
        Getopt gopt = new Getopt(args, flags);
        int c;

        Thread.currentThread().setName("PostTest");
        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
        }

        //
        // Set up the log.
        Logger logger = Logger.getLogger(PostTest.class.getName());

        //
        // The fields of interest.
        List<String> fields = new ArrayList<String>();

        //
        // The partitions that we'll do.
        List<Integer> partNums = new ArrayList<Integer>();

        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'f':
                    fields.add(gopt.optArg);
                    break;

                case 'p':
                    try {
                        partNums.add(new Integer(gopt.optArg));
                    } catch(NumberFormatException nfe) {
                        logger.severe(String.format("Bad partition number %s", gopt.optArg));
                        return;
                    }
                    break;

                case 'l':
                    fullDict = true;
                    break;

                case 'a':
                    allParts = true;
                    break;

                case 's':
                    caseSensitive = true;
                    break;

                case 'q':
                    quiet = true;
                    break;

                case 'w':
                    getWords = true;
                    break;

                default:
                    logger.warning(String.format("Unknown option: %s", (char) c));
                    break;
            }
        }

        if(indexDir == null) {
            logger.severe("Must specify index directory");
            return;
        }

        SearchEngineImpl engine =
                (SearchEngineImpl) com.sun.labs.minion.SearchEngineFactory.getSearchEngine(indexDir);
        PartitionManager pm = engine.getManager();
        
        logger.info(String.format("Opened index"));

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

        int nEntries = 1;
        
        logger.info(String.format("Processing partitions %s", parts));

        //
        // Get the fields of interest.
        Collection<FieldInfo> doFields;
        if(fields.size() > 0) {
            doFields = pm.getMetaFile().getFieldInfo(fields);
        } else {
            doFields = pm.getMetaFile().getFieldInfo(FieldInfo.Attribute.INDEXED);
        }
        
        logger.info(String.format("Using fields %s", doFields));

        //
        // Features for our iterators.
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures(engine.getQueryConfig().getWeightingFunction(),
                engine.getQueryConfig().getWeightingComponents());
        feat.setPositions(getWords);
        
        Random rand = new Random();
        
        //
        // Loop over the partitions.
        for(DiskPartition part : parts) {

            for(FieldInfo fi : doFields) {

                System.out.println(String.format("Processing %s %s", part, fi.getName()));

                Iterator termIter;
                DiskField df = ((InvFileDiskPartition) part).getDF(fi);

                if(fullDict) {
                    termIter = df.getTermDictionary(caseSensitive).iterator();
                } else {
                    List<QueryEntry> el = new ArrayList<QueryEntry>();
                    for(int j = gopt.optInd; j < args.length; j++) {
                        QueryEntry qe = df.getTerm(args[j], caseSensitive);
                        if(qe != null) {
                            el.add(qe);
                        }
                    }
                    termIter = el.iterator();
                }

                termLoop:
                while(termIter.hasNext()) {

                    if(quiet && nEntries % 50000 == 0) {
                        System.out.println("Processed: " + nEntries);
                    }
                    nEntries++;


                    QueryEntry e = (QueryEntry) termIter.next();
                    
                    if(!quiet) {
                        System.out.format(" %s %d docs\n", e.getName(), e.getN());
                    }
                    
                    int nDocs = e.getN();
                    int[] docs = new int[nDocs];
                    int[] idSkipPos = new int[nDocs];
                    int[] posSkipPos = new int[nDocs];
                    int[] idSkipTable = null;
                    int[] offSkipTable = null;
                    PostingsIterator pi = e.iterator(feat);
                    
                    if(pi instanceof PositionPostings.CompressedIterator) {
                        idSkipTable = ((PositionPostings.CompressedIterator) pi).getSkipID();
                        offSkipTable = ((PositionPostings.CompressedIterator) pi).getSkipIDOffsets();
                    }

                    if(pi == null) {
                        continue;
                    }


                    int n = 0;
                    try {
                        if(pi instanceof PositionPostings.CompressedIterator) {
                            idSkipPos[n] = (int) ((PositionPostings.CompressedIterator) pi).getIDPos();
                            posSkipPos[n] = (int) ((PositionPostings.CompressedIterator) pi).getPosPos();
                        }
                        while(pi.next()) {
                            docs[n] = pi.getID();
                            if(getWords) {
                                ((PostingsIteratorWithPositions) pi).getPositions();
                            }
                            n++;
                            if(n < idSkipPos.length && pi instanceof PositionPostings.CompressedIterator) {
                                idSkipPos[n] = (int) ((PositionPostings.CompressedIterator) pi).getIDPos();
                                posSkipPos[n] = (int) ((PositionPostings.CompressedIterator) pi).getPosPos();
                            }
                        }
                    } catch(Exception ex) {
                        System.out.format("Error iterating through entry %s\n", e);
                        ex.printStackTrace(System.out);
                        continue termLoop;
                    }

                    pi.reset();
                    
                    if(idSkipPos != null && idSkipTable != null) {
                        for(int i = 1; i < idSkipTable.length; i++) {
                            int id = idSkipTable[i];
                            int pos = Arrays.binarySearch(docs, id);
                            if(pos < 0) {
                                logger.info(String.format("Couldn't find skip ID %d for %s in %s", id, e.getName(), fi.getName()));
                                continue;
                            }
                            if(idSkipPos[pos] != offSkipTable[i]) {
                                logger.info(String.format("Mismatch in skip table for skip ID %d for %s in %s expected: %d found: %d", 
                                        id, e.getName(), fi.getName(), offSkipTable[i], idSkipPos[pos]));
                            }
                        }
                    }

                    if(n == 0) {
                        if(!quiet) {
                            System.out.println(" No docs");
                        }
                        continue termLoop;
                    }

                    int reps = nDocs * 5;
                    int pos = 0;
                    boolean headerPrinted = false;
                    for(int j = 0; j < reps; j++) {
                        pos = rand.nextInt(n);
                        try {
                            if(!pi.findID(docs[pos])) {
                                if(!headerPrinted) {
                                    if(quiet) {
                                        System.out.println("Problem with term: " + e);
                                    }
                                    System.out.println("  Known present docs");
                                    headerPrinted = true;
                                }
                                System.out.println(" BAD:  " + docs[pos]);
                            } else {
                                if(getWords) {
                                    ((PostingsIteratorWithPositions) pi).getPositions();
                                }
                            }
                        } catch(Exception ex) {
                            System.out.format("Error finding known documents for %s doc: %d\n", 
                                    e, docs[pos]);
                            ex.printStackTrace(System.out);
                            continue termLoop;
                        }

                    }

                    headerPrinted = false;
                    if(n > 1) {
                        try {
                            for(int j = 0; j < reps; j++) {
                                pos = rand.nextInt(n - 1);
                                if(docs[pos] + 1 != docs[pos + 1]) {
                                    if(pi.findID(docs[pos] + 1)) {
                                        if(!headerPrinted) {
                                            if(quiet) {
                                                System.out.format("Problem with term: %s\n", e);
                                            }
                                            System.out.println("  Known absent docs");
                                            headerPrinted = true;
                                        }
                                        System.out.println(" BAD:  " + docs[pos]);
                                    }
                                }
                            }
                        } catch(Exception ex) {
                            System.out.format("Error finding absent documents for %s doc: %d\n",
                                    e, docs[pos]);
                            ex.printStackTrace(System.out);
                            continue termLoop;
                        }
                    }
                }
            }
        }
        
        engine.close();
    }
}