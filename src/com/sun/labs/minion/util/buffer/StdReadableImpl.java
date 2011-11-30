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
 * A class that implements the decoding routines in ReadableBuffer so that
 * readable only buffer implementations can share it.
 */
public abstract class StdReadableImpl implements ReadableBuffer {

    /**
     * Decodes a postive integer that was coded using a specific number of
     * bytes.
     *
     * @param nBytes The number of bytes to use.
     * @return the decoded number.
     */
    public int byteDecode(int nBytes) {
        return (int) byteDecodeLong(nBytes);
    }
    
    /**
     * Decodes a postive integer that was coded using a specific number of
     * bytes from a specific position in the buffer.
     *
     * @param pos The position to decode from.
     * @param nBytes The number of bytes to use.
     * @return the decoded number.
     */
    public int byteDecode(int pos, int nBytes) {
        return (int) byteDecodeLong(pos, nBytes);
    }
    
    /**
     * Decodes a postive long that was coded using a specific number of
     * bytes.
     *
     * @param nBytes The number of bytes to use.
     * @return the decoded number.
     */
    public long byteDecodeLong(int nBytes) {
        long ret = 0;
        int shift = 0;
        for(int i = 0; i < nBytes; i++) {
            ret |= (long) (get() & 0xFF) << shift;
            shift += 8;
        }
        return ret;
    }

    /**
     * Decodes a postive long that was coded using a specific number of
     * bytes from a given position.
     *
     * @param pos The position to decode from.
     * @param nBytes The number of bytes to use.
     * @return the decoded number.
     */
    public long byteDecodeLong(int pos, int nBytes) {
        long ret = 0;
        int shift = 0;
        for(int i = 0; i < nBytes; i++) {
            ret |= (long) (get(pos++) & 0xFF) << shift;
            shift += 8;
        }
        return ret;
    }

    /**
     * Decodes an integer stored using the minimal number of bytes.
     *
     * @return the decoded integer.
     * @see WriteableBuffer#byteEncode
     */
    public int byteDecode() {
        return (int) byteDecodeLong();
    }

    /**
     * Decodes a long stored using the byte encoding.
     *
     * @return the decoded long.
     * @see WriteableBuffer#byteEncode
     */
    public long byteDecodeLong() {

        byte 	curr  = (byte) 0x80;
        long 	res   = 0;
        int 	shift = 0;

        while((curr & 0x80) != 0) {
            curr = get();
            res |= ((long) (curr  & 0x7F)) << shift;
            shift += 7;
        }
        return res;
    }

    /**
     * Skips a byte encoded integer without decoding it.
     *
     * @return the number of bytes skipped.
     */
    public int skipByteEncoded() {
        int init = position();
        while((get() & 0x80) != 0);
        return position() - init;
    }

    /**
     * Decode a float stored in 4 bytes.
     *
     * @return the decoded float.
     */
    public float decodeFloat() {
        return Float.intBitsToFloat(byteDecode(4));
    }

    /**
     * Tests whether a given bit is true or false.
     *
     * @param bitIndex the index of the bit to test.
     * @return true if the bit is 1, false if it is 0
     */
    public boolean test(int bitIndex) {
        int i = bitIndex >>> 3;
        return i > limit() ? false :
            ((get(i) & StdBufferImpl.masks[bitIndex & 0x07]) != 0);
    }
    
    /**
     * Counts the number of bits that are set in a buffer.
     * @return The number of 1 bits in the buffer.
     */
    public int countBits() {
        int n = 0;
        for(int i = 0; i < limit(); i++) {
            n += StdBufferImpl.nBits[(int) (get(i) & 0xff)];
        }
        return n;
    }

    /**
     * Gets a string from this buffer.
     * @return A string representation of this buffer.
     */
    public String getString() {
        int len = byteDecode();
        char[] ret = new char[len];
        int n = 0;
        for(int i = 0; i < len; ) {
            byte b = get();
            if ((b & 0x80) == 0) {
                ret[n++] = (char) b;
                i++;
            } else if ((b & 0xe0) == 0xc0) {
                // 110w wwww 10zz zzzz
                // xxxx xwww wwzz zzzz
                ret[n++] = (char) (((b & 0x1f) << 6) |
                                   (get() & 0x3f));
                i += 2;
            } else if ((b & 0xf0) == 0xe0) {
                // 1110 wwww 10zz zzzz 10xx xxxx
                // wwww zzzz zzxx xxxx
                ret[n++] = (char) (((b & 0x0f) << 12)  |
                                   ((get() & 0x3f) << 6) |
                                   (get() & 0x3f));
                i += 3;
            } else {
                // 1111 0www 10zz zzzz 10xx xxxx 10yy yyyy
                // wwwwzz zzzzxxxx xxyyyyyy
                ret[n++] = (char) (((b & 0x7) << 18) |
                                   ((get() & 0x3f) << 12) |
                                   ((get() & 0x3f) << 6) |
                                   (get() & 0x3f));
                i += 4;
            }
        }

        return new String(ret, 0, n);
    }

    /**
     * Gets a string representation of the bytes in this buffer.
     * @return A string representation of the buffer.
     */
    public String toString() {
        return toString(0, position());
    }

    /**
     * Print the bits in the buffer in the order in which they actually
     * occur.
     * @param mode The type of print out required.
     * @return A string representation of the buffer.
     */
    public String toString(int mode) {

        int start;
        int end;

        switch(mode) {
        case 0:
            start = 0;
            end = limit();
            break;
        case 1:
            start = 0;
            end = position();
            break;
        case 2:
            start = position();
            end = limit();
            break;
        default:
            start = 0;
            end = limit();
        }

        return toString(start, end);
    }

    /**
     * Print the bits in the buffer in the order in which they actually
     * occur.
     * @param start The starting position in the buffer from which to
     * display the bytes.
     * @param end The (exclusive) ending position in the buffer.
     * @return A string representation of the buffer.
     */
    public String toString(int start, int end) {
        
        if(start < 0 || end < 0) {
            return String.format("position: %d limit: %d", position(), limit());
        }
        
        StringBuilder b = new StringBuilder((end - start + 1) * 8);

        for(int i = start; i < end; i++) {
            if(i > start) {
                b.append('\n');
            }
            if(i < 10) {
                b.append("   ");
            } else if(i < 100) {
                b.append("  ");
            } else if(i < 1000) {
                b.append(" ");
            }
            b.append(i).append(' ').append(StdBufferImpl.byteToBinaryString(get(i)));
        }
        return b.toString();
    }


    
} // StdReadableImpl
