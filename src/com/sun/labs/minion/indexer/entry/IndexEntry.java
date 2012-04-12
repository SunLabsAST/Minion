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

import com.sun.labs.minion.indexer.postings.*;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An entry that is used in dictionaries that are built while indexing
 * documents.
 * @param <N> the type of the name in this entry
 */
public class IndexEntry<N extends Comparable> extends Entry<N> {

    private static final Logger logger = Logger.getLogger(IndexEntry.class.getName());

    private boolean used = false;

    /**
     * Creates an entry with a given name and a given set of postings.
     * @param name the name of the entry
     * @param id the id to use for this entry
     * @param post a set of postings to use for this entry.
     */
    public IndexEntry(N name, int id, Postings post) {
        this.name = name;
        this.id = id;
        this.post = post;
        if(post != null) {
            this.type = post.getType();
        } else {
            this.type = Postings.Type.NONE;
        }
    }

    public void add(Occurrence o) {
        post.add(o);
        used = true;
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
    public boolean writePostings(PostingsOutput[] out, int[] idMap)
            throws java.io.IOException {
        
        if(post == null || post.getType() == Postings.Type.NONE) {
            return true;
        }

        if(!used) {
            return false;
        }

        try {
            //
            // Possibly remap the data and get a buffer to write.
            post.remap(idMap);
            
            //
            // Set the elements of the term information, so that they can
            // be encoded later.
            n = post.getN();
            maxFDT = post.getMaxFDT();
            offset = new long[out.length];
            size = new int[out.length];
            post.write(out, offset, size);
            return true;
        } catch(ArithmeticException ex) {
            logger.log(Level.SEVERE, String.format("Error writing entry %s: %s", getName(), ex.getMessage()));
            throw (ex);
        }
    }

    /**
     * Encodes the entry data onto the given buffer.
     *
     * @param b The buffer onto which the postings information should be
     * encoded.  The buffer will be positioned to the correct spot for the
     * encoding.
     */
    public void encodeEntryInfo(WriteableBuffer b) {
        b.byteEncode(n);
        b.byteEncode(maxFDT);
        b.byteEncode(id);
        if(size == null) {
            b.byteEncode(0);
        } else {
            b.byteEncode(size.length);
            for(int i = 0; i < size.length; i++) {
                b.byteEncode(size[i]);
                b.byteEncode(offset[i]);
            }
        }
    }

    @Override
    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        return post.iterator(features);
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
        if(qe.post == null || qe.n == 0) {
            return;
        }

        post.append(qe.post, start, idMap);
        n = post.getN();
        
        //
        // This entry was used if we actually added some postings.
        used = n > 0;
    }

    public void merge(QueryEntry qe, int[] idMap) {
        ((MergeablePostings) post).merge((MergeablePostings) qe.post,
                idMap);
        used = true;
    }

    public boolean isUsed() {
        return used;
    }

    public void setUsed(boolean used) {
        this.used = used;
    }

    public void clear() {
        if(post != null) {
            post.clear();
        }
        used = false;
        id = 0;
        n = 0;
        for(int i = 0; i < size.length; i++) {
            size[i] = 0;
            offset[i] = 0;
        }
    }
}
