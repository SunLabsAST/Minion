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

package com.sun.labs.minion.indexer.dictionary;


import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;

import com.sun.labs.minion.indexer.partition.Partition;

/**
 * An interface for dictionaries.  This interface only supports a few
 * methods that both in memory and disk resident dictionaries will need.
 *
 * <p>
 * 
 * A dictionary is a map from entry names to entries.  An entry name can be
 * any class that defines {@link java.util.Comparator}.
 *
 * <p>
 *
 * @see com.sun.labs.minion.indexer.entry.Entry
 */
public interface Dictionary extends Iterable<QueryEntry> {

    /**
     * Puts a entry into the dictionary.  This will assign an ID to the entry
     * if it is not already in the dictionary.
     *
     * @param name The name of the entry to put in the dictionary.
     * @param e The entry to put in the dictionary.
     * @return Any previous entry stored in the dictionary under the given
     * name.
     */
    public IndexEntry put(Object name, IndexEntry e);

    /**
     * Gets a entry from the dictionary, given the name for the entry.
     *
     * @param name The name of the entry.
     * @return The entry associated with the name, or <code>null</code> if
     * the name doesn't appear in the dictionary.
     */
    public QueryEntry get(Object name);

    /**
     * Gets the partition to which this dictionary belongs.
     *
     * @return the partition to which the dictionary belongs
     */
    public Partition getPartition();

    /**
     * Gets the number of entries in the dictionary.
     *
     * @return the number of entries in the dictionary.
     */
    public int size();

    /**
     * Gets an iterator for the entries in the dictionary.
     *
     * @return An iterator for the entries in the dictionary.
     */
    public DictionaryIterator iterator();

} // Dictionary
