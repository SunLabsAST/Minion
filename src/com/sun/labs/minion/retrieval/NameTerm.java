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

package com.sun.labs.minion.retrieval;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A term for a query that is a simple name.  Such a name can be used
 * as the operand for an <code>&lt;undefined&gt;</code> operator.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class NameTerm extends QueryElement {
    
    private String name;
    
    /**
     * Creates a FieldNameTerm
     */
    public NameTerm(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    @Override
    protected int calculateEstimatedSize() {
        return 0;
    }

    @Override
    protected List<QueryTerm> getQueryTerms(Comparator c) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public String toString() {
        return toString("");
    }

    @Override
    public String toString(String prefix) {
        return super.toString(prefix) + " " + getName();
    }

}
