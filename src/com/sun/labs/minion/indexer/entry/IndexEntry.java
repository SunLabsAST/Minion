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

import com.sun.labs.minion.indexer.postings.Occurrence;

import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * An entry that is used in dictionaries that are built while indexing
 * documents.
 */
public interface IndexEntry extends Entry {

    /**
     * Sets the name of this entry.
     *
     * @param name The name that we wish the entry to have.
     */
    public void setName(Object name);

    /**
     * Sets the ID associated with this entry.
     *
     * @param id The id to use for this entry.
     */
    public void setID(int id);

    /**
     * Adds an occurrence to this index entry.
     *
     * @param o The occurrence to add.
     */
    public void add(Occurrence o);

    /**
     * Writes the postings associated with this entry to some or all of the
     * given channels.
     *
     * @param out The outputs to which we will write the postings.
     * @param idMap A map from the IDs currently used in the postings to
     * the IDs that should be used when the postings are written to disk.
     * This may be <code>null</code>, in which case no remapping will
     * occur.
     * @return true if postings were written, false otherwise
     * @throws java.io.IOException if there is any error writing the
     * postings.
     */
    public boolean writePostings(PostingsOutput[] out,
                              int[] idMap)
        throws java.io.IOException;

    /**
     * Appends the postings from another entry onto this one.  This is used
     * when merging dictionaries.
     * 
     * @param qe The entry that we want to append onto this one.
     * @param start The new starting ID for the partition that the entry
     * was drawn from.
     * @param idMap A map from old IDs in the given postings to new IDs
     * with gaps removed for deleted data.  If this is <code>null</code>,
     * then there are no deleted documents.
     */
    public void append(QueryEntry qe, int start, int[] idMap);
    
    /**
     * Encodes any information associated with the postings onto the given
     * buffer.
     *
     * @param b The buffer onto which the postings information should be
     * encoded.  The buffer will be positioned to the correct spot for the
     * encoding.
     */
    public void encodePostingsInfo(WriteableBuffer b);
    
}// IndexEntry
