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

import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An LRA cache of terms and their associated postings.  Postings are cached per
 * partition.  The cache is meant to handle "terms" that are composed of multiple
 * actual terms, as well as terms that consist of a single string.  This is so
 * that common cases (e.g., morphological variations of a term in querying) can
 * be cached in a single set of postings.
 *
 * <p>
 *
 * Additionally, the cache can handle fielded and unfielded terms.
 */
public class TermCache implements CacheValueComputer<TermCacheKey, TermCacheElement> {

    private static Logger logger = Logger.getLogger(TermCache.class.getName());

    private ConcurrentLRUCache<TermCacheKey, TermCacheElement> cache;

    protected DiskPartition part;

    public TermCache(DiskPartition part) {
        this(200, part);
    }

    public TermCache(int size, DiskPartition part) {
        this.part = part;
        cache = new ConcurrentLRUCache<TermCacheKey, TermCacheElement>(size,
                                                                       this);
    }

    @Override
    public TermCacheElement compute(TermCacheKey key) {
        PostingsIteratorFeatures feat = key.getFeat();

        QueryStats qs = feat == null ? null : feat.getQueryStats();
        TermCacheElement ret;

        if(qs != null) {
            qs.termCacheMisses++;
        }

        if(feat != null && feat.getFields() != null) {
            //
            // We need fielded postings
            ret = new FieldedTermCacheElement(key.getNames(), feat, part);
        } else {
            //
            // Whole-doc postings.
            ret = new TermCacheElement(key.getNames(), feat, part);
        }
        return ret;
    }

    /**
     * Gets an element from the cache.  
     * @param term the name of the element to get.
     * @return the element corresponding to that name.  The postings associated
     * with this term will be for the whole document, as no iterator features
     * were provided.
     */
    public TermCacheElement get(String term) {
        return get(term, null);
    }

    public TermCacheElement get(String term, PostingsIteratorFeatures feat) {
        try {
            return cache.get(new TermCacheKey(term, feat));
        } catch(InterruptedException ex) {
             logger.log(Level.SEVERE,
                     String.format("Exception getting element for %s", term), ex);
           return null;
        }
    }

    public TermCacheElement get(List<String> terms) {
        return get(terms, null);
    }

    public TermCacheElement get(List<String> terms,
                                PostingsIteratorFeatures feat) {

        try {
            return cache.get(new TermCacheKey(terms, feat));
        } catch(InterruptedException ex) {
            logger.log(Level.SEVERE,
                    String.format("Exception getting element for %s", terms), ex);
            return null;
        }
    }

    public String toString() {
        return String.format("cache: %d items %d hits %d misses %.3f ratio",
                cache.size(), cache.getHits(), cache.getMisses(),
                cache.hitRatio());
    }
} // TermCache

