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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;
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
 * This is an English specialization of MorphEngine.
 *
 * @author W. A. Woods
 * 
 * @version        1.0
 * 
 * The rules for this system were developed by W. A. Woods
 * 
 * @see MorphEngine
 */

public abstract class MorphEngFrame extends MorphEngine {

//state variables extending MorphEngine:

  protected MorphCompoundRule[] morphCompoundRules = null; //to be set at initialization

// variables and methods for compound rules:

//bindings that will be set during initialization:

  protected HashSet leftCompoundExceptions; // pm, 06apr04
  protected HashSet rightCompoundExceptions;  // pm, 06apr04
  protected HashSet illegalRoots; // pm, 06apr04
  protected HashSet legalRoots; // pm, 23apr04
  protected HashSet sp_adjFormingPrefixes; // waw, 16apr04
  protected HashSet sp_adjPrefixes;  // waw, 16apr04
  protected HashSet sp_quantityPrefixes; // waw, 16apr04
  protected HashSet sp_rulePrefixes; // waw 8apr04 setting is auto generated
// issue: waw lisp-to-java translator automatically addes sp_ prefix to global vars
// used in morph rules, so these variables have to be named with that convention
//  protected HashSet morphPrefixes; //pm 7apr04 // waw 8apr04
//  protected HashSet morphSuffixes;  //pm 7apr04 // waw 8apr04
//  protected String[][] illegalBeginnings = null;
//  protected String[][] illegalEndings = null;
  protected HashSet illegalBeginnings;
  protected int illegalBeginningsMax; // max char length of any illegal beg
  protected HashSet illegalEndings;
  protected int illegalEndingsMax; //max chars for longest illegal end
  protected HashSet shortYWordStrings;

// The following bindings are set in Morph_en by actions printed by
// print-java-rules, but they are bound here, because they're used in
// methods above the level of Morph_en:

  protected Atom atom_true;
  protected Atom atom_false;
  protected Atom atom_known;
  protected Atom atom_syllabic;
  protected Atom atom_polysyllabic;
  protected Atom atom_archaic;
  protected Atom atom_abbrev;
  protected Atom atom_hypothesized;
  protected Atom atom_leftCompoundException;
  protected Atom atom_rightCompoundException;
  protected Atom atom_illegalRoot;
    //protected Lexicon.Atom atom_compoundProperty;

  protected Atom atom_iCode;
  protected Atom atom_number;
  protected Atom atom_sg;
  protected Atom atom_pl;
  protected Atom atom_sgSlashPl;
  protected Atom atom_tns;
  protected Atom atom_present;
  protected Atom atom_not3sg;
  protected Atom atom_not13sg;
  protected Atom atom_3sg;
  protected Atom atom_2sg;
  protected Atom atom_1sg;
  protected Atom atom_past;
  protected Atom atom_pastpart;
  protected Atom atom_prespart;
  protected Atom atom_guessed;

  protected Atom atom_morphDispatch;
  protected Atom atom_sBlock;
  protected Atom atom_dBlock;
  protected Atom atom_eBlock;
  protected Atom atom_gBlock;
  protected Atom atom_lBlock;
  protected Atom atom_mBlock;
  protected Atom atom_nBlock;
  protected Atom atom_rBlock;
  protected Atom atom_tBlock;
  protected Atom atom_yBlock;
  protected Atom atom_cBlock;
  protected Atom atom_miscBlock;
  protected Atom atom_tionBlock;
  protected Atom atom_estBlock;
  protected Atom atom_iformBlock;
  protected Atom atom_defaultRule;
  protected Atom atom_prefixDispatch;
  protected Atom atom_aPrefixes;
  protected Atom atom_bPrefixes;
  protected Atom atom_cPrefixes;
  protected Atom atom_dPrefixes;
  protected Atom atom_ePrefixes;
  protected Atom atom_hPrefixes;
  protected Atom atom_iPrefixes;
  protected Atom atom_mPrefixes;
  protected Atom atom_nPrefixes;
  protected Atom atom_oPrefixes;
  protected Atom atom_pPrefixes;
  protected Atom atom_rPrefixes;
  protected Atom atom_sPrefixes;
  protected Atom atom_tPrefixes;
  protected Atom atom_uPrefixes;
  protected Atom atom_miscPrefixes;
  protected Atom atom_lexicalPrefixRules;
  protected Atom atom_morphCompoundRules;
  protected Atom atom_ordinalRules;
  protected Atom atom_pluralRule;
  protected Atom atom_thirdSingRule;
  protected Atom atom_pastRule;
  protected Atom atom_ingRule;
  protected Atom atom_doerRule;
  protected Atom atom_comparativeRule;
  protected Atom atom_superlativeRule;
  protected Atom atom_adverbRule;
  protected Atom atom_nessRule;
  protected Atom atom_ableRule;

  protected Atom atom_prefix;
  protected Atom atom_hasPrefix;
  protected Atom atom_suffix;
  protected Atom atom_hasSuffix;

  protected Atom atom_dashApostropheDashS;
  protected Atom atom_dashStarES;
  protected Atom atom_dashES;
  protected Atom atom_dashS;
  protected Atom atom_dashIES;
  protected Atom atom_dashMen;
  protected Atom atom_dashIrr;
  protected Atom atom_sDashD;
  protected Atom atom_sDashEd;
  protected Atom atom_sDashStarEd;
  protected Atom atom_esDashEd;
  protected Atom atom_sDashEdDashStarEd;
  protected Atom atom_rDashSt;
  protected Atom atom_ierDashIest;
  protected Atom atom_erDashEst;

  protected Category cat_name;
  protected Category cat_citySlashCountry;
  protected Category cat_nvadjadvprefsuff;
  protected Category cat_nnvadjadvprefsuff;
  protected Category cat_anynvadjadvprefsuff;
  protected Category cat_punct;
  protected Category cat_npr;
  protected Category cat_nm;
  protected Category cat_nn;

  protected Category cat_n;
  protected Category cat_v;
  protected Category cat_adj;
  protected Category cat_prefix;

  protected Category cat_ppp;   // past or pastpart (for ed endings)
  protected Category cat_nplpp; // npl or pastpart (for en endings)

