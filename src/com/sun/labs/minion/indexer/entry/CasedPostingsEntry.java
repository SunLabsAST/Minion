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

import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.Occurrence;

/**
 * An interface that can be implemented by entries that store postings for
 * case sensitive and case insensitive terms.  This can be used, for
 * example, when building bigram dictionaries.
 */
public interface CasedPostingsEntry {

    /**
     * Sets the case insensitive entry for this entry.
     * @param e the case insensitive entry associated with this entry
     */
    public void setCaseInsensitiveEntry(IndexEntry e);

    /**
     * Gets the case insensitive entry for this entry.
     * @return the case insensitive entry for this entry
     */
    public Entry getCaseInsensitiveEntry();

    /**
     * Adds an occurrence to our case sensitive postings.
     * @param o the occurrence to add to our postings
     */
    public void addCaseSensitive(Occurrence o);

    /**
     * Adds an occurrence to our case insensitive postings.
     * @param o the occurrence which we're adding to our postings
     */
    public void addCaseInsensitive(Occurrence o);

    /**
     * Gets the postings that are case sensitive.
     * @return the postings associated with the case sensitive entry
     */
    public Postings getCaseSensitivePostings();

    /**
     * Gets the postings that are case insenstive.
     * @return the postings associated with the case insensitive entry
     */
    public Postings getCaseInsensitivePostings();
    
    /**
     * Tells us whether the name for this entry occurred in the indexed material.
     * The name of an entry may not have occcurred if, for example, only the
     * cased variant of a term occurs in the indexed material.
     * @return <code>true</code> if the name of this entry actually occurred in
     * the indexed material, <code>false<code> otherwise.
     */
    public boolean nameOccurred();
    
}// CasedPostingsEntry
