package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.ResultAccessor;
import com.sun.labs.minion.indexer.dictionary.BasicField;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A simple implementation of a results accessor that can be used in cases
 * where we don't have a real result set.
 */
public class ResultAccessorImpl implements ResultAccessor {

    private InvFileDiskPartition dp;

    private int currDoc;

    private DocKeyEntry dke;

    private float currScore;

    /**
     * Fetchers for field values.
     */
    private BasicField.Fetcher[] fetchers;

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
            dke = dp.getDocumentTerm(currDoc);
        }
        return dke.getName().toString();
    }

    private BasicField.Fetcher getFetcher(String field) {
        FieldInfo fi = dp.getFieldStore().getFieldInfo(field);
        if(fi == null) {
            return null;
        }
        if(!fi.isSaved()) {
            return null;
        }
        int fid = fi.getID();
        if(fetchers == null) {
            fetchers = new BasicField.Fetcher[fid + 1];
        } else if(fetchers.length < fid) {
            BasicField.Fetcher[] temp = new BasicField.Fetcher[fid + 1];
            System.arraycopy(fetchers, 0, temp, 0, fetchers.length);
            fetchers = temp;
        }
        if(fetchers[fid] == null) {
            fetchers[fid] = dp.getFieldStore().getFetcher(fi);
        }
        return fetchers[fid];
    }

    @Override
    public List<Object> getField(String field) {
        BasicField.Fetcher f = getFetcher(field);
        if(f != null) {
            return f.fetch(currDoc);
        }
        return new ArrayList<Object>();
    }

    @Override
    public Object getSingleFieldValue(String field) {
        BasicField.Fetcher f = getFetcher(field);
        if(f != null) {
            return f.fetchOne(currDoc);
        }
        return null;
    }
}
