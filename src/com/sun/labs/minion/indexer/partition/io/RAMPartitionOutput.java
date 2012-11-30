package com.sun.labs.minion.indexer.partition.io;

import com.sun.labs.minion.indexer.dictionary.io.RAMDictionaryOutput;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.RAMPostingsOutput;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 * A class to hold the various pieces of information that are needed while
 * dumping a partition to memory.
 */
public class RAMPartitionOutput extends AbstractPartitionOutput {

    private static final Logger logger = Logger.getLogger(RAMPartitionOutput.class.getName());

    /**
     * Creates a partition output that will marshal data into in-memory buffers
     * @param manager the manager responsible for the partition that we're dumping
     */
    public RAMPartitionOutput(PartitionManager manager) throws IOException {
        super(manager);
        partDictOut = new RAMDictionaryOutput(manager.getIndexDir());
        vectorLengthsBuffer = new ArrayBuffer(8 * 1024);
        deletionsBuffer = new ArrayBuffer(8 * 1024);
    }

    @Override
    public int startPartition(MemoryPartition partition) throws IOException {
        int ret = super.startPartition(partition);
        if(postOut == null) {
            postOut = new PostingsOutput[postOutFiles.length];
            for(int i = 0; i < postOutFiles.length; i++) {
                postOut[i] = new RAMPostingsOutput();
            }
        }
        return ret;
    }
    
    @Override
    public int startPartition(String[] postingsChannelNames) throws IOException {
        int ret = super.startPartition(postingsChannelNames);
        if(postOut == null) {
            postOut = new PostingsOutput[postOutFiles.length];
            for(int i = 0; i < postOutFiles.length; i++) {
                postOut[i] = new RAMPostingsOutput();
            }
        }
        return ret;
    }

    @Override
    public void flush() throws IOException {
        for(int i = 0; i < postOutFiles.length; i++) {
            RandomAccessFile raf = new RandomAccessFile(postOutFiles[i], "rw");
            ((RAMPostingsOutput) postOut[i]).flush(raf);
            raf.close();
        }
        super.flush();
    }
}
