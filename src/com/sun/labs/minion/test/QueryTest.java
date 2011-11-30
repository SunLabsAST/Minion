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

import com.sun.labs.minion.Document;
import com.sun.labs.minion.engine.SearchEngineImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.lexmorph.LiteMorph;
import com.sun.labs.minion.lexmorph.LiteMorph_en;
import com.sun.labs.minion.retrieval.DocumentVectorImpl;
import com.sun.labs.minion.retrieval.ResultImpl;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.Util;

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.IndexableMap;
import com.sun.labs.minion.ParseException;
import com.sun.labs.minion.Passage;
import com.sun.labs.minion.PassageBuilder;
import com.sun.labs.minion.PassageHighlighter;
import com.sun.labs.minion.Posting;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.Searcher;
import com.sun.labs.minion.SimpleHighlighter;
import com.sun.labs.minion.TermStats;
import com.sun.labs.minion.TextHighlighter;
import com.sun.labs.minion.WeightedFeature;
import com.sun.labs.minion.indexer.DiskField;
import java.io.File;
import java.net.URL;
import com.sun.labs.minion.indexer.MetaFile;
import com.sun.labs.minion.indexer.dictionary.TermStatsDiskDictionary;
import com.sun.labs.util.LabsLogFormatter;
import com.sun.labs.util.command.CommandInterface;
import com.sun.labs.util.command.CommandInterpreter;
import java.util.Arrays;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * QueryTest is a set of utility methods and a query interface to operate on
 * an index.  Run QueryTest with the "-d <index>" option specifying an index
 * directory.  Queries can be issued by simply typing the query and hitting
 * return.  By default, the top 10 results are displayed.  Each line will show
 * the partition number, document ID (within the partition), and the document
 * key that was specified at indexing time.  The data displayed can be changed
 * with the ":display" command.  Arguments to the display command can be the
 * names of any saved field or any of the following:
 * </p>
 * <ul>
 * <li>score: the score, between 0 and 1, of the result
 * <li>dockey: the key of the document
 * <li>partNum: the partition number that the result occurs in
 * <il>docID: the id number (within the partition) of the document
 * <li>dvl: the length of the document vector
 * </ul>
 * <p>
 * In addition, you can put "\n" or "\t" for a newline or a space.  Arguments
 * should be separated with spaces.  The number of results displayed can be
 * changed with the ":n" command.
 * </p>
 * <p>
 * By default, results are sorted according to their score.  Results with the
 * highest score are printed first.  The sorting specification can be changed
 * by using the ":sort" command and giving it a sorting spec, as defined
 * in the {@link com.sun.labs.minion.SearchEngine} interface.  To get the
 * default sort back, use ":sort -score".
 * </p>
 * <p>
 * To see the full set of commands available, use the ":help" command.  To
 * exit QueryTest cleanly, use the ":q" command.
 * <p>
 */
public class QueryTest extends SEMain {

    protected static final Logger logger = Logger.getLogger(QueryTest.class.getName());
    
    private CommandInterpreter shell = new CommandInterpreter();

    protected boolean displayPassage;

    protected boolean complexPassDisplay;

    protected boolean wildCaseSensitive;

    protected boolean vectorLengthNormalization;

    protected DisplaySpec displaySpec;

    protected String sortSpec;

    protected String prompt;

    protected Searcher searcher;

    protected SearchEngineImpl engine;

    protected PartitionManager manager;

    protected LiteMorph morphEn;

    protected int nHits;

    protected long totalTime, nQueries;

    protected Searcher.Grammar grammar = Searcher.Grammar.STRICT;

    protected Searcher.Operator queryOp = Searcher.Operator.AND;

    protected boolean ttyInput = true;

    private SimpleHighlighter shigh;

