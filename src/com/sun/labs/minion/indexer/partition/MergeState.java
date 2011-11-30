package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
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
    public int[] docIDStart;

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
     * The file containing the merged dictionary.
     */
    public File dictFile;

    /**
     * A place where dictionaries can be written;
     */
    public RandomAccessFile dictRAF;
    
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
    public RandomAccessFile vectorLengthRAF;
    
    public MergeState(PartitionManager manager) {
        this.manager = manager;
    }
}
