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
import com.sun.labs.minion.indexer.entry.EntryFactory;
import java.io.RandomAccessFile;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.Partition;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 */
public class CachedDiskDictionary<N extends Comparable> extends DiskDictionary<N> {

    /**
     * The entries in the dictionary, suitable for fetching by ID.
     */
    private QueryEntry<N>[] entries;

    private Map<N,QueryEntry> entriesByName;

    /**
     * The entries in the dictionary in the order they were in the dictionary.
     */
    private QueryEntry<N>[] dictOrderEntries;

    /**
     * Creates a disk dictionary that we can use for querying.
     * @param entryfactory a factory to generate entries from this dictionary.
     * @param decoder A decoder for the names in this dictionary.
     * @param dictFile The file containing the dictionary.
     * @param postFiles The files containing the postings associated with
     * the entries in this dictionary.
     * @throws java.io.IOException if there is any error opening the dictionary
     */
    public CachedDiskDictionary(EntryFactory factory,
            NameDecoder decoder, RandomAccessFile dictFile,
            RandomAccessFile[] postFiles) throws java.io.IOException {
        this(factory, decoder, dictFile, postFiles,
                PostingsInputType.CHANNEL_FULL_POST, BufferType.FILEBUFFER, null);
    }

    /**
     * Creates a disk dictionary that we can use for querying.
     * @param entryClass The class of the entries that the dictionary
     * contains.
     * @param decoder A decoder for the names in this dictionary.
     * @param dictFile The file containing the dictionary.
     * @param postFiles The files containing the postings associated with
     * the entries in this dictionary.
     * @param part The partition with which this dictionary is associated.
     * @throws java.io.IOException if there is any error opening the dictionary
     */
    public CachedDiskDictionary(EntryFactory factory,
            NameDecoder decoder, RandomAccessFile dictFile,
            RandomAccessFile[] postFiles,
            Partition part) throws java.io.IOException {
        this(factory, decoder, dictFile, postFiles,
                PostingsInputType.CHANNEL_FULL_POST, BufferType.FILEBUFFER, part);
    }

    /**
     * Creates a disk dictionary that we can use for querying.
     * @param entryClass The class of the entries that the dictionary
     * contains.
     * @param decoder A decoder for the names in this dictionary.
     * @param dictFile The file containing the dictionary.
     * @param postFiles The files containing the postings associated with
     * the entries in this dictionary.
     * @param postInType The type of postings input to use.
     * @param part The partition with which this dictionary is associated.
     * @throws java.io.IOException if there is any error opening the dictionary
     */
    public CachedDiskDictionary(EntryFactory factory,
            NameDecoder decoder, RandomAccessFile dictFile,
            RandomAccessFile[] postFiles, PostingsInputType postingsInputType,
            BufferType fileBufferType,
            Partition part) throws java.io.IOException {
        this(factory, decoder, dictFile, postFiles, postingsInputType,
                fileBufferType,
                2048,
                1024,
                1024,
                1024, part);
    }

    /**
     * Creates a disk dictionary that we can use for querying.
     * @param nameBufferSize the size of the buffer (in bytes) to use for the entry names
     * @param offsetsBufferSize the size of the buffer (in bytes) to use for the entry name offsets
     * @param infoBufferSize the size of the buffer (in bytes) to use for the entry information
     * @param infoOffsetsBufferSize the size of the buffer (in bytes) to use for the entry information offsets
     * @param entryClass The class of the entries that the dictionary
     * contains.
     * @param fileBufferType the type of file buffers to use for the postings
     * @param decoder A decoder for the names in this dictionary.
     * @param dictFile The file containing the dictionary.
     * @param postFiles The files containing the postings associated with
     * the entries in this dictionary.
     * @param postingsInputType The type of postings input to use.
     * @param part The partition with which this dictionary is associated.
     * @throws java.io.IOException if there is any error opening the dictionary
     */
    public CachedDiskDictionary(EntryFactory factory,
            NameDecoder decoder, RandomAccessFile dictFile,
            RandomAccessFile[] postFiles, PostingsInputType postingsInputType, BufferType fileBufferType,
            int nameBufferSize,
            int offsetsBufferSize, int infoBufferSize, int infoOffsetsBufferSize,
            Partition part) throws java.io.IOException {
        super(factory, decoder, dictFile, postFiles, postingsInputType,
                fileBufferType, -1,
                nameBufferSize, offsetsBufferSize, infoBufferSize,
                infoOffsetsBufferSize, part);

        entries = new QueryEntry[dh.getMaxID()];
        entriesByName = new LinkedHashMap<N, QueryEntry>(dh.getMaxID());

        if(idToPosn != null) {
            dictOrderEntries = new QueryEntry[dh.getMaxID()];
        }

        //
        // Read everything into the cache now.
        int p = 0;
        for(DictionaryIterator i = super.iterator(); i.hasNext();) {
            QueryEntry<N> e = (QueryEntry) i.next();
            entries[e.getID() - 1] = e;
            entriesByName.put(e.getName(), e);
            if(dictOrderEntries != null) {
                dictOrderEntries[p++] = e;
            }
        }
    }

    /**
     * Gets a entry from the dictionary, given the name for the entry.
     *
     * @param name The name of the entry to get.
     * @return The entry associated with the name, or <code>null</code> if
     * the name doesn't appear in the dictionary.
     */
    @Override
    public QueryEntry get(N name) {
        QueryEntry e = entriesByName.get(name);
        if(e == null) {
            return null;
        }
        return (QueryEntry) e.clone();
    }

    /**
     * Gets a entry from the dictionary, given the ID for the entry.
     *
     * @param id the ID to find.
     * @return The block, or <code>null</code> if the ID doesn't occur in
     * our dictionary.
     */
    @Override
    public QueryEntry getByID(int id) {
        if(id < 1 || id > entries.length) {
            return null;
        }
        return (QueryEntry) entries[id-1].clone();
    }

    @Override
    public DictionaryIterator iterator() {
        if(dictOrderEntries != null) {
            return new ArrayDictionaryIterator(dictOrderEntries);
        }
        return new ArrayDictionaryIterator(entries, 0, entries.length);
    }

    /**
     * Gets an iterator for the dictionary that starts and stops at the
     * given indices in the dictionary.  This can be used when we want to
     * process pieces of a dictionary in different threads.
     * @param begin the beginning index in the dictionary, counting from
     * 0.  If this value is less than zero it will be clamped to zero.
     * @param end the ending index in the dictionary, counting from 0.  If
     * this value is greater than the number of entries in the dictionary,
     * it will be limited to that number.
     * @return an iterator for the dictionary.  The elements of the iterator implement the
     * <CODE>Map.Entry</CODE> interface
     */
    @Override
    public DictionaryIterator iterator(int begin, int end) {
        if(dictOrderEntries != null) {
            return new ArrayDictionaryIterator(dictOrderEntries, begin,
                                               end);
        }
        return new ArrayDictionaryIterator(entries, begin, end);
    }
}
