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

package com.sun.labs.minion.test.regression;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * A tuple consists of a set of documents that are related to each other 
 * because of certain conditions regarding the entrys they have. For example,
 * they all contain the same entry. Every tuple has a tuple relation.
 * For example, if the tuple relation is "contains the words 'cat' and
 * 'dog' or the word 'apple'", then the tuple will consists of all the
 * documents that contain the words 'cat' and 'dog' or the word 'apple'.
 * A tuple is initially generated by a {@link TupleFactory TupleFactory}.
 * {@link TupleFilter TupleFilters} take two tuples, and creates a tuple
 * out of them containing documents satisfying the relation specified by
 * the tuple.
 */
public class Tuple {

    private TupleRelation relation;
    private Set documents;
    
    /**
     * Constructs a Tuple with the given list of documents and tuple relation.
     *
     * @param documents the list of documents
     * @param relation the tuple relation
     */
    public Tuple(Collection documents, TupleRelation relation) {
        this.relation = relation;
        documents = new HashSet();
        documents.addAll(documents);
    }

    /**
     * Returns an iterator for the documents of this tuple.
     *
     * @return an iterator for the documents of this tuple
     */
    public Iterator getDocumentIterator() {
        return documents.iterator();
    }

    /**
     * Returns true if this Tuple contains the given document.
     *
     * @param documentName the name of the document
     *
     * @return true if this Tuple contains the document, false otherwise
     */
    public boolean contains(String documentName) {
        return documents.contains(documentName);
    }

    /**
     * Returns the relation of this tuple.
     *
     * @return the relation of this tuple
     */
    public TupleRelation getRelation() {
        return relation;
    }

    /**
     * Returns the number of documents in the tuple.
     *
     * @return the number of documents in the tuple
     */
    public int getSize() {
        return documents.size();
    }
}
