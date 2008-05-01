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

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;

import com.sun.labs.minion.indexer.entry.Entry;


interface ThreeEntryQueryStringFactory {
    public String getQueryString(Entry e1, Entry e2, Entry e3);
}


/**
 * Generates queries that contains three entries. The types of queries
 * it creates are:
 * <ol>
 * <li>((e1 and e2) or e3)</li>
 * <li>((e1 or e2) and e3)</li>
 * <li>(e1 and e2 and e3)</li>
 * <li>(e1 or e2 or e3)</li>
 * <li>(e1 and (not e2) and (not e3))</li>
 * <li>(e1 or e2 and (not e3))</li>
 * </ol>
 */
public class ThreeEntryQueryFactory {
	QueryFactory queryFactory;

    public ThreeEntryQueryFactory(QueryFactory factory) {
		queryFactory = factory;
	}
    
    ThreeEntryQueryFactory() {
    	
    }


	/**
     * Create all the three entry queries and write them to the given writer.
     *
     * @param n the maximum number of queries to create
     * @param entryIterator iterator for entries
     * @param writer the writer to write the queries to
     */
    public void createQueries(int n, Iterator entryIterator, Writer writer)
        throws IOException {

        /* ((e1 and e2) or e3) */
        createE1AndE2OrE3Queries((int)(n * 0.15), entryIterator, writer);

        /* ((e1 or e2) and e3) */
        createE1OrE2AndE3Queries((int)(n * 0.15), entryIterator, writer);
        
        /* (e1 and e2 and e3) */
        createE1AndE2AndE3Queries((int)(n * 0.2), entryIterator, writer);
        
        /* (e1 or e2 or e3) */
        createE1OrE2OrE3Queries((int)(n * 0.2), entryIterator, writer);
        
        /* (e1 and (not e2) and (not e3)) */
        createE1AndNotE2AndNotE3Queries((int)(n*0.15), entryIterator, writer);
        
        /* (e1 or e2 and (not e3)) */
        createE1OrE2AndNotE3Queries((int)(n * 0.15), entryIterator, writer);
    }
    

    /**
     * Creates queries of the type:
     * <code>
     * ((e1 &lt;and&gt; e2) &lt;or&gt; e3)
     * </code>
     *
     * @param n the maximum number of queries to create
     * @param entryIterator iterator for entries
     * @param writer the writer to write the queries to
     */
    protected void createE1AndE2OrE3Queries(int n, Iterator entryIterator,
                                            Writer writer) throws IOException {
        /* ((e1 and e2) or e3) */
        createQueries
            (n, entryIterator, writer,
             (new ThreeEntryQueryStringFactory() {
                     public String getQueryString(Entry e1, Entry e2,
                                                  Entry e3) {
                         return ("((" + queryFactory.getMinionEntry(e1) + " <and> " +
                                 queryFactory.getMinionEntry(e2) + ") <or> " + queryFactory.getMinionEntry(e3) + 
                                 ")");
                     }
                 }));
    }


    /**
     * Creates queries of the type:
     * <code>
     * ((e1 &lt;or&gt; e2) &lt;and&gt; e3)
     * </code>
     *
     * @param n the maximum number of queries to create
     * @param entryIterator iterator for entries
     * @param writer the writer to write the queries to
     */
    protected void createE1OrE2AndE3Queries(int n, Iterator entryIterator,
                                            Writer writer) throws IOException {
        /* ((e1 or e2) and e3) */
        createQueries
            (n, entryIterator, writer,
             (new ThreeEntryQueryStringFactory() {
                     public String getQueryString(Entry e1, Entry e2,
                                                  Entry e3) {
                         return ("((" + queryFactory.getMinionEntry(e1) + " <or> " +
                                 queryFactory.getMinionEntry(e2) + ") <and> " + queryFactory.getMinionEntry(e3) +
                                 ")");
                     }
                 }));
    }


