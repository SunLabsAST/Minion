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

import com.sun.labs.minion.util.buffer.FileReadableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A postings input that extends a file backed buffer.  The postings that
 * are returned are this buffer.  This is useful in situations where we're
 * iterating through a large portion of the postings for a file and the
 * postings are not multipart (e.g., <code>IDPostings</code>).
 */
public class FileBackedPostingsInput extends FileReadableBuffer
    implements PostingsInput {

    protected static String logTag = "FBPI";

    public FileBackedPostingsInput(RandomAccessFile raf,
                                   long offset,
                                   int buffSize) 
        throws java.io.IOException {
        super(raf, offset, buffSize*2, buffSize);
    } // FileBackedPostingsInput constructor
    
    /**
     * Reads a set of postings, returning them in a buffer suitable for
     * decoding.  The buffer that is returned is backed by the file
     * containing the postings.
     * 
     * @param offset The offset in the input at which the postings can be
     * found.
     * @param size The number of bytes to read to get the postings.
     * @throws java.io.IOException if there is any error reading the
     * postings.
     * @return A readable buffer containing the postings.
     */
    @Override
    public ReadableBuffer read(long offset, int size)
        throws java.io.IOException {

        //
        // Reset the members that keep track of the buffer's start and end
        // positions in the underlying file.  The superclass will take care
        // of reading from the file if necessary!
        bs = offset;
        be = offset + size;
        pos = bs;
        return this;
    }

} // FileBackedPostingsInput
