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
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.WeightedField;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Set;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.indexer.MetaFile;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.entry.FieldedDocKeyEntry;
import com.sun.labs.minion.indexer.postings.FieldedPostingsIterator;
import com.sun.labs.minion.pipeline.StopWords;
import com.sun.labs.minion.util.StopWatch;
import java.util.logging.Logger;

/**
 * An implementation of document vector that provides for a composite document
 * vector, that is, a document vector made by taking a linear combination of 
 * more than one vectored field.
 * 
 * @see DocumentVectorImpl for an implementation that uses features from the
 * whole document or from just one vectored field.
 */
public class CompositeDocumentVectorImpl implements DocumentVector {

    /**
     * The search engine that generated this vector.
     */
    protected transient SearchEngine e;

    /**
     * The document key for this entry.
     */
    private transient DocKeyEntry key;

    /**
     * The name of the key, which will survive transport.
     */
    private String keyName;

    /**
     * A linear combination of the fields composing this vector.
     */
    protected WeightedField[] fields;

    /**
     * The field from which this document vector was generated.  A 1 in position
     * i indicates that the field with ID i is used for this vector.  Element
     * zero is reserved for the unnammed body field.  If this array is null, then
     * the document vector is built from all of the vectored fields in this 
     * document.
     */
    private transient int[] fieldIDs;

    /**
     * The weights for the fields from which this vector was generated.  The weight
     * for a field whose ID is i is in the ith position in this vector.
     */
    private transient float[] fieldWeights;

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
     * The per-field weighted features for the document.
     */
    protected WeightedFeature[][] fieldFeatures;

    /**
     * The per-field lengths of the vectors.
     */
    protected float[] fieldLengths;

    /**
     * Whether we've had our features initialized.
     */
    protected boolean initialized = false;

    /**
     * Whether we've been normalized.
     */
    protected boolean normalized = false;

    Logger logger = Logger.getLogger(getClass().getName());

    protected static String logTag = "CDVI";

    protected transient StopWords ignoreWords;

    protected CompositeDocumentVectorImpl() {
    }

    /**
     * Creates a document vector for a particular field from a search result.
     *
     * @param r The search result for which we want a document vector.
     * @param fields a linear combination of fields and weights that should be
     * used to build this document vector.  The field names provided in the array should
     * be the names of vectored fields.  If a provided field name does not name
     * a vectored field, a warning will be logged, but the operation will proceed.
     * 
     * <P>If this paramater contains a weighted field whose name is <code>null</code>,
     * that indicates that the data from the unnamed body field should be used with
     * the associated weight.
     * 
     * <P>It is probably a good idea if the weights associated with the fields
     * sum to 1, although it is not required.  If the weights do not sum to one, 
     * then you may get document similarities greater than 1 as the result of a 
     * <code>findSimilar<code> operation.
     */
    public CompositeDocumentVectorImpl(ResultImpl r, WeightedField[] fields) {
        this(r.set.getEngine(), r.ag.part.getDocumentTerm(r.doc), fields);
    }

    /**
     * Creates a document vector for a given document.
     *
     * @param e The search engine with which the docuemnt is associated.
     * @param key The entry from the document dictionary for the given
     * document.
     * @param fields a linear combination of the vectored fields in this document
     * that we will use to build the document vector.
     * If this value is <code>null</code> a vector for the whole document will
     * be returned.  If one of the values in a non-<code>null</code> array has 
     * the name of the field set to <code>null</code>, then the vector will include
     * data from the unnamed body field. If one of the fields provided is not
     * a vectored field, then a warning will be issued, but processing will
     * proceed.
     */
    public CompositeDocumentVectorImpl(SearchEngine e,
            DocKeyEntry key, WeightedField[] fields) {
        this(e, key, fields, e.getQueryConfig().getWeightingFunction(),
                e.getQueryConfig().getWeightingComponents());
    }

    public CompositeDocumentVectorImpl(SearchEngine e,
            DocKeyEntry key, WeightedField[] fields,
            WeightingFunction wf,
            WeightingComponents wc) {

        this.e = e;
        this.key = key;
        keyName = key.getName().toString();
        this.wf = wf;
        this.wc = wc;

        setFields(fields);
        initFeatures();
    }

