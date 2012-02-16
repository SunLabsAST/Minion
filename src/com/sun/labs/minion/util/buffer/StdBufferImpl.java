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

import java.nio.charset.Charset;
import java.util.logging.Logger;


/**
 * A abstract class that implements most of the
 * <code>WriteableBuffer</code> and <code>ReadableBuffer</code> interfaces
 * in terms of the two implemented interfaces' <code>get</code> and
 * <code>put</code> methods.  These can be overidden in subclasses if the
 * performance is unacceptable.
 * @see WriteableBuffer
 * @see ReadableBuffer
 */
public abstract class StdBufferImpl implements WriteableBuffer, ReadableBuffer {
    
    private static final Logger logger = Logger.getLogger(StdBufferImpl.class.getName()); 
    
    /**
     * Gets the number of bytes required to directly encode a given number.
     * @return The number of bytes required to directly encode the
     * number.
     * @param n The number that we want to encode.
     */
    public static int bytesRequired(long n) {
        for(int i = 1; i < maxValues.length; i++) {
            if(n < maxValues[i]) {
                return i;
            }
        }
        return 0;
    }

    /**
     * Gets the number of bytes required to byte-encode a given number.
     *
     * @param n the number to test
     * @return the number of bytes require to byte encode the given number.
     */
    public static int bytesForByteEncoding(long n) {
        int nBytes = 0;
        do {
            n >>>= 7;
            nBytes++;
        } while(n > 0);

        return nBytes;
    }

    //
    // The implementation of the Buffer methods is left abstract.

    //
    // Implementation of the encoding WriteableBuffer methods.
    // put/append/set are left for concrete subclasses.

    /**
     * Encodes a positive long onto a writable in a given number of bytes.  This
     * encoding is compatible with DataOutput's writeInt.
     * 
     * @return The buffer, to allow chained invocations.
     * @param n The number to encode.
     * @param nBytes The number of bytes to use in the encoding.
     */
    public WriteableBuffer byteEncode(long n, int nBytes) {
        for(int shift = 8 * (nBytes-1); shift >= 0; shift -= 8) {
            put((byte) ((n >>> shift) & 0xFF));
        }
        return this;
    }

    /**
     * Encodes a positive long directly, using a given number of
     * bytes, starting at the given position in the units.
     * 
     * @return The buffer, to allow chained invocations.
     * @param pos The position to start encoding.
     * @param n The number to encode.
     * @param nBytes The number of bytes to use in the encoding.
     */
    public WriteableBuffer byteEncode(long pos, long n, int nBytes) {
        for(int shift = 8 * (nBytes-1); shift >= 0; shift -= 8) {
            put(pos++, (byte) ((n >>> shift) & 0xFF));
        }
        return this;
    }    

    /**
     * Encodes an integer in a byte-aligned fashion, using the minimal
     * number of bytes.  The basic idea: use the 7 lower order bits of a
     * byte to encode a number.  If the 8th bit is 0, then there are no
     * further bytes in this number.  If the 8th bit is one, then the next
     * byte continues the number.  Note that this means that a number that
     * would completely fill an integer will take 5 bytes to encode.
     * 
     * @return the number of bytes used to encode the number.
     * @param n The number to encode.
     */
    public int byteEncode(long n) {
        if(n < 0) {
            throw new ArithmeticException(String.format("Negative value %d cannot by byte encoded", n));
        }
        int nBytes = 0;

        //
        // We need to encode 0 as a byte, so we use a do/while.
        do {
            byte curr = (byte) (n & 0x7F);
            if(n > 0x7f) {
                curr |= 0x80;
            }
            put(curr);
            n >>>= 7;
            nBytes++;
        } while(n > 0);
	
        return nBytes;
    }

    /**
     * Encodes a floating point value in 4 bytes.
     *
     * @param f the floating point number to encode
     * @return the number of bytes used to encode the float, which is
     * always 4.
     */
    public WriteableBuffer encode(float f) {
        return byteEncode(Float.floatToRawIntBits(f), 4);
    }

    /**
     * Determines the size of the UTF-8 encoding of a character sequence.
     * @param s The sequence that we wish to encode.
     * @return The number of bytes in the UTF-8 encoding of the sequence.
     */
    protected static int sizeUTF8(CharSequence s) {
        int l = 0;
        for(int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            if (c < 128) {
                l++;
            } else if (c <= 0x7ff) {
                l += 2;
            } else if (c <= 0xffff) {
                l += 3;
            } else if (c <= 0x10ffff) {
                l += 4;
            }
        }
        return l;
    }

    /**
     * Appends a readable buffer onto this buffer.
     * @return The buffer, to allow chained invocations.
     * @param b The buffer that we wish to append onto this buffer.
     */
    public WriteableBuffer append(ReadableBuffer b) {
        return append(b, b.remaining());
    }

    /**
     * Appends a given number of bytes from a readable buffer onto this
     * buffer.
     * @return The buffer, to allow chained invocations.
     * @param b The buffer that we wish to append onto this buffer.
     * @param n The number of bytes from the given buffer to append onto this buffer.
     */
    public WriteableBuffer append(ReadableBuffer b, long n) {

        //
        // This is probably very slow!
        for(long i = 0; i < n; i++) {
            put(b.get());
        }
        return this;
    }

