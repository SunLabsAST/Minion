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

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/** internal class definitions **/

/* class Lexicon.WordToken is a class that contains a word and a capitalization
string used to record a particular case variation of a word. */

//   public class WordToken{
//     private Word wordObject;
//     private String wordString;

//     private WordToken(String wrdString){
//       wordObject = makeWord(wrdString);
//       wordString = wrdString;
//     }

//     public synchronized String toString(){
//       return "WORDTOKEN:"+wordString;
//     }

//     public synchronized String printString(){
//       return wordString;
//     }

//   }

/* class WordEntry has the heavy parts of a Word */
public class WordEntry {

    /**
     *
     */
    private final Lexicon lexicon;

    private Logger logger = Logger.getLogger(getClass().getName());

    ConcurrentHashMap properties; //property list of this word

    public Number numericalValue; //the numerical value of this word, if any

    boolean numeralp; // true for words made of digits[and dots]

    public Object[] sensenamep;

    // public variables for recording dictionary information:
    public Category[] noPenaltyCategories; //categories with no penalty

    public Category[] penalty1Categories; //categories with penalty 1

    public Category[] penalty2Categories; //categories with penalty 2

    public Category[] penalty3Categories; //categories with penalty 3

    public Category[] rootCategories; //categories of which this word is root

    public Atom[] features; //syntactic features

    public List capcodes; //capitalization codes: ic, lc, uc, or list of int

    public Word[] roots; //direct roots (base forms) of this word

    public Word[] prefixes; //prefixes this word has

    public Word[] suffixes; //suffixes this word has

    public Value[] compoundOf; //words this word is a compound of - maybe ambig.

    public Word[] variantOf; //words this word is a variant of

    public Word[] ikoParents; //concepts this word names a kind of

    public Word[] iioParents; //concepts this word names an instance of

    public Atom[] inflectionCodes; //codes for inflecting this word: -s, s-ed

    public Word[] subsenses; //subordinate senses of this word

