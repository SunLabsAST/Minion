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
package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.Facet;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.HLPipeline;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.ResultsFilter;
import com.sun.labs.minion.ScoreModifier;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.indexer.dictionary.DiskDictionaryBundle.Fetcher;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.util.QueuableIterator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.directory.SearchResult;

/**
 * An implementation of the results set interface.
 */
public class ResultSetImpl implements ResultSet {

    /**
     * The query, as provided by tmhe user.
     */
    protected QueryElement query;

    /**
     * The search engine that is creating the results set.
     */
    protected SearchEngine e;

    /**
     * The configuration used for the query that generated this results
     * set.
     */
    protected QueryConfig qc;

    /**
     * The statistics for the collection.
     */
    protected CollectionStats cs;

    /**
     * The sorting specification for this query.
     */
    protected SortSpec sortSpec;

    /**
     * A results filter to use when getting results.
     */
    protected ResultsFilter rf;

    /**
     * A score modifier to use when getting results.
     */
    private ScoreModifier sm;

    /**
     * A set of query statistics for the query that produced this set.
     */
    private QueryStats qs;

    /**
     * The results of the search:  a list of ArrayGroups.
     */
    protected List<ArrayGroup> results;

    /**
     * A pipeline that can be used for highlighting documents from this
     * set.
     */
    protected HLPipeline hlp;

    /**
     * The time spent parsing and evaluating the query, in milliseconds.
     */
    protected long queryTime;

    protected static Logger logger = Logger.getLogger(ResultSetImpl.class.getName());

    protected static String logTag = "RSI";

    /**
     * Creates an empty result set.
     */
    public ResultSetImpl(SearchEngine e) {
        this.e = e;
    }

    /**
     * Creates a result set for the given query by evaluating the query.
     *
     * @param query The query typed by the user.
     * query.
     * @param e The engine against which we will run the query.
     * @param qs The statistics to accumulate for this query.
     * @param qc The query configuration for this query.
     * @param partitions The partitions against which we will run the
     * query.
     */
    public ResultSetImpl(QueryElement query,
            QueryConfig qc,
            QueryStats qs,
            Collection<DiskPartition> partitions,
            SearchEngine e) {
        this.query = query;
        this.e = e;
        this.qc = qc;
        this.qs = qs;
        this.sortSpec = new SortSpec(e.getManager(), qc.getSortSpec());
        this.qs.queryW.start();

        //
        // Set up the collection-level statistics for the weighting
        // function that we will use.
        query.setQueryConfig(qc);
        query.setWeightingFunction(qc.getWeightingFunction());
        query.setWeightingComponents(qc.getWeightingComponents());

        //
        // Go ahead and evaluate.
        try {
            results = (new QueryEvaluator()).eval(partitions, query);
        } catch(Exception qe) {
            logger.log(Level.SEVERE, "Error evaluating query: " + query, qe);
            results = new ArrayList();
        }

        //
        // In order that the document counts are accurate, we need to
        // remove deleted documents at this point.
        for(int i = 0; i < results.size(); i++) {
            results.get(i).removeDeleted();
        }
        this.qs.queryW.stop();
    } // ResultSetImpl constructor

    /**
     * Constructs a partial result set from the docs passed
     * in.
     *
     * @param parent the full result set that this is a subsest of
     * @param docs the docs to be included in this subset
     */
    public ResultSetImpl(ResultSetImpl parent, AGDocs[] docs) {
        e = parent.e;
        query = parent.query;
        qc = parent.qc;
        cs = parent.cs;
        sortSpec = parent.sortSpec;
        rf = parent.rf;
        hlp = parent.hlp;
        queryTime = parent.queryTime;

        results = new ArrayList();
        for(int i = 0; (i < docs.length) && (docs[i] != null); i++) {
            ArrayGroup ag = (ArrayGroup) docs[i].group.clone();
            ag.retain(docs[i].docIDs);
            results.add(ag);
        }
    }

    /**
     * Creates a result set from a list of groups.
     */
    public ResultSetImpl(SearchEngine e, QueryConfig qc, List results) {
        this.e = e;
        this.qc = qc;
        this.sortSpec = new SortSpec(e.getManager(), qc.getSortSpec());
        this.results = results;
    }

    /**
     * Creates a result set from a list of groups.
     */
    public ResultSetImpl(SearchEngine e, String spec, List results) {
        this.e = e;
        qc = e.getQueryConfig();
        qc.setSortSpec(spec);
        this.sortSpec = new SortSpec(e.getManager(), spec);
        this.results = results;
    }

