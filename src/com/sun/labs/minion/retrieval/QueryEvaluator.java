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

import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.query.Relation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

public class QueryEvaluator {

    protected static final Logger logger = Logger.getLogger(QueryEvaluator.class.
            getName());

    /**
     * Evaluates a field term.
     *
     * @param parts The partitions to evaluate against.
     * @param name The name of the field.
     * @param op The operator to use.
     * @param value The value to compare against.
     * @return a list of
     * <code>ArrayGroup</code>s containing the results for the corresponding
     * partitions.
     */
    public List eval(List<DiskPartition> parts,
                     QueryConfig qc,
                     String name, Relation.Operator op, String value) {
        QueryElement qe = new FieldTerm(name, op, value);
        qe.setQueryConfig(qc);
        return eval(parts, qe);
    }

    /**
     * Evaluates a query
     *
     * @param parts The partitions to evaluate against.
     * @param qe The query element, with query config and weight function
     * already set
     * @return a list of
     * <code>ArrayGroup</code>s containing the results for the corresponding
     * partitions.
     */
    public List eval(Collection<DiskPartition> parts, QueryElement qe) {

        List ret = new ArrayList();

        //
        // We want to check for a single all-asterisk wildcard, which is a query
        // for all documents.
        if(qe instanceof DictTerm) {
            String qt = ((DictTerm) qe).getName();
            if(qt.matches("\\**")) {
                for(DiskPartition p : parts) {
                    if(p.isClosed()) {
                        continue;
                    }
                    ArrayGroup ag = new NegativeGroup();
                    ag.part = p;
                    ret.add(ag);
                }
                return ret;
            }
        }

        //
        // This is a normal evaluation.
        for(DiskPartition p : parts) {
            if(p.isClosed()) {
                continue;
            }
            qe.setPartition(p);
            ArrayGroup ag = qe.eval(null);
            if(ag != null) {
                ag.part = p;
                ag.queryTerms = qe.getQueryTerms();
                ret.add(ag);
            }
        }
        return ret;
    }
} // QueryEvaluator
