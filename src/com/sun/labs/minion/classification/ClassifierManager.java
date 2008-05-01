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

import com.sun.labs.minion.FieldValue;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.File;
import java.util.Iterator;

import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.util.props.ConfigBoolean;
import com.sun.labs.util.props.ConfigComponentList;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.ConfigString;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * The ClassifierManager is a specialization of the PartitionManager.
 * It performs the same roll on classifier partitions that the Partition
 * Manager performs on partitions.
 */
public class ClassifierManager extends PartitionManager {

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
     * The result splitter used for classification in this partition.
     */
    protected ResultSplitter splitterInstance;

    /**
     * A set of human selected terms for inclusion or exclusion in a set of
     * classifiers.
     */
    private Map<String, HumanSelected> humanSelected;

    /**
     * Constructs the ClassifierManager.  The Term selector and clusterer are
     * provided at initialization time and will be passed to other classes as
     * needed.
     */
    public ClassifierManager() {
        subDir = "class";
        logTag = "ClasMAN";
    }

    public ClassifierModel getModelInstance() {
        return modelInstance;
    }

    public FeatureSelector getSelectorInstance() {
        return selectorInstance;
    }

    public FeatureClusterer getClustererInstance() {
        return clustererInstance;
    }

    public boolean doClassification() {
        return doClassification;
    }

    /**
     * Gets the name of the field to which classes will be assigned during
     * classification.
     * @return the name of the field to which classes will be assigned
     */
    public String getClassesField() {
        return classesField;
    }

    /**
     * Gets the number of features to use for classifiers.
     * @return the number of features to use for classification
     */
    public int getNumClassifierFeatures() {
        return numClassifierFeatures;
    }

    public HumanSelected getHumanSelected(String name) {
        if(humanSelected == null) {
            return null;
        }
        return humanSelected.get(name);
    }

    /**
     * Creates a new classifier based on the classifier model for this
     * collection, the documents in the ResultSet, and the set of currently
     * indexed documents.  If an existing className is provided, the existing
     * classifier will be replaced.
     *
     * @param className the name of the class to create or replace
     * @param docs the documents to use as exemplars for the class
     */
    public void trainClassifier(String className, ResultSet docs) {
    }

    /**
     * Signals the ClassifierManager that all the classifiers currently in
     * memory should be dumped to disk so that they can be used for classifying
     * new documents.  Classifiers that are just trained (and only in memory)
     * cannot be used for classification.
     * 
     * @throws java.io.IOException if there is any error dumping the partition
     */
    public void dump() throws java.io.IOException {
    }

    /**
     * Gets a classifier model for the given class name.
     *
     * @param cname the name of the classifier that we want to get
     * @return a classifier model with the given name, or null if there is
     * no such model
     *
     */
    public ClassifierModel getClassifier(String cname) {
        synchronized(activeParts) {
            for(Iterator i = activeParts.iterator(); i.hasNext();) {
                ClassifierDiskPartition cdp = (ClassifierDiskPartition) i.next();
                ClassifierModel model = cdp.getClassifier(cname);
                if(model != null) {
                    return model;
                }
            }
        }
        return null;
    }

    /**
     * Find classifiers that are similar to the named classifier.
     * @param cname the name of the classifier for which we want to find similar
     * classifiers
     * @return a list of the similar classifiers, along with scores indicating
     * the degree of similarity.
     */
    public List<FieldValue> findSimilar(String cname, int n) {
        ClassifierModel cm = getClassifier(cname);
        if(cm == null) {
            return new ArrayList<FieldValue>();
        }
        Map<String, Float> scores = new HashMap<String, Float>();
        for(DiskPartition dp : getActivePartitions()) {
            ((ClassifierDiskPartition) dp).findSimilar(cm, scores);
        }
        PriorityQueue<FieldValue> q = new PriorityQueue<FieldValue>();
        for(Map.Entry<String, Float> e : scores.entrySet()) {
            if(q.size() < n) {
                q.offer(new FieldValue(e.getKey(), e.getValue()));
            } else {
                FieldValue top = q.peek();
                if(e.getValue() > top.getScore()) {
                    q.poll();
                    q.offer(new FieldValue(e.getKey(), e.getValue()));
                }
            }
        }

        List<FieldValue> ret = new ArrayList<FieldValue>();
        while(q.size() > 0) {
            ret.add(q.poll());
        }
        Collections.reverse(ret);
        return ret;
    }

