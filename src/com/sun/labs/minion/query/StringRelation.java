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

import java.util.EnumSet;

/**
 * A query element for the string operators, which includes all of the valid
 * operators from <code>Relation</code>, as well as <code>substring</code>,
 * <code>matches</code>, <code>starts</code>, <code>ends</code>.
 *
 * @see Relation
 */
public class StringRelation extends Relation {

    /**
     * Creates a relation for a string field.
     * @param field the field that the relation should operate on.  This should be
     * a field that has the <code>SAVED</code> attribute and is of type <code>STRING</code>.
     * If either of these preconditions is violated, a warning will be issued when
     * the query is evaluated by the engine.
     * @param operator the operator to use for the relation
     * @param value the value that is being compared to the values in the
     * field
     */
    public StringRelation(String field, Operator operator, String value) {
        if(!operator.isStringValid()) {
            throw new IllegalArgumentException(operator +
                    " is not a valid StringRelation operator");
        }
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

}
