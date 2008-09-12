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
import com.sun.labs.minion.indexer.entry.TermStatsEntry;

import com.sun.labs.minion.util.FileLockException;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.sun.labs.minion.classification.ClassifierDiskPartition;
import com.sun.labs.minion.classification.ClassifierManager;
import com.sun.labs.minion.classification.ClassifierModel;
import com.sun.labs.minion.classification.ClusterDiskPartition;
import com.sun.labs.minion.classification.ClusterManager;
import com.sun.labs.minion.classification.Feature;
import com.sun.labs.minion.classification.FeatureCluster;
import com.sun.labs.minion.classification.FeatureClusterSet;
import com.sun.labs.minion.classification.FeatureClusterer;
import com.sun.labs.minion.classification.FeatureSelector;
import com.sun.labs.minion.classification.KnowledgeSourceClusterer;
import com.sun.labs.minion.classification.MIFeatureSelector;
import com.sun.labs.minion.classification.Rocchio;
import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.PosPostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.lexmorph.LiteMorph;
import com.sun.labs.minion.lexmorph.LiteMorph_en;
import com.sun.labs.minion.lextax.DiskTaxonomy;
import com.sun.labs.minion.retrieval.DocumentVectorImpl;
import com.sun.labs.minion.retrieval.FieldTerm;
import com.sun.labs.minion.retrieval.ResultImpl;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.cache.TermCache;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.util.StopWatch;
import com.sun.labs.minion.util.Util;

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.FieldFrequency;
import com.sun.labs.minion.FieldValue;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.IndexableMap;
import com.sun.labs.minion.Log;
import com.sun.labs.minion.Passage;
import com.sun.labs.minion.PassageBuilder;
import com.sun.labs.minion.PassageHighlighter;
import com.sun.labs.minion.Posting;
import com.sun.labs.minion.Progress;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.ResultsCluster;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.Searcher;
import com.sun.labs.minion.SimpleHighlighter;
import com.sun.labs.minion.TermStats;
import com.sun.labs.minion.TextHighlighter;
import com.sun.labs.minion.WeightedField;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.URL;
import java.util.HashSet;
import com.sun.labs.minion.classification.ExplainableClassifierModel;
import com.sun.labs.minion.indexer.MetaFile;
import com.sun.labs.minion.indexer.dictionary.LightIterator;
import com.sun.labs.minion.indexer.dictionary.TermStatsDictionary;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.DocumentVectorLengths;
import com.sun.labs.minion.lexmorph.disambiguation.Unsupervised;
import com.sun.labs.minion.retrieval.FieldEvaluator;
import com.sun.labs.minion.retrieval.MultiDocumentVectorImpl;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.util.Collections;
import java.util.logging.Handler;
import java.util.logging.Logger;

public class QueryTest extends SEMain {

    protected static Log log;

    protected static final String logTag = "QT";

    protected final static boolean DEBUG = false;

    protected DecimalFormat scoreForm;

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

    protected long totalTime,  nQueries;

    protected int grammar = Searcher.GRAMMAR_STRICT;
    
    protected int queryOp = Searcher.OP_AND;

    protected boolean ttyInput = true;

    protected Unsupervised.Model currModel;
    
    protected PrintStream output;

    //
    // Field operators.
    protected static Map<String, Integer> opMap =
            new HashMap<String, Integer>();

    private SimpleHighlighter shigh;

    static {
        opMap.put("equal", new Integer(FieldTerm.EQUAL));
        opMap.put("less", new Integer(FieldTerm.LESS));
        opMap.put("greater", new Integer(FieldTerm.GREATER));
        opMap.put("leq", new Integer(FieldTerm.LEQ));
        opMap.put("geq", new Integer(FieldTerm.GEQ));
        opMap.put("matches", new Integer(FieldTerm.MATCHES));
        opMap.put("substring", new Integer(FieldTerm.SUBSTRING));
        opMap.put("similar", new Integer(FieldTerm.SIMILAR));
        opMap.put("starts", new Integer(FieldTerm.STARTS));
        opMap.put("ends", new Integer(FieldTerm.ENDS));
        opMap.put("not$equal", new Integer(FieldTerm.NOT$EQUAL));
    }

    public QueryTest(URL cmFile, String indexDir, String engineType, String ds,
            String ss, PrintStream output) throws java.io.IOException,
            SearchEngineException {
        if(engineType == null) {
            engine =
                    (SearchEngineImpl) SearchEngineFactory.getSearchEngine(indexDir,
                    cmFile);
        } else {
            engine =
                    (SearchEngineImpl) SearchEngineFactory.getSearchEngine(indexDir,
                    engineType, cmFile);
        }
        manager = engine.getManager();
        searcher = engine;
        morphEn = LiteMorph_en.getMorph();
        displaySpec = new DisplaySpec(ds);
        sortSpec = ss;
        scoreForm = new DecimalFormat("###0.000");
        displayPassage = false;
        this.output = output;
        shigh = new SimpleHighlighter("<font color=\"#FF0000\">", "</font>",
                            "<b>", "</b>");
    }

    public void help() {
        output.println(":n <num>                Set number of hits to return\n" +
                ":dp                     Toggle passage display\n" +
                ":c                      Complex passage display\n" +
                ":h                      Display this message\n" +
                ":sort <sortspec>        Specify the sort order for hits\n" +
                ":display <displayspec>  " +
                "Specify the fields to be displayed for hits\n" +
                ":sleep <milliseconds>   Sleep for some time\n" +
                "\nClassification:\n" +
                ":class <query>          Invoke a classifier and print the features\n" +
                ":feat <num> <query>     " +
                "Perform feature selection on the query results\n" +
                ":cf <classname>         Display the features for a class\n" +
                ":cfw <classname>         Display the features for a class, sorted by weight\n" +
                ":train <class> <query>  Train a classifier with a name and a query\n" +
                ":clusters <term>        Show all clusters in all partitions containing <term>\n" +
                ":clusternamed <name>    Show the named cluster in each partition\n" +
                ":pclusters              Prints all clusters, by partition\n" +
                ":csim <class> <dockey>  Compute similarity between a classifier and a doc\n" +
                "\nGeneral:\n" +
                ":gram [web|strict]      Print or set the grammar to use\n" +
                ":bq <query>             Batch query, prints results 100 at a time\n" +
                ":term <term>            Look up a term entry in each partition\n" +
                ":termi <term>           Case-insensitive version of ':term'\n" +
                ":dis <term>             Build a disambiguator for a term\n" +
                ":merge [<part> ...]     Merge all or some index partitions\n" +
                ":classmerge             Merge all classifier partitions\n" +
                ":clustermerge           Merge all cluster partitions\n" +
                ":ob                     Prints number of small docs and total docs\n" +
                ":ts                     Prints term stats dictionary\n" +
                ":cts [<part> ...]       Calculate and dump term stats\n" +
                ":rts                    Re-generates term stats using all active partitions\n" +
                "\nWild Card, etc:\n" +
                ":sw                     Toggles case-sensitivity for wild cards\n" +
                ":wild <pattern>         Prints matching terms from each partition\n" +
                ":morph <term>           Prints matching variants from each partition\n" +
                ":variants <term>        Prints the LiteMorph variants of a term\n" +
                ":stem <term>            Prints stem-matched terms from each partition\n" +
                ":spell <term>           Prints the top 10 spelling variants of the term\n" +
                "\nTaxonomy\n" +
                ":child <term>           Prints taxonomic children of a term\n" +
                ":par <term>             Prints taxonomic parents of a term\n" +
                ":sub <term>             Prints subsumed terms for <term>\n" +
                "\nFields & Postings\n" + ":fs <part> <field>      \n" +
                ":sim <dockey> <dockey>  Compute similarity between docs\n" +
                ":findsim <dockey> [<skim>]      Find documents similar to a doc\n" +
                ":ffs <field> <dockey> [<skim>]   Find documents similar to a doc based on a particular field\n" +
                ":cfs <dockey> <field> <weight> ... [<skim>]   Find documents similar to a doc based on a particular field combination\n" +
                ":dv <dockey>            Show the document vector for a doc\n" +
                ":fdv <dockey> <field>   Show the document vector for a field in a doc\n" +
                ":ft <field> <op> <val>  Evaluate a field query\n" +
                ":del <dockey>           Deletes the document\n" +
                ":deld <partNum> <docid> [<docid...] Deletes the documents from the given partition\n" +
                ":ddup <fromPart> <inPart> Deletes all dups from a partition that are in another partition\n" +
                ":delq <query>           Delete the results of a query (asking first to confirm)\n" +
                ":get <part> <docid> <fields ...>  Prints field values\n" +
                ":gav <field>            Prints field values from all docs\n" +
                ":gtv <field> [<n>]      Print the top n most frequent field values\n" +
                ":gm <field> <pat>       Get matching values from the field\n" +
                ":docpost <dockey>       Prints ID postings from the doc dict\n" +
                ":pt <term>              Tests findID in postings iterator\n" +
                ":post <type> <term ...> Prints postings.  type is: field <name>,doc,np,freq\n" +
                ":main <part>            Print all entries in a partition\n" +
                ":di [<part>]            Dumps all document entries for all/one partitions\n" +
                ":ddi\n" +
                ":ds                     Gets the number of entries with 1 posting\n" +
                ":dt <dockey>            Prints the partition that contains the doc\n" +
                ":top <field>            Gets the most frequent values for the given saved field\n" +
                "\n" + ":q                      Quit\n" +
                "Any text with no starting ':' issues a query");
    }

    public static void usage() {
        System.err.println("Usage: java com.sun.labs.minion.test.QueryTest [options] -d indexDir");
    }

    public void stats() {
        if(ttyInput) {

            output.println("Status:");

            //
            // Collection stats.
            Iterator l = manager.getActivePartitions().iterator();

            output.print(" Active part numbers: ");
            while(l.hasNext()) {
                DiskPartition p = (DiskPartition) l.next();
                output.print(p.getPartitionNumber() + " ");
            }
            output.println("");

            output.println(" Sorting specification is: " + sortSpec);
            // output.print(" Display specification is: ");
            // for(int i = 0; i < displaySpec.length; i++) {
            // if(i > 0) {
            // output.print(",");
            // }
            // output.print(displaySpec[i]);
            // }
            // output.println("");

            // output.println(" Current query language: " +
            // currentLocale.getLanguage());

            output.println(" Partitions have:\n" + "  " + manager.getNDocs() +
                    " documents\n" + "  " + manager.getNTokens() + " tokens\n" +
                    "  " + manager.getNTerms() + " terms");
        }
    }

