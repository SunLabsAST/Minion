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
import com.sun.labs.minion.retrieval.FieldTerm;
import com.sun.labs.minion.retrieval.QueryElement;
import com.sun.labs.minion.util.StringUtil;
import java.io.Serializable;
import java.util.Collection;
import java.util.EnumSet;

/**
 * A class for a simple relational query, like price &lt; 10.  Such queries can
 * be run on any field that has the <code>SAVED</code> attribute.  While this
 * class can be used for simple relations on string fields, {@link StringRelation}
 * provides the full set of relational operators that can be applied to string
 * fields.
 *
 * <p>
 *
 * Note that this API cannot currently check whether the field provided for the
 * relation is a saved field.  This will be checked when the query is evaluated
 * and a warning provided then.
 */
public class Relation extends Element implements Serializable {

    protected String field;

    protected Operator operator;

    protected String value;

    protected boolean caseSensitive;

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

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

    @Override
    public void addField(String field) {
        throw new UnsupportedOperationException("Can't restrict a relation to " +
                field);
    }

    @Override
    public void setFields(Collection<String> fields) {
        throw new UnsupportedOperationException("Can't restrict a relation to " +
                fields);
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

    @Override
    public QueryElement getQueryElement(QueryPipeline pipeline) {
        FieldTerm ret =  new FieldTerm(field, operator, value);
        ret.setMatchCase(caseSensitive);
        return ret;
    }

    @Override
    public String toQueryString() {
        if(value.indexOf(' ') > 0) {
            return String.format("%s %s \"%s\"", field, operator.getRep(), StringUtil.escapeQuotes(value));
            
        }
        return String.format("%s %s %s", field, operator.getRep(), StringUtil.escapeQuotes(value));
    }

    @Override
    public String toString() {
        return String.format("(%s %s %s %s)", operator.getRep(), field, value, caseSensitive);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final Relation other = (Relation) obj;
        if((this.field == null) ? (other.field != null) : !this.field.equals(other.field)) {
            return false;
        }
        if(this.operator != other.operator) {
            return false;
        }
        if((this.value == null) ? (other.value != null) : !this.value.equals(other.value)) {
            return false;
        }
        return true;
    }
    
    
}
