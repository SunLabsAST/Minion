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
import java.util.Hashtable;
//import Lexicon;

/*
; copyright (c) by W.A. Woods.  All rights reserved.
; The following software was created in 1989, 1990, 1991, 1992, 1993, 1994,
; 1995, 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2003, and 2004,
; but has not been published within the meaning of the copyright law.
; This file may be made available by the author to selected individuals
; for specific research projects, but may not to be copied, distributed, or
; used for other purposes without explicit permission from the author.

; This software is licensed "as is." The author expressly disclaims any
; warranties of any kind and accepts no liabilities resulting from its use.
*/

/**
 * This is a specialization of MorphEngFrame containing functions used by
 * the English morphological rules in Morph_en.
 *
 * @author W. A. Woods
 * 
 * @version        1.0
 * 
 * The rules for this system were developed by W. A. Woods
 * 
 * @see MorphEngine
 */

public abstract class MorphEngFns extends MorphEngFrame {

// Note: search for tbd, fake, and weak for methods that need further work.

  /**
   * Flag for turning off chemical name test.  Should be true in the
   * production version.
   */

  protected boolean skipChemName = true;

 /* minimum length for a component of a lexical compound.*/
  protected int minimumCompoundLength = 3;

  protected boolean strongChemTestFlag = false;

  protected String[] barredChemicalEndings = new String[] {
    "ing", "ed", "s", 
    // "i|y  s|z ing",  // allomerizing
    // "i|y  s|z e d|s ",
    "y", // blocks quicksilvery
    // "fy, 
    // "ate", // thiocynanate .. use suffix rule
    //"al", "an", // polyvinylformal
    // "ating", "ated", "ates", 
    // "fying", "fied", "fies",
    "ise", "ize", "yse", "yze",  
    // "ide" // transactinide, but * trioxide
    "ous", "ic", // blocks diaminopropionic, trichloroacetic -- complex adj
    "able", // blocks alkalisable
    "tion", // blocks alkalisation 
  };

  protected String consonantChars = "bcdfghjklmnpqrstvwxyz???";
  protected String vowelChars = "aeiou??????????????????????????";



// The following are bound here and initialized in Morph_en:

  protected Word[] wordSet_Ise_Ize;
  protected HashSet assimilatedPrefixes = new HashSet(); //pm 8apr04



// methods used in morphological analysis:


  /**
   * Applies the chemical name tests to the indicated word.
   */

/*
  //tbd - check that this works, when chemicalNameTest and nounInflectCode are done
  // second version replaces the first

  protected void doChemicalNameTest(Lexicon.Word thisLex, MorphState state) {
    if (!skipChemName && chemicalNameTest(thisLex)) {
      Lexicon.Atom[] features = {atom_iCode, nounInflectCode(thisLex)};
      addCatSense(state, cat_nm, thisLex, features);
    }
    return;
  }

  protected void doChemicalNameTest(Lexicon.Word testWord, MorphState state) {
    if (!skipChemName && chemicalNameTest(testWord)) {
      testWord.addInflectionCode(nounInflectCode(testWord));
      addCatSense(state, cat_nm, testWord, null);
    }
    return;
  }

*/


  /**
   * Method called as part of testing rule conditions
   * to see if a word has the property 'guessed'.
   */

  protected boolean isGuessedWord (Word x) {
   if (x.getdict(atom_guessed) != null)
     return true;
   else
     return false;
  }

  /**
   * Method for checking for special forms before attempting morph rules.
   */

  public Word morphPrecheck(Word word) {
    //does the precheck tests
    //tbd finish this
    if (word.numeralp()) return word;
    String itemstring = word.getWordString();
    Category category = null;
    int pos = -1;
    if ((itemstring.charAt(0) == '!') && ((pos = itemstring.indexOf('/')) > 0) &&
        ((category = dict.getCategory(itemstring.substring(1, pos))) != null)) {
      // Recognized a sense name
      word.markDict(dict.formAtom, dict.rootAtom, true);
      processCatSense(word, category, word, null, 0);
      return word;
    }
    int itemlength = itemstring.length();
    if (itemlength < 1) {
    // Make lexical entry for empty word -- no categories, but not empty
      return word;
    }
    boolean allAlpha = true;
    int apostrophe = 0;
    int hyphens = 0;
    int underscores = 0;
    int spaces = 0;
    int slashes = 0;
    int periods = 0;
    int finalperiod = 0;
    int initialcap = 0;
    int internalcap = 0;
    int someupper = 0;
    int somelower = 0;
    int somealpha = 0;
    int somedigit = 0;
    int colons = 0;
    int commas = 0;
    int otherchar = 0;
    int somevowel = 0;
    char c;
    for (int i = 0 ; i < itemlength; i++) {
      c = itemstring.charAt(i);
      if(c <= 122 && c >= 97) {  //the most frequent case, lowercase a...z
        somelower = somelower+1;
        somealpha = somealpha+1;
        if("aeiouy".indexOf(c) > -1) //tbd - accented versions of these
          somevowel = somevowel+1;
      }
      else if(c <= 90 && c >= 65) {  //the next most frequent case, uppercase A...Z
        someupper = someupper+1;
        somealpha = somealpha+1;
        if (i > 0) internalcap = internalcap+1;
        else initialcap = initialcap+1;
        if("AEIOUY".indexOf(c) > -1) //tbd - accented versions of these
          somevowel = somevowel+1;
      }
      else if(c <= 57 && c >= 48) {  //the next most frequent case, numbers 0...9
        somedigit = somedigit+1;
        allAlpha = false;
      }
      else if(c == '-') { //the next most frequent case, a hyphen
        hyphens = hyphens+1;
        allAlpha = false;
      }
      else if(c == ',') { //the next most frequent case, a comma
        commas = commas+1;
        allAlpha = false;
      }
      else if(c == '.') { //the next most frequent case, a period
        periods = periods+1;
        allAlpha = false;
        if(i == (itemlength-1))
          finalperiod = finalperiod+1;
      }
      else if(c == '/') { //the next most frequent case, a slash
        slashes = slashes+1;
        allAlpha = false;
      }
      else if(c == ':') { //the next most frequent case, a colon
        otherchar = otherchar+1;
        colons = colons+1;
        allAlpha = false;
      }
      else if(c == 39) { //the next most frequent case, an apostrophe
        apostrophe = apostrophe+1;
        allAlpha = false;
      }
      else if(c == '_') { //the next most frequent case, an underscore
        underscores = underscores+1;
        allAlpha = false;
      }
      else if(c == 32 || c == 160)  {    // space and nbsp
        // ASCII character 160 is nonbreak whitespace.
        spaces = spaces+1;
        allAlpha = false;
      }
      // The rest of the white space characters for break:
      else if((c <= 13 && c >= 9) ||        // Tab, Linefeed, PageUp, Page, Return
              (c == 0 || c == 3)  ||        // Null, Enter
              (c > 255 && Character.isWhitespace(c))  // any higher unicode whitespace
             ) {
        spaces = spaces+1;
        allAlpha = false;
      }
      else if(c < 128) { //any remaining ascii chars
        otherchar = otherchar+1;
        allAlpha = false;
      }
      else if(!Character.isLetterOrDigit(c)) { //any other nonalphanum chars
        otherchar = otherchar+1;
        allAlpha = false;
      }
      else if(Character.isLowerCase(c)) {  //the next most frequent case, lowercase alpha
        somelower = somelower+1;
        somealpha = somealpha+1;
        if("aeiouy".indexOf(c) > -1) //tbd - accented versions of these
          somevowel = somevowel+1;
      }
      else if(Character.isUpperCase(c)) {  //the next most frequent case, uppercase alpha
        someupper = someupper+1;
        somealpha = somealpha+1;
        if (i > 0) internalcap = internalcap+1;
        else initialcap = initialcap+1;
        if("AEIOUY".indexOf(c) > -1) //tbd - accented versions of these
          somevowel = somevowel+1;
      }
      else if(Character.isDigit(c)) {  //the next most frequent case, numbers
        somedigit = somedigit+1;
        allAlpha = false;
      }
      else {
        otherchar = otherchar+1;
        allAlpha = false;
      }
    }
/* tbd
           (if (or initialcap internalcap) ;capture the capcode property for this spelling
             (addtodictprop item 'capcodes (make-capcode itemstring) t t))
*/
    if (allAlpha) { //if nothing but alpha, then it's ordinary word, so go on to apply rules
      return null;
    }
   //tbd finish this:
    if (somealpha > 0 && somedigit > 0) {      //both alpha and digit involved
      word.markDict(dict.formAtom, dict.rootAtom, true);
      processCatSense(word, cat_npr, word, null, 0);
      return word;
    }
    //from morph-precheck at: ((and (not somealpha) (not somedigit)) 'punct)
    else if (somealpha < 1 && somedigit < 1) { //no alpha or digit involved
      processCatSense(word, cat_punct, word, null, 0);
      return word;
    }
    if (otherchar > 0) {                       //some otherchar involved
      word.markDict(dict.formAtom, dict.rootAtom, true);
      processCatSense(word, cat_npr, word, null, 0);
      return word;
    }
    else return null;
  }

