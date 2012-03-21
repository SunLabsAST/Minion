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
import com.sun.labs.minion.indexer.partition.*;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A mutator that indexes a simplified version of Wikipedia entries so that we
 * can test indexing and re-indexing.
 */
public class WikipediaMutator implements Runnable {

    protected PrintWriter out;
    
    protected SearchEngine engine;

    protected Thread indexingThread;

    protected List<IndexableMap> indexList;

    protected String stuff;

    protected int nLines;

    protected int maxUpd;

    protected int nUpdates;

    protected int updateInterval;

    static final Logger logger =
            Logger.getLogger(WikipediaMutator.class.getName());

    private static String indexDir;

    public WikipediaMutator(SearchEngine engine, List<IndexableMap> indexList, File outputFile)
            throws SearchEngineException, IOException {
        this.engine = engine;
        this.indexList = indexList;
        out = new PrintWriter(new FileWriter(outputFile));
    }

    public void setMaxUpd(int maxUpd) {
        this.maxUpd = maxUpd;
    }

    public void setnLines(int nLines) {
        this.nLines = nLines;
    }

    public void setnUpdates(int nUpdates) {
        this.nUpdates = nUpdates;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    private static void defineFields(SearchEngine engine, boolean simpleFields)
            throws SearchEngineException {
        EnumSet<FieldInfo.Attribute> indexed = FieldInfo.getIndexedAttributes();
        EnumSet<FieldInfo.Attribute> indexedAndSaved = EnumSet.copyOf(indexed);
        indexedAndSaved.add(FieldInfo.Attribute.SAVED);
        EnumSet<FieldInfo.Attribute> savedOnly = EnumSet.of(
                FieldInfo.Attribute.SAVED);

        FieldInfo fi;
        if(simpleFields) {
            indexed.remove(FieldInfo.Attribute.POSITIONS);
            indexed.remove(FieldInfo.Attribute.VECTORED);
            indexed.remove(FieldInfo.Attribute.CASED);
            indexedAndSaved.remove(FieldInfo.Attribute.POSITIONS);
            indexedAndSaved.remove(FieldInfo.Attribute.VECTORED);
            indexedAndSaved.remove(FieldInfo.Attribute.CASED);
            fi = new FieldInfo("title",
                               savedOnly, FieldInfo.Type.STRING);
        } else {
            fi = new FieldInfo("title",
                               indexedAndSaved, FieldInfo.Type.STRING,
                               FieldInfo.DEFAULT_PIPELINE_FACTORY_NAME);
        }
        engine.defineField(fi);
        fi = new FieldInfo("text", indexed, FieldInfo.Type.NONE,
                           FieldInfo.DEFAULT_PIPELINE_FACTORY_NAME);
        engine.defineField(fi);
        fi = new FieldInfo("timestamp", savedOnly, FieldInfo.Type.DATE);
        engine.defineField(fi);
        fi = new FieldInfo("id", savedOnly, FieldInfo.Type.INTEGER);
        engine.defineField(fi);
    }

    @Override
    public void run() {

        int nIter = 1;
        boolean done = false;
        Random rand = new Random();

        //
        // Loop until we're done.
        mainLoop:
        while(!done) {

            //
            // How many documents will we do this time around?  Note that
            // the answer can be 0!
            int toIndex = rand.nextInt(maxUpd);
            String ls = String.format("Iteration %d %d docs", nIter, toIndex);
            logger.log(Level.INFO, ls);
            out.format("%d %s\n", System.currentTimeMillis(), ls);

            for(int i = 0; i < toIndex; i++) {

                //
                // Try to index it.
                IndexableMap map = indexList.get(rand.nextInt(indexList.size()));
                out.format("%d i %s\n", System.currentTimeMillis(), map.getKey());
                try {
                    engine.index(map);
                } catch(Exception se) {
                    logger.log(Level.SEVERE, 
                               String.format("Error indexing document %s", map.getKey()),
                               se);
                }
            }

            if(toIndex > 0) {
                try {
                    engine.flush();
                } catch(SearchEngineException se) {
                    logger.log(Level.SEVERE, "Error flushing", se);
                }
            }

            if(updateInterval > 0) {

                int ms = rand.nextInt(updateInterval * 1000);

                logger.info(String.format("Sleeping %d ms", ms));

                try {
                    Thread.sleep(ms);
                } catch(InterruptedException ie) {
                }
            }

            //
            // See whether we're done.
            done = nUpdates >= 0 && nIter >= nUpdates;
            nIter++;
            out.flush();
        }
        out.close();
    }

    public static void usage() {
        System.out.println(
                "Usage: java Mutator\n" 
                + " -i <Wikipedia data>   "
                + "Wiki data in PBLML (Required)\n" 
                + " -d <index dir>   "
                + "The index directory\n" 
                + " -k               " 
                + "Keep all partitions, even reaped ones"
                + " -r <num>         "
                + "The number of times to update, < 0 means run continuously (default: 10)\n"
                + " -s <num>         "
                + "Maximum number of seconds between update runs\n"
                + " -t <num>         " + 
                "The number of threads to run\n"
                + " -n <num>         "
                + "The number of lines to use from the input file (default: all)\n"
                + " -m <num>          "
                + "The maximum number of files to index/update\n"
                + " -x <file>        "
                + "An XML configuration file specifying extra props for "
                + "the index");
    }

    public static void main(String[] args) throws java.io.IOException,
            SearchEngineException {

        String flags = "i:d:ks:n:m:o:r:t:x:";
        String stuff = null;
        int updateInterval = 10;
        int nUpdates = 10;
        int nLines = Integer.MAX_VALUE;
        int maxUpd = 100;
        int nThreads = 1;
        boolean reapDoesNothing = false;
        Getopt gopt = new Getopt(args, flags);
        int c;
        URL cmFile = null;
        File outputDir = new File("mut");

        if(args.length == 0) {
            usage();
            return;
        }

        //
        // Handle the options.
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'i':
                    stuff = gopt.optArg;
                    break;

                case 'd':
                    indexDir = gopt.optArg;
                    break;
                case 'k':
                    reapDoesNothing = true;
                    break;

                case 's':
                    try {
                        updateInterval = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        //
                        // Unknown type.
                        logger.warning("Invalid update interval: " + gopt.optArg);
                        usage();
                        return;
                    }
                    break;

                case 'm':
                    try {
                        maxUpd = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        //
                        // Unknown type.
                        logger.warning("Invalid maximum update: " + gopt.optArg);
                        usage();
                        return;
                    }
                    break;

                case 'n':
                    try {
                        nLines = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        //
                        // Unknown type.
                        logger.warning("Invalid number of lines: " + gopt.optArg);
                        usage();
                        return;
                    }
                    break;
                    
                case 'o':
                    outputDir = new File(gopt.optArg);
                    break;

                case 'r':
                    try {
                        nUpdates = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        //
                        // Unknown type.
                        logger.warning("Invalid number of updates: "
                                + gopt.optArg);
                        usage();
                        return;
                    }
                    break;
                case 't':
                    try {
                        nThreads = Integer.parseInt(gopt.optArg);
                    } catch(NumberFormatException nfe) {
                        //
                        // Unknown type.
                        logger.warning("Invalid number of threads: "
                                + gopt.optArg);
                        usage();
                        return;
                    }
                    break;
                case 'x':
                    cmFile = (new File(gopt.optArg)).toURI().toURL();
                    break;
            }

        }

