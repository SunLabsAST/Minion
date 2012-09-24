package com.sun.labs.minion;


import java.util.logging.Logger;

/**
 * A container for information related to a query term and a result.  This
 * information can be 
 */
public class QueryTerm {

    private static final Logger logger = Logger.getLogger(QueryTerm.class.getName());
    
    /**
     * The frequency of this term in a document.
     */
    private int tf;
    
    /**
     * The number of documents this term appears in.
     */
    private int df;
    
    /**
     * The weight assigned to this term, according to whatever weighting function
     * was used during querying ({@link com.sun.labs.minion.retrieval.TFIDF} by default).
     */
    private float weight;

    public int getTf() {
        return tf;
    }

    public void setTf(int tf) {
        this.tf = tf;
    }

    public int getDf() {
        return df;
    }

    public void setDf(int df) {
        this.df = df;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }
    
}
