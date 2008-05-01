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



/**
 * An interface that can be used when merging dictionaries to map one entry
 * to another.  This will be especially useful in cases where we need to
 * remap entry names and entry IDs, such as when we are merging document
 * dictionaries or when we are merging the document ID to entries
 * dictionaries in the field store.
 *
 * Instances of implementations of this class will be passed to {@link
 * com.sun.labs.minion.indexer.dictionary.DiskDictionary#merge(IndexEntry, com.sun.labs.minion.indexer.dictionary.NameEncoder, com.sun.labs.minion.indexer.partition.PartitionStats, com.sun.labs.minion.indexer.dictionary.DiskDictionary[], EntryMapper[], int[], int[][], java.io.RandomAccessFile, com.sun.labs.minion.indexer.postings.io.PostingsOutput[], boolean)}
 */
public interface EntryMapper {

    /**
     * Maps one entry to another.
     *
     * @param e The entry to map.
     * @return The mapped entry, or <code>null</code> if this entry should
     * not be included in a merged dictionary.  It is up to classes using
     * the entry mapper to account for this case!
     */
    public Entry map(Entry e);
    
}// EntryMapper
