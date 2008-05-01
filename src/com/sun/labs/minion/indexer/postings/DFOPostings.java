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

package com.sun.labs.minion.indexer.postings;


import java.util.Arrays;

import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.DescriptiveStats;

import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.Buffer;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import com.sun.labs.minion.util.Util;

/**
 * A postings class for storing IDs, frequencies, and field and word
 * position information.  The data is encoded into two buffers.
 *
 * <p>
 *
 * The first buffer contains document ID and frequency information for each
 * document and an offset into the second buffer where field and position
 * information is stored for a particular document.
 *
 * <ol>
 *
 * <li>The number of IDs in the postings is byte encoded.</li>
 *
 * <li>The last ID in the postings list is byte encoded.</li>
 *
 * <li>The last offset in the postings list is byte encoded.</li>
 *
 * <li>The number of skips in the skip table is byte encoded.</li>
 *
 * <li>The skip table.  The number of entries per skip is dependent on the
 * application.  The skip table has the following structure.
 *
 * <ol>
 *
 * <li>The number of skips is byte encoded.</li>
 *
 * <li>For each skip we encode:
 *
 * <ol>
 *
 * <li>The ID of skipped entry.  This is byte encoded as a delta from the
 * previous ID in the skip table</li>
 *
 * <li>The position in the encoded data to skip to.  This is byte encoded
 * as a delta from the previous position in the skip table.  Note that this
 * position is relative to the <em>end</em> of the skip table!</li>
 *
 * <li>The offset into the second buffer for this document.</li>
 *
 * </ol>
 *
 * </ol>
 *
 * <li>For each document we encode:
 *
 * <ol>
 *
 * <li>The ID of the document, byte encoded as a delta from the previous ID in
 * the postings list.</li>
 *
 * <li>The term frequency for this ID, byte encoded as is.</li>
 *
 * <li>The offset in the second buffer where the position and field
 * information can be found for this document.</li>
 *
 * </ol>
 *
 * </ol>
 *
 * <p>
 *
 * The second buffer contains encoded field and position information.  For
 * each document, the data is structured in the following way:
 *
 * <ol>
 *
 * <li>The number of fields for the document is byte encoded</li>
 *
 * <li>For each field we encode:
 *
 * <ol>
 *
 * <li>The field ID is byte encoded</li>
 *
 * <li>The number of occurrences of the field is byte encoded.</li>
 *
 * <li>The word position of each occurrence, byte encoded as a series of
 * deltas.
 *
 * </ol>
 *
 * </ol>
 *
 */
public class DFOPostings implements Postings {
    
    /**
     * The compressed document and frequency postings.
     */
    protected Buffer dfo;
    
    /**
     * The compressed field and position information.
     */
    protected Buffer fnp;
    
    /**
     * Whether we're building these postings by appending.
     */
    protected boolean appending;
    
    /**
     * The number of IDs in the postings.
     */
    protected int nIDs;
    
    /**
     * The total number of occurrences in the postings.
     */
    protected int to;
    
    /**
     * The maximum frequency encountered in the postings.
     */
    protected int maxfdt;
    
    /**
     * After getting the buffers, this member will contain the split point
     * between the buffers for the documents, frequencies, and offsets and
     * the buffers for the field and position information.
     */
    protected int splitPoint;
    
    /**
     * The previous ID encountered during indexing.
     */
    protected int prevID;
    
    /**
     * The last ID in this postings list.
     */
    protected int lastID;
    
    /**
     * The last positions offset in this postings list.
     */
    protected int lastOff;
    
    /**
     * The frequency of current ID.
     */
    protected int freq;
    
    /**
     * The number of fields in the current documents.
     */
    protected int nFields;
    
    /**
     * The field frequency information.
     */
    protected int[] ffreq;
    
    /**
     * The previous field positions.
     */
    protected int[] prevFPosn;
    
    /**
     * The field position information.
     */
    protected WriteableBuffer[] fposn;
    
    /**
     * The IDs in the skip table.
     */
    protected int[] skipID;
    
    /**
     * The offsets in the skip table.
     */
    protected int[] skipOff;
    
    /**
     * The positions in the skip table.
     */
    protected int[] skipPos;
    
    /**
     * The number of skips in the skip table.
     */
    protected int nSkips;
    
    /**
     * The position in the compressed representation where the data
     * starts.
     */
    protected int dataStart;
    
    /**
     * The number of documents in a skip.
     */
    protected int skipSize = 64;
    
