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

import java.util.List;
import java.util.Set;

/**
 * A class that holds the results of a search.
 */
public interface ResultSet {

    /**
     * Gets the search engine that generated this result set.
     * @return the search engine that generated this result set.
     */
    public SearchEngine getEngine();
    
    /**
     * Sets the sorting specification used by this result set.
     * @param sortSpec a sorting specification for this result set.
     */
    public void setSortSpec(String sortSpec);

    /**
     * Sets the results filter that will be used to decide whether results
     * should be returned from this set.
     * 
     * @param rf the results filter to use
     * @see ResultsFilter
     * @see CompositeResultsFilter
     */
    public void setResultsFilter(ResultsFilter rf);

    /**
     * Sets the score modifier that will be used to modify the scores of the
     * documents returned from this set.
     * 
     * @param sm the score modifier to use
     */
    public void setScoreModifier(ScoreModifier sm);

    /**
     * Gets a subset of the query results stored in this set.
     *
     * @param start The position in the set to start getting results,
     * indexed from 0.
     * @param n The number of results to get.  Note that you may get less
     * than this number of results if there are insufficient results in the
     * set.
     * @return A list of <code>Result</code> instances containing the
     * search results.
     *
     * @throws SearchEngineException if there is some error getting the
     * results.
     */
    public List<Result> getResults(int start, int n) throws SearchEngineException;

    /**
     * Gets a subset of the query results stored in this set.
     *
     * @param start The position in the set to start getting results,
     * indexed from 0.
     * @param n The number of results to get.  Note that you may get less
     * than this number of results if there are insufficient results in the
     * set.
     * @param rf a filter to apply against candidate results
     * @return A list of <code>Result</code> instances containing the
     * search results.
     *
     * @throws SearchEngineException if there is some error getting the
     * results.
     */
    public List<Result> getResults(int start, int n, ResultsFilter rf) throws SearchEngineException;

    /**
     * Gets all of the query results in the set.  Note that this list may
     * be very large!
     * @return A list of all the results in the set.
     * @param sorted If <code>true</code>, then the results will be sorted
     * according to the sorting specification that was provided with the
     * query.  If <code>false</code>, then the results will be returned in
     * an arbitrary order.
     * @throws com.sun.labs.minion.SearchEngineException if there is any error processing the result set
     */
    public List<Result> getAllResults(boolean sorted) throws SearchEngineException;
    
    /**
     * Gets all of the query results in the set.  Note that this list may
     * be very large!
     * @return A list of all the results in the set.
     * @param sorted If <code>true</code>, then the results will be sorted
     * according to the sorting specification that was provided with the
     * query.  If <code>false</code>, then the results will be returned in
     * an arbitrary order.
     * @param rf a filter to apply against candidate results
     * @throws com.sun.labs.minion.SearchEngineException if there is any error processing the result set
     */
    public List<Result> getAllResults(boolean sorted, ResultsFilter rf) throws SearchEngineException;

    /**
     * Gets the statistics for the query that generated this set.
     * @return the query statistics for the query that generated this set.
     */
    public QueryStats getQueryStats();
    
    /**
     * Weights the results in this set.
     *
     * @param w a weight to apply multiplicatively to each of the scores in 
     * this set.
     * @return a new result set with the weights applied.  If no normalization 
     * is requested, then the scores for results in the new set may be greater
     * than 0.
     */
    public ResultSet weight(float w);
    
    /**
     * Computes the intersection of this result set with another result set.
     *
     * @param rs the result set with which this set will be intersected.
     * @return a new result set containing only thos documents that occur in 
     * both this set and the set passed in.  The score for documents in the 
     * returned set that occur in both sets will be the sum of the scores
     * from the two sets.  Note that this may result in scores greater than
     * 1 for these documents!
     */
    public ResultSet intersect(ResultSet rs);
    
    /**
     * Computes the intersection of this result set with another result set.
     *
     * @param rs the result set with which this set will be intersected.
     * @return a new result set containing the documents that occur in 
     * either this set or the set passed in (or both).  The score for documents in the 
     * returned set that occur in both sets will be the sum of the scores
     * from the two sets.  Note that this may result in scores greater than
     * 1 for these documents!
     */
    public ResultSet union(ResultSet rs);
    

    /**
     * Computes the difference of this result set and another result set.
     *
     * @param rs the result set with which this set will be intersected.
     * @return a new result set containing the documents that occur in 
     * this set, but <em>not</em> the set passed in.
     */
    public ResultSet difference(ResultSet rs);

    /**
     * Indicates whether the query contained a syntax error.
     *
     * @return <code>true</code> if there was such an error,
     * <code>false</code> otherwise.
     */
    public boolean querySyntaxError();

    /**
     * Gets the size of the results set, that is, the number of documents
     * that matched the query.  If this number is 0, it may be worthwhile
     * to see if the reason is that there was a syntax error in the query.
     *
     * @return The number of documents matching the query.
     * @see #querySyntaxError
     */
    public int size();

    /**
     * Gets the number of documents that are in the whole index.
     * @return the number of documents in the entire collection
     */
    public int getNumDocs();

    /**
     * Gets the amount of time that it took to evaluate the query, in
     * milliseconds.
     * @return the amount of time that it took to evaluate this query, in
     * milliseconds
     */
    public long getQueryTime();
    
    /**
     * Clusters the results into this set into a number of clusters.  The 
     * algorithm used depends on how the engine that generated this set of 
     * results is configured.
     *
     * @param k the maximum number of clusters to return
     * @return a set of clusters containing the results in this set.  The number of clusters
     * returned may be less than or equal to <code>k</code>
     * @throws SearchEngineException if there are any errors generating the clusters
     * @see ResultsCluster
     */
    public Set<ResultsCluster> cluster(int k) throws SearchEngineException;
    
    /**
     * Clusters the results into this set into a number of clusters.  The 
     * algorithm used depends on how the engine that generated this set of 
     * results is configured.
     *
     * @param field the name of a vectored field upon which the clustering 
     * should be based.  A value of <code>null</code> indicates that all vectored
     * fields should be considered, while an empty string indicates that data
     * in no explicit field should be considered.
     * @param k the maximum number of clusters to return
     * @return a set of clusters containing the results in this set.  The number of clusters
     * returned may be less than or equal to <code>k</code>
     * @throws SearchEngineException if there are any errors generating the clusters
     * @see ResultsCluster
     */
    public Set<ResultsCluster> cluster(String field, int k) throws SearchEngineException;
    
    /**
     * Groups the results in this set into a number of clusters.  The results are
     * broken into groups based on the values in the given field.  If a result
     * has more than one value for this field, then it will appear in the group
     * for each field.
     *
     * @param field the name of the field for which we wish to group the results
     * @param ignoreCase if <code>true</code> case will not be taken into account when
     * grouping the results.  Clearly, this only makes sense for string fields.
     * @return a set of clusters grouped by the values of the given field
     * @throws SearchEngineException if there are any errors grouping the
     * results
     */
    public Set<ResultsCluster> groupBy(String field, boolean ignoreCase) throws SearchEngineException;

} // ResultSet