    public WordEntry(Lexicon lexicon, Word wd) {
        this.lexicon = lexicon;
        String wdstring = wd.wordstring;

        //properties = new ConcurrentReaderHashMap(Lexicon.defaultCatListSize*2);
        sensenamep = null;
        numericalValue = null;
        numeralp = false; //pmartin 23dec99
        if(LexiconUtil.startsLikeNumber(wdstring)) {
            try {
                numericalValue = new Double(wdstring);
            } //see if this word should have a numericalNumber value
            catch(NumberFormatException e) {
            }
        }
        if(numericalValue != null) {
            noPenaltyCategories = new Category[1];
            // pmartin 12dec00
            if(Character.isLetter(wdstring.charAt(wdstring.length() - 1))) {
                //"27d" is seen as a double by Java reader....fix it
                numericalValue = null;
                noPenaltyCategories[0] = this.lexicon.alphanumCategory;
            } else {
                numeralp = true;
                if(wdstring.indexOf(".") < 0) { //there's no decimal point
                    noPenaltyCategories[0] = this.lexicon.integerCategory;
                    if((numericalValue.doubleValue() > Integer.MAX_VALUE) ||
                            (numericalValue.doubleValue() < Integer.MIN_VALUE)) {
                        this.putdict(this.lexicon.numValErrorAtom,
                                this.lexicon.trueAtom);
                    } else {
                        numericalValue = new Integer(numericalValue.intValue());
                    }
                    this.iioParents = new Word[]{this.lexicon.integerWord};
                } else {
                    noPenaltyCategories[0] = this.lexicon.numberCategory;
                }
                // pmartin 6dec00 make non-standard numbers variants of standard form
                String numberString = numericalValue.toString();
                if(!(numberString.equalsIgnoreCase(wdstring))) {
                    //System.out.println("in new WordEntry of number: wordstring=" +
                    //                         wdstring + " and numberString=" +
                    //                         numberString);
                    Word stdNumberWord = this.lexicon.makeWord(numberString);
                    variantOf = new Word[]{stdNumberWord};
                    stdNumberWord.addVariants(wd);
                }
            }
        }
        if(LexiconUtil.senseNameStringp(wdstring)) { //this may be a sense name
            int i, j;
            i = wdstring.indexOf("/");
            Word form = null;
            Word qual = null;
            String catStr = null;
            if(i > 0) {
                /* It is a sensename so we must build the sensenamep array
                and fill in the correct senseof, subsenses, and iko
                slots.  If other sensenames are indicated by this name we
                make sure that they are created and linked as well.

                sensenamep[0] = the category of this sensename word.
                sensenamep[1] = the word form this is a senseof
                if there is a chain, this is the word that is not a sensename
                sensenamep[2] = the optional string of modifiers after the
                slash that follows the form
                senseof = immediate parent sense; a word if this sensename
                had no mods or the sensename with 1 fewer mods then self
                senseof.subsenses += this; add in the downpointer.
                 */
                catStr = wdstring.substring(0, i + 1);
                Category cat = this.lexicon.getCategory(catStr.substring(1, i));
                if(cat != null) {
                    noPenaltyCategories = new Category[]{cat};
                    if(cat.isRootCategory()) {
                        rootCategories = new Category[]{cat};
                    }
                    // for cat, word, tailstring
                    String tailstring = "";
                    j = wdstring.indexOf("/", i + 1);
                    if(j >= 0) {
//                form = innerMakeWord(wdstring.substring(i+1,j));
// waw 02-18-05:
                        form = this.lexicon.innerMakeWord(wdstring.substring(i +
                                1, j), wd.morphCache);
                        tailstring = wdstring.substring(j); // includes initial /
                        if(tailstring.length() > 1) {
                            String qstr = tailstring.substring(1);
                            int jplus = qstr.indexOf("+");
                            if(jplus > 0) {
                                qstr = qstr.substring(0, jplus);
                            }
                            qual = this.lexicon.makeSenseWordIfNeeded(cat, qstr);
                        }
                    } else {
//              form = innerMakeWord(wdstring.substring(i+1)); // waw 02-18-05:
                        form = this.lexicon.innerMakeWord(wdstring.substring(i +
                                1), wd.morphCache);
                    }
                    sensenamep = new Object[]{cat, form, tailstring};
                } else {
                    logger.finest("**** warning: unknown category in " +
                            wdstring + " -- not accepted as sensename");
                }

                // pmartin 20july01 ...make sure we can find the master word ..
                if(form != null) {  // beefed up pmartin 18apr02
                    this.putdict(this.lexicon.senseofAtom,
                            new List(this.lexicon, form));
                    form.addSubsense(wd);
// waw 03-31-05: don't do this, since it may add v to a word that's vt, etc.
//              if (!(form.isNonnameFormOfCat(cat))) form.addWordCategory(cat, 0);
                    if(qual != null) {
                        this.addIkoParents(new Word[]{qual});
                    }
                }
            }
        } else if(LexiconUtil.compoundStringp(wdstring)) { // make a compound-of property
            Word[] components = null;
            if(wdstring.indexOf("_") > -1) {
                components = this.lexicon.makeWordSet(wdstring, "_",
                        wd.morphCache);
            } else if(wdstring.indexOf("-") > -1) {
                components = this.lexicon.makeWordSet(wdstring, "-",
                        wd.morphCache);
            }
            if(components != null) {
                this.addCompoundOf(components);
            }
        }
    ////if (linkToWord) wd.wordEntry = this;
    ////System.out.println("leaving new WordEntry :: " + wd.printEntryString());
    }

