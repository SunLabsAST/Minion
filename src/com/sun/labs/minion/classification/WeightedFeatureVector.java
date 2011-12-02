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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import com.sun.labs.minion.util.Util;
import java.util.logging.Logger;

/**
 * A class for holding a weighted feature vector.  Such a vector may hold
 * features drawn from a single index partition or features drawn from a
 * number of index partitions.  It is up to the user of the class to keep
 * track of how each vector is used!
 */
public class WeightedFeatureVector {

    /**
     * An array to hold the features clusters that make up our vector.
     * This array must be ordered by cluster name!
     */
    protected FeatureCluster[] v;

    /**
     * The number of features in our vector.
     */
    protected int nFeat;

    /**
     * Whether we've been normalized.
     */
    protected boolean normalized;

    static Logger logger = Logger.getLogger(WeightedFeatureVector.class.getName());

    protected static String logTag = "WFV";

    /**
     * Creates a feature vector from a collection of feature clusters.
     * @param c a collection of feature clusters from which we should make 
     * a feature vector.
     */
    public WeightedFeatureVector(Collection<FeatureCluster> c) {
        v = new FeatureCluster[c.size()];
        for(FeatureCluster fc : c) {
            v[nFeat++] = fc.copy();
        }
    } // WeightedFeatureVector constructor

    /**
     * Creates a feature vector from a collection of feature clusters and some
     * associated weights
     * @param fcs a collection of feature clusters
     * @param weights weights associated with the clusters
     */
    public WeightedFeatureVector(FeatureClusterSet fcs, float[] weights) {
        v = new FeatureCluster[fcs.size()];
        for(FeatureCluster fc : fcs) {
            v[nFeat] = fc.copy();
            v[nFeat].setWeight(weights[nFeat]);
            nFeat++;
        }
    } // WeightedFeatureVector constructor

    /**
     * Creates a feature vector that can hold a certain number of
     * features.
     *
     * @param nFeat the initial number of features
     */
    public WeightedFeatureVector(int nFeat) {
        v = new FeatureCluster[nFeat];
    }

    /**
     * Creates a feature vector that's a copy of the given vector.
     * @param fv the vector that we want to copy
     */
    public WeightedFeatureVector(WeightedFeatureVector fv) {
        v = new FeatureCluster[fv.nFeat];
        nFeat = fv.nFeat;
        for(int i = 0; i < fv.nFeat; i++) {
            v[i] = fv.v[i].copy();
        }
    }

    /**
     * Gets the size of this vector, which is the number of features it
     * contains.
     *
     * @return the number of features in this vector
     */
    public int size() {
        return nFeat;
    }

    /**
     * Adds a feature to this vector.  This method assumes that the feature
     * has not already been added, so be careful to only add a feature
     * once!  Also, features should be added in name order (which should be
     * the same as ID order!)
     *
     * @param f the feature cluster
     */
    public void add(FeatureCluster f) {

        //
        // Make sure we don't get too big!
        if(nFeat + 1 >= v.length) {
            FeatureCluster[] temp = new FeatureCluster[v.length + 256];
            System.arraycopy(v, 0, temp, 0, v.length);
            v = temp;
        }

        v[nFeat++] = f;
    }

    public FeatureCluster getCluster(String name) {
        for(int i = 0; i < v.length; i++) {
            if(v[i] == null) {
                return null;
            }
            if(v[i].getName().equals(name)) {
                return v[i];
            }
        }
        return null;
    }

