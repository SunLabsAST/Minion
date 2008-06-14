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

package com.sun.labs.minion.retrieval;

import java.util.Arrays;

import com.sun.labs.minion.Passage;
import com.sun.labs.minion.PassageHighlighter;

import com.sun.labs.minion.pipeline.Token;

import com.sun.labs.minion.util.MinionLog;
import com.sun.labs.minion.util.Util;

public class PassageImpl implements Passage, Comparable {

    /**
     * The penalty score associated with this passage.
     */
    protected float penalty;

    /**
     * The query terms making up this passage.
     */
    protected String[] qt;

    /**
     * The terms from the document matching the query terms.
     */
    protected String[] mt;

    /**
     * The word numbers associated with this passage.
     */
    protected int[] posns;

    /**
     * The positions in the <code>tokens</code> array of the words
     * associated with this passage.
     */
    protected int[] tokenPosns;

    /** 
     * The list of starting positions for the highlighted tokens in this 
     * passage
     */
    protected int[] tokenStarts;

    /** 
     * The list of ending positions for the highlighted tokens in this 
     * passage
     */
    protected int[] tokenEnds;
    
    /**
     * The amount of context to keep.
     */
    protected int context;

    /**
     * The start of the range we want to collect for this passage.
     */
    protected int start;

    /**
     * The end of the range we want to collect for this passage.
     */
    protected int end;

    /**
     * The size of the passage, in tokens.
     */
    protected int size;

    /**
     * The size of the passage, in characters.
     */
    protected int charSize;

    /**
     * The maximum length, in characters, of any highlighted passage that
     * we'll return.  A value of -1 means the whole passage will be
     * returned.
     */
    protected int maxSize;

    /**
     * Whether we're finished collecting this field.
     */
    protected boolean finished;

    /**
     * The tokens and punctuation making up this passage, collected while
     * parsing the document.
     */
    protected Token[] tokens;

    /**
     * The full passage, highlighted.
     */
    protected String fullHLValue;

    /**
     * The full passage, unhighlighted.
     */
    protected String fullUnHLValue;

    /**
     * The elided passage, highlighted.
     */
    protected String elidedHLValue;

    /**
     * The elided passage, unhighlighted.
     */
    protected String elidedUnHLValue;

    protected static MinionLog log = MinionLog.getLog();
    protected static String logTag = "PI";

    /**
     * Creates a passage for the given set of positions.
     *
     * @param posns The word positions of the terms making up the passage.
     * @param penalty The penalty associated with this passage.
     * @param qt The terms used in the query.
     * @param context The size of the surrounding context to put in the
     * passage, in words.  -1 means take the entire containing field.
     * @param maxSize The maximum length, in characters, of any highlighted
     * passage that we will return.  -1 means that there is no maximum
     * length.
     */
    public PassageImpl(int[] posns, float penalty, String[] qt, int context,
                       int maxSize) {

        this.penalty = penalty;
        this.qt = qt;

        if(posns == null || posns.length == 0) {
            this.posns = new int[0];
            this.context = -1;
        } else {
            this.posns = posns;
            this.context = context;
        }
        tokenPosns = new int[this.posns.length];
        this.mt = new String[qt.length];
        this.maxSize = maxSize;

        //
        // We'll set the token positions to -1 to start with.
        for(int i = 0; i < tokenPosns.length; i++) {
            tokenPosns[i] = -1;
        }

        //
        // Figure out a start and end based on the context.
        if(this.context == -1) {
            //
            // If it's -1, we'll take anything.
            start = -1;
            end = -1;
            tokens = new Token[32];
        } else {

            //
            // Figure out the min and max positions and use those with the
            // context.
            int max = Integer.MIN_VALUE;
            int min = Integer.MAX_VALUE;
            for(int i = 0; i < posns.length; i++) {
                if(posns[i] > 0) {
                    max = Math.max(posns[i], max);
                    min = Math.min(posns[i], min);
                }
            }
            start = min - context;
            if(start < 1) {
                start = 1;
            }
            end = max + context;
            tokens = new Token[end-start+1];
        }
    } // PassageImpl constructor

    
    /**
     * Gets the penalty score associated with this passage.
     */
    public float getScore() {
        return penalty;
    }

    /**
     * Tries to add a token to this passage.  The token will only be added
     * if it falls in the range defined by the passage.
     *
     * @param t The token to try to add.
     */
    protected boolean add(Token t) {

        //
        // If we're done collecting, then we're done.
        if(finished) {
            return false;
        }
        
        int wn = t.getWordNum();

        //
        // See if this token falls into the range.
        if(start == -1 || (wn >= start && wn <= end)) {
            if(size >= tokens.length) {
                Token[] temp = new Token[tokens.length*2];
                System.arraycopy(tokens, 0, temp, 0, tokens.length);
                tokens = temp;
            }
            tokens[size] = t;
            for(int i = 0; i < posns.length; i++) {
                if(posns[i] == wn) {
                    tokenPosns[i] = size;
                    mt[i % mt.length] = t.getToken();
                }
            }
            size++;
            charSize += t.getToken().length();
            return true;
        }

        return false;
    }

