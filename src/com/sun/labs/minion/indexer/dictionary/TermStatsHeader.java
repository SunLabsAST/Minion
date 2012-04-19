package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * A class to hold the offsets of the per-field term stat dictionaries.
 */
public class TermStatsHeader implements Iterable<Map.Entry<Integer, Long>> {

    private static final Logger logger = Logger.getLogger(TermStatsHeader.class.getName());
    
    private Map<Integer, Long> offsets;

    public TermStatsHeader() {
        offsets = new TreeMap<Integer, Long>();
    }

    public TermStatsHeader(RandomAccessFile raf) throws java.io.IOException {
        offsets = new HashMap<Integer, Long>();
        int n = raf.readInt();
        for(int i = 0; i < n; i++) {
            offsets.put(raf.readInt(), raf.readLong());
        }
    }

    public void write(RandomAccessFile raf) throws java.io.IOException {
        raf.writeInt(offsets.size());
        for(Entry<Integer, Long> e : offsets.entrySet()) {
            raf.writeInt(e.getKey());
            raf.writeLong(e.getValue());
        }
    }

    public void write(WriteableBuffer b) throws java.io.IOException {
        b.byteEncode(offsets.size(), 4);
        for(Entry<Integer, Long> e : offsets.entrySet()) {
            b.byteEncode(e.getKey(), 4);
            b.byteEncode(e.getValue(), 8);
        }
    }

    public void addOffset(int fieldID, long offset) {
        offsets.put(fieldID, offset);
    }

    public long getOffset(int fieldID) {
        Long l = offsets.get(fieldID);
        if(l == null) {
            return -1;
        }
        return l;
    }
    
    public void clear() {
        offsets.clear();
    }

    @Override
    public Iterator<Entry<Integer, Long>> iterator() {
        return offsets.entrySet().iterator();
    }

    public int size() {
        return offsets.size();
    }

    @Override
    public String toString() {
        return "TermStatsHeader{" + "offsets=" + offsets + '}';
    }


}
