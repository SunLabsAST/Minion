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

package com.sun.labs.minion;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that composes a number of individual results filter.  In order for a
 * result to pass this filter, it must pass each of the individual filters.  Note
 * that in order for this class to really be serializable, the filters it's holding
 * must be serializable!
 */
public class CompositeResultsFilter implements ResultsFilter, Serializable {

    private List<ResultsFilter> filters;
    
    private int nTested;
    
    private int nPassed;
    
    /**
     * Creates a composite filter.
     * @param filters the filters to apply
     */
    public CompositeResultsFilter(List<ResultsFilter> filters) {
        this.filters = filters;
    }
    
    /**
     * Creates a composite of two filters. 
     */
    public CompositeResultsFilter(ResultsFilter f1, ResultsFilter f2) {
        filters = new ArrayList<ResultsFilter>();
        filters.add(f1);
        filters.add(f2);
    }
    
    /**
     * Adds a filter to the list of filters to apply.
     * @param rf the filter to add.
     */
    public void addFilter(ResultsFilter rf) {
        filters.add(rf);
    }
    
    @Override
    public boolean filter(ResultAccessor ra) {
        nTested++;
        for(ResultsFilter rf : filters) {
            if(!rf.filter(ra)) {
                return false;
            }
        }
        nPassed++;
        return true;
    }
    
    @Override
    public int getTested() {
        return nTested;
    }
    
    @Override
    public int getPassed() {
        return nPassed;
    }
}
