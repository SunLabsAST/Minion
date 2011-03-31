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

import com.sun.labs.minion.IndexableMap;
import com.sun.labs.minion.QueryPipeline;
import com.sun.labs.minion.SearchEngineException;
import java.util.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.ParseException;

import com.sun.labs.minion.retrieval.*;
import com.sun.labs.minion.pipeline.TokenCollectorStage;
import com.sun.labs.minion.document.tokenizer.UniversalTokenizer;

import com.sun.labs.minion.Searcher;
import com.sun.labs.minion.pipeline.QueryPipelineImpl;
import com.sun.labs.minion.pipeline.Stage;
import com.sun.labs.minion.pipeline.StemStage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class transforms the output of JavaCC into a tree of
 * query elements that the query evaluator can understand.
 * It's more than meets the eye.
 *
 * @author Jeff Alexander
 */

public class StrictTransformer extends Transformer
{
    public static void main(String args[]) throws Exception {
        String q = ""; // the query
        
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        while ((q = input.readLine()) != null) {
            StrictParser p = new StrictParser(new StringReader(q));
            SimpleNode n = (SimpleNode)p.doParse();
            n.dump(":");
            StrictTransformer t = new StrictTransformer();

            //
            // Create the pipeline, pushing on stages back to front
            LinkedList<Stage> stages = new LinkedList<Stage>();
            TokenCollectorStage tcs = new TokenCollectorStage();
            stages.push(tcs);
            
            StemStage stemStage = new StemStage(stages.peek());
            stages.push(stemStage);
            
            UniversalTokenizer tokStage = new UniversalTokenizer(stages.peek());
            tokStage.noBreakCharacters = "*?";
            stages.push(tokStage);
            
            QueryPipelineImpl pipeline =
                    new QueryPipelineImpl(null, null, stages);
            QueryElement e =
                    t.transformTree(n, Searcher.Operator.PAND, pipeline);
            n.dump("+");
            if (e != null) {
                e.dump("!");
                // and check getQueryTerms implementation
                List l = e.getQueryTerms();
                String names = "";
                Iterator it = l.iterator();
                while (it.hasNext()) {
                    DictTerm dt = (DictTerm)it.next();
                    names = names + dt.toString() + " ";
                }
                System.out.println("terms: " + names);
            }
        }
    }

    protected static Logger logger = Logger.getLogger(ScoredQuickOr.class.getName());


    public StrictTransformer() {
        
    }
    
    /** 
     * Transforms an abstract syntax tree provided by JJTree+JavaCC into
     * a tree of QueryElements that can be used by the query evaluator.
     * 
     * @param root the root node of the tree returned from the Parser
     * @param defaultOperator specified the default operator to use when no
     * other operator is provided between terms in the query.  Valid values are
     * defined in the {@link com.sun.labs.minion.Searcher} interface
     * @return the root node of a tree describing a query
     */
    public QueryElement transformTree(
            SimpleNode root,
            Searcher.Operator defaultOperator,
            QueryPipeline pipeline)
        throws ParseException
    {
        // Perform all the clean-up operations, then create
        // the tree of query elements
        removeClutter(root);
        if (!validateTors(root)) {
            throw new ParseException("Encountered an invalid TOR.  TORs may not contain sub-expressions", 0);
        }
        collapseOrs(root, false);
        collapseAnds(root, false, SimpleNode.INVALID, SimpleNode.INVALID);
        collapsePassages(root, false);

        collapsePhrases(root, pipeline);
        
        // Figure out the root element, then seed the recursive call:
        QueryElement rootQE = null;
        if (root.jjtGetNumChildren() != 0) {
            rootQE = makeQueryElements((SimpleNode)root.jjtGetChild(0), defaultOperator);
        }
        return rootQE;
    }

