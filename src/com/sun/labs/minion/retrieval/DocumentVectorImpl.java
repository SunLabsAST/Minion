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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
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
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.LightIterator;
import com.sun.labs.minion.indexer.entry.FieldedDocKeyEntry;
import com.sun.labs.minion.indexer.entry.TermStatsEntry;
import com.sun.labs.minion.pipeline.StopWords;
import com.sun.labs.minion.retrieval.cache.TermCache;
import com.sun.labs.minion.retrieval.cache.TermCacheElement;
import com.sun.labs.minion.util.Util;
import java.io.Externalizable;
import java.util.logging.Logger;

/**
 * A class that holds a weighted document vector for a given document from
 * a given partition.  This implementation is meant to handle features from
 * either the entire document or a single vectored field.
 * 
 * @see CompositeDocumentVectorImpl for an implementation that can handle 
 * features from multiple vectored fields.
 */
public class DocumentVectorImpl implements DocumentVector, Externalizable {

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
    protected transient DocKeyEntry key;

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

    private static Logger logger = Logger.getLogger(DocumentVectorImpl.class.getName());

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
        this(r.set.getEngine(), r.ag.part.getDocumentTerm(r.doc), null);
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
        this(r.set.getEngine(), r.ag.part.getDocumentTerm(r.doc), field);
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
            DocKeyEntry key, String field) {
        this(e, key, field, e.getQueryConfig().getWeightingFunction(),
                e.getQueryConfig().getWeightingComponents());
    }

    public DocumentVectorImpl(SearchEngine e,
            DocKeyEntry key, String field,
            WeightingFunction wf,
            WeightingComponents wc) {

        this.e = e;
        this.key = key;
        keyName = key.getName().toString();
        setField(field);
        this.wf = wf;
        this.wc = wc.setDocument(this.key, field);
        length = wc.dvl;
        initFeatures();
    }

    public DocumentVector copy() {
        DocumentVectorImpl ret = new DocumentVectorImpl();
        ret.e = e;
        ret.key = key;
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

    public DocKeyEntry getEntry() {
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

            if(f1.getName().equals(f2.getName()) &&
                    f1.getWeight() == f2.getWeight()) {
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
    public Map<String, Float> getSimilarityTerms(DocumentVector dv) {
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
                    WeightedFeature wf = new WeightedFeature(f1.getName(), combined);
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

        //
        // If the number of terms that we'll consider is a substantial portion of
        // the number of terms in the dictionary, then we're going to do things
        // a bit differently.
        if(v == null && skimPercent == 1 &&
                key.getN() >=
                0.1 * key.getPartition().getManager().getNTerms()) {
            return bigFindSimilar(sortOrder, skimPercent);
        }

        //
        // Get the features if we need them
        getFeatures();

        qs.queryW.start();

        //
        // OK, we can do things the usual way:  process the document vector
        // postings, build up the weighted features, and then process the
        // postings.
        //
        // How many features will we actually consider here?
        WeightedFeature[] sf;
        if((skimPercent < 1) && (v.length > 10)) {
            int nf = (int) Math.floor(skimPercent * v.length);
            PriorityQueue<WeightedFeature> wfq =
                    new PriorityQueue<WeightedFeature>(nf,
                    WeightedFeature.getInverseWeightComparator());
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
        DiskPartition part = null;
        if(key != null) {
            part = (DiskPartition) key.getPartition();
        }
        PostingsIteratorFeatures feat =
                new PostingsIteratorFeatures(wf, wc);
        feat.setFields(fields);
        feat.setQueryStats(qs);
        for(DiskPartition curr : e.getManager().getActivePartitions()) {

            DictionaryIterator di = curr.getMainDictionaryIterator();
            ScoredQuickOr qor = new ScoredQuickOr(curr, 1024);
            qor.setQueryStats(qs);
            qor.setField(fieldID);

            TermCache termCache = curr.getTermCache();

            for(WeightedFeature f : sf) {
                //
                // Do things by ID for the partition that the document vector
                // was drawn from!
                QueryEntry entry =
                        part == curr ? f.getEntry() : di.get(f.getName());
                if(entry != null) {

                    PostingsIterator pi;
                    if(termCache != null) {
                        TermCacheElement el = termCache.get(f.getName(), feat);
                        pi = el.iterator();
                    } else {
                        wf.initTerm(wc.setTerm(f.getName()));
                        pi = entry.iterator(feat);
                    }

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

        ResultSetImpl ret =
                new ResultSetImpl(e, sortOrder, groups);
        ret.setQueryStats(qs);
        return ret;
    }

    /**
     * Builds the features for the feature vector.
     */
    private void initFeatures() {
        if(key instanceof FieldedDocKeyEntry) {
            v = ((FieldedDocKeyEntry) key).getWeightedFeatures(fieldID, wf, wc);
        } else {
            v = key.getWeightedFeatures(wf, wc);
        }
    }

    /**
     * Computes the similar documents from scratch, using iteration rather than
     * lookups.
     */
    private ResultSet bigFindSimilar(String sortOrder, double skimPercent) {

        //
        // Get an iterator for the terms in the document.
        PostingsIteratorFeatures feat =
                new PostingsIteratorFeatures(wf, wc);
        feat.setFields(fields);
        feat.setQueryStats(qs);
        qs.queryW.start();
        PostingsIterator pi = key.iterator(feat);
        DiskPartition part =
                (DiskPartition) key.getPartition();

        //
        // Get an iterator for the term stats dictionary.
        DictionaryIterator tsi = part.getManager().getTermStatsDict().iterator();

        //
        // Handle the partition that the document was drawn from first.  We'll
        // iterate through the main dictionary for this one too.
        LightIterator mdi = part.getMainDictionary().literator();

        //
        // As we're going through this partition, we'll compute the weighted
        // features, which we'll use for the other partitions.
        List<WeightedFeature> fl =
                new ArrayList<WeightedFeature>(key.getN());

        //
        // The groups for the result set.
        List<ScoredGroup> groups =
                new ArrayList<ScoredGroup>();
        ScoredQuickOr qor =
                new ScoredQuickOr(part, part.getNDocs());

        while(pi.next()) {
            int termID = pi.getID();

            //
            // Get to the main dictionary iterator for this term.
            while(mdi.next()) {
                if(mdi.getID() == termID) {
                    break;
                }
            }

            QueryEntry mde = mdi.getEntry();

            //
            // Get to the term stats for this term.
            String name = mde.getName().toString();
            TermStatsEntry tse = null;
            while(tsi.hasNext()) {
                tse = (TermStatsEntry) tsi.next();
                if(tse.getName().equals(name)) {
                    break;
                }
            }

            //
            // Make a weighted feature for this term.
            wc.setTerm(tse.getTermStats()).setDocument(pi);
            wf.initTerm(wc);
            WeightedFeature twf =
                    new WeightedFeature(name, pi.getID(),
                    wf.termWeight(wc) / wc.dvl);
            twf.setFreq(pi.getFreq());
            fl.add(twf);
            qor.add(mde.iterator(feat), twf.getWeight());
        }

        groups.add((ScoredGroup) qor.getGroup());

        //
        // Get our array of features, while we're here.
        v = fl.toArray(new WeightedFeature[0]);

        //
        // Now handle the rest of the partitions.
        for(DiskPartition curr : e.getManager().getActivePartitions()) {

            if(curr == part) {
                continue;
            }

            qor = new ScoredQuickOr(curr, curr.getNDocs());
            qor.setField(fieldID);

            //
            // We're going to iterate through the other dictionaries as
            // well.
            mdi = curr.getMainDictionary().literator();
            if(!mdi.next()) {
                continue;
            }

            tsi = part.getManager().getTermStatsDict().iterator();
            for(WeightedFeature f : v) {

                String mdin = mdi.getName().toString();

                while(mdin.compareTo(f.getName()) < 0) {
                    if(mdi.next()) {
                        mdin = mdi.getName().toString();
                    } else {
                        break;
                    }
                }

                if(mdin.equals(f.getName())) {

                    //
                    // Get the term stats.
                    TermStatsEntry tse = null;
                    while(tsi.hasNext()) {
                        tse = (TermStatsEntry) tsi.next();
                        if(tse.getName().equals(f.getName())) {
                            break;
                        }
                    }

                    QueryEntry mde = mdi.getEntry();
                    wf.initTerm(wc.setTerm(tse.getTermStats()));

                    //
                    // If we got an entry in this partition, add its postings
                    // to the quick or.
                    qor.add(mde.iterator(feat), f.getWeight());
                } else {
                    qor.addWeightOnly(f.getWeight());
                }
            }

            //
            // Add the results for this partition into the list
            // of results.
            ScoredGroup sg = (ScoredGroup) qor.getGroup();
            groups.add(sg);
        }

        for(ScoredGroup sg : groups) {
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
            sg.removeDeleted();
        }
        qs.queryW.stop();
        ((SearchEngineImpl) e).addQueryStats(qs);

        ResultSetImpl ret =
                new ResultSetImpl(e, sortOrder, groups);
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

        //
        // Figure out the ID of the field that we're building a vector for.  If
        // the field value is null, we won't do any field restriction, i.e.,
        // we'll use the entire body of the document.  If the field is the empty
        // string, then we'll use the data that doesn't occur in any field in
        // the document (aka "the body").  If the field value isn't null and isn't
        // empty, we'll treat it as the name of a (hopefully vectored) field.
        //
        // We're going to need the field ID in order to fetch the correct weight
        // from the postings iterator when we're iterating through them, and to
        // do normalization correctly when findSimilar is called.
        fieldID = -1;
        if(field != null) {
            if(field.equals("")) {
                //
                // The no-field field.
                fields = new int[]{1};
                fieldID = 0;
            } else {
                fields = ((SearchEngineImpl) e).getManager().getMetaFile().
                        getFieldArray(field);
                for(int i = 0; i < fields.length; i++) {
                    if(fields[i] == 1) {
                        fieldID = i;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Writes the object to the provided output using a gzipped output stream to
     * save space (the resulting serialized data is 2-3 times smaller than it is
     * with the normal serialization approach.)  Changes to this method will require
     * a change to the serial version and will require modifications to the
     * <code>readExternal</code> method to reflect the changes.
     * @param out the output where the
     * @throws java.io.IOException if there is an error writing the object
     * @see #serialVersionUID
     * @see #readExternal(java.io.ObjectInput)
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(serialVersionUID);
        out.writeBoolean(keyName != null);
        if (keyName != null) {
            out.writeUTF(keyName);
        }
        out.writeBoolean(normalized);
        out.writeBoolean(field != null);
        if (field != null) {
            out.writeUTF(field);
        }
        out.writeInt(v.length);
        for (WeightedFeature f : v) {
            out.writeUTF(f.getName());
            out.writeFloat(f.getWeight());
        }
    }

    /**
     * Reads the vector from the provided input.  This method reads a serial version
     * first.
     * @param in the input from which the vector will be read.
     * @throws java.io.IOException if there is an error reading from the input
     * or if the object being read has a different version than the one for this
     * class
     * @throws java.lang.ClassNotFoundException if there is an error instantiating
     * the vector
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        long svuid = in.readLong();
        if(svuid != serialVersionUID) {
            throw new java.io.InvalidClassException(String.format("Read class version %d looking for %d", svuid, serialVersionUID));
        }
        if (in.readBoolean()) {
            keyName = in.readUTF();
        }
        normalized = in.readBoolean();
        if (in.readBoolean()) {
            field = in.readUTF();
        }
        int l = in.readInt();
        v = new WeightedFeature[l];
        for(int i = 0; i < v.length; i++) {
            String n = in.readUTF();
            float w = in.readFloat();
            v[i] = new WeightedFeature(n, w);
        }
    }
}
