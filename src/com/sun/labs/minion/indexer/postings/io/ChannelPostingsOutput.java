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

import java.io.IOException;
import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;

import com.sun.labs.minion.util.ChannelUtil;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * An implementation of <code>PostingsOutput</code> that buffers the
 * postings, eventually writing them to a channel.
 */
public class ChannelPostingsOutput extends AbstractPostingsOutput {

    /**
     * A buffer.
     */
    protected ByteBuffer buff;

    /**
     * The channel.
     */
    protected FileChannel chan;

    /**
     * The default buffer size, 64K.
     */
    protected static final int DEFAULT_SIZE = 1 << 16;

    /**
     * Creates a postings output channel that will write to the given
     * channel.
     * @param chan The channel to write data to.
      */
    public ChannelPostingsOutput(FileChannel chan) {
        this(chan, DEFAULT_SIZE);
    }
    
    /**
     * Creates a postings output channel that will write to the given
     * channel, buffering the given amount.
     * @param chan The channel to write data to.
     * @param buffSize The size of the buffer to use for writing postings.
     */
    public ChannelPostingsOutput(FileChannel chan,
                                 int buffSize) {
        this.chan  = chan;
        buff       = ByteBuffer.allocateDirect(buffSize);
    } // PostingsOutputChannel constructor

    /**
     * Writes a set of postings to the channel (possibly buffering them).
     * @return The number of bytes written.
     * @param b The buffer to write.
     * @throws java.io.IOException If there is any error writing the postings.
     */
    public int write(WriteableBuffer b) throws java.io.IOException {

        long size = b.position();

        //
        // If there's not enough room in the buffer, then flush the
        // buffer.
        if(size > buff.remaining()) {
            flush();
        }

        //
        // If there's still too much stuff in it, then write it directly,
        // otherwise buffer it.
        if(size > buff.remaining()) {
            b.write(chan);
        } else {
            b.write(buff);
        }

        return (int) size;
    }

    /**
     * Writes a set of postings encoded onto a buffer to the channel
     * (possibly buffering them).
     * @param b The buffers to write.
     * @throws java.io.IOException If there is any error writing the postings.
     * @return The number of bytes written.
     */
    public int write(WriteableBuffer [] b) throws java.io.IOException {
        return write(b, 0, b.length);
    }
    
    /**
     * Writes a subsequence of a set of postings encoded onto a buffer to
     * the channel (possibly buffering them).
     * @param b The buffers to write.
     * @param offset The offset in <code>b</code> where we will begin
     * writing bytes.
     * @param length The length of the subsequence of <code>b</code> for
     * which we will write postings.
     * @throws java.io.IOException If there is any error writing to the channel.
     * @return The number of bytes written.
     */
    public int write(WriteableBuffer[] b, int offset, int length)
        throws java.io.IOException {

        int written = 0;
        for(int i = offset; i < offset + length; i++) {
            written += write(b[i]);
        }
        return written;
    }

    /**
     * Gets the position of the the channel.
     * @throws java.io.IOException If there is any error getting the position.
     * @return The current position of the postings output.
     */
    public long position() throws java.io.IOException {
        return chan.position() + buff.position();
    }

    /**
     * Flushes the buffer to the channel.
     * @throws java.io.IOException If there is any error writing data to the channel.
     */
    public void flush() throws java.io.IOException {
        if(buff.position() > 0) {
            ChannelUtil.writeFully(chan,
                                   (ByteBuffer) buff.flip());
        }
        buff.clear();
    }

    public void cleanUp() {
    }
    
} // PostingsOutputChannel
