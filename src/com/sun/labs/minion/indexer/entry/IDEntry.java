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

package com.sun.labs.minion.indexer.entry;

import com.sun.labs.minion.indexer.postings.IDPostings;
import com.sun.labs.minion.indexer.postings.MergeablePostings;
import com.sun.labs.minion.indexer.postings.Postings;

import com.sun.labs.minion.util.buffer.ReadableBuffer;

public class IDEntry extends SinglePostingsEntry implements MergeableEntry {

    public IDEntry() {
        super(null);
    } // IDEntry constructor
    
    public IDEntry(Object name) {
        super(name);
    } // IDEntry constructor

    public Entry getEntry(Object name) {
        return new IDEntry(name);
    }
    
    /**
     * Gets the appropriate postings type for the class.  These postings
     * should be useable for indexing.
     *
     * @return Postings suitable for use when indexing.
     */
    protected Postings getPostings() {
        return new IDPostings();
    }

    /**
     * Reads the postings for this class, returning a set of postings
     * useful at query time.
     *
     * @param input The buffer containing the postings read from the
     * postings file.
     * @return The postings for this entry.
     */
    protected Postings getPostings(ReadableBuffer input) {
        return new IDPostings(input);
    }

    public void merge(QueryEntry qe, int[] idMap) {
        if(p == null) {
            p = getPostings();
        }
        ((MergeablePostings) p).merge(((MergeablePostings) ((IDEntry) qe).p), idMap);
    }

} // IDEntry
