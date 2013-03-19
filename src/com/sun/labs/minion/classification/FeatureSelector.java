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


import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.pipeline.StopWords;
import com.sun.labs.minion.retrieval.WeightingComponents;

/**
 * Selects terms from a given document or set of documents,
 * relative to the collection the terms are part of.
 */

public interface FeatureSelector {
    /**
     *  Provides a set of human selected terms that should be included or
     * excluded from consideration during the feature selection process.
     */
    public void setHumanSelected(HumanSelected hs);
    
    /**
     * Selects the features from the documents in the training set.
     *
     * @param set the set of feature clusters from the training set.
     * @param numFeatures the number of features to select.
     * @return a sorted set of the features
     */
    public FeatureClusterSet select(FeatureClusterSet set,
            WeightingComponents wc,
            int numTrainingDocs,
            int numFeatures,
            SearchEngine engine);
    
    /**
     * Sets a stopword list:  words that should be ignored when selecting
     * features.
     *
     * @param stopWords the set of words to ignore when performing feature
     * selection.
     */
    void setStopWords(StopWords stopWords);
}
