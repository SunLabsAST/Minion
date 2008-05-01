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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * Applies the AND filter function to two input tuples. This means
 * that the resulting tuples will only contain documents that both tuples
 * have. For example, if tuple A has documents containing the word "apple",
 * and tuple B has documents containing the word "boy", then applying
 * this AND filter to these two tuples will create a tuple that has
 * documents containing both of the words "apple" and "boy". Effectively,
 * it is returning the intersection of the two tuples.
 */
public class ANDTupleFilter implements TupleFilter {

    /**
     * Applies the AND filter function to the two input tuples.
     * The resulting tuple will only contain documents that appear in
     * both the input tuples.
     *
     * @param tuple1 the first tuple
     * @param tuple2 the second tuple
     *
     * @return the resulting tuple
     */
    public Tuple filter(Tuple tuple1, Tuple tuple2) {

        /* figure out which documents appear in both tuples */
        List documents = new LinkedList();
        for (Iterator i = tuple1.getDocumentIterator(); i.hasNext(); ) {
            String documentName = (String) i.next();
            if (tuple2.contains(documentName)) {
                documents.add(documentName);
            }
        }

        /* creates the AND TupleRelation */
        TupleRelation andRelation = new TupleRelation("<and>");
        andRelation.setLeft(tuple1.getRelation());
        andRelation.setRight(tuple2.getRelation());

        return (new Tuple(documents, andRelation));
    }
}