    /**
     * Adds a feature vector to this vector, returning a new vector.
     *
     * @param fv the vector to add to this one.
     * @param fac1 a factor to apply to the weights in this vector
     * @param fac2 a factor to apply to the weights in the other vector
     * @param dropNegative if <code>true</code> features with a negative
     * weight are left out of the resulting vector
     * @return the vector representing the sum of the two vectors
     */
    public WeightedFeatureVector add(WeightedFeatureVector fv,
            float fac1, float fac2,
            boolean dropNegative) {
        //
        // Make the return vector with a good guess at the number of
        // features.
        WeightedFeatureVector ret =
                new WeightedFeatureVector(Math.max(nFeat, fv.nFeat));
        int i1 = 0;
        int i2 = 0;
        while(i1 < nFeat && i2 < fv.nFeat) {
            FeatureCluster f1 = v[i1];
            FeatureCluster f2 = fv.v[i2];

            int cmp = f1.getName().compareTo(f2.getName());

            if(cmp == 0) {
                float w = fac1 * f1.getWeight() +
                        fac2 * f2.getWeight();
                if(w > 0 || !dropNegative) {
                    // FIXME: here and below, can we always assume that clusters
                    // FIXME: of the same name have the same contents?
                    FeatureCluster c = f1.copy();
                    c.setWeight(w);
                    ret.add(c);
                }
                i1++;
                i2++;
            } else if(cmp < 0) {
                float w = fac1 * f1.getWeight();
                if(w > 0 || !dropNegative) {
                    FeatureCluster c = f1.copy();
                    c.setWeight(w);
                    ret.add(c);
                }
                i1++;
            } else {
                float w = fac2 * f2.getWeight();
                if(w > 0 || !dropNegative) {
                    FeatureCluster c = f2.copy();
                    c.setWeight(w);
                    ret.add(c);
                }
                i2++;
            }
        }

        while(i1 < nFeat) {
            FeatureCluster f1 = v[i1++];
            float w = fac1 * f1.getWeight();
            if(w > 0 || !dropNegative) {
                FeatureCluster c = f1.copy();
                c.setWeight(w);
                ret.add(c);
            }
        }

        while(i2 < fv.nFeat) {
            FeatureCluster f2 = fv.v[i2++];
            float w = fac2 * f2.getWeight();
            if(w > 0 || !dropNegative) {
                FeatureCluster c = f2.copy();
                c.setWeight(w);
                ret.add(c);
            }
        }

        return ret;
    }

    /**
     * Adds a feature vector to this vector, returning a new vector.
     *
     * @param fv the vector to add to this one.
     * @return a vector representing the sum of this vector and the 
     * provided vector
     */
    public WeightedFeatureVector add(WeightedFeatureVector fv) {
        return add(fv, 1, 1, false);
    }

    /**
     * Subtracts a feature vector from this vector, returning a new vector.
     *
     * @param fv the vector to subtract from this one.
     * @param fac1 a factor to apply to the weights in this vector
     * @param fac2 a factor to apply to the weights in the other vector
     * @param dropNegative if <code>true</code> features with a negative
     * weight are left out of the resulting vector
     * @return a vector representing the sum of this vector and the 
     * provided vector
     */
    public WeightedFeatureVector sub(WeightedFeatureVector fv,
            float fac1, float fac2,
            boolean dropNegative) {
        return add(fv, fac1, -fac2, dropNegative);
    }

    /**
     * Subtracts a feature vector from this vector, returning a new vector.
     *
     * @param fv the vector to subtract from this one.
     * @return a vector representing the difference of this vector and the 
     * provided vector
     */
    public WeightedFeatureVector sub(WeightedFeatureVector fv) {
        return sub(fv, 1, 1, false);
    }

    /**
     * Subtracts a feature vector from this vector, returning a new vector.
     *
     * @param fv the vector to subtract from this one.
     * @param dropNegative if <code>true</code> features with a negative
     * weight are left out of the resulting vector
     * @return a vector representing the difference of this vector and the 
     * provided vector
     */
    public WeightedFeatureVector sub(WeightedFeatureVector fv,
            boolean dropNegative) {
        return sub(fv, 1, 1, dropNegative);
    }

    /**
     * Multiplies a feature vector by a scalar, producing a new vector.
     * @param s a scalar that we want to multiply the weights by
     * @return a vector representing the current vector multiplied by the provided
     * scalar
     */
    public WeightedFeatureVector mult(float s) {
        WeightedFeatureVector ret = new WeightedFeatureVector(nFeat);
        for(int i = 0; i < nFeat; i++) {
            ret.v[i] = v[i].copy();
            ret.v[i].setWeight(v[i].getWeight() * s);
            ret.nFeat = nFeat;
        }
        return ret;
    }

    /**
     * Calculate the dot product of this feature vector and another feature
     * vector.
     *
     * @param fv another weighted feature vector
     * @return the dot product of the two vectors (i.e. the sum of the
     * products of the components in each dimension)
     */
    public float dot(WeightedFeatureVector fv) {
        float res = 0;
        int i1 = 0;
        int i2 = 0;
        int x = 0;
        while(i1 < nFeat && i2 < fv.nFeat) {
            FeatureCluster f1 = v[i1];
            FeatureCluster f2 = fv.v[i2];

            int cmp = f1.getName().compareTo(f2.getName());

            if(cmp == 0) {

                //
                // The cluster names are the same, so we'll have some
                // non-zero value to add for this cluster's dimension
                res += f1.getWeight() * f2.getWeight();
                i1++;
                i2++;
            } else if(cmp < 0) {
                //
                // fv is zero in this dimension
                i1++;
            } else {
                //
                // v is zero in this dimension
                i2++;
            }
        }
        return res;
    }

