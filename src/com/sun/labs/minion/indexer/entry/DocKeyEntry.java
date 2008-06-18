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

import java.io.IOException;
import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.indexer.partition.DiskPartition;

import com.sun.labs.minion.indexer.postings.DocumentVectorPostings;
import com.sun.labs.minion.indexer.postings.MergeablePostings;
import com.sun.labs.minion.indexer.postings.Postings;

import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * A class for holding entries in the document dictionary.  Such entries
 * have ID and frequency postings and they encode their IDs into the
 * dictionary, since that information cannot be recovered any other way.
 */
public class DocKeyEntry extends SinglePostingsEntry implements MergeableEntry {
    
    /**
     * The length of the document, in words.
     */
    protected int docLen;

    /**
     * A previous entry, appended onto our entry, used to detect when we've 
     * got a duplicate key during a merge.
     */
    protected DocKeyEntry prevEntry;
    
    /**
     * The original document ID before remapping.
     */
    protected int origID;

    protected static String logTag = "DKE";
    
    public DocKeyEntry() {
        super(null);
    } // DocKeyEntry constructor
    
    public DocKeyEntry(Object name) {
        super(name);
    } // DocKeyEntry constructor

    public Entry getEntry(Object name) {
        return new DocKeyEntry(name);
    }
    
    /**
     * Gets a new entry that contains a copy of the data in this entry.
     *
     * @return a new entry containing a copy of hte data in this entry.
     * @throws ClassCastException if the provided entry is not of type
     * <code>SinglePostingsEntry</code>
     */
    public Entry getEntry() {
        DocKeyEntry dke = (DocKeyEntry) super.getEntry();
        dke.docLen = docLen;
        return dke;
    }
    
    public int getOrigID() {
        return origID;
    }

    /**
     * Appends, with a check for a duplicate key, which is bad.
     */
    public void append(QueryEntry qe, int start, int[] idMap) {
        
        //
        // Some partition types merge their documents (e.g., cluster partitions),
        // in which case we don't want to check for duplicate keys.
        if(!((DiskPartition) qe.getPartition()).docsAreMerged()) {
            if(prevEntry != null) {
                throw new DuplicateKeyException(prevEntry);
            }
            prevEntry = (DocKeyEntry) qe;
        }
        super.append(qe, start, idMap);
    }
    
    /**
     * Merges the entries in the postings underlying the other document key with 
     * the entries in the postings for this key.  During indexing, we may want to 
     * merge the contents of two document key 
     * entries, for example, when dumping feature clusters during classification. 
     * so we need to be able to get the list of entries in the underlying postings.
     *
     */
    public void merge(QueryEntry qe, int[] map) {
        if(p == null) {
            p = getPostings();
        }
        ((DocumentVectorPostings) p).merge((MergeablePostings) ((DocKeyEntry) qe).p, map);
    }
    
    /**
     * Gets an array of weighted features associated with this document key.
     * This can be used to generate a document vector for a document that was
     * indexed but not dumped to disk.
     * 
     * @param wf a weighting function to use to get the weight for the entries
     *        in the document vector
     * @param wc a set of weighting components to use with the weighting
     *        function.
     * @see com.sun.labs.minion.engine.SearchEngineImpl#getDocumentVector(Document,String)
     */
    public WeightedFeature[] getWeightedFeatures(
            WeightingFunction wf,
            WeightingComponents wc) {
        if(p == null) {
            try {
                readPostings();
            } catch(IOException ex) {
                log.error(logTag, 0, "Error reading postings for: " + name);
                return new WeightedFeature[0];
            }
        }
        return ((DocumentVectorPostings) p).getWeightedFeatures(
                id,
                dict,
                wf, wc);
    }
    
    /**
     * Gets the appropriate postings type for the class.  These postings
     * should be useable for indexing.
     *
     * @return A set of ID and frequency postings.
     */
    public Postings getPostings() {
        return new DocumentVectorPostings();
    }

    /**
     * Gets a set of postings useful at query time.
     *
     * @param input The buffer containing the postings read from the
     * postings file.
     * @return A set of ID and frequency postings.
     */
    protected Postings getPostings(ReadableBuffer input) {
        return new DocumentVectorPostings(input);
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

        //
        // Do the standard writing stuff.
        if (super.writePostings(out, idMap) == true) {

            if(p == null) {
                return true;
            }

            //
            // Get the document length from the postings.
            docLen = p.getTotalOccurrences();
            return true;
        }
        return true;
    }
    
    /**
     * Encodes any information associated with the postings onto the given
     * buffer.  We override the parent's method because we need to encode
     * the ID for our term.
     *
     * @param b The buffer onto which the postings information should be
     * encoded.  The buffer will be positioned to the correct spot for the
     * encoding.
     */
    public void encodePostingsInfo(WriteableBuffer b) {
        super.encodePostingsInfo(b);
        b.byteEncode(docLen);
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
        docLen = b.byteDecode();
        id     = b.byteDecode();
        origID = id;
    }

    /**
     * Returns the total number of occurrences, which is the same as the
     * document length.
     */
    public int getTotalOccurrences() {
        return getDocumentLength();
    }

    /**
     * Gets the document length in words.
     */
    public int getDocumentLength() {
        return docLen;
    }

    /**
     * Gets the length of the vector associated with this document.
     */
    public float getDocumentVectorLength() {
        return getDocumentVectorLength(null);
    }
    
    public float getDocumentVectorLength(String field) {
        return ((DiskPartition) dict.getPartition()).getDocumentVectorLength(id, field);
    }
    
    public float getDocumentVectorLength(int fieldID) {
        return ((DiskPartition) dict.getPartition()).getDocumentVectorLength(id, fieldID);
    }
    
} // DocKeyEntry
