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

import com.sun.labs.minion.indexer.dictionary.DictionaryFactory;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.IDEntry;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;

import java.util.logging.Logger;

/**
 * A disk partition that will hold classifier data.
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.13 $
 */

public class ClusterDiskPartition extends DiskPartition
{

    static Logger logger = Logger.getLogger(ClusterDiskPartition.class.getName());

    protected static String logTag = "ClustDP";

    protected ClassifierModel modelInstance;
    
    protected FeatureClusterer clustererInstance;

    /** 
     * Constructs a disk partition for a specific partition
     * number.
     * 
     * @param partNum the number of this partition
     * @param manager the classifier manager for this partition
     */
    public ClusterDiskPartition(Integer partNum,
                                ClusterManager manager,
            DictionaryFactory mainDictFactory,
            DictionaryFactory documentDictFactory)
        throws java.io.IOException {
        super(partNum, manager, mainDictFactory, documentDictFactory);
        modelInstance = manager.getClassManager().getModelInstance();
        clustererInstance = manager.getClassManager().getClustererInstance();
    }

    /** 
     * Gets a cluster by name
     * 
     * @param clusterName the name of the cluster
     * @return the cluster with the given name
     */
    public FeatureCluster getCluster(String clusterName) {
        DocKeyEntry ent = (DocKeyEntry) docDict.get(clusterName);
        return ent == null ? null : makeCluster(ent);
    }

    /** 
     * Gets the clusters that contain the given feature
     * 
     * @param fname the feature to find
     * @return the cluster containing that feature
     */
    public FeatureClusterSet getClustersContaining(String fname) {
        IDEntry ent = (IDEntry)mainDict.get(fname);
        FeatureClusterSet fcs = new FeatureClusterSet();
        
        if (ent == null) {
            return fcs;
        }
        
        PostingsIterator it =
            ent.iterator(new PostingsIteratorFeatures());

        if (it == null) {
            logger.severe("No postings iterator for: " + ent.getName());
            return fcs;
        }

        //
        // For each "doc" (ie cluster) that it appears in, get the features
        // for that cluster and throw them in the result set
        while (it.next()) {
            DocKeyEntry docEnt = (DocKeyEntry)docDict.getByID(it.getID());
            fcs.add(makeCluster(docEnt));
        }
        return fcs;
    }
    
    protected FeatureCluster makeCluster(DocKeyEntry docEntry) {
        FeatureCluster cluster = clustererInstance.newCluster();
        cluster.setName((String)docEntry.getName());
        
        //
        // Iterate through the postings and make a feature
        // for each id
        PostingsIterator it = docEntry.iterator(new PostingsIteratorFeatures());
        if(it == null) {
            logger.severe("No feature iterator for: " + docEntry.getName());
            return cluster;
        }

        while (it.next()) {
            //
            // Decode the current info into a feature
            Feature f = clustererInstance.newFeature();
            f.setID(it.getID());

            //
            // Look up the feature's name in the main dict.
            Entry mainEntry = mainDict.getByID(f.getID());
            f.setName((String)mainEntry.getName());
            cluster.add(f);
        }
        return cluster;
    }

    
    /**
     * Reaps the given classifier partition.
     *
     * @param m The manager associated with the partition.
     * @param n The partition number to reap.
     */
    protected static void reap(PartitionManager m, int n) {
        DiskPartition.reap(m, n);
    }

    /** 
     * Returns true if documents in this partition type can be merged -
     * that is, that the postings of two same-named docs in different
     * partitions will be combined.
     * 
     * @return true for clusters
     */
    public boolean docsAreMerged() {
        return true;
    }

}
