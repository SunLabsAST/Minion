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

import java.util.SortedSet;
import java.util.Iterator;

import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.IDEntry;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.DocOccurrence;

import java.util.logging.Logger;

/**
 * A memory partition that will hold classifier data.
 *
 * @author Jeff Alexander
 */

public class ClusterMemoryPartition extends MemoryPartition
{
    /**
     * The log.
     */
 Logger logger = Logger.getLogger(getClass().getName());

    /**
     * The tag for this module.
     */
    protected static String logTag = "CMP";

    protected PartitionManager partitionManager;

    /** 
     * The number of clusters we've indexed into this partition 
     */
    protected int partClusters = 0;
    
    /** 
     * Constructs a ClusterMemoryPartition for general use.
     * 
     */
    public ClusterMemoryPartition() {
    }

    public int getNDocs() {
        return docDict.size();
    }

    protected void addClusters(FeatureClusterSet clusters) {
        //
        // Add data to main and doc dictionaries
        int numFeatures = 0;
        
        DocOccurrence d = new DocOccurrence();
        for (Iterator cit = clusters.iterator(); cit.hasNext(); ) {
            FeatureCluster cluster = (FeatureCluster)cit.next();
            SortedSet contents = cluster.getContents();
            numFeatures += contents.size();
            //
            // For each cluster, make an entry in the doc dict,
            // then throw all the members of the cluster in as
            // words in that doc.
            String docName = cluster.getName();
            DocKeyEntry clusterEntry = (DocKeyEntry)docDict.newEntry(docName);
            DocKeyEntry old = (DocKeyEntry) docDict.put(docName, clusterEntry);
            
            //
            // If there's already an entry, get rid of it, but we need to make
            // sure that we keep whatever's in this cluster already!
            if (old != null) {
                logger.fine("Duplicate cluster in partition: " + docName +
                         " deleting old version: " + old.getID());
                del.delete(old.getID());
                clusterEntry.merge(old, null);
            }

            //
            // Loop through the features in this cluster
            for (Iterator features = contents.iterator(); features.hasNext();) {
                Feature f = (Feature)features.next();

                //
                // Either use the existing entry for this feature, or
                // make a new entry
                String name = f.getName();
                IndexEntry entry = (IndexEntry)mainDict.get(name);
                
                if (entry == null) {
                    //
                    // Entry doesn't exist... create it
                    entry = (IDEntry)mainDict.newEntry(name);
                    mainDict.put(name, entry);
                }
                
                //
                // Add the feature/occurance to the main dict
                f.setID(clusterEntry.getID());
                entry.add(f);

                //
                // Add the feature/occurance to the doc dict
                d.setEntry(entry);
                clusterEntry.add(d);
            }

        }

        finishedAddingClusters(numFeatures);
    }

    protected void finishedAddingClusters(int numFeaturesAdded) {
    }
    
    /** 
     * Dumps the data that is specific to the classifier partition.
     * This will be any custom data that the classifier wants to store
     * as well as the set of docs that contributed to training each
     * classifier.  This method is called automatically by the dump()
     * method of MemoryPartition and doesn't need to be called directly.
     * 
     * @param sorted a sorted listed of all main dictionary entries
     * @throws java.io.IOException if there is any error writing data.
     */
    protected void dumpCustom(Entry[] sorted)
        throws java.io.IOException {

    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        partManager = (PartitionManager) ps.getComponent(PROP_PART_MANAGER);
    }

    public String getName() {
        return null;
    }

    @ConfigComponent(type=com.sun.labs.minion.indexer.partition.PartitionManager.class)
    public static final String PROP_PART_MANAGER = "part_manager";

    /**
     * The disk partition manager for this index.
     */ 
    private PartitionManager partManager;
}
