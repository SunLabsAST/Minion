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
package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.Field;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.LightIterator;
import com.sun.labs.minion.indexer.dictionary.TermStatsDiskDictionary;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.TermStatsQueryEntry;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.NIOFileReadableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that holds the document vector lengths for a partition.  It can
 * be used at indexing or query time to build the document vector lengths
 * and dump them to disk.  The stored document vector lengths are used for
 * document vector length normalization during querying and classification
 * operations.
 * 
 * <p>
 * 
 * The lengths are represented using a file-backed buffer.
 */
public class DocumentVectorLengths {

    /**
     * A buffer containing the vector lengths for the whole document.
     */
    protected ReadableBuffer vecLens;

    /**
     * The maximum ID in the vector lengths.
     */
    protected int maxDocumentID;

    /**
     * A standard buffer size to use, in bytes.
     */
    protected static int BUFF_SIZE = 8192;

    private static final Logger logger = Logger.getLogger(
            DocumentVectorLengths.class.getName());

    /**
     * Opens a set of document vector lengths.
     *
     * @param raf the file containing the lengths
     * @param buffSize the size of the buffer to use when storing the
     * lengths.
     * @throws java.io.IOException if there is any error reading or writing
     * the vector lengths.
     */
    public DocumentVectorLengths(RandomAccessFile raf, int buffSize)
            throws java.io.IOException {

        //
        // Read the maximum ID and the number of vectored field lengths.
        maxDocumentID = raf.readInt();

        //
        // Open the file of vector lengths and then create our buffer.
        vecLens = new NIOFileReadableBuffer(raf, raf.getFilePointer(), buffSize);
    }

    /**
     * Calculates a set of document vector lengths from a partition using the
     * global set of term statistics for a field.  The global term stats for the field
     * may be re-written as a side effect.
     *
     * @param f the field whose vectors we're dumping
     * @param tsdFile the file where we'll write a new term stats dictionary.  If this
     * is <code>null</code>, then we won't re-write the term stats.
     * @param vlFile the file where we'll be writing the vector lengths
     * @param gts the dictionary of global term stats.
     * @throws java.io.IOException if there is any error writing the vector lengths
     */
    public static void calculate(Field f, PartitionOutput partOut,
                                 TermStatsDiskDictionary gts)
            throws java.io.IOException {
        Partition p = f.getPartition();
        calculate(f.getInfo(), 
                  p.getNDocs(), 
                  p.maxDocumentID,
                  p.getPartitionManager(),
                  (DictionaryIterator<String>) f.getTermDictionary(false).iterator(),
                  partOut.getVectorLengthsBuffer(), gts);
    }

    public static void calculate(FieldInfo fi,
                                 int nDocs,
                                 int maxDocID,
                                 PartitionManager manager,
                                 DictionaryIterator<String> mdi,
                                 WriteableBuffer vectorLengthsBuffer,
                                 TermStatsDiskDictionary gts)

            throws java.io.IOException {

        float[] vl = new float[maxDocID + 1];

        //
        // Get a set of postings features for running the postings.
        WeightingFunction wf = manager.getQueryConfig().
                getWeightingFunction();
        WeightingComponents wc = manager.getQueryConfig().
                getWeightingComponents();
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures(wf, wc);

        //
        // There are three possibilities here:
        // 1. There are no global term stats yet.  In this case we only process
        //    the new main dictionary.
        // 2. The number of entries in the main dictionary is within a factor
        //    of five of the number of entries in the term stats dictionary.
        //    In this case, we'll iterate through both dictionaries.
        // 3. The number of entries in the main dictionary is small in comparison
        //    to the number of entries in the term stats dictionary.  In this case,
        //    we'll use a jumping strategy for finding the term stats corresponding
        //    to particular tokens.
        if(gts == null) {
            calculateWithNoTermStats(fi, mdi, vl, feat);
            writeVectorLengths(maxDocID, vl, vectorLengthsBuffer);
            return;
        }        
        //
        // Get an iterator for the term stats.
        DiskDictionary<String> tsd = gts.getDictionary(fi);
        if(tsd == null) {
            //
            // We had a dictionary, but it didn't have term stats for this field,
            // so we can calculate with no term stats.
            calculateWithNoTermStats(fi, mdi, vl, feat);
            writeVectorLengths(maxDocID, vl, vectorLengthsBuffer);
            return;
        }
        LightIterator<String> gti = tsd.literator();
        
        if(mdi.getNEntries() * 10 >= tsd.size()) {
            calculateWithIteration(fi, mdi, gti, vl, feat);
        } else {
            calculateWithAdvance(fi, mdi, gti, vl, feat);
        }
        writeVectorLengths(maxDocID, vl, vectorLengthsBuffer);
    }
    
