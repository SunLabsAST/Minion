package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.indexer.Field;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * A class to hold the offsets of the per-field term stat dictionaries.
 */
public class TermStatsHeader implements Iterable<Map.Entry<Integer, long[]>> {

    private static final Logger logger = Logger.getLogger(TermStatsHeader.class.getName());
    
    private Map<Integer, long[]> offsets;

    public TermStatsHeader() {
        offsets = new TreeMap<Integer, long[]>();
    }

    public TermStatsHeader(RandomAccessFile raf) throws java.io.IOException {
        offsets = new HashMap<Integer, long[]>();
        int n = raf.readInt();
        for(int i = 0; i < n; i++) {
            int id = raf.readInt();
            long[] fo = new long[Field.TermStatsType.values().length];
            for(int j = 0; j < fo.length; j++) {
                fo[j] = raf.readLong();
            }
            offsets.put(id, fo);
        }
    }

    public void write(RandomAccessFile raf) throws java.io.IOException {
        raf.writeInt(offsets.size());
        for(Entry<Integer, long[]> e : offsets.entrySet()) {
            raf.writeInt(e.getKey());
            for(long o : e.getValue()) {
                raf.writeLong(o);
            }
        }
    }

    public void write(WriteableBuffer b) throws java.io.IOException {
        b.byteEncode(offsets.size(), 4);
        for(Entry<Integer, long[]> e : offsets.entrySet()) {
            b.byteEncode(e.getKey(), 4);
            for(long o : e.getValue()) {
                b.byteEncode(o, 8);
            }
        }
    }

    public void addOffset(int fieldID, Field.TermStatsType type, long offset) {
        long[] fo = offsets.get(fieldID);
        if(fo == null) {
            fo = new long[Field.TermStatsType.values().length];
            Arrays.fill(fo, -1);
            offsets.put(fieldID, fo);
        }
        fo[type.ordinal()] = offset;
    }

    public long getOffset(int fieldID) {
        return getOffset(fieldID, Field.TermStatsType.RAW);
    }
    
    public long getOffset(int fieldID, Field.TermStatsType type) {
        long[] fo = offsets.get(fieldID);
        if(fo == null) {
            return -1;
        }
        return fo[type.ordinal()];
    }
    
    public void clear() {
        offsets.clear();
    }

    @Override
    public Iterator<Entry<Integer, long[]>> iterator() {
        return offsets.entrySet().iterator();
    }

    public int size() {
        return offsets.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TermStatsHeader{offsets=");
        boolean first = true;
        for(Map.Entry<Integer,long[]> e : offsets.entrySet()) {
            if(!first) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append(": ").append(Arrays.toString(e.
                    getValue()));
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }
}
