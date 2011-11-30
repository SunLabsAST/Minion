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
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import com.sun.labs.minion.retrieval.ResultImpl;
import com.sun.labs.minion.util.LogMath;
import com.sun.labs.minion.util.StopWatch;
import java.util.logging.Logger;

/**
 * An EM algorithm for unsupervised sense disambiguation.  This is based on
 * the discussion in "Foundations of Statistical Natural Language Processing"
 * by Manning and Schutze, MIT Press, 1999.
 *
 * Note that we're doing all of the math in log space here to avoid multiplying
 * lots of small numbers together.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class Unsupervised {

    /**
     * The contexts in which a term occurs.
     */
    private Context[] contexts;

    /**
     * The set of features occuring in contexts.  This is indexed by the feature
     * id.
     */
    private String[] features;

    private SearchEngine e;

    private String term;

    private String field;

    private Random rand;

    private Model model;

    private LogMath lm;

    private int numModelsPerK = 2;

    private float epsilon = 1E-15f;

    private int maxContexts;

    /**
     * The number of contexts.
     */
    int I;

    /**
     * The number of features.
     */
    int J;

    static Logger logger = Logger.getLogger(Unsupervised.class.getName());

    /**
     * Creates a disambiguator.
     */
    public Unsupervised(
            SearchEngine e,
            String term,
            String field) throws SearchEngineException {
        this(e, term, field, 20, 500, Integer.MAX_VALUE);
    }

    /**
     * Creates a disambiguator.
     */
    public Unsupervised(
            SearchEngine e,
            String term,
            String field,
            int maxContextFeat,
            int maxTotalFeat,
            int maxContexts) throws SearchEngineException {
        ResultSet rs;
        if(field == null) {
            rs = e.search(String.format("'%s'", term));
        } else {
            rs = e.search(String.format("%s <contains> '%s'", field, term));
        }

        init(rs, term, field, maxContextFeat, maxTotalFeat, maxContexts);
    }

    public Unsupervised(ResultSet rs, String term, int maxContextFeat,
            int maxTotalFeat,
            int maxContexts)
            throws SearchEngineException {
        init(rs, term, field, maxContextFeat, maxTotalFeat, maxContexts);
    }

    protected void init(ResultSet rs, String term, String field,
            int maxContextFeat, int maxTotalFeat, int maxContexts)
            throws SearchEngineException {

        e = rs.getEngine();
        this.term = term;
        this.field = field;
        lm = new LogMath(1.0001f, true);
        rand = new Random();

        //
        // Get the list of results.
        ArrayList<Context> tc = new ArrayList<Context>();
        if(rs.size() < maxContexts) {
            //
            // We want all of the contexts.
            for(Result r : rs.getAllResults(false)) {
                QueryEntry dke = ((ResultImpl) r).getKeyEntry();
                tc.add(new Context(term, field, dke, maxContextFeat));
            }
        } else {

            //
            // We have too many contexts, so choose some randomly.
            java.util.List<Result> rl = new LinkedList<Result>(rs.getAllResults(
                    false));
            while(tc.size() < maxContexts) {
                Result r = rl.remove(rand.nextInt(rl.size()));
                tc.add(new Context(term, field, ((ResultImpl) r).getKeyEntry(),
                        maxContextFeat));
            }
        }

        contexts = tc.toArray(new Context[0]);
        I = contexts.length;

        //
        // Compute the total feature set.
        Map<String, TermFreq> tfs = new HashMap<String, TermFreq>();
        for(Context c : contexts) {
            for(TermFreq ctf : c.getTerms().values()) {
                TermFreq ttf = tfs.get(ctf.term);
                if(ttf == null) {
                    ttf = new TermFreq(ctf.term, ctf.freq);
                    tfs.put(ttf.term, ttf);
                } else {
                    ttf.freq += ctf.freq;
                }
            }
        }

        PriorityQueue<TermFreq> stf = new PriorityQueue<TermFreq>(tfs.values());
        while(stf.size() > maxTotalFeat) {
            stf.poll();
        }

        features = new String[stf.size()];
        for(int i = 0; stf.size() > 0; i++) {
            features[i] = stf.poll().term;
        }
        com.sun.labs.minion.util.Util.sort(features);

        for(Context c : contexts) {
            c.setCounts(features);
        }

        J = features.length;
    }

    /**
     * Sets the number of models to compute per value of K.  This allows us
     * to generate a few models for each value of K and keep the best one.
     */
    public void setModels(int numModelsPerK) {
        this.numModelsPerK = numModelsPerK;
    }

    public Model disambiguate() {
        return disambiguate(2, 7);
    }

    /**
     * Disambiguates a term, looking for the best number of senses to use.
     *
     * @param minK the minimum number of senses to consider.
     * @param maxK the maximum number of senses to consider.
     * @return the model with the highest likelihood
     */
    public Model disambiguate(int minK, int maxK) {
        Model ret = null;
        for(int i = minK; i <= maxK; i++) {

            //
            // We'll generate some models here.
            for(int j = 0; j < numModelsPerK; j++) {
                Model m = disambiguate(i);
                if(ret == null || ret.ll < m.ll) {
                    ret = m;
                }
            }
        }
        return ret;
    }

    public Model disambiguate(int K) {
        StopWatch dw = new StopWatch();
        dw.start();
        Model prevModel = new Model();
        Model currModel = new Model(K);
        Model bestModel = new Model();
        int nIter = 0;

        //
        // OK, here's the main disambiguation loop.  Note that we let it run a
        // couple of times before we start keeping track of the best model and that
        // we'll limit ourselves to 100 iterations or whenever the difference in
        // likelihood drops below epsilon.
        //
        // We'll keep the best model generated after the first few.
        while(nIter < 100) {
            prevModel.copy(currModel);
            currModel.eStep();
            currModel.mStep();
            float diff = Math.abs(currModel.ll - prevModel.ll);
            if(nIter > 2 && currModel.ll > bestModel.ll) {
                bestModel.copy(currModel);
            }
            if(diff < epsilon && nIter > 5) {
                break;
            }
            nIter++;
        }
        model = bestModel;
        dw.stop();
        logger.info(term + " " + K + " took " + dw.getTime() + " ms for " +
                nIter + " iterations.");
        return model;
    }

    /**
     * The model associated with a particular estimation.
     */
    public class Model {

        Model() {
            pvs = new float[J][];
            pcs = new float[I][];
            h = new float[I][];
            ll = Float.NEGATIVE_INFINITY;
        }

        /**
         * Randomly initializes a model for this word with K
         * senses.
         */
        Model(int K) {
            this.K = K;
            pvs = new float[K][J];
            pcs = new float[K][I];
            ps = new float[K];
            h = new float[K][I];
            ll = Float.NEGATIVE_INFINITY;

            //
            // We'll just use non-zero random numbers to initialize without worrying
            // about whether they're real distributions.
            for(int k = 0; k < K; k++) {
                for(int j = 0; j < J; j++) {
                    while(pvs[k][j] == 0) {
                        pvs[k][j] = lm.linearToLog(rand.nextDouble());
                    }
                }
            }

            //
            // Initialize the probability of each sense.  Note that we don't
            // worry about summing to 1 here.
            for(int k = 0; k < K; k++) {
                while(ps[k] == 0) {
                    ps[k] = lm.linearToLog(rand.nextDouble());
                }
            }
        }

        /**
         * Creates a model by copying another, so that we can remember the
         * last model.
         */
        protected void copy(Model m) {
            K = m.K;
            ll = m.ll;
            ps = m.ps.clone();
            for(int k = 0; k < K; k++) {
                pvs[k] = m.pvs[k].clone();
            }
            for(int k = 0; k < K; k++) {
                pcs[k] = m.pcs[k].clone();
                h[k] = m.h[k].clone();
            }
        }

        /**
         * Computes the log likelihood of the context given this model, i.e.,
         * l(C|\mu).  Note that we transform from the log domain back to the
         * probability domain to make this computation.
         */
        public float computeLL() {
            ll = 0;
            for(int i = 0; i < I; i++) {
                float temp = lm.getLogZero();
                for(int k = 0; k < K; k++) {
                    temp = lm.addAsLinear(temp, lm.multiplyAsLinear(pcs[k][i],
                            ps[k]));
                }
                ll += temp;
            }
            return ll;
        }

        /**
         * Computes P(c<sub>i</sub> | s<sub>k</sub>) using the naive Bayes
         * assumption.
         */
        public void computePCS() {
            for(int k = 0; k < K; k++) {
                for(int i = 0; i < I; i++) {
                    pcs[k][i] = computePCS(contexts[i], k);
                }
            }
        }

        public float computePCS(Context c, int k) {
            float prod = 0;
            for(int j = 0; j < J; j++) {
                prod += lm.powerAsLinear(pvs[k][j], c.counts[j]);
            }
            return prod;
        }

        /**
         * Performs the E step of the EM algorithm, estimating the probability
         * of s<sub>k</sub> generating c<sub>i</sub>, h<sub>i,k</sub>.
         */
        public void eStep() {
            computePCS();
            for(int i = 0; i < I; i++) {
                float sum = lm.getLogZero();
                float[] prod = new float[K];
                for(int k = 0; k < K; k++) {
                    prod[k] = lm.multiplyAsLinear(ps[k], pcs[k][i]);
                    sum = lm.addAsLinear(sum, prod[k]);
                }
                for(int k = 0; k < K; k++) {
                    h[k][i] = lm.divideAsLinear(prod[k], sum);
                }
            }
        }

        /**
         * Performs the M step of the EM algorithm, re-estimating the model
         * parameters P(v<sub>j</sub> | s<sub>k</sub>) and P(s<sub>k</sum>)
         * using maximum likelihood.
         */
        public void mStep() {
            for(int k = 0; k < K; k++) {

                //
                // Recompute P(v|s_k)
                float[] sums = new float[J];
                float[] pvst = pvs[k];
                float totalSum = lm.getLogZero();
                for(int j = 0; j < J; j++) {
                    sums[j] = countSum(j, k);
                    totalSum = lm.addAsLinear(totalSum, sums[j]);
                }
                for(int j = 0; j < J; j++) {
                    pvst[j] = lm.divideAsLinear(sums[j], totalSum);
                }

                //
                // Recompute P(s_k)
                float[] ht = h[k];
                float hsum = lm.getLogZero();
                for(int i = 0; i < I; i++) {
                    hsum = lm.addAsLinear(hsum, ht[i]);
                }
                ps[k] = lm.divideAsLinear(hsum, lm.linearToLog(I));
            }
            computeLL();
        }

        protected float countSum(int j, int k) {
            float sum = lm.getLogZero();
            float[] ht = h[k];
            for(int i = 0; i < I; i++) {
                sum = lm.addAsLinear(sum, lm.multiplyAsLinear(lm.linearToLog(
                        contexts[i].counts[j]), ht[i]));
            }
            return sum;
        }

        public int getNumSenses() {
            return K;
        }

        /**
         * Disambiguates a give term found in a given context against this model
         * @param context the words that occur with the word that we want to
         * disambiguate.
         * @return the index of the sense
         * @see #getSenseLabels
         */
        public int disambiguate(String[] context) {
            com.sun.labs.minion.util.Util.sort(context);
            float[] sp = new float[K];
            for(int k = 0; k < K; k++) {
                sp[k] = ps[k];
            }

            int j = 0;
            for(String v : context) {
                int cmp = 0;
                while(j < features.length && (cmp = features[j].compareTo(v)) <
                        0) {
                    j++;
                }
                if(j >= features.length) {
                    break;
                }
                if(cmp == 0) {
                    for(int k = 0; k < K; k++) {
                        sp[k] += pvs[k][j];
                    }
                }
            }

            float max = sp[0];
            int maxp = 0;
            for(int k = 1; k < K; k++) {
                if(sp[k] > max) {
                    max = sp[k];
                    maxp = k;
                }
            }

            return maxp;
        }

        /**
         * Gets labels for the senses in the model.
         */
        public String[] getSenseLabels() {

            if(senseLabels == null) {

                //
                // Get the top features for each sense.
                TermProb[][] top = new TermProb[K][];
                int[] cp = new int[K];
                for(int k = 0; k < K; k++) {
                    top[k] = getTopFeatures(k, K + 2);
                }

                for(int k = 0; k < K; k++) {
                    while(checkLabel(k, cp, top));
                }

                senseLabels = new String[K];
                for(int k = 0; k < K; k++) {
                    senseLabels[k] = term + "/" + top[k][cp[k]].term;
                }
            }
            return senseLabels;

        }

        private boolean checkLabel(int ck, int[] cp, TermProb[][] top) {
            TermProb curr = top[ck][cp[ck]];
            for(int k = ck + 1; k < K; k++) {
                TermProb tp = top[k][cp[k]];
                if(curr.term.equals(tp.term)) {
                    if(curr.prob < tp.prob) {
                        cp[ck]++;
                        return true;
                    } else {
                        cp[k]++;
                    }
                }
            }
            return false;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("log likelihood: " + ll + "\n");
            for(int k = 0; k < K; k++) {
                TermProb[] tp = getTopFeatures(k, 5);
                sb.append("sense " + (k + 1) + String.format(" (%.3f)", lm.
                        logToLinear(ps[k])) +
                        ": [");
                for(int i = 0; i < tp.length; i++) {
                    if(i > 0) {
                        sb.append(", ");
                    }
                    sb.append(String.format("%s %f", tp[i].term, tp[i].prob));
                }
                sb.append("]\n");
            }
            return sb.toString();
        }

        /**
         * Gets the top n most probable features for a given sense.
         * @param k the number of senses
         * @param n the numer of features to retreive
         * @return the top <code>n</code> most probable features for this sense
         */
        public TermProb[] getTopFeatures(int k, int n) {

            PriorityQueue<TermProb> probs = new PriorityQueue<TermProb>(J,
                    new Comparator<TermProb>() {

                        public int compare(TermProb o1, TermProb o2) {
                            return -1 * o1.compareTo(o2);
                        }
                    });
            for(int j = 0; j < J; j++) {
                probs.offer(new TermProb(features[j], lm.logToLinear(pvs[k][j])));
            }

            ArrayList<TermProb> ret = new ArrayList<TermProb>();
            while(probs.size() > 0 && ret.size() <= n) {
                ret.add(probs.poll());
            }
            return ret.toArray(new TermProb[0]);
        }
        /**
         * The sense labels for this model.
         */
        String[] senseLabels;

        /**
         * The number of senses.
         */
        int K;

        /**
         * The log likelihood of this model.
         */
        float ll;

        /**
         * P(v<sub>j</sub> | s<sub>k</sub>).  Note that K will typically be a lot
         * smaller than J, so we store things in a K by J matrix.
         */
        float[][] pvs;

        /**
         * P(C<sub>i</sub> | s<sub>k</sub>).  Note that K will typically be a lot
         * smaller than I, so we store things in a K by I matrix.
         */
        float[][] pcs;

        /**
         * P(s<sub>k</sub>)
         */
        float[] ps;

        /**
         * The probability that s<sub>k</sub> generated c<sub>i</sub>.
         * Note that K will typically be a lot
         * smaller than I, so we store things in a K by I matrix.
         */
        float[][] h;

    }
}
