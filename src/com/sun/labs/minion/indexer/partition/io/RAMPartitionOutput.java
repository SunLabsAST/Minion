package com.sun.labs.minion.indexer.partition.io;

import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.dictionary.io.RAMDictionaryOutput;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.RAMPostingsOutput;
import com.sun.labs.minion.util.FileLockException;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to hold the various pieces of information that are needed while
 * dumping a partition to memory.
 */
public class RAMPartitionOutput extends AbstractPartitionOutput {

    private static final Logger logger = Logger.getLogger(RAMPartitionOutput.class.getName());

    /**
     * Creates a dump state that can be passed around during dumping.
     * @param partNumber the partition number that we're dumping
     * @param manager the manager responsible for the partition that we're dumping
     * @param files the files into which we'll write the partition data.
     */
    public RAMPartitionOutput(PartitionManager manager) throws IOException, FileLockException {
        super(manager);
    }

    @Override
    public int startPartition() throws IOException {
        int ret = super.startPartition();
        partDictOut = new RAMDictionaryOutput(manager.getIndexDir());
        postOut = new PostingsOutput[postOutFiles.length];
        for(int i = 0; i < postOut.length; i++) {
            postOut[i] = new RAMPostingsOutput();
        }
        vectorLengthsBuffer = new ArrayBuffer(1024);
        deletionsBuffer = new ArrayBuffer(1024);
        return ret;
    }

    @Override
    public DictionaryOutput getTermStatsDictionaryOutput() {
        if(termStatsDictOut == null) {
            try {
                termStatsDictOut = new RAMDictionaryOutput(manager.getIndexDir());
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format("Error getting term stats dictionary"), ex);
                return null;
            }
        }
        return termStatsDictOut;
    }

    @Override
    public void flush(Set<String> keys) throws IOException {
        for(int i = 0; i < postOutFiles.length; i++) {
            RandomAccessFile raf = new RandomAccessFile(postOutFiles[i], "rw");
            ((RAMPostingsOutput) postOut[i]).flush(raf);
            raf.close();
        }
        super.flush(keys);
    }
}
