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
 * ContingencyFeatureCluster.java
 *
 * Created by: Jeff Alexander (ja151348)
 * Created on: Mon Aug 22, 2005	 3:58:45 PM
 * Desc: 
 *
 */

import java.util.Set;
import java.util.SortedSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.labs.minion.util.MinionLog;

/**
 * A cluster of contingency features
 */

public class ContingencyFeatureCluster implements FeatureCluster
{

    /** 
     * A set to hold the contents of this cluster 
     */
    protected List<Feature> contents = new ArrayList<Feature>();

    /** 
     * The name of this cluster 
     */
    protected String name;

    protected static MinionLog log = MinionLog.getLog();

    protected static String logTag = "CFC";

    
    /** 
     * A counter for adding up the a,b,c,d values and 
     * calculating the final weight
     */
    protected ContingencyFeature counter;

    //
    // Used only to speed up lookups, not in any structural
    // kind of way.
    protected Map<Integer,Feature> quickIDMap = new HashMap<Integer,Feature>();
    protected Map<String,Feature> quickNameMap = new HashMap<String,Feature>();

    //
    // The IDs of the docs that have features that are in this cluster
    protected Set<Integer> docIDs;
    
    public ContingencyFeatureCluster() {
        this.counter = new ContingencyFeature();
    }
    
    public ContingencyFeatureCluster(String name) {
        this.name = name;
        this.counter = new ContingencyFeature();
        counter.name = name;
    }

    /** 
     * Create a new feature cluster that is a copy of the given feature
     * cluster.
     * 
     */
    public FeatureCluster copy() {
        ContingencyFeatureCluster ret =
            new ContingencyFeatureCluster(this.name);

        for (Feature f : contents) {
            ret.innerAdd(new ContingencyFeature((ContingencyFeature) f));
        }

        ret.counter = new ContingencyFeature(counter);
        if (docIDs != null) {
            ret.docIDs = new HashSet<Integer>(docIDs);
        }
        return ret;
    }
    
    /** 
     * Add another feature to this cluster, incrementing the counts associated
     * with it.
     * 
     * @param f the feature to add
     */
    public void add(Feature f) {
        ContingencyFeature cf = (ContingencyFeature)f;
        //
        // Add the feature to the list of contents, and add the values
        // to this cluster.  Don't allow duplicates in the clusters
        //counter.sum(cf);
        if (cf.getDocs() != null) {
            if (docIDs == null) {
                docIDs = new HashSet<Integer>();
            }
            docIDs.addAll(cf.getDocs());
            cf.wipeDocs();
            counter.a = docIDs.size();
        } else {
            counter.a += cf.a;
        }
        int pos = contents.indexOf(cf);
        if (pos >= 0) {
            ContingencyFeature existing = (ContingencyFeature)contents.get(pos);
            existing.sum(cf);
        } else {
            innerAdd(cf);
        }
    }

    protected void innerAdd(Feature f) {
        quickIDMap.put(f.getID(), f);
        quickNameMap.put(f.getName(), f);
        contents.add(f);
    }
    
    public void merge(FeatureCluster other) {
        ContingencyFeatureCluster cfc = (ContingencyFeatureCluster)other;

        //
        // Get the contents of the other cluster, and combine with this one
        for(Feature f : cfc) {
            add((ContingencyFeature) f);
        }
        counter.sum(cfc.counter);
    }
    
    /** 
     * Gets the contents of the feature cluster. 
     * 
     * @return the features in the cluster
     */
    public SortedSet<Feature> getContents() {
        return new TreeSet<Feature>(contents);
    };


    //
    // Javadoc is inherited from interface FeatureCluster
    public Feature get(String name) {
        return quickNameMap.get(name);
    }

    //
    // Javadoc is inherited from interface FeatureCluster
    public Feature get(int id) {
        return quickIDMap.get(new Integer(id));
    }

    /**
     * Gets an iterator for the contents of this cluster.
     *
     * @return an iterator for the contents of this cluster.
     */
    public Iterator<Feature> iterator() {
        return contents.iterator();
    }
    
    /** 
     * Compares to feature clusters on the basis of their names 
     */
    public int compareTo(FeatureCluster fc) {
        return name.compareTo(fc.getName());
    }

    /** 
     * Gets the name of this feature cluster.
     * 
     * @return the name of the cluster
     */
    public String getName() {
        return name;
    }
    
    public String getHumanReadableName() {
        String hr = null;
        for(Feature f : contents) {
            if(hr == null || f.getName().length() < hr.length()) {
                hr = f.getName();
            }
        }
        return hr;
    }
    
    /** 
     * Sets the name of this feature cluster.
     * 
     * @param name the new name
     */
    public void setName(String name) {
        this.name = name;
        counter.name = name;
    }
    
    /** 
     * Gets the weight of this feature cluster.
     * 
     * @return the weight
     */
    public float getWeight() {
        return counter.getWeight();
    }
    
    public ContingencyFeature getCounter() {
        return counter;
    }

    /** 
     * Sets the weight of this feature cluster.
     * 
     * @param w the new weight
     */
    public void setWeight(float w) {
        counter.setWeight(w);
    }

    public String toString() {
        List<String> cn = new ArrayList<String>();
        for(Object o : contents) {
            cn.add(((ContingencyFeature) o).getName());
        }
        Collections.sort(cn);
        return String.format("%-15s %d a: %4d b: %4d c: %4d N: %4d weight: %7.3f", 
                getName(), counter.type, counter.a, counter.b, counter.c, counter.N,
                getWeight()) + " contents: " +
                cn.toString();
    }

    public int size() {
        return contents.size();
    }
}