    public QueryTest(URL cmFile, String indexDir, String engineType,
                     String displayFields, String displayFormat,
                     String sortSpec) throws java.io.IOException,
            SearchEngineException {
        if(engineType == null) {
            engine =
                    (SearchEngineImpl) SearchEngineFactory.getSearchEngine(
                    indexDir,
                    cmFile);
        } else {
            engine =
                    (SearchEngineImpl) SearchEngineFactory.getSearchEngine(
                    indexDir,
                    engineType, cmFile);
        }
        manager = engine.getManager();
        searcher = engine;
        morphEn = LiteMorph_en.getMorph();
        displaySpec = new DisplaySpec(displayFields, displayFormat);
        this.sortSpec = sortSpec;
        displayPassage = false;
        shigh = new SimpleHighlighter("<font color=\"#FF0000\">", "</font>",
                                      "<b>", "</b>");
        addCommands();
        setPrompt();
        shell.setDefaultCommand("q");
        shell.setParseQuotes(false);
    }

    public static void usage() {
        System.err.println(
                "Usage: java com.sun.labs.minion.test.QueryTest [options] -d indexDir");
    }

    public void stats() {
        if(ttyInput) {

            shell.out.println("Status:");

            //
            // Collection stats.
            List<DiskPartition> activeParts = manager.getActivePartitions();
            shell.out.format(" %d active partitions: ", activeParts.size());
            for(DiskPartition p : activeParts) {
                shell.out.format("%d (%d) ", p.getPartitionNumber(), p.getNDocs());
            }
            shell.out.println("");
            shell.out.println(" Sorting specification is: " + sortSpec);
            shell.out.println(" Display specification is: " + displaySpec);
            shell.out.println(" Partitions have:\n" + "  " + manager.getNDocs()
                           + " documents\n" + "  " + manager.getNTokens()
                           + " tokens\n" + "  " + manager.getNTerms() + " terms");
        }
    }

    /**
     * Sets the prompt to show which of the toggles are on.
     */
    public void setPrompt() {

        StringBuilder v = new StringBuilder();

        if(displayPassage) {
            v.append('d');
        }

        if(complexPassDisplay) {
            v.append('c');
        }

        v.append("> ");
        shell.setPrompt(v.toString());
    }

    public void displayResults(ResultSet set) {
        displayResults("", set);
    }

    /**
     * Displays the top n results from a set of results.
     */
    public void displayResults(String prefix, ResultSet set) {

        List results;

        try {
            results = set.getResults(0, nHits);
        } catch(SearchEngineException se) {
            logger.log(Level.SEVERE, "Error getting search results", se);
            return;
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error getting search results", e);
            return;
        }

        shell.out.println("Query took: " + set.getQueryTime() + " ms");
        totalTime += set.getQueryTime();
        nQueries++;

        shell.out.println(results.size() + "/" + set.size() + "/"
                       + set.getNumDocs());

        for(Iterator i = results.iterator(); i.hasNext();) {

            Result r = (Result) i.next();

            displaySpec.format(prefix, r);

            //
            // Check for passages.
            if(displayPassage) {
                displaySpec.displayPassages(r);
            }
        }
    }

    protected void queryStats() {
        QueryStats eqs = engine.getQueryStats();
        if(eqs.queryW.getClicks() > 0) {
            shell.out.println(eqs.dump());
        }
    }

    private static String join(String[] a, String j) {
        return join(a, 0, a.length, j);
    }
    
