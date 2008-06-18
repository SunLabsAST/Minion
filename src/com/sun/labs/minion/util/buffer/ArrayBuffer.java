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

import java.io.DataOutput;
import java.io.OutputStream;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import com.sun.labs.minion.util.ChannelUtil;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.NanoWatch;
import com.sun.labs.minion.util.Util;

/**
 * A buffer class for reading and writing that is backed by a growable
 * array of bytes.
 *
 * <p>
 *
 * This implementation is suitable for use in postings types.
 *
 * @see com.sun.labs.minion.indexer.postings.Postings
 */
public class ArrayBuffer extends StdBufferImpl implements Cloneable {

    /**
     * The array backing our data.
     */
    protected byte[] units;
    
    /**
     * The increase factor to use when resizing the buffer.  Initially we will
     * double the size of the buffer, but at each increase in size we will
     * double the increase factor.
     */
    protected int increaseFactor = 2;

    /**
     * The position in the array where the next byte will be written or
     * read.  This is an absolute position in the array.  When reading, we
     * may be given a position that is relative to the beginning of a
     * sliced buffer:  we'll account for that later.
     */
    protected int pos;

    /**
     * The offset of the units array where the buffer we're representing
     * starts.  This is used only when reading a buffer that has been
     * sliced.  When writing, it will always be 0.
     */
    protected int off;

    /**
     * The limit of the units array, i.e., the last byte that can be read.
     * This is relative to the offset of the beginning of the buffer!  This
     * is used only when reading a buffer that has been explicitly limited.
     * When writing it will always be the length of the units array.
     */
    protected int lim;

    /**
     * The log for this buffer.
     */
    protected static MinionLog log = MinionLog.getLog();

    /**
     * A tag to use in logging.
     */
    protected static String logTag = "AB";
    
    /**
     * Creates an array backed buffer that we can use when duplicating or
     * slicing.
     */
    protected ArrayBuffer() {
    }

    /**
     * Creates an array backed buffer with the given initial capacity.
     *
     * @param n The initial capacity of the buffer.
     */
    public ArrayBuffer(int n) {
        units = new byte[n];
        lim = units.length;
    } // ArrayBuffer constructor

    /**
     * Creates an buffer backed by the given array.  This should be used
     * only when creating an entry for reading!
     *
     * @param units The array with which to back the buffer.  This array is
     * used as provided, so changes to it will affect the buffer.
     */
    public ArrayBuffer(byte[] units) {
        this(units, 0, units.length);
    }

    /**
     * Creates an buffer backed by the given array.  This should be used
     * only when creating an entry for reading!
     *
     * @param units The array with which to back the buffer.  This array is
     * used as provided, so changes to it will affect the buffer.
     * @param off The position in the given array where the buffer should
     * start.
     * @param length The number of bytes that should be in the buffer.
     */
    public ArrayBuffer(byte[] units, int off, int length) {
        this.units = units;
        this.pos = off;
        this.off = off;
        this.lim = length;
    }

    /**
     * Checks whether the given position is outside our array bounds.  If
     * it is, we expand so that it is not.
     * @param p The position that we want to check.
     */
    protected void checkBounds(int p) {
        if(p >= units.length) {
            units = Util.expandByte(units, p * increaseFactor);
            increaseFactor *= 2;
            lim = units.length;
        }
    }
    
    //
    // Implementation of Buffer.
    /**
     * Gets the position in the buffer.  This is with respect to any offset
     * gotten from a sliced buffer.
     *
     * @return The position in the buffer.
     */
    public int position() {
        return pos - off;
    }

    /**
     * Sets the position in the buffer.
     *
     * @param position The point to which the buffer's position should be
     * set.
     */
    public void position(int position) {
        pos = position+off;
    }

    /**
     * Gets the amount of space remaining in the buffer.  This is the limit
     * of the buffer minus the current position.
     * @return The number of bytes remaining in the buffer.
     */
    public int remaining() {
        return lim+off - pos;
    }
    
    //
    // Remaining implementation of WriteableBuffer.

    /**
     * Sets the capacity of the buffer to the given amount.
     *
     * @param n The number of bytes that the buffer must be able to store.
     * @return The buffer, for chained invocations.
     */
    public WriteableBuffer capacity(int n) {
        if(n >= units.length) {
            units = Util.expandByte(units, n);
            lim = units.length;
        }
        return this;
    }