  protected Category cat_npl;
  protected Category cat_nsp;
  protected Category cat_past2sg;
  protected Category cat_past3sg;
  protected Category cat_pastnot13sg;
  protected Category cat_past;
  protected Category cat_3sg;
  protected Category cat_2sg;
  protected Category cat_1sg;
  protected Category cat_not13sg;
  protected Category cat_not3sg;
  protected Category cat_pastpart;
  protected Category cat_prespart;

  protected Word word_0;
  protected Word word_1;
  protected Word word_2;
  protected Word word_3;
  protected Word word_un; // 7apr04 pm moved from MorphEngine waw 01-17-05


  /**
   * English-specific methods for guessing plausible forms and roots.
   */

  public boolean isPlausibleFormOfCat(Word thisWord, Category cat){
    boolean result =
           (thisWord.isFormOfCat(cat) &&
            (dict.isKnownWord(thisWord) ||
             // these next clauses are to block creation of "un-verb"s
             !(dict.verbCategory.subsumesCategory(cat)) ||
             !(LexiconUtil.isMembOfArray(word_un, thisWord.getPrefixes()))));
    if (authorFlag && traceFlag) {
      System.out.println("** isPlausibleFormOfCat (" + thisWord + ", " +
                         cat + ") = " + result + ".");
      System.out.println("** hasKnownRoot (" + thisWord + ") = " +
                         hasKnownRoot(thisWord) + ".");
    }
    return result;
  }

  public boolean isPlausibleRootOfCat(Word thisWord, Category cat){
    boolean result =
           (thisWord.isRootOfCat(cat) &&
            (dict.isKnownWord(thisWord) ||
             // these next clauses are to block creation of "un-verb"s
             !(dict.verbCategory.subsumesCategory(cat)) ||
             !(LexiconUtil.isMembOfArray(word_un, thisWord.getPrefixes()))));
    if (authorFlag && traceFlag) {
      System.out.println("** isPlausibleRootOfCat (" + thisWord + ", " +
                         cat + ") = " + result + ".");
      System.out.println("** hasKnownRoot (" + thisWord + ") = " +
                         hasKnownRoot(thisWord) + ".");
    }
    return result;
  }

  protected MorphCompoundRule compoundRule; //current rule for catPreTest & testPreTest to see

  /**
   * Method for making a MorphCompoundRule.
   */

  public MorphCompoundRule cr(String expression, String ruleName, Lexicon dict) {
    MorphCompoundRule result = new MorphCompoundRule(expression, ruleName, dict);
    return result;
  }

  /**
   * Method for converting a temporary MorphRule into a MorphCompoundRule
   * after a Lexicon, dict, has been provided in which to make the categories
   * for the category patterns of the MorphCompoundRule.
   */

  protected MorphCompoundRule[] convertCompoundRules(MorphRule[] ruleSet, String newName,
                                                               Lexicon dict) {
    MorphCompoundRule[] result = new MorphCompoundRule[ruleSet.length];
    for (int i = 0 ; i < ruleSet.length ; i++) {
      //String name = newName + ":" + (i + 1);
      String name = ":" + ruleSet[i].name.substring(6); //remove ":temp-" from name
      result[i] = new MorphCompoundRule(ruleSet[i], name, dict);
    }
    return result;
  }

  /**
   * Determines if a word matches a compound rule for word1 and word2.
   * The variable compoundRule needs to be set before calling this.
   */

  public Vector matchCompoundRule(String wordString, MorphState state,
                                            Word word1,
                                            Word word2,
                                            int depth) {
      if (isFormOfCat(word1, state.compoundRule.categoryPattern[0]) &&
          isFormOfCat(word2, state.compoundRule.categoryPattern[1])) {
        state.word1 = word1;
        state.word2 = word2;
        for (int i =0; i < state.compoundRule.actions.length; i++) {
          if (authorFlag) debug("   doing action: " + state.compoundRule.actions[i]);
          doNumberedAction(state.compoundRule.actions[i], state);
          // catSenses is where results go
        }
      }
      if (state.catSenses.size() > 0) {
        if (authorFlag && traceFlag)
          trace("   finished rule: " + state.compoundRule.name +
                " at depth " + depth +
                "\n        " + "     with matched = "+state.matched+" and "+ 
                state.catSenses.size() + " forms generated");
//        recordResults(word, state.catSenses);
        return state.catSenses;
      }
      else return null;
  }

  /**
   * Method for testing a scratch word for possible decomposition as a
   * compound word.
   */

  Vector morphCompoundTest(Word word, MorphState state) {
   /* short call form */
    return  morphCompoundTest(word, state, null, state.morphCache, 1);
  }


  /**
   * Method for testing a scratch word for possible decomposition as a
   * compound word.
   */

  Vector morphCompoundTest(Word word, MorphState state, Category category) {
   /* short call form */
    return morphCompoundTest(word, state, category, state.morphCache, 1);
  }


  /**
   * Method for testing a scratch word for possible decomposition as a
   * compound word.
   */

  Vector morphCompoundTest(Word word, MorphState state, Category category,
                                        Hashtable useCache) {
    /* short call form */
    return  morphCompoundTest(word, state, category, useCache, 1);
  }


  /**
   * Method for testing a scratch word for possible decomposition as a
   * run-together compound word like 'horseshoe' that is made from the
   * concatenation of two known words.  It makes an appropriate lexicon
   * entry for word from those of the constituent words.
   */

