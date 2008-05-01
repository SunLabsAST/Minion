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


interface TwoEntryQueryStringFactory {
    public String getQueryString(Entry e1, Entry e2);
}


/**
 * Generates queries that contains two entries in it. The types of queries
 * it creates are:
 * <ol>
 * <li>(e1 and e2)</li>
 * <li>(e1 or e2)</li>
 * <li>(e1 or (not e2))</li>
 * <li>(e1 and (not e2))</li>
 * <li>((not e1) and (not e2))</li>
 * <li>((not e1) and e2)</li>
 * <li>((not e1) or e2)</li>
 * </ol>
 */
public class TwoEntryQueryFactory {

    private float andQueryShare = 0.25f;
    private float orQueryShare = 0.25f;
    private float e1OrNotE2QueryShare = 0.1f;
    private float e1AndNotE2QueryShare = 0.1f;
    private float notE1AndNotE2QueryShare = 0.1f;
    private float notE1AndE2QueryShare = 0.1f;
    private float notE1OrE2QueryShare = 0.1f;
    QueryFactory queryFactory;


    public TwoEntryQueryFactory(QueryFactory factory) {
		queryFactory = factory;
	}
    
    TwoEntryQueryFactory() {
    	
    }


	/**
     * Create all the two entry queries and write them to the given writer.
     *
     * @param n the total number of queries to create
     * @param entryIterator iterator for entries
     * @param writer the writer to write the queries to
     */
    public void createQueries(int n, Iterator entryIterator, Writer writer)
        throws IOException {

        /* (e1 and e2) */
        createAndQueries
            ((int) (n * andQueryShare), entryIterator, writer);
        
        /* (e1 or e2) */
        createOrQueries
            ((int) (n * orQueryShare), entryIterator, writer);
        
        /* (e1 or (not e2)) */
        createE1OrNotE2Queries
            ((int) (n * e1OrNotE2QueryShare), entryIterator, writer);
        
        /* (e1 and (not e2)) */
        createE1AndNotE2Queries
            ((int) (n * e1AndNotE2QueryShare), entryIterator, writer);
        
        /* ((not e1) and (not e2)) */
        createNotE1AndNotE2Queries
            ((int) (n * notE1AndNotE2QueryShare), entryIterator, writer);
        
        /* ((not e1) and e2) */
        createNotE1AndE2Queries
            ((int) (n * notE1AndE2QueryShare), entryIterator, writer);
        
        /* ((not e1) or e2) */
        createNotE1OrE2Queries
            ((int) (n * notE1OrE2QueryShare), entryIterator, writer);
    }


    /**
     * Create <code> (e1 &lt;and&gt; e2) </code> queries.
     *
     * @param n the number of queries to create
     * @param iterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the created queries to
     * @param qsf the object that returns the appropriate query string
     */
    protected void createQueries(int n, Iterator iterator, Writer writer,
                               TwoEntryQueryStringFactory qsf)
        throws IOException {
        for (int i = 0; i < n; i++) {
            if (iterator.hasNext()) {
                Entry e1 = (Entry) iterator.next();
                if (iterator.hasNext()) {
                    Entry e2 = (Entry) iterator.next();
                    String query = qsf.getQueryString(e1, e2);
                    writer.write(query + "\n");
                } else {
                    break;
                }
            } else {
                break;
            }
        }
    }


    /**
     * Create <code> (e1 &lt;and&gt; e2) </code> queries.
     *
     * @param n the number of queries to create
     * @param entryIterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the queries to
     */
    protected void createAndQueries(int n, Iterator entryIterator,
                                 Writer writer) throws IOException {
        createQueries(n, entryIterator, writer,
                      new TwoEntryQueryStringFactory() {
                          public String getQueryString(Entry e1, Entry e2) {
                              return ("(" + queryFactory.getMinionEntry(e1) + " <and> " +
                                      queryFactory.getMinionEntry(e2) + ")");
                          }
                      });
    }

