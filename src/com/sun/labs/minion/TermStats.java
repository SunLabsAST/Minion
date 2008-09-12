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

/**
 * An interface to some standard term statistics.
 *
 * @see SearchEngine.getTermStats
 */
public interface TermStats {

    /**
     * Gets the number of documents in which the term occurs.
     *
     * @return the number of documents in the collection that contain the term.
     */
    int getDocFreq();

    /**
     * Gets the maximum term frequency for the term in this collection.
     * @return the maximum term frequency.
     */
    int getMaxFDT();

    /**
     * Gets the name of the term whose stats we're holding.
     */
    String getName();

    /**
     * Gets the total number of occurrences of the term in the collection.
     *
     * @return the total number of occurrences of the term in all documents
     * in the collection.
     */
    int getTotalOccurrences();

}