    public List<WeightedFeature> explain(String cname1, String cname2, int n) {
        ClassifierModel cm1 = getClassifier(cname1);
        ClassifierModel cm2 = getClassifier(cname2);
        if(cm1 == null || cm2 == null) {
            return new ArrayList<WeightedFeature>();
        }
        SortedSet<WeightedFeature> s =
                new TreeSet<WeightedFeature>(WeightedFeature.getInverseWeightComparator());
        FeatureClusterSet fcs2 = cm2.getFeatures();
        for(FeatureCluster fc1 : cm1.getFeatures()) {
            FeatureCluster fc2 = fcs2.get(fc1.getName());
            if(fc2 != null) {
                s.add(new WeightedFeature(fc1.getHumanReadableName(), fc1.getWeight() * fc2.getWeight()));
            }
        }
        
        Iterator<WeightedFeature> i = s.iterator();
        List<WeightedFeature> ret = new ArrayList<WeightedFeature>();
        while(i.hasNext() && n > 0) {
            ret.add(i.next());
            n--;
        }
        return ret;
    }

    /**
     * Computes the similarity between a document and a classifier.
     */
    public float similarity(String cname, String key) {
        synchronized(activeParts) {
            for(Iterator i = activeParts.iterator(); i.hasNext();) {
                ClassifierDiskPartition cdp = (ClassifierDiskPartition) i.next();
                ClassifierModel model = cdp.getClassifier(cname);
                if(model != null) {
                    return model.similarity(key);
                }
            }
        }
        log.warn(logTag, 3, "No classifier with name: " + cname);
        return 0;
    }

    /**
     * Begin classification of a set of documents in memory.  The argument
     * provided should be the disk partition of the documents to
     * classify.  Each row (result[i]) of the output will represent the
     * classification of a single document.  The first position
     * (result[i][0]) will be the key of the document, and any additional
     * positions will be the names of classes into which the document was
     * classified.
     *
     * @param sdp the disk partition to classify
     * @param maxDocID the maximum document ID in the partition we're classifying
     * @return a 2-d array of string representing the classification of each doc
     */
    public Map<String, ClassificationResult> classify(DiskPartition sdp) {
        Map<String, ClassificationResult> results =
                new HashMap<String, ClassificationResult>();

        //
        // Classify the dictionary against all the classes in all
        // of the partitions
        for(Iterator partIt = getActivePartitions().iterator();
                partIt.hasNext();) {
            ClassifierDiskPartition cdp =
                    (ClassifierDiskPartition) partIt.next();
            cdp.classify(sdp, null, results);

            //
            // Classify using any other field pairs that we were given.
            for(ExtraClassification ec : extraClassifications) {
                cdp.classify(sdp, ec, results);
            }
        }
        return results;
    }

    /**
     * Gets a model-specific data file name for use when dumping or merging
     * classifier partitions.
     */
    public File makeModelSpecificFile(int partNumber) {
        return new File(indexDir + File.separator + "p" +
                partNumber + ".msd");
    }

    /**
     * A method to reap a single partition.  This can be overridden in a
     * subclass so that the reap method will work for the super and
     * subclass.
     */
    protected void reapPartition(int partNumber) {
        ClassifierDiskPartition.reap(this, partNumber);
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        doClassification = ps.getBoolean(PROP_DO_CLASSIFICATION);
        selectorClassName = ps.getString(PROP_SELECTOR_CLASS_NAME);
        clustererClassName = ps.getString(PROP_CLUSTERER_CLASS_NAME);
        modelClassName = ps.getString(PROP_MODEL_CLASS_NAME);
        splitterClassName = ps.getString(PROP_SPLITTER_CLASS_NAME);
        try {
            selectorInstance = (FeatureSelector) Class.forName(selectorClassName).
                    newInstance();
        } catch(Exception e) {
            log.debug(logTag, 0, "Exception: " + e);
            throw new PropertyException(ps.getInstanceName(),
                    PROP_SELECTOR_CLASS_NAME, "Unable to load class: " +
                    selectorClassName);
        }
        try {
            clustererInstance = (FeatureClusterer) Class.forName(clustererClassName).
                    newInstance();
        } catch(Exception e) {
            throw new PropertyException(ps.getInstanceName(),
                    PROP_CLUSTERER_CLASS_NAME, "Unable to load class: " +
                    clustererClassName);
        }
        try {
            modelInstance = (ClassifierModel) Class.forName(modelClassName).
                    newInstance();
        } catch(Exception e) {
            throw new PropertyException(ps.getInstanceName(),
                    PROP_MODEL_CLASS_NAME, "Unable to load class: " +
                    modelClassName);
        }
        try {
            splitterInstance = (ResultSplitter) Class.forName(splitterClassName).
                    newInstance();
        } catch(Exception e) {
            throw new PropertyException(ps.getInstanceName(),
                    PROP_SPLITTER_CLASS_NAME, "Unable to load class: " +
                    splitterClassName);
        }
        super.newProperties(ps);
        classesField = ps.getString(PROP_CLASSES_FIELD);
        numClassifierFeatures = ps.getInt(PROP_NUM_CLASSIFIER_FEATURES);

        String hs = ps.getString(PROP_HUMAN_SELECTED);
        if(hs != null) {
            humanSelected = parseHumanSelected(ps.getInstanceName(), hs);
        }

        extraClassifications =
                (List<ExtraClassification>) ps.getComponentList(PROP_EXTRA_CLASSIFICATIONS);
    }

