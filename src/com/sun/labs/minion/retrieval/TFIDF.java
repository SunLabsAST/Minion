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
package com.sun.labs.minion.retrieval;

import java.util.logging.Logger;

/**
 * An implementation of the standard tfidf weighting function.
 * 
 */
public class TFIDF implements WeightingFunction {

    protected static Logger logger = Logger.getLogger(TFIDF.class.getName());

    protected static String logTag = "TFIDF";

    /**
     * Initializes the TFIDF weighting function for the given term,
     * calculating the collection-level IDF component, which is returned.
     * @param wc a set of weighting components.
     */
    public float initTerm(WeightingComponents wc) {
        if(wc.ft == 0) {
            wc.wt = 0;
        } else {
            wc.wt = (float) Math.log((float) wc.N / wc.ft);
        }

        //
        // If we got zero, then we're going to have trouble with weighting
        // things, so let's give a little weight.
        if(wc.wt == 0) {
            wc.wt = 1e-7f;
        }
        return wc.wt;
    }

    /**
     * Calculates the weight for a particular term in a particular
     * document, given a set of weighting components.
     *
     * @return the weight of the given term in the given document.
     */
    public float termWeight(WeightingComponents wc) {
        float res = 0;
        if(wc.fdt != 0) {
            res = wc.wt * (float) (1 + Math.log(wc.fdt));
        }
        return res;
    }
} // TFIDF
