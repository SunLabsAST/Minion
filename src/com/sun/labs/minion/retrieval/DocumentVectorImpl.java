/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */
package com.sun.labs.minion.retrieval;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.WeightedFeature;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.pipeline.StopWords;
import com.sun.labs.minion.util.Util;
import java.io.Serializable;
import java.util.logging.Logger;

/**
 * A class that holds a weighted document vector for a given document from
 * a given partition.  This implementation is meant to handle features from
 * either the entire document or a single vectored field.
 * 
 * @see CompositeDocumentVectorImpl for an implementation that can handle 
 * features from multiple vectored fields.
 */
public class DocumentVectorImpl implements DocumentVector, Serializable {

    /**
     * We'll need to send this along when serializing as we're doing our own
     * serialization via the <code>Externalizable</code> interface.
     */
    public static final long serialVersionUID = 2L;

    /**
     * The search engine that generated this vector.
     */
    protected transient SearchEngine e;

    /**
     * The document key for this entry.
     */
    protected transient QueryEntry key;

    /**
     * The field for this entry.
     */
    protected transient DiskField df;

    /**
     * The name of the key, which will survive transport.
     */
    protected String keyName;

    /**
     * The fields from which this document vector was generated.
     */
    protected transient int[] fields;

    /**
     * The weighting function to use for computing term weights.
     */
    protected transient WeightingFunction wf;

    /**
     * A set of weighting components that can be used when calculating term
     * weights.
     */
    protected transient WeightingComponents wc;

    /**
     * An array to hold the features that make up our vector.  This array
     * must be ordered by feature name!
     */
    protected WeightedFeature[] v;

    /**
     * The length of this document vector.
     */
    protected float length;

    /**
     * Whether we've been normalized.
     */
    protected boolean normalized = false;

    protected transient QueryStats qs = new QueryStats();

    private static Logger logger = Logger.getLogger(DocumentVectorImpl.class.
            getName());

    protected transient StopWords ignoreWords;

    protected String field;

    protected transient int fieldID;

    public DocumentVectorImpl() {
    }

    /**
     * Creates a document vector from a search result.
     *
     * @param r The search result for which we want a document vector.
     */
    public DocumentVectorImpl(ResultImpl r) {
        this(r.set.getEngine(),
             r.ag.part.getDocumentDictionary().getByID(r.doc), null);
    }

    /**
     * Creates a document vector for a particular field from a search result.
     *
     * @param r The search result for which we want a document vector.
     * @param field The name of the field for which we want the document vector.
     * If this value is <code>null</code> a vector for the whole document will
     * be returned.  If the named field is not a field that was indexed with the
     * vectored attribute set, the resulting document vector will be empty!
     */
    public DocumentVectorImpl(ResultImpl r, String field) {
        this(r.set.getEngine(),
             r.ag.part.getDocumentDictionary().getByID(r.doc), field);
    }

    public DocumentVectorImpl(SearchEngine e,
                              WeightedFeature[] basisFeatures) {
        this.e = e;
        this.key = null;
        QueryConfig qc = e.getQueryConfig();
        wf = qc.getWeightingFunction();
        wc = qc.getWeightingComponents();
        v = basisFeatures;

        //
        // Compute the length.
        double ss = 0;
        for(int i = 0; i < v.length; i++) {
            ss += v[i].getWeight() * v[i].getWeight();
        }
        length = (float) Math.sqrt(ss);

        ignoreWords = qc.getVectorZeroWords();
    }

    /**
     * Creates a document vector for a given document.
     *
     * @param e The search engine with which the docuemnt is associated.
     * @param key The entry from the document dictionary for the given
     * document.
     * @param field The name of the field for which we want the document vector.
     * If this value is <code>null</code> a vector for the whole document will
     * be returned.  If this value is the empty string, then a vector for the text
     * not in any defined field will be returned.  If the named field is not a
     * field that was indexed with the
     * vectored attribute set, the resulting document vector will be empty!
     */
    public DocumentVectorImpl(SearchEngine e,
                              QueryEntry key, String field) {
        this(e, key, field, e.getQueryConfig().getWeightingFunction(),
             e.getQueryConfig().getWeightingComponents());
    }

