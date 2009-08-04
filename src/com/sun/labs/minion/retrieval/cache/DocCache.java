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
import java.util.Map;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;

import com.sun.labs.minion.indexer.partition.DiskPartition;

import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.LRACache;

/**
 * An LRA cache of terms and their associated postings.
 */
public class DocCache extends LRACache<String,DocCacheElement> {
    
    protected SearchEngine engine;
    
    protected WeightingFunction wf;
    
    protected WeightingComponents wc;
    
    public DocCache(SearchEngine engine) {
        this(200, engine, null , null);
    }
    
    public DocCache(int size, SearchEngine engine) {
        this(size, engine, null , null);
    }
    
    public DocCache(int size, SearchEngine engine, WeightingFunction wf, WeightingComponents wc) {
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
    } // TermCache constructor
    
    public DocCacheElement get(String key, String field, DiskPartition p) {
        
        String hk = p + key + field;
        DocCacheElement e = get(hk);
        if(e == null) {
            e = new DocCacheElement((DocKeyEntry) p.getDocumentTerm(key), p, field, wf, wc);
            put(hk, e);
        }
        return e;
    }
    
    public DocCacheElement get(DocKeyEntry dke, String field, DiskPartition p) {
        String hk = p + dke.getName().toString() + field;
        DocCacheElement e = get(hk);
        if(e == null) {
            e = new DocCacheElement(dke, p, field, wf, wc);
            put(hk, e);
        }
        return e;
    }
    
    public WeightingComponents getWeightingComponents() {
        return wc;
    }
    
    public WeightingFunction getWeightingFunction() {
        return wf;
    }
    
} // TermCache
