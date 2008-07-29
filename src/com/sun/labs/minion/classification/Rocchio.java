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

package com.sun.labs.minion.classification;

import java.io.RandomAccessFile;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.Progress;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;

import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.FieldedPostingsIterator;

import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;

import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.DictTerm;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.ScoredQuickOr;
import com.sun.labs.minion.retrieval.cache.TermCache;
import com.sun.labs.minion.retrieval.cache.TermCacheElement;
import com.sun.labs.minion.retrieval.TermStats;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;

import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.StopWatch;

/**
 * A classifier model that does Rocchio-style classification.
 */
public class Rocchio implements ClassifierModel, BulkClassifier, ExplainableClassifierModel {

    private String modelName;
    
    /**
     * The engine that this classifier is part of.
     */
    protected SearchEngine e;

    /**
     * A term cache to use when building classifiers.
     */
    protected TermCache tc;

    /**
     * The features that we will use for our model.
     */
    protected FeatureClusterSet features;

    /**
     * A Set of features of features
     */
    protected FeatureClusterSet clusters;

    /**
     * The similarity threshold for our classifier.
     */
    protected float threshold;

    protected float ba;

    protected float bb;

    protected float bg;

    /**
     * A manager for the partitions we're classifying against.
     */
    protected PartitionManager manager;

    /**
     * The name of the field into which our classification results will go.
     */
    protected String fieldName;

    /**
     * The name of the vectored field whose contents were used to train the
     * classifier.
     */
    protected String fromField;

    /**
     * A set of rank cutoffs to use for dynamic query zoning.
     */
    protected static int[] rankCutoff = {500, 1000, 2000, 4000};

    /**
     * Values of the beta and gamma parameters to try.
     */
    protected static float alpha[] = {0, 8};

    protected static float beta[] = {32, 64, 128};

    protected static float gamma[] = {32, 64, 128};

    protected static MinionLog log = MinionLog.getLog();

    protected static String logTag = "ROCC";

    protected DecimalFormat form = new DecimalFormat("###0.00000");

    int nFeed = 0;

    private float thresholdEpsilon = 0.001f;

    public Rocchio() {
    } // Rocchio constructor
    

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    public float getThreshold() {
        return threshold;
    }

    // Implementation of com.sun.labs.minion.classification.ClassifierModel
    /**
     * Gets combined stats for a term cluster, priming the term cache, if
     * necessary.
     */
    protected TermStats getTermStats(FeatureCluster fc) {
        TermStats ts = tc.getTermStats(fc.getName());
        if(ts == null) {

            //
            // If we didn't have term stats, then our cache hasn't been filled with
            // the components of this cluster.  Do that now.
            ts = new TermStats(fc.getName());
            for(Iterator i = manager.getActivePartitions().iterator(); i.hasNext();) {
                TermCacheElement tce = tc.get(fc.getName(),
                        (DiskPartition) i.next());
                for(Iterator j = fc.getContents().iterator(); j.hasNext();) {
                    tce.add((WeightedFeature) j.next());
                }
                ts.add(tce.getTermStats());
            }
            tc.setTermStats(ts);
        }
        return ts;
    }

