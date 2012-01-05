package com.sun.labs.minion.indexer.dictionary.io;

import com.sun.labs.minion.indexer.dictionary.DictionaryHeader;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.NameEncoder;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.DataOutput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A base class for dictionary output that operates on buffers without knowing
 * the underlying implementation.
 */
public abstract class AbstractDictionaryOutput implements DictionaryOutput {

    private static final Logger logger = Logger.getLogger(AbstractDictionaryOutput.class.getName());
    
    /**
     * The dictionary that we're currently dumping.
     */
    protected MemoryDictionary dict;

    /**
     * A buffer to hold term information.
     */
    protected WriteableBuffer entryInfo;

    /**
     * A buffer to hold term information offsets.
     */
    protected WriteableBuffer entryInfoOffsets;

    /**
     * A buffer to hold the offsets of the uncompressed names in the
     * merged dictionary.
     */
    protected WriteableBuffer nameOffsets;

    /**
     * A buffer to hold names.
     */
    protected WriteableBuffer names;

    /**
     * A buffer where finished dictionaries will be written, and extra data
     * will be appended.
     */
    protected WriteableBuffer completed;

    /**
     * The previous name that was encoded.
     */
    private Comparable prevName;

    /**
     * The header for the current dictionary that we're outputting.
     */
    private DictionaryHeader header;

    /**
     * An encoder for the names in the current dictionary.
     */
    private NameEncoder encoder;

    /**
     * How the current dictionary should be renumbered.
     */
    private MemoryDictionary.Renumber renumber;

    /**
     * Whether we've started a dictionary.
     */
    protected boolean started;

    /**
     * Whether we've finished a dictionary.
     */
    protected boolean finished = true;

    /**
     * A place to store the id to position map for one dictionary.
     */
    private int[] idToPosn = null;

    public void start(MemoryDictionary dict, NameEncoder encoder, 
            MemoryDictionary.Renumber renumber, int nChans) {
        if(!finished) {
            throw new IllegalStateException("Starting another dictionary before finishing the previous one");
        }
        this.header = new DictionaryHeader(nChans);
        this.encoder = encoder;
        this.renumber = renumber;
        started = true;
        finished = false;
        prevName = null;
        
        //
        // If we're not supposed to re-number the dictionary entries as we output them, 
        // then we need to keep an ID to position map.  Make sure we have the space
        // to store that map.
        if(renumber == MemoryDictionary.Renumber.NONE) {
            if(dict == null) {
                throw new IllegalStateException(String.format("Can't start a dictionary with %s without a dictionary", renumber));
            } else {
                if(idToPosn == null) {
                    idToPosn = new int[dict.getMaxID() + 1];
                } else if(dict.getMaxID() >= idToPosn.length) {
                    idToPosn = Arrays.copyOf(idToPosn, dict.getMaxID() + 1);
                }
            }
            Arrays.fill(idToPosn, 0);
        }
    }

    @Override
    public DictionaryHeader getHeader() {
        return header;
    }

    @Override
    public void write(IndexEntry e) {
        if(!started) {
            throw new IllegalStateException("Can't write entries when the dictionary has not been started");
        }

        //
        // See if this a entry that should be uncompressed.  If so, we need
        // to record the position.
        if(header.size % 4 == 0) {
            nameOffsets.byteEncode(names.position(), 4);
            prevName = null;
        }

        //
        // Encode the name.
        encoder.encodeName(prevName, e.getName(), names);

        //
        // Encode the entry information, first taking note of where
        // this information is being encoded.
        entryInfoOffsets.byteEncode(entryInfo.position(), 4);
        e.encodeEntryInfo(entryInfo);

        //
        // Keep the ID to position map up to date, if necessary.
        if(renumber == MemoryDictionary.Renumber.NONE) {
            idToPosn[e.getID()] = (int) header.size;
        }

        prevName = e.getName();

        //
        // The dictionary is now one bigger!
        header.size++;
        header.maxEntryID = Math.max(header.maxEntryID, e.getID());
    }