    /** 
     * This recursive method creates the tree of query elements based
     * on the root node passed in.  The tree passed in should already
     * have been pruned as appropriate by the various methods in this
     * class.
     * 
     * @param node
     * @param defaultOperator specified the default operator to use when no
     * other operator is provided between terms in the query.  Valid values are
     * defined in the {@link com.sun.labs.minion.Searcher} interface
     * @return the top-level query element.
     */
    protected static QueryElement makeQueryElements(SimpleNode node, Searcher.Operator defaultOperator)
        throws ParseException
    {
        ArrayList theKids = new ArrayList();
        int order = 0;
        
        Iterator cit = node.getChildren().iterator();
        while (cit.hasNext()) {
            SimpleNode curr = (SimpleNode)cit.next();
            QueryElement qe = makeQueryElements(curr, defaultOperator);
            if (qe != null) {
                qe.setOrder(order++);
                theKids.add(qe);
            }
        }
        QueryElement res = StrictElementFactory.make(node, theKids, defaultOperator);
        return res;
    }
    

    /** 
     * Determines if the node has any significance or if it is just
     * a passthrough that can be skipped.
     * 
     * @param node the node to check
     * @return true if the node is insignficant (semantically speaking)
     */
    protected static boolean isPassThrough(SimpleNode node) {
        if (node instanceof StrictASTqe ||
            node instanceof StrictASTqp) {
            return true; // qes and qps hold no context
        }
        
        if ((node instanceof StrictASTqeparen) || (node instanceof StrictASTq)) {
            // If there is no operator here, it is a passthrough.
            // Otherwise, there could be a SEQUENCE, WEIGHT, or WITHIN
            // in which case, it would have meaning and can't be skipped
            if (node.jjtGetNumChildren() > 1) {
                //
                // Special case: There may or may not be an operator, but
                // either way, this node is holding multiple children
                // so it is signficant.  Chances are this will get treated
                // as an "undefined" node later.
                return false;
            } else if ((node.operator == SimpleNode.INVALID)
                       && (node.unary == SimpleNode.INVALID)) {
                return true;
            }
        }

        if (node instanceof StrictASTterm) {
            // If there is no unary operator, we're good to go
            if (node.unary == SimpleNode.INVALID) {
                return true;
            }
        }
        return false;
    }


/** 
 * Removes clutter nodes -- the nodes that have only a single
 * child and provide no additional context
 * 
 * @param node the node to clean, pass in the root when calling
 *  externally
 * @return the nodes children if the node is clutter and not the root
 */
    protected static ArrayList removeClutter(SimpleNode node) {
        ArrayList kidsToRemove = new ArrayList();
        ArrayList myNewKids = new ArrayList();
        
        // First deal with the recursive call downwards:
        Iterator cit = node.getChildren().iterator();
        while (cit.hasNext()) {
            SimpleNode curr = (SimpleNode)cit.next();
            // See if current is giving up custody of its children
            ArrayList res = removeClutter(curr);
            if (!res.isEmpty()) {
                myNewKids.addAll(res);
                kidsToRemove.add(curr);
            }
        }

        boolean isPass = isPassThrough(node);

        if (isPass && myNewKids.isEmpty()) {
            return node.getChildren();
        } else if (isPass) {
            // I'm a passthrough, but so is my kid
            return myNewKids;
        } else {
            // I'm not a passthrough, but I may
            // have some new kids
            node.getChildren().addAll(myNewKids);
            node.getChildren().removeAll(kidsToRemove);
            return new ArrayList();
        }
        
    }