    public void clearWordEntry(boolean wipeall) { //pmartin and ww 26aug99
        // boolean savesense = (sensenamep != null);
        ConcurrentHashMap savedProps = null;
        // HashSet salvageProperties has atoms we shall save the value of
        if(!wipeall && (properties != null)) {
            Atom akey;
            Enumeration propEnum = properties.keys();
            while(propEnum.hasMoreElements()) {
                akey = (Atom) propEnum.nextElement();
                if(this.lexicon.salvageProperties.contains(akey)) {
                    if(savedProps == null) {
                        savedProps = new ConcurrentHashMap(
                                Lexicon.PROPERTIES_HASH_SIZE);
                    }
                    savedProps.put(akey, properties.get(akey));
                }
            }
        }
        if(wipeall) //||  //pmartin 25july02
        //(!numeralp && (sensenamep == null))
        {
            noPenaltyCategories = null;
            numericalValue = null;
            numeralp = false;
            sensenamep = null;
            ikoParents = null;
            iioParents = null;
            rootCategories = null;
            variantOf = null;
            subsenses = null;

        }
        // we kill the rest in any case
        penalty1Categories = null;
        penalty2Categories = null;
        penalty3Categories = null;
        features = null;
        capcodes = null;
        roots = null;
        prefixes = null;
        suffixes = null;
        inflectionCodes = null;
        //  no no no!!! just delete the word if you don't like the string
        //  pmartin 5jan00
        // wordstring = null;

        // restore saved properties if needed, else clear them all
        properties = savedProps;
    }

    /* addWordCategory adds to the category lists of a word */
    /*** The new category is added at the specified penalty level, and
     *  any instance of that category at some other penalty level is removed.
     */
    public Category[] addWordCategory(Category newCat, int penalty) {
        // pmartin 27Aug99 ..
        // tweaked to add rootCategory info and fix bad rootCat .. pmartin 29jun01
        if((newCat.isRootCategory()) && !(newCat.isInArray(rootCategories))) {
            rootCategories =
                    LexiconUtil.addArrayElement(newCat, rootCategories);
        }
        if(newCat.isInArray(noPenaltyCategories)) {
            if(penalty == 0) {
                return noPenaltyCategories;
            } else {
                noPenaltyCategories =
                        LexiconUtil.removeArrayElement(newCat,
                        noPenaltyCategories);
            }
        } else if(newCat.isInArray(penalty1Categories)) {
            if(penalty == 1) {
                return penalty1Categories;
            } else {
                penalty1Categories =
                        LexiconUtil.removeArrayElement(newCat,
                        penalty1Categories);
            }
        } else if(newCat.isInArray(penalty2Categories)) {
            if(penalty == 2) {
                return penalty2Categories;
            } else {
                penalty2Categories =
                        LexiconUtil.removeArrayElement(newCat,
                        penalty2Categories);
            }
        } else if(newCat.isInArray(penalty3Categories)) {
            if(penalty == 3) {
                return penalty3Categories;
            } else {
                penalty3Categories =
                        LexiconUtil.removeArrayElement(newCat,
                        penalty3Categories);
            }
        }
        switch(penalty) {
            case 0:
                noPenaltyCategories =
                        LexiconUtil.addArrayElement(newCat, noPenaltyCategories);
                return noPenaltyCategories;
            case 1:
                penalty1Categories =
                        LexiconUtil.addArrayElement(newCat, penalty1Categories);
                return penalty1Categories;
            case 2:
                penalty2Categories =
                        LexiconUtil.addArrayElement(newCat, penalty2Categories);
                return penalty2Categories;
            case 3:
            default:
                penalty3Categories =
                        LexiconUtil.addArrayElement(newCat, penalty3Categories);
                return penalty3Categories;
        }
    }