        if(stuff == null) {
            logger.warning("You must specify wikipedia data.");
            usage();
            return;
        }
        
        if(outputDir.exists()) {
            if(!outputDir.isDirectory()) {
                logger.warning(String.format("Output directory %s is not a directory", outputDir));
                usage();
                return;
            }
        } else {
            if(!outputDir.mkdirs()) {
                logger.warning(String.format("Unable to make output directory %s", outputDir));
                usage();
                return;
            }
        }
        
        //
        // Set up the log.
        Logger rl = Logger.getLogger("");
        Handler fh = new FileHandler((new File(outputDir, "mut.log")).getAbsolutePath());
        rl.addHandler(fh);
        for(Handler h : rl.getHandlers()) {
            h.setFormatter(new SimpleLabsLogFormatter());
            h.setLevel(Level.ALL);
        }

        
        Logger sl;
        sl = Logger.getLogger(MemoryPartition.class.getName());
        sl.setLevel(Level.FINE);
        sl = Logger.getLogger(InvFileMemoryPartition.class.getName());
        sl.setLevel(Level.FINE);
        sl = Logger.getLogger(InvFileMemoryPartition.class.getName());
        sl.setLevel(Level.FINE);
        sl = Logger.getLogger(Marshaller.class.getName());
        sl.setLevel(Level.FINE);
        sl = Logger.getLogger(DiskPartition.class.getName());
        sl.setLevel(Level.FINE);
        sl = Logger.getLogger(InvFileDiskPartition.class.getName());
        sl.setLevel(Level.FINE);