    /** 
     * Checks to make sure that any TOR nodes have only plain terms
     * associated with them.
     *
     * @param node the node to check
     * @return true if all TORs are valid
     */
    protected static boolean validateTors(SimpleNode node) {
        boolean isTorNode = false;
        //
        // TOR nodes may only have terms contained in them.
        // (i.e. not terms that are actually sub expressions)
        if (node.operator == StrictParserConstants.TOR) {
            isTorNode = true;
        }

        Iterator cit = node.getChildren().iterator();
        while (cit.hasNext()) {
            SimpleNode curr = (SimpleNode)cit.next();
            if (isTorNode) {
                if (!(curr instanceof StrictASTterm)
                    || curr.value == null) {
                    //
                    // This TOR contains something that
                    // isn't a simple term.  Abort!
                    return false;
                } else {
                    continue;
                }
            }
            if (validateTors(curr) == false) {
                return false;
            }
        }
        return true;
    }
    
    
    /** 
     * Collapse any OR nodes that are just a chain or ORs into
     * a single OR node with many children.  Also checks to make sure
     * that any TOR nodes have only plain terms associated with them.
     * 
     * @param node
     */
    protected static ArrayList collapseOrs(SimpleNode node, boolean passChildrenUp) {
        // Start at the bottom, and pull the ORs up
        ArrayList kidsToRemove = new ArrayList();
        ArrayList myNewKids = new ArrayList();

        boolean isOrNode = false;
        if (node instanceof StrictASTqiOR || node instanceof StrictASTqpOR) {
            if (node.operator != StrictParserConstants.TOR) {
                isOrNode = true;
            }
        }

        // First deal with the recursive call downwards:
        Iterator cit = node.getChildren().iterator();
        while (cit.hasNext()) {
            SimpleNode curr = (SimpleNode)cit.next();

            // See if current is giving up custody of its children
            ArrayList res = collapseOrs(curr, isOrNode);
            if (!res.isEmpty()) {
                myNewKids.addAll(res);
                kidsToRemove.add(curr);
            }
        }

        node.getChildren().addAll(myNewKids);
        node.getChildren().removeAll(kidsToRemove);
        
        if (isOrNode && passChildrenUp) {
            return node.children;
        }
        return new ArrayList();
    }