    protected static MinionLog log = MinionLog.getLog();
    
    protected static String logTag = "DFOP";
    
    /**
     * Makes a postings entry that is useful during indexing.
     */
    public DFOPostings() {
        dfo       = new ArrayBuffer(256);
        fnp       = new ArrayBuffer(512);
        ffreq     = new int[16];
        prevFPosn = new int[16];
        fposn     = new WriteableBuffer[16];
    }
    
    /**
     * Makes a postings entry that is useful during querying.
     *
     * @param input the data read from a postings file.
     */
    public DFOPostings(ReadableBuffer input) {
        this(input, 0, 0, 0);
    }
    
    /**
     * Makes a postings entry that is useful during querying.
     *
     * @param input the data read from a postings file.
     * @param offset The offset in the buffer from which we should start
     * reading.  If this value is greater than 0, then we need to share the
     * bit buffer, since we may be part of a larger postings entry that
     * will need multiple readers.
     * @param size The size of the data in the sub-buffer.
     *
     */
    public DFOPostings(ReadableBuffer input, int offset, int size,
            int fnpSize) {
        
        ReadableBuffer b1;
        if(offset > 0  || size > 0) {
            b1 = input.slice(offset, size);
        } else {
            b1 = input;
        }
        
        ReadableBuffer b2 = null;
        if(fnpSize-size > 0) {
            b2 = input.slice(offset+size,
                    fnpSize-size);
        }
        init(b1, b2);
    }
    
    public DFOPostings(ReadableBuffer b1, ReadableBuffer b2) {
        init(b1, b2);
    }

    protected void init(ReadableBuffer b1, ReadableBuffer b2) {
        
        dfo = b1;
        fnp = b2;
        
        //
        // Get the initial data.
        nIDs = ((ReadableBuffer) dfo).byteDecode();
        lastID = ((ReadableBuffer) dfo).byteDecode();
        lastOff = ((ReadableBuffer) dfo).byteDecode();
        
        //
        // Decode the skip table.
        nSkips = ((ReadableBuffer) dfo).byteDecode();
        if(nSkips > 0) {
            
            skipID = new int[nSkips+1];
            skipPos = new int[nSkips+1];
            skipOff = new int[nSkips+1];
            int ci = 0;
            int cp = 0;
            int co = 0;
            for(int i = 1; i <= nSkips; i++) {
                ci += ((ReadableBuffer) dfo).byteDecode();
                cp += ((ReadableBuffer) dfo).byteDecode();
                co += ((ReadableBuffer) dfo).byteDecode();
                skipID[i] = ci;
                skipPos[i] = cp;
                skipOff[i] = co;
            }
            
            //
            // Now, the values for the bit positions of the entries are
            // from the *end* of the skip table, and we want them to be
            // from the beginning of the buffer so that we can use
            // BABuffer.seek to jump to them.  So we need to figure out
            // how much to add.
            dataStart = dfo.position();
            for(int i = 0; i <= nSkips; i++) {
                skipPos[i] += dataStart;
            }
        } else {
            dataStart = dfo.position();
        }
    }
    
    /**
     * Adds a skip to the skip table.
     *
     * @param id The ID that the skip is pointing to.
     * @param pos The position in the postings to skip to.
     */
    protected void addSkip(int id, int pos, int off) {
        if(skipID == null) {
            skipID = new int[4];
            skipPos = new int[4];
            skipOff = new int[4];
        } else if(nSkips + 1 >= skipID.length) {
            skipID = Util.expandInt(skipID, skipID.length*2);
            skipPos = Util.expandInt(skipPos, skipID.length);
            skipOff = Util.expandInt(skipOff, skipID.length);
        }
        skipID[nSkips] = id;
        skipPos[nSkips] = pos;
        skipOff[nSkips++] = off;
    }
    
    protected int encodeBasic() {
        
        //
        // Keep track of the postings stats.
        maxfdt = Math.max(freq, maxfdt);
        to += freq;
        
        //
        // Encode the ID, frequency, and offset information.
        return
                ((WriteableBuffer) dfo).byteEncode(lastID - prevID) +
                ((WriteableBuffer) dfo).byteEncode(freq) +
                ((WriteableBuffer) dfo).byteEncode(fnp.position() - lastOff);
    }
    
