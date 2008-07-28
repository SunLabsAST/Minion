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


import com.sun.labs.minion.indexer.postings.DFOPostings;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.Occurrence;

import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import com.sun.labs.minion.util.CharUtils;

public class CasedDFOEntry extends CasedEntry {

    protected static final int CS = 0;

    protected static final int CI = 1;

    protected static String logTag = "CDFOE";

    /**
     * The maximum term frequencies.
     */
    protected int[] maxfdt;

    /**
     * The total number of occurrences.
     */
    protected long[] to;

    /**
     * The sizes of the data including fields and positions data.
     */
    protected int[] fnpSize;

    /**
     * Creates an entry of this type with no name.
     */
    public CasedDFOEntry() {
        this(null);
    } // CasedIDEntry constructor
    
    /**
     * Creates an entry of this type.
     *
     * @param name The name of the entry.
     */
    public CasedDFOEntry(Object name) {
        super(name);
    } // CasedIDEntry constructor

    public Entry getEntry(Object name) {
        return new CasedDFOEntry(name);
    }
    
    /**
     * Duplicates the contents of this entry, except for any postings.
     */
    public Entry getEntry() {
        CasedDFOEntry ne = (CasedDFOEntry) super.getEntry();
        ne.fnpSize = new int[fnpSize.length];
        for(int i = 0; i < fnpSize.length; i++) {
            ne.fnpSize[i] = fnpSize[i];
        }
        ne.totalSize = totalSize;
        return ne;
    }

    protected void init() {
        p       = new DFOPostings[2];
        n       = new int[2];
        maxfdt  = new int[2];
        to      = new long[2];
        size    = new int[2];
        fnpSize = new int[2];
        offset  = new long[2];
    }
    
    /**
     * Creates a set of postings for indexing or merging.
     *
     * @param pos The position in the array of postings where we'll start
     * creating postings.
     */
    protected void initPostings(int pos) {
        p[pos] = new DFOPostings();
    }
    
    //
    // Implementation of IndexEntry.

    /**
     * Gets a new instance of the implementing class.
     *
     * @param name The name to give the new instance.
     */
    public IndexEntry getInstance(Object name) {
        return new CasedDFOEntry(name);
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
            b.byteEncode(maxfdt[i]);
            b.byteEncode(to[i]);
            b.byteEncode(size[i]);
            b.byteEncode(fnpSize[i]);
            b.byteEncode(offset[i]);
        }
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

            //
            // Collect our postings statistics, taking note when we have an
            // empty postings list!
            n[i] = p[i].getN();
            if(n[i] == 0) {
                continue;
            }

            if(maxfdt[i] == 0) {
                maxfdt[i] = p[i].getMaxFDT();
                to[i] = p[i].getTotalOccurrences();
            }
            
            WriteableBuffer[] b = p[i].getBuffers();
            if (p[i].getN() == 0) {
                p[i] = null;
                continue;
            }
            postingsWritten = true;
            //
            // Get the offset of the postings.
            offset[i] = out[0].position();

            //
            // Consider the postings that have DFO data.
            size[i] = b[0].position() + b[1].position();

            //
            // Consider the postings that have field and position data.
            fnpSize[i] = size[i] + b[2].position();

            //
            // Write all of the data.
            out[0].write(b);
            p[i] = null;
        }
        return postingsWritten;
    }

    /**
     * Gets the maximum fdt value from these postings.  We'll take the
     * value from the case insensitive postings, if we have them.
     */
    public int getMaxFDT() {
        return n[CI] == 0 ? maxfdt[CS] : maxfdt[CI];
    }

    /**
     * Gets the total number of occurrences in these postings.  We'll take
     * the value from the case insensitive postings, if we have them.
     */
    public long getTotalOccurrences() {
        return n[CI] == 0 ? to[CS] : to[CI];
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

        CasedDFOEntry cie = (CasedDFOEntry) qe;
        
        for(int i = 0; i < p.length; i++) {

            if(cie.p[i] == null) {
                continue;
            }

            if(p[i] == null) {
                initPostings(i);
            }

            p[i].append(cie.p[i], start, idMap);

            //
            // Keep track of our postings stats as we go.
            if(idMap == null) {
                to[i] += cie.to[i];
                if(to[i] < 0) {
                    log.warn(logTag, 3, "Exceeded long on " + name + " clamping to max");
                    to[i] = Long.MAX_VALUE;
                }
                maxfdt[i] = Math.max(maxfdt[i], cie.maxfdt[i]);
            } else {
                maxfdt[i] = p[i].getMaxFDT();
                to[i] = p[i].getTotalOccurrences();
                if(to[i] < 0) {
                    log.warn(logTag, 3, "Exceeded long on " + name + " clamping to max");
                    to[i] = Long.MAX_VALUE;
                }
            }
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
            p      = new DFOPostings[2];
            n      = new int[2];
            maxfdt = new int[2];
            to     = new long[2];
            size   = new int[2];
            offset = new long[2];
        }
        b.position(pos);
        for(int i = 0; i < n.length; i++) {
            n[i]        = b.byteDecode();
            maxfdt[i]   = b.byteDecode();
            to[i]       = b.byteDecodeLong();
            size[i]     = b.byteDecode();
            fnpSize[i]  = b.byteDecode();
            totalSize  += fnpSize[i];
            offset[i]   = b.byteDecodeLong();
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

            //
            // Create the postings with both buffers.
            p[pos] = new DFOPostings(postIn[0].read(offset[pos],
                                                    fnpSize[pos]),
                                     0, size[pos], fnpSize[pos]);
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

        if(totalSize == 0) {
            return;
        }

        //
        // Do one read and divide the data.  We'll read from the first
        // offset that has data and read the total size necessary.  Then
        // we'll instantiate the different postings as necessary.
        ReadableBuffer all = postIn[0].read(size[CS] > 0 ? offset[CS] : offset[CI],
                                            totalSize);
        

        if(size[CS] > 0) {
            p[CS] = new DFOPostings(all, 0,
                                    size[CS], fnpSize[CS]);
        }

        if(size[CI] > 0) {
            p[CI] = new DFOPostings(all,
                                    fnpSize[CS], size[CI],
                                    fnpSize[CI]);
        }
    }

    /**
     * Gets the number of postings associated with this entry.  This is
     * used to sort entries by their frequency during query operations.
     *
     * @return The number of postings associated with this entry.
     */
    public int getN() {
        return Math.max(n[CS], n[CI]);
    }

    public boolean hasPositionInformation() {
        return true;
    }

    public boolean hasFieldInformation() {
        return true;
    }
    
    /**
     * Gets an iterator that will iterate through a set of postings
     * associated with this entry.  
     *
     * @param features The features that the iterator must support
     * @return An iterator for the postings.
     */
    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        try {

            int which = CI;

            if(features != null) {
                if(features.getCaseSensitive() ||
                   CharUtils.isUncased((String) name)) {
                    which = CS;
                }
            }

            //
            // If we need field or position data, then load that.
            if(features != null && (features.getPositions() ||
                                    features.getFields() != null ||
                                    features.getMult() != null)) {
                readPostings(which);
            } else {

                //
                // We only need the DFO data.
                if(p[which] == null && size[which] > 0) {
                    p[which] =
                        new DFOPostings(postIn[0].read(offset[which],
                                                      size[which]));
                }
            }

            if(p[which] != null) {
                return p[which].iterator(features);
            } else {
                return null;
            }
        } catch (java.io.IOException ioe) {
            log.error(logTag, 1, "Error reading postings for " + name,
                      ioe);
            return null;
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

} // CasedDFOEntry
