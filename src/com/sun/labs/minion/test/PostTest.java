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
import com.sun.labs.minion.indexer.Field.DictionaryType;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.PositionPostings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.PostingsIteratorWithPositions;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.util.LabsLogFormatter;
import com.sun.labs.util.NanoWatch;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A test program that will exercise the postings iterators for a set of
 * partitions.
 */
public class PostTest implements Runnable {
    
    private static final Logger logger = Logger.getLogger(PostTest.class.getName());
    
    private SearchEngineImpl engine; 
    
    private InvFileDiskPartition part;
    
    private List<String> terms;
    
    private Collection<FieldInfo> fields;
    
    private boolean caseSensitive;
    
    private boolean quiet;
    
    private boolean getWords;
    
    private PrintWriter output;
    
    private Random rand;
    
    private CountDownLatch latch;
    
    private NanoWatch fw = new NanoWatch();
    
    private NanoWatch iw = new NanoWatch();
    
    private long docsIterated = 0;

    public PostTest(SearchEngineImpl engine,
            DiskPartition part, Collection<FieldInfo> fields, 
            List<String> terms, boolean caseSensitive, 
            boolean getWords,
            boolean quiet, PrintWriter output, CountDownLatch latch) {
        this.engine = engine;
        this.part = (InvFileDiskPartition) part;
        this.fields = fields;
        this.terms = terms;
        this.caseSensitive = caseSensitive;
        this.quiet = quiet;
        this.output = output;
        this.latch = latch;
        rand = new Random();
    }