    public CompositeDocumentVectorImpl(SearchEngine e,
            WeightedFeature[] wf, WeightedField[] fields) {
        this.e = e;
        this.key = null;
        keyName = null;
        setFields(fields);
        for(int i = 0; i < fieldFeatures.length; i++) {
            if(this.fields[i] != null) {
                fieldFeatures[i] = wf.clone();
            }
        }
        initialized = true;
    }

    public DocumentVector copy() {
        CompositeDocumentVectorImpl ret = new CompositeDocumentVectorImpl();
        ret.e = e;
        ret.key = key;
        ret.keyName = keyName;
        ret.wf = wf;
        ret.wc = wc;
        ret.ignoreWords = ignoreWords;
        ret.initialized = initialized;
        ret.fieldIDs = fieldIDs.clone();
        ret.fieldLengths = fieldLengths.clone();
        ret.fieldWeights = fieldLengths.clone();
        ret.fields = fields.clone();
        ret.fieldFeatures = new WeightedFeature[fieldFeatures.length][];
        for(int i = 0; i < fieldFeatures.length; i++) {
            if(fieldFeatures[i] != null) {
                ret.fieldFeatures[i] = fieldFeatures[i].clone();
            }
        }
        return ret;
    }

    private void setFields(WeightedField[] fields) {

        //
        // Get the number of fields in the engine, so that we can do arrays that
        // are the same length.
        MetaFile mf = ((SearchEngineImpl) e).getManager().getMetaFile();
        int nf = mf.size();

        //
        // We're good, no remapping needed.
        fieldIDs = new int[nf + 1];
        fieldWeights = new float[nf + 1];
        fieldLengths = new float[nf + 1];
        fieldFeatures = new WeightedFeature[nf + 1][];
        this.fields = new WeightedField[nf + 1];
        for(WeightedField field : fields) {

            //
            // We could have been called from setEngine, which means that 
            // we're using another vector's fields array, which means that 
            // some of the fields might be null.
            if(field == null) {
                continue;
            }
            int fid = getFieldID(field, mf);
            this.fields[fid] = field;
            fieldIDs[fid] = 1;
            fieldWeights[fid] = field.getWeight();
            fieldLengths[fid] = key.getDocumentVectorLength(fid);
        }
    }

    private void remapFields() {
        MetaFile mf = ((SearchEngineImpl) e).getManager().getMetaFile();
        int nf = mf.size();
        //
        // We need to re-arrange.  Lets figure out the mapping of old to new
        // field IDs.
        int[] map = new int[fieldIDs.length];
        for(int i = 0; i < fields.length; i++) {
            if(fields[i] == null) {
                continue;
            }
            int fid = getFieldID(fields[i], mf);
            //
            // Field i in the original engine, maps to fid in this engine.
            map[i] = fid;
        }

        int[] tfid = new int[nf + 1];
        float[] tfw = new float[nf + 1];
        float[] tfl = new float[nf + 1];
        WeightedFeature[][] tff = new WeightedFeature[nf + 1][];
        WeightedField[] tf = new WeightedField[nf + 1];

        //
        // OK, map old into new.
        for(int i = 0; i < fields.length; i++) {
            if(fields[i] == null) {
                continue;
            }
            int mid = map[i];
            int fid = getFieldID(fields[i], mf);
            tfid[fid] = fieldIDs[mid];
            tfw[fid] = fieldWeights[mid];
            tfl[fid] = fieldLengths[mid];
            tff[fid] = fieldFeatures[mid];
            tf[fid] = fields[i];
        }
        fieldIDs = tfid;
        fieldWeights = tfw;
        fieldLengths = tfl;
        fieldFeatures = tff;
        fields = tf;
    }

    private int getFieldID(WeightedField field, MetaFile mf) {
        int fid;
        if(field.getFieldName() == null) {
            fid = 0;
        } else {
            FieldInfo fi = mf.getFieldInfo(field.getFieldName());
            if(!fi.isVectored()) {
                logger.warning("Non vectored field: " + fi.getName() +
                        " for composite document vector");
            }
            fid = fi.getID();
        }
        return fid;
    }

    public DocKeyEntry getEntry() {
        return key;
    }

    public SearchEngine getEngine() {
        return e;
    }

    public void setEngine(SearchEngine e) {
        this.e = e;
        QueryConfig qc = e.getQueryConfig();
        wf = qc.getWeightingFunction();
        wc = qc.getWeightingComponents();
        ignoreWords = qc.getVectorZeroWords();
        remapFields();
    }

