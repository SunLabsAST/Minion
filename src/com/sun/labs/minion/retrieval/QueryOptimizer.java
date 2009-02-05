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

import java.util.List;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.HashMap;
import java.util.Iterator;

import com.sun.labs.minion.query.Relation;

/**
 * The QueryOptimizer steps through a query, looking for
 * changes that can be made to optimize the evaluation
 * of the query.  This is very simplistic right now --
 * it will combine relational operators into range
 * operators where possible.
 *
 * @author Jeff Alexander
 */

public class QueryOptimizer
{
    
    public QueryOptimizer() {

    }

    /** 
     * Optimize the current QE and returns it.  This method
     * is non-static so that we could eventually add some
     * evaluation that needs state.
     * 
     * @param qe the query element to optimize
     * @return the optimized query
     */
    public QueryElement optimize(QueryElement qe) {
        //
        // This is dumb and ugly
        if (qe instanceof And) {
            And qand = (And)qe;
            List operands = qand.getOperands();
            //
            // Are there 2 operands with the same field?  First figure out
            // which if any fields are used and group them.
            HashMap fields = new HashMap();
            for (Iterator oit = operands.iterator(); oit.hasNext(); ) {
                QueryElement curr = (QueryElement)oit.next();
                if (curr instanceof FieldTerm) {
                    FieldTerm ft = (FieldTerm)curr;
                    List terms = (List)fields.get(ft.getName());
                    if (terms == null) {
                        terms = new ArrayList();
                    }
                    terms.add(ft);
                    fields.put(ft.getName(), terms);
                }
            }

            //
            // Now see if there are two of any field and if they're
            // appropriate for being replaced with a range op.
            for (Iterator keyIt = fields.keySet().iterator(); keyIt.hasNext(); ) {
                String currKey = (String)keyIt.next();
                List terms = (List)fields.get(currKey);

                if (terms.size() == 2) {
                    FieldTerm ft1 = (FieldTerm)terms.get(0);
                    FieldTerm ft2 = (FieldTerm)terms.get(1);

                    Object objLower = null;
                    boolean incLower = false;
                    Object objUpper = null;
                    boolean incUpper = false;
                    //
                    // Is term one a < or <= and term two a
                    // > or >=?
                    if (((ft1.getOp() == Relation.Operator.LESS_THAN)
                         || (ft1.getOp() == Relation.Operator.LEQ))
                        && ((ft2.getOp() == Relation.Operator.GREATER_THAN)
                            || (ft2.getOp() == Relation.Operator.GEQ))) {
                        
                        objUpper = ft1.getValue();
                        incUpper = ft1.getOp() == Relation.Operator.LEQ;
                        objLower = ft2.getValue();
                        incLower = ft2.getOp() == Relation.Operator.GEQ;
                    }
                    //
                    // Is term one a > or >= and term two a
                    // < or <=?
                    if (((ft1.getOp() == Relation.Operator.GREATER_THAN)
                         || (ft1.getOp() == Relation.Operator.GEQ))
                        && ((ft2.getOp() == Relation.Operator.LESS_THAN)
                            || (ft2.getOp() == Relation.Operator.LEQ))) {
                        objUpper = ft2.getValue();
                        incUpper = ft2.getOp() == Relation.Operator.LEQ;
                        objLower = ft1.getValue();
                        incLower = ft1.getOp() == Relation.Operator.GEQ;
                    }
                    
                    //
                    // Did we get values for objLower and objUpper?
                    if ((objLower != null) && (objUpper != null)) {
                        FieldTerm result = new FieldTerm(ft1.getName(),
                                                         objLower, incLower,
                                                         objUpper, incUpper);
                        //
                        // Remove the individual field terms and replace
                        // with this one.
                        operands.set(operands.indexOf(ft1), result);
                        operands.remove(ft2);
                    }
                }
            }
        }
        
        if (qe instanceof Operator) {
            ListIterator it = ((Operator)qe).getOperands().listIterator();
            while (it.hasNext()) {
                QueryElement currElem = (QueryElement)it.next();
                QueryElement newElem = optimize(currElem);
                if (newElem != currElem) {
                    it.set(newElem);
                }
            }
        }
        return qe;
    }
}
