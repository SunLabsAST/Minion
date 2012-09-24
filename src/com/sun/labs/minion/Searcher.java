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

package com.sun.labs.minion;

import com.sun.labs.minion.query.Element;

/**
 * An interface for things that know how to run a search.
 */
public interface Searcher {

    public enum Operator {
        PAND,
        AND,
        OR,
        PASSAGE
    }

    public enum Grammar {
        WEB,
        STRICT,
        LUCENE
    }

    /**
     * Runs a query against the engines, returning a set of results.
     *
     * @param query The query to run, in our query syntax.
     * @return a set of results containing the documents that match the query
     */
    public ResultSet search(String query) throws SearchEngineException;

    /**
     * Runs a query against the engines, returning a set of results.
     *
     * @param query The query to run, in our query syntax.
     * @param queryConfig A query configuration to use for this query.  If this
     * is <code>null</code> then the default configuration will be used.
     * @return a set of results containing the documents that match the query
     */
    public ResultSet search(String query, QueryConfig queryConfig) throws SearchEngineException;

    /**
     * Runs a query against the engines, returning a set of results.
     *
     * @param query The query to run, in our query syntax.
     * @param sortSpec How the results should be sorted.  This is a set of
     * comma-separated saved field names, each preceeded by a <code>+</code> (for
     * increasing order) or by a <code>-</code> (for decreasing order).
     * @return a set of results containing the documents that match the query
     */
    public ResultSet search(String query, String sortSpec) throws SearchEngineException;

    /**
     * Runs a query against the engines, returning a set of results.
     *
     * @param query The query to run, in our query syntax.
     * @param sortSpec How the results should be sorted. This is a set of
     * comma-separated saved field names, each preceeded by a <code>+</code>
     * (for increasing order) or by a <code>-</code> (for decreasing order).
     * @param queryConfig A query configuration to use for this query. If this
     * is <code>null</code> then the default configuration will be used.
     * @return a set of results containing the documents that match the query
     */
    public ResultSet search(String query, String sortSpec, QueryConfig queryConfig) throws
            SearchEngineException;

    /**
     * Runs a query against the engines, returning a set of results.
     *
     * @param query The query to run, in our query syntax.
     * @param sortSpec How the results should be sorted.  This is a set of
     * comma-separated field names, each preceeded by a <code>+</code> (for
     * increasing order) or by a <code>-</code> (for decreasing order).
     * @param defaultOperator the operator to use between terms when
     * none is specified in the query
     * @param grammar specifies the grammar to use to parse the query.  Valid values
     * are defined in the {@link com.sun.labs.minion.Searcher} interface
     */
    public ResultSet search(String query, String sortSpec,
                             Operator defaultOperator, Grammar grammar)
            throws SearchEngineException;
    
    /**
     * Runs a query against the engines, returning a set of results.
     *
     * @param query The query to run, in our query syntax.
     * @param sortSpec How the results should be sorted.  This is a set of
     * comma-separated field names, each preceeded by a <code>+</code> (for
     * increasing order) or by a <code>-</code> (for decreasing order).
     * @param defaultOperator the operator to use between terms when
     * none is specified in the query
     * @param grammar specifies the grammar to use to parse the query.  Valid values
     * are defined in the {@link com.sun.labs.minion.Searcher} interface
     * @param queryConfig A query configuration to use for this query. If this
     * is <code>null</code> then the default configuration will be used.
     */
    public ResultSet search(String query, String sortSpec,
                            Operator defaultOperator, Grammar grammar,
                            QueryConfig queryConfig)
            throws SearchEngineException;
    
    /**
     * Runs a query against the index, returning a set of results.
     *
     * @param el the query, expressed using the programattic query API
     * @return the set of documents that match the query
     * @throws com.sun.labs.minion.SearchEngineException if there are any errors
     * evaluating the query
     */
    ResultSet search(Element el) throws SearchEngineException;

    /**
     * Runs a query against the index, returning a set of results.
     *
     * @param el the query, expressed using the programattic query API
     * @param sortOrder How the results should be sorted. This is a set of
     * comma-separated field names, each preceeded by a <code>+</code> (for
     * increasing order) or by a <code>-</code> (for decreasing order).
     * @return the set of documents that match the query
     * @throws com.sun.labs.minion.SearchEngineException if there are any errors
     * evaluating the query
     */
    ResultSet search(Element el, String sortOrder) throws SearchEngineException;

    /**
     * Runs a query against the index, returning a set of results.
     *
     * @param el the query, expressed using the programattic query API
     * @param sortOrder How the results should be sorted. This is a set of
     * comma-separated field names, each preceeded by a <code>+</code> (for
     * increasing order) or by a <code>-</code> (for decreasing order).
     * @param queryConfig A query configuration to use for this query. If this
     * is <code>null</code> then the default configuration will be used.
     * @return the set of documents that match the query
     * @throws com.sun.labs.minion.SearchEngineException if there are any errors
     * evaluating the query
     */
    ResultSet search(Element el, String sortOrder, QueryConfig queryConfig)
            throws SearchEngineException;


}// Searcher