  protected Vector morphCompoundTest(Word word, MorphState state,
                                                  Category category,
                                                  Hashtable useCache, int pass) {
    String lexString = word.getWordString();
    state.lex = word;
    boolean tryPass2 = false;
    boolean tryPass3 = false;
    boolean matchFound = false;
    int minimumCompoundLength = 3;
    int length ;

    if (category == null)
      category = cat_anynvadjadvprefsuff;

    if (((length = lexString.length()) > 4) &&
        (lexString.indexOf('-') < 0) &&
        (lexString.indexOf('_') < 0)) {
        //Value[] tempValueSeqResult ;
      for (int i = length - minimumCompoundLength ; i > 1 ; i--) {
        String word1String = lexString.substring(0, i);
        String word2String = lexString.substring(i);

        state.word1 = dict.getWord(word1String);
        state.word2 = dict.getWord(word2String);
        if (state.word1 == null)
          state.word1 = (Word)useCache.get(word1String);
        if (state.word2 == null)
          state.word2 = (Word)useCache.get(word2String);

        if ((state.word1 != null) &&
            (state.word2 != null) &&
            ((i >= minimumCompoundLength) ||
             isFormOfCat(state.word1 , cat_prefix)) &&
            (!leftCompoundExceptions.contains(word1String)) &&
            (!rightCompoundExceptions.contains(word2String)) &&
//            (((state.word1 != null) && syllabic(state.word1)) || syllabic(word1String)) &&
//            (((state.word2 != null) && syllabic(state.word2)) || syllabic(word2String)) &&
            (plausibleWord(word1String)) &&
            (plausibleWord(word2String)) &&
//tbd: figure out how to invoke morph with cmode == known
//            ((state.word1 != null) || (morph(state.word1, null, atom_known) != null)) &&
//            ((state.word2 != null) || (morph(state.word2, null, atom_known) != null)) &&
            ((state.word1 != null)) && // || (morph(word1, null, atom_known) != null)) &&
            ((state.word2 != null)) && // || (morph(word2, null, atom_known) != null)) &&
            ((isFormOfCat(state.word1 , cat_name) &&
              isFormOfCat(state.word2 , cat_name) &&
              (tryPass3 = true) && //set up to do pass 3 -- only do name compounds on pass 3
              (pass > 2)) ||
             ((isFormOfCat(state.word1 , cat_nnvadjadvprefsuff)) &&
              //(isNonnameFormOfCat(state.word1 , cat_nvadjadvprefsuff)) &&
              (state.word1.getdict(atom_hypothesized) == null) &&
              (!(state.word1.hasFeature(atom_archaic))) &&
              (isFormOfCat(state.word2 , cat_citySlashCountry) ||
               isFormOfCat(state.word2 , cat_nnvadjadvprefsuff)) &&
               //isNonnameFormOfCat(state.word2 , cat_nvadjadvprefsuff)) &&
              // (state.word2.getdict(atom_abbrev) == null) && //don't need this in Java version
              (!inflectedRightCompoundException(state.word2)) && //is this condition necessary?
              (!((pass < 2) &&                       //will only try plural word1 that ends
                 word1String.endsWith("s") &&        //in s in pass 2, so it will get
                 isFormOfCat(state.word1 , cat_npl) && //e.g., minesweeper vs groundskeeper
                 (tryPass2 = true) //set up to do pass 2
                )) &&
              (!((pass < 2) &&                       //will only try past or pastpart
                 word1String.endsWith("ed") &&       //word1 in pass 2, so it will get
                 isFormOfCat(state.word1 , cat_ppp) && //e.g., daredevil (not dared+evil)
                 (tryPass2 = true) //set up to do pass 2
                )) &&
              (!((pass < 2) &&                       //will only try en plural or pastpart
                 word1String.endsWith("en") &&       //word1 in pass 2, so it will get
                 isFormOfCat(state.word1 , cat_nplpp) && //e.g., vaxenable (not vaxen+able)
                 (tryPass2 = true) //set up to do pass 2
                )) &&
              (morphCompoundAnalysis (morphCompoundRules,
                                      ":morph-compound-rules", state,
                                      state.word1, state.word2, null))
              )
             )) {
          matchFound = true;
          if (authorFlag) {
            if (traceFlag)
              trace("morphCompoundTest found match in pass " +
                    pass + " for " + state.word1 + " + " + state.word2);
//            state.lex.markDict(dict.derivationAtom,
//                               dict.makeAtom("morphCompoundTest-pass-" + pass +
//                                             "-for-" + word1String + "+" +
//                                             word2String),
//                               true);
            }
          break;
          }
        }
        if (matchFound){
          return state.catSenses;
        }
        if (!matchFound && (pass == 1) && tryPass2 &&
            (morphCompoundTest(word, state, category, useCache, 2) != null))
          return state.catSenses;
        else if (!matchFound && tryPass3 &&
                 (morphCompoundTest(word, state, category, useCache, 3) != null))
          return state.catSenses;
        else return null;
    }
    return null;
  }

  /**
   * Method to initialize state variables before compound rule match.
   */

   protected void initializeCompoundState(MorphState state) {
  //called before each new morphCompound rule is tried
    state.killLeftNum = 0;
    state.killRightNum = 0;
    state.testval = true;
    state.likelihood = -1;
    state.knownRoot = false;
  }

  /**
   * Attempts the morphological analysis of the current word as a compound
   * of the specified word1 and word2 connected by the indicated connector.
   */

  protected boolean morphCompoundAnalysis (MorphCompoundRule[] rules,
                                           String blockName,
                                           MorphState state,
                                           Word word1,
                                           Word word2,
                                           String useConnector) {
    state.word1 = word1;
    state.word2 = word2;
    state.connector = useConnector;
    Vector results = null;
    if (authorFlag && traceFlag)
      trace(" Analyzing " + state.word1.getWordString() + " + " +
            state.word2.getWordString() +
            " with rules " + blockName + " at depth " + state.depth);
    for (int n = 0; n < rules.length; n++) {
      if (authorFlag) debug("   trying rule " +
              blockName + "-" + (1 + n) + ":" + "\n    " +
              rules[n] + ", at depth " + state.depth);
      initializeCompoundState(state);
      state.compoundRule = rules[n]; // set current rule for matchCompoundRule to operate on
      results = matchCompoundRule(state.lexString, state, state.word1, state.word2,
                                  state.depth);
      if (results != null) { // rule n matched
        if (authorFlag) {
          if (traceFlag)
            trace("  " + state.lexString + ": generated " + state.catSenses.size() +
                  " results for rule " + blockName + "-" + (n+1) + ", at depth " +
                  state.depth);
          state.lex.markDict(dict.derivationAtom,
                             dict.makeAtom("morph-compound-rules-" + (n+1)),
                             true);
        }
        break;
      }
    }
    return ((results != null));
  }

  /**
   * Tests whether the indicated word is an inflected form of a word that
   * is a right-compound exception (i.e., is disallowed as word2 of a
   * compound word analysis).
   */

