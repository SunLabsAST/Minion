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
package com.sun.labs.minion.indexer.entry;

import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.Occurrence;

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.util.logging.Logger;

/**
 * A class for holding cased dictionary entries.
 */
public abstract class CasedEntry extends BaseEntry implements CasedPostingsEntry {

    /**
     * A reference to the case insensitive postings that are associated
     * with this entry.
     */
    protected IndexEntry ciEntry;

    /**
     * Postings for the case sensitive and case insensitive versions of
     * saved field values.
     */
    protected Postings[] p;

    /**
     * The number of each type of postings.
     */
    protected int[] n;

    /**
     * The size of each kind of postings.
     */
    protected int[] size;

    /**
     * The total size of the postings.
     */
    protected int totalSize;

    /**
     * The offset of the case sensitive postings.
     */
    protected long[] offset;

    /**
     * A log.
     */
    Logger logger = Logger.getLogger(getClass().getName());

    /**
     * A tag.
     */
    protected static String logTag = "CSFE";

    /**
     * Creates a cased postings entry.
     */
    public CasedEntry() {
        this(null);
    }

    /**
     * Creates a cased postings entry.
     */
    public CasedEntry(Object name) {
        super(name);
        init();
    }

    /**
     * Initializes the arrays containing postings, sizes, etc.
     */
    protected abstract void init();

    /**
     * Initializes postings at a given position in the postings array.
     */
    protected abstract void initPostings(int pos);

    //
    // Remaining implementation of Entry.
    /**
     * Gets a new entry that contains a copy of the data in the given
     * entry.
     *
     * @return a new entry.
     */
    public Entry getEntry() {
        CasedEntry ne = (CasedEntry) getEntry(name);
        ne.id = id;
        ne.postIn = postIn;
        ne.dict = dict;
        ne.n = new int[n.length];
        ne.size = new int[size.length];
        ne.offset = new long[offset.length];
        for(int i = 0; i < n.length; i++) {
            ne.n[i] = n[i];
            ne.size[i] = size[i];
            ne.offset[i] = offset[i];
        }
        return ne;
    }

    /**
     * Adds an occurrence to this index entry.
     *
     * @param o The occurrence to add.
     * 
     */
    public void add(Occurrence o) {
        addCaseSensitive(o);
        if(ciEntry != null) {
            ((CasedPostingsEntry) ciEntry).addCaseInsensitive(o);
        }
    }

    /**
     * Adds an occurrence to the postings at a given position.
     */
    protected void add(Occurrence o, int pos) {
        if(p[pos] == null) {
            initPostings(pos);
        }
        p[pos].add(o);
    }

    /**
     * Returns the number of channels needed to store the postings for this
     * entry type.
     */
    public int getNumChannels() {
        return 1;
    }

    /**
     * Encodes any information associated with the postings onto the given
     * buffer.
     *
     * @param b The buffer onto which the postings information should be
     * encoded.  The buffer will be positioned to the correct spot for the
     * encoding.
     */
    public void encodePostingsInfo(WriteableBuffer b) {
        for(int i = 0; i < p.length; i++) {
            b.byteEncode(n[i]);
            b.byteEncode(size[i]);
            b.byteEncode(offset[i]);
        }
    }

    /**
     * Decodes the postings information associated with this entry.
     *
     * @param b The buffer containing the encoded postings information.
     * @param pos The position in <code>b</code> where the postings
     * information can be found.
     */
    public void decodePostingsInfo(ReadableBuffer b, int pos) {
        if(p == null) {
            init();
        }
        b.position(pos);
        for(int i = 0; i < n.length; i++) {
            n[i] = b.byteDecode();
            size[i] = b.byteDecode();
            offset[i] = b.byteDecodeLong();
        }
    }

    //
    // Implementation of CasedPostingsEntry.
    /**
     * Sets the case insensitive entry for this entry.
     */
    public void setCaseInsensitiveEntry(IndexEntry e) {
        ciEntry = e;
    }

    /**
     * Gets the case insensitive entry for this entry.
     */
    public Entry getCaseInsensitiveEntry() {
        return ciEntry;
    }
} // CasedEntry
