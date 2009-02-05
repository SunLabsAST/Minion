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

import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import java.util.ArrayList;
import java.util.List;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.query.Relation;

/**
 * A class that can be used to evaluate a simple parametric query against a 
 * single saved field, like 
 * <code>x = 7</code> or <code>name = foo</code> or <code>7 &lt; x &lt; 25</code>.
 * The aim is to provide an easy way to generate such queries without having
 * to worry about quoting values in the query language.
 */
public class FieldEvaluator {
    
    /**
     * The field term that we'll evaluate.
     */
     FieldTerm term;
    
    /**
     * Creates an evaluator for a simple parametric query.
     * @param field the field to evaluate against
     * @param op the operation to use
     * @param val the value to test
     */
    public FieldEvaluator(String field, Relation.Operator op, Object val) {
        term = new FieldTerm(field, op, val.toString());
    }
    
    /**
     * Creates an evaluator for a range query on a saved field.
     * @param field the field to evaluate against
     * @param lower the lower bound for the range
     * @param includeLower whether the lower bound should be included in the range
     * @param upper the upper bound for the range
     * @param includeUpper whether the upper bound should be included in the range
     */
    public FieldEvaluator(String field, Object lower, 
            boolean includeLower, Object upper, boolean includeUpper) {
        term = new FieldTerm(field, lower, includeLower, upper, includeUpper);
    }

    /**
     * Evaluates the parametric query against the data in an engine.
     * @param e the engine to evaluate against
     * @return a result set containing the documents that match the query.
     */
    public ResultSet eval(SearchEngine e) {
        return eval(e, "-score");
    }
    
    /**
     * Evaluates the parametric query against the data in an engine.
     * @param e the engine to evaluate against
     * @param sortSpec a specification to use when sorting the result set
     * @return a result set containing the documents that match the query.
     */
    public ResultSet eval(SearchEngine e, String sortSpec) {
        
        term.setQueryConfig(e.getQueryConfig());
        
        List<DiskPartition> parts = ((SearchEngineImpl) e).getPM().getActivePartitions();
        
        List<ArrayGroup> groups = new ArrayList<ArrayGroup>();

        //
        // Evaluate the term.
        for(DiskPartition p : parts) {
            term.setPartition(p);
            ArrayGroup ag = term.eval(null);
            ag.part = p;
            ag.queryTerms = term.getQueryTerms();
            ag.normalize();
            groups.add(ag);
        }
        
        return new ResultSetImpl(e, sortSpec, groups);
    }
}
