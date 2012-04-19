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
import com.sun.labs.minion.util.buffer.FileReadableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A postings input that is backed by a random access file.
 */
public class FilePostingsInput implements PostingsInput {

    /**
     * The file from which we'll read our postings.
     */
    protected RandomAccessFile raf;

    /**
     * Whether we'll read all of the postings or read them incrementally.
     */
    protected boolean full;

    /**
     * Creates a postings input that will read postings from the given
     * file.
     * 
     * <p>
     * 
     * This implementation is most useful for reading postings that will be used in
     * evaluating queries.
     * @param raf The file that the postings will be read from.
     * @param full If <code>true</code> then all of the postings will be
     * read in at once.
     */
    public FilePostingsInput(RandomAccessFile raf, boolean full) {
        this.raf = raf;
        this.full = full;
    } // FilePostingsInput constructor
    
    // Implementation of com.sun.labs.minion.indexer.postings.io.PostingsInput

    /**
     * Returns a set of postings that are backed by the file on disk, so
     * that minimal memory is used.
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

        if(full) {
            byte[] val = new byte[size];
            synchronized(raf) {
                raf.seek(offset);
                raf.readFully(val);
            }
            return new ArrayBuffer(val);
        } else {
            //
            // We'll use a half-K buffer.
            return new FileReadableBuffer(raf, offset, size, 512);
        }
    }

} // FilePostingsInput
