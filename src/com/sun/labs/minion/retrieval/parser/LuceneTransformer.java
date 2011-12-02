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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;

import java.text.ParseException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.labs.minion.document.tokenizer.UniversalTokenizer;

import com.sun.labs.minion.pipeline.TokenCollectorStage;

import com.sun.labs.minion.retrieval.*;

import com.sun.labs.minion.Searcher;
import com.sun.labs.minion.pipeline.QueryPipelineImpl;
import com.sun.labs.minion.pipeline.Stage;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class transforms the output of a JavaCC/JJTree tree into a tree
 * of query elements that the query evaluator can understand.
 
 * @author Jeff Alexander
 */

public class LuceneTransformer extends Transformer
{
    protected static final Logger logger =
            Logger.getLogger(LuceneTransformer.class.getName());
    
    public static void main(String args[]) throws Exception {
        String q = ""; // the query
        BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
        while ((q = input.readLine()) != null) {
            LuceneParser p = new LuceneParser(new StringReader(q));
            SimpleNode n = (SimpleNode)p.doParse();
            n.dump(":");
            LuceneTransformer t = new LuceneTransformer();

            //
            // Create the pipeline, pushing on stages back to front
            LinkedList<Stage> stages = new LinkedList<Stage>();
            TokenCollectorStage tcs = new TokenCollectorStage();
            stages.push(tcs);
            
            UniversalTokenizer tokStage = new UniversalTokenizer(stages.peek());
            tokStage.noBreakCharacters = "*?";
            stages.push(tokStage);
            
            QueryPipelineImpl pipeline = new QueryPipelineImpl(null, null, stages);
            QueryElement e = t.transformTree(n, Searcher.Operator.PAND, pipeline);
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


    public LuceneTransformer() {

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
        collapseOrs(root, false);
        collapseAnds(root, false);
        handleAttributes(root);
        
        collapsePhrases(root, pipeline);
        
        // Figure out the root element, then seed the recursive call:
        QueryElement rootQE = null;
        if (root.jjtGetNumChildren() == 1) {
            rootQE = makeQueryElements((SimpleNode)root.jjtGetChild(0), defaultOperator);
        } else if (root.jjtGetNumChildren() > 1) {
            rootQE = makeQueryElements(root, defaultOperator);
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
    protected static QueryElement makeQueryElements(SimpleNode node,
                                                    Searcher.Operator defaultOperator)
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
        QueryElement res = LuceneElementFactory.make(node,
                                                     theKids,
                                                     defaultOperator);
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
        if (node instanceof LuceneASTgroup) {
            // A group is always a pass-through (containing a q)
            return true;
        }
        
        if (node instanceof LuceneASTq) {
            // If there is no operator here, it is a passthrough.
            if (node.jjtGetNumChildren() > 1) {
                //
                // This node is holding multiple children so it is
                // signficant.  Chances are this will get treated
                // as an "undefined" node later.
                return false;
            } else {
                return true;
            }
        }

        if (node instanceof LuceneASTterm) {
            // If there is no value or unary, this term is empty and a passthrough
            if (node.value.equals("") && (node.unary == SimpleNode.INVALID)) {
                return true;
            }
        }
        return false;
    }


    /** 
     * Removes clutter nodes -- the nodes that have only a single
     * child and provide no additional context.  The return parameter
     * is used internally by the recursive call.  If a node determines
     * itself to be clutter, it returns its children in the ArrayList.
     * 
     * @param node the node to clean: pass in the root when calling
     *  externally
     * @return the node's children if the node is clutter (and not the root)
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
            // I'm a passthrough, but so was my kid
            return myNewKids;
        } else {
            // I'm not a passthrough, but I may
            // have some new kids
            node.getChildren().addAll(myNewKids);
            node.getChildren().removeAll(kidsToRemove);
            // I'm not giving any of my kids up for adoption,
            // so return an empty list of orphans
            return new ArrayList();
        }
        
    }


    
    
    /** 
     * Collapse any OR nodes that are just a chain of ORs into
     * a single OR node with many children.
     * 
     * @param node
     */
    protected static ArrayList collapseOrs(SimpleNode node,
                                           boolean parentIsOr) {
        // Start at the bottom, and pull the ORs up
        ArrayList kidsToRemove = new ArrayList();
        ArrayList myNewKids = new ArrayList();

        boolean isOrNode = false;
        if (node instanceof LuceneASTOR) {
            isOrNode = true;
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
        
        if (isOrNode && parentIsOr) {
            return node.children;
        }
        return new ArrayList();
    }


    /** 
     * Collapse any AND nodes that are just a chain of ANDSs into
     * a single AND node with many children.
     * 
     * @param node
     */
    protected static ArrayList collapseAnds(SimpleNode node,
                                            boolean parentIsAnd) {
        // Start at the bottom, and pull the ANDs up
        ArrayList kidsToRemove = new ArrayList();
        ArrayList myNewKids = new ArrayList();

        boolean isAndNode = false;
        if (node instanceof LuceneASTAND) {
            isAndNode = true;
        }

        // First deal with the recursive call downwards:
        Iterator cit = node.getChildren().iterator();
        while (cit.hasNext()) {
            SimpleNode curr = (SimpleNode)cit.next();
            // See if current is giving up custody of its children
            ArrayList res = collapseAnds(curr, isAndNode);
            if (!res.isEmpty()) {
                myNewKids.addAll(res);
                kidsToRemove.add(curr);
            }
        }

        node.getChildren().addAll(myNewKids);
        node.getChildren().removeAll(kidsToRemove);
        
        if (isAndNode && parentIsAnd) {
            return node.children;
        }
        return new ArrayList();
    }

    /** 
     * Handles the extra attributes attached to terms in Lucene.
     * Detects the fuzzy "~" operator (uses morph instead of the
     * Levenshtein distance for now), a boost "^" operator, and the
     * proximit "~" operator on phrases.  Also sets the not operator
     * for "-" (or "!" as a bonus) and makes an and node for "+".
     * Finally, it checks and removes attributes that don't apply
     * since the grammar is lenient with them.  This may recreate
     * some otherwise empty "q" nodes that hold the necessary
     * attributes.
     * 
     * @param node the node whose children should be analyzed
     */
    protected static void handleAttributes(SimpleNode node) {
        ArrayList kidsToRemove = new ArrayList();
        ArrayList myNewKids = new ArrayList();
        
        //
        // Fix each child then its children
        Iterator cit = node.getChildren().iterator();
        while (cit.hasNext()) {
            SimpleNode curr = (SimpleNode)cit.next();

            if (curr instanceof LuceneASTterm) {
                //
                // First, take the boost off the end if
                // there is one.  Boost is valid on both
                // phrases and terms
                int idx = curr.value.lastIndexOf('^');
                int quoteIdx = curr.value.lastIndexOf('"');
                if (quoteIdx > idx) {
                    // carot is in a quote... forget it
                    idx = -1;
                }
                if (idx >= 0) {
                    String postCarot = curr.value.substring(idx + 1);
                    try {
                        Double.parseDouble(postCarot);
                        SimpleNode boost = new LuceneASTq((LuceneParser)curr.parser, LuceneParserTreeConstants.JJTQ);
                        boost.jjtSetParent(node);
                        boost.jjtAddChild(curr, 0);
                        curr.jjtSetParent(boost);
                        myNewKids.add(boost);
                        kidsToRemove.add(curr);
                        boost.operator = LuceneParserConstants.BOOST;
                        boost.value = postCarot;
                    } catch (NumberFormatException e) {
                        // Not a parsable number
                        // Ignore it... or do we want to fail and report it?
                    }
                    curr.value = curr.value.substring(0, idx);
                }
                //
                // Now look for a ~ on the end
                idx = curr.value.indexOf('~');
                if (idx >= 0) {
                    //
                    // If this is a quoted phrase, then the tilde
                    // means to do a proximity search and it will
                    // be followed by an integer value determining
                    // the range.
                    if (curr.value.startsWith("\"")) {
                        //
                        // There must be a proximity value
                        String postTilde = curr.value.substring(idx + 1);
                        try {
                            Integer.parseInt(postTilde);
                            SimpleNode prox = new LuceneASTq((LuceneParser)curr.parser, LuceneParserTreeConstants.JJTQ);
                            prox.jjtSetParent(node);
                            prox.jjtAddChild(curr, 0);
                            curr.jjtSetParent(prox);
                            myNewKids.add(prox);
                            kidsToRemove.add(curr);
                            prox.operator = LuceneParserConstants.PROX;
                            prox.value = postTilde;
                        } catch (NumberFormatException e) {
                            // Not a parsable number
                            // Ignore it... or do we want to fail and report it?
                        }
                    } else {
                        //
                        // Not a quote, so this ~ means that it should
                        // do a fuzzy operation.  We don't do that, so
                        // just make sure that morph is set.
                        curr.doMorph = true;
                    }
                    curr.value = curr.value.substring(0, idx);
                }

                //
                // Now prefix ops...
                if (curr.value.startsWith("+")) {
                    SimpleNode and = new LuceneASTAND((LuceneParser)curr.parser, LuceneParserTreeConstants.JJTAND);
                    and.jjtSetParent(node);
                    and.jjtAddChild(curr, 0);
                    curr.jjtSetParent(and);
                    myNewKids.add(and);
                    kidsToRemove.add(curr);
                    and.operator = LuceneParserConstants.AND;
                    curr.value = curr.value.substring(1);
                } else if (curr.value.startsWith("-")) {
                    curr.unary = LuceneParserConstants.NOT;
                    curr.value = curr.value.substring(1);
                }
            } else if (curr instanceof LuceneASTfield) {
                // see that field names are kosher
                if (curr.value.indexOf('^') >= 0) {
                    curr.value = curr.value.substring(0, curr.value.indexOf('~'));
                }
                if (curr.value.indexOf('~') >= 0) {
                    curr.value = curr.value.substring(0, curr.value.indexOf('~'));
                }
                if (curr.value.startsWith("+")) {
                    curr.value = curr.value.substring(1);
                }
                if (curr.value.startsWith("-")) {
                    curr.value = curr.value.substring(1);
                }
            }

            //
            // Analyze this node's children
            handleAttributes(curr);
        }

        // Update list of children based on changes from above
        node.getChildren().addAll(myNewKids);
        node.getChildren().removeAll(kidsToRemove);
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

            if ((curr instanceof LuceneASTterm)
                && (!curr.value.equals(""))) {

                // If it is a single-quoted string, don't touch!
                // (but do remove quotes)
                if (isSingleQuoted(curr.value) &&
                    !(node instanceof LuceneASTdoParse)) {
                    curr.value = curr.value.substring(1, curr.value.length() - 1);
                    //
                    // If somebody wanted to include a "'" in the string,
                    // they would have escaped it with a backslash.  Get
                    // rid of that if it happened.
                    curr.value = curr.value.replaceAll("\\\\'", "'");

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
                if (tokens.length > 1) {
                    // We split the string, so put each token
                    // in its own term node and make a phrase
                    // that contains them
                    SimpleNode phrase = new LuceneASTAND((LuceneParser)curr.parser, LuceneParserTreeConstants.JJTAND);
                    phrase.jjtSetParent(curr);
                    phrase.operator = LuceneParserConstants.PHRASE;
                    if (curr.unary == LuceneParserConstants.NOT) {
                        phrase.unary = LuceneParserConstants.NOT;
                    }
                    for (int i = 0; i < tokens.length; i++) {
                        SimpleNode currTerm = (SimpleNode)curr.clone();
                        currTerm.value = tokens[i];
                        //
                        // If we just set the whole phrase to have a not
                        // node, then don't also set the kids.
                        if (phrase.unary == LuceneParserConstants.NOT) {
                            currTerm.unary = SimpleNode.INVALID;
                        }
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


}

