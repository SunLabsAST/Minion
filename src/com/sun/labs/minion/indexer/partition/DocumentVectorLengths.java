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

import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.Field;
import com.sun.labs.minion.indexer.MemoryField;
import java.io.RandomAccessFile;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DictionaryWriter;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary.MemoryDictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.dictionary.TermStatsDictionary;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.entry.TermStatsIndexEntry;
import com.sun.labs.minion.indexer.entry.TermStatsQueryEntry;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.buffer.FileWriteableBuffer;
import com.sun.labs.minion.util.buffer.NIOFileReadableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.util.Iterator;
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

    static Logger logger = Logger.getLogger(
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
    public static void calculate(MemoryField f,
                                 RandomAccessFile tsdFile,
                                 RandomAccessFile vlFile,
                                 TermStatsDictionary gts)
            throws java.io.IOException {

        calculate(f, f.getTermDictionary(false).iterator(),
                  tsdFile, vlFile, gts);
    }

    public static void calculate(DiskField f,
                                 RandomAccessFile tsdFile,
                                 RandomAccessFile vlFile,
                                 TermStatsDictionary gts)
            throws java.io.IOException {

        calculate(f, f.getTermDictionary(false).iterator(),
                  tsdFile, vlFile, gts);
    }

    public static void calculate(Field f,
                                 Iterator<Entry> mdi,
                                 RandomAccessFile tsdFile,
                                 RandomAccessFile vlFile,
                                 TermStatsDictionary gts)
            throws java.io.IOException {

        //
        // Get iterators for our two dictionaries and a place to write the new term stats.
        DictionaryIterator gti = gts.iterator();
        DictionaryWriter gtw = null;

        Partition p = f.getPartition();

        boolean adjustStats = tsdFile != null;

        if(adjustStats) {
            gtw = new DictionaryWriter(
                    p.getPartitionManager().getIndexDir(),
                    new StringNameHandler(), 0,
                    MemoryDictionary.Renumber.RENUMBER);
        }

        //
        // Get a set of postings features for running the postings.
        WeightingFunction wf = p.getPartitionManager().getQueryConfig().
                getWeightingFunction();
        WeightingComponents wc = p.getPartitionManager().getQueryConfig().
                getWeightingComponents();
        if(adjustStats) {
            wc.N += p.getNDocs();
        }
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures(wf, wc);

        //
        // Get the first entries from the dictionaries.
        Entry mde = null;
        if(mdi.hasNext()) {
            mde = mdi.next();
        }

        TermStatsQueryEntry gte = null;
        if(gti.hasNext()) {
            gte = (TermStatsQueryEntry) gti.next();
        }

        float[] vl = new float[f.getMaximumDocumentID() + 1];

        //
        // Iterate until there are no more entries in either of the dictionaries.
        while(mde != null || gte != null) {

            int cmp;
            if(mde == null) {
                cmp = 1;
            } else if(gte == null) {
                cmp = -1;
            } else {
                cmp = ((Comparable) mde.getName()).compareTo(gte.getName());
            }

            //
            // The entry to use for the merged global term stats dictionary.
            TermStatsIndexEntry we;

            if(cmp == 0) {
                //
                // Both iterators have the term.  Combine stats!
                we = new TermStatsIndexEntry(gte);
                TermStatsImpl ts = gte.getTermStats();
                if(adjustStats) {
                    ts.add(mde);
                }
                wf.initTerm(wc.setTerm(ts));
                addPostings(mde.iterator(feat), vl);
                gte = null;
                mde = null;
            } else if(cmp < 0) {
                //
                // Only the new partition has the term.  Create the stats.
                we = new TermStatsIndexEntry(mde.getName().toString(), 0);
                TermStatsImpl ts = we.getTermStats();
                ts.add(mde);
                wf.initTerm(wc.setTerm(ts));
                addPostings(mde.iterator(feat), vl);
                mde = null;
            } else {
                //
                // Only the global file has the stats.  Keep them.
                we = new TermStatsIndexEntry(gte);
                gte = null;
            }

            //
            // Write the entry to the new global term stats dictionary.
            if(adjustStats) {
                gtw.write(we);
            }

            //
            // Pump whichever iterators are necessary.
            if(gte == null && gti.hasNext()) {
                gte = (TermStatsQueryEntry) gti.next();
            }

            if(mde == null && mdi.hasNext()) {
                mde = mdi.next();
            }
        }

        //
        // Write the document vectors.
        vlFile.writeInt(f.getMaximumDocumentID());
        FileWriteableBuffer b = new FileWriteableBuffer(vlFile, 8192);
        for(int i = 1; i < vl.length; i++) {
            b.encode((float) Math.sqrt(vl[i]));
        }
        b.flush();

        if(adjustStats) {
            gtw.finish(tsdFile);
        }

    }

    /**
     * Adds the postings from a given iterator to the vector lengths that we're
     * building.
     *
     * @param pi the iterator
     * @param vl the lengths to add to.
     */
    private static void addPostings(PostingsIterator pi,
                                    float[] vl) {
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
            scores[i] /= (lvl.decodeFloat() * qw);
        }
    }
} // DocumentVectorLengths

