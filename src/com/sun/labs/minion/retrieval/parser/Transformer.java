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

package com.sun.labs.minion.retrieval.parser;

import com.sun.labs.minion.QueryPipeline;
import com.sun.labs.minion.Searcher;
import java.text.ParseException;

import com.sun.labs.minion.retrieval.QueryElement;

/**
 * This class transforms the output of JavaCC into a tree of
 * query elements that the query evaluator can understand.
 * It's more than meets the eye.
 *
 * @author Jeff Alexander
 */

public abstract class Transformer
{

    /** 
     * Transforms an abstract syntax tree provided by JJTree+JavaCC into
     * a tree of QueryElements that can be used by the query evaluator.
     * 
     * @param root the root node of the tree returned from the Parser
     * @param defaultOperator specified the default operator to use when no
     * other operator is provided between terms in the query.  Valid values are
     * defined in the {@link com.sun.labs.minion.Searcher} interface
     * @param pipeline a pipeline for transforming free text in the query
     * @return the root node of a tree describing a query
     */
    public abstract QueryElement transformTree(SimpleNode root,
            Searcher.Operator defaultOperator,
            QueryPipeline pipeline)
        throws ParseException;

    /** 
     * Determines if a string is a quoted string.  If it starts and ends with '
     * or starts and ends with " then it is a quoted string.
     * 
     * @param str the string to check
     * @return true if the string is quoted
     */
    public static boolean isQuoted(String str) {
        if (isDoubleQuoted(str) || isSingleQuoted(str)) {
            return true;
        }
        return false;
    }

    /** 
     * Determines if a string is double quoted, that is, it begins and ends
     * with a " character.
     * 
     * @param str the string to check
     * @return true if the string is double quoted
     */
    public static boolean isDoubleQuoted(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return true;
        }
        return false;
    }

    
    /** 
     * Determines if a string is single quoted, that is, it begins and ends
     * with a ' character.
     * 
     * @param str the string to check
     * @return true if the string is single quoted
     */
    public static boolean isSingleQuoted(String str) {
        if (str.startsWith("'") && str.endsWith("'")) {
            return true;
        }
        return false;
    }
}
