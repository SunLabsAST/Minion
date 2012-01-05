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
import com.sun.labs.minion.retrieval.TermStatsImpl;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A term statistics dictionary to use at query time.
 */
public class TermStatsDiskDictionary implements Closeable {

    private static final Logger logger = Logger.getLogger(TermStatsDiskDictionary.class.getName());

    private int dictNumber;

    private TermStatsHeader header;

    private PartitionManager manager;

    private File termStatsFile;

    private RandomAccessFile raf;

    private DiskDictionary[] fieldDicts;

    private long closeTime;

    private boolean closed;

    private int size;

    public TermStatsDiskDictionary(int dictNumber,
            File termStatsFile,
            PartitionManager manager) throws
            java.io.IOException {
        this.dictNumber = dictNumber;
        this.termStatsFile = termStatsFile;
        this.manager = manager;

        fieldDicts = new DiskDictionary[manager.getMetaFile().size() + 1];

        //
        // The very first go-round, we might not have stats.
        if(!termStatsFile.exists()) {
            header = new TermStatsHeader();
        } else {
            raf = new RandomAccessFile(termStatsFile, "r");
            long headerPos = raf.readLong();
            raf.seek(headerPos);
            header = new TermStatsHeader(raf);
            for(Map.Entry<Integer, Long> e : header) {
                if(e.getValue() >= 0) {
                    raf.seek(e.getValue());
                    fieldDicts[e.getKey()] = new DiskDictionary(new TermStatsEntryFactory(), new StringNameHandler(), raf, 
                            null, DiskDictionary.PostingsInputType.CHANNEL_FULL_POST, 
                            DiskDictionary.BufferType.NIOFILEBUFFER, 1024, 
                            128 * 1024, 128 * 1024, 128*1024, 128*1024, null);
                    size = Math.max(fieldDicts[e.getKey()].size(), size);
                }
            }
        }
    }

    /**
     * Gets the term statistics associated with the given term.
     *
     * @param term the term for which we want collection statistics.
     * @param field the field for which we want stats for the term
     * @return the term statistics associated with the given term, or <code>null</code>
     * if that term does not occur in the index
     */
    public TermStatsQueryEntry getTermStats(String term, String field) {
        DiskDictionary dd = getDictionary(field);
        return dd == null ? null : (TermStatsQueryEntry) dd.get(term);
    }

    /**
     * Gets the term stats across all the fields.
     *
     * @param term the term to get stats for
     * @return the combined term statistics
     */
    public TermStatsImpl getTermStats(String term) {
        TermStatsImpl total = new TermStatsImpl(term);
        total.clear();
        for(DiskDictionary dd : fieldDicts) {
            if(dd != null) {
                TermStatsQueryEntry qe = (TermStatsQueryEntry) dd.get(term);
                if(qe != null) {
                    total.add(qe.getTermStats());
                }
            }
        }
        return total;
    }

    /**
     * Gets the term statistics associated with the given term.
     *
     * @param term the term for which we want collection statistics.
     * @param fi the field for which we want stats for the term
     * @return the term statistics associated with the given term, or <code>null</code>
     * if that term does not occur in the index
     */
    public TermStatsQueryEntry getTermStats(String term, FieldInfo fi) {
        DiskDictionary dd = getDictionary(fi);
        return dd == null ? null : (TermStatsQueryEntry) dd.get(term);
    }

    /**
     * Gets an for this dictionary.
     * @param field the field for which we want the iterator
     * @return an iterator for the term stats.
     */
    public DictionaryIterator<String> iterator(String field) {
        DiskDictionary<String> dd = getDictionary(field);
        return dd == null ? null : (DictionaryIterator<String>) dd.iterator();
    }
    
    public LightIterator<String> literator(String field) {
        DiskDictionary<String> dd = getDictionary(field);
        return dd == null ? null : dd.literator();
    }
    
    /**
     * Gets an for this dictionary.
     * @param field the field for which we want the iterator
     * @return an iterator for the term stats.
     */
    public DictionaryIterator<String> iterator(FieldInfo field) {
        if(field.getID() >= fieldDicts.length) {
            return null;
        }
        DiskDictionary dd = fieldDicts[field.getID()];
        return dd == null ? null : (DictionaryIterator<String>) dd.iterator();
    }

    public LightIterator literator(FieldInfo field) {
        if(field.getID() >= fieldDicts.length) {
            return null;
        }
        DiskDictionary dd = fieldDicts[field.getID()];
        return dd == null ? null : dd.literator();
    }

    public DiskDictionary<String> getDictionary(String field) {
        FieldInfo fi = manager.getMetaFile().getFieldInfo(field);
        return getDictionary(fi);
    }

    public DiskDictionary<String> getDictionary(FieldInfo fi) {
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
        if(closeTime <= currTime) {
            try {
                if(raf != null) {
                    raf.close();
                }
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format(
                        "Error closing term stats file %s", termStatsFile), ex);
            }
            closed = true;
            return true;
        }
        return false;
    }

    public void setClosed() {
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    public void createRemoveFile() {
        try {
            manager.makeRemovedTermStatsFile(dictNumber).createNewFile();
        } catch(IOException ex) {
            logger.log(Level.SEVERE, String.format("Unable to make removed file for TSD %d", dictNumber), ex);
        }
    }
    
    @Override
    public String toString() {
        return String.format("TSD: %d", dictNumber);
    }
}