    public DocumentVectorImpl(SearchEngine e,
                              QueryEntry key, String field,
                              WeightingFunction wf,
                              WeightingComponents wc) {

        this.e = e;
        this.key = key;
        keyName = key.getName().toString();
        setField(field);
        this.wf = wf;
        this.wc = wc.setDocument(this.key, ((InvFileDiskPartition) key.
                getPartition()).getDF(field));
        length = wc.dvl;
        initFeatures();
    }

    public DocumentVectorImpl(String key, DiskField df, WeightingFunction wf,
                              WeightingComponents wc) {
        e = df.getPartition().getPartitionManager().getEngine();
        this.key = df.getVector(key);
        keyName = key;
        this.df = df;
        this.wf = wf;
        this.wc = wc.setDocument(this.key, df);
        length = wc.dvl;
        initFeatures();
    }

    /**
     * Builds the features for the feature vector.
     */
    private void initFeatures() {

        QueryEntry ve = df.getVector(keyName);
        if(ve == null) {
            v = new WeightedFeature[0];
            return;
        }

        PostingsIterator pi = ve.iterator(new PostingsIteratorFeatures());
        if(pi == null) {
            v = new WeightedFeature[0];
            return;
        }

        v = new WeightedFeature[pi.getN()];
        int p = 0;
        while(pi.next()) {
            int tid = pi.getID();
            QueryEntry qe = df.getTerm(tid, false);
            v[p++] = new WeightedFeature(qe.getName().toString(), tid, pi.
                    getWeight());
        }
    }

    public DocumentVector copy() {
        DocumentVectorImpl ret = new DocumentVectorImpl();
        ret.e = e;
        ret.key = key;
        ret.df = df;
        ret.keyName = keyName;
        ret.field = field;
        ret.fields = fields != null ? fields.clone() : fields;
        ret.wf = wf;
        ret.wc = wc;
        ret.ignoreWords = ignoreWords;
        ret.v = v != null ? v.clone() : null;
        return ret;
    }

    public WeightedFeature[] getFeatures() {
        if(v == null) {
            initFeatures();
        }
        return v;
    }

    /**
     * Sets the search engine that this vector will use, which is useful when 
     * we've been unserialized and need to get ourselves back into shape.
     * 
     * @param e the engine to use
     */
    public void setEngine(SearchEngine e) {
        this.e = e;
        QueryConfig qc = e.getQueryConfig();
        wf = qc.getWeightingFunction();
        wc = qc.getWeightingComponents();
        ignoreWords = qc.getVectorZeroWords();
        qs = new QueryStats();
        setField(field);
    }

    public QueryEntry getEntry() {
        return key;
    }

    public SearchEngine getEngine() {
        return e;
    }

    /**
     * Calculates the dot product of this document vector with another.
     *
     * @param dvi another document vector
     * @return the dot product of the two vectors (i.e. the sum of the
     * products of the components in each dimension)
     */
    public float dot(DocumentVectorImpl dvi) {
        dvi.getFeatures();
        getFeatures();
        return dot(dvi.v);
    }

    /**
     * Calculates the dot product of this feature vector and another feature
     * vector.
     *
     * @param wfv a weighted feature vector
     * @return the dot product of the two vectors (i.e. the sum of the
     * products of the components in each dimension)
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
     * Two document vectors are equal if all their weighted features
     * are equal (in both name and weight)
     *
     * @param dv the document vector to compare this one to
     * @return true if the document vectors have equal weighed features
     */
    public boolean equals(Object dv) {
        if(!(dv instanceof DocumentVectorImpl)) {
            return false;
        }
        DocumentVectorImpl other = (DocumentVectorImpl) dv;

        //
        // Quick check for equal numbers of terms.
        if(other.v.length != v.length) {
            return false;
        }

        other.getFeatures();
        getFeatures();
        int i = 0;
        while(i < v.length) {
            WeightedFeature f1 = v[i];
            WeightedFeature f2 = other.v[i];

            if(f1.getName().equals(f2.getName()) && f1.getWeight() == f2.
                    getWeight()) {
                i++;
                continue;
            }
            return false;
        }
        return true;
    }

