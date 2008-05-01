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

import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.Entry;

import com.sun.labs.minion.indexer.postings.Postings;

import com.sun.labs.minion.util.buffer.ReadableBuffer;

/**
 * An entry for the doc dictionary in the cluster partition.
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.2 $
 */

public class ClusterEntry extends DocKeyEntry
{

    public ClusterEntry() {
        super();
    }

    public ClusterEntry(Object name) {
        super(name);
    }

    public Entry getEntry(Object name) {
        return new ClusterEntry(name);
    }

    /**
     * Gets the appropriate postings type for the class.  These postings
     * should be useable for indexing.
     *
     * @return A set of ID and frequency postings.
     */
    public Postings getPostings() {
        return new ClusterPostings();
    }

    /**
     * Gets a set of postings useful at query time.
     *
     * @param input The buffer containing the postings read from the
     * postings file.
     * @return A set of ID and frequency postings.
     */
    protected Postings getPostings(ReadableBuffer input) {
        return new ClusterPostings(input);
    }

}
