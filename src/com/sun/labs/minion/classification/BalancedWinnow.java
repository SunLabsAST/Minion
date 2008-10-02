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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.Progress;
import com.sun.labs.minion.DocumentVector;

import java.util.PriorityQueue;

import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;

import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;

import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;

import com.sun.labs.minion.retrieval.DocumentVectorImpl;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.retrieval.cache.TermCache;

import com.sun.labs.minion.util.MinionLog;
import java.util.Map;

/**
 * An implementation of the Balanced Winnow classification
 * algorithm.  An instance of BalancedWinnow represents
 * a classifier for a particular class.  Classifiers can
 * be trained and used to classify documents.
 */
public class BalancedWinnow implements ClassifierModel {

    private static final String logTag = "WINN";

    private static MinionLog log = MinionLog.getLog();

    private float theta = 0;

    private float alpha = 1.1f;

    private float beta = 0.909f;

    private float numWinnows = 50;

    private float avgActiveFeatures = 1;

    private SearchEngine engine;

    private FeatureClusterSet selectedFeatures;

    private ArrayList selectedTerms;

    private String fieldName;

    private String fromField;
    
    private String modelName;

    //
    // Classifier Models are instantiated from the config
    // so the constructor doesn't do anything.
    public BalancedWinnow() {
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getModelName() {
        return modelName;
    }

    /**
     * Train a balanced winnow classifier.  Balanced winnow
     * starts with a generic classifier and uses classification
     * mistakes to nudge the weighted feature vector along until
     * it fits the class.  Since winnow is an on-line learning
     * classifier, no knowledge of the collection as a whole is
     * needed, except for a few high-level stats.
     *
     * Several parameters are used in BalancedWinnow:
     *
     * Two weight vectors are used to store upper and lower weight
     * scores.  The difference of the two scores is the coefficient
     * of the classifier vector.  The weights are modified by alpha
     * and beta (described below) when the corresponding features are
     * encountered in misclassified documents.  When a positive
     * exmaple is misclassified, the upper weight is multiplied
     * by alpha and the lower weight is multiplied by beta.  When
     * a negative example is misclassified, the upper weight is
     * multiplied by beta and the lower weight is multiplied by
     * alpha.
     *
     * Theta is the threshold within which documents need to be to
     * be considered as part of the class.
     *
     * Alpha is the "promotion" parameter.  When winnow misclassifies
     * an example, the weights of the selected features in
     * the example document are modified by this parameter.  Alpha
     * by definition is a > 1.
     *
     * Beta is the "demotion" parameter.  When winnow misclassifies
     * a negative example, the weights of the selected features in
     * the example document are modified by this parameter.  Beta
     * by definition is 0 < b < 1.
     *
     * With these parameters, misclassified positives cause the
     * overall weight to be increased, while misclassified
     * negatives cause the overall weight to be decreased.  This
     * can cause overall weights to go negative for negative
     * indicators in features.
     *
     * @param name name of classifier
     * @param manager the partition manager for the collection
     * @param training the set of documents in the training set
     * @param selectedFeatures the set of feature (clusters) to use
     * @param tc a term cache for looking up terms in the collection
     * @param progress an object to use to report progress
     */
    public void train(String name,
            String fieldName,
            PartitionManager manager,
            ResultSetImpl training,
            FeatureClusterSet selectedFeatures,
            Map<String, TermStatsImpl> termStats,
            Map<DiskPartition, TermCache> termCaches,
            Progress progress) throws SearchEngineException {
        //log.debug(logTag, 0, "training based on " + training.size() + " docs");
        this.fieldName = fieldName;
        this.selectedFeatures = selectedFeatures;


        //
        // The result set passed in to this method represents the
        // positive examples of this class.  To run winnow, we also
        // need negative examples.  We'll find those by constructing
        // a query zone out of the selected features.  The query zone
        // will contain positive and negative examples.  We'll use
        // (number) of the documents that are most similar but not
        // in the set of positive examples as the negative examples.

        nextStep(progress, "Determining query zone");

        //
        // Construct the query zone:
        QueryZone qz = new QueryZone(training,
                fromField,
                selectedFeatures,
                termStats,
                termCaches,
                manager);

        //
        // Compute the query zone:
        PriorityQueue<QueryZone.HE> h = qz.computeZone();

        //
        // Since we'll iterate through our positive and negative
        // training docs many times, we'll first collect all
        // the docs that we want to use, and translate them into
        // simple arrays indicating the strength of each feature
        // in each document.  The values will be in the same order
        // as the features in the feature cluster set.

        // JLA: We should probably use the rank cutoff from Rocchio

        int numNegDocs = 0;
        int numPosDocs = 0;
        //
        // Collect up positive and negative examples
        HashMap<DiskPartition, List> partToDocListNeg =
                new HashMap<DiskPartition, List>();
        HashMap<DiskPartition, List> partToDocListPos =
                new HashMap<DiskPartition, List>();
        while(h.size() > 0 && (numPosDocs < 500)) {
            //
            // Get the top element of the heap:
            QueryZone.HE el = h.poll();

            int docID = el.getDoc();
            DiskPartition part = el.getPartition();
            if(el.removeFromTraining(docID)) {
                //
                // This was a training doc
                numPosDocs++;
                List curr = partToDocListPos.get(part);
                if(curr == null) {
                    curr = new ArrayList();
                    partToDocListPos.put(part, curr);
                }
                curr.add((DocKeyEntry) part.getDocumentTerm(docID));
            } else if(numNegDocs < training.size()) {
                numNegDocs++;
                List curr = partToDocListNeg.get(part);
                if(curr == null) {
                    curr = new ArrayList();
                    partToDocListNeg.put(part, curr);
                }
                curr.add((DocKeyEntry) part.getDocumentTerm(docID));
            }

            if(el.next()) {
                h.offer(el);
            }
        }

        nextStep(progress, "Collecting strengths for training data");
        //
        // Now convert those into lists of strength arrays
        List posDocArrays = getStrengthArrays(partToDocListPos,
                selectedFeatures,
                termCaches);
        float temp = avgActiveFeatures;
        List negDocArrays = getStrengthArrays(partToDocListNeg,
                selectedFeatures,
                termCaches);
        avgActiveFeatures = temp;
        nextStep(progress, "Getting default weight vectors");

        //
        // Set up two weight vectors for upper and lower weights.
        // The values of the upper, according to Dagan, Karov,
        // and Roth should start at (2*theta) / d where d is
        // the average number of active features in each doc.
        // An active feature is a selected feature that occurs
        // in a doc.  This requires some computation and may
        // not really be necessary.  Instead, we'll just base
        // the default weights off of theta.  We'll try keeping
        // the distance between them to see how that does.

        //
        // In a few other implementations, theta is set to
        // the number of features.
        theta = selectedFeatures.size();

        float defUpperWeight = 2f * (theta / avgActiveFeatures);
        float defLowerWeight = 1f * (theta / avgActiveFeatures);
        //log.debug(logTag, 0, "avgActiveFeatures: " + avgActiveFeatures + " and theta: " + theta);
        //log.debug(logTag, 0, "defUp: " + defUpperWeight + " defLo: " + defLowerWeight);

        float[] upperWeights = new float[selectedFeatures.size()];
        float[] lowerWeights = new float[selectedFeatures.size()];

        Arrays.fill(upperWeights, defUpperWeight);
        Arrays.fill(lowerWeights, defLowerWeight);


        //
        // Now that we have the example set, run through the docs,
        // modifying the weights in the upper and lower vectors.
        // We run through the set many times, continuing to nudge
        // our weight vectors until either we get everything right,
        // or we've run out of iterations.
        nextStep(progress, "Calculating optimal Winnow vector");
        //log.debug(logTag, 0, "numPos: " + posDocArrays.size() + " numNeg: " + negDocArrays.size());
        int count = 0;
        boolean cont = true;
        while(cont && count < numWinnows) {
            cont = false;
            //
            // Do all the positive docs
            if(!winnow(upperWeights, lowerWeights, posDocArrays, true)) {
                cont = true;
            }
            //
            // Then all the negative docs
            if(!winnow(upperWeights, lowerWeights, negDocArrays, false)) {
                cont = true;
            }
            count++;
        }

        //
        // Now wrap up our weight arrays into the selected features
        // and store them away so that the classifier manager can
        // retrieve and store them.
        int i = 0;
        for(Iterator it = this.selectedFeatures.iterator(); it.hasNext();) {
            FeatureCluster fc = (FeatureCluster) it.next();
            fc.setWeight(upperWeights[i] - lowerWeights[i]);
            i++;
        }

    }

    protected List getStrengthArrays(Map<DiskPartition, List> partToDocList,
            FeatureClusterSet clusterSet,
            Map<DiskPartition,TermCache> termCaches) {
        List results = new ArrayList();

        //
        // Go through one partition at a time, building the arrays
        for(DiskPartition part : partToDocList.keySet()) {
            //
            // Collect all the term IDs of the features in the feature cluster
            // set.
            //
            // We'll build a map from term IDs in this partition to the index in
            // the set of clusters where that term's weight should be contributed.
            HashMap<Integer, Integer> idToIndex = new HashMap<Integer, Integer>();
            int index = 0;
            for(Iterator fcsIt = clusterSet.iterator(); fcsIt.hasNext();) {
                FeatureCluster cluster = (FeatureCluster) fcsIt.next();
                for(Iterator fcIt = cluster.iterator(); fcIt.hasNext();) {
                    Feature f = (Feature) fcIt.next();
                    QueryEntry e = part.getTerm(f.getName());
                    if(e != null) {
                        idToIndex.put(e.getID(), index);
                    }
                }
                index++;
            }

            //
            // Now iterate through each training doc, filling in the term
            // weights for each doc
            List docs = partToDocList.get(part);
            for(Iterator it = docs.iterator(); it.hasNext();) {
                float[] strengths = new float[clusterSet.size()];
                DocKeyEntry doc = (DocKeyEntry) it.next();
                //
                // JLA:
                // should we go term by term in the doc, or use findID to find
                // only the features we know we care about?
                // SJG:
                // We can probably sort the termIDs and then we should be able
                // to iterate through the postings and the sorted termIDs concurrently.
                for(PostingsIterator p =
                        doc.iterator(new PostingsIteratorFeatures());
                        p.next();) {
                    Integer i = idToIndex.get(p.getID());
                    if(i != null) {

                        //
                        // Associate this term back to the cluster it
                        // came from
                        float str = strength(p.getFreq());
                        strengths[i] += str;
                        avgActiveFeatures += str;
                    }
                }
                results.add(strengths);
            }
        }
        avgActiveFeatures /= (float) results.size();
        return results;
    }

    /**
     * Actually computes the winnow sums and modifies the upper and
     * lower weights according to balanced winnow as described in the
     * train method.
     *
     * @param upperWeight the upper/positive weight array
     * @param lowerWeight the lower/negative weight array
     * @param strengthArrays the arrays representing the example docs
     * @param expectPositive true if the examples are positive examples
     * @return true if winnow made no changes to the arrays
     */
    protected boolean winnow(float[] upperWeight,
            float[] lowerWeight,
            List strengthArrays,
            boolean expectPositive) {

        int numCorrect = 0;
        int numWrong = 0;
        boolean ret = true;
        for(Iterator it = strengthArrays.iterator(); it.hasNext();) {
            float[] currDoc = (float[]) it.next();
            float score = 0;
            //
            // See what our score is for this document
            for(int i = 0; i < currDoc.length; i++) {
                float val = (upperWeight[i] - lowerWeight[i]) * currDoc[i];
                score += val;
            }
            numCorrect++;
            //
            // If we expected a positive sample and didn't get it, we learn something
            if(expectPositive && (score < theta)) {
                //
                // According to balanced winnow, when we misclassify a
                // positive example doc, the upper weights are promoted
                // by alpha (for terms in the doc) and the lower weights
                // are demoted by beta (for terms in the doc)
                for(int i = 0; i < currDoc.length; i++) {
                    if(currDoc[i] != 0) {
                        upperWeight[i] *= alpha;
                        lowerWeight[i] *= beta;
                    }
                }
                ret = false;
                numCorrect--;
                numWrong++;
            }

            //
            // If we expected a negative sample and didn't get it, we learn something
            if(!expectPositive && (score > theta)) {
                //
                // According to balanced winnow, when we misclassify a
                // negative example doc, the upper weights are demoted
                // be beta (for terms in the doc) and the lower weights
                // are promoted by alpha (for terms in the doc)
                for(int i = 0; i < currDoc.length; i++) {
                    if(currDoc[i] != 0) {
                        upperWeight[i] *= beta;
                        lowerWeight[i] *= alpha;
                    }
                }
                ret = false;
                numCorrect--;
                numWrong++;
            }
        }
        if(expectPositive) {
            float maxUp = 0;
            float minDown = 1000;
            for(int i = 0; i < upperWeight.length; i++) {
                maxUp = Math.max(upperWeight[i], maxUp);
                minDown = Math.min(lowerWeight[i], minDown);
            }
        //log.debug(logTag, 0, "Winnow got " + numCorrect + " docs right and " + numWrong + " docs wrong.");
        //log.debug(logTag, 0, " and maxUpper: " + maxUp + " and minLower: " + minDown);
        }
        return ret;
    }

    protected float strength(int freq) {
        //
        // we'll use the square root measure
        //return (float)Math.sqrt(freq);
        return (freq > 0 ? 1 : 0);
    }

    //
    // Inherited javadoc.  Get the features we ended up determining
    // to be important for this classifier.
    public FeatureClusterSet getFeatures() {
        return selectedFeatures;
    }

    public Feature getFeature() {
        return new WeightedFeature();
    }

    public void setEngine(SearchEngine e) {
        this.engine = e;
    }

    /**
     * Writes the threshold out that describes the minimum closeness that
     * a vector must have to this classifier.
     *
     * @param raf the file (correctly positioned) to write the threshold to
     */
    public void dump(RandomAccessFile raf) throws java.io.IOException {
        //
        // Dumps this classifier to the file
        raf.writeFloat(theta);
    }

    public void setFeatures(FeatureClusterSet f) {
        this.selectedFeatures = f;
    }

    /**
     * Reads the threshold for this classifier.
     *
     * @param raf the file (correctly positioned) to read the threshold from
     */
    public void read(RandomAccessFile raf) throws java.io.IOException {
        theta = raf.readFloat();
    }

    public float[] classify(DiskPartition sdp) {
        //log.debug(logTag, 0, "theta is " + theta);
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();

        float[] result = new float[sdp.getMaxDocumentID() + 1];

        for(Iterator i = selectedFeatures.iterator(); i.hasNext();) {
            FeatureCluster cluster = (FeatureCluster) i.next();
            int[] clusterFreq = new int[sdp.getMaxDocumentID() + 1];
            for(Iterator ci = cluster.getContents().iterator(); ci.hasNext();) {
                Feature f = (Feature) ci.next();
                QueryEntry e = (QueryEntry) sdp.getTerm(f.getName());

                if(e == null) {

                    //
                    // If the dictionary doesn't have this term, its ok.
                    continue;
                }

                PostingsIterator pi = e.iterator(feat);
                while(pi.next()) {
                    clusterFreq[pi.getID()] += pi.getFreq();
                }
            }
            for(int c = 0; c < clusterFreq.length; c++) {
                result[c] +=
                        cluster.getWeight() * strength(clusterFreq[c]);
            }
        }

        for(int i = 0; i < result.length; i++) {
            if(result[i] != 0) {
                //log.debug(logTag, 0, "score is " + result[i]);
                if(result[i] < theta) {
                    result[i] = 0 - Math.abs(result[i]);
                }
            }
        }
        return result;

    }

    //
    // Inherit javadoc.  Compute similarity to classifier.
    public float similarity(String key) {
        try {
            DocumentVector dv = engine.getDocumentVector(key);
            if(dv == null) {
                return 0;
            }
            return similarity(dv);
        } catch(SearchEngineException see) {
            //FIXME
            return 0;
        }
    }

    //
    // Inherit javadoc.  Compute similarity to classifier.
    public float similarity(DocumentVector v) {
        DocumentVectorImpl dv = (DocumentVectorImpl) v;
        WeightedFeatureVector fv = new WeightedFeatureVector(selectedFeatures.size());

        //
        // Build a new version of the passed in document vector with
        // winnow-style strengths
        DocKeyEntry docEntry = dv.getEntry();
        DiskPartition part = (DiskPartition) docEntry.getPartition();
        PostingsIterator pit = docEntry.iterator(new PostingsIteratorFeatures());

        //
        // We may have a document with no postings, so return 0 in that case.
        if(pit == null) {
            return 0;
        }

        WeightedFeature[] docFeats = new WeightedFeature[dv.getFeatures().length];
        int i = 0;
        while(pit.next()) {
            String name = (String) part.getTerm(pit.getID()).getName();
            docFeats[i++] = new WeightedFeature(name, strength(pit.getFreq()));
        }
        dv = new DocumentVectorImpl(dv.getEngine(), docFeats);

        //
        // Build up a feature vector for the features that represent
        // this class with their weights
        for(Iterator it = selectedFeatures.iterator(); it.hasNext();) {
            fv.add((FeatureCluster) it.next());
        }

        //
        // Multiply the weights by the stengths.
        float f = dv.dot(fv.getWeightedFeatures());

        //
        // Check whether we would have classified this.
        if(f >= theta) {
            return f;
        }
        return -f;
    }

    public float similarity(ClassifierModel cm) {
        float sum = 0;
        FeatureClusterSet fcs = cm.getFeatures();
        for(FeatureCluster fc : fcs) {
            FeatureCluster ourFC = selectedFeatures.get(fc.getName());
            if(ourFC != null) {
                sum += fc.getWeight() * ourFC.getWeight();
            }
        }
        return sum;
    }

    protected void nextStep(Progress p, String str) {
        if(p != null) {
            p.next(str);
        }
    }

    public ClassifierModel newInstance() {
        return new BalancedWinnow();
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
}