  protected boolean inflectedRightCompoundException(Word word) {
    Word[] roots = word.getInflectionRoots();
    if (roots == null) return false;
    for (int j = 0 ; j < roots.length ; j++) {
             Word x = roots[j];
             if (rightCompoundExceptions.contains(x.getWordString())) {
               return true;
             }
    }
    return false;
  }

    /**
     * Method that determines if a word string matches the current rule.
     * This version, in MorphEngFrame, overrides a more general one in
     * MorphEngine, for efficiency, since the English morphology rules
     * don't require the full generality of the more general method.
     */

    @Override
    public Vector match(String wordString, MorphRule thisRule, MorphState state,
                           int depth, int skipnum) {
        int killRight = state.rule.killnum;
        int killLeft = state.rule.leftkillnum;
        int patternLength = state.rule.pattern.length;
        int matchCount = 0; //how many times this elt matched
        int started = 0; //flag indicating that right end is bound
        int [] altState = null;
        Vector alts = new Vector();
        HashMap doneStates = new HashMap();
        Long stateCode;
        boolean matched = true;

        //skipnum positions at the right end have already been
        //tested by the dispatch method
        int length = wordString.length();
        int position = length-1; //position in word
        int i = patternLength-1; //position in pattern
        if (state.rule.rightAnchor || (skipnum > 0)) started = 1; //turn on adjacency constraint
//note: the following won't work for patterns like +, *, and ?
// which can match more or less than one position per pattern element
        if (state.rule.leftAnchor && !state.rule.rightAnchor && (position > i)) {
          position = i;
          started = 1;
        }
        matched = true;
        while ((matched && (i > -1)) ||
               (!matched && alts.size() > 0)) {
            if ((i < 0) || !matched || (position < 0)) {
              if ((position < 0) && matched && (i > -1) &&
                  (state.rule.pattern[i].length() > 0) &&
                  ("?*<>".indexOf(state.rule.pattern[i].charAt(0)) > -1)) {
                //go on and try these pattern cases
              }
              else if (alts.size() > 0) {
                altState = (int[])alts.lastElement();
                alts.removeElement(altState);
                if (authorFlag && debugFlag)
                  debug("   resuming alternative: " + (altState[1]+1) + ":" +
                        altState[2] + " at " + (altState[0]+1));
                position = altState[0];
                i = altState[1]; //this is always >=0 because altState only saves when i > -1
                matchCount = altState[2]; //how many times this elt matched
                killLeft = altState[5]; //leftkillnum
                killRight = altState[6]; //(right) killnum
                started = altState[7];
                matched = true;
              }
              else {
                matched = false;
                if (authorFlag && debugFlag)
                  debug("    no more match possibilities");
                break;
              }
            }
            if (position > -1) { //stateCodes are only constructed for position and i >= 0
              // position is < length and matchCount is either 0 or 1
              stateCode = new Long(((long) position << 32) |
                                   ((long) i << 16) |
                                   matchCount);
              if (doneStates.get(stateCode) == null)
                doneStates.put(stateCode, stateCode);
              if (authorFlag && debugFlag)
                debug("   testing pattern "+state.rule.pattern[i]+" at "+(i+1)+
                      " against "+
                      wordString.charAt(position)+" at "+(position+1)+
                      " in "+wordString);
            }
            else
              if (authorFlag && debugFlag)
                debug("   testing pattern "+state.rule.pattern[i]+" at "+(i+1)+
                      " against null at left end in "+wordString);

            //try to match pattern element:

            if (position < 0) {
                if ((state.rule.pattern[i].startsWith("?")) ||
                    (state.rule.pattern[i].startsWith("*"))) {
                  i--;
                }
                else {
                  matched = false;
                 // break;
                }
            }

            //"&" match duplicate of previous letter
            else if (state.rule.pattern[i].equals("&")) {
                if ((position > 0) &&
                    (wordString.charAt(position) == wordString.charAt(position-1))) {
                  started = 1;
                  i--;
                }
                else if (!((position > 1) && (started < 1))) {
                  matched = false;
                 // break;
                }
                position--;
            }

            //"." pattern can match anywhere
            else if (state.rule.pattern[i].startsWith(".")) { 
                if (matchCount == 0 &&
                    state.rule.pattern[i].indexOf(wordString.charAt(position), 1) >= 0) {
                  //it matches here for first time, so go to next pattern element
                  started = 1; //do this first here so it'll be saved in the altState
                  matchCount=0; //matchCount for next pattern element
                  i--;
                  position--;
                }
                else if (i == 0 && position == 0 && matchCount == 0) {
                  matched = false;
                //  break;
                }
                else if (matchCount > 0) {
                  // have already matched, now skip any number of anything
                  //started = 1; //should already be 1
                  //adjust killnums to compensate for additional skipped positions
                  if (i >= patternLength-state.rule.killnum) killRight--;
                  else if (i < state.rule.leftkillnum) killLeft--;
                  matchCount=0; //matchCount for next pattern element
                  i--;
                  position--;
                }
                else { //still looking for a match, move left one position
                  //adjust killnums to compensate for additional skipped positions
                  if (i >= patternLength-state.rule.killnum) killRight--;
                  else if (i < state.rule.leftkillnum) killLeft--;
                  position--;
                }
            }

            //match pattern characters against input character
            else if (state.rule.pattern[i].indexOf(wordString.charAt(position)) < 0) {
                // doesn't match here
                if ((started < 1) && (position > 0)) //unanchored match
                  position--; //move position and keep looking
                else {
                  if (authorFlag && debugFlag)
                    debug("    match failed");
                  matched = false;
                 // break;
                }
            }

            //otherwise, character matches pattern -- decide what to do next

            else if ((i == 0) && state.rule.leftAnchor &&
                     (position > 0) && (started > 0)) {
                matched = false;
                if (authorFlag && debugFlag)
                  debug("    left anchored match failed");
               // break;
            }
            else if ((i == 0) && matched &&
                     ((state.rule.leftAnchor == false) || (position <= 0))) {
                break; //match is true
            }

            else { //character matches pattern
                   //treat rightmost unanchored match as +
                  started = 1;
                  i--;
                  position--;
            }
        }
        if (matched && state.rule.leftAnchor && (position > 0)) {
          matched = false;
          if (authorFlag && debugFlag)
            debug("    left anchored match failed");
        }
        // All done with the comparing. If we've got a match then
        // try the actions in the right-hand side to see if conditions
        // are met and, if so, build an entry.
        if (matched) {
                if (killLeft > 0) //made available for use in actions
                  state.prefixString = wordString.substring(0, killLeft);
                if (killRight > 0) //made available for use in actions
                  state.suffixString = wordString.substring(length-killRight);
        //tbd check thisRule versus state.rule here, and state.rule vs thisRule previously
                int[] ruleActions = thisRule.actions; //capture this before the rule changes
                for (int j = 0; j < ruleActions.length; j++) {
                  doAction(ruleActions[j], state); // catSenses is where results go
                }
            }
        if (state.catSenses.size() > 0) {
//          recordResults(word, state.catSenses);
          if (authorFlag && traceFlag) {
            trace("   finished rule: " + thisRule.name + " at depth " + depth +
                  "\n        " + "     with matched = "+matched+" and "+ 
                  state.catSenses.size() + " forms generated");
          }
          return state.catSenses;
        }
        else return null;
    }


// method for processing catSense entries in recordResults:

