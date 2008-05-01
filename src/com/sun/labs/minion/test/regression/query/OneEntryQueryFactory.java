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


/**
 * Generates queries that contains one entry in it. The types of queries
 * it creates are:
 * <ol>
 * <li>(e1)</li>
 * <li>(not e1)</li>
 * <li>(field contains e1)</li>
 * <li>(not field contains e1)</li>
 * </ol>
 */
public class OneEntryQueryFactory {

    private float entryQueryShare = 0.25f;
    private float notQueryShare = 0.25f;
    private float fieldContainsQueryShare = 0.25f;
    private float fieldNotContainsQueryShare = 0.25f;
    private QueryFactory queryFactory;


    public OneEntryQueryFactory(QueryFactory factory) {
		queryFactory = factory;
	}
    
    OneEntryQueryFactory() {
    	
    }

	/**
     * Create all the one entry queries and write them to the given writer.
     *
     * @param n the total number of queries to create
     * @param entryIterator iterator for entries
     * @param fieldEntryIterator iterator for field related entries
     * @param writer the writer to write the queries to
     */
    public void createQueries(int n, Iterator entryIterator,
                              Iterator fieldEntryIterator, Writer writer) 
        throws IOException {

        /* (e1) */
        createEntryQueries
            ((int) (n * entryQueryShare), entryIterator, writer);
        
        /* (not e1) */
        createNotQueries
            ((int) (n * notQueryShare), entryIterator, writer);

        /* (field contains e1) */
        createFieldContainsQueries
            ((int) (n * fieldContainsQueryShare), fieldEntryIterator, writer);

        /* (not field contains e1) */
        createFieldNotContainsQueries
            ((int) (n * fieldNotContainsQueryShare), fieldEntryIterator,
             writer);
    }

    /**
     * Creates queries that have only one entry in it.
     *
     * @param n the number of queries to generate
     * @param iterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the queries to
     *
     */
    protected void createEntryQueries(int n, Iterator iterator, 
                                      Writer writer) throws IOException {
        for (int i = 0; i < n; i++) {
            if (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                String query = "(" + queryFactory.getMinionEntry(entry) + ")";
                writer.write(query + "\n");
            } else {
                //System.out.println("Generated " + i + " entry queries.");
                break;
            }
        }
    }

    /**
     * Creates <code>(&lt;not&gt; e)</code> queries.
     *
     * @param n the number of queries to generate
     * @param iterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the queries to
     *
     */
    protected void createNotQueries(int n, Iterator iterator, Writer writer)
        throws IOException {

        for (int i = 0; i < n; i++) {
            if (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                String query = "(<not> " + queryFactory.getMinionEntry(entry) + ")";
                writer.write(query + "\n");
            } else {
                //System.out.println("Generated " + i + " <not> entry queries.");
                break;
            }
        }
    }

    /**
     * Creates <code>(field &lt;contains&gt; "e")</code> queries.
     *
     * @param n the number of queries to generate
     * @param iterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the queries to
     */
    protected void createFieldContainsQueries(int n, Iterator iterator,
                                              Writer writer)
        throws IOException {
        for (int i = 0; i < n; i++) {
            if (iterator.hasNext()) {
                FieldEntry fieldEntry = (FieldEntry) iterator.next();
                String query = "(" + fieldEntry.field + " <contains> " + 
                queryFactory.getMinionEntry(fieldEntry) + ")";
                writer.write(query + "\n");
            } else {
                break;
            }
        }
    }

    /**
     * Creates <code> (&lt;not&gt; field &lt;contains&gt; e) queries.
     *
     * @param n the number of queries to generate
     * @param iterator the entry iterator from which to obtain the entries
     * @param writer the writer to write the queries to
     */
    protected void createFieldNotContainsQueries(int n, Iterator iterator,
                                                 Writer writer)
        throws IOException {
        for (int i = 0; i < n; i++) {
            if (iterator.hasNext()) {
                FieldEntry fieldEntry = (FieldEntry) iterator.next();
                String query = "(<not> " + fieldEntry.field +
                    " <contains> " + queryFactory.getMinionEntry(fieldEntry) + ")";
                writer.write(query + "\n");
            } else {
                break;
            }
        }
    }
}
