package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.WeightedFeature;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.pipeline.StopWords;
import com.sun.labs.minion.util.Util;
import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 *
 */
public abstract class AbstractDocumentVector implements DocumentVector, Serializable {

    /**
     * The search engine that generated this vector.
     */
    protected transient SearchEngine e;

    /**
     * Words that can be ignored in the vector we're building
     */
    protected transient StopWords ignoreWords;

    /**
     * The document key for this entry.
     */
    protected transient String key;

    /**
     * The entry from the document dictionary for this partition.
     */
    protected transient QueryEntry keyEntry;

    /**
     * The length of this document vector.
     */
    protected float length;

    /**
     * Whether we've been normalized.
     */
    protected boolean normalized = false;

    /**
     * Query stats for the operations that we're performing.
     */
    protected transient QueryStats qs = new QueryStats();

    /**
     * An array to hold the features that make up our vector. This array must be
     * ordered by feature name!
     */
    protected WeightedFeature[] v;

    /**
     * A set of weighting components that can be used when calculating term
     * weights.
     */
    protected transient WeightingComponents wc;

    /**
     * The weighting function to use for computing term weights.
     */
    protected transient WeightingFunction wf;

    public AbstractDocumentVector() {
    }

    /**
     * Calculates the dot product of this feature vector and another feature
     * vector.
     *
     * @param wfv a weighted feature vector
     * @return the dot product of the two vectors (i.e. the sum of the products
     * of the components in each dimension)
     */
    public float dot(WeightedFeature[] wfv) {
        getFeatures();
        float res = 0;
        int i1 = 0;
        int i2 = 0;
        while(i1 < v.length && i2 < wfv.length) {
            WeightedFeature f1 = v[i1];
            WeightedFeature f2 = wfv[i2];
            int cmp = f1.getName().compareTo(f2.getName());
            if(cmp == 0) {
                if(ignoreWords == null || !ignoreWords.isStop(f1.getName())) {
                    //
                    // The term names are the same, so we'll have some
                    // non-zero value to add for this term's dimension
                    res += f1.getWeight() * f2.getWeight();
                }
                i1++;
                i2++;
            } else if(cmp < 0) {
                //
                // fv is zero in this dimension
                i1++;
            } else {
                //
                // v is zero in this dimension
                i2++;
            }
        }
        return res;
    }

    /**
     * Gets the search engine used to build this vector.
     *
     * @return the engine
     */
    public SearchEngine getEngine() {
        return e;
    }

    /**
     * Gets the features representing this vector.
     *
     * @return the features.
     */
    public abstract WeightedFeature[] getFeatures();

    @Override
    public String getKey() {
        return key;
    }

    /**
     * Gets the number of distinct terms in this document vector (the
     * cardinality, rather than the length)
     *
     * @return the number of distinct terms
     */
    public int getNumDistinct() {
        return getFeatures().length;
    }

    /**
     * Gets a sorted set of features.
     *
     * @return a set of the features in this vector, sorted by name
     */
    public SortedSet<WeightedFeature> getSet() {
        getFeatures();
        SortedSet<WeightedFeature> ret = new TreeSet();
        for(int i = 0; i < v.length; i++) {
            if(v[i].getWeight() != 0) {
                ret.add(v[i]);
            }
        }
        return ret;
    }

    /**
     * Computes the similarity between this document vector and the supplied
     * vector. The larger the value, the greater the similarity. The measurement
     * returned is the cosine of the angle between the vectors.
     *
     * @param otherVector the vector representing the document to compare this
     * vector to
     * @return the cosine of the angle between the two vectors
     */
    @Override
    public float getSimilarity(DocumentVector otherVector) {
        if(otherVector instanceof AbstractDocumentVector) { 
            return dot(((AbstractDocumentVector) otherVector).getFeatures());
        } else {
            throw new IllegalArgumentException("Can't compute similarity to non AbstractDocumentVectors");
        }
    }