    /** 
     * Collapse any AND nodes that are just a chain or similar ANDSs into
     * a single AND node with many children.
     * 
     * @param node
     */
    protected static ArrayList collapseAnds(SimpleNode node,
                                            boolean passChildrenUp,
                                            int andType, int nearVal) {
        // Start at the bottom, and pull the ORs up
        ArrayList kidsToRemove = new ArrayList();
        ArrayList myNewKids = new ArrayList();

        boolean isSameAndNode = false;
        if (node instanceof StrictASTqiAND || node instanceof StrictASTqpAND) {
            int nodeVal = (node.value.equals("")? SimpleNode.INVALID : Integer.parseInt(node.value));
            if (((andType == node.operator) && (nearVal == nodeVal)) || (andType == -1)) {
                isSameAndNode = true;
            }
            andType = node.operator;
            nearVal = nodeVal;
        }

        // First deal with the recursive call downwards:
        Iterator cit = node.getChildren().iterator();
        while (cit.hasNext()) {
            SimpleNode curr = (SimpleNode)cit.next();
            // See if current is giving up custody of its children
            ArrayList res = collapseAnds(curr, isSameAndNode, andType, nearVal);
            if (!res.isEmpty()) {
                myNewKids.addAll(res);
                kidsToRemove.add(curr);
            }
        }

        node.getChildren().addAll(myNewKids);
        node.getChildren().removeAll(kidsToRemove);
        
        if (isSameAndNode && passChildrenUp) {
            return node.children;
        }
        return new ArrayList();
    }


        
    /** 
     * Collapse any PASSAGE nodes that have a set of terms in an undefined node
     * down into a single PASSAGE node.
     * 
     * @param node
     */
    protected static ArrayList collapsePassages(SimpleNode node, boolean passChildrenUp) {
        // Start at the bottom, and pull the ORs up
        ArrayList kidsToRemove = new ArrayList();
        ArrayList myNewKids = new ArrayList();

        boolean isPassageNode = false;
        if ((node instanceof StrictASTqpPASS)
            || (node instanceof StrictASTqUND && passChildrenUp)) {
            isPassageNode = true;
        }

        // First deal with the recursive call downwards:
        Iterator cit = node.getChildren().iterator();
        while (cit.hasNext()) {
            SimpleNode curr = (SimpleNode)cit.next();
            // See if current is giving up custody of its children
            ArrayList res = collapsePassages(curr, isPassageNode);
            if (!res.isEmpty()) {
                myNewKids.addAll(res);
                kidsToRemove.add(curr);
            }
        }

        node.getChildren().addAll(myNewKids);
        node.getChildren().removeAll(kidsToRemove);
        
        if ((node instanceof StrictASTqUND || node instanceof StrictASTqpPASS)
            && passChildrenUp) {
            return node.children;
        }
        return new ArrayList();
    }

            
    /** 
     * Creates phrases for terms that would have been tokenized by the
     * tokenizer upon indexing.  Also strips quotes from terms.
     * 
     * This method will traverse to above the leaf Term nodes and see if any
     * of them need to be converted into Phrase nodes with individual
     * terms hanging off of them.
     * 
     * @param node
     */
    protected static void collapsePhrases(
                SimpleNode node,
                QueryPipeline pipeline)
            throws ParseException {
        // Start at the bottom, and pull the ORs up
        ArrayList kidsToRemove = new ArrayList();
        ArrayList myNewKids = new ArrayList();
        
        // First deal with the base case, then the recursive call downwards:
        Iterator cit = node.getChildren().iterator();
        while (cit.hasNext()) {
            SimpleNode curr = (SimpleNode)cit.next();

            if ((curr instanceof StrictASTterm)
                && (!curr.value.equals(""))) {

                // If it is a single-quoted string, don't touch!
                // (but do remove quotes)
                if (isSingleQuoted(curr.value) /*&&
                                                 !(node instanceof StrictASTdoParse)*/) {
                    curr.value = curr.value.substring(1, curr.value.length() - 1);
                    //
                    // If somebody wanted to include a "'" in the string,
                    // they would have escaped it with a backslash.  Get
                    // rid of that if it happened.
                    curr.value = curr.value.replaceAll("\\\\'", "'");

                    continue;
                }

                //
                // Or if this is a field name, don't touch it.
                if (curr.unary == StrictParserConstants.UNDEFINED) {
                    //
                    // Just keep moving.. nothing to see here.
                    continue;
                }

                if (curr.value.indexOf("*") >= 0 || curr.value.indexOf("?") >= 0) {
                    curr.doWild = true;
                }
                
                //
                // This child is a leaf term node so see if it needs splitting
                // into a phrase.  If so, add it to kidsToRemove and put
                // the result phrase in myNewKids
                
                //
                // Start by throwing the term into a dummy document that we'll
                // feed through a pipeline to process the text
                IndexableMap docMap = new IndexableMap("query");
                docMap.put(null, curr.value);
                try {
                    pipeline.index(docMap);
                } catch (SearchEngineException ex) {
                    logger.log(Level.INFO, "Exception in QueryPipeline", ex);
                    throw new ParseException("Failed to tokenize query text",
                            -1);
                }
                pipeline.flush();
                String[] tokens = pipeline.getTokens();
                
                //
                // See what came out the other end othe pipeline
                if (tokens.length > 1) {
                    // We split the string, so put each token
                    // in its own term node and make a phrase
                    // that contains them
                    SimpleNode phrase = new StrictASTqiAND((StrictParser)curr.parser, StrictParserTreeConstants.JJTQIAND);
                    phrase.jjtSetParent(curr);
                    phrase.operator = StrictParserConstants.PHRASE;
                    
                    for (int i = 0; i < tokens.length; i++) {
                        SimpleNode currTerm = (SimpleNode)curr.clone();
                        currTerm.value = tokens[i];
                        phrase.jjtAddChild(currTerm, i);
                    }
                    myNewKids.add(phrase);
                    kidsToRemove.add(curr);
                } else if (tokens.length == 1) {
                    //
                    // This was just one token, but reassign the text anyway
                    // in case the tokenizer removed a leading or trailing
                    // non-word character (like "-5" with or without quotes)
                    curr.value = tokens[0];
                } else if (tokens.length == 0) {
                    //
                    // No valid tokens were found in the string, so all we
                    // got was punctuation.  This should be ignored.
                    kidsToRemove.add(curr);
                }
            } else {
                // Make the recursive call to traverse downwards
                collapsePhrases(curr, pipeline);
            }

        }

        node.getChildren().addAll(myNewKids);
        node.getChildren().removeAll(kidsToRemove);
        
    }


