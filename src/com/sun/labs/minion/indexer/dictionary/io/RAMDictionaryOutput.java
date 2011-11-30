package com.sun.labs.minion.indexer.dictionary.io;

import com.sun.labs.minion.util.buffer.ArrayBuffer;
import java.io.File;

/**
 * A class for outputting dictionaries to RAM-based buffers.
 */
public class RAMDictionaryOutput extends AbstractDictionaryOutput {
    
    public RAMDictionaryOutput(File path) throws java.io.IOException {
        names = new ArrayBuffer(1 << 17);
        nameOffsets = new ArrayBuffer(1 << 15);
        entryInfo = new ArrayBuffer(1 << 17);
        entryInfoOffsets = new ArrayBuffer(1 << 15);
        completed = new ArrayBuffer(1 << 18);
    }
}
