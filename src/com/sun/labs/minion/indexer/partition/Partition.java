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
package com.sun.labs.minion.indexer.partition;

import java.util.logging.Logger;

/**
 * A superclass for <code>Partition</code>s.  This is mostly used to
 * provide a bit of information that we want subclasses to have in common.
 */
public abstract class Partition implements Comparable<Partition> {

    /**
     * The log.
     */
    private static final Logger logger = Logger.getLogger(Partition.class.getName());
    
   /**
     * The manager for this partition.
     */
    protected PartitionManager manager;

    /**
     * The header for this partition.
     */
    protected PartitionHeader header;

    /**
     * The number of this partition.
     */
    protected int partNumber;

    /**
     * The maximum document ID that was allocated in the partition.
     */
    protected int maxDocumentID;

    private String partitionName;

    /**
     * Gets the number of undeleted documents in the partition.
     * @return the number of undeleted documents in the partition.
     */
    public abstract int getNDocs();
    
    /**
     * Gets the maximum document ID in the partition.
     */
    public int getMaxDocumentID() {
        return maxDocumentID;
    }

    /**
     * Gets the number for this partition.
     * @return the partition number.
     */
    public int getPartitionNumber() {
        return partNumber;
    }

    public PartitionHeader getPartitionHeader() {
        return header;
    }

    /**
     * Gets the manager for this partition.
     * 
     * @return the manager for this partition
     */
    public PartitionManager getPartitionManager() {
        return manager;
    }

    protected void setPartitionManager(PartitionManager manager) {
        this.manager = manager;
    }
    
    public String[] getPostingsChannelNames() {
        return new String[] {"post"};
    }
    
    /**
     * Compares two partitions by their partition numbers.
     * @param p the partition to compare to.
     */
    @Override
    public int compareTo(Partition p) {
        return partNumber - p.partNumber;
    }
    
    public String getPartitionName() {
        return partitionName;
    }

    public void setPartitionName(String partitionName) {
        this.partitionName = partitionName;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 61 * hash
                + (this.manager != null ? this.manager.hashCode() : 0);
        hash = 61 * hash + partNumber;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final Partition other = (Partition) obj;
        if(this.manager != other.manager &&
                (this.manager == null || !this.manager.equals(other.manager))) {
            return false;
        }
        if(this.partNumber != other.partNumber) {
            return false;
        }
        return true;
    }
} // Partition