    /* ***** Some utility methods ******/
    

    public static boolean opIsOr(int op) {
        if (op == StrictParserConstants.OR
            || op == StrictParserConstants.SOR
            || op == StrictParserConstants.TOR) {
            return true;
        }
        return false;
    }


    public static boolean opIsAnd(int op) {
        if (op == StrictParserConstants.AND
            || op == StrictParserConstants.SAND
            || op == StrictParserConstants.NEAR
            || op == StrictParserConstants.NEARN
            || op == StrictParserConstants.PHRASE
            || op == StrictParserConstants.WITHIN) {
            return true;
        }
        return false;

    }

    public static int getOp(SimpleNode field)
        throws ParseException
    {
        if (!(field instanceof StrictASTqif)) {
            throw new ParseException("Invalid element type for field operation", 0);
        }        //
        // The operator field (indexed or saved) should
        // always be the second child since JJTree gives
        // us the nodes in order.  If we don't find the
        // operator there, though, we can iterate through
        // the children to find it.
        SimpleNode op = (SimpleNode)field.jjtGetChild(1);
        if (!(op instanceof StrictASTindexed_fieldoperator || op instanceof StrictASTsaved_fieldoperator)) {
            // The 2nd child wasn't a field, so iterate
            System.out.println("need to iterate");
            op = null;
            Iterator it = field.getChildren().iterator();
            while (it.hasNext()) {
                SimpleNode n = (SimpleNode)it.next();
                if (n instanceof StrictASTindexed_fieldoperator || n instanceof StrictASTsaved_fieldoperator) {
                    op = n;
                }
            }
        }

        // If op is null, we couldn't find an operation.  This
        // shouldn't ever happen in a field
        if (op == null) {
            throw new ParseException("Failed to locate operator for field", 0);
        }
        return op.operator;
    }


    public static String getFieldName(SimpleNode field)
        throws ParseException
    {
        if (!(field instanceof StrictASTqif)) {
            throw new ParseException("Invalid element type for field operation", 0);
        }
        //
        // The fieldname field should
        // always be the first child since JJTree gives
        // us the nodes in order.  If we don't find the
        // fieldname there, though, we can iterate through
        // the children to find it.
        SimpleNode name = (SimpleNode)field.jjtGetChild(0);
        if (!(name instanceof StrictASTfieldname)) {
            // The 1st child wasn't a name, so iterate
            System.out.println("need to iterate");
            name = null;
            Iterator it = field.getChildren().iterator();
            while (it.hasNext()) {
                SimpleNode n = (SimpleNode)it.next();
                if (n instanceof StrictASTfieldname) {
                    name = n;
                }
            }
        }

        // If op is null, we couldn't find an operation
        if (name == null) {
            throw new ParseException("Failed to locate field name for field", 0);
        }
        return name.value;
    }

    public static String getFieldValue(SimpleNode field)
        throws ParseException
    {
        if (!(field instanceof StrictASTqif)) {
            throw new ParseException("Invalid element type for field operation", 0);
        }
        //
        // The value field should
        // always be the third child since JJTree gives
        // us the nodes in order.  If we don't find the
        // value there, though, we can iterate through
        // the children to find it.

        //
        // We might have removed the value (or the field name) if
        // the tokenizer didn't find any tokens in it.  Give an
        // empty string back if this is the case.
        if (((SimpleNode)field).jjtGetNumChildren() < 3) {
            return "";
        }
        SimpleNode value = (SimpleNode)field.jjtGetChild(2);
        if (!(value instanceof StrictASTsaved_fieldvalue)) {
            // The 3rd child wasn't a value, so iterate
            System.out.println("need to iterate");
            value = null;
            Iterator it = field.getChildren().iterator();
            while (it.hasNext()) {
                SimpleNode n = (SimpleNode)it.next();
                if (n instanceof StrictASTsaved_fieldvalue) {
                    value = n;
                }
            }
        }

        // If op is null, we couldn't find an operation
        if (value == null) {
            throw new ParseException("Failed to locate value for field", 0);
        }
        return value.value;
    }
}
