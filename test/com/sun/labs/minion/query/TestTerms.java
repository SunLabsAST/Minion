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
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.retrieval.ResultSetImpl;
import java.util.EnumSet;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the terms.
 */
public class TestTerms extends TestBase {

    public TestTerms() {
    }

    @Test
    public void standard() throws SearchEngineException {
        for(String term : terms) {
            Term t = new Term(term);
            t.addModifier(Term.Modifier.MORPH);
            ResultSet rs1 = mi.getSearchEngine().search(t);
            ResultSet rs2 = mi.getSearchEngine().search(term);
            assertTrue(String.format("term: %s rs1: %d rs2: %d", term, rs1.size(), rs2.size()),
                    ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
        }

        for(int i = 0; i < terms.length; i++) {

            //
            // Even though this term is mixed case, it should be treated as
            // case insensitive, beacuse we haven't set the case modifier on
            // the term.
            Term t = new Term(csTerms[i]);
            t.addModifier(Term.Modifier.MORPH);
            ResultSet rs1 = mi.getSearchEngine().search(t);
            ResultSet rs2 = mi.getSearchEngine().search(terms[i]);
            assertTrue(String.format("csterm: %s term: %s rs1: %d rs2: %d",
                    csTerms[i],
                    terms[i],
                    rs1.size(), rs2.size()),
                    ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
        }
    }

    @Test
    public void testCaseOperator() throws SearchEngineException {

        //
        // Test for the given case.
        for(String term : terms) {
            Element el = new Term(term, EnumSet.of(Term.Modifier.CASE));
            ResultSet rs1 = mi.getSearchEngine().search(el);
            ResultSet rs2 = mi.getSearchEngine().search(String.format("<case> <exact> %s", term));
            assertTrue(String.format("term: %s rs1: %d rs2: %d", term,
                    rs1.size(), rs2.size()),
                    ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
        }

        for(String lterm : terms) {
            String term = Character.toUpperCase(lterm.charAt(0)) + lterm.substring(1);
            Element el = new Term(term, EnumSet.of(Term.Modifier.CASE));
            ResultSet rs1 = mi.getSearchEngine().search(el);
            ResultSet rs2 = mi.getSearchEngine().search(String.format("<case> <exact> %s", term));
            assertTrue(String.format("term: %s rs1: %d rs2: %d", term,
                    rs1.size(), rs2.size()),
                    ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
        }
    }

    @Test
    public void testMorphOperator() throws SearchEngineException {
        for(String term : terms) {
            Element el = new Term(term, EnumSet.of(Term.Modifier.MORPH));
            ResultSet rs1 = mi.getSearchEngine().search(el);
            ResultSet rs2 = mi.getSearchEngine().search(String.format(
                    "%s", term));
            assertTrue(String.format("term: %s rs1: %d rs2: %d", term,
                    rs1.size(), rs2.size()),
                    ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
        }
    }

    @Test
    public void testCaseMorph() throws SearchEngineException {

        for(String term : terms) {
            Element el = new Term(term, EnumSet.of(Term.Modifier.CASE, Term.Modifier.MORPH));
            ResultSet rs1 = mi.getSearchEngine().search(el);
            ResultSet rs2 = mi.getSearchEngine().search(String.format(
                    "<case> <morph> %s", term));
            assertTrue(String.format("term: %s rs1: %d rs2: %d", term,
                    rs1.size(), rs2.size()),
                    ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
        }
    }

    @Test
    public void testWild() throws SearchEngineException {
        for(String term : terms) {
            term = term.substring(0, 3) + "*";
            Element el = new Term(term, EnumSet.of(Term.Modifier.WILDCARD));
            ResultSet rs1 = mi.getSearchEngine().search(el);
            ResultSet rs2 = mi.getSearchEngine().search(String.format(
                    "%s", term));
            assertTrue(String.format("term: %s rs1: %d rs2: %d", term,
                    rs1.size(), rs2.size()),
                    ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
        }
        
    }

    @Test
    public void testStemOperator() throws SearchEngineException {
        for(String term : terms) {
            Element el = new Term(term, EnumSet.of(Term.Modifier.STEM));
            ResultSet rs1 = mi.getSearchEngine().search(el);
            ResultSet rs2 = mi.getSearchEngine().search(String.format(
                    "<stem> %s", term));
            assertTrue(String.format("term: %s rs1: %d rs2: %d", term,
                    rs1.size(), rs2.size()),
                    ((ResultSetImpl) rs1).same((ResultSetImpl) rs2));
        }

    }
}