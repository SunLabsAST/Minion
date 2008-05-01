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
import com.sun.labs.minion.indexer.entry.EntryMapper;
import com.sun.labs.minion.indexer.*;

/**
 * An implementation of <code>EntryMapper</code> that is used to remap the
 * names of the entries in the document-ID-to-field-values dictionary in
 * the field store.  The names (which are integers) are mapped to the new
 * names in the merged document ID space.
 */
public class LongNameEntryMapper implements EntryMapper {

    /**
     * The new starting ID for the document keys from this partition.  This
     * will be used when there is no garbage collection going on for the
     * partition.
     */
    protected int start;
    
    /**
     * A mapping from old document IDs to new document IDs in this
     * partition when garbage collection will occur.  A new ID of -1
     * indicates that the given key will not occur in the merged partition.
     * If there is no garbage collection for this partition, this will be
     * <code>null</code>
     */
    protected int[] idMap;
    
    /**
     * Creates a entry mapper for the documents from one partition.
     *
     * @param start The new starting ID for the document keys from this
     * partition.  This will be used when there is no garbage collection
     * going on for the partition.
     * @param idMap A mapping from old document IDs to new document IDs in
     * this partition when garbage collection will occur.  A new ID of -1
     * indicates that the given key will not occur in the merged
     * partition.  If there is no garbage collection under way for this
     * partition, then this element can be <code>null</code>.
     */
    public LongNameEntryMapper(int start, int[] idMap) {
        this.start = start;
        this.idMap = idMap;
    }
    
    /**
     * Maps a entry from a single partition document dictionary to a merged
     * partition document dictionary.  This will remap the document's ID to
     * an ID in the merged partition.
     *
     * @param e The entry to map.
     * @return The mapped entry, or <code>null</code> if this entry should
     * not be included in a merged dictionary.  It is up to classes using
     * the entry mapper to account for this case!
     */
    public Entry map(Entry e) {

        int name = ((Long) e.getName()).intValue();
        
        //
        // We may as well remap the ID while we're at it...
        if(idMap == null) {
            e.setID(e.getID() + start - 1);
        } else {
            e.setID(idMap[name]);
            if(e.getID() == -1) {

                //
                // Get rid of deleted information.
                return null;
            }
            e.setID(e.getID() + start - 1);
        }

        //
        // Reset the name of the entry to the new ID.
        e.setName(new Long(e.getID()));
        return e;
    }
    
} // LongNameEntryMapper
