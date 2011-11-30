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

import com.sun.labs.minion.util.CharUtils;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.IndexEntry;

import com.sun.labs.minion.indexer.postings.OccurrenceImpl;

public class MemoryBiGramDictionary extends MemoryDictionary {
    
    protected static String logTag = "MBG";
    
    /**
     * Creates a bigram dictionary.  Such a dictionary will be populated a
     * term at a time.
     */
    public MemoryBiGramDictionary(EntryFactory factory) {
        super(factory);
    } 
    
    /**
     * Adds an entry from another dictionary to the bigram dictionary.
     * This will break the name of the entry into bigrams and add those
     * bigrams to the dictionary.  Note that this method assumes that the
     * names of the entries passed in are strings.
     *
     * @param name the name to extract bigrams from
     * @param id the id of the term in the parent dictionary.
     */
    public void add(String name, int id) {
        char[] bgc = new char[2];
        
        //
        // This is an occurrence that we'll use for all the bigrams in the
        // name of this entry.
        OccurrenceImpl o = new OccurrenceImpl(id);
        
        //
        // Iterate over the current term, making a series of overlapping
        // bigrams.
        bgc[0]   = (char) 0;
        int    e = name.length();
        for(int i = 0; i <= e; i++) {
            
            bgc[1] = i == e ? (char) 0 : name.charAt(i);
            
            String bigram = new String(bgc);
            
            //
            // Add a bigram occurence for this ID.
            IndexEntry bge = put(bigram);
            
            //
            // Add an occurrence to the postings for this entry.
            bge.add(o);
            bgc[0] = bgc[1];
        }
    }
} // MemoryBiGramDictionary
