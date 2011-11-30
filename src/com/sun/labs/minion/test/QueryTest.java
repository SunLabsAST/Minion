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
import com.sun.labs.minion.indexer.entry.TermStatsQueryEntry;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
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
import com.sun.labs.minion.FieldFrequency;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.FieldValue;
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
import com.sun.labs.minion.WeightedField;
import com.sun.labs.minion.indexer.DiskField;
import java.io.File;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.net.URL;
import java.util.HashSet;
import com.sun.labs.minion.indexer.MetaFile;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.TermStatsDiskDictionary;
import com.sun.labs.minion.lexmorph.disambiguation.Unsupervised;
import com.sun.labs.minion.query.Relation;
import com.sun.labs.minion.retrieval.MultiDocumentVectorImpl;
import com.sun.labs.util.LabsLogFormatter;
import java.util.Arrays;
import java.util.Collections;
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

    protected static Logger logger = Logger.getLogger(QueryTest.class.getName());

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

    protected long totalTime, nQueries;

    protected Searcher.Grammar grammar = Searcher.Grammar.STRICT;

    protected Searcher.Operator queryOp = Searcher.Operator.AND;

    protected boolean ttyInput = true;

    protected Unsupervised.Model currModel;

    protected PrintStream output;

    private SimpleHighlighter shigh;

    public QueryTest(URL cmFile, String indexDir, String engineType,
                     String displayFields, String displayFormat,
                     String ss, PrintStream output) throws java.io.IOException,
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
        sortSpec = ss;
        scoreForm = new DecimalFormat("###0.000");
        displayPassage = false;
        this.output = output;
        shigh = new SimpleHighlighter("<font color=\"#FF0000\">", "</font>",
                                      "<b>", "</b>");
    }

    public void help() {
        output.println(":n <num>                Set number of hits to return\n"
                       + ":dp                     Toggle passage display\n"
                       + ":c                      Complex passage display\n"
                       + ":h                      Display this message\n"
                       + ":sort <sortspec>        Specify the sort order for hits\n"
                       + ":display <displayspec>  "
                       + "Specify the fields to be displayed for hits\n"
                       + ":sleep <milliseconds>   Sleep for some time\n"
                       + "\nClassification:\n"
                       + ":class <query>          Invoke a classifier and print the features\n"
                       + ":feat <num> <query>     "
                       + "Perform feature selection on the query results\n"
                       + ":cf <classname>         Display the features for a class\n"
                       + ":cfw <classname>         Display the features for a class, sorted by weight\n"
                       + ":train <class> <query>  Train a classifier with a name and a query\n"
                       + ":clusters <term>        Show all clusters in all partitions containing <term>\n"
                       + ":clusternamed <name>    Show the named cluster in each partition\n"
                       + ":pclusters              Prints all clusters, by partition\n"
                       + ":csim <class> <dockey>  Compute similarity between a classifier and a doc\n"
                       + "\nGeneral:\n"
                       + ":gram [web|strict]      Print or set the grammar to use\n"
                       + ":bq <query>             Batch query, prints results 100 at a time\n"
                       + ":term <term>            Look up a term entry in each partition\n"
                       + ":termi <term>           Case-insensitive version of ':term'\n"
                       + ":termstats <term>       Gets collection statistics for a term\n"
                       + ":dis <term>             Build a disambiguator for a term\n"
                       + ":merge [<part> ...]     Merge all or some index partitions\n"
                       + ":classmerge             Merge all classifier partitions\n"
                       + ":clustermerge           Merge all cluster partitions\n"
                       + ":ob                     Prints number of small docs and total docs\n"
                       + ":ts                     Prints term stats dictionary\n"
                       + ":cvl [<part> ...]       Calculate and dump vector lengths for a given partition\n"
                       + ":rts                    Re-generates term stats using all active partitions\n"
                       + "\nWild Card, etc:\n"
                       + ":sw                     Toggles case-sensitivity for wild cards\n"
                       + ":wild <pattern>         Prints matching terms from each partition\n"
                       + ":morph <term>           Prints matching variants from each partition\n"
                       + ":morphy <term>          Prints variants that will be generated for a term\n"
                       + ":variants <term>        Prints the LiteMorph variants of a term\n"
                       + ":stem <term>            Prints stem-matched terms from each partition\n"
                       + ":spell <term>           Prints the top 10 spelling variants of the term\n"
                       + "\nTaxonomy:\n"
                       + ":child <term>           Prints taxonomic children of a term\n"
                       + ":par <term>             Prints taxonomic parents of a term\n"
                       + ":sub <term>             Prints subsumed terms for <term>\n"
                       + "\nFields & Postings:\n"
                       + ":postsize <partnum> <outfile>\n"
                       + "                        Computes postings size statistics\n"
                       + ":fs <part> <field>      \n"
                       + ":sim <dockey> <dockey>  Compute similarity between docs\n"
                       + ":findsim <dockey> [<skim>]      Find documents similar to a doc\n"
                       + ":ffs <field> <dockey> [<skim>]   Find documents similar to a doc based on a particular field\n"
                       + ":cfs <dockey> <field> <weight> ... [<skim>]   Find documents similar to a doc based on a particular field combination\n"
                       + ":dv <dockey>            Show the document vector for a doc\n"
                       + ":fdv <field> <dockey>   Show the document vector for a field in a doc\n"
                       + ":ft <field> <op> <val>  Evaluate a field query\n"
                       + ":del <dockey>           Deletes the document\n"
                       + ":deld <partNum> <docid> [<docid...] Deletes the documents from the given partition\n"
                       + ":ddup <fromPart> <inPart> Deletes all dups from a partition that are in another partition\n"
                       + ":delq <query>           Delete the results of a query (asking first to confirm)\n"
                       + ":get <part> <docid> <fields ...>  Prints field values\n"
                       + ":gav <field>            Prints field values from all docs\n"
                       + ":gtv <field> [<n>]      Print the top n most frequent field values\n"
                       + ":gm <field> <pat>       Get matching values from the field\n"
                       + ":docpost <dockey>       Prints ID postings from the doc dict\n"
                       + ":pt <term>              Tests findID in postings iterator\n"
                       + ":post <type> <term ...> Prints postings.  type is: field <name>,doc,np,freq\n"
                       + ":main <part>            Print all entries in a partition\n"
                       + ":di [<part>]            Dumps all document entries for all/one partitions\n"
                       + ":ddi\n"
                       + ":ds                     Gets the number of entries with 1 posting\n"
                       + ":dt <dockey>            Prints the partition that contains the doc\n"
                       + ":top <field>            Gets the most frequent values for the given saved field\n"
                       + "\n" + ":q                      Quit\n"
                       + "Any text with no starting ':' issues a query");
    }

    public static void usage() {
        System.err.println(
                "Usage: java com.sun.labs.minion.test.QueryTest [options] -d indexDir");
    }

    public void stats() {
        if(ttyInput) {

            output.println("Status:");

            //
            // Collection stats.
            List<DiskPartition> activeParts = manager.getActivePartitions();
            output.format(" %d active partitions: ", activeParts.size());
            for(DiskPartition p : activeParts) {
                output.format("%d (%d) ", p.getPartitionNumber(), p.getNDocs());
            }
            output.println("");
            output.println(" Sorting specification is: " + sortSpec);
            output.println(" Display specification is: " + displaySpec);
            output.println(" Partitions have:\n" + "  " + manager.getNDocs()
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
            logger.log(Level.SEVERE, "Error getting search results", se);
            return;
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Error getting search results", e);
            return;
        }

        output.println("Query took: " + set.getQueryTime() + " ms");
        totalTime += set.getQueryTime();
        nQueries++;

        output.println(results.size() + "/" + set.size() + "/"
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
            output.println(eqs.dump());
        }
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
            } catch (ParseException pe) {
                logger.log(Level.WARNING, "", pe);
            } catch(SearchEngineException se) {
                logger.log(Level.SEVERE, "Error running search", se);
            }
        } else if(q.equals(":stats")) {
            stats();
        } else if(q.startsWith(":all")) {
            String[] vals =
                    parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            Set<String> fields = new HashSet<String>();
            fields.add(vals[0]);
            Set<String> terms = new HashSet<String>();
            for(int i = 1; i < vals.length; i++) {
                terms.add(vals[i]);
            }
            try {
                ResultSet rs = engine.allTerms(terms, fields);
                displayResults(rs);
            } catch(SearchEngineException see) {
                logger.log(Level.SEVERE, "Error finding all terms", see);
            }
        } else if(q.startsWith(":any")) {
            String[] vals =
                    parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            Set<String> fields = new HashSet<String>();
            fields.add(vals[0]);
            Set<String> terms = new HashSet<String>();
            for(int i = 1; i < vals.length; i++) {
                terms.add(vals[i]);
            }
            try {
                ResultSet rs = engine.anyTerms(terms, fields);
                displayResults(rs);
            } catch(SearchEngineException see) {
                logger.log(Level.SEVERE, "Error finding all terms", see);
            }
        } else if(q.startsWith(":qop")) {
            String op = q.substring(q.indexOf(' ') + 1).trim().toUpperCase();
            try {
                queryOp = Searcher.Operator.valueOf(op);
            } catch (IllegalArgumentException ex) {
                output.format("Didn't recognize operator, valid options are: %s\n",
                        Arrays.toString(Searcher.Operator.values()));
            }
        } else if(q.startsWith(":deff")) {
            QueryConfig qc = engine.getQueryConfig();
            String[] fields =
                    parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            for(String field : fields) {
                qc.addDefaultField(field);
            }
            engine.setQueryConfig(qc);
        } else if(q.startsWith(":qstats")) {
            queryStats();
        } else if(q.startsWith(":nd")) {
            output.println("ndocs: " + engine.getNDocs());
        } else if(q.startsWith(":ts ")) {
            String[] fields =
                    parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            for(String field : fields) {
                TermStatsDiskDictionary tsd = manager.getTermStatsDict();
                DictionaryIterator di = tsd.iterator(field);
                if(di == null) {
                    output.format("No term stats for %s", field);
                    continue;
                }
                output.format("%d term stats for %s:", di.getNEntries(), field);
                while(di.hasNext()) {
                    TermStatsQueryEntry tse = (TermStatsQueryEntry) di.next();
                    output.println(tse.getTermStats());
                }
            }
        } else if(q.startsWith(":diff ")) {
            try {
                String[] args =
                        parseMessage(q.substring(q.indexOf(' ') + 1).trim());
                DocumentVector dv =
                        engine.getDocumentVector(args[0], "content");
                ResultSet r1 = dv.findSimilar();
                ResultSet r2 = engine.anyTerms(Collections.singleton(args[1]),
                                               Collections.singleton("content"));
                ResultSet diff = r1.difference(r2);
                displayResults(diff);
            } catch(SearchEngineException ex) {
                logger.log(Level.SEVERE, "Error", ex);
            }
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
                logger.log(Level.SEVERE, "Error running search", se);
            }
        } else if(q.startsWith(":sleep ")) {
            try {
                long st = Long.parseLong(q.substring(q.indexOf(' ') + 1).trim());
                Thread.sleep(st);
            } catch(NumberFormatException nfe) {
                logger.warning(String.format("Invalid sleep time: %s", q.substring(q.indexOf(
                        ' ') + 1).
                        trim()));
            } catch(InterruptedException ie) {
            }

        } else if(q.startsWith(":n ")) {

            try {
                nHits = Integer.parseInt(q.substring(q.indexOf(' ') + 1).trim());
            } catch(NumberFormatException nfe) {
                logger.warning(String.format("Invalid number of hits: %s", q.substring(q.
                        indexOf(' ') + 1).
                        trim()));
            }
        } else if(q.startsWith(":sort")) {

            //
            // Get the sort spec.
            sortSpec = q.substring(q.indexOf(' ') + 1).trim();
        } else if(q.startsWith(":display ") || q.startsWith(
                ":displayFields ")) {
            displaySpec.setDisplayFields(q.substring(q.indexOf(' ') + 1).trim());
        } else if(q.startsWith(":displayFormat ") || (q.startsWith(":fo "))) {
            displaySpec.setFormatString(q.substring(q.indexOf(' ') + 1).trim());
        } else if(q.startsWith(":gram")) {
            if(q.indexOf(' ') < 0) {
                output.println("Currently using " +
                        grammar + " grammar");
            } else {
                String g = q.substring(q.indexOf(' ') + 1).trim().toUpperCase();
                try {
                    grammar = Searcher.Grammar.valueOf(g);
                } catch (IllegalArgumentException ex) {
                    output.format(
                            "Unrecognized grammar, valid values are: %s\n",
                            Arrays.toString(Searcher.Grammar.values()));
                }
            }
        } else if(q.startsWith(":q")) {

            try {
                if(engine != null) {
                    engine.close();
                }
            } catch(SearchEngineException se) {
                logger.log(Level.SEVERE, "Error closing search engine", se);
            }
            return -1;
        } else if(q.startsWith(":term ")) {

            //
            // Case sensitive term lookup.
            String term = CharUtils.decodeUnicode(q.substring(
                    q.indexOf(' ') + 1).trim());
            for(DiskPartition p : manager.getActivePartitions()) {
                for(DiskField df : ((InvFileDiskPartition) p).getDiskFields()) {
                    if(!df.getInfo().hasAttribute(FieldInfo.Attribute.INDEXED)) {
                        continue;
                    }
                    Entry e = df.getTerm(term, true);
                    if(e == null) {
                        output.format("%s field: %s null\n", p,
                                      df.getInfo().getName());
                    } else {
                        output.format("%s field: %s %s (%s) %d\n", 
                                      p,
                                      df.getInfo().getName(),
                                      e.getName(), 
                                      Util.toHexDigits(e.getName().toString()), 
                                      e.getN());
                    }
                }
            }
        } else if(q.startsWith(":termstats ")) {
            //
            // Case sensitive term lookup.
            String term = CharUtils.decodeUnicode(q.substring(
                    q.indexOf(' ') + 1).trim());
            TermStats ts = manager.getTermStatsDict().getTermStats(term);
            if(ts == null) {
                output.println("Term not found");
            } else {
                output.println("Term stats: " + ts);
            }
        } else if(q.startsWith(":morphy ")) {
            String term = CharUtils.decodeUnicode(q.substring(
                    q.indexOf(' ') + 1).trim());
            List<String> variants = new ArrayList<String>(engine.
                    getTermVariations(term));
            Collections.sort(variants);
            output.println("Variants for " + term);
            for(String v : variants) {
                output.println(" " + v);
            }

        } else if(q.startsWith(":termi ")) {

            //
            // Case insensitive term lookup.
            String term = CharUtils.decodeUnicode(q.substring(
                    q.indexOf(' ') + 1).trim());
            for(DiskPartition p : manager.getActivePartitions()) {
                for(DiskField df : ((InvFileDiskPartition) p).getDiskFields()) {
                    if(!df.getInfo().hasAttribute(FieldInfo.Attribute.INDEXED)) {
                        continue;
                    }
                    Entry e = df.getTerm(term, false);
                    if(e == null) {
                        output.format("%s field: %s null\n", p,
                                      df.getInfo().getName());
                    } else {
                        output.format("%s field: %s %s (%s) %d\n", p,
                                      df.getInfo().getName(),
                                      e.getName(), Util.toHexDigits(e.getName().
                                toString()), e.getN());
                    }
                }
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
                    logger.info("label: " + l);
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

                output.println("Disambiguate: " + currModel.getSenseLabels()[l]);
            } else {
                output.println("No current disambiguation model!");
            }

        } else if(q.startsWith(":sw")) {
            wildCaseSensitive = !wildCaseSensitive;
            output.println("wildCaseSensitive: " + wildCaseSensitive);
        } else if(q.startsWith(":wild ")) {

            try {
                String pat = q.substring(q.indexOf(' ') + 1).trim();
                for(DiskPartition p : manager.getActivePartitions()) {
                    for(DiskField df :
                            ((InvFileDiskPartition) p).getDiskFields()) {
                        output.format("Field: %s\n", df.getInfo().getName());
                        output.flush();
                        List<QueryEntry> entries = df.getMatching(pat,
                                                                  wildCaseSensitive,
                                                                  -1, -1);
                        if(entries.isEmpty()) {
                            output.println("No matches");
                        } else {
                            output.format("%d matches\n", entries.size());
                            for(QueryEntry e : entries) {
                                output.println("");
                                output.format(" %s (%d)\n", e.getName(),
                                              e.getN());
                            }
                        }
                    }
                }

            } catch(Exception e) {
                logger.log(Level.SEVERE, "Exception in :wild", e);
                return 0;
            }
        } else if(q.startsWith(":morph ")) {
            String t = q.substring(q.indexOf(' ') + 1).trim();
            Set<String> morphs = morphEn.variantsOf(t);
            List<String> variants = new ArrayList<String>();
            variants.add(t);
            variants.addAll(morphs);
            List<DiskPartition> parts = manager.getActivePartitions();
            for(DiskPartition p : parts) {
                output.println("Partition " + p.getPartitionNumber()
                               + " variants: ");
                for(DiskField df :
                        ((InvFileDiskPartition) p).getDiskFields()) {
                    int docFreq = 0;
                    int totalVariants = 0;
                    output.format("Field: %s\n", df.getInfo().getName());
                    for(String variant : variants) {
                        Entry e = df.getTerm(variant, true);
                        if(e != null) {
                            output.format(" %s (%d)\n", e.getName(), e.getN());
                            docFreq += e.getN();
                            totalVariants++;
                        }
                    }
                    output.println("Total document frequency: " + docFreq);
                    output.println("Total variants:           " + totalVariants);
                }
            }
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
                for(DiskField df :
                        ((InvFileDiskPartition) p).getDiskFields()) {

                    QueryEntry stem = df.getStem(t);
                    output.print("Partition " + p.getPartitionNumber() + ": ");
                    if(stem == null) {
                        output.println("No matches");
                    } else {
                        output.format(" %s (%d)\n", stem.getName(), stem.getN());
                    }
                }
            }

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
                for(Iterator l = manager.getActivePartitions().iterator(); l.
                        hasNext();) {
                    DiskPartition p = (DiskPartition) l.next();
                    if(p.getPartitionNumber() == pn) {

                        DictionaryIterator i =
                                ((InvFileDiskPartition) p).getFieldIterator(
                                field);
                        if(i != null) {
                            output.println(p.toString());
                            while(i.hasNext()) {
                                Entry e = (Entry) i.next();
                                output.println("value: " + e.getName() + " " + e.
                                        getN());
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

            for(Iterator l = manager.getActivePartitions().iterator();
                    l.hasNext();) {
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

            Relation.Operator op = Relation.Operator.valueOf(operator.
                    toUpperCase());
            if(op == null) {
                output.println("Unknown operator: " + op);
                return 1;
            }

            try {
                Relation r = new Relation(field, op, value);
                ResultSet set = engine.search(r);
                displayResults(set);
            } catch(SearchEngineException ex) {
                logger.log(Level.SEVERE, "Error searching field term", ex);
            }
        } else if(q.startsWith(":top ")) {
            String f = q.substring(q.indexOf(' ') + 1).trim();
            for(FieldFrequency ff : engine.getTopFieldValues(f, nHits, true)) {
                output.println(ff.getFreq() + " " + ff.getVal());
            }
        } else if(q.startsWith(":del ")) {
            String k = q.substring(q.indexOf(' ') + 1).trim();
            manager.deleteDocument(k);
        } else if(q.startsWith(":delq ")) {
            q = q.substring(q.indexOf(' ') + 1).trim();
            try {
                //
                // Run the query
                ResultSet rs = searcher.search(q, sortSpec,
                        Searcher.Operator.AND, grammar);
                displayResults(rs);
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(System.in));
                output.print(
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

        } else if(q.startsWith(":deld ")) {
            String[] fields =
                    parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            if(fields.length >= 2) {
                int part = Integer.parseInt(fields[0]);
                for(DiskPartition p : manager.getActivePartitions()) {
                    if(p.getPartitionNumber() == part) {
                        for(int i = 1; i < fields.length; i++) {
                            boolean success = p.deleteDocument(Integer.parseInt(
                                    fields[i]));
                            output.printf("Deletion of %d in %d returned %s ",
                                          Integer.parseInt(fields[i]),
                                          p.getPartitionNumber(), success);
                        }
                    }
                }
            }
        } else if(q.startsWith(":ddup ")) {
            String[] args = parseMessage(q.substring(q.indexOf(' ') + 1).trim());
            if(args.length == 2) {
                int keepPartNum = Integer.parseInt(args[1]);
                DiskPartition keepPart = null;
                int delPartNum = Integer.parseInt(args[0]);
                DiskPartition delPart = null;
                for(DiskPartition p : manager.getActivePartitions()) {
                    if(p.getPartitionNumber() == keepPartNum) {
                        keepPart = p;
                    } else if(p.getPartitionNumber() == delPartNum) {
                        delPart = p;
                    }
                }
                Iterator di = keepPart.getDocumentIterator();
                while(di.hasNext()) {
                    QueryEntry key = (QueryEntry) di.next();
                    boolean success = delPart.deleteDocument(key.toString());
                    if(success) {
                        System.out.println("Removed key " + key
                                           + " from partition " + delPartNum);
                    }
                }
            } else {
                System.out.println(
                        ":ddup <remove dup from part> <found in part>");
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

                    for(DiskPartition p : manager.getActivePartitions()) {
                        if(p.getPartitionNumber() == pn) {

                            for(i = 0; i < f.length; i++) {
                                DiskField df = ((InvFileDiskPartition) p).getDF(
                                        f[i]);
                                Object v = df.getFetcher().fetch(d);
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
                                                             (String) key.
                            getName());

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

                logger.info("parts: " + parts);

                PartitionManager.Merger merger =
                        manager.getMergerFromNumbers(parts);
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

            for(DiskPartition p : manager.getActivePartitions()) {
                if((partNum == -1) || (partNum == p.getPartitionNumber())) {
                    for(DiskField df :
                            ((InvFileDiskPartition) p).getDiskFields()) {
                        DiskDictionary<String> dd = df.getTermDictionary(true);
                        if(dd == null) {
                            continue;
                        }
                        for(Entry qe : dd) {
                            output.println(qe);
                        }
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
                    output.println("Similarity score: " + dv1.getSimilarity(dv2));
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
                    Map<String, Float> words = dv1.getSimilarityTermMap(dv2);
                    for(Iterator it = words.keySet().iterator(); it.hasNext();) {
                        String curr = (String) it.next();
                        output.println(curr + ": \t" + words.get(curr));
                    }
                }
            } catch(StringIndexOutOfBoundsException e) {
                output.println("Syntax error, try \":help\"");
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

        } else if(q.startsWith(":mffs")) {
            String[] vals = parseMessage(q.substring(q.indexOf(' ')).trim());
            String field = vals[0];
            List<DocumentVector> vecs = new ArrayList();
            for(int i = 1; i < vals.length; i++) {
                DocumentVector dv = engine.getDocumentVector(vals[i], field);
                if(dv != null) {
                    vecs.add(dv);
                }
            }
            MultiDocumentVectorImpl dv =
                    new MultiDocumentVectorImpl(vecs, field);
            dv.setEngine(engine);
            output.println("dv: " + dv);
            ResultSet rs = dv.findSimilar("-score");
            displayResults(rs);

        } else if(q.startsWith(":ffs ")) {
            try {
                String[] vals = parseMessage(q.substring(q.indexOf(' ')).trim());
                String key = vals.length > 1 ? vals[1] : vals[0];
                String field = vals.length > 1 ? vals[0] : null;
                double skim =
                        vals.length > 2 ? Double.parseDouble(vals[2]) : 1.0;
                DocumentVector dv;
                if(field != null) {
                    dv = engine.getDocumentVector(key, field);
                } else {
                    dv = engine.getDocumentVector(key);
                }
                output.println("dv: " + dv);
                if(dv != null) {
                    ResultSet rs = ((DocumentVectorImpl) dv).findSimilar(
                            "-score",
                            skim);
                    displayResults(rs);
                } else {
                    output.println("No such doc: " + key);
                }
            } catch(Exception ex) {
                output.println("Exception finding similar");
                ex.printStackTrace(output);
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
                                                             lwf.toArray(
                        new WeightedField[0]));
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
                    output.println("Sets overlapped by " + newLen + "/"
                                   + fullLen);
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
                    output.println("Sets overlapped by " + newLen + "/"
                                   + fullLen);
                }
                //
                // Print out total overlap
                float percent = (totalOverlap / (float) totalResults) * 100;
                output.println("Cumulative overlap: " + totalOverlap + "/"
                               + totalResults + " (" + percent + "&)");
                output.println("Total time for full findSims: " + fullTime
                               + "ms");
                output.println("Total time for partial findSims: " + partTime
                               + "ms");
                output.println("Speedup: " + fullTime / partTime);
            } catch(SearchEngineException e) {
                output.println("Ooops: " + e);
            }
        } else if(q.startsWith(":dv")) {
            String[] vals = parseMessage(q);
            String field = vals[1];
            String dockey = vals[2];
            DocumentVector dv = engine.getDocumentVector(dockey, field);
            if(dv == null) {
                output.println("Unknown key: " + vals[1]);
            } else {
                dumpDocVectorByWeight(dv);
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
        QueryTest qt = new QueryTest(
                cmFile,
                indexDir,
                engineType,
                "partNum,docID,dockey",
                "%5d%7d %s",
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

        output.println("Average time over " + qt.nQueries + " queries: "
                       + (float) qt.totalTime / (float) qt.nQueries + " ms");

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

        /**
         * The values we'll print.
         */
        protected Object[] vals;

        protected TextHighlighter th = new TextHighlighter();

        protected String formatString;

        /**
         * Makes a display spec from a string representation. The representation
         * is a comma separated list of field names. In addition to the names
         * defined in a given index, the user may use the sequences
         * <code>\n</code> and <code>\t</code> to display newlines or tabs
         * in the output.
         */
        public DisplaySpec(String displayFields, String formatString) {
            setDisplayFields(displayFields);
            setFormatString(formatString);
        }

        public void setDisplayFields(String displayFields) {
            //
            // Split the input at commas.
            StringTokenizer tok = new StringTokenizer(displayFields, ", ");
            fields = new String[tok.countTokens()];
            vals = new Object[fields.length];
            int i = 0;
            StringBuilder df = new StringBuilder();
            while(tok.hasMoreTokens()) {

                String fn = tok.nextToken();
                fields[i] = fn;
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
                i++;
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

            output.print(prefix);

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
            output.format(formatString, vals);
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
                        for(Iterator p = passages.iterator(); p.hasNext() && k
                                                                             < 4;
                                k++) {
                            Passage pass = (Passage) p.next();
                            pass.highlight(ph, true);
                            String[] mTerms = pass.getMatchingTerms();
                            output.print("  "
                                         + scoreForm.format(pass.getScore()));
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
                    output.println("  " + (p.size() - n) + " passages not shown");
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

        public String toString() {
            return "fields: " + Util.arrayToString(fields) + " format: \""
                   + formatString + "\"";
        }
    }
}
