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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.OutputStream;

import java.nio.ByteBuffer;

import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;


/**
 * An interface for buffers that can be written to.  The main operations
 * supported by such buffers are the <code>put</code> methods that allow a
 * single byte to be placed on the buffer, and the various
 * <code>byteEncode</code> methods that allow a single integer to be
 * encoded using a specified number of bytes or to be encoded in the
 * minimal number of bytes using a 7 bit encoding.  Additionally, we allow
 * strings to be encoded onto a buffer using a UTF-8 encoding.
 * 
 * <p>
 * 
 * Writeable buffers can be written to a number of different outputs.
 * Implementing classes may check for specific instances of the various outputs
 * in order to provide faster output.
 * 
 * <p>
 * 
 * It is an absolute requirement that implementations be able to grow as needed: 
 * users of the buffers will expect this to be the case.
 */
public interface WriteableBuffer extends Buffer {
    
    /**
     * Sets the capacity of the buffer to the given amount.
     *
     * @param n The number of bytes that the buffer must be able to store.
     * @return The buffer, for chained invocations.
     */
    public WriteableBuffer capacity(long n);

    /**
     * Puts a single byte onto this buffer.
     * @param b The byte to put on the buffer
     * @return This buffer, for chained invocations.
     */
    public WriteableBuffer put(byte b);
    
    /**
     * Puts a single byte onto this buffer at the given position.
     * @param p The position where the byte should be put.
     * @param b The byte to put on the buffer
     * @return This buffer, for chained invocations.
     */
    public WriteableBuffer put(long p, byte b);
    
    /**
     * Puts an array of bytes onto this buffer at the current position.
     */
    public WriteableBuffer put(byte[] bytes);

    /**
     * Encodes a long directly, using a given number of
     * bytes.  Note that if <code>n</code> is negative, and <code>nBytes</code> is less than 
     * 8, it's likely that you will not be able to recover the actual number.
     *
     * @param n The number to encode.
     * @param nBytes The number of bytes to use in the encoding.
     * @return The buffer, to allow chained invocations.
     */
    public WriteableBuffer byteEncode(long n, int nBytes);
    
    /**
     * Encodes a positive long directly, using a given number of
     * bytes, starting at the given position in the buffer.
     *
     * @param pos The position in the buffer where we should start
     * encoding.
     * @param n The number to encode.
     * @param nBytes The number of bytes to use in the encoding.
     * @return The buffer, to allow chained invocations.
     * @throws ArithmeticException if the integer to encode is less than 0.
     */
    public WriteableBuffer byteEncode(long pos, long n, int nBytes);

    /**
     * Encodes an integer in a byte-aligned fashion, using the minimal
     * number of bytes.  The basic idea: use the 7 lower order bits of a
     * byte to encode a number.  If the 8th bit is 0, then there are no
     * further bytes in this number.  If the 8th bit is one, then the next
     * byte continues the number.  Note that this means that a number that
     * would completly fill an integer will take 5 bytes to encode.
     *
     * @param n The number to encode.
     * @return the number of bytes used to encode the number.
     * @throws ArithmeticException if the integer to encode is less than 0.
     */
    public int byteEncode(long n) throws ArithmeticException;

    /**
     * Encodes a floating point value in 4 bytes.
     *
     * @param f the floating point number to encode
     * @return the buffer
     */
    public WriteableBuffer encode(float f);

    /**
     * Appends a readable buffer onto this buffer.
     * @return The buffer, to allow chained invocations.
     * @param b The buffer that we wish to append onto this buffer.
     */
    public WriteableBuffer append(ReadableBuffer b);

    /**
     * Appends a given number of bytes from a readable buffer onto this
     * buffer.
     * @return The buffer, to allow chained invocations.
     * @param b The buffer that we wish to append onto this buffer.
     * @param n The number of bytes to append onto this buffer.
     */
    public WriteableBuffer append(ReadableBuffer b, long n);

    /**
     * Computes the logical OR of this buffer and another.
     *
     * @param b The buffer to or with this one.
     * @return The buffer, to allow chained invocations.
     */
    public WriteableBuffer or(ReadableBuffer b);

    /**
     * Computes the logical XOR of this buffer and another.
     *
     * @param b The buffer to or with this one.
     * @return The buffer, to allow chained invocations.
     */
    public WriteableBuffer xor(ReadableBuffer b);

    /**
     * Encodes a character sequence onto this buffer.  The sequence is encoded in the
     * following way:  first, the number of bytes used to encode the string is
     * encoded using the <CODE>byteEncode</CODE> method.  Then, the characters in the
     * sequence are encoded using a UTF-8 encoding.
     * @param s The sequence to encode
     * @return The buffer, to allow chained invocations.
     */
    public WriteableBuffer encode(CharSequence s);
    
    /**
     * Encodes this string as a number of UTF-8 bytes.  This method
     * is different from {@link #encode(java.lang.CharSequence)} in that the 
     * number of bytes is encoded using 4 bytes, so that the data can be read
     * in by a {@link DataInput}.
     */
    public WriteableBuffer encodeAsBytes(String s, Charset cs);

    /**
     * Sets the given bit to 1.
     * @param bitIndex the index of the bit to set to 1.
     * @return This buffer, for chained invocations.
     */
    public WriteableBuffer set(long bitIndex);

    /**
     * Clears out the buffer.
     * @return This buffer, for chained invocations.
     */
    public WriteableBuffer clear();

    /**
     * Gets a readable buffer from this writeable one.  It is OK for the
     * readable version of the buffer to share the representation of the
     * buffer contents.
     * @return A version of this buffer as a readable buffer.  The readable buffer <I>may</I>
     * share the underlying representation with the writeable buffer, but this is not
     * required.
     */
    public ReadableBuffer getReadableBuffer();

    /**
     * Write the buffer to a new IO buffer.
     * @param b The <CODE>java.nio</CODE> buffer to which we will write this buffer.
     */
    public void write(ByteBuffer b);
    
    /**
     * Write this buffer to another.
     * @param b the buffer to which we'll write our data.
     */
    public void write(WriteableBuffer b);

    /**
     * Write the buffer to a channel.
     * @param chan The channel to which the buffer should be written.
     * @throws java.io.IOException if there is any error writing the buffer.
     */
    public void write(WritableByteChannel chan) throws java.io.IOException;
    
    /**
     * Writes a portion of the buffer to a channel
     * @param chan the channel to write to
     * @param start the inclusive starting position in the buffer
     * @param end  the exclusive ending position in the buffer.
     */
    public void write(WritableByteChannel chan, long start, long end) throws java.io.IOException;

    /**
     * Write the buffer to a data output.
     * @param o The output to which the buffer should be written.
     * @throws java.io.IOException If there is any error writing the buffer.
     */
    public void write(DataOutput o) throws java.io.IOException;

    /**
     * Write the buffer to a stream.
     * @param os The stream to which the buffer should be written.
     * @throws java.io.IOException If there is any error writing the buffer.
     */
    public void write(OutputStream os) throws java.io.IOException;

}// WriteableBuffer