    /**
     * Encodes the data for a single ID.  This is a delta from the previous
     * ID, the frequency, and a delta from the previous position offset.
     */
    protected int encode() {
        
        //
        // Encode the ID, frequency and offset information.
        int nBytes = encodeBasic();
        
        //
        // Store the last position offset for these postings.
        lastOff = fnp.position();
        
        //
        // Encode the field information onto our fnp buffer.
        ((WriteableBuffer) fnp).byteEncode(nFields);
        for(int i = 0; i < ffreq.length; i++) {
            if(ffreq[i] > 0) {
                nBytes += ((WriteableBuffer) fnp).byteEncode(i) +
                        ((WriteableBuffer) fnp).byteEncode(ffreq[i]);
            }
        }
        
        //
        // Add the position information.
        for(int i = 0; i < ffreq.length; i++) {
            if(ffreq[i] > 0) {
                nBytes += fposn[i].position();
                ((WriteableBuffer) fnp).append(fposn[i].getReadableBuffer());
                fposn[i].clear();
                ffreq[i] = 0;
                prevFPosn[i] = 0;
            }
        }
        
        return nBytes;
    }
    
    /**
     * Sets the skip size.
     */
    public void setSkipSize(int size) {
        skipSize = size;
    }
    
    /**
     * Adds an occurrence to the postings list.
     *
     * @param o The occurrence.
     * 
     */
    public void add(Occurrence o) {
        
        FieldOccurrence fo = (FieldOccurrence) o;
        
        if(fo.getID() == lastID) {
            freq += fo.getCount();
            addFields(fo);
            return;
        }
        
        if(lastID != 0) {
            encode();
        }
        
        //
        // Reset for this document.
        freq    = fo.getCount();
        prevID  = lastID;
        lastID  = fo.getID();
        nFields = 0;
        
        //
        // Record the field information.
        addFields(fo);
        
        //
        // See if we need to add a skip.
        nIDs++;
        if(nIDs % skipSize == 0) {
            addSkip(lastID, dfo.position(), fnp.position());
        }
    }
    
    /**
     * Adds an occurrence to all relevant fields.
     * @param fo an occurrence that includes information about what fields are
     * currently active.
     */
    protected void addFields(FieldOccurrence fo) {
        
        int[] activeFields = fo.getFields();
        
        //
        // Add any field information that we have for this occurrence.
        if(activeFields[0] != 0) {
            add(0, fo);
        }
        
        //
        // Make sure our field information is long enough.
        if(ffreq.length <= activeFields.length) {
            ffreq = Util.expandInt(ffreq, activeFields.length);
            prevFPosn = Util.expandInt(prevFPosn, activeFields.length);
            WriteableBuffer[] temp = new WriteableBuffer[activeFields.length];
            System.arraycopy(fposn, 0, temp, 0, fposn.length);
            fposn = temp;
        }
        
        for(int i = 1; i < activeFields.length; i++) {
            if(activeFields[i] > 0) {
                add(i, fo);
            }
        }
    }
    
    /**
     * Adds an occurrence to a given field.
     * @param f the ID of the field to which we're adding an occurrence
     * @param fo the occurrence that we're adding to this field
     */
    private void add(int f, FieldOccurrence fo) {
        
        if(ffreq[f] == 0) {
            nFields++;
        }
        ffreq[f] += fo.getCount();
        if(fposn[f] == null) {
            fposn[f] = new ArrayBuffer(8);
        }
        
        fposn[f].byteEncode(fo.getPos() - prevFPosn[f]);
        prevFPosn[f] = fo.getPos();
    }
    
    /**
     * Gets the number of IDs in the postings list.
     * @return the number of IDs in the postings list.
     */
    public int getN() {
        return nIDs;
    }
    
    public int getLastID() {
        return lastID;
    }
    
    /**
     * Gets the maximum frequency in the postings list.
     */
    public int getMaxFDT() {
        return maxfdt;
    }
    
    /**
     * Gets the total number of occurrences in the postings list.
     */
    public int getTotalOccurrences() {
        return to;
    }
    
    /**
     * Finishes off the encoding by adding any data that we collected for
     * the last document.
     */
    public void finish() {
        
        //
        // Handle the data that we currently have.
        if(lastID != 0 && !appending) {
            encode();
        }
    }
    
    /**
     * Gets the size of the postings, in bytes.
     */
    public int size() {
        return dfo.position();
    }
    
