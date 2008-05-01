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

package com.sun.labs.minion.classification;

import java.util.Iterator;

import com.sun.labs.minion.SearchEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary.DiskDictionaryIterator;
import com.sun.labs.minion.indexer.entry.QueryEntry;

import com.sun.labs.minion.indexer.partition.DiskPartition;

import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.pipeline.StopWords;
import com.sun.labs.minion.retrieval.TermStats;
import com.sun.labs.minion.retrieval.WeightingComponents;

import com.sun.labs.minion.util.MinionLog;

/**
 * A feature selector that builds contingency features.  The weights
 * calculated from the contingency features depend on the type that is
 * given to the constructor for this class.
 *
 * @see ContingencyFeature#MUTUAL_INFORMATION
 * @see ContingencyFeature#CHI_SQUARED
 */
public class ContingencyFeatureSelector implements FeatureSelector {

    /**
     * How the weights should be calculated for the contingency table.
     */
    protected int type;

    /**
     * Words to ignore during selection.
     */
    protected StopWords stopWords;

    /**
     * A log.
     */
    protected static MinionLog log = MinionLog.getLog();

    /**
     * A tag.
     */
    protected static String logTag = "CFS";

    /**
     * A set of human selected terms for inclusion or exclusion.
     */
    private HumanSelected hs;

    public ContingencyFeatureSelector() {
        this(ContingencyFeature.MUTUAL_INFORMATION);
    }

    /**
     * Makes a feature selector that returns features that use a
     * contingency table to calculate weight.  The type of weight
     * calculated depends on the type that we're given.
     *
     * @param type the type of weight to calculate
     * @see ContingencyFeature#MUTUAL_INFORMATION
     * @see ContingencyFeature#CHI_SQUARED
     */
    public ContingencyFeatureSelector(int type) {
        this.type = type;
    } // ContingencyFeatureSelector constructor

    // Implementation of com.sun.labs.minion.classification.FeatureSelector
    /**
     * Provides a set of human selected terms that should be included or
     * excluded from consideration during the feature selection process.
     * @param hs a set of human selected terms that should be included or
     * excluded during feature selection.
     */
    public void setHumanSelected(HumanSelected hs) {
        this.hs = hs;
    }

    /**
     * Selects the features from the documents that have the highest mutual
     * information with the class represented by the given training set.
     *
     * @param training the set of features in the training set.
     * @param wc a set of weighting components to use when weighting terms
     * @param numTrainingDocs the number of training documents
     * @param numFeatures the number of features to select.
     * @param engine the search engine the features are from
     * @return a sorted set of the features
     */
    public FeatureClusterSet select(FeatureClusterSet training,
                                     WeightingComponents wc,
                                     int numTrainingDocs,
                                     int numFeatures,
                                     SearchEngine engine) {

        //
        // We'll proceed partition by partition.  For each partition we'll
        // build an array of ContingencyFeatures, counting up the values
        // from the training set for that partition.  We'll heap the
        // resulting features.
        // FIXME: Since FeatureClusterSet is in order, we don't really need
        // FIXME: to build this heap.
        PriorityQueue<FeatureCluster> h = new PriorityQueue<FeatureCluster>();
        for(FeatureCluster fc : training) {
            h.offer(fc);
        }

        //
        // Now we have a heap of features that is organized by name.  We
        // want to combine the counts that we've collected so far for the
        // features with the same name and then add the remaining counts.
        // For this we'll need the size of the training set and the total
        // number of documents in the system.
        int tsize = numTrainingDocs;
        int N = engine.getNDocs();

        //
        // We'll keep a heap with the top numFeatures features.
        PriorityQueue<FeatureCluster> m =
                new PriorityQueue<FeatureCluster>(training.size(),
                                                  new ClusterWeightComparator());

        List<FeatureCluster> mustAppear = new ArrayList<FeatureCluster>();

        heapLoop:
        while(h.size() > 0) {
            ContingencyFeatureCluster curr =
                    (ContingencyFeatureCluster) h.poll();
            ContingencyFeatureCluster top = (ContingencyFeatureCluster) h.peek();
            while(top != null && top.getName().equals(curr.getName())) {
                h.poll();
                curr.counter.a += top.counter.a;
                top = (ContingencyFeatureCluster) h.peek();
            }

            //
            // If we have human selected features, and we're supposed to exclude
            // some component of this cluster, then just keep going.
            if(hs != null) {
                for(Feature f : curr) {
                    if(hs.exclude(f.getName())) {
                        continue heapLoop;
                    }
                }
            }

            //
            // If this feature is named for a stop word or contains any stop words,
            // then leave it and keep going.
            if(stopWords != null) {
                for(Feature f : curr) {
                    if(stopWords.isStop(f.getName())) {
                        continue heapLoop;
                    }
                }
            }

            computeContingency(curr, engine, wc, tsize, N);

            if(tsize > 5 && discardFeature(curr.counter, engine)) {
                continue;
            }

            //
            // If this is a feature we've been told to keep, just put it on
            // our list.
            if(hs != null) {
                for(Feature f : curr) {
                    if(hs.include(f.getName())) {
                        mustAppear.add(curr);
                        continue heapLoop;
                    }
                }

                //
                // Apply any weight to this cluster.
                curr.setWeight(curr.getWeight() * hs.getWeight(curr.getName()));
            }


            //
            // See if we should put this value on our heap.
            if(m.size() < numFeatures) {
                m.offer(curr);
            } else {
                top = (ContingencyFeatureCluster) m.peek();
                if(curr.getWeight() > top.getWeight()) {
                    m.poll();
                    m.offer(curr);
                }
            }
        }

        FeatureClusterSet ret = new FeatureClusterSet();

        //
        // Add the features that must appear.
        for(FeatureCluster fc : mustAppear) {
            ret.add(fc);
        }

        while(m.size() > 0) {
            ret.add(m.poll());
        }

        return ret;
    }

