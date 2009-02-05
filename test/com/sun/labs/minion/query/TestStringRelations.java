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
 * Tests for the string relational operators.
 */
public class TestStringRelations extends TestBase {

    public static final String[] strings = new String[]{
        "bug",
        "available",
        "stats",
        "problem",
        "aix",
        "#pragma",
        "shared",
        "storing",
        "dtrace",
        "information"
    };

    public TestStringRelations() {
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
    public void testSubstring() throws SearchEngineException {
        for(String s : strings) {
            runQuery("subject", Operator.SUBSTRING, s);
        }
    }

    @Test
    public void testStarts() throws SearchEngineException {
        for(String s : strings) {
            runQuery("subject", Operator.STARTS, s);
        }
    }

    @Test
    public void testEnds() throws SearchEngineException {
        for(String s : strings) {
            runQuery("subject", Operator.STARTS, s);
        }
    }
}