  /**
   * copies inflectional features from specified root word.
   * copy-features (root cat) copies a root-features
   * list or an inflection code from root -- used in morph rules
   * to get the value to be returned for a cat action.
   * It doesn't really copy the structure.
   * copy-features is only used in cases where the work has already
   * been done and the features are already
   * on the word, but they need to be returned as values from cat arcs
   * so that the Lisp system will put
   * them back on the word after it erases the temporary information that
   * is on the word at the time.
   * Hence, I don't think it needs to do anything at all in the Java version.
   */

  protected Value[] copyFeatures(Word root, Category cat) {
  // tbd make sure this doesn't really need to do anything
  //  if (!root.isFormOfCat(root, cat) {
  //    }
    return null;
  }

  /**
   * formsToUse makes a list of the inflection categories that this
   * root has, which are to be passed on to a derived word
   */
    // pm 9apr04
    
  protected ArrayList formsToUse(Word root, Category cat) {
    //for subcategories of cat that are nonRootCategories, collect them
    if (!root.isFormOfCat(cat))
      return null;
    ArrayList results = new ArrayList();
    Category[][] iCatArrays = root.nonRootCategories();
    // root.nonRootCategories() is an array of arrays of categories
    // for penalties 0-3, respectively
    if (iCatArrays == null)
      return null;
    for (int i = 0 ; i < iCatArrays.length ; i++) {
      Category[] iCats = iCatArrays[i];
      for (int j = 0 ; j < iCats.length ; j++) {
        Word penalty = dict.smallIntegers[i];
        results.add(penalty);
        results.add(iCats[j]);
      }
    }
    return results;
  }

  /** Helper for formsWithPrefix:
   * Certain prefixes assimilate the first consonant of the word
   * with which they are compounded.  "ad" is one such.
   * pm 05apr04
   */
   public String assimilatedPrefix(String wdstr, Word pref){
       String prefstr = pref.getWordString();
       if (prefstr.equals("ad")){ // add other examples here!
           int apos = prefstr.length() - 1;
           String asim = wdstr.substring(0,apos);
           if (assimilatedPrefixes.contains(asim))
               return asim;
       }
       return prefstr;
   }
           
  /** formsWithPrefix (rtword cat prefix featureList prefixStr killString)
   * constructs a root-features list for prefix+rtword 
   * in the indicated category cat, using the lexical
   * information for rtword and adding the given prefix.
   * The argument, prefix-string, if provided indicates that
   * the prefix string to be appended to the root is slightly
   * different from the standard form of the prefix as
   * specified in prefix -- e.g., re- for prefix re.
   *
   * rebuilt from original weak version 05apr04 pmartin
   */
  public Value[] formsWithPrefix(Word rootwd, Category cat,
                                       Word prefix, Value[] featureList,
                                       String prefixStr, String killString) {
   if (prefix == null) {
     if (authorFlag)
       System.out.println ("*** error: no prefix in formsWithPrefix for " +
                           rootwd + " as " + cat);
     prefix = dict.makeWord("null-prefix");
     if (prefixStr == null) prefixStr = "null-prefix";
   }
    if (authorFlag)
      trace("formsWithPrefix(" + rootwd.printString() + ", " +
            cat.printString() + ", " + prefix.printString() + ", " + 
            LexiconUtil.arrayToString(featureList) + ", " + prefixStr + ", "
            + killString + ")");
    if (rootwd == null) return null;
    String rtwdstr = rootwd.getWordString();
    if (prefixStr == null || prefixStr.length() == 0)
        prefixStr = assimilatedPrefix(rtwdstr, prefix);
    else prefixStr = prefixStr.toLowerCase();
    ArrayList valueFeatures = new ArrayList();
    Category[][] iCatArrays = rootwd.nonRootCategories();
    // root.nonRootCategories() is an array of arrays of categories
    // for penalties 0-3, respectively
    if (iCatArrays != null) {
      for (int i = 0 ; i < iCatArrays.length ; i++) {
        Category[] iCats = iCatArrays[i];
        Word penaltyProp = dict.makeWord(i);
        if (iCats != null) {
          for (int j = 0 ; j < iCats.length ; j++) {
            if (cat == null || cat.subsumesCategory(iCats[j])) {
              valueFeatures.add(penaltyProp);
              valueFeatures.add(iCats[j]);
            }
          }
        }
      }
    }
    if (featureList != null) {
      for (int i = 0 ; i < featureList.length ; i++) {
        valueFeatures.add(featureList[i++]);
        valueFeatures.add(featureList[i]);
      }
    }

    Word[] iio = rootwd.getIioParents();
    if (iio != null){
        valueFeatures.add(dict.iioAtom);
        valueFeatures.add(dict.makeList(iio));
    }
    Atom[] icodes = rootwd.getInflectionCodes();
    if (icodes != null){
        valueFeatures.add(dict.icodesAtom);
        valueFeatures.add(dict.makeList(icodes));
    }

            
    //tbd finish this by adding selected features from root and making
    //new name for result
    //issue this is inconsistent with the way the translator translates
    //penalty properties
    //for words that it analyzes.  One or the other has to change
    //-- see processCatSense
    Value[] value = new Value[valueFeatures.size()];
    value = (Value[])(valueFeatures.toArray(value));
    if (authorFlag)
      trace("formsWithPrefix returning " + LexiconUtil.arrayToString(value));
    return value;
  }

