package com.sun.labs.minion.indexer.postings.io;

import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 * An implementation of postings output that writes the postings into a RAM
 * buffer for later dumping.
 */
public class RAMPostingsOutput extends AbstractPostingsOutput {
    
    private static final Logger logger = Logger.getLogger(RAMPostingsOutput.class.getName());
    
    private ArrayBuffer buff;
    
    private final static int DEFAULT_BUFFER_SIZE = 5 * 1024 * 1024;
    
    public RAMPostingsOutput() {
        this(DEFAULT_BUFFER_SIZE);
    }
    
    public RAMPostingsOutput(int initialSize) {
        //
        // These guys will increase by chunks of 256K, rather than increasing
        // by a factor of two because otherwise they get too large too fast.
        buff = new ArrayBuffer(initialSize, 256 * 1024);
    }

    public int write(WriteableBuffer b) throws IOException {
        int n = (int) b.position();
        b.write(buff);
        return n;
    }

    public int write(WriteableBuffer[] buffs) throws IOException {
        int n = 0;
        for(WriteableBuffer b : buffs) {
            n += b.position();
            b.write(buff);
        }
        return n;
    }

    public long position() throws IOException {
        return buff.position();
    }

    public void flush() throws IOException {
        //
        // Doesn't do anything, since we're all in RAM.
    }
    
    public void flush(RandomAccessFile raf) throws IOException {
        buff.write(raf.getChannel());
        buff.clear();
    }
    
    public RAMPostingsInput asInput() {
        return new RAMPostingsInput(buff);
    }

    public void cleanUp() {
        buff.clear();
    }
}
