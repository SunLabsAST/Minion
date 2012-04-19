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
//import Value;
//import Lexicon;



/* Phrase should really be an interface since there are only specializations 
 *  of it, but it is itself already a specialization of Value .... 
 */

public class Phrase implements Value {

    /*** the other forms of Value are more private so that only
	 make[Atom/List/Word] can make one of them
	 BUT we opened this one up so that "new" constructors for each
	 phrase type will work ....
    */
    public Phrase () {
    }

    @Override
    public boolean listp() {return false;}
    @Override
    public boolean phrasep() {return true;} 
    @Override
    public boolean wordp() {return false;}
    @Override
    public boolean categoryp() {return false;}

    @Override
    public Number numericalValue() {return null;}

    public Lexicon lexicon() {return null;}

   /* we don't really need a Lexicon here since we don't intern these 
     in a Lexicon's hash table, and this way we can mix Words from 
     different lexicons into a single phrase */

    @Override
    public boolean isInArray (Value[] array) { 
	//tests if this node is in an array
      return LexiconUtil.isMembOfArray(this, array);
    }

    @Override
    public boolean eq(Value obj) { //tells whether this value is eq another
      return (this == obj);}

    @Override
    public boolean equal(Value obj) { //tells whether this value is eq another
      return (this == obj);}

    @Override
    public String getWordString (){
	return "Phrase_with_no_getWordString_method";
    }
    @Override
    public synchronized String toString () {
      return "Phrase_with_no_toString_method";
    }

    @Override
    public synchronized String printString () {
      return toString();
    }

    @Override
     public synchronized String safePrintString () { //pmartin 7nov00
	return toString();
    }

    @Override
   public synchronized String phraseNameString () { //pmartin 30nov00
	return printString();
    }
  }

