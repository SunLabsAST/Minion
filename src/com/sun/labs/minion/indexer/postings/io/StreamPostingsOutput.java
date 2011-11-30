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
import java.io.IOException;
import java.io.OutputStream;

import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * A postings output that writes the postings to an output stream.  This stream
 * should be buffered for the best performance.
 */
public class StreamPostingsOutput implements PostingsOutput {

    /**
     * The stream to which we will write our postings.
     */
    protected OutputStream stream;

    /**
     * The current offset in the stream.
     */
    protected long offset;

    /**
     * Creates a postings output from the given stream.  It is a very good
     * idea to use a buffered stream! The stream's initial offset is taken
     * to be 0.
     *
     * @param stream The stream to which postings will be written.
     */
    public StreamPostingsOutput(OutputStream stream) {
        this(stream, 0);
    }
    
    /**
     * Creates a postings output from the given stream.  The stream's
     * initial offset is taken to be 0.
     *
     * @param stream The stream to which postings will be written.
     * @param offset The initial offset for the stream.
     */
    public StreamPostingsOutput(OutputStream stream, long offset) {
        this.stream = stream;
        this.offset = offset;
    } // StreamPostingsOutput constructor
    
    /**
     * Writes out the given buffer of postings.
     * @param b A buffer of postings to write.
     * @throws java.io.IOException if there is any error writing the
     * postings.
     * @return The number of bytes written.
     */
    public int write(WriteableBuffer b) throws java.io.IOException {
        int size = b.position();
        b.write(stream);
        offset += size;
        return size;
    }
    
    /**
     * Writes a set of postings encoded onto a number of buffers.
     * @param b The buffers to write.
     * @throws java.io.IOException if there is any error writing the
     * postings.
     * @return The number of bytes written.
     */
    public long write(WriteableBuffer [] b) throws java.io.IOException {
        return write(b, 0, b.length);
    }
    
    /**
     * Writes a subsequence of a set of postings encoded onto a number of
     * buffers to the output.
     * @param b The buffers to write.
     * @param offset The offset in <code>b</code> where we will begin
     * writing bytes.
     * @param length The length of the subsequence of <code>b</code> for
     * which we will write postings.
     * @throws java.io.IOException if there is any error writing the
     * postings.
     * @return The number of bytes written.
     */
    public long write(WriteableBuffer[] b, int offset, int length)
        throws java.io.IOException {
        long ret = 0;
        for(int i = offset; i < offset+length; i++) {
            ret += write(b[i]);
        }
        return ret;
    }

    @Override
    public long write(Postings p) throws IOException {
        return write(p.getBuffers());
    }

    /**
     * Gets the position of the current output.
     * @throws java.io.IOException if there is any error.
     * @return The current position of the output.
     */
    public long position() throws java.io.IOException {
        return offset;
    }

    /**
     * Flushes any buffered output to the underlying storage.
     *
     * @throws java.io.IOException if there is any error flushing the
     * postings.
     */
    public void flush() throws java.io.IOException {
        stream.flush();
    }

    /** 
     * Closes the output stream
     *
     * @throws java.io.IOException if there is an error closing the stream
     */
    public void close() throws java.io.IOException {
        flush();
        stream.close();
    }
} // StreamPostingsOutput