    /**
     * Tells us that our field has ended.  If we're collecting an entire
     * field, We return another instance of passage so that we can collect
     * more occurrences of this field, should they pop up.
     */
    protected PassageImpl endField() {
        if(!finished && context == -1) {
            finished = true;
            return new PassageImpl(posns, penalty, qt, context, maxSize);
        }
        return null;
    }
    
    public String highlight(PassageHighlighter highlighter,
                          boolean htmlEncode) {
        StringBuffer b = new StringBuffer();

        //
        // See if this is a passage that we collected, even though we
        // didn't have positions.  If so, we'll just cat together the
        // tokens.
        if(tokenPosns.length == 0) {
            for(int i = 0; i < size; i++) {
                b.append(tokens[i].getToken());
            }
            fullHLValue = b.toString();
            fullUnHLValue = fullHLValue;

            //
            // If we need to restrict the size of the passage, take the
            // initial substring of the required size.
            if(maxSize > 0 && maxSize < charSize) {
                elidedHLValue = fullHLValue.substring(0, maxSize);
                elidedUnHLValue = fullUnHLValue.substring(0, maxSize);
            } else {
                elidedHLValue = fullHLValue;
                elidedUnHLValue = fullUnHLValue;
            }
            return elidedHLValue;
        }

        //
        // figure out where the passage starts and ends in the tokens
        // array.
        int pStart = Integer.MAX_VALUE;
        int pEnd = Integer.MIN_VALUE;
        for(int i = 0; i < tokenPosns.length; i++) {
            if(posns[i] > 0) {
                pStart = Math.min(tokenPosns[i], pStart);
                pEnd = Math.max(tokenPosns[i], pEnd);
            }
        }


        //
        // Initialize the token starts and ends lists
        tokenStarts = new int[tokenPosns.length];
        tokenEnds = new int[tokenPosns.length];
        int startEndCnt = 0;
        
        StringBuffer uhl = new StringBuffer();
        b.append(highlighter.startContext());
        for(int i = 0; i < size; i++) {

            if(i == pStart) {
                b.append(highlighter.startPassage());
            }

            boolean found = false;
            int[] tokenPosnsSorted = new int[tokenPosns.length];
            System.arraycopy(tokenPosns, 0, tokenPosnsSorted, 0, tokenPosns.length);
            Arrays.sort(tokenPosnsSorted);
            int prev = -1;
            for(int j = 0; j < tokenPosns.length; j++) {
                if (tokenPosnsSorted[j] == prev) {
                    continue;
                }
                if(i == tokenPosnsSorted[j]) {
                    highlighter.highlightMatching(tokens[i].getToken(),
                                                  tokens[i].getStart(),
                                                  b,
                                                  htmlEncode);
                    tokenStarts[startEndCnt] = tokens[i].getStart();
                    tokenEnds[startEndCnt] = tokens[i].getEnd();
                    startEndCnt++;
                    found = true;
                }
                prev = tokenPosnsSorted[j];
            }

            if(!found) {
                b.append(tokens[i].getToken());            
            }
            

            if(i == pEnd) {
                b.append(highlighter.endPassage());
            }

            //
            // Keep track of unhighlightedness.
            uhl.append(tokens[i].getToken());
        }
        b.append(highlighter.endContext());
        
        fullHLValue = b.toString();
        fullUnHLValue = uhl.toString();

        if(maxSize > 0 && maxSize < charSize) {
            elidedHLValue = elide(highlighter, htmlEncode);
            elidedUnHLValue = elide(null, false);
        } else {
            elidedHLValue = fullHLValue;
            elidedUnHLValue = fullUnHLValue;
        }
        return elidedHLValue;
    }

    public String highlight(PassageHighlighter highlighter) {
        return highlight(highlighter, false);
    }
    
    public String getHLValue() {
        return getHLValue(true);
    }

    public String getHLValue(boolean elided) {
        return elided ? elidedHLValue : fullHLValue;
    }
	    
    public String getUnHLValue(boolean elided) {
        return elided ? elidedUnHLValue : fullUnHLValue;
    }