    /**
     * Trains the classifier on a set of documents.  Training a Rocchio
     * classifier consists of the following set of steps:
     *
     * <ol>
     *
     * <li> Select the features upon which to base the classifier.
     *
     * <li> Build a results set by computing the or of the selected
     * features.
     *
     * <li> Take the top R documents from this results set, where R is the
     * size of the document set given for training purposes.  This is the
     * <em>query zone</em>.
     *
     * <li> Create two document vectors, <em>rel</em> and <em>nonrel</em>
     *
     * <li> For each document, <em>d</em> in the query zone:
     *
     * <ol>
     *
     * <li> If <em>d</em> is in the training set, then add <em>d</em> to
     * <em>rel</em>.
     *
     * <li> If <em>d</em> is not in the training set, then add <em>d</em> to
     * <em>nonrel</em>.
     *
     * </ol>
     *
     * <li> The <em>feedback query</em> is computed as beta/R * <em>rel</em> -
     * <em> gamma/(N-R) * <em>nonrel</em>, where R is the number of
     * training documents.
     *
     * </ol>
     *
     * @param name The name of the class, as specified by the application.
     * @param manager the manager for the partitions against which we're
     * training
     * @param training A set of results containing the training documents for
     * the class.
     * @param selectedFeatures the set of features to use when training this classifier
     */
    public void train(String name,
            String fieldName,
            PartitionManager manager,
            ResultSetImpl training,
            FeatureClusterSet selectedFeatures,
            TermCache tc,
            Progress progress) throws SearchEngineException {

        this.fieldName = fieldName;
        this.manager = manager;
        this.tc = tc;

        nextStep(progress, "Determining query zone");

        //
        // We'll need a weighting function and a set of weighting
        // components for a lot of stuff.
        WeightingFunction wf = tc.getWeightingFunction();
        WeightingComponents wc = tc.getWeightingComponents();

        //
        // Now, the features that were selected have weights generated by
        // the computation in the feature selector.  We need a set of
        // features that have weights generated from the term weights in
        // the collection.  We'll calculate those now.
        //
        // In the Rocchio formulation, we need a vector that contains the
        // query feature weights, which is exactly the vector that we want
        // to create for running queries, so we'll build that now too.
        FeatureClusterSet cwFeatures = new FeatureClusterSet();
        WeightedFeatureVector qFeatures =
                new WeightedFeatureVector(selectedFeatures.size());

        for(FeatureCluster c : selectedFeatures) {

            //
            // Get the term stats associated with the term cluster.
            TermStats ts = getTermStats(c);

            //
            // Copy the current cluster and set its weight using the term stats
            // that we just collected.
            FeatureCluster cwf = c.copy();
            cwf.setWeight(wf.initTerm(wc.setTerm(ts)));
            cwFeatures.add(cwf);
            qFeatures.add(cwf.copy());
        }

        //
        // We want to normalize the features in our set of weighted feature
        // clusters so that we don't have to keep normalizing everything.
        cwFeatures.normalize();

        //
        // We're going to iterate through the training set partition by
        // partition, since that will allow us to do the maximal number of
        // things using integer IDs, rather than strings.
        //
        // For each partition, we'll build a big query from all of the
        // features that were selected.  The big query will be doing
        // lookups of all the terms in all of the feature clusters, and
        // we'll cache the integer IDs for those terms so that later we can
        // match them against the terms that we find in documents in the
        // result set that we'll build from the big query.
        //
        // The results of the big query will be the basis for a heap of big
        // queries from each partition.  We'll build that heap now.
        PriorityQueue<HE> h = new PriorityQueue<HE>();
        List<HE> featureVecs = new ArrayList<HE>();
        for(Iterator<ArrayGroup> i = training.resultsIterator(); i.hasNext();) {
            ArrayGroup tg = i.next();
            BigQuery bq = new BigQuery(tc, tg, fromField, wf, wc);

            //
            // Add all of our selected features to the query.  Note that
            // we're using the features that have collection derived
            // weights, not feature selection derived weights!
            bq.addFeatureClusters(cwFeatures);

            //
            // Make an element for our heap out of this big query, and
            // remember the element in a list.
            HE el = new HE(cwFeatures, bq);
            if(el.next()) {
                featureVecs.add(el);
                h.offer(el);
            }
        }

        nextStep(progress, "Building optimal query");

        //
        // We want to keep a few counts around.
        int nRel = 0;
        int nNonRel = 0;
        int pos = 1;
        float maxF1 = Float.MIN_VALUE;
        DiskPartition maxPart = null;
        PostingsIterator maxPI = null;
        QueryEntry maxDE = null;
        int maxPos = 0;

        //
        // The number of docs in the training set and in the whole
        // collection.
        int R = training.size();
        int N = training.getEngine().getNDocs();

        WeightedFeatureVector bestVector = null;
        float maxAvgP = Float.MIN_VALUE;

        //
        // We'll iterate through the ranks in our rank cutoff array, seeing
        // at each rank how well we're doing against the training data.
        for(int k = 0; k < rankCutoff.length && h.size() > 0; k++) {

            //
            // If we're out of results, then we're done.
            if(h.size() == 0) {
                break;
            }

            //
            // Otherwise, we'll go up to the next rank cutoff.
            int K = rankCutoff[k];

            //
            // We now want to build the optimal query by finding the sum of the
            // feature vectors for the relevant and non relevant documents in
            // our query zone.
            //
            // We'll do this by iterating through the top K documents in the
            // results set, adding the positive and negative examples to the
            // appropriate feature vectors.
            while(h.size() > 0 && pos < K) {

                //
                // Get the top element of the heap.
                HE el = h.poll();

                //
                // Make sure the current document gets handled.
                if(el.handleCurr()) {
                    nRel++;
                } else {
                    nNonRel++;
                }

                //
                // Advance the top element's iterator, and if there are more
                // documents, put it back on the heap.
                if(el.next()) {
                    h.offer(el);
                }
                pos++;
            }

            if(nRel == 0 || nNonRel == 0) {
                continue;
            }

            //
            // Now we want to combine the per-partition feature weights into
            // cross partition feature weights.
            float[] positive = new float[selectedFeatures.size()];
            float[] negative = new float[selectedFeatures.size()];
            for(HE el : featureVecs) {
                for(int j = 0; j < el.pos.length; j++) {
                    positive[j] += el.pos[j];
                }

                for(int j = 0; j < el.neg.length; j++) {
                    negative[j] += el.neg[j];
                }
            }

            //
            // Now we have feature vectors for the relevant and non-relevant
            // documents.  Let's get the cross-partition vectors for these
            // documents.
            WeightedFeatureVector relVec = new WeightedFeatureVector(cwFeatures,
                    positive);
            WeightedFeatureVector nonRelVec =
                    new WeightedFeatureVector(cwFeatures, negative);

            //
            // We need to compute the feedback query by subtracting these
            // vectors.  This relies on the beta and gamma values which
            // modify the relative importance of positive and negative
            // examples.  We'll look for the best combination.
            alphaLoop:
            for(int a = 0; a < alpha.length; a++) {

                //
                // Alpha times the query features.
                WeightedFeatureVector qa = qFeatures.mult(alpha[a]);

                betaLoop:
                for(int b = 0; b < beta.length; b++) {

                    //
                    // Beta times the relevant features.
                    WeightedFeatureVector brel =
                            relVec.mult(beta[b] / (float) nRel);

                    gammaLoop:
                    for(int g = 0; g < gamma.length; g++) {

                        //
                        // We'll exclude some cases.
                        if(beta[b] != 1 && beta[b] == gamma[g]) {
                            continue;
                        }

                        //
                        // Gamma times the non-relevant features.
                        WeightedFeatureVector gnonrel =
                                nonRelVec.mult(gamma[g] / (float) nNonRel);


                        //
                        // Compute the feedback query.
                        WeightedFeatureVector diff =
                                qa.add(brel.sub(gnonrel, true), 1, 1, true);

                        //
                        // If the difference vector is empty, then onto the next
                        // value for beta.
                        if(diff.size() == 0) {
                            continue betaLoop;
                        }

                        diff.normalize();

                        //
                        // Get the 11 point average precision for this query
                        // vector.
                        FQR fqr = runFeedback(
                                cwFeatures,
                                diff,
                                featureVecs,
                                R, wf, wc);

                        if(fqr.avgP() > maxAvgP) {
                            maxAvgP = fqr.avgP();
                            threshold = fqr.getThreshold();
                            bestVector = diff;
                            ba = alpha[a];
                            bb = beta[b];
                            bg = gamma[g];
                        }
                    }
                }
            }
        }

        //
        // Now we just need to make a set of features.
        if(bestVector == null) {
            features = new FeatureClusterSet();
        } else {
            features = bestVector.getSet();
            features.removeZero();
        }

    }

