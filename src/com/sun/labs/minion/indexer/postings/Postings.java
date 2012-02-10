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


import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import java.io.IOException;

/**
 * An interface for the postings associated with a term in a dictionary.
 */
public interface Postings {

    /**
     * The types of postings that we support.
     */
    public enum Type {
        ID,
        ID_FREQ,
        ID_FREQ_POS,
        DOC_VECTOR,
        NONE;
        
        public static Postings getPostings(Type type) {
            switch(type) {
                case ID:
                    return new IDPostings();
                case ID_FREQ:
                    return new IDFreqPostings();
                case ID_FREQ_POS:
                    return new PositionPostings();
                case DOC_VECTOR:
                    return new DocumentVectorPostings();
                default:
                    return null;
            }
        }

        public static Postings getPostings(Type type, PostingsInput[] in, long[] offset, int[] size) throws IOException {
            switch(type) {
                case ID:
                    return new IDPostings(in, offset, size);
                case ID_FREQ:
                    return new IDFreqPostings(in, offset, size);
                case ID_FREQ_POS:
                    return new PositionPostings(in, offset, size);
                case DOC_VECTOR:
                    return new DocumentVectorPostings(in, offset, size);
                default:
                    return null;
            }
        }
    }

    /**
     * Gets the type of these postings.
     * 
     * @return the type of these postings.
     */
    public Type getType();
    
    /**
     * Gets the names of the channels to which this postings type will want to
     * write/read it's postings.  This lets us use useful names for the postings
     * files, rather than numbers.  Note that the first element will be assumed
     * to be "post", since that's our standard postings file.
     */
    public String[] getChannelNames();
    
    /**
     * Adds an occurrence to the postings list.
     *
     * @param o The occurrence.
     */
    public void add(Occurrence o);
    
    /**
     * Writes the postings to a set of postings output.  The sizes of the 
     * postings and the offsets where they were written are recorded 
     * as side-effects.
     */
    public void write(PostingsOutput[] out, long[] offset, int[] size) throws java.io.IOException;

    /**
     * Gets the number of IDs in the postings list.
     * @return the number of IDs in the list
     */
    public int getN();
    
    /**
     * Gets the last ID in the postings list.
     * @return the last ID
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
    
    /**
     * Clears the data from this postings object, which will allow it to be
     * re-used during indexing.  This doesn't make much sense for query-time
     * postings.
     */
    public void clear();
    
    /**
     * Provides a string description of the postings.  Do not use this in an
     * inner loop!
     * 
     * @param verbose if <code>true</code> a more verbose description (possibly
     * including all of the postings data) will be generated.
     */
    public String describe(boolean verbose);

}// Postings