    public void fixIcatProperties(Word myWord, boolean pppast) {
        //pmartin 9aug01
        // pmartin added myWord for debugging 19mar04
        /*** spots property combos under number, tense, agr, and form that
         *  should be reflected as icats .. installs the icats at proper
         *  penalty level. pppast set true means that pastpart implies
         *  past (as in morphed, but not copied, props).
         *  Choices derived from WWoods code for
         *  processCatSense in MorphEngFrame as of 8aug01.
         */
        Value agrV, numV, tenseV, formV;
        List agrL, numL, tenseL, formL;
        numV = getdict(this.lexicon.numberAtom);
        formV = getdict(this.lexicon.formAtom);
        if((formV != null) && (formV.listp())) {
            formL = (List) formV;
        } else {
            formL = null;
        }

        agrV = getdict(this.lexicon.agrAtom);
        if(agrV == null) // pm 19mar04
        {
            agrV = getdict(this.lexicon.pncodeAtom);
        }
        if((agrV != null) && (agrV.listp())) {
            agrL = (List) agrV;
        } else {
            agrL = null;
        }

        tenseV = getdict(this.lexicon.tenseAtom);
        if(tenseV == null) {
            tenseV = getdict(this.lexicon.tnsAtom);
        }

//         System.out.println("in fixIcatProps of " + myWord.printEntryString());
//         String formLStr = "<null>";
//         if (formL != null) formLStr = formL.toString();
//         String agrLStr = "<null>";
//         String tenseVStr = "<null>";
//         if (tenseV != null) tenseVStr = tenseV.toString();
//         if (agrL != null) agrLStr = agrL.toString();
//         System.out.println("formL = " + formLStr +
//                            "\nAgrL = " + agrLStr +
//                            "\ntenseV = " + tenseVStr);

        if((numV != null) && (numV.listp())) {
            numL = (List) numV;
            if(LexiconUtil.isInList(this.lexicon.plAtom, numL)) {
                mergeIcat(this.lexicon.nounCategory, this.lexicon.nplCategory);
            } else if(LexiconUtil.isInList(this.lexicon.sgSlashPlAtom, numL)) {
                mergeIcat(this.lexicon.nounCategory, this.lexicon.nspCategory);
            }
        // note: a grep of biglif 9aug01 showed no others to consider
        }
        if((tenseV != null) && (tenseV.listp())) {
            tenseL = (List) tenseV;
            if(LexiconUtil.isInList(this.lexicon.pastAtom, tenseL)) {
                mergeIcat(this.lexicon.verbCategory, this.lexicon.pastCategory);
            }
            if((LexiconUtil.isInList(this.lexicon.pastpartAtom, formL)) ||
                    (LexiconUtil.isInList(this.lexicon.pastDashPartAtom, formL)) ||
                    (getdict(this.lexicon.pastpartAtom) != null) ||
                    (getdict(this.lexicon.pastDashPartAtom) != null)) {
                if(pppast) {
                    mergeIcat(this.lexicon.verbCategory,
                            this.lexicon.pastCategory);
                }
                mergeIcat(this.lexicon.verbCategory,
                        this.lexicon.pastpartCategory);
                remdict(this.lexicon.pastpartAtom);
                remdict(this.lexicon.pastDashPartAtom);
            }
            if(LexiconUtil.isInList(this.lexicon.presentAtom, tenseL)) {
                if((LexiconUtil.isInList(this.lexicon.threeSAtom, agrL)) ||
                        (LexiconUtil.isInList(this.lexicon.threeSgAtom, agrL)) ||
                        (LexiconUtil.isInList(this.lexicon.threeSingAtom, formL)) ||
                        (getdict(this.lexicon.threeSgAtom) != null)) {
                    mergeIcat(this.lexicon.verbCategory,
                            this.lexicon.v3sgCategory);
                    remdict(this.lexicon.threeSgAtom);
                } else if((LexiconUtil.isInList(this.lexicon.twoSAtom, agrL)) ||
                        (LexiconUtil.isInList(this.lexicon.twoSgAtom, agrL)) ||
                        (LexiconUtil.isInList(this.lexicon.twoSingAtom, formL)) ||
                        (getdict(this.lexicon.twoSgAtom) != null)) {
                    mergeIcat(this.lexicon.verbCategory,
                            this.lexicon.v2sgCategory);
                    remdict(this.lexicon.twoSgAtom);
                } else if((LexiconUtil.isInList(this.lexicon.oneSAtom, agrL)) ||
                        (LexiconUtil.isInList(this.lexicon.oneSgAtom, agrL)) ||
                        (LexiconUtil.isInList(this.lexicon.oneSingAtom, formL)) ||
                        (getdict(this.lexicon.oneSgAtom) != null)) {
                    mergeIcat(this.lexicon.verbCategory,
                            this.lexicon.v1sgCategory);
                    remdict(this.lexicon.oneSgAtom);
                } else if((LexiconUtil.isInList(this.lexicon.one3SAtom, agrL)) ||
                        (LexiconUtil.isInList(this.lexicon.one3SgAtom, agrL)) ||
                        (LexiconUtil.isInList(this.lexicon.one3SingAtom, formL)) ||
                        (getdict(this.lexicon.one3SgAtom) != null)) {
                    mergeIcat(this.lexicon.verbCategory,
                            this.lexicon.v13sgCategory);
                    remdict(this.lexicon.one3SgAtom);
                } else if((LexiconUtil.isInList(this.lexicon.not13SAtom, agrL)) ||
                        (LexiconUtil.isInList(this.lexicon.not13SgAtom, agrL)) ||
                        (LexiconUtil.isInList(this.lexicon.not13SingAtom, formL)) ||
                        (getdict(this.lexicon.not13SgAtom) != null)) {
                    mergeIcat(this.lexicon.verbCategory,
                            this.lexicon.not13sgCategory);
                    remdict(this.lexicon.not13SgAtom);
                } else {
                    mergeIcat(this.lexicon.verbCategory,
                            this.lexicon.not3sgCategory);
                }
            }
        }
        if((LexiconUtil.isInList(this.lexicon.prespartAtom, formL)) ||
                (LexiconUtil.isInList(this.lexicon.presDashPartAtom, formL)) ||
                (getdict(this.lexicon.prespartAtom) != null) ||
                (getdict(this.lexicon.presDashPartAtom) != null)) {
            mergeIcat(this.lexicon.verbCategory, this.lexicon.prespartCategory);
            remdict(this.lexicon.prespartAtom);
            remdict(this.lexicon.presDashPartAtom);
        }
        if((LexiconUtil.isInList(this.lexicon.pastpartAtom, formL)) ||
                (LexiconUtil.isInList(this.lexicon.pastDashPartAtom, formL)) ||
                (getdict(this.lexicon.pastpartAtom) != null) ||
                (getdict(this.lexicon.pastDashPartAtom) != null)) {
            mergeIcat(this.lexicon.verbCategory, this.lexicon.pastpartCategory);
            remdict(this.lexicon.pastpartAtom);
            remdict(this.lexicon.pastDashPartAtom);
        }

    //System.out.println("Leaving fixICatProps:" + myWord.printEntryString());
    }

