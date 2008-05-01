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

import com.sun.labs.minion.ClusterStatistics;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of the cluster statistics interface.
 * @author Stephen Green <stephen.green@sun.com>
 */
public class ClusterStatisticsImpl implements ClusterStatistics {
    
    private List<Double> v;
    
    private boolean meanComputed;
    
    private double mean;
    
    private boolean varianceComputed;
    
    private double variance;
    
    public ClusterStatisticsImpl() {
        v = new ArrayList<Double>();
    }
    
    public void add(double d) {
        v.add(d);
        meanComputed = false;
        varianceComputed = false;
    }
    
    public int size() {
        return v.size();
    }
    /**
     * Calculates the mean distance of the cluster elements from the cluster
     * centroid.
     */
    public double mean() {
        if(!meanComputed) {
            double sum = 0;
            for(Double d : v) {
                sum += d;
            }
            
            mean = sum / v.size();
        }
        return mean;
    }
    
    /**
     * Calculates the variance of the distance of the cluster elements from the
     * cluster centroid.  This implementation calculates the bias-corrected
     * sample variance.
     */
    public double variance() {
        if(!varianceComputed) {
            double m = mean();
            double ss = 0;
            for(Double d : v) {
                double diff = d - m;
                ss += (diff * diff);
            }
            variance = ss / (v.size() - 1);
        }
        return variance;
    }
    
    public String toString() {
        return String.format("%d %.3f %.3f", size(), mean(), variance());
    }
}
