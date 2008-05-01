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

import com.sun.labs.minion.indexer.entry.CasedPostingsEntry;

import com.sun.labs.minion.util.CharUtils;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.IDFreqEntry;
import com.sun.labs.minion.indexer.entry.IndexEntry;

import com.sun.labs.minion.indexer.postings.OccurrenceImpl;

public class MemoryBiGramDictionary extends MemoryDictionary {
    
    protected static String logTag = "MBG";
    
    /**
     * Creates a bigram dictionary.  Such a dictionary will be populated a
     * term at a time.
     */
    public MemoryBiGramDictionary() {
        super(IDFreqEntry.class);
    } // MemoryBiGramDictionary constructor
    
    /**
     * Creates and populates a bigram dictionary from a set of dictionary
     * entries.  Note that the names of these entries are assumed to be
     * instances of <code>String</code>.
     *
     * @param e Entries whose names should be added to the dictionary.
     */
    public MemoryBiGramDictionary(Entry[] e) {
        super(IDFreqEntry.class);
        for(Entry x : e) {
            
            //
            // If this is a cased postings entry for a term that did not occur
            // in the data, then we won't bother adding bigrams, since that 
            // will be covered by the variations of this term that did occur.
            if((x instanceof CasedPostingsEntry) &&
                    !((CasedPostingsEntry) x).nameOccurred()) {
                continue;
            }
            add(x);
        }
    } // MemoryBiGramDictionary constructor
    
    /**
     * Adds an entry from another dictionary to the bigram dictionary.
     * This will break the name of the entry into bigrams and add those
     * bigrams to the dictionary.
     *
     * @param entry The entry whose name should be added to the bigram
     * dictionary.
     */
    public void add(Entry entry) {
        char[] bgc = new char[2];
        
        //
        // This is an occurrence that we'll use for all the bigrams in the
        // name of this entry.
        OccurrenceImpl o = new OccurrenceImpl(entry.getID());
        
        //
        // Iterate over the current term, making a series of overlapping
        // bigrams.
        bgc[0]   = (char) 0;
        String n = CharUtils.toLowerCase(entry.getName().toString());
        int    e = n.length();
        for(int i = 0; i <= e; i++) {
            
            bgc[1] = i == e ? (char) 0 : n.charAt(i);
            
            String bigram = new String(bgc);
            
            //
            // Add a bigram occurence for this ID.
            IndexEntry bge = (IndexEntry) get(bigram);
            if(bge == null) {
                bge = newEntry(bigram);
                put(bigram, bge);
            }
            
            //
            // Add an occurrence to the postings for this entry.
            bge.add(o);
            bgc[0] = bgc[1];
        }
    }
    
} // MemoryBiGramDictionary
