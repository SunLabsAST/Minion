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

package com.sun.labs.minion.clustering;

import com.sun.labs.minion.retrieval.ResultImpl;
import com.sun.labs.minion.util.StopWatch;

/**
 * A K means clustering algorithm for search results.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class KMeans extends AbstractClusterer {
    
    public static String logTag = "KM";
    
    public KMeans() {
        super();
    }
    
    /**
     * Cluster the results.
     */
    public void cluster() {
        StopWatch sw = new StopWatch();
        sw.start();
        clusters = new double[k][];
        //
        // Generate k random points in N space to start.
        for(int i = 0; i < clusters.length; i++) {
            clusters[i] = ClusterUtil.normalizedRandomPoint(N);
        }
        
        //
        // We'll foolishly keep going forever-ish.
        int nIter = 0;
        while(true) {
            
            int nChanges = assignToClusters();
            
            //
            // We'll quite if we didn't reassign any of the docs and we've not
            // been running for too long.
            if(nChanges == 0 && nIter < 1000) {
                break;
            }
            
            recomputeClusters();
            nIter++;
        }
        sw.stop();
        logger.info("clustering took: " + sw.getTime());
    }
    
    private int assignToClusters() {
        int nChanges = 0;
        for(int i = 0; i < els.length; i++) {
            double min = Double.MAX_VALUE;
            int mp = 0;
            for(int j = 0; j < clusters.length; j++) {
                double dist = ClusterUtil.euclideanDistance(els[i].point, clusters[j]);
                if(dist < min) {
                    min = dist;
                    mp = j;
                }
            }
            if(els[i].member != mp) {
                els[i].member = mp;
                ((ResultImpl) els[i].r).setScore((float) min);
                nChanges++;
            }
        }
        return nChanges;
    }
    
    /**
     * Recomputes the cluster centroids.
     */
    private void recomputeClusters() {
        
        //
        // Compute the cluster centroids.
        clusters = new double[k][N];
        int[] s = new int[k];
        for(int i = 0; i < els.length; i++) {
            ClusterUtil.add(clusters[els[i].member],
                    els[i].point);
            s[els[i].member]++;
        }
        for(int i = 0; i < clusters.length; i++) {
            ClusterUtil.div(clusters[i], s[i]);
        }
        
        //
        // Find the point closest to the centroid and use that.
        int[] closest = new int[k];
        double[] cd = new double[k];
        for(int i = 0; i < cd.length; i++) {
            cd[i] = Double.MAX_VALUE;
        }
        for(int i = 0; i < els.length; i++) {
            int c = els[i].member;
            double dist = ClusterUtil.euclideanDistance(els[i].point, clusters[c]);
            if(dist < cd[c]) {
                cd[c] = dist;
                closest[c] = i;
            }
        }
        for(int i = 0; i < clusters.length; i++) {
            clusters[i] = els[closest[i]].point;
        }
    }       
 }
