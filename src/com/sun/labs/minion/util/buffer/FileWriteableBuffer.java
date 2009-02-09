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
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;

import com.sun.labs.minion.util.ChannelUtil;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class for a writeable buffer that encodes data directly to a file,
 * utilizing an in-memory buffer.  While the positional encoding methods
 * are supported, you are strongly advised against using this
 * implementation of <code>WriteableBuffer</code> for anything other than
 * writing successive bytes.
 *
 * <p>
 *
 * This implementation is primarily used by the {@link
 * com.sun.labs.minion.indexer.dictionary.DictionaryWriter DictionaryWriter} class to
 * write interim pieces of dictionaries to a file during dumping and
 * merging operations.
 *
 * <p>
 *
 * Note that this implementation provides a {@link #flush} method that will
 * ensure that any data currently in memory is written to disk.
 */
public class FileWriteableBuffer implements WriteableBuffer {

    /**
     * The file to which we'll write our data.
     */
    protected RandomAccessFile raf;

    /**
     * Our in memory buffer.
     */
    protected byte[] buff;

    /**
     * The offset in the file where we started writing our buffer.
     */
    protected long off;

    /**
     * The position in the buffer we're writing.
     */
    protected int pos;

    /**
     * The position in our in-memory buffer.
     */
    protected int bPos;

    /**
     * Our default buffer size.
     */
    protected static int DEFAULT_BUFF_SIZE = 1024;

    /**
     * A log.
     */
    Logger logger = Logger.getLogger(getClass().getName());

    /**
     * A tag for our log entries.
     */
    protected static String logTag = "FWB";

    /**
     * Creates a buffer that will write to the underlying file.
     * @param raf The file to which we'll write our data.
     * @throws java.io.IOException If there is an error getting the current file pointer.
     */
    public FileWriteableBuffer(RandomAccessFile raf)
            throws java.io.IOException {
        this(raf, DEFAULT_BUFF_SIZE);
    } // FileWriteableBuffer constructor

    /**
     * Creates a buffer that will write to the underlying file.
     * @param raf The file to which we'll write our data.
     * @param size The size of the in-memory buffer to use.
     * @throws java.io.IOException If there is an error getting the current
     * file pointer. 
     */
    public FileWriteableBuffer(RandomAccessFile raf,
            int size)
            throws java.io.IOException {
        this.raf = raf;
        off = raf.getFilePointer();
        buff = new byte[size];
    } // FileWriteableBuffer constructor

    /**
     * Gets the limit of this buffer, i.e., the last writeable position.
     * @return <CODE>Integer.MAX_VALUE</CODE>, since the underlying
     * representation is a file. 
     */
    public int limit() {
        return Integer.MAX_VALUE;
    }

    /**
     * Sets the limit of this buffer, i.e., the last readable position.
     * @param l The limit to set.  This has no effect on this buffer.
     */
    public void limit(int l) {
    }

    /**
     * Gets the current position.
     * @return The current position.
     */
    public int position() {
        return pos;
    }

    /**
     * Sets the current position.  This will flush the buffer to disk and
     * reposition the file relative to the initial offset at which the buffer
     * was started.
     * @param p The position.
     */
    public void position(int p) {
        flush();
        try {
            raf.seek(off + p);
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error seeking", ioe);
        }
    }

    /**
     * Returns the number of bytes remaining in the buffer, which is nonsense.
     * @return <CODE>Integer.MAX_VALUE</CODE>, since the file may contain that much data.
     */
    public int remaining() {
        return Integer.MAX_VALUE;
    }

    /**
     * Flushes our in-memory buffer to the disk.
     */
    public void flush() {
        if(bPos == 0) {
            return;
        }
        try {
            raf.write(buff, 0, bPos);
            bPos = 0;
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error writing file", ioe);
            buff = null;
        }
    }

    // Implementation of com.sun.labs.minion.util.buffer.WriteableBuffer
    /**
     * Sets the capacity of this buffer to the given amount.
     * @param n The capacity that we want.
     * @return This buffer, for chained invocations.
     */
    public WriteableBuffer capacity(int n) {
        return this;
    }

    /**
     * Puts a single byte onto this buffer.
     * @param b The byte to put on the buffer
     * @return This buffer, allowing chained invocations.
     */
    public WriteableBuffer put(byte b) {
        if(bPos >= buff.length) {
            flush();
        }
        buff[bPos++] = b;
        pos++;
        return this;
    }

    /**
     * Puts a single byte onto this buffer at the given position.  This is
     * horribly inefficient, but it will work.  If you need to do absolute
     * puts, you should consider a different implementation of
     * <code>WriteableBuffer</code>.
     *
     * @param p The position where the byte should be put.
     * @param b The byte to put on the buffer
     * @return This buffer, allowing chained invocations.
     */
    public WriteableBuffer put(int p, byte b) {
        flush();
        try {
            long x = raf.getFilePointer();
            raf.seek(off + p);
            raf.writeByte(b);
            raf.seek(x);
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error writing file", ioe);
        }
        return this;
    }

    /**
     * Encodes a positive long onto a writeable in a given number of bytes.
     * 
     * @return The buffer, to allow chained invocations.
     * @param n The number to encode.
     * @param nBytes The number of bytes to use in the encoding.
     */
    public WriteableBuffer byteEncode(long n, int nBytes) {
        for(int i = 0; i < nBytes; i++) {
            put((byte) (n & 0xFF));
            n >>= 8;
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
    public WriteableBuffer byteEncode(int pos,
            long n,
            int nBytes) {
        for(int i = 0; i < nBytes; i++) {
            put(pos++, (byte) (n & 0xFF));
            n >>= 8;
        }
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
            throw new ArithmeticException(String.format(
                    "Negative value %d cannot by byte encoded", n));
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
     * @param n The number of bytes to append onto this buffer.
     */
    public WriteableBuffer append(ReadableBuffer b, int n) {
        for(int i = 0; i < n; i++) {
            put(b.get());
        }
        return this;
    }

    /**
     * Computes the logical OR of this buffer and another.
     *
     * @param b The buffer to or with this one.
     * @return The buffer, to allow chained invocations.
     */
    public WriteableBuffer or(ReadableBuffer b) {
        return this;
    }

    /**
     * Computes the logical XOR of this buffer and another.
     *
     * @param b The buffer to or with this one.
     * @return The buffer, to allow chained invocations.
     */
    public WriteableBuffer xor(ReadableBuffer b) {
        return this;
    }

    /**
     * Encodes a character sequence onto the buffer.  The sequence is encoded in the
     * following way:  first, the number of bytes used to encode the string is
     * encoded using the <CODE>byteEncode</CODE> method.  Then, the characters in the
     * sequence are encoded using a UTF-8 encoding.
     * @param s The character sequence to encode.
     * @return This buffer, allowing chained invocations.
     */
    public WriteableBuffer encode(CharSequence s) {

        int l = StdBufferImpl.sizeUTF8(s);
        byteEncode(l);

        for(int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            if(c < 128) {
                put((byte) c);
            } else if(c <= 0x7ff) {
                put((byte) ((c >> 6) | 0xc0));
                put((byte) ((c & 0x3f) | 0x80));
            } else if(c <= 0xffff) {
                put((byte) ((c >> 12) | 0xe0));
                put((byte) (((c >> 6) & 0x3f) | 0x80));
                put((byte) ((c & 0x3f) | 0x80));
            } else if(c <= 0x10ffff) {
                put((byte) ((c >> 18) | 0xf0));
                put((byte) (((c >> 12) & 0x3f) | 0x80));
                put((byte) (((c >> 6) & 0x3f) | 0x80));
                put((byte) ((c & 0x3f) | 0x80));
            }
        }
        return this;
    }

    /**
     * Sets the given bit to 1 in the given buffer.
     * @param bitIndex the index of the bit to set to 1.
     * @return This buffer, allowing chained invocations.
     */
    public WriteableBuffer set(int bitIndex) {
        return this;
    }

    /**
     * Clears the buffer.  This seeks to the initial offset and truncates the 
     * file to that length.
     *
     * @return This buffer, allowing chained invocations.
     */
    public WriteableBuffer clear() {
        try {
            raf.seek(off);
            raf.setLength(off);
        } catch(java.io.IOException ioe) {
        }
        return this;
    }

    /**
     * Gets a readable buffer from this writeable one.  It is OK for the
     * readable version of the buffer to share the representation of the
     * buffer contents.
     * @return An instance of <CODE>FileReadableBuffer</CODE> that is backed by the same file.
     * @see FileReadableBuffer
     */
    public ReadableBuffer getReadableBuffer() {
        flush();
        return new FileReadableBuffer(raf, off, pos, 1024);
    }

    /**
     * Write the buffer to a new IO buffer.
     * @param b The buffer to which we'll write our data.
     */
    public void write(ByteBuffer b) {
        try {
            //
            // Write the bytes in the file.
            ChannelUtil.readFully(raf.getChannel(), off, b);

            //
            // Write the bytes in the buffer.
            if(bPos > 0) {
                b.put(buff, 0, bPos);
            }
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error writing to buffer", ioe);
        }
    }

    /**
     * Write the buffer to a channel.  This is done efficiently if the
     * channel is an instance of <code>FileChannel</code>.
     * @param chan The channel to which the buffer should be written.
     * @throws java.io.IOException If there is an error writing the data.
     */
    public void write(WritableByteChannel chan)
            throws java.io.IOException {
        if(chan instanceof FileChannel) {
            //
            // Transfer the bytes already in the file.
            if(pos - bPos > 0) {
                ChannelUtil.transferFully(raf.getChannel(),
                        off, pos - bPos, (FileChannel) chan);
            }

            //
            // Write the bytes in the in-memory buffer.
            if(bPos > 0) {
                chan.write(ByteBuffer.wrap(buff, 0, bPos));
            }

        } else {
            //
            // Write this buffer to a real byte buffer and then send it out. 
            ByteBuffer b = ByteBuffer.allocate(pos);
            write(b);
            b.flip();
            ChannelUtil.writeFully(chan, b);
        }
    }

    /**
     * Write the buffer to a data output.  This is done efficiently if the
     * output is a <code>RandomAccessFile</code>.
     * @param o The output to which the buffer should be written.
     * @throws java.io.IOException If there is an error writing the data.
     */
    public void write(DataOutput o)
            throws java.io.IOException {
        if(o instanceof RandomAccessFile) {
            //
            // If it's a random access file, we can do a channel transfer.
            write(((RandomAccessFile) o).getChannel());
        } else {
            flush();
            raf.seek(off);
            int nWritten = 0;
            while(nWritten < pos) {
                int nRead = raf.read(buff);
                if(nRead < 0) {
                    break;
                }
                o.write(buff, 0, nRead);
                nWritten += nRead;
            }
        }
    }

    /**
     * Write the buffer to a stream.  If the stream is an instance of
     * <code>FileOutputStream</code>, this is done efficiently.
     * @see #write(WritableByteChannel)
     * @param os The stream to which the buffer should be written.
     * @throws java.io.IOException If there is any error writing the data.
     */
    public void write(OutputStream os)
            throws java.io.IOException {
        if(os instanceof FileOutputStream) {
            write(((FileOutputStream) os).getChannel());
        } else {
            //
            // We'll write the data from the on-disk buffer into the output
            // stream in buffer-sized chunks.  First we'll flush our data to
            // the disk so that we can re-use our buffer.
            flush();
            raf.seek(off);
            int nWritten = 0;
            while(nWritten < pos) {
                int nRead = raf.read(buff);
                if(nRead < 0) {
                    break;
                }
                os.write(buff, 0, nRead);
                nWritten += nRead;
            }
        }
    }
} // FileWriteableBuffer

