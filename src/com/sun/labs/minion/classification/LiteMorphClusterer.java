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
import java.util.Set;

import com.sun.labs.minion.lexmorph.LiteMorph;
import com.sun.labs.minion.lexmorph.LiteMorph_en;

import java.util.logging.Logger;

/**
 * Provides an implementation of a feature clusterer built around the light
 * morphology engine.  Morphs of the same word will be grouped together into
 * clusters.
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.7 $
 */
public class LiteMorphClusterer extends ContingencyFeatureClusterer {

    /** 
     * The log 
     */
    Logger logger = Logger.getLogger(getClass().getName());

    /** 
     * The log tag 
     */
    protected static String logTag = "MRPH";

    /**
     * The morphological analyzer to use.  Default to english.
     */
    public LiteMorph morpher = LiteMorph_en.getMorph();

    protected HashMap<String, FeatureCluster> clusterMap =
            new HashMap<String, FeatureCluster>();

    public LiteMorphClusterer() {
    }

    public LiteMorphClusterer(int type) {
        super(type);
    }

    public FeatureClusterer newInstance() {
        return new LiteMorphClusterer();
    }

    protected void addFeature(ContingencyFeature feat) {
        String name = feat.getName().toLowerCase();

        Set<String> morphs = morpher.variantsOf(name);

        if(morphs.size() == 0) {
            morphs = new HashSet<String>();
            morphs.add(name);
        }
        //
        // See if the feature is already in a cluster.  It will
        // be in the cluster map based on its shortest morph, earliest
        // in the alphabet.
        // If it is in, then all the morphs will be.  Otherwise,
        // add a new cluster with all the morphs
        String shortest = name;
        for(Iterator<String> morphIterator = morphs.iterator(); morphIterator.
                hasNext();) {
            String morph = morphIterator.next();
            if(morph.length() < shortest.length()) {
                if(morph.compareTo(shortest) < 0) {
                    shortest = morph;
                }
            }
        }

        ContingencyFeatureCluster clust =
                (ContingencyFeatureCluster) clusterMap.get(shortest);
        if(clust == null) {
            clust = new ContingencyFeatureCluster(shortest);
            for(Iterator<String> morphIterator = morphs.iterator(); morphIterator.
                    hasNext();) {
                String morph = morphIterator.next();
                if(morph.equalsIgnoreCase(feat.getName())) {
                    continue;
                }
                ContingencyFeature curr = new ContingencyFeature(feat);
                curr.setName(morph);
                clust.add(curr);
            }
            clust.add(feat);
            clust.docIDs = null;
            clusterMap.put(shortest, clust);
        }

    }

    protected FeatureClusterSet getClusters() {
        //
        // Now sort the feature clusters and return it
        FeatureClusterSet fcs = new FeatureClusterSet(clusterMap.values());
        clusterMap = new HashMap<String, FeatureCluster>();
        return fcs;
    }
}
