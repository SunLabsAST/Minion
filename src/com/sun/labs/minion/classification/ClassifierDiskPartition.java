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

import java.io.File;
import java.io.RandomAccessFile;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map;
import java.util.TreeMap;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.entry.Entry;

import com.sun.labs.minion.classification.FeaturePostings.Featurator;
import com.sun.labs.minion.indexer.dictionary.DictionaryFactory;

import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.ChannelUtil;
import com.sun.labs.minion.util.StopWatch;

import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.FileReadableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * A disk partition that will hold classifier data.
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.41 $
 */
public class ClassifierDiskPartition extends DiskPartition {

    /**
     * The file containing the model specific data for this partition.
     */
    protected RandomAccessFile msd;

    /**
     * A buffer containing the offsets for the model specific data for each
     * of our classifiers.
     */
    protected ReadableBuffer msdOff;

    /**
     * The number of models that we're storing.
     */
    protected int nModels;

    /**
     * The place where the model specific data starts in the file.
     */
    protected long dataStart;

    protected static MinionLog log = MinionLog.getLog();

    protected static String logTag = "CDP";

    protected ClassifierModel modelInstance;

    /**
     * Constructs a disk partition for a specific partition
     * number.
     *
     * @param partNum the number of this partition
     * @param manager the classifier manager for this partition
     */
    public ClassifierDiskPartition(Integer partNum,
                                    ClassifierManager manager,
                                    DictionaryFactory mainDictFactory,
                                    DictionaryFactory documentDictFactory)
            throws java.io.IOException {
        super(partNum, manager, mainDictFactory, documentDictFactory);

        modelInstance = manager.getModelInstance();

        //
        // Open the model specific data file and read things in.
        msd = new RandomAccessFile(manager.makeModelSpecificFile(partNumber),
                                   "r");

        nModels = msd.readInt();
        msdOff = new FileReadableBuffer(msd, 4, nModels * 8, 1024);
        dataStart = 4 + nModels * 8;
    }

    protected ClassifierModel getClassifier(String cname) {
        initDocDict();
        if(modelMap != null) {
            return modelMap.get(cname);
        }
        FeatureEntry fe = (FeatureEntry) docDict.get(cname);
        if(fe != null  && !isDeleted(fe.getID())) {
            return getClassifier(fe);
        }
        return null;
    }
    
    public void findSimilar(ClassifierModel cm, Map<String,Float> scores) {
        invert();
        float[] sum = new float[allModels.length];
        FeatureClusterSet fcs = cm.getFeatures();
        for(FeatureCluster fc : fcs) {
            ClassificationFeature cf = features.get(fc.getName());
            for(ClassifierScore cs : cf.getScores()) {
                sum[cs.getID()] += fc.getWeight() * cs.getScore();
            }
        }
        for(int i = 0; i < sum.length; i++) {
            if(sum[i] > 0) {
                scores.put(allModels[i].getModelName(), sum[i]);
            }
        }
    }
    
    /**
     * Things to fix after the open house:  the main dictionary in the 
     * classifiers doesn't store the feature scores for the documents (i.e., the
     * classifiers.)  So we can't do bulk evaluation without inverting the 
     * document vectors.  We'll do that once and keep it here.
     */
    protected Map<String,ClassificationFeature> features;
    
    protected ClassifierModel[] allModels;
    
    protected Map<String,ClassifierModel> modelMap;
    
    protected ClassifierModel[] getAllModels() {
        invert();
        return allModels;
    }
    
    protected Map<String, ClassificationFeature> invert() {
        initDocDict();
        if(features == null) {
            features = new TreeMap<String, ClassificationFeature>();
            allModels = new ClassifierModel[getMaxDocumentID() + 1];
            modelMap = new HashMap<String,ClassifierModel>();
            for(QueryEntry qe : docDict) {
                FeatureEntry entry = (FeatureEntry) qe;
                ClassifierModel model = getClassifier(entry);
                modelMap.put(model.getModelName(), model);
                allModels[qe.getID()] = model;

                for(FeatureCluster fc : model.getFeatures()) {
                    ClassificationFeature cf = features.get(fc.getName());
                    if(cf == null) {
                        cf = new ClassificationFeature(fc);
                        features.put(fc.getName(), cf);
                    }
                    cf.addScore(qe.getID(), fc.getWeight());
                }
            }
        }
        return features;
    }

