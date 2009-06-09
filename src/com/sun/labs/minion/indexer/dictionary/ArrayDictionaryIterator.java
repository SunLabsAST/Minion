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
import com.sun.labs.minion.indexer.postings.io.PostingsInput;

/**
 * A dictionary iterator for an array of terms.
 */
public class ArrayDictionaryIterator implements DictionaryIterator {

    private DiskDictionary dd;
    
    private QueryEntry[] entries;
    
    private PostingsInput[] buffInputs;

    private int curr;
    
    private int begin;
    
    private int end;

    public ArrayDictionaryIterator(DiskDictionary dd, QueryEntry[] entries) {
        this(dd, entries, 0, entries != null ? entries.length : 0, null);
    }
    
    public ArrayDictionaryIterator(DiskDictionary dd, QueryEntry[] entries,
            PostingsInput[] buffInputs) {
        this(dd, entries, 0, entries != null ? entries.length : 0, buffInputs);
    }
    
    public ArrayDictionaryIterator(DiskDictionary dd, QueryEntry[] entries, int begin, int end) {
        this(dd, entries, begin, end, null);
    }
    
    public ArrayDictionaryIterator(DiskDictionary dd, QueryEntry[] entries, int begin, int end,
            PostingsInput[] buffInputs) {
        this.dd = dd;
        this.buffInputs = buffInputs;
        if(entries == null) {
            this.entries = new QueryEntry[0];
            curr = 0;
            begin = 0;
            end = 0;
        } else {
            this.entries = entries;
            curr = begin;
            this.begin = begin;
            this.end = end;
        }
    } // ArrayDictionaryIterator constructor
    
    // Implementation of java.util.Iterator

    /**
     * Describe <code>remove</code> method here.
     *
     */
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove term");
    }

    /**
     * Describe <code>next</code> method here.
     *
     * @return an <code>Object</code> value
     */
    public QueryEntry next() {
        if(curr < end) {
            return entries[curr++];
        }

        throw new java.util.NoSuchElementException("No more terms!");
    }

    /**
     * Describe <code>hasNext</code> method here.
     *
     * @return a <code>boolean</code> value
     */
    public boolean hasNext() {
        return curr < end;
    }
    
    /**
     * Estimates the number of documents in the postings for the entries 
     * represented by this iterator
     *
     * @return an estimate of the number of documents for the postings in
     * the entries represented by this iterator.  Note that this is likely an
     * overestimate.
     */
    public int estimateSize() {
        int sz = 0;
        for(int i = begin; i < end; i++) {
            sz += entries[i].getN();
        }
        return sz;
    }

    public int getNEntries() {
        return entries.length;
    }

    public void setActualOnly(boolean actualOnly) {
    }

    public QueryEntry get(Object name) {
        return dd.get(name);
    }

    public QueryEntry get(int id) {
        return dd.getByID(id);
    }
    
} // ArrayDictionaryIterator
