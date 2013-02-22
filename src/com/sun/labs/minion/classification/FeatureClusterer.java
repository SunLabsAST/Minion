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
import com.sun.labs.minion.retrieval.ResultSetImpl;

/**
 * The Feature Clusterer provides the interface to create clusters of
 * features.  The clusters themselves will in turn be
 * features.  Features that fall into the same cluster
 * will have their weights (or components thereof) summed.
 *
 * @author Jeff Alexander
 */

public interface FeatureClusterer {
    /**
     * A non-static factory method to create a feature clusterer
     *
     * @return a feature clusterer instance
     */
    public FeatureClusterer newInstance();
    
    /**
     * A non-static factory method to create a feature cluster
     *
     * @return a feature cluster instance
     */
    public FeatureCluster newCluster();
    
    /**
     * A non-static factory method to create a feature of the type used
     * by this clusterer
     *
     * @return a feature instance
     */
    public Feature newFeature();
    
    /**
     * Sets the field from which features should be drawn.
     *
     * @param field the name of a vectored field upon which the clustering
     * should be based.  A value of <code>null</code> indicates that all vectored
     * fields should be considered, while an empty string indicates that data
     * in no explicit field should be considered.
     *
     *
     */
    public void setField(String field);
    
    /**
     * Creates a set of clusters based on all of the terms in
     * the documents contained in the ResultSet.
     *
     * @param s the set of documents from which features are gathered
     * @return a set of clusters (Features) of features
     */
    public FeatureClusterSet cluster(ResultSetImpl s);
}
