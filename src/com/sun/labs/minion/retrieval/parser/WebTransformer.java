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
import com.sun.labs.minion.pipeline.TokenCollectorStage;
import com.sun.labs.minion.document.tokenizer.Tokenizer;
import com.sun.labs.minion.document.tokenizer.UniversalTokenizer;
import com.sun.labs.minion.util.MinionLog;

import com.sun.labs.minion.Searcher;

/**
 * This class transforms the output of JavaCC into a tree of
 * query elements that the query evaluator can understand.
 * It's more than meets the eye.
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.2 $
 */

public class WebTransformer extends Transformer
{
    protected final static String validMods = "+-~/";
    
    public static void main(String args[]) throws Exception {
        WebParser p = new WebParser(System.in);
        SimpleNode n = (SimpleNode)p.doParse();
        n.dump(":");
        WebTransformer t = new WebTransformer();
        QueryElement e = t.transformTree(n);
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


    public WebTransformer() {
        
    }


    /** 
     * Transforms an abstract syntax tree provided by JJTree+JavaCC into
     * a tree of QueryElements that can be used by the query evaluator.
     * 
     * @param root the root node of the tree returned from the Parser
     * @return the root node of a tree describing a query
     */
    public QueryElement transformTree(SimpleNode root)
        throws ParseException
    {
        return transformTree(root, Searcher.OP_PAND);
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
    public QueryElement transformTree(SimpleNode root, int defaultOperator)
        throws ParseException
    {
        QueryElement result = null;
        //
        // The AST should be pretty simple for the web query language.  There
        // should be a single root "q" element, containing a set of "qe"s, each
        // containing a single term node
        TokenCollectorStage tcs = new TokenCollectorStage();

        //
        // Make a bunch of buckets to contain each of the operator types
        List ors = new ArrayList();
        List ands = new ArrayList();

        SimpleNode q = (SimpleNode)root.getChildren().iterator().next();
        Iterator cit = q.getChildren().iterator();
        while (cit.hasNext()) {
            SimpleNode curr = (SimpleNode)cit.next();
            String mods = getMods(curr.value);
            String term = curr.value.substring(mods.length(), curr.value.length());
            curr.value = term;
            QueryElement qe = WebElementFactory.make(curr, mods, tcs);

            if (qe != null) {
                if (isNot(mods)) {
                    ands.add(new Not(qe));
                } else if (isOr(mods)) {
                    ors.add(qe);
                } else if (isAnd(mods)) {
                    ands.add(qe);
                } else {
                    switch (defaultOperator) {
                        case Searcher.OP_AND:
                        case Searcher.OP_PAND:
                            ands.add(qe);
                            break;
                        case Searcher.OP_OR:
                            ors.add(qe);
                            break;
                        default:
                            throw new ParseException("Unknown default operator", 0);
                    }
                }
            }
        }

        //
        // If anything was put together with OR, put them all into an OR
        // then add it to the ANDs list.
        if (!ors.isEmpty()) {
            QueryElement or = new Or(ors);
            ands.add(or);
        }

        //
        // Now determine which kind of AND to make
        if (defaultOperator == Searcher.OP_PAND) {
            result = new PAnd(ands);
        } else {
            result = new And(ands);
        }

        return result;
    }

    public static boolean isAnd(String mods) {
        if (mods.indexOf("+") > -1) {
            return true;
        }
        return false;
    }

    public static boolean isNot(String mods) {
        if (mods.indexOf("-") > -1) {
            return true;
        }
        return false;
    }

    public static boolean isOr(String mods) {
        if (mods.indexOf("/") > -1) {
            return true;
        }
        return false;
    }

    public static String getMods(String term) {
        String mods = "";
        int i = 0;
        //
        // For each leading char, see if it is
        // a modifier char. If it is, put it in
        // mods.
        while (i < term.length()) {
            char curr = term.charAt(i);
            if (validMods.indexOf(curr) > -1) {
                if (mods.indexOf(curr) < 0) {
                    mods = mods + curr;
                } else {
                    // We already had this modifier
                    // so this char is part of the string
                    return mods;
                }
            } else {
                //
                // this wasn't a valid modifier, so
                // we've found all the mods we will
                return mods;
            }
        }
        return mods;
    }
}