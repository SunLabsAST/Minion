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

import java.util.Set;

/**
 * An interface that clients can implement to be notified when things happen 
 * in an index.
 * 
 * @see SearchEngine#addIndexListener
 */
public interface IndexListener {
    
    /**
     * Indicates that a new partition has been added to the index.  This method
     * will only be called when data is dumped into the index by indexing activity
     * (that is, it will not be called when a new partition is added due to 
     * merging existing partitions.).  When this method is called, the data will
     * be available for searching, etc. using the search engine's API.
     * 
     * @param e the engine that contains the partition that was dumped
     * @param keys the keys of the documents that are in the newly dumped partition. 
     * The elements of the set can be safely cast to <code>String</code>.
     */
    public void partitionAdded(SearchEngine e, Set<Object> keys);
    
}
