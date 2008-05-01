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

package com.sun.labs.minion.indexer.postings;

import com.sun.labs.minion.indexer.entry.Entry;

/**
 * An occurrence to use for document dictionaries.  Such an occurrence includes
 * the entry from the main dictionary that is to be added to a document vector.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class DocOccurrence extends OccurrenceImpl {
    
    /**
     * The dictionary entry for the term that is occurring.
     */
    protected Entry e;
    
    /**
     * The ids of the field in which the occurrence was found.
     */
    protected int[] fields;
    
    /**
     * Adds an entry to this occurrence, so that we can track the entries that
     * make up a vector for faster dumping.
     */
    public void setEntry(Entry e) {
        this.e = e;
    }
    
    public Entry getEntry() {
        return e;
    }
    
    public int[] getFields() {
        return fields;
    }
    
    public void setFields(int[] fields) {
        this.fields = fields;
    }
            
}