    /**
     * Calculates the dot product of this document vector with another.  Because
     * a composite document vector is composed of multiple fields with associated 
     * weights, the dot product of two vectors will take these fields and weights 
     * into account.
     * 
     * <p>When the other document vector contains a field that this one does not, 
     * (or vice versa (note: it's probably not a good idea to compute the dot 
     * product of such vectors!)), then there will be no contribution from that field.  When
     * the two document vectors have a field in common, the of the vectors will
     * be multiplied by any associated field weights before they are multiplied
     * together.
     *
     * @param dvi another document vector
     * @return the dot product of the two vectors (i.e. the sum of the
     * products of the components in each dimension)
     */
    public float dot(CompositeDocumentVectorImpl dvi) {
        initFeatures();
        dvi.initFeatures();
        float ret = 0;

        //
        // Why are we taking a min here? Because a vector generated at a 
        // different time may have a different number of fields defined.  This
        // should be rare, but not impossible.
        int l = Math.min(fieldIDs.length, dvi.fieldIDs.length);
        for(int i = 0; i < l; i++) {
            if(fieldIDs[i] == 1 && dvi.fieldIDs[i] == 1) {
                ret += dot(fieldFeatures[i], dvi.fieldFeatures[i]) *
                        fieldWeights[i] * dvi.fieldWeights[i];
            }
        }
        return ret;
    }

