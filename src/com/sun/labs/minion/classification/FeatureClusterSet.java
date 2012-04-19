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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A set of feature clusters.  The set is sorted based on cluster
 * name.
 */
public class FeatureClusterSet implements Iterable<FeatureCluster> {

    protected HumanSelected hs;
    protected SortedSet<FeatureCluster> contents;
    //
    // Used only to speed up lookups, not in any structural
    // kind of way.
    protected Map<String,FeatureCluster> quickMap = new HashMap<String,FeatureCluster>();

    public FeatureClusterSet() {
        contents = new TreeSet<FeatureCluster>();
    }

    /**
     * Creates a FeatureClusterSet from the contents of a Collection.
     * An exception is thrown if any member of clusters is not a
     * FeatureCluster.
     *
     * @param clusters a collection of FeatureClusters
     */
    public FeatureClusterSet(Collection<FeatureCluster> clusters) {
        contents = new TreeSet<FeatureCluster>(clusters);
    }

    /**
     * Create a copy of the provided FeatureClusterSet
     * @param other the set to copy
     */
    public FeatureClusterSet(FeatureClusterSet other) {
        contents = new TreeSet<FeatureCluster>();
        for (FeatureCluster fc : other) {
            add(fc.copy());
        }
    }

    public void setHumanSelected(HumanSelected hs) {
        this.hs = hs;
    }

    /**
     * Gets the feature cluster with the given name.
     *
     * @param name the name of the cluster to get
     * @return the cluster, or null if no such cluster is in the set
     */
    public FeatureCluster get(String name) {
        return quickMap.get(name);
    }

    /**
     * Gets the feature cluster that contains the given feature.
     *
     * @param feature the feature to find in a cluster
     * @return the cluster containing f or null if no cluster contains f
     */
    public FeatureCluster getContaining(Feature feature) {
        return getContaining(feature.getName());
    }

    /**
     * Gets the feature cluster that contains the given feature.
     *
     * @param name the name of the feature to find in a cluster
     * @return the cluster containing f or null if no cluster contains f
     */
    public FeatureCluster getContaining(String name) {
        for (Iterator it = contents.iterator(); it.hasNext();) {
            FeatureCluster curr = (FeatureCluster) it.next();
            Feature f = curr.get(name);
            if (f != null) {
                return curr;
            }
        }
        return null;
    }

    /**
     * Merge the contents of all the cluster sets.
     * Clusters with the same name are merged.
     *
     * @param clusterSets other the other FCS
     */
    public static FeatureClusterSet merge(Collection<FeatureClusterSet> clusterSets) {
        //
        // Like other merges, throw all the clusters in the cluster sets
        // on a heap, then combine the ones with the same name.
        PriorityQueue<FeatureCluster> heap = new PriorityQueue<FeatureCluster>();

        for (FeatureClusterSet fcs : clusterSets) {
            for (FeatureCluster fc : fcs) {
                heap.offer(fc);
            }
        }

        //
        // Take the clusters off the heap (now in alphabetical order)
        // and merge 'em into the result set.
        FeatureClusterSet results = new FeatureClusterSet();

        while (heap.size() > 0) {
            FeatureCluster curr = heap.poll();
            FeatureCluster top = heap.peek();
            FeatureCluster merged = curr.copy();
            while (top != null && top.getName().equals(curr.getName())) {
                heap.poll();
                merged.merge(top);
                top = heap.peek();
            }
            results.add(merged);
        }
        return results;
    }

    /**
     * Adds a feature cluster to this set.
     *
     * @param cluster the cluster to add
     */
    public void add(FeatureCluster cluster) {
        quickMap.put(cluster.getName(), cluster);
        contents.add(cluster);
    }

    /**
     * Removes feature clusters with zero weight from the set.
     */
    public void removeZero() {
        for (Iterator<FeatureCluster> i = contents.iterator(); i.hasNext();) {
            FeatureCluster c = i.next();
            if (c.getWeight() == 0) {
                i.remove();
            }
        }
    }

    /**
     * Normalizes the weights of the feature clusters so that the overall
     * set has a unit length.
     */
    public void normalize() {
        float l = 0;
        for (FeatureCluster c : contents) {
            l += (c.getWeight() * c.getWeight());
        }
        l = (float) Math.sqrt(l);
        for (FeatureCluster c : contents) {
            c.setWeight(c.getWeight() / l);
        }
    }

    /**
     * Gets an iterator over the cluster set.  
     *
     * @return an iterator over FeatureCluster
     */
    @Override
    public Iterator<FeatureCluster> iterator() {
        return contents.iterator();
    }

    /**
     * Gets the number of clusters in this cluster set
     *
     * @return the size of the cluster set
     */
    public int size() {
        return contents.size();
    }

    /**
     * Gets the contents of this cluster as a SortedSet
     *
     * @return the FeatureClusters in this set
     */
    public SortedSet<FeatureCluster> getContents() {
        return contents;
    }

    public FeatureCluster[] toArray() {
        return contents.toArray(new FeatureCluster[0]);
    }

    /**
     * Gets a subset of this feature cluster set consisting of the first
     * N features in the set.  If this is a set that just came out of
     * feature selection, then this is the top weighted clusters.  If this
     * is a set straight from clustering, it is alphabetical.
     *
     * @param n the number of features to use in the set
     */
    public FeatureClusterSet subsetFirstN(int n) {
        FeatureClusterSet fcs = new FeatureClusterSet();
        Iterator it = contents.iterator();
        for (int i = 0; it.hasNext();
                i++) {
            FeatureCluster fc = (FeatureCluster) it.next();
            if (i >= n && hs != null) {
                //
                // If we're past the number we were given and there are human
                // selected terms, then we need to check for human selected terms
                // in the remainder of the features of this set.
                for (Iterator j = fc.iterator(); j.hasNext();) {
                    if (hs.include(((Feature) j.next()).getName())) {
                        fcs.add(fc);
                        continue;
                    }
                }
            } else {
                fcs.add(fc);
            }
        }
        return fcs;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (FeatureCluster fc : contents) {
            sb.append(fc + "\n");
        }
        return sb.toString();
    }
}
