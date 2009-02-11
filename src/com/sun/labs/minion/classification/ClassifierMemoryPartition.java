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

import com.sun.labs.minion.QueryConfig;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.File;
import java.io.RandomAccessFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.partition.MemoryPartition;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.IndexEntry;

import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import com.sun.labs.minion.retrieval.cache.TermCache;


import com.sun.labs.minion.util.buffer.WriteableBuffer;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.Progress;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.TermStats;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.minion.pipeline.StopWords;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.retrieval.cache.DocCache;
import com.sun.labs.minion.retrieval.cache.TermCacheElement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A memory partition that will hold classifier data.
 *
 * @author Jeff Alexander
 */
public class ClassifierMemoryPartition extends MemoryPartition {
    /**
     * The feature selector used by all classifiers in this partition
     */

    protected FeatureSelector selectorInstance;

    /**
     * The feature clusterer used by all classifiers in this partition
     */
    protected FeatureClusterer clustererInstance;

    /**
     * The model used to classify docs in this partition
     */
    protected ClassifierModel modelInstance;

    /**
     * A list of the models trained into this partition, so that we can
     * dump their model specific data.
     */
    protected List models;

    /**
     * The log.
     */
 Logger logger = Logger.getLogger(getClass().getName());

    /**
     * The tag for this module.
     */
    protected static String logTag = "CMP";

    /**
     * The number of classes we've indexed into this partition
     */
    protected int partClasses = 0;

    /**
     * Constructs a ClassifierMemoryPartition for general use.
     *
     */
    public ClassifierMemoryPartition() {

    }

    public int getNDocs() {
        return docDict.size();
    }

    /**
     * Train a classifier.  Either creates a new classifier with the given
     * name or will replace a classifier if the specified name exists
     *
     * @param name the name of the new class, or an existing class
     * @param fieldName the name of the field where classification results for this
     * classifier will be stored
     * @param fromField the vectored field from which features will be selected.
     * @param results the set of results to use to train the classifier
     * @param progress where to send progress events
     * @throws com.sun.labs.minion.SearchEngineException if there are any errors
     * during training
     */
    public void train(String name, String fieldName, String fromField,
            ResultSetImpl results, Progress progress)
            throws SearchEngineException {

        if(progress != null) {
            progress.start(7);
        }

        //
        // Choose the model tuned with the best parameters
        ResultSplitter splitter = null;
        try {
            splitter = (ResultSplitter) ((ClassifierManager) manager).splitterInstance.getClass().
                    newInstance();
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Failed to instantiate splitter: " +
                    ((ClassifierManager) manager).splitterClassName, e);
            throw new SearchEngineException("Failed to instantiate result set splitter",
                    e);
        }
        splitter.init(results, indexConfig);
        ClassifierModel model = selectBestModel(name, fieldName, fromField,
                results, splitter, progress);
        
        if(model == null) {
            logger.warning("Unable to train model for " + name);
            return;
        }