    public void setEngine(SearchEngine e) {
        this.e = e;
    }

    public float checkThreshold(float score) {
        return score < threshold ? -score : score;
    //        float diff = threshold - score;
    //        if(diff < 0) {
    //            return score;
    //        }
    //        return diff < 0.01f ? score : -score;
    }

    public float similarity(String key) {

        DocKeyEntry dke =
                (DocKeyEntry) ((SearchEngineImpl) e).getDocumentTerm(key);
        if(dke == null) {
            return 0;
        }

        //
        // Make a map from terms to frequencies in this document, by iterating through the postings.
        Map<String, Integer> terms = new HashMap<String, Integer>();
        PostingsIterator pi = dke.iterator(null);
        if(pi == null) {
            return 0;
        }

        while(pi.next()) {
            QueryEntry qe =
                    ((DiskPartition) dke.getPartition()).getTerm(pi.getID());
            terms.put(qe.getName().toString(),
                    pi.getFreq());
        }

        WeightingFunction wf =
                ((SearchEngineImpl) e).getPM().getQueryConfig().
                getWeightingFunction();
        WeightingComponents wc =
                ((SearchEngineImpl) e).getPM().getQueryConfig().
                getWeightingComponents();

        //
        // Now, iterate through the clusters in this classifier, building up a set of term stats and
        // then figuring out cluster weights.
        float sum = 0;
        for(Iterator i = features.getContents().iterator(); i.hasNext();) {
            FeatureCluster fc = (FeatureCluster) i.next();
            TermStats clusterStats = new TermStats(fc.getName());
            int fdt = 0;
            for(Iterator j = fc.getContents().iterator(); j.hasNext();) {
                Feature f = (Feature) j.next();
                Integer df = terms.get(f.getName());
                fdt += (df == null ? 0 : df);
                clusterStats.add(wc.setTerm(f.getName()).getTermStats());
            }

            //
            // OK, now if the terms in this cluster actually occurred in this document, then figure
            // out the weight for this cluster in this document and then add it to the sum that we're
            // building.
            if(fdt > 0) {
                wc.setTerm(clusterStats);
                wf.initTerm(wc);
                wc.fdt = fdt;
                sum += wf.termWeight(wc) * fc.getWeight();
            }
        }
        sum /= dke.getDocumentVectorLength();
        return checkThreshold(sum);
    }

    public float similarity(DocumentVector v) {
        return similarity(v.getKey());
    }
    
    public float similarity(ClassifierModel cm) {
        float sum = 0;
        FeatureClusterSet fcs = cm.getFeatures();
        for(FeatureCluster fc : fcs) {
             FeatureCluster ourFC = features.get(fc.getName());
            if(ourFC != null) {
                sum += fc.getWeight() * ourFC.getWeight();
            }
        }
        return sum;
    }

