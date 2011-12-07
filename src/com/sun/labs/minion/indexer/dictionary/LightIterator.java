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

package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.indexer.entry.QueryEntry;

/**
 * An interface for a lightweight iterator for a dictionary.  The idea here is
 * similar to the idea for the postings iterator:  don't return an object from
 * the <code>next</code> method, but provide accessors for the data that we want.
 */
public interface LightIterator extends Comparable {

    /**
     * Advances the iterator to the next entry in the dictionary.
     * 
     * @return <code>true</code> if there is such an entry, <code>false</code>
     * otherwise.
     */
    public boolean next();
    
    
    /**
     * Gets the name of the entry at the head of the iterator.  This can be
     * used for comparisons.
     */
    public Comparable getName();
    
    /**
     * Gets the entire current entry, re-using an existing entry rather than 
     * creating a new one.
     * 
     * @return the current entry.
     */
    public QueryEntry getEntry(QueryEntry qe);
    
    
}
