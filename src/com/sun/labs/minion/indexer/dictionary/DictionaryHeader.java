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

import java.io.RandomAccessFile;

/**
 * A class that contains the header information for a dictionary.
 */
public class DictionaryHeader {

    /**
     *  A magic number, written last.
     */
    protected int magic;

    /**
     * The number of entries in the dictionary.
     */
    protected int size;

    /**
     * The maximum ID assigned to an entry, which may be different than the
     * size.
     */
    protected int maxEntryID;

    /**
     * The offset of the ID to position map in the file.
     */
    protected long idToPosnPos;

    /**
     * The size of the map from ID to position in the dictionary.
     */
    protected int idToPosnSize;

    /**
     * The number of bytes that each ID to posn map entry requires.
     */
    protected int idToPosnBytes = 4;

    /**
     * The position of the names offset buffer.
     */
    protected long nameOffsetsPos;

    /**
     * The size of the names offset buffer.
     */
    protected int nameOffsetsSize;

    /**
     * The number of bytes used to encode the offsets for the uncompressed
     * names.
     */
    protected int nameOffsetsBytes = 4;

    /**
     * The position of the names buffer.
     */
    protected long namesPos;
    
    /**
     * The size of the names buffer.
     */
    protected int namesSize;

    /**
     * The number of name offsets.
     */
    protected int nOffsets;

    /**
     * The position of the entry info offsets buffer.
     */
    protected long entryInfoOffsetsPos;
    
    /**
     * The size of the entry info offsets buffer.
     */
    protected int entryInfoOffsetsSize;

    /**
     * The number of bytes used to encode the offsets for the term
     * information.
     */
    protected int entryInfoOffsetsBytes = 4;

    /**
     * The position of the entry info buffer.
     */
    protected long entryInfoPos;

    /**
     * The size of the entry info buffer.
     */
    protected int entryInfoSize;

    /**
     * The starting offsets for postings associated with this dictionary.
     */
    protected long[] postStart;

    /**
     * The ending offsets for postings associated with this dictionary.
     */
    protected long[] postEnd;

    /**
     * Good magic.
     */
    protected static final int GOOD_MAGIC = Integer.MAX_VALUE;
    protected static final int BAD_MAGIC = Integer.MIN_VALUE;

    /**
     * Creates an empty header, suitable for filling during dumping.
     *
     * @param n The number of postings channels associated with the
     * dictionary for which this is a header.
     */
    public DictionaryHeader(int n) {
        postStart = new long[n];
        postEnd = new long[n];
        magic = BAD_MAGIC;
    }

    /**
     * Creates a dictionary header by reading it from the provided channel.
     */
    public DictionaryHeader(RandomAccessFile dictFile)
        throws java.io.IOException {
        read(dictFile);
    }

    /**
     * Computes derived values.
     */
    protected void computeValues() {
        nameOffsetsBytes      = 4;
        entryInfoOffsetsBytes = 4;
        nOffsets              = nameOffsetsSize / nameOffsetsBytes;
        idToPosnBytes         = 4;
    }


    /**
     * Tells the header that the magic is good.
     */
    public void goodMagic() {
        magic = GOOD_MAGIC;
    }

    /**
     * Reads a dictionary header from the given channel.
     */
    public void read(RandomAccessFile dictFile)
        throws java.io.IOException {
	
        size                 = dictFile.readInt();
        maxEntryID           = dictFile.readInt();
        idToPosnPos          = dictFile.readLong();
        idToPosnSize         = dictFile.readInt();
        nameOffsetsPos       = dictFile.readLong();
        nameOffsetsSize      = dictFile.readInt();
        namesPos             = dictFile.readLong();
        namesSize            = dictFile.readInt();
        entryInfoOffsetsPos  = dictFile.readLong();
        entryInfoOffsetsSize = dictFile.readInt();
        entryInfoPos         = dictFile.readLong();
        entryInfoSize        = dictFile.readInt();
        int n = dictFile.readInt();
        postStart = new long[n];
        postEnd = new long[n];
        for(int i = 0; i < n; i++) {
            postStart[i] = dictFile.readLong();
            postEnd[i] = dictFile.readLong();
        }
        magic = dictFile.readInt();
        if(magic != GOOD_MAGIC) {
            throw new java.io.IOException("Error reading magic number");
        }
        computeValues();
    }

    /**
     * Writes a dictionary header to the given channel.
     */
    public void write(RandomAccessFile dictFile)
        throws java.io.IOException {
        dictFile.writeInt(size);
        dictFile.writeInt(maxEntryID);
        dictFile.writeLong(idToPosnPos);
        dictFile.writeInt(idToPosnSize);
        dictFile.writeLong(nameOffsetsPos);
        dictFile.writeInt(nameOffsetsSize);
        dictFile.writeLong(namesPos);
        dictFile.writeInt(namesSize);
        dictFile.writeLong(entryInfoOffsetsPos);
        dictFile.writeInt(entryInfoOffsetsSize);
        dictFile.writeLong(entryInfoPos);
        dictFile.writeInt(entryInfoSize);
        dictFile.writeInt(postStart.length);
        for(int i = 0; i < postStart.length; i++) {
            dictFile.writeLong(postStart[i]);
            dictFile.writeLong(postEnd[i]);
        }
        dictFile.writeInt(magic);
    }

    /**
     * Gets the maximum ID in the dictionary.
     */
    public int getMaxID() {
        return maxEntryID;
    }
    
    public String toString() {
        return "size: " + size + " maxEntryID: " + maxEntryID + " nos: " + nameOffsetsSize + " ns: " + namesSize + 
                " ios: " + entryInfoOffsetsSize + " is: " + entryInfoSize + " id2p: " + idToPosnSize;
    }

} // DictionaryHeader