    private static void writeVectorLengths(int maxDocID, float[] vl, WriteableBuffer vectorLengthsBuffer) {

        //
        // Write the document vectors.
        vectorLengthsBuffer.byteEncode(maxDocID, 4);
        for(int i = 1; i < vl.length; i++) {
            vectorLengthsBuffer.encode((float) Math.sqrt(vl[i]));
        }
    }

    private static void calculateWithNoTermStats(
            FieldInfo fi,
            DictionaryIterator<String> mdi, float[] vl, 
            PostingsIteratorFeatures feat) {
        WeightingFunction wf = feat.getWeightingFunction();
        WeightingComponents wc = feat.getWeightingComponents();
               
        while(mdi.hasNext()) {

            Entry<String> mde = mdi.next();
            try {
                wc.Ft = mde.getTotalOccurrences();
                wc.ft = mde.getN();
                wf.initTerm(wc);
                PostingsIterator pi = mde.iterator(feat);
                addPostings(pi, vl);
            } catch(RuntimeException ex) {
                logger.log(Level.SEVERE, String.format("Error generating vector lengths for %s at term %s",
                        fi.getName(), mde.getName()));
                throw (ex);
            }
        }
    }

    private static void calculateWithAdvance(
            FieldInfo fi,
            DictionaryIterator<String> mdi, 
            LightIterator<String> gti,
            float[] vl, 
            PostingsIteratorFeatures feat) {
        
        TermStatsQueryEntry pgte = null;
        TermStatsQueryEntry gte = null;
        WeightingFunction wf = feat.getWeightingFunction();
        WeightingComponents wc = feat.getWeightingComponents();

        while(mdi.hasNext()) {

            Entry<String> mde = mdi.next();
            try {
                gte = (TermStatsQueryEntry) gti.advanceTo(mde.getName(), pgte);

                if(gte != null) {

                    //
                    // We found the term in the global stats.
                    TermStatsImpl ts = gte.getTermStats();
                    wf.initTerm(wc.setTerm(ts));
                    pgte = gte;
                } else {
                    //
                    // The term is only in the dictionary for the partition.
                    wc.Ft = mde.getTotalOccurrences();
                    wc.ft = mde.getN();
                    wf.initTerm(wc);
                }
                PostingsIterator pi = mde.iterator(feat);
                addPostings(pi, vl);
            } catch(RuntimeException ex) {
                logger.log(Level.SEVERE, String.format("Error generating vector lengths for %s at term %s",
                        fi.getName(), mde.getName()));
                throw (ex);
            }
        }
    }

