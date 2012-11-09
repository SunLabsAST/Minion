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

import com.sun.labs.minion.WeightedFeature;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.Field.TermStatsType;
import com.sun.labs.minion.indexer.dictionary.Dictionary;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.LightIterator;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * A class to hold postings for the document vectors.  For these postings,
 * the IDs that we store are the IDs of the terms that occurred in the
 * document.  Along with the IDs, we store the frequency of occurrence of
 * each term.
 *
 * <p>
 *
 * During indexing, we will encounter term IDs in a (seemingly) random
 * order, so in this case we store the entries from the dictionaries and their
 * frequencies.
 */
public class DocumentVectorPostings extends IDFreqPostings implements MergeablePostings {
    
    private static final Logger logger = Logger.getLogger(DocumentVectorPostings.class.getName());

    /**
     * Storage for the entries making up this set of postings.
     */
    protected Map<Object, EntryFreq> entries;

    /**
     * Creates a set of postings suitable for use during indexing.
     */
    public DocumentVectorPostings() {
        super();
        entries = new HashMap<Object, EntryFreq>();
    } // ReMapPostings constructor

    /**
     * Creates a set of postings suitable for use during querying.
     *
     * @param b a buffer containing the encoded postings.
     */
    public DocumentVectorPostings(PostingsInput[] in, long[] offset, int[] size) throws IOException {
        super(in, offset, size);
    } // ReMapPostings constructor

    /**
     * Adds an occurrence to the postings.  We're just keeping a set of the
     * entries added to a document.
     *
     * @param o the occurrence to add.
     */
    @Override
    public void add(Occurrence o) {
        Entry e = ((DocOccurrence) o).getEntry();
        EntryFreq ef = entries.get(e.getName());
        if(ef == null) {
            ef = new EntryFreq(e);
            entries.put(e.getName(), ef);
        }
        ef.add(o.getCount());
    }

    @Override
    public void merge(MergeablePostings mp, int[] map) {
        DocumentVectorPostings dvp = (DocumentVectorPostings) mp;
        for(Map.Entry<Object, EntryFreq> e : dvp.entries.entrySet()) {
            EntryFreq ef = entries.get(e.getKey());
            if(ef == null) {
                entries.put(e.getKey(), e.getValue());
            } else {
                ef.add(e.getValue().freq);
            }
        }
    }

    /**
     * Estimates the size of the postings associated with this document.
     */
    @Override
    public int size() {
        if(idBuff != null) {
            return super.size();
        }
        return nIDs * 2;
    }

    /**
     * Finishes off the encoding, which does nothing in this case.
     */
    public void finish() {
    }

    /**
     * Remaps the IDs in the postings, using the provided ID map.  This
     * encodes the postings onto a buffer and as a side effect calculates
     * any necessary document statistics.
     *
     * @param idMap a map from old IDs to new IDs.
     */
    @Override
    public void remap(int[] idMap) {
        
        //
        // If we were generated by a merge, then just quit.
        if(idMap == null) {
            return;
        }
        
        //
        // Sort our entry set by the ID, so that we can dump things in ID order.
        EntryFreq[] se = new EntryFreq[entries.size()];
        entries.values().toArray(se);
        Util.sort(se, new Comparator<EntryFreq>() {
            @Override
            public int compare(EntryFreq o1, EntryFreq o2) {
                return o1.e.getID() - o2.e.getID();
            }
        });
        
        //
        // Encode the stuff in our list of entries and frequencies.
        int prevID = 0;
        WriteableBuffer temp = new ArrayBuffer(se.length * 2);
        to = 0;
        for(int i = 0; i < se.length; i++) {
            int id = se[i].e.getID();
            int freq = se[i].freq;
            
            nIDs++;
            if(nIDs % skipSize == 0) {
                addSkip(id, (int) temp.position());
            }

            //
            // Keep track of our postings stats.
            to += freq;
            maxfdt = Math.max(maxfdt, freq);

            //
            // Encode the data for this term.
            temp.byteEncode(id - prevID);
            temp.byteEncode(freq);
            prevID = id;
        }
        idBuff = temp;
        lastID = prevID;
    }

