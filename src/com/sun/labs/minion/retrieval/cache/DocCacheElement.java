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
package com.sun.labs.minion.retrieval.cache;

import com.sun.labs.minion.WeightedFeature;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.retrieval.SingleFieldDocumentVector;
import com.sun.labs.minion.retrieval.WeightingComponents;
import com.sun.labs.minion.retrieval.WeightingFunction;
import java.util.logging.Logger;

/**
 *
 */
public class DocCacheElement {

    private static Logger logger = Logger.getLogger(DocCacheElement.class.
            getName());

    private SingleFieldDocumentVector dvi;

    private DiskField df;

    /**
     * Creates an element for a document vector
     * @param key the key of the document whose vector we want
     * @param df the field from which we're drawing the vector
     * @param wf the weighting function to use for generating the vector
     * @param wc the weighting components to use for generating the vector
     */
    public DocCacheElement(String key, DiskField df,
            WeightingFunction wf, WeightingComponents wc) {
        dvi = new SingleFieldDocumentVector(key, df, wf, wc);
    }

    public WeightedFeature[] getFeatures() {
        return dvi.getFeatures();
    }

    public SingleFieldDocumentVector getDVI() {
        return dvi;
    }
}