    public String explain(String key, boolean includeDocTerms) {

        DocKeyEntry dke =
                (DocKeyEntry) ((SearchEngineImpl) e).getDocumentTerm(key);
        if(dke == null) {
            return "Document key: " + key + " not found";
        }

        //
        // Make a map from terms to frequencies in this document, by iterating through the postings.
        Map<String, Integer> terms = new HashMap<String, Integer>();
        PostingsIterator pi = dke.iterator(null);
        if(pi == null) {
            return "Document " + key + " has no text";
        }

        StringBuilder exp = new StringBuilder();
        if(includeDocTerms) {
            exp.append("|Document terms |");
        }
        while(pi.next()) {
            QueryEntry qe =
                    ((DiskPartition) dke.getPartition()).getTerm(pi.getID());
            terms.put(qe.getName().toString(),
                    pi.getFreq());
            if(includeDocTerms) {
                exp.append(String.format(" (%s,%d)", qe.getName(), pi.getFreq()));
            }
        }
        if(includeDocTerms) {
            exp.append("|\n\n");
        }

        WeightingFunction wf =
                ((SearchEngineImpl) e).getPM().getQueryConfig().
                getWeightingFunction();
        WeightingComponents wc =
                ((SearchEngineImpl) e).getPM().getQueryConfig().
                getWeightingComponents();

        //
        // Now, iterate through the clusters in this classifier, building up a set of term stats and
        // then figuring out cluster weights.
        float sum = 0;
        int nTerms = 0;
        exp.append(String.format("|%-20s |%9s|%9s|%9s|\n", "*Feature Name*",
                "*Document Weight*", "*Feature Weight*", "*Product*"));
        for(Iterator i = features.getContents().iterator(); i.hasNext();) {
            FeatureCluster fc = (FeatureCluster) i.next();
            TermStats clusterStats = new TermStats(fc.getName());
            int fdt = 0;
            for(Iterator j = fc.getContents().iterator(); j.hasNext();) {
                Feature f = (Feature) j.next();
                Integer df = terms.get(f.getName());
                fdt += (df == null ? 0 : df);
                clusterStats.add(wc.setTerm(f.getName()).getTermStats());
            }

            //
            // OK, now if the terms in this cluster actually occurred in this document, then figure
            // out the weight for this cluster in this document and then add it to the sum that we're
            // building.
            if(fdt > 0) {
                nTerms++;
                wc.setTerm(clusterStats);
                wf.initTerm(wc);
                wc.fdt = fdt;
                exp.append(String.format("|%-20s |%9.5f|%9.5f|%9.5f|\n",
                        fc.getName(), wf.termWeight(wc),
                        fc.getWeight(), wf.termWeight(wc) * fc.getWeight()));
                sum += wf.termWeight(wc) * fc.getWeight();
            }
        }

        //
        // If we didn't find any terms, then just return null.
        if(nTerms == 0) {
            return null;
        }
        sum /= dke.getDocumentVectorLength();
        exp.append(String.format("|%-20s |||%27.5f|\n", "Normalized sum:", sum));

        exp.append("Document is" + (checkThreshold(sum) > 0 ? " " : " not ") +
                "in class<br>\n");
        return exp.toString();
    }

    public List<WeightedFeature> explain(String key) {
        DocKeyEntry dke =
                (DocKeyEntry) ((SearchEngineImpl) e).getDocumentTerm(key);
        List<WeightedFeature> ret = new ArrayList<WeightedFeature>();
        if(dke == null) {
            return ret;
        }

        //
        // Make a map from terms to frequencies in this document, by 
        // iterating through the postings for the from field.
        Map<String, Integer> terms = new HashMap<String, Integer>();
        QueryConfig qc = ((SearchEngineImpl) e).getPM().getQueryConfig();
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures(
                qc.getWeightingFunction(),
                qc.getWeightingComponents());
        WeightingFunction wf = feat.getWeightingFunction();
        WeightingComponents wc = feat.getWeightingComponents();
        feat.setFields(e.getPM().getMetaFile().getFieldArray(fromField));
        PostingsIterator pi = dke.iterator(null);
        if(pi == null) {
            return ret;
        }

        while(pi.next()) {
            QueryEntry qe =
                    ((DiskPartition) dke.getPartition()).getTerm(pi.getID());
            terms.put(qe.getName().toString(),
                    pi.getFreq());
        }

        //
        // Now, iterate through the clusters in this classifier, building up a set of term stats and
        // then figuring out cluster weights.
        float sum = 0;
        int nTerms = 0;
        for(Iterator i = features.getContents().iterator(); i.hasNext();) {
            FeatureCluster fc = (FeatureCluster) i.next();
            TermStats clusterStats = new TermStats(fc.getName());
            int fdt = 0;
            
            //
            // We want a non-stemmed name for this feature, so we'll choose the
            // shortest name in the cluster as the "root" to return with the
            // explanation.
            for(Iterator j = fc.getContents().iterator(); j.hasNext();) {
                Feature f = (Feature) j.next();
                Integer df = terms.get(f.getName());
                fdt += (df == null ? 0 : df);
                clusterStats.add(wc.setTerm(f.getName()).getTermStats());
            }

            //
            // OK, now if the terms in this cluster actually occurred in this document, then figure
            // out the weight for this cluster in this document and then add it to the sum that we're
            // building.
            if(fdt > 0) {
                nTerms++;
                wc.setTerm(clusterStats);
                wf.initTerm(wc);
                wc.fdt = fdt;
                float contrib = wf.termWeight(wc) * fc.getWeight();
                ret.add(new WeightedFeature(fc.getHumanReadableName(), contrib));
                sum += contrib;
            }
        }

        //
        // Turn the weights into a proportion of contribution.
        for(WeightedFeature f : ret) {
            f.setWeight(f.getWeight() / sum);
        }
        Collections.sort(ret, WeightedFeature.invWeightComparator);

        //
        // If we didn't find any terms, then just return an empty list.
        if(nTerms == 0) {
            return ret;
        }
        return ret;
    }