  public Value[] formsWithPrefix(Word root, Category cat,
                                         Word prefix, Value[] featureList,
                                         String prefixString) {
    return formsWithPrefix(root, cat, prefix, featureList, prefixString, null);
  }

  public Value[] formsWithPrefix(Word root, Category cat,
                                         Word prefix, Value[] featureList) {
    return formsWithPrefix(root, cat, prefix, featureList, null, null);
  }

  public Value[] formsWithPrefix(Word root, Category cat,
                                         Word prefix) {
    return formsWithPrefix(root, cat, prefix, null, null, null);
  }

  public List makeFormsForCompound(Word root, Word mod,
                                           Category cat, String connector,
                                           boolean suffixFlag) {
    if (authorFlag)
//       System.out.println("fake makeFormsForCompound(" + root.printString() + ", " +
      trace("fake makeFormsForCompound(" + root.printString() + ", " +
            mod.printString() + ", " + cat.printString() + ", " +
            connector + ", " + suffixFlag + ") returning null");
    // tbd: implement this -- this is converted by the lisp-to-java translator
    // into calls to addCompoundCatSenses, so is not called by any rules.
    return null;
  }

  public List makeFormsForCompound(Word root, Word mod,
                                           Category cat, String connector) {
    return makeFormsForCompound(root, mod, cat, connector, false);
  }

  /**
   * Tests whether the indicated word is a plural noun that can not
   * be a singular noun.
   */
  public boolean mustBePluralNoun(Word word) {
    return (word.canBePluralNoun() && !(word.canBeSingularNoun()));
  }

  String[] doubleLetterPattern = {consonantChars, vowels, "bdgklmnprt"};

  /**
   * Tests whether the indicated word is worth considering as a
   * possible superlative adjective or adverb or an archaic 2sg verb.
   */
  public boolean mayBeSuperlativeEtc(Word word) {
    String wordstring = word.getWordString();
    int len = wordstring.length();
    if (len < 5 || !(wordstring.endsWith("est"))) return false;
    String rootstring = wordstring.substring(0, len-3);
    return (plausibleRoot(word) &&
            (polysyllabic(rootstring) ||
             !patternCheck(rootstring, doubleLetterPattern) ||
             dict.isKnownWord(wordstring.substring(0, len-2))));
  }

  public Word getInflectedForm(Word word, Atom tag){
    // Used only in prefix rules for prefix a and with tag = prespart
    // tbd define this.
    if (authorFlag)
      trace("fake getInflectedForm(" + word.printString() + ", " + 
            tag.printString() + ") returning the word");
    return word;
  }

  public boolean quickcheck(Word word, Category cat){
    // Note all lower case in this method name.
    // This method tests category membership of a word without morphing
    // the word (i.e., using whatever is already known about the word).
    return (word != null && isFormOfCat(word, cat));
  }

  public Value[] getStandardFeatures(Word word, Category cat,
                                     Atom tag){
    if (authorFlag)
      trace("fake getStandardFeatures(" + word.printString() + ", " + 
            cat.printString() + ", " + tag.printString() + 
            ") returning null Value[]");    
    return null;
  }

  public Value[] makeNoun(Word wd) {
    if (authorFlag)
      trace("weak makeNoun(" + wd.printString() +
            ") returning nounInflectCode");
    wd.addWordCategory(cat_n, 0);
    return wd.addInflectionCode(nounInflectCode(wd));
  }

  public Value[] makeVerb(Word wd) {
    if (authorFlag)
      trace("weak makeVerb(" + wd.printString() +
            ") returning verbInflectCode");
    wd.addWordCategory(cat_v, 0);
    return wd.addInflectionCode(verbInflectCode(wd));
  }

  public Word firstPastRoot(Word wd){
   // tweaked pmartin 16aug01
   // tweaked again pmartin 08aug02 to always return a word
    Word[] rts = wd.getInflectionRoots(cat_past);
    Word rt;
    if ((rts == null) || (rts.length < 1))
        rts = wd.getWordEntry().roots;
    if ((rts != null) && (rts.length > 0))
        rt = rts[0];
    else rt = wd;
    if (authorFlag)
      trace("tweaked firstPastRoot(" + wd.printString() + 
            ") returning " + rt);
    return rt;
  }

  public Word firstPastpartRoot(Word wd){
   // tweaked pmartin 16aug01
   // tweaked again pmartin 08aug02 to always return a word
    Word[] rts = wd.getInflectionRoots(cat_pastpart);
    Word rt;
    if ((rts == null) || (rts.length < 1))
        rts = wd.getWordEntry().roots;
    if ((rts != null) && (rts.length > 0))
        rt = rts[0];
    else rt = wd;
    if (authorFlag)
      trace("tweaked firstPastpartRoot(" + wd.printString() + 
            ") returning " + rt);
    return rt;
  }

  public void addPenalty(Word wd, Category cat, int ii){
      wd.addWordCategory(cat, ii);
  }

  public String butLastCharString (String oldstr){
    if ((oldstr == null) || (oldstr.length() < 2)) return "";
    else return oldstr.substring(0, (oldstr.length() - 2));
  }

  public String butLastCharString (String oldstr, int nlast){
    if ((oldstr == null) || (oldstr.length() < (nlast + 1))) return "";
    else return oldstr.substring(0, (oldstr.length() - (nlast + 1)));
  }

  public Word butLastChar(Word wd){
    String blcStr = butLastCharString(wd.getWordString());
    if (blcStr.length() > 0) 
        return dict.makeWord(blcStr);
    else return null;
  }

  public boolean notPrefix(Word wd, Word[] pfxs){
     Word[] wpxs = wd.getPrefixes();
     if ((pfxs == null) || (pfxs.length == 0) ||
         (wpxs == null) || (wpxs.length == 0)) return true;
     return !(LexiconUtil.intersectp(wpxs, pfxs));
  }

  public boolean notSuffix(Word wd, Word[] sfxs){
     Word[] wsxs = wd.getSuffixes();
     if ((sfxs == null) || (sfxs.length == 0) ||
         (wsxs == null) || (wsxs.length == 0)) return true;
     return !(LexiconUtil.intersectp(wsxs, sfxs));
  }

  public Word[] getVRoots (Word wd){
    return wd.getRoots(cat_v);
  }

  public static Value nodeSequenceFirst (Value[] vals){
    if (vals != null)
      return vals[0];
    else return null;
  }

  /**
   * Method for testing whether a word
   * is an initial letter plus a last name
   * (e.g. wwoods) and returns an appropriate
   * dictprop value if so.
   */
  public Value[] initialPlusLastnameVal(Word word, Word lastname) {
    //tbd implement this, and test the corresponding rule in Morph_en.
    // but since this option is turned off because it finds too many false positives
    // this is not a high priority
    if (authorFlag)
      trace("fake initialPlusLastnameVal(" + word.printString() + " , " +
            lastname.printString() + ") returning null");
   return null;
  }

