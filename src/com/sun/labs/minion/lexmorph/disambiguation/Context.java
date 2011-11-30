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
package com.sun.labs.minion.lexmorph.disambiguation;

import java.util.HashMap;
import java.util.Map;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import java.util.logging.Logger;

/**
 * A context in which a word occurs, used for sense disambiguation.
 */
public class Context {

    private QueryEntry key;

    /**
     * A map from terms in the context to the number of occurrences.
     */
    private Map<String, TermFreq> terms;

    protected int[] counts;

    static Logger logger = Logger.getLogger(Context.class.getName());

    public static final String logTag = "CON";

    /**
     * Creates a context for use in disambiguating a term
     * @param disTerm the term to disambiguate
     * @param field the field from which contexts should be drawn
     * @param key the document that represents the context
     * @param maxFeat the maximum number of features to keep from the context.
     * Currently the most frequent features are selected.
     */
    public Context(String disTerm, String field, QueryEntry key, int maxFeat) {
        this.key = key;
        InvFileDiskPartition part = (InvFileDiskPartition) key.getPartition();
        terms = new HashMap<String, TermFreq>();
        PostingsIteratorFeatures feat = null;
        if(field != null) {
            feat = new PostingsIteratorFeatures();
        }
        PostingsIterator pi = key.iterator(feat);
        if(pi == null) {
            return;
        }

        while(pi.next()) {
            QueryEntry qe = part.get(field, pi.getID(), false);
            String term = qe.getName().toString();
            if(term.equals(disTerm)) {
                continue;
            }
            terms.put(term, new TermFreq(term, pi.getFreq()));
        }
    }

    /**
     * Sets the counts for the features in an array.
     * 
     * @param features 
     */
    public void setCounts(String[] features) {
        counts = new int[features.length];
        for(int i = 0; i < features.length; i++) {
            TermFreq tf = terms.get(features[i]);
            if(tf == null) {
                continue;
            }
            counts[i] = tf.freq;
        }
    }

    /**
     * Gets the term to frequency map.
     * @return a map from terms in the context to the frequencies
     */
    public Map<String, TermFreq> getTerms() {
        return terms;
    }
}

