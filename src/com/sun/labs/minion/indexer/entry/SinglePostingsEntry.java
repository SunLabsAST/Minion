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
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.Occurrence;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * An abstract base class for those entries that only define a single
 * postings type.  This is a convenience class that implements most of the
 * behaviour needed for an entry that can be used at indexing or query time.
 */
public abstract class SinglePostingsEntry extends BaseEntry {
    
    /**
     * The postings associated with this entry.
     */
    protected Postings p;
    
    /**
     * The number of postings associated with this entry.
     */
    protected int n;
    
    /**
     * The size of the postings, in bytes.
     */
    protected int size;
    
    /**
     * The offset of the postings in a postings file.
     */
    protected long offset;
    
    public static long tsize;
    
    /**
     * A log.
     */
    protected static MinionLog log = MinionLog.getLog();
    
    /**
     * A tag for the log.
     */
    protected static String logTag = "SPE";
    
    /**
     * Creates an entry.
     */
    public SinglePostingsEntry() {
        this(null);
    } // SinglePostingsEntry constructor
    
    /**
     * Creates an entry.
     */
    public SinglePostingsEntry(Object name) {
        super(name);
    } // SinglePostingsEntry constructor
    
    /**
     * Gets the appropriate postings type for the class.  These postings
     * should be useable for indexing.
     *
     * @return Postings suitable for use when indexing.
     */
    protected abstract Postings getPostings();
    
    /**
     * Reads the postings for this class, returning a set of postings
     * useful at query time.
     *
     * @param input The buffer containing the postings read from the
     * postings file.
     * @return The postings for this entry.
     */
    protected abstract Postings getPostings(ReadableBuffer input);
    
    //
    // Remaining implementation of Entry.
    
    /**
     * Gets a new entry that contains a copy of the data in this entry.
     *
     * @return a new entry containing a copy of hte data in this entry.
     * @throws ClassCastException if the provided entry is not of type
     * <code>SinglePostingsEntry</code>
     */
    public Entry getEntry() {
        SinglePostingsEntry ne = (SinglePostingsEntry) getEntry(name);
        ne.id     = id;
        ne.postIn = postIn;
        ne.dict   = dict;
        ne.n      = n;
        ne.size   = size;
        ne.offset = offset;
        return ne;
    }
    
    /**
     * Copies the data from another entry of this type into this entry.  A
     * convenience method for subclasses.
     */
    protected void copyData(SinglePostingsEntry e) {
    }
    
    //
    // Implementation of IndexEntry.
    
    /**
     * Adds an occurrence at indexing time.
     */
    public void add(Occurrence o) {
        if(p == null) {
            p = getPostings();
        }
        p.add(o);
    }
    
    /**
     * Returns the number of channels needed to store the postings for this
     * entry type.
     */
    public int getNumChannels() {
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
        n      = p.getN();
        offset = out[0].position();
        size   = (int) out[0].write(b);
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
        b.byteEncode(n);
        b.byteEncode(size);
        b.byteEncode(offset);
    }
    
    /**
     * Appends the postings from another entry onto this one.
     *
     * @param qe The entry that we want to append onto this one.
     * @param start The new starting ID for the partition that the entry
     * was drawn from.
     * @param idMap A map from old IDs in the given postings to new IDs
     * with gaps removed for deleted data.  If this is <code>null</code>,
     * then there are no deleted documents.
     */
    public void append(QueryEntry qe, int start, int[] idMap) {
        SinglePostingsEntry spe = (SinglePostingsEntry) qe;
        if(spe.p == null || spe.n == 0) {
            return;
        }
        
        if(p == null) {
            p = getPostings();
        }
        
        p.append(spe.p, start, idMap);
        n = p.getN();
    }
    
    //
    // Implementation of QueryEntry
    
    /**
     * Decodes the postings information associated with this entry.
     *
     * @param b The buffer containing the encoded postings information.
     * @param pos The position in <code>b</code> where the postings
     * information can be found.
     */
    public void decodePostingsInfo(ReadableBuffer b, int pos) {
        b.position(pos);
        n      = b.byteDecode();
        size   = b.byteDecode();
        offset = b.byteDecodeLong();
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
        p = getPostings(postIn[0].read(offset, size));
    }
    
    /**
     * Gets the number of postings associated with this entry.  This is
     * used to sort entries by their frequency during query operations.
     *
     * @return The number of postings associated with this entry.
     */
    public int getN() {
        return n;
    }
    
    /**
     * Gets the total number of occurrences associated with this entry.
     * For this base class, this is the same as n.
     *
     * @return The total number of occurrences associated with this entry.
     */
    public long getTotalOccurrences() {
        return n;
    }
    
    /**
     * Gets the maximum frequency in the postings associated with this
     * entry.  For this base class, this is always 1.
     */
    public int getMaxFDT() {
        return 1;
    }
    
    public boolean hasPositionInformation() {
        return false;
    }
    
    public boolean hasFieldInformation() {
        return false;
    }
    
    /**
     * Gets an iterator that will iterate through the postings associated
     * with this entry.
     *
     * @param features A set of features that the iterator must support.
     * @return An iterator for the postings.  Returns null if there are no
     * postings.
     */
    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        if(p == null) {
            QueryStats qs = features == null ? null : features.getQueryStats();
            try {
                if(qs != null) {
                    qs.postReadW.start();
                    qs.postingsSize += size;
                }
                readPostings();
            } catch (java.io.IOException ioe) {
                log.error(logTag, 1, "Error reading postings for " +
                        name);
                return null;
            } finally {
                if (qs != null) {
                    qs.postReadW.stop();
                }
            }
        }
        
        //
        // p could still be null here if there were no postings
        if (p == null) {
            return null;
        }
        return p.iterator(features);
    }
    
    
} // SinglePostingsEntry
