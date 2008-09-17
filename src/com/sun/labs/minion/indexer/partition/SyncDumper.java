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
import com.sun.labs.util.props.ConfigBoolean;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import com.sun.labs.minion.pipeline.Stage;

/**
 * A dumper that will dump partitions synchronously.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class SyncDumper implements Dumper {

    private PartitionManager pm;
    
    /**
     * Whether we should do gc after (some) dumps.
     */
    private boolean doGC;
    
    /**
     * After how many dumps we should do a GC, if that's required.
     */
    private int gcInterval;
    
    /**
     * The number of dumps that we've done since the last gc.
     */
    private int nDumps;
    
    /**
     * Creates a SyncDumper
     */
    public SyncDumper() {
    }

    public void setSearchEngine(SearchEngine e) {
        pm = e.getManager();
    }

    public int getQueueLength() {
        return 1;
    }

    /**
     * Dumps the given stage synchronously.
     */
    public void dump(Stage s) {
        s.dump(null);
        
        //
        // Check whether we need to do GC after this dump.
        if(doGC) {
            nDumps++;
            if(nDumps == gcInterval) {
                System.gc();
                nDumps = 0;
            }
        }
        
        //
        // Do a merge if one is needed.
        PartitionManager.Merger m = pm.getMerger();
        if(m != null) {
            m.run();
        }
    }

    public void finish() {
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        doGC = ps.getBoolean(PROP_DO_GC);
        gcInterval = ps.getInt(PROP_GC_INTERVAL);
    }
    
    @ConfigBoolean(defaultValue=true)
    public static final String PROP_DO_GC = "do_gc";
    
    @ConfigInteger(defaultValue=5)
    public static final String PROP_GC_INTERVAL = "gc_interval";
}
