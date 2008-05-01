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
import com.sun.labs.minion.indexer.postings.FieldedDocumentVectorPostings;
import com.sun.labs.minion.indexer.postings.MergeablePostings;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.buffer.ReadableBuffer;

/**
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class FieldedDocKeyEntry extends DocKeyEntry {

    /**
     * Creates a FieldedDocKeyEntry
     */
    public FieldedDocKeyEntry() {
        super(null);
    }

    public FieldedDocKeyEntry(Object name) {
        super(name);
    } // DocKeyEntry constructor

    public Entry getEntry(Object name) {
        return new FieldedDocKeyEntry(name);
    }

    /**
     * Gets a new entry that contains a copy of the data in this entry.
     *
     * @return a new entry containing a copy of hte data in this entry.
     * @throws ClassCastException if the provided entry is not of type
     * <code>SinglePostingsEntry</code>
     */
    public Entry getEntry() {
        FieldedDocKeyEntry dke = (FieldedDocKeyEntry) super.getEntry();
        dke.docLen = docLen;
        return dke;
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
        ((FieldedDocumentVectorPostings) p).merge((MergeablePostings) ((DocKeyEntry) qe).p, null);
    }

    /**
     * Gets the appropriate postings type for the class.  These postings
     * should be useable for indexing.
     *
     * @return A set of ID and frequency postings.
     */
    public Postings getPostings() {
        return new FieldedDocumentVectorPostings();
    }

    /**
     * Gets a set of postings useful at query time.
     *
     * @param input The buffer containing the postings read from the
     * postings file.
     * @return A set of ID and frequency postings.
     */
    protected Postings getPostings(ReadableBuffer input) {
        return new FieldedDocumentVectorPostings(input);
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
        if(super.writePostings(out, idMap) == true) {

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
     * Gets an array of weighted features associated with a field in this
     * document key.  This can be used to generate a document vector for a
     * document that was indexed but not dumped to disk.
     *
     * @param field the ID of the field for which we want postings.
     * @param wf a weighting function to use to get the weight for the entries
     * in the document vector
     * @param wc a set of weighting components to use with the weighting function.
     * @return an array of weighted features in the field
     * @see com.sun.labs.minion.engine.SearchEngineImpl#getDocumentVector(Document,String)
     */
    public WeightedFeature[] getWeightedFeatures(
            int field,
            WeightingFunction wf,
            WeightingComponents wc) {
        if(p == null) {
            try {
                readPostings();
            } catch(IOException ex) {
                log.error(logTag, 0, "Error reading postings for document " + name);
                return new WeightedFeature[0];
            }
        }
        if(p == null) {
            log.warn(logTag, 0, "Empty postings for " + name + " " + size);
            return new WeightedFeature[0];
        }
        return ((FieldedDocumentVectorPostings) p).getWeightedFeatures(
                field,
                id,
                dict,
                wf, wc);
    }
}
