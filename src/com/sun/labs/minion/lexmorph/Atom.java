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

package com.sun.labs.minion.lexmorph;

import com.sun.labs.minion.util.BitBuffer;
import java.util.concurrent.ConcurrentHashMap;

class Atom implements Value, Comparable {
    /**
	 * 
	 */
	protected final Lexicon lexicon;
	protected String wordstring;
    protected int index;
    protected Number numericalValue; //the numerical value of this word, if any
    protected ConcurrentHashMap  props; //property list of this atom

    // the following is private so that only makeAtom can make atoms
    protected Atom (Lexicon lexicon, String str) {
      this.lexicon = lexicon;
	wordstring = str.toLowerCase();  //pmartin 13sep99
      index = 0; // means currently not set
      props = null; ///new Properties();
      numericalValue = null;
      try {numericalValue = new Double(wordstring);
      } //see if this atom has a numerical value
      catch (NumberFormatException e) {
      }
      if (numericalValue != null) {
          try {numericalValue = new Integer(wordstring);
          } //see if this atom has an integer numerical value
          catch (NumberFormatException e) {
          }
      }
    }

    public boolean listp() { //tells whether this value is a list
      return false;}

    public boolean wordp() {return false;}

    public boolean categoryp() {return false;}

    public boolean phrasep() {return false;}

    public Number numericalValue() {return numericalValue;}

    public  boolean numeralp(){return (numericalValue != null);}

    public Lexicon lexicon() {return this.lexicon;}

    // tweaked pmartin 18 Aug 99 to use the static method
    public boolean isInArray (Value[] array) { //tests if atom is in an array
      return LexiconUtil.isMembOfArray(this, array);
    }

    public boolean eq(Value obj) { //tells whether this value is eq another
      return (this == obj);}

    public boolean equal(Value obj) { //tells whether this value is eq another
      return (this == obj);}

    public int compareTo(Object other){ //pm 27aug02
      String otherStr = ((Atom)other).wordstring;
      return wordstring.compareTo(otherStr);
    }

    public synchronized String toString () {
      return "ATOM:"+wordstring;
    }

    public synchronized String printString () {
      return wordstring;
    }

    public synchronized String phraseNameString () {
      return wordstring;
    }

    public synchronized String getWordString () {
      return wordstring;
    }

    public synchronized String safePrintString () { // no unsafe atoms!
      return wordstring;
    }

    public synchronized String printEntryString (){// for debugging
        return wordstring + ": " + this.lexicon.hashCode();
    }

    public BitBuffer encode(){ // encode Atom
        return encode(new BitBuffer());
    }
    public BitBuffer encode(BitBuffer bb){ // encode Atom
        LexiconUtil.encodeNumber(bb, numericalValue);
        LexiconUtil.encodeValueHash(bb, props);
        return bb;
    }

     protected int assignAtomIndex (){
         int newIdx = this.lexicon.highAtomIndex + 1;
         if ((newIdx) >= Lexicon.IndexAtomLimit) {
             System.err.println("Too many atoms and cats " + newIdx);
             return -1;
         }
         this.lexicon.highAtomIndex++;
         this.lexicon.valueIndexTable[newIdx] = this;
         Lexicon.log.debug(Lexicon.logTag, 10, "assignAtomIndex " + newIdx + " for " +
                   printEntryString());
         index = newIdx;
         return newIdx;
     }

    /// end Atom methods //////

  }
