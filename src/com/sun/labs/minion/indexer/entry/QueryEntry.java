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

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.QueryStats;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An entry that is returned from dictionaries during querying operations.
 * @param <N> the type of the name that this entry has.
 */
public class QueryEntry<N extends Comparable> extends Entry<N> implements
        Cloneable {

    private static Logger logger = Logger.getLogger(QueryEntry.class.getName());

    /**
     * The input channels that are associated with the postings for this
     * entry.
     */
    protected PostingsInput[] postIn;
    
    /**
     * The field from which this term was drawn.
     */
    private FieldInfo field;
    
    protected QueryEntry() {
        
    }

    /**
     * Creates an entry.
     * @param name the name of the entry
     * @param type the type of postings associated with the entry.
     * @param b a buffer from which the entry's information can be decoded.
     */
    public QueryEntry(N name, Postings.Type type, ReadableBuffer b) {
        this.name = name;
        this.type = type;
        decode(b);
    }
    
    public void decode(ReadableBuffer b) {
        n = b.byteDecode();
        maxFDT = b.byteDecode();
        id = b.byteDecode();
        int npi = b.byteDecode();
        size = new int[npi];
        offset = new long[npi];
        for(int i = 0; i < size.length; i++) {
            size[i] = b.byteDecode();
            offset[i] = b.byteDecodeLong();
        }
    }

    /**
     * Reads the main postings associated with this entry, which usually 
     * consists of id and frequency information.
     *
     * @throws java.io.IOException if there is an error reading the postings.
     */
    public void readPostings() throws java.io.IOException {
        if (type != Postings.Type.NONE) {
            post = Postings.Type.getPostings(type, postIn, offset, size);
        }
    }
    
    public boolean hasPositionInformation() {
        return false;
    }

    public FieldInfo getField() {
        return field;
    }

    public void setField(FieldInfo field) {
        this.field = field;
    }

    @Override
    public Postings getPostings() {
        if(post == null) {
            try {
            readPostings();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, String.format("Error reading postings!"), ex);
            }
        }
        return post;
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
        if (post == null) {
            QueryStats qs = features == null ? null : features.getQueryStats();
            try {
                if (qs != null) {
                    qs.postReadW.start();
                    qs.postingsSize += size[0];
                }
                readPostings();
            } catch (java.io.IOException ioe) {
                logger.severe("Error reading postings for " + name);
                return null;
            } finally {
                if (qs != null) {
                    qs.postReadW.stop();
                }
            }
        }

        //
        // p could still be null here if there were no postings
        if (post == null) {
            return null;
        }
        return post.iterator(features);
    }

    /**
     * Sets the inputs that will be used to read postings for this
     * entry.  This allows us to use different kinds of inputs at different times.
     *
     * @param postIn The inputs from which the postings data can be read.
     */
    public void setPostingsInput(PostingsInput[] postIn) {
        this.postIn = postIn;
    }

    public Object clone() {
        try {
            QueryEntry qe = (QueryEntry) super.clone();
            qe.postIn = null;
            return qe;
        } catch (CloneNotSupportedException ex) {
            logger.log(Level.SEVERE, String.format("Error cloning query entry"),
                    ex);
            return null;
        }
    }
}
