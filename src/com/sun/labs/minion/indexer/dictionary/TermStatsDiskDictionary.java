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
import com.sun.labs.minion.indexer.Field;
import com.sun.labs.minion.indexer.entry.TermStatsEntryFactory;
import com.sun.labs.minion.indexer.entry.TermStatsQueryEntry;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
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

    private DiskDictionary[][] fieldDicts;
    
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

        fieldDicts = new DiskDictionary[manager.getMetaFile().size() + 1][];

        //
        // The very first go-round, we might not have stats.
        if(!termStatsFile.exists()) {
            header = new TermStatsHeader();
        } else {
            raf = new RandomAccessFile(termStatsFile, "r");
            long headerPos = raf.readLong();
            raf.seek(headerPos);
            header = new TermStatsHeader(raf);
            for(Map.Entry<Integer, long[]> e : header) {
                long[] fo = e.getValue();
                fieldDicts[e.getKey()] = new DiskDictionary[fo.length];
                for(int i = 0; i < fo.length; i++) {
                    if(fo[i] >= 0) {
                        logger.info(String.format("Loading %d: %d", e.getKey(), fo[i]));
                        raf.seek(fo[i]);
                        fieldDicts[e.getKey()][i] = new DiskDictionary(
                                new TermStatsEntryFactory(),
                                new StringNameHandler(),
                                raf,
                                null,
                                DiskDictionary.PostingsInputType.CHANNEL_FULL_POST,
                                DiskDictionary.BufferType.NIOFILEBUFFER,
                                1024,
                                128
                                * 1024, 128 * 1024, 128 * 1024, 128 * 1024, null);
                        size = Math.max(fieldDicts[e.getKey()][i].size(), size);
                    }
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
        return getTermStats(term, field, Field.TermStatsType.RAW);
    }    

    public TermStatsQueryEntry getTermStats(String term, String field, Field.TermStatsType type) {
        DiskDictionary dd = getDictionary(field, type);
        return dd == null ? null : (TermStatsQueryEntry) dd.get(term);
    }

    /**
     * Gets the term stats across all the fields.
     *
     * @param term the term to get stats for
     * @return the combined term statistics
     */
    public TermStatsImpl getTermStats(String term) {
        return getTermStats(term, Field.TermStatsType.RAW);
    }

    /**
     * Generates combined term stats across all of the fields for a given term.
     * @param term the term for which we want stats
     * @param type what type of stats we want (e.g., for raw vs. stemmed)
     * @return 
     */
    public TermStatsImpl getTermStats(String term, Field.TermStatsType type) {
        TermStatsImpl total = new TermStatsImpl(term);
        total.clear();
        for(DiskDictionary[] dd : fieldDicts) {
            if(dd != null) {
                TermStatsQueryEntry qe = (TermStatsQueryEntry) dd[type.ordinal()].get(term);
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
        return getTermStats(term, fi, Field.TermStatsType.RAW); 
    }
    
    public TermStatsQueryEntry getTermStats(String term, FieldInfo fi, Field.TermStatsType type) {
        DiskDictionary dd = getDictionary(fi, type);
        return dd == null ? null : (TermStatsQueryEntry) dd.get(term);
    }

    /**
     * Gets an iterator for the raw term statistics dictionary.
     * @param field the field for which we want the iterator
     * @return an iterator for the term stats.
     */
    public DictionaryIterator<String> iterator(String field) {
        return iterator(field, Field.TermStatsType.RAW);
    }
    
    public DictionaryIterator<String> iterator(String field, Field.TermStatsType type) {
        DiskDictionary<String> dd = getDictionary(field, type);
        return dd == null ? null : (DictionaryIterator<String>) dd.iterator();
    }
    
    public LightIterator<String> literator(String field) {
        return literator(field, Field.TermStatsType.RAW);
    }
    
    public LightIterator<String> literator(String field, Field.TermStatsType type) {
        DiskDictionary<String> dd = getDictionary(field, type);
        return dd == null ? null : dd.literator();
    }
    
    /**
     * Gets an for this dictionary.
     * @param field the field for which we want the iterator
     * @return an iterator for the term stats.
     */
    public DictionaryIterator<String> iterator(FieldInfo field) {
        return iterator(field, Field.TermStatsType.RAW);
    }    

    public DictionaryIterator<String> iterator(FieldInfo field, Field.TermStatsType type) {
        DiskDictionary dd = getDictionary(field, type);
        return dd == null ? null : (DictionaryIterator<String>) dd.iterator();
    }

    public LightIterator literator(FieldInfo field) {
        return literator(field, Field.TermStatsType.RAW);
    }
    
    public LightIterator literator(FieldInfo field, Field.TermStatsType type) {
        DiskDictionary dd = getDictionary(field, type);
        return dd == null ? null : dd.literator();
    }

    public DiskDictionary<String> getDictionary(String field, Field.TermStatsType type) {
        FieldInfo fi = manager.getMetaFile().getFieldInfo(field);
        return getDictionary(fi, type);
    }

    public DiskDictionary<String> getDictionary(FieldInfo fi, Field.TermStatsType type) {
        if(fi == null) {
            return null;
        }
        
        if(fi.getID() >= fieldDicts.length) {
            return null;
        }

        DiskDictionary[] dicts = fieldDicts[fi.getID()];
        if(dicts != null) {
            return dicts[type.ordinal()];
        }
        return null;
    }

    public int size() {
        return size;
    }

    @Override
    public void setCloseTime(long closeTime) {
        this.closeTime = closeTime;
    }

    @Override
    public long getCloseTime() {
        return closeTime;
    }

    @Override
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

    @Override
    public void setClosed() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void createRemoveFile() {
        try {
            manager.makeRemovedTermStatsFile(dictNumber).createNewFile();
        } catch(IOException ex) {
            logger.log(Level.SEVERE, String.format("Unable to make removed file for TSD %d", dictNumber), ex);
        }
    }
    
    @Override
    public String toString() {
        return String.format("TSD: %d Dicts: %s", dictNumber, Arrays.toString(fieldDicts));
    }
}
