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

import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.util.Util;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A dictionary that will be used during indexing.  The entries will be
 * stored in a {@link java.util.Map}.
 *
 * <p>
 *
 * The dictionary is instantiated with the class of the entries that it
 * will contain.  It provides the ability to make entries of the
 * appropriate type, given the name that will map to that entry.  We
 * provide a special case for entries that store information for the case
 * insensitive version of a particular name.
 *
 * <p>
 *
 * At the time that the dictionary is written to disk, the entries are
 * sorted by name.  At this time, the IDs of the entries may be reassigned
 * in name order (they were originally assigned in order of addition to the
 * dictionary.)  If this is the case, then a mapping between old and new
 * IDs will be stored and can be retrieved by anyone who needs it.
 *
 * <p>
 *
 * Note that the size of the dictionary at dump time and the
 *
 * @param <N> the type of the names in the dictionary.
 */
public class MemoryDictionary<N extends Comparable> implements Dictionary<N> {

    /**
     * The partition with which this dictionary is associated.
     */
    protected Partition part;

    /**
     * A map to hold the entries.
     */
    protected Map<N, IndexEntry> map;

    /**
     * The class of the entries that we will be holding.
     */
    protected EntryFactory factory;

    /**
     * The ID that we will assign to entries as they are added.
     */
    protected int id;

    /**
     * A map from the IDs assigned before sorting to the IDs assigned after
     * sorting.  Which way the map goes depends on the
     * <code>renumber</code> parameter of {@link #dump}
     */
    protected int[] idMap;

    /**
     * The sorted entries from the dictionary, computed at dump time and 
     * saved.
     */
    private IndexEntry[] sortedEntries;
    
    /**
     * The number of used entries in this dictionary.  Computed at sort time,
     * so if this is non-zero, then the dictionary has been sorted.
     */
    private int nUsed;
    
    /**
     * The log.
     */
    private static final Logger logger = Logger.getLogger(MemoryDictionary.class.getName());

    /**
     * The tag for this module.
     */
    protected static String logTag = "MD";

    /**
     * An enumeration of the kinds of renumbering that may need to be done when
     * dumping a dictionary to disk.
     */
    public enum Renumber {

        /**
         * No renumbering is necessary.  This is typical in, for example, 
         * document dictionaries.
         */
        NONE,
        /**
         * Renumbering is necessary.  Entries in the dictionaries will have their
         * IDs renumbered so that the IDs are in the same order as the names of
         * the entries in the dictionary.
         */
        RENUMBER

    }

    /**
     * An enumeration of the kinds of ID maps that may have to be built when
     * dumping a dictionary.
     */
    public enum IDMap {

        /**
         * No map needs to be kept.
         */
        NONE,
        /**
         * A map from the old IDs to the new IDs should be kept.
         */
        OLD_TO_NEW,
        /**
         * A map from the new IDs to the old IDs should be kept.
         */
        NEW_TO_OLD,}

    protected MemoryDictionary() {
    }

    /**
     * Creates a dictionary that can be used during indexing.
     *
     * @param factory a factory that will generate entries for this dictionary
     */
    public MemoryDictionary(EntryFactory factory) {
        map = new HashMap<N, IndexEntry>();
        id = 0;
        this.factory = factory;
    }

    @Override
    public String[] getPostingsChannelNames() {
        if(factory != null) {
            return factory.getPostingsChannelNames();
        }
        return null;
    }
    
    public Set<N> getKeys() {
        return map.keySet();
    }

    /**
     * Puts an entry name into the dictionary, creating an entry for the name
     * if none exists.
     *
     * @param name The name of the entry.
     * @return The entry corresponding to the given name
     */
    public IndexEntry<N> put(N name) {
        IndexEntry<N> old = map.get(name);
        if(old == null) {
            old = factory.getIndexEntry(name, ++id);
            old.setDictionary(this);
            map.put(name, old);
        }
        return old;
    }
    
    /**
     * Puts an entry into the dictionary directly.  This will clone the 
     * given entry so that it can have its own postings.
     * 
     * @param entry the entry to add to the dictionary
     * @return The entry corresponding to the given name.
     */
    public IndexEntry<N> put(IndexEntry<N> entry) {
        N name = entry.getName();
        IndexEntry<N> oldEntry = map.get(name);
        if(oldEntry == null) {
            oldEntry =  factory.getIndexEntry(entry);
            oldEntry.setDictionary(this);
            map.put(name, oldEntry);
        } else {
            oldEntry.setID(entry.getID());
        }
        return oldEntry;
    }