    /**
     * Runs a feedback query with the current estimate of the optimal
     * query.
     *
     * @param opt the current optimal query, as calculated by the vector
     * difference.
     * @param queryZone a query zone that we can use to drive per-partition
     * processing
     * @param nRel the number of relevant documents, which is the number of
     * training examples
     * @return the average precision on the query.
     */
    protected FQR runFeedback(
            FeatureClusterSet cwFeatures,
            WeightedFeatureVector opt,
            List queryZone,
            int nRel,
            WeightingFunction wf,
            WeightingComponents wc) {

        nFeed++;

        //
        // We'll run the given features as a big query against the current
        // set of partitions and heap the results.
        PriorityQueue<HE> h = new PriorityQueue<HE>();
        for(Iterator i = queryZone.iterator(); i.hasNext();) {
            BigQuery q = new BigQuery(((HE) i.next()).bq);
            for(int j = 0; j < opt.nFeat; j++) {
                q.addFeatureCluster(opt.v[j]);
            }
            HE el = new HE(cwFeatures, q);
            if(el.next()) {
                h.offer(el);
            }
        }

        //
        // Now, we'll work through the heap, calculating feedback query
        // results.
        FQR fqr = new FQR(nRel);

        while(h.size() > 0 && !fqr.done()) {

            //
            // Get the top element of the heap.
            HE el = h.poll();

            //
            // Get the document ID for this element.
            Integer docID = new Integer(el.iter.getDoc());

            //
            // If this is a training document, then calculate the
            // recall and precision.
            if(el.bq.trainingIDs.contains(docID) ||
                    el.bq.seenIDs.contains(docID)) {
                fqr.addRel(el.iter.getScore());
            } else {
                fqr.addNonRel(el.iter.getScore());
            }

            //
            // Go onto the next document, if there is one.
            if(el.next()) {
                h.offer(el);
            }
        }

        fqr.finish();
        return fqr;
    }

    /**
     * Gets the features that this classifier model will be using for
     * classification.  This method must return a set containing instances
     * of {@link Feature}.
     *
     * @return A set of features that will be used for classification.
     * @see Feature
     */
    public FeatureClusterSet getFeatures() {
        return features;
    }

    /**
     * Gets a single feature of the type that this classifier model uses.
     * This feature will be filled in from the data stored for the
     * classifier.
     *
     * @return a new feature to be used during classification
     */
    public Feature getFeature() {
        return new WeightedFeature();
    }

    /**
     * Dumps any classifier specific data to the given file.  Currently
     * this writes out the threshold for our classifier.
     *
     * @param raf The file to which the data can be dumped.
     */
    public void dump(RandomAccessFile raf) throws java.io.IOException {
        raf.writeFloat(threshold);
        raf.writeFloat(ba);
        raf.writeFloat(bb);
        raf.writeFloat(bg);
    }

    /**
     * Sets the feature clusters that the classifier model will use for
     * classification.
     *
     * @param f the set of features.
     * @see FeatureCluster
     */
    public void setFeatures(FeatureClusterSet f) {
        this.features = f;
    }

    /**
     * Reads any classifier specific data from the given file.
     *
     * @param raf The file from which the data can be read.  The file will
     * be positioned appropriately so that the data can be read.
     */
    public void read(RandomAccessFile raf) throws java.io.IOException {
        threshold = raf.readFloat();
        ba = raf.readFloat();
        bb = raf.readFloat();
        bg = raf.readFloat();
    }

    /**
     * Classifies a set of documents.
     *
     * For a Rocchio classifier the classification process is as follows:
     *
     * <ol>
     *
     * <li> For each term in our feature set, get the term from the
     * provided dictionary
     *
     * <li> Iterate through the postings for the term, multiplying the term
     * weights by the feature weight.
     *
     * <li> Collect the per-document scores.  If a score exceeds our
     * threshold, classify that document into the class.
     *
     * </ol>
     *
     * @param sdp a disk partition representing the recently dumped
     * documents.
     * @return An array of float.  For a given document ID in the documents
     * that were classified, if that element of the array is greater than
     * 0, then the document should be classified into that class.  The
     * absolute value of the element indicates the similarity of that
     * document to the classifier model.
     */
    public float[] classify(DiskPartition sdp) {

        //
        // We have feature clusters, which are clusters of terms.  We need
        // to combine the statistics for these terms across the partitions
        // currently in the index and across the clusters.
        //
        // This is a two stage process.  First, we'll process the postings for
        // the terms in the clusters and build a set of raw counts for the terms
        // in the clusters in the documents in the new partitions.  Second, we'll
        // use those counts to generate term statistics for the clusters.  This
        // will give use the "DF" portion of the weighting function.  The counts
        // themselves will give us the "TF" portion of the weighting function.
        //
        // Once we have the counts, we can compute the scores for the documents.
        float[] scores = new float[sdp.getMaxDocumentID() + 1];
        float clen = 0;

        //
        // Now we have cross-cluster term stats and term counts.  Let's
        // calculate the weights.
        WeightingFunction wf =
                sdp.getManager().getQueryConfig().getWeightingFunction();
        WeightingComponents wc =
                sdp.getManager().getQueryConfig().getWeightingComponents();
        if(wc.N == 0) {
            wc.N = sdp.getNDocs();
        }

        //
        // If we have a defined from field, then we need to make postings
        // iterator features to get data just from that field.
        PostingsIteratorFeatures feat = null;
        if(fromField != null) {
            feat = new PostingsIteratorFeatures(wf, wc);
            feat.setFields(sdp.getManager().getMetaFile().getFieldArray(fromField));
        }

        for(FeatureCluster cluster : features) {

            //
            // We'll collect stats across all the terms in this cluster.
            TermStats clusterStats = new TermStats(cluster.getName());
            clen += (cluster.getWeight() * cluster.getWeight());

            int[] counts = getCounts(cluster, feat, clusterStats, sdp);

            for(int j = 0; j < scores.length; j++) {
                if(counts[j] > 0) {
                    wc.fdt = counts[j];
                    scores[j] += wf.termWeight(wc) * cluster.getWeight();
                }
            }
        }

        //
        // Now we have the scores for each document.  They need to be
        // normalized by the length of the document vector and by the length
        // of the classifier vector.
        int fromFieldID =
                sdp.getManager().getMetaFile().getVectoredFieldID(fromField);
        clen = (float) Math.sqrt(clen);
        for(int i = 0; i < scores.length; i++) {
            if(scores[i] > 0) {
                scores[i] /=
                        (clen * sdp.getDocumentVectorLength(i, fromFieldID));

                //
                // If the current score is lower than the threshold, then make
                // it negative, to indicate a failed classification.
                scores[i] = checkThreshold(scores[i]);
            }
        }

        return scores;
    }

