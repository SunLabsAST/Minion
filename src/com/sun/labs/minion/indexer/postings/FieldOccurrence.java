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
 * A interface for occurrence data that takes into account the fields and
 * the position where the occurrence was found.
 */
public interface FieldOccurrence extends Occurrence {

    /**
     * Gets the position at which the occurrence was found.
     *
     * @return the position where the occurrence was found.
     */
    public int getPos();

    /**
     * Gets the fields that are active at the time of the occurrence.
     *
     * @return an array that is as long as the number of defined fields.
     * The <em>i<sup>th</sup></em> element of this array indicates the
     * current position in the field whose ID is <em>i</em>.  If element 0
     * of this array is greater than zero, then no fields are currently
     * active.
     */
    public int[] getFields();

} // FieldOccurrence
