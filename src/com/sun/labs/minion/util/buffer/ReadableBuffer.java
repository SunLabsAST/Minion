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


/**
 * An interface for buffers that can be read from.  The main operations on such
 * buffers are the {@link #get()} operations and the various {@link #byteDecode()}
 * operations.
 * 
 * <p>
 * 
 * Unlike {@link WriteableBuffer}s, we don't provide any methods for reading in a
 * readable buffer.  This is the responsibility of the <code>PostingsInput</code>
 * implementations.
 * @see WriteableBuffer
 * 
 * @see com.sun.labs.minion.indexer.postings.io.PostingsInput
 */
public interface ReadableBuffer extends Buffer {
    
    /**
     * Duplicates this buffer, so that it can be used safely by other
     * readers.
     * @return A new buffer that is a duplicate of the current buffer.  The
     * new buffer will share the underlying representation of the buffer,
     * but will have an independant position.
     */
    public ReadableBuffer duplicate();

    /**
     * Slices this buffer so that a sub-buffer can be used.  The buffer is
     * sliced from the current position.
     * @param p The position at which the buffer should be sliced.
     * @param s The number of bytes that should be in the sliced buffer.
     * @return A new buffer that contains a slice of the data from this
     * buffer.  The new buffer will share the underlying representation of
     * the data, but data will be read starting from the given offset and
     * the new buffer will be limited to the given size.
     */
    public ReadableBuffer slice(int p, int s);

    /**
     * Gets a byte from this buffer.
     * @return The byte at the current position.  Calling this method
     * advances the position.
     */
    public byte get();

    /**
     * Gets a byte from this buffer at the given position.
     * @param i The position from which we would like to read a byte.
     * 
     * @return The byte at the given position.
     */
    public byte get(int i);

    /**
     * Decodes a postive integer that was coded using a specific number of
     * bytes.
     *
     * @param nBytes The number of bytes to use.
     * @return the decoded number.
     */
    public int byteDecode(int nBytes);

    /**
     * Decodes a postive integer that was coded using a specific number of
     * bytes from a specific position in the buffer.
     *
     * @param pos The position to decode from.
     * @param nBytes The number of bytes to use.
     * @return the decoded number.
     */
    public int byteDecode(int pos, int nBytes);

    /**
     * Decodes a postive long that was coded using a specific number of
     * bytes.
     *
     * @param nBytes The number of bytes to use.
     * @return the decoded number.
     */
    public long byteDecodeLong(int nBytes);

    /**
     * Decodes a postive long that was coded using a specific number of
     * bytes from a given position.
     *
     * @param pos The position to decode from.
     * @param nBytes The number of bytes to use.
     * @return the decoded number.
     */
    public long byteDecodeLong(int pos, int nBytes);
    
    /**
     * Decodes an integer stored using our 7 bit encoding scheme.
     * @return the decoded integer.
     * @see WriteableBuffer#byteEncode
     */
    public int byteDecode();

    /**
     * Decodes a long stored using the 7 bit encoding.
     * @return the decoded long.
     * @see WriteableBuffer#byteEncode
     */
    public long byteDecodeLong();

    /**
     * Decode a float stored in 4 bytes.
     *
     * @return the decoded float.
     */
    public float decodeFloat();

    /**
     * Skips an integer encoded using the 7 bit encoding without actually 
     * decoding the value.
     * @return the number of bytes skipped.
     * @see WriteableBuffer#byteEncode
     */
    public int skipByteEncoded();

    /**
     * Decodes a string from this buffer.
     * @return The string.
     * @see WriteableBuffer#encode(CharSequence)
     */
    public String getString();

    /**
     * Tests whether a given bit is true or false.
     *
     * @param bitIndex the index of the bit to test.
     * @return true if the bit is 1, false if it is 0
     */
    public boolean test(int bitIndex);

    /**
     * Counts the number of bits that are on in the buffer.
     * @return The number of 1 bits in the buffer.
     */
    public int countBits();
        
}// ReadableBuffer
