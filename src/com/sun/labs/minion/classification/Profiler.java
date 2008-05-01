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

import com.sun.labs.util.props.Configurable;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileMemoryPartition;

/**
 * An interface for profilers that will run after dump time for a new partition.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public interface Profiler extends Configurable {
    
    /**
     * Runs the profiler on a newly dumped disk partition.
     * @param engine the engine in which we'll do the profiling.
     * @param mp the memory partition that's being profiled against.
     * @param dp the partition that we're going to profile against.
     * @return <code>true</code> if modifications were made to the memory partition,
     * <code>false</code> otherwise.
     * 
     */
    public boolean profile(SearchEngineImpl engine, InvFileMemoryPartition mp, InvFileDiskPartition dp);
}