    /**
     * Gets a classifier model from an entry in our document dictionary.
     */
    protected ClassifierModel getClassifier(FeatureEntry fe) {

        // note: can we just reuse a single instance?
        ClassifierModel model = modelInstance.newInstance();
        model.setModelName(fe.getName().toString());

        //
        // Set the features for the model - first, make the features
        Set features = makeFeatures(fe);
        FeatureClusterSet clusters = new FeatureClusterSet();

        //
        // Now expand them into clusters
        ClusterManager cm =
                ((SearchEngineImpl) manager.getEngine()).getClusterManager();

        //
        // Look in each cluster partition for feature clusters
        for(Iterator it = cm.getActivePartitions().iterator(); it.hasNext();) {
            ClusterDiskPartition part = (ClusterDiskPartition) it.next();

            //
            // For each feature, get the cluster contents
            for(Iterator fit = features.iterator(); fit.hasNext();) {
                WeightedFeature feat = (WeightedFeature) fit.next();
                FeatureCluster cluster = part.getCluster(feat.getName());
                if(cluster != null) {
                    cluster.setWeight(feat.getWeight());
                    clusters.add(cluster);
                }
            }
        }
        model.setFeatures(clusters);
        model.setEngine(manager.getEngine());

        //
        // Allow the model to read in its custom data
        synchronized(msd) {
            try {
                msd.seek(msdOff.byteDecodeLong((fe.getID() - 1) * 8, 8));
                model.setFieldName(msd.readUTF());
                String ff = msd.readUTF();
                if(ff.equals("")) {
                    ff = null;
                }
                model.setFromField(ff);
                model.read(msd);
            } catch(java.io.IOException ioe) {
                log.error(logTag, 1, "Error reading model " +
                          "specific data for: " + fe.getName(), ioe);
            }
        }

        return model;
    }

    /**
     * Classifies all the documents in a disk partition.  Uses the classifier
     * model that is defined by the index/query configuration.   The result
     * is an array of collections of strings.  Each position in the array
     * corresponds to a document with the id of the position.  The collection
     * contains strings that represent the names of the classes to which the
     * document belongs.  If a position is null, the document belongs to no
     * classes defined in this partition.
     *
     * @param sdp a disk partition
     * @param ec a (possibly <code>null</code>) pair of field names.  One is the
     * name of the field from which classifiers were built.  If this pair is 
     * non-<code>null</code>, then only classifiers
     * that were built from the contents of the classifier from field in the pair will be considered.
     * Also, if this pair is non-<code>null</code> then whatever classifiers are
     * applied will be applied against the contents of the document from field in the
     * pair.  If this pair is <code>null</code>, then classification proceeds as
     * usual.
     * @param results a map to fill up with classification results
     */
    public void classify(DiskPartition sdp,
                          ExtraClassification ec,
                          Map<String, ClassificationResult> results) {
        initMainDict();
        initDocDict();
        
        if(ec == null && modelInstance instanceof BulkClassifier) {
            StopWatch sw = new StopWatch();
            sw.start();
            invert();
            
            //
            // Get a from field.  Bit of a hack here:  we could have trained
            // classifiers from different fields.  FIXME.
            String fromField = null;
            String resultsField = null;
            for(int i = 0; i < allModels.length; i++) {
                if(allModels[i] != null) {
                    fromField = allModels[i].getFromField();
                    resultsField = allModels[i].getFieldName();
                    if(fromField != null) {
                        break;
                    }
                }
            }
            
            float[][] scores = ((BulkClassifier) modelInstance).classify(fromField, this, sdp);
            
            int neval = 0;
            for(int i = 0 ; i < scores.length; i++) {
                if(allModels[i] != null) {
                }
                if(scores[i] != null) {
                    int nInClass = assembleResults(scores[i], allModels[i].getModelName(), resultsField, results);
                    neval++;
                }
            }
            
            sw.stop();
            log.log(logTag, 3, String.format("Evaluated %d classifiers in %dms", neval, sw.getTime()));
            return;
        }
        
        //
        // For each class in this partition, use the classifier model
        // to determine which of the docs in the dict are in which
        // classes.
        for(DictionaryIterator dictIt = docDict.iterator();
                dictIt.hasNext();) {

            FeatureEntry entry = (FeatureEntry) dictIt.next();
            ClassifierModel model = getClassifier(entry);

            //
            // If we have a restriction to a particular classifier field, then
            // apply it now.
            if(ec != null &&
                    !model.getFromField().equals(ec.getClassifierFromField())) {
                continue;
            }

            //
            // We may have been given a number of fields against which to apply
            // the classifiers.  We'll assume for the moment however that that's 
            // not the case.
            List<String> fromFields =
                    Collections.singletonList(model.getFromField());
            List<String> resultFields =
                    Collections.singletonList(model.getFieldName());
            if(ec != null) {
                fromFields = ec.getDocumentFromFields();
                resultFields = ec.getClassifierResultFields();
            }

            //
            // OK, classify the data in the from fields list, adding the 
            // data to the results for the provided results fields list.
            for(int i = 0; i < fromFields.size(); i++) {

                StopWatch sw = new StopWatch();
                sw.start();

                model.setFromField(fromFields.get(i));
                String resultField = resultFields.get(i);

                //
                // Perform the classification
                float[] scores = model.classify(sdp);
                
                int nInClass = assembleResults(scores, model.getModelName(), resultField, results);


                sw.stop();
                log.log(logTag, 4, String.format("\"%s\" %dms, %d/%d",
                                                 entry.getName(),
                                                 sw.getTime(),
                                                 nInClass, scores.length - 1));
            }
        }
    }
    
