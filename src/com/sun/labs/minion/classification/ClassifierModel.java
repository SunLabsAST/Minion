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


import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.Progress;



import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;

import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.retrieval.cache.TermCache;
import java.util.Map;

/**
 * An interface for training and using classifiers.
 */
public interface ClassifierModel {

    /**
     * Trains the classifier on a set of documents.
     *
     * @param name the name of the class, as specified by the application
     * @param fieldName the name of the field where the results of this classifier will
     * be stored
     * @param manager the manager for the partitions against which we're
     * training
     * @param docs a set of results containing the training documents for
     * the class.
     * @param fcs the set of features to use when training this classifier
     * @param termStats A map from names to term statistics for the feature
     * clusters.  This map will be populated with all of the elements of
     * <code>fcs</code> when this method is called.
     * @param termCaches A map from partitions to term caches containing the
     * uncompressed postings for the feature clusters in <code>fcs</code>.  The
     * caches will be fully populated with the clusters from <code>fcs</code> when
     * this method is called.
     * @throws SearchEngineException if there is any problem training the
     * classifier.
     */
    public void train(String name,
            String fieldName,
            PartitionManager manager,
            ResultSetImpl docs,
            FeatureClusterSet fcs,
            Map<String, TermStatsImpl> termStats,
            Map<DiskPartition, TermCache> termCaches,
            Progress progress) throws SearchEngineException;
    
    /**
     * Sets the name of the model.
     */
    public void setModelName(String modelName);
    
    /**
     * Gets the name of the model.
     */
    public String getModelName();
    
    /**
     * Gets the field name where the results of this classifier will be stored.
     */
    public String getFieldName();
    
    /**
     * Sets the name of the field where the results of this classifier will be stored.
     */
    public void setFieldName(String fieldName);
    
    /**
     * Sets the name of the field from which the classifier was built, since
     * we'll want to classify against terms only from that field.
     * @param fromField the name of the field that was used to generate features
     */
    public void setFromField(String fromField);
    
    public String getFromField();

    /**
     * Gets the features that this classifier model will be using for
     * classification.  This method must return a set containing instances
     * of {@link Feature}.
     *
     * @return A set of features that will be used for classification.
     * @see Feature
     */
    public FeatureClusterSet getFeatures();

    /**
     * Gets a single feature of the type that this classifier model uses.
     * This feature will be filled in from the data stored for the
     * classifier.
     *
     * @return a new feature to be used during classification
     */
    public Feature getFeature();

    /**
     * Sets the search engine that this classifier is part of.
     */
    public void setEngine(SearchEngine e);

    /**
     * Dumps any classifier specific data to the given file.  This is only
     * for data that is not stored in the standard dictionaries.
     *
     * @param raf The file to which the data can be dumped.
     */
    public void dump(RandomAccessFile raf) throws java.io.IOException;

    /**
     * Sets the features that the classifier model will use for
     * classification.  The provided set will only contain instances of
     * {@link Feature}.
     *
     * @param f the set of features.
     * @see Feature
     */
    public void setFeatures(FeatureClusterSet f);

    /**
     * Reads any classifier specific data from the given file.
     *
     * @param raf The file from which the data can be read.  The file will
     * be positioned appropriately so that the data can be read.
     */
    public void read(RandomAccessFile raf) throws java.io.IOException;

    /**
     * Classifies a disk partition of documents.
     *
     * @param sdp a disk partition
     * @return An array of float.  For a given document ID in the documents
     * that were classified, if that element of the array is greater than
     * 0, then the document should be classified into that class.  The
     * value of the element indicates the similarity of that document to
     * the classifier model.
     */
    public float[] classify(DiskPartition sdp);

    /**
     * Computes the similarity of the given document and the classifier.
     *
     * @param key the key of the document for which we wish to compute
     * similarity
     * @return the similarity between the document and this classifier.
     * The absolute value of the return value indicates the degree of
     * similarity.  A return value that is greater than 0 should indicate
     * that the given document would be classified into this class.  A
     * return value less than 0 should indicate that the given document
     * would not be classified into this class.
     */
    public float similarity(String key);

    /**
     * Computes the similarity of the given document vector and the classifier.
     *
     * @param v the document vector with which we want to calculate
     * similarity
     * @return the similarity between the document and this classifier.
     * The absolute value of the return value indicates the degree of
     * similarity.  A return value that is greater than 0 should indicate
     * that the given document would be classified into this class.  A
     * return value less than 0 should indicate that the given document
     * would not be classified into this class.
     */
    public float similarity(DocumentVector v);
    
    /**
     * Computes the similarity between this classifier model and another.
     * @param cm the model we want to compute the similarity to
     * @return the similarity between this classifier model and the other
     */
    public float similarity(ClassifierModel cm);

    /** 
     * Creates a new instance of this classifier model.  This is a non-static
     * factory method.
     * 
     * @return a new instance of the classifier model
     */
    public ClassifierModel newInstance();
}// ClassifierModel
