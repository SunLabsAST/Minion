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
    public long size;

    /**
     * The maximum ID assigned to an entry, which may be different than the
     * size.
     */
    public long maxEntryID;

    /**
     * The offset of the ID to position map in the file.
     */
    public long idToPosnPos;

    /**
     * The size of the map from ID to position in the dictionary.
     */
    public long idToPosnSize;

    /**
     * The position of the names offset buffer.
     */
    public long nameOffsetsPos;

    /**
     * The size of the names offset buffer.
     */
    public long nameOffsetsSize;

    /**
     * The position of the names buffer.
     */
    public long namesPos;

    /**
     * The size of the names buffer.
     */
    public long namesSize;

    /**
     * The number of name offsets.
     */
    public long nOffsets;

    /**
     * The position of the entry info offsets buffer.
     */
    public long entryInfoOffsetsPos;

    /**
     * The size of the entry info offsets buffer.
     */
    public long entryInfoOffsetsSize;

    /**
     * The position of the entry info buffer.
     */
    public long entryInfoPos;

    /**
     * The size of the entry info buffer.
     */
    public long entryInfoSize;

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
        size = b.byteDecodeLong(8);
        maxEntryID = b.byteDecodeLong(8);
        idToPosnPos = b.byteDecodeLong(8);
        idToPosnSize = b.byteDecodeLong(8);
        nameOffsetsPos = b.byteDecodeLong(8);
        nameOffsetsSize = b.byteDecodeLong(8);
        namesPos = b.byteDecodeLong(8);
        namesSize = b.byteDecodeLong(8);
        entryInfoOffsetsPos = b.byteDecodeLong(8);
        entryInfoOffsetsSize = b.byteDecodeLong(8);
        entryInfoPos = b.byteDecodeLong(8);
        entryInfoSize = b.byteDecodeLong(8);
        int n = b.byteDecode(4);
        postStart = new long[n];
        postEnd = new long[n];
        for(int i = 0; i < n; i++) {
            postStart[i] = b.byteDecodeLong(8);
            postEnd[i] = b.byteDecodeLong(8);
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

        size = dictFile.readLong();
        maxEntryID = dictFile.readLong();
        idToPosnPos = dictFile.readLong();
        idToPosnSize = dictFile.readLong();
        nameOffsetsPos = dictFile.readLong();
        nameOffsetsSize = dictFile.readLong();
        namesPos = dictFile.readLong();
        namesSize = dictFile.readLong();
        entryInfoOffsetsPos = dictFile.readLong();
        entryInfoOffsetsSize = dictFile.readLong();
        entryInfoPos = dictFile.readLong();
        entryInfoSize = dictFile.readLong();
        int n = dictFile.readInt();
        postStart = new long[n];
        postEnd = new long[n];
        for(int i = 0; i < n; i++) {
            postStart[i] = dictFile.readLong();
            postEnd[i] = dictFile.readLong();
        }
        magic = dictFile.readInt();
        if(magic != GOOD_MAGIC) {
            logger.info(String.format("bad magic: %s", this));
            throw new java.io.IOException("Error reading magic number");
        }
        computeValues();
    }

    /**
     * Writes a dictionary header to the given channel.
     */
    public void write(RandomAccessFile dictFile)
            throws java.io.IOException {
        dictFile.writeLong(size);
        dictFile.writeLong(maxEntryID);
        dictFile.writeLong(idToPosnPos);
        dictFile.writeLong(idToPosnSize);
        dictFile.writeLong(nameOffsetsPos);
        dictFile.writeLong(nameOffsetsSize);
        dictFile.writeLong(namesPos);
        dictFile.writeLong(namesSize);
        dictFile.writeLong(entryInfoOffsetsPos);
        dictFile.writeLong(entryInfoOffsetsSize);
        dictFile.writeLong(entryInfoPos);
        dictFile.writeLong(entryInfoSize);
        dictFile.writeInt(postStart.length);
        for(int i = 0; i < postStart.length; i++) {
            dictFile.writeLong(postStart[i]);
            dictFile.writeLong(postEnd[i]);
        }
        dictFile.writeInt(magic);
    }

    public void write(WriteableBuffer b) {
        b.byteEncode(size, 8);
        b.byteEncode(maxEntryID, 8);
        b.byteEncode(idToPosnPos, 8);
        b.byteEncode(idToPosnSize, 8);
        b.byteEncode(nameOffsetsPos, 8);
        b.byteEncode(nameOffsetsSize, 8);
        b.byteEncode(namesPos, 8);
        b.byteEncode(namesSize, 8);
        b.byteEncode(entryInfoOffsetsPos, 8);
        b.byteEncode(entryInfoOffsetsSize, 8);
        b.byteEncode(entryInfoPos, 8);
        b.byteEncode(entryInfoSize, 8);
        b.byteEncode(postStart.length, 4);
        for(int i = 0; i < postStart.length; i++) {
            b.byteEncode(postStart[i], 8);
            b.byteEncode(postEnd[i], 8);
        }
        b.byteEncode(magic, 4);
    }

    /**
     * Gets the maximum ID in the dictionary.
     */
    public int getMaxID() {
        return (int) maxEntryID;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("size: " + size
                + " maxEntryID: " + maxEntryID
                + " id2pp: " + idToPosnPos
                + " id2p: " + idToPosnSize 
                + " nameOffsetsPos: " + nameOffsetsPos
                + " nameOffsetsSize: " + nameOffsetsSize
                + " namesPos: " + namesPos
                + " namesSize: " + namesSize
                + " entryInfoOffsetsPos: " + entryInfoOffsetsPos
                + " entryInfoOffsetsSize: " + entryInfoOffsetsSize
                + " entryInfoPos: " + entryInfoPos
                + " entryInfoSize: " + entryInfoSize);

        for(int i = 0; i < postStart.length; i++) {
            if(i > 0) {
                b.append(", ");
            }
            b.append('[').append(postStart[i]);
            b.append(postEnd[i]).append(']');
        }
        b.append("magic: ").append(magic);
        return b.toString();
    }
} // DictionaryHeader
