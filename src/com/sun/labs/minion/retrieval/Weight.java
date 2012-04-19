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

public class Weight extends UnaryOperator {

    private float adjustment = 0;
    
    /** 
     * Adjusts the weight of the contained query element by the given
     * adjustment factor.  The adjustment is multiplicative, so an
     * adjustment of 1.0 is equivalent to not using the weight operator.
     * 
     * @param operand the query element to which the weight adjustment is applied
     * @param adjustment a multiplier for the weight
     */
    public Weight(QueryElement operand, float adjustment) {
        super(operand);
        this.adjustment = adjustment;
    } // Weight constructor

    @Override
    public ArrayGroup eval(ArrayGroup ag) {
        QueryElement operand = (QueryElement) operands.get(0);
        ScoredGroup result = operand.eval(ag).getScored();
        result.destructiveMult(adjustment);
        return result;
    }
    
    /**
     * Estimates the size of the results set. This is simply the estimated
     * size of the set that will be produced by the single operand.
     */
    @Override
    protected int calculateEstimatedSize() {
        return ((QueryElement) operands.get(0)).estimateSize();
    }

    @Override
    public String toStringMod() {
        return super.toStringMod() + " " + adjustment;
    }
} // Weight
