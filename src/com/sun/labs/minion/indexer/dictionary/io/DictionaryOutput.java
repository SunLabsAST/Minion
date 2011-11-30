package com.sun.labs.minion.indexer.dictionary.io;

import com.sun.labs.minion.indexer.dictionary.DictionaryHeader;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.NameEncoder;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.RandomAccessFile;

/**
 * An interface for outputting encoded dictionary data.  A single instance of
 * a dictionary output can be used for outputting multiple dictionaries.
 */
public interface DictionaryOutput extends WriteableBuffer {
    
    public void start(MemoryDictionary dict, NameEncoder encoder, MemoryDictionary.Renumber renumber, int nChans);
    
    /**
     * Gets a header for the dictionary currently being dumped.
     * @return 
     */
    public DictionaryHeader getHeader();
    
    /**
     * Writes an entry from a dictionary to this output.
     * @param e the entry to write
     * @throws IllegalStateException if you try to write entries before calling
     * {@link #start}
     */
    public void write(IndexEntry e);

    /**
     * Tells this dictionary output that we're done writing entries for this
     * dictionary.
     * @throws IllegalStateException if you try to finish a dictionary before
     * starting one.
     */
    public void finish();
    
    /**
     * Flushes the data stored here to permanent storage.
     * @param file the file to which the data should be stored.
     * @throws IllegalStateException if you try to flush while a dictionary is
     * being written.
     */
    public void flush(RandomAccessFile file) throws java.io.IOException;
    
    /**
     * Cleans up the result of a failed dictionary output operation, dropping
     * whatever data 
     */
    public void cleanUp();
    
    /**
     * Writes a buffer to the data for this dictionary output. Buffers can only
     * be written to a dictionary output when a dictionary is <em>not</em> being
     * written to the output.
     * @param b the buffer to append
     * @throws IllegalStateException if we're in the process of writing a 
     * dictionary to this output
     */
    public void write(ReadableBuffer b);
    
    /**
     * Closes this dictionary output.
     */
    public void close() throws java.io.IOException;
}
