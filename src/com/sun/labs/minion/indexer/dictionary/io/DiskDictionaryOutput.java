package com.sun.labs.minion.indexer.dictionary.io;

import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.FileWriteableBuffer;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 * A class for outputting dictionaries to disk-based files using disk-based
 * temporary buffers.
 */
public class DiskDictionaryOutput extends AbstractDictionaryOutput {
    
    private static final Logger logger = Logger.getLogger(DiskDictionaryOutput.class.getName());
    
    /**
     * A file to hold the temporary names buffer.
     */
    protected File namesFile;

    /**
     * Random access file for the temporary names buffer.
     */
    protected RandomAccessFile namesRAF;

    /**
     * A file to hold the temporary name offsets buffer.
     */
    protected File nameOffsetsFile;

    /**
     * Random access file for the temporary names offsets buffer.
     */
    protected RandomAccessFile nameOffsetsRAF;

    /**
     * A file to hold the temporary info buffer.
     */
    protected File infoFile;

    /**
     * Random access file for the temporary names buffer.
     */
    protected RandomAccessFile infoRAF;

    /**
     * A file to hold the temporary name offsets buffer.
     */
    protected File infoOffsetsFile;

    /**
     * Random access file for the temporary names offsets buffer.
     */
    protected RandomAccessFile infoOffsetsRAF;
    
    /**
     * A file to hold the temporary name offsets buffer.
     */
    protected File completedFile;

    /**
     * Random access file for the temporary names offsets buffer.
     */
    protected RandomAccessFile completedRAF;
    
    /**
     * Default output buffer size.
     */
    protected static final int DEFAULT_OUT_BUFFER_SIZE = 128 * 1024;

    public DiskDictionaryOutput(File path) throws java.io.IOException {
        this(path, DEFAULT_OUT_BUFFER_SIZE);
    }
    
    public DiskDictionaryOutput(File path, int bufferSize) throws java.io.IOException {

        //
        // Temp files for the buffers we'll write.
        namesFile = Util.getTempFile(path, "names", ".n");
        namesRAF = new RandomAccessFile(namesFile, "rw");
        names = new FileWriteableBuffer(namesRAF, bufferSize);
        
        nameOffsetsFile = Util.getTempFile(path, "offsets", ".no");
        nameOffsetsRAF = new RandomAccessFile(nameOffsetsFile, "rw");
        nameOffsets = new FileWriteableBuffer(nameOffsetsRAF, bufferSize);

        infoFile = Util.getTempFile(path, "info", ".i");
        infoRAF = new RandomAccessFile(infoFile, "rw");
        entryInfo = new FileWriteableBuffer(infoRAF, bufferSize);

        infoOffsetsFile = Util.getTempFile(path, "infooff", ".io");
        infoOffsetsRAF = new RandomAccessFile(infoOffsetsFile, "rw");
        entryInfoOffsets = new FileWriteableBuffer(infoOffsetsRAF, bufferSize);
        
        completedFile = Util.getTempFile(path, "comp", ".comp");
        completedRAF = new RandomAccessFile(completedFile, "rw");
        completed = new FileWriteableBuffer(completedRAF, bufferSize);
    }

    @Override
    public void close() throws java.io.IOException {
        super.close();
        namesRAF.close();
        namesFile.delete();
        nameOffsetsRAF.close();
        nameOffsetsFile.delete();
        infoRAF.close();
        infoFile.delete();
        infoOffsetsRAF.close();
        infoOffsetsFile.delete();
        ((FileWriteableBuffer) completed).flush();
        completedRAF.close();
        completedFile.delete();
    }
}
