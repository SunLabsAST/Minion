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

import com.sun.labs.minion.retrieval.FieldTerm;
import com.sun.labs.minion.retrieval.QueryElement;
import java.util.EnumSet;

/**
 * A class for a simple relational query, like price &lt; 10.  Such queries can
 * be run on any field that has the <code>SAVED</code> attribute.  While this
 * class can be used for simple relations on string fields, {@link StringRelation}
 * provides the full set of relational operators that can be applied to string
 * fields.
 */
public class Relation extends Element {

    protected String field;

    protected Operator operator;

    protected String value;

    /**
     * The saved field operators.
     */
    public enum Operator {

        EQUALS("="),
        LESS_THAN("<"),
        GREATER_THAN(">"),
        LEQ("<="),
        GEQ(">="),
        NOT_EQUAL("!="),
        MATCHES("<matches>"),
        SUBSTRING("<substring>"),
        STARTS("<starts>"),
        ENDS("<ends>"),
        SIMILAR("<similar>"),
        RANGE;

        private String rep;

        Operator() {

        }
        
        Operator(String rep) {
            this.rep = rep;
        }

        public String getRep() {
            return rep;
        }

        public boolean isValid() {
            return valid.contains(this);
        }

        public boolean isStringValid() {
            return isValid() || stringValid.contains(this);
        }

        public boolean isRangeValid() {
            return rangeValid.contains(this);
        }
        
        /**
         * The set of operators that are valid for any relational query.
         */
        protected static EnumSet<Operator> valid =
                EnumSet.of(Operator.EQUALS,
                Operator.NOT_EQUAL,
                Operator.GEQ,
                Operator.GREATER_THAN,
                Operator.LEQ,
                Operator.LESS_THAN);

        /**
         * The set of operators that are valid for range queries.
         */
        protected static EnumSet<Operator> rangeValid =
                EnumSet.of(Operator.GEQ, Operator.GREATER_THAN,
            Operator.LEQ, Operator.LESS_THAN);

        /**
         * The set of operators that are valid only for string queries.
         */
        private static EnumSet<Operator> stringValid =
                EnumSet.of(Operator.ENDS,
                Operator.MATCHES,
                Operator.SIMILAR,
                Operator.STARTS,
                Operator.SUBSTRING);
    }

    protected Relation() {
        
    }

    public Relation(String field, Operator operator, String value) {
        if(!operator.isValid()) {
            throw new IllegalArgumentException(operator + " is not a valid operator for a simple relation");
        }
        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public Operator getOperator() {
        return operator;
    }

    public String getValue() {
        return value;
    }

    public QueryElement getQueryElement() {
        return new FieldTerm(field, operator, value);
    }

    public String toString() {
        return field + " " + operator.getRep() + " " + value;
    }
}
