package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.indexer.postings.PostingsIterator;

/**
 *
 */
public class FreqGroup {

    private String term;
    
    private int[] docs;
    
    private int[] freqs;
    
    private float[] weights;
    
    public FreqGroup(String term, PostingsIterator pi) {
        this.term = term;
        docs = new int[pi.getN()];
        freqs = new int[pi.getN()];
        weights = new float[pi.getN()];
        int p = 0;
        while(pi.next()) {
            docs[p] = pi.getID();
            freqs[p] = pi.getFreq();
            weights[p] = pi.getWeight();
            p++;
        }
    }
    
    public FreqGrouperator grouperator(int index) {
        return new FreqGrouperator(index);
    }

    public class FreqGrouperator implements Comparable<FreqGrouperator> {

        int p = -1;
        
        int[] ld = docs;
        int[] lf = freqs;
        
        int index;
        
        public FreqGrouperator(int index) {
            this.index = index;
        }
        
        public boolean next() {
            return ++p < docs.length;
        }
        
        public int getID() {
            return docs[p];
        }
        
        public int getFreq() {
            return freqs[p];
        }

        public float getWeight() {
            return weights[p];
        }

        public int getIndex() {
            return index;
        }
        
        @Override
        public int compareTo(FreqGrouperator o) {
            return ld[p] - o.ld[o.p];
        }
        
    }
    
}
