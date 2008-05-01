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

/**
 * A single posting from a document, composed of a term and the frequency
 * of that term in the document.
 *
 * @author Stephen Green <stephen.green@sun.com>
 * @see Document#getPostings
 */
public class Posting implements Serializable {

    /**
     * The term represented by this posting.
     */
    protected String term;

    /**
     * The frequency of the term.
     */
    protected int freq;
    
    public Posting() {
        
    }

    /**
     * Creates a posting for a given term and frequency.
     * @param term the term represented by this posting.  Spaces will be trimmed from the 
     * beginning and end of this term
     * @param freq the frequency of the term in the index.
     */
    public Posting(String term, int freq) {
        this.term = term.trim();
        this.freq = freq;
    }

    /**
     * Gets the term associated with this posting.
     * @return the term associated with this posting
     */
    public String getTerm() {
        return term;
    }
    
    /**
     * Gets the frequency of this term in this document.
     * @return the frequency of this term in this document.
     */
    public int getFreq() {
        return freq;
    }

    /**
     * Generates a string representation of this posting
     * @return a string representation of the posting
     */
    @Override
    public String toString() {
        return String.format("<%s,%d>", term, freq);
    }
}