    /**
     * Sets the prompt to show which of the toggles are on.
     */
    public void setPrompt() {

        StringBuffer v = new StringBuffer();

        if(displayPassage) {
            v.append('d');
        }

        if(complexPassDisplay) {
            v.append('c');
        }

        v.append("> ");
        prompt = v.toString();
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
            log.error(logTag, 1, "Error getting search results", se);
            return;
        } catch(Exception e) {
            log.error(logTag, 1, "Error getting search results", e);
            return;
        }

        output.println("Query took: " + set.getQueryTime() + " ms");
        totalTime += set.getQueryTime();
        nQueries++;

        output.println(results.size() + "/" + set.size() + "/" +
                set.getNumDocs());

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
        output.println(eqs.dump());
    }

    /**
     * Parses the given message into an array of strings.
     *
     * @param message 	the string to be parsed.
     * @return the parsed message as an array of strings
     */
    protected String[] parseMessage(String message) {
        int tokenType;
        List words = new ArrayList(20);
        StreamTokenizer st = new StreamTokenizer(new StringReader(message));

        st.resetSyntax();
        st.whitespaceChars(0, ' ');
        st.wordChars('!', 255);
        st.quoteChar('"');
        st.quoteChar('\"');
        st.commentChar('#');

        while(true) {
            try {
                tokenType = st.nextToken();
                if(tokenType == StreamTokenizer.TT_WORD) {
                    words.add(st.sval);
                } else if(tokenType == '\'' || tokenType == '"') {
                    words.add(st.sval);
                } else if(tokenType == StreamTokenizer.TT_NUMBER) {
                    output.println("Unexpected numeric token!");
                } else {
                    break;
                }
            } catch(IOException e) {
                break;
            }
        }
        return (String[]) words.toArray(new String[words.size()]);
    }

    protected int processCommand(String q) {
        if(q.startsWith(":qu")) {

            //
            // Run a query.
            q = q.substring(q.indexOf(' ') + 1).trim();
            try {
                ResultSet r = searcher.search(q, sortSpec,
                        queryOp, grammar);
                displayResults(r);
            } catch(SearchEngineException se) {
                log.error(logTag, 1, "Error running search", se);
            }
        } else if(q.startsWith(":qop")) {
            String op = q.substring(q.indexOf(' ') + 1).trim();
            if(op.equalsIgnoreCase("and")) {
                queryOp = Searcher.OP_AND;
            } else if(op.equalsIgnoreCase("or")) {
                queryOp = Searcher.OP_OR;
            } else if(op.equalsIgnoreCase("pand")) {
                queryOp = Searcher.OP_PAND;
            }
        } else if(q.startsWith(":qstats")) {
            queryStats();
        } else if(q.startsWith(":nd")) {
            output.println("ndocs: " + engine.getNDocs());
        } else if(q.startsWith(":clust ")) {

            String[] fields =
                    parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            String field = null;
            if(fields.length > 1) {
                field = fields[1];
            }

            int K = 10;
            if(fields.length > 2) {
                K = Integer.parseInt(fields[2]);
            }

            try {
                ResultSet r = searcher.search(fields[0], sortSpec,
                        Searcher.OP_AND, grammar);
                output.println("Clustering " + r.size() + " results");
                Set<ResultsCluster> cs = r.cluster(field, K);
                for(ResultsCluster rc : cs) {
                    output.format("Cluster: %s %d %.3f %.4f\n",
                            rc.getDescription(10),
                            rc.getStatistics().size(),
                            rc.getStatistics().mean(),
                            rc.getStatistics().variance());
                    Result cr = rc.getMostCentralResult();
                    displayResults("\t", rc.getResults());
                    displayResults("\t", rc.getResults());
                }
            } catch(SearchEngineException se) {
                log.error(logTag, 1, "Error running search", se);
            } catch(Exception e) {
                log.error(logTag, 1, "Exception clustering", e);
            }
        } else if(q.startsWith(":export ")) {
            q = q.substring(q.indexOf(' ') + 1).trim();
            try {
                PrintWriter ow =
                        new PrintWriter(new BufferedWriter(new FileWriter(q)));
                engine.export(ow);
                ow.close();
            } catch(IOException ex) {
                ex.printStackTrace();
            }
        } else if(q.startsWith(":ts")) {
            TermStatsDictionary tsd = manager.getTermStatsDict();
            output.println("Term stats dictionary has: " + tsd.size());
            DictionaryIterator di = tsd.iterator();
            while(di.hasNext()) {
                TermStatsEntry tse = (TermStatsEntry) di.next();
                output.println(tse.getTermStats());
            }
            tsd.iterationDone();
        } else if(q.startsWith(":class ")) {

            //
            // Run a query.
            q = q.substring(q.indexOf(' ') + 1).trim();
            try {
                ResultSetImpl r = (ResultSetImpl) searcher.search(q, "");
                ClassifierModel roc = new Rocchio();
                FeatureSelector fs = new MIFeatureSelector();
                //FeatureClusterer fc = new MorphClusterer();
                FeatureClusterer fc = new KnowledgeSourceClusterer();
                FeatureClusterSet feats = fc.cluster(r);
                TermCache tc = new TermCache(-1, manager.getEngine());
                feats = fs.select(feats, tc.getWeightingComponents(),
                        r.size(), 200, (SearchEngineImpl) manager.getEngine());
                roc.train("test", "test", manager, r, feats, tc, null);
                FeatureClusterSet feat = roc.getFeatures();
                log.debug(logTag, 0, "features: " + feat.size());
            // for(Iterator i = feat.iterator(); i.hasNext(); ) {
            // output.println("feature: " + i.next());
            // }
            } catch(SearchEngineException se) {
                log.error(logTag, 1, "Error running search", se);
            } catch(Exception e) {
                log.error(logTag, 1, "Error", e);
            }
        } else if(q.startsWith(":describe ")) {
            q = q.substring(q.indexOf(' ') + 1).trim();
            ClassifierModel cm =
                    ((SearchEngineImpl) engine).getClassifierManager().
                    getClassifier(q);

            ExplainableClassifierModel ecm = (ExplainableClassifierModel) cm;
            if(ecm == null) {
                log.log(logTag, 0, "No classifier for: " + q);
            } else {

                output.println("---++ Classifier " + q);
                output.println(ecm.describe());
            }

        } else if(q.startsWith(":feat ")) {

            try {

                q = q.substring(q.indexOf(' ') + 1);

                int nf = Integer.parseInt(q.substring(0, q.indexOf(' ')));

                //
                // Run a query.
                q = q.substring(q.indexOf(' ') + 1).trim();

                ResultSet r = searcher.search(q, "");
                StopWatch sw = new StopWatch();
                sw.start();
                FeatureSelector fs = new MIFeatureSelector();
                //FeatureClusterer fc = new ContingencyFeatureClusterer();
                //FeatureClusterer fc = new LiteMorphClusterer();
                //FeatureClusterer fc = new MorphClusterer();
                //FeatureClusterer fc = new StemmingClusterer();
                FeatureClusterer fc = new KnowledgeSourceClusterer();
                FeatureClusterSet clusts = fc.cluster((ResultSetImpl) r);
                FeatureClusterSet s2 = fs.select(clusts,
                        r.getEngine().getQueryConfig().getWeightingComponents(),
                        r.size(), nf, engine);
                sw.stop();
                TreeSet s3 = new TreeSet(
                        new com.sun.labs.minion.classification.ClusterWeightComparator(
                        false, true));
                s3.addAll(s2.getContents());
                output.println("CFS took: " + sw.getTime() + " " + s3.size());
                for(Iterator i = s3.iterator(); i.hasNext();) {
                    FeatureCluster curr = (FeatureCluster) i.next();
                    output.println(curr.getName() + " " + curr.getWeight());
                // for (Iterator ci = curr.getContents().iterator();
                // ci.hasNext();) {
                // Feature f = (Feature)ci.next();
                // output.println(f.getName() + " " + curr.getWeight());
                // }
                }

            } catch(SearchEngineException se) {
                log.error(logTag, 1, "Error running search", se);
            } catch(Exception e) {
                log.error(logTag, 1, "Error", e);
            }
        } else if(q.startsWith(":train ")) {
            //
            // Train a classifier:
            // :train <classname> <query>
            try {
                q = q.substring(q.indexOf(' ') + 1).trim();
                String className = q.substring(0, q.indexOf(' '));
                q = q.substring(q.indexOf(' ') + 1).trim();

                //
                // First run the query
                ResultSet r = searcher.search(q, "");

                Progress p = new Progress() {

                    public void start(int steps) {
                    }

                    public void next(String s) {
                        log.debug(logTag, 4, "Progress: " + s);
                    }

                    public void next() {
                    }

                    public void done() {
                    }
                };

                //
                // Now train the classifier
                engine.trainClass(r, className, "test", null, p);
            } catch(SearchEngineException se) {
                log.error(logTag, 1, "Error running search", se);
            } catch(Exception e) {
                log.error(logTag, 1, "Error", e);
            }
        } else if(q.startsWith(":clusters ")) {
            //
            // Print out the clusters that a term is found in
            String term = q.substring(q.indexOf(' ') + 1).trim();
            try {
                ClusterManager cm = engine.getClusterManager();
                for(Iterator it = cm.getActivePartitions().iterator(); it.hasNext();) {
                    ClusterDiskPartition part = (ClusterDiskPartition) it.next();
                    output.println("Partition " + part + ":");
                    QueryEntry ent = part.getTerm(term);
                    if(ent != null) {
                        PostingsIterator pit =
                                ent.iterator(new PostingsIteratorFeatures());
                        output.println("Found term " + term + " with ID " +
                                ent.getID());
                        FeatureClusterSet clusters =
                                part.getClustersContaining(term);
                        String termStr = "";

                        for(Iterator fcit = clusters.iterator(); fcit.hasNext();) {
                            FeatureCluster fc = (FeatureCluster) fcit.next();
                            pit.next();
                            termStr += "[Cluster: " + fc.getName() + "/" +
                                    pit.getID() + "] ";
                            for(Iterator fit = fc.getContents().iterator(); fit.hasNext();) {

                                Feature feat = (Feature) fit.next();
                                termStr += feat.getName();
                                termStr += "/" + feat.getID();
                                termStr += ", ";
                            }
                            termStr += "\n";
                        }
                        output.println(termStr);
                    }
                }
            } catch(Exception e) {
                log.error(logTag, 1, "Exception!", e);
            }
        } else if(q.equals(":pclusters")) {
            try {
                ClusterManager cm = engine.getClusterManager();
                for(Iterator it = cm.getActivePartitions().iterator(); it.hasNext();) {
                    ClusterDiskPartition part = (ClusterDiskPartition) it.next();
                    output.println(part);
                    for(Iterator docit = part.getDocumentIterator(); docit.hasNext();) {
                        QueryEntry ent = (QueryEntry) docit.next();
                        output.println(ent);
                    }
                }
            } catch(Exception e) {
                log.error(logTag, 1, "Exception!", e);
            }
        } else if(q.equals(":checkclusters")) {
            try {
                ClusterManager cm = engine.getClusterManager();
                for(Iterator it = cm.getActivePartitions().iterator(); it.hasNext();) {
                    ClusterDiskPartition part = (ClusterDiskPartition) it.next();
                    output.println(part);
                    for(Iterator docit = part.getDocumentIterator(); docit.hasNext();) {
                        QueryEntry ent = (QueryEntry) docit.next();
                        String docName = (String) ent.getName();
                        FeatureCluster cluster = part.getCluster(docName);
                        Feature self = cluster.get(docName);
                        if(self == null) {
                            output.println("Consistency check failed for cluster " +
                                    docName);
                        }
                    }
                }
            } catch(Exception e) {
                log.error(logTag, 1, "Exception!", e);
            }
        } else if(q.startsWith(":clusternamed ")) {
            //
            // Print out the cluster with the given name
            String cname = q.substring(q.indexOf(' ') + 1).trim();
            try {
                ClusterManager cm = engine.getClusterManager();
                for(Iterator it = cm.getActivePartitions().iterator(); it.hasNext();) {
                    ClusterDiskPartition part = (ClusterDiskPartition) it.next();
                    FeatureCluster cluster = part.getCluster(cname);
                    output.println("Partition " + part + ":");
                    String termStr = "";
                    if(cluster == null) {
                        continue;
                    }
                    QueryEntry doc = part.getDocumentTerm(cname);
                    output.println("Found doc " + cname + " with ID " +
                            doc.getID());
                    for(Iterator fit = cluster.getContents().iterator(); fit.hasNext();) {
                        Feature feat = (Feature) fit.next();
                        termStr += feat.getName() + "/" + feat.getID();
                        termStr += ", ";
                    }
                    output.println(termStr);

                }
            } catch(Exception e) {
                log.error(logTag, 1, "Exception!", e);
            }
        } else if(q.startsWith(":cf ")) {
            //
            // Get the features for a named classifier.
            String cname = q.substring(q.indexOf(' ') + 1).trim();

            try {
                ClassifierManager cm = engine.getClassifierManager();
                for(Iterator i = cm.getActivePartitions().iterator(); i.hasNext();) {
                    Set s =
                            ((ClassifierDiskPartition) i.next()).getFeatures(cname);
                    if(s == null) {
                        continue;
                    }
                    TreeSet ts = new TreeSet();
                    ts.addAll(s);
                    for(Iterator j = ts.iterator(); j.hasNext();) {
                        WeightedFeature f = (WeightedFeature) j.next();
                        // output.println("f: " + j.next());
                        output.format("%-20s %7.5f\n", f.getName(),
                                f.getWeight());
                    }
                }
            } catch(Exception e) {
                log.error(logTag, 1, "Error getting features", e);
            }
        } else if(q.startsWith(":cfw ")) {
            //
            // Get the features for a named classifier.
            String cname = q.substring(q.indexOf(' ') + 1).trim();

            try {
                ClassifierManager cm = engine.getClassifierManager();
                for(Iterator i = cm.getActivePartitions().iterator(); i.hasNext();) {
                    Set s =
                            ((ClassifierDiskPartition) i.next()).getFeatures(cname);
                    if(s == null) {
                        continue;
                    }
                    SortedSet sorted = new TreeSet(new Comparator() {

                        public int compare(Object o1, Object o2) {
                            return ((WeightedFeature) o1).getWeight() -
                                    ((WeightedFeature) o2).getWeight() >= 0 ? 1
                                    : -1;
                        }

                        public boolean equals(Object o) {
                            return true;
                        }
                    });
                    sorted.addAll(s);

                    for(Iterator j = sorted.iterator(); j.hasNext();) {
                        WeightedFeature f = (WeightedFeature) j.next();
                        // output.println("f: " + j.next());
                        output.format("%-20s %7.5f\n", f.getName(),
                                f.getWeight());
                    }
                }
            } catch(Exception e) {
                log.error(logTag, 1, "Error getting features", e);
            }
        } else if(q.startsWith(":diff ")) {
            String[] args = parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            DocumentVector dv = engine.getDocumentVector(args[0], "content");
            ResultSet r1 = dv.findSimilar();
            ResultSet r2 = engine.anyTerms(Collections.singleton(args[1]),
                    Collections.singleton("content"));
            ResultSet diff = r1.difference(r2);
            displayResults(diff);
        } else if(q.startsWith(":bq ")) {
            q = q.substring(q.indexOf(' ') + 1).trim();
            try {
                ResultSet r = searcher.search(q, sortSpec);
                int batchSize = 100;
                int cur = 0;
                while(cur < r.size()) {
                    output.println(cur + " / " + r.size());
                    List results = r.getResults(cur, batchSize);
                    cur += batchSize;
                    for(Iterator i = results.iterator(); i.hasNext();) {
                        displaySpec.format((Result) i.next());
                    }
                }
                displayResults(r);
            } catch(SearchEngineException se) {
                log.error(logTag, 1, "Error running search", se);
            }
        } else if(q.startsWith(":sleep ")) {
            try {
                long st = Long.parseLong(q.substring(q.indexOf(' ') + 1).trim());
                Thread.sleep(st);
            } catch(NumberFormatException nfe) {
                log.warn(logTag, 1, "Invalid sleep time: " + q.substring(q.indexOf(' ') + 1).
                        trim());
            } catch(InterruptedException ie) {
            }

        } else if(q.startsWith(":n ")) {

            try {
                nHits = Integer.parseInt(q.substring(q.indexOf(' ') + 1).trim());
            } catch(NumberFormatException nfe) {
                log.warn(logTag, 1, "Invalid number of hits: " + q.substring(q.indexOf(' ') + 1).
                        trim());
            }
        } else if(q.startsWith(":sort")) {

            //
            // Get the sort spec.
            sortSpec = q.substring(q.indexOf(' ') + 1).trim();
        } else if(q.startsWith(":display")) {
            displaySpec =
                    new DisplaySpec(q.substring(q.indexOf(' ') + 1).trim());
        } else if(q.startsWith(":gram")) {
            if(q.indexOf(' ') < 0) {
                output.println("Currently using " +
                        Searcher.GRAMMARS[grammar] + " grammar");
            } else {
                String g = q.substring(q.indexOf(' ') + 1).trim();

                if(g.toLowerCase().equals("web")) {
                    grammar = Searcher.GRAMMAR_WEB;
                } else if(g.toLowerCase().equals("strict")) {
                    grammar = Searcher.GRAMMAR_STRICT;
                } else if(g.toLowerCase().equals("lucene")) {
                    grammar = Searcher.GRAMMAR_LUCENE;
                } else {
                    output.println("Unrecognized grammar, valid values are \"full\", \"web\" or \"lucene\"");
                }
            }
        } else if(q.startsWith(":q")) {

            try {
                if(engine != null) {
                    engine.close();
                }
            } catch(SearchEngineException se) {
                log.error(logTag, 1, "Error closing search engine", se);
            }
            return -1;
        } else if(q.startsWith(":term ")) {

            //
            // Case sensitive term lookup.
            String term = CharUtils.decodeUnicode(q.substring(
                    q.indexOf(' ') + 1).trim());
            for(DiskPartition p : manager.getActivePartitions()) {
                Entry e = p.getTerm(term, true);
                output.println("Partition " + p.getPartitionNumber() +
                        ":\n " + e + (e == null ? ""
                        : (" (" + Util.toHexDigits(e.getName().toString()) +
                        ") ") + e.getN()));
            }
        } else if(q.startsWith(":termstats " )) {
            //
            // Case sensitive term lookup.
            String term = CharUtils.decodeUnicode(q.substring(
                    q.indexOf(' ') + 1).trim());
            TermStats ts = engine.getTermStats(term);
            if(ts == null) {
                output.println("Term not found");
            } else {
                output.println("Term stats: " + ts);
            }
            
        } else if(q.startsWith(":termi ")) {

            //
            // Case insensitive term lookup.
            String term = CharUtils.decodeUnicode(q.substring(
                    q.indexOf(' ') + 1).trim());
            Iterator l = manager.getActivePartitions().iterator();
            while(l.hasNext()) {
                DiskPartition p = (DiskPartition) l.next();
                Entry e = p.getTerm(term, false);
                output.println("Partition " + p.getPartitionNumber() + ": " + (e == null
                        ? "No entries" : (e.toString() + " " + e.getID() + " " +
                        e.getN())));
            }
        } else if(q.startsWith(":dis ")) {
            String[] fields = parseMessage(q.substring(
                    q.indexOf(' ') + 1).trim());
            int K = 5;
            String term = fields[0];
            String field = null;
            if(fields.length > 1) {
                field = fields[1];
            }
            if(fields.length > 2) {
                K = Integer.parseInt(fields[2]);
            }
            int mf = 1000;
            if(fields.length > 3) {
                mf = Integer.parseInt(fields[3]);
            }
            int maxCon = Integer.MAX_VALUE;
            if(fields.length > 4) {
                maxCon = Integer.parseInt(fields[4]);
            }
            try {
                Unsupervised dis = 
                        new Unsupervised(engine, term, field, 40, mf,
                        maxCon);
                dis.setModels(4);
                currModel = dis.disambiguate(2, K);
                output.println("model:" + currModel.toString());
                String[] labels = currModel.getSenseLabels();
                for(String l : labels) {
                    log.debug(logTag, 0, "label: " + l);
                }
            } catch(Exception e) {
                System.err.println("Exception during disambiguation");
                e.printStackTrace(System.err);
            }
        } else if(q.startsWith(":dist ")) {
            if(currModel != null) {
                String[] fields = parseMessage(q.substring(
                        q.indexOf(' ') + 1).trim());
                Document doc = engine.getDocument(fields[0]);
                List<Posting> pl = doc.getPostings(fields[1]);
                String[] dt = new String[pl.size()];

                //
                // Get the context.
                int n = 0;
                for(Posting p : pl) {
                    dt[n++] = p.getTerm();
                }

                int l = currModel.disambiguate(dt);

                output.println("Disambiguate: " +
                        currModel.getSenseLabels()[l]);
            } else {
                output.println("No current disambiguation model!");
            }

        } else if(q.startsWith(":sw")) {
            wildCaseSensitive = !wildCaseSensitive;
            output.println("wildCaseSensitive: " + wildCaseSensitive);
        } else if(q.startsWith(":wild ")) {

            try {
                String pat = q.substring(q.indexOf(' ') + 1).trim();
                Iterator l = manager.getActivePartitions().iterator();
                long start = System.currentTimeMillis();
                int totalVariants = 0;
                while(l.hasNext()) {
                    InvFileDiskPartition p = (InvFileDiskPartition) l.next();
                    output.print("Partition " + p.getPartitionNumber() +
                            ": ");
                    QueryEntry[] e = p.getMatching(pat, wildCaseSensitive, -1,
                            -1);
                    if(e == null) {
                        output.println("No matches");
                    } else {
                        output.println("");
                        totalVariants += e.length;
                        for(int c = 0; c < e.length; c++) {
                            output.println(" " + e[c]);
                        }
                    }
                }

                output.println("Expansion took: " +
                        (float) (System.currentTimeMillis() - start) / 1000.0 +
                        " s");
                output.println("Total variants: " + totalVariants);
            } catch(Exception e) {
                log.log(logTag, 0, "Exception in :wild", e);
                return 0;
            }
        } else if(q.startsWith(":spell ")) {
            try {
                String pat = q.substring(q.indexOf(' ') + 1).trim();
                Iterator l = manager.getActivePartitions().iterator();
                long start = System.currentTimeMillis();
                StopWatch sw = new StopWatch();

                while(l.hasNext()) {
                    InvFileDiskPartition p = (InvFileDiskPartition) l.next();
                    output.print("Partition " + p.getPartitionNumber() +
                            ": ");
                    sw.start();
                    QueryEntry[] e = p.getSpellingVariants(pat,
                            wildCaseSensitive,
                            10, -1);
                    sw.stop();
                    if(e == null) {
                        output.println("No matches");
                    } else {
                        output.println("");
                        for(int c = 0; c < e.length; c++) {
                            output.println(" " + e[c]);
                        }
                    }
                }

                //output.println("Spelling took: "
                //        + (float) (System.currentTimeMillis() - start) / 1000.0
                //        + " s");
                output.println("Spelling took: " + sw.getTime() + "ms");
            } catch(Exception e) {
                log.log(logTag, 0, "Exception in :spell", e);
                return 0;
            }
        } else if(q.startsWith(":morph ")) {
            String t = q.substring(q.indexOf(' ') + 1).trim();
            Set<String> morphs = morphEn.variantsOf(t);
            List<String> variants = new ArrayList<String>();
            variants.add(t);
            variants.addAll(morphs);
            List<DiskPartition> parts = manager.getActivePartitions();
            int docFreq = 0;
            int totalVariants = 0;
            for(DiskPartition p : parts) {
                output.println("Partition " + p.getPartitionNumber() +
                        " variants: ");
                for(Iterator<String> iter = variants.iterator(); iter.hasNext();) {
                    Entry e = p.getTerm(iter.next());
                    if(e != null) {
                        output.println(" " + e);
                        docFreq += e.getN();
                        totalVariants++;
                    }
                }
            }
            output.println("Total document frequency: " + docFreq);
            output.println("Total variants:           " + totalVariants);
        } else if(q.startsWith(":variants ")) {
            String t = q.substring(q.indexOf(' ') + 1).trim();
            Set<String> morphs = morphEn.variantsOf(t);
            String res = "Variants: ";
            for(Iterator iter = morphs.iterator(); iter.hasNext();) {
                res += iter.next() + " ";
            }
            output.println(res);
        } else if(q.startsWith(":stem ")) {
            String t = q.substring(q.indexOf(' ') + 1).trim();
            int docFreq = 0;
            int totalVariants = 0;
            List<DiskPartition> parts = manager.getActivePartitions();

            for(DiskPartition p : parts) {
                QueryEntry[] terms = ((InvFileDiskPartition) p).getStemMatches(
                        t, false, -1, -1);
                output.print("Partition " + p.getPartitionNumber() + ": ");
                if(terms == null) {
                    output.println("No matches");
                } else {
                    output.println("");
                    totalVariants += terms.length;
                    for(int c = 0; c < terms.length; c++) {
                        output.println(" " + terms[c]);
                        docFreq += terms[c].getN();
                    }
                }
            }
            output.println("Total document frequency: " + docFreq);
            output.println("Total variants: " + totalVariants);

        } else if(q.startsWith(":fi ")) {
            StringTokenizer tok = new StringTokenizer(q.substring(
                    q.indexOf(' ') + 1).trim());
            String field = null;
            int pn = -1;
            try {
                field = tok.nextToken();
                pn = Integer.parseInt(field);
                field = tok.nextToken();
            } catch(NumberFormatException nfe) {
            }

            output.println(field + " " + pn);

            if(pn == -1) {
                for(Iterator i = manager.getFieldIterator(field); i.hasNext();) {
                    output.println("value: " + i.next());
                }
            } else {
                for(Iterator l = manager.getActivePartitions().iterator(); l.hasNext();) {
                    DiskPartition p = (DiskPartition) l.next();
                    if(p.getPartitionNumber() == pn) {

                        DictionaryIterator i =
                                ((InvFileDiskPartition) p).getFieldIterator(field);
                        if(i != null) {
                            output.println(p.toString());
                            while(i.hasNext()) {
                                Entry e = (Entry) i.next();
                                output.println("value: " + e.getName() + " " +
                                        e.getN());
                            }
                        }
                    }
                }
            }

        } else if(q.startsWith(":fs ")) {
            StringTokenizer tok = new StringTokenizer(q.substring(
                    q.indexOf(' ') + 1).trim());
            String field = null;
            int pn = -1;
            try {
                field = tok.nextToken();
                pn = Integer.parseInt(field);
                field = tok.nextToken();
            } catch(NumberFormatException nfe) {
            }

            for(Iterator l = manager.getActivePartitions().iterator(); l.hasNext();) {
                DiskPartition p = (DiskPartition) l.next();
                if(p.getPartitionNumber() == pn) {

                    int[] n = new int[10];
                    int t = 0;
                    DictionaryIterator i =
                            ((InvFileDiskPartition) p).getFieldIterator(field);

                    if(i != null) {
                        while(i.hasNext()) {
                            Entry e = (Entry) i.next();
                            int x = e.getN();
                            if(x == 1) {
                                x = 0;
                            } else {
                                x = Math.min((int) Math.log(x), n.length - 1);
                            }
                            n[x]++;
                            t++;
                        }
                        for(int j = 0; j < n.length; j++) {
                            output.println(j + " " + n[j]);
                        }
                        output.println("Total: " + t);
                    }
                }
            }
        } else if(q.startsWith(":fields")) {
            MetaFile mf = manager.getMetaFile();
            output.println(mf);
        } else if(q.startsWith(":ft ")) {
            String[] fields = parseMessage(q.substring(
                    q.indexOf(' ') + 1).trim());
            String field = fields[0];
            String operator = fields[1].toLowerCase();
            String value = fields[2];

            Integer op = opMap.get(operator);
            if(op == null) {
                output.println("Unknown operator: " + op);
                return 1;
            }

            FieldEvaluator fe = new FieldEvaluator(field, op, value);
            ResultSet set = fe.eval(engine);
            displayResults(set);
        } else if(q.startsWith(":top ")) {
            String f = q.substring(q.indexOf(' ') + 1).trim();
            for(FieldFrequency ff : engine.getTopFieldValues(f, nHits, true)) {
                output.println(ff.getFreq() + " " + ff.getVal());
            }
        } else if(q.startsWith(":del ")) {
            String k = q.substring(q.indexOf(' ') + 1).trim();
            manager.deleteDocument(k);
        } else if (q.startsWith(":delq ")) {
            q = q.substring(q.indexOf(' ') + 1).trim();
            try {
                //
                // Run the query
                ResultSet rs = searcher.search(q, sortSpec, 
                        Searcher.OP_PAND, grammar);
                displayResults(rs);
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(System.in));
                output.print(
                        "Delete the documents matching this query? (y/n) ");
                String response = br.readLine();
                if (response.toLowerCase().equals("y") ||
                        response.toLowerCase().equals("yes")) {
                    List<Result> results = rs.getAllResults(false);
                    for (Result r : results) {
                        manager.deleteDocument(r.getKey());
                    }
                }
            } catch (SearchEngineException se) {
                log.error(logTag, 1, "Error running search", se);
            } catch (IOException e) {
                log.error(logTag, 1, "Failed to read answer", e);
            }

        } else if(q.startsWith(":deld ")) {
            String[] fields =
                    parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            if(fields.length >= 2) {
                int part = Integer.parseInt(fields[0]);
                for(DiskPartition p : manager.getActivePartitions()) {
                    if(p.getPartitionNumber() == part) {
                        for(int i = 1; i < fields.length; i++) {
                            boolean success = p.deleteDocument(Integer.parseInt(fields[i]));
                            output.printf("Deletion of %d in %d returned %s ", Integer.parseInt(fields[i]),
                                    p.getPartitionNumber(), success);
                        }
                    }
                }
            }
        } else if (q.startsWith(":ddup ")) {
            String[] args = parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            if (args.length == 2) {
                int keepPartNum = Integer.parseInt(args[1]);
                DiskPartition keepPart = null;
                int delPartNum = Integer.parseInt(args[0]);
                DiskPartition delPart = null;
                for (DiskPartition p: manager.getActivePartitions()) {
                    if (p.getPartitionNumber() == keepPartNum) {
                        keepPart = p;
                    } else if (p.getPartitionNumber() == delPartNum) {
                        delPart = p;
                    }
                }
                Iterator di = keepPart.getDocumentIterator();
                while(di.hasNext()) {
                    QueryEntry key = (QueryEntry) di.next();
                    boolean success = delPart.deleteDocument(key.toString());
                    if (success) {
                        System.out.println("Removed key " + key + " from partition " + delPartNum);
                    }
                }
            } else {
                System.out.println(":ddup <remove dup from part> <found in part>");
            }
        } else if(q.startsWith(":get ")) {
            StringTokenizer tok = new StringTokenizer(q.substring(
                    q.indexOf(' ') + 1).trim());
            int nTok = tok.countTokens();
            if(nTok >= 3) {

                int pn, d, i;
                String s = null;
                try {
                    s = tok.nextToken();
                    pn = Integer.parseInt(s);
                    s = tok.nextToken();
                    d = Integer.parseInt(s);
                    String[] f = new String[nTok - 2];
                    i = 0;
                    while(tok.hasMoreTokens()) {
                        f[i++] = tok.nextToken();
                    }

                    Iterator l = manager.getActivePartitions().iterator();
                    while(l.hasNext()) {
                        DiskPartition p = (DiskPartition) l.next();
                        if(p.getPartitionNumber() == pn) {

                            for(i = 0; i < f.length; i++) {
                                Object v =
                                        ((InvFileDiskPartition) p).getSavedFieldData(f[i],
                                        d);
                                output.println(f[i] + ": " + v);
                            }
                        }
                    }
                } catch(NumberFormatException nfe) {
                    output.println("Illegal number: " + s);
                }
            }
        } else if(q.startsWith(":gm ")) {
            String[] fields = parseMessage(q.substring(
                    q.indexOf(' ') + 1).trim());
            if(fields.length > 1) {
                SortedSet<FieldValue> matching =
                        engine.getMatching(fields[0], fields[1]);
                for(FieldValue fv : matching) {
                    output.format("%.3f %s\n", fv.getScore(), fv.getValue());
                }
            }
        } else if(q.startsWith(":gav ")) {
            StringTokenizer tok = new StringTokenizer(q.substring(
                    q.indexOf(' ') + 1).trim());
            String field = tok.nextToken();
            Iterator l = manager.getActivePartitions().iterator();
            while(l.hasNext()) {
                Iterator di = ((DiskPartition) l.next()).getDocumentIterator();
                while(di.hasNext()) {
                    QueryEntry key = (QueryEntry) di.next();
                    List classes = manager.getAllFieldValues(field,
                            (String) key.getName());

                    //if (classes == null) {
                    //    output.println(key);
                    //} else {
                    output.println(classes);
                //}
                }
            }
        } else if(q.startsWith(":gtv ")) {
            StringTokenizer tok = new StringTokenizer(q.substring(
                    q.indexOf(' ') + 1).trim());
            String field = tok.nextToken();
            int n = 20;
            if(tok.hasMoreElements()) {
                String num = tok.nextToken();
                try {
                    n = Integer.valueOf(num);
                } catch(NumberFormatException e) {
                    output.println("Invalid number of results specified");
                    return 1;
                }
            }
            List vals = engine.getTopFieldValues(field, n, true);
            for(Object o : vals) {
                output.println(o);
            }
        } else if(q.startsWith(":docpost ")) {
            StringTokenizer tok = new StringTokenizer(q.substring(
                    q.indexOf(' ') + 1).trim());
            if(tok.countTokens() > 0) {
                //
                // Looks up a doc by its key and prints
                // out the term ids associated with it in its postings
                String docKey = tok.nextToken();

                //
                // Get an Entry for this document
                for(Iterator partIt = manager.getActivePartitions().iterator(); partIt.hasNext();) {
                    DiskPartition p = (DiskPartition) partIt.next();
                    QueryEntry dt = p.getDocumentTerm(docKey);
                    if(dt != null) {
                        output.println("Doc postings for docID " +
                                dt.getID() + " in partition " +
                                p.getPartitionNumber() + ":");
                        output.println(dt.getN() + " terms");
                        for(PostingsIterator postIt =
                                dt.iterator(new PostingsIteratorFeatures()); postIt.next();) {
                            int tid = postIt.getID();
                            output.print(" " + p.getTerm(tid).toString() +
                                    " (" + tid + "," + postIt.getFreq() + ")");
                        }
                        output.println("");
                    }
                }
            }
        } else if(q.startsWith(":pt ")) {
            String term = q.substring(q.indexOf(' ') + 1).trim();
            Iterator l = manager.getActivePartitions().iterator();
            Random rand = new Random();
            while(l.hasNext()) {
                DiskPartition p = (DiskPartition) l.next();
                QueryEntry e = p.getTerm(term);
                if(e == null) {
                    continue;
                }
                try {
                    PostingsIterator pi =
                            e.iterator(new PostingsIteratorFeatures());
                    int[] docs = new int[pi.getN()];
                    int n = 0;
                    while(pi.next()) {
                        docs[n++] = pi.getID();
                    }
                    pi.reset();
                    for(int i = 0; i < docs.length; i++) {
                        if(!pi.findID(docs[i])) {
                            output.println("Linear couldn't find: " +
                                    docs[i]);
                        }
                    }
                    for(int i = 0; i < docs.length * 3; i++) {
                        int x = rand.nextInt(docs.length);
                        int y = rand.nextInt(docs.length);
                        int t = docs[x];
                        docs[x] = docs[y];
                        docs[y] = t;
                    }
                    pi.reset();
                    for(int i = 0; i < docs.length; i++) {
                        if(!pi.findID(docs[i])) {
                            output.println("Random couldn't find: " +
                                    docs[i]);
                            output.println(Util.arrayToString(docs));
                        }
                    }
                } catch(Exception ite) {
                    output.println("Exception: " + ite);
                }
            }
        } else if(q.startsWith(":post ")) {
            StringTokenizer tok = new StringTokenizer(q.substring(
                    q.indexOf(' ') + 1).trim());
            if(tok.countTokens() > 0) {
                String type = tok.nextToken().toLowerCase();

                String[] terms = new String[tok.countTokens()];
                int i = 0;
                while(tok.hasMoreTokens()) {
                    terms[i++] = CharUtils.decodeUnicode(tok.nextToken());
                }
                Iterator l = manager.getActivePartitions().iterator();
                while(l.hasNext()) {
                    DiskPartition p = (DiskPartition) l.next();
                    output.println("Postings for partition " +
                            p.getPartitionNumber() + ":");

                    boolean fielded = type.toLowerCase().equals("field");
                    for(i = fielded ? 1 : 0; i < terms.length; i++) {
                        QueryEntry e = p.getTerm(terms[i]);

                        if(e == null) {
                            output.println(" None");
                        } else {
                            output.print(e + " (" + e.getN() + "): ");

                            PostingsIteratorFeatures feat =
                                    new PostingsIteratorFeatures();
                            if(type.toLowerCase().equals("doc")) {
                                try {
                                    PostingsIterator pi = e.iterator(feat);
                                    while(pi.next()) {
                                        output.print(" " + pi.getID());
                                        output.flush();
                                    }
                                } catch(Exception ite) {
                                    output.println("Exception: " + ite);
                                }
                            } else if(type.toLowerCase().equals("np")) {

                                try {
                                    feat.setPositions(true);
                                    feat.setFields(null);
                                    PostingsIterator pi = e.iterator(feat);
                                    while(pi.next()) {
                                        int[][] posn =
                                                ((PosPostingsIterator) pi).getPositions();
                                    }
                                } catch(Exception ite) {
                                    output.println("Exception: " + ite);
                                }
                            } else if(type.toLowerCase().equals("freq")) {
                                PostingsIterator pi = e.iterator(feat);
                                while(pi.next()) {
                                    output.print(" " + pi.getID() + " " +
                                            pi.getFreq());
                                    output.flush();
                                }
                            } else if(type.toLowerCase().equals("field")) {

                                output.println("");
                                if(!terms[0].equals("null")) {
                                    feat.setFields(
                                            new String[]{terms[0].equals("body")
                                        ? null
                                        : terms[0]
                                    },
                                            (InvFileDiskPartition) p);
                                } else {
                                    feat.setFields(null);
                                }
                                feat.setPositions(true);

                                PostingsIterator pi = e.iterator(feat);
                                while(pi.next()) {

                                    output.print(" " + pi.getID() + " " +
                                            pi.getFreq());
                                    if(p.isDeleted(pi.getID())) {
                                        output.println(" deleted");
                                        continue;
                                    }
                                    int[][] posn =
                                            ((PosPostingsIterator) pi).getPositions();
                                    for(int j = 0; j < posn.length; j++) {
                                        if(posn[j][0] > 0) {
                                            output.print(" (" + j + ") " + Util.arrayToString(
                                                    posn[j], 0,
                                                    posn[j][0] + 1));
                                        }
                                    }
                                    output.println("");
                                }
                            }
                            output.println("");
                        }
                    }
                }
            }
        } else if(q.startsWith(":main ")) {
            try {
                int pn =
                        Integer.parseInt(q.substring(q.indexOf(' ') + 1).trim());
                Iterator l = manager.getActivePartitions().iterator();
                while(l.hasNext()) {
                    DiskPartition p = (DiskPartition) l.next();
                    if(p.getPartitionNumber() == pn) {
                        Iterator d = p.getMainDictionaryIterator();
                        while(d.hasNext()) {
                            Entry e = (Entry) d.next();
                            output.println(e +
                                    // " (" +
                                    // Util.escape(t.getName().toString())
                                    // +
                                    // ")" +
                                    ": " + e.getID());
                        }
                        break;
                    }
                }
            } catch(Exception nfe) {
                log.error(logTag, 0, "Exception", nfe);
                return 0;
            }
        } else if(q.startsWith(":lmain ")) {
            try {
                int pn =
                        Integer.parseInt(q.substring(q.indexOf(' ') + 1).trim());
                Iterator l = manager.getActivePartitions().iterator();
                while(l.hasNext()) {
                    DiskPartition p = (DiskPartition) l.next();
                    if(p.getPartitionNumber() == pn) {
                        LightIterator d = p.getMainDictionary().literator();
                        while(d.next()) {
                            output.println(d.getName() +
                                    // " (" +
                                    // Util.escape(t.getName().toString())
                                    // +
                                    // ")" +
                                    ": " + d.getID());
                        }
                        break;
                    }
                }
            } catch(Exception nfe) {
                log.error(logTag, 0, "Exception", nfe);
                return 0;
            }

        } else if(q.startsWith(":merge")) {

            int nTokens = 0;
            int p = q.indexOf(' ');
            StringTokenizer st = null;
            if(p != -1) {
                st = new StringTokenizer(q.substring(p).trim());
                nTokens = st.countTokens();
            }

            if(nTokens == 0) {
                output.println("Merging all partitions");
                manager.mergeAll();
            } else {
                ArrayList<Integer> parts = new ArrayList<Integer>();
                while(st.hasMoreTokens()) {
                    try {
                        parts.add(new Integer(st.nextToken()));
                    } catch(NumberFormatException nfe) {
                    }
                }

                log.debug(logTag, 0, "parts: " + parts);

                PartitionManager.Merger merger =
                        manager.getMergerFromNumbers(parts);
                merger.merge();
            }
        } else if(q.startsWith(":classmerge")) {

            int nTokens = 0;
            int p = q.indexOf(' ');
            StringTokenizer st = null;
            if(p != -1) {
                st = new StringTokenizer(q.substring(p).trim());
                nTokens = st.countTokens();
            }

            if(nTokens == 0) {
                output.println("Merging all classifier partitions");
                engine.getClassifierManager().mergeAll();
            } else {
                ArrayList<Integer> parts = new ArrayList<Integer>();
                while(st.hasMoreTokens()) {
                    try {
                        parts.add(new Integer(st.nextToken()));
                    } catch(NumberFormatException nfe) {
                    }
                }

                PartitionManager.Merger merger =
                        engine.getClassifierManager().
                        getMergerFromNumbers(parts);
                if(merger != null) {
                merger.merge();
                } else {
                    output.println("Null merger for classifier partitions");
                }
            }
        } else if(q.startsWith(":clustermerge")) {
            int nTokens = 0;
            int p = q.indexOf(' ');
            StringTokenizer st = null;
            if(p != -1) {
                st = new StringTokenizer(q.substring(p).trim());
                nTokens = st.countTokens();
            }

            ClusterManager cm = engine.getClusterManager();

            if(nTokens == 0) {
                output.println("Merging all cluster partitions");
                cm.mergeAll();
            } else {
                ArrayList parts = new ArrayList();
                while(st.hasMoreTokens()) {
                    try {
                        parts.add(new Integer(st.nextToken()));
                    } catch(NumberFormatException nfe) {
                    }
                }

                log.debug(logTag, 0, "parts: " + parts);

                PartitionManager.Merger merger = cm.getMergerFromNumbers(parts);
                merger.merge();
            }
        } else if(q.startsWith(":dict")) {

            String[] vals = parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            int partNum = -1;
            String start = null;
            String end = null;
            if(vals.length > 0) {
                partNum = Integer.parseInt(vals[0]);
            }
            if(vals.length > 1) {
                start = vals[1];
            }
            if(vals.length > 2) {
                end = vals[2];
            }
            Iterator l = manager.getActivePartitions().iterator();
            while(l.hasNext()) {
                DiskPartition part = (DiskPartition) l.next();
                if((partNum == -1) || (partNum == part.getPartitionNumber())) {
                    Iterator di = part.getMainDictionaryIterator(start, end);
                    while(di.hasNext()) {
                        QueryEntry e = (QueryEntry) di.next();
                        output.println(e);
                    }
                }
            }
        } else if(q.startsWith(":di")) {
            int partNum = -1;
            try {
                partNum =
                        Integer.parseInt(q.substring(q.indexOf(' ') + 1).trim());
            } catch(Exception e) {
            // no part num
            }
            Iterator l = manager.getActivePartitions().iterator();
            while(l.hasNext()) {
                DiskPartition part = (DiskPartition) l.next();
                if((partNum == -1) || (partNum == part.getPartitionNumber())) {
                    Iterator di = part.getDocumentIterator();
                    while(di.hasNext()) {
                        QueryEntry key = (QueryEntry) di.next();
                        output.println(key.getID() + " " + key);
                    }
                }
            }
        } else if(q.startsWith(":docTerm ")) {
            String key = q.substring(q.indexOf(' ') + 1);
            DocKeyEntry dke = manager.getDocumentTerm(key);
            if(dke != null) {
                output.format("%d: %s\n", dke.getID(), dke.getName());
            } else {
                output.println("No document for key: " + key);
            }
        } else if(q.startsWith(":ob")) {
            Iterator l = manager.getActivePartitions().iterator();
            int ones = 0;
            int tot = 0;
            while(l.hasNext()) {
                Iterator di =
                        ((DiskPartition) l.next()).getMainDictionaryIterator();
                while(di.hasNext()) {
                    QueryEntry e = (QueryEntry) di.next();
                    if(e.getN() <= 5) {
                        ones++;
                    }
                    tot++;
                }
            }
            output.println("ones: " + ones);
            output.println("tot:  " + tot);
        } else if(q.startsWith(":docit ")) {
            String[] vals = parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            int partNum = -1;
            String start = null;
            String end = null;
            if(vals.length > 0) {
                partNum = Integer.parseInt(vals[0]);
            }
            if(vals.length > 1) {
                start = vals[1];
            }
            if(vals.length > 2) {
                end = vals[2];
            }
            for(DiskPartition part : manager.getActivePartitions()) {
                if((partNum == -1) || (partNum == part.getPartitionNumber())) {
                    Iterator di = part.getDocumentIterator();
                    while(di.hasNext()) {
                        QueryEntry e = (QueryEntry) di.next();
                        output.println(e);
                    }
                }
            }
        } else if(q.startsWith(":ddi")) {
            StopWatch sw = new StopWatch();
            sw.start();
            Iterator l = manager.getActivePartitions().iterator();
            PostingsIteratorFeatures pif = new PostingsIteratorFeatures();
            while(l.hasNext()) {
                Iterator di =
                        ((DiskPartition) l.next()).getMainDictionaryIterator();
                while(di.hasNext()) {
                    QueryEntry e = null;
                    try {
                        e = (QueryEntry) di.next();
                        output.println("e: " + e);
                        log.setLevel(4);
                        pif.setCaseSensitive(true);
                        PostingsIterator pi = e.iterator(pif);
                        int nonNull = 0;
                        if(pi != null) {
                            nonNull++;
                            while(pi.next()) {
                            // output.print(pi.getID() + " ");
                            }
                        // output.println("");
                        }
                        pif.setCaseSensitive(false);
                        pi = e.iterator(pif);
                        if(pi != null) {
                            nonNull++;
                            while(pi.next()) {
                            // output.print(pi.getID() + " ");
                            }
                        // output.println("");
                        }
                        log.setLevel(3);
                        if(nonNull == 0) {
                            output.println("Both null: " + e);
                        }
                    } catch(Exception ex) {
                        output.println("Exception: " + e);
                        ex.printStackTrace(output);
                        return 0;
                    }
                }
            }
            sw.stop();
            output.println("Iteration took: " + sw.getTime() + "ms");

        } else if(q.startsWith(":ds")) {
            int[] levels = new int[10];
            int[] sub10 = new int[10];
            Iterator l = manager.getActivePartitions().iterator();
            int x = 0;
            while(l.hasNext()) {
                Iterator di =
                        ((DiskPartition) l.next()).getMainDictionaryIterator();
                while(di.hasNext()) {
                    QueryEntry e = (QueryEntry) di.next();
                    int n = e.getN();
                    if(n < sub10.length) {
                        sub10[n]++;
                    }
                    levels[(int) Math.floor(Math.log10(n))]++;
                    x++;
                    if(x % 100000 == 0) {
                        output.println("Processed " + x + " entries");
                    }
                }
            }
            output.format("%20s%30s\n", "Frequncy", "Count");
            for(int i = 1; i < sub10.length; i++) {
                output.format("%20d%30d\n", i, sub10[i]);
            }
            output.format("%20s%30s\n", "Log Frequncy", "Count");
            for(int i = 0; i < levels.length; i++) {
                output.format("%20d%30d\n", i, levels[i]);
            }

        } else if(q.startsWith(":dt ")) {
            String key = q.substring(q.indexOf(' ')).trim();
            for(Iterator l = manager.getActivePartitions().iterator(); l.hasNext();) {
                DiskPartition dp = (DiskPartition) l.next();
                QueryEntry e = dp.getDocumentTerm(key);
                if(e != null) {
                    output.println(dp + ": " + e.getID() +
                            " " + dp.isDeleted(e.getID()));
                }
            }
        } else if(q.startsWith(":sim ")) {
            q = q.substring(q.indexOf(' ') + 1).trim();
            String[] values = parseMessage(q);
            try {
                String key1 = values[0];
                String key2 = values[1];

                DocumentVector dv1 = engine.getDocumentVector(key1);
                DocumentVector dv2 = engine.getDocumentVector(key2);

                if(dv1 == null) {
                    output.println("No such doc: " + key1);
                } else if(dv2 == null) {
                    output.println("No such doc: " + key2);
                } else {
                    output.println("Similarity score: " +
                            dv1.getSimilarity(dv2));
                }
            } catch(StringIndexOutOfBoundsException e) {
                output.println("Syntax error, try \":help\"");
            }
        } else if(q.startsWith(":simwords ")) {
            q = q.substring(q.indexOf(' ') + 1).trim();
            try {
                String key1 = q.substring(0, q.indexOf(' '));
                String key2 = q.substring(q.indexOf(' ') + 1).trim();

                DocumentVector dv1 = engine.getDocumentVector(key1);
                DocumentVector dv2 = engine.getDocumentVector(key2);

                if(dv1 == null) {
                    output.println("No such doc: " + key1);
                } else if(dv2 == null) {
                    output.println("No such doc: " + key2);
                } else {
                    Map<String, Float> words = dv1.getSimilarityTerms(dv2);
                    for(Iterator it = words.keySet().iterator(); it.hasNext();) {
                        String curr = (String) it.next();
                        output.println(curr + ": \t" + words.get(curr));
                    }
                }
            } catch(StringIndexOutOfBoundsException e) {
                output.println("Syntax error, try \":help\"");
            }
        } else if(q.startsWith(":csim")) {
            q = q.substring(q.indexOf(' ') + 1).trim();
            String cn = q.substring(0, q.indexOf(' '));
            String key = q.substring(q.indexOf(' ') + 1).trim();
            ClassifierManager cm = engine.getClassifierManager();
            output.println("Similarity score: " + cm.similarity(cn, key));
        } else if(q.startsWith(":cts ")) {
            //
            // Calcuate term stats for a given partition using the current
            // term stats dictionary.
            String[] vals = parseMessage(q.substring(q.indexOf(' ')).trim());
            int partNum = Integer.parseInt(vals[0]);
            for(Iterator i = manager.getActivePartitions().iterator(); i.hasNext();) {
                DiskPartition p = (DiskPartition) i.next();
                if(p.getPartitionNumber() != partNum) {
                    continue;
                }
                DocumentVectorLengths dvl = p.getDVL();
                try {
                    dvl.calculateLengths(p, manager.getTermStatsDict(), true);
                } catch(Exception e) {
                    output.println("Error calculating");
                    e.printStackTrace(output);
                }
            }
        } else if(q.startsWith(":rts")) {
            try {
                manager.recalculateTermStats();
            } catch(IOException ex) {
                output.println("Error generating term stats: " + ex);
            } catch(FileLockException ex) {
                output.println("Error generating term stats: " + ex);
            }
        } else if(q.startsWith(":findsim")) {
            String[] vals = parseMessage(q.substring(q.indexOf(' ')).trim());
            String key = vals[0];
            double skim = vals.length > 1 ? Double.parseDouble(vals[1]) : 1.0;
            DocumentVector dv = engine.getDocumentVector(key);
            if(dv != null) {
                ResultSet rs = ((DocumentVectorImpl) dv).findSimilar("-score",
                        skim);
                displayResults(rs);
            } else {
                output.println("No such doc: " + key);
            }
        } else if(q.startsWith(":mfs")) {
            String[] vals = parseMessage(q.substring(q.indexOf(' ')).trim());
            List<DocumentVector> dvs = new ArrayList<DocumentVector>();
            for(String key : vals) {
                DocumentVector dv = engine.getDocumentVector(key);
                if(dv != null) {
                    System.out.println("dv: " + dv);
                    dvs.add(dv);
                }
            }
            MultiDocumentVectorImpl dv = new MultiDocumentVectorImpl(dvs);
            ResultSet rs = dv.findSimilar("-score");
            displayResults(rs);
            
        } else if(q.startsWith(":ffs ")) {
            String[] vals = parseMessage(q.substring(q.indexOf(' ')).trim());
            String key = vals.length > 1 ? vals[1] : vals[0];
            String field = vals.length > 1 ? vals[0] : null;
            double skim = vals.length > 2 ? Double.parseDouble(vals[2]) : 1.0;
            DocumentVector dv;
            if(field != null) {
                dv = engine.getDocumentVector(key, field);
            } else {
                dv = engine.getDocumentVector(key);
            }
            if(dv != null) {
                ResultSet rs = ((DocumentVectorImpl) dv).findSimilar("-score",
                        skim);
                displayResults(rs);
            } else {
                output.println("No such doc: " + key);
            }
        } else if(q.startsWith(":cfs ")) {
            String[] vals = parseMessage(q.substring(q.indexOf(' ')).trim());
            if(vals.length > 3) {
                String key = vals[vals.length - 1];
                List<WeightedField> lwf = new ArrayList<WeightedField>();
                for(int i = 0; i < vals.length - 2; i += 2) {
                    String fname = vals[i];
                    if(fname.equals("null")) {
                        fname = null;
                    }
                    lwf.add(new WeightedField(fname,
                            Float.parseFloat(vals[i + 1])));
                }
                DocumentVector dv = engine.getDocumentVector(key,
                        lwf.toArray(new WeightedField[0]));
                if(dv != null) {
                    ResultSet rs = dv.findSimilar("-score");
                    displayResults(rs);
                } else {
                    output.println("No such doc: " + key);
                }
            }
        } else if(q.startsWith(":checksim")) {
            String key = q.substring(q.indexOf(' ')).trim();
            DocumentVectorImpl dv =
                    (DocumentVectorImpl) engine.getDocumentVector(key);
            if(dv != null) {
                ResultSet rsFull = dv.findSimilar();
                ResultSet rs100 = dv.findSimilar("-score", 0.75);
                //
                // Now let's see how far they're off from eachother.  We'll
                // evaluate based on what would be the first three pages
                // of results.  Nobody is likely to look beyond that anyway.
                try {
                    List rFull = rsFull.getResults(0, 30);
                    List r100 = rs100.getResults(0, 30);
                    //
                    // Cheap test:
                    int fullLen = rFull.size();
                    rFull.retainAll(r100);
                    int newLen = rFull.size();
                    output.println("Sets overlapped by " + newLen + "/" +
                            fullLen);
                    displayResults(rs100);
                } catch(SearchEngineException e) {
                    output.println("Ooops: " + e);
                }
            } else {
                output.println("No such doc: " + key);
            }
        } else if(q.startsWith(":simtest")) {
            String val = q.substring(q.indexOf(' ')).trim();
            float cutOff = Float.parseFloat(val);
            try {
                ResultSet rs = searcher.search("the", "");
                List results = rs.getResults(0, 500);
                int totalResults = 0;
                long fullTime = 0;
                long partTime = 0;
                int totalOverlap = 0;
                for(Iterator it = results.iterator(); it.hasNext();) {
                    Result r = (Result) it.next();
                    DocumentVectorImpl dv =
                            (DocumentVectorImpl) r.getDocumentVector();
                    ResultSet rsFull = dv.findSimilar();
                    fullTime += rsFull.getQueryTime();
                    ResultSet rs100 = dv.findSimilar("-score", 0.75);
                    partTime += rs100.getQueryTime();
                    List rFull = rsFull.getResults(0, 30);
                    List r100 = rs100.getResults(0, 30);
                    //
                    // Cheap test:
                    int fullLen = rFull.size();
                    rFull.retainAll(r100);
                    int newLen = rFull.size();
                    totalResults += fullLen;
                    totalOverlap += newLen;
                    output.println("Sets overlapped by " + newLen + "/" +
                            fullLen);
                }
                //
                // Print out total overlap
                float percent = (totalOverlap / (float) totalResults) * 100;
                output.println("Cumulative overlap: " + totalOverlap + "/" +
                        totalResults + " (" + percent + "&)");
                output.println("Total time for full findSims: " + fullTime +
                        "ms");
                output.println("Total time for partial findSims: " +
                        partTime + "ms");
                output.println("Speedup: " + fullTime / partTime);
            } catch(SearchEngineException e) {
                output.println("Ooops: " + e);
            }
        } else if(q.startsWith(":checkparts")) {
            for(Iterator it = manager.getActivePartitions().iterator(); it.hasNext();) {
                InvFileDiskPartition dp = (InvFileDiskPartition) it.next();
                int count = 0;
                for(DictionaryIterator dit = dp.getFieldIterator("asin"); dit.hasNext();) {
                    String asin = (String) ((QueryEntry) dit.next()).getName();
                    QueryEntry e = dp.getDocumentTerm(asin);
                    if(e == null) {
                        count++;
                    }
                }
                output.println(dp + " has " + count +
                        " null docs, and a total of " + dp.getNDocs() + " docs");

            }
        } else if(q.startsWith(":fdv")) {
            String[] vals = parseMessage(q);
            DocumentVector dv = vals.length == 2 ? engine.getDocumentVector(vals[1], "")
                    : engine.getDocumentVector(vals[1], vals[2]);
            if(dv == null) {
                output.println("Unknown key: " + vals[1]);
            } else {
                dumpDocVectorByWeight(dv);
            }
        } else if(q.startsWith(":dv ")) {
            String key = q.substring(q.indexOf(' ')).trim();
            DocumentVector dv = engine.getDocumentVector(key);
            if(dv == null) {
                output.println("Unknown key: " + key);
            } else {
                dumpDocVectorByWeight(dv);
            }
        } else if(q.startsWith(":pdv ")) {
            String[] vals = parseMessage(q.substring(q.indexOf(' ')).trim());
            int pn = Integer.parseInt(vals[0]);
            int did = Integer.parseInt(vals[1]);
            for(DiskPartition part : manager.getActivePartitions()) {
                if(part.getPartitionNumber() == pn) {
                    DocKeyEntry dke = part.getDocumentTerm(did);
                    output.println("key: " + dke.getName());
                    DocumentVector dv = new DocumentVectorImpl(engine, dke, null);
                    output.println("vector: " + dv);
                }
            }
       } else if(q.startsWith(":pkey ")) {
            String[] vals = parseMessage(q.substring(q.indexOf(' ')).trim());
            int pn = Integer.parseInt(vals[0]);
            Set<String> keys = new HashSet<String>();
            for(int i = 1; i < vals.length; i++) {
                int did = Integer.parseInt(vals[i]);
                for(DiskPartition part : manager.getActivePartitions()) {
                    if(part.getPartitionNumber() == pn) {
                        DocKeyEntry dke = part.getDocumentTerm(did);
                        output.printf("id: %d key: \"%s\"\n", did, dke.getName());
                        keys.add(dke.getName().toString());
                    }
                }
            }
            output.println("keys: " + keys);
            if(keys.size() == 1) {
                String k = keys.iterator().next();
                for(DiskPartition part : manager.getActivePartitions()) {
                    if(part.getPartitionNumber() == pn) {
                        DocKeyEntry dke = part.getDocumentTerm(k);
                        DocumentVector dv = new DocumentVectorImpl(engine, dke, null);
                        output.println("key: " + dke);
                        output.println("dv: " + dv);
                    }
                }
            }
        } else if(q.startsWith(":cterms")) {
            for(Iterator it = engine.getClusterManager().getActivePartitions().
                    iterator(); it.hasNext();) {
                ClusterDiskPartition cdp = (ClusterDiskPartition) it.next();
                output.println("Terms in Partition " + cdp + ":");
                for(Iterator dit = cdp.getMainDictionaryIterator(); dit.hasNext();) {
                    QueryEntry e = (QueryEntry) dit.next();
                    output.println(e.getName() + " (" + e.getID() + ")");
                }
            }

        } else if(q.startsWith(":h") && !q.startsWith(":hg")) {

            //
            // Help!
            help();

        } else if(q.startsWith(":dp")) {
            displayPassage = !displayPassage;
            setPrompt();
        } else if(q.trim().equals(":c")) {
            complexPassDisplay = !complexPassDisplay;
            setPrompt();
        } else if(q.startsWith(":child")) {
            String term = q.substring(q.indexOf(' ') + 1).trim();
            Iterator l = manager.getActivePartitions().iterator();
            while(l.hasNext()) {
                InvFileDiskPartition p = (InvFileDiskPartition) l.next();
                DiskTaxonomy tax = p.getTaxonomy();
                if(tax == null) {
                    output.println("no taxonoomy");
                } else {
                    Set children = tax.getChildren(term);
                    output.println("Partition " + p.getPartitionNumber() +
                            ":");
                    if(children != null) {
                        for(Iterator iter = children.iterator(); iter.hasNext();) {
                            output.println(iter.next());
                        }
                    } else {
                        output.println(" No entries");
                    }
                }
            }
        } else if(q.startsWith(":par")) {
            String term = q.substring(q.indexOf(' ') + 1).trim();
            Iterator l = manager.getActivePartitions().iterator();
            while(l.hasNext()) {
                InvFileDiskPartition p = (InvFileDiskPartition) l.next();
                DiskTaxonomy tax = p.getTaxonomy();
                if(tax == null) {
                    output.println("no taxonomy");
                } else {
                    Set parents = tax.getParents(term);
                    output.println("Partition " + p.getPartitionNumber() +
                            ":");
                    if(parents != null) {
                        for(Iterator iter = parents.iterator(); iter.hasNext();) {
                            output.println(iter.next());

                        }
                    } else {
                        output.println(" No entries");
                    }
                }
            }
        } else if(q.startsWith(":sub")) {
            String term = q.substring(q.indexOf(' ') + 1).trim();
            Iterator l = manager.getActivePartitions().iterator();
            while(l.hasNext()) {
                InvFileDiskPartition p = (InvFileDiskPartition) l.next();
                DiskTaxonomy tax = p.getTaxonomy();
                if(tax == null) {
                    output.println("no taxonomy");
                } else {
                    Set subC = tax.getSubsumed(term, -1);
                    output.println("Partition " + p.getPartitionNumber() +
                            ":");
                    if(subC != null) {
                        for(Iterator iter = subC.iterator(); iter.hasNext();) {
                            output.println(iter.next());
                        }
                    } else {
                        output.println(" No subsumed concepts");
                    }
                }
            }
        } else {
            return 0;
        }
        return 1;
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
                if(((WeightedFeature) o1).getWeight() >
                        ((WeightedFeature) o2).getWeight()) {
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
            output.println(wf.toString());
        }
        output.format("length: %.2f\n", Math.sqrt(sum));
    }

    public static void main(String[] args) throws java.io.IOException,
            NumberFormatException, SearchEngineException {

        if(args.length == 0) {
            usage();
            return;
        }

        String flags = "d:l:nf:x:pt:o:";
        Getopt gopt = new Getopt(args, flags);
        String inputFile = null;
        int logLevel = 3;
        int c;
        String indexDir = null;
        URL cmFile = null;
        String engineType = null;
        
        PrintStream output = System.out;

        boolean vln = false;
        boolean profiling = false;

        if(args.length == 0) {
            usage();
            return;
        }

        Thread.currentThread().setName("QueryTest");
        
        for(Handler h : Logger.getLogger("").getHandlers()) {
            h.setFormatter(new SimpleLabsLogFormatter());
        }

        //
        // Handle the options.
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'l':
                    logLevel = Integer.parseInt(gopt.optArg);
                    break;

                case 'n':
                    vln = true;
                    break;

                case 'f':
                    inputFile = gopt.optArg;
                    break;
                    
                case 'o':
                    output = new PrintStream(gopt.optArg, "utf-8");
                    break;

                case 'p':
                    profiling = true;
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
        log = Log.getLog();
        log.setStream(output);
        log.setLevel(logLevel);


        QueryTest qt = new QueryTest(
                cmFile,
                indexDir,
                engineType,
                "partNum,\\t,docID,\\t,aura-type,aura-key",
                "-score", 
                output);

        BufferedReader input;

        if(inputFile != null) {
            input = new BufferedReader(new InputStreamReader(
                    new FileInputStream(inputFile), "UTF-8"));
            qt.ttyInput = false;
        } else {
            input = new BufferedReader(new InputStreamReader(System.in));
        }

        qt.nHits = 10;
        qt.setPrompt();
        qt.stats();

        output.print(qt.prompt);
        output.flush();

        String q;

        //
        // Quit at end of file.
        inputLoop:
        while((q = input.readLine()) != null) {
            if(q.length() == 0) {
                output.print(qt.prompt);
                output.flush();
                continue;
            }

            if(!qt.ttyInput) {
                output.println(q);
            }

            //
            // Default is a full query.
            if(q.indexOf(':') != 0) {
                q = ":qu " + q;
            }

            if(!q.startsWith(":qu")) {
                q = q.trim();
            }
            /*
            //
            // See if we are doing a pipe
            int pipeInd = q.indexOf(" |");
            int redirInd = q.indexOf(" >");
            int ret = -2;
            if (pipeInd >= 0) {
            String pipe = q.substring(pipeInd + 2).trim();
            q = q.substring(0, pipeInd);
            String[] subCmd = pipe.split(" ");
            PrintStream stdOut = output;
            Process subProc = Runtime.getRuntime().exec(subCmd);
            System.setOut(new PrintStream(subProc.getOutputStream()));
            Pipe.between(subProc.getInputStream(), stdOut);
            ret = qt.processCommand(q);
            stdOut.flush();
            output.close();
            try {
            subProc.waitFor();
            } catch (InterruptedException e) {
            stdOut.println("Interrupted");
            }
            System.setOut(stdOut);
            subProc.destroy();
            } else if (redirInd >= 0) {
            String outFile = q.substring(redirInd + 2).trim();
            q = q.substring(0, redirInd);
            PrintStream stdOut = output;
            FileOutputStream fOut = new FileOutputStream(new File(outFile));
            System.setOut(new PrintStream(fOut));
            Log.setStream(fOut);
            ret = qt.processCommand(q);
            output.flush();
            output.close();
            Log.setStream(stdOut);
            System.setOut(stdOut);
            } else {
            ret = qt.processCommand(q);
            }
             */

            int ret = qt.processCommand(q);
            switch(ret) {
                case 0:
                    output.println("Unknown command");
                    break;
                case -1:
                    break inputLoop;
            }

            output.print(qt.prompt);
            output.flush();
        }

        output.println("Average time over " + qt.nQueries + " queries: " +
                (float) qt.totalTime / (float) qt.nQueries + " ms");

        if(profiling) {
            output.println("Sleeping a while");
            try {
                Thread.sleep(600000);
            } catch(Exception e) {
            }
        }

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

        protected TextHighlighter th = new TextHighlighter();

        /**
         * Makes a display spec from a string representation. The representation
         * is a comma separated list of field names. In addition to the names
         * defined in a given index, the user may use the sequences
         * <code>\n</code> and <code>\t</code> to display newlines or tabs
         * in the output.
         */
        public DisplaySpec(String s) {

            //
            // Split the input at commas.
            StringTokenizer tok = new StringTokenizer(s, ", ");
            fields = new String[tok.countTokens()];
            int i = 0;
            while(tok.hasMoreTokens()) {
                fields[i++] = tok.nextToken();
            }
        }

        /**
         * Format a result.
         */
        public void format(Result r) {
            format("", r);
        }

        /**
         * Format a result.
         */
        public void format(String prefix, Result r) {

            output.print(prefix);

            Document d = null;

            //
            // Display the fields that the user asked for.
            // We'll catch a few special cases.
            for(int i = 0; i < fields.length; i++) {

                if(i > 0) {
                    output.print(" ");
                }

                String fn = fields[i];

                if(fn.equals("dockey")) {
                    output.print(r.getKey());
                } else if(fn.equals("score")) {
                    output.print(scoreForm.format(r.getScore()));
                } else if(fn.equals("indexName")) {
                    output.print(r.getIndexName());
                } else if(fn.equals("docID")) {
                    output.print(((ResultImpl) r).getDocID());
                } else if(fn.equals("partNum")) {
                    output.print(((ResultImpl) r).getPartNum());
                } else if(fn.equals("dvl")) {
                    ResultImpl ri = (ResultImpl) r;
                    output.print(ri.getPart().getDocumentVectorLength(
                            ri.getDocID()));
                } else if(fn.startsWith("v:")) {
                    fn = fn.substring(2);
                    if(d == null) {
                        d = r.getDocument();
                    }
                    List<Posting> post = d.getPostings(fn);
                    if(post != null) {
                        output.println(post);
                    }
                } else if(fn.equals("\\n")) {
                    output.print('\n');
                } else if(fn.equals("\\t")) {
                    output.print('\t');
                } else {
                    List val = r.getField(fn);
                    if(val.size() == 1) {
                        output.print(val.get(0));
                    } else {
                        output.print(val);
                    }
                }
            }
            output.println("");
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
                        output.println("Hits from " + me.getKey());
                        int k = 0;
                        for(Iterator p = passages.iterator(); p.hasNext() && k <
                                4; k++) {
                            Passage pass = (Passage) p.next();
                            pass.highlight(ph, true);
                            String[] mTerms = pass.getMatchingTerms();
                            output.print("  " +
                                    scoreForm.format(pass.getScore()));
                            for(int l = 0; l < mTerms.length; l++) {
                                output.print(" " + mTerms[l]);
                            }
                            output.println("");
                            String hp = pass.getHLValue(true);
                            hp = reformat(hp.replace('\n', ' '), "   ", 72);
                            output.println(hp);
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
                    output.print("  " + scoreForm.format(pass.getScore()));
                    for(int l = 0; l < mTerms.length; l++) {
                        output.print(" " + mTerms[l]);
                    }
                    output.println("");
                    String hp = pass.getHLValue(true);
                    hp = reformat(hp.replace('\n', ' '), "   ", 72);
                    output.println(hp);
                    n++;
                }

                if(n < p.size()) {
                    output.println("  " + (p.size() - n) +
                            " passages not shown");
                }
            }
        }

        /**
         * Reformats a string to fit within a given number of columns with a
         * given prefix on each line.
         */
        public String reformat(String text, String prefix, int column) {
            StringBuffer output = new StringBuffer();
            output.append(prefix);
            int currCol = prefix.length();
            StringTokenizer tok = new StringTokenizer(text);
            while(tok.hasMoreTokens()) {
                String curr = tok.nextToken();
                if(currCol + curr.length() >= column) {
                    output.append("\n" + prefix);
                    currCol = prefix.length();
                }

                output.append(curr + " ");
                currCol += curr.length() + 1;
            }
            return output.toString();
        }
    }
}
