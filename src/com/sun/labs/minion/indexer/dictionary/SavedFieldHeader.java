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

import com.sun.labs.minion.util.buffer.StdBufferImpl;

/**
 * A header for saved field information.
 */
class SavedFieldHeader {

    /**
     * The number of documents for which we're storing data.
     */
    protected int nDocs;

    /**
     * The offset of the values dictionary.
     */
    protected long valOffset;

    /**
     * The offset of the bigram dictionary for a character field.
     */
    protected long bgOffset;

    /**
     * The offset of the docs to values offsets.
     */
    protected long dtvOffsetOffset;

    /**
     * The size of the docs to values offset data.
     */
    protected int dtvOffsetSize;

    /**
     * The number of bytes used to encode offset data.
     */
    protected int offsetBytes;

    /**
     * The offset of the docs to values data.
     */
    protected long dtvOffset;

    /**
     * The size of the docs to values data.
     */
    protected int dtvSize;

    /**
     * Creates a header.
     */
    public SavedFieldHeader() {
    } // SavedFieldHeader constructor

    /**
     * Creates a header, reading the data from the given channel.
     */
    public SavedFieldHeader(RandomAccessFile f)
        throws java.io.IOException {
        read(f);
    }

    /**
     * Reads a field header from the given channel.
     */
    public void read(RandomAccessFile f)
        throws java.io.IOException {
        nDocs           = f.readInt();
        valOffset       = f.readLong();
        bgOffset        = f.readLong();
        dtvOffsetOffset = f.readLong();
        dtvOffsetSize   = f.readInt();
        dtvOffset       = f.readLong();
        dtvSize         = f.readInt();
        offsetBytes     = StdBufferImpl.bytesRequired(dtvSize);
    }
    
    /**
     * Writes a field header to the given channel.
     */
    public void write(RandomAccessFile f)
        throws java.io.IOException {
        f.writeInt(nDocs);
        f.writeLong(valOffset);
        f.writeLong(bgOffset);
        f.writeLong(dtvOffsetOffset);
        f.writeInt(dtvOffsetSize);
        f.writeLong(dtvOffset);
        f.writeInt(dtvSize);
    }
} // SavedFieldHeader
