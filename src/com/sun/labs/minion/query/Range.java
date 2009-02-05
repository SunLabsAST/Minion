/*
 * Copyright 2007-2009 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.labs.minion.query;

import com.sun.labs.minion.query.Relation.Operator;
import com.sun.labs.minion.retrieval.FieldTerm;
import com.sun.labs.minion.retrieval.QueryElement;

/**
 * A range query.  A range query returns all the elements whose
 * values fall between certain values.  The end points of the range can be
 * inclusive or exclusive, depending on the relational operators that are used
 * to build the range.
 */
public class Range extends Element {

    private String field;

    private Operator leftOp;

    private Operator rightOp;

    private String leftVal;

    private String rightVal;

    private void checkOp(Operator o) {
        if(!o.isRangeValid()) {
            throw new IllegalArgumentException(String.format("Operator %s is not a valid" +
                    " range operator.", o));
        }
    }

    /**
     * Creates a range operator.
     * @param field the field that we're interested in
     * @param leftOp the operator for the left hand end of the range.  Must be either {@link Relation.Operator.GEQ} or
     * {@link Relation.Operator.GREATER_THAN}
     * @param leftVal the value for the left hand end of the range.
     * @param rightOp the operator for the right hand end of the range.  Must be either {@link Relation.Operator.LEQ} or
     * {@link Relation.Operator.LESS_THAN}
     * @param rightVal the value for the right hand end of the range.
     * @throws IllegalArgumentException if the preconditions for the operator types and positions are not met.
     */
    public Range(String field, 
            Operator leftOp, String leftVal,
            Operator rightOp, String rightVal) {
        
        //
        // Check to make sure these are valid range operators.
        checkOp(leftOp);
        checkOp(rightOp);

        //
        // Make sure the range is enclosed!
        if(leftOp == Operator.LEQ || leftOp == Operator.LESS_THAN) {
            throw new IllegalArgumentException(String.format(
                    "Operator %s is not valid for the left end of a range", leftOp));
        }

        if(rightOp == Operator.GEQ || rightOp == Operator.GREATER_THAN) {
            throw new IllegalArgumentException(String.format(
                    "Operator %s is not valid for the right end of a range", leftOp));
        }
        
        this.field = field;
        this.leftOp = leftOp;
        this.rightOp = rightOp;
        this.leftVal = leftVal;
        this.rightVal = rightVal;
    }

    public String getField() {
        return field;
    }

    public Operator getRightOperator() {
        return rightOp;
    }

    public Operator getLeftOperator() {
        return leftOp;
    }

    public String getRightValue() {
        return rightVal;
    }

    public String getLeftValue() {
        return leftVal;
    }

    public QueryElement getQueryElement() {
        return new FieldTerm(field,
                leftVal, leftOp == Operator.GEQ,
                rightVal, rightOp == Operator.LEQ);
    }

    public String toString() {
        return field + " " + leftOp + " " + leftVal + " <and> " +
                field + " " + rightOp + " " + rightVal;
    }
}
