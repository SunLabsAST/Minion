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
package com.sun.labs.minion.indexer;

import java.io.RandomAccessFile;
import java.util.Arrays;


/**
 * A header for saved field information.
 */
class FieldHeader {

    /**
     * The ID of the field.
     */
    protected int fieldID;

    /**
     * The maximum document ID for the partition in which this field resides.
     */
    protected int maxDocID;

    /**
     * The offsets of the starts of the dictionaries that make up the fields.
     */
    protected long[] dictOffsets = new long[MemoryDictionaryBundle.Type.values().length];

    /**
     * The offset of the start of the bigram dictionary for the tokens in the
     * field, if any.
     */
    protected long tokenBGOffset;

    /**
     * The offset of the start of the bigram dictionary for the saved values in the
     * field, if any.
     */
    protected long savedBGOffset;

    /**
     * Where we'll find the buffer that maps from document ID to a position
     * in the doc-to-value data.
     */
    protected long dtvPosOffset;

    /**
     * Where we'll find the doc to value data.
     */
    protected long dtvOffset;

    /**
     * Where we'll find the document lengths for this field.
     */
    protected long vectorLengthOffset;

    /**
     * Creates a header.
     */
    public FieldHeader() {
    } // SavedFieldHeader constructor

    /**
     * Creates a header, reading the data from the given channel.
     */
    public FieldHeader(RandomAccessFile f)
            throws java.io.IOException {
        read(f);
    }

    /**
     * Reads a field header from the given channel.
     */
    public void read(RandomAccessFile f) throws java.io.IOException {
        fieldID = f.readInt();
        maxDocID = f.readInt();
        int n = f.readInt();
        for(int i = 0; i < n; i++) {
            dictOffsets[i] = f.readLong();
        }
        tokenBGOffset = f.readLong();
        savedBGOffset = f.readLong();
        dtvPosOffset = f.readLong();
        dtvOffset = f.readLong();
        vectorLengthOffset = f.readLong();
    }

    /**
     * Writes a field header to the given channel.
     */
    public void write(RandomAccessFile f)
            throws java.io.IOException {
        f.writeInt(fieldID);
        f.writeInt(maxDocID);
        f.writeInt(dictOffsets.length);
        for(int i = 0; i < dictOffsets.length; i++) {
            f.writeLong(dictOffsets[i]);
        }
        f.writeLong(tokenBGOffset);
        f.writeLong(savedBGOffset);
        f.writeLong(dtvPosOffset);
        f.writeLong(dtvOffset);
        f.writeLong(vectorLengthOffset);
    }

    @Override
    public String toString() {
        return "FieldHeader{\n " + "fieldID=" + fieldID + "\n maxDocID="
                + maxDocID + "\n dictOffsets=" + Arrays.toString(dictOffsets)
                + "\n tokenBGOffset=" + tokenBGOffset + "\n savedBGOffset="
                + savedBGOffset + "\n dtvPosOffset=" + dtvPosOffset
                + "\n dtvOffset=" + dtvOffset + "\n vectorLengthOffset="
                + vectorLengthOffset + "\n}";
    }
    
    
} // SavedFieldHeader

