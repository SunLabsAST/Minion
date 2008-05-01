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

import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;

import com.sun.labs.minion.indexer.partition.PartitionManager;


/**
 * The ClusterManager is a specialization of the PartitionManager.
 * It performs the same roll on cluster partitions that the Partition
 * Manager performs on inverted file partitions.
 */

public class ClusterManager extends PartitionManager
{
    
    protected ClassifierManager classifierManager;
    
    /** 
     * Constructs the ClusterManager.  Since the Cluster Manager only provides
     * a main dictionary and a document dictionary, nothing special is needed.
     * 
     */
    public ClusterManager() {
        subDir = "cluster";
        logTag = "ClstMAN";
    }
    
     
    /** 
     * Signals the ClusterManager that all the clusters currently in
     * memory should be dumped to disk so that they can be used for classifying
     * new documents.
     */
    public void dump() throws java.io.IOException {
    }


    /**
     * A method to reap a single partition.  This can be overridden in a
     * subclass so that the reap method will work for the super and
     * subclass.
     */
    protected void reapPartition(int partNumber) {
        ClusterDiskPartition.reap(this, partNumber);
    }

    /** 
     * Gets the class manager that this instance is working with 
     * @return the classifier manager for this cluster manager
     */
    public ClassifierManager getClassManager() {
        return classifierManager;
    }
    
    public void newProperties(PropertySheet ps) throws PropertyException {
        classifierManager = (ClassifierManager) ps.getComponent(PROP_CLASSIFIER_MANAGER);
        super.newProperties(ps);
    }

    public String getName() {
        return name;
    }

    @ConfigComponent(type=com.sun.labs.minion.classification.ClassifierManager.class)
    public static final String PROP_CLASSIFIER_MANAGER = "class_manager";

}
