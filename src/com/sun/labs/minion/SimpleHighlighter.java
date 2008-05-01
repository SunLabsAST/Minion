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

package com.sun.labs.minion;

import com.sun.labs.minion.util.Util;

/**
 * A simple tag-based passage highlighter.  Used when we just have
 * start/end tags for the passage and matching terms.
 */
public class SimpleHighlighter implements PassageHighlighter {
    public SimpleHighlighter(String pst, String pet,
                             String wst, String wet) {
        passageStartTag  = pst;
        passageEndTag    = pet;
        matchingStartTag = wst;
        matchingEndTag   = wet;
    }

    /**
     * Starts the context for the passage.
     */
    public String startContext() {
        return "";
    }

    /**
     * Ends the context for the passage.
     */
    public String endContext() {
        return "";
    }

    /**
     * Starts the highlighting for the passage.
     *
     * @return The code for highlighting the start of the passage.  This
     * element will be added to the output passage.
     */
    public String startPassage() {
        return passageStartTag;
    }

    /**
     * Ends the highlighting for the entire passage.
     *
     * @return The code for ending the highlight of the passage.  This
     * element will be added to the output passage.
     */
    public String endPassage() {
        return passageEndTag;
    }

    /**
     * Produces a string that will be used as an ellipsis when removing
     * text from overly long passages.
     * @return the string to use to indicate an ellipsis in a passage.
     */
    public String ellipsis() {
        return " #...# ";
    }

    /**
     * Highlights one of the actual matching terms in the query.
     *
     * @param term The matching term.
     * @param pos The position of the matching term.
     * @param sb a buffer into which we will encode the term
     * @return The code for highlighting the term, including the term!
     */
    public StringBuffer highlightMatching(String term, int pos,
                                          StringBuffer sb, boolean htmlEncode) {
        if(htmlEncode) {
            sb.append(matchingStartTag);
            Util.htmlEncode(term, sb);
            sb.append(matchingEndTag);
        } else {
            sb.append(matchingStartTag + term + matchingEndTag);
        }
        return sb;
    }

    String passageStartTag, passageEndTag;
    String matchingStartTag, matchingEndTag;
} // SimpleHighlighter
