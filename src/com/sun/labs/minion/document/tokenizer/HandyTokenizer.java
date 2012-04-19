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

package com.sun.labs.minion.document.tokenizer;

import com.sun.labs.minion.pipeline.Token;
import com.sun.labs.minion.pipeline.TokenCollectorStage;
import java.util.Iterator;

/**
 * A helper class to tokenize strings and return an iterator for the
 * results.
 */
public class HandyTokenizer  {
    
    /**
     * Creates a new handy tokenizer.
     */
    public HandyTokenizer() {
	tcs = new TokenCollectorStage();
	tok = new UniversalTokenizer(tcs);
    }

    /**
     * Tokenize the given string and hand back an iterator for the tokens
     * it produces.  Note that the iterator is backed by an array that may
     * change if you call tokenize again!
     *
     * @param s The string to tokenize.
     * @return an <code>Iterator</code> for the tokens that were
     * generated.
     */
    public Iterator tokenizeToIterator(String s) {
	tcs.reset();
	if(c == null || c.length < s.length()) {
	    c = s.toCharArray();
	} else {
	    s.getChars(0, s.length(), c, 0);
	}

	tok.text(c, 0, s.length());
	tok.flush();
	return tcs.iterator();
    }

    /**
     * Tokenize the given string and hand back an array of the tokens
     * it produces.
     *
     * @param s The string to tokenize.
     * @return an array containing the tokens.
     */
    public String[] tokenizeToArray(String s) {
	tcs.reset();
	if(c == null || c.length < s.length()) {
	    c = s.toCharArray();
	} else {
	    s.getChars(0, s.length(), c, 0);
	}

	tok.text(c, 0, s.length());
	tok.flush();
	Token[] toks = tcs.getTokens();
	String[] ret = new String[toks.length];
	for(int i = 0; i < ret.length; i++) {
	    ret[i] = toks[i].getToken();
	}
	return ret;
    }

    /**
     * A token collecting stage.
     */
    protected TokenCollectorStage tcs;
    /**
     * A tokenizer.
     */
    protected UniversalTokenizer tok;
    /**
     * Characters to tokenize.
     */
    protected char[] c;
    
} // HandyTokenizer
