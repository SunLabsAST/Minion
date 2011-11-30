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

import com.sun.labs.minion.util.ChannelUtil;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;


/**
 * A buffer that uses java.nio buffers as the backing store.
 *
 * <p>
 *
 * This implementation is suitable for use in postings types.
 *
 * @see com.sun.labs.minion.indexer.postings.Postings
 */
public class NIOBuffer extends StdBufferImpl {

    /**
     * A buffer backing our data.
     */
    protected ByteBuffer units;

    /**
     * Creates a buffer that can be used when duplicating or slicing.
     */
    protected NIOBuffer() {
    }

    /**
     * Creates a buffer with the given initial capacity.
     * @param n The initial capacity of the buffer.
     */
    public NIOBuffer(int n) {
        this(n, false);
    }

    /**
     * Creates a buffer using the given buffer.  This will duplicate the
     * given buffer.
     * @param units The buffer to duplicate and use as our internal representation.
     */
    public NIOBuffer(ByteBuffer units) {
        this.units = units.duplicate();
    }

    /**
     * Creates a buffer with the given initial capacity.
     *
     * @param n The initial capacity.
     * @param direct If true, the buffer should be allocated as a direct
     * buffer.
     */
    public NIOBuffer(int n, boolean direct) {
        if(direct) {
            units = ByteBuffer.allocateDirect(n);
        } else {
            units = ByteBuffer.allocate(n);
        }
    } // NIOBuffer constructor
    
    /**
     * Checks whether the given position is outside our array bounds.  If
     * it is, we expand so that it is not.
     * @param p The position which we want to check.
     */
    protected void checkBounds(long p) {
        if(p >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("NIOBuffer can only hold Integer.MAX_VALUE bytes");
        }
        
        int ip = (int) p;
        if(ip >= units.limit()) {
            ByteBuffer temp;
            if(units.isDirect()) {
                temp = ByteBuffer.allocateDirect(ip*2);
            } else {
                temp = ByteBuffer.allocate(ip*2);
            }
            temp.put((ByteBuffer) units.flip());
            units = temp;
        }
    }
    
    /**
     * Gets the position in the buffer.
     *
     * @return The position in the buffer.
     */
    public long position() {
        return units.position();
    }

    /**
     * Sets the position in the buffer.
     *
     * @param position The point to which the buffer's position should be
     * set.
     */
    public void position(long position) {
        if(position >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("NIOBuffer can only hold Integer.MAX_VALUE bytes");
        }
        units.position((int) position);
    }

    /**
     * Gets the amount of space remaining in the buffer.
     * @return The number of bytes remaining in the buffer.
     */
    public long remaining() {
        return units.remaining();
    }
    
    //
    // Remaining implementation of WriteableBuffer.

    /**
     * Sets the capacity of the buffer to the given amount.
     *
     * @param n The number of bytes that the buffer must be able to store.
     * @return The buffer, for chained invocations.
     */
    public WriteableBuffer capacity(long n) {
        if(n >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("NIOBuffer can only hold Integer.MAX_VALUE bytes");
        }
        
        int in = (int) n;
        if(in >= units.limit()) {
            ByteBuffer temp;
            if(units.isDirect()) {
                temp = ByteBuffer.allocateDirect(in);
            } else {
                temp = ByteBuffer.allocate(in);
            }
            temp.put((ByteBuffer) units.flip());
            units = temp;
        }
        return this;
    }

    /**
     * Puts a single byte onto this buffer.
     * @param b The byte to put on the buffer
     * @return This buffer, allowing chained invocations.
     */
    public WriteableBuffer put(byte b) {
        checkBounds(units.position()+1);
        units.put(b);
        return this;
    }

    /**
     * Puts a single byte onto this buffer at the given position.
     * @param p The position where the byte should be put.
     * @param b The byte to put on the buffer
     * @return This buffer, allowing chained invocations.
     */
    public WriteableBuffer put(long p, byte b) {
        checkBounds(p);
        units.put((int) p, b);
        return this;
    }

    public WriteableBuffer put(byte[] bytes) {
        checkBounds(units.position() + bytes.length);
        units.put(bytes);
        return this;
    }
    
    

