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

import com.sun.labs.minion.Feature;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.pipeline.StopWords;
import com.sun.labs.minion.retrieval.WeightingComponents;
import java.util.PriorityQueue;

/**
 * Selects the highest weighted features.
 */
public class WeightedFeatureSelector implements FeatureSelector {

    /**
     * Words to ignore during selection.
     */
    protected StopWords stopWords;

    @Override
    public void setHumanSelected(HumanSelected hs) {
    }

    @Override
    public FeatureClusterSet select(FeatureClusterSet set,
            WeightingComponents wc, int numTrainingDocs, int numFeatures,
            SearchEngine engine) {
            PriorityQueue<FeatureCluster> heap =
                    new PriorityQueue<FeatureCluster>(numFeatures);
        for(FeatureCluster fc : set) {
            if(discardFeature(fc)) {
                continue;
            }
            if(heap.size() < numFeatures) {
                heap.offer(fc);
            } else {
                FeatureCluster top = heap.peek();
                if(fc.compareTo(top) > 0) {
                    heap.poll();
                    heap.offer(fc);
                }
            }
        }
        return new FeatureClusterSet(heap);
    }
    
    private boolean discardFeature(FeatureCluster fc) {
        if(stopWords != null) {
            for(Feature f : fc) {
                if(stopWords.isStop(f.getName())) {
                    return true;
                }
            }
            if(stopWords.isStop(fc.getName())) {
                return true;
            }
        }
        String n = fc.getName();
        
        if(n.length() <= 3) {
            return true;
        }
        
        for(int i = 0; i < n.length(); i++) {
            if(Character.isDigit(n.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setStopWords(StopWords stopWords) {
        this.stopWords = stopWords;
    }

}