    public float[][] classify(String fromField, ClassifierDiskPartition cdp,
            DiskPartition sdp) {
        StopWatch sw = new StopWatch();
        sw.start();
        Map<String, ClassificationFeature> cfeat = cdp.invert();
        ClassifierModel[] models = cdp.getAllModels();
        sw.stop();
        
        float[][] scores = new float[models.length][];
        float[] clen = new float[models.length];

        WeightingFunction wf =
                sdp.getManager().getQueryConfig().getWeightingFunction();
        WeightingComponents wc =
                sdp.getManager().getQueryConfig().getWeightingComponents();
        if(wc.N == 0) {
            wc.N = sdp.getNDocs();
        }

        //
        // If we have a defined from field, then we need to make postings
        // iterator features to get data just from that field.
        PostingsIteratorFeatures feat = null;
        if(fromField != null) {
            feat = new PostingsIteratorFeatures(wf, wc);
            feat.setFields(sdp.getManager().getMetaFile().getFieldArray(fromField));
        }
        //
        // Handle all the features at once.
        for(ClassificationFeature cf : cfeat.values()) {
            
            FeatureCluster cluster = cf.getCluster();
            
            //
            // We'll collect stats across all the terms in this cluster.
            TermStats clusterStats = new TermStats(cluster.getName());

            //
            // Get the counts from the partition we're classifying.  These'll 
            // be the same for all classifiers.
            int[] counts = getCounts(cluster, feat, clusterStats, sdp);
            
            //
            // Modify the scores arrays for the classifiers that had this feature.
            for(ClassifierScore cs : cf.getScores()) {
                float[] temp = scores[cs.getID()];
                if(temp == null) {
                    temp = new float[sdp.getMaxDocumentID() + 1];
                    scores[cs.getID()] = temp;
                }
                
                //
                // Keep track of the classifier lengths.
                clen[cs.getID()] += cs.getScore() * cs.getScore();
                for(int j = 0; j < temp.length; j++) {
                    if(counts[j] > 0) {
                        wc.fdt = counts[j];
                        temp[j] += wf.termWeight(wc) * cs.getScore();
                    }
                }
            }
        }
        
        //
        // Now we have the scores for each classifier and each document.  They need to be
        // normalized by the length of the document vector and by the length
        // of the classifier vector.
        int fromFieldID =
                sdp.getManager().getMetaFile().getVectoredFieldID(fromField);
        for(int i = 0; i < scores.length; i++) {
            Rocchio rocc = (Rocchio) models[i];
            float[] temp = scores[i];
            if(temp == null) {
                continue;
            }
            clen[i] = (float) Math.sqrt(clen[i]);
            for(int j = 0; j < temp.length; j++) {
                if(temp[j] > 0) {
                    temp[j] /=
                            (clen[i] * sdp.getDocumentVectorLength(j,
                            fromFieldID));

                    //
                    // If the current score is lower than the threshold, then make
                    // it negative, to indicate a failed classification.
                    temp[j] = rocc.checkThreshold(temp[j]);
                }
            }
        }
        
        return scores;
    }

    private int[] getCounts(FeatureCluster fc, 
            PostingsIteratorFeatures feat, 
            TermStats clusterStats,
            DiskPartition sdp) {
        int[] counts = new int[sdp.getMaxDocumentID() + 1];
        int[] fields = feat.getFields();
        WeightingFunction wf = feat.getWeightingFunction();
        WeightingComponents wc = feat.getWeightingComponents();
        for(Feature f : fc) {

            QueryEntry e = sdp.getTerm(f.getName());

            //
            // Add the stats for this feature from the other partitions in the
            // index.
            clusterStats.add(wc.setTerm(f.getName()).getTermStats());

            if(e == null) {
                continue;
            }

            PostingsIterator pi = e.iterator(feat);
            while(pi.next()) {
                if(feat != null) {
                    int[] ff = ((FieldedPostingsIterator) pi).getFieldFreq();
                    int sum = 0;
                    for(int j = 0; j < fields.length && j < ff.length; j++) {
                        if(fields[j] > 0) {
                            sum += ff[j];
                        }
                    }
                    counts[pi.getID()] += sum;
                } else {
                    counts[pi.getID()] += pi.getFreq();
                }
            }
        }

        //
        // Now that we have raw counts for the documents, we can compute
        // term statistics for this cluster in this partition.
        for(int j = 0; j < counts.length; j++) {
            if(counts[j] > 0) {
                clusterStats.add(1, counts[j]);
            }
        }
        wf.initTerm(wc.setTerm(clusterStats));
        
        return counts;
    }

    public ResultSet findSimilar() {
        return findSimilar(fromField);
    }

