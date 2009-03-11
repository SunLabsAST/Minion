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

/**
 * A boolean not operator.  Note that boolean not is a unary operator.
 */
public class Not extends Operator {

    private Element element;

    /**
     * Creates an empty boolean negation.  This will return all documents.
     */
    public Not() {
        super();
    }

    /**
     * Creates a boolean negation of the given element.  A document will be
     * in the result set for this operator only if it is <em>not</em> in the
     * result set for the provided element.
     *
     * <p>
     *
     * A boolean negation is strictly evaluated:  a document is either in the result
     * set or it is not.  Documents in the result set for this operator will not
     * have scores associated with them.
     * @param element
     */
    public Not(Element element) {
        this.element = element;
        operands = new ArrayList<Element>();
        operands.add(element);
    }

    public QueryElement getQueryElement() {
        return new com.sun.labs.minion.retrieval.Not(element.getQueryElement());
    }

}