  /**
   * make-suffix-def (lex root suffix) makes up the definition of the
   * word lex as derived from root by the addition of suffix.
   */
  // pm 21apr04
  public Value[] makeSuffixDef(Word lexword, Word rt,
                                       Word suf) {
    if (suf == null) {
      if (authorFlag)
        System.out.println ("*** error: no suffix in makeSuffixDef for " +
                            lexword + " with " + rt);
      suf = dict.makeWord("null-suffix");
    }
    if (authorFlag)
      trace("makeSuffixDef(" + lexword.printString() + ", " +
            rt.printString() + ", " + suf.printString() + ") ");
    Word rel, def, lexicalRel;
    String relStr;
    lexicalRel = (Word)suf.getdict(dict.relationAtom);
    if (lexicalRel != null){
      rel = lexicalRel;
      relStr = rel.getWordString();
    } else {
      String str = suf.getWordString();
      if (str.equals("ic") || // "acidic"
          str.equals("atic") || // "dogmatic", "schematic"
          str.equals("otic"))  // "despotic"
          relStr ="related_to";
      else if (str.equals("ness")){  // "happiness"
        if (rt.isFormOfCat(dict.adjCategory))
          relStr = "state_of_being";
        else if (rt.isFormOfCat(dict.nounCategory))
          relStr = "state_of_being_a";
        else relStr = "related_to";
      }else if (str.equals("tion") || str.equals("ment") ||
        str.equals("ure") || str.equals("ry")) // "erasure"
        relStr = "nominalization_of";
      else if (((str.equals("al")) || str.equals("y")) &&
               rt.isFormOfCat(dict.verbCategory))
        relStr = "nominalization_of";
      // "al" choices "remedial" vs "tidal"
      // "y" depends on root v "perjury"  or not "funny"
      else relStr = "related_to";
      rel = dict.makeWord(relStr);
    }
    rel.addWordCategory(dict.prepCategory, 0);
    //tbd  need to figure out a good cstruct here?
    String defStr = relStr + "_" + rt.getWordString();
    def = dict.makeWord(defStr);
    lexword.addIkoParent(def);
    return new Value[]{dict.rootAtom, rt, dict.suffixAtom, suf};
  }

  public void addUnitInfo(Word towd, Word fromwd){
    if (authorFlag)
      trace("first stab addUnitInfo(" + towd.printString() + ", " +
            fromwd.printString() + ") ");
    towd.addIioParents(fromwd.getIioParents());
  }

  public Value addToLeftCompoundEntries(Word w1, Word w2){
    if (authorFlag)
      trace("weak addToLeftCompoundEntries("+  w1.printString() + ", " + 
            w2.printString() +  ") returning null");
    return dict.makeListOfWords(w1, w2);
  }

  public Value addToRightCompoundEntries(Word w1, Word w2){
    if (authorFlag)
      trace("weak addToRightCompoundEntries("+  w1.printString() + ", " + 
            w2.printString() +  ") returning null");
    return dict.makeListOfWords(w1, w2);
  }

  public void addCatSenseList(MorphState state, Category cat,
                              Word root, List dslist){
    // use the elements of the list .. but we can't just grab the list
    int len = dslist.length();
    Value[] cv = new Value[len];
    for (int ii=0; ii<len; ii++) cv[ii] = dslist.elementAt(ii);
    addCatSense(state, cat, root, cv);
   } 
 
  public void addCatSenses(MorphState state, Category cat,
                           List lexAndDStructs){
     if (lexAndDStructs != null) 
         for (int i=0; i<lexAndDStructs.length(); i++)
             addCatSenseList(state, cat, (Word)lexAndDStructs.elementAt(i), 
                             (List)lexAndDStructs.elementAt(i+1));
   } 
 
  public void addCompoundSense(Word lw, List wdlist){
    if (authorFlag)
      trace("weak 9apr04 addCompoundSense(" + lw.printString() +
            ", " + wdlist + ")");
    // some callers nest the list too deeply..pm 22apr04
    List ll = wdlist;
    if (ll.length() < 2){
        Value vl = ll.elementAt(0);
        if (vl.listp()) ll = (List)vl;
    }
    lw.addCompoundOf(ll);
    // tbd -- probably needs to update compounds too??
  }

  /** CheckVariant (word killNum addString) checks
   * whether word is a variant of the word formed by
   * killing killNum letters from the end and adding
   * addString.  E.g., (check-variant 'billie 2 'y)
   * would by true if 'billy is listed in the 'variant-of
   * property of 'billie."    pm7may04
   */
  public boolean checkVariant(Word word, int killNum, String addString){
    boolean match = false;
    String wdstr = word.getWordString();
    int len = wdstr.length();
    if (len>killNum){
        String tvarstr = wdstr.substring(0,len-killNum) + 
                         addString.toLowerCase();
        if (authorFlag)
          trace("checkVariant looking for " + tvarstr);
        Word[][] allVarWds = new Word[4][];
        allVarWds[0] = word.getVariantOf();
        allVarWds[1] = word.getVariants();
        allVarWds[2] = word.getNicknameOf();
        allVarWds[3] = word.getNicknames();
        for (int i=0; i<4; i++)
            if (allVarWds[i] != null)
                for (int j=0; j<allVarWds[i].length; j++)
                    if (allVarWds[i][j].getWordString().equals(tvarstr)){
                        match = true;
                        break;
                    }
    }
    if (authorFlag)
      trace("checkVariant(" + word.printString() + ", " + killNum +
            ", " + addString + ") returning " + match);
    return match;
  }

  /**
   *  isAdvPlusPastpart (root double ending) tests to see
   * if root+double+ending is a compound of an adverb
   * with a transitive past participle, like
   * hungertested.
   * double is t or nil indicating whether or not to
   * double the final letter of root.
   * This function is used in morph rules.
   */
  public boolean isAdvPlusPastpart(Word wroot, boolean flag, 
                                   Word ending){
    if (authorFlag)
      trace("fake isAdvPlusPastpart(" + wroot.printString() + ", " + flag +
            ", " + ending.printString() + ") returning false");
    return false;
  }

  public boolean isNPlusPastpart(Word wroot, boolean flag, 
                                 Word ending){
    if (authorFlag)
      trace("fake isNPlusPastpart(" + wroot.printString() + ", " + flag +
            ", " + ending.printString() + ") returning false");
   return false;
  }