    /**
     * Encodes a character sequence onto the buffer.  The sequence is
     * encoded in the following way: first, the number of bytes used to
     * encode the string is encoded using the <CODE>byteEncode</CODE>
     * method.  Then, the characters in the sequence are encoded using a
     * UTF-8 encoding.
     * 
     * @param s The sequence that we wish to encode.
     * @return This buffer, for chained invocations.
     */
    public WriteableBuffer encode(CharSequence s) {

        int l = sizeUTF8(s);
        byteEncode(l);
        
        for(int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            if (c < 128) {
                put((byte) c);
            } else if (c <= 0x7ff) {
                put((byte) ((c >> 6) | 0xc0));
                put((byte) ((c & 0x3f) | 0x80));
            } else if (c <= 0xffff) {
                put((byte) (        (c >> 12) | 0xe0));
                put((byte) (((c >> 6) & 0x3f) | 0x80));
                put((byte) (       (c & 0x3f) | 0x80));
            } else if (c <= 0x10ffff) {
                put((byte) (         (c >> 18) | 0xf0));
                put((byte) (((c >> 12) & 0x3f) | 0x80));
                put((byte) ( ((c >> 6) & 0x3f) | 0x80));
                put((byte) (        (c & 0x3f) | 0x80));
            }
        }
        return this;
    }

    public WriteableBuffer encodeAsBytes(String s, Charset cs) {
        byte[] bytes = s.getBytes(cs);
        byteEncode(bytes.length, 4);
        put(bytes);
        return this;
    }

    /**
     * Sets the given bit to 1 in the given buffer.
     * @param bitIndex the index of the bit to set to 1.
     * @return This buffer, for chained invocations.
     */
    public WriteableBuffer set(long bitIndex) {
        long i = bitIndex >>> 3;
        capacity(i+1);
        put(i, (byte) (get(i) | masks[(int) (bitIndex & 0x07L)]));
        if(i >= position()) {
            position(i+1);
        }
        return this;
    }

    /**
     * Clears the buffer.  This default implementation simply sets the
     * position to 0.
     * @return This buffer, for chained invocations.
     */
    public WriteableBuffer clear() {
        position(0);
        return this;
    }

    //
    // Partial implementation of ReadableBuffer. get, limit are left for
    // subclasses.

    /**
     * Decodes a positive integer that was coded using a specific number of
     * bytes.
     *
     * @param nBytes The number of bytes to use.
     * @return the decoded number.
     */
    public int byteDecode(int nBytes) {
        return (int) byteDecodeLong(nBytes);
    }

    /**
     * Decodes an integer that was coded using a specific number of
     * bytes from a specific position in the buffer.
     *
     * @param pos The position to decode from.
     * @param nBytes The number of bytes to use.
     * @return the decoded number.
     */
    public int byteDecode(long pos, int nBytes) {
        return (int) byteDecodeLong(pos, nBytes);
    }

    /**
     * Decodes a long that was coded using a specific number of
     * bytes.
     * 
     * @return the decoded number.
     * @param nBytes The number of bytes to use.
     */
    public long byteDecodeLong(int nBytes) {
        long ret = 0;
        for(int i = 0, shift = (nBytes - 1) * 8; i < nBytes; i++, shift -= 8) {
            ret |= (long) (get() & 0xFF) << shift;
        }
        return ret;
    }

    /**
     * Decodes a long that was coded using a specific number of
     * bytes from a given position.
     * 
     * @return the decoded number.
     * @param pos The position to decode from.
     * @param nBytes The number of bytes to use.
     */
    public long byteDecodeLong(long pos, int nBytes) {
        long ret = 0;
        for(int i = 0, shift = (nBytes - 1) * 8; i < nBytes; i++, shift -= 8) {
            ret |= (long) (get(pos++) & 0xFF) << shift;
        }
        return ret;
    }

    /**
     * Decodes an integer stored using the minimal number of bytes.
     * @return the decoded integer.
     * @see #byteEncode
     */
    public int byteDecode() {
        return (int) byteDecodeLong();
    }

    /**
     * Decodes a long stored using the 7 bit encoding.
     * @return the decoded long.
     * @see #byteEncode
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
     * Decode a float stored in 4 bytes.
     *
     * @return the decoded float.
     */
    public float decodeFloat() {
        return Float.intBitsToFloat(byteDecode(4));
    }

    /**
     * Skips an integer encoded using our 7 bit encoding without actually
     * decoding it.
     * @return the number of bytes skipped.
     */
    public int skipByteEncoded() {
        long init = position();
        while((get() & 0x80) != 0);
        return (int) (position() - init);
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
        return i >= limit() ? false :
            ((get(i) & masks[(int) (bitIndex & 0x07L)]) != 0);
    }

    /**
     * Gets a string from this buffer.
     * @return The decoded string.
     */
    @Override
    public String getString() {
        int l = byteDecode();
        char[] ret = new char[l];
        int n = 0;
        for(int i = 0; i < l; ) {
            byte b = get();
            int c;
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
    

    @Override
    public String toString() {
        return toString(0, position(), DecodeMode.BYTE_ENCODED);
    }

    @Override
    public long countBits() {
        return countBits(0, limit());
    }

    public long countBits(long start, long end) {
        int n = 0;
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

        if (start < 0 || end < 0) {
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
        
        for (long i = start, j = 0; i < end; i++, j += 8) {
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
                    if ((curr & 0x80) == 0) {
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
                    if (inCurr % 8 == 0) {
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

    /**
     * Build a string representation of a byte with the bits in the right
     * order.
     * @param n The number to represent
     * @return A string containing a representation of the bits in this byte.
     */
    public static String byteToBinaryString(byte n) {

        byte save = n;
        StringBuilder b = new StringBuilder(8);

        for(int i = 0; i < 8; i++) {
            if(i == 4) {
                b.append("|");
            }
            if((n & (byte) 0x80) != 0) {
                b.append("1");
            } else {
                b.append("0");
            }
            n <<= 1;
        }

        b.append(String.format(" (%3d)", ((int) (save & 0xff))));
        return b.toString();
    }
} // StdBufferImpl
