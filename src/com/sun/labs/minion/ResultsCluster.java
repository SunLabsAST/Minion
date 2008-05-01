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

package com.sun.labs.minion;

import java.util.List;

/**
 * A cluster of results from a search engine.
 */
public interface ResultsCluster {
  
    /**
     * Gets a list of strings describing this cluster.  These strings might be 
     * words or phrases from the documents or particular saved field values.
     * The particluar clustering 
     * algorithm will decide which strings to use to describe the cluster.
     *
     * @param n the maximum number of strings to return.
     * @return a list of strings describing the contents of the cluster.
     */
    public List<String> getDescription(int n);
    
    /**
     * Get the result closest to the centroid of this cluster.
     * @return the result which is the closest to the centroid of the cluster
     */
    public Result getMostCentralResult();
    
    /**
     * Gets the results that make up this cluster.
     *
     * @return the results that make up this cluster.
     */
    public ResultSet getResults();
    
    /**
     * Gets any name that might have been assigned to this cluster during the
     * clustering process.
     *
     * @return any name assigned to this cluster, or <code>null</code> if no name 
     * was assigned.
     */
    public String getName();
    
    /**
     * Gets statistics describing the cluster.
     * @return statistics for this cluster
     */
    public ClusterStatistics getStatistics();
    
    /**
     * Determines whether the document that has the given key is a member of this
     * cluster.
     * @param key the key of the document that we want to check
     * @return <code>true</code> if the document with the given key is a member
     * of this cluster, <code>false</code> otherwise.
     */
    public boolean contains(String key);
    
    /**
     * Calculates the distance of the document with the given key from the 
     * cluster centroid, giving you an idea of how central it is to the cluster.
     *
     * @param key the key of a document for which we require the distance from
     * the cluster centroid.  This document does not need to be a member of
     * this cluster in order to do this computation.
     * @return the distance from the centroid of this cluster to the document
     * that has the given key
     */
    public double distance(String key);

    /**
     * Returns the number of items in this cluser.
     * @return the number of items in this cluster
     */
    public int size();
}
