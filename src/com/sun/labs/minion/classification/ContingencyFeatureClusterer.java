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

import com.sun.labs.minion.util.MinionLog;

import com.sun.labs.minion.indexer.partition.DiskPartition;

import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.ResultSetImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.retrieval.cache.DocCache;
import com.sun.labs.minion.retrieval.cache.DocCacheElement;


/**
 * This class provides an implementation of a feature clusterer that
 * clusters contingency features.  This class is meant to provide
 * structure for more sophisticated clusterers, and as such its
 * clustering is very basic - it only clusters terms into groups
 * by themselves.
 */

public class ContingencyFeatureClusterer implements FeatureClusterer {
    /**
     * The log
     */
    protected static MinionLog log = MinionLog.getLog();
    
    /**
     * The log tag
     */
    protected static String logTag = "CFC";
    
    /**
     * The field from which features should be drawn.
     */
    protected String field;
    
    /**
     * The type of contingency feature to use.
     */
    protected int type;
    
    /**
     * Internal storage for the clusters that are generated
     */
    protected FeatureClusterSet clusters;
    
    /**
     * A cache of document vectors.
     */
    private DocCache dc;
    
    public ContingencyFeatureClusterer() {
        clusters = new FeatureClusterSet();
        type = ContingencyFeature.MUTUAL_INFORMATION;
    }
    
    /**
     * A Feature Clusterer operates on a set of featurees.  When a new
     * clusterer is created, a new set of clusters will be operated on.
     * Sets of clusters correspond to partitions, and these partitions
     * will likely be merged.
     */
    public ContingencyFeatureClusterer(int type) {
        clusters = new FeatureClusterSet();
        this.type = type;
    }
    
    public FeatureClusterer newInstance() {
        return new ContingencyFeatureClusterer();
    }
    
    public FeatureCluster newCluster() {
        return new ContingencyFeatureCluster();
    }
    
    public Feature newFeature() {
        return new ContingencyFeature(type);
    }
    
    public void setDocCache(DocCache dc) {
        this.dc = dc;
    }
    
    /**
     * Creates a set of clusters based on all of the terms in
     * the documents contained in the ResultSet.
     *
     * @param s the set of documents from which features are gathered
     * @return a set of clusters (Features) of features
     */
    public FeatureClusterSet cluster(ResultSetImpl s) {
        ArrayList<FeatureClusterSet> clusterSets = new ArrayList<FeatureClusterSet>();
        for (Iterator rsi = s.resultsIterator(); rsi.hasNext(); ) {
            for (ContingencyFeature cf : collectFeatures((ArrayGroup)rsi.next())) {
                addFeature(cf);
            }
            clusterSets.add(getClusters());
        }
        FeatureClusterSet result = FeatureClusterSet.merge(clusterSets);
        return result;
    }
    
    /**
     * Adds a feature to this feature clusterer.  This may create a new
     * cluster for the feature, or may add the feature to an existing
     * cluster.  If you're writing your own clusterer, override this
     * method.
     *
     * @param cf the feature to add
     */
    protected void addFeature(ContingencyFeature cf) {
        //
        // If we add the same, we should probably combine the counts
        FeatureCluster existing = clusters.get(cf.name);
        if (existing != null) {
            existing.add(cf);
        } else {
            ContingencyFeatureCluster c =
                    new ContingencyFeatureCluster(cf.name);
            c.add(cf);
            clusters.add(c);
        }
    }
    
    /**
     * Returns a set of feature clusters.  Feature Clusters in this case
     * will be ContingencyFeatures that represent clusters rather than
     * single features.  If you're writing your own clusterer, override
     * this method.
     *
     * @return a set of clusters
     */
    protected FeatureClusterSet getClusters() {
        FeatureClusterSet ret = clusters;
        clusters = new FeatureClusterSet();
        return ret;
    }
    
    /**
     * Collects terms from the array group, creating contingency features
     * for each one.
     *
     * @param ag the array group
     * @return a set of contingency features
     */
    protected Set<ContingencyFeature> collectFeatures(ArrayGroup ag) {
        
        DiskPartition part = ag.getPartition();
        ContingencyFeature[] ret = new ContingencyFeature[128];
        Map<Integer,String> tm = new HashMap<Integer,String>();

        //
        // Get a private copy of the document dictionary.
        DictionaryIterator ddi = (DictionaryIterator) part.getDocumentIterator();
        
        //
        // For each document ID that we encounter, we'll pull the
        // document term and then iterate through the terms in that,
        // document.
        int processed = 0;
        for(ArrayGroup.DocIterator i = ag.iterator(); i.next(); ) {
            DocKeyEntry e = (DocKeyEntry) ddi.get(i.getDoc());
            if(e == null) {
                log.warn(logTag, 3, "No document term for " +
                        part + " " + i.getDoc());
                continue;
            }
            
            DocCacheElement dce = dc.get(e, field, part);
            processed++;
            if(ag.getSize() > 1000 && processed % 1000 == 0) {
                log.log(logTag, 3, String.format("Processed %d/%d", processed, ag.getSize()));
            }
            
            //
            // Make sure we've got enough room for these features.
            WeightedFeature[] v = dce.getFeatures();
            
            //
            // We might have an empty vector!
            if(v.length == 0) {
                continue;
            }
            
            if(v[v.length-1].getID() >= ret.length) {
                ContingencyFeature[] temp = new ContingencyFeature[v[v.length-1].getID()+1];
                System.arraycopy(ret, 0, temp, 0, ret.length);
                ret = temp;
            }
            
            //
            // Compute the count of these features in the documents in the set.
            for(WeightedFeature wf : v) {
                
                int id = wf.getID();
                
                if(ret[id] == null) {
                    
                    //
                    // Make a new contingency feature of the right type.
                    ret[id] = new ContingencyFeature(type);
                    ret[id].setName(wf.getName());
                }
                
                //
                // This feature is in a document that's in the training set
                // (i.e., in the class, so increment that element of the
                // contingency table.
                //ret[id].a++;
                ret[id].addDoc(e.getID());
            }
        }
        log.log(logTag, 3, String.format("Processed %d/%d", processed,
                ag.getSize()));

        
        //
        // Now, for each feature, check to see whether we want to ignore it.  If
        // not, go ahead and add it to our feature cluster set.
        Set<ContingencyFeature> cfset = new HashSet<ContingencyFeature>();
        for(int i = 0; i < ret.length; i++) {
            if (ret[i] != null && !ret[i].getName().matches(".*\\d.*")) {
                cfset.add(ret[i]);
            }
        }
        
        return cfset;
    }
    
    public void setField(String field) {
        this.field = field;
    }
}