        //
        // Add it to our list of models for this partition.  We're synchronized
        // so that we can do the bulk of the training for multiple classifiers
        // in parallel.
        synchronized(this) {
            models.add(model);

            //
            // use getFeatures to get what features it is made up of
            FeatureClusterSet features = model.getFeatures();

            //
            // Store away our feature clusters
            clusterMemoryPartition.addClusters(features);

            //
            // create a new classifier entry in doc dict
            FeatureEntry classEntry = (FeatureEntry) docDict.newEntry(name);
            Entry old = docDict.put(name, classEntry);

            //
            // If there's already an entry, get rid of it
            if(old != null) {
                logger.warning("Duplicate class in partition: " + name +
                        " deleting old version: " + old.getID());
                del.delete(old.getID());
            }

            // add IDEntries to main dict for each cluster with the classifier doc id
            // then add the feature to the doc dict
            for(Iterator i = features.iterator(); i.hasNext();) {
                FeatureCluster fc = (FeatureCluster) i.next();

                //
                // See if this feature is already in the main dict
                IndexEntry mde = (IndexEntry) mainDict.get(fc.getName());

                if(mde == null) {
                    mde = mainDict.newEntry(fc.getName());
                    mainDict.put(fc.getName(), mde);
                }

                WeightedFeature f =
                        new WeightedFeature(fc.getName(), fc.getWeight());

                //
                // Add this occurrence to the main dict entry
                f.setID(classEntry.getID());
                mde.add(f);

                //
                // Add an occurrence of this term in the doc dict
                f.setID(mde.getID());
                classEntry.add(f);
            }

            // finish off this classifier
            endTrainingClass(features.size());
        }
    }

    protected ClassifierModel selectBestModel(String name,
            String fieldName,
            String fromField,
            ResultSetImpl results,
            ResultSplitter splitter,
            Progress progress)
            throws SearchEngineException {

        //
        // We'll make a term cache that won't throw away anything, since
        // we're not sure how many terms we'll need it to hold.  We'll set up 
        // a weighting function and components here, since we want to make sure
        // that we don't lose term statistics information while we're computing
        // lots of weights.
        QueryConfig qc = manager.getEngine().getQueryConfig();
        WeightingFunction wf = qc.getWeightingFunction();
        WeightingComponents wc = qc.getWeightingComponents();
        Map<DiskPartition,TermCache> termCaches = new HashMap<DiskPartition,TermCache>();
        Map<String,TermStatsImpl> termStats = new HashMap<String,TermStatsImpl>();
        DocCache dc = new DocCache(-1, manager.getEngine(), wf, wc);

        //
        // We want to be able to train on some of the results, then validate
        // our training on the rest.  We'll see how the results look by testing
        // on the validation set, then we can try to tweak the input into the
        // training algorithm to see if we can improve the performance on the
        // validation set.
        nextStep(progress, "Creating Training/Validation Split");

        ClassifierModel bestModel = null; // overall best model
        float bestAcc = 0; // accuracy of overall best model
        ClassifierModel prevModel = null; // the temporary / local best model
        float prevAcc = 0; // the temporary / local best accuracy
        float prevSim = 0;
        float bestSim = 0;

        while((results.size() > splitter.getMinDocs()) && splitter.nextSplit()) {

            ResultSetImpl training = splitter.getTrainSet();
            ResultSetImpl validation = splitter.getValidateSet();

            nextStep(progress, "Clustering Features");
            //
            // Harvest the feature clusters from the results
            FeatureClusterer clusterer = clustererInstance.newInstance();

            clusterer.setDocCache(dc);
            clusterer.setField(fromField);
            FeatureClusterSet clusters = clusterer.cluster((training == null ? results
                    : training));
            nextStep(progress, "Selecting Clusters");

            //
            // We could conceivably get a split where the labeled training
            // documents all have no data, in which case we just try the next split.
            if(clusters.size() == 0) {
                logger.info("Split had no training data");
                continue;
            }

            //
            // Determine the number of features to start with
            int nFeat = ((ClassifierManager) manager).getNumClassifierFeatures();
            
            //
            // Now select the clusters that will be used for training
            selectorInstance.setHumanSelected(((ClassifierManager) manager).getHumanSelected(name));
            selectorInstance.setStopWords(stopWords);
            FeatureClusterSet selectedClusters =
                    selectorInstance.select(clusters,
                    wc,
                    (training == null ? results.size() : training.size()),
                    nFeat,
                    partManager.getEngine());

            primeCaches(selectedClusters, termCaches, termStats);

            //
            // Now we'll do a loop, working to determine how many features
            // to use to get the best results.  We'll start with 100% of the
            // number of features provided by the user then we'll back off
            // from there.  We'll train on the training set, then check
            // our answers on the validation set.
            if(training != null) {
                boolean doBackoff =
                        manager.getIndexConfig().getDoFeatureBackoff();
                double range = 0.1;
                if(doBackoff == false) {
                    range = 1.0;
                }

                prevAcc = 0;
                prevSim = 0;
                for(double i = 1.0; i >= range; i -= 0.1) {
                    int currNFeat = (int) (((double) nFeat) * i);
                    FeatureClusterSet selectedSubset =
                            selectedClusters.subsetFirstN(currNFeat);

                    //
                    // Create a classifier model
                    ClassifierModel curr = modelInstance.newInstance();
                    curr.setEngine(manager.getEngine());
                    curr.setFromField(fromField);

                    //
                    // Call train on the model
                    curr.train(name,
                            fieldName,
                            partManager,
                            training,
                            selectedSubset,
                            termStats,
                            termCaches,
                            progress);

                    //
                    // Now see how it does for similarity with the validation
                    // docs
                    float acc = 0;
                    float sim = 0;
                    for(Iterator valIt = validation.getAllResults(false).
                            iterator();
                            valIt.hasNext();) {
                        Result res = (Result) valIt.next();
                        float currSim = curr.similarity(res.getKey());
                        sim += currSim;
                        if(currSim > 0) {
                            acc++;
                        }
                    }
                    acc = acc / validation.size();

                    //
                    // So far, cumulative similarity seems to work best for
                    // a metric for best classifier, as compared to accuracy.
                    if(acc >= prevAcc) {
                        //                    if (sim >= prevSim) {
                        prevModel = curr;
                        prevAcc = acc;
                        prevSim = sim;
                        continue;
                    }

                    break;
                }
                if(prevAcc >= bestAcc) {
                    //                if (prevSim >= bestSim) {
                    bestAcc = prevAcc;
                    bestSim = prevSim;
                    bestModel = prevModel;
                }

            }
        }

        if((bestModel == null) || (prevAcc == 0)) {
            nextStep(progress, "Clustering Features");

            //
            // Harvest the feature clusters from the results
            FeatureClusterer clusterer = clustererInstance.newInstance();
            clusterer.setDocCache(dc);
            clusterer.setField(fromField);
            FeatureClusterSet clusters = clusterer.cluster(results);
            
            primeCaches(clusters, termCaches, termStats);
            
            if(clusters.size() == 0) {
                return null;
            }

            nextStep(progress, "Selecting Clusters");

            //
            // Determine the number of features to start with
            int nFeat = ((ClassifierManager) manager).getNumClassifierFeatures();

            //
            // Now select the clusters that will be used for training
            FeatureClusterSet selectedClusters =
                    selectorInstance.select(clusters,
                    wc,
                    results.size(),
                    nFeat,
                    partManager.getEngine());
            //
            // There wasn't a model, meaning we didn't get
            // to do a training/validation split
            bestModel = modelInstance.newInstance();

            //
            // Call train on the model
            bestModel.train(name,
                    fieldName,
                    partManager,
                    results,
                    selectedClusters,
                    termStats,
                    termCaches,
                    progress);

        }
        bestModel.setFromField(fromField);
        return bestModel;
    }

    private void primeCaches(FeatureClusterSet selectedClusters,
            Map<DiskPartition, TermCache> termCaches,
            Map<String, TermStatsImpl> termStats) {
        //
        // Go ahead and prime the term cache and the term stats map, since we'll
        // be doing *a lot* of processing on these terms.
        for (DiskPartition p : partManager.getActivePartitions()) {
            TermCache tc = termCaches.get(p);
            if (tc == null) {
                // We'll have the caches that we create hold all of the terms that
                // we uncompress, since we'll likely need them all.
                tc = new TermCache(-1, p);
                termCaches.put(p, tc);
            }
            for (FeatureCluster fc : selectedClusters) {
                TermStats ts = termStats.get(fc.getName());
                if (ts != null) {
                    //
                    // We've already primed this term.
                    continue;
                }
                List<String> featNames = new ArrayList<String>();
                for(Feature f : fc) {
                    featNames.add(f.getName());
                }
                TermCacheElement tce = tc.get(featNames);
                termStats.put(fc.getName(), tce.getTermStats());
            }
        }

    }

    protected void endTrainingClass(int featureSize) {
        partClasses++;
    }

    /**
     * Dumps the data that is specific to the classifier partition.
     * This will be any custom data that the classifier wants to store
     * as well as the set of docs that contributed to training each
     * classifier.  This method is called automatically by the dump()
     * method of MemoryPartition and doesn't need to be called directly.
     *
     * @param sorted a sorted listed of all main dictionary entries
     * @throws java.io.IOException if there is any error writing data.
     */
    protected void dumpCustom(Entry[] sorted)
            throws java.io.IOException {

        //
        // Dump our model specific data to the classifier model file.
        File manFile =
                ((ClassifierManager) manager).makeModelSpecificFile(partNumber);
        RandomAccessFile msdFile = new RandomAccessFile(manFile, "rw");
        WriteableBuffer b = new ArrayBuffer(models.size() * 8);

        //
        // Write the number of models and then skip over the place where
        // the offsets will be stored.
        msdFile.writeInt(models.size());
        msdFile.seek(models.size() * 8 + 4);

        //
        // Dump the data for each model, noting the offset for each chunk
        // of data.
        for(Iterator i = models.iterator(); i.hasNext();) {
            b.byteEncode(msdFile.getFilePointer(), 8);
            ClassifierModel cm = (ClassifierModel) i.next();
            msdFile.writeUTF(cm.getFieldName());
            String ff = cm.getFromField();
            if(ff == null) {
                ff = "";
            }
            msdFile.writeUTF(ff);
            cm.dump(msdFile);
        }

        //
        // Zip back and write the offsets.
        long pos = msdFile.getFilePointer();
        msdFile.seek(4);
        b.write(msdFile);
        msdFile.seek(pos);
        models.clear();
        msdFile.close();
    }

    protected void nextStep(Progress p, String str) {
        if(p != null) {
            p.next(str);
        }
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        partManager = (PartitionManager) ps.getComponent(PROP_PART_MANAGER);
        clusterMemoryPartition =
                (ClusterMemoryPartition) ps.getComponent(PROP_CLUSTER_MEMORY_PARTITION);
        stopWords = (StopWords) ps.getComponent(PROP_STOPWORDS);
        models = new ArrayList();

        modelInstance = ((ClassifierManager) manager).getModelInstance();
        selectorInstance = ((ClassifierManager) manager).getSelectorInstance();
        clustererInstance = ((ClassifierManager) manager).getClustererInstance();
    }
    @ConfigComponent(type = com.sun.labs.minion.indexer.partition.PartitionManager.class)
    public static final String PROP_PART_MANAGER =
            "part_manager";

    private PartitionManager partManager;

    @ConfigComponent(type = ClusterMemoryPartition.class)
    public static final String PROP_CLUSTER_MEMORY_PARTITION =
            "cluster_memory_partition";
    
    @ConfigComponent(type = com.sun.labs.minion.pipeline.StopWords.class, mandatory=false)
    public static final String PROP_STOPWORDS = "stopwords";
    
    private StopWords stopWords;

    private ClusterMemoryPartition clusterMemoryPartition;

}
