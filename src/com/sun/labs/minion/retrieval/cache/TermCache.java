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


import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.util.LRACache;

/**
 * An LRA cache of terms and their associated postings.  This is partition specific.
 */
public class TermCache extends LRACache<String,TermCacheElement> {
    
    protected DiskPartition part;
    
    public TermCache(DiskPartition part) {
        this(200, part);
    }
    
    public TermCache(int size, DiskPartition part) {
        super(size);
        this.part = part;
    }

    /**
     * Creates a new element for this cache.  Note that the new element is not
     * added to the cache, since no terms have been added to the element yet.
     * @param name the name of the cache element.
     * @return a new cache element
     */
    public TermCacheElement create(String name) {
        return new TermCacheElement(name, part);
    }

    /**
     * Gets an element from the cache.  Overridden to add synchronization, since
     * this cache will be used by multiple threads.
     * @param name the name of the element to get.
     * @return the element corresponding to that name.
     */
    public synchronized TermCacheElement get(String name) {
        return super.get(name);
    }

    /**
     * Puts an element into the cache.  This can be used by someone who has
     * added postings to an element and now wants to make sure that the 
     * results are available in the cache.
     * @param el the element to put into the cache
     * @throws IllegalArgumentException if you attempt to add an element that is
     * drawn from a partition different than the one for this cache.
     */
    public synchronized void put(TermCacheElement el) {
        if(el.part != part) {
            throw new IllegalArgumentException(
                    "Attempt to add element for partition " +
                    el.part + " to cache for partition " + part);
        }
        put(el.name, el);
    }
} // TermCache
