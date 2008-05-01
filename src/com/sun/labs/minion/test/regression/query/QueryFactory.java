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

package com.sun.labs.minion.test.regression.query;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.List;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.util.Getopt;

/**
 * <p>
 * Generates a set of 1-entry, 2-entries and 3-entries queries.
 * Currently the entries that it generates are:
 * <code>
 * <br>(e1)
 * <br>(not e1)
 * <br>(field contains e1)
 * <br>(not field contains e1)
 * <br>(e1 and e2)
 * <br>(e1 or e2)
 * <br>(e1 or (not e2))
 * <br>(e1 and (not e2))
 * <br>((not e1) and (not e2))
 * <br>((not e1) and e2)
 * <br>((not e1) or e2)
 * <br>((e1 and e2) or e3)
 * <br>((e1 or e2) and e3)
 * <br>(e1 and e2 and e3)
 * <br>(e1 or e2 or e3)
 * <br>(e1 and (not e2) and (not e3))
 * <br>(e1 or e2 and (not e3))
 * </code>
 * </p>
 *
 * <p>
 * This class will need as input:
 * <code>
 * <br>arg[0] - an index directory
 * <br>arg[1] - the maximum number of queries to generate
 * <br>arg[2] - the name of the text file to which the queries are written to
 * </code>
 * </p>
 *
 * <p> 
 * This class will first select entries from the index using the 
 * {@link com.sun.labs.minion.test.regression.query.TestEntrySelector TestEntrySelector} and the
 * {@link com.sun.labs.minion.test.regression.query.TestFieldEntrySelector TestFieldEntrySelector}.
 * The selected entries will then be used to create the queries.
 * </p>
 *
 * <p>
 * Entries are picked to generate queries according to their natural order
 * of occurrence. However, one can randomly pick entries to form queries.
 * This can be accomplished by implementing an Iterator that randomly
 * returns entries. This way, in the
 * {@link #createQueries(java.lang.String) createQueries} method, one can
 * substitute the Iterator from a list for the random Iterator. This
 * way, all the One/Two/ThreeEntryQueryFactory will randomly select entries
 * to form queries.
 * </p>
 */
public class QueryFactory {

    private List entries;
    private List fieldEntries;
    private int numQueries;
    private float oneQueryShare = 0.3f;
    private float twoQueryShare = 0.4f;
    private float threeQueryShare = 0.3f;
    private static boolean exact,  stem,  morph = false;
    /**
     * The prefix (if any) to be used in minion queries
     */
    private static String ENTRY_PREFIX = "";
    /**
     * Flag indicating whether to produce a lower case query for minion queries
     */
    private boolean lowerCase;

    /**
     * Constructs a QueryFactory with the given EntrySelector.
     *
     * @param entries the list of entries from which to create queries
     * @param fieldEntries the field entries from which to create queries
     * @param numQueries the total number of queries to produce
     */
    public QueryFactory(List entries, List fieldEntries, int numQueries, boolean lowerCase) {
        this.entries = entries;
        this.fieldEntries = fieldEntries;
        this.numQueries = numQueries;
        this.lowerCase = lowerCase;
    }

