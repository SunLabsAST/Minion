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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A class implementing the <code>if</code> query operator.  This operator
 * takes a single argument and returns a strict boolean results set.
 */
public class If extends UnaryOperator {

    /**
     * Constructs an <code>if</code> operator that will use the given
     * operand .
     */
    public If(QueryElement operand) {
        super(operand);
    } // If constructor

    /**
     * Evaluates this operator, returning the results.
     */
    public ArrayGroup eval(ArrayGroup ag) {
        QueryElement operand = (QueryElement) operands.get(0);
        operand.strictEval = true;
        return operand.eval(ag);
    }
} // If