    /**
     * Create <code> (e1 &lt;or&gt; e2) </code> queries.
     *
     * @param n the number of queries to create
     * @param entryIterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the queries to
     */
    protected void createOrQueries(int n, Iterator entryIterator,
                                   Writer writer) throws IOException {
        createQueries(n, entryIterator, writer,
                      new TwoEntryQueryStringFactory() {
                          public String getQueryString(Entry e1, Entry e2) {
                              return ("(" + queryFactory.getMinionEntry(e1) + " <or> " +
                                      queryFactory.getMinionEntry(e2) + ")");
                          }
                      });
    }


    /**
     * Create <code> (e1 &lt;or&gt; &lt;not&gt; e2) </code> queries.
     *
     * @param n the number of queries to create
     * @param entryIterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the queries to
     */
    protected void createE1OrNotE2Queries(int n, Iterator entryIterator,
                                          Writer writer) throws IOException {
        createQueries(n, entryIterator, writer,
                      new TwoEntryQueryStringFactory() {
                          public String getQueryString(Entry e1, Entry e2) {
                              return ("(" + queryFactory.getMinionEntry(e1) + " <or> <not> " +
                                      queryFactory.getMinionEntry(e2) + ")");
                          }
                      });
    }


    /**
     * Create <code> (e1 &lt;and&gt; &lt;not&gt; e2) </code> queries.
     *
     * @param n the number of queries to create
     * @param entryIterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the queries to
     */
    protected void createE1AndNotE2Queries(int n, Iterator entryIterator,
                                        Writer writer) throws IOException {
        createQueries(n, entryIterator, writer,
                      new TwoEntryQueryStringFactory() {
                          public String getQueryString(Entry e1, Entry e2) {
                              return ("(" + queryFactory.getMinionEntry(e1) + " <and> <not> " +
                                      queryFactory.getMinionEntry(e2) + ")");
                          }
                      });
    }


    /**
     * Create <code> (&lt;not&gt; e1 &lt;and&gt; &lt;not&gt; e2) </code>
     * queries.
     *
     * @param n the number of queries to create
     * @param entryIterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the queries to
     */
    protected void createNotE1AndNotE2Queries(int n, Iterator entryIterator,
                                           Writer writer) throws IOException {
        createQueries
            (n, entryIterator, writer,
             new TwoEntryQueryStringFactory() {
                 public String getQueryString(Entry e1, Entry e2) {
                     return ("(<not> " + queryFactory.getMinionEntry(e1) + " <and> <not> " +
                             queryFactory.getMinionEntry(e2) + ")");
                 }
             });
    }

    
    /**
     * Create <code> (&lt;not&gt; (e1 &lt;and&gt; e2)) </code> queries.
     *
     * @param n the number of queries to create
     * @param entryIterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the queries to
     */
    protected void createNotE1AndE2Queries(int n, Iterator entryIterator,
                                        Writer writer) throws IOException {
        createQueries(n, entryIterator, writer,
                      new TwoEntryQueryStringFactory() {
                          public String getQueryString(Entry e1, Entry e2) {
                              return ("(<not> (" + queryFactory.getMinionEntry(e1) + " <and> " +
                                      queryFactory.getMinionEntry(e2) + "))");
                          }
                      });
    }

    
    /**
     * Create <code> (&lt;not&gt; (e1 &lt;or&gt; e2)) </code> queries.
     *
     * @param n the number of queries to create
     * @param entryIterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the queries to
     */
    protected void createNotE1OrE2Queries(int n, Iterator entryIterator,
                                          Writer writer) throws IOException {
        createQueries(n, entryIterator, writer,
                      new TwoEntryQueryStringFactory() {
                          public String getQueryString(Entry e1, Entry e2) {
                              return ("(<not> (" + queryFactory.getMinionEntry(e1) + " <or> " +
                                      queryFactory.getMinionEntry(e2) + "))");
                          }
                      });
    }
}
