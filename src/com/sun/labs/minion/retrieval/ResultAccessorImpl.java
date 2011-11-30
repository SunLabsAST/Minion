package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.ResultAccessor;
import com.sun.labs.minion.indexer.DiskDictionaryBundle;
import com.sun.labs.minion.indexer.DiskDictionaryBundle.Fetcher;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import java.util.ArrayList;
import java.util.List;

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
    private Fetcher[] fetchers;

    public ResultAccessorImpl() {
    }

    public void setPartition(InvFileDiskPartition dp) {
        this.dp = dp;
        if(fetchers != null) {
            for(int i = 0; i < fetchers.length; i++) {
                fetchers[i] = null;
            }
        }
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
        FieldInfo fi = dp.getPartitionManager().getMetaFile().getFieldInfo(field);
        if(fi == null) {
            return null;
        }
        if(!fi.hasAttribute(FieldInfo.Attribute.SAVED)) {
            return null;
        }
        int fid = fi.getID();
        if(fetchers == null) {
            fetchers = new Fetcher[fid + 1];
        } else if(fetchers.length < fid) {
           Fetcher[] temp = new Fetcher[fid + 1];
            System.arraycopy(fetchers, 0, temp, 0, fetchers.length);
            fetchers = temp;
        }
        if(fetchers[fid] == null) {
            fetchers[fid] = dp.getDF(fi).getFetcher();
        }
        return fetchers[fid];
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
}