    /**
     * Gets a <code>ByteBuffer</code> whose contents represent the
     * postings.  These buffers can safely be written to streams.
     *
     * The format is as follows:
     * NumIDs:LastID:NumSkipEntries[:skipID:skipPos]*:<PostingsData>
     *
     * @return A <code>ByteBuffer</code> containing the encoded postings
     * data.
     */
    public WriteableBuffer[] getBuffers() {
        
        WriteableBuffer temp = new ArrayBuffer((nSkips+1)*4 + 16);
        
        //
        // Encode the number of IDs and the last ID
        temp.byteEncode(nIDs);
        temp.byteEncode(lastID);
        temp.byteEncode(lastOff);
        
        //
        // Encode the skip table.
        temp.byteEncode(nSkips);
        int pi = 0;
        int pp = 0;
        int po = 0;
        
        for(int i = 0; i < nSkips; i++) {
            temp.byteEncode(skipID[i] - pi);
            temp.byteEncode(skipPos[i] - pp);
            temp.byteEncode(skipOff[i] - po);
            pi = skipID[i];
            pp = skipPos[i];
            po = skipOff[i];
        }
        
        return new WriteableBuffer[] {
            temp,
            (WriteableBuffer) dfo,
            (WriteableBuffer) fnp
        };
    }
    
    /**
     * Remaps the IDs in this postings list according to the given
     * old-to-new ID map.
     *
     * <p>
     *
     * This is tricky, because we can't assume that the remapped IDs will
     * maintain the order of the IDs, even if the IDs have changed.  Thus,
     * we need to uncompres all of the IDs and then put them back together.
     *
     * @param idMap A map from the IDs currently in use in the postings to
     * new IDs.
     */
    public void remap(int[] idMap) {
    }
    
    /**
     * Appends another set of postings to this one.
     *
     * @param p The postings to append.  Implementers can safely assume
     * that the postings being passed in are of the same class as the
     * implementing class.
     * @param start The new starting document ID for the partition
     * that the entry was drawn from.
     */
    public void append(Postings p, int start) {
        DFOPostings other = (DFOPostings) p;
        
        appending = true;
        
        //
        // Check for empty postings on the other side.
        if(other.nIDs == 0) {
            return;
        }
        
        //
        // What's the index of the next byte to be written to the postings?
        int dfoPos = dfo.position();
        int fnpPos = fnp.position();
        
        //
        // This is tricky, so pay attention: The skip positions for the
        // entry we're appending are based on the byte index from the
        // beginning of the encoded documents.  We're about to take off the
        // first document ID and reencode it as a delta, which may change
        // the number of bits that it requires (it will proabably take more
        // bytes, since it's likely to be a small number originally and now
        // it will be a delta between two larger numbers).  So, we need to
        // figure out the difference between the old number of bytes and
        // the new number of bytes.  This is the dadj variable.
        //
        // The offsets for the field and position data have a similar
        // problem, so we'll use the padj variable to calculate this.
        //
        // Start out by getting the first ID number from the other
        // postings, noting how many bytes it took to encode it.
        int dadj = other.dfo.position();
        int firstID = ((ReadableBuffer) other.dfo).byteDecode();
        dadj = other.dfo.position() - dadj;
        
        //
        // The new ID.
        int newID = firstID + start - 1;
        
        //
        // Get the frequency, which we'll use in a minute.
        int ofreq = ((ReadableBuffer) other.dfo).byteDecode();
        
        //
        // Now get the offset, noting how many bytes it takes.
        int padj = other.dfo.position();
        ((ReadableBuffer) other.dfo).byteDecode();
        padj = other.dfo.position() - padj;
        
        //
        // At this point, we want to decide whether we should put in a skip
        // at the start of the appended data.  If we never put in a skip
        // then a postings list that builds up gradually will never have
        // any (or only a few) skips, even though there might be thousands
        // of postings.  For now, we'll add a skip if the number of
        // postings currently in the entry doesn't show enough skips, given
        // our skipe size.
        if(nSkips < (nIDs /skipSize)) {
            addSkip(newID, dfoPos, fnpPos);
        }
        
        //
        // Now, reencode the data, noting how many bytes it takes to encode
        // the new ID and new offset.
        int dnb = ((WriteableBuffer) dfo)
        .byteEncode((firstID + start - 1) - lastID);
        ((WriteableBuffer) dfo)
        .byteEncode(ofreq);
        int pnb = ((WriteableBuffer) dfo).byteEncode(fnpPos - lastOff);
        
        //
        // Encode the new fnp offset and record the lastOffset.
        lastOff = fnpPos;
        
        //
        // Now we can finally calculate the real adjustment to the offsets
        // in the appended buffer of data.
        dadj = dnb - dadj;
        padj = pnb - padj;
        
        //
        // We can simply append the remaining dfo postings and the fnp data
        // from the other entry, if there is any.
        ((WriteableBuffer) dfo).append((ReadableBuffer) other.dfo);
        ((WriteableBuffer) fnp).append((ReadableBuffer) other.fnp);
        
        //
        // The last ID on this entry is now the last ID from the entry we
        // appended, suitably remapped.  The last offset is increased by
        // the given amount.
        lastID  = other.lastID + start - 1;
        
        //
        // Increment the number of documents in this new entry.
        nIDs += other.nIDs;
        
        if(other.nSkips > 0) {
            for(int i = 1; i <= other.nSkips; i++) {
                addSkip(other.skipID[i] + start - 1,
                        other.skipPos[i] - other.dataStart + dfoPos + dadj + padj,
                        other.skipOff[i] + fnpPos);
            }
        }
        
        //
        // The new last offset is derived from the last offset for the data
        // that we just added.
        lastOff = other.lastOff + fnpPos;
    }
    