    /**
     * Gets the search engine that generated this results set.
     */
    @Override
    public SearchEngine getEngine() {
        return e;
    }

    @Override
    public void setSortSpec(String sortSpec) {
        this.sortSpec = new SortSpec(e.getManager(), sortSpec);
    }

    @Override
    public void setResultsFilter(ResultsFilter rf) {
        this.rf = rf;
    }

    @Override
    public void setScoreModifier(ScoreModifier sm) {
        this.sm = sm;
    }

    public QueryElement getQuery() {
        return query;
    }
    
    /**
     * Gets the results from this result set ranked using a score field that is associated
     * with another field.  This is useful, for example, when trying to sort a 
     * set of results by a classifier confidence score.  We can't use a standard
     * sorting specification to do this because it always uses the first value for
     * a field, which may not be the right one.
     * 
     * @param start the position in the ranked list to start getting results
     * @param n the number of results to return
     * @param field the field containing the values that the search was done with
     * @param value the value that we searched for in the field
     * @param scoreField the field containing the scores associated with the field
     * @return a list of the results with the highest score in the score field 
     * corresponding to the position of the value in the given field.  If the 
     * same value is stored more than once in the given field, the first associated
     * score is used for ranking.
     * @throws com.sun.labs.minion.SearchEngineException if there is any exception
     * getting the results
     */
    public List<Result> getResultsForScoredField(int start, int n,
            String field, Object value, String scoreField) throws SearchEngineException {

        try {
            //
            // Info for our two fields.
            FieldInfo vfi = e.getFieldInfo(field);
            FieldInfo sfi = e.getFieldInfo(scoreField);
            if(vfi == null || sfi == null) {
                logger.warning("Unknown field in getResultsForScoredField!");
                return new ArrayList<Result>();
            }

            //
            // A place to organize our hits and a place to store the
            // information for one hit.
            PriorityQueue<ResultImpl> sorter =
                    new PriorityQueue<ResultImpl>(start + n);
            ResultImpl curr = new ResultImpl();
            List<Object> values = new ArrayList<Object>();
            List<Object> scores = new ArrayList<Object>();

            for(Iterator i = results.iterator(); i.hasNext();) {
                ArrayGroup ag = (ArrayGroup) i.next();
                //
                // Fetchers for our two fields. Huzzah!      
                Fetcher vf = ((InvFileDiskPartition) ag.part).
                        getDF(vfi).getFetcher();
                Fetcher sf = ((InvFileDiskPartition) ag.part).
                        getDF(sfi).getFetcher();
                ArrayGroup.DocIterator iter = ag.iterator();

                while(iter.next()) {

                    //
                    // Find the score associatd with the value for this result.
                    values.clear();
                    scores.clear();
                    vf.fetch(iter.getDoc(), values);
                    sf.fetch(iter.getDoc(), scores);
                    float score = 0;
                    for(Iterator j = values.iterator(), k = scores.iterator(); j.
                            hasNext() &&
                            k.hasNext();) {
                        try {
                            Object v = j.next();
                            Double s = (Double) k.next();
                            if(value.equals(v)) {
                                score = s.floatValue();
                                break;
                            }
                        } catch(ClassCastException cce) {
                            throw new SearchEngineException("Score field " +
                                    scoreField + " is not Float!");
                        }
                    }

                    //
                    // Fill in our result from the iterator and use the score
                    // we found.
                    curr.init(this, ag, null, iter.getDoc(), score);

                    //
                    // If we don't have enough results, just put this on the
                    // heap.
                    if(sorter.size() < start + n) {
                        if(rf == null || rf.filter(curr)) {
                            sorter.offer(curr);
                            curr = new ResultImpl();
                        }
                    } else {

                        //
                        // See if this one is larger than the one on the heap.
                        // If it is, *then* run the filter, so that we only run
                        // the filter for the ones that need it.
                        ResultImpl top = sorter.peek();
                        if(curr.score > top.score &&
                                (rf == null || rf.filter(curr))) {

                            //
                            // It is.  Replace the top and use the old top the
                            // next time around the loop.
                            top = sorter.poll();
                            sorter.offer(curr);
                            curr = top;
                        }
                    }
                }
            }

            //
            // Dump the heap to a list.  We're pulling the hits off in
            // least-to-greatest order, so we'll need to reverse the list.
            List<Result> ret = new ArrayList<Result>(sorter.size());
            while(sorter.size() > 0) {
                ret.add(sorter.poll());
            }
            Collections.reverse(ret);

            //
            // Advance to the starting postion.
            for(int i = 0; ret.size() > 0 && i < start; i++) {
                ret.remove(0);
            }

            //
            // Return the rest.
            return ret;
        } catch(Exception ex) {
            throw new SearchEngineException("Error getting search results", ex);
        }
    }

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
    @Override
    public List<Result> getResults(int start, int n) throws SearchEngineException {
        return getResults(start, n, null);
    }