    /**
     * Gets an array of weighted feature from this WeightedFeatureVector.
     * The WeightedFeatureVector is flattened -- that is, the resulting
     * array will have an entry for each feature in each cluster where
     * the weight of the feature is the weight of the cluster from which
     * it came.  The array is in order by WeightedFeature.
     *
     * @return an array of the weighted features from this vector of clusters
     */
    public WeightedFeature[] getWeightedFeatures() {
        //
        // take all the features out of clusters and assign them the
        // weight of the cluster, then put them into an array that
        // can easily be compared to a doc vector
        ArrayList<WeightedFeature> features = new ArrayList<WeightedFeature>();
        for(int i = 0; i < nFeat; i++) {
            for(Iterator fit = v[i].getContents().iterator(); fit.hasNext();) {
                WeightedFeature f = (WeightedFeature) fit.next();
                f.setWeight(v[i].getWeight());
                features.add(f);
            }
        }
        //
        // Sort the results and return them.
        WeightedFeature[] ret = features.toArray(new com.sun.labs.minion.classification.WeightedFeature[features.
                size()]);
        Util.sort(ret);
        return ret;
    }

    /**
     * Normalizes the length of this vector to 1.
     */
    public void normalize() {
        if(normalized) {
            return;
        }
        float l = length();
        for(int i = 0; i < nFeat; i++) {
            v[i].setWeight(v[i].getWeight() / l);
        }
        normalized = true;
    }

    /**
     * Gets the euclidean length of this vector.
     * @return the length of this vector
     */
    public float length() {
        if(normalized) {
            return 1;
        }
        float l = 0;
        for(int i = 0; i < nFeat; i++) {
            l += v[i].getWeight() * v[i].getWeight();
        }
        return (float) Math.sqrt(l);
    }

    /**
     * Gets a sorted set of features.
     *
     * @return a set of the features in this vector, sorted by name
     */
    public FeatureClusterSet getSet() {
        FeatureClusterSet ret = new FeatureClusterSet();
        for(int i = 0; i < nFeat; i++) {
            ret.add(v[i]);
        }
        return ret;
    }

    /**
     * Given a number of partition-specific feature vectors, generate a
     * new, cross-partition feature vector.  The resulting vector is
     * a vector of feature clusters, where the features in the input
     * vectors have been combined back into the clusters they originally
     * came from and the cluster weights are the sums of the weights of
     * the component features from the vectors.
     *
     * <p>
     *
     * Note that this could be done using straightforward addition, but
     * this will be more efficient in space and time.
     *
     * @param vecs a list of weighted feature vectors from a number of
     * partitions
     * @param clusters the clusters that we want to make cross partition
     * @return a cross-partition feature vector
     */
    public static WeightedFeatureVector getCrossPartition(List vecs,
            FeatureClusterSet clusters) {

        PriorityQueue<FeatureCluster> h = new PriorityQueue<FeatureCluster>();
        int max = Integer.MIN_VALUE;

        //
        // For each vector, get the clusters and put them on the heap.
        for(Iterator i = vecs.iterator(); i.hasNext();) {
            WeightedFeatureVector wfv = (WeightedFeatureVector) i.next();

            max = Math.max(wfv.nFeat, max);

            //
            // For each element of the group, make a weighted feature and
            // put it on the heap for later combination.
            for(int j = 0; j < wfv.nFeat; j++) {
                h.offer(wfv.v[j]);
            }
        }


        WeightedFeatureVector ret = new WeightedFeatureVector(max);

        //
        // Empty the heap, collecting up the clusters that share a name,
        // combining their weights.
        while(h.size() > 0) {
            FeatureCluster curr = h.poll().copy();
            FeatureCluster top = h.peek();
            while(top != null && top.getName().equals(curr.getName())) {
                h.poll();
                // FIXME: I think this should merge clusters, not just add weights
                curr.setWeight(curr.getWeight() + top.getWeight());
                ;
                top = h.peek();
            }

            //
            // curr is now a unique (in the set of WFVs passed in) feature cluster.
            // it can be added to the output set.
            ret.add(curr);

        }

        return ret;
    }

    public String toString() {
        return toString(0);
    }

    public String toString(int t) {
        StringBuffer b = new StringBuffer();
        for(int i = 0; i < nFeat; i++) {
            if(i > 0) {
                if(t == 0) {
                    b.append('\n');
                } else {
                    b.append(", ");
                }
            }
            if(t != 0) {
                b.append('(');
            }
            b.append(String.format("%s %5.3f", v[i].getName(), v[i].getWeight()));
            if(t != 0) {
                b.append(')');
            }
        }
        return b.toString();
    }
} // WeightedFeatureVector
