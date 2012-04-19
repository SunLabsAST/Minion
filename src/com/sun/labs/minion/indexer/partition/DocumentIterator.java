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

package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.Document;
import java.util.Iterator;
import java.util.NoSuchElementException;
import com.sun.labs.minion.engine.DocumentImpl;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import java.util.Collection;

/**
 * An iterator for all of the documents in a set of partitions.
 */
public class DocumentIterator implements Iterator<Document> {
    
    protected PartitionManager m;
    
    protected Collection<DiskPartition> parts;
    
    protected DiskPartition currPart;
    
    protected QueryEntry currEntry;
    
    protected Iterator<DiskPartition> partIter;
    
    protected Iterator docIter;
    
    protected boolean hasNextCalled;
    
    /**
     * Creates a DocumentIterator that will iterate through all of the documents
     * managed by the given partition manager.
     * @param m the manager for the partitions for which we want a document
     * iterator.
     */
    public DocumentIterator(PartitionManager m) {
        this.m = m;
        parts = m.getActivePartitions();
        partIter = parts.iterator();
    }

    @Override
    public boolean hasNext() {
        
        hasNextCalled = true;
        
        //
        // If the current partition is null, see if there's another partition to
        // try.
        if(currPart == null) {
            if(partIter.hasNext()) {
                currPart = partIter.next();
                while(currPart.isClosed()) {
                    if(partIter.hasNext()) {
                        currPart = partIter.next();
                    } else {
                        currPart = null;
                        break;
                    }
                }

                if(currPart == null) {
                    currEntry = null;
                    return false;
                }
                
                docIter = currPart.getDocumentIterator();
            } else {
                currEntry = null;
                return false;
            }
        }
        
        //
        // If there's nothing left on this document iterator, then move onto the 
        // next partition.
        if(!docIter.hasNext()) {
            currPart = null;
            return hasNext();
        }
        
        //
        // Get the next entry from the current document iterator and check to
        // see if it's been deleted.  If so, we'll scan ahead and recur.
        currEntry = (QueryEntry) docIter.next();
        if(currPart.isDeleted(currEntry.getID())) {
            //
            // Scan the rest of this partition, looking for the next non-
            // deleted doc.  If we don't find one, make the recursive call
            // to switch partitions.
            while (docIter.hasNext()) {
                currEntry = (QueryEntry) docIter.next();
                if (!currPart.isDeleted(currEntry.getID())) {
                    break;
                }
            }
            //
            // Check our result - if there are no more entries and the current
            // one is deleted, move to the next partition
            if (!docIter.hasNext() && currPart.isDeleted(currEntry.getID())) {
                return hasNext();
            }
            //
            // Otherwise, fall through since we have a good entry in this
            // partition.
        }
        
        //
        // If we got here, then we're good.
        hasNextCalled = true;
        return true;
    }

    @Override
    public Document next() {
        if(!hasNextCalled) {
            hasNext();
            hasNextCalled = false;
        }
        
        if(currEntry == null) {
            throw new NoSuchElementException("No more documents");
        }
        
        return new DocumentImpl(currEntry);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove from document dictionary");
    }
    
}
