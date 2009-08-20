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
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.cache.DocCache;
import com.sun.labs.minion.retrieval.cache.DocCacheElement;
import java.util.logging.Logger;

/**
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class SimpleClusterer implements FeatureClusterer {

    private String field;

    private DocCache dc;

    static Logger logger = Logger.getLogger(SimpleClusterer.class.getName());

    private static String logTag = "SIMPC";

    /**
     * Creates a SimpleClusterer
     */
    public SimpleClusterer() {
    }

    public FeatureClusterer newInstance() {
        return new SimpleClusterer();
    }

    public FeatureCluster newCluster() {
        return new SimpleFeatureCluster();
    }

    public Feature newFeature() {
        return new WeightedFeature();
    }

    public void setField(String field) {
        this.field = field;
    }

    public void setDocCache(DocCache dc) {
        this.dc = dc;
    }

    /**
     * Clusters features with the same name, adding the weights.
     */
    public FeatureClusterSet cluster(ResultSetImpl s) {
        Map<String, FeatureCluster> cls = new HashMap<String, FeatureCluster>();
        for(Iterator rsi = s.resultsIterator(); rsi.hasNext();) {
            ArrayGroup ag = (ArrayGroup) rsi.next();
            Set<WeightedFeature> features = collectFeatures(ag);
            for(WeightedFeature wf : features) {
                SimpleFeatureCluster sfc = (SimpleFeatureCluster) cls.get(wf.
                        getName());
                if(sfc == null) {
                    cls.put(wf.getName(), new SimpleFeatureCluster(wf));
                } else {
                    sfc.addWeight(wf.getWeight());
                }
            }
        }
        return new FeatureClusterSet(cls.values());
    }

    /**
     * Collects terms from the array group, creating contingency features
     * for each one.
     *
     * @param ag the array group
     * @return a set of contingency features
     */
    protected Set<WeightedFeature> collectFeatures(ArrayGroup ag) {

        DiskPartition part = ag.getPartition();
        WeightedFeature[] ret = new WeightedFeature[128];
        Map<Integer, String> tm = new HashMap<Integer, String>();

        //
        // For each document ID that we encounter, we'll pull the
        // document term and then iterate through the terms in that,
        // document.
        for(ArrayGroup.DocIterator i = ag.iterator(); i.next();) {
            DocKeyEntry e = (DocKeyEntry) part.getDocumentTerm(i.getDoc());
            if(e == null) {
                logger.warning("No document term for " +
                        part + " " + i.getDoc());
                continue;
            }

            DocCacheElement dce = dc.get(e, field, part);

            //
            // Make sure we've got enough room for these features.
            WeightedFeature[] v = dce.getFeatures();

            //
            // We might have an empty vector!
            if(v.length == 0) {
                continue;
            }

            if(v[v.length - 1].getID() >= ret.length) {
                WeightedFeature[] temp = new WeightedFeature[v[v.length - 1].
                        getID() + 1];
                System.arraycopy(ret, 0, temp, 0, ret.length);
                ret = temp;
            }

            //
            // Add the weights from the features in this doc to the features in
            // the array.
            for(WeightedFeature wf : v) {

                int id = wf.getID();

                if(ret[id] == null) {
                    ret[id] = new WeightedFeature(wf);
                } else {
                    ret[id].setWeight(ret[id].getWeight() + wf.getWeight());
                }
            }
        }

        //
        // Now, for each feature, check to see whether we want to ignore it.  If
        // not, go ahead and add it to our feature cluster set.
        HashSet<WeightedFeature> cfset = new HashSet<WeightedFeature>();

        for(int i = 0; i < ret.length; i++) {
            if(ret[i] != null && !ret[i].getName().matches(".*\\d.*")) {
                cfset.add(ret[i]);
            }
        }

        return cfset;
    }
}
