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

import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultsCluster;
import com.sun.labs.minion.SearchEngineException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.retrieval.ResultImpl;
import java.util.logging.Logger;

/**
 * A class to do supervised word sense disambiguation using a Naive Bayes approach.
 *
 * This is based on the discussion in "Foundations of Statistical Natural
 * Language Processing" by Manning and Schutze, MIT Press, 1999.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class Supervised implements Serializable {

    /**
     * The term we're disambiguating.
     */
    private String term;

    private String field;

    /**
     * The senses making up this disambiguator.
     */
    private List<Sense> senses;

    /**
     * The total set of terms across all senses.
     */
    private Set<String> vocab;

    static Logger logger = Logger.getLogger(Supervised.class.getName());

    private static String logTag = "SUPD";

    public Supervised() {
    }

    /**
     * Creates a supervised disambiguator.
     */
    public Supervised(String term, String field, List<Sense> senses) {
        this.term = term;
        this.field = field;
        this.senses = senses;
    }

    public Supervised(String term, String field, Set<ResultsCluster> clusters,
            int totalCount, int maxFeat)
            throws SearchEngineException {
        Map<String, List<Result>> cll = new HashMap<String, List<Result>>();
        for(ResultsCluster cl : clusters) {
            cll.put(cl.getName(), cl.getResults().getAllResults(false));
        }
        init(term, field, cll, totalCount, maxFeat);
    }

    public Supervised(String term, String field,
            Map<String, List<Result>> clusters, int totalCount, int maxFeat)
            throws SearchEngineException {
        init(term, field, clusters, totalCount, maxFeat);
    }

    public void init(String term, String field,
            Map<String, List<Result>> clusters, int totalCount, int maxFeat)
            throws SearchEngineException {
        this.term = term;
        this.field = field;

        //
        // Figure out the vocabulary.  We'll start by collecting the term counts
        // across all the contexts for all the senses.  Then we'll choose the top
        // maxFeat of those.  We'll keep the list of contexts per sense around, 
        // since we'll need those in a bit.
        Map<String, TermFreq> counts = new HashMap<String, TermFreq>();
        List<List<Context>> ac = new ArrayList<List<Context>>();
        int sum = 0;
        for(Map.Entry<String, List<Result>> ce : clusters.entrySet()) {
            List<Context> cs = new ArrayList<Context>();
            for(Result r : ce.getValue()) {
                Context c = new Context(term, field, ((ResultImpl) r).
                        getKeyEntry(), maxFeat);
                for(Map.Entry<String, TermFreq> tfe : c.getTerms().entrySet()) {
                    TermFreq tf = counts.get(tfe.getKey());
                    if(tf == null) {
                        tf = new TermFreq(tfe.getKey());
                        counts.put(tfe.getKey(), tf);
                    }
                    sum += tfe.getValue().getFreq();
                    tf.add(tfe.getValue());
                }
                cs.add(c);
            }
            ac.add(cs);
        }

        //
        // The probability of a term that's not in a sense's contexts.  We want
        // this to be the same for all senses.
        double missingP = 1.0 / sum;

        //
        // Choose the maxFeat most frequent terms.
        PriorityQueue<TermFreq> h = new PriorityQueue<TermFreq>();
        for(TermFreq tf : counts.values()) {
            if(h.size() < maxFeat) {
                h.offer(tf);
            } else {
                if(tf.getFreq() > h.peek().getFreq()) {
                    h.poll();
                    h.offer(tf);
                }
            }
        }

        //
        // Store the vocabulary, which we'll pass into the senses.
        vocab = new HashSet<String>();
        while(h.size() > 0) {
            TermFreq tf = h.poll();
            vocab.add(tf.getTerm());
        }

        //
        // Compute the senses.
        senses = new ArrayList<Sense>();
        Iterator<Map.Entry<String, List<Result>>> ci = clusters.entrySet().
                iterator();
        Iterator<List<Context>> li = ac.iterator();
        while(ci.hasNext()) {
            Map.Entry<String, List<Result>> me = ci.next();
            List<Context> cs = li.next();
            Sense s = new Sense(term, vocab, missingP, me.getKey(), totalCount,
                    cs);
            senses.add(s);
        }
    }

    /**
     * Disambiguates a particular context using these senses.
     * @param r the result containing the context to disambiguate.
     * @return the most probable sense for the term in this result
     */
    /**
     * Disambiguates a particular context using these senses.
     * @param r the result containing the context to disambiguate.
     * @return the most probable sense for the term in this result
     */
    /**
     * Disambiguates a particular context using these senses.
     * @param r the result containing the context to disambiguate.
     * @return the most probable sense for the term in this result
     */
    public Sense disambiguate(Result r) {
        return disambiguate(((ResultImpl) r).getKeyEntry());
    }

    /**
     * Disambiguates a particular context using these senses.
     * 
     * @param dke the DocKeyEntry representing the context to disambiguate.
     * @return the most probable sense for the term in this result
     */
    public Sense disambiguate(DocKeyEntry dke) {
        PostingsIterator pi = dke.iterator(null);
        List<String> context = new ArrayList<String>();
        DiskPartition dp = (DiskPartition) dke.getPartition();
        if(pi == null) {
            return null;
        }
        while(pi.next()) {
            String t = dp.getTerm(pi.getID()).getName().toString();
            if(t.equals(term)) {

                //
                // We won't include the term itself!
                continue;
            }
            context.add(t);
        }

        double max = 0;
        Sense maxSense = null;
        for(Sense s : senses) {
            double temp = s.score(context);
            if(maxSense == null || temp > max) {
                max = temp;
                maxSense = s;
            }
        }
        return maxSense;
    }

    public String getTerm() {
        return term;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Supervised disambiguator for: " + term);
        for(Sense s : senses) {
            sb.append("\n" + s.toString());
        }
        return sb.toString();
    }

    public List<Sense> getSenses() {
        return senses;
    }
}
