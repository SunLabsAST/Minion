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

package com.sun.labs.minion.classification;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.SinglePostingsEntry;

import com.sun.labs.minion.indexer.postings.Postings;

import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

import com.sun.labs.minion.util.buffer.WriteableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;

public class FeatureEntry extends SinglePostingsEntry {

    int infoSize;

    public FeatureEntry() {
        // Used by reflection
        this(null);
    } // FeatureEntry constructor
    
    public FeatureEntry(Object name) {
        super(name);
    } // FeatureEntry constructor
    
    public Entry getEntry(Object name) {
        return new FeatureEntry(name);
    }

    
    /**
     * Duplicates the contents of this entry, except for any postings.
     */
    public Entry getEntry() {
        FeatureEntry ne = (FeatureEntry) super.getEntry();
        ne.infoSize = infoSize;
        return ne;
    }

    /**
     * Gets the appropriate postings type for the class.  These postings
     * should be useable for indexing.
     *
     * @return Postings suitable for use when indexing.
     */
    protected Postings getPostings() {
        return new FeaturePostings();
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
        return new FeaturePostings(input.slice(0, size),
                                   input.slice(size, infoSize));
    }

    /**
     * Gets the maximum frequency in the postings associated with this
     * entry.  This is non-sensical for these postings, so we'll always
     * return 1.
     *
     * @return 1.
     */
    public int getMaxFDT() {
        return 1;
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
        n        = p.getN();
        size     = b[0].position() + b[1].position();
        infoSize = b[2].position();
        offset   = out[0].position();
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
        b.byteEncode(infoSize);
        b.byteEncode(id);
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
        infoSize = b.byteDecode();
        id = b.byteDecode();
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
        ReadableBuffer b = postIn[0].read(offset, size+infoSize);
        p = new FeaturePostings(b.slice(0, size),
                                b.slice(size, infoSize));
    }

} // FeatureEntry
