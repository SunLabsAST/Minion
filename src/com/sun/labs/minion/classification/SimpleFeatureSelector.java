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

import com.sun.labs.minion.SearchEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import com.sun.labs.minion.clustering.ClusterStatisticsImpl;
import com.sun.labs.minion.pipeline.StopWords;
import com.sun.labs.minion.retrieval.WeightingComponents;

/**
 * A class that selects the top n features from a set of documents based on the
 * weights assigned by a term weighting function.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class SimpleFeatureSelector implements FeatureSelector {
    
    private StopWords stopWords;
    
    /**
     * Creates a SimpleFeatureSelector
     */
    public SimpleFeatureSelector() {
    }

    public void setHumanSelected(HumanSelected hs) {
    }

    public FeatureClusterSet select(FeatureClusterSet set, 
            WeightingComponents wc, 
            int numTrainingDocs, 
            int numFeatures, 
            SearchEngine engine) {
        PriorityQueue<FeatureCluster> pq = 
                new PriorityQueue<FeatureCluster>();
        ClusterStatisticsImpl csi = new ClusterStatisticsImpl();
        List<SimpleFeatureCluster> temp = new ArrayList<SimpleFeatureCluster>();
        for(FeatureCluster fc : set.getContents()) {
            if(stopWords != null && stopWords.isStop(fc.getName())) {
                continue;
            }
            csi.add(fc.getWeight());
            temp.add((SimpleFeatureCluster) fc);
        }

        double threshold = csi.mean() + 100 * csi.variance();
        for(SimpleFeatureCluster fc : temp) {
            if(fc.getWeight() < threshold) {
                pq.offer(fc);
            }
        }
        
        while(pq.size() > numFeatures) {
            pq.poll();
        }
        
        return new FeatureClusterSet(pq);
    }

    public void setStopWords(StopWords stopWords) {
        this.stopWords = stopWords;
    }
}
