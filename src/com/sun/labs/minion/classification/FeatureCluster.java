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

/*
 * FeatureCluster.java
 *
 */

import com.sun.labs.minion.Feature;
import java.util.Comparator;
import java.util.Iterator;
import java.util.SortedSet;

/**
 * A cluster of features
 */
public interface FeatureCluster extends Comparable<FeatureCluster>, Iterable<Feature> {

    /**
     * Gets the name of this feature cluster.
     *
     * @return the name
     */
    public String getName();
    
    /**
     * Gets a human-readable (for example, non-stemmed) name for this cluster.
     */
    public String getHumanReadableName();

    /**
     * Sets the name of this feature cluster.
     *
     * @param name the name
     */
    public void setName(String name);

    /**
     * Returns a set of features that are the contents of this cluster
     */
    public SortedSet getContents();

    /**
     * Gets the weight associated with this feature cluster
     */
    public float getWeight();

    /**
     * Sets the weight associated with this feature cluster
     *
     * @param weight the new weight
     */
    public void setWeight(float weight);

    /**
     * Gets the feature with the given name out of this cluster.
     *
     * @param name the feature name
     * @return the feature with the given name
     */
    public Feature get(String name);

    /**
     * Gets the feature with the given id out of this cluster. This
     * method assumes that only one feature will have the given id.
     *
     * @param id the feature id
     * @return the feature with the given id
     */
    public Feature get(int id);

    /**
     * Gets an iterator for the contents of this cluster.
     *
     * @return an iterator for the contents of this cluster.
     */
    @Override
    public Iterator<Feature> iterator();

    /**
     * Adds a feature to this feature cluster
     *
     * @param f the feature to add
     */
    public void add(Feature f);

    /**
     * Return a copy of this feature cluster
     */
    public FeatureCluster copy();

    /**
     * Merge another feature cluster into this one, preventing
     * duplicates.
     *
     * @param other the other cluster
     */
    public void merge(FeatureCluster other);

    public int size();
    
    /**
     * A weight comparator for feature clusters.
     */
    public static Comparator<FeatureCluster> weightComparator =
            new Comparator<FeatureCluster>() {

        @Override
                public int compare(FeatureCluster o1,
                        FeatureCluster o2) {
                    if(o1.getWeight() < o2.getWeight()) {
                        return -1;
                    }

                    if(o1.getWeight() > o2.getWeight()) {
                        return 1;
                    }
                    return 0;
                }

        @Override
                public boolean equals(Object o) {
                    return false;
                }
            };
}
