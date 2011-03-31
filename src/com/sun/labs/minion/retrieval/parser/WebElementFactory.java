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
import java.text.ParseException;
import com.sun.labs.minion.document.tokenizer.UniversalTokenizer;
import com.sun.labs.minion.retrieval.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Creates QueryElements based on SimpleNodes generated from the parser
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.3 $
 */

public class WebElementFactory
{
    protected static final Logger logger =
            Logger.getLogger(WebElementFactory.class.getName());
    /** 
     * Makes a query element for the provided node.  This will only be
     * called on term (leaf) nodes for the web query language.
     *
     * @param node the node to turn into a query element
     * @return the query element for this node.
     */
    public static QueryElement make(SimpleNode node, String mods, QueryPipeline pipeline)
        throws ParseException
    {
        QueryElement qe = null;
        if (node.jjtGetNumChildren() > 0) {
            throw new ParseException("Tried to make node for non-leaf term", 0);
        }
        
        switch (node.id) {
            case WebParserTreeConstants.JJTTERM:
                //
                // This term may have punctuation in it that
                // would cause the tokenizer to split it up.
                // In this case, make a phrase of however many
                // terms the string gets tokenized into.
                String[] tokens = tokenize(node.value, pipeline);
                if (tokens.length > 1) {
                    // We split the string, so put each token
                    // in its own term node and make a phrase
                    // that contains them
                    List terms = new ArrayList();

                    for (int i = 0; i < tokens.length; i++) {
                        DictTerm t = new DictTerm(tokens[i]);
                        setMods(t, mods);
                        t.setOrder(i);
                        terms.add(t);
                    }
                    qe = new Phrase(terms);
                } else if (tokens.length == 1) {
                    DictTerm t = new DictTerm(tokens[0]);
                    setMods(t, mods);
                    qe = t;
                } else {
                    qe = null;
                }
                break;
            case WebParserTreeConstants.JJTPHRASETERM:
                //
                // Strip off the quotes and turn the phrase
                // into a bunch of terms under a phrase node
                node.value = node.value.substring(1, node.value.length() - 1);
                String[] phraseTokens = tokenize(node.value, pipeline);
                if (phraseTokens.length >= 1) {
                    List terms = new ArrayList();
                    for (int i = 0; i < phraseTokens.length; i++) {
                        DictTerm t = new DictTerm(phraseTokens[i]);
                        setMods(t, mods);
                        t.setOrder(i);
                        terms.add(t);
                    }
                    qe = new Phrase(terms);
                } else {
                    qe = null;
                }
                break;
            case WebParserTreeConstants.JJTPASSAGETERM:
                //
                // Strip off the brackets and turn the
                // passage into a bunch of terms under a pand node
                node.value = node.value.substring(1, node.value.length() - 1);
                String[] passageTokens = tokenize(node.value, pipeline);
                if (passageTokens.length >= 1) {
                    List pTerms = new ArrayList();
                    for (int i = 0; i < passageTokens.length; i++) {
                        DictTerm t = new DictTerm(passageTokens[i]);
                        setMods(t, mods);
                        t.setOrder(i);
                        pTerms.add(t);
                    }
                    qe = new PAnd(pTerms);
                } else {
                    qe = null;
                }
                break;
            default:
                throw new ParseException("Unknown term type encountered", 0);
        }

        return qe;
    }

    public static String[] tokenize(String term, QueryPipeline pipeline)
            throws ParseException {
        //
        // Start by throwing the term into a dummy document that we'll
        // feed through a pipeline to process the text
        IndexableMap docMap = new IndexableMap("query");
        docMap.put(null, term);
        try {
            pipeline.index(docMap);
        } catch (SearchEngineException ex) {
            logger.log(Level.INFO, "Exception in QueryPipeline", ex);
            throw new ParseException("Failed to tokenize query text",
                    -1);
        }
        pipeline.flush();
        return pipeline.getTokens();
    }
    
    public static void setMods(DictTerm theTerm, String mods) {
        if (theTerm.getName().indexOf("*") >= 0 || theTerm.getName().indexOf("?") >= 0) {
            theTerm.setDoWild(true);
        }
        if (mods.indexOf("~") >= 0) {
            theTerm.setDoExpand(true);
        }
        theTerm.setDoMorph(true);
    }
}
