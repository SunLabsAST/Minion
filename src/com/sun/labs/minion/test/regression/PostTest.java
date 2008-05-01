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

package com.sun.labs.minion.test.regression;

import com.sun.labs.minion.SearchEngineException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.PosPostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;

import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.MinionLog;

/**
 * A test program that will exercise the postings iterators for a set of
 * partitions.
 */
public class PostTest {
    
    /**
     *
     */
    protected static void usage() {
        System.out.println(
                "Usage: java PostTest [options] term term ...\n" +
                " Options:\n" +
                "  -d <index dir>   " +
                "Directory containing index (Required)\n" +
                "  -f <field name>  " +
                "Restrict iterators to this field\n" +
                "  -w               " +
                "Get words for the documents retrieved\n" +
                "  -p <number>      " +
                "Exercise postings from the given partition\n" +
                "  -a               " +
                "Do all partitions in the index\n" +
                "  -l               " +
                "Do all terms in a partition\n" +
                "  -q               Stay quiet"
                );
        System.out.println("\n" +
                "The p and f options may be specified multiple times," +
                "if so desired.");
        
    }
    
    /**
     *
     * @param args
     * @throws java.io.IOException
     * @throws com.sun.labs.minion.SearchEngineException
     */
    public static void main(String[] args) throws java.io.IOException, SearchEngineException {
        
        if(args.length == 0) {
            usage();
            return;
        }
        
        String 	flags 	 = "d:f:p:laqw";
        String 	indexDir = null;
        boolean fullDict = false;
        boolean allParts = false;
        boolean quiet 	 = false;
        boolean getWords = false;
        boolean caseSensitive = false;
        Getopt 	gopt 	 = new Getopt(args, flags);
        int 	c;
        
        //
        // Set up the log.
        MinionLog log = MinionLog.getLog();
        log.setStream(System.out);
        log.setStream(MinionLog.ERROR, System.err);
        log.setLevel(3);
        String logTag = "PostTest";
        
        //
        // The fields of interest.
        List<String> fields = new ArrayList<String>();
        
        //
        // The partitions that we'll do.
        List<Integer> partNums = new ArrayList<Integer>();
        
        while ((c = gopt.getopt()) != -1) {
            switch (c) {
                
            case 'd':
                indexDir = gopt.optArg;
                break;
                
            case 'f':
                fields.add(gopt.optArg);
                break;
                
            case 'p':
                try {
                    partNums.add(new Integer(gopt.optArg));
                } catch (NumberFormatException nfe) {
                    log.error("findID", 0,
                            "Bad partition number: " + gopt.optArg);
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
                log.warn("PostTest", 0, "Unknown option: " + (char) c);
                break;
            }
        }
        
        if(indexDir == null) {
            log.error(logTag, 0,
                    "Must specify index directory");
            return;
        }
        
        SearchEngineImpl engine = (SearchEngineImpl) com.sun.labs.minion.SearchEngineFactory.getSearchEngine(indexDir);
        PartitionManager pm = engine.getManager();
        
        //
        // The partitions.
        DiskPartition[] parts = pm.getActivePartitions().toArray(new DiskPartition[0]);
        if(!allParts) {
            for(int i = 0; i < parts.length; i++) {
                int pn = parts[i].getPartitionNumber();
                if(!partNums.contains(pn)) {
                    parts[i] = null;
                }
            }
        }
        
        int nEntries = 1;
        
        //
        // Get the fields of interest.
        int[] fieldArray = null;
        if(fields.size() > 0) {
            fieldArray = pm.getMetaFile().getFieldArray(fields.toArray(new String[0]));
        }
        
        //
        // Features for our iterators.
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures(engine.getQueryConfig().getWeightingFunction(),
                engine.getQueryConfig().getWeightingComponents());
        feat.setCaseSensitive(caseSensitive);
        feat.setFields(fieldArray);
        feat.setPositions(getWords);
        
        //
        // Loop over the partitions.
        for(DiskPartition part : parts) {
            
            if(part == null) {
                continue;
            }
            
            System.out.println("Processing " + part);
            
            Iterator termIter;
            
            if(fullDict) {
                termIter = part.getMainDictionaryIterator();
            } else {
                List<QueryEntry> el = new ArrayList<QueryEntry>();
                for(int j = gopt.optInd; j < args.length; j++) {
                    QueryEntry qe = part.getTerm(args[j]);
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
                    
                    int nDocs = e.getN();
                    int[] docs = new int[nDocs];
                    
                    PostingsIterator pi = e.iterator(feat);
                    
                    if(pi == null) {
                        continue;
                    }
                    
                    if(!quiet) {
                        System.out.println(" " + e);
                    }
                    
                    int n = 0;
                    try {
                        while(pi.next()) {
                            docs[n] = pi.getID();
                            if(getWords) {
                                ((PosPostingsIterator) pi).getPositions();
                            }
                            n++;
                        }
                    } catch (Exception itere) {
                        System.out.println("Error iterating through entry " +
                                e);
                        itere.printStackTrace(System.out);
                        continue termLoop;
                    }
                    
                    
                    pi.reset();
                    
                    if(n == 0) {
                        if(!quiet) {
                            System.out.println(" No docs");
                        }
                        continue termLoop;
                    }
                    
                    int reps = nDocs*5;
                    int pos = 0;
                    boolean headerPrinted = false;
                    for(int j = 0; j < reps; j++) {
                        pos = (int) (Math.random() * n);
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
                                    ((PosPostingsIterator) pi).getPositions();
                                }
                            }
                        } catch (Exception ex) {
                            System.out.println("Error finding known documents for " +
                                    e + " doc: " +
                                    docs[pos]);
                            ex.printStackTrace(System.out);
                            continue termLoop;
                        }
                        
                    }
                    
                    headerPrinted = false;
                    if(n > 1) {
                        try {
                            for(int j = 0; j < reps; j++) {
                                pos = (int) (Math.random() * (n-1));
                                if(docs[pos] + 1 != docs[pos+1]) {
                                    if(pi.findID(docs[pos]+1)) {
                                        if(!headerPrinted) {
                                            if(quiet) {
                                                System.out.println(
                                                        "Problem with term: " + e);
                                            }
                                            System.out.println("  Known absent docs");
                                            headerPrinted = true;
                                        }
                                        System.out.println(" BAD:  " + docs[pos]);
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            System.out.println("Error finding absent documents for " +
                                    e + " doc: " +
                                    docs[pos]);
                            ex.printStackTrace(System.out);
                            continue termLoop;
                        }
                    }
                }
        }
        
        engine.close();
    }
} // PostTest
