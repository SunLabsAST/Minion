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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.sun.labs.minion.QueryConfig;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.util.MinionLog;

public class QueryEvaluator {
    
    protected static MinionLog log = MinionLog.getLog();
    
    protected static String logTag = "EVAL";

    /**
     * Evaluates a field term.
     *
     * @param parts The partitions to evaluate against.
     * @param name The name of the field.
     * @param op The operator to use.
     * @param value The value to compare against.
     * @return a list of <code>ArrayGroup</code>s containing the results
     * for the corresponding partitions.
     */
    public List eval(List parts,
            QueryConfig qc,
            String name, int op, String value) {
        QueryElement qe = new FieldTerm(name, op, value);
        qe.setQueryConfig(qc);
        return eval(parts, qe);
    }
    
    /**
     * Evaluates a query
     *
     * @param parts The partitions to evaluate against.
     * @param qe The query element, with query config and weight function already set
     * @return a list of <code>ArrayGroup</code>s containing the results
     * for the corresponding partitions.
     */
    public List eval(List parts, QueryElement qe) {
        
        List ret = new ArrayList();
        
        //
        // We want to check for a single all-asterisk wildcard, which is a query
        // for all documents.
        if(qe instanceof DictTerm) {
            DictTerm dt = (DictTerm) qe;
            String qt = ((DictTerm) qe).getName();
            if(qt.matches("\\**")) {
                for(Iterator i = parts.iterator(); i.hasNext(); ) {
                    DiskPartition p = (DiskPartition) i.next();
                    ArrayGroup ag = new NegativeGroup();
                    ag.part = p;
                    ret.add(ag);
                }
                return ret;
            }
        }
        
        //
        // This is a normal evaluation.
        for(Iterator i = parts.iterator(); i.hasNext(); ) {
            DiskPartition p = (DiskPartition) i.next();
            qe.setPartition(p);
            ArrayGroup ag = qe.eval(null);
            ag.part = p;
            ag.queryTerms = qe.getQueryTerms();
            qe.qs.normW.start();
            ag.normalize();
            qe.qs.normW.stop();
            ret.add(ag);
        }
        return ret;
    }

} // QueryEvaluator
