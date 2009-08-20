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

import java.util.LinkedList;
import java.util.List;
import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.indexer.dictionary.Dictionary;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.util.logging.Logger;

/**
 * A set of fielded document vector postings.  We will store a set of document vector
 * postings for each field that is being vectorized.
 *
 * @author stgreen
 */
public class FieldedDocumentVectorPostings implements Postings,
        MergeablePostings {

    DocumentVectorPostings[] postings;

    DocumentVectorPostings full;

    static Logger logger = Logger.getLogger(FieldedDocumentVectorPostings.class.getName());

    public static final String logTag = "FDVP";

    /** Creates a new instance of FieldedDocumentVectorPostings */
    public FieldedDocumentVectorPostings() {
        full = new DocumentVectorPostings();
    }

    /**
     * Creates a set of fielded document vector postings from a buffer.
     */
    public FieldedDocumentVectorPostings(ReadableBuffer b) {
        int n = b.byteDecode();
        int maxField = b.byteDecode();
        int[] offs = new int[maxField + 1];
        for(int i = 0; i < offs.length; i++) {
            offs[i] = -1;
        }
        postings = new DocumentVectorPostings[maxField + 1];
        b.byteDecode();
        b.byteDecode();
        //
        // Decode all of the offsets.
        for(int i = 1; i < n; i++) {
            offs[b.byteDecode()] = b.byteDecode();
        }

        //
        // Get the postion of the end of the offsets.
        int p = b.position();
        full = new DocumentVectorPostings(b);
        for(int i = 0; i < offs.length; i++) {
            if(offs[i] != -1) {
                b.position(p + offs[i]);
                postings[i] = new DocumentVectorPostings(b);
            }
        }
    }

    public void setSkipSize(int size) {
    }

    public void add(Occurrence o) {

        DocOccurrence d = (DocOccurrence) o;

        //
        // Make sure we have enough room for all of the postings.
        if(postings == null || d.fields.length > postings.length) {
            DocumentVectorPostings[] temp =
                    new DocumentVectorPostings[d.fields.length];
            if(postings != null) {
                System.arraycopy(postings, 0, temp, 0, postings.length);
            }
            postings = temp;
        }

        for(int i = 0; i < d.fields.length; i++) {
            if(d.fields[i] == 0) {
                continue;
            }

            if(postings[i] == null) {
                postings[i] = new DocumentVectorPostings();
            }
            postings[i].add(d);
        }

        full.add(d);
    }

    public int getN() {
        return full.getN();
    }

    public int getLastID() {
        return full.getLastID();
    }

    public long getTotalOccurrences() {
        return full.getTotalOccurrences();
    }

    public int getMaxFDT() {
        return full.getMaxFDT();
    }

    public void finish() {
        full.finish();
        if(postings != null) {
            for(int i = 0; i < postings.length; i++) {
                if(postings[i] != null) {
                    postings[i].finish();
                }
            }
        }
    }

    public int size() {
        return full.size();
    }

    /**
     * Gets the buffers for these postings, which includes all of the buffers for the fields as
     * well as the buffer for the complete document and a set of offsets into the buffers.
     */
    public WriteableBuffer[] getBuffers() {

        //
        // Compute the number of buffers that we'll be encoding here.
        int n = 1;
        int maxField = 0;
        if(postings != null) {
            for(int i = 0; i < postings.length; i++) {
                if(postings[i] != null) {
                    n++;
                    maxField = i;
                }
            }
        }

        LinkedList<WriteableBuffer> buffs = new LinkedList<WriteableBuffer>();
        WriteableBuffer offs = new ArrayBuffer(32);

        //
        // Encode the number of fields to expect in the input and the maximum
        // field ID.
        offs.byteEncode(n);
        offs.byteEncode(maxField);
        long offset = 0;

        //
        // The first set of postings is at offset 0 (after the offsets!)
        offs.byteEncode(0);
        offs.byteEncode(offset);
        offset += addBuffers(buffs, full);
        if(postings != null) {
            for(int i = 0; i < postings.length; i++) {
                if(postings[i] != null) {
                    offs.byteEncode(i);
                    offs.byteEncode(offset);
                    offset += addBuffers(buffs, postings[i]);
                }
            }
        }
        buffs.addFirst(offs);
        return buffs.toArray(new WriteableBuffer[0]);
    }

    public void remap(int[] idMap) {
        full.remap(idMap);
        if(postings != null) {
            for(int i = 0; i < postings.length; i++) {
                if(postings[i] != null) {
                    postings[i].remap(idMap);
                }
            }
        }
    }

    public void merge(MergeablePostings mp, int[] map) {
        FieldedDocumentVectorPostings f = (FieldedDocumentVectorPostings) mp;
        full.merge(f.full, map);
        if(f.postings != null) {
            if(postings == null) {
                postings = new DocumentVectorPostings[f.postings.length];
            }
            if(postings.length < f.postings.length) {
                DocumentVectorPostings[] temp =
                        new DocumentVectorPostings[f.postings.length];
                System.arraycopy(postings, 0, temp, 0, postings.length);
                postings = temp;
            }
            for(int i = 0; i < f.postings.length; i++) {
                if(postings[i] == null) {
                    postings[i] = new DocumentVectorPostings();
                }
                postings[i].merge(f.postings[i], map);
            }
        }
    }

    public void append(Postings p, int start) {
        append(p, start, null);
    }

    public void append(Postings p, int start, int[] idMap) {
        FieldedDocumentVectorPostings f = (FieldedDocumentVectorPostings) p;
        full.append(f.full, start, idMap);
        if(f.postings != null) {
            if(postings == null) {
                postings = new DocumentVectorPostings[f.postings.length];
            }
            if(postings.length < f.postings.length) {
                DocumentVectorPostings[] temp =
                        new DocumentVectorPostings[f.postings.length];
                System.arraycopy(postings, 0, temp, 0, postings.length);
                postings = temp;
            }
            for(int i = 0; i < f.postings.length; i++) {
                if(f.postings[i] != null) {
                    if(postings[i] == null) {
                        postings[i] = new DocumentVectorPostings();
                    }
                    postings[i].append(f.postings[i], start, idMap);
                }
            }
        }
    }

    /**
     * Gets the entries for a particular field in this set of postings as an array of weighted features.
     * @param field the ID of the field for which we want the entries.  If this is -1, then we
     * want the vector for the full document.
     * @param docID the id of this document, if it is in an already dumped partition.
     * @param dict a dictionary that we can use to fetch term names when all we
     * have is IDs.
     * @param wf a weighting function to use to weight the entries in the document vector.
     * @param wc a set of weighting components to use in the weighting fucntion.
     *
     */
    public WeightedFeature[] getWeightedFeatures(
            int field,
            int docID,
            Dictionary dict,
            WeightingFunction wf,
            WeightingComponents wc) {
        if(field == -1) {
            return full.getWeightedFeatures(docID, -1, dict, wf, wc);
        }

        if(field < postings.length && postings[field] != null) {
            return postings[field].getWeightedFeatures(docID, field, dict, wf,
                    wc);
        }

        return new WeightedFeature[0];
    }

    /**
     * Gets an iterator for a set of fielded postings.
     *
     * @param features the features for the iterator that we will return.  The field for which
     * we want postings will be specified in the fields element of the features.  If multiple fields
     * are specified, we will return postings for the first field (by field ID) that we have postings for.  If
     * the features are <code>null</code> or there are no fields specified, then postings for all fields will be returned.
     * @return a postings iterator for the postings for the field specified in the features.
     */
    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        if(features == null) {
            return full.iterator(null);
        }

        PostingsIteratorFeatures nofields = (PostingsIteratorFeatures) features.
                clone();
        nofields.setFields(null);

        int[] fields = features.getFields();
        if(fields == null) {
            return full.iterator(nofields);
        }

        //
        // Look for a good field to return.
        for(int i = 0; i < fields.length; i++) {
            if(fields[i] == 1) {
                if(postings != null && postings.length > i && postings[i] !=
                        null) {
                    return postings[i].iterator(nofields);
                }
            }
        }

        return null;
    }

    private int addBuffers(List<WriteableBuffer> buffs,
            DocumentVectorPostings full) {
        int len = 0;
        WriteableBuffer[] b = full.getBuffers();
        for(int i = 0; i < b.length; i++) {
            len += b[i].position();
            buffs.add(b[i]);
        }
        return len;
    }
}
