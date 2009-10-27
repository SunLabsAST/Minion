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

import java.io.IOException;
import java.io.RandomAccessFile;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.retrieval.ArrayGroup;

/**
 * An interface that can be implemented by various saved field types.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public interface SavedField extends Comparable {
    /**
     * Adds data to a saved field.
     *
     *
     * @param docID the document ID for the document containing the saved
     * data
     * @param data The actual field data.
     */
    void add(int docID, Object data);
    
    /**
     * Writes the data to the provided stream.
     *
     *
     * @param path The path of the index directory.
     * @param dictFile The file where the dictionary will be written.
     * @param postOut A place to write the postings associated with the
     * values.
     * @param maxID The maximum document ID for this partition.
     * @throws java.io.IOException if there is an error during the
     * writing.
     */
    void dump(String path, RandomAccessFile dictFile,
            PostingsOutput[] postOut, int maxID) throws IOException;
    
    /**
     * Gets a particular value from the field.
     *
     *
     * @param v The value to get.
     * @param caseSensitive If true, case should be taken into account when
     * iterating through the values.  This value will only be observed for
     * character fields!
     * @return The term associated with that name, or <code>null</code> if
     * that term doesn't occur in the indexed material.
     * @throws UnsupportedOperationException if the implementing field type does
     * not support getting documents by value.
     */
    QueryEntry get(Object v, boolean caseSensitive);
    
    /**
     *
     * Get the field info object for this field.
     *
     *
     * @return the FieldInfo
     */
    FieldInfo getField();
    
    /**
     * Retrieve data from a saved field.
     *
     *
     * @param docID the document ID that we want data for.
     * @param all If <code>true</code>, return all known values for the
     * field in the given document.  If <code>false</code> return only one
     * value.
     * @return If <code>all</code> is <code>true</code>, then return a
     * <code>List</code> of the values stored in the given field in the
     * given document.  If <code>all</code> is <code>false</code>, a single
     * value of the appropriate type will be returned.
     *
     * <P>
     *
     * If the given name is not the name of a saved field, or the document
     * ID is invalid, <code>null</code> will be returned.
     */
    Object getSavedData(int docID, boolean all);
    
    /**
     * Gets a group of all the documents that do not have any values saved for
     * this field.
     *
     * @param ag a set of documents to which we should restrict the search for
     * documents with undefined field values.  If this is <code>null</code> then
     * there is no such restriction.
     * @return a set of documents that have no defined values for this field.
     * This set may be restricted to documents occurring in the group that was
     * passed in.
     */
    public ArrayGroup getUndefined(ArrayGroup ag);

    public void close();
    
    
    /**
     * Gets an iterator for the values in this field.
     */
    DictionaryIterator iterator(Object lowerBound, boolean includeLower, Object upperBound, boolean includeUpper);
    
    /**
     * Gets the number of saved items that we're storing.
     */
    int size();
    
    /**
     * Clears a saved field, if it's open for indexing.
     */
    void clear();
    
    /**
     * Merges a number of saved fields.
     *
     *
     * @param path The path to the index directory.
     * @param fields An array of fields to merge.
     * @param maxID The max doc ID in the new partition
     * @param starts The new starting document IDs for the partitions.
     * @param nUndel The number of undeleted documents in each partition
     * @param docIDMaps A map for each partition from old document IDs to
     * new document IDs.  IDs that map to a value less than 0 have been
     * deleted.  A null array means that the old IDs are the new IDs.
     * @param dictFile The file to which the merged dictionaries will be
     * written.
     * @param postOut The output to which the merged postings will be
     * written.
     * @throws java.io.IOException if there is an error during the merge.
     */
    void merge(String path, SavedField[] fields, int maxID, int[] starts,
            int[] nUndel,
            int[][] docIDMaps, RandomAccessFile dictFile,
            PostingsOutput postOut) throws java.io.IOException;
    
}
