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

import java.util.Collection;
import java.util.Vector;
import java.util.logging.Logger;

class List implements Value {

    /**
     *
     */
    private final Lexicon lexicon;

    public Value[] contents;

    private static Logger logger = Logger.getLogger(List.class.getName());

    public List(Lexicon lexicon) {
        this.lexicon = lexicon;
        contents = new Value[10]; // pmartin made public 01aug01
    }

    public List(Lexicon lexicon, int siz) {
        this.lexicon = lexicon;
        contents = new Value[siz]; // pmartin 18apr02
    }

    public List(Lexicon lexicon, Value[] inputArray) { // pmartin 18apr02
        this.lexicon = lexicon;
        contents = inputArray;
    }

    public List(Lexicon lexicon, Value newval) {
        this.lexicon = lexicon;
        contents = new Value[]{newval};
    }

    public List(Lexicon lexicon, Vector inputVector) {  //pmartin 27Sept99
        this.lexicon = lexicon;
        int iSize = inputVector.size();
        contents = new Value[iSize];
        inputVector.copyInto(contents);
    }

    public List(Lexicon lexicon, Collection inColl) {  //pmartin 5dec01
        this.lexicon = lexicon;
        Value[] va = new Value[inColl.size()];
        contents = (Value[]) inColl.toArray(va);
    }

    public List(Lexicon lexicon, Value v1, Value v2) { // pmartin 21dec00
        this.lexicon = lexicon;
        Value[] newArray = new Value[2];
        newArray[0] = v1;
        newArray[1] = v2;
        contents = newArray;
    }

    public List(Lexicon lexicon, List oldL) { //pmartin 11mar04 == clone
        this.lexicon = lexicon;
        Value[] oldva = oldL.contents;
        int olen = oldva.length;
        Value[] nva = new Value[olen];
        System.arraycopy(oldva, 0, nva, 0, olen);
        contents = nva;
    }

    @Override
    public boolean listp() {
        return true; //tells whether this value is a list
    }

    @Override
    public boolean phrasep() {
        return false;
    }

    @Override
    public boolean wordp() {
        return false;
    }

    @Override
    public boolean categoryp() {
        return false;
    }

    //    public Number numericalValue() {
    //      return null; //no numerical value for lists
    //   }
    public boolean numeralp() {
        return false;
    }

    @Override
    public Number numericalValue() { // for recursion mostly .. pmartin 17may01
        if((contents == null) || (contents.length < 1)) {
            return null;
        } else {
            return contents[0].numericalValue();
        }
    }

    @Override
    public boolean isInArray(Value[] array) { //tests if this node is in an array
        return LexiconUtil.isMembOfArray(this, array);
    }

    public Lexicon lexicon() {
        return this.lexicon;
    }

    public int length() {
        return contents.length;
    }

    public Value elementAt(int i) {
        return contents[i];
    }

    public boolean hasMemb(Value obj) { //memb does eq checks in lists
        int i = 0;
        while(i < contents.length) {
            if(contents[i] == obj) { // found member
                return true;
            }
            i++;
        }
        return false;
    }

    public boolean hasMember(Value obj) { //member does equal checks in lists
        int i = 0;
        while(i < contents.length) {
            if(contents[i] == obj) { // found member
                return true;
            } else if(obj.listp() && contents[i].listp() &&
                    ((List) contents[i]).equal(obj)) {
                return true;
            }
            i++;
        }
        return false;
    }

    @Override
    public boolean eq(Value obj) { //tells whether this value is eq another
        return (this == obj);
    }

    @Override
    public boolean equal(Value obj) {
        if((obj == null) || (!obj.listp())) {
            return false; //lists can only equal lists
        }
        if(((List) obj).length() != contents.length) {
            return false;
        }
        Value[] objContents = ((List) obj).contents;
        int i = 0;
        while(i < contents.length) {
            if(contents[i].listp()) {
                if(!objContents[i].listp()) {
                    return false;
                }
                if(!((List) contents[i]).equal(objContents[i])) {
                    return false;
                }
            } else if(objContents[i].listp()) {
                return false;
            } else if(contents[i] != objContents[i]) {
                return false;
            }
            i++;
        }
        return true;
    }

    public Word[] wordsIn() {
        /** returns the words in a list or complains
         */
        if(contents == null || contents.length == 0) {
            return null;
        }
        int siz = contents.length;
        int j = 0;
        Value temp;
        Word[] out = new Word[siz];
        for(int i = 0; i < siz; i++) {
            temp = contents[i];
            if(temp instanceof Word) {
                out[j++] = (Word) temp;
            } else {
                logger.finest("can't make a word of element " + i +
                        " in List " + this);
            }
        }
        if(j == siz) {
            return out;
        }
        Word[] oops = new Word[j];
        for(int i = 0; i < j; i++) {
            oops[i] = out[i];
        }
        return oops;
    }

    @Override
    public synchronized String toString() {
        if(contents == null) {
            return "List:null";
        }
        String val = "LIST:(";
        Value elt;
        int i = 0;
        while(i < contents.length) {
            elt = contents[i];
            if(i > 0) {
                val = val + " ";
            }
            if(elt == null) {
                val = val + "null";
            } else {
                val = val + elt.toString();
            }
            i++;
        }
        return val + ")";
    }

    @Override
    public synchronized String printString() {
        if(contents == null) {
            return "null";
        }
        String val = "(";
        Value elt;
        int i = 0;
        while(i < contents.length) {
            elt = contents[i];
            if(i > 0) {
                val = val + " ";
            }
            if(elt == null) {
                val = val + "null";
            } else {
                val = val + elt.printString();
            }
            i++;
        }
        return val + ")";
    }

    @Override
    public synchronized String getWordString() {
        if((contents == null) || (contents.length == 0)) {
            return "";
        }
        String val = "(" + contents[0].getWordString();
        Value elt;
        for(int i = 1; i < contents.length; i++) {
            elt = contents[i];
            if(elt != null) {
                val = val + "_" + elt.printString();
            }
        }
        return val + ")";
    }

    @Override
    public String phraseNameString() {
        return getWordString();
    }

    @Override
    public synchronized String safePrintString() {
        if(contents == null) {
            return "null";
        }
        String val = "(";
        Value elt;
        int i = 0;
        while(i < contents.length) {
            elt = contents[i];
            if(i > 0) {
                val = val + " ";
            }
            if(elt == null) {
                val = val + "null";
            } else {
                val = val + elt.safePrintString();
            }
            i++;
        }
        return val + ")";
    }

    public List append(List lmore) { //pmartin 11mar04
        int mylen = contents.length;
        int morlen = lmore.length();
        if(mylen == 0) {
            return new List(this.lexicon, lmore);
        }
        if(morlen == 0) {
            return new List(this.lexicon, this);
        }
        Value[] newva = new Value[mylen + morlen];
        for(int ii = 0; ii < mylen; ii++) {
            newva[ii] = contents[ii];
        }
        for(int ii = 0; ii < morlen; ii++) {
            newva[mylen + ii] = lmore.contents[ii];
        }
        return new List(this.lexicon, newva);
    }

    /// end List methods //////
}
