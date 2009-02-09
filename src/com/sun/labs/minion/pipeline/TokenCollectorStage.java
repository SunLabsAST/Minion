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
package com.sun.labs.minion.pipeline;

import java.util.Iterator;
import com.sun.labs.minion.util.Util;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A stage that collects the tokens that come down the pipe into an array
 * that someone can retrieve.  Used for tokenizing strings from the passage
 * retrieval component.
 */
public class TokenCollectorStage extends StageAdapter {

    /**
     * Instantiates a token collecting stage that will only collect tokens
     * in the given ranges.
     */
    public TokenCollectorStage(int[] sc, int[] ec) {
        super(null);
        nRanges = Math.min(sc.length, ec.length);
        ranges = new Range[nRanges];
        for(int i = 0; i < nRanges; i++) {
            ranges[i] = new Range(sc[i], ec[i]);
        }
        count = 0;
        punct = new StringBuffer();
    }

    /**
     * Instantiates a token collecting stage.  This stage never has a
     * downstream stage or pipe.
     */
    public TokenCollectorStage() {
        super(null);
        ranges = new Range[1];
        ranges[0] = new Range(-1, -1);
        count = 0;
        nRanges = 1;
        punct = new StringBuffer();
    }

    /**
     * Processes a token.
     */
    public void token(Token tok) {

        addPunct();

        //
        // Add this token to the ranges.
        for(int i = 0; i < nRanges; i++) {
            ranges[i].add(tok);
        }
    }

    /**
     * Add punctuation.
     */
    protected void addPunct() {
        if(punct.length() > 0) {
            Token t = new Token(punct.toString(),
                    punctWord, Token.PUNCT,
                    pStart, pEnd);
            for(int i = 0; i < nRanges; i++) {
                ranges[i].add(t);
            }
            pStart = -1;
            punct.setLength(0);
        }
    }

    /**
     * Keeps track of the punctuation that we've been given, so that we
     * don't have multiple elements in the array of punctuation.
     */
    public void punctuation(Token punct) {
        this.punct.append(punct.getToken());
        if(pStart == -1) {
            pStart = punct.getStart();
        }
        pEnd = punct.getEnd();
        punctWord = punct.getWordNum();
    }

    /**
     * Resets the stage.
     */
    public void reset() {
        for(int i = 0; i < nRanges; i++) {
            ranges[i].nTokens = 0;
        }
        count = 0;
    }

    /**
     * Resets the stage with new starts and ends.
     */
    public void reset(int[] s, int[] e, boolean wf[]) {

        //
        // Make sure there's enough room.
        int len = Math.min(s.length, e.length);
        if(len > nRanges) {
            Range[] temp = new Range[len];
            System.arraycopy(ranges, 0, temp, 0, nRanges);
            for(int i = nRanges; i < len; i++) {
                temp[i] = new Range(-1, -1);
            }
            ranges = temp;
        }
        nRanges = len;

        //
        // Reset the ranges.
        for(int i = 0; i < nRanges; i++) {
            ranges[i].reset(s[i], e[i], wf[i]);
        }
        count = 0;
    }

    /**
     * Returns the collected tokens and punctuation.
     *
     * @return an array of strings containing the tokens and punctuation
     * that the tokenizer created.
     */
    public Token[] getTokens() {
        return ranges[0].getTokens();
    }

    /**
     * Returns an iterator for the tokens that we collected.
     */
    public Iterator iterator() {
        return ranges[0].iterator();
    }

