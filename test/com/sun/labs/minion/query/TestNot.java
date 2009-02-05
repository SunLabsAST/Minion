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
import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the and operator.
 */
public class TestNot extends TestBase {

    public TestNot() {
    }

    @Test
    public void basicTest() throws SearchEngineException {
        for(String t1 : terms) {
            Term term = new Term(t1, EnumSet.of(Term.Modifier.MORPH));
            Not not = new Not(term);
            ResultSet rs1 = mi.getSearchEngine().search(not);
            ResultSet rs2 = mi.getSearchEngine().search(String.format(
                    "<not> %s", t1));
            assertTrue(String.format("rs1: %d rs2: %d",
                    rs1.size(), rs2.size()),
                    ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
        }
    }

    @Test
    public void negatedConjunction() throws SearchEngineException {
        for(String t1 : terms) {
            Term term1 = new Term(t1, EnumSet.of(Term.Modifier.MORPH));
            for(String t2 : terms) {
                Term term2 = new Term(t2, EnumSet.of(Term.Modifier.MORPH));
                And and = new And(new Element[]{term1, term2});
                Not not = new Not(and);
                ResultSet rs1 = mi.getSearchEngine().search(not);
                ResultSet rs2 = mi.getSearchEngine().search(String.format(
                        "<not> (%s <and> %s)", t1, t2));
                assertTrue(String.format("rs1: %d rs2: %d",
                        rs1.size(), rs2.size()),
                        ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
            }
        }
    }
    
    @Test
    public void negatedDisjunction() throws SearchEngineException {
        for(String t1 : terms) {
            Term term1 = new Term(t1, EnumSet.of(Term.Modifier.MORPH));
            for(String t2 : terms) {
                Term term2 = new Term(t2, EnumSet.of(Term.Modifier.MORPH));
                Or or = new Or(new Element[]{term1, term2});
                Not not = new Not(or);
                ResultSet rs1 = mi.getSearchEngine().search(not);
                ResultSet rs2 = mi.getSearchEngine().search(String.format(
                        "<not> (%s <or> %s)", t1, t2));
                assertTrue(String.format("rs1: %d rs2: %d",
                        rs1.size(), rs2.size()),
                        ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
            }
        }
    }
}