  /**
   *  compoundWithSuffix (lex cat false-root kill suffix relation)
   * makes a lexical entry for lex using the information on the
   * 'compound-of property of false-root and adding the indicated suffix.
   *
   * The first element of the compounds-property should be a list of
   * two words, word1 and word2 that form the compound-word false-root.
   * The word lex will be interpreted as having a head constructed from
   * word2 plus the suffix, modified by word1 using the indicated relation.
   *
   * If kill is a number greater than 0, then that number of characters
   * is dropped from the end of word2 before adding suffix.  If kill is
   * -1, then the final letter of word2 is doubled before adding suffix.
   * If kill is 0, then nothing is added or deleted before adding suffix.
   * For example, the lexical entry for 'courseplotter is constructed
   * by the expression (compound-with-suffix lex 'n root -1 'er 'of)
   * after an apparent (but false) root, 'courseplot, has been obtained
   * by removing the characters corresponding to the pattern (& e r)
   * from the word lex = 'courseplotter.
   */
  // pm 9apr04
  public List compoundWithSuffix(Word lx, Category ct,
                                         Word flsrt, int ii,
                                         Word sf, Word prep){
    // returns list of alternating words and list-form of their dicstructs
    if (authorFlag)
      trace("fake compoundWithSuffix(" + lx.printString() + ", " +
            ct.printString() + ", " + flsrt.printString() + ", " + ii + ", " +
            sf.printString() + ", " + prep.printString() + ") returning null");
    return null;
  }

  /**
   * copyFeatsForCategory(Lexicon.Category cat)
   * isolates the details of which features should be copied for
   * a given major category.  It is currently simple -- it returns
   * noting if the category is not matched in the table.
   * may need some default feats for no match?  pm 9apr04.
   * 20apr04 changed exact cat matches to subsumption tests
   */
  public Atom[] copyFeatsForCategory(Category cat){
      Atom[] feats = null;
      if (dict.nounCategory.subsumesCategory(cat))
          feats = new Atom[]
              {dict.numberAtom, dict.massAtom, dict.penaltyAtom};
      else if (dict.adjCategory.subsumesCategory(cat))
          feats = new Atom[]
              {dict.comparativeAtom, dict.superlativeAtom,
                             dict.scalarAtom, dict.penaltyAtom};
      else if (dict.verbCategory.subsumesCategory(cat))
          feats = new Atom[]
              {dict.tnsAtom, dict.pncodeAtom, dict.pastpartAtom,
               dict.prespartAtom, dict.untensedAtom, dict.penaltyAtom};
      return feats;
  }

  /** addCompSufCatSenses is a "call convertor" to switch the old
   * form of lisp calls that depended on a "suffix flag" to more
   * openly switch into compound or suffix calls.
   */
  public void addCompSufCatSenses(MorphState mst, Category lcat,
                                         Word rtwd, Word sfwd,
                                        Category ct, String conct){
    addCompoundSuffixCatSenses(mst, lcat, mst.lex, ct, rtwd, 0,
                               sfwd, dict.modifiedByWord);
  }
    
  /** addCompoundSuffixCatSenses is
   *  derived from lisp make-forms-for-compound in suffix case;
   * and from compound-with-suffix (lex cat false-root kill suffix relation)
   *
   * compound-with-suffix (lex cat false-root kill suffix relation)
   *  makes a lexical entry for lex using the information on the
   * 'compound-of property of false-root and adding the indicated suffix.
   * The first element of the compounds-property should be a list of
   * two words, word1 and word2 that form the compound-word false-root.
   * The word lex will be interpreted as having a head constructed from
   * word2 plus the suffix, modified by word1 using the indicated relation.
   * If kill is a number greater than 0, then that number of characters
   * is dropped from the end of word2 before adding suffix.  If kill is
   * -1, then the final letter of word2 is doubled before adding suffix.
   * If kill is 0, then nothing is added or deleted before adding suffix.
   *
   * For example, the lexical entry for 'courseplotter is constructed
   * by the expression (compound-with-suffix lex 'n root -1 'er 'of)
   * after an apparent (but false) root, 'courseplot, has been obtained
   * by removing three characters, corresponding to the pattern (& e r),
   * from the word lex = 'courseplotter."
   *
   *  make-forms-for-compound (word1 word2 cat connector suffix-flag)
   *  constructs a forms list for the cat senses of a compound
   *  word by copying information from the cat senses of word1
   *  and compounding word2 with the root form of word1 using
   *  the specified connector if specified 
   */
  public void addCompoundSuffixCatSenses(MorphState mst, Category lcat,
                                         Word lxwd,
                                         Category ct,
                                         Word flsrtwd,
                                         int kill, Word sfwd,
                                         Word prep){ 
 
    if (authorFlag)
      trace("addCompoundSuffixCatSenses(" + lcat + ", " + lxwd.printString() +
            ", " + ct.printString() + ", " + flsrtwd.printString() + ", " +
            kill + ", " + sfwd.printString() + ", " + prep.printString() +
            ")");
   Value[] comps = flsrtwd.getCompoundOf();
   if (comps == null) {
       System.err.println("expected a compound-of property:\n" +
                          flsrtwd.printEntryString());
       return;
   }
   Word[] compwds = ((List)comps[0]).wordsIn();
   Word wd2 = compwds[1];
   String headstr = wd2.getWordString();
   int len = headstr.length();
   if (kill > 0 && len > kill) headstr = headstr.substring(0,len-kill-1);
   if (kill < 0) headstr = headstr + headstr.substring(len-2,len-1);
   headstr = headstr + sfwd.getWordString();
   Word head = dict.knownOrScratchWord(headstr, mst.morphCache);
   Value[] rootFeats = copyRootFeats(head, ct);
   
   String modname = headstr + prep.getWordString() + sfwd.getWordString();
   Word compwd = dict.knownOrScratchWord(modname, mst.morphCache);
   dict.makeStructuredConcept(compwd, lcat, dict.makeModifierList(sfwd,prep,head));
   List derivList =
            dict.makeList(new Value[]
            {head, prep, sfwd, dict.makeAtom("add-compound-suffix-cat-senses")});

   lxwd.setRoots(new Word[]{head});
   lxwd.remdict(dict.guessedAtom);
   lxwd.addFalseRoot(flsrtwd);
   Atom infCode;
   if (dict.anynCategory.subsumesCategory(lcat))
       infCode = nounInflectCode(lxwd);
   else if (dict.verbCategory.subsumesCategory(lcat))
       infCode = verbInflectCode(lxwd);
   else infCode = dict.starIcodeAtom;

   Value[] compFeats = new Value[]{ dict.suffixAtom, sfwd,
                                    dict.derivationAtom, derivList,
                                    dict.icodesAtom, infCode};
   addCatSense(mst, lcat, lxwd, LexiconUtil.concatArrays(compFeats,rootFeats));

  }

