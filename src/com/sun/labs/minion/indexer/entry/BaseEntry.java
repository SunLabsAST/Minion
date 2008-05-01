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

import com.sun.labs.minion.indexer.postings.io.PostingsInput;

/**
 * An abstract base class for all entry types.  This supplies the elements
 * common to all entries, namely an ID and a name.  The class used for
 * naming entries must implement <code>Comparable</code> so that entries
 * can be sorted by name when dictionaries are dumped to disk.
 */
public abstract class BaseEntry implements IndexEntry, QueryEntry {

    /**
     * The ID of this entry.
     */
    protected int id;

    /**
     * The name of this entry.  The class used for naming entries must
     * implement <code>Comparable</code> so that entries can be sorted by
     * name when dictionaries are dumped to disk.
     */
    protected Object name;

    /**
     * The input channels that are associated with the postings for this
     * entry.
     */
    protected PostingsInput[] postIn;

    /**
     * The dictionary that this entry was drawn from.
     */
    protected Dictionary dict;

    /**
     * Creates a base entry with no name.
     */
    public BaseEntry() {
    }

    /**
     * Creates an entry with a given name.
     * @param name the name of the entry
     */
    public BaseEntry(Object name) {
        setName(name);
    }

    //
    // Implementation of Entry.
    
    /**
     * Gets the name of this entry
     *
     * @return The name of this entry.
     */
    public Object getName() {
        return name;
    }

    /**
     * Sets the name of this entry.
     *
     * @param name The name that we wish the entry to have.
     */
    public void setName(Object name) {
        this.name = name;
    }

    /**
     * Gets the ID associated with this entry.
     */
    public int getID() {
        return id;
    }

    /**
     * Sets the ID associated with this entry.
     *
     * @param id The id to use for this entry.
     */
    public void setID(int id) {
        this.id = id;
    }

    /**
     * Compares two entries by the order of their names.
     */
    public int compareTo(Object o) {
        return ((Comparable) name).compareTo(((BaseEntry) o).name);
    }
    
    //
    // Implementation of QueryEntry.

    /**
     * Sets the channels that will be used to read postings for this
     * entry.  This allows us to use different channel implementation
     * strategies at different times.
     *
     * @param postIn The inputs from which the postings data can be read.
     */
    public void setPostingsInput(PostingsInput[] postIn) {
        this.postIn = postIn;
    }

    /**
     * Sets the dictionary that this entry was drawn from.
     */
    public void setDictionary(Dictionary dict) {
        this.dict = dict;
    }
    
    /**
     * Gets the partition from which this entry was drawn.
     */
    public Partition getPartition() {
        return ((Dictionary) dict).getPartition();
    }

    public String toString() {
		return name == null ? "null" : name.toString(); 
    }

} // BaseEntry
