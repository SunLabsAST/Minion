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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * An abstract class for a query operator.
 */
public abstract class Operator extends Element implements Iterable<Element> {

    /**
     * An operator has a list of operands, which are elements, either terms or
     * other operators.
     */
    protected List<Element> elements;

    /**
     * Creates an operator with an empty list of operands.
     *
     * @see #add(com.sun.labs.minion.query.Element)
     */
    public Operator() {
        elements = new ArrayList<Element>();
    }

    /**
     * Creates an operator with the provided operands.
     *
     * @param elements the operands for the operator.
     */
    public Operator(Element... elements) {
        this.elements = new ArrayList<Element>();
        for(Element e : elements) {
            this.elements.add(e);
        }
    }

    /**
     * Creates an operator with the given list of operands.  A shallow copy of
     * the operand list is taken.
     * @param elements
     */
    public Operator(Collection<Element> elements) {
        this.elements = new ArrayList<Element>(elements);
    }

    public Operator add(Element element) {
        elements.add(element);
        return this;
    }

    public Iterator<Element> iterator() {
        return elements.iterator();
    }

    public Collection<Element> getOperands() {
        return new ArrayList<Element>(elements);
    }
}
