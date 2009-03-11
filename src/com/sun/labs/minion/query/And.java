/*
 * Copyright 2007-2009 Sun Microsystems, Inc. All Rights Reserved.
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
package com.sun.labs.minion.query;

import com.sun.labs.minion.retrieval.QueryElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A boolean and operator
 */
public class And extends Operator {

    /**
     * Creates an empty and operator.  This would evaluate to an empty set of
     * documents.
     */
    public And() {
        super();
    }

    /**
     * Creates an and operator that will operate on the provided elements.
     * @param elements the elements that should be anded together.  A document
     * will appear in the result set for this operator only if it appears in the
     * result sets of all of the provided elements.
     */
    public And(Collection<Element> elements) {
        super(elements);
    }

    /**
     * Creates an and operator that will opearate on the provided elements.
     *
     * @param elements the elements that should be anded together.
     */
    public And(Element[] elements) {
        super(elements);
    }

    public QueryElement getQueryElement() {
        List<QueryElement> operands = new ArrayList();
        for(Element e : Operator.this.operands) {
            operands.add(e.getQueryElement());
        }
        return new com.sun.labs.minion.retrieval.And(operands);
    }

}
