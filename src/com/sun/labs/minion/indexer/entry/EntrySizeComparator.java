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

package com.sun.labs.minion.indexer.entry;

import java.util.Comparator;



/**
 * A class that can be used to compare terms based upon the number of IDs
 * that are in their postings lists.
 */
public class EntrySizeComparator implements Comparator {

    public EntrySizeComparator() {
	
    } // EntrySizeComparator constructor
    
    /**
     * Determines whether two terms have the same number of IDs associated
     * with them.
     *
     * @param object another <code>Term</code>.
     * @return <code>true</code> if the terms contain the same number of
     * IDs, <code>false</code> otherwise.
     */
    @Override
    public boolean equals(Object object) {
        return this == object;
    }

    /**
     * Compares two terms based on the number of IDs that are associated
     * with them.
     *
     * @param o1 A term.
     * @param o2 Another term.
     * @return A value less than, greater than, or equal to 0 if
     * <code>o1</code> has less, more, or the same number of terms 
     */
    @Override
    public int compare(Object o1, Object o2) {
        return ((Entry) o1).getN() - ((Entry) o2).getN();
    }
    
} // EntrySizeComparator
