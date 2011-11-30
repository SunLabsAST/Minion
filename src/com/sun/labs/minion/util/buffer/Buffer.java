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
 * An interface for the buffers used by the search engine.  Buffers have a
 * position that indicates where the next byte will be written or read, a
 * limit that indicates the last byte that may be written or read, and a
 * way to determine how much space is remaining in a buffer.
 *
 * @see WriteableBuffer
 * @see ReadableBuffer
 */
public interface Buffer {

    public static enum DecodeMode {

        BYTE_ENCODED, INTEGER, LONG
    }
    
    public static enum Portion {

        ALL, BEGINNING_TO_POSITION, FROM_POSITION_TO_END
    }
    
    /**
     * Bitmasks to add a single bit to a byte.
     */
    public static int[] masks = {1, 2, 4, 8, 16, 32, 64, 128};
    
    /**
     * The number of bytes that are required for encoding a number given
     * our 7 bit encoding strategy.
     */
    public static final long[] maxBEValues = {
        0,
        1L << 7,
        1L << 14,
        1L << 21,
        1L << 28,
        1L << 35,
        1L << 42,
        1L << 49,
        1L << 56,
        1L << 63,
        Long.MAX_VALUE};

    /**
     * The maximum values that can be encoded using a given number of
     * bytes.
     */
    public static final long[] maxValues = {
        0, 1L << 8, 1L << 16, 1L << 24, 1L
        << 32, 1L << 40, 1L << 48, 1L << 56, Long.MAX_VALUE};

    /**
     * The number of 1 bits in a byte, as a lookup table.
     */
    public static final int[] nBits = {0, 1, 1, 2, 1, 2, 2, 3, 1, 2, 2, 3, 2, 3,
        3, 4, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 1, 2, 2, 3, 2, 3,
        3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5,
        5, 6, 1, 2, 2, 3, 2, 3, 3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4,
        4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5,
        5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 1, 2, 2, 3, 2, 3,
        3, 4, 2, 3, 3, 4, 3, 4, 4, 5, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5,
        5, 6, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5, 5, 6, 3, 4, 4, 5, 4, 5,
        5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 2, 3, 3, 4, 3, 4, 4, 5, 3, 4, 4, 5, 4, 5,
        5, 6, 3, 4, 4, 5, 4, 5, 5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 3, 4, 4, 5, 4, 5,
        5, 6, 4, 5, 5, 6, 5, 6, 6, 7, 4, 5, 5, 6, 5, 6, 6, 7, 5, 6, 6, 7, 6, 7,
        7, 8};

    /**
     * Gets the position in the buffer.
     *
     * @return The position in the buffer.
     */
    public long position();

    /**
     * Sets the position in the buffer.
     *
     * @param position The point to which the buffer's position should be
     * set.
     */
    public void position(long position);

    /**
     * Gets the limit of this buffer, i.e., the last readable position.
     */
    public long limit();

    /**
     * Sets the limit of this buffer, i.e., the last readable position.
     */
    public void limit(long l);

    /**
     * Gets the amount of space remaining in the buffer.
     */
    public long remaining();

    /**
     * Counts the number of bits that are on in the buffer.
     * @return The number of 1 bits in the buffer.
     */
    long countBits();

    /**
     * Gets a byte from this buffer at the given position.
     * @param i The position from which we would like to read a byte.
     *
     * @return The byte at the given position.
     */
    byte get(long i);

    /**
     * Generates a string representation of a portion of the buffer indicated
     * by the mode.
     */
    String toString(ReadableBuffer.Portion portion, ReadableBuffer.DecodeMode decode);

    /**
     * Generates a string representation of a portion of the buffer.
     *
     * @param start the starting offset in the buffer
     * @param end the exclusive ending offset in the buffer
     * @return a string with a representation of the bits.
     */
    String toString(long start, long end, ReadableBuffer.DecodeMode decode);

}// Buffer
