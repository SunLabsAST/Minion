/*
 * Copyright 2007-2011 Sun Microsystems, Inc. All Rights Reserved.
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
 * An exception that is thrown when a parsing error occurs.  ParseException
 * is a subclass of {@link SearchEngineException} that allows for special
 * handling of errors generated during query parsing.  The exception message
 * provides details about where the error occurred and those details are
 * also available programmatically.
 */
public class ParseException extends SearchEngineException {
    protected String badToken;
    
    protected int offset;
    
    /**
     * Creates a parse exception with the given token image that occurs at
     * the given offset.
     * @param badToken the token that was bad
     * @param offset the offset of the first character of the token
     */
    public ParseException(String badToken, int offset) {
        super("Parsing failed when reading token \"" + badToken +
                "\" at column " + offset);
        this.badToken = badToken;
        this.offset = offset;
    }
    
    /**
     * Returns the first invalid token that was parsed
     * 
     * @return the first invalid token that was parsed
     */
    public String getToken() {
        return badToken;
    }
    
    /**
     * Returns the offset of the first character of the first invalid token.
     * 
     * @return the offset of the first character of the first invalid token
     */
    public int getOffset() {
        return offset;
    }
}
