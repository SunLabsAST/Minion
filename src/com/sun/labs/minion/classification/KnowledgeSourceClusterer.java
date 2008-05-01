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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.labs.minion.knowledge.KnowledgeSource;
import com.sun.labs.minion.retrieval.ResultSetImpl;

import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.SearchEngine;

/**
 * Provides an implementation of a feature clusterer built around 
 * a knowledge source. Variants of the same word will be grouped together into
 * clusters. (Adapted from LiteMorphClusterer.)
 * 
 * @author Bernard Horan
 * @version $Id: KnowledgeSourceClusterer.java,v 1.1.2.5 2008/01/17 18:34:30 stgreen Exp $
 */
public class KnowledgeSourceClusterer extends ContingencyFeatureClusterer {

    /**
     * A map to hold the clusters keyed on roots
     */
    protected Map<String, FeatureCluster> clusterMap;

    private KnowledgeSource knowledgeSource;

    /**
     * Create a new instance of me
     * 
     * @param type
     */
    public KnowledgeSourceClusterer(int type) {
        super(type);
        initializeClusterMap();
    }

    /**
     * Initiliaze the cluster map
     */
    private void initializeClusterMap() {
        clusterMap = new HashMap<String, FeatureCluster>();
    }

    /**
     * Default constructor
     */
    public KnowledgeSourceClusterer() {
        super();
        initializeClusterMap();
    }

    /**
     * Initialize the morphology. <br>
     * The index config for the search engine MUST have specified that 
     * the taxonomy is enabled and also provided a lexicon for the morphology engine.
     * @param anEngine a search engine
     */
    private void initializeKnowledgeSource(SearchEngine anEngine) {
        QueryConfig queryConfig = anEngine.getQueryConfig();
        knowledgeSource = queryConfig.getKnowledgeSource();
        if (knowledgeSource == null) {
            throw new RuntimeException("No knowledge source specified in query config");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.sun.labs.minion.classification.ContingencyFeatureClusterer#cluster(com.sun.labs.minion.retrieval.ResultSetImpl)
     */
    public FeatureClusterSet cluster(ResultSetImpl s) {
        initializeKnowledgeSource(s.getEngine());
        return super.cluster(s);
    }

    /* (non-Javadoc)
     * @see com.sun.labs.minion.classification.ContingencyFeatureClusterer#newInstance()
     */
    public FeatureClusterer newInstance() {
        return new KnowledgeSourceClusterer();
    }

    /* (non-Javadoc)
     * @see com.sun.labs.minion.classification.ContingencyFeatureClusterer#getClusters()
     */
    protected FeatureClusterSet getClusters() {
        //
        // Sort the feature clusters and return it
        FeatureClusterSet fcs = new FeatureClusterSet(clusterMap.values());
        initializeClusterMap();
        return fcs;
    }

    /* (non-Javadoc)
     * @see com.sun.labs.minion.classification.ContingencyFeatureClusterer#addFeature(com.sun.labs.minion.classification.ContingencyFeature)
     */
    protected void addFeature(ContingencyFeature feat) {
        String name = feat.getName();
    
        Set<String> variants = knowledgeSource.variantsOf(name);
        
        if (variants.size() == 0) {
            variants = new HashSet<String>();
            variants.add(name);
        }
        //
        // See if the feature is already in a cluster.  It will
        // be in the cluster map based on its shortest variant, earliest
        // in the alphabet.
        // If it is in, then all the variants will be.  Otherwise,
        // add a new cluster with all the variants
        String shortest = name;
        for (Iterator<String> variantIterator = variants.iterator(); variantIterator.hasNext();) {
            String variant = variantIterator.next();
            if (variant.length() < shortest.length()) {
                if (variant.compareTo(shortest) < 0) {
                    shortest = variant;
                }
            }
        }
        
        ContingencyFeatureCluster clust = (ContingencyFeatureCluster) clusterMap.get(shortest);
        if (clust == null) {
            clust = new ContingencyFeatureCluster(shortest);
            for (Iterator<String> variantIterator = variants.iterator(); variantIterator.hasNext();) {
                String variant = variantIterator.next();
                if (variant.equalsIgnoreCase(feat.getName())) {
                    continue;
                }
                ContingencyFeature curr = new ContingencyFeature(feat);
                curr.setName(variant);
                clust.add(curr);
            }
            clust.add(feat);
            clust.docIDs = null;
            clusterMap.put(shortest, clust);
        } 
        
    }

}
