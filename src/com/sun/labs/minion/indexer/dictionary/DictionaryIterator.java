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

import com.sun.labs.minion.indexer.entry.Entry;
import java.util.Iterator;

/**
 * An interface for iterators for a dictionary.
 */
public interface DictionaryIterator<N extends Comparable> extends Iterator<Entry<N>>, Comparable<DictionaryIterator<N>> {
    
    /**
     * Gets the name of the entry at the head of the iterator.  This method is 
     * mostly used when we want to heap entries.
     * 
     * @return the name of the entry at the head of the iterator, or <code>null</code>
     * if the iterator has not been advanced yet.
     */
    public N getName();

    /**
     * Estimates the total number of IDs held in the postings for
     * all of the terms in the iterator.
     * @return an estimate of the number of IDs held in the postings for all
     * of the terms in the iterator.
     */
    public int estimateSize();

    /**
     * Gets the number of entries that will be returned by this iterator.
     * @return the number of entries that this iterator will return
     */
    public int getNEntries();

    /**
     * Tells the iterator to not use the buffered postings that it may have.
     * This is useful in situations where we don't need the streaming
     * postings.
     * @param unbufferedPostings if <code>true</code> then the entries will
     * use unbuffered postings.
     */
    public void setUnbufferedPostings(boolean unbufferedPostings);

} // DictionaryIterator
