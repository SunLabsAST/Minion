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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


import com.sun.labs.minion.indexer.partition.DiskPartition;

import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.ScoredGroup;
import com.sun.labs.minion.retrieval.cache.TermCache;
import com.sun.labs.minion.retrieval.cache.TermCacheElement;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;

import java.util.logging.Logger;

/**
 * A helper class for running a big query during classification
 * operations.  The query is run against a single partition.
 */
public class BigQuery {

    /**
     * The partition upon which we're operating.
     */
    protected DiskPartition part;

    /**
     * The field we'll be pulling data from, if there is one.
     */
    protected int fromFieldID;

    protected TermCache tc;

    /**
     * A weighting function to use to calculate term weights.
     */
    protected WeightingFunction wf;

    /**
     * A set of weighting components to use when calculating term weights.
     */
    protected WeightingComponents wc;

    /**
     * Combined scores for our big query.
     */
    protected float[] scores;

    /**
     * A set containing the document IDs of the training examples for this
     * query.
     */
    protected Set<Integer> trainingIDs;

    /**
     * A set containing the document IDs of the training examples that
     * we've already seen.
     */
    protected Set<Integer> seenIDs;

    /**
     * A scored array group that we can use to store final query results
     */
    protected ScoredGroup sg;

    int p;

    Logger logger = Logger.getLogger(getClass().getName());

    protected static String logTag = "BQ";

    /**
     * The postings iterators that can be used to fetch weights for each 
     * of the features making up this big query.
     */
    protected PostingsIterator[] featureIterators;

    protected FeatureCluster[] features;

    /**
     * Creates a big query helper for a given partition.
     *
     * @param tc a cache of postings for terms
     * @param tg the array group containing training data for this partition
     * @param fromField the name of the field terms are coming from
     * @param wf a weighting function to use for weighting terms
     * @param wc a set of weighting components
     */
    public BigQuery(TermCache tc,
            ArrayGroup tg,
            String fromField,
            WeightingFunction wf,
            WeightingComponents wc) {

        //
        // Set the values that we'll need.
        this.tc = tc;
        this.part = tg.getPartition();

        //
        // Figure out the ID of the field of interest.
        fromFieldID = part.getManager().getMetaFile().getVectoredFieldID(
                fromField);

        this.wf = wf;
        this.wc = wc;
        scores = new float[part.getMaxDocumentID() + 1];

        //
        // Make a set of the IDs of the training documents.  First, make
        // sure that we remove deleted documents.
        tg.removeDeleted();
        trainingIDs = new HashSet<Integer>(tg.getSize());
        for(ArrayGroup.DocIterator i = tg.iterator(); i.next();) {
            trainingIDs.add(i.getDoc());
        }
        seenIDs = new HashSet<Integer>(tg.getSize());

    } // BigQuery constructor

    /**
     * A copy constructor.
     * @param bq the query that we want to copy.
     */
    public BigQuery(BigQuery bq) {
        this.tc = bq.tc;
        this.part = bq.part;
        this.wf = bq.wf;
        this.wc = bq.wc;
        this.fromFieldID = bq.fromFieldID;
        trainingIDs = bq.trainingIDs;
        seenIDs = bq.seenIDs;
        scores = new float[part.getMaxDocumentID() + 1];
        features = new FeatureCluster[bq.features.length];
        featureIterators = new PostingsIterator[bq.featureIterators.length];
    }

    /**
     * Adds a number of features clusters to the helper.
     *
     * @param features the set of features to add
     */
    public void addFeatureClusters(FeatureClusterSet features) {
        featureIterators = new PostingsIterator[features.size()];
        this.features = new FeatureCluster[features.size()];
        for(Iterator i = features.iterator(); i.hasNext();) {
            addFeatureCluster((FeatureCluster) i.next());
        }
    }

    /**
     * Adds a cluster to the helper.
     *
     * @param cluster the cluster to be added
     */
    public void addFeatureCluster(FeatureCluster cluster) {

        //
        // Get the term cache element for this cluster and then fetch the weights.
        TermCacheElement e = tc.get(cluster.getName());
        PostingsIterator pi = e.iterator(wc, wf);
        while(pi.next()) {
            scores[pi.getID()] += pi.getWeight() * cluster.getWeight();
        }
        features[p] = cluster;
        featureIterators[p++] = pi;
    }

    /**
     * Gets the scored group associated with this partition
     * @return the group associated with this partition.
     */
    public ScoredGroup getGroup() {
        if(sg == null) {
            sg = new ScoredGroup(2048);
            for(int i = 0; i < scores.length; i++) {
                if(scores[i] > 0) {
                    sg.addDoc(i, scores[i] / part.getDocumentVectorLength(i,
                            fromFieldID));
                }
            }
            sg.setPartition(part);
            sg.removeDeleted();
            sg.sort(true);
        }
        return sg;
    }
} // BigQuery

