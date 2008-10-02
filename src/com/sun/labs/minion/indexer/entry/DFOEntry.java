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
import com.sun.labs.minion.indexer.postings.DFOPostings;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;

import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

import com.sun.labs.minion.util.buffer.WriteableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;

public class DFOEntry extends SinglePostingsEntry {

    /**
     * The total number of occurrences in the postings list associated with
     * this entry.
     */
    protected long to;

    /**
     * The maximum frequency of the term across the documents in the
     * postings list.
     */
    protected int maxfdt;

    /**
     * The size of the data including fields and positions data.
     */
    protected int fnpSize;
    
    public DFOEntry() {
        super(null);
    } // DFOEntry constructor
    
    public DFOEntry(Object name) {
        super(name);
    } // DFOEntry constructor

    public Entry getEntry(Object name) {
        return new DFOEntry(name);
    }
    
    /**
     * Gets a new entry that contains a copy of the data in this entry.
     *
     * @return a new entry.
     * @throws ClassCastException if the provided entry is not of type
     * <code>DFOEntry</code>
     */
    public Entry getEntry() {
        DFOEntry de = (DFOEntry) super.getEntry();
        de.to = to;
        de.maxfdt = maxfdt;
        de.fnpSize = fnpSize;
        return de;
    }

    /**
     * Gets a new instance of the implementing class.
     *
     * @param name The name to give the new instance.
     */
    public IndexEntry getInstance(Object name) {
        return new DFOEntry(name);
    }
    
    /**
     * Gets the appropriate postings type for the class.  These postings
     * should be useable for indexing.
     *
     * @return Postings suitable for use when indexing.
     */
    protected Postings getPostings() {
        return new DFOPostings();
    }

    /**
     * Reads the postings for this class, returning a set of postings
     * useful at query time.
     *
     * @param input The buffer containing the postings read from the
     * postings file.
     * @return The postings for this entry.
     */
    protected Postings getPostings(ReadableBuffer input) {
        return new DFOPostings(input);
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
        if(p == null) {
            return false;
        }

        //
        // Finish any ongoing encoding in our postings entry.
        p.finish();

        //
        // Possibly remap the data and get a buffer to write.
        p.remap(idMap);
        
        WriteableBuffer[] b = p.getBuffers();
        if (p.getN() == 0) {
            return false;
        }
        //
        // Set the elements of the term information, so that they can
        // be encoded later.
        n = p.getN();
        to = p.getTotalOccurrences();
        maxfdt = p.getMaxFDT();

        //
        // Get the offset of the postings.
        offset = out[0].position();

        //
        // Consider the postings that have DFO data.
        size = b[0].position() + b[1].position();

        //
        // Consider the postings that have field and position data.
        fnpSize = size + b[2].position();

        out[0].write(b);
        return true;
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
        super.encodePostingsInfo(b);
        b.byteEncode(to);
        b.byteEncode(maxfdt);
        b.byteEncode(fnpSize);
    }

    /**
     * Decodes the postings information associated with this entry.
     *
     * @param b The buffer containing the encoded postings information.
     * @param pos The position in <code>b</code> where the postings
     * information can be found.
     */
    public void decodePostingsInfo(ReadableBuffer b, int pos) {
        super.decodePostingsInfo(b, pos);
        to = b.byteDecodeLong();
        maxfdt = b.byteDecode();
        fnpSize = b.byteDecode();
        tsize += fnpSize;
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
        if(size == 0) {
            return;
        }
        p = new DFOPostings(postIn[0].read(offset, fnpSize),
                            0, size, fnpSize);
    }

    /**
     * Gets the total number of occurrences associated with this entry.
     * For this base class, this is the same as n.
     *
     * @return The total number of occurrences associated with this entry.
     */
    public long getTotalOccurrences() {
        return to;
    }

    /**
     * Gets the maximum frequency in the postings associated with this
     * entry.  For this base class, this is always 1.
     */
    public int getMaxFDT() {
        return maxfdt;
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
        QueryStats qs = features == null ? null : features.getQueryStats();
        try {

            if(qs != null) {
                qs.postReadW.start();
            }
            //
            // If we need field or position data, then load that.
            if(features != null && (features.getPositions() ||
                                    features.getFields() != null ||
                                    features.getMult() != null)) {
                if(qs != null) {
                    qs.postingsSize += fnpSize;
                }
                readPostings();
            } else {

                if (qs != null) {
                    qs.postingsSize += size;
                }
                //
                // We only need the DFO data.
                if(p == null && size > 0) {
                    p = new DFOPostings(postIn[0].read(offset, size));
                }
            }

            if(p != null) {
                return p.iterator(features);
            } else {
                return null;
            }
        } catch (java.io.IOException ioe) {
            log.error(logTag, 1, "Error reading postings for " + name,
                      ioe);
            return null;
        } finally {
            if(qs != null) {
                qs.postReadW.stop();
            }
        }
    }
} // DFOEntry
