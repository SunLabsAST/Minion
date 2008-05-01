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
 * An interface for occurrence information that can be used during
 * indexing.  An occurrence supports a single operation:  getting the ID of
 * the occurrence.
 */
public interface Occurrence {

    /**
     * Gets the ID of the term in this occurrence.
     *
     * @return the ID for the term.
     */
    public int getID();

    /**
     * Sets the ID for the term in this occurrence.
     *
     * @param id the ID.
     */
    public void setID(int id);

    /**
     * Gets the count of occurrences that this occurrence represents.
     *
     * @return the number of occurrences.
     */
    public int getCount();

    /**
     * Sets the count of occurrences that this occurrence represents.
     *
     * @param count the number of occurrences.
     */
    public void setCount(int count);

} // Occurrence
