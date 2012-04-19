package com.sun.labs.minion.indexer.postings.io;

import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.util.logging.Logger;

/**
 *
 */
public abstract class AbstractPostingsOutput implements PostingsOutput {
    
    private static final Logger logger = Logger.getLogger(AbstractPostingsOutput.class.getName());
    
    private ArrayBuffer tempBuffer = new ArrayBuffer(1024);

    @Override
    public WriteableBuffer getTempBuffer() {
        return tempBuffer.clear();
    }
}