  /** addCompoundCatSenses is derived from
   *  lisp make-forms-for-compound in the "compound" case;
   *  make-forms-for-compound (word1 word2 cat connector)
   *  constructs a forms list for the cat senses of a compound
   *  word by copying information from the cat senses of word2
   *  and compounding word1 with the root form of word2 using
   *  the specified connector (if specified).
   *  lcat is the targetted lexical category, while rcat is root
   *  category type to use for seeking properties.
   */
  public void addCompoundCatSenses (MorphState mst, Category lcat,
                                      Word mod, Word rootwd,
                                      Category rcat, String conct){
    String conctStr = conct;
    if (conctStr == null) conctStr = "<null>";
    //trace
    if (authorFlag)
//      System.out.println("weak 9apr04 addCompoundCatSenses(" + lcat + ", " +
      trace("weak 9apr04 addCompoundCatSenses(" + lcat + ", " +
                         mod.printString() + ", " +
                         rootwd.printString() + ", " + rcat + ", " +
                         conctStr + ") called");
    Word rtrt = rootwd.getRoot();
    Word useRoot = rtrt;
    if (rtrt == null) useRoot = rootwd;
    Category useCat = lcat;
    if (lcat.subsumesCategory(rcat)) useCat = rcat;

    Value[] rootFeats = copyRootFeats(rootwd, rcat);
    String modname = rootwd.getWordString() + "_modified_by_" + mod.getWordString();
//    String cname = "!" + useCat.printString() + "/" + mst.lexString + "/" +
//                   modname;
    Word compwd = dict.knownOrScratchWord(modname, mst.morphCache);
    dict.makeStructuredConcept(compwd, useCat, dict.makeModifierList(mod, rootwd));
    List derivList =
        dict.makeList(new Value[]
            {useRoot, dict.prefixAtom, mod, dict.makeAtom("add-compound-cat-senses")});
    Value[] compFeats = new Value[]{
        dict.rootAtom, useRoot, dict.ikoAtom, compwd, dict.prefixAtom, mod,
        dict.entailsAtom, mod, dict.derivationAtom, derivList
    };
    addCatSense(mst, useCat, mst.lex, LexiconUtil.concatArrays(compFeats,rootFeats));
  }

  public Word makeStructuredCompound(Word word, Category cat,
                                             List struct){
    // tweaked 9apr04 pmartin
    dict.makeStructuredConcept(word, cat, struct);
    return word;
  }

  /* Test if word is in category cat and is a compound word. */
  public boolean isCompoundCat(Word word, Category cat){
    boolean icc = (isFormOfCat(word, cat) && 
                   // test for compound-of property
                   word.getCompoundOf() != null);
    if (authorFlag)
      trace("isCompoundCat(" + word.printString() + ", " + cat.printString() +
            ") returning " + icc);
    return icc;
  }

  public Value[] copyRootFeats(Word rootwd, Category rcat){
    Atom[] copyfeats = copyFeatsForCategory(rcat);
    Value[] rootFeats = null;
    if (copyfeats != null){
      ArrayList cfv = new ArrayList();
      for (int i=0; i<copyfeats.length; i++){
        Value v = rootwd.getdict(copyfeats[i]);
        if (v != null){
          cfv.add(copyfeats[i]);
          cfv.add(v);
        }
      }
      if (cfv.size() > 0){
        rootFeats = new Value[cfv.size()];
        rootFeats = (Value[])(cfv.toArray(rootFeats));
      }
    }
    return rootFeats;
  }

  public String makePlural(Word wd){
    if (authorFlag)
      trace("fake makePlural(" + wd.printString() + 
            ") returning word string with an S");
    return wd.getWordString() + "s";
  }

  public Word[] getRightConstituents(Word wd){
    if (authorFlag)
      trace("fake getRightConstituents(" + wd.printString() +
            ") returning null");
    return null;
  }

  /**
   * suffixedVerbP (word) tests if word is a suffixed form of a verb.
   */
  public boolean suffixedVerbP (Word wd){
    boolean ans = false;
    boolean ans1 = (wd.getSuffixes() != null);
    if (ans1){
      Word[] rts = wd.getRoots();
      if (rts != null)
         for (int i=0; i<rts.length; i++)
            if (rts[i].isFormOfCat(dict.verbCategory)){
                ans = true;
                break;
            }
    }
    if (!ans){
        Word[] sfxs = wd.getSuffixes();
        if (sfxs != null)
            for (int i=0; i<sfxs.length; i++)
                if (sfxs[i].isFormOfCat(dict.verbCategory)){
                    ans = true;
                    break;
                }
    }
        
  if (authorFlag)
    trace("suffixedVerbP(" + wd.printString() + ") returning "+ ans);
    return ans;
  }

  /** lexicalPrefixTest (MorphState state, Word lex) tests if lex can be
   * analyzed as a prefix plus root (for a prefix
   * that is known in the lexicon but not as a
   * prefix rule) -- used in lexical-prefix-rules.
   * Note: this can set properties on words that
   * are later analyzed differently
   */
  public boolean lexicalPrefixTest (MorphState state, Word wd){//pm 01apr04
      // waw 03-11-05 added MorphState argument to avoid potential looping
      // may wish to set these differently, so bind up front
      int preflimit = 2;
      int sufflimit = 2;
      String ws = wd.getWordString();
      int wslen = ws.length();
      boolean foundOne = false;
      Hashtable morphCache = null;
      if (state != null) morphCache = state.morphCache;
      if (morphCache == null) morphCache = wd.morphCache;
      for (int ii=preflimit; ii <= (wslen-sufflimit); ii++){
          String prefStr = ws.substring(0, ii);
          String suffStr = ws.substring(ii);
          // don't do anything if prefix is already explicitly handled by a rule
          if (sp_rulePrefixes.contains(prefStr) ||
              leftCompoundExceptions.contains(prefStr) ||
              rightCompoundExceptions.contains(suffStr) ||
              !dict.isKnownWord(prefStr)  )
              continue;
          Word prefWd = dict.getWord(prefStr); 
          if (prefWd == null ||   // this shouldn't be possible...
              ! prefWd.isFormOfCat(dict.prefixCategory) )
              continue;

          if (suffStr.indexOf("-") != -1) continue; // no hyphens in suff
          Word suffWd = dict.knownOrScratchWord(suffStr, morphCache);
// issue: waw 03-11-05 is the morphCache from the word ever good enough here?  We
// really need to give lexicalPrefixTest a state and take the morphCache from there.
// Failing that, the morphCache from wd should work, if we have one, since that means
// we are calling lexicalPrefixTest on a scratch word that we are analyzing (in which
// case it should have the correct morphCache as its morphCache value).  But if we
// are analyzing a real word with an empty entry then it would have a null
// morphCache value and the postulated root might have to be a scratch word and we
// won't have a morphCache to put it in, so there is a possiblity that when it is
// analyzed there could be a recursive loop.
          if (rootGoodForPrefix(suffWd)) {
              foundOne = true;
              addCompoundSenseStuff(wd, prefWd, suffWd);
          }else{
              // try gemination
              char gem = ws.charAt(ii-1);
              if ((gem == 'o') || //(morpho+ology => morphology
                  (gem == 'a') ){ // contra+alto => contralto
                  suffStr = ws.substring(ii-1);
                  suffWd = dict.knownOrScratchWord(suffStr, morphCache);
                  if (rootGoodForPrefix(suffWd)) {
                      foundOne = true;
                      addCompoundSenseStuff(wd, prefWd, suffWd);
                  }
              }
          }
      }
      return foundOne;
   }

  // auxilliary method -- integrate with others?? pm 01apr04
  // tbd also should add compound meaning?
  public void addCompoundSenseStuff(Word wd, Word pref,
                                      Word suf){
        List derivList =
            dict.makeList(new Value[]
            {suf, dict.prefixAtom, pref,
             dict.makeAtom("lexical-prefix-test")});
        wd.addtodictprop(dict.derivationAtom, derivList);
        wd.addPrefix(pref);
        wd.addRoot(suf);
        wd.addCompoundOf(new Word[]{pref, suf});
  }

