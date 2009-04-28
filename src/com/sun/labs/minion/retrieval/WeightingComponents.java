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
package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.indexer.entry.DocKeyEntry;

import com.sun.labs.minion.indexer.postings.PostingsIterator;

import java.util.logging.Logger;

/**
 * A class that will hold all of the components necessary to implement any
 * number of weighting functions.  The names and descriptions here are
 * (mostly) taken from the Moffat and Zobel paper <a
 * href="http://www.cs.rmit.edu.au/~jz/fulltext/sigirforum98.pdf">Exploring
 * the Similarity Space</a>.
 *
 * <p>
 *
 * The components that this class contains comprise statistics at two
 * levels of description.  First, there are the collection-level statistics
 * that are calculated across all of the partitions contained in an index.
 * Second, there are the document-level statistics that are set
 * per term or document being processed, depending on the context.
 *
 * <p>
 *
 * For example, in typical query processing scenarios we will create a set
 * of weighting components from the collection statistics at the start of
 * query evaluation.  As each term in the query is processed, we will set
 * the document-level term statistics using the {@link #setTerm} method.
 * As we process each document in the postings list associated with a term,
 * we will set the document-level statistics directly.
 *
 * <p>
 *
 * Note that this class is provided as a convienience and is merely
 * intended as a container into which a number of statistics can be placed.
 * There is <em>no</em> checking done with regard to the validity of the
 * statistics that are placed into it.  The use of inappropriate statistics
 * may lead to strange results when calculating term weights.
 */
public class WeightingComponents {

    /**
     * A set of collection statistics.  If this value is set by the
     * constructor or by the <code>setCollection</code> method, then the
     * weighting components can handle term statistics lookups on their own.
     */
    public CollectionStats cs;

    /**
     * The statistics that we were given or that we retrieved for the last
     * call to <code>setTerm</code>.
     */
    public TermStatsImpl ts;

    /**
     * The total number of documents in the collection.  Collection-level
     * statistic.
     */
    public int N;

    /**
     * The number of distinct terms in the collection.  This is very likely
     * an overestimate, as many terms will be shared in the various
     * partitions' main dictionaries.  Collection-level statistic.
     */
    public int n;

    /**
     * The number of tokens in the collection, i.e., the sum of the lengths
     * of all the documents.  Collection-level statistic.
     */
    public long nTokens;

    /**
     * The frequency of term <em>t</em> in document
     * <em>d</em>. Document-level statistic.
     */
    public int fdt;

    /**
     * The total number of occurrences of term <em>t</em> in the whole
     * collection.  Document-level statistic.
     */
    public long Ft;

    /**
     * The total number of documents containing term <em>t</em>.
     * Document-level statistic.
     */
    public int ft;

    /**
     * The maximum term frequency in the collection.  For all terms
     * <em>t</em> in the collection and all documents <em>d</em> in the
     * partition, this is the maximum value of f<sub><em>d,t</em></sub>,
     * the frequency of term <em>t</em> in document <em>d</em>.
     * Collection-level statistic.
     */
    public int maxfdt;

    /**
     * The maximum document frequency in the collection.  This is given by
     * the term that has the largest number of documents associated with
     * it, across all dictionaries in the collection.  This will most
     * likely be an underestimate, as it most likely will not take into
     * account the fact that the same term occurs in more than one
     * partition!  Collection-level statistic.
     */
    public int maxft;

    /**
     * The number of distinct terms in document <em>d</em>.  Document-level
     * statistic.
     */
    public int nd;

    /**
     * The total number of words in document <em>d</em>.  Document-level
     * statistic.
     */
    public long ld;

    /**
     * The length of the document vector for the current document.
     */
    public float dvl;

    /**
     * The average document length, in words.  Collection-level statistic.
     */
    public float avgDocLen;

    /**
     * A collection level term weight.
     *
     * @see WeightingFunction#initTerm(WeightingComponents)
     */
    public float wt;

    protected static Logger logger = Logger.getLogger(WeightingComponents.class.getName());

    protected static String logTag = "WC";

    /**
     * Creates a set of weighting components.
     */
    public WeightingComponents() {
    }

    /**
     * Initalizes a set of weighting components from a set of collection
     * statistics.  This will initialize all of the collection-level
     * statistics, but will leave the document-level statistics at their
     * default values.
     *
     * @see #setTerm
     */
    public WeightingComponents(CollectionStats s) {
        setCollection(s);
    } // WeightingComponents constructor

    /**
     * Initializes the collection-level statistics.
     * @return this set of weighting components.
     */
    public WeightingComponents setCollection(CollectionStats s) {
        cs = s;
        N = s.nDocs;
        n = s.nd;
        nTokens = s.nTokens;
        maxfdt = s.maxfdt;
        maxft = s.maxft;
        avgDocLen = s.avgDocLen;
        return this;
    }

    /**
     * Initializes any document-level statistics that can be determined
     * from a term.  This method requires a set of collection statistics to
     * have been set at instantiation time or via the
     * <code>setCollection</code> method.  If there are no such statistics
     * a warning is issued and the components in this object <em>will
     * not be modified</em>!
     *
     * @param name the name of the term whose statistics we need.
     * @return this set of weighting components.
     */
    public WeightingComponents setTerm(String name) {
        if(cs == null) {
            logger.warning("No collection stats to get term stats for: " +
                    name);
        } else {
            ts = cs.getTermStats(name);
            if(ts == null) {
                ts = new TermStatsImpl(name);
            }
            setTerm(ts);
        }
        return this;
    }

    /**
     * Initializes any document-level statistics that can be determined
     * from a set of term statistics.
     *
     * @param s a set of statistics for a term.
     * @return this set of weighting components.
     */
    public WeightingComponents setTerm(TermStatsImpl s) {
        ts = s;
        Ft = s.Ft;
        ft = s.ft;
        return this;
    }

    public TermStatsImpl getTermStats() {
        return ts;
    }

    public TermStatsImpl getTermStats(String term) {
        return cs.getTermStats(term);
    }

    public void setTermStats(String term, TermStatsImpl ts) {
        cs.setTermStats(term, ts);
    }

    /**
     * Initializes any document-level statistics that can be determined
     * from a document key.
     *
     * @param key a document key entry from a dicitionary.
     * @return this set of weighting components.
     */
    public WeightingComponents setDocument(DocKeyEntry key) {
        return setDocument(key, null);
    }

    public WeightingComponents setDocument(DocKeyEntry key, String field) {
        nd = key.getN();
        ld = key.getTotalOccurrences();
        dvl = key.getDocumentVectorLength(field);
        return this;
    }

    /**
     * Initalizes any per-document statistics that can be gotten from a
     * postings iterator.
     *
     * @param pi a postings iterator that is being processed
     * @return this set of weighting components.
     */
    public WeightingComponents setDocument(PostingsIterator pi) {
        fdt = pi.getFreq();
        return this;
    }

    public String toString() {
        return String.format("ts: %s N: %d ft: %d fdt: %d", ts, N, ft, fdt);
    }
} // WeightingComponents
