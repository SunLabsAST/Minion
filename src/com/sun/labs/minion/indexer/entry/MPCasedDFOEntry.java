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

import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

public class MPCasedDFOEntry extends CasedDFOEntry {

    public MPCasedDFOEntry() {
        super(null);
    }

    public MPCasedDFOEntry(Object name) {
        super(name);
    } // MPCasedDFOEntry constructor
    
    public Entry getEntry(Object name) {
        return new MPCasedDFOEntry(name);
    }
    
    /**
     * Duplicates the contents of this entry, except for any postings.
     */
    public Entry getEntry() {
        MPCasedDFOEntry ne = (MPCasedDFOEntry) getEntry(name);
        ne.id            = id;
        ne.postIn        = postIn;
        ne.dict          = dict;
        ne.n             = new int[n.length];
        ne.size          = new int[size.length];
        ne.offset        = new long[offset.length];
        for(int i = 0; i < n.length; i++) {
            ne.n[i]        = n[i];
            ne.size[i]     = size[i];
            ne.size[i+2]   = size[i+2];
            ne.offset[i]   = offset[i];
            ne.offset[i+2] = offset[i+2];
        }
        return ne;
    }

    protected void init() {
        p         = new DFOPostings[2];
        n         = new int[2];
        size      = new int[4];
        offset    = new long[4];
    }
    
    /**
     * Returns the number of channels needed to store the postings for this
     * entry type.
     */
    public int getNumChannels() {
        return 2;
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
            b.byteEncode(size[i+2]);
            b.byteEncode(offset[i]);
            b.byteEncode(offset[i+2]);
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

            n[i] = p[i].getN();
            if(n[i] == 0) {
                continue;
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
            offset[i+2] = out[1].position();

            //
            // Consider the postings that have DFO data.
            size[i] = b[0].position() + b[1].position();
            size[i+2] = b[2].position();

            //
            // Write all of the data.
            out[0].write(b[0]);
            out[0].write(b[1]);
            out[1].write(b[2]);
            p[i] = null;
        }
        return postingsWritten;
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
            n[i]        = b.byteDecode();
            size[i]     = b.byteDecode();
            size[i+2]   = b.byteDecode();
            offset[i]   = b.byteDecodeLong();
            offset[i+2] = b.byteDecodeLong();
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
                                                    size[pos]),
                                     postIn[1].read(offset[pos+2],
                                                    size[pos+2]));
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

} // MPCasedDFOEntry