  public boolean rootGoodForPrefix(Word rootWord){
    return (plausibleRoot (rootWord) &&
            rootWord.isFormOfCat(dict.compoundableCats) &&
            // block "mere" in "blastomere"
            ! rootWord.isFormOfCat(dict.suffixCategory));
  }

  public boolean romanNumeralP (Word wd){
    if (authorFlag)
      trace("fake romanNumeralP(" + wd.printString() + ") returning false");
    return false;
  }

  public boolean isIrregularVerbForm (Word wd){//04may04
    String wdstr = wd.printString();
    boolean ans = (wd.isFormOfCat(dict.pppCategory));
    boolean ans2 = false;
    if (ans){
        ans2 =(!wdstr.endsWith("ed") ||
               wdstr.equals("fed") ||
               wdstr.equals("led") ||  // added pm 5may04
               wdstr.equals("shed") ||
               wdstr.equals("wed"));
        if (!ans2){
            Value[] cls = wd.getCompoundOf();
            if (cls != null)
                for (int i=0; i<cls.length; i++){
                    Word[] comps = ((List)cls[i]).wordsIn();
                    int len = comps.length;
                    if (isIrregularVerbForm(comps[len-1])){
                        ans2 = true;
                        break;
                    }
                }
        }
        ans = ans2;
    }
  // old lisp included these
  //           (memb word '(beat bet bid burst burnt
  //                        cast cut dipt hit hurt knit
  //                        let plead put quit read
  //                        set shed shit shut slit 
  //                        spread thrust wed wet))
  //also  need crept run saw swept wept lept ...      
    if (authorFlag)
      trace("isIrregularVerbForm(" + wdstr + ") returning " + ans);
    return ans;
  }

// This one should replace the next one when the rule compiler is changed
// to produce a state variable for this
  public boolean lastCharCapitalizedP(MorphState state){
    if (authorFlag)
      trace("fake lastCharCapitalizedP(state) returning false");
    return false;
  }

  public boolean lastCharCapitalizedP(){
    if (authorFlag)
      trace("fake lastCharCapitalizedP() returning false");
    return false;
  }

  /** hasHyphenatedRootVerb (word1 connector word2) tests
   * if the hyphenated pair word1-word2 has a corresponding
   * root verb -- e.g., heart-warming doesn't.
   *  This is deprecated in favor of has-known-compound-root
   */
  public boolean hasHyphenatedRootVerb(Word w1, String cn,
                                       Word w2){ //05apr04 pm
    Word[] iroots = w2.getInflectionRoots();
    if (iroots != null && iroots.length > 0){
      for (int i=0; i<iroots.length; i++){
        Word proot = iroots[i];
        if (proot != w2){
            String cstr = w1.getWordString() + cn +
                          proot.getWordString();
            Word croot = dict.getWord(cstr);
            // waw 03-11-05 eliminated making scratch word from this test
            if (croot != null &&
                croot.isRootOfCat(dict.verbCategory))
              return true;
        }
      }
    }
    return false;
  }
    
  /** hasKnownCompoundRoot (word1 connector word2 &optional category)
   * tests whether word1 plus connector plus some root of word2
   * is a known word, and in the indicated category (if category
   * is specified) -- used in morph-compound-rules to see if a
   * hyphenated pair word1-word2 has a corresponding root verb
   * -- e.g., heart-warming doesn't.
   *   pmartin addition on 05apr04 is to also look for category
   *    compound of wd1 and wd2 without a connector
   */
  public boolean hasKnownCompoundRoot(Word wd1, String con, 
                                      Word wd2){
    return hasKnownCompoundRoot(wd1, con, wd2, null);
  }

  // pmartin 05apr04
  public boolean hasKnownCompoundRoot(Word wd1, String con, 
                                      Word wd2, Category cat){
    Word[] iroots = wd2.getInflectionRoots();
    if (iroots != null && iroots.length > 0){
        for (int i=0; i<iroots.length; i++){
            Word testroot = iroots[i];
            if (testroot != wd2){
                String cstr = wd1.getWordString() + con +
                    testroot.getWordString();
                Word croot = dict.getWord(cstr);
                // waw 03-11-05 eliminated making scratch word from this test
                if (croot != null &&
                    (cat == null || croot.isRootOfCat(cat)))
                    return true;
                cstr = wd1.getWordString() + testroot.getWordString();
                croot = dict.getWord(cstr);
                // waw 03-11-05 eliminated making scratch word from this test
                if (croot != null &&
                    (cat == null || croot.isRootOfCat(cat)))
                    return true;
            }
        }
    }
    return false;
  }

  public Word[] morphInflect(Word wd, Atom nam){
    if (authorFlag)
      trace("fake morphInflect(" + wd.printString() + ", " +
            nam.printString() + ") returning null");
    return null;
  }

  public Word firstInflectionRoot(Word wd){
    // tweaked pmartin 16aug01
    Word[] rts = wd.getInflectionRoots();
    Word rt = null;
    if ((rts == null) || (rts.length < 1)) rt = null;
    else rt = rts[0];
    if (authorFlag)
      trace("weak firstInflectionRoot(" + wd.printString() + 
            ") returning " + rt);
    return rt;
  }

  public Value[] wordListCheck(Word wd, Category cat){
    // This morph engine does not provide for external word list checks
    // for nouns and verbs.
    return null;
  }


  /**
   * nounInflectCode (word) computes the default inflection
   * code for the indicated word as a noun.  
   */
  public Atom nounInflectCode(Word nounwd){
    //hand-converted pmartin 23july01

    String wdstr = nounwd.getWordString();
    // this is lowercase string of the word
    int wlen = wdstr.length();
    char lastCh = wdstr.charAt(wlen-1);
    if ((wlen < 3) || !(Character.isLetter(lastCh)))
        //;e.g. a's, cd's, m-16's
        return atom_dashApostropheDashS;
  
    else if (((consonantChars.indexOf(wdstr.charAt(wlen-3)) > -1) ||
              ((wlen > 3) && 
               (wdstr.charAt(wlen-4) == 'q') &&
               (wdstr.charAt(wlen-3) == 'u'))) &&  
             (vowelChars.indexOf(wdstr.charAt(wlen-2)) > -1) &&
             ("sz".indexOf(lastCh) > -1) &&
             (finalStress(nounwd)))
        return atom_dashStarES;
 
    else if (((lastCh == 'h') &&
              ("cs".indexOf(wdstr.charAt(wlen-2)) > -1)) ||
             ("jsxz".indexOf(lastCh) > -1))
        return atom_dashES;
 
    else if ((lastCh == 'y') &&
             // y preceded by a consonant
             ((consonantChars.indexOf(wdstr.charAt(wlen-2)) > -1) ||
              (wdstr.charAt(wlen-3) == 'q') &&
              (wdstr.charAt(wlen-2) == 'u'))  &&
             // and not a noun ONLY by being a proper name
             (nounwd.isNonnameFormOfCat(cat_nn)))
        return atom_dashIES;
 
    else if ((wdstr.endsWith("human")) || (wdstr.endsWith("german")))
        return atom_dashS;
 
    else if (wdstr.endsWith("man"))
        return atom_dashMen;
 
    else if ((wdstr.endsWith("-in-law")) ||  // "xxes-in-law"
             (wdstr.indexOf("-per") > -1))   // "parts-per-million"
        return atom_dashIrr;
 
    else return atom_dashS;

  }