    @Override
    public void clear() {
        super.clear();
        entries.clear();
    }
    
    

    /**
     * Gets the entries in this set of postings as an array of weighted features. 
     * We will attempt to do this as efficiently as possible.  In particular, if
     * the number of terms in the document is a substantial proportion of the 
     * number of terms in the dictionary for this partition, we will attempt to 
     * iterate through the dictionary in such a way as to minimize the number of
     * dictionary lookups required.
     * 
     * @param docID the id of this document, if it is in an already dumped partition.
     * @param fieldID the id of the field from which the postings were drawn
     * @param dict a dictionary that we can use to fetch term names when all we
     * have is IDs.
     * @param wf a weighting function to use to weight the entries in the document vector.
     * @param wc a set of weighting components to use in the weighting function.
     * @return an array of weighted features corresponding to the terms in this
     * document.  Note that the <code>getEntry</code> method for these features
     * will return the dictionary entry for the term from the partition holding
     * the document.  This is a convenience to avoid multiple dictionary lookups
     * in this partition.
     *
     */
    public WeightedFeature[] getWeightedFeatures(
            int docID,
            int fieldID,
            Dictionary dict,
            WeightingFunction wf,
            WeightingComponents wc, 
            TermStatsType termStatsType) {

        if(entries == null) {

            //
            // We have the postings from disk.
            PostingsIteratorFeatures feat = new PostingsIteratorFeatures(wf, wc);
            PostingsIterator pi = iterator(feat);
            if(pi == null) {
                return new WeightedFeature[0];
            }

            DiskField df = ((InvFileDiskPartition) dict.getPartition()).getDF(
                    fieldID);

            wc.dvl = df.getDocumentVectorLength(docID);
            List<WeightedFeature> fl = new ArrayList<WeightedFeature>();
            Dictionary termDictionary = df.getTermDictionary(termStatsType);
            if(getN() > 0.1 * termDictionary.size()) {
                //
                // If we have a substantial portion of the entries in the dictionary,
                // then just iterate through the dictionary and pick out the names.
                LightIterator di = ((DiskDictionary) termDictionary).literator();
                QueryEntry qe = null;
                while(pi.next()) {
                    while(di.next()) {
                        qe = di.getEntry(qe);
                        if(pi.getID() == qe.getID()) {
                            String name = qe.getName().toString();
                            wc.setTerm(name).setDocument(pi);
                            wf.initTerm(wc);
                            WeightedFeature twf =
                                    new WeightedFeature(name, pi.getID(),
                                    wf.termWeight(wc) /
                                    wc.dvl);
                            twf.setFreq(pi.getFreq());
                            twf.setEntry(qe);
                            fl.add(twf);
                            break;
                        }
                    }
                }
            } else {
                DiskDictionary dd = (DiskDictionary) termDictionary;
                while(pi.next()) {
                    QueryEntry qe = dd.getByID((int) pi.getID());
                    String name = qe.getName().toString();
                    wc.setTerm(name).setDocument(pi);
                    wf.initTerm(wc);
                    WeightedFeature twf = new WeightedFeature(name, pi.getID(),
                            wf.termWeight(wc) /
                            wc.dvl);
                    twf.setFreq(pi.getFreq());
                    twf.setEntry(qe);
                    fl.add(twf);
                }
            }
            return fl.toArray(new WeightedFeature[0]);
        } else {
            //
            // We have an entries map, so we'll use that.
            WeightedFeature[] ret = new WeightedFeature[entries.size()];
            int x = 0;
            for(EntryFreq ef : entries.values()) {
                String term = ef.e.getName().toString();
                wc.setTerm(term);
                wc.fdt = ef.freq;
                wf.initTerm(wc);
                ret[x] = new WeightedFeature(term, wf.termWeight(wc));
                ret[x].setFreq(ef.freq);
                x++;
            }

            //
            // Sort the weighted features by name before returning them.
            Util.sort(ret);
            return ret;
        }
    }

    /**
     * A combined entry and its frequency.
     */
    class EntryFreq {

        public Entry e;

        public int freq;

        public EntryFreq(Entry e) {
            this.e = e;
        }

        public void add(int x) {
            freq += x;
        }
    }
} // DocumentVectorPostings

