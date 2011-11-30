package com.sun.labs.minion.indexer.dictionary.io;

import com.sun.labs.minion.util.buffer.ArrayBuffer;
import java.io.File;

/**
 * A class for outputting dictionaries to RAM-based buffers.
 */
public class RAMDictionaryOutput extends AbstractDictionaryOutput {
    
    public RAMDictionaryOutput(File path) throws java.io.IOException {
        int increaseAmount = 32 * 1024;
        names = new ArrayBuffer(1 << 17, increaseAmount);
        nameOffsets = new ArrayBuffer(1 << 15, increaseAmount);
        entryInfo = new ArrayBuffer(1 << 17, increaseAmount);
        entryInfoOffsets = new ArrayBuffer(1 << 15, increaseAmount);
        completed = new ArrayBuffer(1 << 18, increaseAmount * 2);
    }
}