    /**
     * Finds the documents that are most similar to this classifier, whether
     * they are in the class or not.
     */
    public ResultSet findSimilar(String fromField) {

        PartitionManager m = e.getManager();

        //
        // Now we have cross-cluster term stats and term counts.  Let's
        // calculate the weights.
        QueryConfig qc = m.getQueryConfig();
        WeightingFunction wf = qc.getWeightingFunction();
        WeightingComponents wc = qc.getWeightingComponents();

        //
        // If we have a defined from field, then we need to make postings
        // iterator features to get data just from that field.
        PostingsIteratorFeatures feat = null;
        int field = -1;
        if(fromField != null) {
            feat = new PostingsIteratorFeatures(wf, wc);
            int[] fields = m.getMetaFile().getFieldArray(fromField);
            for(int i = 0; i < fields.length; i++) {
                if(fields[i] > 0) {
                    field = i;
                    break;
                }
            }
            feat.setFields(fields);
        }

        List<ArrayGroup> groups = new ArrayList<ArrayGroup>();
        for(DiskPartition dp : (List<DiskPartition>) m.getActivePartitions()) {
            Set<String> terms = new HashSet<String>();
            List<QueryEntry> qes = new ArrayList<QueryEntry>();
            int estSize = 0;
            for(FeatureCluster cluster : (Set<FeatureCluster>) features.getContents()) {
                for(Feature f : (Set<Feature>) cluster.getContents()) {

                    //
                    // Get the morphological variants and build a single list
                    // of query elements to process.
                    DictTerm dt = new DictTerm(f.getName());
                    dt.setQueryConfig(qc);
                    dt.setDoMorph(true);
                    dt.setPartition(dp);
                    QueryEntry[] tqe = dt.getTerms();
                    for(int i = 0; i < tqe.length; i++) {
                        if(!terms.contains(tqe[i].getName().toString())) {
                            terms.add(tqe[i].getName().toString());
                            qes.add(tqe[i]);
                            estSize += tqe[i].getN();
                        }
                    }
                }
            }

            //
            // Combine the term scores for this partition.
            ScoredQuickOr qor = new ScoredQuickOr(dp, estSize);
            if(field > 0) {
                qor.setField(field);
            }
            Collections.sort(qes);
            for(QueryEntry qe : qes) {
                wc.setTerm((String) qe.getName());
                wf.initTerm(wc);
                PostingsIterator pi = qe.iterator(feat);
                if(pi == null) {
                    continue;
                }
                qor.add(pi);
            }

            ArrayGroup ag = qor.getGroup();
            ag.normalize();
            groups.add(ag);
        }
        ResultSetImpl ret = new ResultSetImpl(e, qc, groups);
        ret.setSortSpec("-score");
        return ret;
    }

    public ClassifierModel newInstance() {
        return new Rocchio();
    }

    protected void nextStep(Progress p, String str) {
        if(p != null) {
            p.next(str);
        }
    }

