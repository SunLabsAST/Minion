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

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import java.util.PriorityQueue;
import com.sun.labs.minion.indexer.entry.CasedEntry;
import com.sun.labs.minion.indexer.entry.CasedIDEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;


/**
 * A class that will iterate through all of the saved values in a set of
 * partitions for a given field.
 */
public class FieldIterator implements Iterator {

    /**
     * A heap holding the individual iterators.
     */
    protected PriorityQueue<HE> h;

    /**
     * Used to store the frequency of the current field returned by "next()"
     */
    protected int freq;
    
    /**
     * Creates an iterator for all of the unique field values in the given
     * partitions.
     *
     * @param parts The partitions from which we'll draw field values.
     * @param field The field whose values we want to iterate through.
     */
    public FieldIterator(List parts, String field, boolean ignoreCase) {
        h = new PriorityQueue<HE>();
        for(Iterator i = parts.iterator(); i.hasNext(); ) {
            HE e = new HE((InvFileDiskPartition) i.next(), field, ignoreCase);
            if(e.next()) {
                h.offer(e);
            }
        }
    } // FieldIterator constructor
    
    // Implementation of java.util.Iterator

    /**
     * Unsupported operation.
     *
     */
    public void remove() {
        throw new UnsupportedOperationException("Can't remove field values");
    }

    /**
     * Gets the next field value.  The field values are returned in
     * increasing order, depending on the field type.
     *
     * @return The next field value.
     */
    public Object next() {
        freq = 0;
        if(h.size() == 0) {
            throw new NoSuchElementException("No more values");
        }
        
        Object ret = h.peek().value;
        while(h.size() > 0 && h.peek().value.equals(ret)) {
            HE e = h.poll();
            freq += e.freq;
            if(e.next()) {
                h.offer(e);
            }
        }
            
        return ret;
    }
    
    public int getFreq() {
        return freq;
    }

    /**
     * Indicates whether there is another field value to return.
     *
     * @return <code>true</code> if there is another value to return,
     * <code>false</code> otherwise.
     */
    public boolean hasNext() {
        return h.size() > 0;
    }

    /**
     * An element to put on the heap.
     */
    protected class HE implements Comparable<HE> {

        /**
         * An iterator for the field values.
         */
        protected Iterator iter;

        /**
         * The next value from the iterator.
         */
        protected Object value;
 
        /**
         * The number of times the value occurs
         */
        protected int freq;

        protected boolean ignoreCase;
        
        protected HE(InvFileDiskPartition p, String field, boolean ignoreCase) {
            this.ignoreCase = ignoreCase;
            iter = p.getFieldIterator(field);
        }

        /**
         * Advances to the next entry.
         *
         * @return <code>true</code> if there is a next entry.
         */
        protected boolean next() {

            if(iter == null) {
                return false;
            }
            
            while(iter.hasNext()) {
                QueryEntry e = (QueryEntry) iter.next();
                if(ignoreCase && e instanceof CasedIDEntry) {
                    freq = ((CasedIDEntry) e).getNCaseInsensitive();
                    if(freq == 0) {
                        //
                        // This only has case-sensitive postings, so we
                        // don't want this one!
                        continue;
                    }
                    value = e.getName();
                } else {
                    value = e.getName();
                    freq = e.getN();
                }
                return true;
            }
            return false;
        }

        protected int getFreq() {
            return freq;
        }
        
        public int compareTo(HE o) {
            return ((Comparable) value).compareTo(o.value);
        }
    }
    
} // FieldIterator
