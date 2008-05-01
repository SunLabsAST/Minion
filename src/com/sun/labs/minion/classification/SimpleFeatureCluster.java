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

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;

/**
 * A feature cluster containing a single term and a weight assigned by a standard
 * term weighting funciton.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class SimpleFeatureCluster implements FeatureCluster {
  
    private WeightedFeature wf;
    
    private SortedSet<Feature> contents;
    
    public SimpleFeatureCluster() {
        contents = new TreeSet<Feature>();
    }
    
    /**
     * Creates a SimpleFeatureCluster
     */
    public SimpleFeatureCluster(String name, float weight) {
        wf = new WeightedFeature(name, weight);
        contents = new TreeSet<Feature>();
    }
    
    public SimpleFeatureCluster(WeightedFeature f) {
        wf = f;
        contents = new TreeSet<Feature>();
        contents.add(wf);
    }

    public String getName() {
        return wf.getName();
    }
    
    public String getHumanReadableName() {
        return wf.getName();
    }

    public void setName(String name) {
        wf.setName(name);
        contents.add(wf);
    }

    public SortedSet getContents() {
        return contents;
    }

    public float getWeight() {
        return wf.getWeight();
    }
    
    public void combine(WeightedFeature wf) {
        this.wf.combine(wf);
    }

    public void setWeight(float weight) {
        wf.setWeight(weight);
    }
    
    public void addWeight(float weight) {
        wf.setWeight(wf.getWeight() + weight);
    }
    
    public void computeWeight(WeightingFunction f,
            WeightingComponents wc) {
        wc.setTerm(wf.getName());
        wc.fdt = wf.getFreq();
        wf.setWeight(f.termWeight(wc));
    }
    
    public Feature get(String name) {
        return wf;
    }

    public Feature get(int id) {
        return wf;
    }

    public Iterator iterator() {
        return contents.iterator();
    }
    

    public void add(Feature f) {
        contents.add(f);
    }

    public FeatureCluster copy() {
        SimpleFeatureCluster ret = new SimpleFeatureCluster(wf.getName(), 
                wf.getWeight());
        ret.contents.addAll(contents);
        return ret;
    }

    public void merge(FeatureCluster other) {
        wf.combine(((SimpleFeatureCluster) other).wf);
        contents.addAll(other.getContents());
    }

    public int size() {
        return contents.size();
    }

    public int compareTo(FeatureCluster fc) {
        float ow = fc.getWeight();
        if(wf.getWeight() < ow) {
            return -1;
        }
        if(wf.getWeight() > ow) {
            return 1;
        } 
        return wf.getName().compareTo(fc.getName());
    }
    
    public String toString() {
        return wf.toString();
    }
}
