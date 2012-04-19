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

package com.sun.labs.minion.indexer.postings.io;

import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.Postings.Type;
import java.io.IOException;
import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;

import com.sun.labs.minion.util.ChannelUtil;

import com.sun.labs.minion.util.buffer.ChannelReadableBuffer;
import com.sun.labs.minion.util.buffer.NIOBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;

/**
 * A class for reading postings from a channel.
 * 
 * <p>
 * 
 * This implementation is most useful for reading postings that will be used in
 * evaluating queries.
 */
public class ChannelPostingsInput implements PostingsInput {

    /**
     * The channel.
     */
    protected FileChannel chan;

    /**
     * If true, full buffers will be read from the channel.
     */
    protected boolean full;

    /**
     * Creates a postings channel that will read postings from the given
     * channel, using the given base offset.
     * @param chan The channel from which we'll read postings.
     * @param full If <CODE>true</CODE>, then we will fully read postings from the postings file
     * before returning them.  In this case, an instance of 
     * {@link com.sun.labs.minion.util.buffer.NIOBuffer NIOBuffer} is returned. If <CODE>false</CODE>, then postings will be only 
     * partially read from the file.  In this case, an instance of 
     * {@link com.sun.labs.minion.util.buffer.ChannelReadableBuffer ChannelReadableBuffer} is returned.
     */
    public ChannelPostingsInput(FileChannel chan, boolean full) {
        this.chan = chan;
        this.full = full;
    } // PostingsInputChannel constructor

    /**
     * Reads postings from the channel.
     * @param offset The offset from which the postings will be
     * read.
     * @param size The size of the postings to read.
     * @throws java.io.IOException If there is any error reading from the channel.
     * @return A readable buffer containing the postings.
     * @see com.sun.labs.minion.util.buffer.ReadableBuffer
     */
    @Override
    public ReadableBuffer read(long offset, int size)
        throws java.io.IOException {
        if(full) {
            return
                new NIOBuffer(
                              (ByteBuffer) ChannelUtil.readFully(chan,
                                                                 offset,
                                                                 ByteBuffer.allocate(size))
                              .flip());
        } else {
            return new ChannelReadableBuffer(chan, offset, size, 512);
        }
    }

} // PostingsInputChannel
