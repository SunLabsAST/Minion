package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.ResultAccessor;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.dictionary.DiskDictionaryBundle.Fetcher;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple implementation of a results accessor that can be used in cases
 * where we don't have a real result set.
 */
public class ResultAccessorImpl implements ResultAccessor {

    private InvFileDiskPartition dp;

    private int currDoc;

    private QueryEntry dke;

    private float currScore;

    /**
     * Fetchers for field values.
     */
    private Map<String,Fetcher> fetchers = new HashMap<String, Fetcher>();

    public ResultAccessorImpl() {
    }

    public void setPartition(InvFileDiskPartition dp) {
        this.dp = dp;
        fetchers.clear();
    }

    public void setCurrDoc(int currDoc) {
        this.currDoc = currDoc;
    }

    public void setCurrScore(float currScore) {
        this.currScore = currScore;
    }

    @Override
    public float getScore() {
        return currScore;
    }

    @Override
    public String getKey() {
        if(dke == null) {
            dke = dp.getDocumentDictionary().getByID(currDoc);
        }
        return dke.getName().toString();
    }

    private Fetcher getFetcher(String field) {
        
        Fetcher fetcher = fetchers.get(field);
        
        if(fetcher != null) {
            return fetcher;
        }
        
        DiskField df = dp.getDF(field);
        if(df == null) {
            return null;
        }
        
        fetcher = df.getFetcher();
        fetchers.put(field, fetcher);
        return fetcher;
    }

    @Override
    public List<Object> getField(String field) {
        Fetcher f = getFetcher(field);
        if(f != null) {
            return f.fetch(currDoc);
        }
        return new ArrayList<Object>();
    }

    @Override
    public Object getSingleFieldValue(String field) {
        Fetcher f = getFetcher(field);
        if(f != null) {
            return f.fetchOne(currDoc);
        }
        return null;
    }

    @Override
    public boolean containsAny(String field, int[] ids) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean containsAll(String field, int[] ids) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