    /**
     * Appends a given number of bytes from a readable buffer onto this
     * buffer.
     * @return The buffer, to allow chained invocations.
     * @param b The buffer that we want to append to this buffer.
     * @param n The number of bytes to append onto this buffer.
     */
    @Override
    public WriteableBuffer append(ReadableBuffer b, long n) {

        //
        // We can handle ArrayBuffers much faster.
        if(b instanceof NIOBuffer) {
            NIOBuffer o = (NIOBuffer) b;
            if(units.remaining() < o.units.remaining()) {
                ByteBuffer temp;
                if(units.isDirect()) {
                    temp =
                        ByteBuffer.allocateDirect(units.capacity() +
                                                  o.units.remaining());
                } else {
                    temp =
                        ByteBuffer.allocate(units.capacity() +
                                            o.units.remaining());
                }
                units.flip();
                temp.put(units);
                units = temp;
            }
            units.put(o.units);
            return this;
        } else {

            //
            // Use the slow way.
            return super.append(b, n);
        }
    }

    /**
     * Computes the logical OR of this buffer and another.
     *
     * @param b The buffer to or with this one.
     * @return The buffer, to allow chained invocations.
     */
    public WriteableBuffer or(ReadableBuffer b) {
        checkBounds(b.limit());
        for(int i = 0; i < b.limit(); i++) {
            units.put(i, (byte) (b.get(i) | units.get(i)));
        }
        return this;
    }

    /**
     * Computes the logical XOR of this buffer and another.
     *
     * @param b The buffer to or with this one.
     * @return The buffer, to allow chained invocations.
     */
    public WriteableBuffer xor(ReadableBuffer b) {
        checkBounds(b.limit());
        for(int i = 0; i < b.limit(); i++) {
            units.put(i, (byte) (b.get(i) ^ units.get(i)));
        }
        return this;
    }

    /**
     * Write the buffer to a new IO buffer.  This will use the bulk put operation on
     * our internal representation.
     * @param b The buffer to which we will write our data.
     */
    public void write(ByteBuffer b) {
        b.put((ByteBuffer) units.flip());
    }

    /**
     * Write the buffer to a channel.
     * @param chan The channel to which the buffer should be written.
     * @throws java.io.IOException If there is any error during writing.
     */
    public void write(WritableByteChannel chan)
        throws java.io.IOException {
        write(chan, 0, units.position());
    }

    public void write(WritableByteChannel chan, long start, long end) throws IOException {
        ByteBuffer dup = units.duplicate();
        dup.position((int) start);
        dup.limit((int) end);
        ChannelUtil.writeFully(chan, dup);
    }

    public void write(WriteableBuffer b) {
        b.append(getReadableBuffer());
    }

    /**
     * Gets an array of bytes from the units.
     * @return An array containing the data in the buffer.
     */
    protected byte[] array() {
        if(units.hasArray()) {
            return units.array();
        }

        byte[] ret = new byte[units.position()];
        ((ByteBuffer) units.flip()).get(ret);
        return ret;
    }

    /**
     * Write the buffer to a data output.
     * @param o The output to which the buffer should be written.
     * @throws java.io.IOException If there is any error writing the buffer.
     */
    public void write(DataOutput o)
        throws java.io.IOException {

        if(o instanceof java.io.RandomAccessFile) {
            write(((java.io.RandomAccessFile) o).getChannel());
            return;
        }
        int pos = units.position();
        o.write(array(), 0, pos);
    }

    /**
     * Write the buffer to a stream.
     * @param os The stream to which the buffer should be written.
     * @throws java.io.IOException If there is any error writing the data.
     */
    public void write(OutputStream os)
        throws java.io.IOException {
        int pos = units.position();
        os.write(array(), 0, pos);
    }
    
    /**
     * Gets a readable buffer from this writeable one.  The readable buffer
     * will share the representation with the buffer that generated it.
     * @return A new readable buffer that shares the representation of the data.
     */
    public ReadableBuffer getReadableBuffer() {
        NIOBuffer ret = new NIOBuffer();
        ret.units = units.duplicate();
        ret.units.flip();
        return ret;
    }

    //
    // Remaining implementation of ReadableBuffer.
    
    /**
     * Duplicates this buffer, so that it can be used safely by other
     * readers.
     * @return A new buffer that shares the data representation with this buffer, but has an
     * independent position.
     */
    public ReadableBuffer duplicate() {
        NIOBuffer ret = new NIOBuffer();
        ret.units = units.duplicate();
        return ret;
    }

