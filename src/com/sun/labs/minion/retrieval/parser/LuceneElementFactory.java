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

import java.util.*;
import java.text.ParseException;

import com.sun.labs.minion.retrieval.*;

import com.sun.labs.minion.Searcher;

/**
 * Creates QueryElements based on SimpleNodes generated from the parser
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.3 $
 */

public class LuceneElementFactory
{

    /** 
     * Makes a query element for the provided token node.  The tree of
     * query elements is built bottom up, so an array of child query
     * element nodes must be passed in.
     * 
     * @param node the node to turn into a query element
     * @param children an array list of QueryElement objects that
     *                 represent the children of the current node.
     * @param defaultOperator specified the default operator to use when no
     * other operator is provided between terms in the query.  Valid values are
     * defined in the {@link com.sun.labs.minion.Searcher} interface
     * @return the query element for this node.
     */
    public static QueryElement make(SimpleNode node, ArrayList children,
                                    int defaultOperator)
        throws ParseException
    {
        switch (node.id) {
            case LuceneParserTreeConstants.JJTAND:
                return makeAnd(node, children);
            case LuceneParserTreeConstants.JJTOR:
                return makeOr(node, children);
            case LuceneParserTreeConstants.JJTTERM:
                return makeTerm(node, children);
            case LuceneParserTreeConstants.JJTUND:
            case LuceneParserTreeConstants.JJTDOPARSE:
                return makeUndefined(node, children, defaultOperator);
            case LuceneParserTreeConstants.JJTQ:
                if (node.jjtGetNumChildren() > 1) {
                    return makeUndefined(node, children, defaultOperator);
                } else {
                    return makeModifier(node, children);
                }
            case LuceneParserTreeConstants.JJTFIELD:
                return makeField(node, children);
            case LuceneParserTreeConstants.JJTRANGE:
                // We don't make nodes for these -- they'll
                // be gathered up by the makeField method, etc
                return null;
            default:
                break;
        }
        return null;
    }


    private static QueryElement makeAnd(SimpleNode node, ArrayList children)
        throws ParseException
    {
        switch (node.operator) {
            case LuceneParserConstants.AND:
                return new And(children);
            case LuceneParserConstants.PHRASE:
                if (node.unary == LuceneParserConstants.NOT) {
                    return new Not(new Phrase(children));
                } else {
                    return new Phrase(children);
                }
            case LuceneParserConstants.PAND:
                return new PAnd(children);
            default:
                throw new ParseException("Unknown type of AND encountered", 0);
        }
    }

    private static QueryElement makeOr(SimpleNode node, ArrayList children)
        throws ParseException
    {
        switch (node.operator) {
            case LuceneParserConstants.OR:
                return new Or(children);
            default:
                throw new ParseException("Unknown type of OR encountered", 0);
        }
    }
    
    private static QueryElement makeTerm(SimpleNode node, ArrayList children)
        throws ParseException
    {
        // Make the term and any modifiers
        DictTerm theTerm = new DictTerm(node.value);
        setMods(theTerm, node);
        
        
        if (node.unary == SimpleNode.INVALID) {
            return theTerm;
        }
        else if (node.unary == LuceneParserConstants.NOT) {
            return new Not(theTerm);
        }
        
        return null;
    }

    public static void setMods(DictTerm theTerm, SimpleNode node) {
        //
        // Not all of these are currently used, but do no harm being
        // here.  Only morph is currently supported by the ~ operator
        theTerm.setMatchCase(node.matchCase);
        theTerm.setDoMorph(node.doMorph);
        theTerm.setDoStem(node.doStem);
        theTerm.setDoWild(node.doWild);
        theTerm.setDoExpand(node.doExpand);
    }
    
    /** 
     * Creates operators for some of the added markups that can be performed
     * on terms of phrases such as ^ and ~
     *
     * @param node the node defining the modifier
     * @param children the children of the node
     * @return the query element for the operator
     */
    private static QueryElement makeModifier(SimpleNode node, ArrayList children)
        throws ParseException
    {
        QueryElement res = null;
        switch (node.operator) {
            case LuceneParserConstants.PROX:
                res = new Near(children, Integer.valueOf(node.value));
                break;
            case LuceneParserConstants.BOOST:
                res = new Weight((QueryElement)children.get(0),
                                 Float.valueOf(node.value));
                break;
        }

        //
        // If we still don't have a result, we shouldn't have made a
        // prefix operator here.
        if (res == null) {
            throw new ParseException("Unknown modifier for sub-expression encountered", 0);
        }
        return res;
    }



    /** 
     * Define a field, possibly including a range element from the node.
     * A field will either have a child describing the value to search for,
     * or no child if the field is a range (the range should be determined
     * by examining the node passed in's child.
     * 
     * @param node the field not, possibly with a range child
     * @param children the child(ren) of this field for non-range queries
     * @return the field element
     */
    private static QueryElement makeField(SimpleNode node, ArrayList children)
        throws ParseException
    {

        QueryElement result;

        if (!children.isEmpty()) {
            //
            // This is a regular field term, add the field to the child
            // of this node (and likely any of its childrenn).
            if (children.size() == 0) {
                return null;
            }
            else if (children.size() != 1) {
                throw new ParseException("Encountered field with multiple values " + children.size(), 0);
            }
            QueryElement kid = (QueryElement)children.get(0);
            kid.addSearchFieldName(node.value);
            result = kid;
        } else {
            //
            // This is a range field operator, so dig in to get the details.
            if (node.jjtGetNumChildren() != 1) {
                throw new ParseException("Expected a single range definition but found " + node.jjtGetNumChildren() + " child node(s)", 0);
            }
            SimpleNode range = (SimpleNode)node.jjtGetChild(0);

            String fieldName = node.value;
            String lower = range.lower;
            if (Transformer.isQuoted(lower)) {
                lower = lower.substring(1, lower.length() - 1);
            }
            String upper = range.upper;
            if (Transformer.isQuoted(upper)) {
                upper = upper.substring(1, upper.length() - 1);
            }
            
            result = new FieldTerm(fieldName,
                                   lower, range.incLower,
                                   upper, range.incUpper);
        }
        return result;
    }
    
    private static QueryElement makeUndefined(SimpleNode node, ArrayList children,
                                              int defaultOperator)
        throws ParseException
    {
        QueryElement res = null;
        switch (defaultOperator) {
            case Searcher.OP_AND:
                node.operator = LuceneParserConstants.AND;
                res = makeAnd(node, children);
                break;
            case Searcher.OP_PAND:
                node.operator = LuceneParserConstants.PAND;
                res = makeAnd(node, children);
                break;
            case Searcher.OP_OR:
                node.operator = LuceneParserConstants.OR;
                res = makeOr(node, children);
                break;
            default:
                throw new ParseException("No known default operator defined", 0);
        }

        return res;
    }
    
}