    /**
     * Gets an entry from the dictionary, given the name for the entry.
     *
     * @param name The name of the entry.
     * @return The entry associated with the name, or <code>null</code> if
     * the name doesn't appear in the dictionary.
     */
    public IndexEntry get(N name) {
        return map.get(name);
    }

    /**
     * Deletes an entry from the dictionary, given the name for the entry.
     *
     * @param name The name of the entry.
     * @return The entry associated with the name, or <code>null</code> if
     * the name doesn't appear in the dictionary.
     */
    public IndexEntry remove(N name) {
        return map.remove(name);
    }

    @Override
    public Partition getPartition() {
        return part;
    }

    public void setPartition(Partition partition) {
        part = partition;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Iterator<Entry<N>> iterator() {
        return new MemoryDictionaryIterator();
    }

    /**
     * Clears the dictionary, emptying it of all data.
     */
    public void clear() {
        for(Iterator<Map.Entry<N, IndexEntry>> i = map.entrySet().iterator(); i.hasNext();) {
            Map.Entry<N, IndexEntry> e = i.next();
            IndexEntry entry = e.getValue();
            if(entry.getN() > 1) {
                entry.clear();
            } else {
                i.remove();
            }
        }
        if(sortedEntries != null) {
            for(int i = 0; i < nUsed; i++) {
                sortedEntries[i] = null;
            }
        }
        id = 0;
        nUsed = 0;
    }

    public IndexEntry[] getSortedEntries() {
        return sortedEntries;
    }

    /**
     * Sorts the dictionary entries.  Depending on the value of
     * <code>renumber</code>, new IDs may be assigned to the entries in
     * their new, sorted order.
     *
     * @param renumber whether the entries in the dictionary should be 
     * renumbered in order of the names
     * @param idMapType what kind of map (if any) should be kept between the
     * old and new IDs
     * @return <code>true</code> if there were entries that were sorted, 
     * <code>false</code> otherwise.
     */
     protected boolean sort(Renumber renumber, IDMap idMapType) {
        
//        logger.fine(String.format("sort: %s %s", renumber, idMapType));
        
        //
        // Figure out how many used elements there were in the map.
        nUsed = 0;
        for(IndexEntry e : map.values()) {
            if(e.isUsed()) {
                nUsed++;
            }
        }
        
        if(nUsed == 0) {
            return false;
        }
        
        //
        // Make sure that we've the space for them.
        if(sortedEntries == null || sortedEntries.length < nUsed) {
            sortedEntries = new IndexEntry[nUsed];
        }
        
        if(idMapType != IDMap.NONE) {
            if(idMap == null || idMap.length <= getMaxID()) {
                idMap = new int[getMaxID()+1];
            }
        }
        
        nUsed = 0;
        for(IndexEntry e : map.values()) {
            if(e.isUsed()) {
//                if(nUsed <= 10) {
//                    logger.fine(String.format("si %d %s %d", nUsed, e.getName(), e.getID()));
//                }
                sortedEntries[nUsed++] = e;
            }
        }
        
        //
        // Sort.
        Util.sort(sortedEntries, 0, nUsed);

        //
        // Renumber entries if necessary.
        switch(renumber) {
            case NONE:
                break;
            case RENUMBER:
                int newID = 1;
                switch(idMapType) {
                    case NONE:
                        for(int i = 0; i < nUsed; i++) {
                            Entry e = sortedEntries[i];
                            e.setID(newID++);
                        }
                        break;
                    case OLD_TO_NEW:
                        for(int i = 0; i < nUsed; i++) {
                            Entry e = sortedEntries[i];
                            try {
                            idMap[e.getID()] = newID;
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                logger.log(Level.SEVERE, String.format("aiobe idMap: %d id: %d newID: %d maxID: %d",
                                        idMap.length, e.getID(), newID, getMaxID()));
                                throw ex;
                            }
                            e.setID(newID++);
                        }
                        break;
                    case NEW_TO_OLD:
                        for(int i = 0; i < nUsed; i++) {
                            Entry e = sortedEntries[i];
                            idMap[newID] = e.getID();
                            e.setID(newID++);
                        }
                        break;
                }
        }
        return true;
    }

    /**
     * Gets the largest ID in this dictionary as of the time the method
     * is called.
     *
     * @return the latest id given out
     */
    public int getMaxID() {
        return id;
    }

    /**
     * Gets a map from the IDs assigned before sorting to the IDs assigned
     * after sorting.
     *
     * @return an array of int containing the map.
     */
    public int[] getIdMap() {
        return idMap;
    }

    /**
     * Marshalls the dictionary and the associated postings to a partition output
     * that will eventually be flushed out to disk.  Once the
     * marshalling is complete, the pointer in the file must be pointing to a
     * position *after* the data just written.  This is so that we may dump
     * multiple dictionaries and postings types to the same channel.
     *
     *  @param partOut the output where the dictionary should be marshalled.
     * @return <code>true</code> if some entries were marshalled, <code>false</code> otherwise.
     */
    public boolean marshall(PartitionOutput partOut) throws java.io.IOException {
        
        //
        // Sort the entries in preparation for writing them out.  This will
        // generate an old-to-new ID mapping if we're renumbering.  If this
        // method returns false, then there weren't any entries sorted, since 
        // there weren't any used in this dictionary (even if the map has 
        // entries), so we should just quit now.
        if(!sort(partOut.getDictionaryRenumber(), partOut.getDictionaryIDMap())) {
            return false;
        }

//        logger.fine(String.format("Marshalling %d entries %s", nUsed, partOut.getDictionaryRenumber()));

        DictionaryOutput dictOut = partOut.getPartitionDictionaryOutput();
        PostingsOutput[] postOut = partOut.getPostingsOutput();

        //
        // Start the dump.
        dictOut.start(this, 
                partOut.getDictionaryEncoder(),
                partOut.getDictionaryRenumber(),
                partOut.getDictionaryRenumber() == MemoryDictionary.Renumber.NONE,
                postOut.length);

        DictionaryHeader dh = dictOut.getHeader();
        dh.maxEntryID = id;

        //
        // Record the starting positions for each of our postings outputs.
        for(int i = 0; i < postOut.length; i++) {
            dh.postStart[i] = postOut[i].position();
        }

        int[] postingsIDMap = partOut.getPostingsIDMap();

        //
        // Write the postings and entries.
        for(int i = 0 ; i < nUsed; i++) {
            IndexEntry entry = sortedEntries[i];
            if(entry.writePostings(postOut, postingsIDMap)) {
                dictOut.write(entry);
            }
        }

        //
        // Flush whatever's left in the postings buffers. 
        for(int i = 0; i < postOut.length; i++) {
            postOut[i].flush();
            dh.postEnd[i] = postOut[i].position();
        }

        //
        // We're done dumping this dictionary.
        dictOut.finish();

        return true;
    }

    /**
     * A class that implements a dictionary iterator for this dictionary.
     */
    public class MemoryDictionaryIterator implements DictionaryIterator<N> {

        protected boolean actualOnly;

        int pos = 0;
        
        Entry<N> currEntry = null;

        public MemoryDictionaryIterator() {
            if(sortedEntries == null) {
                throw new UnsupportedOperationException("Entries must be sorted before they can be iterated!");
            }
        }

        @Override
        public boolean hasNext() {
            return pos < nUsed;
        }

        @Override
        public Entry next() {
            currEntry = sortedEntries[pos++];
            return currEntry;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Dictionary is read only");
        }

        @Override
        public N getName() {
            if(currEntry == null) {
                return null;
            }
            return currEntry.getName();
        }

        @Override
        public int estimateSize() {
            int est = 0;
            for(int i = 0; i < nUsed; i++) {
                est += sortedEntries[i].getN();
            }
            return est;
        }

        @Override
        public int getNEntries() {
            return nUsed;
        }

        @Override
        public void setUnbufferedPostings(boolean unbufferedPostings) {
        }

        @Override
        public int compareTo(DictionaryIterator o) {
            return sortedEntries[pos].getName().compareTo(o.getName());
        }
    }
}
