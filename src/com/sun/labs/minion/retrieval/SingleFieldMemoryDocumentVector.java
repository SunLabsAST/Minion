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
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.WeightedFeature;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.Field;
import com.sun.labs.minion.indexer.Field.DictionaryType;
import com.sun.labs.minion.indexer.Field.TermStatsType;
import com.sun.labs.minion.indexer.MemoryField;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.postings.DocumentVectorPostings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.util.Util;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Logger;

/**
 * A class that holds a weighted document vector for a given document from a
 * given partition. This implementation is meant to handle features from either
 * the entire document or a single vectored field.
 *
 * @see CompositeDocumentVectorImpl for an implementation that can handle
 * features from multiple vectored fields.
 */
public class SingleFieldMemoryDocumentVector extends AbstractDocumentVector
        implements Serializable {

    private static final Logger logger = Logger.
            getLogger(SingleFieldMemoryDocumentVector.class.getName());

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

    /**
     * The memory field from which this document vector was generated.
     */
    protected transient MemoryField mf;

    /**
     * What kind of term stats we're using.
     */
    Field.TermStatsType termStatsType;

    public SingleFieldMemoryDocumentVector() {
    }

    /**
     * Creates a single fielded document vector from the data stored in an
     * in-memory field.
     */
    public SingleFieldMemoryDocumentVector(MemoryField mf, String key,
                                           SearchEngine engine) {
        this.mf = mf;
        this.key = key;
        this.engine = (SearchEngineImpl) engine;
        this.field = mf.getInfo();
        wf = engine.getQueryConfig().getWeightingFunction();
        wc = engine.getQueryConfig().getWeightingComponents();

        //
        // Get the dictionary from which we'll draw the vector and the one
        // from which we'll draw terms, then look up the document.
        MemoryDictionary<String> vecDict;
        if(mf.isStemmed()) {
            termStatsType = Field.TermStatsType.STEMMED;
            vecDict = (MemoryDictionary<String>) mf.
                    getDictionary(DictionaryType.STEMMED_VECTOR);
        } else {
            termStatsType = Field.TermStatsType.RAW;
            vecDict = (MemoryDictionary<String>) mf.
                    getDictionary(DictionaryType.RAW_VECTOR);
        }

        //
        // Make sure we compute weights with the right term stats!
        wc.setTermStatsType(termStatsType);

        IndexEntry<String> vecEntry = vecDict.get(key);
        if(vecEntry == null) {
            logger.warning(String.format("No document vector for key %s", key));
            v = new WeightedFeature[0];
            return;
        }

        DocumentVectorPostings dvp = (DocumentVectorPostings) vecEntry.
                getPostings();
        v = dvp.getWeightedFeatures(vecEntry.getID(), field.getID(), null, wf,
                                    wc);
        length = 0;
        for(WeightedFeature feat : v) {
            length += feat.getWeight() * feat.getWeight();
        }
        length = (float) Math.sqrt(length);
        normalize();
    }

    @Override
    public Collection<FieldInfo> getFields() {
        return Collections.singletonList(field);
    }

    @Override
    public DocumentVector copy() {
        SingleFieldMemoryDocumentVector ret = new SingleFieldMemoryDocumentVector();
        ret.engine = engine;
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
        return v;
    }

    public QueryEntry getEntry() {
        return keyEntry;
    }

    /**
     * Calculates the dot product of this document vector with another.
     *
     * @param dvi another document vector
     * @return the dot product of the two vectors (i.engine. the sum of the
     * products of the components in each dimension)
     */
    public float dot(SingleFieldMemoryDocumentVector dvi) {
        dvi.getFeatures();
        getFeatures();
        return dot(dvi.v);
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
        return SingleFieldMemoryDocumentVector.
                                                           findSimilar(engine,
                                                                       getFeatures(),
                                                                       field,
                                                                       sortOrder,
                                                                       skimPercent,
                                                                       wf, wc, termStatsType);
    }

    protected static ResultSet findSimilar(SearchEngine e,
                                           WeightedFeature[] v,
                                           FieldInfo field,
                                           String sortOrder, double skimPercent,
                                           WeightingFunction wf,
                                           WeightingComponents wc,
                                           TermStatsType termStatsType) {

        QueryStats qs = new QueryStats();
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
            qor.addField(field);
            for(WeightedFeature f : sf) {

                QueryEntry entry = cdf.getTerm(f.getName(), false);
                if(entry == null) {
                    qor.addWeightOnly(f.getWeight());
                    continue;
                }
                TermStatsImpl tsi = cdf.getTermStats(f.getName(),
                                                     termStatsType);
                wf.initTerm(wc.setTerm(tsi));
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
}
