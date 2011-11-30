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

import com.sun.labs.minion.QueryStats;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Collections;
import java.util.PriorityQueue;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.EntryMapper;
import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.minion.indexer.postings.Occurrence;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.io.FilePostingsInput;
import com.sun.labs.minion.indexer.postings.io.ChannelPostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsInput;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.FileReadableBuffer;
import com.sun.labs.minion.util.buffer.NIOFileReadableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.File;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A base class for all classes that implement dictionaries for use during
 * querying.
 * @param <N> the type of the names in the dictionary.
 */
public class DiskDictionary<N extends Comparable> implements Dictionary<N> {

    /**
     * The header for the dictionary.
     */
    protected DictionaryHeader dh;

    /**
     * The type of entry that we contain.
     */
    protected EntryFactory factory;

    /**
     * The map from entry IDs to positions in the dictionary.
     */
    protected ReadableBuffer idToPosn;

    /**
     * The entry names.
     */
    protected ReadableBuffer names;

    /**
     * The offsets of the names of the uncompressed entries.
     */
    protected ReadableBuffer nameOffsets;

    /**
     * The information for the entries.
     */
    protected ReadableBuffer entryInfo;

    /**
     * The offsets for the entry information.
     */
    protected ReadableBuffer entryInfoOffsets;

    /**
     * An in-memory binary search tree to support searching for terms.
     */
    private BinarySearchTree bst;

    private String name;

    /**
     * A lookup state local to each thread
     */
    protected static ThreadLocal<WeakHashMap<DiskDictionary, LookupState>> threadLookupStates =
            new ThreadLocal<WeakHashMap<DiskDictionary, LookupState>>() {

                protected WeakHashMap<DiskDictionary, LookupState> initialValue() {
                    return new WeakHashMap<DiskDictionary, LookupState>();
                }
            };

    /**
     * A decoder for the names in this dictionary.
     */
    protected NameDecoder<N> decoder;

    /**
     * The dictionary file.
     */
    protected RandomAccessFile dictFile;

    /**
     * The postings files.
     */
    protected RandomAccessFile[] postFiles;

    /**
     * Our postings inputs.
     */
    protected PostingsInput[] postIn;

    /**
     * The partition that we are associated with.
     */
    protected Partition part;

    /**
     * The log.
     */
    protected static Logger logger = Logger.getLogger(DiskDictionary.class.
            getName());

    /**
     * The tag for this module.
     */
    protected static String logTag = "DD";

    public enum PostingsInputType {

        /**
         * File channel and fully load the postings.
         */
        CHANNEL_FULL_POST,
        /**
         * File channel and partially load the postings.
         */
        CHANNEL_PART_POST,
        /**
         * Random access file and fully load postings.
         */
        FILE_FULL_POST,
        /**
         * Random access file and partially load postings.
         */
        FILE_PART_POST

    }

    /**
     * An enum for the kinds of file-backed buffers that we can use to
     * read the dictionary data.
     */
    public enum BufferType {

        FILEBUFFER,
        NIOFILEBUFFER

    }
    private BufferType fileBufferType;

    /**
     * Creates an dict
     */
    protected DiskDictionary() {
    }

    /**
     * Creates a disk dictionary that we can use for querying.
     * @param factory A factory to generate entries in this dictionary.
     * @param decoder A decoder for the names in this dictionary.
     * @param dictFile The file containing the dictionary.
     * @param postFiles The files containing the postings associated with
     * the entries in this dictionary.
     * @throws java.io.IOException if there is any error opening the dictionary
     */
    public DiskDictionary(EntryFactory<N> factory,
                          NameDecoder decoder,
                          RandomAccessFile dictFile,
                          RandomAccessFile[] postFiles) throws
            java.io.IOException {
        this(factory, decoder, dictFile, postFiles,
             PostingsInputType.FILE_FULL_POST,
             BufferType.NIOFILEBUFFER,256,
             2048, 1024, 1024, 1024, null);
    }

    /**
     * Creates a disk dictionary that we can use for querying.
     * @param factory A factory to generate entries for this dictionary
     * @param decoder A decoder for the names in this dictionary.
     * @param dictFile The file containing the dictionary.
     * @param postFiles The files containing the postings associated with
     * the entries in this dictionary.
     * @param part The partition with which this dictionary is associated.
     * @throws java.io.IOException if there is any error opening the dictionary
     */
    public DiskDictionary(EntryFactory factory,
                          NameDecoder decoder,
                          RandomAccessFile dictFile,
                          RandomAccessFile[] postFiles,
                          Partition part) throws java.io.IOException {
        this(factory, decoder, dictFile, postFiles,
             PostingsInputType.CHANNEL_FULL_POST,
             BufferType.NIOFILEBUFFER, 256,
             2048, 1024, 1024, 1024, part);
    }

    /**
     * Creates a disk dictionary that we can use for querying.
     * @param factory A factory to generate entries for this dictionary.
     * @param decoder A decoder for the names in this dictionary.
     * @param dictFile The file containing the dictionary.
     * @param postFiles The files containing the postings associated with
     * the entries in this dictionary.
     * @param postingsInputType The type of postings input to use.
     * @param part The partition with which this dictionary is associated.
     * @throws java.io.IOException if there is any error opening the dictionary
     */
    public DiskDictionary(EntryFactory factory,
                          NameDecoder decoder, RandomAccessFile dictFile,
                          RandomAccessFile[] postFiles,
                          PostingsInputType postingsInputType,
                          Partition part) throws java.io.IOException {
        this(factory, decoder, dictFile, postFiles, postingsInputType,
             BufferType.NIOFILEBUFFER,
             256, 2048, 1024, 1024, 1024, part);
    }

    /**
     * Creates a disk dictionary that we can use for querying.
     * @param factory a factory for entries in this dictionary.
     * @param nameBufferSize the size of the buffer (in bytes) to use for the entry names
     * @param offsetsBufferSize the size of the buffer (in bytes) to use for the entry name offsets
     * @param infoBufferSize the size of the buffer (in bytes) to use for the entry information
     * @param postingsInputType the type of postings input to use.
     * @param fileBufferType the type of buffers to use for holding the dictionary data
     * @param infoOffsetsBufferSize the size of the buffer (in bytes) to use for the entry information offsets
     * @param decoder A decoder for the names in this dictionary.
     * @param dictFile The file containing the dictionary.
     * @param postFiles The files containing the postings associated with
     * the entries in this dictionary.
     * @param cacheSize The number of entries to use in the name and
     * position caches.
     * @param part The partition with which this dictionary is associated.
     * @throws java.io.IOException if there is any error opening the dictionary
     */
    public DiskDictionary(EntryFactory factory,
                          NameDecoder decoder, RandomAccessFile dictFile,
                          RandomAccessFile[] postFiles,
                          PostingsInputType postingsInputType,
                          BufferType fileBufferType,
                          int cacheSize,
                          int nameBufferSize, int offsetsBufferSize,
                          int infoBufferSize,
                          int infoOffsetsBufferSize, Partition part) throws
            java.io.IOException {
        this.factory = factory;
        this.dictFile = dictFile;
        this.postFiles = postFiles;
        this.decoder = decoder;
        this.part = part;
        this.fileBufferType = fileBufferType;
        
        //
        // Read the header.
        dh = new DictionaryHeader(dictFile);

        if(postFiles != null) {
            this.postIn = new PostingsInput[postFiles.length];
            //
            // Create the postings inputs.
            for(int i = 0; i < this.postIn.length; i++) {
                switch(postingsInputType) {
                    case CHANNEL_FULL_POST:
                        postIn[i] =
                                new ChannelPostingsInput(
                                postFiles[i].getChannel(),
                                                         true);
                        break;
                    case CHANNEL_PART_POST:
                        postIn[i] =
                                new ChannelPostingsInput(
                                postFiles[i].getChannel(),
                                                         false);
                        break;
                    case FILE_FULL_POST:
                        postIn[i] =
                                new FilePostingsInput(postFiles[i], true);
                        break;
                    case FILE_PART_POST:
                        postIn[i] =
                                new FilePostingsInput(postFiles[i], false);
                        break;
                }
            }
        }

        if(dh.idToPosnSize > 0) {
            idToPosn =
                    new NIOFileReadableBuffer(dictFile, dh.idToPosnPos,
                                              dh.idToPosnSize);
        }

        setUpBuffers(nameBufferSize, offsetsBufferSize, infoBufferSize,
                     infoOffsetsBufferSize);
        bst = new BinarySearchTree(cacheSize);
    }

