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

import com.sun.labs.minion.util.ChannelUtil;
import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;

public class FieldStoreHeader {

    /**
     * The number of field offsets that we're storing.
     */
    protected int nFields;

    /**
     * A buffer to hold the offsets of the dictionaries for the fields.
     */
    protected LongBuffer lb;

    /**
     * The byte buffer underlying our buffer of longs.
     */
    protected ByteBuffer bb;

    /**
     * Creates a header for the given number of fields.
     */
    public FieldStoreHeader(int nFields) {
        this.nFields = nFields;
        bb = ByteBuffer.allocate(nFields*8+4);
        bb.putInt(nFields);
        lb = bb.asLongBuffer();
    } // FieldStoreHeader constructor

    /**
     * Creates a header for the given number of fields by reading it from
     * the given channel.
     *
     * @param c The channel from which to read the header.
     * @throws java.io.IOException if there is any error reading the
     * header.
     */
    public FieldStoreHeader(FileChannel c)
        throws java.io.IOException {
        read(c);
    } // FieldStoreHeader constructor

    /**
     * Adds a dictionary offset for a given field.
     *
     * @param f The ID of the field whose offset we want to add.
     * @param offset The offset of the field dictionary.
     */
    public void addOffset(int f, long offset) {
        lb.put(f-1, offset);
    }

    /**
     * Gets an offset for a given field.
     *
     * @param f The ID of the field whose offset we want to get.
     */
    public long getOffset(int f) {
        return lb.get(f-1);
    }

    /**
     * Writes the header to the given channel.
     *
     * @param c The channel to which we will write the header.
     * @throws java.io.IOException if there is any error writing the data.
     */
    public void write(FileChannel c)
        throws java.io.IOException {
        ChannelUtil.writeFully(c,
                               (ByteBuffer) bb.position(bb.capacity()).flip());
    }

    /**
     * Reads the header from the given channel
     *
     * @param c The channel from which to read the header.
     * @throws java.io.IOException if there is any error reading the
     * header.
     */
    public void read(FileChannel c)
        throws java.io.IOException {

        //
        // We'll do a separate read to get the number of fields, since we
        // need to know that to know how much data to read for the field
        // offsets.
        nFields = ((ByteBuffer) 
                   ChannelUtil.readFully(c, ByteBuffer.allocate(4)).flip())
            .getInt();
        
        bb = ByteBuffer.allocate(nFields*8+4);
        bb.putInt(nFields);
        lb = bb.asLongBuffer();
        ChannelUtil.readFully(c, bb);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(nFields + ":");
        for(int i = 1; i <= nFields; i++) {
            sb.append(" " + getOffset(i));
        }
        return sb.toString();
    }
    
} // FieldStoreHeader