    @Override
  protected void processCatSense(Word word, Category thisCat,
                                              Word thisRoot,
                                              Value[] features,
                                              int penalty) {
    Category useCategory = thisCat;
    // thisCat is the default category to use if features doesn't specialize it

    if (features != null && features.length > 0) {
      Value property = null;
      Value value = null;
      Value agrValue = null;
      Word[] valueArray = null;
      if (authorFlag && traceFlag) {
        System.out.println("    processCatSense for " +
                           (isKnownWord(word.getWordString()) ? "known"
                                       : "unknown") +
                           " word = " + word.getWordString() +
                           ", cat = " + thisCat +
                           ",\n    root = " +
                           (isKnownWord(thisRoot.getWordString()) ? "known"
                                       : "unknown") +
                           " word " + thisRoot.getWordString() +
                           ", penalty = " + penalty);
        System.out.print("    features:  ");
        for (int i=0; i<features.length; i++) {
          System.out.print(features[i].toString() + ", ");
        }
        System.out.print("\n");
      }
      for (int i = 0 ; i < features.length ; i++) {
        // note: the stepping of i across the property value happens at the end.
        property = features[i];
        if (i+1 < features.length)
          value = features[i+1];
        else value = atom_true;

    // Now check for inflectional (nonroot) categories:

        if (property == atom_number) {
          if (value == atom_pl)
            useCategory = cat_npl;
          else if (value == atom_sgSlashPl)
            useCategory = cat_nsp;
          //the above are the only two values of number property that rules produce
          //tbd: (what about those that may be copied from existing entries --
          // e.g., by COPY-FEATURES, FORMS-WITH-PREFIX, or MAKE-FORMS-FOR-COMPOUND
        }
        else if (property == atom_tns || property == dict.tenseAtom) {
            // System.out.println("processCatSense found tense");
            agrValue = (Value)getpropFromPlistArray(dict.agrAtom, features);
            if (agrValue == null)
                agrValue = (Value)getpropFromPlistArray(dict.pncodeAtom, features);
          if (value == dict.pastAtom) {
              // System.out.println("processCatSense found past");
            if ((getpropFromPlistArray(dict.pastpartAtom, features) != null) ||
                (getpropFromPlistArray(dict.pastDashPartAtom, features) != null)){
              word.addWordCategory(dict.pastCategory, penalty); //add this one now
              //word.addWordCategory(dict.makeCategory("past"), penalty); //add this one now
              useCategory = dict.pastpartCategory; //this will be added later
              //useCategory = dict.makeCategory("pastpart"); //this will be added later
            }
            if ( agrValue == dict.threeSAtom ||
                 agrValue == dict.threeSgAtom ||
                 agrValue == dict.threeSingAtom ){
              useCategory = dict.vpast3sgCategory;
              // useCategory = dict.makeCategory("past3sg");
            }
            else if ( agrValue == dict.twoSAtom ||
                      agrValue == dict.twoSgAtom ||
                      agrValue == dict.twoSingAtom ) {
              useCategory = dict.vpast2sgCategory;
              // useCategory = dict.makeCategory("past2sg");
            }
            else useCategory = dict.pastCategory;
            // else useCategory = dict.makeCategory("past");
          }
          else if (value == dict.presentAtom) {
              // System.out.println("processCatSense found present");
            if ( agrValue == dict.threeSAtom ||
                 agrValue ==  dict.threeSgAtom ||
                 agrValue == dict.threeSingAtom  ) {
              useCategory = dict.v3sgCategory;
              // useCategory = dict.makeCategory("3sg");
            }
            else if  (agrValue == dict.twoSAtom ||
                      agrValue == dict.twoSgAtom ||
                      agrValue == dict.twoSingAtom ) {
              useCategory = dict.v2sgCategory;
              // useCategory = dict.makeCategory("2sg");
            }
            else useCategory = dict.not3sgCategory;
            // else useCategory = dict.makeCategory("not3sg");
          }
        }
/*
          if (value == atom_past) {
            if (getpropFromPlistArray(atom_pastpart, features) != null) {
              word.addWordCategory(cat_past, penalty); // add this one now
              useCategory = cat_pastpart;              // this will be added later
            }
            else if (getpropFromPlistArray(atom_2sg, features) != null) {
              useCategory = cat_past2sg;
            }
            else if (getpropFromPlistArray(atom_3sg, features) != null) {
              useCategory = cat_past3sg;
            }
            else if (getpropFromPlistArray(atom_not13sg, features) != null) {
              useCategory = cat_pastnot13sg;
            }
            else useCategory = cat_past;
          }
          else if (value == atom_present) {
            if (getpropFromPlistArray(atom_3sg, features) != null) {
              useCategory = cat_3sg;
            }
            else if (getpropFromPlistArray(atom_2sg, features) != null) {
              useCategory = cat_2sg;
            }
            else if (getpropFromPlistArray(atom_1sg, features) != null) {
              useCategory = cat_1sg;
            }
            else if (getpropFromPlistArray(atom_not13sg, features) != null) {
              useCategory = cat_not13sg;
            }
            else useCategory = cat_not3sg;
          }
        }
*/
        else if (property == atom_pastpart)
          useCategory = cat_pastpart;
        else if (property == atom_prespart)
          useCategory = cat_prespart;

    // Now check for penalties (these are already checked for in recordResults):

// The following conditions should only
// apply to specific cats if we have a penalties property with multiple cats
// which shouldn't happen in the morph rules, and should already have been
// eliminated in the lexicon.  However, such lists of penalty number followed
// by categories is produced by one of the prefix or compound constructors
// so that's what this code was for.  I may need to change the format of the
// rules produced by make-java-rules to agree with the form produced by that
// constructor.  In MorphEngFns, formsWithPrefix is such a constructor.

        else if (property == word_0) { //penalty zero inflectional categories
          Category iCat = (Category)value;
          if (thisCat.subsumesCategory(iCat))
            useCategory = iCat;
        }
        else if (property == word_1) { //penalty zero inflectional categories
          Category iCat = (Category)value;
          if (thisCat.subsumesCategory(iCat))
            useCategory = iCat;
        }
        else if (property == word_2) { //penalty zero inflectional categories
          Category iCat = (Category)value;
          if (thisCat.subsumesCategory(iCat))
            useCategory = iCat;
        }
        else if (property == word_3) { //penalty zero inflectional categories
          Category iCat = (Category)value;
          if (thisCat.subsumesCategory(iCat))
            useCategory = iCat;
        }

    // Now check for special properties:

        else if (value != null && (property == atom_prefix || property == atom_hasPrefix)) {
          if (value.wordp()){
            valueArray = new Word[] {(Word)value};
            word.addPrefixes(valueArray); // pm6apr04
          }
          else if (value.listp()) {
            Value[] contents = ((List)value).contents;
            int length = contents.length;
            Word[] target = new Word[length];
            System.arraycopy(target, 0, contents, 0, length);
            word.addPrefixes(target);
            }
          else if (authorFlag && traceFlag)
            trace( "*** warning: unrecognized value for prefix: " + value);
        }
        else if (value != null && (property == atom_suffix || property == atom_hasSuffix)) {
          if (value.wordp()){
              valueArray = new Word[] {(Word)value};
              word.addSuffixes(valueArray); //pm 6apr04
          }
          else if (value.listp()) {
            Value[] contents = ((List)value).contents;
            int length = contents.length;
            Word[] target = new Word[length];
            System.arraycopy(target, 0, contents, 0, length);
            word.addSuffixes(target);
          }
          else if (authorFlag && traceFlag)
            trace( "*** warning: unrecognized value for suffix: " + value);
        }
        else if (property == atom_penalty || property == atom_penalties) {
          //do nothing - already taken into account
        }
        else if (property instanceof Atom) {
          word.markDict((Atom)property, value, true);
        }
        else {
          // Can't do word.markDict((Lexicon.Atom)property, value, true);
          if (authorFlag)
            System.out.println("Warning: property name " + property +
                               " is not an Atom");
        }
        i++;
      }
    }
    word.addWordCategory(useCategory, penalty);
      // The following now is automatic in addWordCategory as of 6-29-01
      //   if (thisCat.isRootCategory())
      //     rootCats.addElement(thisCat);
    return;
  }

// methods used in rules:

