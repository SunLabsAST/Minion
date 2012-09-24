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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

public abstract class Operator extends QueryElement {

    protected static final Logger logger = Logger.getLogger(Operator.class.
            getName());

    /**
     * The query elements (<code>QueryTerm</code>s and <code>Operator</code>s)
     * that are the operands of this operator.
     */
    protected List<QueryElement> operands;

    public Operator() {
    }

    public Operator(List operands) {
        this.operands = operands;
    } // Operator constructor

    /**
     * Sets the partition that we will be operating on.
     */
    @Override
    public void setPartition(DiskPartition part) {

        //
        // Set the partition for each of our operands.
        for(QueryElement operand : operands) {
            operand.setQueryStats(qs);
            operand.setPartition(part);
        }

        //
        // Sort the elements by their estimated result set size.
        Collections.sort(operands);

        //
        // Do the set partition for this level.
        super.setPartition(part);
    }

    /**
     * Sets the current query configuration.
     */
    @Override
    public void setQueryConfig(QueryConfig qc) {
        super.setQueryConfig(qc);
        for(QueryElement operand : operands) {
            operand.setQueryConfig(qc);
        }
    }

    /** 
     * Gets the internal copy of the list of operands to this operator
     *
     * @return the operands
     */
    public List getOperands() {
        return operands;
    }

    /** 
     * Gets the internal copy of the list of operands to this operator
     *
     */
    public void setOperands(List operands) {
        this.operands = operands;
    }

    /**
     * Sets the current weighting function.  This should be propagated to
     * any components of the query element.
     */
    @Override
    protected void setWeightingFunction(WeightingFunction wf) {
        super.setWeightingFunction(wf);
        for(QueryElement operand : operands) {
            operand.setWeightingFunction(wf);
        }
    }

    /**
     * Sets the current weighting components.  This should be propagated to
     * any components of the query element.
     */
    @Override
    protected void setWeightingComponents(WeightingComponents wc) {
        super.setWeightingComponents(wc);
        for(QueryElement operand : operands) {
            operand.setWeightingComponents(wc);
        }
    }

    /** 
     * Adds a field name to which the search should should be restricted
     *
     * @param fieldName the name of the field
     */
    @Override
    public void addSearchFieldName(String fieldName) {
        super.addSearchFieldName(fieldName);
        for(QueryElement operand : operands) {
            operand.addSearchFieldName(fieldName);
        }
    }

    @Override
    public List getQueryTerms(Comparator c) {
        List terms = new ArrayList();
        List subs = new ArrayList();
        for(QueryElement operand : operands) {
            if(operand instanceof DictTerm) {
                terms.add(operand);
            } else {
                subs.addAll(operand.getQueryTerms(c));
            }
        }

        Collections.sort(terms, c);
        terms.addAll(subs);
        return terms;
    }

    @Override
    public String toString() {
        return toString("");
    }

    @Override
    public String toString(String prefix) {
        StringBuffer mine = new StringBuffer();
        mine.append(super.toString(prefix) + toStringMod() + " estSize: " +
                estSize + "\n");
        if(operands != null) {
            for(QueryElement operand : operands) {
                if(operand != null) {
                    mine.append(operand.toString(prefix + " ") + "\n");
                }
            }
        }
        return mine.toString();
    }

    public String toStringMod() {
        // Get any modifier to print next to this string
        return "";
    }
} // Operator
