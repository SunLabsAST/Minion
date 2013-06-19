package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * A collection of query term statistics for a single document.
 * 
 * @see SearchEngineImpl#getQueryTermStats(com.sun.labs.minion.QueryConfig, java.util.Collection, java.lang.String, com.sun.labs.minion.indexer.Field.TermStatsType)
 */
public class QueryTermDocStats implements Iterable<QueryTermDocStats.TermStat> {

    private static final Logger logger = Logger.getLogger(QueryTermDocStats.class.getName());
    
    /**
     * A search result that can be (eventually) returned
     */
    private ResultImpl result;
 
    /**
     * The vector length of the document.
     */
    private float docLength;
    
    /**
     * The per-query term stats for this document.
     */
    private List<TermStat> queryTerms = new ArrayList<TermStat>();

    public QueryTermDocStats(InvFileDiskPartition part, int docID, float docLength) {
        result = new ResultImpl();
        result.partition = part;
        result.doc = docID;
        this.docLength = docLength;
    }
    
    public void add(String term, int tf, float weight) {
        queryTerms.add(new TermStat(term, tf, weight));
    }

    /**
     * Get the search result corresponding to this document. The score of the
     * result will be a normalized sum of the weights of the individual terms and
     * should correspond well to the score that the document would have recieved
     * from an OR query including those terms.
     * @return the search result corresponding to this document.
     */
    public ResultImpl getResult() {
        float sum = 0;
        for(TermStat s : queryTerms) {
            sum += s.weight;
        }
        result.score = sum / docLength;
        return result;
    }

    /**
     * Gets the vector length of the document.
     * @return the vector length of the document.
     */
    public float getDocLength() {
        return docLength;
    }

    @Override
    public Iterator<TermStat> iterator() {
        return queryTerms.iterator();
    }

    /**
     * Gets the list of individual term stats.
     * @return the list of term statistics.
     */
    public List<TermStat> getQueryTerms() {
        return queryTerms;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(result.getKey());
        for(TermStat ts : queryTerms) {
            sb.append(' ').append(ts.toString());
        }
        return sb.toString();
    }

    /**
     * An individual term statistic.
     */
    public class TermStat {
        
        String term;

        int tf;

        float weight;
        
        public TermStat(String term, int tf, float weight) {
            this.term = term;
            this.tf = tf;
            this.weight = weight;
        }

        public String getTerm() {
            return term;
        }

        public int getTf() {
            return tf;
        }

        public float getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return String.format("%s %d %.3f", term, tf, weight);
        }
    }
}