  /**
   * Applies the morph-precheck tests to the indicated word.
   * Called in act2 in Morph_en
   */

  protected void doMorphPrecheck(Word lex, MorphState state) {
    // call addCatSense to produce catSenses as a side effect
    // tbd: implement this if want to do prechecks from rules, else
    // go with current arrangment where morph(word) calls morphPrecheck
    return;
  }

  /**
   * Attempts to analyze the indicated word as a compound.
   */

  protected void doMorphCompoundTest(Word lex, MorphState state) {
    // call morphCompoundTest
    if (authorFlag && traceFlag)
      trace(" Analyzing " + lex.getWordString() +
            " with doMorphCompoundTest at depth " + state.depth);
    morphCompoundTest(lex, state); //creates catsenses as a side effect
    return;
  }

  /**
   * Gets the first word of a hypothesized compound decomposition during the
   * operation of a MorphCompoundRule.
   */

  protected Word word1(MorphState state) {
    return state.word1;
    }

  /**
   * Gets the second word of a hypothesized compound decomposition during the
   * operation of a MorphCompoundRule.
   */

  protected Word word2(MorphState state) {
    return state.word2;
    }

  /**
   * Gets the first word of a known decomposition of a compound word.
   */


  public Word word1(Word compoundWord) {
    //gets the first word of a compound word
    // changed 01apr04 pmartin
    Word[] firstDeco = firstDecomposition(compoundWord);
    if (firstDeco == null || firstDeco.length == 0)
        return null;
    return firstDeco[0];
  }


  /**
   * Gets the second word of a known decomposition of a compound word.
   */

  public Word word2(Word compoundWord) {
    //gets the second word of a compound word
      // changed 01apr04 pmartin
    Word[] firstDeco = firstDecomposition(compoundWord);
    if (firstDeco == null || firstDeco.length == 0)
        return null;
    return firstDeco[1];
  }

  public Word[] firstDecomposition(Word compoundWord){
    // pm 01apr04  25jun04
    Value[] compounds = compoundWord.getCompoundOf();
    if (compounds == null || compounds.length < 1 || compounds[0] == null ||
        !(compounds[0] instanceof List)) { // waw 12-22-04
      if (authorFlag && traceFlag) {
        if (compounds == null) {
          // This is OK when attempting to set a prefix in a comound rule
          // whose test condition has already failed, but the assignment
          // statement is not conditionalized on testVal
          System.out.println("*** null compounds in firstDecomposition of " +
                             compoundWord); // waw 01-19-05
        }
        else if (compounds.length < 1)
          System.out.println("*** empty compounds in firstDecomposition of " +
                             compoundWord); // waw 01-19-05
        else
          System.out.println("*** nonlist compounds[0] in firstDecomposition of " +
                             compoundWord + ": " + compounds[0]); // 01-19-05
      }
      return null;
    }
    Value[] vwords = ((List)compounds[0]).contents;
    if (vwords == null) return null;
    if (vwords instanceof Word[])
        return (Word[])vwords;
    else {
        if (authorFlag)
           System.out.println("wrong type for " + vwords +
                              " in firstDecomp of\n " +
                              compoundWord.printEntryString());
        Word[] fw = fixNonWords(vwords);
        if (fw != null) return fw;
        else {
              if (authorFlag)
                System.out.println("can't fix value array");
            return null;
        }
    }
 }