    public int assembleResults(float[] scores, 
            String modelName, String resultField,
            Map<String, ClassificationResult> results) {
        
        //
        // Assemble the results
        ClassificationResult cr = results.get(resultField);
        if(cr == null) {
            cr = new ClassificationResult(resultField);
            results.put(resultField, cr);
        }
        int nInClass = 0;
        for(int j = 0; j < scores.length; j++) {
            if(scores[j] > 0) {
                nInClass++;
                cr.add(j, scores[j], modelName);
            } else {
                if(manager.getIndexConfig().storeNonClassified() &&
                        scores[j] < 0) {
                    cr.add(j, scores[j], modelName);
                }
            }
        }
        return nInClass;
    }

    public Set getFeatures(String cname) {
        initMainDict();
        initDocDict();
        FeatureEntry ent = (FeatureEntry) docDict.get(cname);
        return ent == null ? null : makeFeatures(ent);
    }

    protected Set makeFeatures(FeatureEntry entry) {
        initMainDict();
        Set features = new TreeSet();

        //
        // Iterate through the postings and make a feature
        // for each id
        Featurator it =
                (Featurator) entry.iterator(new PostingsIteratorFeatures());
        if(it == null) {
            log.error(logTag, 1, "No feature iterator for: " + entry.getName());
            return features;
        }

        while(it.next()) {
            //
            // Decode the current info into a feature
            Feature f = modelInstance.getFeature();
            f.setID(it.getID());
            it.decode(f);

            //
            // Look up the feature's name in the main dict.
            Entry mainEntry = mainDict.get(f.getID());
            f.setName((String) mainEntry.getName());
            features.add(f);
        }
        return features;
    }

    /**
     * Gets the length of a document vector for a given document.  For
     * classifier partitions, this is assumed to always be 1.
     *
     * @param docID the ID of the document for whose vector we want the length
     * @return 1.
     */
    public float getDocumentVectorLength(int docID) {
        return 1;
    }

