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

import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A postings input that can be used when streaming through a lot of postings
 * sequentially as when merging or evaluating field queries. Random access is
 * not supported, but sequential access should work well.
 */
public class StreamPostingsInput implements PostingsInput {

    /**
     * A random access file.
     */
    RandomAccessFile raf;

    /**
     * The offset of the start of our in-memory buffer.
     */
    protected long ms;

    /**
     * The offset of the end of our in-memory buffer.
     */
    protected long me;

    /**
     * Our in-memory buffer.
     */
    protected byte[] b;

    private static final Logger logger = Logger.getLogger(StreamPostingsInput.class.
            getName());

    /**
     * Creates a stream postings input.
     *
     * @param postFile The file from which the postings should be read.
     * @param buffSize The size of the buffer to use.
     * @throws java.io.IOException If there is any error reading the postings.
     */
    public StreamPostingsInput(RandomAccessFile postFile,
                               int buffSize)
            throws java.io.IOException {
        this(postFile, 0, buffSize);
    } // StreamPostingsInput constructor

    /**
     * Creates a stream postings input.
     *
     * @param postFile The file from which the postings should be read.
     * @param offset The offset at which the postings start.
     * @param buffSize The size of the buffer to use.
     * @throws java.io.IOException If there is any error reading the postings.
     */
    public StreamPostingsInput(RandomAccessFile postFile,
                               long offset,
                               int buffSize)
            throws java.io.IOException {
        raf = postFile;
        b = new byte[buffSize];
        read(offset);
    } // StreamPostingsInput constructor

    /**
     * Reads some bytes from the stream.
     *
     * @param off The offset in the file from which the bytes should be read.
     * @throws java.io.IOException If there is any error reading from the
     * stream.
     * @return The number of bytes read.
     */
    protected int read(long off) throws java.io.IOException {
        synchronized(raf) {
            raf.seek(off);
            int n = raf.read(b);
            ms = off;
            me = off + n;
            return n;
        }
    }

    /**
     * Returns a buffer constructed from our internal buffer.
     *
     * @param offset The offset in the input at which the postings can be found.
     * @param size The number of bytes to read to get the postings.
     * @throws java.io.IOException if there is any error reading the postings.
     * @return A readable buffer containing the postings.
     */
    @Override
    public ReadableBuffer read(long offset, int size)
            throws java.io.IOException {

        ArrayBuffer ret = new ArrayBuffer(size);

        if(offset < ms) {
            read(offset);
        }

        //
        // The number of bytes that we can pull out of our current buffer.
        int ib = 0;

        //
        // We may have skipped some postings in the stream due to (for example)
        // their lack of uncased terms.  We need to make sure that this offset 
        // is still in our range!
        if(offset < me) {
            ib = (int) (me - offset);
        }

        //
        // Get what we can from the current buffer.
        if(ib >= size) {

            //
            // We can get it all.
            ret.put(b, (int) (offset - ms), size);
            ret.position(0);
            ret.limit(size);
            return ret;
        }

        //
        // The number of bytes left to transfer.
        int left = size;

        //
        // We can get some.
        if(ib > 0) {
            ret.put(b, (int) (offset - ms), ib);
            left -= ib;
            offset += ib;
        }

        //
        // Loop, reading blocks of data until we've filled the buffer.
        while(left > 0) {
            int n = read(offset);
            int x = Math.min(left, n);
            offset += n;
            ret.put(b, 0, x);
            left -= x;
        }
        ret.position(0);
        ret.limit(size);
        return ret;
    }
} // StreamPostingsInput
