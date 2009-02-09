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

import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.IDPostings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.Occurrence;

import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.util.logging.Level;

/**
 * A class for storing ID only postings for dictionary entries that need to
 * store case sensitive and case insensitive postings.  Such entries can be
 * used for bigram dictionaries or for character saved fields that need to
 * be searched case insensitively.
 *
 */
public class CasedIDEntry extends CasedEntry {

    protected static final int CS = 0;

    protected static final int CI = 1;

    /**
     * Creates an entry of this type with no name.
     */
    public CasedIDEntry() {
        this(null);
    } // CasedIDEntry constructor

    /**
     * Creates an entry of this type.
     *
     * @param name The name of the entry.
     */
    public CasedIDEntry(Object name) {
        super(name);
    } // CasedIDEntry constructor

    public Entry getEntry(Object name) {
        return new CasedIDEntry(name);
    }

    protected void init() {
        p = new IDPostings[2];
        n = new int[2];
        size = new int[2];
        offset = new long[2];
    }

    /**
     * Creates a set of postings for indexing or merging.
     *
     * @param pos The position in the array of postings where we'll start
     * creating postings.
     */
    protected void initPostings(int pos) {
        p[pos] = new IDPostings();
    }

    //
    // Implementation of IndexEntry.
    /**
     * Returns the number of channels needed to store the postings for this
     * entry type.
     */
    public int getNumChannels() {
        return 1;
    }

    /**
     * Returns the maximum frequency encoded in the postings, which is
     * always 1 for ID only postings.
     */
    public int getMaxFDT() {
        return 1;
    }

    /**
     * Returns the total number of occurrences for these postings, which is
     * just the number of postings for ID only postings.
     */
    public long getTotalOccurrences() {
        return p[CI] == null ? n[CS] : n[CI];
    }

    /**
     * Writes the postings associated with this entry to some or all of the
     * given channels.
     *
     * @param out The outputs to which we will write the postings.
     * @param idMap A map from the IDs currently used in the postings to
     * the IDs that should be used when the postings are written to disk.
     * This may be <code>null</code>, in which case no remapping will
     * occur.
     * @return true if postings were written, false otherwise
     * @throws java.io.IOException if there is any error writing the
     * postings.
     */
    public boolean writePostings(PostingsOutput[] out,
            int[] idMap)
            throws java.io.IOException {
        boolean postingsWritten = false;
        for(int i = 0; i < p.length; i++) {

            if(p[i] == null) {
                continue;
            }

            p[i].finish();
            p[i].remap(idMap);

            n[i] = p[i].getN();
            if(n[i] == 0) {
                continue;
            }
            offset[i] = out[0].position();
            WriteableBuffer[] b = p[i].getBuffers();
            if(p[i].getN() == 0) {
                p[i] = null;
                continue;
            }
            postingsWritten = true;
            size[i] = (int) out[0].write(b);
        }
        return postingsWritten;
    }

    /**
     * Appends the postings from another entry onto this one.  This is used
     * when merging dictionaries.
     * 
     * @param qe The entry that we want to append onto this one.
     * @param start The new starting ID for the partition that the entry
     * was drawn from.
     * @param idMap A map from old IDs in the given postings to new IDs
     * with gaps removed for deleted data.  If this is <code>null</code>,
     * then there are no deleted documents.
     */
    public void append(QueryEntry qe, int start, int[] idMap) {

        CasedIDEntry cie = (CasedIDEntry) qe;

        for(int i = 0; i < p.length; i++) {
            if(cie.p[i] == null) {
                continue;
            }

            if(p[i] == null) {
                initPostings(i);
            }

            p[i].append(cie.p[i], start, idMap);
        }
    }

