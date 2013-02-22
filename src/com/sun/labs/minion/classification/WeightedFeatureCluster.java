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
import com.sun.labs.minion.WeightedFeature;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 *
 */
public class WeightedFeatureCluster implements FeatureCluster {

    String name;
    
    SortedSet<Feature> contents;
    
    Map<String,WeightedFeature> quick;
    
    float weight;
    
    public WeightedFeatureCluster() {
        contents = new TreeSet<Feature>();
        quick = new HashMap<String,WeightedFeature>();
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getHumanReadableName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public SortedSet getContents() {
        return contents;
    }

    @Override
    public float getWeight() {
        return weight;
    }

    @Override
    public void setWeight(float weight) {
        this.weight = weight;
    }

    @Override
    public Feature get(String name) {
        return quick.get(name);
    }

    @Override
    public Feature get(int id) {
        return null;
    }

    @Override
    public Iterator<Feature> iterator() {
        return contents.iterator();
    }

    @Override
    public void add(Feature f) {
        contents.add(f);
        quick.put(f.getName(), (WeightedFeature) f);
        weight += ((WeightedFeature) f).getWeight();
    }

    @Override
    public FeatureCluster copy() {
        WeightedFeatureCluster wfc = new WeightedFeatureCluster();
        wfc.setName(name);
        wfc.contents = new TreeSet<Feature>(contents);
        wfc.quick = new HashMap<String,WeightedFeature>(quick);
        wfc.setWeight(weight);
        return wfc;
    }

    @Override
    public void merge(FeatureCluster other) {
    }

    @Override
    public int size() {
        return contents.size();
    }

    @Override
    public int compareTo(FeatureCluster o) {
        float ow = ((WeightedFeatureCluster) o).getWeight();
        if(weight < ow) {
            return -1;
        }
        if(weight > ow) {
            return 1;
        }
        return 0;
    }

}