    private static void calculateWithIteration(
            FieldInfo fi,
            DictionaryIterator<String> mdi,
            LightIterator<String> gti,
            float[] vl,
            PostingsIteratorFeatures feat) {
        
        if(!gti.next()) {
            calculateWithNoTermStats(fi, mdi, vl, feat);
            return;
        }
        
        String gtn = gti.getName();
        
        TermStatsQueryEntry pgte = null;
        WeightingFunction wf = feat.getWeightingFunction();
        WeightingComponents wc = feat.getWeightingComponents();
        while(mdi.hasNext()) {

            Entry<String> mde = mdi.next();
            String mden = mde.getName();
            try {
                int cmp;
                while((cmp = gtn.compareTo(mden)) < 0) {
                    if(!gti.next()) {
                        break;
                    }
                    gtn = gti.getName();
                }

                if(cmp == 0) {
                    pgte = (TermStatsQueryEntry) gti.getEntry(pgte);
                    wc.setTerm(pgte.getTermStats());
                } else {
                    //
                    // The term is only in the dictionary for the partition.
                    wc.Ft = mde.getTotalOccurrences();
                    wc.ft = mde.getN();
                }

                wf.initTerm(wc);
                PostingsIterator pi = mde.iterator(feat);
                addPostings(pi, vl);
            } catch(RuntimeException ex) {
                logger.log(Level.SEVERE, String.format("Error generating vector lengths for %s at term %s",
                        fi.getName(), mde.getName()));
                throw (ex);
            }
        }

        //
        // Handle whatever's left in the main dictionary, if we ran out of term
        // stats before we ran out of the main dictionary.
        calculateWithNoTermStats(fi, mdi, vl, feat);
}

    /**
     * Adds the postings from a given iterator to the vector lengths that we're
     * building.
     *
     * @param pi the iterator
     * @param vl the lengths to add to.
     */
    private static void addPostings(PostingsIterator pi, float[] vl) {
        if(pi != null) {
            while(pi.next()) {
                float w = pi.getWeight();
                vl[pi.getID()] += w * w;
            }
        }
    }

    /**
     * Gets the length of a document associated with this partition.  This
     * will be used at query and classification time.  Note that our buffer
     * uses 0 based indexing, so we need to subtract one from the document
     * ID!
     *
     * @param docID the ID of the document whose vector length we wish to
     * retrieve.
     * @return the vector length of the document with the given ID
     */
    public synchronized float getVectorLength(int docID) {
        if(docID > maxDocumentID) {
            throw new IndexOutOfBoundsException(String.format(
                    "Document ID %d is greater than max %d", docID,
                    maxDocumentID));
        }
        vecLens.position((docID - 1) * 4);
        return vecLens.decodeFloat();
    }

    /**
     * Normalizes a set of document scores all in one go, using a local buffer
     * copy to avoid synchronization and churn in the buffer.  This will modify
     * the <code>scores</code> array.
     * 
     * @param docs the document IDs to normalize
     * @param scores the document scores
     * @param p the number of document IDs and scores in the array
     * @param qw the query weight to use for normalization
     */
    public void normalize(int[] docs, float[] scores, int p, float qw) {
        ReadableBuffer lvl = vecLens.duplicate();
        
        for(int i = 0; i < p; i++) {
            lvl.position((docs[i] - 1) * 4);
            float vl = lvl.decodeFloat();
            if(docs[i] == 17778) {
                logger.info(String.format("%d score: %.3f vl: %.3f qw: %.3f", docs[i], scores[i], vl, qw));
            }
            scores[i] /= (vl * qw);
        }
    }
    
    /**
     * Normalizes a set of scores generated across multiple fields.
     * 
     * @param dvls the document vector lengths for the fields in question.
     * @param docs the document IDs
     * @param scores the scores to normalize
     * @param p the number of elements in the docs array that contain document
     * IDs
     * @param qw the query weight.
     */
    public static void normalize(List<DocumentVectorLengths> dvls, int[] docs, float[] scores, int p, float qw) {
        if(dvls.size() == 1) {
            if(dvls.get(0) == null) {
                return;
            }
            dvls.get(0).normalize(docs, scores, p, qw);
            return;
        }
        ReadableBuffer[] lvls = new ReadableBuffer[dvls.size()];
        for(int i = 0; i < dvls.size(); i++) {
             lvls[i] = dvls.get(i).vecLens.duplicate();
        }
        
        for(int i = 0; i < p; i++) {
            float norm = 0;
            int vlpos = (docs[i] - 1) * 4;
            for(ReadableBuffer lvl : lvls) {
                lvl.position(vlpos);
                norm += lvl.decodeFloat();
            }
            if(norm == 0) {
                norm = 1;
            } else {
                norm /= lvls.length;
            }
            scores[i] /= (norm * qw);
        }
    }
} // DocumentVectorLengths

