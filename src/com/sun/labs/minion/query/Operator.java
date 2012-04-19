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

import com.sun.labs.minion.util.StringUtil;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * An abstract class for a query operator.
 */
public abstract class Operator extends Element implements Iterable<Element>, Serializable {

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
    
    public boolean isEmpty() {
        return elements.isEmpty();
    }
    
    public int size() {
        return elements.size();
    }

    @Override
    public Iterator<Element> iterator() {
        return elements.iterator();
    }

    public Collection<Element> getOperands() {
        return new ArrayList<Element>(elements);
    }
    
    protected static String getInfixOperatorQueryString(String opName, List<Element> elements, Set<String> fields, boolean strict) {
        StringBuilder sb = new StringBuilder();
        if(strict) {
            sb.append("<if> (");
        }
        
        if(fields != null && !fields.isEmpty()) {
            sb.append(StringUtil.toString(",", fields));
            sb.append(" <contains> (");
        }
        
        boolean first = true;
        for(Element element : elements) {
            if(!first) {
                sb.append(' ');
                sb.append(opName);
                sb.append(' ');
            }
            sb.append('(');
            sb.append(element.toQueryString());
            sb.append(')');
            first = false;
        }
        
        if(fields != null && fields.size() > 0) {
            sb.append(')');
        }
        
        if(strict) {
            sb.append(')');
        }

        return sb.toString();
    }
    
    protected static String getPrefixOperatorQueryString(String opName, List<Element> elements, Set<String> fields) {
        StringBuilder sb = new StringBuilder();
        if(fields != null && !fields.isEmpty()) {
            sb.append(StringUtil.toString(",", fields));
            sb.append(" <contains> (");
        }
        sb.append('(');
        sb.append(opName);
        for(Element element : elements) {
            sb.append(' ');
            sb.append(element.toQueryString());
        }
        sb.append(')');
        if(fields != null && !fields.isEmpty()) {
            sb.append(')');
        }
        return sb.toString();
    }
   
}
