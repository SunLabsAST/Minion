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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * A class that holds a weighted document vector for a given document from a
 * given partition. This implementation is meant to handle features from either
 * the entire document or a single vectored field.
 *
 * @see CompositeDocumentVectorImpl for an implementation that can handle
 * features from multiple vectored fields.
 */
public class MultiFieldDocumentVector extends AbstractDocumentVector {

    private static final Logger logger =
            Logger.getLogger(MultiFieldDocumentVector.class.getName());

    /**
     * We'll need to send this along when serializing as we're doing our own
     * serialization via the
     * <code>Externalizable</code> interface.
     */
    public static final long serialVersionUID = 2L;

    /**
     * The fields from which this vector was generated.
     */
    protected FieldInfo[] fields;

    public MultiFieldDocumentVector() {
    }

    /**
     * Creates a document vector with a set of precomputed features.
     *
     * @param e the engine that we'll use for similarity computations
     * @param basisFeatures the features to use for the vector.
     */
    public MultiFieldDocumentVector(SearchEngine e,
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
     * Creates a document vector from a search result, using the default fields
     * defined in the query configuration associated with the engine that
     * generated the result.
     *
     * @param r The search result for which we want a document vector.
     */
    public MultiFieldDocumentVector(ResultImpl r) {
        this(r.set.getEngine(), r.getKey(), null);
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
    public MultiFieldDocumentVector(ResultImpl r, String field) {
        this(r.set.getEngine(), r.getKey(),
             new FieldInfo[]{r.set.getEngine().getFieldInfo(field)});
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
    public MultiFieldDocumentVector(SearchEngine e,
                                    String key,
                                    FieldInfo[] fields) {
        this(e, key, fields, e.getQueryConfig().getWeightingFunction(),
             e.getQueryConfig().getWeightingComponents());
    }

    public MultiFieldDocumentVector(SearchEngine e,
                                    String key,
                                    FieldInfo[] fields,
                                    WeightingFunction wf,
                                    WeightingComponents wc) {

        this.e = e;
        this.key = key;
        if(fields == null) {
            Set<FieldInfo> deff = e.getQueryConfig().getDefaultFields();
            if(deff == null || deff.isEmpty()) {
                throw new IllegalArgumentException(
                        "Must either define default fields or pass fields in!");
            }
            this.fields = deff.toArray(new FieldInfo[deff.size()]);
        } else {
            this.fields = fields;
        }
        this.wf = wf;
        this.wc = wc;
        initFeatures();
    }

    @Override
    public DocumentVector copy() {
        MultiFieldDocumentVector ret = new MultiFieldDocumentVector();
        ret.e = e;
        ret.key = key;
        ret.keyEntry = keyEntry;
        ret.fields = fields;
        ret.wf = wf;
        ret.wc = wc;
        ret.ignoreWords = ignoreWords;
        ret.v = v != null ? v.clone() : null;
        return ret;
    }

    /**
     * A container class for data that we find looking up features.
     */
    private static class LocalTermStats {

        int freq;

        TermStatsImpl ts;

        public LocalTermStats(String name) {
            ts = new TermStatsImpl(name);
        }

        public void add(int freq, TermStatsImpl ts) {
            this.freq += freq;
            this.ts.add(ts);
        }
    }

    /**
     * Builds the features for the feature vector.
     */
    private void initFeatures() {

        Map<String, LocalTermStats> tm = new HashMap<String, LocalTermStats>();

        for(FieldInfo fi : fields) {

            if(!fi.hasAttribute(FieldInfo.Attribute.VECTORED)) {
                logger.warning(String.format(
                        "Can't get vector for %s for unvectored field %s", key,
                                             fi.getName()));
            }

            //
            // Get the data for the field in the partition containing this key.
            DiskField df = e.getPM().getField(key, fi);
            if(df == null) {
                continue;
            }

            //
            // Get the dictionary from which we'll draw the vector and the one
            // from which we'll draw terms, then look up the document.
            DiskDictionary vecDict;
            DiskDictionary termDict;
            if(df.isStemmed()) {
                vecDict = df.getDictionary(
                        MemoryDictionaryBundle.Type.STEMMED_VECTOR);
                termDict = df.getDictionary(
                        MemoryDictionaryBundle.Type.STEMMED_TOKENS);
            } else {
                vecDict = df.getDictionary(
                        MemoryDictionaryBundle.Type.RAW_VECTOR);
                termDict = df.getDictionary(
                        MemoryDictionaryBundle.Type.CASED_TOKENS);
            }

            QueryEntry<String> vecEntry = vecDict.get(key);
            if(vecEntry == null) {
                logger.warning(String.format(
                        "No vector for %s in %s? That shouldn't have happened",
                                             key, fi.getName()));
                continue;
            }

            //
            // Now iterate through the term IDs, looking them up as we go and
            // accumulating the term statistics.
            PostingsIterator pi = vecEntry.iterator(null);
            if(pi == null) {
                logger.warning(String.format("No postings for %s in %s", key,
                                             fi.getName()));
                continue;
            }
            while(pi.next()) {
                QueryEntry<String> termEntry = termDict.getByID(pi.getID());
                if(termEntry == null) {
                    logger.warning(String.format(
                            "Tried to get term %d in %s for %s, but failed?",
                                                 pi.getID(), key, fi.getName()));
                    continue;
                }
                //
                // Accumulate the frequency and term stats for this entry.
                TermStatsImpl tsi = (TermStatsImpl) e.getTermStats(termEntry.
                        getName(), fi);
                LocalTermStats lts = tm.get(termEntry.getName());
                if(lts == null) {
                    lts = new LocalTermStats(termEntry.getName());
                    tm.put(termEntry.getName(), lts);
                }
                lts.add(pi.getFreq(), tsi);
            }
        }


        //
        // Make the actual feature vector, computing the vector length as we go.
        v = new WeightedFeature[tm.size()];
        int p = 0;
        length = 0;
        for(Map.Entry<String, LocalTermStats> ent : tm.entrySet()) {
            //
            // Set up for weighting.
            wc.setTerm(ent.getValue().ts);
            wc.fdt = ent.getValue().freq;
            wf.initTerm(wc);
            v[p] = new WeightedFeature(ent.getKey(), wf.termWeight(wc));
            length += (v[p].getWeight() * v[p].getWeight());
        }

        length = (float) Math.sqrt(length);

        //
        // Sort by name!
        Util.sort(v, WeightedFeature.NAME_COMPARATOR);
    }

    @Override
    public WeightedFeature[] getFeatures() {
        if(v == null) {
            initFeatures();
        }
        return v;
    }

    /**
     * Calculates the dot product of this document vector with another.
     *
     * @param dvi another document vector
     * @return the dot product of the two vectors (i.e. the sum of the products
     * of the components in each dimension)
     */
    public float dot(MultiFieldDocumentVector dvi) {
        dvi.getFeatures();
        getFeatures();
        return dot(dvi.v);
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
        if(!(dv instanceof MultiFieldDocumentVector)) {
            return false;
        }
        MultiFieldDocumentVector other = (MultiFieldDocumentVector) dv;

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
                    new PriorityQueue<WeightedFeature>(
                    nf, WeightedFeature.INVERSE_WEIGHT_COMPARATOR);
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

            int[] freqs = new int[dp.getMaxDocumentID()];
            //
            // OK, here's how it's going to go.  Since we need to meld together
            // postings from separate fields, we're going to want to work in a
            // frequency domain until we've collected all the data for a particular 
            // term and then we can transform it into weighted term data.
            //
            // We'll use a heap of postings iterators whose elements are the 
            // per-field term iterators so that we can gather the per-doc
            // frequency information.
            PriorityQueue<PostingsIterator> ph =
                    new PriorityQueue<PostingsIterator>(fields.length);
            for(WeightedFeature f : sf) {

                //
                // Collect the iterators and term stats.
                TermStatsImpl tsi = new TermStatsImpl(f.getName());
                ph.clear();
                for(FieldInfo field : fields) {
                    DiskField cdf = curr.getDF(field);
                    if(cdf == null) {
                        continue;
                    }
                    QueryEntry entry = cdf.getTerm(f.getName(), false);
                    if(entry == null) {
                        continue;
                    }
                    tsi.add((TermStatsImpl) e.getTermStats(f.getName(), field));
                    PostingsIterator pi = entry.iterator(null);
                    if(pi == null) {
                        continue;
                    }
                    pi.next();
                    ph.offer(pi);
                }

                //
                // Collect the frequencies.
                while(!ph.isEmpty()) {
                    PostingsIterator pi = ph.poll();
                    freqs[pi.getID()] += pi.getFreq();
                    if(pi.next()) {
                        ph.offer(pi);
                    }
                }

                //
                // Compute weights.
                wc.setTerm(tsi);
                wf.initTerm(wc);
                for(int i = 0; i < freqs.length; i++) {
                    if(freqs[i] == 0) {
                        continue;
                    }
                }
            }
        }
        qs.queryW.stop();
        ((SearchEngineImpl) e).addQueryStats(qs);

        ResultSetImpl ret = new ResultSetImpl(e, sortOrder, groups);
        ret.setQueryStats(qs);
        return ret;
    }
}
