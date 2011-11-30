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

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 * A class that contains the header information for a dictionary.
 */
public class DictionaryHeader {

    private static final Logger logger = Logger.getLogger(DictionaryHeader.class.getName());

    /**
     *  A magic number, written last.
     */
    public int magic;

    /**
     * The number of entries in the dictionary.
     */
    public int size;

    /**
     * The maximum ID assigned to an entry, which may be different than the
     * size.
     */
    public int maxEntryID;

    /**
     * The offset of the ID to position map in the file.
     */
    public long idToPosnPos;

    /**
     * The size of the map from ID to position in the dictionary.
     */
    public int idToPosnSize;

    /**
     * The position of the names offset buffer.
     */
    public long nameOffsetsPos;

    /**
     * The size of the names offset buffer.
     */
    public int nameOffsetsSize;

    /**
     * The position of the names buffer.
     */
    public long namesPos;

    /**
     * The size of the names buffer.
     */
    public int namesSize;

    /**
     * The number of name offsets.
     */
    public int nOffsets;

    /**
     * The position of the entry info offsets buffer.
     */
    public long entryInfoOffsetsPos;

    /**
     * The size of the entry info offsets buffer.
     */
    public int entryInfoOffsetsSize;

    /**
     * The position of the entry info buffer.
     */
    public long entryInfoPos;

    /**
     * The size of the entry info buffer.
     */
    public int entryInfoSize;

    /**
     * The starting offsets for postings associated with this dictionary.
     */
    public long[] postStart;

    /**
     * The ending offsets for postings associated with this dictionary.
     */
    public long[] postEnd;

    /**
     * Good magic.
     */
    public static final int GOOD_MAGIC = Integer.MAX_VALUE;

    public static final int BAD_MAGIC = Integer.MIN_VALUE;

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
    
    public DictionaryHeader(ReadableBuffer b) throws java.io.IOException {
        size = b.byteDecode(4);
        maxEntryID = b.byteDecode(4);
        idToPosnPos = b.byteDecode(8);
        idToPosnSize = b.byteDecode(4);
        nameOffsetsPos = b.byteDecode(8);
        nameOffsetsSize = b.byteDecode(4);
        namesPos = b.byteDecode(8);
        namesSize = b.byteDecode(4);
        entryInfoOffsetsPos = b.byteDecode(8);
        entryInfoOffsetsSize = b.byteDecode(4);
        entryInfoPos = b.byteDecode(8);
        entryInfoSize = b.byteDecode(4);
        int n = b.byteDecode(4);
        postStart = new long[n];
        postEnd = new long[n];
        for(int i = 0; i < n; i++) {
            postStart[i] = b.byteDecode(8);
            postEnd[i] = b.byteDecode(8);
        }
        magic = b.byteDecode(4);
        if(magic != GOOD_MAGIC) {
            throw new java.io.IOException("Error reading magic number");
        }
        computeValues();
    }

    /**
     * Computes derived values.
     */
    public void computeValues() {
        nOffsets = nameOffsetsSize / 4;
        if(maxEntryID == 0) {
            maxEntryID = size + 1;
        }
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

        size = dictFile.readInt();
        maxEntryID = dictFile.readInt();
        idToPosnPos = dictFile.readLong();
        idToPosnSize = dictFile.readInt();
        nameOffsetsPos = dictFile.readLong();
        nameOffsetsSize = dictFile.readInt();
        namesPos = dictFile.readLong();
        namesSize = dictFile.readInt();
        entryInfoOffsetsPos = dictFile.readLong();
        entryInfoOffsetsSize = dictFile.readInt();
        entryInfoPos = dictFile.readLong();
        entryInfoSize = dictFile.readInt();
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

    public void write(WriteableBuffer b) {
        b.byteEncode(size, 4);
        b.byteEncode(maxEntryID, 4);
        b.byteEncode(idToPosnPos, 8);
        b.byteEncode(idToPosnSize, 4);
        b.byteEncode(nameOffsetsPos, 8);
        b.byteEncode(nameOffsetsSize, 4);
        b.byteEncode(namesPos, 8);
        b.byteEncode(namesSize, 4);
        b.byteEncode(entryInfoOffsetsPos, 8);
        b.byteEncode(entryInfoOffsetsSize, 4);
        b.byteEncode(entryInfoPos, 8);
        b.byteEncode(entryInfoSize, 4);
        b.byteEncode(postStart.length, 4);
        for(int i = 0; i < postStart.length; i++) {
            b.byteEncode(postStart[i], 8);
            b.byteEncode(postEnd[i], 7);
        }
        b.byteEncode(magic, 1);
    }

    /**
     * Gets the maximum ID in the dictionary.
     */
    public int getMaxID() {
        return maxEntryID;
    }

    @Override
    public String toString() {
        return "size: " + size
                + " maxEntryID: " + maxEntryID
                + " namesPos: " + namesPos
                + " namesSize: " + namesSize
                + " nameOffsetsPos: " + nameOffsetsPos
                + " nameOffsetsSize: " + nameOffsetsSize
                + " entryInfoPos: " + entryInfoPos
                + " entryInfoSize: " + entryInfoSize
                + " entryInfoOffsetsPos: " + entryInfoOffsetsPos
                + " entryInfoOffsetsSize: " + entryInfoOffsetsSize
                + " id2p: " + idToPosnSize;
    }
} // DictionaryHeader
