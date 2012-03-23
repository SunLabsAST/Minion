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

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.WeightedFeature;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionaryBundle;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.util.Util;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A class that holds a weighted document vector for a given document from a
 * given partition. This implementation is meant to handle features from either
 * the entire document or a single vectored field.
 *
 * @see CompositeDocumentVectorImpl for an implementation that can handle
 * features from multiple vectored fields.
 */
public class SingleFieldDocumentVector extends AbstractDocumentVector implements Serializable {

    private static final Logger logger = Logger.getLogger(SingleFieldDocumentVector.class.getName());

    /**
     * We'll need to send this along when serializing as we're doing our own
     * serialization via the
     * <code>Externalizable</code> interface.
     */
    public static final long serialVersionUID = 2L;

    /**
     * The field from which this document vector was generated.
     */
    protected transient FieldInfo field;

    public SingleFieldDocumentVector() {
    }

    /**
     * Creates a document vector for a particular field from a search result.
     *
     * @param r The search result for which we want a document vector.
     * @param field The name of the field for which we want the document vector.
     * If this value is
     * <code>null</code> a vector for the whole document will be returned. If
     * the named field is not a field that was indexed with the vectored
     * attribute set, the resulting document vector will be empty!
     */
    public SingleFieldDocumentVector(ResultImpl r, String field) {
        this(r.set.getEngine(),
                r.ag.part.getDocumentDictionary().getByID(r.doc), field);
    }

    /**
     * Creates a document vector for a given document.
     *
     * @param e The search engine with which the document is associated.
     * @param key The entry from the document dictionary for the given document.
     * @param field The name of the field for which we want the document vector.
     * If this value is
     * <code>null</code> a vector for the whole document will be returned. If
     * this value is the empty string, then a vector for the text not in any
     * defined field will be returned. If the named field is not a field that
     * was indexed with the vectored attribute set, the resulting document
     * vector will be empty!
     */
    public SingleFieldDocumentVector(SearchEngine e,
            QueryEntry key, 
            String field) {
        this(e, key, field, e.getQueryConfig().getWeightingFunction(),
                e.getQueryConfig().getWeightingComponents());
    }

    public SingleFieldDocumentVector(SearchEngine e,
            QueryEntry key,
            String field,
            WeightingFunction wf,
            WeightingComponents wc) {
        this.e = e;
        this.keyEntry = key;
        this.key = key.getName().toString();
        setField(field);
        this.wf = wf;
        this.wc = wc;
        initFeatures();
    }

    public SingleFieldDocumentVector(SearchEngine e,
            WeightedFeature[] basisFeatures,
            String field) {
        this.e = e;
        this.key = null;
        QueryConfig qc = e.getQueryConfig();
        wf = qc.getWeightingFunction();
        wc = qc.getWeightingComponents();
        v = basisFeatures.clone();

        //
        // Compute the length.
        double ss = 0;
        for(WeightedFeature feat : v) {
            ss += feat.getWeight() * feat.getWeight();
        }
        length = (float) Math.sqrt(ss);
        ignoreWords = qc.getVectorZeroWords();
    }

    /**
     * Builds the features for the feature vector.
     */
    private void initFeatures() {

        //
        // Get the data for the field in the partition containing this key.
        DiskField df = e.getPM().getField(key, field);
        if(df == null) {
            v = new WeightedFeature[0];
        }

        //
        // Get the dictionary from which we'll draw the vector and the one
        // from which we'll draw terms, then look up the document.
        DiskDictionary vecDict;
        DiskDictionary termDict;
        if(df.isStemmed()) {
            vecDict = df.getDictionary(MemoryDictionaryBundle.Type.STEMMED_VECTOR);
            termDict = df.getDictionary(MemoryDictionaryBundle.Type.STEMMED_TOKENS);
        } else {
            vecDict = df.getDictionary(MemoryDictionaryBundle.Type.RAW_VECTOR);
            termDict = df.getDictionary(MemoryDictionaryBundle.Type.CASED_TOKENS);
        }

        QueryEntry vecEntry = vecDict.get(keyEntry.getID());
        if(vecEntry == null) {
            v = new WeightedFeature[0];
            return;
        }

        PostingsIterator pi = vecEntry.iterator(new PostingsIteratorFeatures(wf, wc));
        if(pi == null) {
            v = new WeightedFeature[0];
            return;
        }

        v = new WeightedFeature[pi.getN()];
        int p = 0;
        while(pi.next()) {
            int tid = pi.getID();
            QueryEntry<String> termEntry = termDict.getByID(tid);
            TermStatsImpl tsi = (TermStatsImpl) e.getTermStats(termEntry.getName(), field);
            wc.setTerm(tsi).setDocument(pi);
            wf.initTerm(wc);
            WeightedFeature feat = new WeightedFeature(termEntry, pi.getFreq(), pi.getWeight());
            v[p++] = feat;
        }
    }

    @Override
    public DocumentVector copy() {
        SingleFieldDocumentVector ret = new SingleFieldDocumentVector();
        ret.e = e;
        ret.key = key;
        ret.keyEntry = keyEntry;
        ret.field = field;
        ret.wf = wf;
        ret.wc = wc;
        ret.ignoreWords = ignoreWords;
        ret.v = v != null ? v.clone() : null;
        return ret;
    }

    @Override
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
    @Override
    public void setEngine(SearchEngine e) {
        this.e = e;
        QueryConfig qc = e.getQueryConfig();
        wf = qc.getWeightingFunction();
        wc = qc.getWeightingComponents();
        ignoreWords = qc.getVectorZeroWords();
        qs = new QueryStats();
    }

    public QueryEntry getEntry() {
        return keyEntry;
    }

    @Override
    public SearchEngine getEngine() {
        return e;
    }

    /**
     * Calculates the dot product of this document vector with another.
     *
     * @param dvi another document vector
     * @return the dot product of the two vectors (i.e. the sum of the products
     * of the components in each dimension)
     */
    public float dot(SingleFieldDocumentVector dvi) {
        dvi.getFeatures();
        getFeatures();
        return dot(dvi.v);
    }

    /**
     * Calculates the dot product of this feature vector and another feature
     * vector.
     *
     * @param wfv a weighted feature vector
     * @return the dot product of the two vectors (i.e. the sum of the products
     * of the components in each dimension)
     */
    @Override
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
     * Two document vectors are equal if all their weighted features are equal
     * (in both name and weight)
     *
     * @param dv the document vector to compare this one to
     * @return true if the document vectors have equal weighed features
     */
    @Override
    public boolean equals(Object dv) {
        if(!(dv instanceof SingleFieldDocumentVector)) {
            return false;
        }
        SingleFieldDocumentVector other = (SingleFieldDocumentVector) dv;

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

            if(f1.getName().equals(f2.getName())
                    && f1.getWeight() == f2.getWeight()) {
                i++;
                continue;
            }
            return false;
        }
        return true;
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

    /**
     * Finds similar documents to this one. An OR is run with all the terms in
     * the documents. The resulting docs are returned ordered from most similar
     * to least similar.
     *
     * @param sortOrder a string describing the order in which to sort the
     * results
     * @param skimPercent a number between 0 and 1 representing what percent of
     * the features should be used to perform findSimilar
     * @return documents similar to the one this vector represents
     */
    @Override
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
                    WeightedFeature.INVERSE_WEIGHT_COMPARATOR);
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

            DiskField cdf = curr.getDF(field);

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
            sg.normalize();
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

    private void setField(String field) {
        this.field = e.getFieldInfo(field);
    }
}