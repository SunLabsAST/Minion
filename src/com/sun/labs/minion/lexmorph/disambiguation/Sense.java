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

import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.util.LogMath;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Logger;

/**
 * A class representing a particular sense of a word.  This can be used for
 * supervised sense diambiguation.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class Sense implements Serializable {

    /**
     *  The term.
     */
    private String term;

    /**
     * The name of this sense.
     */
    private String name;

    /**
     * A map from words to the log probability of this sense, given a
     * particular vocabulary item.
     */
    private Map<String, Float> pvs;

    /**
     * The log probability of this sense.
     */
    private float ps;

    /**
     *  A log math we can use to do math on the probabilities.
     */
    transient private LogMath lm = new LogMath(1.0001f, true);

    static private Logger logger = Logger.getLogger(Sense.class.getName());

    public Sense() {
        logger.info("here!");
        lm = new LogMath(1.0001f, true);
    }

    /**
     * Creates a sense for a term.
     * @param term the term of which this is a sense (e.g., <em>bank</em>)
     * @param vocab the complete set of terms across all the senses for a term,
     * which we can use to figure out probabilities for words not in our contexts.
     * @param missingP the probability to use for a term that's not in the contexts.
     * @param name the name of the sense (e.g., <em>river</em>
     * @param totalCount the total count of occurences of this word over all
     * senses.
     * @param contexts the contexts in which this sense occurs
     * @throws com.sun.labs.minion.SearchEngineException if there are any errors
     * processing the sense.
     */
    public Sense(String term,
            Set<String> vocab,
            double missingP,
            String name,
            int totalCount,
            List<Context> contexts) throws SearchEngineException {
        this.term = term;
        this.name = name;
        lm = new LogMath(1.0001f, true);
        ps = lm.linearToLog((double) contexts.size() / totalCount);
        float pm = lm.linearToLog(missingP);

        //
        // Collect up the terms and the counts from the contexts.
        Map<String, TermFreq> counts = new HashMap<String, TermFreq>();
        for(Context c : contexts) {
            for(Map.Entry<String, TermFreq> e : c.getTerms().entrySet()) {
                TermFreq tf = counts.get(e.getKey());
                if(tf == null) {
                    tf = new TermFreq(e.getKey());
                    counts.put(e.getKey(), tf);
                }
                tf.add(e.getValue());
            }
        }

        //
        // Compute the probability of the sense given each of the words in the
        // contexts.  Note that we only use words that are in the vocabulary!
        int sum = 0;
        for(String t : vocab) {
            TermFreq tf = counts.get(t);
            if(tf != null) {
                sum += tf.getFreq();
            }
        }

        //
        // Now compute the term probabilities.  We'll add one to the term freqs
        // so that we have reasonable (?) probabilities for terms that are in the
        // vocabulary that don't occur in any context for this sense.
        pvs = new HashMap<String, Float>();
        for(String t : vocab) {
            TermFreq tf = counts.get(t);
            if(tf != null) {
                float x = lm.linearToLog((tf.getFreq() + 1.0) / sum);
                pvs.put(tf.getTerm(), x);
            } else {

                //
                // This term isn't in any context.  Give it a small probability.
                pvs.put(t, pm);
            }
        }
    }

    /**
     * Computes the probability of a given context containing the
     * that context being this sense.
     * @param context the terms in the context we're evaluating
     * @return the probability of the word in this context having this sense
     */
    public double score(List<String> context) {
        if(lm == null) {
            lm = new LogMath(1.0001f, true);
        }

        float ret = ps;
        int cterms = 0;
        for(String s : context) {
            Float p = pvs.get(s);
            if(p != null) {
                ret += p;
                cterms++;
            }
        }
        return cterms > 0 ? ret : Double.NEGATIVE_INFINITY;
    }

    @Override
    public String toString() {
        PriorityQueue<Map.Entry<String, Float>> pq = new PriorityQueue<Map.Entry<String, Float>>(pvs.size(),
                new Comparator<Map.Entry<String, Float>>() {

            @Override
                    public int compare(Map.Entry<String, Float> o1,
                            Map.Entry<String, Float> o2) {
                        if(o1.getValue() < o2.getValue()) {
                            ;
                            return -1;
                        } else if(o1.getValue() > o2.getValue()) {
                            return 1;
                        } else {
                            return o1.getKey().compareTo(o2.getKey());
                        }
                    }
                });
        for(Map.Entry<String, Float> e : pvs.entrySet()) {
            pq.offer(e);
        }
        while(pq.size() > 10) {
            pq.poll();
        }

        if(lm == null) {
            lm = new LogMath(1.0001f, true);
        }

        List<Map.Entry<String, Float>> temp =
                new ArrayList<Map.Entry<String, Float>>();
        while(pq.size() > 0) {
            temp.add(pq.poll());
        }
        Collections.reverse(temp);
        StringBuilder sb = new StringBuilder();
        sb.append(term + "/" + name + ": [");
        for(Iterator<Map.Entry<String, Float>> i = temp.iterator(); i.hasNext();) {
            Map.Entry<String, Float> e = i.next();
            sb.append(String.format("%s %f", e.getKey(), lm.logToLinear(e.
                    getValue())));
            if(i.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();

    }

    public String getTerm() {
        return term;
    }

    public String getName() {
        return name;
    }
}