    @Override
    public void run() {

        PostingsIteratorFeatures feat = new PostingsIteratorFeatures(engine.getQueryConfig().getWeightingFunction(),
                engine.getQueryConfig().getWeightingComponents());
        feat.setPositions(getWords);

        for(FieldInfo fi : fields) {

            output.println(String.format(" Field %s", fi.getName()));

            Iterator termIter;
            DiskField df = part.getDF(fi);

            int nEntries = 1;

            if(terms.isEmpty()) {
                DiskDictionary dd;
                if(caseSensitive) {
                    dd = (DiskDictionary) df.getDictionary(DictionaryType.CASED_TOKENS);
                } else {
                    dd = (DiskDictionary) df.
                            getDictionary(
                            DictionaryType.UNCASED_TOKENS);
                }
                if(dd == null) {
                    output.format("No dictionary for %s?\n", fi.getName());
                    continue;
                }
                termIter = dd.iterator();
            } else {
                List<QueryEntry> el = new ArrayList<QueryEntry>();
                for(String term : terms) {
                    QueryEntry qe = df.getTerm(term, caseSensitive);
                    if(qe != null) {
                        el.add(qe);
                    }
                }
                termIter = el.iterator();
            }

            termLoop:
            while(termIter.hasNext()) {

                if(quiet && nEntries % 50000 == 0) {
                    output.println("  Processed: " + nEntries);
                }
                nEntries++;


                QueryEntry e = (QueryEntry) termIter.next();

                if(!quiet) {
                    output.format("  %s %d docs\n", e.getName(), e.getN());
                }

                int nDocs = e.getN();
                int[] docs = new int[nDocs];
                int[] idSkipPos = new int[nDocs];
                int[] posSkipPos = new int[nDocs];
                int[] idSkipTable = null;
                int[] offSkipTable = null;
                PostingsIterator pi = e.iterator(feat);

                if(pi == null) {
                    continue;
                }

                if(pi instanceof PositionPostings.CompressedIterator) {
                    idSkipTable = ((PositionPostings.CompressedIterator) pi).getSkipID();
                    offSkipTable = ((PositionPostings.CompressedIterator) pi).getSkipIDOffsets();
                }

                int n = 0;
                try {
                    if(pi instanceof PositionPostings.CompressedIterator) {
                        idSkipPos[n] = (int) ((PositionPostings.CompressedIterator) pi).getIDPos();
                        posSkipPos[n] = (int) ((PositionPostings.CompressedIterator) pi).getPosPos();
                    }
                    iw.start();
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
                    iw.stop();
                    docsIterated += nDocs;
                } catch(Exception ex) {
                    output.format("Error iterating through entry %s %d\n", e.getName(), e.getN());
                    ex.printStackTrace(output);
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
                        output.println("  No docs");
                    }
                    continue termLoop;
                }

                int reps = nDocs * 5;
                int pos = 0;
                boolean headerPrinted = false;
                for(int j = 0; j < reps; j++) {
                    pos = rand.nextInt(n);
                    try {
                        fw.start();
                        boolean found = pi.findID(docs[pos]);
                        fw.stop();
                        if(!found) {
                            if(!headerPrinted) {
                                if(quiet) {
                                    output.println("   Problem with term: " + e);
                                }
                                output.println("    Known present docs");
                                headerPrinted = true;
                            }
                            output.println("     BAD:  " + docs[pos]);
                        } else {
                            if(getWords) {
                                ((PostingsIteratorWithPositions) pi).getPositions();
                            }
                        }
                    } catch(Exception ex) {
                        output.format("Error finding known documents for %s doc: %d\n",
                                e, docs[pos]);
                        ex.printStackTrace(output);
                        continue termLoop;
                    }

                }

                headerPrinted = false;
                if(n > 1) {
                    try {
                        for(int j = 0; j < reps; j++) {
                            pos = rand.nextInt(n - 1);
                            if(docs[pos] + 1 != docs[pos + 1]) {
                                fw.start();
                                boolean found = pi.findID(docs[pos] + 1);
                                fw.stop();
                                if(found) {
                                    if(!headerPrinted) {
                                        if(quiet) {
                                            output.format("   Problem with term: %s\n", e);
                                        }
                                        output.println("    Known absent docs");
                                        headerPrinted = true;
                                    }
                                    output.println("     BAD:  " + docs[pos]);
                                }
                            }
                        }
                    } catch(Exception ex) {
                        output.format("    Error finding absent documents for %s doc: %d\n",
                                e, docs[pos]);
                        ex.printStackTrace(output);
                        continue termLoop;
                    }
                }
                
            }

            if(quiet && nEntries % 50000 != 0) {
                output.println("  Processed: " + nEntries);
                output.flush();
            }
            logger.info(String.format("Finished %s in %s", fi.getName(), part));
        }
        logger.info(String.format("Finished %s", part));
        output.flush();
        latch.countDown();
    }

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

    public static String toString(int[] a, int[] b) {
        StringBuilder s = new StringBuilder(a.length * 4);
        s.append('[');
        int prev = 0;
        for(int i = 0; i < a.length && i < b.length; i++) {
            if(i > 0) {
                s.append(", ");
            }
            s.append('<').append(a[i] - prev).append(',').append(a[i]).append(',').append(b[i]).append('>');

            prev = a[i];
        }
        return s.toString();
    }

    public static void main(String[] args) throws java.io.IOException, SearchEngineException {

        if(args.length == 0) {
            usage();
            return;
        }

        String flags = "d:f:p:lo:aqsw";
        String indexDir = null;
        boolean allParts = false;
        boolean quiet = false;
        boolean getWords = false;
        boolean caseSensitive = false;
        File outputDir = new File(System.getProperty("user.dir"));
        Getopt gopt = new Getopt(args, flags);
        int c;

        Thread.currentThread().setName("PostTest");
        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
        }

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

                case 'o':
                    outputDir = new File(gopt.optArg);
                    if(!outputDir.exists()) {
                        if(!outputDir.mkdirs()) {
                            logger.log(Level.SEVERE, String.format("Unable to make output directory %s", outputDir));
                            usage();
                            return;
                        }
                    }
                    if(!outputDir.isDirectory()) {
                        logger.log(Level.SEVERE, String.format("Output directory %s is not a directory", outputDir));
                        usage();
                        return;
                    }
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
        
        logger.info(String.format("Opened index with %d docs", engine.getNDocs()));

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
        
        logger.info(String.format("Using parts: %s", parts));
        
        //
        // Get the fields of interest.
        Collection<FieldInfo> doFields;
        if(fields.size() > 0) {
            doFields = pm.getMetaFile().getFieldInfo(fields);
        } else {
            doFields = pm.getMetaFile().getFieldInfo(FieldInfo.Attribute.INDEXED);
        }
        
        logger.info(String.format("Using fields %s", doFields));
        
        List<String> terms = new ArrayList<String>();
        for(int i = gopt.optInd; i < args.length; i++) {
            terms.add(args[i]);
        }

        CountDownLatch latch = new CountDownLatch(parts.size());
        PrintWriter[] outputs = new PrintWriter[parts.size()];
        PostTest[] postTest = new PostTest[parts.size()];
        Thread[] partThreads = new Thread[parts.size()];
        for(int i = 0; i < postTest.length; i++) {
            DiskPartition part = parts.get(i);
            File outFile = new File(outputDir, String.format("%06d.out", part.getPartitionNumber()));
            logger.info(String.format("New output file %s", outFile));
            outputs[i] = new PrintWriter(new FileWriter(outFile));
            postTest[i] = new PostTest(engine, part, doFields, terms, caseSensitive, getWords, quiet, outputs[i], latch);
            partThreads[i] = new Thread(postTest[i]);
            partThreads[i].setDaemon(true);
            partThreads[i].setName("PostTest-" + i);
            partThreads[i].start();
        }
        
        try {
            latch.await();
        } catch(InterruptedException ex) {
        }
        
        for(PrintWriter output : outputs) {
            output.close();
        }
        
        NanoWatch fw = new NanoWatch();
        NanoWatch iw = new NanoWatch();
        long docsIterated = 0;
        for(PostTest pt : postTest) {
            fw.accumulate(pt.fw);
            iw.accumulate(pt.iw);
            docsIterated += pt.docsIterated;
        }
        
        logger.info(String.format("Iterated %,d docs in %,.2fms %,.2fdocs/ms."
                + " Average iteration time %,.2fns/docs", 
                          docsIterated, 
                          iw.getTimeMillis(),
                          docsIterated / iw.getTimeMillis(), 
                iw.getTimeNanos() / (double) docsIterated));
        logger.info(String.format("Found %,d docs in %,.2fms. %,.2f finds/ms. "
                + "Average findID time %,.2fns", 
                fw.getClicks(), fw.getTimeMillis(), 
                fw.getClicks() / fw.getTimeMillis(),
                fw.getAvgTime()));
        
        engine.close();
    }
}