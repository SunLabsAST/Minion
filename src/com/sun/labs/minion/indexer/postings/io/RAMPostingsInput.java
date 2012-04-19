package com.sun.labs.minion.indexer.postings.io;

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 */
public class RAMPostingsInput implements PostingsInput {
    
    private static final Logger logger = Logger.getLogger(RAMPostingsInput.class.getName());
    
    private ReadableBuffer buff;
    
    public RAMPostingsInput(ReadableBuffer buff) {
        this.buff = buff;
    }

    @Override
    public ReadableBuffer read(long offset, int size) throws IOException {
        return buff.slice(offset, size);
    }

}
