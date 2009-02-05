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

import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.query.Relation.Operator;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the range operator.
 */
public class TestRanges extends TestBase {

    public static final String[] dates = new String[]{
        "08/17/2007",
        "08/17/2007 15:00:00",
        "Fri Aug 17 12:50:03 EDT 2007",};

    public static final String[] ints = new String[]{
        "1", "200", "1000", "1300", "1400",
    };

    public TestRanges() {
    }

    public void runQuery(String field, Operator leftOp, String leftVal,
            Operator rightOp, String rightVal) throws SearchEngineException {
        Range r = new Range(field, leftOp, leftVal, rightOp, rightVal);
        String q = String.format("(%s %s \"%s\") <and> (%s %s \"%s\")",
                field, leftOp.getRep(), leftVal,
                field, rightOp.getRep(), rightVal);
        ResultSet rs1 = mi.getSearchEngine().search(r);
        ResultSet rs2 = mi.getSearchEngine().search(q);
        assertTrue(String.format("rs1: %d rs2: %d",
                rs1.size(), rs2.size()),
                ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
    }

    @Test
    public void testOpenRanges() throws SearchEngineException {
        for(String i1 : ints) {
            for(String i2 : ints) {
                runQuery("sequence",
                        Operator.GREATER_THAN, i1,
                        Operator.LESS_THAN, i2);
            }
        }
    }

    @Test
    public void testClosedRanges() throws SearchEngineException {
        for(String i1 : ints) {
            for(String i2 : ints) {
                runQuery("sequence",
                        Operator.GEQ, i1,
                        Operator.LEQ, i2);
            }
        }
    }

    @Test
    public void testLeftOpenRanges() throws SearchEngineException {
        for(String i1 : ints) {
            for(String i2 : ints) {
                runQuery("sequence",
                        Operator.GREATER_THAN, i1,
                        Operator.LEQ, i2);
            }
        }
    }

    @Test
    public void testRightOpenRanges() throws SearchEngineException {
        for(String i1 : ints) {
            for(String i2 : ints) {
                runQuery("sequence",
                        Operator.GEQ, i1,
                        Operator.LESS_THAN, i2);
            }
        }
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void badLeftOperator() {
        Range r = new Range("sequence", Operator.LEQ, "10", Operator.LEQ, "17");
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void badRightOperator() {
        Range r = new Range("sequence", Operator.GEQ, "10", Operator.GREATER_THAN, "17");
    }

    @Test(expected=com.sun.labs.minion.SearchEngineException.class)
    public void badType() throws SearchEngineException {
        runQuery("sequence", Operator.GEQ, "bad", Operator.LEQ, "17");
    }

}