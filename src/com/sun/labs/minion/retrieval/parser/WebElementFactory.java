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
import com.sun.labs.minion.pipeline.TokenCollectorStage;
import com.sun.labs.minion.document.tokenizer.Tokenizer;
import com.sun.labs.minion.document.tokenizer.UniversalTokenizer;
import com.sun.labs.minion.retrieval.*;

import com.sun.labs.minion.Searcher;

/**
 * Creates QueryElements based on SimpleNodes generated from the parser
 *
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.3 $
 */

public class WebElementFactory
{

    /** 
     * Makes a query element for the provided node.  This will only be
     * called on term (leaf) nodes for the web query language.
     *
     * @param node the node to turn into a query element
     * @return the query element for this node.
     */
    public static QueryElement make(SimpleNode node, String mods, TokenCollectorStage tcs)
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
                String[] tokens = tokenize(node.value, tcs);
                if (tokens.length > 1) {
                    // We split the string, so put each token
                    // in its own term node and make a phrase
                    // that contains them
                    List terms = new ArrayList();
                    
                    for (int i = 0; i < tokens.length; i++) {
                        DictTerm t = new DictTerm(tokens[i]);
                        setMods(t, mods);
                        terms.add(t);
                    }
                    qe = new Phrase(terms);
                } else if (tokens.length == 1) {
                    DictTerm t = new DictTerm(node.value);
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
                String[] phraseTokens = tokenize(node.value, tcs);
                if (phraseTokens.length >= 1) {
                    List terms = new ArrayList();
                    for (int i = 0; i < phraseTokens.length; i++) {
                        DictTerm t = new DictTerm(phraseTokens[i]);
                        setMods(t, mods);
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
                String[] passageTokens = tokenize(node.value, tcs);
                if (passageTokens.length >= 1) {
                    List pTerms = new ArrayList();
                    for (int i = 0; i < passageTokens.length; i++) {
                        DictTerm t = new DictTerm(passageTokens[i]);
                        setMods(t, mods);
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

    public static String[] tokenize(String term, TokenCollectorStage tcs) {
        String[] results;
        
        UniversalTokenizer tok = new UniversalTokenizer(tcs, false);
        // Don't break strings on the wildcard chars
        tok.noBreakCharacters = "*?";
        char[] vals = term.toCharArray();
        tcs.reset();
        tok.text(vals, 0, vals.length);
        tok.flush();
        com.sun.labs.minion.pipeline.Token[] tokens = tcs.getTokens();
        // Put each token in the results
        results = new String[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            results[i] = tokens[i].getToken();
        }
        return results;
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
