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

import java.util.ArrayList;
import java.util.HashSet;

import com.sun.labs.minion.util.BitBuffer;

public class Category extends Atom {
    /**
	 * 
	 */
	public Category[] subcats;
    public boolean isRootCategory = true; //default is root category
    public State startState = null; //most don't have one  //pmartin 27Aug99
    public long[] myCatBits = null; // single bit for basic cats
    public long[] subCatsBits = null;  //filled in by makeSubcatBits

    Category (Lexicon lexicon, String namestring) {
      super (lexicon, namestring);
    }

    public synchronized String toString () {
      return "CATEGORY:" + super.wordstring;
    }

    public boolean categoryp() {return true;}


    protected int assignCategoryIndex (){
      int newIdx = this.lexicon.highCatIndex + 1;
      if ((newIdx) >= Lexicon.IndexCategoryLimit) {
        System.err.println("Too many categories " + newIdx);
        return -1;
      }
      this.lexicon.highCatIndex++;
      this.lexicon.valueIndexTable[newIdx] = this;
      Lexicon.log.debug(Lexicon.logTag, 10, "assignCategoryIndex " + newIdx + " for " +
                printEntryString());
      index = newIdx;
      return newIdx;
    }

    public BitBuffer encode(){ // encode Category
      return encode(new BitBuffer());
    }

    public BitBuffer encode(BitBuffer bb){ // encode Category
      LexiconUtil.encodeNumber(bb, numericalValue);
      LexiconUtil.encodeValueHash(bb, props);
      LexiconUtil.encodeCategories(bb, subcats);
      bb.push(isRootCategory);
      return bb;
    }

    public boolean isInArray (Category[] array) {
      // System.out.println("debugging category.isInArry with cat = "+
        // this.toString() +
      //                         " and array= " + toStringArray(array));
      boolean answer = LexiconUtil.isMembOfArray(this, array);
      //System.out.println("debugging category.isInArray -- returning " + answer);
      return answer;
    }

    public boolean isRootCategory() {return isRootCategory;}

    public Category[] getSubcats () {
      return subcats;
    }

    public boolean subsumesOneOf (Category[] cats) {
      int i = 0;
      if (cats == null) return false;
      while (i < cats.length) {
        if (this.subsumesCategory(cats[i])) return true;
        i++;
      }
      return false;
    }

    public boolean subsumesCategory (Category cat, HashSet triedCats) {
    // public boolean subsumesCategory (Category cat, Category[] triedCats) {
//       System.out.println("debugging subsumescat of cat=" + cat.printString() +
//             " and subcats=" + arrayToString(subcats));
//         System.out.println(" and tried cats = {{");
//         for (int i=0; i < triedCats.size(); i++)
//             System.out.println(triedCats.elementAt(i).toString() + " ");

      if (this == null) return false;
      if (this == cat) return true;
      if (subcats == null) return false;
      if (triedCats.contains(this)) return false;
      triedCats.add(this);
       //if ((i+1)>=triedCats.length)
       //          System.out.println(
       //   "subsumption test array is too small for this=" +
       //                             this + " and cat=" + cat);
       //triedCats[i]=this;
       //triedCats[i+1] = null;
      int i = 0;
      while (i < subcats.length) {
        if (subcats[i].subsumesCategory(cat, triedCats)) return true;
        i++;
      }
      return false;
    }

    public void showCategory(){ // 25july01 pmartin
        String rs = "root";
        if (!(isRootCategory)) rs = "NOT root";
        System.out.print(super.wordstring + " (" + rs + ") ");
        Category[] scats = subcats;
        if (scats != null){
            System.out.print("subcats: ");
            for (int i =0; i < scats.length; i++)
                System.out.print(scats[i].printString() + " ");
        }
        if (myCatBits != null){
            System.out.print("\ncatBits = ");
            for (int i=0; i < myCatBits.length; i++)
                System.out.print(Long.toHexString(myCatBits[i]) + " ");
            }
        if (subCatsBits != null){
            System.out.print("\nsubCatsBits = ");
            for (int i=0; i < subCatsBits.length; i++)
                System.out.print(Long.toHexString(subCatsBits[i]) + " ");
        }
        System.out.println("");
    }

    //tweaked pmartin 6 August 99
    // tweaked again 14aug01 pmartin to save Vector init time
    // untweaked 10sep01 to restore vector for multi-thread future
    // added subCatsBits code 12mar02
    // tweaked once more 20nov02 to remove Vector
    // pmartin added simple equal test 9mar04
    public boolean subsumesCategory (Category cat) {
        if (this == cat) return true;
        if (cat == null) return false; // pmartin 11mar04
        long[] thisBits = subCatsBits;
        long[] catBits = cat.myCatBits;
        if ((thisBits != null) & (catBits != null)) {
            int siz = thisBits.length;
            for (int i=0; i < siz; i++)
                if ((thisBits[i] & catBits[i]) != 0) return true;
            return false;
        }
      HashSet triedCats = new HashSet(Lexicon.defaultCategoryTableSize);
      return this.subsumesCategory(cat, triedCats);
    }

    public  Category[] collectExcluding(Category ecat){
       ArrayList gscats = new ArrayList();
       if (! ecat.subsumesCategory(this)) return new Category[]{this};
       if (subcats == null) return null;
       for (int i=0; i < subcats.length; i++)
           if (! ecat.subsumesCategory(subcats[i]))
               gscats.add(subcats[i]);
       if (gscats.size() == 0) return null;
       Category[] ans = new Category[gscats.size()];
       return (Category[])gscats.toArray(ans);
    }

    // pmartin 27Aug99 added instance var and these 2 methods to support parser
    public void setStartState(State ss){
      startState = ss;
    }

    public State getStartState(){
      return startState;
    }

    /// end Category methods //////

  }
