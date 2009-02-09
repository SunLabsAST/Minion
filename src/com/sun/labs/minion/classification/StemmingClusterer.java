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

import com.sun.labs.minion.util.PorterStemmer;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Provides a clusterer that groups features that have the same stems.
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.7 $
 */
public class StemmingClusterer extends ContingencyFeatureClusterer {

    /**
     * The log 
     */
    Logger logger = Logger.getLogger(getClass().getName());

    /** 
     * The log tag 
     */
    protected static String logTag = "STEM";

    protected PorterStemmer stemmer = new PorterStemmer();

    protected HashMap clusterMap = new HashMap();

    public StemmingClusterer() {
    }

    public StemmingClusterer(int type) {
        super(type);
    }

    public FeatureClusterer newInstance() {
        return new StemmingClusterer();
    }

    protected void addFeature(ContingencyFeature feat) {
        String name = feat.getName().toLowerCase();
        stemmer.add(name.toCharArray(), name.length());
        stemmer.stem();
        String stem = stemmer.toString();

        //
        // See if we have already encountered this feature stem
        ContingencyFeatureCluster clust =
                (ContingencyFeatureCluster) clusterMap.get(stem);
        if(clust != null) {
            clust.add(feat);
        } else {
            clust = new ContingencyFeatureCluster(stem);
            clust.add(feat);
            clusterMap.put(stem, clust);
        }
    }

    protected FeatureClusterSet getClusters() {
        //
        // Now sort the feature clusters and return it
        FeatureClusterSet fcs = new FeatureClusterSet(clusterMap.values());
        clusterMap = new HashMap();
        return fcs;
    }
}