    /**
     * Puts a single byte onto this buffer.
     * @param b The byte to put on the buffer
     * @return This buffer, to allow chained invocations.
     */
    public WriteableBuffer put(byte b) {
        return put(pos++, b);
    }
        

    /**
     * Puts a single byte onto this buffer at the given position.
     * @param p The position where the byte should be put.
     * @param b The byte to put on the buffer
     * @return This buffer, to allow chained invocations.
     */
    public WriteableBuffer put(int p, byte b) {
        checkBounds(p+1);
        units[p] = b;
        return this;
    }

   /**
     * Encodes an integer in a byte-aligned fashion, using the minimal
     * number of bytes.  The basic idea: use the 7 lower order bits of a
     * byte to encode a number.  If the 8th bit is 0, then there are no
     * further bytes in this number.  If the 8th bit is one, then the next
     * byte continues the number.  Note that this means that a number that
     * would completly fill an integer will take 5 bytes to encode.
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
        // Make a good guess for how many bytes its going to take to do the
        // encoding, saving bounds checks for each byte.
        if(n < Integer.MAX_VALUE) {
            if(n < 16384) {
                checkBounds(pos+2);
            } else {
                checkBounds(pos+5);
            }
        } else {
            checkBounds(pos+10);
        }

        //
        // We need to encode 0 as a byte, so we use a do/while.
        do {
            byte curr = (byte) (n & 0x7F);
            if(n > 0x7f) {
                curr |= 0x80;
            }
            units[pos++] = curr;
            n >>>= 7;
            nBytes++;
        } while(n > 0);
	
        return nBytes;
    }

    /**
     * Puts a number of bytes onto this buffer.
     * @param b An array from which we'll take the bytes.
     * @param o The offset in the array at which we'll start.
     * @param n The number of bytes to take.
     * @return This buffer, to allow chained invocations.
     */
    public WriteableBuffer put(byte[] b, int o, int n) {
        capacity(pos+n);
        System.arraycopy(b, o, units, pos, n);
        pos += n;
        return this;
    }

    /**
     * Appends a given number of bytes from a readable buffer onto this
     * buffer.
     * @return The buffer, to allow chained invocations.
     * @param b The buffer that we wish to append onto this buffer.
     * @param n The number of bytes to append onto this buffer.
     */
    public WriteableBuffer append(ReadableBuffer b, int n) {

        //
        // Quick check.
        if(n == 0) {
            return this;
        }
        
        //
        // Expand our array here, if necessary.
        capacity(pos+n);
        
        //
        // We can handle ArrayBuffers much faster.
        if(b instanceof ArrayBuffer) {
            ArrayBuffer o = (ArrayBuffer) b;
            System.arraycopy(o.units, o.pos, units, pos, n);
            o.pos += n;
            pos += n;
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
            units[i] = (byte) (b.get(i) | units[i]);
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
            units[i] = (byte) (b.get(i) ^ units[i]);
        }
        return this;
    }

    /**
     * Write the buffer to a new IO buffer.  We use the buffer's bulk
     * <code>put</code> method to do so efficiently.
     * @param b The <code>java.nio</code> buffer onto which we will write
     * this buffer.
     * @see java.nio.ByteBuffer#put(byte[],int,int)
     */
    public void write(ByteBuffer b) {
        b.put(units, 0, pos);
    }

    /**
     * Write the buffer to a channel.  The buffer is converted to a 
     * {@link java.nio.ByteBuffer} and then written to the channel.
     * @param chan The channel to which the buffer should be written.
     * @throws java.io.IOException If there is any error writing to the channel.
     */
    public void write(WritableByteChannel chan)
        throws java.io.IOException {
        ChannelUtil.writeFully(chan,
                               (ByteBuffer) ByteBuffer.wrap(units, 0, pos)
                               .position(pos)
                               .flip());
    }

    /**
     * Write the buffer to a data output.  The underlying array is written
     * using the {@link java.io.DataOutput#write(byte[],int,int) write}
     * method of <CODE>DataOutput</CODE>.
     * @param o The output to which the buffer will be written.
     * @throws java.io.IOException If there is any error writing the buffer.
     */
    public void write(DataOutput o)
        throws java.io.IOException {
        o.write(units, 0, pos);
    }

    /**
     * Writes the buffer to a stream.
     * @param os The stream to which the buffer should be written.
     * @throws java.io.IOException If there is any error writing the buffer.
     */
    public void write(OutputStream os)
        throws java.io.IOException {
        os.write(units, 0, pos);
    }