    // hacked up in desperation 25june04 pmartin
    public Word[] fixNonWords(Value[] nonWords){
        Word[] fixedWords = new Word[nonWords.length];
        for (int ii=0; ii<nonWords.length; ii++){
            if (nonWords[ii] instanceof Word)
                fixedWords[ii] = (Word)nonWords[ii];
            else if (nonWords[ii] instanceof Atom){
                String ws = ((Atom)nonWords[ii]).getWordString();
                Word found = realOrCachedWord(ws);
                if (found == null) {
                    if (authorFlag)
                      System.out.println("no real or cached word " + ws);
                    return null;
                }else fixedWords[ii] = found;
            }else return null;
        }
        return fixedWords;
    }

    // hacked up 25jun04 pmartin
   public Word realOrCachedWord(String ws){
       Word  lw = dict.getWord(ws);
       // can't reach state here unless I learn more
       //    if (lw == null)
       //      lw = (Lexicon.Word)state.morphCache.get(ws);
       return lw;
   }
  /**
   * Tests whether the indicated word could be analyzed as the indicated
   * category cat, without contradicting any constraints.
   */

  public boolean mayBeCat(MorphState state, Word word,
                          Category cat) {
    // Issue: we need a state to set likelihood
    // Other tests like this don't require access to state.
    // Need to add may-be-cat to list with morph-prefix to get an additional
    // first argument that is a state when the Lisp-to-Java rule translator
    // operates, in order for this method to be called.
    if (authorFlag && traceFlag) {
      System.out.println("** mayBeCat (" + word + ", " + cat + ")");
      System.out.println("** isRootOfCat (" + word + ", " + cat + ") = " +
                         word.isRootOfCat(cat) + ".");
      System.out.println("** hasKnownRoot (" + word + ") = " +
                         hasKnownRoot(word) + ".");
    }
    state.likelihood = -1;
    if (possibleRoot(word) == false) return false;
    if (word.isRootOfCat(cat)) return true;
    if (word.getdict(dict.compoundOfAtom) != null) {
      //if it is a compound, only allow categories that it actually has
      return false;
    }
    if (syllabic(word)) {
      int numTestVal = 10;
      if (cat == cat_n) numTestVal = 80;
      else if (cat == cat_v) numTestVal = 50;
      else if (cat == cat_adj) numTestVal = 30;
      state.likelihood = numTestVal;
      return true; //else return true for anything syllabic
    }
    return false; //else return false
  }

  /**
   * Tests for illegal root strings.
   */

  public boolean illegalBeginning(String wordString){ // pm 22apr04
      int max = wordString.length();
      if (illegalBeginningsMax < max) max = illegalBeginningsMax;
      for (int i=1; i<=max; i++) {
        if (illegalBeginnings.contains(wordString.substring(0,i)))
          return true;
      }
      return false;
  }

  public boolean illegalEnding(String wordString){ // pm 22apr04
      int wordSize = wordString.length();
      int max = illegalEndingsMax;
      if (max > wordSize) max = wordSize;
      for (int i=1; i<=max; i++) {
        String sub = wordString.substring(wordSize-i);
        if (illegalEndings.contains(sub)) 
          return true;
      }
      return false;
  }

  public boolean illegalRoot(String wordString){ // pm 23apr04
      return illegalRoots.contains(wordString);
  }

  /**
   * Tests if this word is a plausible root word for a morphological decomposition.
   */

  public boolean plausibleRoot(Word word) {
    String wordString = word.getWordString();
    int hyphenPos = wordString.indexOf("-");
    if (hyphenPos < 0) {
      return (possibleRootStringTest(wordString) &&
              possibleRootWordTest(word));
    }
    else {
      while (hyphenPos > -1) {
        wordString = wordString.substring(hyphenPos+1);
        hyphenPos = wordString.indexOf("-");
      }
    return (possibleRootStringTest(wordString) &&
            possibleRootWordTest(dict.getWord(wordString)));
    }
  }

  /**
   * Tests if this word is a possible root word for a morphological decomposition.
   */

  public boolean possibleRoot(Word word) {
  return (possibleRootStringTest(word.getWordString()) &&
          possibleRootWordTest(word));
  }

  /**
   * Tests string part of a possible root test.
   */

  public boolean possibleRootStringTest(String wordString) {
      return (wordString.length() > 2 &&
              syllabic(wordString) &&
              !illegalEnding(wordString) &&
              !illegalBeginning(wordString) &&
              !illegalRoot(wordString));
  }

  /**
   * Tests word part of a possible root test.
   */

  public boolean possibleRootWordTest(Word word) {
    if (word == null) return true; // unknown words are ok (i.e., not pronouns)
    else
      return (!isFormOfCat(word, dict.proCategory) || // don't do here+ity, hisity, etc.
              legalRoots.contains(word.getWordString()));
  }

  /**
   * Tests if this word is a plausible word for compounding or just guessing.
   */

  public boolean plausibleWord(Word word) {
      return plausibleWord(word.getWordString());
  }

    /**
   * Tests if this word string is a plausible word for compounding or just guessing.
   */

  public boolean plausibleWord(String wordString) {
      return ((wordString.length() > 2) &&
              syllabic(wordString) &&
              !illegalEnding(wordString) &&
              !illegalBeginning(wordString));
  }

//  String vowels = "aeiouyAEIOUY????????????????????????????????????????????????????";
//  String accents = "????????????????????????????????????????????????????";
//  String otherWordChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_'";
  String vowels = "aeiouy??????????????????????????";
  String accents = "??????????????????????????";
  String otherWordChars = "abcdefghijklmnopqrstuvwxyz-_'";
  String consonantChars = "bcdfghjklmnpqrstvwxz";
  String aeiyVowels = "aeiy??????????????";
  String oVowels = "o??????";
  String eiyVowels = "eiy?????????";
  String aVowels = "a?????";

