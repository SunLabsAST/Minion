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

/**
 * An interface for providing call back functions when doing passage
 * highlighting. A passage consists of three parts:
 *
 * <ol> <li> The context. This is the text before and after the actual passage.
 * The size of the context is determined by the application. This interface
 * provides callbacks for the beginning and end of the context. <li> The
 * passage. This is the actual passage that was determined from the document.
 * This interface provides callbacks for the beginning and ending of the
 * passage. <li> The matching terms. These are the terms from the document that
 * matched the terms in the query. This interface provides a callback that will
 * be called each time a matching term is encountered. </ol>
 *
 */
public interface PassageHighlighter {

    /**
     * Starts the context for the passage.
     *
     * @return the starting context for the passage.
     */
    public String startContext();

    /**
     * Ends the context for the passage.
     *
     * @return the tag to use for the end of the context for this passage.
     */
    public String endContext();

    /**
     * Starts the highlighting for the passage.
     *
     * @return The code for highlighting the start of the passage. This element
     * will be added to the output passage.
     */
    public String startPassage();

    /**
     * Ends the highlighting for the entire passage.
     *
     * @return The code for ending the highlight of the passage. This element
     * will be added to the output passage.
     */
    public String endPassage();

    /**
     * Produces a string that will be used as an ellipsis when removing text
     * from overly long passages.
     *
     * @return the string to be used as an ellipsis in the passage.
     */
    public String ellipsis();

    /**
     * Highlights one of the actual matching terms in the query.
     *
     * @param term The matching term.
     * @param pos The position of the matching term.
     * @param b A buffer to encode the term onto.
     * @param htmlEncode If
     * <code>true</code> the term must be html encoded before being added.
     * @return The code for highlighting the term, including the term!
     */
    public StringBuilder highlightMatching(String term, int pos, StringBuilder b,
                                           boolean htmlEncode);
} // PassageHighlighter
