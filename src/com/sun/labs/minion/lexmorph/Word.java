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
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;

import com.sun.labs.minion.util.BitBuffer;
import com.sun.labs.minion.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Word implements Value {

    /**
     *
     */
    private final Lexicon lexicon;

    private Logger logger = Logger.getLogger(getClass().getName());

    protected String wordstring;

    protected boolean dontPurge;

    protected int index;
    //NOWORDENTRY for word stubs
    //NODISKINDEX for not yet on disk,
    //DISKOUTDATED for dirty

    BitBuffer bbuf;

    WordEntry wordEntry;

    char[] dangerousChars = new char[]{':', ';'};  //pmartin 3nov00 for quoting

    public Hashtable morphCache = null; // waw added 02-18-05

    // The constructor is private, so only the method makeWord can make words
    Word(Lexicon lexicon, String str) {
        this.lexicon = lexicon;
        wordstring = str;
        dontPurge = false;
        bbuf = null;
        index = Lexicon.NOWORDENTRY;
        Double testNum = null;
        if(LexiconUtil.startsLikeNumber(str)) { //see if this word has a numericalNumber value
            try {
                testNum = new Double(wordstring);
            } catch(NumberFormatException e) {
            }
        }
        if((testNum != null) || (LexiconUtil.senseNameStringp(str)) ||
                (LexiconUtil.compoundStringp(str))) {
            index = Lexicon.NODISKINDEX;
            wordEntry = new WordEntry(this.lexicon, this);
        } else {
            wordEntry = null;
        }
    }

    Word(Lexicon lexicon, String str, Hashtable cache) { // waw 03-19-05
        this.lexicon = lexicon;
        wordstring = str;
        dontPurge = false;
        bbuf = null;
        index = Lexicon.NOWORDENTRY;
        Double testNum = null;
        if(LexiconUtil.startsLikeNumber(str)) { //see if this word has a numericalNumber value
            try {
                testNum = new Double(wordstring);
            } catch(NumberFormatException e) {
            }
        }
        if(cache != null) { // waw 03-19-05
            // need to do this before new WordEntry is made for sense names
            morphCache = cache;
            cache.put(str, this);
        }
        if((testNum != null) || (LexiconUtil.senseNameStringp(str)) ||
                (LexiconUtil.compoundStringp(str))) {
            index = Lexicon.NODISKINDEX;
            wordEntry = new WordEntry(this.lexicon, this);
        } else {
            wordEntry = null;
        }
    }

    /* pmartin 24Aug99 .. these need to be integrated to makeWord  */
    private Word(Lexicon lexicon, Double num) {
        this(lexicon, num.toString());
        this.markNumWord(num);
    }

    private Word(Lexicon lexicon, Integer num) {
        this(lexicon, num.toString());
        this.markNumWord(num);
    }

    private Word(Lexicon lexicon, int inum) {
        this(lexicon, Integer.toString(inum));
        this.markNumWord(new Integer(inum));
    }

    public Word markNumWord(Number num) {
        WordEntry we = wordEntry;
        if(we == null) {
            we = new WordEntry(this.lexicon, this);
        }
        we.sensenamep = null;
        we.numericalValue = num;
        we.numeralp = true;
        if(num instanceof Integer) {
            we.noPenaltyCategories =
                    new Category[]{this.lexicon.integerCategory};
        } else {
            we.noPenaltyCategories = new Category[]{this.lexicon.numberCategory};
        }
        this.wordEntry = we;
        return this;
    }

    public boolean listp() {
        return false;
    }

    public boolean wordp() {
        return true;
    }

    public boolean categoryp() {
        return false;
    }

    public boolean phrasep() {
        return false;
    }

    public Number numericalValue() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.numericalValue;
        }
    }

    public WordEntry makeWordEntry() { //pmartin 31aug01
        WordEntry we = getWordEntry();
        if(we == null) {
            we = new WordEntry(this.lexicon, this);
            index = (index & Lexicon.WORDINDEXMASK) | Lexicon.DISKOUTDATED;
            this.wordEntry = we;
            this.lexicon.madeWordEntries++;
        }
        return we;
    }

    public boolean hasWordEntry() { //pmartin 4sep01  // needs more !!
        return (wordEntry != null);
    }

    public boolean ownsWordEntry() {
        // true if one is present or there is one that is compressed
        return (wordEntry != null || bbuf != null || (index &
                Lexicon.MARKEDINDEX) == 0);
    }

    public WordEntry getWordEntry() { //pmartin 31aug01
        // only fluff it if it has been there -- no morph
        // pmartin 1aug02 -- use bbuf if non-null, even if there is a WE
        WordEntry we;
        BitBuffer localbb = bbuf;

        if(localbb != null) {
            BitBuffer mybb = new BitBuffer(true, localbb);
            this.lexicon.decodeWord(mybb, (index & Lexicon.WORDINDEXMASK),
                    Lexicon.UNPACKALL);
            bbuf = null;  // order matters when multi-threaded!
            return wordEntry;
        } else if(wordEntry != null) {
            return wordEntry;
        } else if((index & Lexicon.MARKEDINDEX) == 0) {
            we = reloadDiskWord();
            wordEntry = we;
            return we;
        } else {
            return null;
        }
    }

    public WordEntry removeWordEntry() { //pmartin 4sep01
        WordEntry we = wordEntry;
        wordEntry = null;
        return we;
    }

    /*** crush() encodes the word entry of a word if it has one,
     * removes the WordEntry from the word, and attaches the bitbuffer
     * to the word.  If productionFlag is true, the properties of the
     * word are filtered to remove those that are not needed in a
     * production environment.
     */
    public boolean crush() {
        if(!hasWordEntry()) {
            return false;
        }
        if(this.lexicon.productionFlag) {
            filterProps(this.lexicon.productionUnneeded);
        }
//       String idxTags = "";
//       if ((index & NODISKINDEX) != 0) idxTags += "NODISKINDEX ";
//       if ((index & DISKOUTDATED) != 0) idxTags += "DISKOUTDATED ";
//       if ((index & NOWORDENTRY) != 0) idxTags += "NOWORDENTRY ";

//       if (wordstring.equals("whither")) {
//           System.out.println("******* " + idxTags + (WORDINDEXMASK & index) +
//                          " " + printEntryString());
//       }
//       if ((MARKEDINDEX & index) != 0){
//           System.out.println("crush skips " + idxTags + (WORDINDEXMASK & index) +
//                          " " + printEntryString());
//           return false;
//       }
        BitBuffer bb = new BitBuffer();
        bbuf = encode(bb);
        wordEntry = null;
        return true;
    }

    public void filterProps(HashSet badprops) { // hacked for concurrency
        WordEntry we = getWordEntry();
        ConcurrentHashMap props = null;
        boolean changed = false;
        if(we != null) {
            props = we.properties;
        }
        if(props != null) {
            Enumeration temp = props.keys();
            while(temp.hasMoreElements()) {
                Atom key = (Atom) temp.nextElement();
                if(badprops.contains(key)) {
                    changed = true;
                    props.remove(key);
                }
            }
            if(props.isEmpty()) {
                props = null;
            }
            if(changed) {
                we.properties = props;
                this.wordEntry = we;
            }
        }
    }

    public void dirty() { // mark the word no longer the same as its disk record
        index = (index & Lexicon.WORDINDEXMASK) | Lexicon.DISKOUTDATED;
        bbuf = null; //just in case
    }

    protected int assignWordIndex() {
        if((this.lexicon.highWordIndex + 1) >= this.lexicon.IndexWordLimit) {
            this.lexicon.incFixWordIndex();
        }
        int newIdx = this.lexicon.highWordIndex++;
        this.lexicon.valueIndexTable[newIdx] = this;
        logger.finest("assignWordIndex " + newIdx +
                " for " +
                printEntryString());
        this.lexicon.diskIndexTable[newIdx] = -1;  // no disk for this one
        if(!hasWordEntry()) {
            index = newIdx | Lexicon.NOWORDENTRY;
        } else {
            index = newIdx;
        }
        return newIdx;
    }

    public BitBuffer encode() { // pmartin 17sep01
        return encode(new BitBuffer());
    }

    public BitBuffer encode(BitBuffer bb) { // pmartin 17sep01
        if((index & Lexicon.WORDINDEXMASK) <= 0) {
            assignWordIndex();
        }
        WordEntry we = wordEntry; // any bit bufs are lost
        bb.push(dontPurge);
        if(we == null) {
            bb.push(false);
        } else {
            bb.push(true);
            LexiconUtil.encodeCategories(bb, we.noPenaltyCategories);
            LexiconUtil.encodeCategories(bb, we.penalty1Categories);
            LexiconUtil.encodeCategories(bb, we.penalty2Categories);
            LexiconUtil.encodeCategories(bb, we.penalty3Categories);
            LexiconUtil.encodeCategories(bb, we.rootCategories);
            LexiconUtil.encodeAtoms(bb, we.features);
            LexiconUtil.encodeList(bb, we.capcodes); //cap codes: ic, lc, uc, or list of int
            LexiconUtil.encodeWords(bb, we.roots);
            LexiconUtil.encodeWords(bb, we.prefixes);
            LexiconUtil.encodeWords(bb, we.suffixes);
            LexiconUtil.encodeValues(bb, we.compoundOf); //lists of words this word is a compound of
            LexiconUtil.encodeWords(bb, we.variantOf);
            LexiconUtil.encodeWords(bb, we.ikoParents);
            LexiconUtil.encodeWords(bb, we.iioParents);
            LexiconUtil.encodeAtoms(bb, we.inflectionCodes);
            LexiconUtil.encodeWords(bb, we.subsenses);
            bb.push(we.numeralp);
            LexiconUtil.encodeNumber(bb, we.numericalValue);
            LexiconUtil.encodeValueHash(bb, we.properties);
            if(we.sensenamep == null) {
                bb.push(false);
            } else {
                bb.push(true);
                LexiconUtil.encodeCategoryP(bb, (Category) we.sensenamep[0]);
                LexiconUtil.encodeWordP(bb, (Word) we.sensenamep[1]);
                bb.encodeUTF((String) we.sensenamep[2]);
            }
        }
        return bb;
    }

    public WordEntry reloadDiskWord() {//  31aug01
        int windex = index;  // local copy for multi thread
        WordEntry we = null;
        if((windex & (Lexicon.NODISKINDEX | Lexicon.NOWORDENTRY)) != 0) {
            return null;
        } else if(this.lexicon.binLexRAF == null) {
            System.err.println("No open bin lex random access file");
            System.err.println("Cannot restore " + wordstring + " with index " +
                    windex);
        } else if((windex & Lexicon.MARKEDINDEX) == 0) {
            Word ww = (Word) this.lexicon.reloadBinObj(windex,
                    this.lexicon.binLexRAF);
            we = ww.wordEntry;
        } else {
            windex = (windex & Lexicon.WORDINDEXMASK) | Lexicon.DISKOUTDATED;
            we = new WordEntry(this.lexicon, this);
            index = windex;
            this.wordEntry = we;
        }
        return we;
    }

    public void setNumericalValue(int numval) {//pmartin 23dec99 for words with class val
        setNumericalValue(new Integer(numval));
    }

    public void setNumericalValue(double numval) {
        setNumericalValue(new Double(numval));
    }

    public void setNumericalValue(float numval) {
        setNumericalValue(new Float(numval));
    }

    public void setNumericalValue(long numval) {
        setNumericalValue(new Long(numval));
    }

    public void setNumericalValue(Number numval) {
        //pmartin 23dec99 for words with class val
        // pmartin 19apr00 added redundancy test
        WordEntry we = makeWordEntry();
        if(we.numeralp) {
            if(numval.equals(we.numericalValue)) {
                /* empty */
                // System.out.println("redundant attempt to set numericalValue of "+
                //this.toString());
            } else {
                logger.finest("Warning: denied attempt to change value of " +
                        "a numeral number ->" +
                        this.wordstring + " to " + numval.toString());
            }
        } else {
            dirty();
            we.numericalValue = numval;
        }
    }

    public Number findNumericalValue() { //  pmartin 22 dec 99
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Word[] allAncestors;
        Number numv = we.numericalValue;

        if(numv != null) {
            return numv;
        } else if(we.numeralp) {
            logger.finest("*****Broken word " + this.
                    toString() +
                    " is numeralp but has no numericalValue");
            return null;
        } else if((allAncestors = getAllNonPersonParents()) != null) {
            //  System.out.println("  chasing roots " +
            //                      printStringArray(allAncestors));
            for(int i = 0; i < allAncestors.length; i++) {
                Word root = allAncestors[i];
                //System.out.println("  inspecting root " + root.toString());
                if((numv = root.numericalValue()) != null) {
                    // System.out.println(" found numval " + numv);
                    break;
                }
            }
        }
        dirty();
        we.numericalValue = numv;
        return numv;
    }

    public Word normalizeIfNumber() { //pmartin 26apr01
        // returns a word for the numeric value if this has one
        WordEntry we = getWordEntry();
        if(we == null) {
            return this;
        }
        Number nv = we.numericalValue;
        if((nv == null) || (we.numeralp)) {
            return this;
        } else {
            return this.lexicon.makeWord(nv);
        }
    }

    public boolean generalNumberp() { // added PMartin for LispATN needs .. 2 July 99
        // only returns correctly if findNumericalValue has worked on word.
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return (we.numericalValue != null);
        }
    }

    public boolean numeralp() { // added PMartin for LispATN needs .. 9 Aug 99
        /*** true if the word has a string representation of digits
        (and decimals) (and possible exponent notation)
         */
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return we.numeralp;
        }
    }

    public boolean integerWordp() {
        // added PMartin for LispATN needs .. 13Apr00
        /*** true if the word has its numerical value stored as an Integer */
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        }
        Number nv = we.numericalValue;
        return ((nv != null) && (nv instanceof Integer));
    }

    public Number getNumericalFirstVal(Atom propTag) {
        // pmartin 17may01 for morphology uses
        Value tv = this.getdict(propTag);
        if(tv == null) {
            return null;
        } else if((tv.wordp()) || (tv instanceof Atom)) // category too
        {
            return tv.numericalValue();
        } else if(tv.listp()) {
            return ((List) tv).elementAt(0).numericalValue();
        } else {
            return null;
        }
    }

    public Word makeKnownWord() { //force recursive installation
        if(Lexicon.authorFlag && Lexicon.wordTrace) {
            logger.finest("Lexicon: makeKnownWord " +
                    printEntryString());
        }
        Word knownWord = this.lexicon.getWord(this.wordstring);
        if(knownWord != null) {
            boolean same = (knownWord == this);
            if(Lexicon.authorFlag && Lexicon.wordTrace) {
                if(same) {
                    logger.finest(" ** was already in");
                } else {
                    logger.finest("*** being ignored for known " +
                            knownWord.printEntryString());
                }
            }
        } else {
            if(Lexicon.authorFlag && Lexicon.wordTrace) {
                logger.finest(" ** being installed");
            }
            this.morphCache = null; // clear morphCache if there is one // waw 02-18-05
            this.lexicon.installWord(this, this.wordstring);
        }
        if(Lexicon.authorFlag && Lexicon.wordTrace) {
            logger.finest(" and forcing parts known\n");
        }
        makeKnownParts();
        return this;
    }

    public Word makeKnownWordInner() {  // pmartin 26sep01
        if(Lexicon.authorFlag && Lexicon.wordTrace) {
            logger.finest("Lexicon: makeKnownWordInner " + printEntryString());
        }
        Word knownWord = this.lexicon.getWord(this.wordstring);
        if(knownWord != null) {
            boolean same = (knownWord == this);
            if(Lexicon.authorFlag && Lexicon.wordTrace) {
                if(same) {
                    logger.finest(" ** was already in\n");
                } else {
                    logger.finest("*** being ignored for known " +
                            knownWord.printEntryString() + "\n");
                }
            }
            return knownWord; //return the already known word
        } else {
            if(Lexicon.authorFlag && Lexicon.wordTrace) {
                logger.finest(" ** being installed\n");
            }
            this.morphCache = null; // clear morphCache if there is one // waw 02-18-05
            this.lexicon.installWord(this, this.wordstring);
            makeKnownParts();
            return this;
        }
    }

    public Word[] makeKnownWords(Word[] wds) {
        if(wds != null) {
            for(int i = 0; i < wds.length; i++) {
                wds[i] = wds[i].makeKnownWordInner();
            }
        }
        return wds;
    }

    public List makeKnownWordsList(List wdslst) {
        // pm 30mar04 added recursion for nested word lists
        Value wle;
        if((wdslst != null) && (wdslst.length() > 0)) {
            for(int i = 0; i < wdslst.contents.length; i++) {
                wle = wdslst.contents[i];
                if(wle == null) {
                } else if(wle.wordp()) {
                    wdslst.contents[i] = ((Word) wle).makeKnownWordInner();
                } else if(wle.listp()) {
                    wdslst.contents[i] = makeKnownWordsList((List) wle);
                } else {
                    logger.finest("warning -- discarding element num " + i + " of wdlist " + wdslst.toString() +
                            " is not a word.");
                }
            }
        }
        return wdslst;
    }

    public Word makeKnownParts() { // make scratch words inside it real
        /// note as of 11dec01 doesn't do c-structs internals!
        //
        WordEntry we = getWordEntry();
        if(we == null) {
            return this;  // nothing to fixup
        }
        dirty(); // we will make changes
        we.roots = makeKnownWords(we.roots);
        we.prefixes = makeKnownWords(we.prefixes);
        we.suffixes = makeKnownWords(we.suffixes);
        if(we.compoundOf != null) {
            for(int i = 0; i < we.compoundOf.length; i++) {
                we.compoundOf[i] = makeKnownWordsList((List) we.compoundOf[i]);
            }
        }
        we.variantOf = makeKnownWords(we.variantOf);
        we.ikoParents = makeKnownWords(we.ikoParents);
        we.iioParents = makeKnownWords(we.iioParents);
        we.subsenses = makeKnownWords(we.subsenses);
        if(we.sensenamep != null) {
            we.sensenamep[1] =
                    ((Word) we.sensenamep[1]).makeKnownWordInner();
        }

        if((we.properties != null) && (we.properties.size() > 0)) {
            ConcurrentHashMap oldHash = we.properties;
            Enumeration temp = oldHash.keys();
            ConcurrentHashMap newProps =
                    new ConcurrentHashMap(oldHash.size());
            while(temp.hasMoreElements()) {
                Atom key = (Atom) temp.nextElement();
                Value val = (Value) oldHash.get(key);
                if((val != null) && this.lexicon.isWordProp(key.wordstring)) {
                    // System.out.println("debugging makeKnownParts: key = " +
                    //                      key.toString() + " and val is " +
                    //                      val.toString());

                    if(val.wordp()) {
                        val = ((Word) val).makeKnownWordInner();
                    } else if(val.listp()) {
                        val = makeKnownWordsList((List) val);
                    }
                }
                newProps.put(key, val);
            }
            we.properties = newProps;
        }
        return this;
    }

    public Word mergeWord(Word other) { //pmartin 5dec01
        // copy in stuff from other but keep non-conflicing props
        // use these calls to finesse all compression and paging issues
        //System.out.println("mergeWord called: this= " + printEntryString() +
        //                          "\n other= " + other.printEntryString());
        WordEntry we = makeWordEntry();
        WordEntry owe = other.getWordEntry();
        if(owe == null) {
            return this;  // nothing to merge
        }
        dontPurge = dontPurge || other.dontPurge;
        dirty(); // we will make changes
        Category[] newCats;
        we.noPenaltyCategories =
                LexiconUtil.mergeArrays(we.noPenaltyCategories,
                owe.noPenaltyCategories);
        HashSet done = new HashSet();
        LexiconUtil.hashArray(done, we.noPenaltyCategories);
        newCats =
                LexiconUtil.mergeExcluding(done, we.penalty1Categories,
                owe.penalty1Categories);
        LexiconUtil.hashArray(done, newCats);
        we.penalty1Categories = newCats;
        newCats =
                LexiconUtil.mergeExcluding(done, we.penalty2Categories,
                owe.penalty2Categories);
        LexiconUtil.hashArray(done, newCats);
        we.penalty2Categories = newCats;
        newCats =
                LexiconUtil.mergeExcluding(done, we.penalty3Categories,
                owe.penalty3Categories);
        LexiconUtil.hashArray(done, newCats);
        we.penalty3Categories = newCats;
        we.rootCategories = LexiconUtil.mergeArrays(we.rootCategories,
                owe.rootCategories);
        we.features = LexiconUtil.mergeArrays(we.features, owe.features);
        we.capcodes = this.lexicon.mergeLists(we.capcodes, owe.capcodes);
        we.roots = LexiconUtil.mergeArrays(we.roots, owe.roots);
        we.prefixes = LexiconUtil.mergeArrays(we.prefixes, owe.prefixes);
        we.suffixes = LexiconUtil.mergeArrays(we.suffixes, owe.suffixes);
        we.compoundOf = LexiconUtil.mergeArrays(we.compoundOf, owe.compoundOf);
        we.variantOf = LexiconUtil.mergeArrays(we.variantOf, owe.variantOf);
        we.ikoParents = LexiconUtil.mergeArrays(we.ikoParents, owe.ikoParents);
        we.iioParents = LexiconUtil.mergeArrays(we.iioParents, owe.iioParents);
        we.inflectionCodes = LexiconUtil.mergeArrays(we.inflectionCodes,
                owe.inflectionCodes);
        we.subsenses = LexiconUtil.mergeArrays(we.subsenses, owe.subsenses);
        if(owe.properties != null) {
            Enumeration temp = owe.properties.keys();
            while(temp.hasMoreElements()) {
                Atom key = (Atom) temp.nextElement();
                Value othVal = (Value) owe.properties.get(key);
                Value newVal = (Value) we.properties.get(key);
                if(newVal == null) {
                    we.putdict(key, othVal);
                } else {
                    we.putdict(key, this.lexicon.mergeValues(newVal, othVal));
                }
            }
        }
        if(owe.numeralp) {
            we.numeralp = owe.numeralp;
            we.numericalValue = owe.numericalValue;
        }
        if(owe.sensenamep != null) {
            we.sensenamep = owe.sensenamep;
        }

        return this;
    }

    public boolean loopingProperty(Word vword) {
        /** used to test properties that form a kind of variant.
         * true if this word is a senseword and is already iko vword
         */
        if(vword == null) {
            return false;
        }
        if(vword == this) {
            return true; // it actually happens!
        }
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        }
        if(vword.wordOrSensewordMemb(we.ikoParents) ||
                vword.wordOrSensewordMemb(we.subsenses) ||
                vword.wordOrSensewordMemb(getAbbrevs()) ||
                vword.wordOrSensewordMemb(getMisspellings()) ||
                vword.wordOrSensewordMemb(getNicknames()) ||
                vword.wordOrSensewordMemb(getVariants())) {
            return true;
        }
        if(we.sensenamep == null) {
            return false;
        }
        Word sw = getSenseWord();
        if(vword == sw) {
            return true;
        }
        if(vword.wordOrSensewordMemb(sw.variantParents()) ||
                vword.wordOrSensewordMemb(sw.getAbbrevOf()) ||
                vword.wordOrSensewordMemb(sw.getCutVarOf()) ||
                vword.wordOrSensewordMemb(sw.getMisspellingOf()) ||
                vword.wordOrSensewordMemb(sw.getNicknameOf())) {
            return true;
        }
        return false;
    }

    public boolean wordOrSensewordMemb(Word[] ups) {
        if(ups != null) {
            if(LexiconUtil.isMembOfArray(this, ups)) {
                return true;
            }
            for(int i = 0; i < ups.length; i++) {
                if(ups[i].sensenamep()) {
                    if(this == ups[i].getSenseWord()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // these accessors need to be reviewed for protection issues..
    //  pmartin 9may01
    public List getCapcodes() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.capcodes;
        }
    }

    public Value[] getCompoundOf() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.compoundOf;
        }
    }

    public void setCompoundOf(Value[] cwordlists) { // pmartin 16july01
        // keeps argument array so send it a final copy
        WordEntry we = makeWordEntry();
        dirty();
        we.compoundOf = cwordlists;
    }

    public void addCompoundOf(Word[] cwords) { // pmartin 24july01
        // copies array so arg array can be reused;
        addCompoundOf(this.lexicon.makeList(cwords));
    }

    public void addCompoundOf(List cwlist) { // pmartin 24july01
        WordEntry we = makeWordEntry();
        boolean dirtp = we.addCompoundOf(cwlist);
        if(dirtp) {
            this.wordEntry = we;
            dirty();
        }
    }

    public Word[] getIioParents() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.iioParents;
        }
    }

    public Word[] addIioParent(Word iioParent) { //pmartin 18july01
        WordEntry we = makeWordEntry();
        Word[] oldIios = we.iioParents;
        Word[] newIios = LexiconUtil.addToArray(oldIios, iioParent);
        if(newIios != oldIios) {
            dirty();
            we.iioParents = newIios;
        }
        return newIios;
    }

    public Word[] addIioParents(Word[] newIioParents) { //pmartin 18july01
        WordEntry we = makeWordEntry();
        Word[] oldIios = we.iioParents;
        Word[] newIios = LexiconUtil.mergeArrays(oldIios, newIioParents);
        if(newIios != oldIios) {
            dirty();
            we.iioParents = newIios;
        }
        return newIios;
    }

    public Atom[] getInflectionCodes() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.inflectionCodes;
        }
    }

    public Atom[] addInflectionCode(Atom newCode) { //pmartin 17july01
        WordEntry we = makeWordEntry();
        Atom[] oldInflCodes = we.inflectionCodes;
        if(LexiconUtil.memb(newCode, oldInflCodes) >= 0) {
            return oldInflCodes;
        } else {
            int oldLen = 0;
            if(oldInflCodes != null) {
                oldLen = oldInflCodes.length;
            }
            Atom[] newInflCodes = new Atom[oldLen + 1];
            for(int i = 0; i < oldLen; i++) {
                newInflCodes[i] = oldInflCodes[i];
            }
            newInflCodes[oldLen] = newCode;
            dirty();
            we.inflectionCodes = newInflCodes;
            return newInflCodes;
        }
    }

    public Atom[] addInflectionCodes(Atom[] newCodes) {
        WordEntry we = makeWordEntry();
        Atom[] oldCodes = we.inflectionCodes;
        HashSet hcode = new HashSet();
        for(int i = 0; i < oldCodes.length; i++) {
            hcode.add(oldCodes[i]);
        }
        for(int i = 0; i < newCodes.length; i++) {
            hcode.add(newCodes[i]);
        }
        int newC = hcode.size();
        Atom[] newInfl = new Atom[newC];
        newInfl = (Atom[]) (hcode.toArray(newInfl));
        dirty();
        we.inflectionCodes = newInfl;
        return newInfl;
    }

    public Word[] getIkoParents() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.ikoParents;
        }
    }

    public Word removeIkoParent(Word ikoKill) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Word[] oldIkos = we.ikoParents;
        if(oldIkos == null || !(ikoKill.isInArray(oldIkos))) {
            return null;
        }
        int isiz = oldIkos.length;
        Word[] newIkos = null;
        if(isiz > 1) {
            newIkos = new Word[isiz - 1];
            int j = 0;
            for(int i = 0; i < isiz; i++) {
                if(oldIkos[i] != ikoKill) {
                    newIkos[j++] = oldIkos[i];
                }
            }
        }
        dirty();
        we.ikoParents = newIkos;
        return ikoKill;
    }

    public Word[] setIkoParent(Word p) {
        WordEntry we = makeWordEntry();
        Word[] ikos = new Word[]{p};
        dirty();
        we.ikoParents = ikos;
        return ikos;
    }

    public Word[] setIkoParents(Word[] ikos) {
        WordEntry we = makeWordEntry();
        dirty();
        we.ikoParents = ikos;
        return ikos;
    }

    public Word[] addIkoParent(Word ikoParent) { //pmartin 16july01
        WordEntry we = makeWordEntry();
        Word[] oldIkos = we.ikoParents;
        Word[] newIkos = LexiconUtil.addToArray(oldIkos, ikoParent);
        if(oldIkos == newIkos) {
            return oldIkos;
        } else {
            dirty();
            we.ikoParents = newIkos;
            return newIkos;
        }
    }

    public Word[] addIkoParents(Word[] newIkoParents) {
        WordEntry we = makeWordEntry();
        boolean dirtp = we.addIkoParents(newIkoParents);
        if(dirtp) {
            this.wordEntry = we;
            dirty();
        }
        return we.ikoParents;
    }

    public Word[] addIkoParents(List newParents) {
        if((newParents == null) || (newParents.length() == 0)) {
            return null;
        }
        Value[] newvs = newParents.contents;
        Word[] newps = new Word[newvs.length];
        for(int i = 0; i < newvs.length; i++) {
            newps[i] = (Word) newvs[i];
        }
        return addIkoParents(newps);
    }

    public Word getRoot() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            Word[] wroots = we.roots;
            if((wroots != null) && (wroots.length > 0)) {
                return wroots[0];
            } else {
                return null;
            }
        }
    }

    public Word[] getRoots() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.roots;
        }
    }

    public Word[] getRoots(Category cat) { //pmartin 16may01
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Word[] wroots = we.roots;
        if(wroots == null) {
            return null; //waw 31may01
        }
        int wrlen = wroots.length;
        if(wrlen == 0) {
            return null;
        }
        HashSet catRoots = new HashSet(wrlen);
        for(int i = 0; i < wrlen; i++) {
            if(wroots[i].isFormOfCat(cat)) {
                catRoots.add(wroots[i]);
            }
        }
        int crlen = catRoots.size();
        if(crlen == 0) {
            return null;
        } else {
            Word[] crArray = new Word[crlen];
            crArray = (Word[]) (catRoots.toArray(crArray));
            return crArray;
        }
    }

    public Word[] addRoots(List newRootsList) { //pmartin 8mar02
        Value[] vw = newRootsList.contents;
        int isiz = vw.length;
        if(isiz < 1) {
            return null;
        }
        Word[] ww = new Word[isiz];
        for(int i = 0; i < isiz; i++) {
            ww[i] = (Word) vw[i];
        }
        return addRoots(ww);
    }

    public Word[] addRoots(Word[] newRoots) { // pmartin 17july01
        WordEntry we = makeWordEntry();
        Word[] wroots = we.roots;
        Word[] nwroots = LexiconUtil.mergeArrays(wroots, newRoots);
        if(nwroots == wroots) {
            return nwroots;
        } else {
            dirty();
            we.roots = nwroots;
            return nwroots;
        }
    }

    public Word[] removeRoots(Word[] badRoots) { // pmartin 25mar04
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Word[] wroots = we.roots;
        if(wroots == null || wroots.length == 0) {
            return null;
        }
        if(badRoots == null || badRoots.length == 0) {
            return wroots;
        }
        boolean changed = false;
        HashSet hroots = new HashSet(wroots.length);
        for(int i = 0; i < wroots.length; i++) {
            hroots.add(wroots[i]);
        }
        for(int i = 0; i < badRoots.length; i++) {
            if(hroots.contains(badRoots[i])) {
                hroots.remove(badRoots[i]);
                changed = true;
            }
        }
        if(!changed) {
            return wroots;
        }
        Word[] newRoots = new Word[hroots.size()];
        newRoots = (Word[]) (hroots.toArray(newRoots));
        dirty();
        we.roots = newRoots;
        return newRoots;
    }

    public Word[] addRoot(Word newRoot) { // pmartin 17july01
        WordEntry we = makeWordEntry();
        Word[] wroots = we.roots;
        Word[] nwroots = LexiconUtil.addToArray(wroots, newRoot);
        if(nwroots == wroots) {
            return nwroots;
        } else {
            dirty();
            we.roots = nwroots;
            return nwroots;
        }
    }

    public Word[] setRoots(Word[] newrts) {
        WordEntry we = makeWordEntry();
        we.roots = newrts;
        dirty();
        return newrts;
    }

    public Word[] getPrefixes() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.prefixes;
        }
    }

    public Word[] addPrefix(Word newPref) { //pmartin 01apr04
        return addPrefixes(new Word[]{newPref});
    }

    public Word[] addPrefixes(Word[] newPrefs) { //pmartin 17july01
        WordEntry we = makeWordEntry();
        Word[] wprefs = we.prefixes;
        Word[] prefs = LexiconUtil.mergeArrays(wprefs, newPrefs);
        if(prefs != wprefs) {
            dirty();
            we.prefixes = prefs;
        }
        return prefs;
    }

    public Word[] getSuffixes() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.suffixes;
        }
    }

    public Word[] addSuffixes(Word[] newSuffs) { //pmartin 17july01
        WordEntry we = makeWordEntry();
        Word[] wsuffs = we.suffixes;
        Word[] suffs = LexiconUtil.mergeArrays(wsuffs, newSuffs);
        if(suffs != wsuffs) {
            dirty();
            we.suffixes = suffs;
        }
        return suffs;
    }

    public Word[] getSubsenses() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.subsenses;
        }
    }

    public Word[] addSubsenses(Word[] newSubs) { //pmartin 17july01
        WordEntry we = makeWordEntry();
        Word[] wsubs = we.subsenses;
        Word[] subs = LexiconUtil.mergeArrays(wsubs, newSubs);
        if(subs != wsubs) {
            dirty();
            we.subsenses = subs;
        }
        return subs;
    }

    public Word[] addSubsense(Word newSub) { //waw 19july01
        WordEntry we = makeWordEntry();
        Word[] wsubs = we.subsenses;
        Word[] wsubs2 = LexiconUtil.addToArray(wsubs, newSub);
        if(wsubs != wsubs2) {
            dirty();
            we.subsenses = wsubs2;
        }
        return wsubs2;
    }

    public Word[] getCutVarOf() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        return LexiconUtil.valueWords(we.getdict(this.lexicon.cutVarOfAtom));
    }

    public Word[] addCutVarOf(Word newa) {
        WordEntry we = makeWordEntry();
        Value old = we.getdict(this.lexicon.cutVarOfAtom);
        Word[] newAbbrvs;
        if(old == null) {
            newAbbrvs = new Word[]{newa};
        } else {
            newAbbrvs =
                    LexiconUtil.addToArray(LexiconUtil.valueWords(old), newa);
        }
        we.putdict(this.lexicon.cutVarOfAtom, new List(this.lexicon, newAbbrvs));
        return newAbbrvs;
    }

    public Word[] getVariantOf() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.variantOf;
        }
    }

    public Word[] addVariantOf(Word newv) {
        //	System.out.println(this.wordstring + " addVariantOf(" + newv.wordstring + ")");
        WordEntry we = makeWordEntry();
        Word[] allVos = LexiconUtil.addToArray(we.variantOf, newv);
        we.variantOf = allVos;
        return allVos;
    }

    public Word[] addVariantOf(Word[] newvs) {
        //System.out.println(this + " addVariantOf(" + newvs + ")");
        WordEntry we = makeWordEntry();
        Word[] allVos = LexiconUtil.mergeArrays(we.variantOf, newvs);
        we.variantOf = allVos;
        return allVos;
    }

    public Word[] getVariants() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        return LexiconUtil.valueWords(we.getdict(this.lexicon.variantsAtom));
    }

    public Word[] addVariants(Word newv) {
        WordEntry we = makeWordEntry();
        Word[] newVars = LexiconUtil.addToArray(this.getVariants(), newv);
        we.putdict(this.lexicon.variantsAtom, new List(this.lexicon, newVars));
        return newVars;
    }

    public Word[] getAllVariants() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        return LexiconUtil.valueWords(we.getdict(this.lexicon.allVariantsAtom));
    }

    public Word[] addAllVariants(Word newv) {
        WordEntry we = makeWordEntry();
        Word[] newVars = LexiconUtil.addToArray(this.getAllVariants(), newv);
        we.putdict(this.lexicon.allVariantsAtom, new List(this.lexicon, newVars));
        return newVars;
    }

    public boolean variantsLinked() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return (we.getdict(this.lexicon.variantsLinkedAtom) ==
                    this.lexicon.trueAtom);
        }
    }

    public Word[] getAbbrevOf() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        return LexiconUtil.valueWords(we.getdict(this.lexicon.abbrevAtom));
    }

    public Word[] getAbbrevs() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        return LexiconUtil.valueWords(we.getdict(this.lexicon.abbrevsAtom));
    }

    public Word[] addAbbrevOf(Word newa) {
        WordEntry we = makeWordEntry();
        Value old = we.getdict(this.lexicon.abbrevAtom);
        Word[] newAbbrvs;
        if(old == null) {
            newAbbrvs = new Word[]{newa};
        } else {
            newAbbrvs =
                    LexiconUtil.addToArray(LexiconUtil.valueWords(old), newa);
        }
        we.putdict(this.lexicon.abbrevAtom, new List(this.lexicon, newAbbrvs));
        return newAbbrvs;
    }

    public Word[] addAbbrevs(Word newa) {
        WordEntry we = makeWordEntry();
        Word[] newAbbrvs = LexiconUtil.addToArray(getAbbrevs(), newa);
        we.putdict(this.lexicon.abbrevsAtom, new List(this.lexicon, newAbbrvs));
        return newAbbrvs;
    }

    public Word[] getNicknameOf() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        return LexiconUtil.valueWords(we.getdict(this.lexicon.nicknameOfAtom));
    }

    public Word[] getNicknames() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        return LexiconUtil.valueWords(we.getdict(this.lexicon.nicknamesAtom));
    }

    public Word[] addNicknameOf(Word newa) {
        WordEntry we = makeWordEntry();
        Value old = we.getdict(this.lexicon.nicknameOfAtom);
        Word[] newAbbrvs;
        if(old == null) {
            newAbbrvs = new Word[]{newa};
        } else {
            newAbbrvs =
                    LexiconUtil.addToArray(LexiconUtil.valueWords(old), newa);
        }
        we.putdict(this.lexicon.nicknameOfAtom,
                new List(this.lexicon, newAbbrvs));
        return newAbbrvs;
    }

    public Word[] addNicknames(Word newa) {
        WordEntry we = makeWordEntry();
        Word[] newAbbrvs = LexiconUtil.addToArray(getNicknames(), newa);
        we.putdict(this.lexicon.nicknamesAtom, new List(this.lexicon, newAbbrvs));
        return newAbbrvs;
    }

    protected Word makeVariantSenseName(Word vof, Category fcat) {
//        String fcatStr = "nullCategory";
//        if (fcat != null) fcatStr = fcat.printString();
//        System.out.println(this.printString() +
//                           ".makeVariantSenseName(" + vof.printString() +
//                           ", " + fcatStr + ")");
        String tail, tailtail;
        Category vofcat;
        if(vof.sensenamep()) {
            vofcat = vof.getSenseCat();
            tail = vof.getSenseWord().wordstring;
            tailtail = vof.getSenseTailString();
            if((tailtail != null) && (tailtail.length() > 0)) {
                tail += tailtail;
            }
        } else {
            vofcat = fcat;
            tail = vof.wordstring;
        }
        Word newSenseName = this.getOrMakeVariantSenseName(vofcat, vof, tail);
//        System.out.println("--- makeVSN returning: " +
//                           newSenseName.printEntryString());
        return newSenseName;
    }

    protected Word[] makeVariantSenseWords(Word abrvof) {
        Word[] awsns, subs;
        if(abrvof.sensenamep()) {
            awsns = new Word[]{makeVariantSenseName(abrvof, null)};
        } else if((subs = abrvof.getSubsenses()) != null) {
            awsns = new Word[subs.length];
            for(int i = 0; i < subs.length; i++) {
                awsns[i] = makeVariantSenseName(subs[i], null);
            }
        } else {
            Category[] acats = abrvof.getNonFeatCats();
            if(acats == null || acats.length == 0) {
                acats = new Category[]{this.lexicon.wordCategory};
            }
            awsns = new Word[acats.length];
            for(int i = 0; i < acats.length; i++) {
                Word fullw;
                if(acats.length == 1) {
                    fullw = abrvof;
                } else {
                    fullw = abrvof.makeSenseName(acats[i]);
                }
                awsns[i] = makeVariantSenseName(fullw, acats[i]);
            }
        }
        return awsns;
    }

    public Word[] getInverseVariantLinks() {
        if(getWordEntry() == null) {
            return null;
        }
        HashSet av = null;
        boolean empty = true;
        Word[] avars = getAllVariants();
        if(avars != null) {
            empty = false;
        }
        Word[] ivars = getVariants();
        if(ivars != null) {
            empty = false;
        }
        Word[] msps = getMisspellings();
        if(msps != null) {
            empty = false;
        }
        Word[] nkns = getNicknames();
        if(nkns != null) {
            empty = false;
        }
        Word[] abrvs = getAbbrevs();
        if(abrvs != null) {
            empty = false;
        }
        if(empty) {
            return null;
        }
        av = new HashSet();
        if(avars != null) {
            for(int i = 0; i < avars.length; i++) {
                av.add(avars[i]);
            }
        }
        if(ivars != null) {
            for(int i = 0; i < ivars.length; i++) {
                av.add(ivars[i]);
            }
        }
        if(msps != null) {
            for(int i = 0; i < msps.length; i++) {
                av.add(msps[i]);
            }
        }
        if(nkns != null) {
            for(int i = 0; i < nkns.length; i++) {
                av.add(nkns[i]);
            }
        }
        if(abrvs != null) {
            for(int i = 0; i < abrvs.length; i++) {
                av.add(abrvs[i]);
            }
        }
        Word[] iwords = new Word[av.size()];
        av.toArray(iwords);
        return iwords;
    }

    public void makeVariantLinks() {
        WordEntry we = getWordEntry();
        if(we == null || we.getdict(this.lexicon.variantsLinkedAtom) ==
                this.lexicon.trueAtom) {
            return;
        }
        Word[] vsw;
        Word[] abof = getAbbrevOf();
        if(abof != null) {
            for(int i = 0; i < abof.length; i++) {
                vsw = makeVariantSenseWords(abof[i]);
                for(int j = 0; j < vsw.length; j++) {
                    vsw[j].addIioParent(this.lexicon.abbreviationWord);
                }
            }
        }
        abof = getNicknameOf();
        if(abof != null) {
            for(int i = 0; i < abof.length; i++) {
                vsw = makeVariantSenseWords(abof[i]);
                for(int j = 0; j < vsw.length; j++) {
                    vsw[j].addIioParent(this.lexicon.nicknameWord);
                }
            }
        }
        Word[] varof = getVariantOf();
        if(varof != null) {
            for(int i = 0; i < varof.length; i++) {
                makeVariantSenseWords(varof[i]);
            }
        }

        Word[] mspof = getMisspellingOf();
        if(mspof != null) {
            for(int i = 0; i < mspof.length; i++) {
                vsw = makeVariantSenseWords(mspof[i]);
                for(int j = 0; j < vsw.length; j++) {
                    vsw[j].addIioParent(this.lexicon.misspellingWord);
                }
            }
        }
        we.putdict(this.lexicon.variantsLinkedAtom, this.lexicon.trueAtom);
    }

    public Word[] getEntails() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        return LexiconUtil.valueWords(we.getdict(this.lexicon.entailsAtom));
    }

    public Word[] getEntailedBy() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        return LexiconUtil.valueWords(we.getdict(this.lexicon.entailedbyAtom));
    }

    public Word[] addEntails(Word newe) {
        WordEntry we = makeWordEntry();
        Word[] newEntails = LexiconUtil.addToArray(getEntails(), newe);
        we.putdict(this.lexicon.entailsAtom, new List(this.lexicon, newEntails));
        Word[] neweb = LexiconUtil.addToArray(newe.getEntailedBy(), this);
        newe.makeWordEntry().putdict(this.lexicon.entailedbyAtom, new List(
                this.lexicon, neweb));
        return newEntails;
    }

    public boolean hasFalseRoot(Word frt) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        }
        Value frts = getdict(this.lexicon.falseRootAtom);
        if(frts == null) {
            return false;
        }
        if(frts instanceof Word) {
            return (frts == frt);
        }
        if(frts instanceof List) {
            return ((List) frts).hasMember(frt);
        }
        return false;
    }

    public Word[] getFalseRoots() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        return LexiconUtil.valueWords(we.getdict(this.lexicon.falseRootAtom));
    }

    public Word[] addFalseRoot(Word newfrt) {
        WordEntry we = makeWordEntry();
        Word[] newFalseRoots = LexiconUtil.addToArray(getFalseRoots(), newfrt);
        we.putdict(this.lexicon.falseRootAtom, new List(this.lexicon,
                newFalseRoots));
        return newFalseRoots;
    }

    public Word[] applyFalseRoot(Word newfrt) {// pm 25mar04
        if(Lexicon.authorFlag && Lexicon.debug) {
            logger.finest("Lexicon: addFalseRoot " +
                    newfrt.wordstring +
                    " to " + this.printEntryString());
        }
        Word[] falsies = addFalseRoot(newfrt);
        return removeRoots(falsies);
    }

    public Word[] getMisspellingOf() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return LexiconUtil.valueWords(we.getdict(
                    this.lexicon.misspellingOfAtom));
        }
    }

    public Word[] getMisspellings() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return LexiconUtil.valueWords(we.getdict(
                    this.lexicon.misspellingsAtom));
        }
    }

    public Word[] addMisspellingOf(Word newm) {
        WordEntry we = makeWordEntry();
        Word[] newMsplofs = LexiconUtil.addToArray(getMisspellingOf(), newm);
        we.putdict(this.lexicon.misspellingOfAtom, new List(this.lexicon,
                newMsplofs));
        return newMsplofs;
    }

    public Word[] addMisspellings(Word newm) {
        WordEntry we = makeWordEntry();
        Word[] newMspls = LexiconUtil.addToArray(getMisspellings(), newm);
        we.putdict(this.lexicon.misspellingsAtom, new List(this.lexicon,
                newMspls));
        return newMspls;
    }

    public Word[] getSenseParents() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.getSenseParents();
        }
    }

    public boolean punctuationp() { //pmartin 25Aug99
        //  boolean isPunctCat = this.isFormOfCat(.punctCategory);
        //  boolean ikoPunct = this.isKindOf(makeWord("punctuation_mark"));
        //   return (isPunctCat || ikoPunct);
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return we.isFormOfCat(this.lexicon.punctCategory);
        }
    }


    /* the isKnownWord(string) method only tests whether a
    word exists that has the given string as its string.
    The isKnownWord method for words should determine from
    some property of the word whether it was added automatically
    or came (perhaps by morphing) from a known dictionary entry*/
    public boolean isKnownWord() { //pmartin 25Aug99
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return (!(we.isFormOfCat(this.lexicon.unknownCategory)) &&
                    !this.guessedWordp());
        }
    }

    public void markGuessedWord() { //pmartin 5Jan00
        markGuessedWord(this.lexicon.unknownCategory);
    }

    public boolean guessedWordp() {
        return authorp(this.lexicon.guessedAtom);
    }

    public boolean guessedNamep() { //pmartin 21dec00
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return (we.getdict(this.lexicon.guessedNameAtom) != null);
        }
    }

    public List guessedNameIndex() { //pmartin 21dec00
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return (List) (we.getdict(this.lexicon.guessedNameAtom));
        }
    }

    public void markGuessedName() {
        markGuessedName("unknownDoc", 0);
    }

    public void markGuessedName(String file, int indx) { //pmartin 6feb01
        if(file == null) {
            file = "unknownDoc";
        }
        Atom fatom = this.lexicon.makeAtom(file);
        Atom natom = this.lexicon.makeAtom(indx);
        List srcList = new List(this.lexicon, fatom, natom);
        makeWordEntry().putdict(this.lexicon.guessedNameAtom, srcList);
    }

    public void addGuessedWordMark() {
        makeWordEntry().putdict(this.lexicon.authorAtom,
                this.lexicon.guessedAtom);
    }

    public void addMorphServerWordMark() {
        makeWordEntry().putdict(this.lexicon.authorAtom,
                this.lexicon.morphServerAtom);
    }

    public void markNotYetMorphed() { //pmartin 28jun01 support morphRoot
        makeWordEntry().putdict(this.lexicon.morphStatusAtom,
                this.lexicon.notYetMorphedAtom);
    }

    public void clearNotYetMorphed() {
        makeWordEntry().remdict(this.lexicon.morphStatusAtom);
    }

    public boolean testNotYetMorphed() {// true iff flagged as not yet
        // morphed
        // Morph loops if true return for empty word (a "stub") 19mar04
        WordEntry we = getWordEntry();
        if(we == null) {
            return false; //
        } else {
            return (we.getdict(this.lexicon.morphStatusAtom) ==
                    this.lexicon.notYetMorphedAtom);
        }
    }

    public boolean authorp(Atom testAuth) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        }
        Value auth = we.getdict(this.lexicon.authorAtom);
        return ((testAuth == auth) ||
                ((auth != null) &&
                auth.listp() &&
                ((List) auth).hasMemb(testAuth)));
    }

    public void markTransmitted() { //pmartin 4aug00 for concept Store
        makeWordEntry().putdict(this.lexicon.transmitFlagAtom,
                this.lexicon.trueAtom);
    }

    public boolean transmittedp() { // pmartin 4aug00 for the conceptStore
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return (we.getdict(this.lexicon.transmitFlagAtom) != null);
        }
    }

    public void markGuessedWord(Category guessCat) { //pmartin 5Jan00
        addWordCategory(guessCat, 0);
        addGuessedWordMark();
    }

    public void markDerivedVerb(Word rootv, Atom form, Atom tense) {
        // pmartin 6Jan00
        WordEntry we = makeWordEntry();
        we.roots = new Word[]{rootv};
        we.addWordCategory(this.lexicon.verbCategory, 0);
        addGuessedWordMark();
        we.markDict(this.lexicon.formAtom, form, true);
        if(tense != null) {
            we.markDict(this.lexicon.tenseAtom, tense, true);
        }
        if((form == this.lexicon.pastpartAtom) || (form ==
                this.lexicon.prespartAtom)) {
            we.addWordCategory(this.lexicon.adjCategory, 0);
        }
        we.fixIcatProperties(this, true);
    }

    public void fixIcatProperties(boolean pppast) { //pmartin 9aug01
        /*** spots property combos under number, tense, agr, and form that
         *  should be reflected as icats .. installs the icats at proper
         *  penalty level. pppast set true means that pastpart implies
         *  past (as in morphed, but not copied, props).
         *  Choices derived from WWoods code for
         *  processCatSense in MorphEngFrame as of 8aug01.
         */
        makeWordEntry().fixIcatProperties(this, pppast);
    }

    public void mergeIcat(Category nonIcat, Category icat) {
        //pmartin 10aug01
        int penalty = getCatPenalty(nonIcat);
        if(penalty > 3) {
            penalty = 0;
        }
        addWordCategory(icat, penalty);
    }

    public Lexicon lexicon() {
        return this.lexicon;
    } //the lexicon this is in

    // query interface for morph and grammar rules to ask lexical questions:
    public boolean canBePluralNoun() {
        return isFormOfCat(this.lexicon.nplCategory);
    }

    public boolean canBeSingularNoun() {
        return isFormOfCat(this.lexicon.nsgCategory);
    }

    public boolean hasFeature(Atom feature) { /* to replace check-dict */
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return feature.isInArray(we.features);
        }
    }

    public boolean hasPrefix(Word[] list) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return LexiconUtil.intersectp(list, we.prefixes);
        }
    }

    public boolean hasSuffix(Word[] list) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return LexiconUtil.intersectp(list, we.suffixes);
        }
    }

    public boolean isFormOfCat(Category c) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        }
        return we.isFormOfCat(c);
    }

    public boolean hasCommonCat() { // for word
        boolean hcc = isFormOfCat(this.lexicon.nonnameCats);
        if(Lexicon.authorFlag && Lexicon.debug) {
            logger.finest(wordstring +
                    ".hasCommonCat() " + hcc);
        }
        return hcc;
    }

    public int getCatPenalty(Category c) { //pmartin 17may01
        WordEntry we = getWordEntry();
        if(we == null) {
            return 4; // the bogus penalty flag
        } else {
            return we.getCatPenalty(c);
        }
    }

    /* these calls are for use in accessing the simpler category system of
    the C-based concept store ... pmartin 26jul00  */
    public boolean isFormOfNoun() {
        return isFormOfCat(this.lexicon.nounCategory);
    }

    public boolean isFormOfVerb() {
        return isFormOfCat(this.lexicon.verbCategory);
    }

    public boolean isFormOfAdj() {
        return isFormOfCat(this.lexicon.adjCategory);
    }

    public boolean isFormOfAdv() {
        return isFormOfCat(this.lexicon.advCategory);
    }

    public boolean isHyphenated() {
        return (wordstring.indexOf("-") >= 0);
    }

    public boolean isInstanceOf(Word kind) { /* to replace is-instanceof */
        Word[] iios = getIioParents();
        return ((iios != null) &&
                (iios.length > 0) &&
                LexiconUtil.isMembOfArray(kind, iios));
    }

    public boolean isKindOf(Word parent) { /* to replace is-kindof */
        return parent.subsumesOneOf(getIkoParents());
    }

    public boolean someSenseIsKindOf(Word parent) {// pmartin 15may01
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        }
        if(isKindOf(parent)) {
            return true;  // pmartin 9aug01
        }
        Word[] subs = we.subsenses;
        if(subs == null) {
            return false;
        } else {
            for(int ii = 0; ii < subs.length; ii++) {
                if(subs[ii].isKindOf(parent)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isNonnameFormOfCat(Category c) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return we.isNonnameFormOfCat(c);
        }
    }

    // pmartin additions .. replaces calls to hasNonnameCat(categories)
    // where categories were all known categories of words...  18 Aug 99
    public boolean isNonnameFormOfCat(Category[] cc) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return we.isNonnameFormOfCat(cc);
        }
    }

    public boolean hasMultipleCats() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        }
        int cats = 0;
        if(we.noPenaltyCategories != null) {
            cats += we.noPenaltyCategories.length;
        }
        if(we.penalty1Categories != null) {
            cats += we.penalty1Categories.length;
        }
        if(we.penalty2Categories != null) {
            cats += we.penalty2Categories.length;
        }
        if(we.penalty3Categories != null) {
            cats += we.penalty3Categories.length;
        }
        return (cats > 1);
    }

    public boolean isAmbiguous() {
        WordEntry we = getWordEntry();
        return (((we != null) &&
                (we.subsenses != null) &&
                (we.subsenses.length > 1)) ||
                hasMultipleCats());
    }

    public Category[] getNonFeatCats() {
        return getAllCats(this.lexicon.featCategoriesHash);
    }

    public Category[] getAllCats() {
        return getAllCats(null);
    }

    public Category[] getAllCats(HashSet exclude) {
        // pmartin 5sep99
        /** collects all the categories recorded for a word */
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        HashSet catV = new HashSet(Lexicon.defaultCatListSize);
        Category cat;
        Category[] allCats;
        if(we.noPenaltyCategories != null) {
            for(int i = 0; i < we.noPenaltyCategories.length; i++) {
                cat = we.noPenaltyCategories[i];
                if(exclude == null || !exclude.contains(cat)) {
                    catV.add(cat);
                }
            }
        }
        if(we.penalty1Categories != null) {
            for(int i = 0; i < we.penalty1Categories.length; i++) {
                cat = we.penalty1Categories[i];
                if(exclude == null || !exclude.contains(cat)) {
                    catV.add(cat);
                }
            }
        }
        if(we.penalty2Categories != null) {
            for(int i = 0; i < we.penalty2Categories.length; i++) {
                cat = we.penalty2Categories[i];
                if(exclude == null || !exclude.contains(cat)) {
                    catV.add(cat);
                }
            }
        }
        if(we.penalty3Categories != null) {
            for(int i = 0; i < we.penalty3Categories.length; i++) {
                cat = we.penalty3Categories[i];
                if(exclude == null || !exclude.contains(cat)) {
                    catV.add(cat);
                }
            }
        }
        if(catV.size() == 0) {
            return null;
        }
        allCats = new Category[catV.size()];
        return (Category[]) (catV.toArray(allCats));
    }

    public boolean hasNonnameCat() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return (we.isNonnameFormOfCat(we.noPenaltyCategories) ||
                    we.isNonnameFormOfCat(we.penalty1Categories) ||
                    we.isNonnameFormOfCat(we.penalty2Categories) ||
                    we.isNonnameFormOfCat(we.penalty3Categories));
        }
    }

    public boolean isNonpenaltyFormOfCat(Category c) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return c.subsumesOneOf(we.noPenaltyCategories);
        }
    }

    public boolean isStrictlyProperNoun() { /* to replace is-only-npr */
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return (we.isFormOfCat(this.lexicon.nprCategory) &&
                    !we.isFormOfCat(this.lexicon.nnCategory));
        }
    }

    public boolean isPenaltyFormOfCat(Category c) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return we.isPenaltyFormOfCat(c);
        }
    }

    public boolean isPenaltyFormOfCat(Category c, int penalty) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return we.isPenaltyFormOfCat(c, penalty);
        }
    }

    public boolean isRootOfCat(Category c) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return we.isRootOfCat(c);
        }
    }

    public boolean looksLike(Category c) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        }
        Category[] npc = we.noPenaltyCategories;
        return (npc != null) && (npc.length > 0) && c.subsumesCategory(npc[0]);
    }

    public boolean noPrefix() { /* to replace not-prefix */
        WordEntry we = getWordEntry();
        if(we == null) {
            return true;
        }
        Word[] pr = we.prefixes;
        return (pr == null) || (pr.length == 0);
    }

    public boolean noSuffix() { /* to replace not-suffix */
        WordEntry we = getWordEntry();
        if(we == null) {
            return true;
        }
        Word[] sf = we.suffixes;
        return (sf == null) || (sf.length == 0);
    }

    public boolean polysyllabic() { //temporary definition
        return syllabic();
    }

    public boolean syllabic() { //temporary definition, ok for phrase extract
        return !isFormOfCat(this.lexicon.alphanumCategory);
    }

    public boolean testIcode(Category c, Atom[] codes) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        }
        Atom[] wic = we.inflectionCodes;
        if(!(we.isFormOfCat(c)) || (wic == null)) {
            return false;
        }
        for(int i = 0; i < codes.length; i++) {
            for(int j = 0; j < wic.length; j++) {
                if(codes[i] == wic[j]) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean sensenamep() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return (we.sensenamep != null);
        }
    }

    public Category getSenseCat() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Object[] sen = we.sensenamep;
        if(sen != null) {
            return (Category) sen[0];
        } else {
            return null;
        }
    }

    public Word getSenseWord() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return this;
        }
        Object[] sen = we.sensenamep;
        if(sen != null) {
            return (Word) sen[1];
        } else {
            return this;
        }
    }

    public String getSenseTailString() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Object[] sen = we.sensenamep;
        if(sen != null) {
            return (String) sen[2];
        } else {
            return null;
        }
    }

    public Word makeSenseName(Category c) {
        return this.makeSenseName(c, "");
    }

    public Word makeSenseName(Category c, String tail) {
        Category useCat = c;
        if(useCat == null) {
            useCat = this.lexicon.wordCategory;
        }
        if(c == this.lexicon.nounCategory &&
                isFormOfCat(this.lexicon.nmCategory)) {
            useCat = this.lexicon.ncCategory;
        } else if(c == this.lexicon.verbCategory) {
            if(isFormOfCat(this.lexicon.vtCategory) && !(isFormOfCat(
                    this.lexicon.viCategory))) {
                useCat = this.lexicon.vtCategory;
            } else if(isFormOfCat(this.lexicon.viCategory) && !(isFormOfCat(
                    this.lexicon.vtCategory))) {
                useCat = this.lexicon.viCategory;
            }
        }
        String sname = "!" + useCat.printString() + "/" + wordstring;
        if(tail != null && tail.length() > 0) {
            sname = sname + "/" + tail;
        }
        return this.lexicon.innerMakeWord(sname);
    }

    public Word makeSenseName(Category c, Word parentKind, String inflect) {
        String tail = inflect;
        if(tail == null) {
            tail = "";
        }
        if(tail.length() > 0 && tail.charAt(0) != '+') {
            tail = "+" + tail;
        }
        if(parentKind != null) {
            tail = parentKind.wordstring + tail;
        }
        return makeSenseName(c, tail);
    }

    public Word makeSenseName(Category c, Word parentKind) {
        if(parentKind == null) {
            return makeSenseName(c);
        } else {
            return makeSenseName(c, parentKind, null);
        }
    }

    public Word getOrMakeVariantSenseName(Category c, Word varOf, String tail) {
        /* note that the varOf word is redundant  -- included in tail.
         */
        if(c == null) {
            c = this.lexicon.wordCategory;
        }
        //      System.out.println(this.wordstring + ".getOrMakeVariantSenseName(" +
//                           c.printString() + ", " + varOf.wordstring + ", " +
//                           tail + ")");

        String spelling = wordstring;
        if(sensenamep()) {
            spelling = getSenseWord().wordstring;
        }
        String vsName = "!" + c.printString() + "/" + spelling;
        if(tail != null && tail.length() > 0) {
            vsName += "/" + tail;
        }
        Word vsWord = this.lexicon.getWord(vsName);
        Word exactVarOf = varOf;
        if(vsWord == null) {
            vsWord = this.lexicon.makeWord(vsName);
            exactVarOf = vsWord.removeIkoParent(varOf);
        }
        if(exactVarOf != null) {
            vsWord.addVariantOf(exactVarOf);
            exactVarOf.addAllVariants(vsWord);
        }
//         System.out.println(" --- getOrMake returning " +
//                            vsWord.printEntryString());
        return vsWord;
    }

    public Word makeSenseNameIfNeeded(Category c, Word pKind, String infl) {
        Word sw = this;
        if(isFormOfCat(c) && isAmbiguous()) {
            sw = makeSenseName(c, pKind, infl);
        } else {
            if(pKind != null) {
                addIkoParent(pKind);
            }
        }
        return sw;
    }

    public Word makeSenseNameIfNeeded(Category c, Word parentKind) {
        return makeSenseNameIfNeeded(c, parentKind, null);
    }

    public Word makeSenseNameIfNeeded(Category c) {
        return makeSenseNameIfNeeded(c, null, null);
    }

    public Word makeSenseNameIfNeeded(Category[] cs) {
        return makeSenseNameIfNeeded(cs, null, null);
    }

    public Word makeSenseNameIfNeeded(Category[] cs, Word parentKind) {
        return makeSenseNameIfNeeded(cs, parentKind, null);
    }

    public Word makeSenseNameIfNeeded(Category[] cs, Word pKind, String infl) {
        for(int ii = 0; ii < cs.length; ii++) {
            if(isFormOfCat(cs[ii])) {
                return makeSenseNameIfNeeded(cs[ii], pKind, infl);
            }
        }
        return this;
    }

    public Word[] makeSenseNamesIfNeeded(Category c, Word pKind, String infl) {
        Word[] needed = new Word[]{this};
        HashSet ws = new HashSet(Lexicon.defaultCatListSize);
        if((c != null) && isFormOfCat(c) && isAmbiguous()) {
            Word[] senses = this.getSubsenses();
            if(senses != null) {
                for(int is = 0; is < senses.length; is++) {
                    Category senseCat = senses[is].getSenseCat();
                    if((senseCat != null) && c.subsumesCategory(senseCat)) {
                        ws.add(senses[is]);
                    }
                }
            }
            if(ws.size() > 0) {
                needed = (Word[]) ws.toArray(needed);
            }
        } else {
            if(pKind != null) {
                addIkoParent(pKind);
            }
        }
        return needed;
    }

    public Word[] makeSenseNamesIfNeeded(Category c, Word parentKind) {
        return makeSenseNamesIfNeeded(c, parentKind, null);
    }

    public Word[] makeSenseNamesIfNeeded(Category c) {
        return makeSenseNamesIfNeeded(c, null, null);
    }

    public Word[] makeSenseNamesIfNeeded(Category[] cs) {
        return makeSenseNamesIfNeeded(cs, null, null);
    }

    public Word[] makeSenseNamesIfNeeded(Category[] cs, Word parentKind) {
        return makeSenseNamesIfNeeded(cs, parentKind, null);
    }

    public Word[] makeSenseNamesIfNeeded(Category[] cs, Word pKind, String infl) {
        // pm 9 March04 from WWoods lisp code
        HashSet ws = new HashSet(Lexicon.defaultCatListSize);
        if(cs != null) {
            for(int ii = 0; ii < cs.length; ii++) {
                if(isFormOfCat(cs[ii])) {
                    ws.add(makeSenseNameIfNeeded(cs[ii], pKind, infl));
                }
            }
        }
        Word[] senses = new Word[]{this};
        if((ws.size() > 0)) {
            senses = (Word[]) ws.toArray(senses);
        }
        return senses;
    }

    public boolean hasRoot(Word rt) { //pmartin hack 15may01
        WordEntry we = getWordEntry();
        if(we == null) {
            return false;
        } else {
            return LexiconUtil.isMembOfArray(rt, we.roots);
        }
    }

    public Word[] getAllRoots() { /* to replace get-any-root */
        HashSet values = new HashSet(Lexicon.defaultCatListSize);
        Stack stack = new Stack();
        Word current = null;
        stack.push(this);
        while(!stack.empty()) {  //recursively get all roots
            current = (Word) stack.pop();
            if(current != this) {
                values.add(current);
            }
            Word[] currentDirectRoots = current.getDirectRoots();
            if(currentDirectRoots != null) {
                for(int i = 0; i < currentDirectRoots.length; i++) {
                    if(!values.contains(currentDirectRoots[i])) {
                        stack.push(currentDirectRoots[i]);
                    }
                }
            }
        }
        Word[] warray = new Word[values.size()];
        warray = (Word[]) (values.toArray(warray));
        return warray;
    }

    public Word[] getAllParents(HashSet seen) {
        /* roots (and variantOfs) and iko and iio parents */
        /* pmartin 13 oct 00 . .. tweaked to use a  HashSet 12nov02 .*/
        /* needed to find numerical value of roman num and word senses */

        boolean debugOntology = false; /* turn on for bad ontology bugs!  */
        if(Lexicon.authorFlag) {
            if(debugOntology) {
                String seenStr = "[";
                Iterator seenit = seen.iterator();
                while(seenit.hasNext()) {
                    seenStr = seenStr + " " +
                            ((Word) seenit.next()).printString();
                }
                System.out.println("in getAllParents of " + this.printString() +
                        " seen are:" + seenStr + " ]");
            }
        }
        Word[] wroots = getAllRoots();
        if(Lexicon.authorFlag) {
            if(debugOntology) {
                System.out.println("in getAllParents roots are:" +
                        LexiconUtil.printStringArray(wroots));
            }
        }
        Word[] nrParents = nonRootParents();
        if(Lexicon.authorFlag) {
            if(debugOntology) {
                System.out.println("in getAllParents non-roots are:" +
                        LexiconUtil.printStringArray(nrParents));
            }
        }
        Word parent;
        HashSet ancs = new HashSet(Lexicon.defaultCatListSize);
        ArrayList newAncs = new ArrayList(Lexicon.defaultCatListSize);
        if(wroots != null) {
            for(int i = 0; i < wroots.length; i++) {
                parent = wroots[i];
                if(!seen.contains(parent)) {
                    seen.add(parent);
                    ancs.add(parent);
                    newAncs.add(parent);
                }
            }
        }

        if(nrParents != null) {
            for(int i = 0; i < nrParents.length; i++) {
                parent = nrParents[i];
                if(!seen.contains(parent)) {
                    newAncs.add(parent);
                    ancs.add(parent);
                    seen.add(parent);
                }
            }
        }

        for(int i = 0; i < newAncs.size(); i++) {
            wroots = ((Word) newAncs.get(i)).getAllParents(seen);
            if(wroots != null) {
                for(int j = 0; j < wroots.length; j++) {
                    ancs.add(wroots[j]);
                }
            }
        }

        Word[] allAnc = new Word[ancs.size()];
        allAnc = (Word[]) (ancs.toArray(allAnc));
        if(Lexicon.authorFlag) {
            if(debugOntology) {
                System.out.println("in getAllParents allAnc are:" +
                        LexiconUtil.printStringArray(allAnc));
            }
        }
        return allAnc;
    }

    public Word[] nonRootParents() {
        return this.nonRootParents(false);
    }

    public Word[] nonRootParents(boolean nonames) {
        /* conflates iio and iko */
        // nonames means don't follow paths from proper name
      /* tweaked 16oct00 pmartin to avoid looping */
        boolean trace = false;
        boolean filter = nonames && this.isFormOfCat(this.lexicon.nameCategory);
        HashSet nrp = new HashSet();
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Word maybe;
        Word[] iios = we.iioParents;
        if(Lexicon.authorFlag) {
            if(trace) {
                logger.finest("in nonRootParents of " +
                        this.printString() +
                        " iio are:" + LexiconUtil.printStringArray(iios));
            }
        }
        int i;
        if(iios != null) {
            for(i = 0; i < iios.length; i++) {
                nrp.add(iios[i]);
            }
        }
        if(Lexicon.authorFlag) {
            if(trace) {
                System.out.println("in nonRootParents of " + this.printString() +
                        " iko are:" +
                        LexiconUtil.printStringArray(we.ikoParents));
            }
        }
        Word[] allIkoParents = getIkoParents(false); // true causes "zig zag"
        if(allIkoParents != null) {
            for(i = 0; i < allIkoParents.length; i++) {
                nrp.add(allIkoParents[i]);
            }
        }
        List sensParents = (List) this.getdict(this.lexicon.senseofAtom);
        if(Lexicon.authorFlag) {
            if(trace) {
                String senseOf = "null";
                if(sensParents != null) {
                    senseOf = sensParents.printString();
                }
                System.out.println("in nonRootParents of " + this.printString() +
                        " senseOf are:" + senseOf);
            }
        }
        if(sensParents != null) {
            for(i = 0; i < sensParents.length(); i++) {
                nrp.add(sensParents.elementAt(i));
            }
        }

        Word[] variantParents = we.variantOf;
        if(variantParents != null) {
            for(i = 0; i < variantParents.length; i++) {
                nrp.add(variantParents[i]);
            }
        }
        // get rid of self from hash
        nrp.remove(this);

        int inrp = 0;
        int nrpSize = nrp.size();
        Word[] maybeParents = new Word[nrpSize];
        Iterator it = nrp.iterator();
//      System.out.println("debug nrp filter = " + filter + " and stopper siz = " +
//                         personStopperHash.size());
        while(it.hasNext()) {
            maybe = (Word) it.next();
            if(!(filter && (this.lexicon.personStopperHash.contains(maybe)))) {
                maybeParents[inrp++] = maybe;
            }
        }

        if(inrp == 0) {
            return null;
        }
        if(inrp == nrpSize) {
            return maybeParents;
        }
        Word[] parents = new Word[inrp];
        System.arraycopy(maybeParents, 0, parents, 0, inrp);
        if(Lexicon.authorFlag) {
            if(trace) {
                System.out.println("nonRootParents of " + this.printString() +
                        " returning " + LexiconUtil.printStringArray(parents));
            }
        }
        return parents;
    }

    public Word[] variantParents() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.variantOf;
        }
    }

    public Word[] allVariantParents() {
        /** returns variant Parentss of the word of all known kinds */
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Word[] vars = we.variantOf;
        Word[] abrvs = getAbbrevOf();
        if(vars == null && abrvs == null) {
            return null;
        }
        HashSet avp = new HashSet();
        if(vars != null) {
            for(int i = 0; i < vars.length; i++) {
                avp.add(vars[i]);
            }
        }
        if(abrvs != null) {
            for(int i = 0; i < abrvs.length; i++) {
                avp.add(abrvs[i]);
            }
        }
        int avpSize = avp.size();
        if(avpSize == 0) {
            return null;
        }
        Iterator it = avp.iterator();
        Word[] avParents = new Word[avpSize];
        int i = 0;
        while(it.hasNext()) {
            avParents[i++] = (Word) it.next();
        }
        return avParents;
    }

    public Word[] allVariantChildren() {
        /** returns inverse variants of the word of all known kinds */
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Word[] vars = getVariants();
        Word[] abrvs = getAbbrevs();
        if(vars == null && abrvs == null) {
            return null;
        }
        HashSet avp = new HashSet();
        if(vars != null) {
            for(int i = 0; i < vars.length; i++) {
                avp.add(vars[i]);
            }
        }
        if(abrvs != null) {
            for(int i = 0; i < abrvs.length; i++) {
                avp.add(abrvs[i]);
            }
        }
        int avpSize = avp.size();
        if(avpSize == 0) {
            return null;
        }
        Iterator it = avp.iterator();
        Word[] avChildren = new Word[avpSize];
        int i = 0;
        while(it.hasNext()) {
            avChildren[i++] = (Word) it.next();
        }
        return avChildren;
    }

    public Word[] senseParentRoots(boolean nprOK) {
        /** Differs from vanilla nonRootParents by descending into
        subsenses of its intial word. (for text words)
        allows proper nouns only if nprOK is true.
         */
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Word[] iios = we.iioParents;
        Word[] ikos = we.ikoParents;
        Word[] sups = we.getSenseParents();
        Word[] subs = we.subsenses;
        Word[] vars = we.variantOf;
        int max = 0;
        if(iios != null) {
            max += iios.length;
        }
        if(ikos != null) {
            max += ikos.length;
        }
        if(sups != null) {
            max += sups.length;
        }
        if(subs != null) {
            max += subs.length;
        }
        if(vars != null) {
            max += vars.length;
        }

        if(max == 0) {
            return null;
        }
        HashSet notpr = new HashSet(max);
        HashSet pr = null;
        if(!nprOK) {
            pr = new HashSet(max);
        }

        if(iios != null) {
            for(int i = 0; i < iios.length; i++) {
                if(nprOK || iios[i].hasCommonCat()) {
                    notpr.add(iios[i]);
                } else {
                    pr.add(iios[i]);
                }
            }
        }

        if(ikos != null) {
            for(int i = 0; i < ikos.length; i++) {
                if(nprOK || ikos[i].hasCommonCat()) {
                    notpr.add(ikos[i]);
                } else {
                    pr.add(ikos[i]);
                }
            }
        }

        if(sups != null) {
            for(int i = 0; i < sups.length; i++) {
                if(nprOK || sups[i].hasCommonCat()) {
                    notpr.add(sups[i]);
                } else {
                    pr.add(sups[i]);
                }
            }
        }

        if(subs != null) {
            for(int i = 0; i < subs.length; i++) {
                if(nprOK || subs[i].hasCommonCat()) {
                    notpr.add(subs[i]);
                } else {
                    pr.add(subs[i]);
                }
            }
        }

        if(vars != null) {
            for(int i = 0; i < vars.length; i++) {
                if(nprOK || vars[i].hasCommonCat()) {
                    notpr.add(vars[i]);
                } else {
                    pr.add(vars[i]);
                }
            }
        }

        Iterator it = null;
        int psize;
        if((psize = notpr.size()) > 0) {
            it = notpr.iterator();
        } else if((psize = pr.size()) > 0) {
            it = pr.iterator();
        } else {
            return null;
        }

        Word[] parents = new Word[psize];
        int i = 0;
        while(it.hasNext()) {
            parents[i++] = (Word) it.next();
        }
        return parents;
    }

    public Word[] getAllNonPersonParents() { /* faster?? */
        HashSet seen;
        seen = (HashSet) (this.lexicon.personStopperHash.clone());
        seen.add(this);
        return this.getAllParents(seen);
    }

    public Word[] getAllParents() { /* roots and iko and iio parents */
        HashSet seen = new HashSet(Lexicon.defaultCatListSize);
        seen.add(this);
        return this.getAllParents(seen);
    }

    public Word[] getIkoParents(boolean useSubSenses) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return we.getIkoParents(useSubSenses);
        }
    }

    public Word[] getDirectRoots() {
        /* patched pmartin 21 dec 99 to avoid root loops */
        // patched 1 may 02 to avoid VariantOf (now in nonRootParents)
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Word[] myRoots = we.roots;
        if(!(isInArray(myRoots))) {
            return myRoots;
        } else if(myRoots.length == 1) {
            return null;
        } else {
            int unselfn = myRoots.length - 1;
            Word[] unself = new Word[unselfn];
            int j = 0;
            for(int i = 0; i < myRoots.length; i++) {
                if(this != myRoots[i]) {
                    unself[j++] = myRoots[i];
                }
            }
            return unself;
        }
    }

    // tweaked to support alternate form with inclusion Category
    // .. pmartin  6 August 99
    public Word[] getInflectionRoots() {
        return this.getInflectionRoots(null);
    }

    /***
     * the category arg selects which subsenses we want the roots to be for.
     * It is not a restriction on the roots themselves.
     *
     *  pmartin 16nov01
     */
    public Word[] getInflectionRoots(Category inCat) {
        HashSet values = new HashSet(Lexicon.defaultCatListSize);
        Word[] droots;
        Word[] senses = getSubsenses();
        if((inCat != null) && (senses != null) && (senses.length > 0)) {
            // we've got to find it among these
            for(int i = 0; i < senses.length; i++) {
                if(senses[i].isFormOfCat(inCat)) {
                    droots = senses[i].getDirectRoots();
                    if(droots != null) {
                        for(int j = 0; j < droots.length; j++) {
                            values.add(droots[j]);
                        }
                    }
                }
            }
        }
//             String inCatStr = "null";
//             if (inCat != null) inCatStr = inCat.printString();
//               System.out.print(this.printString()+ " ifc nonRoot = " +
//                                  this.isFormOfCat(nonrootCategory) );
//             if (inCat == null)
//                 System.out.println("and cat was null");
//             else
//                 System.out.println(" ifc(" + inCatStr + ") = " +
//                                    this.isFormOfCat(inCat));
        if(values.size() == 0) {
            droots = getDirectRoots();
//                 Word[] proots = we.roots;
//                  System.out.println("droots = " + toStringArray(droots) +
//                      " and proots = " + toStringArray(proots));
            if((droots != null) && (droots.length > 0) &&
                    this.isFormOfCat(this.lexicon.nonrootCategory) &&
                    ((inCat == null) || (this.isFormOfCat(inCat)))) {
                for(int i = 0; i < droots.length; i++) {
                    if(droots[i].getSenseWord() != this) {
                        values.add(droots[i]);
                    }
                }
            }
        }
        if(values.size() > 0) {
            Word[] warray = new Word[values.size()];
            warray = (Word[]) (values.toArray(warray));
            return warray;
        } else {
            return null;
        }
    }

    public boolean checkRootFeature(Atom feat) {
        /// hacked up pmartin 12Apr00
        Word[] inflRoots = this.getInflectionRoots();
        if(inflRoots != null) // pmartin 31aug00
        {
            for(int i = 0; i < inflRoots.length; i++) {
                if(inflRoots[i].hasFeature(feat)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Word[] getPastRoots() {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else if(we.isFormOfCat(this.lexicon.pastCategory)) {
            return we.roots;
        } else {
            return null;
        }
    }

    public boolean subsumesOneOf(Word[] words) {
        int i = 0;
        if(words == null) {
            return false;
        }
        while(i < words.length) {
            if(this.subsumesWord(words[i])) {
                return true;
            }
            i++;
        }
        return false;
    }

    public boolean subsumesWord(Word word, HashSet triedWords) {
        if(this == word) {
            return true;
        }
        if(triedWords.contains(word)) {
            return false;
        }
        triedWords.add(word);
        WordEntry we = word.getWordEntry();
        if(we == null) {
            return false;
        }
        Word[] wdikos = we.ikoParents;
        if(wdikos == null) {
            return false;
        }
        for(int i = 0; i < wdikos.length; i++) {
            if(this.subsumesWord(wdikos[i], triedWords)) {
                return true;
            }
        }
        return false;
    }

    public boolean subsumesWord(Word word) {
        if(this == word) {
            return true;
        }
        WordEntry we = word.getWordEntry();
        if(we == null) {
            return false;
        }
        Word[] wdikos = we.ikoParents;
        if(wdikos == null) {
            return false;
        }

        HashSet triedWords = new HashSet(Lexicon.defaultCatListSize);
        triedWords.add(word);
        for(int i = 0; i < wdikos.length; i++) {
            if(this.subsumesWord(wdikos[i], triedWords)) {
                return true;
            }
        }
        return false;
    }

    //waw: approximate test
    public boolean wordIsEmpty() { //pmartin 4sep01
        WordEntry we = getWordEntry();
        if(we == null) {
            return true;
        } /* no categories at all means the word is a "loose end"  or contraction*/ else {
            return ((we.noPenaltyCategories == null) &&
                    (we.penalty1Categories == null) &&
                    (we.penalty2Categories == null) &&
                    (we.penalty3Categories == null) &&
                    (we.properties == null));  // pmartin 26oct00 for "i'd"
        }
    }

    public Word morph() {  //placeholder 'til morphology installed..
        // pmartin 6sep99
        //
        if(wordIsEmpty()) {
            this.lexicon.makeGuessedWord(this);     //pmartin added guess call 10jan00
        }
        return this;
    }

    public String getWordString() {
        return wordstring;
    }

    /* markDict(...) is used in morph to add properties to a word */
    public Value markDict(Atom p, Value val, boolean addToList) {
        return makeWordEntry().markDict(p, val, addToList);
    }

    /* markDict(...) is used in morph to add properties to a word */
    public Value markDict(Atom p, Value[] valArray, boolean addToList) {
        return makeWordEntry().markDict(p, valArray, addToList);
    }

    // lexicon maintenance methods:

    /* setWordCategories sets the category lists of a word */
    public Category[] setWordCategories(String str, int penalty) {
        Category[] cats = this.lexicon.getCategories(str, " ,\t");
        if(cats != null) {
            WordEntry we = makeWordEntry();
            if(penalty > 2) {
                we.penalty3Categories = cats;
            } else if(penalty > 1) {
                we.penalty2Categories = cats;
            } else if(penalty > 0) {
                we.penalty1Categories = cats;
            } else {
                we.noPenaltyCategories = cats;
            }
            return cats;
        } else {
            return null;
        }
    }


    /* addWordCategory adds to the category lists of a word */
    public Category[] addWordCategory(Category newCat, int penalty) {
        /*** The new category is added at the specified penalty level, and
         *  any instance of that category at some other penalty level is removed.
         */
        return makeWordEntry().addWordCategory(newCat, penalty);
    }

    public Category[][] nonRootCategories() { //pmartin 2aug01
        /*** returns an array of four elements, each of which is an array
         *  of the categories that correspond to that penalty level of the
         *  word.  Only those categories NOT subsumed by isRootCategory
         *  are returned.
         */
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Category[][] nrcats = new Category[4][];
        nrcats[0] = LexiconUtil.selectNonRootCategories(we.noPenaltyCategories);
        nrcats[1] = LexiconUtil.selectNonRootCategories(we.penalty1Categories);
        nrcats[2] = LexiconUtil.selectNonRootCategories(we.penalty2Categories);
        nrcats[3] = LexiconUtil.selectNonRootCategories(we.penalty3Categories);
        return nrcats;
    }

    public synchronized String printEntryString() {
        String val = this.safePrintString() + ":";
        WordEntry we = getWordEntry();
        if(we != null) {
            val = val + we.printWordEntryString();
        } else {
            val = val + ";";
        }
        return val;
    }

    /* addtodictprop adds property values to property lists of words */
    // pmartin added null and empty value hardening 11jun01
    public Value addtodictprop(Atom p, Value newval, boolean atEnd) {
        WordEntry we = makeWordEntry();
        dirty();
        return we.addtodictprop(p, newval, atEnd);
    }

    public Value addtodictprop(Atom p, Value newval) {
        WordEntry we = makeWordEntry();
        dirty();
        return we.addtodictprop(p, newval, true);
    }

    public Word[] getForms() { //pmartin 22may01
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        ConcurrentHashMap props = we.properties;
        if(props == null) {
            return null;
        }
        Value formsList = (Value) props.get(this.lexicon.formsAtom);
        if(formsList != null) {
            return (Word[]) ((List) formsList).contents;
        } else {
            return null;
        }
    }

    public Value getdict(Atom p) {
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        ConcurrentHashMap props = we.properties;
        if(props == null) {
            return null;
        }
        return (Value) props.get(p);
    }

    public Value getFirstVal(Atom feat) { // shrunk pmartin 01aug01
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        } else {
            return LexiconUtil.firstVal(we.getdict(feat));
        }
    }

    public Word[] getFirstDecomposition() { // pmartin 22may01 + 24july01
        WordEntry we = getWordEntry();
        if(we == null) {
            return null;
        }
        Value[] compOf = we.compoundOf;
        if((compOf == null) || (compOf.length == 0)) {
            return null;
        } else if(compOf[0].wordp()) {
            int cl = compOf.length;
            Word[] fw = new Word[cl];
            for(int i = 0; i < cl; i++) {
                fw[i] = (Word) compOf[i];
            }
            return fw;
        } else if(compOf[0].listp()) {
            Value[] con = ((List) compOf[0]).contents;
            int cl = con.length;
            Word[] fw = new Word[cl];
            for(int i = 0; i < cl; i++) {
                fw[i] = (Word) con[i];
            }
            return fw;
        } else {
            System.err.println("weird value in " + this + " compoundOf = " +
                    compOf);
            return null;
        }
    }

    public Word[] getFirstDecomposition(Atom feat) { // pmartin 15may01
        // + 24july01
        Value val;
        if(feat == this.lexicon.compoundOfAtom) {
            return getFirstDecomposition();
        } else {
            val = this.getdict(feat);
        }
        if(val == null) {
            return null;
        }
        if(val.listp()) {
            Value[] con = ((List) val).contents;
            if((con == null) || (con.length == 0)) {
                return null;
            }
            if(con[0].listp()) {
                return (Word[]) ((List) con[0]).contents;
            } else {
                return (Word[]) con;
            }
        } else {
            return null;
        }
    }

    public Word firstWord() { // get first word of compound
        //  pmartin 16may01 + 24juuly01 + 12may04
        Word[] comp1 = getFirstDecomposition();
        if(comp1 == null) {
            return null;
        } else {
            return comp1[0];
        }
    }

    public Word restWords() {
        /*** unbarred phrase-word of all non-first words of compound
         *  pmartin 17may01 + 24july01
         */
        Word[] rws = getFirstDecomposition();
        if((rws == null) || (rws.length < 2)) {
            return null;
        } else {
            int rwl = rws.length - 1;
            Word[] rrw = new Word[rwl];
            for(int i = 0; i < rwl; i++) {
                rrw[i] = rws[i + 1];
            }
            return this.lexicon.makePhraseWord(rrw);
        }
    }

    public Value putdict(Atom p, Value val) {
        WordEntry we = getWordEntry();
        if(we == null) {
            if(val == null) {
                return val;
            } else {
                we = makeWordEntry();
            }
        }
        if(val.equal(getdict(p))) {
            return val;
        }
        dirty();
        return we.putdict(p, val);
    }

    public Value remdict(Atom p) {
        return makeWordEntry().remdict(p);
    }

    public void purge() {  //pmartin 27sep01
        if(dontPurge) {
            return;
        } else if((index & Lexicon.NODISKINDEX) != 0) // morph-made word
        {
            removeWord();
        } else if((index & Lexicon.MARKEDINDEX) == 0) {
            wordEntry = null;  // preserve just the word stub
        }        // other vals are DISKOUTDATED and NOWORDENTRY, both left as is
    }

    public void dontPurge() {
        dontPurge = true;
    }

    public void clearWord() { //pmartin and ww 26aug99
        clearWord(false);
    }

    public void clearWord(boolean forsure) {
        WordEntry we = getWordEntry();
        if(we != null) {
            dirty();
            we.clearWordEntry(forsure);
        }
    }

    public void removeWord() {
        String key = wordstring;
        clearWord(true);
        this.lexicon.wordTable.remove(key);
        this.lexicon.wordFreePos--;
    }

    public boolean hasParents() { // true if any kind of parent
        WordEntry we = wordEntry;
        if(we == null) {
            return false;
        }
        return ((we.variantOf != null) || // variants are parents
                (we.roots != null) || // roots are morphological parents
                (we.ikoParents != null) || // ikos are kind-of parents
                (we.iioParents != null) || // iios are instance-of parents
                (we.getdict(this.lexicon.senseofAtom) != null));  // senseOf are sense parents
    }

    // miscellaneous methods:
    public boolean eq(Value obj) { //tells whether this value is eq another
        return (this == obj);
    }

    public boolean equal(Value obj) { //tells whether this value is eq another
        return (this == obj);
    }

    /* copied from (my altered versions of) Atom ..PMartin 2 July  */
    public boolean isInArray(Value[] array) { //tests if this word is in an array
        return LexiconUtil.isMembOfArray(this, array);
    }

    public synchronized String toString() {
        return "WORD:" + wordstring;
    }

    public synchronized String printString() {
        return wordstring;
    }

    public synchronized String phraseNameString() {
        // for nesting inside phrase printing
        return wordstring;
    }

    public synchronized String safePrintString() {
        if(dangerousChars.length > 0) {
            for(int i = 0; i < dangerousChars.length; i++) {
                if(wordstring.indexOf(dangerousChars[i]) != -1) {
                    return this.lexicon.openQuoteStr + wordstring +
                            this.lexicon.closeQuoteStr;
                }
            }
        }
        return wordstring;
    }

    /// end Word methods //////
}
