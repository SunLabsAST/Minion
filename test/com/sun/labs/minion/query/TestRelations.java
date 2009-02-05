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
 * Tests for the relational operators.
 */
public class TestRelations extends TestBase {

    public static final String[] dates = new String[]{
        "08/17/2007",
        "08/17/2007 15:00:00",
        "Fri Aug 17 12:50:03 EDT 2007",};

    public static final String[] ints = new String[]{
        "1", "200", "1000", "1300", "1400",
    };

    public TestRelations() {
    }

    private void runQuery(String field, Operator o, String val) throws SearchEngineException {
        Relation r = new Relation(field, o, val);
        ResultSet rs1 = mi.getSearchEngine().search(r);
        ResultSet rs2 = mi.getSearchEngine().search(String.format(
                "%s %s \"%s\"", field, o.getRep(), val));
        assertTrue(String.format("rs1: %d rs2: %d",
                rs1.size(), rs2.size()),
                ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
    }

    @Test
    public void testLessThan() throws SearchEngineException {
        for(String date : dates) {
            runQuery("date", Operator.LESS_THAN, date);
            runQuery("date", Operator.LEQ, date);
        }
        for(String i : ints) {
            runQuery("sequence", Operator.LESS_THAN, i);
            runQuery("sequence", Operator.LEQ, i);
        }
    }

    @Test
    public void testGreaterThan() throws SearchEngineException {
        for(String date : dates) {
            runQuery("date", Operator.GREATER_THAN, date);
            runQuery("date", Operator.GEQ, date);
        }
        for(String i : ints) {
            runQuery("sequence", Operator.GREATER_THAN, i);
            runQuery("sequence", Operator.GEQ, i);
        }
    }

    @Test
    public void testEquals() throws SearchEngineException {
        for(String date : dates) {
            runQuery("date", Operator.EQUALS, date);
        }
        for(String i : ints) {
            runQuery("sequence", Operator.EQUALS, i);
        }
    }

    @Test(expected = java.lang.IllegalArgumentException.class)
    public void badOperator() {
        Relation r = new Relation("sequence", Operator.SUBSTRING, "10");
    }

    @Test(expected = com.sun.labs.minion.SearchEngineException.class)
    public void badType() throws SearchEngineException {
        runQuery("sequence", Operator.GEQ, "bad");
    }

}