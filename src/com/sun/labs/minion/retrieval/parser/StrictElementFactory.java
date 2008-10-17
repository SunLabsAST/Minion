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

public class StrictElementFactory
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
            case StrictParserTreeConstants.JJTQIAND:
            case StrictParserTreeConstants.JJTQPAND:
                return makeAnd(node, children);
            case StrictParserTreeConstants.JJTQIOR:
            case StrictParserTreeConstants.JJTQPOR:
                return makeOr(node, children);
            case StrictParserTreeConstants.JJTTERM:
                return makeTerm(node, children);
            case StrictParserTreeConstants.JJTQUND:
                return makeUndefined(node, children, defaultOperator);
            case StrictParserTreeConstants.JJTQ:
            case StrictParserTreeConstants.JJTQEPAREN:
                if (node.jjtGetNumChildren() > 1) {
                    return makeUndefined(node, children, defaultOperator);
                } else {
                    return makePrefix(node, children);
                }
            case StrictParserTreeConstants.JJTQPPASS:
                return makePassage(node, children);
            case StrictParserTreeConstants.JJTQIF:
                return makeField(node, children);
            case StrictParserTreeConstants.JJTFIELDNAME:
            case StrictParserTreeConstants.JJTSAVED_FIELDVALUE:
            case StrictParserTreeConstants.JJTSAVED_FIELDOPERATOR:
            case StrictParserTreeConstants.JJTINDEXED_FIELDOPERATOR:
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
            case StrictParserConstants.AND:
            case StrictParserConstants.SAND:
                return new And(children);
            case StrictParserConstants.NEAR:
                return new Near(children);
            case StrictParserConstants.NEARN:
                return new Near(children, Integer.parseInt(node.value));
            case StrictParserConstants.PAND:
                return new PAnd(children);
            case StrictParserConstants.PHRASE:
                return new Phrase(children);
            case StrictParserConstants.WITHIN:
                return new Within(children, Integer.parseInt(node.value));
            default:
                throw new ParseException("Unknown type of AND encountered", 0);
        }
    }

    private static QueryElement makeOr(SimpleNode node, ArrayList children)
        throws ParseException
    {
        switch (node.operator) {
            case StrictParserConstants.OR:
            case StrictParserConstants.SOR:
                return new Or(children);
            case StrictParserConstants.TOR:
                return makeTor(node, children);
            default:
                throw new ParseException("Unknown type of OR encountered", 0);
        }
    }

    private static QueryElement makeTor(SimpleNode node, ArrayList children)
        throws ParseException
    {
        //
        // Make a MultiDictTerm with all of the children's values as its value.
        // children should contain all DictTerms
        return new MultiDictTerm(children);
    }
    
    private static QueryElement makeTerm(SimpleNode node, ArrayList children)
        throws ParseException
    {
        // If this term really holds a term, set the modifiers for it
        DictTerm theTerm = null;
        QueryElement theArg;
        if (!node.value.equals("")) {
            // This term has a real term in it, it isn't just a holder for
            // some sub-expression
            if (node.doExact &&
                       (node.doMorph || node.doStem || node.doExpand)) {
                throw new ParseException("Incompatible modifiers: <EXACT> may not be used with <MORPH>, <STEM>, or <EXPAND>. (Term: " + node.value + ")", 0);
            }
            theTerm = new DictTerm(node.value);
            setMods(theTerm, node);
            theArg = theTerm;
        } else {
            // This term is just a holder for some sub expression
            theArg = (QueryElement)children.get(0);
        }
        
        
        if (node.unary == SimpleNode.INVALID) {
            return theTerm;
        }
        else if (node.unary == StrictParserConstants.NOT) {
            return new Not(theArg);
        }
        else if (node.unary == StrictParserConstants.HIDE) {
            return new Hide(theArg);
        }
        else if (node.unary == StrictParserConstants.IF) {
            return new If(theArg);
        }
        else if (node.unary == StrictParserConstants.UNDEFINED) {
            if (!node.value.equals("")) {
                return new Undefined(new NameTerm(node.value));
            } else {
                //
                // There was no string value associated with
                // this term, so it must actually be a sub-expression.
                // This isn't allowed.
                throw new ParseException("Undefined can only be applied to a field name", 0);
            }
        }
        
        return null;
    }

    public static void setMods(DictTerm theTerm, SimpleNode node) {
        theTerm.setMatchCase(node.matchCase);
        //    theTerm.setDoExact(node.doExact); ??
        theTerm.setDoMorph(node.doMorph);
        theTerm.setDoStem(node.doStem);
        theTerm.setDoWild(node.doWild);
        theTerm.setDoExpand(node.doExpand);
    }
    
    /** 
     * Creates nodes for operators that were encoded along with a paren.
     * These are the prefix operators: SEQUENCE, WEIGHT
     * And these are the unary ops: NOT, HIDE, IF
     * @param node
     * @param children
     * @return 
     */
    private static QueryElement makePrefix(SimpleNode node, ArrayList children)
        throws ParseException
    {
        QueryElement res = null;
        switch (node.operator) {
            case StrictParserConstants.SEQUENCE:
                res = new Sequence((QueryElement)children.get(0));
                break;
            case StrictParserConstants.WEIGHT:
                res = new Weight((QueryElement)children.get(0),
                                 Float.valueOf(node.value));
                break;
        }

        //
        // If there was no prefix op, don't try to use it when
        // checking for a unary op.  If there was, do use it.
        QueryElement inUnary = res;
        if (res == null) {
            inUnary = (QueryElement)children.get(0);
        }
        switch (node.unary) {
            case StrictParserConstants.NOT:
                res = new Not(inUnary);
                break;
            case  StrictParserConstants.HIDE:
                res = new Hide(inUnary);
                break;
            case StrictParserConstants.IF:
                res = new If(inUnary);
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


    private static QueryElement makePassage(SimpleNode node, ArrayList children)
        throws ParseException
    {
        if (node.operator == StrictParserConstants.PASSAGE) {
            return new Passage(children);
        } else {
            throw new ParseException("Unknown type of PASSAGE encountered", 0);
        }
    }

/** 
 * This one is a little tricky since there are two types of field queries.
 * 
 * For indexed field queries, the field node doesn't actually exist -- they
 * just consist of field names set on their children.  Since they always
 * have just a single child, that child will be returned from the method
 * with the appropriate values set.
 * 
 * For saved field queries, we create a field term and dig the operator
 * and operands out of the children of the field node (for which there
 * were no query elements created).
 * 
 * @param node
 * @param children
 * @return 
 */
    private static QueryElement makeField(SimpleNode node, ArrayList children)
        throws ParseException
    {
        int theOp = StrictTransformer.getOp(node);
        QueryElement result;
        
        if (theOp == StrictParserConstants.CONTAINS) {
            // Extract the field name and set it on what should be the only
            // child since this is an indexed field.  If it has no children,
            // don't make a field.
            if (children.size() == 0) {
                return null;
            }
            else if (children.size() != 1) {
                throw new ParseException("Encountered field with multiple values " + children.size(), 0);
            }
            QueryElement kid = (QueryElement)children.get(0);
            kid.addSearchFieldName(StrictTransformer.getFieldName(node));
            result = kid;
        } else {
            // This is a saved field, grab the operands
            String name = StrictTransformer.getFieldName(node);
            String value = StrictTransformer.getFieldValue(node);
            if (StrictTransformer.isQuoted(value)) {
                value = value.substring(1, value.length() - 1);
            }
            FieldTerm.Operator opcode;
            switch (theOp) {
                case StrictParserConstants.STARTS:
                    opcode = FieldTerm.Operator.STARTS;
                    break;
                case StrictParserConstants.ENDS:
                    opcode = FieldTerm.Operator.ENDS;
                    break;
                case StrictParserConstants.MATCHES:
                    opcode = FieldTerm.Operator.MATCHES;
                    break;
                case StrictParserConstants.SIMILAR:
                    opcode = FieldTerm.Operator.SIMILAR;
                    break;
                case StrictParserConstants.SUBSTRING:
                    opcode = FieldTerm.Operator.SUBSTRING;
                    break;
                case StrictParserConstants.LESS:
                case StrictParserConstants.LT:
                    opcode = FieldTerm.Operator.LESS;
                    break;
                case StrictParserConstants.EQUALS:
                    opcode = FieldTerm.Operator.EQUAL;
                    break;
                case StrictParserConstants.NOTEQUAL:
                    // For !=, we'll make a regular equals field,
                    // but below, we'll wrap it in a Not node
                    opcode = FieldTerm.Operator.EQUAL;
                    break;
                case StrictParserConstants.GREATER:
                case StrictParserConstants.GT:
                    opcode = FieldTerm.Operator.GREATER;
                    break;
                case StrictParserConstants.LEQ:
                    opcode = FieldTerm.Operator.LEQ;
                    break;
                case StrictParserConstants.GEQ:
                    opcode = FieldTerm.Operator.GEQ;
                    break;
                default:
                    throw new ParseException("Unknown field operator", 0);
            }
            result = new FieldTerm(name, opcode, value);
            if (theOp == StrictParserConstants.NOTEQUAL) {
                result = new Not(result);
            }
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
                node.operator = StrictParserConstants.AND;
                res = makeAnd(node, children);
                break;
            case Searcher.OP_PAND:
                node.operator = StrictParserConstants.PAND;
                res = makeAnd(node, children);
                break;
            case Searcher.OP_OR:
                node.operator = StrictParserConstants.OR;
                res = makeOr(node, children);
                break;
            default:
                throw new ParseException("No known default operator defined", 0);
        }

        if (node.unary == StrictParserConstants.NOT) {
            res = new Not(res);
        }
        else if (node.unary == StrictParserConstants.HIDE) {
            res = new Hide(res);
        }
        else if (node.unary == StrictParserConstants.IF) {
            res = new If(res);
        }

        return res;
    }
    
}