    /**
     * Gets a readable buffer from this writeable one.  The readable buffer
     * will share the representation with the buffer that generated it.
     * @return A version of this buffer as a readable buffer.
     */
    public ReadableBuffer getReadableBuffer() {
        ArrayBuffer ret = new ArrayBuffer();
        ret.units = units;
        ret.pos = 0;
        ret.off = 0;
        ret.lim = pos;
        return ret;
    }

    //
    // Remaining implementation of ReadableBuffer.

    /**
     * Duplicates this buffer, so that it can be used safely by other
     * readers.
     * @return A new readable buffer that shares the underlying
     * representation with this buffer.
     */
    public ReadableBuffer duplicate() {
        ArrayBuffer ret = new ArrayBuffer();
        ret.units = units;
        ret.pos   = pos;
        ret.off   = off;
        return ret;
    }

    /**
     * Slices this buffer so that a sub-buffer can be used.
     * @param p The position at which the buffer should be sliced.
     * @param s The number of bytes that should be in the sliced buffer.
     * @return A new readable buffer that shares the underlying
     * representation with this buffer.
     */
    public ReadableBuffer slice(int p, int s) {
        ArrayBuffer ret = new ArrayBuffer();
        ret.units = units;
        ret.pos = p+off;
        ret.off = ret.pos;
        ret.limit(s);
        return ret;
    }
    
    /**
     * The limit of this buffer, i.e., the last readable position.
     * @return The position of the last byte that can be read from or
     * written to this buffer.
     */
    public int limit() {
        return lim;
    }

    /**
     * Sets the limit of this buffer, i.e., the last readable position.
     * @param l The limit that we wish to set.
     */
    public void limit(int l) {
        lim = l;
    }

    /**
     * Gets the next byte from this buffer.
     * @return The byte at the current position.  The position will then be advanced.
     */
    public byte get() {
        return units[pos++];
    }

    /**
     * Gets a byte from this buffer at the given position.  This position
     * is relative to any any offset into the array that comes from being a
     * slice of another buffer.
     * @param i The position from which we would like to get the byte.
     * @return The byte at the given position.
     */
    public byte get(int i) {
        return units[off+i];
    }

    /**
     * Decodes an integer stored using the 7 bit encoding.  This is a
     * specialization of the general method that uses get for efficiency
     * reasons.
     * 
     * @return the decoded int.
     * @see #byteEncode
     */
    public int byteDecode() {

        byte 	curr  = (byte) 0x80;
        int 	res   = 0;
        int 	shift = 0;

        while((curr & 0x80) != 0) {
            curr = units[pos++];
            res |= (curr & 0x7F) << shift;
            shift += 7;
        }
        return res;
    }

    /**
     * Clones this buffer.
     * @return A copy of this buffer.  This copy will not share the underlying representation.
     */
    public Object clone() {
        ArrayBuffer result = null;
        try {
            result = (ArrayBuffer) super.clone();
            result.units = units.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
        return result;
    }

    /**
     * A test program that will encode and decode random integers.
     * @param args The arguments.
     * @throws java.lang.Exception If there is any problem.
     */
    public static void main(String[] args) throws Exception {

        java.util.Random rand = new java.util.Random();
        int n = Integer.parseInt(args[0]);
        ArrayBuffer b = new ArrayBuffer(n);
        int[] nums = new int[n+1];
        int[] limits = {255, 65535, 16777215, Integer.MAX_VALUE};
        for(int limit = 1; limit <= 4; limit++) {
	    
            NanoWatch nw = new NanoWatch();
            nw.start();
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
            nw.stop();
            System.out.format("nbytes Took: %.3fms\n", nw.getTimeMillis());
        }

        b.clear();
        NanoWatch nw = new NanoWatch();
        nw.start();
        for(int i = 0; i < n; i++) {
            nums[i] = rand.nextInt(Integer.MAX_VALUE);
            b.byteEncode(nums[i]);
        }
        nw.stop();
        System.out.format("minimal encode took %.3fms\n", nw.getTimeMillis());
        nw.reset();
        nw.start();
        b.position(0);
        for(int i = 0; i < n; i++) {
            int x = b.byteDecode();
            if(x != nums[i]) {
                System.out.println("Encoded: " + nums[i] + " decoded: " + x);
            }
        }
        nw.stop();
        System.out.format("minimal decode took %.3fms\n", nw.getTimeMillis());
    }
} // ArrayBuffer