    /**
     * Gets a subset of the query results stored in this set.
     *
     * @param start The position in the set to start getting results,
     * indexed from 0.
     * @param n The number of results to get.  Note that you may get less
     * than this number of results if there are insufficient results in the
     * set.
     * @param rf a filter that will be used to determine whether results
     * should be put on the heap or not.
     * @return A list of <code>Result</code> instances containing the
     * search results.
     *
     * @throws SearchEngineException if there is some error getting the
     * results.
     */
    @Override
    public List<Result> getResults(int start, int n, ResultsFilter rf) throws SearchEngineException {

        //
        // If we were not given a results filter here, use the one associated
        // with the instance.
        if(rf == null) {
            rf = this.rf;
        }

        //
        // Quick check for someone asking for everything.
        if(start + n >= size()) {
            List<Result> l = getAllResults(true, rf);
            if(start > l.size()) {
                return new ArrayList<Result>();
            }
            return l.subList(start, l.size());
        }

        try {
            //
            // A place to organize our hits and a place to store the
            // information for one hit.
            PriorityQueue<ResultImpl> sorter =
                    new PriorityQueue<ResultImpl>(start + n);
            ResultImpl curr = new ResultImpl();
            curr.setQueryStats(qs);

            for(Iterator i = results.iterator(); i.hasNext();) {
                ArrayGroup ag = (ArrayGroup) i.next();
                ag.setScoreModifier(sm);
                SortSpec pss = new SortSpec(sortSpec,
                        (InvFileDiskPartition) ag.part);
                ArrayGroup.DocIterator iter = ag.iterator();
                while(iter.next()) {

                    //
                    // Fill in our result from the iterator.
                    curr.init(this,
                            ag,
                            pss,
                            iter.getDoc(),
                            iter.getScore());

                    //
                    // If we don't have enough results, just put this on the
                    // heap.
                    if(sorter.size() < start + n) {
                        if(rf == null || rf.filter(iter)) {
                            curr.setFields();
                            sorter.offer(curr);
                            curr = new ResultImpl();
                        }
                    } else {

                        //
                        // See if this one is larger than the one on the heap.
                        // If it is, *then* run the filter, so that we only run
                        // the filter for the ones that need it.
                        ResultImpl top = sorter.peek();
                        if(curr.compareTo(top) > 0) {
                            if(rf == null || rf.filter(iter)) {

                                //
                                // It is.  Replace the top and use the old top the
                                // next time around the loop.
                                curr.setFields();
                                top = sorter.poll();
                                sorter.offer(curr);
                                curr = top;
                            }
                        }
                    }
                }
            }

            //
            // Dump the heap to a list.  We're pulling the hits off in
            // least-to-greatest order, so we'll need to reverse the list.
            List<Result> ret = new ArrayList<Result>(sorter.size());
            while(sorter.size() > 0) {
                ret.add(sorter.poll());
            }
            Collections.reverse(ret);

            //
            // Advance to the starting postion.
            for(int i = 0; ret.size() > 0 && i < start; i++) {
                ret.remove(0);
            }

            //
            // Return the rest.
            return ret;
        } catch(Exception ex) {
            throw new SearchEngineException("Error getting search results", ex);
        }
    }

    /**
     * Gets all of the query results in the set.  Note that this list may
     * be very large!
     *
     * @param sorted If <code>true</code>, then the results will be sorted
     * according to the sorting specification that was provided with the
     * query.  If <code>false</code>, then the results will be returned in
     * an arbitrary order.
     * @return A list of all the results in the set.
     */
    @Override
    public List<Result> getAllResults(boolean sorted) throws SearchEngineException {
        return getAllResults(sorted, null);
    }

