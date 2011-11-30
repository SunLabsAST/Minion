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

package com.sun.labs.minion.retrieval.cache;

import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.indexer.DiskField;

import com.sun.labs.minion.indexer.partition.DiskPartition;

import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.LRACache;

/**
 * A cache of document vectors.
 */
public class DocCache implements CacheValueComputer<String, DocCacheElement> {
    
    protected SearchEngine engine;
    
    protected WeightingFunction wf;
    
    protected WeightingComponents wc;

    protected DiskField df;
    
    public DocCache(DiskField df) {
        this(200, df);
    }
    
    public DocCache(int size, DiskField df) {
        this.df = df;
        QueryConfig qc =
                df.getPartition().getPartitionManager().getQueryConfig();
        this.wf = qc.getWeightingFunction();
        this.wc = qc.getWeightingComponents();
    } // TermCache constructor

    public DocCacheElement compute(String key) {
        return new DocCacheElement(key, df, wf, wc);
    }

    public WeightingComponents getWeightingComponents() {
        return wc;
    }
    
    public WeightingFunction getWeightingFunction() {
        return wf;
    }
    
} // TermCache
