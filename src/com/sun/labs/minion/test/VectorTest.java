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

import com.sun.labs.minion.IndexConfig;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.Log;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;

import com.sun.labs.minion.util.Getopt;

/**
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class VectorTest {
    
    protected static DecimalFormat form = new DecimalFormat("###0.00");
    
    /** Creates a new instance of VectorTest */
    public VectorTest() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        int vecLen = 10;
        int    c;
        int nDocs;
        String indexDir = null;
        String cmFile = null;
        String stuff = null;
        IndexConfig indexConfig = null;
        boolean doTest = false;
        boolean doCheck = false;
        boolean doSort = false;
        
        Getopt gopt  = new Getopt(args, "cd:i:n:x:st");
        
        //
        // Set up the logging for the search engine.  We'll send everything
        // to the standard output, except for errors, which will go to
        // standard error.  We'll set the level at 3, which is pretty
        // verbose.
        Log log = Log.getLog();
        log.setStream(System.out);
        log.setLevel(3);
        String logTag = "VT";
        
        //
        // Handle the options.
        while ((c = gopt.getopt()) != -1) {
            switch (c) {
                
                case 'c':
                    doCheck = true;
                    break;
                case 'd':
                    indexDir = gopt.optArg;
                    break;
                case 'i':
                    stuff = gopt.optArg;
                    break;
                case 'n':
                    vecLen = Integer.parseInt(gopt.optArg);
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
            engine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(cmFile, indexDir);
        } catch (SearchEngineException se) {
            log.error("Indexer", 1, "Error opening collection", se);
            return;
        }
        
        nDocs = engine.getNDocs();
        
        if(stuff != null) {
            BufferedReader r;
            try {
                r = new BufferedReader(new FileReader(stuff));
            } catch (java.io.IOException ioe) {
                log.error("Indexer", 0, "Error opening list of files", ioe);
                return;
            }
            
            String n;
            
            try {
                while((n = r.readLine()) != null) {
                    
                    IndexableFile f = new IndexableFile(n);
                    
                    //
                    // We'll use a sequenced map because we want to process things
                    // the same way, every time!
                    Map m = new LinkedHashMap();
                    
                    
                    //
                    // Put the date in the map.
                    m.put("last-mod", new Date(f.lastModified()));
                    
                    //
                    // Finally, put in the file to index.  We won't be saveing the
                    // data into an explicit field, but that is possible.
                    m.put("file", f);
                    
                    //
                    // Make up a feature vector.
                    double[] fv = new double[vecLen];
                    for(int i = 0; i < fv.length; i++) {
                        fv[i] = (nDocs+1) * (i+1);
                    }
                    m.put("features", fv);
                    
                    m.put("dn", new Integer(nDocs+1));
                    
                    
                    try {
                        engine.index(f.getAbsolutePath(), m);
                        nDocs++;
                        if(nDocs % 1000 == 0) {
                            log.log(logTag, 0, "Indexed " + nDocs + " docs");
                        }
                        
                    } catch (Exception e) {
                        log.error(logTag, 0, "Error indexing", e);
                    }
                }
            } catch (java.io.IOException ioe) {
                log.error("Indexer", 0,
                        "Error reading from list of files, continuing", ioe);
            }
 
            //
            // We're now done indexing, so flush out the data and get ready to do
            // some lookups!
            engine.flush();
        }
        
        if(!doTest) {
            engine.close();
            return;
        }
        
      
        //
        // The number of documents indexed in the engine.
        int n = engine.getNDocs();
        int tc = n * n;
        int nc = 0;
        int p = tc / 10;

        long start = System.currentTimeMillis();
        long curr = start;
        for(Iterator i = engine.getManager().getActivePartitions().iterator(); i.hasNext();) {
            DiskPartition dp = (DiskPartition) i.next();
            for(Iterator d = dp.getDocumentIterator(); d.hasNext(); ) {
                DocKeyEntry dke = (DocKeyEntry) d.next();
                
                //
                // Get the feature vector for this document.
                double[] feat = (double[]) engine.getFieldValue("features",
                        dke.getName().toString());
                int dn = (int) ((Long) engine.getFieldValue("dn", dke.getName().toString())).longValue();
                
                //
                // We'll check for null, no matter what.
                if(feat == null) {
                    System.out.println("null features for: " + dke.getName() + " " +
                            dn);
                    continue;
                }
                
                if(doCheck) {
                    //
                    // Check whether the values are the same.
                    for(int f = 0; f < feat.length; f++) {
                        if(feat[f] != dn * (f+1)) {
                            System.out.println(dn + " " + f + " " +
                                    feat[f] + " " + (dn * (f+1)));
                        }
                    }
                }
                
                //
                // Run the similarity computation for this document.
                ResultSet rs = engine.getSimilar((String) dke.getName(),
                        "features");
                List l = rs.getAllResults(doSort);
                
                if(doCheck) {
                    //
                    // See whether the distance/similarity calculation works.
                    for (Iterator j = l.iterator(); j.hasNext();) {
                        Result r = (Result) j.next();
                        double[] ovec = (double[]) r.getField("features").get(0);
                        double dist = 0;
                        for(int f = 0; f < feat.length; f++) {
                            double x = feat[f] - ovec[f];
                            dist += x * x;
                        }
                        dist = Math.sqrt(dist);
                        if(r.getScore() != (float) dist) {
                            System.out.println("Error: " + dke.getName() + " " +
                                    r.getKey() + " " + r.getScore() + " " + dist);
                        }
                        nc++;
                        if(nc % p == 0) {
                            System.out.println(nc + "/" + tc + " " + (System.currentTimeMillis() - curr));
                            curr = System.currentTimeMillis();
                        }
                    }
                }
            }
        }
        
        long time = System.currentTimeMillis() - start;
        System.out.println("Calc took: " + time);
        if(doCheck) {
            System.out.println(form.format(nc / (double) time) + " calcs per ms");
        } else {
            System.out.println(form.format(tc / (double) time) + " calcs per ms");
        }
        
        engine.close();
    }
}
