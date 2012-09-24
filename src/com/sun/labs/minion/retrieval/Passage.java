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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * A class implementing the <code>&lt;passage&gt;</code> operator.
 */
public class Passage extends Proximity {

    /**
     * Creates a passage operator for the given operands.
     */
    public Passage(List operands) {
        super(operands);

        //
        // The maximum window size is related to the number of query
        // terms.
        maxWindow = 100 + terms.length * 25;

        //
        // We'll allow half of the terms to go missing.
        maxMissing = Math.min(terms.length / 2, 9);
    }

    /**
     * Estimates the size of the results set. This is the minimum of the
     * sum of the sizes of the terms and the minimum size of any operators
     * that are operands, since these will be anded with the set.
     */
    @Override
    protected int calculateEstimatedSize() {
        int tsz = 0;
        int osz = Integer.MAX_VALUE;
        for(QueryElement operand : operands) {
            if(operand instanceof DictTerm) {
                tsz += operand.estimateSize();
            } else {
                osz = Math.min(osz, operand.estimateSize());
            }
        }
        return Math.min(tsz, osz);
    }

    /**
     * Evaluates this passage operator, returning the results.
     */
    @Override
    public ArrayGroup eval(ArrayGroup ag) {

        //
        // We'll do an or of the terms, and then restrict that with any
        // non-terms in the operands.
        ScoredGroup candidates = null;
        for(QueryElement operand : operands) {
            if(operand instanceof DictTerm) {
                if(candidates == null) {
                    candidates = (ScoredGroup) operand.eval(null);
                } else {
                    candidates = (ScoredGroup) candidates.union(operand.eval(null));
                }
            }
        }

        //
        // Restrict the candidate set, if we were given an array group as
        // an argument.
        if(ag != null) {
            candidates = (ScoredGroup) candidates.intersect(ag);
        }

        //
        // Now, we'll run back through the operands again, intersecting
        // any non-terms that we find with our candidate set.
        for(QueryElement operand : operands) {
            if(!(operand instanceof DictTerm)) {
                candidates = (ScoredGroup) operand.eval(candidates);
            }
        }

        //
        // Now make a list of the terms and pass it through.
        List tl = new ArrayList();
        tl.addAll(Arrays.asList(terms));
        return evalTerms(candidates, tl);
    }

    /**
     * Evaluates this passage operator for a particular document, returning
     * the passages found in that particular document.  This eval method is
     * meant to be used when building passages for display after
     * retrieval.  It assumes that all of the operands given to the
     * constructor are terms.
     */
    public ArrayGroup eval(int doc) {
        ScoredGroup candidates = new ScoredGroup(1);
        candidates.docs[candidates.size++] = doc;

        //
        // Now make a list of the terms and pass it through.
        List tl = new ArrayList();
        tl.addAll(Arrays.asList(terms));
        return evalTerms(candidates, tl);
    }
}
