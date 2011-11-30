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

import com.sun.labs.minion.indexer.postings.FieldOccurrence;


/**
 * A class encapsulating all of our knowledge about a given token.
 * Instances of this class are passed down an indexing pipeline as they are
 * parsed from the file.
 */
public class Token implements FieldOccurrence {
    
    /**
     * The string for a token.
     */
    protected String token;
    
    /**
     * The ordinal number of this word in the document.
     */
    protected int wordNum;
    
    /**
     * The type of this token, whether standard, bigram, or punctuation.
     */
    protected int type;
    
    /**
     * The starting character offset for the token.
     */
    protected int start;
    
    /**
     * The ending character offset for the token.
     */
    protected int end;
    
    /**
     * The occurrence count for this token.
     */
    protected int count;
    
    /**
     * An ID assigned to this token.
     */
    protected int id;
    
    /**
     * A set of fields active for this token.
     */
    protected int[] fields;
    
    /**
     * An indicator to show if this token contains digits
     * (The taxonomy classifier ignores such tokens.)
     */
    protected boolean containsDigits;
    
    public static final int NORMAL = 1;
    
    public static final int BIGRAM = 2;
    
    public static final int PUNCT = 3;
    
    public Token() {
    }
    
    /**
     * Creates a token.
     */
    public Token(String token, int count) {
        this(token, 1, NORMAL, 1, 2, count);
    }
    
    /**
     * Creates a token.
     */
    public Token(String token, int wordNum, int type) {
        this(token, wordNum, type, 0, 0, 1);
    }
    
    /**
     * Creates a token that can be passed down the pipeline.
     *
     * @param token The string tokenized from the input data
     * @param wordNum The ordinal word number of this token in the indexed
     * material.
     * @param start The starting character offset of this token
     * @param end The ending character offset of this token
     */
    public Token(String token, int wordNum, int start, int end) {
        this(token, wordNum, NORMAL, start, end, 1);
    }
    
    /**
     * Creates a token that can be passed down the pipeline.
     *
     * @param token The string tokenized from the input data
     * @param wordNum The ordinal word number of this token in the indexed
     * material.
     * @param type The type of this token, from our constant types
     * @param start The beginning character offset of this token
     * @param end The ending character offset of this token
     */
    public Token(String token, int wordNum,
            int type, int start, int end) {
        this(token, wordNum, type, start, end, 1);
    }
    
    /**
     * Creates a token that can be passed down the pipeline.
     *
     * @param token The string tokenized from the input data
     * @param wordNum The ordinal word number of this token in the indexed
     * material.
     * @param type The type of this token, from our constant types
     * @param start The beginning character offset of this token
     * @param end The ending character offset of this token
     */
    public Token(String token, int wordNum,
            int type, int start, int end, int count) {
        reset(token, wordNum, type, start, end, count);
    }
    
    public Token reset(String token, int wordNum,
            int type, int start, int end) {
        return reset(token, wordNum, type, start, end, 1);
    }
    
    public Token reset(String token, int wordNum,
            int start, int end) {
        return reset(token, wordNum, Token.NORMAL, start, end, 1);
    }
    
    public Token reset(String token, int wordNum,
            int type, int start, int end, int count) {
        this.token   = token;
        this.wordNum = wordNum;
        this.type    = type;
        this.start   = start;
        this.end     = end;
        this.count   = count;
        containsDigits = false;
        for(int i = 0; i < token.length(); i++) {
            if(Character.isDigit(token.charAt(i))) {
                containsDigits = true;
                break;
            }
        }
        return this;
    } // Token constructor
    
    public int length() {
        return token.length();
    }
    
    public String getToken() {
        return token;
    }
    
    /**
     * This method is intentionally package-private.  In classification,
     * we'll reset the token to a stemmed token.  Otherwise, this object
     * should be immutable.
     */
    public void setToken(String token) {
        this.token = token;
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public int getWordNum() {
        return wordNum;
    }
    
    public void incrWordNum() {
        wordNum++;
    }
    
    public int getStart() {
        return start;
    }
    
    public int getEnd() {
        return end;
    }
    
    @Override
    public String toString() {
        return String.format("[%s, %d, %d, %d, %d, %s]",
                token, wordNum, start, end, count, type);
    }
    
    //
    // Implementation of FieldOccurrence.
    
    /**
     * Gets the ID of the term in this occurrence.
     *
     * @return the ID for the term.
     */
    public int getID() {
        return id;
    }
    
    /**
     * Sets the ID for this token.
     */
    public void setID(int id) {
        this.id = id;
    }
    
    /**
     * Gets the count of occurrences for this token.
     *
     * @return the number of occurrences.
     */
    public int getCount() {
        return count;
    }
    
    /**
     * Sets the word number for this token.
     */
    public void setWordNum(int wordNum) {
        this.wordNum = wordNum;
    }
    
    /**
     * Sets the count of occurrences that this occurrence represents.
     *
     * @param count the number of occurrences.
     */
    public void setCount(int count) {
        this.count = count;
    }
    
    /**
     * Gets the position at which the occurrence was found.
     *
     * @return the position where the occurrence was found.
     */
    public int getPos() {
        return wordNum;
    }
    
    /**
     * Sets the position for this token.
     */
    public void setPos(int pos) {
        this.wordNum = pos;
    }
    
    /**
     * Gets the fields that are active at the time of the occurrence.
     *
     * @return an array that is as long as the number of defined fields.
     * The <em>i<sup>th</sup></em> element of this array indicates the
     * current position in the field whose ID is <em>i</em>.  If element 0
     * of this array is greater than zero, then no fields are currently
     * active.
     */
    public int[] getFields() {
        return fields;
    }
    
    public void setFields(int[] fields) {
        this.fields = fields;
    }
    
    public boolean containsDigits() {
        return containsDigits;
    }

} // Token