    public String describe() {
        List<FeatureCluster> fw = new ArrayList<FeatureCluster>();
        fw.addAll(features.contents);
        Collections.sort(fw, new Comparator<FeatureCluster>() {

            public int compare(FeatureCluster o1, FeatureCluster o2) {
                if(o2.getWeight() > o1.getWeight()) {
                    return 1;
                }

                if(o2.getWeight() < o1.getWeight()) {
                    return -1;
                }

                return 0;
            }
        });
        StringBuilder desc = new StringBuilder();
        desc.append(String.format("Threshold: %.5f\n\n%d feature clusters (by importance):\n\n",
                threshold, features.contents.size()));
        desc.append("%%sortable\n");
        desc.append("||Feature Name||Weight||Contents\n");
        for(Iterator i = fw.iterator(); i.hasNext();) {
            FeatureCluster fc = (FeatureCluster) i.next();
            desc.append(String.format("|%-30s |%5.3f |", fc.getName(),
                    fc.getWeight()));
            for(Iterator j = fc.getContents().iterator(); j.hasNext();) {
                WeightedFeature f = (WeightedFeature) j.next();
                desc.append(f.getName());
                if(j.hasNext()) {
                    desc.append(", ");
                }
            }
            desc.append("\n");
        }
        return desc.toString();
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public void setFromField(String fromField) {
        this.fromField = fromField;
    }

    public String getFromField() {
        return fromField;
    }

    /**
     * A class to hold a single element of the heap that we'll use to
     * negotiate the results of queries.
     */
    protected class HE implements Comparable<HE> {

        public BigQuery bq;

        public FeatureClusterSet fcs;

        public WeightingFunction wf;

        public WeightingComponents wc;

        public ArrayGroup.DocIterator iter;

        public float[] pos;

        public float[] neg;

        /**
         * Creates an element for the heap for a given array group.
         *
         * @param bq the query for which we're building centroids
         */
        public HE(FeatureClusterSet fcs, BigQuery bq) {
            this.fcs = fcs;
            this.bq = bq;
            pos = new float[fcs.size()];
            neg = new float[fcs.size()];
            iter = bq.getGroup().iterator();
        }

        /**
         * Advances our iterator, returning true if there is a next
         * element.
         */
        public boolean next() {
            return iter.next();
        }

        /**
         * Handles the document at the head of the iterator.
         *
         * @return <code>true</code> if the document was in the training
         * set, <code>false</code> otherwise
         */
        public boolean handleCurr() {
            int curr = iter.getDoc();

            if(bq.trainingIDs.remove(curr)) {
                add(pos, curr);
                bq.seenIDs.add(curr);
                return true;
            } else {
                add(neg, curr);
                return false;
            }
        }

        /**
         * Finishes off any remaining training documents.
         */
        public void finish() {
            for(Integer did : bq.trainingIDs) {
                add(pos, did);
            }
        }

        /**
         * Adds a document to one of the centroids that we're maintaining.
         *
         * @param docID the ID of the document to add
         */
        private void add(float[] scores, int docID) {
            for(int i = 0; i < bq.featureIterators.length; i++) {
                if(bq.featureIterators[i].findID(docID)) {
                    scores[i] += bq.featureIterators[i].getWeight();
                }
            }
        }

        /**
         * Our comparison will be based on the iterators, but we'll return the
         * opposite of their result, since we want to be used in a max heap.
         */
        public int compareTo(HE o) {
            return -iter.compareTo(o.iter);
        }
    }

    /**
     * A class to collate and hold the results of a feedback query.
     */
    protected class FQR {

        /**
         * The number of retrieved documents at which we'll measure our
         * levels.
         */
        protected int rl[];

        /**
         * 11 precision points.
         */
        protected float ppts[];

        /**
         * The f1 measure at our precision points.
         */
        protected float f1pts[];

        /**
         * The similarities corresponding to our precision points.
         */
        protected float sim[];

        /**
         * The delta for the recall levels
         */
        protected float rd;

        /**
         * The last calculated precision.
         */
        protected float p;

        /**
         * The last calculated recall.
         */
        protected float r;

        /**
         * The current position in our arrays.
         */
        protected int pos;

        /**
         * The maximum f1 encountered.
         */
        protected float maxF1;

        /**
         * The similarity associated with the maximum f1 measure.
         */
        protected float maxF1Sim;

        /**
         * The number of relevant docs encountered.
         */
        protected int rel;

        /**
         * The number of non-relevant docs encountered.
         */
        protected int nonrel;

        /**
         * The total number of relevant docs.
         */
        protected int R;

        public FQR(int R) {
            this.R = R;

            //
            // If we have at least 10 documents, we can get an
            // 11 point average precision.  If we have less than
            // 10, we should user fewer points and divide up the
            // space evenly.
            int min = Math.min(10, R);
            int incr = R / min;

            rl = new int[min + 1];
            ppts = new float[min + 1];
            f1pts = new float[min + 1];
            sim = new float[min + 1];

            //
            // Set up the counts of relevant documents where we'll measure
            // our precision points.
            for(int i = 0,  c = 0; i < rl.length; i++, c += incr) {
                rl[i] = c;
            }

            //
            // Make sure we end at 100% recall!
            rl[0] = 1;
            rl[rl.length - 1] = R;

        }

        /**
         * Adds a new relevant document.  This method also keeps track of
         * the precision at each of our
         *
         * @param s the similarity associated with the document.
         */
        public void addRel(float s) {

            rel++;

            //
            // See if we've exceeded our current recall level.
            if(rel >= rl[pos]) {
                r = recall();
                p = precision();
                ppts[pos] = p;
                f1pts[pos] = f1();
                sim[pos] = s;

                //
                // Now perform the interpolation bit.  We track back down
                // through the previous levels of recall.  If the precision
                // at an earlier level was lower than at this level, then reset
                // the precision to be what it was at this level.  According to
                // Foundations of Statistical NLP, this is valid because "the
                // user will be willing to look at more documents if the
                // precision goes up."
                for(int currPos = pos;
                        (currPos >= 0) && (ppts[currPos] < p);
                        currPos--) {
                    ppts[currPos] = p;
                }
                pos++;
            }

            //
            // If this is a new maximum f1, then keep track of the
            // similarity.
            float f1 = f1();
            if(f1 > maxF1) {
                maxF1 = f1;
                maxF1Sim = s;
            }
        }

        /**
         * Adds a new relevant document.
         *
         * @param s the similarity associated with the document.
         */
        public void addNonRel(float s) {
            nonrel++;
        }

        /**
         * Fills out the last entry in our precision points.
         */
        public void finish() {

            //
            // If we never got past zero recall, then zero out the precision
            // at 0 so that we don't accidentally get a precision of 1.000 when
            // we didn't get enough docs!
            if(pos == 1) {
                ppts[0] = 0;
                return;
            }
            for(int i = pos; i < ppts.length; i++) {
                ppts[i] = ppts[pos - 1];
            }
        }

        /**
         * Gets the average precision.
         */
        public float avgP() {
            float sum = 0;
            for(int i = 0; i < ppts.length; i++) {
                sum += ppts[i];
            }
            return sum / ppts.length;
        }

        /**
         * Gets the threshold that one would want to use for this feedback
         * query.
         */
        public float getThreshold() {
            return maxF1Sim;
        //             float maxF1 = Float.MIN_VALUE;
        //             int p = 0;
        //             for(int i = 0; i < f1pts.length; i++) {
        //                 if(maxF1 < f1pts[i]) {
        //                     maxF1 = f1pts[i];
        //                     p = i;
        //                 }
        //             }
        //             return sim[p];
        }

        /**
         * Returns <code>true</code> if we've hit recall of 1.0.
         */
        public boolean done() {
            return rel >= R;
        }

        public float recall() {
            return rel / (float) R;
        }

        public float precision() {
            return rel / (float) (rel + nonrel);
        }

        public float f1() {
            return (2 * recall() * precision()) / (recall() + precision());
        }
    }
} // Rocchio

