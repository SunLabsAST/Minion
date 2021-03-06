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

import java.util.Collection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import java.util.Comparator;

import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.entry.QueryEntry;

import com.sun.labs.minion.util.Util;

import com.sun.labs.minion.QueryConfig;

/**
 * MultiDictTerm provides the implementation of the TOR operator.
 * The class both extends DictTerm and contains one or more instances
 * of DictTerm.  During evaluation, almost all of the DictTerm code
 * is reused in this class with just a little code added in.  When
 * setPartition is called, MultiDictTerm will fill in its own
 * list of dict entries with the entries generated by calling
 * setPartition on each of the DictTerms it contains.
 *
 * @author Jeff Alexander
 */

public class MultiDictTerm extends DictTerm
{
    protected Collection terms;

    public MultiDictTerm(Collection dictTerms) {
        super(null);
        if (dictTerms == null) {
            terms = new ArrayList();
        } else {
            terms = dictTerms;
        }
        doMorph = false;
        doStem = false;
        doWild = false;
        doExpand = false;
    }

    /** 
     * Set the query config for all the sub-terms.
     */
    public void setQueryConfig(QueryConfig qc) {
        super.setQueryConfig(qc);

        for (Iterator it = terms.iterator(); it.hasNext();) {
            DictTerm term = (DictTerm)it.next();
            term.setQueryConfig(qc);
        }
    }
    
    /** 
     * Overrides the standard DictTerm setPartition.  This will
     * call setPartition on all of terms contained in this
     * object, then fill in DictTerm.dictEntries with the
     * dictEntries from all of them.
     * 
     * @param part the partition that we will be evaluating against
     */
    public void setPartition(DiskPartition part) {
        //
        // First, call up to setPartition up the inheritance chain.
        // Some things need to get set higher up, but then we'll replace
        // whatever DictTerm changed.
        
        super.setPartition(part);
        Set variants = new HashSet();

        //
        // Get all the dictionary entries for all the dict terms.
        for (Iterator it = terms.iterator(); it.hasNext();) {
            DictTerm term = (DictTerm)it.next();
            term.setPartition(part);
            for (int i = 0; i < term.dictEntries.length; i++) {
                variants.add(term.dictEntries[i]);
            }
        }

        //
        // Assemble them into dictEntries and sort.
        dictEntries = new QueryEntry[variants.size()];
        variants.toArray(dictEntries);
        Util.sort(dictEntries, (Comparator)this);
    }

    public String getName() {
        return "MultiDictTerm";
    }
    
    public String toString() {
        String res = "";
        for (Iterator it = terms.iterator(); it.hasNext();) {
             DictTerm term = (DictTerm)it.next();
             res += term.toString() + " ";
        }
        return res;
    }

    public String toString(String prefix) {
        String res = super.toString(prefix) + "\n";
        for (Iterator it = terms.iterator(); it.hasNext();) {
             DictTerm term = (DictTerm)it.next();
             res += term.toString(prefix + " ") + "\n";
        }
        return res;
    }

}
