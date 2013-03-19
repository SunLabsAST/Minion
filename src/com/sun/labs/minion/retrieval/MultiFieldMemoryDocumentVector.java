package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.WeightedFeature;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.Field;
import com.sun.labs.minion.indexer.MemoryField;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.postings.DocumentVectorPostings;
import com.sun.labs.minion.retrieval.MultiFieldDocumentVector.LocalTermStats;
import com.sun.labs.minion.util.Util;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A document vector built from multiple fields from an in-memory index.
 */
public class MultiFieldMemoryDocumentVector extends AbstractDocumentVector
        implements Serializable {

    private static final Logger logger = Logger.
            getLogger(MultiFieldMemoryDocumentVector.class.getName());
    
    private Set<FieldInfo> vectorFields = new HashSet<FieldInfo>();
    
    private MultiFieldMemoryDocumentVector() {
        
    }

    public MultiFieldMemoryDocumentVector(Collection<MemoryField> fields,
                                          String key, SearchEngine engine) {
        this.engine = (SearchEngineImpl) engine;
        this.key = key;
        wf = engine.getQueryConfig().getWeightingFunction();
        wc = engine.getQueryConfig().getWeightingComponents();
        initFeatures(fields);
    }

    private void initFeatures(Collection<MemoryField> fields) {
        Map<String, LocalTermStats> tm = new HashMap<String, LocalTermStats>();

        //
        // Operate per-field.
        for(MemoryField mf : fields) {
            
            FieldInfo info = mf.getInfo();
            vectorFields.add(info);
            
            //
            // Get the dictionary from which we'll draw the vector and the one
            // from which we'll draw terms, then look up the document.
            MemoryDictionary<String> vecDict;
            if(mf.isStemmed()) {
                termStatsType = Field.TermStatsType.STEMMED;
                vecDict = (MemoryDictionary<String>) mf.
                        getDictionary(Field.DictionaryType.STEMMED_VECTOR);
            } else if(mf.isUncased()) {
                termStatsType = Field.TermStatsType.UNCASED;
                vecDict = (MemoryDictionary<String>) mf.
                        getDictionary(Field.DictionaryType.UNCASED_VECTOR);
            } else {
                termStatsType = Field.TermStatsType.CASED;
                vecDict = (MemoryDictionary<String>) mf.
                        getDictionary(Field.DictionaryType.CASED_VECTOR);
            }

            //
            // Make sure we compute weights with the right term stats!
            wc.setTermStatsType(termStatsType);

            IndexEntry<String> vecEntry = vecDict.get(key);
            if(vecEntry == null) {
                logger.warning(String.format("No document vector for key %s",
                                             key));
                v = new WeightedFeature[0];
                return;
            }

            //
            // Get the postings for this document.
            DocumentVectorPostings dvp = (DocumentVectorPostings) vecEntry.
                    getPostings();
            WeightedFeature[] features = dvp.getWeightedFeatures(vecEntry.getID(), info.getID(), null,
                                        wf,
                                        wc, termStatsType);
            
            for(WeightedFeature feature : features) {
                
                //
                // Accumulate the frequency and term stats for this entry.
                TermStatsImpl tsi = (TermStatsImpl) engine.
                        getTermStats(feature.getName(), info);
                LocalTermStats lts = tm.get(feature.getName());
                if(lts == null) {
                    lts = new LocalTermStats(feature.getName());
                    tm.put(feature.getName(), lts);
                }
                lts.add(feature.getFreq(), tsi);
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
            WeightedFeature feat = new WeightedFeature(ent.getKey(), wf.
                    termWeight(wc));
            length += (feat.getWeight() * feat.getWeight());
            v[p++] = feat;
        }

        length = (float) Math.sqrt(length);
        normalize();

        //
        // Sort by name!
        Util.sort(v, WeightedFeature.NAME_COMPARATOR);
}

    @Override
    public WeightedFeature[] getFeatures() {
        return v;
    }

    @Override
    public Collection<FieldInfo> getFields() {
        return vectorFields;
    }

    @Override
    public DocumentVector copy() {
        MultiFieldMemoryDocumentVector ret = new MultiFieldMemoryDocumentVector();
        ret.engine = engine;
        ret.key = key;
        ret.keyEntry = keyEntry;
        ret.vectorFields = vectorFields;
        ret.wf = wf;
        ret.wc = wc;
        ret.ignoreWords = ignoreWords;
        ret.v = v != null ? v.clone() : null;
        return ret;
    }

    @Override
    public ResultSet findSimilar(String sortOrder, double skimPercent) {
        return MultiFieldDocumentVector.findSimilar(engine, v, vectorFields.
                toArray(new FieldInfo[0]), sortOrder, skimPercent, wf, wc,
                                                    termStatsType);
    }
}
