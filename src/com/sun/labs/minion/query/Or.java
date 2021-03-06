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

import com.sun.labs.minion.QueryPipeline;
import com.sun.labs.minion.retrieval.QueryElement;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A boolean or operator.
 */
public class Or extends Operator implements Serializable {

    public Or() {
        super();
    }

    public Or(Collection<Element> elements) {
        super(elements);
    }

    public Or(Element... elements) {
        super(elements);
    }
    
    public QueryElement getQueryElement(QueryPipeline pipeline) {
        List<QueryElement> operands = new ArrayList();
        for(Element e : elements) {
            operands.add(e.getQueryElement(pipeline));
        }
        return new com.sun.labs.minion.retrieval.Or(operands);
    }

    @Override
    public String toQueryString() {
        return Operator.getInfixOperatorQueryString("<or>", elements, fields, strict);
    }

    public String toString() {
        return "(Or " + strict + " " + elements + ")";
    }

}
