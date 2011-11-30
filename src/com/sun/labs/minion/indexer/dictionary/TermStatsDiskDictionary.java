/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */
package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.Closeable;
import com.sun.labs.minion.indexer.entry.TermStatsEntryFactory;
import com.sun.labs.minion.indexer.entry.TermStatsQueryEntry;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import java.io.RandomAccessFile;
import java.util.Map;

/**
 * A term statistics dictionary to use at query time.
 */
public class TermStatsDiskDictionary implements Closeable {

    private int dictNumber;

    private TermStatsHeader header;

    private PartitionManager manager;

    private DiskDictionary[] fieldDicts;

    private long closeTime;

    private boolean closed;

    private int size;

    public TermStatsDiskDictionary(int dictNumber,
                                   RandomAccessFile raf,
                                   PartitionManager manager) throws
            java.io.IOException {
        this.dictNumber = dictNumber;
        long headerPos = raf.readLong();
        raf.seek(headerPos);
        header = new TermStatsHeader(raf);
        fieldDicts = new DiskDictionary[manager.getMetaFile().size() + 1];
        for(Map.Entry<Integer, Long> e : header) {
            raf.seek(e.getValue());
            fieldDicts[e.getKey()] = new DiskDictionary(
                    new TermStatsEntryFactory(),
                    new StringNameHandler(), raf, null);
            size = Math.max(fieldDicts[e.getKey()].size(), size);
        }
    }

    /**
     * Gets the term statistics associated with the given term.
     * 
     * @param term the term for which we want collection statistics.
     * @return the term statistics associated with the given term, or <code>null</code>
     * if that term does not occur in the index
     */
    public TermStatsQueryEntry getTermStats(String term, String field) {
        DiskDictionary dd = getDict(field);
        return dd == null ? null : (TermStatsQueryEntry) dd.get(term);
    }

    /**
     * Gets an for this dictionary.
     * @return an iterator for the term stats.
     */
    public DictionaryIterator iterator(String field) {
        DiskDictionary dd = getDict(field);
        return dd == null ? null : dd.iterator();
    }

    public DictionaryIterator iterator(FieldInfo field) {
        if(field.getID() >= fieldDicts.length) {
            return null;
        }
        DiskDictionary dd = fieldDicts[field.getID()];
        return dd == null ? null : dd.iterator();
    }

    private DiskDictionary getDict(String field) {
        FieldInfo fi = manager.getMetaFile().getFieldInfo(field);
        if(fi == null) {
            return null;
        }
        return fieldDicts[fi.getID()];
    }

    public int size() {
        return size;
    }

    public void setCloseTime(long closeTime) {
        this.closeTime = closeTime;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public boolean close(long currTime) {
        closed = closeTime <= currTime;
        return closed;
    }

    public void setClosed() {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public void createRemoveFile() {
        manager.makeRemovedTermStatsFile(dictNumber);
    }
}
