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
import java.io.RandomAccessFile;


import com.sun.labs.minion.indexer.entry.IndexEntry;


import java.util.logging.Logger;

/**
 * A class that will write a dictionary to a file.  This can be used when
 * dumping or merging dictionaries.
 * @param <N> the type of the names that we'll be writing.
 */
public class DictionaryWriter<N extends Comparable> {
    
    private static final Logger logger = Logger.getLogger(DictionaryWriter.class.getName());

    private DictionaryOutput dictOut;

    /**
     * Creates a dictionary writer that will write data to disk.
     *
     * @param path The path where the temporary files should be written.
     * @param encoder An encoder for the names of the entries.
     * @param nChans The number of postings channels used by the
     * dictionary.
     * @param renumber A flag indicating how entries in the dictionary were
     * renumbered during sorting.  We only care about {@link
     * MemoryDictionary.Renumber#NONE}, value which indicates to us that we
     * need to keep a map from entry ID to position in the dictionary.
     * @throws java.io.IOException if there was an error writing to disk
     */
    public DictionaryWriter(DictionaryOutput dictOut,
            NameEncoder<N> encoder,
            int nChans,
            MemoryDictionary.Renumber renumber)
            throws java.io.IOException {

        this.dictOut = dictOut;
        dictOut.start(encoder, renumber, nChans);
        
    } // DictionaryWriter constructor

    /**
     * Writes an entry to the dictionary.
     * @param e the entry to write
     */
    public void write(IndexEntry<N> e) {
        dictOut.write(e);
    }

    /**
     * Finishes by writing the dictionary to the given file.
     * @param dictFile the file where the complete dictionary will be written
     */
    public void finish(RandomAccessFile dictFile)
            throws java.io.IOException {
        
        dictOut.finish();
    }
} // DictionaryWriter
