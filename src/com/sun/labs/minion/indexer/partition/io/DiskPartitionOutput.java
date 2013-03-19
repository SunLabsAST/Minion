package com.sun.labs.minion.indexer.partition.io;

import com.sun.labs.minion.indexer.dictionary.io.DiskDictionaryOutput;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.FileWriteableBuffer;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 * A class to hold the various pieces of information that are needed while
 * dumping a partition.
 */
public class DiskPartitionOutput extends AbstractPartitionOutput {

    private static final Logger logger = Logger.getLogger(DiskPartitionOutput.class.getName());
    
    /**
     * The file that will hold our on-disk vector lengths as we build them.
     */
    private File vecLenFile;
    
    /**
     * The random access file that will contain the vector lengths.
     */
    private RandomAccessFile vecLenRAF;
    
    /**
     * The file for the deletion buffer.
     */
    private File delFile;
    
    /**
     * The file that will contain the vector lengths.
     */
    private RandomAccessFile delRAF;

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
        
        vecLenFile = Util.getTempFile(manager.getIndexDir(), "veclens", ".vl");
        vecLenRAF = new RandomAccessFile(vecLenFile, "rw");
        vectorLengthsBuffer = new FileWriteableBuffer(vecLenRAF, 64 * 1024);
        
        delFile = Util.getTempFile(manager.getIndexDir(), "temp", ".del");
        delRAF = new RandomAccessFile(delFile, "rw");
        deletionsBuffer = new FileWriteableBuffer(delRAF, 64 * 1024);
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
    public int startPartition(String[] postingsChannelNames) throws IOException {
        int ret = super.startPartition(postingsChannelNames);
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
    public void close() throws IOException {
        super.close();
        if(postStream != null) {
            for(int i = 0; i < postStream.length; i++) {
                postStream[i].close();
            }
        }
        vecLenRAF.close();
        vecLenFile.delete();
        delRAF.close();
        delFile.delete();
    }

    @Override
    public String toString() {
        return "DPO: " + super.getName() + ' ' + partition + " partition number: " + partitionNumber; 
    }
    
    
}
