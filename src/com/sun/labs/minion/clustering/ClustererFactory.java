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
package com.sun.labs.minion.clustering;

import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.ConfigString;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import com.sun.labs.minion.classification.FeatureClusterer;
import com.sun.labs.minion.classification.FeatureSelector;
import com.sun.labs.minion.pipeline.StopWords;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A factory for results clusterers.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class ClustererFactory implements com.sun.labs.util.props.Configurable {

    protected String name;

    private Class<FeatureSelector> featureSelectorClass;

    private Class<FeatureClusterer> featureClustererClass;

    private Class<AbstractClusterer> resultsClustererClass;

    Logger logger = Logger.getLogger(getClass().getName());

    public static final String logTag = "RCF";

    /**
     * Creates a ClustererFactory
     */
    public ClustererFactory() {
    }

    public AbstractClusterer getResultsClusterer() {
        return getResultsClusterer(resultsClustererClass, numFeatures);
    }

    public AbstractClusterer getResultsClusterer(
            Class<AbstractClusterer> clustererClass) {
        return getResultsClusterer(clustererClass, numFeatures);
    }

    public AbstractClusterer getResultsClusterer(
            Class<AbstractClusterer> clustererClass,
            int nFeatures) {
        AbstractClusterer ret = null;
        try {
            ret = clustererClass.newInstance();
            FeatureClusterer fc = getFeatureClusterer();
            FeatureSelector fs = getFeatureSelector();
            if(fc != null && fs != null) {
                ret.setFeatureInfo(fc, fs, nFeatures);
            }
        } catch(InstantiationException ex) {
            logger.log(Level.SEVERE, "Error instantiating results clusterer:" +
                    clustererClass + ", " + ex);
        } catch(IllegalAccessException ex) {
            logger.log(Level.SEVERE, "Error instantiating results clusterer:" +
                    clustererClass + ", " + ex);
        } finally {
            return ret;
        }
    }

    public FeatureSelector getFeatureSelector() {
        FeatureSelector ret = null;
        try {
            ret = featureSelectorClass.newInstance();
            ret.setStopWords(stopWords);
        } catch(IllegalAccessException ex) {
            logger.log(Level.SEVERE, "Error instantiating feature selector:" +
                    featureSelectorClass + ", " + ex);
        } catch(InstantiationException ex) {
            logger.log(Level.SEVERE, "Error instantiating feature selector:" +
                    featureSelectorClass + ", " + ex);
        } finally {
            return ret;
        }
    }

    public FeatureClusterer getFeatureClusterer() {
        FeatureClusterer ret = null;
        try {
            ret = featureClustererClass.newInstance();
        } catch(InstantiationException ex) {
            logger.log(Level.SEVERE, "Error instantiating feature clusterer:" +
                    featureClustererClass + ", " + ex);
        } catch(IllegalAccessException ex) {
            logger.log(Level.SEVERE, "Error instantiating feature clusterer:" +
                    featureClustererClass + ", " + ex);
        } finally {
            return ret;
        }
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        featureSelectorClassName =
                ps.getString(PROP_FEATURE_SELECTOR_CLASS_NAME);
        featureClustererClassName =
                ps.getString(PROP_FEATURE_CLUSTERER_CLASS_NAME);
        numFeatures = ps.getInt(PROP_NUM_FEATURES);
        resultsClustererClassName =
                ps.getString(PROP_RESULTS_CLUSTERER_CLASS_NAME);

        //
        // Get the actual classes for the names.
        try {
            featureSelectorClass =
                    (Class<FeatureSelector>) Class.forName(
                    featureSelectorClassName);
        } catch(ClassNotFoundException ex) {
            throw new PropertyException(ps.getInstanceName(),
                    PROP_FEATURE_SELECTOR_CLASS_NAME, "Unknown feature selector class: " +
                    featureSelectorClassName);
        }
        try {
            featureClustererClass =
                    (Class<FeatureClusterer>) Class.forName(
                    featureClustererClassName);
        } catch(ClassNotFoundException ex) {
            throw new PropertyException(ps.getInstanceName(),
                    PROP_FEATURE_CLUSTERER_CLASS_NAME, "Unknown feature clusterer class: " +
                    featureClustererClassName);
        }
        try {
            resultsClustererClass =
                    (Class<AbstractClusterer>) Class.forName(
                    resultsClustererClassName);
        } catch(ClassNotFoundException ex) {
            throw new PropertyException(ps.getInstanceName(),
                    PROP_RESULTS_CLUSTERER_CLASS_NAME, "Unknown results clusterer class: " +
                    resultsClustererClassName);
        }
        stopWords = (StopWords) ps.getComponent(PROP_STOP_WORDS);
    }

    public String getName() {
        return name;
    }

    public int getNumFeatures() {
        return numFeatures;
    }

    public void setNumFeatures(int numFeatures) {
        this.numFeatures = numFeatures;
    }
    @ConfigString(defaultValue =
    "com.sun.labs.minion.classification.MIFeatureSelector")
    public static final String PROP_FEATURE_SELECTOR_CLASS_NAME =
            "feature_selector_class_name";

    private String featureSelectorClassName;

    @ConfigString(defaultValue =
    "com.sun.labs.minion.classification.StemmingClusterer")
    public static final String PROP_FEATURE_CLUSTERER_CLASS_NAME =
            "feature_clusterer_class_name";

    private String featureClustererClassName;

    @ConfigInteger(defaultValue = 800)
    public static final String PROP_NUM_FEATURES =
            "num_features";

    private int numFeatures;

    @ConfigString(defaultValue = "com.sun.labs.minion.clustering.KMeans")
    public static final String PROP_RESULTS_CLUSTERER_CLASS_NAME =
            "results_clusterer_class_name";

    private String resultsClustererClassName;

    public static final String CLUSTERER_FACTORY_CONFIG_NAME =
            "results_clusterer_factory";

    @ConfigComponent(type = com.sun.labs.minion.pipeline.StopWords.class)
    public static final String PROP_STOP_WORDS =
            "stop_words";

    private StopWords stopWords;

}
