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

import com.sun.labs.minion.SearchEngine;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import com.sun.labs.minion.pipeline.Stage;

/**
 * A dumper that will throw away partitions without doing anything.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class ThrowDumper implements Dumper {

    /**
     * Creates a SyncDumper
     */
    public ThrowDumper() {
    }

    public void setSearchEngine(SearchEngine e) {
    }

    public int getQueueLength() {
        return 1;
    }

    /**
     * Dumps the given stage synchronously.
     */
    public void dump(Stage s) {
        com.sun.labs.minion.util.MinionLog.debug("TD", 0, "dump: " + ((MemoryPartition) s).docDict.size() + " " + ((MemoryPartition) s).mainDict.size());
    }

    public void finish() {
    }

    public void newProperties(PropertySheet propertySheet) throws PropertyException {
    }
}
