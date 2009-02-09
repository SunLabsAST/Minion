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

import java.io.File;
import java.io.RandomAccessFile;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DictionaryWriter;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.dictionary.TermStatsDictionary;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.entry.TermStatsEntry;
import com.sun.labs.minion.indexer.postings.FieldedPostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.FileLock;
import com.sun.labs.minion.util.FileLockException;
import com.sun.labs.minion.util.buffer.FileReadableBuffer;
import com.sun.labs.minion.util.buffer.FileWriteableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.util.concurrent.TimeUnit;
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
     * The partition whose values we're storing.
     */
    protected DiskPartition part;

    /**
     * The file that (will) contain the document vector lengths.
     */
    protected File vlFile;

    /**
     * The random access file that we'll  use to back our buffer.
     */
    protected RandomAccessFile raf;

    /**
     * A buffer containing the vector lengths for the whole document.
     */
    protected ReadableBuffer vecLens;

    /**
     * Buffers containing vector lengths for the vectored fields.
     */
    protected ReadableBuffer[] fieldLens;

    /**
     * A standard buffer size to use, in bytes.
     */
    protected static int BUFF_SIZE = 4096;

    Logger logger = Logger.getLogger(getClass().getName());

    protected static String logTag = "DVL";

    /**
     * Creates a set of vector lengths for a given partition.  If the file
     * of vector lengths already exists, it is opened for use.  If the
     * file doesn't exist, then the vector lengths will be created by a
     * multitude of threads.
     *
     * @param part the partition whose document vector lengths we will
     * calculate.
     * @param adjustStats if <code>true</code> then if we have to calculate the
     * vector lengths, then we will modify the global term stats.
     * @throws java.io.IOException if there is any error reading or writing
     * the vector lengths.
     */
    public DocumentVectorLengths(DiskPartition part, boolean adjustStats)
            throws java.io.IOException {
        this(part, BUFF_SIZE, adjustStats);
    } // DocumentVectorLengths constructor

    /**
     * Creates a set of vector lengths for a given partition.  If the file
     * of vector lengths already exists, it is opened for use.  If the
     * file doesn't exist, then the vector lengths will be calculated and
     * then stored to the file.
     *
     * @param part the partition whose vector lengths we're storing.
     * @param buffSize the size of the buffer to use when storing the
     * lengths.
     * @param adjustStats if <code>true</code> then if we have to calculate the
     * vector lengths, then we will modify the global term stats.
     * @throws java.io.IOException if there is any error reading or writing
     * the vector lengths.
     */
    public DocumentVectorLengths(DiskPartition part, int buffSize,
            boolean adjustStats)
            throws java.io.IOException {
        this.part = part;
        vlFile = part.getManager().
                makeVectorLengthFile(part.getPartitionNumber());

        //
        // We want to lock the file so that we don't unneccessarily
        // recompute things.  We'll time out the lock after 40 seconds.
        FileLock lock =
                new FileLock(new File(part.getManager().getLockDir()), vlFile,
                40, TimeUnit.SECONDS);
        try {
            lock.acquireLock();

            //
            // If the file doesn't exist, then calculate the lengths.
            if(!vlFile.exists()) {
                calculateLengths(part, part.getManager().getTermStatsDict(),
                        adjustStats);
            }

            raf = new RandomAccessFile(vlFile, "r");

            //
            // Read the maximum ID and the number of vectored field lengths.
            int maxID = raf.readInt();
            int nf = raf.readInt();
            long offset = raf.getFilePointer();
            if(nf > 0) {
                int n = raf.readInt();
                fieldLens = new ReadableBuffer[nf];
                offset = raf.getFilePointer();
                for(int i = 0; i < n;
                        i++) {
                    raf.seek(offset);
                    int f = raf.readInt();
                    fieldLens[f] = new FileReadableBuffer(raf,
                            raf.getFilePointer(),
                            buffSize);

                    //
                    // The offset of the next buffer takes into account the field ID
                    // and the encoded document lengths.
                    offset += 4 + (maxID * 4);
                }
            }

            //
            // Open the file of vector lengths and then create our buffer.
            vecLens = new FileReadableBuffer(raf, offset, buffSize);
            lock.releaseLock();
        } catch(FileLockException fle) {
            logger.severe("Error locking vector lengths file: " + vlFile + ": " + fle);
        }
    }

    /**
     * Calculates a set of document vector lengths from a partition using a
     * global set of term statistics.  The global term stats may be re-written
     * as a side effect.
     *
     * @param p the partition for which we're calculating document vector lengths
     * @throws com.sun.labs.minion.util.FileLockException if we can't lock the vector length
     * file
     * @throws java.io.IOException if there is any error writing the vector lengths
     * @param gts the dictionary of global term stats.
     * @param adjustStats if <code>true</code>, the global term stats will be
     * modified to include the statistics from the term in the partition.  This
     * will be the case when computing vector lengths for a new partition, but
     * not when computing vector lengths for a merged partition, since in that
     * case the global term stats will already include data from the partitions
     * that were merged.  If this paramater is <code>false</code> the global
     * stats will not be rewritten.
     */
    public void calculateLengths(DiskPartition p,
            TermStatsDictionary gts, boolean adjustStats)
            throws FileLockException, java.io.IOException {

        //
        // Get iterators for our two dictionaries and a place to write the new term stats.
        DictionaryIterator gti = gts.iterator();
        DictionaryIterator mdi = p.getMainDictionary().iterator();
        DictionaryWriter gtw = null;

        if(adjustStats) {
            gtw = new DictionaryWriter(p.getManager().getIndexDir(),
                    new StringNameHandler(), null, 0,
                    MemoryDictionary.Renumber.RENUMBER);
        }

        //
        // Get a set of postings features for running the postings.
        WeightingFunction wf = p.getQueryConfig().getWeightingFunction();
        WeightingComponents wc = p.getQueryConfig().getWeightingComponents();
        if(adjustStats) {
            wc.N += part.getNDocs();
        }
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures(wf, wc);
        int[] vectored = null;

        //
        // See whether we need to get the vectored fields.  If this index
        // isn't building fielded document vectors, then we don't need to bother.
        if(p.getManager().hasFieldedVectors()) {
            vectored = p.getManager().getMetaFile().getVectoredFields();
        }
        feat.setFields(vectored);
        float[][] fvl = null;
        if(vectored != null) {
            fvl = new float[vectored.length][];
        }
        float[] vl = new float[p.getMaxDocumentID() + 1];

        //
        // Get the first entries from the dictionaries.
        QueryEntry mde = null;
        if(mdi.hasNext()) {
            mde = mdi.next();
        }

        TermStatsEntry gte = null;
        if(gti.hasNext()) {
            gte = (TermStatsEntry) gti.next();
        }

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
            TermStatsEntry we;

            if(cmp == 0) {
                //
                // Both iterators have the term.  Combine stats!
                we = gte;
                TermStatsImpl ts = gte.getTermStats();
                if(adjustStats) {
                    ts.add(mde);
                }
                wf.initTerm(wc.setTerm(ts));
                addPostings(p, mde.iterator(feat), ts, vectored, fvl, vl);
                gte = null;
                mde = null;
            } else if(cmp < 0) {
                //
                // Only the new partition has the term.  Create the stats.
                we = new TermStatsEntry(mde.getName().toString());
                TermStatsImpl ts = we.getTermStats();
                ts.add(mde);
                wf.initTerm(wc.setTerm(ts));
                addPostings(p, mde.iterator(feat), ts, vectored, fvl, vl);
                mde = null;
            } else {
                //
                // Only the global file has the stats.  Keep them.
                we = gte;
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
                gte = (TermStatsEntry) gti.next();
            }

            if(mde == null && mdi.hasNext()) {
                mde = mdi.next();
            }
        }

        //
        // Write the new term stats dictionary.
        if(adjustStats) {

            int tsn = part.getManager().getMetaFile().getNextTermStatsNumber();
            File ntsf = part.getManager().makeTermStatsFile(tsn);
            RandomAccessFile gtraf = new RandomAccessFile(ntsf, "rw");
            gtw.finish(gtraf);
            gtraf.close();

            //
            // It's now safe to use this term stats dictionary.
            part.getManager().getMetaFile().setTermStatsNumber(tsn);
            part.getManager().updateTermStats();
        }

        //
        // Reduce the refcount in the term stats dictionary so that it can close.
        gts.iterationDone();

        //
        // Write the document vector lengths.
        dump(fvl, vl, part);
    }

    /**
     * Dumps the vector lengths to the appropriate file.
     * @param fvl the specific field vector lengths for any vectored fields
     * @throws java.io.IOException if there is any error writing the vector
     * lengths
     * @param vl the vector lengths for all vectored data in the documents
     * @param p the partitions with which these vector lengths are associated
     */
    private void dump(float[][] fvl, float[] vl,
            DiskPartition p)
            throws java.io.IOException {

        RandomAccessFile r = new RandomAccessFile(vlFile, "rw");

        r.writeInt(p.getMaxDocumentID());

        //
        // Dump the fielded vectors, if we have them.
        if(fvl != null) {
            int n = 0;
            for(int i = 0; i < fvl.length;
                    i++) {
                if(fvl[i] != null) {
                    n++;
                }
            }
            r.writeInt(fvl.length);
            r.writeInt(n);
            for(int i = 0; i < fvl.length;
                    i++) {
                if(fvl[i] != null) {
                    r.writeInt(i);
                    dump(new FileWriteableBuffer(r, 8192), fvl[i]);
                }
            }
        } else {
            r.writeInt(0);
        }
        dump(new FileWriteableBuffer(r, 8192), vl);
        r.close();
    }

    private void dump(WriteableBuffer b, float[] weights) {
        for(int i = 1; i < weights.length;
                i++) {
            b.encode((float) Math.sqrt(weights[i]));
        }
        ((FileWriteableBuffer) b).flush();
    }

    private void addPostings(DiskPartition p,
            PostingsIterator pi, TermStatsImpl ts, int[] vectored,
            float[][] fvl, float[] vl) {
        if(pi != null) {
            if(pi instanceof FieldedPostingsIterator) {

                //
                // Calculate weights for the various fields and for the
                // whole document.
                while(pi.next()) {
                    float[] fw =
                            ((FieldedPostingsIterator) pi).getFieldWeights();
                    for(int i = 0; i < fw.length && i < fvl.length;
                            i++) {
                        if(vectored[i] != 0 && fw[i] > 0) {
                            if(fvl[i] == null) {
                                fvl[i] = new float[p.getMaxDocumentID() + 1];
                            }
                            fvl[i][pi.getID()] += fw[i] * fw[i];
                        }
                    }
                    float w = pi.getWeight();
                    vl[pi.getID()] += w * w;
                }
            } else {

                while(pi.next()) {
                    float w = pi.getWeight();
                    vl[pi.getID()] += w * w;
                }
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
        normalize(docs, scores, p, qw, -1);
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
     * @param fieldID the ID of the field that the scores were computed from and that
     * should be used for normalization.
     */
    public void normalize(int[] docs, float[] scores, int p, float qw,
            int fieldID) {
        ReadableBuffer lvl;

        switch(fieldID) {
            case -1:
                lvl = vecLens.duplicate();
                break;
            default:
                if(fieldLens[fieldID] == null) {
                    return;
                }
                lvl = fieldLens[fieldID].duplicate();
        }

        for(int i = 0; i < p; i++) {
            lvl.position((docs[i] - 1) * 4);
            scores[i] /= (lvl.decodeFloat() * qw);
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
     * @param fieldID the ID of the field for which we're looking for the length.
     * A field ID of -1 is interpreted as a request for the length using all
     * vectored fields.  If this field was not vectored, then a length of 1 is returned, so that
     * dividing weights by document lengths won't cause problems.
     * @return the length of the vector for the given ID and vectored field
     */
    public synchronized float getVectorLength(int docID, int fieldID) {
        switch(fieldID) {
            case -1:
                vecLens.position((docID - 1) * 4);
                return vecLens.decodeFloat();
            default:
                if(fieldLens[fieldID] == null) {
                    return 1;
                }
                fieldLens[fieldID].position((docID - 1) * 4);
                return fieldLens[fieldID].decodeFloat();
        }
    }

    /**
     * Closes the file associated with the document lengths.
     * @throws java.io.IOException if there is any error closing the file
     */
    public synchronized void close()
            throws java.io.IOException {
        raf.close();
    }
} // DocumentVectorLengths

