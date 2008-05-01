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

import java.io.RandomAccessFile;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import java.util.Set;
import com.sun.labs.minion.indexer.entry.CasedPostingsEntry;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.QueryEntry;

import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.minion.indexer.partition.PartitionStats;

import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.Util;


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
 */
public class MemoryDictionary implements Dictionary {
    
    /**
     * The partition with which this dictionary is associated.
     */
    protected Partition part;
    
    /**
     * A map to hold the entries.
     */
    protected Map<Object,Entry> map;
    
    /**
     * The class of the entries that we will be holding.
     */
    protected Class entryClass;
    
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
     * The log.
     */
    protected static MinionLog log = MinionLog.getLog();
    
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
        OLDTONEW,
        
        /**
         * A map from the new IDs to the old IDs should be kept.
         */
        NEWTOOLD,
    }
    
    /**
     * Creates a dictionary that can be used during indexing.
     *
     */
    public MemoryDictionary(Class entryClass) {
        map = new HashMap<Object,Entry>();
        id  = 0;
        this.entryClass = entryClass;
    }
    
    public Class getEntryClass() {
        return entryClass;
    }
    
    public Set<Object> getKeys() {
        return map.keySet();
    }
    
    /**
     * Gets a new entry that can be added to this dictionary.
     *
     * @param name The name of the entry.
     */
    protected IndexEntry simpleNewEntry(Object name) {
        try {
            IndexEntry e = (IndexEntry) entryClass.newInstance();
            e.setID(++id);
            e.setName(name);
            return e;
        } catch (Exception e) {
            log.error(logTag, 1, "Error instantiating entry", e);
            return null;
        }
    }
    
    /**
     * Gets a new, possibly cased, entry that can be added to this
     * dictionary.  If the entry is cased, then this method assumes that
     * the name provided is a string!  This method will take care to set
     * the pointer to the lower-case version of the entry so that cased
     * postings can be added correctly.
     *
     * @param name The name of the new entry.
     */
    public IndexEntry newEntry(Object name) {
        IndexEntry e = simpleNewEntry(name);
        if(e instanceof CasedPostingsEntry) {
            
            //
            // We may need to set the pointer to the lowercase entry for
            // this new entry.  First we lowercase the string.
            String lc = CharUtils.toLowerCase((String) name);
            IndexEntry lce = null;
            if(lc.equals(name)) {
                
                //
                // If the uppercase version of the name is also equal to
                // the name, then we only need one set of postings, so only
                // set the lowercase entry if we need to.
                if(!CharUtils.isUncased((String) name)) {
                    
                    //
                    // This is only the lowercase version, so the lowercase
                    // entry is the new entry.
                    lce = e;
                }
            } else {
                
                //
                // This is not the lowercase version, so we may need to
                // make a new entry for that and that will be the lowercase
                // entry.
                lce = (IndexEntry) get(lc);
                if(lce == null) {
                    lce = simpleNewEntry(lc);
                    put(lc, lce);
                }
            }
            
            if(lce != null) {
                //
                // Set the pointers in both of the entries.
                ((CasedPostingsEntry) lce).setCaseInsensitiveEntry(lce);
                ((CasedPostingsEntry) e).setCaseInsensitiveEntry(lce);
            }
        }
        return e;
    }
    
    /**
     * Puts an entry into the dictionary.  This will assign an ID to the
     * entry if it is not already in the dictionary.
     *
     * @param name The name of the entry.
     * @param e The entry to put in the dictionary.
     * @return Any previous value stored in the dictionary under the name
     * of the given entry.
     */
    public IndexEntry put(Object name, IndexEntry e) {
        IndexEntry old = (IndexEntry) map.put(name, e);
        e.setDictionary(this);
        return old;
    }
    
    /**
     * Given a name, figure out how big it is in bytes.
     */
    protected static int getSize(Object name) {
        if(name instanceof String) {
            return ((String) name).length() * 2;
        }
        return 4;
    }
    
    /**
     * Gets an entry from the dictionary, given the name for the entry.
     *
     * @param name The name of the entry.
     * @return The entry associated with the name, or <code>null</code> if
     * the name doesn't appear in the dictionary.
     */
    public QueryEntry get(Object name) {
        return (QueryEntry) map.get(name);
    }
    
    /**
     * Deletes an entry from the dictionary, given the name for the entry.
     *
     * @param name The name of the entry.
     * @return The entry associated with the name, or <code>null</code> if
     * the name doesn't appear in the dictionary.
     */
    public Entry remove(Object name) {
        return map.remove(name);
    }
    
    /**
     * Gets the partition to which this dictionary belongs.
     * @return the partition
     */
    public Partition getPartition() {
        return part;
    }
    
    public void setPartition(Partition partition) {
        part = partition;
    }
    
    /**
     * Gets the number of entries in the dictionary.
     */
    public int size() {
        return map.size();
    }
    
    /**
     * Gets an iterator for the entries in the dictionary.
     *
     * @return An iterator for the entries in the dictionary.
     */
    public DictionaryIterator iterator() {
        return new MemoryDictionaryIterator();
    }
    
    /**
     * Clears the dictionary, emptying it of all data.
     */
    public void clear() {
        map.clear();
        id 	        = 0;
        idMap 	    = null;
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
     * @return The entries, in sorted order.
     */
    protected IndexEntry[] sort(Renumber renumber, IDMap idMapType) {
        IndexEntry[] entries = Util.sort(map.values().toArray(new IndexEntry[0]));
        
        //
        // Check if we need to keep an ID map.
        if(idMapType != IDMap.NONE) {
            idMap = new int[entries.length+1];
        }
        
        switch (renumber) {
            case NONE:
                break;
            case RENUMBER:
                switch (idMapType) {
                    case NONE:
                        for (int i = 0; i < entries.length;
                             i++) {
                            entries[i].setID(i + 1);
                        }
                        break;
                    case OLDTONEW:
                        for (int i = 0; i < entries.length;
                             i++) {
                            IndexEntry e = entries[i];
                            idMap[e.getID()] = i + 1;
                            e.setID(i + 1);
                        }
                    case NEWTOOLD:
                        for (int i = 0; i < entries.length;
                             i++) {
                            IndexEntry e = entries[i];
                            idMap[i + 1] = e.getID();
                            e.setID(i + 1);
                        }
                        break;
                }
        }
        return entries;
    }
    
    /**
     * Gets the largest ID in this dictionary as of the time the method
     * is called.
     *
     * @return the latest id given out
     */
    public int getMaxId() {
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
     * Prepares a dictionary for dumping.  This can be used by subclasses
     * to do anything that needs doing before dumping begins.
     *
     * @param sortedEntries entries from another dictionary.
     */
    public void dumpPrepare(IndexEntry[] sortedEntries) {
    }
    
    /**
     * Dumps the dictionary and the associated postings to files.  Once the
     * dumping is complete, the pointer in the file must be pointing to a
     * position *after* the data just written.  This is so that we may dump
     * multiple dictionaries and postings types to the same channel.
     *
     * @param path The path to the directory where the dictionary should be
     * dumped.
     * @param encoder An encoder for the names of the entries.
     * @param dictFile The file where the dictionary will be dumped.
     * @param postOut The place where the postings will be dumped.
     * @param renumber How entries should be renumbered at dump time.
     * @param idMap what kind of map from old to new IDs should be kept
     * @param postIDMap A map from old IDs used in the postings to new IDs.
     * This map will be given to the postings from the dictionary before
     * they are dumped to disk, allowing the postings to be remapped before
     * the dump.  This is useful when the postings in one dictionary
     * contain IDs that have been remapped during a dump operation, such as
     * those in a document dictionary.  If this value is <code>null</code>,
     * no remapping will take place.
     * @return An array of entries in the order that they were dumped.
     * @throws java.io.IOException When there is an error writing either of
     * the channels.
     */
    public IndexEntry[] dump(String path,
            NameEncoder encoder,
            RandomAccessFile dictFile,
            PostingsOutput[] postOut,
            Renumber renumber,
            IDMap idMap,
            int[] postIDMap)
            throws java.io.IOException {
        return dump(path, encoder, null, dictFile, postOut, renumber, idMap, postIDMap);
    }
    
    /**
     * Dumps the dictionary and the associated postings to files.  Once the
     * dumping is complete, the pointer in the file must be pointing to a
     * position *after* the data just written.  This is so that we may dump
     * multiple dictionaries and postings types to the same channel.
     *
     * @param path The path to the directory where the dictionary should be
     * dumped.
     * @param encoder An encoder for the names of the entries.
     * @param partStats a set of partition statistics that we will
     * contribute to while dumping the dictionary.  May be <code>null</code>.
     * @param dictFile The file where the dictionary will be dumped.
     * @param postOut The place where the postings will be dumped.
     * @param renumber An integer indicating whether and how entries should
     * be renumbered at dump time.
     * @param postIDMap A map from old IDs used in the postings to new IDs.
     * This map will be given to the postings from the dictionary before
     * they are dumped to disk, allowing the postings to be remapped before
     * the dump.  This is useful when the postings in one dictionary
     * contain IDs that have been remapped during a dump operation, such as
     * those in a document dictionary.  If this value is <code>null</code>,
     * no remapping will take place.
     * @return An array of entries in the order that they were dumped.
     * @throws java.io.IOException When there is an error writing either of
     * the channels.
     */
    public IndexEntry[] dump(String path,
            NameEncoder encoder,
            PartitionStats partStats,
            RandomAccessFile dictFile,
            PostingsOutput[] postOut,
            Renumber renumber,
            IDMap idMapType,
            int[] postIDMap)
            throws java.io.IOException {
        log.log(logTag, 5, "Dumping: " + map.size() + " entries");
        
        //
        // Get a writer for the dictionary.  If we're not renumbering, it
        // will need to keep an ID to position map.
        DictionaryWriter dw = new DictionaryWriter(path,
                encoder,
                partStats,
                postOut.length,
                renumber);
        
        //
        // Set the max entry ID.
        dw.dh.maxEntryID = id;
        
        //
        // Record the starting positions for each of our postings outputs.
        for(int i = 0; i < postOut.length; i++) {
            dw.dh.postStart[i] = postOut[i].position();
        }
        
        //
        // Sort the entries in preparation for writing them out.  This will
        // generate an old-to-new ID mapping if we're renumbering.
        IndexEntry[] sorted = sort(renumber, idMapType);
        
        //
        // Write the postings and entries.
        for(int i = 0; i < sorted.length; i++) {
            
            //
            // Give subclasses a crack at the entry.
            processEntry(sorted[i]);
            
            
            //
            // Write it out.
            if (sorted[i].writePostings(postOut, postIDMap) == true) {
                dw.write(sorted[i]);
            }
        }
        
        //
        // Flush whatever's left in the postings buffers.
        for(int i = 0; i < postOut.length; i++) {
            postOut[i].flush();
            dw.dh.postEnd[i] = postOut[i].position();
        }
        
        //
        // Write the final dictionary.
        dw.finish(dictFile);
        
        return sorted;
    }
    
    /**
     * Processes a single entry before dumping it.  Does nothing at this
     * level.
     *
     * @param e the entry to process.
     */
    public void processEntry(IndexEntry e) {
    }
    
    /**
     * A class that implements a dictionary iterator for this dictionary.
     */
    public class MemoryDictionaryIterator implements DictionaryIterator {
        
        protected Iterator iter;
        
        protected boolean actualOnly;
        
        public MemoryDictionaryIterator() {
            iter = map.values().iterator();
        }
        
        public boolean hasNext() {
            return iter.hasNext();
        }
        
        public QueryEntry next() {
            return (QueryEntry) iter.next();
        }
        
        public void remove() {
            throw new
                    UnsupportedOperationException("Dictionary is read only");
        }
        
        public int estimateSize() {
            return 0;
        }
        
        public int getNEntries() {
            return map.size();
        }
        
        public void setActualOnly(boolean actualOnly) {
            this.actualOnly = actualOnly;
        }

        public QueryEntry get(Object name) {
            return (QueryEntry) map.get(name);
        }

        public QueryEntry get(int id) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }
}