  /**
   * Tests whether the indicated wordstring seems to have syllabic structure
   * -- mostly, does it contain a vowel, but also tests other constraints.
   * -- currently it does a simple test that it contains at least one vowel
   * ends appropriately, and consists of only letters or hyphens or
   * underscores or apostrophes.
   */

  public boolean syllabic(String str) {

    String wordString = str.toLowerCase();
    int length = wordString.length();
    int firstVowelPos = -1;
    for (int i = 0 ; i < length ; i++) {
      if ((firstVowelPos < 0) &&
          vowels.indexOf(wordString.charAt(i)) >= 0)
        firstVowelPos = i;
    }
    if (firstVowelPos < 0) return false; //must have a vowel
    for (int i = 0 ; i < length ; i++) {
      if ((otherWordChars.indexOf(wordString.charAt(i)) < 0) &&
          (accents.indexOf(wordString.charAt(i)) < 0))
        return false;
        break;
    }
    if ((length > 2) && (firstVowelPos == length-1) &&
        (wordString.endsWith("e")) && // only vowel is a final e
        !(wordString.equals("pre"))) //and it's not "pre"
       return false;
    if ((length > 2) && (firstVowelPos == length-1) &&
        (wordString.endsWith("y")) && // only vowel is a final y
        !shortYWordStrings.contains(wordString) && // not in our good list
        //and it's an illegal beginning plus y
        illegalBeginnings.contains(wordString.substring(0,length-1)))
      return false;
    return true; //else return true
  }

  /**
   * syllabic (word) tests whether the indicated word seems to have
   * syllabic structure and caches the results in its lexical entry.
   * Test whether a word looks like a pronouncable
   * word -- currently a simple test that it contains at least one vowel
   *  ends appropriately, and consists of only letters or hyphens or
   * underscores or apostrophe's.
   */

    public boolean syllabic(Word word) { //pm 22apr04 poly test

    Value storedVal = word.getdict(atom_syllabic);
    if (storedVal == atom_true)
      return true;
    else if (storedVal == atom_false)
      return false;
    storedVal = word.getdict(atom_polysyllabic);
    if (storedVal == atom_true)
      return true;
    else if (storedVal == atom_false)
      return false;
    boolean syl = syllabic(word.getWordString());
    if (syl) 
      word.putdict(atom_syllabic, atom_true);
    else 
      word.putdict(atom_syllabic, atom_false);
    return syl;
  }

  /**
   * Tests whether the indicated wordstring seems to have polysyllabic structure
   * -- mostly, does it contain a vowel-consonant-vowel alternation.
   */

  public boolean polysyllabic(String str) {
    /* polysyllabic (wordString) tests whether a word string looks like a polysyllabic
    word. */
    String wordString = str.toLowerCase();
    if (!syllabic(wordString)) return false;
    //tbd add the conditions
    int length = wordString.length();
    char firstVowel = 'b'; //initialized to not a vowel to make compiler happy
    boolean firstVowelFound = false;
    char thenConsonant = 'a'; //initialized to not a consonant to make compiler happy
    boolean thenConsonantFound = false;
    char secondVowel = 'b'; //initialized to not a vowel to make compiler happy
    int secondVowelPos = -1;
    char thisChar;
    for (int i = 0 ; i < length ; i++) {
      thisChar = wordString.charAt(i);
      if (vowels.indexOf(thisChar) >= 0) { //this char is a vowel
        if (secondVowelPos >= 0)
          return true;
        else if (thenConsonantFound) {
          secondVowel = thisChar;
          secondVowelPos = i;
        }
        else if (firstVowelFound &&
                 (aeiyVowels.indexOf(firstVowel) >= 0) &&
                 (oVowels.indexOf(thisChar) >= 0)) {
          secondVowel = thisChar; // = two syllables e.g., zion, scion, eon, aon
          secondVowelPos = i;
        }
        else if (firstVowelFound &&
                 (eiyVowels.indexOf(firstVowel) >= 0) &&
                 (aVowels.indexOf(thisChar) >= 0)) {
          secondVowel = thisChar; // = two syllables e.g., vial, seal
          secondVowelPos = i;
        }
        else if (firstVowelFound &&
                 ("i".indexOf(thisChar) >= 0) &&
                 (wordString.startsWith("sm", i+1))) {
          secondVowel = thisChar; // = two syllables e.g., deism, seism, *feist, *heist
          secondVowelPos = i;
        }
        else {
          firstVowel = thisChar;
          firstVowelFound = true;
        }
      }
      else { //this char is a consonant
        if (secondVowelPos >= 0)
          return true;
        else if (firstVowelFound) {
          thenConsonant = thisChar;
          thenConsonantFound = true;
        }
      }
    }
    if (secondVowelPos < 0) return false;
    if (!thenConsonantFound) return false;
    if (("e".indexOf(secondVowel) >= 0) &&        //final e doesn't count
        (secondVowelPos == length-1) &&           //unless preceded by l or r
        !(("lr".indexOf(thenConsonant) >= 0) &&   //preceded by a consonant
          (length > 3) &&                         //e.g., zoogle, centre, (not poole)
          (consonantChars.indexOf(wordString.charAt(length-3)) >= 0)))
       return false; // note: final ? would count as a second syllable
    else return true;
  }

  /**
   * Tests whether the indicated word seems to have polysyllabic structure
   * and caches the results in its lexical entry.
   */

  public boolean polysyllabic(Word word) {
    /* polysyllabic (word) tests whether a word looks like a polysyllabic
    word. */
    Value storedVal = word.getdict(atom_polysyllabic);
    if (storedVal == atom_true)
      return true;
    else if (storedVal == atom_false)
      return false;
    else if (polysyllabic(word.getWordString())) {
      word.putdict(atom_polysyllabic, atom_true);
      return true;
    }
    else {
      word.putdict(atom_polysyllabic, atom_false);
      return false;
    }
  }

  /**
   * For identifying class when debugging or tracing rules.
   */

  protected static String className = "MorphEngFrame";

} // end of MorphEngFrame class