    /**
     * Returns the currently active ranges.
     */
    public Range[] getRanges() {
        Range[] ret = new Range[nRanges];
        System.arraycopy(ranges, 0, ret, 0, nRanges);
        return ret;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < ranges.length; i++) {
            sb.append(ranges[i].toString());
        }
        return sb.toString();
    }

    public boolean equals(Object o) {
        if(o instanceof TokenCollectorStage) {
            TokenCollectorStage ocs = (TokenCollectorStage) o;
            if(ranges.length != ocs.ranges.length) {
                logger.info("unequal: " + ranges.length + " " +
                        ocs.ranges.length);
                return false;
            }
            for(int i = 0; i < ranges.length; i++) {
                if(!ranges[i].equals(ocs.ranges[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * A class to hold the tokens for a given range of the tokenized
     * material.
     */
    public class Range {

        /**
         * Initializes a range.
         */
        public Range(int s, int e) {
            sw = s;
            ew = e;

            //
            // There's at least this many tokens that will be coming down
            // the pipe.
            int n = ew - sw + 1;
            tokens = new Token[n];
            nTokens = 0;
        }

        /**
         * Tells this range that we're starting a field, and that the first
         * word of the field will be the given word.
         *
         * @param w The word number for the first word in the field.
         */
        public void startField(int w) {
            if(w == sw) {
                collect = true;
            }
        }

        /**
         * Tells this range that we're ending a field.
         */
        public void endField() {
            collect = false;
        }

        /**
         * Tries to add a token to this range.
         */
        public void add(Token token) {

            int w = token.getWordNum();

            //
            // Add it if we have no range defined or if it's in the range.
            if(sw == -1 || (wholeField && collect) ||
                    (!wholeField && w >= sw && w <= ew)) {
                if(nTokens + 1 > tokens.length) {
                    int newCap = tokens.length + 10;
                    tokens = (Token[]) Util.expand(tokens, newCap);
                }
                tokens[nTokens++] = token;
            }
        }

        /**
         * Get an iterator for the tokens in this range.
         */
        public Iterator iterator() {
            return new RangeIterator();
        }

        /**
         * Resets the range.
         */
        public void reset() {
            nTokens = 0;
        }

        /**
         * Resets the range with a new start and end position.
         */
        public void reset(int s, int e, boolean wf) {
            sw = s;
            ew = e;
            wholeField = wf;
            nTokens = 0;
        }

        /**
         * Returns the collected tokens and punctuation.
         *
         * @return an array of strings containing the tokens and punctuation
         * that the tokenizer created.
         */
        public Token[] getTokens() {
            return (Token[]) Util.getExact(tokens, nTokens);
        }

        public boolean equals(Object o) {
            if(o instanceof Range) {
                Range or = (Range) o;

                boolean bad = false;
                for(int i = 0; i < nTokens; i++) {
                    logger.info(tokens[i].getToken() + " " + tokens[i].getType() +
                            " / " +
                            or.tokens[i].getToken() +
                            " " + or.tokens[i].getType());

                    if(!tokens[i].getToken().equals(or.tokens[i].getToken())) {
                        return false;
                    }

                    if(tokens[i].getType() != or.tokens[i].getType()) {
                        logger.warning(tokens[i].getToken() + ": " + tokens[i].
                                getType() + " " + or.tokens[i].getType());
                        bad = true;
                    }
                }

                if(bad) {
                    logger.log(Level.WARNING,
                            "Got all the way through, but bad!");
                }
                return !bad;
            }
            return false;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < nTokens; i++) {
                sb.append(tokens[i].toString() + "\n");
            }
            return sb.toString();
        }

        protected class RangeIterator implements Iterator {

            public RangeIterator() {
                curr = 0;
            }

            public boolean hasNext() {
                return curr < nTokens;
            }

            public Object next() {

                if(curr >= nTokens) {
                    throw new java.util.NoSuchElementException(
                            "No more elements!");
                }
                return tokens[curr++];
            }

            public void remove() {
                throw new UnsupportedOperationException(
                        "Not supported for RangeIterator");
            }
            int curr;

        }
        /**
         * The starting word count for the range.
         */
        protected int sw;

        /**
         * The ending word count for the range.
         */
        protected int ew;

        /**
         * Whether we should collect the entire field surrounding the start
         * and end word, including any punctuation.
         */
        protected boolean wholeField;

        /**
         * Whether we should be collecting words.
         */
        protected boolean collect;

        /**
         * The number of tokens that have been collected.
         */
        protected int nTokens;

        /**
         * The collected tokens.
         */
        protected Token[] tokens;

    }
    /**
     * The number of tokens that have gone by.
     */
    protected int count;

    /**
     * The number of ranges we're considering.
     */
    protected int nRanges;

    /**
     * A buffer for punctuation.
     */
    protected StringBuffer punct;

    /**
     * The word number for the last punctuation that we got.
     */
    protected int punctWord;

    protected int pStart;

    protected int pEnd;

    /**
     * The ranges we're collecting.
     */
    protected Range[] ranges;

    /**
     * Whether we're collecting bigrams.
     */
    protected boolean doingBigrams;

    Logger logger = Logger.getLogger(getClass().getName());

    protected static String logTag = "TCS";

} // TokenCollectorStage
