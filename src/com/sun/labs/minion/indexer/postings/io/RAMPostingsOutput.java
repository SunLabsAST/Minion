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
public class RAMPostingsOutput implements PostingsOutput {
    
    private static final Logger logger = Logger.getLogger(RAMPostingsOutput.class.getName());
    
    private ArrayBuffer buff;
    
    private final static int DEFAULT_BUFFER_SIZE = 1 << 20;
    
    public RAMPostingsOutput() {
        this(DEFAULT_BUFFER_SIZE);
    }
    
    public RAMPostingsOutput(int initialSize) {
        buff = new ArrayBuffer(initialSize);
    }

    public long write(Postings p) throws IOException {
        long n = 0;
        for(WriteableBuffer b : p.getBuffers()) {
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
    }
    
    public RAMPostingsInput asInput() {
        return new RAMPostingsInput(buff);
    }
    
}