    public void mergeIcat(Category nonIcat, Category icat) {
        //pmartin 10aug01
        int penalty = getCatPenalty(nonIcat);
        if(penalty > 3) {
            penalty = 0;
        }
        addWordCategory(icat, penalty);
    }

    public boolean isFormOfCat(Category c) {
        return (c.subsumesOneOf(noPenaltyCategories) ||
                c.subsumesOneOf(penalty1Categories) ||
                c.subsumesOneOf(penalty2Categories) ||
                c.subsumesOneOf(penalty3Categories));
    }

    public int getCatPenalty(Category c) { //pmartin 17may01
        if(c.subsumesOneOf(noPenaltyCategories)) {
            return 0;
        } else if(c.subsumesOneOf(penalty1Categories)) {
            return 1;
        } else if(c.subsumesOneOf(penalty2Categories)) {
            return 2;
        } else if(c.subsumesOneOf(penalty3Categories)) {
            return 3;
        } else {
            return 4; // flag for bogus penalty
        }
    }

    /** isNonnameFormOfCat is called with a category that may
     *  include "anyn" (for most basic noun).  It differs from isFormOfCat because
     *  it tries to exclude a proper noun being the reason a word is a form of
     *  noun. "anyn" subsumes "nn" which subsumes "n".
     */
    public boolean isNonnameFormOfCat(Category c) {
        if(!isFormOfCat(c)) {
            return false;
        }
        if(!this.lexicon.nnCategory.subsumesCategory(c)) {
            return true;
        }
        if(isExplicitCat(this.lexicon.nounCategory)) {
            return true; // pm 16apr04
        }
        if(isNonNounFormOfCat(c)) {
            return true; // pm 26apr04
        }
        if(!isFormOfCat(this.lexicon.nnCategory)) {
            return false;
        }
        return true;
    }

    public boolean isNonNounFormOfCat(Category c) { //pm 26apr04
        Category[] nonNounCats = c.collectExcluding(this.lexicon.nounCategory);
        if(nonNounCats == null) {
            return false;
        }
        for(int i = 0; i < nonNounCats.length; i++) {
            if(isFormOfCat(nonNounCats[i])) {
                return true;
            }
        }
        return false;
    }

