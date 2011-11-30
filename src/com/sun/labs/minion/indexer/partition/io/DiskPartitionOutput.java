package com.sun.labs.minion.indexer.partition.io;

import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.dictionary.io.DiskDictionaryOutput;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to hold the various pieces of information that are needed while
 * dumping a partition.
 */
public class DiskPartitionOutput extends AbstractPartitionOutput {

    private static final Logger logger = Logger.getLogger(DiskPartitionOutput.class.getName());

    /**
     * Streams for the postings output.
     */
    public OutputStream[] postStream;

    /**
     * Creates a dump state that can be passed around during dumping.
     * @param partNumber the partition number that we're dumping
     * @param manager the manager responsible for the partition that we're dumping
     * @param files the files into which we'll write the partition data.
     */
    public DiskPartitionOutput(PartitionManager manager) throws IOException {
        super(manager);
        partDictOut = new DiskDictionaryOutput(manager.getIndexDir());
    }
    
    public DiskPartitionOutput(File outputDir) throws IOException {
        super(outputDir);
    }

    @Override
    public int startPartition(MemoryPartition partition) throws IOException {
        int ret = super.startPartition(partition);
        postStream = new OutputStream[postOutFiles.length];
        postOut = new PostingsOutput[postStream.length];
        for(int i = 0; i < postOutFiles.length; i++) {
            postStream[i] = new BufferedOutputStream(
                    new FileOutputStream(postOutFiles[i]), 512 * 1024);
            postOut[i] = new StreamPostingsOutput(postStream[i]);
        }
        return ret;
    }

    @Override
    public DictionaryOutput getTermStatsDictionaryOutput() {
        if(termStatsDictOut == null) {
            try {
                termStatsDictOut = new DiskDictionaryOutput(partitionManager.getIndexDir());
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format("Error getting term stats dictionary"), ex);
                return null;
            }
        }
        return termStatsDictOut;
    }

    @Override
    public void close() throws IOException {
        super.close();
        if(postStream != null) {
            for(int i = 0; i < postStream.length; i++) {
                postStream[i].close();
            }
        }
    }
}
