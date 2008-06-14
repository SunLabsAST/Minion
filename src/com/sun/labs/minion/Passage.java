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
 * An interface for a class that holds the information for a passage
 * generated from a search.  Classes that implement this interface can be
 * used to get highlighted and un-highlighted versions of the passage for
 * display.
 */
public interface Passage {

    /**
     * The types of passage that we will return.
     */
    public enum Type {

        /**
         * A type indicating that separate passages within a single field
         * should be joined together.
         */
        JOIN, 
        /**
         * A type indicating that separate passages within a single field should
         * be kept separate.
         */
        UNIQUE
    }

    /**
     * Gets the penalty score associated with this passage.
     * @return the score associated with this passage
     */
    public float getScore();

    /**
     * Marks up the passage using the highlighter.
     *
     * @param highlighter The highlighter that will be used to mark up the
     * passage.  If this is <code>null</code> no highlighting will be
     * done.
     * @param htmlEncode If <code>true</code> the highlighted passage will
     * have its text HTML encoded so that it may be safely given to a Web
     * browser.
     *
     * @return the highlighted passage, cut down to the size specified when 
     * the passage was defined.
     * @see #getHLValue
     * @see #getUnHLValue
     */
    public String highlight(PassageHighlighter highlighter, boolean htmlEncode);

    /**
     * Marks up the passage using the highlighter.
     *
     * @param highlighter The highlighter that will be used to mark up the
     * passage.  If this is <code>null</code> no highlighting will be
     * done.
     * @return the highlighted passage, cut down to the size specified when 
     * the passage was defined.
     * @see #getHLValue
     * @see #getUnHLValue
     */
    public String highlight(PassageHighlighter highlighter);

    /**
     * Gets the highlighted value for this passage.
     * @param elided If <code>true</code> returns the passage cut down to
     * the size specified when the passages were made.  If
     * <code>false</code> the unelided, highlighted passage is returned.
     * @return the highlighted value for this passage
     */
    public String getHLValue(boolean elided);
    
    /**
     * Gets the highlighted value for this passage.  The passage will be cut
     * down to the size specified when the passages were made.
     * @return a highlighted string containing the passage.
     */
    public String getHLValue();

    /**
     * Gets the unhighlighted value for this passage.
     * @param elided If <code>true</code> returns the passage cut down to
     * the size specified when the passages were made.  If
     * <code>false</code> the unelided, unhighlighted passage is returned.
     * @return the unhighlighted value of the passage
     */
    public String getUnHLValue(boolean elided);

    /**
     * Gets the character positions of the passage words in the highlighted
     * passage string that was returned earlier.  This really only makes
     * sense if you didn't ask for highlighting!
     * @return the character positions of the words in the highlighted passage string
     */
    public int[] getWordPositions();

    /**
     * Gets the terms from the passage that match the terms in the query.
     *
     * @return an array of strings containing the terms that were found in
     * the document that match the terms from the query that generated this
     * document.  If an element of the array is <code>null</code>, then
     * that means that a term is missing from the passage.
     */
    public String[] getMatchingTerms();

    /**
     * Gets the starting character positions in the document for the terms
     * that make up the passage.
     *
     * @return an array containing the character positions of the start of
     * each term that appears in the passage.  The position of the start of
     * the term is defined as the position of the first letter in the
     * term. This information can be used if it is necessary to highlight
     * the actual document for display.
     */
    public int[] getMatchStart();

    /**
     * Gets the ending character positions in the document for the terms
     * that make up the passage.
     *
     * @return an array containing the character positions of the end of
     * each term that appears in the passage.  The position of the end of a
     * term is defined as the position <em>after</em> the position of the
     * last character in the word.  This information can be used if it is
     * necessary to highlight the actual document for display.
     */
    public int[] getMatchEnd();
} // Passage
