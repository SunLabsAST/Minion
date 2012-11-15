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
import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.Facet;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.IndexableMap;
import com.sun.labs.minion.Passage;
import com.sun.labs.minion.PassageBuilder;
import com.sun.labs.minion.PassageHighlighter;
import com.sun.labs.minion.Posting;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.Searcher;
import com.sun.labs.minion.SimpleHighlighter;
import com.sun.labs.minion.Stemmer;
import com.sun.labs.minion.TermStats;
import com.sun.labs.minion.TextHighlighter;
import com.sun.labs.minion.WeightedFeature;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.Field;
import com.sun.labs.minion.indexer.Field.DictionaryType;
import com.sun.labs.minion.indexer.MetaFile;
import com.sun.labs.minion.indexer.dictionary.Dictionary;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.TermStatsDiskDictionary;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.entry.TermStatsQueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.PostingsIteratorWithPositions;
import com.sun.labs.minion.lexmorph.LiteMorph;
import com.sun.labs.minion.lexmorph.LiteMorph_en;
import com.sun.labs.minion.retrieval.AbstractDocumentVector;
import com.sun.labs.minion.retrieval.QueryTermDocStats;
import com.sun.labs.minion.retrieval.QueryTermStats;
import com.sun.labs.minion.retrieval.ResultImpl;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.SortSpec;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.retrieval.facet.Collapser;
import com.sun.labs.minion.retrieval.facet.ProgressiveDateCollapser;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.FileWriteableBuffer;
import com.sun.labs.util.LabsLogFormatter;
import com.sun.labs.util.NanoWatch;
import com.sun.labs.util.command.CommandInterface;
import com.sun.labs.util.command.CommandInterpreter;
import com.sun.labs.util.command.CompletorCommandInterface;
import com.sun.labs.util.command.EnumCompletor;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import jline.ClassNameCompletor;
import jline.Completor;
import jline.FileNameCompletor;
import jline.NullCompletor;
import jline.SimpleCompletor;

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
    
    private CommandInterpreter shell;

    protected boolean displayPassage;

    protected boolean complexPassDisplay;

    protected boolean wildCaseSensitive;

    protected boolean vectorLengthNormalization;

    protected DisplaySpec displaySpec;

    /**
     * A sorting specification for result sets.
     */
    protected SortSpec resultsSortSpec;

    /**
     * The string representation of the results sorting specification.
     */
    protected String sortSpecString;

    /**
     * A sorting specification for faceting.
     */
    protected SortSpec facetSortSpec;
    
    /**
     * The string representation of the sorting specification.
     */
    protected String facetSpecString;
    
    protected String prompt;

    protected Searcher searcher;

    protected SearchEngineImpl engine;

    protected PartitionManager manager;

    protected LiteMorph morphEn;

    protected int nHits = 10;
    
    protected int nFacets = 10;
    
    protected long totalTime, nQueries;

    protected Searcher.Grammar grammar = Searcher.Grammar.STRICT;

    protected Searcher.Operator queryOp = Searcher.Operator.AND;

    protected boolean ttyInput = true;

    private SimpleHighlighter shigh;
    
    private ResultSetImpl lastResultSet;

    public QueryTest(URL cmFile, String indexDir, String engineType,
                     String inputFile,
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
        init(engine, displayFields, displayFormat, sortSpec);
        shell = new CommandInterpreter(inputFile);
        addCommands(null, shell);
        setPrompt();
        shell.setDefaultCommand("q");
        shell.setParseQuotes(false);
   }

    public QueryTest(SearchEngineImpl engine,
                     String displayFields,
                     String displayFormat,
                     String sortSpec) throws IOException {
        init(engine, displayFields, displayFormat, sortSpec);
    }

    private void init(SearchEngineImpl engine,
                      String displayFields,
                      String displayFormat,
                      String sortSpec) throws IOException {
        
        this.engine = engine;
        QueryConfig qc = this.engine.getQueryConfig();
        qc.setAllUpperIsCI(false);
        this.engine.setQueryConfig(qc);
        manager = engine.getManager();
        manager.setReapDoesNothing(true);
        searcher = engine;
        morphEn = LiteMorph_en.getMorph();
        displaySpec = new DisplaySpec(displayFields, displayFormat);
        sortSpecString = sortSpec;
        if(sortSpecString != null) {
            this.resultsSortSpec = new SortSpec(engine, sortSpec);
        }
        displayPassage = false;
        shigh = new SimpleHighlighter("<font color=\"#FF0000\">", "</font>",
                                      "<b>", "</b>");
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
                shell.out.format("%d (%d/%d/%d) ", 
                        p.getPartitionNumber(), 
                        p.getMaxDocumentID(),
                        p.getNDocs(), 
                        p.getDelMap().getNDeleted());
            }
            shell.out.println("");
            shell.out.println(" Results sorting specification is: " + resultsSortSpec);
            shell.out.println(" Facet sorting specification is:   " + facetSortSpec);
            shell.out.println(" Display specification is:         " + displaySpec);
            Set<FieldInfo> defaultFields = engine.getDefaultFields();
            StringBuilder sb = new StringBuilder();
            for(FieldInfo df : defaultFields) {
                if(sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(df.getName());
            }
            shell.out.println(" Default search fields:    " + sb);
            shell.out.println(" Partitions have:\n" + "  " + manager.getNDocs()
                           + " documents\n" + "  " + manager.getNTokens()
                           + " tokens\n" + "  " + manager.getNTerms() + " terms");
            
        }
    }

    /**
     * Sets the prompt to show which of the toggles are on.
     */
    private void setPrompt() {

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
        NanoWatch nw = new NanoWatch();

        try {
            nw.start();
            results = set.getResults(0, nHits);
            nw.stop();
        } catch(SearchEngineException se) {
            logger.log(Level.SEVERE, "Error getting search results", se);
            return;
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error getting search results", e);
            return;
        }

        shell.out.format("Query took: %dms (%.2fms to sort)\n", set.getQueryTime(), nw.getAvgTimeMillis());
        totalTime += set.getQueryTime();
        nQueries++;

        shell.out.println(results.size() + "/" + set.size() + "/"
                       + set.getNumDocs());
        
        displayResults(prefix, results);
    }
    
    public void displayResults(List<Result> results) {
        displayResults("", results);
    }
    
    public void displayResults(String prefix, List<Result> results) {

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
            if(i > start) {
                sb.append(j);
            }
            sb.append(a[i]);
        }
        return sb.toString();
    }
    
    private static String prefixCommand(String prefix, String command) {
        return prefix == null ? command : prefix + "-" + command;
    }

    public void addCommands(String prefix, final CommandInterpreter shell) {
        this.shell = shell;
        
        shell.addGroup("Query", "Commands for specifying queries and their result formats");
        shell.addGroup("Info", "Information about the index");
        shell.addGroup("Terms", "Information about specific terms");
        shell.addGroup("Maintenance", "Commands that maintain the index");
        shell.addGroup("Other", "Commands for other things");
        
        shell.add(prefixCommand(prefix, "q"), "Query", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                //
                // Run a query.
                String q = join(args, 1, args.length, " ");
                try {
                    ResultSet r = searcher.search(q, sortSpecString, queryOp, grammar);
                    displayResults(r);
                    lastResultSet = (ResultSetImpl) r;
                } catch(Exception ex) {
                    logger.log(Level.SEVERE, String.format("Error running search"), ex);
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "Run a query";
            }
        });
        
        shell.add(prefixCommand(prefix, "tq"), "Query", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args)
                    throws Exception {
                if(args.length < 4) {
                    return getHelp();
                }
                
                List<String> terms = new ArrayList<String>();
                for(int i = 3; i < args.length; i++) {
                    terms.add(args[i]);
                }
                
                QueryConfig qc = engine.getQueryConfig();
                QueryTermStats qts = engine.getQueryTermStats(qc, terms, args[1], Field.TermStatsType.valueOf(args[2].toUpperCase()));
                ci.out.format("%d documents\n", qts.size());
                for(String term : qts.getQueryTerms()) {
                    int df = qts.getDocumentFrequency(term);
                    ci.out.format("%-10s%4d\n", term, df);
                }
                for(QueryTermDocStats qtds : qts) {
                    ResultImpl ri = qtds.getResult();
                    displaySpec.format("  ", ri);
                    for(QueryTermDocStats.TermStat ts : qtds) {
                        ci.out.format("   %s\n", ts);
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "field termstatstype term [term] ... - Get query term stats for the given terms";
            }
            
            @Override
            public Completor[] getCompletors() {
                return new Completor[]{
                            new FieldCompletor(manager.getMetaFile()),
                            new EnumCompletor(Field.TermStatsType.class),
                            new NullCompletor()
                        };
            }
        });
        
        shell.add(prefixCommand(prefix, "last"), "Query", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] strings)
                    throws Exception {
                if(lastResultSet == null) {
                    return "No previous query";
                }
                
                try {
                    displayResults(lastResultSet);
                } catch(Exception ex) {
                    logger.log(Level.SEVERE, String.format(
                            "Error displaying results"), ex);
                }
                
                return "";
            }

            @Override
            public String getHelp() {
                return "Display the results of the last search query";
            }
        });
        
        shell.add(prefixCommand(prefix, "nf"), "Query", new CommandInterface() {
            @Override
            public String execute(CommandInterpreter ci, String[] args) throws
                    Exception {
                if(args.length == 1) {
                    return "Need to specify the number of facets to show";
                }
                nFacets = Integer.parseInt(args[1]);
                return String.format("Set number of facets to %d", nFacets);
            }

            @Override
            public String getHelp() {
                return "num - Set the number of hits to return";
            }
        });
        
        shell.add(prefixCommand(prefix, "facet"), "Query", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args)
                    throws Exception {
                
                if(args.length < 2) {
                    return getHelp();
                }
                
                int resultsToFacetOn = -1;
                
                if(args.length > 2) {
                    resultsToFacetOn = Integer.parseInt(args[2]);
                }
                
                if(lastResultSet == null) {
                    return "No previous query";
                }
                
                FieldInfo ffi = engine.getFieldInfo(args[1]);
                if(ffi == null) {
                    return "Unknown field " + args[1];
                } else if(!ffi.hasAttribute(FieldInfo.Attribute.SAVED)) {
                    return "Can't facet on unsaved field " + args[1];
                }
                
                Collapser collapser = null;
                if(ffi.getType() == FieldInfo.Type.DATE) {
                    collapser = new ProgressiveDateCollapser();
                }
                
                NanoWatch fw = new NanoWatch();
                fw.start();
                List<Facet> lf = lastResultSet.getTopFacets(args[1], facetSortSpec, nFacets, resultsSortSpec, resultsToFacetOn, collapser);
                fw.stop();
                
                ci.out.format("Found %d facets for %s in %d hits in %.3fms\n", 
                              lf.size(), args[1], lastResultSet.size(), fw.getTimeMillis());
                for(Facet f : lf) {
                    ci.out.print("  ");
                    ci.out.println(f);
                    List<Result> lr = f.getTopResults(1, resultsSortSpec);
                    displayResults("   ", lr);
                }
                
                return "";
            }

            @Override
            public String getHelp() {
                return "field [number of top results to facet on]- Get the facets for a particular field from the last set of search results";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile())
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "wf"), "Query", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args)
                    throws Exception {
                try {
                    Class clazz = Class.forName(args[1]);
                    WeightingFunction wf = (WeightingFunction) clazz.
                            newInstance();
                    QueryConfig qc = engine.getQueryConfig();
                    qc.setWeightingFunction(wf);
                    engine.setQueryConfig(qc);
                    return String.format("Weighting function set to %s", args[1]);
                } catch(ClassCastException ex) {
                    return String.format(
                            "%s is not a weighting function class name", args[1]);
                } catch(Exception ex) {
                    return String.format("Couldn't create %s: %s", args[1], ex);
                }
            }

            @Override
            public String getHelp() {
                return "weighting function classname - Sets the weighting function to be used during querying";
            }

            @Override
            public Completor[] getCompletors() {
                try {
                    return new Completor[] {
                        new ClassNameCompletor()
                    };
                } catch (IOException e) {
                    return new Completor[] {
                        new NullCompletor()
                    };
                }
            }
        });
        
        shell.add(prefixCommand(prefix, "norm"), "Query", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] strings)
                    throws Exception {
                QueryConfig qc = engine.getQueryConfig();
                qc.setNormalizeResults(true);
                engine.setQueryConfig(qc);
                return "Result sets will be normalized";
            }

            @Override
            public String getHelp() {
                return "Turn on normalization for result sets";
            }
        });
        
        shell.add(prefixCommand(prefix, "nonorm"), "Query", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] strings)
                    throws Exception {
                QueryConfig qc = engine.getQueryConfig();
                qc.setNormalizeResults(false);
                engine.setQueryConfig(qc);
                return "Result sets will not be normalized";
            }

            @Override
            public String getHelp() {
                return "Turn off normalization for result sets";
            }
        });
        
        shell.add(prefixCommand(prefix, "stats"), "Info", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                 stats();
                 return "";
            }

            @Override
            public String getHelp() {
                return "Show collection statistics";
            }
        });
        
        shell.add(prefixCommand(prefix, "qop"), "Query", new CompletorCommandInterface() {

            @Override
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

            @Override
            public String getHelp() {
                return String.format("qop - Set the default query operator values: %s", 
                        Arrays.toString(Searcher.Operator.values()));
            }

            @Override
            public Completor[] getCompletors() {
                Searcher.Operator[] ops = Searcher.Operator.values();
                String[] vals = new String[ops.length];
                for (int i = 0; i < ops.length; i++) {
                    vals[i] = ops.toString();
                }
                return new Completor[] {
                    new SimpleCompletor(vals)
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "deff"), "Query", new CompletorCommandInterface() {

            @Override
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

            @Override
            public String getHelp() {
                return "field [field]...Set the default search fields";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile())
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "qstats"), "Query", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                queryStats();
                return "";
            }

            @Override
            public String getHelp() {
                return "Get the combined query stats for this session";
            }
        });
        
        shell.add(prefixCommand(prefix, "nd"), "Query", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                return String.format("%d docs", engine.getNDocs());
            }

            @Override
            public String getHelp() {
                return "Get the number of documents in the engine";
            }
        });
        
        shell.add(prefixCommand(prefix, "ts"), "Terms", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                
                TermStatsDiskDictionary tsd = manager.getTermStatsDict();
                if(args.length == 1) {
                    for(FieldInfo fi : manager.getMetaFile()) {
                        DictionaryIterator<String> di = tsd.iterator(fi);
                        if(di != null) {
                            shell.out.format("Term stats for %s\n", fi.getName());
                            while(di.hasNext()) {
                                Entry<String> e = di.next();
                                shell.out.format("  %s: %d\n", e.getName(), e.getN());
                            }
                        }
                        
                    }
                }
                StringBuilder sb = new StringBuilder();
                for(int i = 1; i < args.length; i++) {
                    String word = args[i];
                    TermStats ts = tsd.getTermStats(word);
                    shell.out.format("Overall %s: %s\n", word, ts);
                    for(FieldInfo fi : manager.getMetaFile()) {
                        TermStatsQueryEntry tsqe = tsd.getTermStats(word, fi);
                        if(tsqe != null) {
                            shell.out.format(" %s: %s\n", fi.getName(), tsqe.getTermStats());
                        }
                    }
                }                
                return sb.toString();
            }

            @Override
            public String getHelp() {
                return "Get the overall term statistics for one or more terms";
            }
        });
        
        shell.add(prefixCommand(prefix, "n"), "Query", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    return "Need to specify the number of hits";
                }
                nHits = Integer.parseInt(args[1]);
                return String.format("Set number of hits to %d", nHits);
            }

            @Override
            public String getHelp() {
                return "num - Set the number of hits to return";
            }
        });
        
        shell.add(prefixCommand(prefix, "sort"), "Query", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                sortSpecString = join(args, 1, args.length, " ");
                resultsSortSpec = new SortSpec(engine, sortSpecString);
                return "";
            }

            @Override
            public String getHelp() {
                return "sortspec - Set the sorting spec for result sets";
            }
        });
        
        shell.add(prefixCommand(prefix, "fsort"), "Query", new CommandInterface() {
            @Override
            public String execute(CommandInterpreter ci, String[] args) throws
                    Exception {
                if(args.length == 1) {
                    facetSortSpec = null;
                } else {
                    facetSpecString = join(args, 1, args.length, ",");
                    facetSortSpec = new SortSpec(engine, facetSpecString);
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "sortspec - Set the sorting spec for facets";
            }
        });

        shell.add(prefixCommand(prefix, "display"), "Query", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                displaySpec.setDisplayFields(Arrays.copyOfRange(args, 1, args.length));
                return String.format("Display spec is %s", displaySpec);
            }

            @Override
            public String getHelp() {
                return "Set the display specification for hits";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile())
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "format"), "Query", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                
                if(args.length == 1) {
                    return "Must specify display string";
                }
                displaySpec.setFormatString(join(args, 1, args.length, " "));
                return String.format("Display spec is %s", displaySpec);
            }

            @Override
            public String getHelp() {
                return "Set the display format for hits";
            }
        });
        
        shell.add(prefixCommand(prefix, "gram"), "Query", new CompletorCommandInterface() {

            @Override
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

            @Override
            public String getHelp() {
                return "Set the default grammar to use for parsing queries";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new EnumCompletor(Searcher.Grammar.class)
                };
            }
        });

        shell.add(prefixCommand(prefix, "term"), "Terms", new CommandInterface() {

            @Override
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

            @Override
            public String getHelp() {
                return "[term...] - Perform a case sensitive term lookup for one or more terms";
            }
        });

        shell.add(prefixCommand(prefix, "termi"), "Terms", new CommandInterface() {

            @Override
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

            @Override
            public String getHelp() {
                return "[term...] Perform a case insensitive lookup for one or more terms";
            }
        });
        
        shell.add(prefixCommand(prefix, "stem"), "Terms", new CommandInterface() {
            @Override
            public String execute(CommandInterpreter ci, String[] args) throws
                    Exception {
                if(args.length == 1) {
                    return "Must specify one or more terms";
                }

                for(int i = 1; i < args.length; i++) {
                    String term = CharUtils.decodeUnicode(args[i]);
                    for(DiskPartition p : manager.getActivePartitions()) {
                        for(DiskField df : ((InvFileDiskPartition) p).
                                getDiskFields()) {
                            if(!df.getInfo().hasAttribute(FieldInfo.Attribute.INDEXED) ||
                                    !df.getInfo().hasAttribute(FieldInfo.Attribute.STEMMED)) {
                                continue;
                            }
                            Stemmer stemmer = df.getStemmer();
                            String stem = stemmer.stem(term);
                            Entry e = df.getStem(stem);
                            if(e == null) {
                                shell.out.format("%s field: %s null\n", p,
                                                 df.getInfo().getName());
                            } else {
                                shell.out.format("%s field: %s %s (%s) %d\n", p,
                                                 df.getInfo().getName(),
                                                 e.getName(), Util.
                                        toHexDigits(e.getName().
                                        toString()), e.getN());
                            }
                        }
                    }

                }
                return "";
            }

            @Override
            public String getHelp() {
                return "[term...] Perform a case insensitive lookup for one or more terms";
            }
        });

        shell.add(prefixCommand(prefix, "termByID"), "Terms", new CompletorCommandInterface() {
            @Override
            public String execute(CommandInterpreter ci, String[] args) throws
                    Exception {
                if(args.length < 5) {
                    return "Must specify a partition, a field, a dictionary type and ID";
                }

                int partNum = Integer.parseInt(args[1]);
                String fieldName = args[2];
                DictionaryType type = DictionaryType.
                        valueOf(args[3].toUpperCase());

                for(DiskPartition p : manager.getActivePartitions()) {
                    if(p.getPartitionNumber() == partNum) {
                        DiskField df = ((InvFileDiskPartition) p).getDF(
                                fieldName);
                        if(df == null) {
                            return String.format("No such field %s", fieldName);
                        }
                        DiskDictionary dict = (DiskDictionary) df.getDictionary(type);
                        if(dict == null) {
                            return String.format(
                                    "No dictionary of type %s for %s", type,
                                                 fieldName);
                        }
                        for(int i = 4; i < args.length; i++) {
                            int termID = Integer.parseInt(args[i]);
                            QueryEntry qe = dict.getByID(termID);
                            if(qe == null) {
                                shell.out.
                                        format(
                                        "No term with ID %d in %s of %s\n", termID,
                                        type, fieldName);
                            } else {
                                shell.out.format("Entry: %s\n", qe);
                            }
                        }
                        return "";
                    }
                }
                return String.format("No such partition: %d", partNum);
            }

            @Override
            public String getHelp() {
                return "partNum field dict id [id]...- Gets a term by term id from a particular dictionary and partition";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new NullCompletor(),
                    new FieldCompletor(manager.getMetaFile()),
                    new EnumCompletor(DictionaryType.class),
                    new NullCompletor()
                };
            }
        });
        
        shell.addAlias("termByID", "tbid");
        
        shell.add(prefixCommand(prefix, "wild"), "Terms", new CommandInterface() {

            @Override
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

            @Override
            public String getHelp() {
                return "[expr] [expr...] Match one or more wild card expressions against fields";
            }
        });
        
        shell.add(prefixCommand(prefix, "morph"), "Terms", new CommandInterface() {

            @Override
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

            @Override
            public String getHelp() {
                return "[term] [term...] - Get morphological variants for one or more terms";
            }
        });
        
        shell.add(prefixCommand(prefix, "di"), "Terms", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length != 4) {
                    return "Must specify a partition, a field and a dictionary type";
                }

                int partNum = Integer.parseInt(args[1]);
                String fieldName = args[2];
                DictionaryType type = DictionaryType.valueOf(args[3].toUpperCase());

                for(DiskPartition p : manager.getActivePartitions()) {
                    if(p.getPartitionNumber() == partNum) {
                        DiskField df = ((InvFileDiskPartition) p).getDF(fieldName);
                        if(df == null) {
                            return String.format("No such field %s", fieldName);
                        }
                        Dictionary dict = df.getDictionary(type);
                        if(dict == null) {
                            return String.format("No dictionary of type %s for %s", type, fieldName);
                        }
                        for(Iterator di = dict.iterator(); di.hasNext();) {
                            QueryEntry qe = (QueryEntry) di.next();
                            shell.out.format("Entry: %s %d\n", qe.getName(), qe.getID());
                        }
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "partNum field dict - prints all the terms from a particular field's dictionary and partition";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new NullCompletor(),
                    new FieldCompletor(manager.getMetaFile()),
                    new EnumCompletor(DictionaryType.class)
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "ck"), "Info", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 2) {
                    return getHelp();
                }

                Collection<FieldInfo> vf = manager.getMetaFile().getFieldInfo(FieldInfo.Attribute.VECTORED);
                List<DiskPartition> parts = manager.getActivePartitions();
                for(int i = 1; i < args.length; i++) {
                    for(DiskPartition p : parts) {

                        DiskDictionary dd = p.getDocumentDictionary();
                        QueryEntry key = dd.get(args[i]);
                        if(key == null) {
                            continue;
                        }
                        for(FieldInfo fi : vf) {

                            DiskField df = ((InvFileDiskPartition) p).getDF(fi);

                            DiskDictionary rvd = (DiskDictionary) df.getDictionary(DictionaryType.RAW_VECTOR);
                            QueryEntry re;
                            String res = "no raw";
                            if(rvd != null) {
                                re = rvd.get(args[i]);
                                if(re == null) {
                                    res = "rv term not found!";
                                } else {
                                    res = String.format("rv id: %d", re.getID());
                                }
                            }

                            DiskDictionary svd = (DiskDictionary) df.getDictionary(DictionaryType.RAW_VECTOR);
                            QueryEntry se;
                            String ses = "no stemmed";
                            if(svd != null) {
                                se = rvd.get(args[i]);
                                if(se == null) {
                                    ses = "sv term not found!";
                                } else {
                                    ses = String.format("sv id: %d", se.getID());
                                }
                            }

                            boolean del = p.isDeleted(key.getID());

                            shell.out.format("%s %s %s dd id: %d%s%s %s\n",
                                    p, fi.getName(), args[i], key.getID(),
                                    del ? " deleted " : " ",
                                    res, ses);
                        }
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "field key [key]... - Get the document key from the document dictionary and from any vector dictionaries";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile()),
                    new NullCompletor()
                };
            }
        });

        shell.add(prefixCommand(prefix, "postByDictType"), "Info", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args)
                    throws Exception {
                if(args.length < 3) {
                    return getHelp();
                }

                String field = args[1];
                DictionaryType type = DictionaryType.valueOf(args[2].toUpperCase());
                FieldInfo fi = manager.getFieldInfo(field);
                List<DiskPartition> parts = manager.getActivePartitions();
                for(int i = 3; i < args.length; i++) {
                    String term = args[i];
                    for(DiskPartition p : parts) {
                        DiskField df = ((InvFileDiskPartition) p).getDF(fi);
                        DiskDictionary dd = (DiskDictionary) df.getDictionary(type);
                        if(type == DictionaryType.STEMMED_TOKENS) {
                            Stemmer stemmer = df.getStemmer();
                            if(stemmer != null) {
                                term = stemmer.stem(term);
                            }
                        }

                        QueryEntry qe = dd.get(term);
                        if(qe == null) {
                            continue;
                        }
                        Postings post = qe.getPostings();
                        if(post != null) {
                            shell.out.format(
                                    "Postings for %s (%d) from %s in %s\n",
                                             args[i], qe.getN(), field, p);
                            shell.out.println(post.describe(true));
                        }
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "field dictionaryType term [term]...";
            }
            
            @Override
            public Completor[] getCompletors() {
                return new Completor[]{
                            new FieldCompletor(manager.getMetaFile()),
                            new EnumCompletor(DictionaryType.class),
                            new NullCompletor()
                        };
            }
        });
        
        shell.add(prefixCommand(prefix, "post"), "Info", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 3) {
                    return getHelp();
                }
                
                String field = args[1];
                FieldInfo fi = manager.getFieldInfo(field);
                List<DiskPartition> parts = manager.getActivePartitions();
                for(int i = 2; i < args.length; i++) {
                    for(DiskPartition p : parts) {
                        DiskField df = ((InvFileDiskPartition) p).getDF(fi);
                        QueryEntry qe = df.getTerm(args[i], false);
                        if(qe == null) {
                            continue;
                        }
                        Postings post = qe.getPostings();
                        if(post != null) {
                            shell.out.format("Postings for %s (%d) from %s in %s\n",
                                    args[i], qe.getN(), field, p);
                            shell.out.println(post.describe(false));
                        }
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "field term [term...] - Get a short description of the postings associated with a term in a given field";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile()),
                    new NullCompletor()
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "postfinds"), "Info", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args)
                    throws Exception {
                if(args.length < 5) {
                    return getHelp();
                }

                String field = args[1];
                FieldInfo fi = manager.getFieldInfo(field);
                int partNum = Integer.parseInt(args[2]);
                for(DiskPartition p : manager.getActivePartitions()) {
                    if(p.getPartitionNumber() != partNum) {
                        continue;
                    }
                    DiskField df = ((InvFileDiskPartition) p).getDF(fi);
                    QueryEntry qe = df.getTerm(args[3], false);
                    if(qe == null) {
                        continue;
                    }
                    PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
                    feat.setPositions(true);

                    PostingsIterator pi = qe.iterator(feat);
                    for(int i = 4; i < args.length; i++) {
                        int docID = Integer.parseInt(args[i]);
                        if(pi.findID(docID)) {
                            shell.out.format("Found %d\n", docID);
                            if(pi instanceof PostingsIteratorWithPositions) {
                                int[] posn = ((PostingsIteratorWithPositions) pi).getPositions();
                                shell.out.format("%d %s\n", pi.getFreq(), Arrays.toString(posn));
                            }
                        } else {
                            shell.out.format("Didn't find %d\n", docID);
                        }
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "- field partNum term doc [doc...] Finds docs in a given postings list";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile()),
                    new NullCompletor()
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "postbuff"), "Info", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args)
                    throws Exception {
                if(args.length < 3) {
                    return getHelp();
                }

                String field = args[1];
                FieldInfo fi = manager.getFieldInfo(field);
                List<DiskPartition> parts = manager.getActivePartitions();
                for(int i = 2; i < args.length; i++) {
                    for(DiskPartition p : parts) {
                        DiskField df = ((InvFileDiskPartition) p).getDF(fi);
                        QueryEntry qe = df.getTerm(args[i], false);
                        if(qe == null) {
                            continue;
                        }
                        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
                        feat.setPositions(true);
                        
                        PostingsIterator  pi = qe.iterator(feat);
                        Postings post = qe.getPostings();
                        if(post != null) {
                            shell.out.format(
                                    "Postings for %s (%d) from %s in %s\n",
                                             args[i], qe.getN(), field, p);
                            shell.out.println(post.toString());
                        }
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "field term [term...] - print the buffers for the postings associated with a term in a given field";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile()),
                    new NullCompletor()
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "vpost"), "Info", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 3) {
                    return getHelp();
                }
                
                String field = args[1];
                FieldInfo fi = manager.getFieldInfo(field);
                List<DiskPartition> parts = manager.getActivePartitions();
                for(int i = 2; i < args.length; i++) {
                    for(DiskPartition p : parts) {
                        DiskField df = ((InvFileDiskPartition) p).getDF(fi);
                        QueryEntry qe = df.getTerm(args[i], false);
                        if(qe == null) {
                            continue;
                        }
                        Postings post = qe.getPostings();
                        if(post != null) {
                            shell.out.format("Postings for %s (%d) from %s in %s\n",
                                    args[i], qe.getN(), field, p);
                            shell.out.println(post.describe(true));
                        }
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "field term [term...] - Get a verbose description of the postings associated with a term in a given field";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile()),
                    new NullCompletor()
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "vdpost"), "Info", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 4) {
                    return getHelp();
                }
                
                String field = args[1];
                FieldInfo fi = manager.getFieldInfo(field);
                int docID = Integer.parseInt(args[2]);
                List<DiskPartition> parts = manager.getActivePartitions();
                for(int i = 3; i < args.length; i++) {
                    for(DiskPartition p : parts) {
                        DiskField df = ((InvFileDiskPartition) p).getDF(fi);
                        QueryEntry qe = df.getTerm(args[i], false);
                        if(qe == null) {
                            continue;
                        }
                        Postings post = qe.getPostings();
                        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
                        feat.setPositions(true);
                        if(post != null) {
                            PostingsIteratorWithPositions pi = (PostingsIteratorWithPositions) post.iterator(feat);
                            pi.findID(docID);
                            shell.out.format("Postings for %s (%d) from %s in %s\n",
                                    args[i], docID, field, p);
                            shell.out.format("<%d, %d, [", pi.getID(), pi.getFreq());
                            int[] posns = pi.getPositions();
                            for(int pos = 0; pos < pi.getFreq(); pos++) {
                                if(pos > 0) {
                                    shell.out.print(", ");
                                }
                                shell.out.print(posns[pos]);
                            }
                            shell.out.print("]>\n");
                        }
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "field docID term [term...] - Get a verbose description of the postings associated with a term in a given field";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile()),
                    new NullCompletor()
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "dpost"), "Info", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 4) {
                    return getHelp();
                }
                
                String field = args[1];
                FieldInfo fi = manager.getFieldInfo(field);
                if(fi == null) {
                    return String.format("No such field: %s", field);
                }
                int docID = Integer.parseInt(args[2]);
                List<DiskPartition> parts = manager.getActivePartitions();
                for(int i = 2; i < args.length; i++) {
                    for(DiskPartition p : parts) {
                        DiskField df = ((InvFileDiskPartition) p).getDF(fi);
                        QueryEntry qe = df.getTerm(args[i], false);
                        if(qe == null) {
                            continue;
                        }
                        Postings post = qe.getPostings();
                        if(post != null) {
                            PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
                            feat.setPositions(true);
                            PostingsIterator pi = post.iterator(feat);
                            if(pi.findID(docID)) {
                                shell.out.format("%d freq: %d", docID, pi.getFreq());
                                if(pi instanceof PostingsIteratorWithPositions) {
                                    int[] posns = ((PostingsIteratorWithPositions) pi).getPositions();
                                    shell.out.format(" posns: %s\n", Arrays.toString(posns));
                                } else {
                                    shell.out.println();
                                }
                            }
                        }
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "field docID term [term...] - Get all of the postings associated with a term in a particular document, in a given field";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile()),
                    new NullCompletor()
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "keys"), "Info", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] strings) throws Exception {
                List<DiskPartition> parts = manager.getActivePartitions();
                if(strings.length == 1) {
                    for(DiskPartition p : parts) {
                        listKeys(p);
                    }
                } else {
                    for(int i = 1; i < strings.length; i++) {
                        int pn = Integer.parseInt(strings[i]);
                        for(DiskPartition p : parts) {
                            if(p.getPartitionNumber() == pn) {
                                listKeys(p);
                            }
                        }
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "[pn] [pn]... - List the keys in specified partitions";
            }
        });
        
        shell.add(prefixCommand(prefix, "svals"), "Info", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 3) {
                    return "Must specify partition number and field names";
                }
                int pn = Integer.parseInt(args[1]);
                for(DiskPartition p : manager.getActivePartitions()) {
                    if(p.getPartitionNumber() == pn) {
                        for(int i = 2; i < args.length; i++) {
                            DiskField df = ((InvFileDiskPartition) p).getDF(args[i]);
                            if(df == null) {
                                shell.out.format("No field %s\n", args[i]);
                                continue;
                            }
                            DiskDictionary sdict = df.getSavedValuesDictionary();
                            if(sdict == null) {
                                shell.out.format("No saved values for %s\n", args[i]);
                                continue;
                            }
                            for(Object o : sdict) {
                                Entry e = (Entry) o;
                                shell.out.format("%s %d\n", e.getName(), e.getID());
                            }
                        }
                    }
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "pn field [field]... - Prints all the values for a saved field in that partition";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new NullCompletor(),
                    new FieldCompletor(manager.getMetaFile())
                };
            }
        });

        shell.add(prefixCommand(prefix, "rts"), "Maintenance", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                manager.calculateTermStats();
                return "Term stats recalculated";
            }

            @Override
            public String getHelp() {
                return "Recalculate global term statistics";
            }
        });

        shell.add(prefixCommand(prefix, "rvl"), "Maintenance", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                manager.recalculateVectorLengths();
                return "Vector lengths recalculated";
            }

            @Override
            public String getHelp() {
                return "Recalculate vector lengths for all partitions";
            }
        });

        shell.add(prefixCommand(prefix, "fields"), "Info", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                MetaFile mf = manager.getMetaFile();
                shell.out.println(mf.toString());
                return "";
            }

            @Override
            public String getHelp() {
                return "Print the defined fields";
            }
        });
        
        shell.add(prefixCommand(prefix, "del"), "Maintenance", new CommandInterface() {

            @Override
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

            @Override
            public String getHelp() {
                return "key [key]... Delete a document by key";
            }
        });
        
        shell.add(prefixCommand(prefix, "delq"), "Maintenance", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                String q = join(args, 1, args.length, " ");
                try {
                    //
                    // Run the query
                    ResultSet rs = searcher.search(q, 
                                                   sortSpecString,
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

            @Override
            public String getHelp() {
                return "Delete documents that match a given query";
            }
        });
        
        shell.add(prefixCommand(prefix, "deld"), "Maintenance", new CommandInterface() {

            @Override
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

            @Override
            public String getHelp() {
                return "Delete documents from a numbered partition by document ID";
            }
        });
        
        shell.add(prefixCommand(prefix, "get"), "Info", new CompletorCommandInterface() {

            @Override
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
                            shell.out.format("%s: %s\n", args[i], v);
                        }
                    }
                }
                return "";
            }


            @Override
            public String getHelp() {
                return "partNum docID field [field...] - Get field values from a particular doc";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new NullCompletor(),
                    new NullCompletor(),
                    new FieldCompletor(manager.getMetaFile())
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "merge"), "Maintenance", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length == 1) {
                    ci.out.println("Merging all partitions");
                    manager.mergeAll();
                    logger.info(String.format("FWB writes: %.2fms",
                                              FileWriteableBuffer.ww.
                            getTimeMillis()));
                    return "Merged all partitions";
                } else {
                    ArrayList<Integer> parts = new ArrayList<Integer>();
                    for(int i = 1; i < args.length; i++) {
                        parts.add(Integer.parseInt(args[i]));
                    }

                    PartitionManager.Merger merger = manager.getMergerFromNumbers(parts);
                    merger.merge();
                    logger.info(String.format("FWB writes: %.2fms",
                                              FileWriteableBuffer.ww.
                            getTimeMillis()));
                    return "Merged " + parts;
                }
            }

            @Override
            public String getHelp() {
                return "pn [pn] ... - Merge a number of partitions";
            }
        });
        
        shell.add(prefixCommand(prefix, "reap"), "Maintenance", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws
                    Exception {
                
                ci.out.println("Reaping");
                manager.setReapDoesNothing(false);
                manager.reap();
                manager.setReapDoesNothing(true);
                return "Reaped";
            }

            @Override
            public String getHelp() {
                return "removes deleted partitions";
            }
        });

        shell.add(prefixCommand(prefix, "sim"), new CommandInterface() {

            @Override
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

            @Override
            public String getHelp() {
                return "Compute the similarity between two documents";
            }
        });
        
        shell.add(prefixCommand(prefix, "findsim"), "Query", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 2) {
                    return "Must specify at least one document key";
                }
                
                String key = args[1];
                String[] fields = args.length > 2 ? args[2].split(",") : null;
                double skim = args.length > 3 ? Double.parseDouble(args[3]) : 1.0;
                DocumentVector dv = engine.getDocumentVector(key, fields);
                if(dv != null) {
                    ResultSet rs = dv.findSimilar("-score", skim);
                    displayResults(rs);
                } else {
                    shell.out.println("No such doc: " + key);
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "key [fields] [skim] Find documents similar to the given one, using comma-separated field list";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new NullCompletor(),
                    new FieldCompletor(manager.getMetaFile()),
                    new NullCompletor()
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "mfindsim"), "Query", new CompletorCommandInterface() {
            @Override
            public String execute(CommandInterpreter ci, String[] args) throws
                    Exception {
                if(args.length < 2) {
                    return "Must specify at least one document key";
                }

                String[] keys = args[1].split(",");
                String[] fields = args.length > 2 ? args[2].split(",") : null;
                double skim = args.length > 3 ? Double.parseDouble(args[3]) : 1.0;
                DocumentVector dv = engine.getDocumentVector(Arrays.asList(keys), fields);
                if(dv != null) {
                    ResultSet rs = dv.findSimilar("-score", skim);
                    displayResults(rs);
                } else {
                    shell.out.format("No vector for %s\n", Arrays.asList(keys));
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "key [fields] [skim] Find documents similar to the given one, using comma-separated field list";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new NullCompletor(),
                    new FieldCompletor(manager.getMetaFile()),
                    new NullCompletor()
                };
            }
        });

        shell.add(prefixCommand(prefix, "rf"), "Query", new CommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 2) {
                    return "Must specify query";
                }
                
                String query = join(args, 1, args.length, " ");
                ResultSet rs = engine.search(query);
                if(rs.size() == 0) {
                    return "No hits for search";
                }
                Result r = rs.getResults(0, 1).get(0);
                DocumentVector dv = r.getDocumentVector();
                if(dv != null) {
                    rs = dv.findSimilar("-score", 0.25);
                    displayResults(rs);
                } else {
                    return "No document vector?";
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "[fields] query Find documents similar to the best hit for this query, using comma separated field list";
            }
        });
        
        shell.add(prefixCommand(prefix, "simi"), "Query", new CompletorCommandInterface() {
            @Override
            public String execute(CommandInterpreter ci, String[] args) throws
                    Exception {
                if(args.length < 3) {
                    return "Must specify a field and document text";
                }

                String field = args[1];
                String docText = join(args, 2, args.length, " ");
                IndexableMap doc = new IndexableMap("doc-key");
                doc.put(field, docText);
                DocumentVector dv = engine.getDocumentVector(doc, field);
                
                if(dv != null) {
                    ResultSet rs = dv.findSimilar("-score");
                    displayResults(rs);
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "field [document text] Find documents similar to the given one";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile()),
                    new NullCompletor()
                };
            }
        });

        shell.add(prefixCommand(prefix, "dv"), "Terms", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 3) {
                    return "Must specify field and document key";
                }
                
                String fieldArg = args[1];
                String dockey = args[2];
                String[] fields = fieldArg.split(",");
                DocumentVector dv = engine.getDocumentVector(dockey, fields);
                if(dv == null) {
                    shell.out.println("Unknown key: " + args[1]);
                } else {
                    dumpDocVectorByWeight(dv);
                }
                return "";
            }

            @Override
            public String getHelp() {
                return "field dockey - Show the document vector for a given field in a given document";
            }

            @Override
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FieldCompletor(manager.getMetaFile()),
                    new NullCompletor()
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "describeSimilarity"), "Terms", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args)
                    throws Exception {
                if (args.length < 4) {
                    return "Provide two keys or files and at least one field for comparison";
                }
                String[] fields = Arrays.copyOfRange(args, 3, args.length);
                //
                // See if we have a filename or a doc key for each dv
                DocumentVector dv1 = getDVFromFileOrEngine(args[1],
                                                           fields,
                                                           ci.getOutput());
                DocumentVector dv2 = getDVFromFileOrEngine(args[2],
                                                           fields,
                                                           ci.getOutput());
                
                if(dv1 == null) {
                    ci.getOutput().println("No such doc: " + args[1]);
                } else if(dv2 == null) {
                    ci.getOutput().println("No such doc: " + args[2]);
                } else {
                    Map<String, Float> words = dv1.getSimilarityTermMap(dv2);
                    for(Iterator it = words.keySet().iterator(); it.hasNext();) {
                        String curr = (String) it.next();
                        ci.getOutput().println(curr + ": \t" + words.get(curr));
                    }
                }
                return "";
            }

            public DocumentVector getDV(String arg,
                                        String[] fields,
                                        CommandInterpreter ci)
                    throws Exception {
                DocumentVector dv;
                File f1 = new File(arg);
                if (f1.exists()) {
                    if (fields.length != 1) {
                        //
                        // Right now, we can only do a single field for
                        // in-memory document vectors
                        ci.getOutput()
                                .println("Specify only a single field for " +
                                "file-based DV");
                    }
                    BufferedReader reader =
                            new BufferedReader(new FileReader(f1));
                    String line = null;
                    IndexableMap map = new IndexableMap("mySingleDoc");
                    while ((line = reader.readLine()) != null) {
                        int sep = line.indexOf(' ');
                        map.put(line.substring(0, sep),
                                line.substring(sep + 1));
                    }
                    dv = engine.getDocumentVector(map, fields[0]);
                } else {
                    dv = engine.getDocumentVector(arg, fields);
                }
                return dv;
            }
            
            @Override
            public String getHelp() {
                return "<doc1> <doc2> [field ...] - describe the similarity between two docs";
            }
            
            public Completor[] getCompletors() {
                return new Completor[] {
                    new FileNameCompletor(),
                    new FileNameCompletor(),
                    new FieldCompletor(manager.getMetaFile())
                };
            }
        });
        
        shell.add(prefixCommand(prefix, "log"), "Other", new CompletorCommandInterface() {

            @Override
            public String execute(CommandInterpreter ci, String[] args) throws Exception {
                if(args.length < 3) {
                    return "Must specify class name an level";
                }
                try {
                    Class cl = Class.forName(args[1]);
                    Logger.getLogger(cl.getName()).setLevel(Level.parse(args[2].toUpperCase()));
                } catch(ClassNotFoundException ex) {
                    return "Unknown class: " + args[1];
                } catch(IllegalArgumentException ex) {
                    return "Unknown log level: " + args[2];
                }
                return args[1] + " " + Logger.getLogger(args[1]).getLevel();
            }

            @Override
            public String getHelp() {
                return "class logLevel - Set the log level for a given class";
            }

            @Override
            public Completor[] getCompletors() {
                String[] logLevels = new String[] {
                    Level.ALL.getLocalizedName(),
                    Level.CONFIG.getLocalizedName(),
                    Level.FINE.getLocalizedName(),
                    Level.FINER.getLocalizedName(),
                    Level.FINEST.getLocalizedName(),
                    Level.INFO.getLocalizedName(),
                            Level.OFF.getLocalizedName(),
                            Level.SEVERE.getLocalizedName(),
                            Level.WARNING.getLocalizedName()
                };
                try {
                    return new Completor[] {
                        new ClassNameCompletor(),
                        new SimpleCompletor(logLevels)
                    };
                } catch (IOException e) {
                    return new Completor[] {
                        new NullCompletor(),
                        new SimpleCompletor(logLevels)
                    };
                }
            }
        });
        
    }
    
    private void listKeys(DiskPartition p) {
        DiskDictionary<String> dd = p.getDocumentDictionary();
        for(Entry e : dd) {
            shell.out.println(e.getName());
        }
    }

    private void dumpDocVectorByWeight(DocumentVector dv) {
        //
        // sort by weight:
        SortedSet w = new TreeSet(WeightedFeature.INVERSE_WEIGHT_COMPARATOR);
        w.addAll(((AbstractDocumentVector) dv).getSet());
        float sum = 0;
        for(Iterator it = w.iterator(); it.hasNext();) {
            WeightedFeature wf = (WeightedFeature) it.next();
            sum += wf.getWeight() * wf.getWeight();
            shell.out.println(wf.toString());
        }
        shell.out.format("length: %.2f\n", Math.sqrt(sum));
    }

    private DocumentVector getDVFromFileOrEngine(String key,
                                                 String[] fields,
                                                 PrintStream out)
            throws Exception {
        DocumentVector dv;
        File f1 = new File(key);
        if (f1.exists()) {
            if (fields.length != 1) {
                //
                // Right now, we can only do a single field for
                // in-memory document vectors
                out.println("Specify only a single field for file-based DV");
            }
            BufferedReader reader =
                    new BufferedReader(new FileReader(f1));
            String line = null;
            IndexableMap map = new IndexableMap("mySingleDoc");
            while ((line = reader.readLine()) != null) {
                int sep = line.indexOf(' ');
                map.put(line.substring(0, sep),
                                line.substring(sep + 1));
            }
            dv = engine.getDocumentVector(map, fields[0]);
        } else {
            dv = engine.getDocumentVector(key, fields);
        }
        return dv;
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
                inputFile,
                "partNum docID dockey",
                "%6d%10d %s",
                null);

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
         * The maximum amount of a string field that we'll show.
        */
        protected int[] lengths;

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

        public void setDisplayFields(String[] inFields) {
            
            //
            // Split the input at commas.
            StringBuilder df = new StringBuilder();
            fields = new String[inFields.length];
            lengths = new int[inFields.length];
            Arrays.fill(lengths, -1);
            vals = new Object[inFields.length];
            for(int i = 0; i < inFields.length; i++) {

                int cp = inFields[i].indexOf(':');
                if(cp >= 0) {
                    fields[i] = inFields[i].substring(0, cp);
                    try {
                    lengths[i] = Integer.parseInt(inFields[i].substring(cp+1));
                    } catch (NumberFormatException ex) {
                        logger.warning(String.format("Bad field length spec: %s, Ignoring", inFields[i].substring(cp+1)));
                        lengths[i] = -1;
                    }
                } else {
                    fields[i] = inFields[i];
                }
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
                                "Display field %s is not saved", inFields[i]));
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
                    if(val == null || val.isEmpty()) {
                        vals[i] = null;
                    } else if(val.size() == 1) {
                        vals[i] = val.get(0);
                    } else {
                        vals[i] = val.toString();
                    }
                    if(lengths[i] > 0 && vals[i] instanceof String) {
                        String s = (String) vals[i];
                        if(s.length() > lengths[i]) {
                            vals[i] = s.substring(0, lengths[i]);
                        }
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
                Map mp = pb.getPassages();

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
//                List p = pb.
//                int n = 0;
//                for(Iterator i = p.iterator(); i.hasNext() && n < 4;) {
//                    Passage pass = (Passage) i.next();
//                    pass.highlight(shigh, true);
//                    String[] mTerms = pass.getMatchingTerms();
//                    shell.out.format("  %.3f", pass.getScore());
//                    for(int l = 0; l < mTerms.length; l++) {
//                        shell.out.print(" " + mTerms[l]);
//                    }
//                    shell.out.println("");
//                    String hp = pass.getHLValue(true);
//                    hp = reformat(hp.replace('\n', ' '), "   ", 72);
//                    shell.out.println(hp);
//                    n++;
//                }
//
//                if(n < p.size()) {
//                    shell.out.println("  " + (p.size() - n) + " passages not shown");
//                }
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
    
    /**
     * Provides command line completion based on all currently defined field
     * names.
     */
    static class FieldCompletor implements Completor {
        
        protected MetaFile mf;
        
        public FieldCompletor(SearchEngine engine) {
            this(engine.getPM().getMetaFile());
        }
        
        public FieldCompletor(MetaFile mf) {
            this.mf = mf;
        }
        
        @Override
        public int complete(String buff, int i, List ret) {
            String prefix = "";
            if (buff != null) {
                prefix = buff.substring(0, i);
            }
            //
            // Go through the currently defined fields
            Iterator<FieldInfo> infos = mf.fieldIterator();
            while (infos.hasNext()) {
                String name = infos.next().getName();
                if (name.toLowerCase().startsWith(prefix.toLowerCase())) {
                    ret.add(name);
                }
            }

            //
            // Pad a " " to the end if this was the only completion
            if (ret.size() == 1) {
                ret.set(0, ret.get(0) + " ");
            }
            return (ret.isEmpty() ? -1 : 0);
        }
        
    }
    

}
