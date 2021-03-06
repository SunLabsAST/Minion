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


import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * An interface for the postings associated with a term in a dictionary.
 */
public interface Postings {

    /**
     * Sets the skip size used for building the skip table.  A larger
     * number will result in more IDs being encoded per skip.
     */
    public void setSkipSize(int size);

    /**
     * Adds an occurrence to the postings list.
     *
     * @param o The occurrence.
     */
    public void add(Occurrence o);

    /**
     * Gets the number of IDs in the postings list.
     */
    public int getN();
    
    /**
     * Gets the last ID in the postings list.
     */
    public int getLastID();

    /**
     * Gets the total number of occurrences associated with this set of
     * postings.  This is useful when a single postings entry may comprise
     * multiple occurrences.
     *
     * @return The total number of occurrences associated with these
     * postings.
     */
    public long getTotalOccurrences();

    /**
     * Gets the maximum frequency in the postings associated with this
     * entry.
     *
     * @return the maximum frequency across all of the postings stored in
     * this postings list.
     */
    public int getMaxFDT();

    /**
     * Finishes any ongoing encoding and prepares for the data to be
     * dumped.
     */
    public void finish();

    /**
     * Gets the size of the postings, in bytes.
     */
    public int size();

    /**
     * Gets a number of <code>Buffers</code> whose contents represent the
     * postings.  These buffers can be written to disk.
     *
     * <p>
     *
     * This method must ensure that all of the data used by the entry is
     * properly handled by the time that the method returns.  This method
     * will be called by a dictionary when it is ready to dump the postings
     * data to a stream.
     *
     * @return An array of <code>Buffer</code>s containing the postings
     * data.  All of the data in these buffers must be written to the
     * postings file!
     */
    public WriteableBuffer[] getBuffers();

    /**
     * Remaps the IDs in this postings list according to the given
     * old-to-new ID map.
     *
     * @param idMap A map from the IDs currently in use in the postings to
     * new IDs.
     */
    public void remap(int[] idMap);

    /**
     * Appends another set of postings to this one.
     * 
     * @param p The postings to append.  Implementers can safely assume
     * that the postings being passed in are of the same class as the
     * implementing class.
     * @param start The new starting document ID for the partition
     * that the entry was drawn from.
     */
    public void append(Postings p, int start);

    /**
     * Appends another set of postings to this one, removing any data
     * associated with deleted documents.
     * 
     * @param p The postings to append.  Implementers can safely assume
     * that the postings being passed in are of the same class as the
     * implementing class.
     * @param start The new starting document ID for the partition
     * that the entry was drawn from.
     * @param idMap A map from old IDs in the given postings to new IDs
     * with gaps removed for deleted data.  If this is null, then there are
     * no deleted documents.
     */
    public void append(Postings p, int start, int[] idMap);

    /**
     * Gets an iterator for the postings that satisfies a given set of
     * features.
     * 
     * @param features A set of features that the iterator must support.
     * Note that all implementations of this interface must be able to
     * handle a <code>null</code> value for this parameter!  When a
     * <code>null</code> is returned, the implementing class can either:
     * return an iterator that provides some default behavior or return
     * <code>null</code>.
     * @return A postings iterator that supports the given features.  If
     * the underlying postings do not support a specified feature, then a
     * warning should be logged and <code>null</code> will be returned.
     */
    public PostingsIterator iterator(PostingsIteratorFeatures features);

}// Postings