    /**
     * Calculates the dot product of two sets of weighted features. Assumes that
     * the arrays are ordered by the feature names.
     *
     * @param wfv1 a weighted feature vector
     * @param wfv2 another weighted feature vector
     * @return the dot product of the two vectors (i.e. the sum of the
     * products of the components in each dimension)
     */
    public float dot(WeightedFeature[] wfv1, WeightedFeature[] wfv2) {
        if(wfv1 == null || wfv2 == null) {
            return 0;
        }

        float res = 0;
        int i1 = 0;
        int i2 = 0;

        while(i1 < wfv1.length && i2 < wfv2.length) {
            WeightedFeature f1 = wfv1[i1];
            WeightedFeature f2 = wfv2[i2];

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

    public Set<String> getTerms() {
        initFeatures();
        Set<String> ret = new HashSet<String>();
        for(int i = 0; i < fieldFeatures.length; i++) {
            if(fieldFeatures[i] != null) {
                for(WeightedFeature f : fieldFeatures[i]) {
                    ret.add(f.getName());
                }
            }
        }
        return ret;
    }

    /**
     * Gets a map of term names to weights, where the weights
     * represent the amount the term contributed to the similarity
     * of the two documents.  Only terms that occur in both documents
     * are returned, as all other terms have weight zero.  The keys
     * in the Map are sorted according to the natural ordering
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
        SortedSet s = getSimilarityTerms((CompositeDocumentVectorImpl) dv);
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
     * of WeightedFeatures that represent the terms that the documents
     * have in common and their combined weights.
     *
     * @param dvi the document to compare this one to
     * @return a sorted set of WeightedFeature that occurred in both documents
     */
    public SortedSet<WeightedFeature> getSimilarityTerms(
            CompositeDocumentVectorImpl dvi) {

        //
        // OK, first up, get the contribution from each of the fields that we're 
        // composed of.
        WeightedFeature[][] comp = new WeightedFeature[fieldFeatures.length][];
        for(int i = 0; i < fieldFeatures.length; i++) {
            if(fieldFeatures[i] != null) {
                comp[i] = getSimilarityTerms(fieldFeatures[i],
                        dvi.fieldFeatures[i]);
            }
        }

        WeightedFeature[] comb = combine(comp);
        SortedSet<WeightedFeature> ret =
                new TreeSet<WeightedFeature>(WeightedFeature.
                getInverseWeightComparator());
        for(WeightedFeature f : comb) {
            ret.add(f);
        }
        return ret;
    }

    /**
     * Combines a number of weighted feature vectors according to our
     * field weightings
     * @param vecs the vectors to combine.  We're assuming that the dimensionality
     * and the elements of the vector correspond to those for the <code>fieldFeatures</code>
     * array.
     * @return a vector containing the linear combination of the provided vectors.
     */
    private WeightedFeature[] combine(WeightedFeature[][] vecs) {
        //
        // We'll use our good old friend the priority queue.  First, we
        // build the queue.
        PriorityQueue<HE> h = new PriorityQueue<HE>();
        for(int i = 0; i < vecs.length; i++) {
            if(vecs[i] == null) {
                continue;
            }

            HE el = new HE(fields[i], i, vecs[i]);
            if(el.next()) {
                h.offer(el);
            }
        }

        //
        // Now we exercise the queue combining the weights of features with
        // the same name.  This is a lot like a dictionary merge.
        List<WeightedFeature> ret = new ArrayList<WeightedFeature>();
        while(h.size() > 0) {
            HE top = h.peek();
            WeightedFeature mf = new WeightedFeature(top.cf.getName(), 0);
            while(top != null && top.cf.getName().equals(mf.getName())) {
                top = h.poll();
                mf.setWeight(mf.getWeight() + top.field.getWeight() *
                        top.cf.getWeight());
                if(top.next()) {
                    h.offer(top);
                }
                top = h.peek();
            }
            ret.add(mf);
        }

        return ret.toArray(new WeightedFeature[0]);

    }

    /**
     * Gets the contribution to document similarity from a set of features.
     * 
     * @param wfv1 one set of features
     * @param wfv2 another set of features
     */
    private WeightedFeature[] getSimilarityTerms(WeightedFeature[] wf1,
            WeightedFeature[] wf2) {

        //
        // One has a field we don't.  Yuck, but OK.
        if(wf1 == null || wf2 == null) {
            return null;
        }

        List<WeightedFeature> ret = new ArrayList<WeightedFeature>();
        int i1 = 0;
        int i2 = 0;
        int x = 0;

        //
        // Go in order (alphabetically) through the two arrays, finding
        // terms that occur in both.
        while(i1 < wf1.length && i2 < wf2.length) {
            WeightedFeature f1 = wf1[i1];
            WeightedFeature f2 = wf2[i2];

            int cmp = f1.getName().compareTo(f2.getName());

            if(cmp == 0) {
                if(ignoreWords == null || !ignoreWords.isStop(f1.getName())) {
                    //
                    // We found two terms with the same name.
                    ret.add(new WeightedFeature(f1.getName(),
                            f1.getWeight() * f2.getWeight()));
                }
                i1++;
                i2++;
            } else if(cmp < 0) {
                i1++;
            } else {
                i2++;
            }
        }

        return ret.toArray(new WeightedFeature[0]);
    }

    /**
     * Normalizes the length of this vector to 1.  Since this is a composite
     * vector, we normalize each of the composites.  Not sure this actually 
     * makes much sense.
     */
    public void normalize() {
        if(normalized) {
            return;
        }

        initFeatures();
        for(int i = 0; i < fieldFeatures.length; i++) {
            if(fieldFeatures[i] != null) {
                normalize(fieldFeatures[i], fieldLengths[i]);
            }
        }
        normalized = true;
    }

    private void normalize(WeightedFeature[] wf, float l) {
        for(WeightedFeature f : wf) {
            if(l == 0) {
                f.setWeight(0);
            } else {
                f.setWeight(f.getWeight() / l);
            }
        }
    }

    /**
     * Gets a sorted set of features.
     *
     * @return a set of the features in this vector, sorted by name.  If this
     * document vector is composed of multiple fields with associated weights, 
     * these features will take these weights into account.
     */
    public SortedSet<WeightedFeature> getSet() {
        WeightedFeature[] comb = combine(fieldFeatures);
        SortedSet ret = new TreeSet();
        for(WeightedFeature f : comb) {
            ret.add(f);
        }
        return ret;
    }

    public SortedSet<WeightedFeature> getWeightOrderedSet() {
        WeightedFeature[] comb = combine(fieldFeatures);
        SortedSet ret =
                new TreeSet(WeightedFeature.getInverseWeightComparator());
        for(WeightedFeature f : comb) {
            ret.add(f);
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
        return getSimilarity((CompositeDocumentVectorImpl) otherVector);
    }

    public float getSimilarity(CompositeDocumentVectorImpl otherVector) {
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
     * Finds documents that are similar to this one.  An OR is run with all the terms
     * in the documents.  The resulting docs are returned ordered from
     * most similar to least similar.
     *
     * @param sortOrder a string describing the order in which to sort the results
     * @param skimPercent a number between 0 and 1 representing what percent of the features should be used to perform findSimilar
     * @return documents similar to the one this vector represents
     */
    public ResultSet findSimilar(String sortOrder, double skimPercent) {
        StopWatch sw = new StopWatch();
        sw.start();
        initFeatures();

        //
        // First, we're going to build a set of the names of the features that
        // we want to consider for the find similar.  We'll use this when we're 
        // iterating through the feature vectors to determine if we want to 
        // process the postings for that term.
        Set<String> fname;
        if(skimPercent < 1) {
            //
            // Compose the feature vectors and find the most important features
            // in the composed vector.
            Set<WeightedFeature> all = getWeightOrderedSet();
            int nf = (int) Math.floor(all.size() * skimPercent);
            Iterator<WeightedFeature> i = all.iterator();
            fname = new HashSet<String>();
            for(int p = 0; p < nf && i.hasNext(); p++) {
                fname.add(i.next().getName());
            }
        } else {

            //
            // We're doing everything.
            fname = getTerms();
        }

        //
        // Step through each partition and look up the terms
        // from the weighted feature vectors.  Add the postings for each
        // term in to the QuickOrs for that partition, and keep track of
        // the scored groups generated for each one.
        List<ArrayGroup> groups = new ArrayList<ArrayGroup>();
        DiskPartition part = null;
        if(key != null) {
            part = (DiskPartition) key.getPartition();
        }
        PostingsIteratorFeatures feat =
                new PostingsIteratorFeatures(wf, wc);
        feat.setFields(fieldIDs);
        for(DiskPartition curr : e.getManager().getActivePartitions()) {

            if(curr.isClosed()) {
                continue;
            }

            //
            // For each partition, we'll heap the feature vectors so that we 
            // can process the terms in dictionary order.  For each of the fields
            // that this document vector is composed of, we'll keep a set of
            // document scores for this partition, which we'll compose once
            // we've finished processing the terms.
            //
            // Also, we'll use a local dictionary iterator so that we have our
            // own temp buffers running.
            PriorityQueue<HE> h = new PriorityQueue<HE>();
            float[][] scores = new float[fieldFeatures.length][];
            for(int i = 0; i < fieldFeatures.length; i++) {
                if(fieldFeatures[i] != null) {
                    scores[i] = new float[curr.getMaxDocumentID() + 1];
                    HE el = new HE(fields[i], i, fieldFeatures[i]);
                    if(el.next()) {
                        h.offer(el);
                    }
                }
            }

            //
            // A set of weights that we can apply to the weights from the postings.
            // We're setting it to be long enough that we don't have to sort things
            // out too much.
            float[] fweights = new float[fieldFeatures.length];
            //
            // OK, now process the heap.  We can ignore features that are not in
            // our set of feature names.

            while(h.size() > 0) {
                HE top = h.peek();
                WeightedFeature cf = top.cf;

                if(!fname.contains(cf.getName())) {
                    //
                    // Skip this feature.
                    while(top != null && top.cf.getName().equals(cf.getName())) {
                        top = h.poll();
                        if(top.next()) {
                            h.offer(top);
                        }
                        top = h.peek();
                    }
                }

                //
                // Process this feature.
                Arrays.fill(fweights, 0);
                while(top != null && top.cf.getName().equals(cf.getName())) {
                    top = h.poll();
                    fweights[top.fieldID] = top.cf.getWeight();
                    if(top.next()) {
                        h.offer(top);
                    }
                    top = h.peek();
                }

                //
                // Okey doke.  Now we have the weights for this features for 
                // the fields.  Let's go ahead and process the postings.
                //
                // Do things by ID for the partition that the document vector
                // was drawn from!
                QueryEntry entry =
                        part == curr ? cf.getEntry() : curr.getTerm(cf.getName());
                if(entry != null) {
                    wf.initTerm(wc.setTerm(cf.getName()));
                    PostingsIterator pi = entry.iterator(feat);
                    if(pi == null) {
                        continue;
                    }
                    while(pi.next()) {
                        float[] cweights =
                                ((FieldedPostingsIterator) pi).getFieldWeights();
                        for(int i = 0; i < fweights.length; i++) {
                            if(fweights[i] > 0) {
                                scores[i][pi.getID()] += fweights[i] *
                                        cweights[i];
                            }
                        }
                    }
                }
            }

            //
            // Combine the vectors according to our weights.  Note that we 
            // normalize by the field length and the squared query weight
            // before multiplying by the weight, so the resulting group should
            // already be "normalized".
            float[] comb = new float[curr.getMaxDocumentID() + 1];
            for(int i = 0; i < scores.length; i++) {
                if(scores[i] != null) {
                    float fw = fieldWeights[i];
                    float sq = fieldLengths[i];
                    float[] temp = scores[i];
                    for(int j = 0; j < temp.length; j++) {
                        if(temp[j] > 0) {
                            float dvl = curr.getDocumentVectorLength(j, i);
                            if(dvl > 0) {
                                comb[j] += (temp[j] / (dvl * sq)) * fw;
                            }
                        }
                    }
                }
            }

            //
            // Add the results for this partition into the list
            // of results.
            ScoredGroup sg = new ScoredGroup(curr, comb);
            sg.removeDeleted();
            groups.add(sg);
        }

        ResultSetImpl ret =
                new ResultSetImpl(e, sortOrder, groups);
        sw.stop();
        ret.queryTime = sw.getTime();
        return ret;
    }

    /**
     * Builds the features for the feature vector.
     */
    private void initFeatures() {
        if(initialized) {
            return;
        }
        for(int i = 0; i < fieldFeatures.length; i++) {
            if(fields[i] != null) {
                fieldFeatures[i] =
                        ((FieldedDocKeyEntry) key).getWeightedFeatures(i, wf, wc);
            }
        }
        initialized = true;
    }

    // Doc inherited from interface.  Gets a HashMap of the top N terms, or
    // docLength terms if docLength < N.  The map is from String term names
    // to Float weights.
    public Map<String, Float> getTopWeightedTerms(int nTerms) {

        initFeatures();

        Map<String, Float> m = new LinkedHashMap<String, Float>();
        WeightedFeature[] comb = combine(fieldFeatures);

        //
        // If we're getting everything, then just return everything.
        if(nTerms >= comb.length) {
            for(WeightedFeature f : comb) {
                m.put(f.getName(), f.getWeight());
            }
        } else {

            //
            // Figure out the top n.
            Set<WeightedFeature> s =
                    new TreeSet(WeightedFeature.getInverseWeightComparator());
            for(WeightedFeature f : comb) {
                s.add(f);
            }
            Iterator<WeightedFeature> i = s.iterator();
            for(int j = 0; j < nTerms && i.hasNext(); j++) {
                WeightedFeature wf = i.next();
                m.put(wf.getName(), wf.getWeight());
            }
        }

        return m;
    }

    public String getKey() {
        return keyName;
    }

    public String toString() {
        initFeatures();
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(int i = 0; i < fieldFeatures.length; i++) {
            if(fieldFeatures[i] == null) {
                continue;
            }

            if(!first) {
                sb.append("\n");
            }
            first = false;
            sb.append(String.format("field: %s size: %d length: %.3f ",
                    fields[i].getFieldName(),
                    fieldFeatures[i].length, fieldLengths[i]));
            float ss = 0;
            for(int j = 0; j < fieldFeatures[i].length; j++) {
                ss += fieldFeatures[i][j].getWeight() *
                        fieldFeatures[i][j].getWeight();
                sb.append("\n  <");
                sb.append(fieldFeatures[i][j].toString());
                sb.append('>');
            }
            sb.append(String.format("\nss: %.3f len: %.3f", ss, Math.sqrt(ss)));
        }
        return sb.toString();
    }

    /**
     * A class for a heap entry for an array of weighted feature vector
     * so that we can process a composite document vector by terms.
     */
    class HE implements Comparable<HE> {

        WeightedField field;

        int fieldID;

        WeightedFeature[] wf;

        WeightedFeature cf;

        int pos;

        public HE(WeightedField field, int fieldID, WeightedFeature[] wf) {
            this.field = field;
            this.fieldID = fieldID;
            this.wf = wf;
            pos = -1;
        }

        public boolean next() {
            pos++;
            if(pos < wf.length) {
                cf = wf[pos];
            } else {
                cf = null;
            }
            return pos < wf.length;
        }

        public WeightedFeature getFeature() {
            return wf[pos];
        }

        public int compareTo(CompositeDocumentVectorImpl.HE o) {
            return wf[pos].compareTo(o.wf[o.pos]);
        }
    }
}