    /**
     * Creates the queries and writes them to the given output file.
     *
     * @param outputFile the name of the output file for the queries
     */
    public void createQueries(String outputFile) throws IOException {
        BufferedWriter queryWriter = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile), "UTF8"));
        setDisplaySpec(queryWriter);


        Iterator iterator = entries.iterator();
        Iterator entryIterator = fieldEntries.iterator();

        int numOneQueries = (int) (numQueries * oneQueryShare);
        OneEntryQueryFactory oneFactory = new OneEntryQueryFactory(this);
        oneFactory.createQueries(numOneQueries, iterator, entryIterator, queryWriter);

        int numTwoQueries = (int) (numQueries * twoQueryShare);
        TwoEntryQueryFactory twoFactory = new TwoEntryQueryFactory(this);
        twoFactory.createQueries(numTwoQueries, iterator, queryWriter);

        int numThreeQueries = (int) (numQueries * threeQueryShare);
        ThreeEntryQueryFactory threeFactory = new ThreeEntryQueryFactory(this);
        threeFactory.createQueries(numThreeQueries, iterator, queryWriter);

        queryWriter.close();
    }

    /**
     * Set the display spec of the query test
     * @param writer the output file onto which the spec should be written
     * @throws IOException
     */
    private void setDisplaySpec(BufferedWriter writer) throws IOException {
        writer.write(":display dockey\n");
    }

    public static void usage() {
        System.out.println("Usage: java com.sun.labs.minion.test.regression.query.QueryFactory" +
                "-d indexDirectory -x configFile [-n numQueries] [-f queriesFile]" +
                "[-e (exact)] [-s (stem)] [-m (morph)]");
    }

    /**
     * Runs the query factory. 
     */
    public static void main(String[] args) {


        if (args.length == 0) {
            usage();
            return;
        }

        String flags = "d:ef:lmn:sx:";
        Getopt gopt = new Getopt(args, flags);
        int c;

        String indexDirectory = null;
        String cmFile = null;
        String totalNumQueries = "100";
        String queryFile = "/tmp/queries.txt";
        boolean lowercase = false;


        while ((c = gopt.getopt()) != -1) {
            switch (c) {

                case 'd':
                    indexDirectory = gopt.optArg;
                    break;

                case 'x':
                    cmFile = gopt.optArg;
                    break;

                case 'n':
                    totalNumQueries = gopt.optArg;
                    break;

                case 'f':
                    queryFile = gopt.optArg;
                    break;

                case 'e':
                    exact = true;
                    ENTRY_PREFIX = "<exact>";
                    break;

                case 's':
                    stem = true;
                    ENTRY_PREFIX = "<stem>";
                    break;

                case 'm':
                    morph = true;
                    ENTRY_PREFIX = "<morph>";
                    break;

                case 'l':
                    lowercase = true;
                    break;
            }
        }

        if (indexDirectory == null) {
            usage();
            return;
        }
        if (cmFile == null) {
            usage();
            return;
        }

        if (!checkFlagCount()) {
            usage();
            return;
        }


        try {
            /* first select the interesting entries */
            TestEntrySelector selector = new TestEntrySelector(indexDirectory, cmFile);
            TestFieldEntrySelector fieldSelector = new TestFieldEntrySelector(cmFile, indexDirectory);

            List entries = selector.select();
            List fieldEntries = fieldSelector.select();
            // TestUtils.permutations(3, entries, "queries.txt");

            int numQueries = Integer.parseInt(totalNumQueries);

            /* generate queries from the entries */
            QueryFactory qf = new QueryFactory(entries, fieldEntries,
                    numQueries, lowercase);
            qf.createQueries(queryFile);

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    /**
     * Check to see if too many flags have been set. 
     * @return false if too many flags have been set, true otherwise.
     */
    private static boolean checkFlagCount() {
        int flagCount = 0;
        if (exact) {
            flagCount++;
        }
        if (morph) {
            flagCount++;
        }
        if (stem) {
            flagCount++;
        }
        return flagCount < 2;
    }

    /**
     * Gets the string to be used in a query to the minion search engine
     * @param entry the entry to be used as a query
     * @return a String that will be used in a minion query
     */
    String getMinionEntry(Entry entry) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(ENTRY_PREFIX);
        buffer.append(" ");
        String entryString = entry.toString();
        if (lowerCase) {
            entryString = entryString.toLowerCase();
        }
        buffer.append(entryString);
        return buffer.toString();
    }

    /**
     * Gets the string to be used in a query to the minion search engine
     * @param fieldEntry the field entry to be used as a query
     * @return a String that will be used in a minion query
     */
    String getMinionEntry(FieldEntry fieldEntry) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(ENTRY_PREFIX);
        buffer.append(' ');
        buffer.append('\"');
        String entryString = fieldEntry.entry.toString();
        if (lowerCase) {
            entryString = entryString.toLowerCase();
        }
        buffer.append(entryString);
        buffer.append('\"');
        return buffer.toString();
    }
}