    //
    // Implementation of QueryEntry.
    /**
     * Decodes the postings information associated with this entry.
     *
     * @param b The buffer containing the encoded postings information.
     * @param pos The position in <code>b</code> where the postings
     * information can be found.
     */
    public void decodePostingsInfo(ReadableBuffer b, int pos) {
        if(p == null) {
            p = new IDPostings[2];
            n = new int[2];
            size = new int[2];
            offset = new long[2];
        }
        b.position(pos);
        for(int i = 0; i < n.length; i++) {
            n[i] = b.byteDecode();
            size[i] = b.byteDecode();
            offset[i] = b.byteDecodeLong();
        }
    }

    /**
     * Reads the postings data for this entry.  This method must load all
     * available postings information, since this will be used at
     * dictionary merge time to append postings from one entry onto
     * another.
     *
     * @throws java.io.IOException if there is any error reading the
     * postings.
     */
    public void readPostings(int pos) throws java.io.IOException {
        if(size[pos] == 0) {
            return;
        }
        if(p[pos] == null) {
            p[pos] = new IDPostings(postIn[0].read(offset[pos],
                    size[pos]));
        }
    }

    /**
     * Reads the postings data for this entry.  This method must load all
     * available postings information, since this will be used at
     * dictionary merge time to append postings from one entry onto
     * another.
     *
     * @throws java.io.IOException if there is any error reading the
     * postings.
     */
    public void readPostings() throws java.io.IOException {
        readPostings(CS);
        readPostings(CI);
    }

    /**
     * Gets the number of postings associated with this entry.  This is
     * used to sort entries by their frequency during query operations.
     *
     * @return The number of postings associated with this entry.
     */
    public int getN() {
        return Math.max(n[0], n[1]);
    }

    public boolean hasPositionInformation() {
        return false;
    }

    public boolean hasFieldInformation() {
        return false;
    }

    /**
     * Gets an iterator that will iterate through a set of postings
     * associated with this entry.  These postings will be unfielded.
     *
     * @param features The features that the iterator must support
     * @return An iterator for the postings.
     */
    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        QueryStats qs = features == null ? null : features.getQueryStats();
        try {
            if(qs != null) {
                qs.postReadW.start();
            }
            if(features == null) {
                if(size[CI] == 0) {
                    return null;
                }
                if(qs != null) {
                    qs.postingsSize += size[CI];
                }
                readPostings(CI);
                return p[CI].iterator(features);
            }

            if(features.getCaseSensitive() || CharUtils.isUncased(
                    name.toString())) {
                if(size[CS] == 0) {
                    return null;
                }
                if(qs != null) {
                    qs.postingsSize += size[CS];
                }
                readPostings(CS);
                return p[CS].iterator(features);
            } else {
                if(size[CI] == 0) {
                    return null;
                }
                if(qs != null) {
                    qs.postingsSize += size[CI];
                }
                readPostings(CI);
                return p[CI].iterator(features);
            }
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error reading postings for " + name,
                    ioe);
            return null;
        } finally {
            if(qs != null) {
                qs.postReadW.stop();
            }
        }
    }

    //
    // Implementation of CasedPostingsEntry
    /**
     * Adds an occurrence to the case sensitive postings for this entry.
     *
     * @param o The occurrence to add.
     */
    public void addCaseSensitive(Occurrence o) {
        add(o, CS);
    }

    /**
     * Adds an occurrence to the case insensitive postings for this entry.
     *
     * @param o The occurrence to add.
     */
    public void addCaseInsensitive(Occurrence o) {
        add(o, CI);
    }

    /**
     * Gets the postings that are case sensitive.
     */
    public Postings getCaseSensitivePostings() {
        return p[CS];
    }

    /**
     * Gets the postings that are case insenstive.
     */
    public Postings getCaseInsensitivePostings() {
        return p[CI];
    }

    /**
     * Indicates whether the name of this entry actually occurred in the data,
     * that is, whether it has any case sensitive postings.
     */
    public boolean nameOccurred() {
        return n[CS] > 0;
    }

    public int getNCaseInsensitive() {
        return n[CI];
    }
} // CasedIDEntry
