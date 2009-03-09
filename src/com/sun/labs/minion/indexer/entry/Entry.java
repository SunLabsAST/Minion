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

package com.sun.labs.minion.indexer.entry;

import com.sun.labs.minion.indexer.dictionary.Dictionary;
import com.sun.labs.minion.indexer.partition.Partition;


/**
 * An interface describing things that can be stored in dictionaries,
 * either for indexing purposes or for querying purposes.  All entries have
 * a name that can be gotten.
 */
public interface Entry extends Comparable {

    /**
     * Gets a new entry with the given name.
     *
     * @param name the name that we want to give the entry.
     * @return a new entry.
     */
    public Entry getEntry(Object name);

    /**
     * Gets a new entry that contains a copy of the data in this entry.
     *
     * @return a new entry.
     */
    public Entry getEntry();
    
    /**
     * Gets the name of this entry, which can be any object.
     */
    public Object getName();

    /**
     * Sets the name of the entry, which can be any object.
     */
    public void setName(Object name);

    /**
     * Gets the ID associated with this entry.
     */
    public int getID();

    /**
     * Sets the ID associated with this entry.
     */
    public void setID(int id);
    
    /**
     * Gets the number of postings associated with this entry.  This can be
     * used to sort entries by their frequency during query operations.
     *
     * @return The number of postings associated with this entry.
     */
    public int getN();

    /**
     * Gets the total number of occurrences associated with this entry.
     * This is useful when a single postings entry may comprise multiple
     * occurrences.
     *
     * <p>
     *
     * At the moment, this is only really useful for entries from the main
     * dictionary for a partition.
     *
     * @return The total number of occurrences associated with this entry.
     */
    public long getTotalOccurrences();

    /**
     * Gets the maximum document term frequency from this entry.  For all
     * IDs asssociated with this entry, this is the maximum frequency
     * across all the IDs.
     */
    public int getMaxFDT();

    /**
     * Sets the dictionary that this entry was drawn from.
     */
    public void setDictionary(Dictionary dict);
    
    /**
     * Gets the partition that this entry was drawn from.
     */
    public Partition getPartition();
    
    /**
     * Returns the number of channels needed to store or retrieve the
     * postings for this entry type.
     */
    public int getNumChannels();

}// Entry