    /**
     * Creates a string from a set of tokens that does not exceed the given
     * length.  This length is exclusive of any highlighting markup.
     *
     * <p>
     *
     * The basic idea:  produce the passage in chunks centered around the
     * hit terms.  Begin with chunks that are just the hit terms and then
     * at each step, add tokens before and after the hit terms until the
     * string length limit is reached.
     * @param ph the highlighter to use on the passage
     * @param htmlEncode whether the string should be HTML encoded while highlighting
     * @return a highlighted, elided string of the passage
     */
    public String elide(PassageHighlighter ph, boolean htmlEncode) {

        //
        // We'll just work on the token positions that are actually there.
        int n = 0;
        for(int i = 0; i < tokenPosns.length; i++) {
            if(tokenPosns[i] >= 0) {
                n++;
            }
        }

        //
        // We'll keep track of the positions before and after the hit terms
        // as well as the chunks we're building.
        int[] sp = new int[n];
        int[] before = new int[n];
        int[] after = new int[n];
        StringBuffer[] chunks = new StringBuffer[n];
        int currSize = 0;

        //
        // Set up the positions.  We want just the actual positions, and we
        // want them sorted.
        n = 0;
        for(int i = 0; i < tokenPosns.length; i++) {
            if(tokenPosns[i] >= 0) {
                sp[n++] = tokenPosns[i];
            }
        }
        Arrays.sort(sp);

        //
        // Start up by highlighting the matching terms.
        for(int i = 0; i < sp.length; i++) {
            Token t = tokens[sp[i]];
            chunks[i] = new StringBuffer();
            if(ph != null) {
                if(i == 0) {
                    chunks[i].append(ph.startPassage());
                }
                ph.highlightMatching(t.getToken(), t.getStart(),
                                     chunks[i], htmlEncode);
                if(i == sp.length - 1) {
                    chunks[i].append(ph.endPassage());
                }
            } else {
                chunks[i].append(t.getToken());
            }
            before[i] = sp[i] - 1;
            after[i] = sp[i] + 1;
            currSize += t.getToken().length();
        }


        //
        // OK, we'll fill in the gaps between the matching terms before we
        // add on left and right context.  We'll do that from left to
        // right.
        boolean done = false;
        while(!done) {
            done = true;
            for(int i = 0; i < after.length - 1; i++) {

                //
                // Watch for places with no tokens, and watch for the
                // overlap between words!
                if(after[i] >= before[i+1]) {
                    continue;
                }

                //
                // Append the token after this position and the one before
                // the next position.
                String a = tokens[after[i]++].getToken();
                if(currSize + a.length() >= maxSize) {
                    break;
                }
                chunks[i].append(a);
                currSize += a.length();

                String b = tokens[before[i+1]--].getToken();
                if(currSize + b.length() >= maxSize) {
                    break;
                }

                chunks[i+1].insert(0, b);
                currSize += b.length();

                done = false;
            }
        }

        //
        // If we haven't filled up on the between words stuff, then add
        // context at either end until we do.
        if(currSize < maxSize) {
            done = false;
            while(!done) {
                done = true;
                if(before[0] >= 0) {
                    String s = tokens[before[0]--].getToken();
                    if(currSize + s.length() >= maxSize) {
                        break;
                    }
                    chunks[0].insert(0, s);
                    currSize += s.length();
                    done = false;
                }
                if(after[after.length-1] < size) {
                    String s = tokens[after[after.length-1]++].getToken();
                    if(currSize + s.length() >= maxSize) {
                        break;
                    }
                    chunks[chunks.length-1].append(s);
                    currSize += s.length();
                    done = false;
                }
            }
        }

        //
        // Put together the chunks.
        for(int i = 1; i < chunks.length; i++) {
            if(before[i] > after[i-1]) {
                if(ph != null) {
                    chunks[0].append(ph.ellipsis());
                } else {
                    chunks[0].append(" ... ");
                }
            }
            chunks[0].append(chunks[i]);
        }

        //
        // Put in the context jazz, if required.
        if(ph != null) {
            chunks[0].insert(0, ph.startContext());
            chunks[0].append(ph.endContext());
        }

        return chunks[0].toString();
    }
	    
    /**
     * Gets the character positions of the passage words in the higlighted
     * passage string that was returned earlier.  This really only makes
     * sense if you didn't ask for highlighting!
     */
    public int[] getWordPositions() {
        return posns;
        // 	return pass.getPassageWordPositions();
    }
    
    /**
     * Gets the terms from the passage that match the terms in the query.
     *
     * @return an array of strings containing the terms that were found in
     * the document that match the terms from the query that generated this
     * document.  If an element of the array is <code>null</code>, then
     * that means that a term is missing from the passage.
     */
    public String[] getMatchingTerms() {
        return mt;
    }

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
    public int[] getMatchStart() {
        if (tokenStarts != null) {
            return tokenStarts;
        } else {
            return new int[0];
        }
    }

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
    public int[] getMatchEnd() {
        if (tokenEnds != null) {
            return tokenEnds;
        } else {
            return new int[0];
        }
    }

    public int compareTo(Object o) {
        PassageImpl pi = (PassageImpl) o;
        if(penalty < pi.penalty) {
            return -1;
        }

        if(penalty > pi.penalty) {
            return 1;
        }

        return 0;
    }

    public String toString() {
        return Util.arrayToString(tokenStarts) + " " + Util.arrayToString(tokenEnds);
    }

} // PassageImpl