    /**
     * Appends another set of postings to this one, removing any data
     * associated with deleted documents.
     *
     * @param p The postings to append.  Implementers can safely assume
     * that the postings being passed in are of the same class as the
     * implementing class.
     * @param start The new starting document ID for the partition
     * that the entry was drawn from.
     * @param idMap A map from old IDs in the given postings to new IDs
     * with gaps removed for deleted data.  If this is null, then there are
     * no deleted documents.
     */
    public void append(Postings p, int start, int[] idMap) {
        
        //
        // If there's no id mapping to be done, then do a simple append.
        if(idMap == null) {
            append(p, start);
            return;
        }
        
        DFOPostings other        = (DFOPostings) p;
        int         prevOff     = 0;
        boolean     copyNextTime = false;
        
        appending = true;
        
        //
        // Make sure there's enough capacity in our fields and positions buffer!
        ((WriteableBuffer) fnp).capacity(fnp.position() + other.fnp.limit());
        
        //
        // Loop through the documents.
        for(int i = 0, origID = 0, off = 0; i < other.nIDs; i++) {
            
            origID += ((ReadableBuffer) other.dfo).byteDecode();
            freq    = ((ReadableBuffer) other.dfo).byteDecode();
            off    += ((ReadableBuffer) other.dfo).byteDecode();
            int mapID = idMap[origID];
            
            //
            // Do we need to copy some bytes?
            if(copyNextTime) {
                
                lastOff = fnp.position();
                //
                // Encode the offset in the buffer.
                ((WriteableBuffer) fnp).append((ReadableBuffer) other.fnp,
                        off - prevOff);
                copyNextTime = false;
            }
            
            other.fnp.position(off);
            prevOff = off;
            
            //
            // Skip deleted documents.
            if(mapID < 0) {
                continue;
            }
            
            //
            // Get the document ID for this document in the new partition.
            int newID = mapID + start - 1;
            
            //
            // Increment our ID count, and see if we need to add a skip.
            nIDs++;
            if(nIDs % skipSize == 0) {
                addSkip(newID, dfo.position(), fnp.position());
            }
            
            //
            // Encode the id, frequency, and offset information.
            ((WriteableBuffer) dfo).byteEncode(newID - lastID);
            ((WriteableBuffer) dfo).byteEncode(freq);
            ((WriteableBuffer) dfo).byteEncode(fnp.position() - lastOff);
            
            //
            // Keep track of the postings stats.
            maxfdt = Math.max(maxfdt, freq);
            to += freq;
            
            //
            // Copy the data from the other field and position buffer,
            // which we'll do next time.
            copyNextTime = true;
            lastID = newID;
        }
        
        //
        // Handle the last bit of fnp data.
        if(copyNextTime) {
            lastOff = fnp.position();
            ((WriteableBuffer) fnp).append((ReadableBuffer) other.fnp);
        }
    }
    
