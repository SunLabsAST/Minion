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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A buffer that can be used to read data from a file, keeping minimal
 * amounts of data in memory.
 *
 * <p>
 *
 * This implementation is mainly used by the {@link
 * com.sun.labs.minion.indexer.dictionary.DiskDictionary DiskDictionary} so that only
 * a small amount of dictionary information needs to be in main memory at
 * any given time.
 */
public class FileReadableBuffer extends StdReadableImpl {

    /**
     * A log.
     */
    protected static final Logger logger = Logger.getLogger(FileReadableBuffer.class.getName());

    /**
     * The file containing the buffer.
     */
    protected RandomAccessFile raf;

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
    protected byte[] buff;

    /**
     * The default buffer size, 1KB.
     */
    protected static final int DEFAULT_BUFF_SIZE = 1024;

    /**
     * Creates a readable buffer that is backed by the given file.  The
     * buffer starts at the given offset in the file and extends for the
     * given number of bytes.  The buffer will use the default buffer size
     * for the in-memory buffer.
     * @param raf The file containing the data for our buffer.
     * @param offset The offset in the file where the data for our buffer can be found.
     * @param limit The number of bytes of data in our buffer.
     */
    public FileReadableBuffer(RandomAccessFile raf,
            long offset,
            long limit) {
        this(raf, offset, limit, DEFAULT_BUFF_SIZE);
    }

    /**
     * Creates a readable buffer that is backed by the given file.  The
     * buffer starts at the given offset in the file and extends for the
     * given number of bytes.  The given buffer size will be used for the
     * in-memory buffer.
     * @param raf The file containing the data for our buffer.
     * @param offset The offset in the file where the data for our buffer
     * can be found.
     * @param limit The number of bytes of data in our buffer.
     * @param buffSize The size of the in-memory buffer to use.
     */
    public FileReadableBuffer(RandomAccessFile raf,
            long offset,
            long limit,
            int buffSize) {
        this.raf = raf;
        bs = offset;
        be = offset + limit;
        pos = bs;
        ms = -1;

        //
        // Fill the buffer if the size of the data is smaller than the size
        // of the buffer.
        if(limit > 0 && limit <= buffSize) {
            buff = new byte[(int) limit];
            int n = read(bs);
            ms = bs;
            me = bs + n;
        } else {
            buff = new byte[buffSize];
        }

    } // FileBackedBuffer constructor

    /**
     * Reads a given number of bytes from the file.
     * @param off The offset in the file from which the bytes should be
     * read.
     * @return The number of bytes actually read from the file.
     */
    protected int read(long off) {
        synchronized(raf) {
            try {
                raf.seek(off);
                return raf.read(buff);
            } catch(java.io.IOException ioe) {
                logger.log(Level.SEVERE, 
                        String.format("Error reading from file buffer: bs: %d be: %d pos: %d off: %d", bs, be, pos, off), ioe);
                return -1;
            }
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
     * Returns the number of bytes remaining to be read in the buffer.
     * @return The number of bytes remaining in the buffer.
     */
    public long remaining() {
        return be - pos;
    }

    /**
     * Duplicates this buffer, so that it can be used safely by other
     * readers.  Note that this duplicates the data in the buffer, since it
     * must not be changed by subsequent reads.
     * @return A new buffer duplicating the contents of this buffer.  The
     * buffers are backed by the same underlying file, but they have
     * different in-memory buffers and positions.
     */
    public ReadableBuffer duplicate() {
        return new FileReadableBuffer(raf, bs, be - bs, buff.length);
    }

    /**
     * Slices this buffer so that a sub-buffer can be used.  The buffer is
     * sliced from the current position.  Note that this actually
     * duplicates the data in the buffer, since it must not be changed by
     * subsequent reads.
     * @param p The position at which the buffer should be sliced.
     * @param s The number of bytes that should be in the sliced buffer.
     * @return A new buffer containing a slice of this buffer.  The new
     * buffer shares the underlying file, but has it's own in-memory
     * buffer.  The first position in the sliced buffer is the given
     * position, and the limit on the sliced buffer is the given size.
     */
    public ReadableBuffer slice(long p, long s) {
        return new FileReadableBuffer(raf, bs + p, s, buff.length);
    }

    /**
     * Gets the limit of this buffer, i.e., the last readable position.
     * @return The last readable position in this buffer.
     */
    public long limit() {
        return be - bs;
    }

    /**
     * Sets the limit of this buffer, i.e., the last readable position.
     * @param l The limit that we wish to set for the buffer.
     */
    public void limit(long l) {
        be = bs + l;
    }

    /**
     * Gets the byte at the given position in the buffer.
     * @param i The position from which we wish to get a byte.
     * @return The byte at the given position.
     */
    public byte get(long i) {
        return buff[checkBounds(i + bs)];
    }

    /**
     * Gets the next byte in the buffer.
     * @return The byte at the current buffer position.  This will advance
     * the current position.
     */
    public byte get() {
        return buff[checkBounds(pos++)];
    }

    /**
     * Gets the position of the buffer.
     * @return The current position in the buffer.
     */
    public long position() {
        return pos - bs;
    }

    /**
     * Positions the buffer.
     * @param position The position to which we should set the buffer.
     */
    public void position(long position) {
        this.pos = bs + position;
    }

    /**
     * Gets a string representation of the buffer.
     * @return A string representation of the buffer.
     */
    @Override
    public String toString() {
        return "buff: (" + bs + "," + be + ")" +
                " mem: (" + ms + "," + me + ") " + buff.length;
    }
} // FileBackedBuffer
