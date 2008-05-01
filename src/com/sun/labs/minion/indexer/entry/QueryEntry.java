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


import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.PostingsIterator;

import com.sun.labs.minion.indexer.postings.io.PostingsInput;


import com.sun.labs.minion.util.buffer.ReadableBuffer;

/**
 * An entry that is returned from dictionaries during querying operations.
 */
public interface QueryEntry extends Entry {
    
    /**
     * Sets the inputs that will be used to read postings for this entry.
     * This allows us to use different input implementation strategies at
     * different times.
     *
     * @param postIn The channels from which the postings data can be read.
     */
    public void setPostingsInput(PostingsInput[] postIn);
    
    /**
     * Decodes the postings information associated with this entry.
     *
     * @param b The buffer containing the encoded postings information.
     * @param pos The position in <code>b</code> where the postings
     * information can be found.
     */
    public void decodePostingsInfo(ReadableBuffer b, int pos);
    
    /**
     * Reads the postings data for this entry.  This method must load all
     * available postings information, since this will be used at
     * dictionary merge time to append postings from one entry onto
     * another.
     *
     * @throws java.io.IOException if there is any error reading the
     * postings.
     */
    public void readPostings() throws java.io.IOException;
    
    /**
     * Indicates whether the postings associated with this entry have
     * position information.
     */
    public boolean hasPositionInformation();
    
    /**
     * Indicates whether the postings associated with this entry have 
     * field information.
     */
    public boolean hasFieldInformation();
    
    /**
     * Gets an iterator that will iterate through the postings associated
     * with this entry.
     *
     * @param features A set of features that the iterator must support.
     * @return An iterator for the postings.
     */
    public PostingsIterator iterator(PostingsIteratorFeatures features);
    
}// QueryEntry
