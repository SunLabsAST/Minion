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



import com.sun.labs.minion.indexer.entry.Entry;
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
 * @param <N> the type of the names in the dictionary, which must be comparable.
 * @see com.sun.labs.minion.indexer.entry.Entry
 */
public interface Dictionary<N extends Comparable> extends Iterable<Entry<N>> {
    
    /**
     * Gets the names of the channels needed to read or write the postings
     * associated with the entries in this dictionary.
     */
    public String[] getPostingsChannelNames();
    
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

} // Dictionary