    public Set<String> getTerms() {
        getFeatures();
        Set<String> ret = new HashSet<String>();
        for(WeightedFeature wf : v) {
            ret.add(wf.getName());
        }
        return ret;
    }

    /**
     * Gets a map of term names to weights, where the weights
     * represent the amount the term contributed to the similarity
     * of the two documents.  Only terms that occur in both documents
     * are returned, as all other terms have weight zero.  The keys
     * in the HashMap are sorted according to the natural ordering
     * of their values.  That is, the first string returned from
     * an iterator over the key set will be the term with the
     * highest weight.
     *
     * @param dv the document vector to compare this one to
     * @return a sorted hash map of String names to Float weights
     */
    public Map<String, Float> getSimilarityTermMap(DocumentVector dv) {
        //
        // Get the set of similarity terms, then turn it into
        // an ordered hashmap that is suitable for returning to
        // the user.
        SortedSet s = getSimilarityTerms((DocumentVectorImpl) dv);
        Map<String, Float> res = new LinkedHashMap();
        for(WeightedFeature f : (SortedSet<WeightedFeature>) s) {
            // autobox, transform and roll out!
            res.put(f.getName(), f.getWeight());
        }
        return res;
    }

    /**
     * Gets a sorted (by weight) set of the terms contributing to
     * document similarity with the provided document.  The set consists
     * of WeightedFeatures that represent the terms that each document
     * have in common and their combined weights.
     *
     * @param dvi the document to compare this one to
     * @return a sorted set of WeightedFeature that occurred in both documents
     */
    public SortedSet getSimilarityTerms(DocumentVectorImpl dvi) {

        WeightedFeature[] other = dvi.getFeatures();
        getFeatures();

        int i1 = 0;
        int i2 = 0;
        int x = 0;

        SortedSet s =
                new TreeSet(WeightedFeature.getInverseWeightComparator());
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

        return s;
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
     * Gets the euclidean length of this vector.
     */
    public float length() {
        return length;
    }

    /**
     * Gets a sorted set of features.
     *
     * @return a set of the features in this vector, sorted by name
     */
    public SortedSet getSet() {
        getFeatures();
        SortedSet ret = new TreeSet();
        for(int i = 0; i < v.length; i++) {
            if(v[i].getWeight() != 0) {
                ret.add(v[i]);
            }
        }
        return ret;
    }

    public SortedSet getWeightOrderedSet() {
        getFeatures();
        SortedSet ret =
                new TreeSet(WeightedFeature.getInverseWeightComparator());
        for(int i = 0; i < v.length; i++) {
            if(v[i].getWeight() != 0) {
                ret.add(v[i]);
            }
        }
        return ret;
    }

    /**
     * Computes the similarity between this document vector and the
     * supplied vector.  The larger the value, the greater the similarity.
     * The measurement returned is the cosine of the angle between the
     * vectors.
     *
     * @param otherVector the vector representing the document to compare
     * this vector to
     * @return the cosine of the angle between the two vectors
     */
    public float getSimilarity(DocumentVector otherVector) {
        return getSimilarity((DocumentVectorImpl) otherVector);
    }

    public float getSimilarity(DocumentVectorImpl otherVector) {
        return dot(otherVector);
    }

    /**
     * Finds similar documents to this one.  An OR is run with all the terms
     * in the documents.  The resulting docs are returned ordered from
     * most similar to least similar.
     *
     * @return documents similar to the one this vector represents
     */
    public ResultSet findSimilar() {
        return findSimilar("-score");
    }

    public ResultSet findSimilar(String sortOrder) {
        return findSimilar(sortOrder, 1.0);
    }

    /**
     * Finds similar documents to this one.  An OR is run with all the terms
     * in the documents.  The resulting docs are returned ordered from
     * most similar to least similar.
     *
     * @param sortOrder a string describing the order in which to sort the results
     * @param skimPercent a number between 0 and 1 representing what percent of the features should be used to perform findSimilar
     * @return documents similar to the one this vector represents
     */
    public ResultSet findSimilar(String sortOrder, double skimPercent) {

        getFeatures();
        qs.queryW.start();

        //
        // How many features will we actually consider here?
        WeightedFeature[] sf;
        if((skimPercent < 1) && (v.length > 10)) {
            int nf = (int) Math.floor(skimPercent * v.length);
            PriorityQueue<WeightedFeature> wfq =
                    new PriorityQueue<WeightedFeature>(nf,
                                                       WeightedFeature.
                    getInverseWeightComparator());
            for(WeightedFeature twf : v) {
                if(wfq.size() < nf) {
                    wfq.offer(twf);
                } else {
                    if(wfq.peek().getWeight() < twf.getWeight()) {
                        wfq.remove();
                        wfq.offer(twf);
                    }
                }
            }

            sf = wfq.toArray(new WeightedFeature[0]);
            Util.sort(sf);
        } else {
            sf = v;
        }

        //
        // We now have sf, which is the (possibly skimmed) set of features
        // that we want to use for finding similar documents.  Let's go ahead
        // and find them!
        //
        // Step through each partition and look up the terms
        // in the weighted feature vector.  Add the postings for each
        // term in to the QuickOr for that partition, and keep track of
        // the scored groups generated for each one.
        List<ArrayGroup> groups = new ArrayList<ArrayGroup>();

        //
        // Iterate through the partitions, looking for the features.
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures(wf, wc);
        feat.setQueryStats(qs);
        for(DiskPartition dp : e.getManager().getActivePartitions()) {

            InvFileDiskPartition curr = (InvFileDiskPartition) dp;

            if(curr.isClosed()) {
                continue;
            }

            DiskField cdf = curr.getDF(df.getInfo());

            if(cdf == null) {
                continue;
            }


            ScoredQuickOr qor = new ScoredQuickOr(curr, 1024, true);
            qor.setQueryStats(qs);

            for(WeightedFeature f : sf) {

                QueryEntry entry = cdf.getTerm(f.getName(), false);
                if(entry != null) {
                    wf.initTerm(wc.setTerm(f.getName()));
                }

                PostingsIterator pi = entry.iterator(feat);

                if(pi != null) {
                    //
                    // If we got an entry in this partition, add its postings
                    // to the quick or.
                    qor.add(pi, f.getWeight());
                } else {
                    qor.addWeightOnly(f.getWeight());
                }
            }

            //
            // Add the results for this partition into the list
            // of results.
            ScoredGroup sg = (ScoredGroup) qor.getGroup();
            qs.normW.start();
            if(fields == null) {
                sg.normalize();
            } else {
                for(int i = 0; i < fields.length; i++) {
                    if(fields[i] == 1) {
                        sg.normalize(i);
                        break;
                    }
                }
            }
            qs.normW.stop();
            sg.removeDeleted();
            groups.add(sg);
        }
        qs.queryW.stop();
        ((SearchEngineImpl) e).addQueryStats(qs);

        ResultSetImpl ret = new ResultSetImpl(e, sortOrder, groups);
        ret.setQueryStats(qs);
        return ret;
    }

    // Doc inherited from interface.  Gets a HashMap of the top N terms, or
    // docLength terms if docLength < N.  The map is from String term names
    // to Float weights.
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
        Util.sort(w, WeightedFeature.getInverseWeightComparator());
        Map<String, Float> m = new LinkedHashMap();
        nTerms = Math.min(nTerms, w.length);
        for(int i = 0; i < nTerms; i++) {
            if(ignoreWords == null || !ignoreWords.isStop(w[i].getName())) {
                m.put(w[i].getName(), w[i].getWeight());
            }
        }
        return m;
    }

    public String getKey() {
        return keyName;
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

    public void setField(String field) {
        this.field = field;

        //
        // If we don't have a search engine (possible if we're a subclass or we
        // were sent over RMI), then we have to defer figuring out the field ID
        // until setEngine gets called.
        if(e == null) {
            return;
        }

        df = ((InvFileDiskPartition) key.getPartition()).getDF(field);
    }
}