    public void finish() {
        if(!started) {
            throw new IllegalStateException("Can't finish a dictionary that hasn't been started");
        }
        
        //
        // Don't bother finishing an empty dictionary.
        if(names.position() == 0) {
            started = false;
            finished = true;
            return;
        }

        //
        // Sizes of the various buffers that make up the dictionary.
        header.namesSize = names.position();
        header.nameOffsetsSize = nameOffsets.position();
        header.entryInfoSize = entryInfo.position();
        header.entryInfoOffsetsSize = entryInfoOffsets.position();
        header.computeValues();

        //
        // Copy everything to our completed buffer.
        long headerPos = completed.position();
        header.write(completed);
        if(renumber == MemoryDictionary.Renumber.NONE) {
            header.idToPosnPos = completed.position();
            for(int i = 0; i <= header.maxEntryID; i++) {
                completed.byteEncode(idToPosn[i], 4);
            }
            header.idToPosnSize = (header.maxEntryID + 1) * 4;
        } else {
            header.idToPosnPos = -1;
            header.idToPosnSize = -1;
        }
        
        header.namesPos = completed.position();
        names.write(completed);
        names.clear();
        header.nameOffsetsPos = completed.position();
        nameOffsets.write(completed);
        nameOffsets.clear();
        header.entryInfoPos = completed.position();
        entryInfo.write(completed);
        entryInfo.clear();
        header.entryInfoOffsetsPos = completed.position();
        entryInfoOffsets.write(completed);
        entryInfoOffsets.clear();
        header.goodMagic();
        long endPos = completed.position();
        completed.position(headerPos);
        header.write(completed);
        completed.position(endPos);

        started = false;
        finished = true;
    }

    public void cleanUp() {
        started = false;
        finished = true;
        names.clear();
        nameOffsets.clear();
        entryInfo.clear();
        completed.clear();
    }
    

    public long position() {
        return completed.position();
    }

    public void write(ReadableBuffer b) {
        if(started && !finished) {
            throw new IllegalStateException("Can't write a buffer when writing a dictionary.");
        }
        completed.append(b);
    }

    public void flush(RandomAccessFile file) throws IOException {
        if(started && !finished) {
            throw new IllegalStateException("Can't flush a set of dictionaries before the last one has been flushed");
        }

        completed.write(file.getChannel());
        completed.clear();
    }
    
    public void close() throws java.io.IOException {
        if(started && !finished) {
            throw new IllegalStateException("Can't close dictionary output while it's being used!");
        }
    }

    //
    // Implementation of WriteableBuffer
    public WriteableBuffer byteEncode(long val, int n) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        completed.byteEncode(val, n);
        return this;
    }

    public String toString(long start, long end, DecodeMode decode) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.toString(start, end, decode);
    }

    public String toString(Portion portion, DecodeMode decode) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.toString(portion, decode);
    }

    public long remaining() {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.remaining();
    }

    public void position(long position) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        completed.position(position);
    }

    public void limit(long l) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        completed.limit(l);
    }

    public long limit() {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.limit();
    }

    public byte get(long i) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.get(i);
    }

    public long countBits() {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.countBits();
    }

    public WriteableBuffer xor(ReadableBuffer b) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.xor(b);
    }

    public void write(OutputStream os) throws IOException {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        completed.write(os);
    }

    public void write(DataOutput o) throws IOException {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        completed.write(o);
    }

    public void write(WritableByteChannel chan, long start, long end) throws IOException {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        completed.write(chan, start, end);
    }

    public void write(WritableByteChannel chan) throws IOException {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        completed.write(chan);
    }

    public void write(WriteableBuffer b) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        completed.write(b);
    }

    public void write(ByteBuffer b) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        completed.write(b);
    }

    public WriteableBuffer set(long bitIndex) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.set(bitIndex);
    }

    public WriteableBuffer put(long p, byte b) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.put(p, b);
    }

    public WriteableBuffer put(byte b) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.put(b);
    }

    public WriteableBuffer put(byte[] bytes) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.put(bytes);
    }

    public WriteableBuffer or(ReadableBuffer b) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.or(b);
    }

    public ReadableBuffer getReadableBuffer() {
        return completed.getReadableBuffer();
    }

    public WriteableBuffer encode(CharSequence s) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.encode(s);
    }

    public WriteableBuffer encodeAsBytes(String s, Charset cs) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.encodeAsBytes(s, cs);
    }

    public WriteableBuffer encode(float f) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.encode(f);
    }

    public WriteableBuffer clear() {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.clear();
    }

    public WriteableBuffer capacity(long n) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.capacity(n);
    }

    public int byteEncode(long n) throws ArithmeticException {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.byteEncode(n);
    }

    public WriteableBuffer byteEncode(long pos, long n, int nBytes) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.byteEncode(pos, n, nBytes);
    }

    public WriteableBuffer append(ReadableBuffer b, long n) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.append(b, n);
    }

    public WriteableBuffer append(ReadableBuffer b) {
        if(started && !finished) {
            throw new IllegalStateException("Can't encode to a buffer when writing a dictionary.");
        }
        return completed.append(b);
    }
}
