package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.entry.EntryMapper;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
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
     * A dictionary output for the merged dictionaries.
     */
    public PartitionOutput partOut;
    
    /**
     * The field being merged.
     */
    public FieldInfo info;
    
    /**
     * The number of documents in the merged partition.
     */
    public int nDocs;

    /**
     * The new maximum document ID for the merged data;
     */
    public int maxDocID;
    
    /**
     * Mappers for the entries in a dictionary.  Mostly used for the document
     * dictionary and for the document vector dictionaries so that we can
     * remove the entries for deleted documents.
     */
    public EntryMapper[] docIDMappers;
    
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
     * The new maximum document ID in a partition, after deleted documents are
     * taken into account.  We need to keep this around in order to fill in the
     * data for partitions that didn't have saved data for a particular field
     * when merging partitions.
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
    
    public MergeState(PartitionOutput partOut) {
        this.manager = partOut.getPartitionManager();
        this.partOut = partOut;
    }
}