    /**
     * Slices this buffer so that a sub-buffer can be used.
     * @param l The number of bytes in the sliced buffer.
     * @param p The position at which the buffer should be sliced.
     * @return A new buffer that shares the underlying representation with this buffer.  The
     * starting position for the new buffer is the given position and the sliced buffer 
     * will contain the given number of bytes.
     */
    public ReadableBuffer slice(long p, long l) {
        if(p >= Integer.MAX_VALUE || l >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("NIOBuffer can only hold Integer.MAX_VALUE bytes");
        }
        NIOBuffer ret = new NIOBuffer();
        int tp = units.position();
        ret.units =
            (ByteBuffer) ((ByteBuffer) units.position((int) p))
            .slice().limit((int) l);
        units.position(tp);
        return ret;
    }

    /**
     * Gets the limit of this buffer, i.e., the last readable position.
     * @return This buffer, for chained invocations.
     */
    public long limit() {
        return units.limit();
    }

    /**
     * Sets the limit of this buffer, i.e., the last readable position.
     * @param l The limit for the buffer.
     */
    public void limit(long l) {
        if(l >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("NIOBuffer can only hold Integer.MAX_VALUE bytes");
        }
        units.limit((int) l);
    }

    /**
     * Gets a byte from this buffer.
     * @return Gets the next byte in this buffer, advancing the current position by one.
     */
    public byte get() {
        return units.get();
    }

    /**
     * Gets a byte from this buffer at the given position.
     * @param position The position from which we wish to get a byte.
     * @return The byte at the given position.
     */
    public byte get(long position) {
        if(position >= Integer.MAX_VALUE) {
            throw new IllegalArgumentException("NIOBuffer can only hold Integer.MAX_VALUE bytes");
        }
        return units.get((int) position);
    }

    /**
     * Decodes an integer stored using the 7 bit encoding.  This is a
     * specialization of the general method that uses get for efficiency
     * reasons.
     * 
     * @return the decoded int.
     * @see #byteEncode
     */
    @Override
    public int byteDecode() {

        byte 	curr  = (byte) 0x80;
        int 	res   = 0;
        int 	shift = 0;

        while((curr & 0x80) != 0) {
            curr = units.get();
            res |= ((curr & 0x7F)) << shift;
            shift += 7;
        }
        return res;
    }

    /**
     * A main program to test encoding and decoding of integers.
     * @param args The arguments
     * @throws java.lang.Exception If there is any problem.
     */
    public static void main(String[] args) throws Exception {

        java.util.Random rand = new java.util.Random();
        int n = Integer.parseInt(args[0]);
        NIOBuffer b = new NIOBuffer(n);
        int[] nums = new int[n+1];
        int[] limits = {255, 65535, 16777215, Integer.MAX_VALUE};
        for(int limit = 1; limit <= 4; limit++) {
	    
            long start = System.currentTimeMillis();
            b.clear();
            int max = limits[limit-1];
            System.out.println("Max: " + max);
            for(int i = 0; i < n ; i++) {
                nums[i] = rand.nextInt(max);
                b.byteEncode(nums[i], limit);
            }
            nums[n] = max;
            b.byteEncode(nums[n], limit);

            b.position(0);
            for(int i = 0; i <= n; i++) {
                int x = b.byteDecode(limit);
                if(x != nums[i]) {
                    System.out.println("Encoded: " + nums[i] + " decoded: " + x);
                }
            }
            System.out.println("nbytes Took: " + (System.currentTimeMillis() - start) +
                               "ms");
        }

        b.clear();
        long start = System.currentTimeMillis();
        for(int i = 0; i < n; i++) {
            nums[i] = rand.nextInt(Integer.MAX_VALUE);
            b.byteEncode(nums[i]);
        }
        System.out.println("minimal encode took: " +
                           (System.currentTimeMillis() - start) +
                           "ms");
        start = System.currentTimeMillis();
        b.position(0);
        for(int i = 0; i < n; i++) {
            int x = b.byteDecode();
            if(x != nums[i]) {
                System.out.println("Encoded: " + nums[i] + " decoded: " + x);
            }
        }
        System.out.println("minimal decode took: " +
                           (System.currentTimeMillis() - start) +
                           "ms");
    } // end of main()
} // NIOBuffer
