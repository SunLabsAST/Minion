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

import java.util.Iterator;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.util.buffer.ReadableBuffer;

/**
 * An interface for iterators for a dictionary.
 */
public interface DictionaryIterator extends Iterator<QueryEntry> {

    /**
     * Estimates the total size of the documents held in the postings for
     * all of the terms in the iterator.
     */
    public int estimateSize();

    /**
     * Gets the number of entries that will be returned by this iterator.
     */
    public int getNEntries();

    /**
     * Modifies the iterator so that it only returns entries whose names have
     * actually occurred in the indexed material.  So, for example, if the word
     * <em>Dog</em> occurs in the indexed material, and is the only occurrence
     * of that word, then the entry <em>dog</em> in a cased dictionary would not
     * be returned if this method is called with a value of <code>true</code>.
     *
     * <p>
     *
     * Note that this option really only makes sense for dictionaries that
     * use cased entries.
     *
     * @param actualOnly if <code>true</code> only entries with names
     * that actually occurred in the indexed material will be returned.
     * If <code>false</code> all entries will be returned.
     */
    public void setActualOnly(boolean actualOnly);
    
    /**
     * Tells the iterator to not use the buffered postings that it may have.
     * This is useful in situations where we don't need the streaming
     * postings.
     * @param unbufferedPostings if <code>true</code> then the entries will
     * use unbuffered postings.
     */
    public void setUnbufferedPostings(boolean unbufferedPostings);

    /**
     * Sets a map of the IDs that have been removed from a dictionary and 
     * should therefore not be returned by the iterator.
     * @param deletionMap 
     */
    void setDeletionMap(ReadableBuffer deletionMap);

} // DictionaryIterator
