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
 * A class that holds a string along with some information about any markup
 * used in the string.
 */
public class IndexableString  {

    public enum Type {
        /**
         * A type for plain text fields.
         */
        PLAIN,

        /**
         * A type for HTML fields.
         */
        HTML,

        /**
         * A type for which a custom analyzer is provided.
         */
        CUSTOM
    }

    /**
     * The string to index.
     */
    protected String value;

    /**
     * The type of markup used in the string.
     */
    protected Type markupType;

    /** 
     * The custom analyzer to use if this object is markup type CUSTOM 
     */
    protected CustomAnalyzer canalyzer;

    /**
     * Create a new IndexableString from the parameter
     * @param value a string that forms the basis for a new indexablestring
     */
    public IndexableString(String value) {
        this.value = value;
        this.markupType = Type.PLAIN;
    }

    /**
     * Create a new IndexableString from an existing string with a specified
     * markup type
     * @param value a string that forms the basis for a new IndexableString
     * @param markupType The markup type for the new IndexableString. Should be
     * one of Type.PLAIN or Type.HTML
     */
    public IndexableString(String value, Type markupType) {
        this.value = value;
        this.markupType = markupType;
    }
    
    /** 
     * Creates a new IndexableString from an existing string with a specified
     * markup type and a specified analyzer.
     * 
     * @param value a string that forms the basis for a new IndexableString
     * @param canalyzer the class that will analyze the custom markup type
     */
    public IndexableString(String value, CustomAnalyzer canalyzer) {
        this.value = value;
        this.markupType = Type.CUSTOM;
        this.canalyzer = canalyzer;
    }

    /**
     * Gets the markupType value.
     * @return the markupType value.
     */
    public Type getMarkupType() {
        return markupType;
    }

    /**
     * Sets the markupType value.
     * @param markupType The new markupType value.
     */
    public void setMarkupType(Type markupType) {
        this.markupType = markupType;
    }

    /**
     * Gets the value of the indexable string
     * @return the string to index
     */
    public String getValue() {
        return value;
    }

    /** 
     * Gets the CustomAnalyzer to use with this string
     * 
     * @return the CustomAnalyzer
     */
    public CustomAnalyzer getCustomAnalyzer() {
        return canalyzer;
    }

    /** 
     * Sets the CustomAnalyzer to use with this string
     * 
     * @param canalyzer the CustomAnalyzer
     */
    public void setCustomAnalyzer(CustomAnalyzer canalyzer) {
        this.canalyzer = canalyzer;
    }
} // IndexableString