    /**
     * Finds similar documents to this one. An OR is run with all the terms in
     * the documents. The resulting docs are returned ordered from most similar
     * to least similar.
     *
     * @return documents similar to the one this vector represents
     */
    @Override
    public ResultSet findSimilar() {
        return findSimilar("-score");
    }

    @Override
    public ResultSet findSimilar(String sortOrder) {
        return findSimilar(sortOrder, 1.0);
    }

    @Override
    public Set<String> getTerms() {
        getFeatures();
        Set<String> ret = new HashSet<String>();
        for(WeightedFeature feat : v) {
            ret.add(feat.getName());
        }
        return ret;
    }

    // Doc inherited from interface.  Gets a HashMap of the top N terms, or
    // docLength terms if docLength < N.  The map is from String term names
    // to Float weights.
    @Override
    public Map<String, Float> getTopWeightedTerms(int nTerms) {
        getFeatures();
        //
        // No need to sort when we're getting everything!
        if(nTerms >= v.length) {
            Map<String, Float> ret = new LinkedHashMap<String, Float>();
            for(int i = 0; i < v.length; i++) {
                ret.put(v[i].getName(), v[i].getWeight());
            }
            return ret;
        }
        //
        // We need to sort all the terms by weight.  I'm not concerned
        // with efficiency here, although maybe I should be?
        WeightedFeature[] w = v.clone();
        Util.sort(w, WeightedFeature.INVERSE_WEIGHT_COMPARATOR);
        Map<String, Float> m = new LinkedHashMap();
        nTerms = Math.min(nTerms, w.length);
        for(int i = 0; i < nTerms; i++) {
            if(ignoreWords == null || !ignoreWords.isStop(w[i].getName())) {
                m.put(w[i].getName(), w[i].getWeight());
            }
        }
        return m;
    }

    /**
     * Gets the total number of occurrences of terms in this vector
     *
     * @return the total number of term occurrences
     */
    public int getTotalOccurrences() {
        getFeatures();
        int n = 0;
        for(WeightedFeature feat : v) {
            n += feat.getFreq();
        }
        return n;
    }

    public SortedSet<WeightedFeature> getWeightOrderedSet() {
        getFeatures();
        SortedSet<WeightedFeature> ret = new TreeSet<WeightedFeature>(WeightedFeature.INVERSE_WEIGHT_COMPARATOR);
        for(int i = 0; i < v.length; i++) {
            if(v[i].getWeight() != 0) {
                ret.add(v[i]);
            }
        }
        return ret;
    }

    /**
     * Gets the Euclidean length of this vector.
     */
    public float length() {
        return length;
    }

    /**
     * Normalizes the length of this vector to 1.
     */
    public void normalize() {
        if(normalized) {
            return;
        }
        getFeatures();
        for(int i = 0; i < v.length; i++) {
            if(length() == 0) {
                v[i].setWeight(0);
            } else {
                v[i].setWeight(v[i].getWeight() / length());
            }
        }
        normalized = true;
    }

    /**
     * Sets the search engine that this vector will use, which is useful when
     * we've been unserialized and need to get ourselves back into shape.
     *
     * @param e the engine to use
     */
    @Override
    public void setEngine(SearchEngine e) {
        this.e = e;
        QueryConfig qc = e.getQueryConfig();
        wf = qc.getWeightingFunction();
        wc = qc.getWeightingComponents();
        ignoreWords = qc.getVectorZeroWords();
        qs = new QueryStats();
    }