  /**
   * verbInflectCode (word) computes the default inflection
   * code for the indicated word as a verb.  
   */
  public Atom verbInflectCode(Word verbwd){
   //hand-converted pmartin 23july01

   String wdstr = verbwd.getWordString();
    // this is lowercase string of the word
    int wlen = wdstr.length();
    char lastCh = wdstr.charAt(wlen-1);

    if (wlen < 1) 
        return null;

    else if (lastCh == 'e')
        return atom_sDashD;

    else if ((wlen > 1) && (lastCh == 'h') &&
              ("cs".indexOf(wdstr.charAt(wlen-2)) > -1))  
        return atom_esDashEd;
 
    else if ("jsxz".indexOf(lastCh) > -1)
        return atom_esDashEd;

    else if ((wlen > 2) && 
             (wdstr.endsWith("er")) &&
             (polysyllabic(verbwd)) &&
             //  ** doubling depends on stress in multisyllable words
             // ** these need to be checked
             (consonantChars.indexOf(wdstr.charAt(wlen-3)) > -1))
        return atom_sDashEd;

    else if ((wlen > 2) && 
             (wdstr.endsWith("et")) &&
             (polysyllabic(verbwd)) &&
             // ** these need to be checked
             (consonantChars.indexOf(wdstr.charAt(wlen-3)) > -1))
        return atom_sDashEdDashStarEd;

    else if ((wlen > 2) && 
             (lastCh == 'l') &&
             (vowelChars.indexOf(wdstr.charAt(wlen-2)) > -1) &&
             (polysyllabic(verbwd)) &&
             // ** these need to be checked
             (consonantChars.indexOf(wdstr.charAt(wlen-3)) > -1))
        return atom_sDashEdDashStarEd;

    else if ((wlen > 2) &&  
             ("bdglmnprt".indexOf(lastCh) > -1) &&
             (vowelChars.indexOf(wdstr.charAt(wlen-2)) > -1) &&
             (consonantChars.indexOf(wdstr.charAt(wlen-3)) > -1))
        return atom_sDashStarEd;
 
    else if  (consonantChars.indexOf(lastCh) > -1)
        return atom_sDashEd;
 
    else if (lastCh == wdstr.charAt(wlen-2))
        //words ending in double letters other than s add s
        return atom_sDashEd;

    //// commented out in the Lisp version
    //    else if ((lastCh == 'o') &&
    //            (consonantChars.indexOf(wdstr.charAt(wlen-2)) > -1))
    //        return atom_esDashEd;

    else return atom_sDashEd; // e.g. "ski"
  }

  /**
   *  adj-inflect-code (word) computes the default inflection
   *  code for the indicated word.  
   */
  public Atom adjInflectCode(Word adjwd){
  //hand-converted pmartin 23july01
   String wdstr = adjwd.getWordString();
    // this is lowercase string of the word
    int wlen = wdstr.length();
    char lastCh = wdstr.charAt(wlen-1);

    if (wlen < 2) 
        return null;

    else if (lastCh == 'e')
        return atom_rDashSt;

    else if ((lastCh == 'y') &&
             ((consonantChars.indexOf(wdstr.charAt(wlen-2)) > -1) ||
              ((wdstr.charAt(wlen-3) == 'q') &&
               (wdstr.charAt(wlen-2) == 'u')))) 
        return atom_ierDashIest;

    else return atom_erDashEst;
  }

  /**
   * final-stress (word) tests if word has final
   * stress (used in morph rules to determine whether
   * to double final letters (deferred vs suffered).
   * Since we currently don't know this information
   * about words, it returns true only when the word
   * is monosyllabic and thus trivially final stress.
   */
  public boolean finalStress (Word wd){
    return ((syllabic(wd)) && !(polysyllabic(wd)));
  }

  /**
   * isUndoableVerb(word) tests if the specified
   * word can be an undoable verb.  It does this by
   * checking to see if the corresponding undo verb
   * is known.  Most verbs are not undoable.  We
   * record those that are by explicitly entering 
   * their un forms in the lexicon or in the knownwords
   * list  -- e.g., unfreeze.
   */
  public boolean isUndoableVerb (Word wd){ //pmartin 24july01
     return isUndoableVerb(wd, null);
  }

  // tweaked redone for HashSet pm 31mar04
  public boolean isUndoableVerb (Word wd, HashSet passed){ 
    //pmartin 24july01 
    if (!(wd.isFormOfCat(cat_v))) return false;
    if (wd.isRootOfCat(cat_v)){
        if (wd.hasSuffix(wordSet_Ise_Ize)) return true;
        else {
            Word unroot = dict.getWord("un"+wd.getWordString());
            return ((unroot != null) &&
                    (unroot.isRootOfCat(cat_v)));
        }
    }
    else if (passed == null || passed.contains(wd)){ 
        return false;
    }
    else {
        Word[] ir = wd.getInflectionRoots(); // no morph here!
        if ((ir == null) || (ir.length == 0)) return false;
        if (passed == null) passed = new HashSet();
        passed.add(wd);
        for (int j=0; j<ir.length; j++)
            if (isUndoableVerb(ir[j], passed)) return true;
        return false;
    }
  }

//   public boolean isUndoableVerb (Lexicon.Word wd, Lexicon.Word[] passed){ 
//     //pmartin 24july01
//     if (!(wd.isFormOfCat(cat_v))) return false;
//     if (wd.isRootOfCat(cat_v)){
//         if (wd.hasSuffix(wordSet_Ise_Ize)) return true;
//         else {
//             Lexicon.Word unroot = dict.packLex("un", wd.getWordString());
//             return ((unroot.isKnownWord()) &&
//                     (unroot.isRootOfCat(cat_v)));
//         }
//     }
//     else if (Lexicon.isMembOfArray(wd ,passed)){ 
//         return false;
//     }
//     else {
//         Lexicon.Word[] ir = wd.getInflectionRoots(); // no morph here!
//         if ((ir == null) || (ir.length == 0)) return false;
//         int len = 0;
//         if (passed != null) len = passed.length;
//         Lexicon.Word[] newPassed = new Lexicon.Word[len+1];
//         for (int i=0; i<len; i++) newPassed[i+1] = passed[i];
//         newPassed[0] = wd;
//         for (int j=0; j<ir.length; j++)
//             if (isUndoableVerb(ir[j], newPassed)) return true;
//         return false;
//     }
//   }


  /**
   * For identifying class when debugging or tracing rules.
   */

  protected static String className = "MorphEngFns";

} // end of MorphEngFns class
