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

import java.util.Iterator;
import java.util.List;

public class And extends Operator {

    /**
     * Constructs an <tt>AND</tt> operator that takes a list of operands.
     */
    public And(List operands) {
        super(operands);
    } // And constructor

    
    /**
     * Estimates the size of the results set.
     */
    @Override
    protected int calculateEstimatedSize() {
        return operands.size() > 0 ?
            ((QueryElement) operands.get(0)).estimateSize() :
            0;
    }

    /**
     * Evaluates this operator, returning the results.
     */
    @Override
    public ArrayGroup eval(ArrayGroup ag) {
        return and(ag, operands, strictEval);
    }

    protected static ArrayGroup and(ArrayGroup ag, List operands) {
        return and(ag, operands, false);
    }
    
    /**
     * Runs an intersection of all of the given operands.  This can be used
     * by other classes that want to do an intersection.
     *
     * @param ag Any initial group.  May be <code>null</code>.
     * @param operands The operands to process.
     */
    protected static ArrayGroup and(ArrayGroup ag, List<QueryElement> operands,
                                    boolean strict) {
        ArrayGroup ret = ag;
        for(Iterator i = operands.iterator(); i.hasNext(); ) {
            QueryElement qe = (QueryElement) i.next();
            if(strict) {
                qe.strictEval = strict;
            }
            ret = qe.eval(ret);
        }
        return ret;
    }
    
} // And