    /**
     * Gets all of the query results in the set.  Note that this list may
     * be very large!
     *
     * @param sorted If <code>true</code>, then the results will be sorted
     * according to the sorting specification that was provided with the
     * query.  If <code>false</code>, then the results will be returned in
     * an arbitrary order.
     * @param rf a filter to use to decide which results from the search should
     * be added to the list.  If this is <code>null</code>, then no filter will
     * be applied.
     * @return A list of all the results in the set.
     */
    @Override
    public List<Result> getAllResults(boolean sorted, ResultsFilter rf) throws SearchEngineException {

        //
        // If we weren't given a results filter, use the one associated with the
        // instance.
        if(rf == null) {
            rf = this.rf;
        }

        //
        // Depending on whether we want things sorted or not, we'll use a priority
        // queue or a list.
        Collection<Result> ret =
                sorted ? new PriorityQueue<Result>() : new ArrayList<Result>();

        for(Iterator i = results.iterator(); i.hasNext();) {
            ArrayGroup ag = (ArrayGroup) i.next();
            if(sm != null) {
                ag.setScoreModifier(sm);
            }
            SortSpec pss =
                    new SortSpec(sortSpec, (InvFileDiskPartition) ag.part);
            ArrayGroup.DocIterator iter = ag.iterator();
            while(iter.next()) {
                ResultImpl ri = new ResultImpl(this, ag, pss,
                        iter.getDoc(),
                        iter.getScore());
                if(rf == null || rf.filter(iter)) {
                    ri.setFields();
                    ret.add(ri);
                }
            }
        }

        //
        // If necessary, dump the queue in order into the result list.
        List<Result> fres;
        if(sorted) {
            fres = new ArrayList<Result>();
            while(ret.size() > 0) {
                fres.add(((PriorityQueue<Result>) ret).poll());
            }
            //
            // The heap is a min heap!
            Collections.reverse(fres);
        } else {
            fres = (List<Result>) ret;
        }

        return fres;
    }
    
    public List<Facet> getFacets(String fieldName) throws SearchEngineException {
        FieldInfo field = e.getFieldInfo(fieldName);
        if(field == null) {
            throw new SearchEngineException("Can't facet on unknown field " + fieldName);
        }
        if(!field.hasAttribute(FieldInfo.Attribute.SAVED)) {
            throw new SearchEngineException("Can only facet on saved fields");
        }
        
        //
        // Queue up the results from each partition.
        PriorityQueue<QueuableIterator<LocalFacet>> q = new PriorityQueue(results.size());
        for(ArrayGroup ag : results) {
            List<LocalFacet> l = ((InvFileDiskPartition) ag.part).getDF(field).getFacets(ag.docs, ag.size);
            QueuableIterator<LocalFacet> qi = new QueuableIterator(l.iterator());
            if(qi.hasNext()) {
                qi.next();
                q.add(qi);
            }
        }
        List<Facet> ret = new ArrayList<Facet>();
        //
        // Build facets as we go.
        while(!q.isEmpty()) {
            QueuableIterator<LocalFacet> top = q.peek();
            FacetImpl f = new FacetImpl(field, 
                    (Comparable) top.getCurrent().getFacetValue());
            logger.info(String.format("top: %s", f.getValue()));
            while(q.peek().getCurrent().getFacetValue().equals(f.getValue())) {
                QueuableIterator<LocalFacet> qi = q.poll();
                LocalFacet lf = qi.getCurrent();
                List<Integer> docIDs = lf.getDocIDs();
                for(int docID : docIDs) {
                    f.add(new ResultImpl(this, null, null, docID, 1));
                }
                if(qi.hasNext()) {
                    qi.next();
                    q.add(qi);
                }
                
            }
            ret.add(f);
        }
        return ret;
    }

    public void setQueryStats(QueryStats qs) {
        this.qs = qs;
    }

    @Override
    public QueryStats getQueryStats() {
        return qs;
    }

    /**
     * Indicates whether the query contained a syntax error.
     *
     * @return <code>true</code> if there was such an error,
     * <code>false</code> otherwise.
     */
    @Override
    public boolean querySyntaxError() {
        return false;
    }

    /**
     * Gets the size of the results set, that is, the number of documents
     * that matched the query.  If this number is 0, it may be worthwhile
     * to see if the reason is that there was a syntax error in the query.
     *
     * @return The number of documents matching the query.
     * @see #querySyntaxError
     */
    @Override
    public int size() {
        int size = 0;
        for(Iterator i = results.iterator(); i.hasNext();) {
            size += ((ArrayGroup) i.next()).getSize();
        }
        return size;
    }

    /**
     * Gets the highlighting pipeline.
     */
    public HLPipeline getHLPipeline() {
        if(hlp == null) {
            hlp = e.getHLPipeline();
        }
        return hlp;
    }

    /**
     * Gets the number of documents that are in the whole index.
     */
    @Override
    public int getNumDocs() {
        return e.getNDocs();
    }

