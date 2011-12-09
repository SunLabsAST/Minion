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
import com.sun.labs.minion.util.Util;
import com.sun.labs.util.LabsLogFormatter;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.logging.Handler;
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
     * A log.
     */
    private static final Logger logger = Logger.getLogger(FileWriteableBuffer.class.getName());

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
    protected long pos;

    /**
     * The position in our in-memory buffer.
     */
    protected int bPos;

    /**
     * Our default buffer size.
     */
    protected static int DEFAULT_BUFF_SIZE = 1024;

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
    public FileWriteableBuffer(RandomAccessFile raf, int size)
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
    public long limit() {
        return Long.MAX_VALUE;
    }

    /**
     * Sets the limit of this buffer, i.e., the last readable position.
     * @param l The limit to set.  This has no effect on this buffer.
     */
    public void limit(long l) {
    }

    /**
     * Gets the current position.
     * @return The current position.
     */
    public long position() {
        return pos;
    }

    /**
     * Sets the current position.  This will flush the buffer to disk and
     * reposition the file relative to the initial offset at which the buffer
     * was started.
     * @param position The position.
     */
    public void position(long position) {
        flush();
        try {
            raf.seek(off + position);
            pos = position;
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error seeking", ioe);
        }
    }

    /**
     * Returns the number of bytes remaining in the buffer, which is nonsense.
     * @return <CODE>Integer.MAX_VALUE</CODE>, since the file may contain that much data.
     */
    public long remaining() {
        return Long.MAX_VALUE - pos;
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
    public WriteableBuffer capacity(long n) {
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

    public WriteableBuffer put(byte[] bytes) {

        if(bPos + bytes.length >= buff.length) {
            flush();
        }
        
        //
        // Given the above condition, we either have an empty buffer where
        // everything's been written to disk, or the bytes will fit in the
        // buffer.  
        if(bytes.length >= buff.length) {
            //
            // If there's too many bytes for our buffer, just go ahead and 
            // write them to the file.
            try {
                raf.write(bytes, 0, bytes.length);
            } catch(java.io.IOException ioe) {
                logger.log(Level.SEVERE, "Error writing file", ioe);
                buff = null;
            }
            return this;
        } else {
            //
            // Buffer them up.
            System.arraycopy(bytes, 0, buff, bPos, bytes.length);
            bPos += bytes.length;
        }
        pos += bytes.length;
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
    @Override
    public WriteableBuffer put(long p, byte b) {
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

    @Override
    public WriteableBuffer byteEncode(long n, int nBytes) {
        for(int shift = 8 * (nBytes - 1); shift >= 0; shift -= 8) {
            put((byte) ((n >>> shift) & 0xFF));
        }
        return this;
    }

    @Override
    public WriteableBuffer byteEncode(long pos, long n, int nBytes) {
        for(int shift = 8 * (nBytes - 1); shift >= 0; shift -= 8) {
            byte b = (byte) ((n >>> shift) & 0xFF);
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
    public WriteableBuffer append(ReadableBuffer b, long n) {
        for(long i = 0; i < n; i++) {
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

    @Override
    public WriteableBuffer encodeAsBytes(String s, Charset cs) {
        byte[] bytes = s.getBytes(cs);
        byteEncode(bytes.length, 4);
        put(bytes);
        return this;
    }

    /**
     * Sets the given bit to 1 in the given buffer.
     * @param bitIndex the index of the bit to set to 1.
     * @return This buffer, allowing chained invocations.
     */
    @Override
    public WriteableBuffer set(long bitIndex) {
        long byteIndex = bitIndex >>> 3;
        try {
            synchronized(raf) {
                long x = raf.getFilePointer();
                long bp = off+byteIndex;
                raf.seek(bp);
                byte initial  = 0;
                if(bp < raf.length()) {
                    initial = raf.readByte();
                }
                byte b = (byte) (initial | masks[(int) (bitIndex & 0x07L)]);
                raf.seek(bp);
                raf.writeByte(b);
                if(byteIndex+1 > pos) {
                    pos = byteIndex+1;
                }
                raf.seek(x);
            }
        } catch(IOException ex) {
            logger.log(Level.SEVERE, String.format("Error setting bit %d", bitIndex), ex);
        }
        return this;
    }

    /**
     * Clears the buffer.  This seeks to the initial offset and resets the various
     * parameters.
     *
     * @return This buffer, allowing chained invocations.
     */
    @Override
    public WriteableBuffer clear() {
        try {
            raf.seek(off);
            pos = 0;
            bPos = 0;
            Arrays.fill(buff, (byte) 0);
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
    @Override
    public ReadableBuffer getReadableBuffer() {
        flush();
        FileReadableBuffer ret = new FileReadableBuffer(raf, off, pos, 1024);
        ret.position(0);
        ret.limit(pos);
        return ret;
    }

    /**
     * Write the buffer to a new IO buffer.
     * @param b The buffer to which we'll write our data.
     */
    @Override
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
     * Write the buffer to a new IO buffer.
     * @param b The buffer to which we'll write our data.
     * @param start the offset from which we should start writing.
     * @param end the exclusive end offset where we should stop writing.
     */
    public void write(ByteBuffer b, long start, long end) {
        if(start >= pos || end > pos || start >= end) {
            throw new IllegalArgumentException(String.format("arguments out of range: start: %d end: %d pos: %d", start, end, pos));
        }
        long buffPos = pos - bPos;
        if(start < buffPos) {
            long so = off + start;
            int n;

            //
            // The entire region might be in the file.
            if(end < buffPos) {
                n = (int) (end - start);
            } else {
                n = (int) (buffPos - start);
            }
            try {
                ChannelUtil.readFully(raf.getChannel(), so, n, b);
            } catch(IOException ex) {
                throw new RuntimeException("Error reading from file-backed buffer");
            }
        }

        //
        // See if we need to add bytes from our memory buffer too.
        if(end > buffPos) {
            if(start >= buffPos) {
                b.put(buff, (int) (start - buffPos), (int) (end - start));
            } else {
                b.put(buff, 0, (int) (end-buffPos));
            }
        }
    }

    @Override
    public void write(WriteableBuffer b) {
        if(b instanceof FileWriteableBuffer) {
            FileWriteableBuffer fwb = (FileWriteableBuffer) b;
            fwb.flush();
            try {
                write(fwb.raf.getChannel());
            } catch(IOException ex) {
                throw new RuntimeException("Error writing to file-backed buffer", ex);
            }
            fwb.pos += pos;
        } else {
            b.append(getReadableBuffer());
        }
    }
    
    /**
     * Write the buffer to a channel.  This is done efficiently if the
     * channel is an instance of <code>FileChannel</code>.
     * @param chan The channel to which the buffer should be written.
     * @throws java.io.IOException If there is an error writing the data.
     */
    @Override
    public void write(WritableByteChannel chan)
            throws java.io.IOException {
        write(chan, 0, pos);
    }

    @Override
    public void write(WritableByteChannel chan, long start, long end) throws java.io.IOException {

        if(start >= pos || end > pos || start >= end) {
            throw new IllegalArgumentException(String.format("arguments out of range: start: %d end: %d pos: %d", start, end, pos));
        }
        
        //
        // The position in the buffer we represent where the in-memory buffer
        // starts.
        long buffPos = pos - bPos;
        if(chan instanceof FileChannel) {
            FileChannel mychan = raf.getChannel();
            
            //
            // Transfer whatever bytes are already in the file.
            if(start < buffPos) {
                long so = off + start;
                long n;
                
                //
                // The entire region might be in the file.
                if(end < buffPos) {
                    n = end - start;
                } else {
                    n = buffPos - start;
                }
                ChannelUtil.transferFully(mychan, so, n, (FileChannel) chan);
            }
            
            //
            // See if we need to write the bytes from our memory buffer too.
            if(end > buffPos) {
                ByteBuffer buffToWrite;
                if(start >= buffPos) {
                    buffToWrite = ByteBuffer.wrap(buff, (int) (start - buffPos), (int) (end - start));
                } else {
                    buffToWrite = ByteBuffer.wrap(buff, 0, (int) (end - buffPos));
                }
                ChannelUtil.writeFully((FileChannel) chan, buffToWrite);
            }
        } else {
            
            //
            // Allocate a buffer for writing.
            ByteBuffer b = ByteBuffer.allocateDirect(buff.length);
            FileChannel fchan = raf.getChannel();
            
            //
            // Read bytes from the file.
            if(start < buffPos) {
                long so = off + start;
                long n;
                if(end < buffPos) {
                    n = end - start;
                } else {
                    n = buffPos - start;
                }
                while(n > 0) {
                    int r = fchan.read(b, so);
                    n -= r;
                    so =+ r;
                    b.flip();
                    ChannelUtil.writeFully(chan, b);
                    b.clear();
                }
            }
            if(end > buffPos) {
                if(start >= buffPos) {
                    b.put(buff, (int) (start - buffPos), (int) (end - start));
                } else {
                    b.put(buff, 0, (int) (end - buffPos));
                }
                ChannelUtil.writeFully(chan, b);
            }
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
    @Override
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

    @Override
    public String toString() {
        try {
            return "FileWriteableBuffer{" + "off=" + off + ", pos=" + pos + ", bPos=" + bPos + ", len=" + raf.length() +'}';
        } catch(IOException ex) {
            return "FileWriteableBuffer{" + "off=" + off + ", pos=" + pos + ", bPos=" + bPos + '}';
        }
    }

    
    @Override
    public long countBits() {
        long cp = pos;
        long count = 0;
        byte[] temp = new byte[1024];
        int start = 0;
        int n = 0;
        while(cp > 0) {
            try {
                synchronized(raf) {
                    raf.seek(off + start);
                    n = raf.read(temp);
                }
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format("Error counting bits"), ex);
                return count;
            }
            if(n < 0) {
                break;
            }
            for(int j = 0; j < n; j++) {
                count += StdBufferImpl.nBits[(int) (temp[j] & 0xff)];
            }
            start += n;
            cp -= n;
        }
        
        return count;
    }

    @Override
    public byte get(long i) {
        synchronized(raf) {
            try {
                raf.seek(off + i);
                return raf.readByte();
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format("Error reading buffer"),
                        ex);
                return -1;
            }
        }
    }

    @Override
    public String toString(Portion portion, DecodeMode decode) {
        long start;
        long end;

        switch(portion) {
            case ALL:
                start = 0;
                end = pos;
                break;
            case BEGINNING_TO_POSITION:
                start = 0;
                end = pos;
                break;
            case FROM_POSITION_TO_END:
                start = 0;
                end = pos;
                break;
            default:
                start = 0;
                end = pos;
        }

        return toString(start, end, decode);
    }

    @Override
    public String toString(long start, long end, DecodeMode decode) {

        flush();

        byte[] tb = new byte[(int) (end - start)];
        try {
            long initPos = raf.getFilePointer();
            raf.seek(off + start);
            raf.readFully(tb);
            raf.seek(initPos);
        } catch(IOException ex) {
            logger.log(Level.SEVERE, String.format("Error reading file"), ex);
            return null;
        }

        ArrayBuffer ab = new ArrayBuffer(tb);
        return String.format("off: %d\n%s", off, ab.toString(Portion.ALL, decode));
    }
    
    public static void main(String[] args) throws Exception {
        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
        }
        logger.info(String.format("Max int: %d", Integer.MAX_VALUE));
        RandomAccessFile raf = new RandomAccessFile(args[0], "rw");
        raf.writeInt(-1);
        raf.writeInt(Integer.MAX_VALUE);
        raf.seek(0);
        for(int i = 0; i < 8; i++) {
            byte b = raf.readByte();
            logger.info(String.format("byte: %s", StdBufferImpl.byteToBinaryString(b)));
        }
        raf.seek(0);
        int x = raf.readInt();
        int y = raf.readInt();
        logger.info(String.format("read: %d %d", x, y));
        FileWriteableBuffer fwb = new FileWriteableBuffer(raf);
        fwb.byteEncode(-1, 4);
        fwb.byteEncode(Integer.MAX_VALUE, 4);
        logger.info(String.format("fwb: %s", fwb.toString(Portion.BEGINNING_TO_POSITION, Buffer.DecodeMode.INTEGER)));
        fwb.flush();
        raf.seek(0);
        x = raf.readInt();
        y = raf.readInt();
        logger.info(String.format("read: %d %d", x, y));
        x = raf.readInt();
        y = raf.readInt();
        logger.info(String.format("read: %d %d", x, y));
        
        raf.close();
    }
} // FileWriteableBuffer