    private static String join(String[] a, int start, int end, String j) {
        StringBuilder sb = new StringBuilder();
        for(int i = start; i < end; i++) {
            if(i > 0) {
                sb.append(j);
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }

    private void addCommands() {
        
        shell.addGroup("Query", "Commands for specifying queries and their result formats");
        shell.addGroup("Info", "Information about the index");
        shell.addGroup("Terms", "Information about specific terms");
        shell.addGroup("Maintenance", "Commands that maintain the index");
        
        shell.add("q", "Query", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                //
                // Run a query.
                String q = join(args, 1, args.length, " ");
                try {
                    ResultSet r = searcher.search(q, sortSpec, queryOp, grammar);
                    displayResults(r);
                } catch(Exception ex) {
                    logger.log(Level.SEVERE, String.format("Error running search"), ex);
                }
                return "";
            }

            public String getHelp() {
                return "Run a query";
            }
        });
        
        shell.addAlias("q", "Query");
        
        shell.add("stats", "Info", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                 stats();
                 return "";
            }

            public String getHelp() {
                return "Show collection statistics";
            }
        });
        
        shell.add("qop", "Query", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    return String.format("Query op is: %s", queryOp);
                }
                try {
                    queryOp = Searcher.Operator.valueOf(args[1].toUpperCase());
                } catch(IllegalArgumentException ex) {
                    shell.out.format("Didn't recognize operator, valid options are: %s\n",
                            Arrays.toString(Searcher.Operator.values()));
                }
                return String.format("Query op set to %s", queryOp);
            }

            public String getHelp() {
                return String.format("qop - Set the default query operator values: %s", 
                        Arrays.toString(Searcher.Operator.values()));
            }
        });
        
        shell.add("deff", "Query", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    return "Must specify default fields";
                }
                QueryConfig qc = engine.getQueryConfig();
                for(int i = 1; i < args.length; i++) {
                    qc.addDefaultField(args[i]);
                }
                engine.setQueryConfig(qc);
                return "";
            }

            public String getHelp() {
                return "field [field]...Set the default search fields";
            }
        });
        
        shell.add("qstats", "Query", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                queryStats();
                return "";
            }

            public String getHelp() {
                return "Get the combined query stats for this session";
            }
        });
        
        shell.add("nd", "Query", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                return String.format("%d docs", engine.getNDocs());
            }

            public String getHelp() {
                return "Get the number of documents in the engine";
            }
        });
        
        shell.add("ts", "Terms", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                
                if(args.length == 1) {
                    return "Must specify one or more terms";
                }
                
                TermStatsDiskDictionary tsd = manager.getTermStatsDict();
                StringBuilder sb = new StringBuilder();
                for(int i = 1; i < args.length; i++) {
                    String word = args[i];
                    TermStats ts = tsd.getTermStats(word);
                    sb.append(String.format("%s: %s", word, ts));
                }                
                return sb.toString();
            }

            public String getHelp() {
                return "Get the overall term statistics for one or more terms";
            }
        });
        
        shell.add("n", "Query", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    return "Need to specify the number of hits";
                }
                nHits = Integer.parseInt(args[1]);
                return String.format("Set number of hits to %d", nHits);
            }

            public String getHelp() {
                return "num - Set the number of hits to return";
            }
        });
        
        shell.add("sort", "Query", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                sortSpec = join(args, 1, args.length, " ");
                return "";
            }

            public String getHelp() {
                return "sortspec - Set the sorting spec";
            }
        });
        
        shell.add("display", "Query", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                displaySpec.setDisplayFields(Arrays.copyOfRange(args, 1, args.length));
                return String.format("Display spec is %s", displaySpec);
            }

            public String getHelp() {
                return "Set the display specification for hits";
            }
        });
        
        shell.add("format", "Query", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                
                if(args.length == 1) {
                    return "Must specify display string";
                }
                displaySpec.setFormatString(join(args, 1, args.length, " "));
                return String.format("Display spec is %s", displaySpec);
            }

            public String getHelp() {
                return "Set the display format for hits";
            }
        });
        
        shell.add("gram", "Query", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    return "Currently using " + grammar;
                }
                try {
                    grammar = Searcher.Grammar.valueOf(args[1].toUpperCase());
                } catch(IllegalArgumentException ex) {
                    return String.format(
                            "Unrecognized grammar, valid values are: %s\n",
                            Arrays.toString(Searcher.Grammar.values()));
                }
                return "Grammar set to " + grammar;
            }

            public String getHelp() {
                return "Set the default grammar to use for parsing queries";
            }
        });

        shell.add("term", "Terms", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    return "Must specify at least one term";
                }
                
                for(int i = 1; i < args.length; i++) {
                    String term = CharUtils.decodeUnicode(args[i]);
                    for(DiskPartition p : manager.getActivePartitions()) {
                        for(DiskField df : ((InvFileDiskPartition) p).getDiskFields()) {
                            if(!df.getInfo().hasAttribute(FieldInfo.Attribute.INDEXED)) {
                                continue;
                            }
                            Entry e = df.getTerm(term, true);
                            if(e == null) {
                                shell.out.format("%s field: %s null\n", p,
                                        df.getInfo().getName());
                            } else {
                                shell.out.format("%s field: %s %s (%s) id: %d n: %d\n",
                                        p,
                                        df.getInfo().getName(),
                                        e.getName(),
                                        Util.toHexDigits(e.getName().toString()),
                                        e.getID(),
                                        e.getN());
                            }
                        }
                    }
                }
                return "";
            }

            public String getHelp() {
                return "[term...] - Perform a case sensitive term lookup for one or more terms";
            }
        });

        shell.add("termi", "Terms", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    return "Must specify one or more terms";
                }
                
                for(int i = 1; i < args.length; i++) {
                    String term = CharUtils.decodeUnicode(args[i]);
                    for(DiskPartition p : manager.getActivePartitions()) {
                        for(DiskField df : ((InvFileDiskPartition) p).getDiskFields()) {
                            if(!df.getInfo().hasAttribute(FieldInfo.Attribute.INDEXED)) {
                                continue;
                            }
                            Entry e = df.getTerm(term, false);
                            if(e == null) {
                                shell.out.format("%s field: %s null\n", p,
                                        df.getInfo().getName());
                            } else {
                                shell.out.format("%s field: %s %s (%s) %d\n", p,
                                        df.getInfo().getName(),
                                        e.getName(), Util.toHexDigits(e.getName().
                                        toString()), e.getN());
                            }
                        }
                    }

                }
                return "";
            }

            public String getHelp() {
                return "[term...] Perform a case insensitive lookup for one or more terms";
            }
        });
        
        shell.add("wild", "Terms", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    return "Must specify one or more patterns";
                }

                for(int i = 1; i < args.length; i++) {
                    String pat = args[i];
                    for(DiskPartition p : manager.getActivePartitions()) {
                        for(DiskField df : ((InvFileDiskPartition) p).getDiskFields()) {

                            if(!df.getInfo().hasAttribute(
                                    FieldInfo.Attribute.TOKENIZED)) {
                                continue;
                            }
                            shell.out.format("Field: %s\n", df.getInfo().getName());
                            List<QueryEntry> entries = df.getWildcardMatches(pat,
                                    wildCaseSensitive,
                                    -1, -1);
                            if(entries.isEmpty()) {
                                shell.out.format("No matches\n");
                            } else {
                                shell.out.format("%d token matches\n", entries.size());
                                for(QueryEntry e : entries) {
                                    shell.out.format(" %s (%d)\n", e.getName(),
                                            e.getN());
                                }
                            }
                        }
                    }
                }
                return "";
            }

            public String getHelp() {
                return "[expr] [expr...] Match one or more wild card expressions against fields";
            }
        });
        
        shell.add("morph", "Terms", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    return "Must specify one or more terms";
                }

                for(int i = 1; i < args.length; i++) {
                    Set<String> morphs = morphEn.variantsOf(args[i]);
                    List<String> variants = new ArrayList<String>();
                    variants.add(args[i]);
                    variants.addAll(morphs);
                    List<DiskPartition> parts = manager.getActivePartitions();
                    for(DiskPartition p : parts) {
                        shell.out.format("Partition %d variants\n", p.getPartitionNumber());
                        for(DiskField df :
                                ((InvFileDiskPartition) p).getDiskFields()) {
                            int docFreq = 0;
                            int totalVariants = 0;
                            shell.out.format("Field: %s\n", df.getInfo().getName());
                            for(String variant : variants) {
                                Entry e = df.getTerm(variant, true);
                                if(e != null) {
                                    shell.out.format(" %s (%d)\n", e.getName(), e.getN());
                                    docFreq += e.getN();
                                    totalVariants++;
                                }
                            }
                            shell.out.format("Total document frequency: %7d\n", docFreq);
                            shell.out.format("Total variants:           %7d\n", totalVariants);
                        }
                    }
                }
                return "";
            }

            public String getHelp() {
                return "[term] [term...] - Get morphological variants for one or more terms";
            }
        });

        shell.add("rts", "Maintenance", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                manager.recalculateTermStats();
                return "Term stats recalculated";
            }

            public String getHelp() {
                return "Recalculate global term statistics";
            }
        });

        shell.add("fields", "Info", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                MetaFile mf = manager.getMetaFile();
                shell.out.println(mf.toString());
                return "";
            }

            public String getHelp() {
                return "Print the defined fields";
            }
        });
        
        shell.add("del", "Maintenance", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    return "Must specify keys to delete";
                }
                
                StringBuilder sb = new StringBuilder();
                
                for(int i = 1; i < args.length; i++) {
                    String key = args[i];
                    manager.deleteDocument(key);
                    sb.append(String.format("Deleted %s\n", key));
                }
                return sb.toString();
            }

            public String getHelp() {
                return "key [key]... Delete a document by key";
            }
        });
        
        shell.add("delq", "Maintenance", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                String q = join(args, 1, args.length, " ");
                try {
                    //
                    // Run the query
                    ResultSet rs = searcher.search(q, sortSpec,
                            Searcher.Operator.AND, grammar);
                    displayResults(rs);
                    BufferedReader br =
                            new BufferedReader(new InputStreamReader(System.in));
                    shell.out.print(
                            "Delete the documents matching this query? (y/n) ");
                    String response = br.readLine();
                    if(response.toLowerCase().equals("y") || response.toLowerCase().
                            equals("yes")) {
                        List<Result> results = rs.getAllResults(false);
                        for(Result r : results) {
                            manager.deleteDocument(r.getKey());
                        }
                    }
                } catch(SearchEngineException se) {
                    logger.log(Level.SEVERE, "Error running search", se);
                } catch(IOException e) {
                    logger.log(Level.SEVERE, "Failed to read answer", e);
                }
                return "Deleted documents";
            }

            public String getHelp() {
                return "Delete documents that match a given query";
            }
        });
        
        shell.add("deld", "Maintenance", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {

                if(args.length < 3) {
                    return "Must specify partition number and document IDs";
                }

                int part = Integer.parseInt(args[1]);
                for(DiskPartition p : manager.getActivePartitions()) {
                    if(p.getPartitionNumber() == part) {
                        for(int i = 2; i < args.length; i++) {
                            boolean success = p.deleteDocument(Integer.parseInt(
                                    args[i]));
                            shell.out.format("Deletion of %d in %d returned %s ",
                                    Integer.parseInt(args[i]),
                                    p.getPartitionNumber(), success);
                        }
                    }
                }

                return "";
            }

            public String getHelp() {
                return "Delete documents from a numbered partition by document ID";
            }
        });
        
        shell.add("get", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 4) {
                    return "Must specify a partition number, document ID and a list of fields";
                }
                
                int pn = Integer.parseInt(args[1]);
                int d = Integer.parseInt(args[2]);
                for(DiskPartition p : manager.getActivePartitions()) {
                    if(p.getPartitionNumber() == pn) {

                        for(int i = 3; i < args.length; i++) {
                            DiskField df = ((InvFileDiskPartition) p).getDF(
                                    args[i]);
                            Object v = df.getFetcher().fetch(d);
                            shell.out.format("%s: %s", args[i], v);
                        }
                    }
                }
                return "";
            }


            public String getHelp() {
                return "partNum docID field [field...] - Get field values from a particular doc";
            }
        });
        
        shell.add("merge", "Maintenance", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    ci.out.println("Merging all partitions");
                    manager.mergeAll();
                    return "Merged all partitions";
                } else {
                    ArrayList<Integer> parts = new ArrayList<Integer>();
                    for(int i = 1; i < args.length; i++) {
                        parts.add(Integer.parseInt(args[i]));
                    }

                    PartitionManager.Merger merger = manager.getMergerFromNumbers(parts);
                    merger.merge();
                    return "Merged " + parts;
                }
            }

            public String getHelp() {
                return "pn [pn] ... - Merge a number of partitions";
            }
        });
        
        shell.add("sim", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 3) {
                    return "Must specify two document keys";
                }
                String key1 = args[1];
                String key2 = args[2];

                DocumentVector dv1 = engine.getDocumentVector(key1);
                DocumentVector dv2 = engine.getDocumentVector(key2);

                if(dv1 == null) {
                    shell.out.println("No such doc: " + key1);
                } else if(dv2 == null) {
                    shell.out.println("No such doc: " + key2);
                } else {
                    shell.out.println("Similarity score: " + dv1.getSimilarity(dv2));
                }
                
                return "";
            }

            public String getHelp() {
                return "Compute the similarity between two documents";
            }
        });
        
        shell.add("findsim", "Query", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 2) {
                    return "Must specify document key";
                }
                
                String key = args[1];
                double skim = args.length > 2 ? Double.parseDouble(args[2]) : 1.0;
                DocumentVector dv = engine.getDocumentVector(key);
                if(dv != null) {
                    ResultSet rs = ((DocumentVectorImpl) dv).findSimilar("-score",
                            skim);
                    displayResults(rs);
                } else {
                    shell.out.println("No such doc: " + key);
                }
                return "";
            }

            public String getHelp() {
                return "key [skim] Find documents similar to the given one";
            }
        });
        
        shell.add("dv", "Terms", new CommandInterface() {

            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 3) {
                    return "Must specify field and document key";
                }
                
                String field = args[1];
                String dockey = args[2];
                DocumentVector dv = engine.getDocumentVector(dockey, field);
                if(dv == null) {
                    shell.out.println("Unknown key: " + args[1]);
                } else {
                    dumpDocVectorByWeight(dv);
                }
                return "";
            }

            public String getHelp() {
                return "field dockey - Show the document vector for a given field in a given document";
            }
        });
    }

    private void dumpDocVectorByWeight(DocumentVector dv) {
        SortedSet set = ((DocumentVectorImpl) dv).getSet(); // set sorted by
        // name

        //
        // sort by weight:
        SortedSet w = new TreeSet(new Comparator() {

            public int compare(Object o1, Object o2) {
                if(o1.equals(o2)) {
                    return 0;
                }
                if(((WeightedFeature) o1).getWeight() > ((WeightedFeature) o2).
                        getWeight()) {
                    return -1;
                }
                return 1;
            }

            public boolean equals(Object o) {
                return false;
            }
        });
        w.addAll(set);
        float sum = 0;
        for(Iterator it = w.iterator(); it.hasNext();) {
            WeightedFeature wf = (WeightedFeature) it.next();
            sum += wf.getWeight() * wf.getWeight();
            shell.out.println(wf.toString());
        }
        shell.out.format("length: %.2f\n", Math.sqrt(sum));
    }

    public static void main(String[] args) throws java.io.IOException,
            NumberFormatException, SearchEngineException {

        if(args.length == 0) {
            usage();
            return;
        }

        String flags = "d:l:f:x:t:";
        Getopt gopt = new Getopt(args, flags);
        String inputFile = null;
        int c;
        String indexDir = null;
        URL cmFile = null;
        String engineType = null;

        if(args.length == 0) {
            usage();
            return;
        }

        Thread.currentThread().setName("QueryTest");

        for(Handler h : Logger.getLogger("").getHandlers()) {
            h.setFormatter(new LabsLogFormatter());
            h.setLevel(Level.ALL);
        }

        //
        // Handle the options.
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'l':
                    Logger.getLogger("").setLevel(Level.parse(gopt.optArg));
                    break;

                case 'f':
                    inputFile = gopt.optArg;
                    break;

                case 'x':
                    cmFile = (new File(gopt.optArg)).toURI().toURL();
                    break;

                case 't':
                    engineType = gopt.optArg;
                    break;
            }
        }

        //
        // Setup logging.
        QueryTest qt = new QueryTest(
                cmFile,
                indexDir,
                engineType,
                "partNum docID dockey",
                "%6d%10d %s",
                "-score");

        qt.nHits = 10;
        qt.stats();
        qt.shell.run();
        qt.shell.out.println("Average time over " + qt.nQueries + " queries: "
                       + (float) qt.totalTime / (float) qt.nQueries + " ms");

        qt.engine.close();

        qt.queryStats();
    }

    /**
     * A class that is used to format for display the information from a
     * document hit.
     */
    protected class DisplaySpec {

        /**
         * The fields we'll display.
         */
        protected String[] fields;

        /**
         * The values we'll print.
         */
        protected Object[] vals;

        protected TextHighlighter th = new TextHighlighter();

        protected String formatString;
        
        public DisplaySpec(String displayFields, String formatString) {
            setDisplayFields(displayFields);
            setFormatString(formatString);
        }

        /**
         * Makes a display spec from a string representation. The representation
         * is a comma separated list of field names. In addition to the names
         * defined in a given index, the user may use the sequences
         * <code>\n</code> and <code>\t</code> to display newlines or tabs
         * in the output.
         */
        public DisplaySpec(String[] displayFields, String formatString) {
            setDisplayFields(displayFields);
            setFormatString(formatString);
        }
        public void setDisplayFields(String displayFields) {
            setDisplayFields(displayFields.split("\\s+"));
        }

        public void setDisplayFields(String[] fields) {
            
            //
            // Split the input at commas.
            StringBuilder df = new StringBuilder();
            this.fields = fields;
            vals = new Object[fields.length];
            for(int i = 0; i < fields.length; i++) {

                String fn = fields[i];
                FieldInfo fi = engine.getFieldInfo(fn);
                if(fi == null || !fi.hasAttribute(FieldInfo.Attribute.SAVED)) {
                    if(fn.equals("dockey") || fn.equals("indexName")) {
                        df.append("%s ");
                    } else if(fn.equals("score") || fn.equals("dvl")) {
                        df.append("%.3f ");
                    } else if(fn.equals("docID") || fn.equals("partNum")) {
                        df.append("%d ");
                    } else if(fn.startsWith("v:")) {
                        df.append("%s ");
                    } else {
                        logger.warning(String.format(
                                "Display field %s is not saved", fields[i]));
                        df.append("%s ");
                    }
                } else {
                    switch(fi.getType()) {
                        case DATE:
                        case STRING:
                            df.append("%s ");
                            break;
                        case INTEGER:
                            df.append("%d ");
                            break;
                        case FLOAT:
                            df.append("%.3f ");
                            break;
                    }
                }
            }
            setFormatString(df.toString());

        }

        public void setFormatString(String formatString) {
            this.formatString = formatString;
        }

        /**
         * Format a result.
         * @param r the result to format.
         */
        public void format(Result r) {
            format("", r);
        }

        /**
         * Format a result.
         */
        public void format(String prefix, Result r) {

            shell.out.print(prefix);

            Document d = null;

            //
            // Display the fields that the user asked for.
            // We'll catch a few special cases.
            for(int i = 0; i < fields.length; i++) {

                String fn = fields[i];

                vals[i] = null;

                if(fn.equals("dockey")) {
                    vals[i] = r.getKey();
                } else if(fn.equals("score")) {
                    vals[i] = r.getScore();
                } else if(fn.equals("indexName")) {
                    vals[i] = r.getIndexName();
                } else if(fn.equals("docID")) {
                    vals[i] = ((ResultImpl) r).getDocID();
                } else if(fn.equals("partNum")) {
                    vals[i] = ((ResultImpl) r).getPartNum();
                } else if(fn.startsWith("v:")) {
                    fn = fn.substring(2);
                    if(d == null) {
                        d = r.getDocument();
                    }
                    List<Posting> post = d.getPostings(fn);
                    if(post != null) {
                        vals[i] = post.toString();
                    }
                } else {
                    List val = r.getField(fn);
                    if(val.isEmpty()) {
                        vals[i] = null;
                    } else if(val.size() == 1) {
                        vals[i] = val.get(0);
                    } else {
                        vals[i] = val.toString();
                    }
                }
            }
            shell.out.format(formatString, vals);
            shell.out.println("");
        }

        /**
         * Displays the passages associated with a given result.
         */
        protected void displayPassages(Result r) {

            //
            // Get the document key, which is the filename in our case.
            String filename = r.getKey();
            String enc = (String) r.getSingleFieldValue("enc");

            //
            // If we got null or the empty string for the filename, then
            // things are very weird and we'll just quit.
            if(filename == null || filename.equals("")) {
                return;
            }

            //
            // If the encoding is empty, we'll use null, which will give us
            // the default encoding.
            if(enc != null && enc.equals("")) {
                enc = null;
            }

            //
            // Make a document out of the filename, so that we can
            // highlight it.
            IndexableMap doc =
                    (IndexableMap) makeDocument(new IndexableFile(filename, enc));
            PassageBuilder pb = r.getPassageBuilder();
            if(pb == null) {
                return;
            }
            PassageHighlighter ph = new TextHighlighter();

            if(complexPassDisplay) {

                //
                // Set up some fields for highlighting. These are the kinds of
                // fields that we can parse out of HTML files.
                pb.addPassageField(null, Passage.Type.UNIQUE, 10,
                                   256, true);
                pb.addPassageField("TItle", Passage.Type.JOIN, -1, 256,
                                   true);
                pb.addPassageField("to", Passage.Type.JOIN, -1, 256, true);
                pb.addPassageField("from", Passage.Type.JOIN, -1,
                                   256, true);
                pb.addPassageField("SUBJECT", Passage.Type.JOIN, -1, 256,
                                   true);
                pb.addPassageField("h1", Passage.Type.JOIN, -1, 256, true);
                pb.addPassageField("h2", Passage.Type.JOIN, -1, 256, true);
                Map mp = pb.getPassages(doc.getMap(), 10, 256, true);

                for(Iterator i = mp.entrySet().iterator(); i.hasNext();) {
                    Map.Entry me = (Map.Entry) i.next();
                    List passages = (List) me.getValue();

                    if(passages != null && passages.size() > 0) {
                        shell.out.println("Hits from " + me.getKey());
                        int k = 0;
                        for(Iterator p = passages.iterator(); p.hasNext() && k
                                                                             < 4;
                                k++) {
                            Passage pass = (Passage) p.next();
                            pass.highlight(ph, true);
                            String[] mTerms = pass.getMatchingTerms();
                            shell.out.format("  %.3f", pass.getScore());
                            for(int l = 0; l < mTerms.length; l++) {
                                shell.out.print(" " + mTerms[l]);
                            }
                            shell.out.println("");
                            String hp = pass.getHLValue(true);
                            hp = reformat(hp.replace('\n', ' '), "   ", 72);
                            shell.out.println(hp);
                        }
                    }
                }
            } else {
                List p = pb.getPassages(doc.getMap(), 10, 512);
                int n = 0;
                for(Iterator i = p.iterator(); i.hasNext() && n < 4;) {
                    Passage pass = (Passage) i.next();
                    pass.highlight(shigh, true);
                    String[] mTerms = pass.getMatchingTerms();
                    shell.out.format("  %.3f", pass.getScore());
                    for(int l = 0; l < mTerms.length; l++) {
                        shell.out.print(" " + mTerms[l]);
                    }
                    shell.out.println("");
                    String hp = pass.getHLValue(true);
                    hp = reformat(hp.replace('\n', ' '), "   ", 72);
                    shell.out.println(hp);
                    n++;
                }

                if(n < p.size()) {
                    shell.out.println("  " + (p.size() - n) + " passages not shown");
                }
            }
        }

        /**
         * Reformats a string to fit within a given number of columns with a
         * given prefix on each line.
         */
        public String reformat(String text, String prefix, int column) {
            StringBuilder output = new StringBuilder();
            output.append(prefix);
            int currCol = prefix.length();
            StringTokenizer tok = new StringTokenizer(text);
            while(tok.hasMoreTokens()) {
                String curr = tok.nextToken();
                if(currCol + curr.length() >= column) {
                    output.append("\n").append(prefix);
                    currCol = prefix.length();
                }

                output.append(curr).append(" ");
                currCol += curr.length() + 1;
            }
            return output.toString();
        }

        @Override
        public String toString() {
            return "fields: " + Util.toString(fields) + " format: \""
                   + formatString + "\"";
        }
    }
}
