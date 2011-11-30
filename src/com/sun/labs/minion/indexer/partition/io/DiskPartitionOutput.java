package com.sun.labs.minion.indexer.partition.io;

import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.dictionary.io.DiskDictionaryOutput;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
import com.sun.labs.minion.util.FileLockException;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
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
    public DiskPartitionOutput(PartitionManager manager) throws IOException, FileLockException {
        super(manager);
        File[] files = MemoryPartition.getMainFiles(manager, partNumber);
        partDictOut = new DiskDictionaryOutput(manager.getIndexDir());
        postStream = new OutputStream[files.length - 1];
        postOut = new PostingsOutput[postStream.length];
        for(int i = 1; i < files.length; i++) {
            postStream[i - 1] = new BufferedOutputStream(
                    new FileOutputStream(files[i]), 32768);
            postOut[i - 1] = new StreamPostingsOutput(postStream[i - 1]);
        }

        vectorLengthsBuffer = new ArrayBuffer(1024);
        deletionsBuffer = new ArrayBuffer(1024);
    }

    public DiskPartitionOutput(File indexDir) throws IOException, FileLockException {
        super(indexDir);
        File[] files = new File[] {new File(indexDir, "p1.dict")};
        partDictOut = new DiskDictionaryOutput(manager.getIndexDir());
        postStream = new OutputStream[files.length - 1];
        postOut = new PostingsOutput[postStream.length];
        for(int i = 1; i < files.length; i++) {
            postStream[i - 1] = new BufferedOutputStream(
                    new FileOutputStream(files[i]), 32768);
            postOut[i - 1] = new StreamPostingsOutput(postStream[i - 1]);
        }

        vectorLengthsBuffer = new ArrayBuffer(1024);
        deletionsBuffer = new ArrayBuffer(1024);
    }
    @Override
    public DictionaryOutput getTermStatsDictionaryOutput() {
        if(termStatsDictOut == null) {
            try {
                termStatsDictOut = new DiskDictionaryOutput(manager.getIndexDir());
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
        for(int i = 0; i < postStream.length; i++) {
            postStream[i].close();
        }
    }
}
