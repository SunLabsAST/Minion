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

import com.sun.labs.minion.indexer.dictionary.MemoryDictionaryBundle;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.logging.Logger;


/**
 * A header with information about where various parts of a field can be found.
 */
public class FieldHeader {
    
    private static final Logger logger = Logger.getLogger(FieldHeader.class.getName());

    /**
     * The ID of the field.
     */
    public int fieldID = -1;

    /**
     * The maximum document ID for the partition in which this field resides.
     */
    public int maxDocID = -1;

    /**
     * The offsets of the starts of the dictionaries that make up the fields.
     */
    public long[] dictionaryOffsets = new long[MemoryDictionaryBundle.Type.values().length];

    /**
     * Where we'll find the buffer that maps from document ID to a position
     * in the doc-to-value data.
     */
    public long dtvPosOffset = -1;

    /**
     * Where we'll find the doc to value data.
     */
    public long dtvOffset = -1;

    /**
     * Where we'll find the document vector lengths for this field, both 
     * unstemmed (element 0) and stemmed (element 1).
     */
    public long[] vectorLengthOffsets = new long[Field.DocumentVectorType.values().length];
    
    /**
     * Creates a header.
     */
    public FieldHeader() {
        Arrays.fill(dictionaryOffsets, -1);
        Arrays.fill(vectorLengthOffsets, -1);
    } // FieldHeader constructor

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
            dictionaryOffsets[i] = f.readLong();
        }
        dtvPosOffset = f.readLong();
        dtvOffset = f.readLong();
        for(int i = 0; i < vectorLengthOffsets.length; i++) {
            vectorLengthOffsets[i] = f.readLong();
        }
    }

    /**
     * Writes a field header to the given channel.
     */
    public void write(RandomAccessFile f)
            throws java.io.IOException {
        f.writeInt(fieldID);
        f.writeInt(maxDocID);
        f.writeInt(dictionaryOffsets.length);
        for(int i = 0; i < dictionaryOffsets.length; i++) {
            f.writeLong(dictionaryOffsets[i]);
        }
        f.writeLong(dtvPosOffset);
        f.writeLong(dtvOffset);
        for(int i = 0; i < vectorLengthOffsets.length; i++) {
            f.writeLong(vectorLengthOffsets[i]);
        }
    }
    
    public void write(WriteableBuffer b) {
        b.byteEncode(fieldID, 4);
        b.byteEncode(maxDocID, 4);
        b.byteEncode(dictionaryOffsets.length, 4);
        for(int i = 0; i < dictionaryOffsets.length; i++) {
            b.byteEncode(dictionaryOffsets[i], 8);
        }
        b.byteEncode(dtvPosOffset, 8);
        b.byteEncode(dtvOffset, 8);
        for(int i = 0; i < vectorLengthOffsets.length; i++) {
            b.byteEncode(vectorLengthOffsets[i], 8);
        }
        
    }

    @Override
    public String toString() {
        return "FieldHeader{\n " + "fieldID=" + fieldID + "\n maxDocID="
                + maxDocID + "\n dictOffsets=" + Arrays.toString(dictionaryOffsets)
                + "\n dtvPosOffset=" + dtvPosOffset
                + "\n dtvOffset=" + dtvOffset + "\n vectorLengthOffsets="
                + Arrays.toString(vectorLengthOffsets) + "\n}";
    }
    
    
} // SavedFieldHeader