    private Map<String, HumanSelected> parseHumanSelected(String instanceName,
            String hs) throws PropertyException {
        Map<String, HumanSelected> m = new HashMap<String, HumanSelected>();
        try {
            DocumentBuilder b = DocumentBuilderFactory.newInstance().
                    newDocumentBuilder();
            URL hsu = getClass().getResource(hs);
            if(hsu == null) {
                hsu = (new File(hs)).toURI().toURL();
            }
            Document d = b.parse(hsu.openStream());
            NodeList n = d.getElementsByTagName("classifier");
            for(int i = 0; i < n.getLength(); i++) {
                HumanSelected h = new HumanSelected((Element) n.item(i));
                m.put(h.getName(), h);
            }
        } catch(MalformedURLException ex) {
            throw new PropertyException(instanceName, PROP_HUMAN_SELECTED,
                    "Bad file name: " + hs);
        } catch(SAXException ex) {
            throw new PropertyException(instanceName, PROP_HUMAN_SELECTED,
                    "Parse error: " + ex.getMessage());
        } catch(IOException ex) {
            throw new PropertyException(instanceName, PROP_HUMAN_SELECTED,
                    "Error reading file: " + ex.getMessage());
        } catch(ParserConfigurationException ex) {
            throw new PropertyException(instanceName, PROP_HUMAN_SELECTED,
                    "Error in parser configuration: " + ex.getMessage());
        }
        return m;
    }
    @ConfigBoolean(defaultValue = true)
    public static final String PROP_DO_CLASSIFICATION = "do_classification";

    private boolean doClassification;

    @ConfigString(defaultValue =
    "com.sun.labs.minion.classification.ContingencyFeatureSelector")
    public static final String PROP_SELECTOR_CLASS_NAME =
            "selector_class_name";

    private String selectorClassName;

    @ConfigString(defaultValue = "com.sun.labs.minion.classification.StemmingClusterer")
    public static final String PROP_CLUSTERER_CLASS_NAME =
            "clusterer_class_name";

    private String clustererClassName;

    @ConfigString(defaultValue = "com.sun.labs.minion.classification.Rocchio")
    public static final String PROP_MODEL_CLASS_NAME = "model_class_name";

    private String modelClassName;

    @ConfigString(defaultValue = "com.sun.labs.minion.classification.KFoldSplitter")
    public static final String PROP_SPLITTER_CLASS_NAME =
            "splitter_class_name";

    protected String splitterClassName;

    @ConfigString(defaultValue = "class")
    public static final String PROP_CLASSES_FIELD = "classes_field";

    private String classesField;

    /**
     * A property for an optional set of from fields to use for each classifier.
     */
    @ConfigComponentList(type = com.sun.labs.minion.classification.ExtraClassification.class, defaultList =
    {})
    public static final String PROP_EXTRA_CLASSIFICATIONS =
            "extra_classifications";

    private List<ExtraClassification> extraClassifications;

    @ConfigInteger(defaultValue = 200)
    public static final String PROP_NUM_CLASSIFIER_FEATURES =
            "num_classifier_features";

    private int numClassifierFeatures;

    @ConfigString(mandatory = false)
    public static final String PROP_HUMAN_SELECTED = "human_selected";

}
