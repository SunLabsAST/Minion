package com.sun.labs.minion.retrieval.test;


import com.sun.labs.minion.indexer.partition.DiskPartition;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A holder for data for a query.  We should only make a few of these and re-use
 * them as necessary.
 * 
 */
public class QueryData {

    private static final Logger logger = Logger.getLogger(QueryData.class.getName());
    
    /**
     * The partition for which we're storing data.
     */
    public DiskPartition part;

    /**
     * A place to hold counts per document.
     */
    public int[] counts;
    
    /**
     * A place to hold scores.
     */
    public float[] scores;
    
    public QueryData(DiskPartition part) {
        this.part = part;
        counts = new int[part.getMaxDocumentID()+1];
        scores = new float[counts.length];
    }
    
    public void clearCounts() {
        Arrays.fill(counts, 0);
    }
    
    public void clearScores() {
        Arrays.fill(scores, 0);
    }
    
    public void clear() {
        clearCounts();
        clearScores();
    }
    
}
