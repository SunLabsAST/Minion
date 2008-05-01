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
import java.util.Map;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.cache.DocCache;
import com.sun.labs.minion.util.PorterStemmer;

/**
 *
 */
public class WeightedFeatureClusterer implements FeatureClusterer {
    
    protected PorterStemmer stemmer = new PorterStemmer();

    Map<String,FeatureCluster> m;
    
    public WeightedFeatureClusterer() {
        m = new HashMap<String,FeatureCluster>();
    }

    public FeatureClusterer newInstance() {
        return new WeightedFeatureClusterer();
    }

    public FeatureCluster newCluster() {
        return new WeightedFeatureCluster();
    }

    public Feature newFeature() {
        return new WeightedFeature();
    }

    public void setField(String field) {
    }

    public void setDocCache(DocCache dc) {
    }
    
    public void add(WeightedFeature[] v) {
        for(WeightedFeature f : v) {
            String name = f.getName().toLowerCase();
            stemmer.add(name.toCharArray(), name.length());
            stemmer.stem();
            String stem = stemmer.toString();
            WeightedFeatureCluster wfc = (WeightedFeatureCluster) m.get(stem);
            if(wfc == null) {
                wfc = new WeightedFeatureCluster();
                wfc.setName(stem);
                m.put(stem, wfc);
            }
            wfc.add(f);
        }
    }
    
    public FeatureClusterSet cluster(ResultSetImpl s) {
        return new FeatureClusterSet(m.values());
    }
}
