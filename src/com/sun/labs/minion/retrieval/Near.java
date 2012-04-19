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
import java.util.List;


/**
 * A class for the <code>NEAR</code> operator.  In such an operator, all of
 * the terms must appear in the document and in the passage.
 */
public class Near extends Proximity {

    /**
     * Builds a near operator from a list of operands.  The default size
     * for a near operator (1000 words) is used.
     */
    public Near(List operands) {
        this(operands, 1000);
    }
    
    /**
     * Builds a near operator from a list of operands and a size.
     */
    public Near(List operands, int size) {
        super(operands);

        //
        // The size gives us the maximum window size.
        maxWindow = size+1;

        //
        // All terms must appear in the passage, so we won't allow any
        // missing terms.
        maxMissing = 0;
    }
    
    /**
     * Estimates the size of the results set.  We know that we've sorted
     * our list of operands, so this is simply the size of the first
     * element of that list.
     */
    @Override
    protected int calculateEstimatedSize() {
        return operands.size() > 0 ?
            ((QueryElement) operands.get(0)).estimateSize() :
            0;
    }

    /**
     * Evaluates this proximity operator, returning the results.
     */
    @Override
    public ArrayGroup eval(ArrayGroup ag) {

        //
        // Since all of the terms must appear in a satisfying document, we
        // will do an initial AND of the terms and any embedded operators.
        // This will give us a candidate list.
        ArrayGroup candidates = And.and(ag, operands);

        //
        // Now make a list of the terms and pass it through.
        List tl = new ArrayList();
        for(int i = 0; i < terms.length; i++) {
            tl.add(terms[i]);
        }

        return evalTerms(candidates, tl);
    }

    @Override
    public String toStringMod() {return " (maxWin: " + maxWindow + ") "; }
}
