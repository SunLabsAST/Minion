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

import com.sun.labs.minion.retrieval.ResultSetImpl;

import com.sun.labs.minion.IndexConfig;


/**
 * Result Splitters split a result set into two distinct sets suitable
 * for use in training and validation.
 *
 */

public interface ResultSplitter 
{
/** 
 * Initializes the class.  Since implementors of ResultSplitter will
 * be created via reflection, the default constructor is used.  After
 * instantiation, this init method is called.
 * 
 * @param results the result set to split up
 * @param iC the index config, possibly containing relevent settings for this splitter
 */
    public void init(ResultSetImpl results, IndexConfig iC);
    
    /** 
     * Gets the minimum number of docs needed for this splitter to be
     * useful.
     * 
     * @return a positive integer
     */
    public int getMinDocs();

    /** 
     * Gets the first of the two subset
     * 
     * @return a result set that is a subset of the one passed in
     */
    public ResultSetImpl getTrainSet();

    /** 
     * Gets the second of the two subsets
     * 
     * @return a result set that is a subset of the one passed in
     */
    public ResultSetImpl getValidateSet();

    /** 
     * Advances to the next split, if there is one.  getFirstSet()
     * and getSecondSet() will return the new splits after this
     * method is called
     * 
     * @return true if there is another split available
     */
    public boolean nextSplit();
    
}
