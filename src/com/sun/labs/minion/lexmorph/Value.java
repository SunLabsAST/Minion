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

package com.sun.labs.minion.lexmorph; // move to package by swapping comments here

public interface Value {
    public boolean listp(); //tells whether this value is a list
    public boolean wordp(); //tells whether this value is a word
    public boolean phrasep(); // true only if value is a phrase
    public boolean categoryp(); //tells whether this value is a category
    public Number numericalValue(); //the numerical value of this, if any
    public boolean eq (Value val); //eq predicate
    public boolean equal (Value val); //equal predicate
    public boolean isInArray (Value[] vals); //member predicate
    public String printString(); //clean string to print out
    public String getWordString(); //an underbar-connected string of contents
    public String safePrintString(); //safely quoted clean string to print out
    public String phraseNameString(); // string of phrase structure
  }