    /**
     * Creates queries of the type:
     * <code>
     * (e1 &lt;and&gt; e2 &lt;and&gt; e3)
     * </code>
     *
     * @param n the maximum number of queries to create
     * @param entryIterator iterator for entries
     * @param writer the writer to write the queries to
     */
    protected void createE1AndE2AndE3Queries
        (int n, Iterator entryIterator, Writer writer) throws IOException {
        /* (e1 and e2 and e3) */
        createQueries
            (n, entryIterator, writer,
             (new ThreeEntryQueryStringFactory() {
                     public String getQueryString(Entry e1, Entry e2,
                                                  Entry e3) {
                         return ("(" + queryFactory.getMinionEntry(e1) + " <and> " +
                                 queryFactory.getMinionEntry(e2) + " <and> " + queryFactory.getMinionEntry(e3) +
                                 ")");
                     }
                 }));
    }


    /**
     * Creates queries of the type:
     * <code>
     * (e1 &lt;or&gt; e2 &lt;or&gt; e3)
     * </code>
     *
     * @param n the maximum number of queries to create
     * @param entryIterator iterator for entries
     * @param writer the writer to write the queries to
     */
    protected void createE1OrE2OrE3Queries
        (int n, Iterator entryIterator, Writer writer) throws IOException {
        /* (e1 or e2 or e3) */
        createQueries
            (n, entryIterator, writer,
             (new ThreeEntryQueryStringFactory() {
                     public String getQueryString(Entry e1, Entry e2,
                                                  Entry e3) {
                         return ("(" + queryFactory.getMinionEntry(e1) + " <or> " + 
                                 queryFactory.getMinionEntry(e2) + " <or> " + queryFactory.getMinionEntry(e3) + ")");
                     }
                 }));
    }


    /**
     * Creates queries of the type:
     * <code>
     * (e1 &lt;and&gt; (&lt;not&gt; e2) &lt;and&gt; (&lt;not&gt; e3))
     * </code>
     *
     * @param n the maximum number of queries to create
     * @param entryIterator iterator for entries
     * @param writer the writer to write the queries to
     */
    protected void createE1AndNotE2AndNotE3Queries
        (int n, Iterator entryIterator, Writer writer) throws IOException {
        /* (e1 and (not e2) and (not e3)) */
        createQueries
            (n, entryIterator, writer,
             (new ThreeEntryQueryStringFactory() {
                     public String getQueryString(Entry e1, Entry e2,
                                                  Entry e3) {
                         return ("(" + queryFactory.getMinionEntry(e1) + " <and> (<not> " +
                                 queryFactory.getMinionEntry(e2) + ") <and> (<not> " +
                                 queryFactory.getMinionEntry(e3) + "))");
                     }
                 }));
    }
     

    /**
     * Creates queries of the type:
     * <code>
     * (e1 &lt;or&gt; e2 &lt;and&gt; (&lt;not&gt; e3))
     * </code>
     *
     * @param n the maximum number of queries to create
     * @param entryIterator iterator for entries
     * @param writer the writer to write the queries to
     */
    protected void createE1OrE2AndNotE3Queries
        (int n, Iterator entryIterator, Writer writer) throws IOException {
        /* (e1 or e2 and (not e3)) */
        createQueries
            (n, entryIterator, writer,
             (new ThreeEntryQueryStringFactory() {
                     public String getQueryString(Entry e1, Entry e2,
                                                  Entry e3) {
                         return ("(" + queryFactory.getMinionEntry(e1) + " <or> " + 
                                 queryFactory.getMinionEntry(e2) +
                                 " <and> (<not> " + queryFactory.getMinionEntry(e3) + "))");
                     }
                 }));
    }
    
    
    /**
     * Create all the three entry queries and write them to the given writer.
     *
     * @param n the total number of queries to create
     * @param entryIterator iterator for entries
     * @param writer the writer to write the queries to
     */
    protected void createQueries(int n, Iterator entryIterator, Writer writer,
                              ThreeEntryQueryStringFactory qsf)
        throws IOException {
        for (int i = 0; i < n; i++) {
            if (entryIterator.hasNext()) {
                Entry e1 = (Entry) entryIterator.next();
                if (entryIterator.hasNext()) {
                    Entry e2 = (Entry) entryIterator.next();
                    if (entryIterator.hasNext()) {
                        Entry e3 = (Entry) entryIterator.next();
                        String query = qsf.getQueryString(e1, e2, e3);
                        writer.write(query + "\n");
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }
}
