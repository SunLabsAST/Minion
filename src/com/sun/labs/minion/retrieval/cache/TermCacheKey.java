package com.sun.labs.minion.retrieval.cache;

import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import java.util.Collections;
import java.util.List;

/**
 * A key for the term cache, that we can use to store the terms that we're
 * computing for.
 *
 */
public class TermCacheKey {

    private String key;

    private List<String> names;

    private PostingsIteratorFeatures feat;

    public TermCacheKey(List<String> names, PostingsIteratorFeatures feat) {
        this.feat = feat;
        this.names = names;
        if(this.names.size() > 1) {
            Collections.sort(this.names);
        }
        StringBuilder sb = new StringBuilder();
        for(String n : names) {
            sb.append('/');
            sb.append(n);
        }
        key = sb.toString();
    }

    public TermCacheKey(String name, PostingsIteratorFeatures feat) {
        this(Collections.singletonList(name), feat);
    }

    public PostingsIteratorFeatures getFeat() {
        return feat;
    }

    public String getKey() {
        return key;
    }

    public List<String> getNames() {
        return names;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(obj instanceof TermCacheKey) {
            return key.equals(((TermCacheKey) obj).key);
        }
        return false;
    }

    @Override
    public String toString() {
        return key;
    }
}
