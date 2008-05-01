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
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import com.sun.labs.minion.classification.FeatureCluster;
import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.retrieval.*;

import com.sun.labs.minion.util.LRACache;

/**
 * An LRA cache of terms and their associated postings.
 */
public class TermCache extends LRACache<String,TermCacheElement> {
    
    protected SearchEngine engine;
    
    protected WeightingFunction wf;
    
    protected WeightingComponents wc;
    
    protected Map<String,TermStats> tsm;
    
    public TermCache(SearchEngine engine) {
        this(200, engine, null, null);
    }
    
    public TermCache(int size, SearchEngine engine) {
        this(size, engine, null, null);
    }
    
    public TermCache(int size, SearchEngine engine,
            WeightingFunction wf,
            WeightingComponents wc) {
        super(size);
        this.engine = engine;
        if(wf == null) {
            QueryConfig qc = engine.getQueryConfig();
            this.wf = qc.getWeightingFunction();
            this.wc = qc.getWeightingComponents();
        } else {
            this.wf = wf;
            this.wc = wc;
        }
        tsm = new HashMap<String,TermStats>();
    } // TermCache constructor
    
    public TermCacheElement get(String t, DiskPartition p) {
        TermCacheElement e = get(p + t);
        if(e == null) {
            e = new TermCacheElement(t, p);
            put(p + t, e);
        }
        return e;
    }
    
    public WeightingComponents getWeightingComponents() {
        return wc;
    }
    
    public WeightingFunction getWeightingFunction() {
        return wf;
    }

    /**
     * @deprecated
     */
    public TermStats getTermStats(String t) {
        return tsm.get(t);
    }
    
    public TermStats getTermStats(FeatureCluster fc) {
        TermStats ts = tsm.get(fc.getName());
        if(ts != null) {
            return ts;
        }

        // Prime the cache with the components of this cluster.
        ts = new TermStats(fc.getName());
        for(Iterator i = engine.getManager().getActivePartitions().iterator();
             i.hasNext(); ) {
            TermCacheElement tce = get(fc.getName(), (DiskPartition) i.next());
            for(Iterator j = fc.getContents().iterator(); j.hasNext(); ) {
                tce.add((WeightedFeature) j.next());
            }
            ts.add(tce.getTermStats());
        }
        setTermStats(ts);
        return ts;
    }
    
    public void setTermStats(TermStats s) {
        tsm.put(s.getName(), s);
    }
    
} // TermCache
