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

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;

import java.text.DecimalFormat;


import com.sun.labs.minion.*;
import java.io.File;

import com.sun.labs.minion.util.Getopt;
import java.util.EnumSet;

public class URLIndexTest extends SEMain {
    private static final boolean DEBUG = false;
    
    public static void usage() {
        System.out.println(
                "Usage: java URLIndexTest [options] [file file....]\n" +
                " -i <file>   " +
                "File containing list of URLs (Required)\n" +
                " -d <index dir>   " +
                "Directory containing index (Required)\n" +
                " -x <file>        " +
                "An XML configuration file specifying extra props for " +
                "the index\n" +
                " -l <level>       " +
                "The log level to use"
                );
        return;
    }
    
    /**
     * A format object for formatting the output.
     */
    protected static DecimalFormat form = new DecimalFormat("########0.00");
    
    protected static String toMB(long x) {
        return form.format((float) x / (float) (1024 * 1024));
    }
    
    public static void reportProgress(long start,
            int n) {
        double secs = (System.currentTimeMillis() - start) / 1000.0;
        
        Log.log("Indexer", 3, n + " documents, " +
                form.format(secs) + " s, " +
                form.format(n / (secs / 3600)) + " docs/h " +
                toMB(Runtime.getRuntime().totalMemory()) + "MB");
    }
    
    
    public static void main(String[] args) throws MalformedURLException {
        
        String logTag    = "Indexer";
        String flags     = "i:d:x:l:";
        String urlsFile  = null;
        Getopt gopt      = new Getopt(args, flags);
        int    c;
        long   totalLen  = 0;
        int    nDocs     = 0;
        String indexDir  = null;
        URL cmFile       = null;
        long start = System.currentTimeMillis();
        
        
        if(args.length == 0) {
            usage();
            return;
        }
        
        Thread.currentThread().setName("IndexTest");
        
        int logLevel = 3;
        
        boolean profiling = false;
        
        //
        // Set up the logging for the search engine.  We'll send everything
        // to the standard output, except for errors, which will go to
        // standard error.  We'll set the level at 3, which is pretty
        // verbose.
        Log log = Log.getLog();
        log.setStream(System.out);
        log.setStream(Log.ERROR, System.err);
        log.setLevel(logLevel);
        
        //
        // Handle the options.
        while ((c = gopt.getopt()) != -1) {
            switch (c) {
                
                case 'i':
                    urlsFile = gopt.optArg;
                    break;
                    
                case 'd':
                    indexDir = gopt.optArg;
                    break;
                    
                case 'x':
                    cmFile = (new File(gopt.optArg)).toURI().toURL();
                    break;
                    
                case 'l':
                    try {
                        logLevel = Integer.parseInt(gopt.optArg);
                    } catch (NumberFormatException nfe) {
                    }
                    break;
                    
            }
        }
        
        
        //
        // We may have gotten a larger log level.
        log.setLevel(logLevel);
        
        if(indexDir == null && cmFile == null) {
            log.warn("IndexTest", 0,
                    "You must specify an index directory.");
            usage();
            return;
        }
        
        if(urlsFile == null) {
            log.warn(logTag, 0,
                    "You must specify a file of URLS to index with " +
                    "the -i option on the command line");
            usage();
            return;
        }
        
        //
        // Open our engine for use with the given configuration file
        SearchEngine engine;
        try {
            engine = SearchEngineFactory.getSearchEngine(indexDir, cmFile);
        
            //
            // Get a couple of sets of attributes for the fields that we want to
            // define.  The "IndexedAttributes" are Indexed, Tokenized, and
            // Vectored.
            EnumSet<FieldInfo.Attribute> ia = FieldInfo.getIndexedAttributes();
            //
            // Now make another set where we'll add Saved and Trimmed to the
            // above attributes.
            EnumSet<FieldInfo.Attribute> sa = ia.clone();
            sa.add(FieldInfo.Attribute.SAVED);
            sa.add(FieldInfo.Attribute.TRIMMED);
            engine.defineField(new FieldInfo("url", sa, FieldInfo.Type.STRING));
        } catch (SearchEngineException se) {
            log.error(logTag, 1, "Error opening collection", se);
            return;
        }
        
        if(urlsFile != null) {
            BufferedReader r;
            try {
                r = new BufferedReader(new FileReader(urlsFile));
            } catch (java.io.IOException ioe) {
                log.error(logTag, 0, "Error opening list of files", ioe);
                return;
            }
            
            String n;
            
            try {
                while((n = r.readLine()) != null) {
                    URL toIndex = null;
                    try {
                        toIndex = new URL(n);
                    } catch (MalformedURLException e) {
                        log.warn(logTag, 0, "Skipping bad URL: " + n);
                        continue;
                    }
                    IndexableMap docMap = new IndexableMap(n);
                    docMap.put("url", toIndex.toString());
                    docMap.put("body", toIndex);
                                        
                    try {
                        engine.index(docMap);
                        
                        nDocs++;
                        
                        if(nDocs % 1000 == 0) {
                            reportProgress(start, nDocs);
                        }
                        
                    } catch (SearchEngineException se) {
                        log.error("Indexer", 1, "Error indexing url " + toIndex,
                                se);
                    } catch (Exception e) {
                        log.error(logTag, 0, "Error indexing", e);
                    }
                }
            } catch (java.io.IOException ioe) {
                log.error("Indexer", 0,
                        "Error reading from list of files, continuing", ioe);
            }
        }

        reportProgress(start, nDocs);

        //
        // We're done, close down the engine.
        try {
            engine.close();
        } catch (SearchEngineException se) {
            log.error("Indexer", 1, "Error closing engine",
                    se);
        }
        
        if(profiling) {
            log.log("Indexer", 0, "Finished.  Sleeping for a few minutes.");
            try {
                Thread.sleep(600000);
            } catch (Exception e) {
            }
        }
        
    }
    
} // IndexTest