    /**
     * Gets a sorted (by weight) set of the terms contributing to document
     * similarity with the provided document. The set consists of
     * WeightedFeatures that represent the terms that each document have in
     * common and their combined weights.
     *
     * @param dvi the document to compare this one to
     * @return a sorted set of WeightedFeature that occurred in both documents
     */
    public SortedSet<WeightedFeature> getSimilarityTerms(AbstractDocumentVector dvi) {
        WeightedFeature[] other = dvi.getFeatures();
        getFeatures();
        int i1 = 0;
        int i2 = 0;
        int x = 0;
        SortedSet<WeightedFeature> s = new TreeSet(WeightedFeature.INVERSE_WEIGHT_COMPARATOR);
        //
        // Go in order (alphabetically) through the two arrays, finding
        // terms that occur in both.
        while(i1 < v.length && i2 < dvi.v.length) {
            WeightedFeature f1 = v[i1];
            WeightedFeature f2 = other[i2];
            int cmp = f1.getName().compareTo(f2.getName());
            if(cmp == 0) {
                if(ignoreWords == null || !ignoreWords.isStop(f1.getName())) {
                    //
                    // We found two terms with the same name.
                    float combined = f1.getWeight() * f2.getWeight();
                    WeightedFeature feat = new WeightedFeature(f1.getName(), combined);
                    s.add(feat);
                }
                i1++;
                i2++;
            } else if(cmp < 0) {
                i1++;
            } else {
                i2++;
            }
        }
        return s;
    }

    @Override
    public Map<String, Float> getSimilarityTermMap(DocumentVector vector) {
        if(!(vector instanceof AbstractDocumentVector)) {
            throw new IllegalArgumentException("Can't process non-AbstractDocumentVectors");
        }
        
        getFeatures();
        WeightedFeature[] ov = ((AbstractDocumentVector) vector).getFeatures();

        int i1 = 0;
        int i2 = 0;
        int x = 0;

        SortedSet<WeightedFeature> s = new TreeSet(WeightedFeature.INVERSE_WEIGHT_COMPARATOR);
        //
        // Go in order (alphabetically) through the two arrays, finding
        // terms that occur in both.
        while(i1 < v.length && i2 < ov.length) {
            WeightedFeature f1 = v[i1];
            WeightedFeature f2 = ov[i2];

            int cmp = f1.getName().compareTo(f2.getName());

            if(cmp == 0) {
                if(ignoreWords == null || !ignoreWords.isStop(f1.getName())) {
                    //
                    // We found two terms with the same name.
                    float combined = f1.getWeight() * f2.getWeight();
                    WeightedFeature wf = new WeightedFeature(f1.getName(),
                            combined);
                    s.add(wf);
                }
                i1++;
                i2++;
            } else if(cmp < 0) {
                i1++;
            } else {
                i2++;
            }
        }
        
        Map<String,Float> ret = new TreeMap<String,Float>();
        for(WeightedFeature feat : s) {
            ret.put(feat.getName(), feat.getWeight());
        }

        return ret;

    }

    /**
     * Gets a map of term names to weights, where the weights represent the
     * amount the term contributed to the similarity of the two documents. Only
     * terms that occur in both documents are returned, as all other terms have
     * weight zero. The keys in the HashMap are sorted according to the natural
     * ordering of their values. That is, the first string returned from an
     * iterator over the key set will be the term with the highest weight.
     *
     * @param dv the document vector to compare this one to
     * @return a sorted hash map of String names to Float weights
     */
    public Map<String, Float> getSimilarityTermMap(AbstractDocumentVector dv) {
        //
        // Get the set of similarity terms, then turn it into
        // an ordered hashmap that is suitable for returning to
        // the user.
        SortedSet<WeightedFeature> s = getSimilarityTerms(dv);
        Map<String, Float> res = new LinkedHashMap<String, Float>();
        for(WeightedFeature f : s) {
            // autobox, transform and roll out!
            res.put(f.getName(), f.getWeight());
        }
        return res;
    }
    
    public String toString() {
        getFeatures();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("size: %d length: %.3f ", v.length, length));
        float ss = 0;
        for(int i = 0; i < v.length; i++) {
            ss += v[i].getWeight() * v[i].getWeight();
            sb.append("\n  <");
            sb.append(v[i].toString());
            sb.append('>');
        }
        sb.append(String.format("\nss: %.3f len: %.3f", ss, Math.sqrt(ss)));
        return sb.toString();
    }
}
