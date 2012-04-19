package com.sun.labs.minion.indexer.entry;

import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.Postings.Type;
import com.sun.labs.minion.util.buffer.ReadableBuffer;

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

    @Override
    public QueryEntry getQueryEntry(String name, ReadableBuffer b) {
        return new TermStatsQueryEntry(name, b);
    }
}
