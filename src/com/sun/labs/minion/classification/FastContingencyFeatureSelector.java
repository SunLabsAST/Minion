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

package com.sun.labs.minion.classification;

import com.sun.labs.minion.SearchEngine;
import java.util.Set;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.retrieval.WeightingComponents;

/**
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class FastContingencyFeatureSelector extends ContingencyFeatureSelector {
    
    /**
     * Creates a FastContingencyFeatureSelector
     */
    public FastContingencyFeatureSelector() {
        super();
    }
    
    public FastContingencyFeatureSelector(int type) {
        super(type);
    } // ContingencyFeatureSelector constructor
    
    /**
     * Computes the contingency table using only the term stats.  This will
     * produce inaccurate but fast counts for the table.
     */
    protected void computeContingency(ContingencyFeatureCluster curr,
            SearchEngine engine,
            WeightingComponents wc,
            int tsize, int N) {
        
        TermStatsImpl ts = new TermStatsImpl(curr.getName());
        for(Feature f : (Set<Feature>) curr.getContents()) {
            TermStatsImpl fs = wc.getTermStats(f.getName());
            if(fs != null) {
                ts.add(fs);
            }
        }
        curr.counter.b = ts.getDocFreq() - curr.counter.b;
        curr.counter.c = tsize - curr.counter.a;
        curr.counter.d = N - curr.counter.c;
        curr.counter.N = N;
        
        //
        // If the weight was previously calculated, we need to recalc
        // since we changed the a,b,c,d,N values
        curr.counter.weightCalculated = false;
        curr.getWeight();
    }
}
