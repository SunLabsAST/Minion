package com.sun.labs.minion.indexer.entry;

import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.Postings.Type;

/**
 * A factory for term stats entries.
 */
public class TermStatsEntryFactory extends EntryFactory<String> {

    public TermStatsEntryFactory() {
        type = Type.NONE;
    }

    public TermStatsEntryFactory(Postings.Type type) {
        super(type);
    }

    @Override
    public IndexEntry getIndexEntry(String name, int id) {
        return new TermStatsIndexEntry(name, id);
    }
}