    /**
     * Merges the model specific data for these classifiers.
     */
    protected void mergeCustom(int newPartNumber,
                                DiskPartition[] sortedParts,
                                int[][] idMaps,
                                int newMaxDocID,
                                int[] docIDStart,
                                int[] nUndel,
                                int[][] docIDMaps)
            throws Exception {

        //
        // Dump our model specific data to the classifier model file.
        File mmsdFile =
                ((ClassifierManager) manager).makeModelSpecificFile(newPartNumber);
        RandomAccessFile mmsd =
                new RandomAccessFile(mmsdFile, "rw");
        WriteableBuffer b = new ArrayBuffer(newMaxDocID * 8);

        //
        // Write the number of models and then skip over the place where
        // the offsets will be stored.
        mmsd.writeInt(newMaxDocID);
        mmsd.seek(newMaxDocID * 8 + 4);

        int nOff = 0;

        //
        // Now, for each of the partitions, merge the model specific data.
        for(int i = 0; i < sortedParts.length; i++) {
            ClassifierDiskPartition cdp =
                    (ClassifierDiskPartition) sortedParts[i];

            if(docIDMaps[i] == null) {

                //
                // Remember where we started, so that we can modify the
                // offsets.
                long off = mmsd.getFilePointer();

                //
                // If there are no deleted documents in the partition we're
                // merging, do a straight channel transfer.
                ChannelUtil.transferFully(cdp.msd.getChannel(),
                                          cdp.dataStart,
                                          cdp.msd.length() - cdp.dataStart,
                                          mmsd.getChannel());
                //
                // Now, write new offsets from the ones in the buffer in
                // the partition that we're merging.  We'll start by
                // getting the first offset from the buffer which we'll use
                // to compute deltas from the offsets in the buffer.
                long origOff = cdp.msdOff.byteDecode(0, 8);
                b.byteEncode(off, 8);
                nOff++;
                for(int j = 1,  k = 8; j < cdp.nModels; j++, k += 8) {
                    long diff = cdp.msdOff.byteDecode(k, 8) - origOff;
                    b.byteEncode(off + diff, 8);
                    nOff++;
                }
            } else {

                int[] idMap = docIDMaps[i];

                //
                // We'll try to copy as many undeleted models at a time as
                // we can, so we need to remember where the offset of the
                // undeleted model where we started is.  We'll start with
                // a value less than zero, which will get initialized when
                // we hit the first non-deleted model.  We'll also need to
                // remember which model number it was so that we can
                // re-encode offsets correctly.
                long prevOff = -1;
                int prevMod = 0;

                //
                // Walk through each model, copying over the non-deleted
                // ones.
                for(int j = 0; j < nModels; j++) {

                    //
                    // What's the offset in the msd file of the current
                    // model?
                    long currOff = cdp.msdOff.byteDecode(j * 8, 8);

                    //
                    // If we encounter a model that's been deleted, then we
                    // may need to copy data for undeleted models.
                    if(idMap[j + 1] < 0) {

                        //
                        // If we haven't encountered an undeleted model
                        // yet, then just keep going.
                        if(prevOff == -1) {
                            continue;
                        }
                        long nb = currOff - prevOff;
                        if(nb > 0) {

                            //
                            // We need to encode the offsets for each of
                            // the models that we're copying.  The first
                            // offset will be the current offset in the
                            // file and then we'll re-encode up to this
                            // position in the offsets buffer.
                            long mergeOff = mmsd.getFilePointer();
                            b.byteEncode(mergeOff, 8);
                            nOff++;
                            for(int k = prevMod + 1; k < j; k++) {

                                //
                                // What was the delta from the offset where
                                // we will start copying?
                                long diff = cdp.msdOff.byteDecode(k * 8, 8) -
                                        prevOff;

                                //
                                // Encode the new offset.
                                b.byteEncode(mergeOff + diff, 8);
                                nOff++;
                            }

                            //
                            // Copy the data.
                            ChannelUtil.transferFully(cdp.msd.getChannel(),
                                                      prevOff,
                                                      nb,
                                                      mmsd.getChannel());
                            prevOff = -1;
                        }
                    } else {

                        //
                        // If this is the first undeleted model that we've
                        // encountered, then remember the offset where we
                        // found it.
                        if(prevOff == -1) {
                            prevOff = currOff;
                            prevMod = j;
                        }
                    }
                }
            }
        }

        //
        // Zip back and write the offsets.
        long pos = mmsd.getFilePointer();
        mmsd.seek(4);
        b.write(mmsd);
        mmsd.seek(pos);
    }

    /**
     * Close the files associated with this partition.
     */
    public synchronized boolean close() {
        try {
            if(!super.close()) {
                return false;
            }
            msd.close();
        } catch(java.io.IOException ioe) {
            log.error(logTag, 1, "Error closing classifier partition", ioe);
        }
        return true;
    }

    /**
     * Reaps the given classifier partition.
     *
     * @param m The manager associated with the partition.
     * @param n The partition number to reap.
     */
    protected static void reap(PartitionManager m, int n) {
        ((ClassifierManager) m).makeModelSpecificFile(n).delete();
        DiskPartition.reap(m, n);
    }
}