    /**
     * Gets the amount of time that it took to evaluate the query, in
     * milliseconds.
     */
    @Override
    public long getQueryTime() {
        return (long) qs.queryW.getTimeMillis();
    }

    /**
     * Gets an iterator for the per-partition results.  This is in the
     * implementation only, not the interface!
     */
    public Iterator<ArrayGroup> resultsIterator() {
        return results.iterator();
    }

    public AGDocs newAGDocs(ArrayGroup ag) {
        return new AGDocs(ag);
    }

    @Override
    public ResultSet weight(float w) {
        ResultSetImpl ret = new ResultSetImpl(this.e, this.qc, null);
        ret.results = new ArrayList();
        for(Iterator i = results.iterator(); i.hasNext();) {
            ret.results.add(((ArrayGroup) i.next()).mult(w));
        }
        return ret;
    }

    @Override
    public ResultSet intersect(ResultSet rs) {
        ResultSetImpl ret = new ResultSetImpl(this.e, this.qc, null);
        ret.sortSpec = sortSpec;
        ret.results = new ArrayList();
        for(Iterator<ArrayGroup> i = results.iterator(); i.hasNext();) {
            ArrayGroup ag1 = i.next();
            for(Iterator<ArrayGroup> j = ((ResultSetImpl) rs).results.iterator();
                    j.hasNext();) {
                ArrayGroup ag2 = j.next();
                if(ag1.part == ag2.part) {
                    ArrayGroup iag = ag1.intersect(ag2);
                    iag.setPartition(ag1.part);
                    ret.results.add(iag);
                }
            }
        }
        return ret;
    }

    @Override
    public ResultSet union(ResultSet rs) {
        ResultSetImpl ret = new ResultSetImpl(this.e, this.qc, null);
        ret.results = new ArrayList();
        for(Iterator<ArrayGroup> i = results.iterator(); i.hasNext();) {
            ArrayGroup ag1 = i.next();
            for(Iterator<ArrayGroup> j = ((ResultSetImpl) rs).results.iterator();
                    j.hasNext();) {
                ArrayGroup ag2 = j.next();
                if(ag1.part == ag2.part) {
                    ArrayGroup uag = ag1.union(ag2);
                    uag.setPartition(ag1.part);
                    ret.results.add(uag);
                }
            }
        }
        return ret;
    }

    @Override
    public ResultSet difference(ResultSet rs) {
        ResultSetImpl ret = new ResultSetImpl(this.e, this.qc, null);
        ret.results = new ArrayList();
        for(Iterator<ArrayGroup> i = results.iterator(); i.hasNext();) {
            ArrayGroup ag1 = i.next();
            for(Iterator<ArrayGroup> j = ((ResultSetImpl) rs).results.iterator();
                    j.hasNext();) {
                ArrayGroup ag2 = j.next();
                if(ag1.part == ag2.part) {
                    ArrayGroup iag = ag1.intersect(ag2.getNegative());
                    iag.setPartition(ag1.part);
                    ret.results.add(iag);
                }
            }
        }
        return ret;
    }

    /**
     * A method to test whether two result sets are the same.
     *
     * @param other the other set to test against this one.
     * @return <code>true</code> if the documents contain the same documents
     * and the documents have the same scores, <code>false</code> otherwise.
     */
    public boolean same(ResultSetImpl other) {
        for(Iterator<ArrayGroup> i = results.iterator(); i.hasNext();) {
            ArrayGroup ag1 = i.next();
            for(Iterator<ArrayGroup> j = other.results.iterator();
                    j.hasNext();) {
                ArrayGroup ag2 = j.next();
                if(ag1.part == ag2.part) {
                    if(!ag1.equals(ag2)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "ResultSetImpl{" + "results=" + results + '}';
    }

    public class AGDocs {

        protected int[] docIDs;

        protected ArrayGroup group;

        protected int numDocs;

        public AGDocs(ArrayGroup group) {
            this.group = group;
            numDocs = 0;
            docIDs = new int[10];
        }

        public void add(int id) {
            if(numDocs >= docIDs.length) {
                int[] d = new int[numDocs * 2];
                System.arraycopy(docIDs, 0, d, 0, numDocs);
                docIDs = d;
            }
            docIDs[numDocs++] = id;
        }

        public int[] getIDs() {
            int[] ids = new int[numDocs];
            System.arraycopy(docIDs, 0, ids, 0, numDocs);
            return ids;
        }

        public ArrayGroup getGroup() {
            return group;
        }
    }
} // ResultSetImpl