    protected void computeContingency(ContingencyFeatureCluster curr,
                                       SearchEngine engine,
                                       WeightingComponents wc,
                                       int tsize, int N) {

        if(curr.getContents().size() == 1) {
            TermStats ts =
                    wc.getTermStats((curr.getContents().first()).getName());
            if(ts != null) {
                curr.counter.b = ts.getDocFreq() - curr.counter.a;
            }
        } else {
            //
            // Now curr has the combined data.  Figure out the other three
            // pieces of the puzzle.
            //
            // First, for b, loop through each partition to get the total
            // doc occurrances
            for(Iterator partIt =
                    engine.getPM().getActivePartitions().iterator();
                    partIt.hasNext();) {
                DiskPartition part = (DiskPartition) partIt.next();
                DiskDictionaryIterator dictIt =
                        (DiskDictionaryIterator) part.getMainDictionaryIterator();
                int[] uniqDocs = new int[0];
                for(Iterator clustIt = curr.getContents().iterator();
                        clustIt.hasNext();) {
                    Feature f = (Feature) clustIt.next();
                    QueryEntry e = dictIt.get(f.getName());
                    if(e != null) {
                        int[] currDocs = new int[e.getN()];
                        int ci = 0;
                        PostingsIterator pi = e.iterator(null);
                        if(pi == null) {
                            log.warn(logTag, 4,
                                     "Null postings iterator for entry");
                            continue;
                        }
                        while(pi.next()) {
                            currDocs[ci++] = pi.getID();
                        }

                        uniqDocs = com.sun.labs.minion.util.Util.union(uniqDocs, currDocs);
                    }
                }
                int i = 0, total = 0;
                while(i < uniqDocs.length) {
                    if(uniqDocs[i++] > 0) {
                        total++;
                    }
                }
                curr.counter.b += total;
            }

            //
            // From the total doc occurances, subtract a to get b
            curr.counter.b = curr.counter.b - curr.counter.a;
        }
        curr.counter.c = tsize - curr.counter.a;
        curr.counter.d = N - curr.counter.c;
        curr.counter.N = N;

        //
        // If the weight was previously calculated, we need to recalc
        // since we changed the a,b,c,d,N values
        curr.counter.weightCalculated = false;
        curr.getWeight();
    }

    /**
     * Determines whether a given feature should be discarded from the
     * set.  This can be overridden in subclasses to use different methods
     * for deciding when to drop a feature.
     * @param cf the feature we want to test
     * @param engine the engine we're using to do the test
     * @return <code>true</code> if the feature should be discarded, <code>false</code>
     * if it should be kept
     */
    protected boolean discardFeature(ContingencyFeature cf,
                                      SearchEngine engine) {
        if((cf.a + cf.b) <= 5 || cf.a <= 3) {
            return true;
        }
        return false;
    }

    public void setStopWords(StopWords stopWords) {
        this.stopWords = stopWords;
    }
} // ContingencyFeatureSelector

