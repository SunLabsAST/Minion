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

import java.util.List;

/**
 * An interface for classifier models that will allow explanations to be generated
 * inidicating why (or why not) particular documents were (or were not) classified
 * into a given class.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public interface ExplainableClassifierModel {
    
    /**
     * Describes the classifier model.
     */
    public String describe();
    
    /**
     * Explains why (or why not) the document with the given key would (or would not) 
     * be classified into this class.
     * 
     * @param key the key of the document whose classification we want to explain
     * @param includeDocTerms if true, the explanation will include a description
     * of the terms from the document.
     * @return the explanation as a string.
     */
    public String explain(String key, boolean includeDocTerms);
    
    /**
     * Explains the score that a given document would get for this classifier.
     * 
     * @param key the key of the document that we want to explain
     * @return a  list of features that contributed 
     * to the score.  The weight associated with the features is the proportion
     * of contribution of that feature to the overall score.  The list will
     * be ordered from greatest contribution proportion to least contribution
     * percentage.
     */
    public List<WeightedFeature> explain(String key);
}