    public boolean hasCommonCat() { // for we
        return isFormOfCat(this.lexicon.nonnameCats);
    }

    public boolean isNFormOfCatNN(Category cat) {// pm 20apr04
        // pm 29apr04 probably not used any more
        // this handles the special case that cat n is treated
        // as subsumed by nn -- maybe we want a new name for nn?
        return (cat == this.lexicon.nnCategory &&
                isExplicitCat(this.lexicon.nounCategory));
    }

    public boolean nnSubsumesOneOfN(Category nncat, Category[] cats) {
        // pm 20apr04
        // pm29apr04 probably not used any more since nn subsumes n
        // special hack for nn subsuming explcit n
        return (nncat == this.lexicon.nnCategory &&
                LexiconUtil.isMemberOfArray(this.lexicon.nounCategory, cats));
    }

    public boolean isNonnameFormOfCat(Category[] cc) {
        if(cc != null) {  //pmartin added null test 14sep99
            for(int i = 0; i < cc.length; i++) {
                if(isNonnameFormOfCat(cc[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isExplicitCat(Category c) { //pm 16apr04
        return (LexiconUtil.isMembOfArray(c, noPenaltyCategories) ||
                LexiconUtil.isMembOfArray(c, penalty1Categories) ||
                LexiconUtil.isMembOfArray(c, penalty2Categories) ||
                LexiconUtil.isMembOfArray(c, penalty3Categories));
    }

    public boolean isPenaltyFormOfCat(Category c) {
        if(c.subsumesOneOf(noPenaltyCategories)) {
            return false;
        }
        return (c.subsumesOneOf(penalty1Categories) ||
                c.subsumesOneOf(penalty2Categories) ||
                c.subsumesOneOf(penalty3Categories));
    }

    public boolean isPenaltyFormOfCat(Category c, int penalty) {
        if(c.subsumesOneOf(noPenaltyCategories)) {
            return !(penalty > 0);
        }
        if(c.subsumesOneOf(penalty1Categories)) {
            return !(penalty > 1);
        }
        if(c.subsumesOneOf(penalty2Categories)) {
            return !(penalty > 2);
        }
        if(c.subsumesOneOf(penalty3Categories)) {
            return !(penalty > 3);
        }
        return false;
    }

    boolean tru(String str) {// debugging hack:
        System.out.print(str);
        return true;
    }

    public boolean isRootOfCat(Category c) {// tweaked pm 20apr04
        //System.out.println("isRootOfCat this=" + this + " cat=" + c);
        //if (this.rootCategories == null//)
        //   System.out.println("no root cats");
        //    else for (int i=0; i < this.rootCategories.length//; i++)
        //  System.out.print ("rootcat["+ i+ "]=" + this.rootCategories[i] + ", ");
        if(c.subsumesOneOf(this.rootCategories)) {
            return true;
        }
        //tru("flunkedRootCats ");
        //    if (nnSubsumesOneOfN(c,this.rootCategories)) return true;
        //   tru("flunkedNNSubsumes ");
        return false;
    }

    public boolean addCompoundOf(List cwlist) {
        Value[] oldComp = compoundOf;
        Value[] newComp = LexiconUtil.addValueToArray(oldComp, cwlist);
        if(oldComp != newComp) {
            compoundOf = newComp;
            return true;
        } else {
            return false;
        }
    }

    public boolean addCompoundOf(Word[] cwords) {
        // copies array so arg array can be reused;
        return addCompoundOf(this.lexicon.makeList(cwords));
    }

    public boolean addIkoParents(Word[] newIkoParents) {
        Word[] oldIkos = ikoParents;
        Word[] newIkos = LexiconUtil.mergeArrays(oldIkos, newIkoParents);
        if(oldIkos == newIkos) {
            return false;
        } else {
            ikoParents = newIkos;
            return true;
        }
    }

    public Word[] getSenseParents() { // pmartin 14 May 02
        Value spv = getdict(this.lexicon.senseofAtom);
        if(spv == null) {
            return null;
        }
        List spl = (List) spv;
        if(spl.length() == 0) {
            return null;
        }
        int splen = spl.length();
        Word[] cc = new Word[splen];
        System.arraycopy(spl.contents, 0, cc, 0, splen);
        return cc;
    }

    public Word[] getIkoParents(boolean useSubSenses) {
        // pmartin 8aug01  extendes the simpler form if bool is true
        if((!useSubSenses) || (subsenses == null)) {
            return ikoParents;
        } else {
            HashSet ssIkos = new HashSet(Lexicon.defaultCatListSize);
            if(ikoParents != null) {
                for(int i = 0; i < ikoParents.length; i++) {
                    ssIkos.add(ikoParents[i]);
                }
            }
            for(int i = 0; i < subsenses.length; i++) {
                Word ss = subsenses[i];
                WordEntry sswe = ss.getWordEntry();
                if(sswe != null) {
                    Word[] ssikoParents = sswe.ikoParents;
                    if(ssikoParents != null) {
                        for(int j = 0; j < ssikoParents.length; j++) {
                            ssIkos.add(ssikoParents[j]);
                        }
                    }
                }
            }
            Word[] allIkos = new Word[ssIkos.size()];
            allIkos = (Word[]) (ssIkos.toArray(allIkos));
            return allIkos;
        }
    }

    /* markDict(...) is used in morph to add properties to a word
     */
    public Value markDict(Atom p, Value value, boolean addToList) {
        if(value != null) {
            if(addToList) {
                if(value.listp()) {
                    Value[] array = ((List) value).contents;
                    for(int i = 0; i < array.length; i++) {
                        addtodictprop(p, array[i], true);
                    }
                    return getdict(p);
                } else {
                    return addtodictprop(p, value, true);
                }
            } else {
                return putdict(p, value);
            }
        } else {
            return getdict(p);
        }
    }

    /* markDict(...) is used in morph to add properties to a word
     */
    public Value markDict(Atom p, Value[] valArray, boolean addToList) {
        if(valArray != null) {
            if(addToList) {
                for(int i = 0; i < valArray.length; i++) {
                    addtodictprop(p, valArray[i], true);
                }
                return getdict(p);
            } else {
                return putdict(p, this.lexicon.makeList(valArray));
            }
        } else {
            return getdict(p);
        }
    }

    public synchronized String printWordEntryString() {
        String val = "";
        if(noPenaltyCategories != null) {
            val = val + LexiconUtil.printString(noPenaltyCategories);
        }
        val = val + "; ";
        if(penalty1Categories != null) {
            val = val + "p1:" + LexiconUtil.printString(penalty1Categories) +
                    "; ";
        }
        if(penalty2Categories != null) {
            val = val + "p2:" + LexiconUtil.printString(penalty2Categories) +
                    "; ";
        }
        if(penalty3Categories != null) {
            val = val + "p3:" + LexiconUtil.printString(penalty3Categories) +
                    "; ";
        }
        if(features != null) {
            val = val + "features: " + LexiconUtil.printString(features) +
                    "; ";
        }
        if(capcodes != null) {
            val = val + "capcodes:";
            int i = 0;
            while(i < capcodes.length()) {
                Value element = capcodes.elementAt(i);
                if(i > 0) {
                    val = val + " ";
                }
                if(element.listp() == false) {
                    val = val + element.printString();
                } else {
                    List elements = (List) element;
                    int j = 0;
                    while(j < elements.length()) {
                        if(j > 0) {
                            val = val + ",";
                        }
                        val = val + elements.elementAt(j).printString();
                        j++;
                    }
                }
                i++;
            }
            val = val + ";";
        }
        if(roots != null) {
            val = val + "root:" + LexiconUtil.safePrintString(roots) +
                    "; ";
        }
        if(prefixes != null) {
            val = val + "prefix:" + LexiconUtil.safePrintString(prefixes) +
                    "; ";
        }
        if(suffixes != null) {
            val = val + "suffix:" + LexiconUtil.safePrintString(suffixes) +
                    "; ";
        }
        if(inflectionCodes != null) {
            val = val + "icodes:" + LexiconUtil.safePrintString(inflectionCodes) +
                    "; ";
        }
        if(numericalValue != null) {
            val = val + "numval:" + String.valueOf(numericalValue) +
                    "; ";
        }
        if(compoundOf != null) {
            val = val + "compound-of:" + LexiconUtil.safePrintString(compoundOf) +
                    "; ";
        }
        if(variantOf != null) {
            val = val + "variant-of:" + LexiconUtil.safePrintString(variantOf) +
                    "; ";
        }
        if(ikoParents != null) {
            val = val + "iko:" + LexiconUtil.safePrintString(ikoParents) +
                    "; ";
        }
        if(iioParents != null) {
            val = val + "iio:" + LexiconUtil.safePrintString(iioParents) +
                    "; ";
        }
        if(subsenses != null) {
            val = val + "subsenses:" + LexiconUtil.safePrintString(subsenses) +
                    "; ";
        }

        if(properties != null) {
            TreeMap propsMap = new TreeMap(properties);
            Iterator it = propsMap.entrySet().iterator();
            while(it.hasNext()) {
                Map.Entry mpent = (Map.Entry) it.next();
                Atom key = (Atom) mpent.getKey();
                Value propVal = (Value) mpent.getValue();
                String propValString;
                if(propVal instanceof List) {
                    propValString = LexiconUtil.safePrintString(
                            ((List) propVal).contents);
                } else {
                    propValString = propVal.safePrintString();
                }
                val = val + " " + key.printString() + ": " +
                        // ((Value)properties.get(key)).printString() +
                        // pmartin 14 sep 99 ... line below crashes on cast failure for non-lists
                        // but the code above prints an extra level of parens for lists...
                        ///            Lexicon.this.printString(((List)properties.get(key)).contents) +
                        propValString + ";";
            }
        }
        return val;
    }

    /* addtodictprop adds property values to property lists of words */
    // pmartin added null and empty value hardening 11jun01
    public Value addtodictprop(Atom p, Value value, boolean atEnd) {
        Value oldval = getdict(p);
        if(value == null) {
            return oldval;
        } else if((oldval == null) || (((List) oldval).length() == 0)) {
            Value[] array = new Value[]{value};
            return putdict(p, this.lexicon.makeList(array));
        } else if(oldval.equal(value)) {
            return oldval;
        } else if(!oldval.listp()) {
            Value[] array = new Value[]{oldval, value};
            return putdict(p, this.lexicon.makeList(array));
        } else if(((List) oldval).hasMember(value)) {
            return oldval;
        } else {
            int offset;
            int li = ((List) oldval).length();
            if(Lexicon.authorFlag) {
                if(Lexicon.debug) {
                    logger.finest("this=" + this +
                            " in addtodictprop(" + p.printString() +
                            ", " + value.toString() + ", " +
                            atEnd + ") and old value " + oldval.toString());
                }
            }
            Value[] oldarray = ((List) oldval).contents;
            Value[] array = new Value[li + 1];
            if(atEnd) {
                offset = 0;
                array[li] = value;
            } else {
                offset = 1;
                array[0] = value;
            }
            for(int ii = 0; ii < li; ii++) {
                array[ii + offset] = oldarray[ii];
            }
            return putdict(p, this.lexicon.makeList(array));
        }
    }

    public Value addtodictprop(Atom p, Value value) {
        return addtodictprop(p, value, true);
    }

    public Value getdict(Atom p) {
        if(properties == null) {
            return null;
        }
        return (Value) properties.get(p);
    }

    public Value putdict(Atom p, Value newval) {
        // System.out.println("putdict of WE called with prop " + p.wordstring +
        //                          " and val " + newval);
        if(properties == null) {
            properties = new ConcurrentHashMap(Lexicon.PROPERTIES_HASH_SIZE);
        }
        if(p == null) {
            logger.finest("**** warning: null attribute in putdict " //+
                    // wordstring
                    + " for value: " + newval);
        } else if(newval == null) {
            logger.finest("**** warning: null value in putdict " //+
                    // wordstring
                    + " for property: " + p);
        } else {
            properties.put(p, newval);
        }
        return newval;
    }

    public Value remdict(Atom p) {
        if((p != null) && (properties != null)) {
            //System.out.println("remdict property " + p);
            properties.remove(p);
        }
        return null;
    }

    /// end WordEntry methods //////
}
