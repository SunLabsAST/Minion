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

import java.util.Comparator;

/**
 * A comparator for weighted features that compares features based on their
 * weight.
 */
public class ClusterWeightComparator implements Comparator<FeatureCluster> {

    protected boolean ascending;

    protected boolean dups;
    
    public ClusterWeightComparator() {
        this(true);
    }

    public ClusterWeightComparator(boolean ascending) {
        this.ascending = ascending;
    }

    public ClusterWeightComparator(boolean ascending, boolean dups) {
        this.ascending = ascending;
        this.dups = dups;
    }
    
    /**
     * Tests whether this comparator is equal to another.
     *
     * @param o another comparator.
     */
    public boolean equals(Object o) {
        return o.getClass() == getClass();
    }

    /**
     * Compares two weighted features by their weights.
     *
     * @param fc1 the first feature cluster
     * @param fc2 the second feature cluster
     * @return a negative integer, zero, or a positive integer as the first
     * argument is less than, equal to, or greater than the second.
     */
    public int compare(FeatureCluster fc1, FeatureCluster fc2) {
        float w1 = fc1.getWeight();
        float w2 = fc2.getWeight();
        if(w1 < w2) {
            return ascending ? -1 : 1;
        }

        if(w1 > w2) {
            return ascending ? 1 : -1;
        }

        if (dups) {
            return 1;
        }
        
        return 0;
    }
    
} // WeightedClusterComparator