    public DictionaryHeader getHeader() {
        return dh;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    protected void setUpBuffers(int nameBufferSize, int offsetsBufferSize,
                                int infoBufferSize, int infoOffsetsBufferSize)
            throws java.io.IOException {

        switch(fileBufferType) {
            case FILEBUFFER:
                names = new FileReadableBuffer(dictFile, dh.namesPos,
                                               dh.namesSize,
                                               nameBufferSize);
                nameOffsets =
                        new FileReadableBuffer(dictFile, dh.nameOffsetsPos,
                                               dh.nameOffsetsSize,
                                               offsetsBufferSize);
                entryInfo =
                        new FileReadableBuffer(dictFile, dh.entryInfoPos,
                                               dh.entryInfoSize, infoBufferSize);
                entryInfoOffsets =
                        new FileReadableBuffer(dictFile, dh.entryInfoOffsetsPos,
                                               dh.entryInfoOffsetsSize,
                                               infoOffsetsBufferSize);
                break;
            case NIOFILEBUFFER:
                names = new NIOFileReadableBuffer(dictFile, dh.namesPos,
                                                  dh.namesSize,
                                                  nameBufferSize);
                nameOffsets =
                        new NIOFileReadableBuffer(dictFile, dh.nameOffsetsPos,
                                                  dh.nameOffsetsSize,
                                                  offsetsBufferSize);

                entryInfo =
                        new NIOFileReadableBuffer(dictFile, dh.entryInfoPos,
                                                  dh.entryInfoSize,
                                                  infoBufferSize);
                entryInfoOffsets =
                        new NIOFileReadableBuffer(dictFile,
                                                  dh.entryInfoOffsetsPos,
                                                  dh.entryInfoOffsetsSize,
                                                  infoOffsetsBufferSize);
                break;
        }


    }

    /**
     * Gets the maximum ID in the dictionary.
     * @return the maximum ID in the dictionary
     */
    public int getMaxID() {
        return dh.maxEntryID;
    }

    /**
     * Puts a entry into the dictionary.  For disk-based dictionaries, this
     * will always return null, since those dictionaries are static.
     * @param name the name of the entry to put in the dictionary
     * @return <code>null</code>
     */
    public IndexEntry put(Object name) {
        return null;
    }

    private LookupState getLookupState() {
        WeakHashMap<DiskDictionary, LookupState> map = threadLookupStates.get();
        LookupState lus = map.get(this);
        if(lus == null) {
            lus = new LookupState(this);
            map.put(this, lus);
        }
        return lus;
    }

    /**
     * Gets a entry from the dictionary, given the name for the entry.
     *
     * @param name The name of the entry to get.
     * @return The entry associated with the name, or <code>null</code> if
     * the name doesn't appear in the dictionary.
     */
    public QueryEntry get(N name) {
        //
        // Perform the get using an existing lookup state for this thread (or
        // create a lookup state if there isn't one).
        return get(name, getLookupState());
    }

    /**
     * Gets a entry from the dictionary, given the name for the entry.
     *
     * @param name The name of the entry to get.
     * @param lus a lookup state for this dictionary.  A lookup state can be
     * re-used when doing multiple lookups to save time.  If this parameter is
     * <code>null</code>, a lookup state will be generated for each lookup.
     * @return The entry associated with the name, or <code>null</code> if
     * the name doesn't appear in the dictionary.
     */
    protected QueryEntry get(N name, LookupState lus) {

        if(lus == null) {
            lus = new LookupState(this);
        }

        lus.qs.dictLookups++;
        lus.qs.dictLookupW.start();

        //
        // Check our cache first.
        QueryEntry ret = null;

        //
        // Finding a entry by name is a two-step process.  First, the
        // position of the entry is determined by performing a binary
        // search of the names in this block.  Once the position is
        // found (or not found if the entry isn't present), the entry
        // is actually filled in using the the find method that takes a
        // entry id.
        //
        // Find the position (if any) of this entry:
        int pos = findPos(name, lus);
        if((pos >= 0) || (pos < dh.size)) {
            //
            // The entry exists in the dictionary.  Look it up by position.
            ret = find(pos, lus);
        }

        lus.qs.dictLookupW.stop();
        return ret;
    }

    /**
     * Gets a entry from the dictionary, given the ID for the entry.
     *
     * @param id the ID to find.
     * @return The block, or <code>null</code> if the ID doesn't occur in
     * our dictionary.
     */
    public QueryEntry getByID(int id) {
        return get(id, getLookupState());
    }

    /**
     * Gets a entry from the dictionary, given the ID for the entry.
     *
     * @param id the ID to find.
     * @param lus the current lookup state
     * @return The block, or <code>null</code> if the ID doesn't occur in
     * our dictionary.
     */
    protected QueryEntry get(int id, LookupState lus) {

        //
        // We need to map from the given ID to a position in the
        // dictionary.  If we have a map explicitly for this purpose, then
        // use that map, otherwise the position must be one less than the
        // ID.
        int posn;
        if(lus.localIDToPosn != null) {
            posn = lus.localIDToPosn.byteDecode(id * dh.idToPosnBytes,
                                                dh.idToPosnBytes);
        } else {
            posn = id - 1;
        }

        return find(posn, lus);
    }

    /**
     * Determines the position at which a entry falls.  If the entry is not
     * found, a number representing the position at which the entry would
     * have been found is returned, as described below.
     *
     * @param key the name of the entry to find
     * @param lus a lookup state that carries around copies of the buffers holding
     * the dictionary data
     * @return the position of the entry, if the entry is found; otherwise,
     * <code>(-(location) -1)</code>.  <code>location</code> is defined
     * as the location of the first entry in the block "greater than" the
     * given entry.  If all entries are "less than" the given entry, the
     * size of this block will be returned.  Note that this guarantees
     * that the return value will be >= 0 if and only if the given entry
     * is found in the block.
     */
    protected int findPos(N key, LookupState lus) {
        return findPos(key, lus, false);
    }

    /**
     * Determines the position within this block at which a entry falls.
     * If the entry is not found, a number representing the position at
     * which the entry would have been found is returned, as described
     * below.
     * @return the position of the entry, if the entry is found; otherwise,
     * <code>(-(location) -1)</code>.  <code>location</code> is defined
     * as the location of the first entry in the block "greater than" the
     * given entry.  If all entries are "less than" the given entry, the
     * size of this block will be returned.  Note that this guarantees
     * that the return value will be >= 0 if and only if the given entry
     * is found in the block.
     * @param key the name of the entry to find
     * @param lus a lookup state variable that contains local copies of the
     * dictionary's buffers
     * @param partial if true, treat key as a stem and return as soon
     * as a partial match (one that begins with the stem) is found
     */
    protected int findPos(N key, LookupState lus, boolean partial) {

        //
        // Perform a binary search of the entries that are found at
        // each whole-entry offset.  The entry could be found as a
        // whole entry, or more likely this loop will narrow down which
        // compressed group it is a part of, then we'll iterate through
        // the compressed entries to see if the entry exists.
        //
        // We'll start ou with the in-memory BST.
        BinarySearchTree.Node n = bst.find(key);
        if(n == null) {
            return -1;
        }
        int l = n.lower;
        int u = n.upper;
        int cmp = 0;
        int mid = 0;
        while(l <= u) {

            mid = (l + u) / 2;
            N compare = getUncompressedName(mid, lus);


            //
            // If we're looking for partial matches, check
            // startsWith for the current entry before deciding
            // to keep looking
            if(partial && decoder.startsWith(key, compare)) {

                // This is good enough, although it may not
                // be the first that starts with the key
                return mid * 4;
            }

            cmp = ((Comparable) key).compareTo(compare);

            if(cmp < 0) {
                u = mid - 1;
            } else if(cmp > 0) {
                l = mid + 1;
            } else {

                //
                // Yowsa! We found an exact match.  Since we've been
                // looking only at whole entries, the position is the
                // position of this whole entry multiplied by the
                // number of entries in one compressed group (4).
                return mid * 4;
            }
        }

        //
        // If we get this far, then the key entry wasn't an
        // exact match for any of the whole/uncompressed entries.
        // The value of cmp tells us which block this entry will
        // be in.  If cmp > 0 then the entry is in this block.
        // If cmp < 0 then the entry is in the previous block.
        if(cmp < 0) {
            if(mid == 0) {
                //
                // There's no block before us, and therefore no match for
                // the key entry.
                return -1; // this is ((0 - 0) - 1)
            }
            mid--;
        }


        //
        // Walk this block of entries.  Note we're re-decoding one
        // entry, but we won't worry about this until absolutely necessary.
        int offset =
                lus.localNameOffsets.byteDecode(dh.nameOffsetsBytes * mid,
                                                dh.nameOffsetsBytes);

        lus.localNames.position(offset);

        //
        // The name of the previous entry.
        N prev = null;
        for(int i = 0, index = mid * 4 + i; i < 4 && index < dh.size; i++, index++) {

            N compare = decoder.decodeName(prev, lus.localNames);

            //
            // If we're looking for partial matches, check
            // startsWith for the current entry before deciding
            // to keep looking
            if(partial && decoder.startsWith(key, compare)) {
                // This is good enough, although it may not
                // be the first that starts with the key
                return index;
            }

            cmp = ((Comparable) key).compareTo(compare);

            if(cmp == 0) {
                //
                // We found the entry.
                return index;
            } else if(cmp < 0) {
                //
                // The key is greater than the entry in the block.  We're
                // done.
                return (0 - index) - 1;
            }
            prev = compare;
        }

        //
        // If we reached the end, return nEntries.  Otherwise, the entry
        // would come before the next uncompressed entry
        if(((mid * 4) + 4) >= dh.size) {
            return dh.size;
        } else {
            return (0 - ((mid * 4) + 4)) - 1;
        }
    }

    /**
     * Gets an uncompressed name from the dictionary.  Used during binary
     * searches.
     * @param pos the position of the uncompressed name to get.
     * @param lus the state to use to do the decoding
     * @return the name at the given position.
     */
    private N getUncompressedName(int pos, LookupState lus) {
        //
        // Get the offset of the uncompressed entry.
        int offset =
                lus.localNameOffsets.byteDecode(
                dh.nameOffsetsBytes * pos, dh.nameOffsetsBytes);

        //
        // Get the name of the entry at that position.
        lus.localNames.position(offset);
        return decoder.decodeName(null, lus.localNames);
    }

    /**
     * Finds the entry at the given position in this dictionary.
     * @return The entry at the given position, or <code>null</code> if
     * there is no entry at that position in this block.
     * @param lus the current state of the lookup
     */
    protected QueryEntry find(int posn, LookupState lus) {

        //
        // Bounds check.
        if(posn < 0 || posn >= dh.size) {
            return null;
        }

        //
        // Where's the nearest uncompressed entry, and how far from
        // there do we need to venture to get the entry at this position?
        int ui = posn / 4;
        int n = posn % 4;
        N localName = getUncompressedName(ui, lus);

        //
        // Walk ahead and get the name of the entry we want.
        for(int i = 0; i < n; i++) {
            localName = decoder.decodeName(localName, lus.localNames);
        }

        //
        // Get an entry and return it.
        return newEntry(localName, posn, lus, postIn);
    }

    /**
     * Creates a new entry and fills in its information.
     *
     * @param name The name of the entry to be filled.
     * @param posn The position of this entry in the dictionary.
     * @param lus A lookup state containing copies of the dictionary's data
     * @param postIn The postings channels to use for reading postings.
     * @return The filled entry.
     */
    protected QueryEntry newEntry(N name, int posn,
                                  LookupState lus,
                                  PostingsInput[] postIn) {

        //
        // Position the info buffer for decoding the info for this entry.
        lus.localInfo.position(lus.localInfoOffsets.byteDecode(posn *
                dh.entryInfoOffsetsBytes, dh.entryInfoOffsetsBytes));
        QueryEntry ret = factory.getQueryEntry(name, lus.localInfo);
        ret.setDictionary(this);
        ret.setPostingsInput(postIn);

        //
        // Get the offset in the entry information buffer where we can
        // find the information for this entry.
        return ret;
    }

    /**
     * Gets a set of all the entries with the given stem
     *
     * @param biDict The bigram dictionary to use for the lookup.
     * @param term the stem to look for
     * @param caseSensitive If <code>true</code>, then we should only return
     * entries that match the case of the pattern.
     * @param minLen The minimum length that we'll consider for a stem.
     * @param matchCutOff the cutoff score for matching the variants
     * to the original entry
     * @param maxEntries the maximum number of entries to provide; returns
     * all entries if maxEntries is non-positive
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     * @return an in-order array of the entries beginning with the stem
     */
    public QueryEntry[] getStemMatches(DiskBiGramDictionary biDict,
                                       String term, boolean caseSensitive,
                                       int minLen, float matchCutOff,
                                       int maxEntries, long timeLimit) {
        //
        // To stem the term, take half of the supplied term, or
        // minLen characters, whichever is larger
        int stemLen = Math.max(term.length() / 2, minLen);
        if(stemLen > term.length()) {
            //
            // A stem longer than the term was required
            return null;
        }

        String stem = term.substring(0, stemLen);

        //
        // Get the entries that start with the given stem.
        QueryEntry[] candidates =
                getSubstring(biDict, stem, caseSensitive, true, false,
                             maxEntries, timeLimit);

        //
        // Either there were no candidates or we timed out.
        if(candidates == null || candidates.length == 0) {
            return null;
        }

        List<QueryEntry> ret =
                new ArrayList<QueryEntry>();
        for(int i = 0; i < candidates.length; i++) {
            String s = (String) candidates[i].getName();
            float shr =
                    StringNameHandler.getShared(term, s);
            float diff =
                    shr / (float) Math.max(term.length(),
                                           s.length());

            if(diff >= matchCutOff) {
                ret.add(candidates[i]);
            }
        }

        return ret.toArray(new QueryEntry[0]);
    }

    /**
     * Gets the entries matching the given pattern from the given
     * dictionary.  This can be used by anyone with a dictionary and some
     * matching bigrams.
     *
     * @param biDict The bigrams to use to do the candidate entry
     * selection.
     * @param pat The pattern to match entries against.
     * @param caseSensitive If <code>true</code>, then we should only return
     * entries that match the case of the pattern.
     * @param maxEntries The maximum number of entries to return.  If zero
     * or negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     * @return An array of <code>Entry</code> objects containing the
     * matching entries, or null if there are not such entries, or an array
     * of length zero if the operation timed out before any entries could
     * be matched
     */
    public QueryEntry[] getMatching(DiskBiGramDictionary biDict,
                                    String pat, boolean caseSensitive,
                                    int maxEntries, long timeLimit) {

        //
        // There's two stages to getting entries that match a wildcard
        // pattern.  First, we'll use the bigram dictionary to find
        // the ids of the entries that contain all the bigrams in the
        // pattern.  These are the candidate entries.  Next, we iterate
        // through the candidates to check each one to see if it
        // matches the pattern.  Results that match will get returned.
        // If there was a time limit given, start counting...
        // NOTE: is this sufficient?  if the limit is set to less
        // than the time it takes to retrieve the bigrams and
        // intersect the entry sets, we'll go over. do we actually
        // need to interrupt the thread in this case?  or check
        // the timer lower down in the code?
        QueryTimerTask qtt = new QueryTimerTask();
        if(timeLimit > 0) {
            part.getPartitionManager().getQueryTimer().schedule(qtt, timeLimit);
        }

        //
        // First, get the matching entry IDs from the bigram dictionary.
        int[] entryIds = biDict.getMatching(pat);

        if(qtt.timedOut) {
            // Operation timed out
            return new QueryEntry[0];
        }

        if(entryIds == null) {
            // There's nothing that could match
            return null;
        }

        ArrayList<QueryEntry> res = new ArrayList<QueryEntry>();

        //
        // Get an array to save generating it over and over.
        char[] patArray = pat.toCharArray();
        if(!caseSensitive) {
            Util.toLowerCase(patArray);
        }

        //
        // Now check the entry IDs.
        if(entryIds.length == 0) {
            // There's no bigrams or unigrams to narrow down by.
            // Try everything:
            Iterator entryIt = iterator();
            while((entryIt.hasNext()) && (!qtt.timedOut) &&
                    ((maxEntries <= 0) || (res.size() < maxEntries))) {
                QueryEntry curr = (QueryEntry) entryIt.next();
                if(Util.match(patArray, curr.toString().toCharArray(),
                              caseSensitive)) {
                    res.add(curr);
                }
            }
        } else {
            //
            // Now look up each entry and see if it matches the
            // result.
            for(int i = 0; (i < entryIds.length) && (entryIds[i] != 0) &&
                    (!qtt.timedOut) &&
                    ((maxEntries <= 0) || (res.size() < maxEntries)); i++) {
                QueryEntry curr = getByID(entryIds[i]);
                if(Util.match(patArray, curr.toString().toCharArray(),
                              caseSensitive)) {
                    res.add(curr);
                }
            }
        }

        //
        // Stop the timer if it was still running, and reset the
        // thread-local boolean in case we ever re-use this thread
        return res.toArray(new QueryEntry[0]);
    }

    /**
     * Gets the list of possible spelling corrections, based on terms
     * in the index, for the string that is passed in.
     *
     * @param biDict The bigrams to use to do the candidate entry
     * selection.
     * @param word the word to find alternates for
     * @param caseSensitive If <code>true</code>, then we should only return
     * entries that match the case of the pattern.
     * @param maxEntries The maximum number of entries to return.  If zero
     * or negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     * @return An array of <code>Entry</code> objects containing the
     * matching entries, or null if there are not such entries, or an array
     * of length zero if the operation timed out before any entries could
     * be matched
     */
    public QueryEntry[] getSpellingVariants(DiskBiGramDictionary biDict,
                                            String word, boolean caseSensitive,
                                            int maxEntries, long timeLimit) {

        //
        // We'll find spelling variants in two steps.  First, use the
        // bigram dictionary to find all the words that are made up of
        // some or all of the bigrams in the given word.  This is done
        // by using a ScoredQuickOr that should weight the terms with
        // the most overlap the highest.  Second, we'll combine the
        // terms's collection weight  with the levenshtein edit distance
        // to get the final ordering of the suggestions.
        // If there was a time limit given, start counting...
        // NOTE: is this sufficient?  if the limit is set to less
        // than the time it takes to retrieve the bigrams and
        // intersect the entry sets, we'll go over. do we actually
        // need to interrupt the thread in this case?  or check
        // the timer lower down in the code?
        QueryTimerTask qtt = new QueryTimerTask();
        if(timeLimit > 0) {
            part.getPartitionManager().getQueryTimer().schedule(qtt, timeLimit);
        }

        //
        // First, get the matching entry IDs from the bigram dictionary.
        int[] entryIds = biDict.getAllVariants(word, false);

        if(qtt.timedOut) {
            // Operation timed out
            return new QueryEntry[0];
        }

        if(entryIds == null) {
            // There's nothing that could match
            return null;
        }

        List<Entry> res = new ArrayList<Entry>();

        //
        // Get an array to save generating it over and over.
        if(!caseSensitive) {
            word = word.toLowerCase();
        }
        
        //
        // Now pile up the potential matches and get the edit distances
        // for them.
        final HashMap<String, Double> dist =
                new HashMap<String, Double>();
        if(entryIds.length == 0) {
            return new QueryEntry[0];
        } else {
            //
            // Now look up each entry to see if it is a possible match
            for(int i = 0; (i < entryIds.length) && (entryIds[i] != 0) &&
                    (!qtt.timedOut); i++) {
                Entry curr = getByID(entryIds[i]);
                String name = (String) curr.getName();
                res.add(curr);
                double d = Util.levenshteinDistance(word, name);
                if(d == 0) {
                    d = 0.5;
                }
                d = (d * 2) / Math.log(curr.getN());
                dist.put(name, d);
            }
        }

        Comparator<Entry> levComp =
                new Comparator<Entry>() {

                    public int compare(Entry e1, Entry e2) {
                        double d1 = dist.get(e1.getName());
                        double d2 = dist.get(e2.getName());
                        double cmp = d1 - d2;
                        if(cmp == 0) {
                            return 0;
                        } else if(cmp < 0) {
                            return -1;
                        }
                        return 1;
                    }

                    public boolean equals(Object o) {
                        if(o == this) {
                            return true;
                        }
                        return false;
                    }
                };
        Collections.sort(res, levComp);
        if(maxEntries < res.size()) {
            res = res.subList(0, maxEntries);
        }

        //
        // Stop the timer if it was still running, and reset the
        // thread-local boolean in case we ever re-use this thread
        return res.toArray(new QueryEntry[0]);
    }

    /**
     * Gets the entries matching the given pattern from the given
     * dictionary.   This can be used by anyone with a dictionary and some
     * matching bigrams.
     *
     * @param biDict A dictionary of bigrams built from the entries that are
     * in this dictionary.
     * @param substring The substring to find in the entries.
     * @param caseSensitive If <code>true</code>, then we should look for
     * matches that match the case of the letters in the substring.
     * @param starts If <code>true</code>, the value must start with the
     * given substring.
     * @param ends If <code>true</code>, the value must end with the given
     * substring.
     * @param maxEntries The maximum number of entries to return.  If zero or
     * negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     * @return An array of <code>Entry</code> objects containing the
     * matching entries, or null if there are not such entries, or an
     * array of length zero if the operation timed out before any
     * entries could be matched
     */
    public QueryEntry[] getSubstring(DiskBiGramDictionary biDict,
                                     String substring, boolean caseSensitive,
                                     boolean starts,
                                     boolean ends, int maxEntries,
                                     long timeLimit) {

        //
        // Get a set of candidate strings. We'll set up a timer first...
        QueryTimerTask qtt = new QueryTimerTask();
        if(timeLimit > 0) {
            part.getPartitionManager().getQueryTimer().schedule(qtt, timeLimit);
        }

        //
        // If we're not looking for case sensitive substrings, then
        // downcase the substring now.
        if(!caseSensitive) {
            substring = CharUtils.toLowerCase(substring);
        }

        //
        // First, get the matching entry IDs from the bigram dictionary.
        int[] entryIds = biDict.getMatching(substring, starts, ends);

        if(qtt.timedOut) {
            // Operation timed out
            logger.warning("Lookup timed out");
            return new QueryEntry[0];
        }

        if(entryIds == null) {
            // There's nothing that could match
            return null;
        }

        if(!caseSensitive) {
            substring = CharUtils.toLowerCase(substring);
        }

        ArrayList<QueryEntry> res =
                new ArrayList<QueryEntry>();

        //
        // Now look up each entry and see if it contains the given
        // substring.
        for(int i = 0; (i < entryIds.length) && (entryIds[i] != 0) &&
                (!qtt.timedOut) &&
                ((maxEntries <= 0) || (res.size() < maxEntries)); i++) {
            QueryEntry curr = getByID(entryIds[i]);
            String name = curr.getName().toString();
            if(!caseSensitive) {
                name = CharUtils.toLowerCase(name);
            }
            int pos = name.indexOf(substring);

            //
            // If we found the substring and it meets any start/end
            // criteria, then we can add it to our result set.
            if(pos >= 0 && (!starts || pos == 0) &&
                    (!ends ||
                    (pos + substring.length() ==
                    ((String) curr.getName()).length()))) {
                res.add(curr);
            }
        }

        //
        // Stop the timer if it was still running, and reset the
        // thread-local boolean in case we ever re-use this thread
        return res.toArray(new QueryEntry[0]);
    }

    /**
     * Gets the size of the dictionary.
     *
     * @return the number of entries in the dictionary.
     */
    public int size() {
        return dh.size;
    }

    /**
     * Gets the partition to which this dictionary belongs.
     * @return the partition
     */
    public Partition getPartition() {
        return part;
    }

    /**
     * Sets the partition with which this dictionary is associated
     * @param p the partition with which the dictionary is associated
     */
    public void setPartition(Partition p) {
        part = p;
    }

    /**
     * Gets a buffered version of the postings inputs for this
     * dictionary so that we can stream a bit better through the
     * postings when doing, for example, dictionary merges.
     *
     * @return a buffered set of postings inputs for this dictionary
     */
    protected PostingsInput[] getBufferedInputs() {
        return getBufferedInputs(131072);
    }

    /**
     * Gets a buffered version of the postings inputs for this
     * dictionary so that we can stream a bit better through the
     * postings when doing, for example, dictionary merges.
     *
     * @param buffSize the size of the buffer to use
     * @return a buffered set of postings inputs for this dictionary
     */
    protected PostingsInput[] getBufferedInputs(int buffSize) {
        if(postIn == null) {
            return null;
        }
        PostingsInput[] buffChans =
                new PostingsInput[postIn.length];
        for(int i = 0; i < postFiles.length; i++) {
            try {
                buffChans[i] =
                        new StreamPostingsInput(postFiles[i],
                                                dh.postStart[i], buffSize);
            } catch(java.io.IOException ioe) {
                logger.log(Level.SEVERE,
                           "Error creating postings stream for iterator", ioe);
                buffChans[i] = postIn[i];
            }
        }
        return buffChans;
    }

    /**
     * Gets an iterator for this dictionary.  Just gets an iterator for the
     * block.
     * @return an iterator for the dictionary.  The elements of the iterator implement the
     * <CODE>Map.Entry</CODE> interface
     */
    public DictionaryIterator iterator() {
        return iterator(null, false, null, false);
    }

    public LightIterator literator() {
        return new LightDiskDictionaryIterator();
    }

    /**
     * Creates an iterator that starts iterating at
     * the specified entry, or, if the entry does not exist in the
     * block, starts iterating at the first entry greater than the
     * provided entry.
     *
     * @param startEntry the name of the entry to start iterating at
     * @return the iterator
     * @param includeStart If <code>true</code>, then the iterator
     * will return startEntry, if it is in the dictionary.
     */
    public DictionaryIterator iterator(N startEntry, boolean includeStart) {
        return iterator(startEntry, includeStart, null, false);
    }

    /**
     * Creates an iterator that starts iterating at
     * the specified <code>startEntry</code> and stops iterating at
     * the specified <code>stopEntry</code>.  If either entry does not
     * exist in the block, the first entry greater than the entry provided
     * will be used.
     *
     * @param startEntry the name of the entry to start iterating at, or
     * null to start at the first entry
     * @param includeStart If <code>true</code>, then the iterator
     * will return startEntry, if it is in the dictionary.
     * @param stopEntry the name of the entry to stop iterating at, or
     * null to stop after the last entry
     * @param includeStop if <code>true</code> and
     * <code>stopEntry</code> is non-<code>null</code>, then the
     * iterator will return <code>stopEntry</code>, if it is in the
     * dictionary.
     * @return the iterator
     */
    public DictionaryIterator iterator(N startEntry, boolean includeStart,
                                       N stopEntry, boolean includeStop) {
        return new DiskDictionaryIterator(startEntry, includeStart, stopEntry,
                                          includeStop);
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
    public DictionaryIterator iterator(int begin, int end) {
        return new DiskDictionaryIterator(begin, end);
    }

    /**
     * Merges a number of dictionaries into a single dictionary.  This
     * method will be responsible for merging the postings lists associated
     * with the entries and dumping the new dictionary and postings to the
     * given channels.
     *
     * @param indexDir the directory where the merged dictionaries will be written.
     * This is used for temporary file storage during the merge.
     * @param encoder An encoder for the names in this dictionary.
     * @param dicts The dictionaries to merge.
     * @param mappers A set of entry mappers that will be applied to the
     * dictionaries as entries are considered for the merge.  If this
     * parameter is <code>null</code>, then the entries in the merged
     * dictionary will be renumbered in order of increasing name.
     * @param starts The starting IDs for the new partition.
     * @param postIDMaps Maps from old to new IDs for the IDs in our
     * postings.
     * @param mDictFile The file where the merged dictionary will be
     * written.
     * @param postOut The output where the postings for the merged
     * dictionary will be written
     * @param appendPostings true if postings should be appended rather than
     *        merged
     * @return A set of maps from the old entry IDs to the entry IDs in the
     * merged dictionary.  The element [0][0] of this matrix contains the
     * max entry id in the new dict.
     * @throws java.io.IOException when there is an error during the merge.
     */
    public static int[][] merge(File indexDir,
            NameEncoder encoder,
                         DiskDictionary[] dicts,
                         EntryMapper[] mappers,
                         int[] starts,
                         int[][] postIDMaps, 
                         RandomAccessFile mDictFile,
                         PostingsOutput[] postOut, boolean appendPostings)
            throws java.io.IOException {
        
            //
        // We'll keep a map from old to new IDs for each of the
        // dictionaries and a heap to manage the merge.
        boolean keepIDToPosn = false;
        int[][] idMaps = new int[dicts.length][];
        PriorityQueue<HE> h = new PriorityQueue<HE>();
        DiskDictionary merger = null;
        for(int i = 0; i < dicts.length; i++) {
            DiskDictionary dd = dicts[i];
            if(dd == null) {
                continue;
            }

            if(merger == null) {
                merger = dd;
            }

            if(dd.idToPosn != null) {
                keepIDToPosn = true;
            }
            idMaps[i] = new int[dd.dh.maxEntryID + 1];

            //
            // Make an entry in the heap for this dictionary.
            HE he = new HE(dd, i, mappers == null ? null : mappers[i]);
            if(he.next()) {
                h.offer(he);
            }
        }

        //
        // Make more room if we're merging the doc dict since the idMap
        // will be used for another purpose
        if(mappers != null) {
            idMaps[0] = new int[postIDMaps[0][0] + 1];
        }

        //
        // We'll need a writer for the merged dictionary.
        DictionaryWriter dw =
                new DictionaryWriter(indexDir, encoder,
                                     postOut.length,
                                     keepIDToPosn
                ? MemoryDictionary.Renumber.NONE
                : MemoryDictionary.Renumber.RENUMBER);

        int[] mapped = new int[idMaps.length];

        //
        // Fake the starts array.
        if(starts == null) {
            starts = new int[dicts.length];
            Arrays.fill(starts, 1);
        }

        //
        // The number of entries in the new dictionary.
        while(h.size() > 0) {

            HE top = h.peek();

            //
            // If we have entry mappers, then get the ID from the entry at
            // the top of the heap, which will have been remapped.
            // Otherwise, just set the number in order.
            int newid = mappers != null ? top.curr.getID() : (dw.dh.size + 1);

            //
            // Make a new entry for the merged data.
            IndexEntry me = merger.factory.getIndexEntry(top.curr.getName(), newid);

            //
            // We need to keep track of any mappings that we've made for the 
            // dictionary IDs while processing this entry, because in the end
            // we may not acutally end up writing this entry to the dictionary.
            // In that instance, we'll need to replace the mappings that we made
            // before we knew this with -1, indicating that the given entry 
            // doesn't map to anything.
            Arrays.fill(mapped, 0);
            
            //
            // Get all the equal entries, merging the postings as we go.
            while(top != null && top.curr.getName().equals(me.getName())) {

                top = h.poll();

                //
                // Give a subclass a crack at the merged entry and the
                // entry at the top of the heap.
                customSetup(me, top.curr, starts[top.index],
                            postIDMaps[top.index]);

                //
                // Get the top of the heap and merge it with the entry we're
                // building.
                top.curr.readPostings();
                if(appendPostings) {
                    me.append(top.curr, starts[top.index], postIDMaps[top.index]);
                } else {
                    me.merge(top.curr, postIDMaps[top.index]);
                }

                //
                // If we have a mapper, then IDs are already mapped.  We don't
                // need to return an old->new mapping in idMaps.  Instead, use
                // the 0th element in idMaps to map from individual docs to
                // merged docs.  Note that this should only happen where
                // duplicate doc keys are allowed - namely the cluster
                // partition, and specifically not the real inverted file.
                if(mappers != null) {
                    idMaps[0][top.curr.getID()] = me.getID();
                } else {
                    //
                    // Map the old, original ID for the top entry to the new
                    // ID.
                    idMaps[top.index][top.origID] = me.getID();
                    mapped[top.index] = top.origID;
                }

                //
                // If we advance the top and hit another element, then
                // adjust the top of the heap, which has now changed.  If
                // we hit the end of the iterator, then we should remove it
                // from the heap.
                if(top.next()) {
                    h.offer(top);
                }

                //
                // Peek at the heap for the next go-round.
                top = h.peek();
            }


            //
            // Write the postings for the newly merged entry.
            if(me.writePostings(postOut, null) == true) {

                //
                // Add the new entry to the dictionary that we're building.
                try {
                    dw.write(me);
                } catch(java.lang.ArithmeticException ame) {
                    logger.severe(String.format(
                            "Arithmetic exception encoding postings for entry: %s", me.
                            getName()));
                    throw (ame);
                }
            } else {
                //
                // Remember what we said about not writing the entry to the 
                // merged dictionary?  This is where we notice that we 
                // don't have any postings for a dictionary entry and that we 
                // therefore won't write it to the dictionary.  So, we need
                // to remove whatever mappings we made.  Life is hard.
                for(int i = 0; i < mapped.length; i++) {
                    if(mapped[i] > 0) {
                        idMaps[i][mapped[i]] = -1;
                    }
                }
            }

            if(dw.dh.size % 10000 == 0) {
                logger.fine(String.format(" Merged %d entries", dw.dh.size));
            }
        }

        for(int i = 0; i < postOut.length; i++) {
            postOut[i].flush();
            dw.dh.postEnd[i] = postOut[i].position();
        }

        idMaps[0][0] = dw.dh.maxEntryID;

        //
        // Finish of the writing of the entries to the merged dictionary.
        dw.finish(mDictFile);

        logger.fine(String.format("Merged dictionary has %d entries", dw.dh.size));

        //
        // Return the maps from old to new IDs.
        return idMaps;
    }

    /**
     * Rewrites this dictionary to the files passed in while remapping IDs in
     * the postings to the new IDs passed in.
     * 
     * @param entryFactory factory to create new entries
     * @param encoder name encoder to write new entries
     * @param partStats the partition stats for this partition
     * @param postMap a mapping from old postings ids to new ids
     * @param dictFile a file to write the new dictionary in
     * @param postOut a set of files to write the new postings in
     * @throws java.io.IOException if there is any error reading or writing dictionaries
     */
    public void remapPostings(IndexEntry entryFactory,
                              NameEncoder encoder,
                              int[] postMap,
                              RandomAccessFile dictFile,
                              PostingsOutput[] postOut) throws
            java.io.IOException {

        DictionaryWriter dw =
                new DictionaryWriter(part.getPartitionManager().getIndexDir(), encoder,
                                     postOut.length,
                                     MemoryDictionary.Renumber.NONE);

        //
        // Track what the highest ID is in the new dictionary
        int maxEntryId = 0;

        //
        // Iterate through the entries, rewriting them with mapped
        // postings IDs
        for(Iterator dictIt = iterator(); dictIt.hasNext();) {
            QueryEntry curr = (QueryEntry) dictIt.next();

            IndexEntry ent = factory.getIndexEntry(name, curr.getID());
            maxEntryId = Math.max(maxEntryId, ent.getID());

            curr.readPostings();

            //
            // Write the postings into the new entry, with the new postings IDs.
            //  We can't use append since we'll be giving it postings not
            //  necessarily in order.  Do a manual add like this:
            //
            // Get all the postings
            int[] postings = new int[curr.getN()];
            int i = 0;
            PostingsIterator pit =
                    curr.iterator(new PostingsIteratorFeatures());
            if(pit != null) {
                while(pit.next()) {
                    postings[i++] = postMap[pit.getID()];
                }

                //
                // Sort the postings
                Arrays.sort(postings);

                //
                // Re-write the postings
                for(int j = 0; j < postings.length; j++) {
                    Occurrence o = new Occurrence() {

                        protected int id;

                        @Override
                        public int getID() {
                            return id;
                        }

                        @Override
                        public void setID(int id) {
                            this.id = id;
                        }

                        @Override
                        public int getCount() {
                            return 1;
                        }

                        @Override
                        public void setCount(int count) {
                        }
                    };
                    o.setID(postings[j]);
                    ent.add(o);
                }

                //
                // bigrams?
                //
                // Write the postings for the newly merged entry.
                if(ent.writePostings(postOut, null) == true) {

                    //
                    // Write out the new entry
                    dw.write(ent);
                }
            }
        }

        //
        // Clear out the streams...
        for(int i = 0; i < postOut.length; i++) {
            postOut[i].flush();
            dw.dh.postEnd[i] = postOut[i].position();
        }

        //
        // Update the max entry id
        if(maxEntryId != dw.dh.size) {
            dw.dh.maxEntryID = maxEntryId;
        }

        //
        // Tell the writer to finish up
        dw.finish(dictFile);

        logger.info("Remapped postings");
    }

    /**
     * Do any custom setup required for merging one entry onto another.
     * This is intended for use by subclasses so that they don't need to
     * override the entire merge method, which is a stone PITA to get
     * right.
     *
     * At this level, this method does nothing.
     *
     * @param me the entry onto which we're merging postings.
     * @param e the entry which we are <em>about to merge</em> into the
     * merged entry.
     * @param start the new starting ID for documents for the partition from
     * which the entry we're going to merge are drawn.
     * @param postIDMap a map from old to new IDs for the postings that
     * we're about to merge.
     *
     */
    protected static void customSetup(IndexEntry me, QueryEntry e, int start,
                               int[] postIDMap) {
    }

    public String toString() {
        return String.format("%s (%s) %s size: %d bst nodes: %d",
                             part, getClass().getSimpleName(), name, size(),
                             bst.size);
    }

    /**
     * A class that will act as a heap entry for merging.
     */
    protected static class HE implements Comparable<HE> {

        /**
         * The iterator.
         */
        protected Iterator i;

        /**
         * The current entry.
         */
        protected QueryEntry curr;

        /**
         * The original ID of the current entry.
         */
        protected int origID;

        /**
         * A mapper for our entries.
         */
        protected EntryMapper mapper;

        /**
         * The index of the dictionary.
         */
        protected int index;

        public HE(DiskDictionary dd, int index, EntryMapper mapper) {
            i = dd.iterator();
            this.index = index;
            this.mapper = mapper;
        }

        protected boolean next() {

            while(i.hasNext()) {

                curr = (QueryEntry) i.next();

                //
                // The mapper may modify the ID, so we need to capture it
                // here!
                origID = curr.getID();

                //
                // If we don't have a entry mapper, we're done.
                if(mapper == null) {
                    return true;
                }

                //
                // Go ahead and map the entry.
                curr = (QueryEntry) mapper.map(curr);

                //
                // If this entry is not to appear in the merged dictionary,
                // then try the next entry.
                if(curr == null) {
                    continue;
                }
                return true;
            }
            return false;
        }

        public int compareTo(HE e) {
            int cmp = curr.compareTo(e.curr);
            return cmp == 0 ? index - e.index : cmp;
        }

        public String toString() {
            return curr.getName() + " " + curr.getID() + " " + index;
        }
    }

    /**
     * A class that can be used to encapsulate the dictionary state when doing
     * multiple lookups during querying.
     */
    public static class LookupState {

        ReadableBuffer localNames;

        ReadableBuffer localNameOffsets;

        ReadableBuffer localInfo;

        ReadableBuffer localInfoOffsets;

        ReadableBuffer localIDToPosn;

        QueryStats qs;

        public LookupState(DiskDictionary target) {
            localNames = target.names.duplicate();
            localNameOffsets = target.nameOffsets.duplicate();
            localInfo = target.entryInfo.duplicate();
            localInfoOffsets = target.entryInfoOffsets.duplicate();
            if(target.idToPosn != null) {
                localIDToPosn = target.idToPosn.duplicate();
            }
            qs = new QueryStats();
        }

        public void setQueryStats(QueryStats qs) {
            this.qs = qs;
        }

        public QueryStats getQueryStats() {
            return qs;
        }
    }

    /**
     * A class that can be used as an iterator for this block.
     */
    public class DiskDictionaryIterator implements DictionaryIterator,
            Comparable {

        /**
         * The estimated size of the results set for this iterator.
         */
        protected int estSize = -1;

        /**
         * The position in the dictionary.
         */
        protected int pos;

        /**
         * Our lookup state.
         */
        protected LookupState lus;

        /**
         * The name of the previous entry.
         */
        protected N prevName;

        /**
         * The current entry.
         */
        protected QueryEntry curr;

        /**
         * The position to start iterating from
         */
        protected int startPos;

        /**
         * The position to stop iterating at
         */
        protected int stopPos;

        /**
         * Buffered inputs for reading postings.
         */
        protected PostingsInput[] bufferedPostings;

        /**
         * A flag indicating whether we should try to ignore entries whose names
         * have not actually occurred in the indexed material.
         * This will allow us to iterate
         * through the entries in a dictionary where the names are strings and
         * the entries are cased and only show entry names that actually occurred
         * in the indexed material.
         */
        protected boolean actualOnly;

        /**
         * Whether our entries are cased.
         */
        protected boolean casedEntries;

        protected boolean returnCurr;

        private boolean unbufferedPostings;

        /**
         * Creates a DictionaryIterator that iterates over a range of
         * entries.
         *
         * @param startEntry the name of the entry to start iterating at,
         * or null to start at the first entry
         * @param includeStart if <code>true</code> and
         * <code>startEntry</code> is non-<code>null</code>, then the
         * iterator will return <code>startEntry</code>, if it is in the
         * dictionary.
         * @param stopEntry the name of the entry to stop iterating at,
         * or null to stop after the last entry.
         * @param includeStop if <code>true</code> and
         * <code>stopEntry</code> is non-<code>null</code>, then the
         * iterator will return <code>stopEntry</code>, if it is in the
         * dictionary.
         */
        public DiskDictionaryIterator(N startEntry, boolean includeStart,
                                      N stopEntry, boolean includeStop) {

            lus = new LookupState(DiskDictionary.this);
            pos = 0;
            startPos = 0;
            stopPos = dh.size;

            if(startEntry != null) {
                startPos = findPos(startEntry, lus);

                //
                // If startPos evaluated to a entry that is not
                // in this dictionary block, then start at the
                // first entry greater than startEntry
                if(startPos < 0) {
                    startPos = Math.abs(startPos + 1);
                } else {
                    if(!includeStart && startPos < dh.size) {
                        startPos++;
                    }
                }
            }

            if(stopEntry != null) {
                stopPos = findPos(stopEntry, lus);

                //
                // If stopPos evaluated to a entry that is not
                // in this dictionary block, then stop at the
                // first entry greater than stopEntry
                if(stopPos < 0) {
                    stopPos = Math.abs(stopPos + 1);
                } else {

                    //
                    // If we found the stop entry in the dictionary and
                    // we're supposed to include the stop position,
                    // then we need to increase our stop position by
                    // one in order to capture that last element.
                    if(includeStop && stopPos < dh.size) {
                        stopPos++;
                    }
                }
            }
            
            // Now position the buffer and seed the next() call:
            if(startPos != 0) {
                // Reposition to the start
                QueryEntry<N> temp = find(startPos - 1, lus);
                prevName = temp.getName();
            } else {
                lus.localNames.position(0);
            }

            pos = startPos;

            //
            // Do we have cased entries?
            bufferedPostings = getBufferedInputs();
        }

        /**
         * Gets an iterator for the dictionary that starts and stops at the
         * given indices in the dictionary.  This can be used when we want to
         * process pieces of a dictionary in different threads.
         *
         * @param begin the beginning index in the dictionary, counting from
         * 0.  If this value is less than zero it will be clamped to zero.
         * @param end the ending index in the dictionary, counting from 0.  If
         * this value is greater than the number of entries in the dictionary,
         * it will be limited to that number.
         */
        public DiskDictionaryIterator(int begin, int end) {
            lus = new LookupState(DiskDictionary.this);
            pos = 0;
            startPos = Math.max(0, begin);
            stopPos = Math.min(end, dh.size);

            lus.localNames.position(0);
            pos = startPos;

            bufferedPostings = getBufferedInputs();
        }

        /**
         * Modifies the iterator so that it only returns entries whose names have
         * actually occurred in the indexed material.  So, for example, if the word
         * <em>Dog</em> occurs in the indexed material, and is the only occurrence
         * of that word, then the entry <em>dog</em> in a cased dictionary would not
         * be returned if this method is called with a value of <code>true</code>.
         *
         * <p>
         *
         * Note that this option really only makes sense for dictionaries that
         * use cased entries.
         *
         * @param actualOnly if <code>true</code> only entries with names
         * that actually occurred in the indexed material will be returned.
         * If <code>false</code> all entries will be returned.
         */
        public void setActualOnly(boolean actualOnly) {
            this.actualOnly = actualOnly && casedEntries;
        }

        /**
         * Tells the iterator to not use the buffered postings that it may have.
         * This is useful in situations where we don't need the streaming
         * postings.
         * @param unbufferedPostings if <code>true</code> then the entries will
         * use unbuffered postings.
         */
        public void setUnbufferedPostings(boolean unbufferedPostings) {
            this.unbufferedPostings = unbufferedPostings;
        }

        public boolean hasNext() {

            //
            // If hasNext is called multiple times before next, we need to
            // return true.
            if(returnCurr) {
                return true;
            }

            if(pos >= stopPos) {
                return false;
            }

            N name = decoder.decodeName(prevName, lus.localNames);
            curr = newEntry(name, pos, lus, 
                        unbufferedPostings ? postIn : bufferedPostings);
            prevName = name;
            pos++;
            
            //
            // We can return this element next time around.
            returnCurr = true;
            return true;
        }

        public QueryEntry next() throws java.util.NoSuchElementException {

            //
            // If someone didn't call hasNext(), then we need to advance by doing
            // it ourselves.
            if(!returnCurr) {
                if(hasNext()) {
                    returnCurr = false;
                    return (QueryEntry) curr;
                } else {
                    throw new java.util.NoSuchElementException("No more entries");
                }
            } else {
                returnCurr = false;
                return (QueryEntry) curr;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("Dictionary is read only");
        }

        public int compareTo(Object o) {
            return curr.compareTo(((DiskDictionaryIterator) o).curr);
        }

        /**
         * Estimates the size of the document set that would be
         * returned for all the entries that this iterator would
         * produce.  This is a gross hack!
         */
        public int estimateSize() {

            if(estSize >= 0) {
                return estSize;
            }
            estSize = 0;
            lus.localInfoOffsets.position(startPos * dh.entryInfoOffsetsBytes);
            for(int i = startPos; i < stopPos; i++) {
                lus.localInfo.position(lus.localInfoOffsets.byteDecode(
                        dh.entryInfoOffsetsBytes));
                estSize += lus.localInfo.byteDecode();
            }
            return estSize;
        }

        public int getNEntries() {
            return stopPos - startPos;
        }
    }

    /**
     * A lightweight iterator for this dictionary.
     */
    public class LightDiskDictionaryIterator implements LightIterator {

        /**
         * The position in the dictionary.
         */
        protected int posn;

        protected LookupState lus;

        /**
         * The name of the previous entry.
         */
        protected N prevName;

        public LightDiskDictionaryIterator() {
            lus = new LookupState(DiskDictionary.this);
            posn = -1;
            lus.localNames.position(0);
        }

        public boolean next() {
            posn++;
            if(posn >= dh.size) {
                return false;
            }
            prevName = decoder.decodeName(prevName, lus.localNames);
            return true;
        }

        public Object getName() {
            return prevName;
        }

        public int getN() {
            lus.localInfoOffsets.position(posn * dh.entryInfoOffsetsBytes);
            lus.localInfo.position(lus.localInfoOffsets.byteDecode(
                    dh.entryInfoOffsetsBytes));
            return lus.localInfo.byteDecode();
        }

        public QueryEntry getEntry() {
            return newEntry(prevName, posn, lus, postIn);
        }

        public int getID() {
            if(lus.localIDToPosn != null) {
                return getEntry().getID();
            } else {
                return posn + 1;
            }
        }
    }

    /**
     * A binary search tree that we can fill in with the top few levels of the
     * search tree for this dictionary.
     */
    protected class BinarySearchTree {

        Node root;

        int size;

        int depth;

        public BinarySearchTree(int capacity) {

            //
            // quick approximation to the log base 2 of the cache size.
            depth = (int) (Math.log(capacity) / Math.log(2)) - 1;
            LookupState lus = new LookupState(DiskDictionary.this);
            if(size() > 0) {
                root = new Node(lus, 0, dh.nOffsets - 1);
                root.fill(lus, depth);
            }
        }

        /**
         * Finds the node that's closest to the given name, returning it.
         * @param name the name that we're searching for
         * @return the node closest to the name in the BST.
         */
        public Node find(Object name) {
            Node curr = root;
            if(curr == null) {
                return null;
            }
            Comparable cname = (Comparable) name;
            while(true) {
                int cmp = cname.compareTo(curr.name);
                if(cmp == 0) {
                    return curr;
                } else if(cmp < 0) {
                    //
                    // If the node to the left is null, or the name there is
                    // less than this name, then we need to use this node, as
                    // its start and end span the position where the name must
                    // be.
                    if(curr.left == null ||
                            cname.compareTo(curr.left.name) > 0) {
                        return curr;
                    }
                    curr = curr.left;
                } else {
                    //
                    // If the node to the right is null, or the name there is
                    // greater than this name, then we need to use this node, as
                    // its start and end span the position where the name must be.
                    if(curr.right == null || cname.compareTo(curr.right.name) <
                            0) {
                        return curr;
                    }
                    curr = curr.right;
                }
            }
        }

        /**
         * A node in the binary search tree.
         */
        protected class Node {

            Object name;

            int lower;

            int upper;

            int mid;

            Node left;

            Node right;

            public Node(LookupState lus, int lower, int upper) {
                //
                // Get the name for the node at the midpoint.
                mid = (lower + upper) / 2;
                name = getUncompressedName(mid, lus);
                this.lower = lower;
                this.upper = upper;
                size++;
            }

            public void fill(LookupState lus, int depth) {
                if(depth == 0 || lower >= upper) {
                    return;
                }

                left = new Node(lus, lower, mid - 1);
                left.fill(lus, depth - 1);
                right = new Node(lus, mid + 1, upper);
                right.fill(lus, depth - 1);
            }

            public String toString() {
                return String.format("[%s %d %d %d]",
                                     name,
                                     lower, mid, upper);
            }
        }

        public String toString() {
            return String.format("%d nodes: %s", size, root.toString());
        }
    }
} // DiskDictionary

