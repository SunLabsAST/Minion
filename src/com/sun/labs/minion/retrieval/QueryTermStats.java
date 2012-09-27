package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.TermStats;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A collection of per-document query term statistics.

 *
 * @see SearchEngineImpl#getQueryTermStats(com.sun.labs.minion.QueryConfig,
 * java.util.Collection, java.lang.String,
 * com.sun.labs.minion.indexer.Field.TermStatsType)
 */
public class QueryTermStats implements Iterable<QueryTermDocStats> {

    private static final Logger logger = Logger.getLogger(QueryTermStats.class.getName());
    
    private Map<String, Integer> documentFrequencies = new HashMap<String,Integer>();
    
    private List<QueryTermDocStats> docStats = new ArrayList<QueryTermDocStats>();
    
    public QueryTermStats(SearchEngineImpl engine, FieldInfo field, Field.TermStatsType termStatsType, String[] terms) {
        for(String term : terms) {
            TermStats tsi = engine.getTermStats(term, field, termStatsType);
            if(tsi != null) {
                documentFrequencies.put(term, tsi.getDocFreq());
            }
        }
    }
    
    /**
     * Gets the actual query terms used, which may have been stemmed.
     * @return the query terms used to generate these statistics.
     */
    public Collection<String> getQueryTerms() {
        return documentFrequencies.keySet();
    }
    
    public void add(QueryTermDocStats qtds) {
        docStats.add(qtds);
    }
    
    /**
     * Gets the document frequency of the given term. This is the number of 
     * documents that contain the term in the field that was used to generate
     * these statistics.
     * @param term the term for which we want the document frequency.
     * @return the frequency for the term or 0 if the term did not occur.
     */
    public int getDocumentFrequency(String term) {
        Integer i = documentFrequencies.get(term);
        if(i == null) {
            return 0;
        }
        return i.intValue();
    }
    
    /**
     * Gets the number of documents we have statistics for.
     * @return the number of documents.
     */
    public int size() {
        return docStats.size();
    }

    @Override
    public Iterator<QueryTermDocStats> iterator() {
        return docStats.iterator();
    }
}
