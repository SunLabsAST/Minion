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
     * Decodes a positive integer that was coded using a specific number of
     * bytes.
     *
     * @param nBytes The number of bytes to use.
     * @return the decoded number.
     */
    @Override
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
    @Override
    public int byteDecode(long pos, int nBytes) {
        return (int) byteDecodeLong(pos, nBytes);
    }
    
    @Override
    public long byteDecodeLong(int nBytes) {
        long ret = 0;
        for(int i = 0, shift = (nBytes - 1) * 8; i < nBytes; i++, shift -= 8) {
            ret |= (long) (get() & 0xFF) << shift;
        }
        return ret;
    }

    @Override
    public long byteDecodeLong(long pos, int nBytes) {
        long ret = 0;
        for(int i = 0, shift = (nBytes - 1) * 8; i < nBytes; i++, shift -= 8) {
            ret |= (long) (get(pos++) & 0xFF) << shift;
        }
        return ret;
    }

    /**
     * Decodes an integer stored using the minimal number of bytes.
     *
     * @return the decoded integer.
     * @see WriteableBuffer#byteEncode
     */
    @Override
    public int byteDecode() {
        return (int) byteDecodeLong();
    }

    /**
     * Decodes a long stored using the byte encoding.
     *
     * @return the decoded long.
     * @see WriteableBuffer#byteEncode
     */
    @Override
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
    @Override
    public int skipByteEncoded() {
        long init = position();
        while((get() & 0x80) != 0);
        return (int) (position() - init);
    }

    /**
     * Decode a float stored in 4 bytes.
     *
     * @return the decoded float.
     */
    @Override
    public float decodeFloat() {
        return Float.intBitsToFloat(byteDecode(4));
    }

    /**
     * Tests whether a given bit is true or false.
     *
     * @param bitIndex the index of the bit to test.
     * @return true if the bit is 1, false if it is 0
     */
    @Override
    public boolean test(long bitIndex) {
        long i = bitIndex >>> 3;
        return i > limit() ? false :
            ((get(i) & StdBufferImpl.masks[(int) (bitIndex & 0x07L)]) != 0);
    }
    
    /**
     * Gets a string from this buffer.
     * @return A string representation of this buffer.
     */
    @Override
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
     * Counts the number of bits that are set in a buffer.
     * @return The number of 1 bits in the buffer.
     */
    @Override
    public long countBits() {
        return countBits(0, limit());
    }

    public long countBits(long start, long end) {
        long n = 0;
        for (long i = start; i < end; i++) {
            n += StdBufferImpl.nBits[(int) (get(i) & 0xff)];
        }
        return n;
    }

    @Override
    public String toString(Portion portion, DecodeMode decode) {

        long start;
        long end;

        switch (portion) {
            case ALL:
                start = 0;
                end = limit();
                break;
            case BEGINNING_TO_POSITION:
                start = 0;
                end = position();
                break;
            case FROM_POSITION_TO_END:
                start = position();
                end = limit();
                break;
            default:
                start = 0;
                end = limit();
        }

        return toString(start, end, decode);
    }

    @Override
    public String toString(long start, long end, DecodeMode decode) {

        StringBuilder b = new StringBuilder((int) (end - start + 1) * 8);
        b.append(String.format("position: %d limit: %d\n", position(), limit()));

        if(start < 0 || end < 0) {
            return b.toString();
        }

        b.append(countBits(start, end)).append(" bits set\n");
        int currDecodeInt = 0;
        long currDecode = 0;
        int inCurr = 0;

        int shift = 0;
        switch(decode) {
            case BYTE_ENCODED:
                shift = 0;
                break;
            case INTEGER:
                shift = 24;
                break;
            case LONG:
                shift = 56;
                break;
        }

        for(long i = start, j = 0; i < end; i++, j += 8) {
            byte curr = get(i);
            b.append(String.format("%s%7d%7d %s",
                    i > start ? "\n" : "",
                    j, i,
                    StdBufferImpl.byteToBinaryString(curr)));

            //
            // See if we need to dump an encoded integer that we've been collecting.
            switch(decode) {
                case BYTE_ENCODED:
                    currDecode |= ((long) (curr & 0x7F)) << shift;
                    if((curr & 0x80) == 0) {
                        b.append(String.format(" %8d", currDecode));
                        currDecode = 0;
                        shift = 0;
                    } else {
                        shift += 7;
                    }
                    break;
                case INTEGER:
                    currDecodeInt |= ((int) (curr & 0xFF)) << shift;
                    inCurr++;
                    shift -= 8;
                    if(inCurr % 4 == 0) {
                        b.append(String.format(" %8d", currDecodeInt));
                        currDecodeInt = 0;
                        shift = 24;
                        inCurr = 0;
                    }
                    break;
                case LONG:
                    currDecode |= ((long) (curr & 0xFF)) << shift;
                    inCurr++;
                    shift -= 8;
                    if(inCurr % 8 == 0) {
                        b.append(String.format(" %8d", currDecode));
                        currDecode = 0;
                        shift = 56;
                        inCurr = 0;
                    }
                    break;
            }

        }
        return b.toString();
    }
} // StdReadableImpl
