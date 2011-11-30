package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.NameEncoder;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.dictionary.io.DiskDictionaryOutput;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
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
public class DumpState {

    private static final Logger logger = Logger.getLogger(DumpState.class.getName());

    /**
     * The path where the dump will be occurring.
     */
    public File indexDir;

    /**
     * The number of the partition that is being dumped.
     */
    public int partNumber;

    /**
     * The partition manager for the partition that is being dumped.
     */
    public PartitionManager manager;

    /**
     * A header for the partition being dumped.
     */
    public PartitionHeader partHeader;

    /**
     * A dictionary output for this dump.
     */
    public DictionaryOutput fieldDictOut;

    /**
     * A dictionary output for the term stats for the entire collection.
     */
    public DictionaryOutput termStatsDictOut;

    /**
     * Streams for the postings output.
     */
    public OutputStream[] postStream;

    /**
     * A place to put postings.
     */
    public PostingsOutput[] postOut;

    /**
     * A random access file for document vector length output.
     */
    public RandomAccessFile vectorLengthsFile;

    /**
     * An encoder for names during the dump.
     */
    public NameEncoder encoder;

    /**
     * How entries should be renumbered.
     */
    public MemoryDictionary.Renumber renumber;

    /**
     * How IDs in the dictionary should be remapped.
     */
    public MemoryDictionary.IDMap idMap;

    /**
     * A postings ID map to use when dumping.
     */
    public int[] postIDMap;

    /**
     * The maximum document ID for the dump.
     */
    public int maxDocID;

    /**
     * Creates a dump state that can be passed around during dumping.
     * @param partNumber the partition number that we're dumping
     * @param manager the manager responsible for the partition that we're dumping
     * @param files the files into which we'll write the partition data.
     */
    public DumpState(int partNumber, PartitionManager manager, File[] files) throws IOException {
        this.partNumber = partNumber;
        this.manager = manager;
        indexDir = manager.getIndexDir();
        fieldDictOut = new DiskDictionaryOutput(indexDir);
        postStream = new OutputStream[files.length - 1];
        postOut = new PostingsOutput[postStream.length];
        for(int i = 1; i < files.length; i++) {
            logger.info(String.format("file: %s", files[i]));
            postStream[i - 1] =
                    new BufferedOutputStream(new FileOutputStream(files[i]),
                    32768);
            postOut[i - 1] = new StreamPostingsOutput(postStream[i - 1]);
        }
    }

    public DumpState(String indexDir) throws IOException {
        this(new File(indexDir));
    }

    public DumpState(File indexDir) throws IOException {
        this.indexDir = indexDir;
        fieldDictOut = new DiskDictionaryOutput(indexDir);
        postStream = new OutputStream[0];
        postOut = new PostingsOutput[0];
    }

    public void close() throws IOException {
        for(int i = 0; i < postStream.length; i++) {
            postOut[i].flush();
            postStream[i].close();
        }
        if(fieldDictOut != null) {
            fieldDictOut.close();
        }
        if(termStatsDictOut != null) {
            termStatsDictOut.close();
        }

    }
}
