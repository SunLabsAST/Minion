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
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * An abstract class for an element in a query:  the elements of a query are
 * terms, operators, relations and ranges.
 */
public abstract class Element {

    /**
     * Whether this element should be interpreted in a strict boolean fashion.
     */
    protected boolean strict;

    /**
     * The fields to which this element should be restricted during search.
     */
    protected Set<String> fields;

    /**
     * Indicates whether this element should be evaluated in a strict boolean
     * context.
     *
     * @param strict if <code>true</code> if this element should be evaluated in a strict
     * bolean context
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    /**
     * Gets the strictness of this element.
     * @return <code>true</code> if this element should be evaluated in a strict
     * bolean context, <code>false</code> otherwise.
     */
    public boolean getStrict() {
        return strict;
    }

    public void addField(String field) {
        if(fields == null) {
            fields = new HashSet<String>();
        }
        fields.add(field);
    }

    public void setFields(Collection<String> fields) {
        this.fields = new HashSet<String>(fields);
    }

    public Set<String> getFields() {
        if(fields == null) {
            return null;
        }
        return new HashSet<String>(fields);
    }

    /**
     * Transduces this query element into a "real" query element, one that can
     * be evaluated.
     * @return an evaluatable query element.
     */
    public abstract QueryElement getQueryElement();

}