    /**
     * Gets an iterator for the postings.
     *
     * @param features A set of features that the iterator must support.
     * @return A postings iterator.  The iterators for these postings do
     * not support any of the extra features available.  If any extra
     * features are requested, a warning will be logged and
     * <code>null</code> will be returned.
     */
    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        return new DFOIterator(features);
    }

    public class DFOIterator implements PosPostingsIterator, FieldedPostingsIterator {
        
        /**
         * Our postings as a readable buffer.
         */
        protected ReadableBuffer rdfo;
        
        protected ReadableBuffer rfnp;
        
        /**
         * Whether we've finished the entry.
         */
        protected boolean done;
        
        /**
         * The current ID.
         */
        protected int curr;
        
        /**
         * The frequency for the current ID.
         */
        protected int freq;
        
        /**
         * The ID of a field decoded, if this is the only position in a
         * document.
         */
        protected int field;
        
        /**
         * The position that an ID was found, if this is the only position
         * in a document.
         */
        protected int posn;
        
        /**
         * The current offset.
         */
        protected int currOff;
        
        /**
         * The current block in the skip table where we're processing
         * data.  This will help us speed up find operations.
         */
        protected int cb;
        
        /**
         * A weighting function to use for the frequencies.
         */
        protected WeightingFunction wf;
        
        protected WeightingComponents wc;
        
        /**
         * A set of field multipliers to apply.
         */
        protected float[] mult;
        
        /**
         * The fields for which we're supposed to return postings.
         */
        protected int[] searchFields;
        
        /**
         * Whether we're doing all of the fields.
         */
        protected boolean allFields;
        
        /**
         * The position data.
         */
        protected int[][] posns;
        
        /**
         * The number of fields in the current ID.
         */
        protected int nFields;
        
        /**
         * The field data for the current ID.
         */
        protected int[] fields;
        
        /**
         * Field weights for the current ID.
         */
        protected float[] fieldWeights;
        
        /**
         * Whether we've decoded the field information for the current ID.
         */
        protected boolean fieldsDecoded;
        
        /**
         * Whether we've decoded the position information for the current ID.
         */
        protected boolean positionsDecoded;
        
        private PostingsIteratorFeatures features;
        
        /**
         * Creates a postings iterator for this postings type.
         */
        public DFOIterator(PostingsIteratorFeatures features) {
        
            this.features = features;
            rdfo = (ReadableBuffer) dfo;
            rfnp = (ReadableBuffer) fnp;
            
            //
            // If we have fnp data, then clone it and prepare for field
            // stuff.
            if(fnp != null) {
                fields = new int[8];
                fieldWeights = new float[8];
            }
            
            rdfo.position(dataStart);
            if(features != null) {
                wf           = features.getWeightingFunction();
                wc           = features.getWeightingComponents();
                searchFields = features.getFields();
                mult         = features.getMult();
            }
            
            //
            // If we need to get positions and there are no search fields
            // defined, we're going to do all of the fields.
            allFields = searchFields == null;
            
            //
            // We're already done if there are no IDs.
            done = nIDs == 0;
        }
        
        public PostingsIteratorFeatures getFeatures() {
            return features;
        }
        
        /**
         * Gets the number of IDs in this postings list.
         */
        public int getN() {
            return nIDs;
        }
        
        /**
         * Moves to the next ID in this entry.  This method is different
         * than the <code>java.util.Iterator.next()</code> method in that
         * it does not return an object.  This would require too much
         * object creation overhead during retrieval, and saves the whole
         * <code>hasNext()</code>/<code>next()</code> function call
         * overhead.  You should use the accessor functions for the
         * iterator to find out the actual ID that the iterator is at.
         *
         * @return true if there is a next ID, false otherwise.
         */
        public boolean next() {
            return next(-1, -1, -1);
        }
        
        /**
         * Finds the next document in the postings entry.
         *
         * @param id The ID to use for that document, if we've skipped to
         * this point. If id is less than 0, we will use the ID as it was
         * decoded.
         * @param off The positions offset to use for the next document, if
         * we've skipped to this point.  If this is less than 0, we'll use
         * the offset as decoded.
         * @return <code>true</code> if there is a next ID,
         * <code>false</code> otherwise.
         */
        protected boolean next(int pos, int id, int off) {
            if(done) {
                return false;
            }
            
            //
            // If we had to skip, do it.
            if(pos > 0) {
                rdfo.position(pos);
            }
            
            boolean satisfied = false;
            
            //
            // Now, scan forward, looking for a satisfying ID.
            while(!done && !satisfied) {
                
                //
                // Decode the data for the current ID.
                curr    += rdfo.byteDecode();
                freq     = rdfo.byteDecode();
                currOff += rdfo.byteDecode();
                
                //
                // If we had to skip, the set up the values appropriately.
                if(id > 0) {
                    curr = id;
                    id = -1;
                }
                
                if(off > 0) {
                    currOff = off;
                    off = -1;
                }
                
                //
                // We may be done the next time around.
                done = curr == lastID;
                
                //
                // We haven't decoded any field or position information yet.
                fieldsDecoded = false;
                positionsDecoded = false;
                
                //
                // Check whether this ID satisfies the constraints given by
                // the postings iterator features.  If it does, then we're
                // done.
                satisfied = satisfies();
                if(satisfied) {
                    break;
                }
            }
            
            return satisfied;
        }
        
        /**
         * Does the data for the current ID satisfy the requirements of our
         * postings iterator features?
         */
        protected boolean satisfies() {
            if(fnp == null) {
                
                //
                // There's nothing to satisfy.
                return true;
            }
            
            decodeFields();
            
            //
            // Make sure any field requirements are satisfied.  While we're 
            // doing this, we'll modify the frequency so that it only collects
            // the frequencies for the fields of interest.
            int freq = 0;
            if(searchFields != null) {
                for(int i = 0; i < searchFields.length; i++) {
                    if(searchFields[i] != 0 &&
                            i < fields.length &&
                            fields[i] != 0) {
                        freq += fields[i];
                    }
                }
            }
            
            //
            // If we got here, we're done for.
            return searchFields == null || freq > 0;
        }
        
        /**
         * Finds the given ID in the entry we're iterating through, if it
         * exists.  If the ID occurs in this entry, the iterator is left in
         * a state where the data for that ID has been decoded.  If the ID
         * does not occur in this entry, the iterator is left in a state
         * where the data for the next-highest ID in this entry has been
         * decoded.
         *
         * @param id The ID that we want to find.
         * @return <code>true</code> if the ID occurs in this entry,
         * <code>false</code> otherwise.
         */
        public boolean findID(int id) {
            
//             log.debug(logTag, 0, id + " " + curr + " " + cb + " " + skipID[cb]);
            
            if(nIDs == 0) {
                return false;
            }
            
            //
            // We're only done if we're looking for something past the
            // end.
            if(id > lastID) {
                done = true;
                return false;
            }
            done = false;
            
            //
            // Set up.  Start at the beginning or skip to the right place.
            if(nSkips == 0) {
                if(id < curr) {
                    curr    = 0;
                    currOff = 0;
                    next(dataStart, -1, -1);
                }
            } else {
                
                //
                // We can binary search in the skip table to find the right
                // block of postings to look for this ID, but we really
                // only want to do that when we're sure that we won't find
                // the ID that we want in the current block of postings.
                //
                // On average, we'll have to decode about skipSize/2
                // postings to find the ID in a block of postings if we
                // start at the beginning of the block we find by binary
                // searching.
                //
                // The problem is that the query engine will typically be
                // calling findID with increasing IDs, so we might be
                // better off to continue from where we are, instead of
                // possibly jumping back to the beginning of the block and
                // re-decoding a bunch of stuff.  So, we keep track of the
                // block that we're checking and only binary search if we
                // need to.
                if(cb >= skipID.length || skipID[cb] < id || curr > id) {
//                     log.debug(logTag, 0, " searching");
                    int p = Arrays.binarySearch(skipID, id);
                    if(p < 0) {
                        p = -p - 2;
                    }
                    
                    //
                    // Remember the limit for the next time around.
                    cb = p+1;
                    
                    if(p == 0) {
                        //
                        // The first element is a catch all for the "first"
                        // skip, which means we start at the beginning of the
                        // entry, just as we do when there are no skips.
                        curr    = 0;
                        currOff = 0;
                        next(dataStart, 0, 0);
                    } else {
                        next(skipPos[p], skipID[p], skipOff[p]);
                    }
                }
            }
            
            //
            // Scan forward in the current block of postings until we find
            // or pass the ID that we're looking for.
            while(curr < id) {
                if(!next(-1, -1, -1)) {
                    return false;
                }
            }
            
            return curr == id;
        }
        
        /**
         * Decodes the field information, leaving the pointer in the buffer
         * at the position information.
         */
        public void decodeFields() {
            
            //
            // Handle the single position case.
            if(fieldsDecoded) {
                return;
            }
            
            for(int i = 0; i < fields.length; i++) {
                fields[i] = 0;
                fieldWeights[i] = 0;
            }
            
            rfnp.position(currOff);
            nFields = rfnp.byteDecode();
            for(int i = 0; i < nFields; i++) {
                int f = rfnp.byteDecode();
                if(f >= fields.length) {
                    fields = Util.expandInt(fields, f*2);
                    fieldWeights = new float[f*2];
                }
                fields[f] = rfnp.byteDecode();
            }
            
            fieldsDecoded = true;
        }
        
        /**
         * Resets the iterator to the beginning of the entry.  Data will not be
         * decoded until the <code>next</code> method is called.
         */
        public void reset() {
            rdfo.position(dataStart);
            done    = nIDs == 0;
            curr    = 0;
            currOff = 0;
            cb      = 0;
        }
        
        /**
         * Gets the ID that the iterator is currently pointing at.
         *
         * @return The ID that the iterator is pointing at, or 0 if the
         * iterator has not been advanced yet, or has been exhausted.
         */
        public int getID() {
            return curr;
        }
        
        /**
         * Gets the weight associated with the current ID, as generated by
         * some weighting function.
         */
        public float getWeight() {
            if(wf == null) {
                return freq;
            }
            
            wc.fdt = freq;
            return wf.termWeight(wc);
        }
        
        /**
         * Gets the frequency associated with the current ID.
         */
        public int getFreq() {
            return freq;
        }
        
        /**
         * Returns the positions associated with the current ID.  The positions
         * are divided by field.
         *
         * @return A two-dimensional array of int.  The contents of the
         * <em>i<sup>th</sup></em> element of the array are the positions for
         * the field whose ID is <em>i</em>.  The positions stored at element 0
         * are those positions that are not in any named field.
         */
        public int[][] getPositions() {
            decodeFields();
            
            if(positionsDecoded) {
                return posns;
            }
            
            positionsDecoded = true;
            
            //
            // Make sure we have enough room, or reset the current array
            // for this document.
            if(posns == null ||
                    posns.length < fields.length) {
                posns = new int[fields.length][4];
            } else {
                for(int i = 0; i < posns.length; i++) {
                    posns[i][0] = 0;
                }
            }
            
            
            for(int i = 0; i < fields.length; i++) {
                int ff = fields[i];
                if(ff > 0) {
                    if(!allFields &&
                            searchFields[i] == 0) {
                        continue;
                    }
                    
                    if(ff+1 >= posns[i].length) {
                        posns[i] = new int[ff*2];
                    }
                    posns[i][0] = ff;
                    int pp = 0;
                    for(int j = 1; j <= ff; j++) {
                        posns[i][j] = pp + rfnp.byteDecode();
                        pp = posns[i][j];
                    }
                }
            }
            return posns;
        }
        
        /**
         * Compares this postings iterator to another one.  The comparison
         * is based on the current ID that the iterator is pointing at.
         *
         * @return less than zero, 0, or greater than 0 if the ID at the
         * head of the given postings iterator is less than, equal to, or
         * greater than the ID at the head of this postings iterator,
         * respectively.
         */
        public int compareTo(Object o) {
            return getID() - ((PostingsIterator) o).getID();
        }
        
        /**
         * Tests the equality of this postings iterator and another one.
         *
         * @return <code>true</code> if the iterators are pointing at the
         * same ID, <code>false</code> otherwise.
         */
        public boolean equals(Object o) {
            return getID() == ((PostingsIterator) o).getID();
        }
        
        public int get(int[] ids) {
            int p;
            for(p = 0; p < ids.length && curr != lastID; p++) {
                curr += rdfo.byteDecode();
                rdfo.byteDecode();
                rdfo.byteDecode();
            }
            return p;
        }
        
        public int get(int[] ids, int[] freq) {
            int p;
            for(p = 0; p < ids.length && curr != lastID; p++) {
                curr += rdfo.byteDecode();
                ids[p] = curr;
                freq[p] = rdfo.byteDecode();
                rdfo.byteDecode();
            }
            return p;
        }
        
        public int get(int[] ids, float[] weights) {
            int p;
            for(p = 0; p < ids.length && curr != lastID; p++) {
                curr += rdfo.byteDecode();
                ids[p] = curr;
                freq = rdfo.byteDecode();
                if(wf == null) {
                    weights[p] = freq;
                } else {
                    wc.fdt = freq;
                    weights[p] = wf.termWeight(wc);
                }
                rdfo.byteDecode();
            }
            return p;
        }
        
        public int[] getFieldFreq() {
            return fields;
        }
        
        public float[] getFieldWeights() {
            for(int i = 0; i < fields.length; i++) {
                if(fields[i] > 0) {
                    wc.fdt = fields[i];
                    fieldWeights[i] = wf.termWeight(wc);
                }
            }
            return fieldWeights;
        }
    }
    
} // DFOPostings
