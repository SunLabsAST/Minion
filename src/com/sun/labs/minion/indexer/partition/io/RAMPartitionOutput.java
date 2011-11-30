package com.sun.labs.minion.indexer.partition.io;

import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.dictionary.io.RAMDictionaryOutput;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.RAMPostingsOutput;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to hold the various pieces of information that are needed while
 * dumping a partition to memory.
 */
public class RAMPartitionOutput extends AbstractPartitionOutput {

    private static final Logger logger = Logger.getLogger(RAMPartitionOutput.class.getName());

    /**
     * Creates a partition output that will marshall data into in-memory buffers
     * @param manager the manager responsible for the partition that we're dumping
     */
    public RAMPartitionOutput(PartitionManager manager) throws IOException {
        super(manager);
        partDictOut = new RAMDictionaryOutput(manager.getIndexDir());
    }

    @Override
    public DictionaryOutput getTermStatsDictionaryOutput() {
        if(termStatsDictOut == null) {
            try {
                termStatsDictOut = new RAMDictionaryOutput(partitionManager.getIndexDir());
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format("Error getting term stats dictionary"), ex);
                return null;
            }
        }
        return termStatsDictOut;
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
    public void flush() throws IOException {
        for(int i = 0; i < postOutFiles.length; i++) {
            RandomAccessFile raf = new RandomAccessFile(postOutFiles[i], "rw");
            ((RAMPostingsOutput) postOut[i]).flush(raf);
            raf.close();
        }
        super.flush();
    }
}
