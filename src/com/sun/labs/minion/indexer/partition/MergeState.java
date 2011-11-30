package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.File;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * A container for various merge information that can be passed around during
 * merges, so that we don't have to have 14,000 parameters on each merge call.
 */
public class MergeState implements Cloneable {

    private static final Logger logger = Logger.getLogger(MergeState.class.
            getName());
    
    /**
     * The manager for the partitions being merged.
     */
    public PartitionManager manager;
    
    /**
     * The partitions being merged.
     */
    public DiskPartition[] partitions;
    
    /**
     *  A factory for the entries in the token dictionaries being merged.
     */
    public EntryFactory entryFactory;
    
    /**
     * A dictionary output for the merged dictionaries.
     */
    public DictionaryOutput fieldDictOut;
    
    /**
     * The field being merged.
     */
    public FieldInfo info;
    
    /**
     * The number for the merged partition.
     */
    public int partNumber;

    /**
     * The header for the merged partition.
     */
    public PartitionHeader header;
    
    /**
     * The number of documents in the merged partition.
     */
    public int nDocs;

    /**
     * The new maximum document ID for the merged data;
     */
    public int maxDocID;

    /**
     * An array of the new starting document ID for each of the merged partitions.
     */
    public int[] docIDStarts;
    
    /**
     * An array of fake starting ids, used for merging postings that do not contain
     * document IDs.
     */
    public int[] fakeStarts;

    /**
     * The number of undeleted documents in each partition that is being merged.
     */
    public int[] nUndel;

    /**
     * Maps from old IDs in the postings lists that we'll be merging to new
     * IDs.  Used when garbage collecting postings lists.
     */
    public int[][] postIDMaps;

    /**
     * Maps from old document IDs to new document IDs for the garbage-collected
     * merge.
     */
    public int[][] docIDMaps;
    
    /**
     * Maps from old entry IDs to new entry IDs from the merged dictionary.
     */
    public int[][] entryIDMaps;
    
    /**
     * The files containing the merged postings.
     */
    public File[] postFiles;

    /**
     * A place where postings can be written;
     */
    public PostingsOutput[] postOut;
    
    /**
     * The underlying streams for the postings.
     */
    public OutputStream[] postStreams;
    
    /**
     * A place where vector lengths can be written.
     */
    public WriteableBuffer vectorLengthsBuffer;
    
    public MergeState(PartitionManager manager) {
        this.manager = manager;
    }
}