        //
        // Open the input data.
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(stuff));
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Unable to open file: " + stuff, ioe);
            usage();
            return;
        }

        List<IndexableMap> docs = new ArrayList<IndexableMap>();

        BufferedReader r =
                new BufferedReader(new InputStreamReader(new FileInputStream(
                stuff)));
        String l;
        int line = 0;
        while((l = r.readLine()) != null && docs.size() < nLines) {
            String[] fields = l.split("<sep>");
            if(fields.length != 4) {
                logger.warning(String.format("Weird line %s:%d", stuff,
                                             line));
                continue;
            }
            IndexableMap doc = new IndexableMap(fields[0]);
            doc.put("id", fields[0]);
            doc.put("timestamp", fields[1]);
            doc.put("title", fields[2]);
            doc.put("text", fields[3]);
            docs.add(doc);
        }


        Thread[] threads = new Thread[nThreads];
        WikipediaMutator[] muts = new WikipediaMutator[nThreads];

        SearchEngine engine = SearchEngineFactory.getSearchEngine(indexDir,
                                                                  cmFile);

        sl = Logger.getLogger(MemoryPartition.class.getName());
        sl.setLevel(Level.FINE);
        sl = Logger.getLogger(InvFileMemoryPartition.class.getName());
        sl.setLevel(Level.FINE);
        sl = Logger.getLogger(Marshaller.class.getName());
        sl.setLevel(Level.FINE);
        sl = Logger.getLogger(DiskPartition.class.getName());
        sl.setLevel(Level.FINE);

        engine.getManager().setReapDoesNothing(reapDoesNothing);
        defineFields(engine, false);
        try {
            for(int i = 0; i < nThreads; i++) {
                File outputFile = new File(outputDir, String.format("%03d.out", i+1));
                muts[i] = new WikipediaMutator(engine, docs, outputFile);
                muts[i].setMaxUpd(maxUpd);
                muts[i].setUpdateInterval(updateInterval);
                muts[i].setnLines(nLines);
                muts[i].setnUpdates(nUpdates);
                threads[i] = new Thread(muts[i]);
                threads[i].setName(String.format("Mut-%03d", i));
                threads[i].start();
            }
        } catch(SearchEngineException se) {
            logger.log(Level.SEVERE, "Error creating mutators", se);
            return;
        }

        for(int i = 0; i < nThreads; i++) {
            try {
                threads[i].join();
            } catch(InterruptedException ie) {
            }
        }

        //
        // Close the search engines.
        try {
            engine.close();
        } catch(SearchEngineException se) {
            logger.log(Level.SEVERE, "Error closing engines", se);
        }
    }
}
