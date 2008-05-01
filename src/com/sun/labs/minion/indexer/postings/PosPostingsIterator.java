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

package com.sun.labs.minion.indexer.postings;

/**
 * An interface for postings iterators that can return positions associated
 * with IDs.
 */
public interface PosPostingsIterator extends PostingsIterator {

    /**
     * Returns the positions associated with the current ID.  The positions
     * are divided by field.
     *
     * @return A two-dimensional array of int.  The contents of the
     * <em>i<sup>th</sup></em> element of the array are the positions for
     * the field whose ID is <em>i</em>.  The positions stored at element 0
     * are those positions that are not in any named field.  For each of the sub-arrays,
     * the zeroth element of the array gives the number of word positions in that
     * field.
     */
    public int[][] getPositions();
    
}// PosPostingsIterator
