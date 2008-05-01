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

package com.sun.labs.minion.test.regression.query;

import java.util.List;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.partition.PartitionManager;

/**
 * Selects entrys from an index. This interface will allow the implementation
 * of different strategies for selecting a entry from an index, which
 * is usually maintained by a PartitionManager.
 */
public interface EntrySelector {

    /**
     * Selects entrys from an index. The PartitionManager contains the index
     * from which entrys are selected. Entrys are selected based on criteria
     * implemented by this method.
     *
     * @param manager the PartitionManager which stores the index
     *
     * @return a list of selected entrys
     */
    List<Entry> selectEntries(PartitionManager manager);
}
