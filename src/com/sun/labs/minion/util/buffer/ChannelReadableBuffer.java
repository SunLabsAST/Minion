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
package com.sun.labs.minion.util.buffer;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A readable buffer that's backed by a channel and an in-memory buffer.
 */
public class ChannelReadableBuffer extends StdReadableImpl {

    private static final Logger logger = Logger.getLogger(ChannelReadableBuffer.class.getName());

    /**
     * The channel containing the buffer.
     */
    protected FileChannel chan;

    /**
     * The offset in the file of the start of our in-memory buffer.
     */
    protected long ms;

    /**
     * The offset in the file of the end of our in-memory buffer.
     */
    protected long me;

    /**
     * The offset in the file of the buffer that we represent.
     */
    protected long bs;

    /**
     * The offset in the file of the end of the buffer that we represent. 
     */
    protected long be;

    /**
     * The offset of the current position in the buffer we're
     * representing.
     */
    protected long pos;

    /**
     * The in-memory buffer.
     */
    protected ByteBuffer buff;

    /**
     * The default buffer size, 8KB.
     */
    protected static final int DEFAULT_BUFF_SIZE = 8096;

    /**
     * Creates a buffer backed by a channel.
     * @param raf The file that will provide the channel backing this buffer.
     * @param offset The offset in the file where our buffer is located.
     * @param limit The number of bytes in our buffer.
     */
    public ChannelReadableBuffer(RandomAccessFile raf,
            long offset,
            long limit) {
        this(raf.getChannel(), offset, limit, DEFAULT_BUFF_SIZE);
    }

    /**
     * Creates a buffer backed by a channel.
     * @param buffSize The size of the in-memory buffer to use.
     * @param raf The file that will provide the channel backing this buffer.
     * @param offset The offset in the file where our buffer is located.
     * @param limit The number of bytes in our buffer.
     */
    public ChannelReadableBuffer(RandomAccessFile raf,
            long offset,
            long limit,
            int buffSize) {
        this(raf.getChannel(), offset, limit, buffSize);
    }

    /**
     * Creates a buffer backed by a channel.
     * @param chan The channel backing our buffer.
     * @param buffSize The size of the in-memory buffer to use.
     * @param offset The offset in the file where our buffer is located.
     * @param limit The number of bytes in our buffer.
     */
    public ChannelReadableBuffer(FileChannel chan,
            long offset,
            long limit,
            int buffSize) {
        this.chan = chan;
        bs = offset;
        be = offset + limit;
        pos = bs;
        ms = -1;


        //
        // Fill the buffer if the size of the data is smaller than the size
        // of the buffer.
        if(limit > 0 && limit <= buffSize) {
            buff = ByteBuffer.allocateDirect((int) limit);
            int n = read(bs);
            ms = bs;
            me = bs + n;
        } else {
            buff = ByteBuffer.allocateDirect(buffSize);
        }
    }

    /**
     * Reads a given number of bytes from the channel.
     * @param off The offset in the file from which the bytes should be
     * read.
     * @return The number of bytes actually read.
     */
    protected int read(long off) {
        try {
            buff.clear();
            return chan.read(buff, off);
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error reading from channel", ioe);
            return -1;
        }
    }

    /**
     * Checks whether the given position is within the bounds of our in
     * memory buffer.  If it's not, data will be read into the in-memory
     * buffer.
     *
     * @param p The position that we want to check
     * @return The index in the in-memory buffer that can be read for this
     * position.
     */
    protected int checkBounds(long p) {
        if(ms >= 0 && p >= ms && p < me) {
            return (int) (p - ms);
        }

        int n = read(p);
        ms = p;
        me = p + n;
        return 0;
    }

    /**
     * Gets the number of bytes remaining to be read.
     * @return The number of bytes remaining to be read.
     */
    @Override
    public long remaining() {
        return be - pos;
    }

    /**
     * Duplicates this buffer, so that it can be used safely by other
     * readers.  Note that this duplicates the data in the buffer, since it
     * must not be changed by subsequent reads.
     * @return A new buffer that is backed by the same channel.  The in-memory buffer and
     * position are independent in the new buffer.
     */
    @Override
    public ReadableBuffer duplicate() {
        return new ChannelReadableBuffer(chan, bs, be - bs, buff.capacity());
    }

    /**
     * Slices this buffer so that a sub-buffer can be used.  The buffer is
     * sliced from the current position.  Note that this actually
     * duplicates the data in the buffer, since it must not be changed by
     * subsequent reads.
     * @param p The position at which the buffer should be sliced.
     * @param s The number of bytes that should be in the sliced buffer.
     * @return A new buffer backed by the same channel.  The first position in the new buffer 
     * will be the given position and the new buffer will contain the given number 
     * of bytes.
     */
    @Override
    public ReadableBuffer slice(long p, long s) {
        return new ChannelReadableBuffer(chan, bs + p, s, buff.capacity());
    }

    /**
     * Gets the limit of this buffer, i.e., the last readable position.
     * @return The last readable position in the buffer.
     */
    @Override
    public long limit() {
        return be - bs;
    }

    /**
     * Sets the limit of this buffer, i.e., the last readable position.
     * @param l The limit to set.
     */
    @Override
    public void limit(long l) {
        be = bs + l;
    }

    /**
     * Gets the byte at the given position in the buffer.
     * @param i The position from which we want to get a byte.
     * @return The byte at the given position.
     */
    @Override
    public byte get(long i) {
        return buff.get(checkBounds(i + bs));
    }

    /**
     * Gets the next byte in the buffer.
     * @return Gets the byte at the current position and advances the current position.
     */
    @Override
    public byte get() {
        return buff.get(checkBounds(pos++));
    }

    /**
     * Gets the position of the buffer.
     * @return The current position of the buffer.
     */
    @Override
    public long position() {
        return pos - bs;
    }

    /**
     * Positions the buffer.
     * @param i The position to which we want to set the buffer.
     */
    @Override
    public void position(long i) {
        this.pos = bs + i;
    }

    /**
     * Gets a string representation of the buffer.
     * @return A string representation of the buffer.
     */
    @Override
    public String toString() {
        return "buff: (" + bs + "," + be + ")" +
                " mem: (" + ms + "," + me + ") " + buff.capacity();
    }
} // ChannelReadableBuffer
