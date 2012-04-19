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
import java.util.Hashtable;
import java.util.StringTokenizer;
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

// Search for tbd for outstanding issues.

/**
 * This is a class of objects used to carry the rules for a morphological analysis.
 * This class is extended by language-specific subclasses to contain the rules and
 * actions for a specific language.
 *
 * To analyze words, a language-specific MorphEngine needs to be instantiated and
 * initialized with a specified Lexicon, after which it can be asked to analyze
 * wordstrings into words by calling morph(wordstring) or morph(wordstring, dict).
 * If a wordstring already has a word in the lexicon, morph returns that word.
 * Otherwise, a new word is added to the lexicon with an interpretation determined
 * by the morphological analysis.  The class MorphTest.java can be used to test the
 * operation of the morphological analyzer, by instantiating a MorphEngine and
 * initializing it, and then interpreting command-line commands for loading a
 * lexicon and providing words to be analyzed, with or without tracing.
 *
 * MorphEngine was designed to support multithreading by allowing each thread
 * to instantiate a MorphState and a morphCache to manage the morphological
 * analyses on that thread.  During morphological analysis, additional instances
 * of MorphState may be created for managing the morphological analyses of
 * potential roots of a word being analyzed.
 *
 * @author W. A. Woods
 * 
 * @version        1.0
 * 
 * The rules and code for this system were developed by W. A. Woods
 *
 * This version of the MorphEngine is a work in progress with many design issues
 * still unresolved.  Many such issues are marked with tbd comments.
 * 
 */

public abstract class MorphEngine {

// settable parameters governing behavior

  /* limits depth of recursion in morphological analyses */
  public int maxDepth = 500; //2000; // 25; // 15;

  /* determines whether precheck rules are applied */
  public boolean testPrecheckFlag = false; // true;


// variable for lexicon to be used

  protected Lexicon dict;



/* //These need to be defined in a language-specific extension, such as Morph_en.

  protected static Hashtable varTable = new Hashtable();

  public static Hashtable getVarTable() {
    return varTable;
  }

  protected static void defVar(String varName, String valString) {
    getVarTable().put(varName, valString);
  }

*/

  // These methods must be defined in a language-specific extension:

  /* returns the rule set to start with */
  public abstract MorphRule[] getFirstRuleSet();

  /* returns the prefix rule set */
  public abstract MorphRule[] getPrefixRuleSet();

  /* returns named prefix rule set */
  public abstract MorphRule[] getPrefixRuleSet(Atom ruleSet);

  /* returns the Lexicon used for words, atoms, and categories */
  public abstract Lexicon getMorphDict();

  /* tests for special forms like phone numbers, dates, pathnames, and urls */
  public abstract Word morphPrecheck(Word word);

  /* executes the specified action in the specified state */
  protected abstract void doNumberedAction(int number, MorphState state);

  /* adds a new syntactic category sense to a lexical entry */
  protected abstract void processCatSense(Word word, Category thisCat,
                                                       Word thisRoot,
                                                       Value[] features,
                                                       int penalty);

  /* defines values of all the constants that depend on the Lexicon */
  public abstract void initialize(Lexicon dictionary);


  // Atoms bound here, but initialized in the method initialize

  protected Atom atom_penalties;
  protected Atom atom_penalty;
  protected Atom atom_root;
  protected Atom atom_form;
  protected Atom atom_derivation;

    /* The dict.isKnownWord(string) method only tests whether a 
       word exists that has the given string as its string.  
       The word.isKnownWord() method should determine from
       some property of the word whether it was added automatically
       or came (perhaps by morphing) from a known dictionary entry.
       This method allows MorphEngine to determine what criterion
       to apply.
     */

  public boolean isKnownWord(Word thisWord) {
    // Lexicon's def of dict.isKnownWord(string) just checks that
    // there is a getWord.  This allows for the fact that another
    // thread may have produced a value for this word while this
    // thread was working on it.  All we really want this method
    // to tell us is that the word is a real word, not necessarily
    // that it has been completely analyzed.
/*
      // modified code from Lexicon.Word's isKnownWord() method
      Lexicon.WordEntry we = thisWord.getWordEntry();
      if (we == null) return false;
      else return ( // ! (we.isFormOfCat(unknownCategory)) &&
                    ! thisWord.guessedWordp());
*/
//    return (thisWord == dict.getWord(thisWord.getWordString()));
//    return thisWord.isKnownWord(); // This tests too much
    return isKnownWord(thisWord.getWordString());
  }

  public boolean isKnownWord(String wordString) {
    // Lexicon's def of dict.isKnownWord(string) just checks that
    // there is a getWord.  This allows for the fact that another
    // thread may have produced a value for this word while this
    // thread was working on it.
    return dict.isKnownWord(wordString);
  }

  public boolean hasKnownRoot(Word thisWord) {
    return hasKnownRoot(thisWord, new Hashtable());
  }

  public boolean hasKnownRoot(Word thisWord, Hashtable passed) {
    if (isKnownWord(thisWord.getWordString())) return true;
    Word[] roots = thisWord.getRoots();
    if (roots == null || roots.length < 1) return false;
    if (passed.get(thisWord.getWordString()) != null) return false;
    passed.put(thisWord.getWordString(), thisWord);
    for (int i = 0 ; i < roots.length ; i++) {
      if (hasKnownRoot(roots[i], passed)) return true;
    }
    return false;
  }

  public boolean isFormOfCat(Word thisWord, Category cat) {
    if (authorFlag && traceFlag) {
      System.out.println("** isFormOfCat (" + thisWord + ", " + cat + ") = " +
                         thisWord.isFormOfCat(cat) + ".");
      System.out.println("** hasKnownRoot (" + thisWord + ") = " +
                         hasKnownRoot(thisWord) + ".");
    }
    return thisWord.isFormOfCat(cat) && hasKnownRoot(thisWord);
  }

  public boolean isNonnameFormOfCat(Word thisWord, Category cat) {
    if (authorFlag && traceFlag) {
      System.out.println("** isNonnameFormOfCat (" + thisWord + ", " + cat + ") = " +
                         thisWord.isNonnameFormOfCat(cat) + ".");
      System.out.println("** hasKnownRoot (" + thisWord + ") = " +
                         hasKnownRoot(thisWord) + ".");
    }
    return thisWord.isNonnameFormOfCat(cat) && hasKnownRoot(thisWord);
  }

  public boolean isNonpenaltyFormOfCat(Word thisWord,
                                       Category cat) {
    if (authorFlag && traceFlag) {
      System.out.println("** isNonpenaltyFormOfCat (" + thisWord + ", " + cat + ") = " +
                         thisWord.isNonpenaltyFormOfCat(cat) + ".");
      System.out.println("** hasKnownRoot (" + thisWord + ") = " +
                         hasKnownRoot(thisWord) + ".");
    }
    return thisWord.isNonpenaltyFormOfCat(cat) && hasKnownRoot(thisWord);
  }

  public boolean isPenaltyFormOfCat(Word thisWord,
                                    Category cat) {
    if (authorFlag && traceFlag) {
      System.out.println("** isPenaltyFormOfCat (" + thisWord + ", " + cat + ") = " +
                         thisWord.isPenaltyFormOfCat(cat) + ".");
      System.out.println("** hasKnownRoot (" + thisWord + ") = " +
                         hasKnownRoot(thisWord) + ".");
    }
    return thisWord.isPenaltyFormOfCat(cat) && hasKnownRoot(thisWord);
  }

  public boolean isPenaltyFormOfCat(Word thisWord,
                                    Category cat,
                                    int penalty) {
    if (authorFlag && traceFlag) {
      System.out.println("** isPenaltyFormOfCat (" + thisWord + ", " + cat + ") = " +
                         thisWord.isPenaltyFormOfCat(cat) + ".");
      System.out.println("** hasKnownRoot (" + thisWord + ") = " +
                         hasKnownRoot(thisWord) + ".");
    }
    return thisWord.isPenaltyFormOfCat(cat, penalty) && hasKnownRoot(thisWord);
  }

  public boolean isRootOfCat(Word thisWord, Category cat) {
    if (authorFlag && traceFlag) {
      System.out.println("** isRootOfCat (" + thisWord + ", " + cat + ") = " +
                         thisWord.isRootOfCat(cat) + ".");
      System.out.println("** hasKnownRoot (" + thisWord + ") = " +
                         hasKnownRoot(thisWord) + ".");
    }
    return thisWord.isRootOfCat(cat) && hasKnownRoot(thisWord);
  }

  public boolean isKnownFormOfCat(Word thisWord, Category cat){
    if (authorFlag && traceFlag) {
      System.out.println("** isFormOfCat (" + thisWord + ", " + cat + ") = " +
                         thisWord.isFormOfCat(cat) + ".");
      System.out.println("** isKnownWord (" + thisWord + ") = " +
                         isKnownWord(thisWord) + ".");
    }
    return thisWord.isFormOfCat(cat) && isKnownWord(thisWord);
  }

  public boolean isKnownRootOfCat(Word thisWord, Category cat){
    if (authorFlag && traceFlag) {
      System.out.println("** isRootOfCat (" + thisWord + ", " + cat + ") = " +
                         thisWord.isRootOfCat(cat) + ".");
      System.out.println("** isKnownWord (" + thisWord + ") = " +
                         isKnownWord(thisWord) + ".");
    }
    return thisWord.isRootOfCat(cat) && isKnownWord(thisWord);
  }

  public boolean isKnownKindOf(Word thisWord, Word parent){
    if (authorFlag && traceFlag) {
      System.out.println("** " + thisWord + ".isKindOf (" + parent + ") = " +
                         thisWord.isKindOf(parent) + ".");
      System.out.println("** isKnownWord (" + thisWord + ") = " +
                         isKnownWord(thisWord) + ".");
    }
    return isKnownWord(thisWord) && thisWord.isKindOf(parent);
  }

  public boolean isKnownInstanceOf(Word thisWord, Word parent){
    if (authorFlag && traceFlag) {
      System.out.println("** " + thisWord + ".isInstanceOf (" + parent + ") = " +
                         thisWord.isInstanceOf(parent) + ".");
      System.out.println("** isKnownWord (" + thisWord + ") = " +
                         isKnownWord(thisWord) + ".");
    }
    return isKnownWord(thisWord) && thisWord.isInstanceOf(parent);
  }

  public Value markDict (MorphState state, Word word,
                         Atom p, Value value, boolean addToList) {
//    return word.markDict(p, value, addToList, state.morphCache);
// thought needed to pass morphCache, but morphCache is now carried in the word
    return word.markDict(p, value, addToList);
  }

  public Value markDict (MorphState state, Word word,
                         Atom p, Value[] values, boolean addToList) {
//    return word.markDict(p, value, addToList, state.morphCache);
// thought needed to pass morphCache, but morphCache is now carried in the word
    return word.markDict(p, values, addToList);
  }

  /**
   * Get the morphological analysis of a given word string.
   */

  public Word morph(String wordString) {//pm 30mar04 //waw 19Jan05
    // waw: 03-27-04 made this do makeKnownWord; compare w/ MorphEngine.java-wo-real
    MorphState state = null;
    Word word = dict.getWord(wordString);
    // if word is a number (without commas?), it will already have an entry by now
    // constructed automatically by getWord and will not be empty.
    if (word == null) {
      state = new MorphState(0, new Hashtable());
      word = makeScratchWord(wordString, state.morphCache);
      // The word will now be in state's morphCache.
      if (testPrecheckFlag) {
        Word specialForm = morphPrecheck(word);
        if (specialForm != null) return specialForm.makeKnownWord();
      }
      return morph(word, state);
    }
    if (word.testNotYetMorphed() || word.wordIsEmpty()) {
      if (state == null) {
        // if state is not null, it already has this word in its morphCache;
        // otherwise, this is an unanalyzed word that was already in dict.
        state = new MorphState(0, new Hashtable());
      }
      // 02-20-05 made this conditional
      if (testPrecheckFlag) {
        Word specialForm = morphPrecheck(word);
        if (specialForm != null) return specialForm.makeKnownWord();
      }
      return morph(word, state);
    }
    return word;
  }

  public Word makeScratchWord(String str, Hashtable morphCache){ // waw 19Mar05
    Word wd = dict.makeScratchWord(str, morphCache);
    // The word will now be in the specified morphCache, and any related words
    // that need to be made, such as the sense root of a sense name will also
    // be made in the morphCache instead of made as real words.
    wd.markNotYetMorphed();
    if (authorFlag && traceFlag)
      trace (" made scratch word in morphCache " + wd);
    return wd;
  }

  /**
   * Get the morphological analysis of a scratch word.
   */

    public Word morph(Word word, MorphState state) {

    // Earlier morph rules called morph internally.  Newer rules call
    // makeRealWord.  However, we don't want to make real words for
    // roots of hypothetical words in the morphCache that might call
    // makeRealWord, so if makeRealWord is called from a rule, we just
    // morph it but don't subsequently make it real.  The second argument
    // below says that we are not calling this from inside a rule.

    return makeRealWord(word, state, true);
  }

  /**
   * Get the morphological analysis of a given word token.
   */

  public Word morph(WordToken wtoken) {
    // Ask wtoken to call this.morph on the lowercase string
    // lcToken of the WordToken,
    if (authorFlag && traceFlag)
      trace (" morph called for word-token " + wtoken);
    wtoken.morphWord(this);
    // then return the resulting word.
    return wtoken.getWord();
  }

/*

;; related methods from WordToken

    public WordToken(Lexicon lex, String rawString) {
        this(rawString);
        word = lex.getWord(this);
    }
    
    protected void makeWord(Lexicon lex){
        if (word == null)
            word = lex.makeWord(this);
    }

    protected void morphWord(MorphEngine me){
        Lexicon.Word w = me.morph(lcToken);
        word = w;
    }

    protected void analyzeWord(MorphState ms){
        Lexicon.Word w = ms.frame.analyze(lcToken, ms);
        word = w;
    }

    protected void analyzeWord(MorphEngine me, MorphState ms){
        Lexicon.Word w = me.analyze(lcToken, ms);
        word = w;
    }

*/

  /**
   * Get the morphological analysis of a given word string in cmode known.
   */

  public Word morphKnown(String wordString) {
    Word word = dict.getWord(wordString);
    if (word == null) return null;
    if (word.testNotYetMorphed() || word.wordIsEmpty()) {
      // 02-20-05 made this conditional
      if (testPrecheckFlag) {
        Word specialForm = morphPrecheck(word);
        if (specialForm != null) return specialForm.makeKnownWord();
      }
      return morph(word, new MorphState(0, new Hashtable()));
    }
    return word;
  }

  /**
   * Use the morphological analysis of a given word to check category membership.
   */

  public boolean morphCheck(Word word, Category cat) {
    //require cmode known for this, unless cat == null
    if (word.testNotYetMorphed() || word.wordIsEmpty()) {
      word = subMorph(word, cat, (cat != null), getFirstRuleSet(),
                            new MorphState(0, new Hashtable()));
    }
    if (word == null) return false;
    if (cat != null) {
      if (word.isFormOfCat(cat))
        return true;
      else return false;
    }
    else return true;
  }

  /**
   * Use the morphological analysis of a given word to check category membership
   * in a specified mode.
   */

  public boolean morphCheck(Word word, Category cat, Atom cmode) {
    if (word.testNotYetMorphed() || word.wordIsEmpty()) {
      word = subMorph(word, cat, (cat != null || cmode != null), getFirstRuleSet(),
                            new MorphState(0, new Hashtable()));
    }
    if (word == null) return false;
    if (cat != null) {
      if (word.isFormOfCat(cat))
        return true;
      else return false;
    }
    else if (cmode != null && !isKnownWord(word))
      return false;
    else return true;
  }

  /**
   * Form of makeRealWord as called by morphRules.
   */

  public Word makeRealWord(MorphState state, Word word) {
    // Earlier morph rules called morph internally.  Newer rules call
    // makeRealWord directly.  However, we don't want to make real words
    // for roots of hypothetical words in the morphCache that might call
    // makeRealWord, so we just morph them but don't really make them real.
    // Then the roots of words will be made real automatically if their
    // derived forms are, so if the derived form is only hypothetical
    // (i.e., a scratch word), then we don't want this to make a real word
    // (that was getting linuxization from linuxity because a candidate
    // linuxism had a rule that made linuxity a real word).
    return makeRealWord(word, state, false);
  }

  /**
   * Make a word real if necessary, unless called by a rule.
   */

  public Word makeRealWord(Word word, MorphState state,
                                   boolean notFromRule) {
    // Earlier morph rules called morph internally.  Newer rules call
    // makeRealWord directly.  However, we don't want to make real words
    // for roots of hypothetical words in the morphCache that might call
    // makeRealWord, so we just morph them but don't really make them real.
    // Then the roots of words will be made real automatically if their
    // derived forms are, so if the derived form is only hypothetical
    // (i.e., a scratch word), then we don't want this to make a real word
    // (that was getting linuxization from linuxity because a candidate
    // linuxism had a rule that made linuxity a real word).

    Word thisWord = word;
    if (authorFlag && traceFlag) {
      if (notFromRule) trace (" make real word for " + thisWord);
      else trace (" rule called make real word for " + thisWord);
    }
    if (word.testNotYetMorphed() || word.wordIsEmpty())
      thisWord = subMorph(word, null, false, getFirstRuleSet(), state);
    // Now make this word and any words it mentions real by calling
    // Lexicon.Word's makeKnownWord method, unless called from a rule.
    if (thisWord == null) { // shouldn't happen
      if (authorFlag)
        System.out.println (" warning: failed to morph real word " + word);
      return word.makeKnownWord();
    }
    if (notFromRule) {
      if (authorFlag && traceFlag)
        trace (" making real word for " + thisWord);
      return thisWord.makeKnownWord();
    }
    else return thisWord;
  }

  /**
   * Other signatures for morph.
   */

// The following two signatures are no longer called from morph rules, since they
// are now handled by makeRealWord():

  public Word morph(Word word, Category cat) {
    //require cmode known for this, unless cat == null
    if (word.testNotYetMorphed() || word.wordIsEmpty())
      return subMorph(word, cat, (cat != null), getFirstRuleSet(),
                            new MorphState(0, new Hashtable()));
    else if (cat != null) {
      if (word.isFormOfCat(cat))
        return word;
      else return null;
    }
    else return word;
  }

  public Word morph(Word word, Category cat, Atom cmode) {
    if (word.testNotYetMorphed() || word.wordIsEmpty())
      return subMorph(word, cat, (cat != null || cmode != null), getFirstRuleSet(),
                            new MorphState(0, new Hashtable()));
    else if (cat != null) {
      if (word.isFormOfCat(cat))
        return word;
      else return null;
    }
    else if (cmode != null && !isKnownWord(word))
      return null;
    else return word;
  }

  public Word morph(Word word, Category cat, Atom cmode,
                                         boolean passedListFlag, boolean keepCacheFlag) {
// tbd - do something about passedListFlag and keepCacheFlag ?
//    if ((cat == null) && (cmode == null) && !passedListFlag && !keepCacheFlag)
//      morphCache = new Hashtable(); // really want to do this in a subordinate MorphState
    return morph(word, cat, cmode);
  }

  public Word morph(Word word, Category cat, Atom cmode,
                                         boolean passedListFlag, boolean keepCacheFlag,
                                         Atom capcode) {
// tbd do something about capcode (and passedListFlag and keepCacheFlag ?)
      if (cmode != null || cat != null)
        return subMorph(word, cat, true, getFirstRuleSet(),
                              new MorphState(0, new Hashtable()));
      else return subMorph(word, cat, false, getFirstRuleSet(),
                                 new MorphState(0, new Hashtable()));
  }

  public Word morphPrefix(Word word) {
    return morphPrefix(word, null, false);
  }

  public Word morphPrefix(Word word, Category cat) {
    return morphPrefix(word, cat, false);
  }

  public Word morphPrefix(Word word, Category cat,
                                               boolean cmodeFlag) {
    return morphPrefix(new MorphState(0, new Hashtable()), word, cat, cmodeFlag);
  }

// Added signatures to morphPrefix 27jun02 pmartin (state argument for MorphState):
// This also requires the compilation of any calls to morphPrefix inside morph rules
// to use this version with the specified state variable in order to avoid looping.
// Paul's rule translator automatically adds in that state variable to any calls to
// morph-prefix.  waw 01-31-05: did the same for morphAnalyzePrefix.

  public Word morphPrefix(MorphState state, Word word){
    // fill in null category and cmodeFlag = false.
    return morphPrefix(state, word, null, false);
  }

  public Word morphPrefix(MorphState state, Word word,
                                  Category cat, boolean cmodeFlag) {
    //need to set prefixPhaseFlag true at beginning and reset to false at end
    //also need to force this to use a scratch word, and not make it known afterwards
    //this is used only in conditions in rules to see if a word has a prefix analysis.
    state.prefixPhaseFlag = true;
    Word newWord = subMorph(word, cat, cmodeFlag, getPrefixRuleSet(), state);
    state.prefixPhaseFlag = false;
    return newWord;
 }

  public Word morphAnalyzePrefix(Word word, Atom ruleSetName) {
    return morphAnalyzePrefix(new MorphState(0, new Hashtable()), word, ruleSetName);
  }

  public Word morphAnalyzePrefix(MorphState state, Word word, 
                                         Atom ruleSetName) {
    return subMorph(word, null, false, getPrefixRuleSet(ruleSetName),
                          state);
  }

  protected Word subMorph(Word word, Category cat,
                                  boolean cmodeFlag, MorphRule[] ruleSet,
                                  MorphState state) {
    // subMorph does the work of morphologically analyzing a subordinate word,
    // in a provided MorphState (which is usually a new MorphState created to
    // manage a subordinate morphological analysis) given a ruleSet and a mode
    // The specified MorphState, if not a new one, must be first initialized
    // before calling subMorph.
    if (word.testNotYetMorphed() || word.wordIsEmpty()) {
      // go ahead and morph it
    }
    else if (word.morphCache == null) { // word is not just in a morphCache
      // word is already known, waw 02-19-05
      if (cat == null) return word;
      else if (word.isFormOfCat(cat)) return word; // it's a known word
      else return null;
    }
    else {
      // word is already in morphCache -- either completed or in process
      if (cat == null ||               // in this case, we're analyzing the word in full
          (!cmodeFlag && word.isFormOfCat(cat)) || // this can be true of a scratch word
          (cmodeFlag && isFormOfCat(word, cat))    // this requires a known root
          )
        return word; // waw 01-25-05
      else return null;
    }

    if (word.testNotYetMorphed()) word.clearNotYetMorphed(); // waw 03-24-04
    else {
      if (authorFlag && traceFlag)
        trace ("calling subMorph on " + word +
               ", which is not marked notYetMorphed.");
    }

    // Get a new morphState to analyze this word.
    MorphState newState = new MorphState(state.depth+1, state.morphCache);
    Vector subCatSenses = new Vector();
    int saveDepth = state.depth;
    if (cmodeFlag)
      newState.cModeKnownFlag = true;
    else newState.cModeKnownFlag = false;
//    newState.morphCache = morphCache; //tbd, check about flag to keep morph cache
    Word newWord = analyze(word, newState, ruleSet,
                                   ":unnamed", state.depth+1, subCatSenses);
    if (newWord == null) return null;
    state.depth = saveDepth; //restore the depth that got incremented in the above call
    Word morphedWord = null;
    Word dictWord = dict.getWord(word.getWordString());
    if (dictWord != null) { //word is now in the lexicon
      //some rule has decided that this word was a keeper and moved it to dict
      if (authorFlag && dictWord != newWord) { // && traceFlag) {
        // dictWord might have existed as a stub when we started, so we would have
        // been working on it all along, so that dictWord and newWord are the same word.
        System.out.println (" word " + dictWord.getWordString() +
               " has now been found in lexicon.\n" +
               " switching to the found dict word.");
        System.out.println(" this morphed word entry was: " +
              newWord + " > " + newWord.printEntryString());
        System.out.println(" the new dict word entry is: " +
              dictWord + " > " + dictWord.printEntryString());
      }
      morphedWord = dictWord;
      if (newWord == state.root) { // this word was the root being analyzed
        if (authorFlag && traceFlag)
          trace (" marking state.knownroot for " +
                 (isKnownWord(state.rootString) ? "known" : "unknown") +
                 " word " + state.rootString + "\n as root of " +
                 (isKnownWord(state.lexString) ? "known" : "unknown") +
                 " word " + state.lexString +
                 "\n in " +
                 ruleName(state.rule, state.ruleNum) +
                 " at depth " + state.depth + ": found root in dict.");
        state.knownRoot = true;  // so finding it in dict means it's now knownRoot
      }
      // knownRoot should be set only if word is the root of this derivation
    }
    else {
// if word is knownRoot, make it real (No!)
//      morphedWord = newWord.makeKnownWord();
      morphedWord = newWord;
      if (newState.knownRoot == true && // if this word has a known root and
          newWord == state.root) {      // this word is the root being analyzed
        if (authorFlag && traceFlag)
          trace (" marking state.knownroot for " +
                 (isKnownWord(state.rootString) ? "known" : "unknown") +
                 " word " + state.rootString + "\n as root of " +
                 (isKnownWord(state.lexString) ? "known" : "unknown") +
                 " word " + state.lexString +
                 "\n in " +
                 ruleName(state.rule, state.ruleNum) +
                 " at depth " + state.depth + ": because its root has known root.");
        state.knownRoot = true;        // then mark state to record known-root
      }
    }
    if (cmodeFlag && !state.knownRoot) return null;
//    if (cmodeFlag && !newState.knownRoot) return null;
    else if (cat == null) return morphedWord;
    else if (morphedWord == null) return null;
    else if (morphedWord.isFormOfCat(cat))
      return morphedWord;
    else return null;
  }


  /**
   * Get the morphological analysis of a given word string.
   */

  public Word analyze(String wordString) {
    if (dict == null) {
      if (authorFlag)
        trace(" ** ERROR ** dictionary not defined in getMorphDict; using new empty Lexicon.");
      dict = new Lexicon();
      initialize(dict); //this sets dict in the language-specific subclass
    }
    Word word = dict.getWord(wordString);
    if (word != null && !word.testNotYetMorphed() && !word.wordIsEmpty())
      return word;

    MorphState state = new MorphState(0, new Hashtable());
    return analyze(wordString, state);
  }

  /**
   * Get the morphological analysis of a given word string with given MorphState.
   */

  public Word analyze(String wordString, MorphState state) {
    // This signature can be called publicly to analyze a
    // wordString that should be made a known word after analysis.
    // This method is called by the WordToken methods in the indexer.

    // initialize state before using it for a new word.
    // (resets morphCache if state.keepMorphCacheFlag is not set)
    state.initialize(); // waw 03-31-05
    return analyze(wordString, state, getFirstRuleSet());
  }

  /**
   * Get the morphological analysis of a given word string with specified
   * ruleSet and make a real word as a result.
   */

  protected Word analyze(String wordString, MorphState state,
                                 MorphRule[] rules) {

    Word word = dict.getWord(wordString);

    // if word is a number (without commas?), it will have an entry by now
    // constructed automatically by getWord and will not be empty.

    if (word == null) word = (Word)state.morphCache.get(wordString);
    else if (!word.testNotYetMorphed() && !word.wordIsEmpty()) return word;

    if (word == null) { // it's not in the morphCache either
      word = makeScratchWord(wordString, state.morphCache);
      // at this point word is marked NotYetMorphed.
      if (testPrecheckFlag) {
        Word specialForm = morphPrecheck(word);
        if (specialForm != null) return specialForm.makeKnownWord();
      }
    }
    // pick up and save cModeKnownFlag from the MorphState
    boolean cModeIsKnown = state.cModeKnownFlag;
    Vector catSenses = new Vector(); //working storage to collect results
    if (word.testNotYetMorphed()) word.clearNotYetMorphed(); // waw 03-24-05
    word = analyze(word, state, rules, ":unnamed", 1, catSenses); // waw changed from:
//    word = analyze(wordString, state, rules, ":unnamed", 1, catSenses);
    Word newWord = null;
    if (!cModeIsKnown && word != null && dict.getWord(wordString) == null) {
      if (authorFlag && traceFlag)
        trace ("  making known word " + word);
      // copy entry to main lexicon
      newWord = word.makeKnownWord();
      if (newWord != null) {
        // this should always succeed since makeKnownWord returns the word it's given.
        if (authorFlag && traceFlag)
          trace ("  made known word " + newWord);
        word = newWord; // this shouldn't make any difference
      }
    }
    return word;
  }


  /**
   * Method for doing the morphological analysis of a word string with specified
   * rules and depth and catSenses, possibly for a scratch word that will not be
   * made real.
   */
// tbd: waw 03-24-05 does anyone now call this signature? no -- if so, do I need to fix it?
  protected Word analyze(String wordString, MorphState state,
                                 MorphRule[] rules, String blockName,
                                 int atDepth, Vector catSenses) {

    // Get the morphological analysis of the word from dict if it's there.
    Word word = dict.getWord(wordString);
    state.depth = atDepth;
    if (state.morphCache == null) // this is where morphCache gets set initially
      state.morphCache = new Hashtable(); //tbd check about keepMorphCacheFlag
    if (word == null)
      word = (Word)state.morphCache.get(wordString);
    if (word == null) {
      word = makeScratchWord(wordString, state.morphCache);
      // word will be marked NotYetMorphed at this point.
    }
    if (word.testNotYetMorphed()) {
      word.clearNotYetMorphed(); // waw 03-24-05
      word = analyze(word, state, rules, blockName, state.depth, catSenses);
      if (word == null) {
        if (authorFlag && traceFlag)
          trace ("  analysis of " + word + " failed.");
      }
    }
    return word;
  }

  /**
   * Method for doing the morphological analysis of a scratch word.
   */

  protected Word analyze(Word word, MorphState state,
                                 MorphRule[] rules, String blockName,
                                 int atDepth, Vector resultVector) {
    state.lex = word;
    state.lexString = word.getWordString();
    if (authorFlag && atDepth != state.depth)  // debugging trace probe //tbd remove later
      trace (" *** warning: specified depth " + atDepth +
             " is different from state depth " + state.depth + " in analyze " +
             state.lexString);
    state.depth = atDepth;
    state.frame = this;
    if (state.morphCache == null) // This is where morphCache gets set initially.
      state.morphCache = new Hashtable(); //tbd check about keepMorphCacheFlag
    state.catSenses = resultVector; //communicate this to the MorphState
    state.doneFlag = false; //initialze this for start of new word

    if (state.depth > maxDepth) {
      if (authorFlag)
        System.out.println("*** WARNING *** depth limit exceeded for " +
                           word + " trying " + blockName + " at depth " + atDepth);
      return word;
    }

    tryRules(rules, blockName, state, state.depth, 0);
    if (resultVector.size() > 0) {
      recordResults(word, state, resultVector, atDepth);
      return word;
    }
    return null;
  }

// Methods used in methods for making morphological rule instances.  The methods that
// use these (two signatures of the r method) need to be defined in the language-specific
// subclass (e.g., Morph_en) (defined in Morph_template_en.java, which gets the rules
// merged into it to produce Morph_en).

  /**
   * Method for making the array of action ints from a rule's action
   * string.
   */

  public static int[] makeActions(String actionString) {
    int[] actionArray;
    if (actionString.length() > 0) {
      Vector actionsBuffer = new Vector(actionString.length());
      StringTokenizer temp = new StringTokenizer(actionString, ", \t\n\r");
      while (temp.hasMoreTokens()) {
        actionsBuffer.addElement(temp.nextToken());
      }
      actionArray = new int[actionsBuffer.size()];
      for(int i = 0; i < actionsBuffer.size(); i++) {
        actionArray[i] = Integer.parseInt((String)actionsBuffer.elementAt(i));
      }
    }
    else {
      actionArray = new int[0];
    }
    return actionArray;
  }

  //methods used in performing morphological analysis:

  /**
   * Method to initialize state variables before rule match.
   */

  protected void initializeState(MorphState state) {
    //called before each new rule is tried
    state.rule = null;
    state.ruleNum = -1;
    state.matched = true;
    state.root = null;
    state.rootString = null;
    state.addLeftString = "";
    state.addRightString = "";
    state.substitutionString = "";
    state.killLeftNum = 0;
    state.killRightNum = 0;
    state.testval = true;
    state.likelihood = -1;
//    state.phaseOneFlag = false; //this needs to persist and be managed by rules
//    state.prefixPhaseFlag = false; //this needs to persist and be managed by rules
    state.knownRoot = false;
    state.prefixString = "";
    state.suffixString = "";
    state.prefix = null;
    state.suffix = null;
//    state.doneFlag = false; don't reset this (so can have multiple do & try in rhs)
  }


  /**
   * Variable that causes root to be morphed by setRoot whenever it is set for
   * a test in a rule.  Currently, this is false because an automatic morphRoot
   * is inserted into the rule conditions by the rule compiler.
   */

  public static final boolean morphRootWhenSet = false;

  /**
   * Method called before each doCat and doTest to set up the root to be tested.
   */

  protected void setRoot(MorphState state) {
    if (state.rootString != null)
      return; //previous Root (not nulled) stays at its previous value
    state.testval = true; // new 2004-0420
    String stem;
    // = state.lexString.substring(killLeftNum, state.lexString.length()-killRightNum);
    if (state.leftContextPtr > -1 && state.rightContextPtr > -1) {
      if (state.leftContextPtr >= state.killLeftNum &&
          state.rightContextPtr >= state.leftContextPtr &&
          state.lexString.length()-state.killRightNum >= state.rightContextPtr)
        stem = state.lexString.substring(state.killLeftNum, state.leftContextPtr) +
               state.substitutionString +
               state.lexString.substring(state.rightContextPtr,
                                         state.lexString.length()-state.killRightNum);
      else if (state.killLeftNum > -1 && state.killRightNum > -1 &&
               state.killLeftNum + state.killRightNum <= state.lexString.length()) {
        stem = state.lexString.substring(state.killLeftNum,
                                         state.lexString.length()-state.killRightNum);
        if (authorFlag && traceFlag) // extra traceflag avoids concatenations when not traced
          trace(" ** ERROR ** inconsistent killnums and substitution context " +
                state.killLeftNum + " " + state.leftContextPtr + " " +
                state.rightContextPtr + " " + 
                (state.lexString.length()-state.killRightNum) +
                " (" + state.killRightNum + ") -- subst not done.");
      }
      else {
        stem = state.lexString.substring(state.killLeftNum,
                                         state.lexString.length()-state.killRightNum);
        if (authorFlag && traceFlag)
          trace(" ** ERROR ** inconsistent killnums with substitution " +
                state.killLeftNum + " " + state.leftContextPtr + " " +
                state.rightContextPtr + " " +
                (state.lexString.length()-state.killRightNum) +
                " (" + state.killRightNum + ") -- subst not done.");
      }
    }
    else if (state.leftContextPtr > -1 || state.rightContextPtr > -1) {
      stem = state.lexString;
     //tbd do I need to handle real cases of only one context pointer?
        if (authorFlag && traceFlag)
          trace(" ** ERROR ** inconsistent context pointers and substitution context " +
                state.killLeftNum + " " + state.leftContextPtr + " " +
                state.rightContextPtr + " " + 
                (state.lexString.length()-state.killRightNum) +
                " (" + state.killRightNum + ") -- subst not done.");
    }
    else if (state.killLeftNum > -1 && state.killRightNum > -1 &&
             state.killLeftNum + state.killRightNum <= state.lexString.length()) {
      stem = state.lexString.substring(state.killLeftNum,
                                       state.lexString.length()-state.killRightNum);
    }
    else if (state.killLeftNum > -1 &&
             state.killLeftNum <= state.lexString.length()) {
      stem = state.lexString.substring(state.killLeftNum);
    }
    else if (state.killRightNum > -1 &&
             state.killRightNum <= state.lexString.length()) {
      stem = state.lexString.substring(0, state.lexString.length()-state.killRightNum);
    }
    else {
      stem = "";
        if (authorFlag && traceFlag)
          trace(" ** ERROR ** inconsistent killnums " +
                state.killLeftNum + " " + state.leftContextPtr + " " +
                state.rightContextPtr + " " +
                (state.lexString.length()-state.killRightNum) +
                " (" + state.killRightNum + ")");
    }
    state.rootString = state.addLeftString + stem + state.addRightString;
    if (state.rootString.length() < 1) { //keep from making empty word
      state.root = null; // root == null causes morph tests to fail
      if (authorFlag)
        trace("*** null root in setRoot, because rootString is empty");
      state.testval = false;
//      return; // returning here caused null pointer exception in indexer 01-29-05
    }
    if (authorFlag && traceFlag)
      trace (" ** Considering " +
             (isKnownWord(state.rootString) ? "known" : "unknown") +
             " word " + state.rootString + " as root of " +
             (isKnownWord(state.lexString) ? "known" : "unknown") +
             " word " + state.lexString +
             "\n in " +
             ruleName(state.rule, state.ruleNum) +
             " at depth " + state.depth);
    state.root = dict.getWord(state.rootString);
    if (state.root == null) { // root is not already in the lexicon
      state.root = (Word)state.morphCache.get(state.rootString);
      if (state.root == null) { //root is not in the morphCache, so make a scratch word
        state.root = makeScratchWord(state.rootString, state.morphCache);
        state.knownRoot = false;
        if (morphRootWhenSet) {
          // Turn this off if the rule compiler inserts a call to morphRoot in tests,
          // which it currently does.
          morphRoot(state);
        }
      }
    }
    else { // root was already in the lexicon
      if (wordIsEmpty(state.root) && state.root != state.lex) {
        // don't try to markNotYetMorphed if the root is the same as lex
        state.root.markNotYetMorphed();
        if (morphRootWhenSet) {
          // Turn this off if the rule compiler inserts a call to morphRoot in tests,
          // which it currently does.
          morphRoot(state);
        }
      }
      if (authorFlag && traceFlag)
        trace (" marking state.knownroot for " +
               (isKnownWord(state.rootString) ? "known" : "unknown") +
               " word " + state.rootString + "\n as root of " +
               (isKnownWord(state.lexString) ? "known" : "unknown") +
               " word " + state.lexString +
               "\n in " +
               ruleName(state.rule, state.ruleNum) +
               " at depth " + state.depth);
      state.knownRoot = true;
    }
    if (authorFlag && traceFlag)
      trace (" set root of " +
            (isKnownWord(state.lexString) ? "known" : "unknown") +
            " word " + state.lexString + " to " +
            (isKnownWord(state.root.getWordString()) ? "known" : "unknown") +
            " word " + state.root.getWordString()); // + " with testval = " + state.testval);
  }

  protected boolean wordIsEmpty(Word word) {
    // moved the functionality to Lexicon, so it can interact with missing word entries
    if (word == null) return false;
    else return word.wordIsEmpty();
  }

  protected boolean morphRoot(MorphState state) {
    // This is called only in the conditions of rules, at which point
    // root will be bound and either known or marked.  The calls are
    // currently automatically inserted in Morph_en by the rule compiler.
    if (state.root.testNotYetMorphed()) {
      state.root.clearNotYetMorphed();
      String thisRootString = state.root.getWordString();
      if (authorFlag && traceFlag)
        trace ("  now analyzing root: " + thisRootString + " at depth " + state.depth);
      // Get a new morphState to analyze this root
      //tbd -- define a variable subMorphState to remember and reuse this subord MorphState?
      MorphState newState = new MorphState(state.depth+1, state.morphCache);
      Vector subCatSenses = new Vector(); //make new vector of catsenses for this analysis
      int saveDepth = state.depth;
      Word newRoot = analyze(state.root, newState, getFirstRuleSet(),
                                                 ":unnamed", state.depth+1, subCatSenses);
      Word dictRoot = dict.getWord(thisRootString);
      if (dictRoot != null) {  // The word is now known in the lexicon
        state.root = dictRoot; // set root to the new entry, so any more changes go to both
        if (authorFlag && traceFlag)
          trace (" marking state.knownroot for " +
                 (isKnownWord(state.rootString) ? "known" : "unknown") +
                 " word " + state.rootString + " as root of " +
                 (isKnownWord(state.lexString) ? "known" : "unknown") +
                 " word " + state.lexString +
                 "\n in " +
                 ruleName(state.rule, state.ruleNum) + " in morphRoot." +
                 " at depth " + state.depth);
        state.knownRoot = true;
      }
      else if (newRoot != null && newState.knownRoot) {
        //root should now become known in the lexicon, since the root is known (No!)
        //if (authorFlag && traceFlag)
        //  trace ("  making root a known word: " + newRoot.getWordString());
        //dictRoot = newRoot.makeKnownWord();
        //dictRoot = dict.makeEntry(newRoot.printEntryString()); //copy entry to new entry
        if (authorFlag && traceFlag)
          trace (" marking state.knownroot for newState for " +
                 (isKnownWord(state.rootString) ? "known" : "unknown") +
                 " word " + state.rootString + " as root of " +
                 (isKnownWord(state.lexString) ? "known" : "unknown") +
                 " word " + state.lexString +
                 "\n in " +
                 ruleName(state.rule, state.ruleNum) + " in morphRoot." +
                 " at depth " + state.depth);
        state.knownRoot = true; // Record that this lex has a known root
//        state.root = dictRoot; //set root to the new entry, so any more changes go to both
      }
      else if (newRoot != null) {
        state.root = newRoot;
        state.knownRoot = false;
      }
      else {
        state.knownRoot = false;
      }
      state.depth = saveDepth; //restore the depth that was saved  ??tbd necessary?
    }
    return true;
  }

/*
This information describes how the clauses of morphological rules are represented
in Morph_en:

The following is a schema for a typical doCat:

  if (catPreTest(state) && morphRoot(state) && <this test> && catPostTest(state)) {
    // The morphRoot act may be moved inside <this test> after any string-only tests.
    // This can be followed by one or more markDict operations such as the following:
    state.lex.markDict(<feature>, <value>, <add-to-list-flag>); //repeat as many as specified
    // and is followed by an addCatSense to construct the category sense for this act:
    addCatSense(state, cat, root, <inflectional features>);
  }

The following is a schema for a typical doTest:

  state.testval  = (testPreTest(state) && <this test> && testPostTest(state));
  return;

In the current translator, an explicit morphRoot(state) is inserted
into <this test> at the appropriate point if there are any conditions
that require the root to be tested for syntactic categories or other
dictionary information.  This is inserted after any conjoined test
components that depend only on the root string and not a root word.

*/

  /**
   * Methods for making words from concatenated strings.
   */

  public Word morphPackLex(MorphState state, String wordString) {
    // Handle the fact that we don't want to make a real word if
    // this word is only being used in a hypothetical analysis.
    Word word = dict.getWord(wordString);
    if (word == null) word = (Word)state.morphCache.get(wordString);
    if (word == null) {
      word = makeScratchWord(wordString, state.morphCache);
    }
    return word;
  }

  public Word morphPackLex(MorphState state, String s1, String s2) {
    return morphPackLex(state, s1.concat(s2));
  }

  public Word morphPackLex(MorphState state, String s1, String s2,
                                   String s3) {
    return morphPackLex(state, s1.concat(s2).concat(s3));
  }

  public Word morphPackLex(MorphState state, String s1, String s2,
                                   String s3, String s4) {
    return morphPackLex(state, s1.concat(s2).concat(s3).concat(s4));
  }

  /**
   * Method for describing state in tracing.
   */

  protected String stateTrace (MorphState state) {
    return "act " + state.action + " in " + ruleName(state.rule, state.ruleNum) +
           "\n for " +
           (isKnownWord(state.lexString) ? "known" : "unknown") +
           " word " + state.lexString + ", with root = " +
           (isKnownWord(state.root.getWordString()) ? "known" : "unknown") +
           " word " + state.rootString + ",\n " +
           (state.cModeKnownFlag ? "cmode known, " : "") +
           (state.phaseOneFlag ? "phase p1, " : "") +
           (state.phaseTwoFlag ? "phase p2, " : "") +
           (state.rule.definitePhaseOne ? "def P1 rule, " : "") +
           (state.rule.conditionalPhaseOne ? "cond P1 rule, " : "") +
           "with state.knownRoot = " + state.knownRoot;
  }

  /**
   * Method for describing rule in tracing.
   */

  protected String ruleName (MorphRule rule, int ruleNum) {
    String ruleString = rule.toString();
    if (ruleString.length() < 2) return ruleString; // shouldn't happen
    int pos = 1 + ruleString.substring(1).indexOf(":"); // end of block name
    if (pos > 0 && ruleNum > -1) {
      return ruleString.substring(0, pos) + "-" + (ruleNum + 1) +
             ruleString.substring(pos);
    }
    return ruleString;
  }

  /**
   * Method called as part of a doCat test to check phase-one conditions
   * and the results of the most recent previous doTest, if any.
   */

  protected boolean catPreTest (MorphState state) {
    setRoot(state);
    if (authorFlag && traceFlag)
      trace(" catPreTest " + stateTrace(state));
    if (state.root == null) return false;
    if (state.phaseOneFlag && !state.rule.definitePhaseOne &&
        !state.rule.conditionalPhaseOne)
// morphRoot has not yet been done, so we won't know yet if it's a knownRoot
//        !(state.rule.conditionalPhaseOne && state.knownRoot))
      return false;
    if (!state.phaseOneFlag && state.rule.definitePhaseOne) // already tried in p1
      return false;
//    if (state.prefixPhaseFlag && !state.knownRoot)
//      return false;
    return state.testval;
  }

  /**
   * Method called as part of a doCat test to check phase-one conditions
   * and the results of the most recent previous doTest, if any.
   */

  protected boolean catPostTest (MorphState state) {
    if (authorFlag && traceFlag)
      trace(" catPostTest " + stateTrace(state));
    if (state.phaseOneFlag && !state.rule.definitePhaseOne &&
        !(state.rule.conditionalPhaseOne && state.knownRoot))
      return false;
// tbd Lisp test-rule-condition only does this for phase two, is it ok to generalize?
//    if (!state.knownRoot && state.phaseTwoFlag && isGuessedWord(state.root))
// tbd the following is too strong, since the cat test may not even involve a root.
// and it looks like it's commented out in the lisp version
// -- but doing that here caused it to accept plausibleRoot tests in phase one
// -- so tried adding a phase one condition waw 02-01-05
//    if (!state.knownRoot && isGuessedWord(state.root))
// tbd do some of these state.knownRoot tests need to use hasKnownRoot(state.root)?
    if (!state.knownRoot && state.phaseOneFlag && isGuessedWord(state.root))
     // If the word was guessed, then don't base anything on that guess
     // (unless it's a knownRoot because it's in the lexicon already)
      return false;
    if (state.prefixPhaseFlag && !state.knownRoot)
      return false;
    else return state.testval;
  }

  /**
   * Method called as part of a doTest to check phase-one conditions.
   */

  protected boolean testPreTest (MorphState state) {
    if (authorFlag && debugFlag)
      debug ("  testing testPreTest");
    setRoot(state);
    if (authorFlag && traceFlag)
      trace(" testPreTest " + stateTrace(state));
    if (state.root == null) return false;
    state.likelihood = -1;
    if (state.phaseOneFlag && !state.rule.definitePhaseOne &&
        !state.rule.conditionalPhaseOne)
// morphRoot has not yet been done, so we won't know yet if it's a knownRoot
//        !(state.rule.conditionalPhaseOne && state.knownRoot))
      return false;
    if (!state.phaseOneFlag && state.rule.definitePhaseOne) // already tried in p1
      return false;
//    if (state.prefixPhaseFlag && !state.knownRoot)
//      return false;
    else return true;
  }

  /**
   * Method called as part of a doTest to check phase-one conditions.
   */

  protected boolean testPostTest (MorphState state) {
    if (authorFlag && traceFlag)
      trace(" testPostTest " + stateTrace(state));
    if (state.phaseOneFlag && !state.rule.definitePhaseOne &&
                          !(state.rule.conditionalPhaseOne && state.knownRoot))
      return false;
// tbd Lisp test-rule-condition only does this for phase two, is it ok to generalize?
//    if (!state.knownRoot && state.phaseTwoFlag && isGuessedWord(state.root))
// tbd the following is too strong, since the cat test may not even involve a root.
// and it looks like it's commented out in the lisp version
// -- but doing that in catPostTest caused it to accept plausibleRoot tests in phase one
// -- so tried adding a phase one condition waw 02-01-05
//    if (!state.knownRoot && isGuessedWord(state.root))
    if (!state.knownRoot && state.phaseOneFlag && isGuessedWord(state.root))
     // If the word was guessed, then don't base anything on that guess
     // (unless it's a knownRoot because it's in the lexicon already)
      return false;
    if (state.prefixPhaseFlag && !state.knownRoot)
      return false;
    else return true;
  }

  /**
   * Method called as part of testing rule conditions
   * to see if a word has the property 'guessed'.
   */

  protected boolean isGuessedWord (Word root) {
    // To be redefined in Morph_en when dict is known and atom_guessed is bound
    return false;
  }

  /**
   * Method called as part of a doCat to add a catSense to the catSenses
   * for this word.
   */

   protected void addCatSense(MorphState state, Category cat,
                              Word thisRoot, Value[] features) {
    String senseNameString = null;
/*
    if (state.rule != null && state.rule.possibleSplitRule) {
      //rule is null when doing a compound rule
      if (state.suffix != null) {
        if (state.root != thisRoot)
          senseNameString = "!" + cat.printString() + "/" + thisRoot.getWordString() +
                            "/" + state.root.getWordString() +
                            "+" + state.suffix.getWordString();
        else //don't add + suffix if root is = lex
          senseNameString = "!" + cat.printString() + "/" + thisRoot.getWordString() +
                            "/" + state.root.getWordString();
      }
      else if (state.prefix != null) {
        if (state.root != thisRoot)
          senseNameString = "!" + cat.printString() + "/" + thisRoot.getWordString() +
                            "/" + state.prefix.getWordString() +
                            "+" + state.root.getWordString();
        else //don't add + prefix if root is = lex
          senseNameString = "!" + cat.printString() + "/" + thisRoot.getWordString() +
                            "/" + state.root.getWordString();
      }
    }
*/
    if (state.rule != null && state.rule.possibleSplitRule) {
      //rule is null when doing a compound rule
      if (state.suffix != null) {
        if (state.root != thisRoot)
          senseNameString = "!" + cat.printString() + "/" + state.lexString + "/" +
                             state.root.getWordString() + "+" + state.suffix.getWordString();
        else //don't add + suffix if root is = lex
          senseNameString = "!" + cat.printString() + "/" + state.lexString;
      }
      else if (state.prefix != null) {
        if (state.root != thisRoot)
          senseNameString = "!" + cat.printString() + "/" + state.lexString + "/" +
                            state.prefix.getWordString() + "+" + state.root.getWordString();
        else //don't add + prefix if root is = lex
          senseNameString = "!" + cat.printString() + "/" + state.lexString;
      }
    }
    CatSense sense = new CatSense(cat, state.root, features, senseNameString,
                                  state.likelihood);
    if (authorFlag && traceFlag)
      trace(" adding CatSense " + cat + " to " + state.lexString + " at depth " +
            state.depth + "\n with root = " + 
            (isKnownWord(thisRoot.getWordString()) ? "known" : "unknown") +
            " word " + thisRoot.getWordString() + " and features " +
            dict.makeList(features) + " for sense: " + senseNameString);
    state.catSenses.addElement(sense);
    return;
  }

  /**
   * Method to be called by rule actions to set the right-hand kill number.
   */

  protected void killRight(int num, MorphState state) {
    state.killRightNum = num;
    state.rootString = null;
    return;
  }

  /**
   * Method to be called by rule actions to set the left-hand kill number.
   */

  protected void killLeft(int num, MorphState state) {
    state.killLeftNum = num;
    state.rootString = null;
    return;
  }

  /**
   * Method to be called by rule actions to set the right-hand add string.
   */

  protected void addRight(String affix, MorphState state) {
    if (affix.startsWith("&")) {
      state.addRightString = state.rootString.charAt(state.rootString.length()
                                                     - state.killRightNum)
                             + affix.substring(1);
    }
    else state.addRightString = affix;
    state.rootString = null;
    return;
  }

  /**
   * Method to be called by rule actions to set the left-hand add string.
   */

  protected void addLeft(String affix, MorphState state) {
    state.addLeftString = affix;
    state.rootString = null;
    return;
  }

  protected void doSubst(String affix, MorphState state) {
    state.substitutionString = affix;
    state.rootString = null;
    return;
  }

  protected void doTry(MorphRule[] rules, String blockName, MorphState state) {
    int saveDepth = state.depth;
    state.doneFlag = tryRules(rules, blockName, state, state.depth+1, 0);
    state.depth = saveDepth; //restore the depth that got incremented when we called tryRules
    //restore rule that got reset when we called tryRules
    return;
  }

  /**
   * Method to be called by rule actions to try a specified ruleSet.
   */

  protected void doTry(MorphRule[] rules, String blockName, MorphState state, int skipnum) {
    int saveDepth = state.depth;
    MorphRule[] theseRules = rules;
    state.doneFlag = tryRules(theseRules, blockName, state, state.depth+1, skipnum);
    state.depth = saveDepth; //restore the depth that got incremented when we called tryRules
    //restore rule the got reset when we called tryRules
    return;
  }

  /**
   * Method to be called by rule actions to switch to a specified ruleSet.
   */

  protected void doOnly(MorphRule[] rules, String blockName, MorphState state) {
    int saveDepth = state.depth;
    MorphRule[] theseRules = rules;
    state.doneFlag = tryRules(theseRules, blockName, state, state.depth+1, 0);
    state.doneFlag = true; //force an ending whether there have been results so far or not
    state.depth = saveDepth; //restore the depth that got incremented when we called tryRules
    //restore rule the got reset when we called tryRules
    return;
  }

  /**
   * Method to be called by rule actions to switch to a specified ruleSet
   * with a specified skipnum, indicating a number of final characters
   * that have already been tested.
   */

  protected void doOnly(MorphRule[] rules, String blockName, MorphState state, int skipnum) {
    int saveDepth = state.depth;
    MorphRule[] theseRules = rules;
    state.doneFlag = tryRules(theseRules, blockName, state, state.depth+1, skipnum);
    state.doneFlag = true; //force an ending whether there have been results so far or not
    state.depth = saveDepth; //restore the depth that got incremented when we called tryRules
    //restore rule the got reset when we called tryRules
    return;
  }

  /**
   * Method to be called by analyze to build the lexical entry for a word
   * from a vector of catSenses.
   */

  protected void recordResults(Word word, MorphState state,
                               Vector catSenses, int atDepth) {
//    Lexicon dict = getMorphDict();
    Vector senseNames = new Vector();
    String previousNameString = "";
    if (authorFlag && traceFlag) {
      trace("    recording results for: " +
            (isKnownWord(word.getWordString()) ? "known" : "unknown") +
            " word " + word.getWordString() +
            " at depth " + atDepth);
    }
    for (int i = 0 ; i < catSenses.size() ; i++) {
      String thisNameString = ((CatSense)catSenses.elementAt(i)).senseName;
      if (thisNameString != null &&
          (senseNames.size() == 0 || !previousNameString.equals(thisNameString)))
        senseNames.addElement(previousNameString=thisNameString);
    }
    if (senseNames.size() > 1) { //there are more than one inflectional senses
      for (int i = 0 ; i < senseNames.size() ; i++) { // record info for each sense
//        Lexicon.Word senseWord = dict.makeWord((String)senseNames.elementAt(i));
// *** This shouldn't be making known words for senses when the word may be scratch
        Word senseWord = dict.getWord((String)senseNames.elementAt(i));
        if (senseWord == null) {
          senseWord = (Word)state.morphCache.get(senseNames.elementAt(i));
        }
        if (senseWord == null) {
          senseWord = makeScratchWord((String)senseNames.elementAt(i),
                                           state.morphCache);
// This causes dict to make a WordEntry for senseWord, which used to make a real
// entry for word.  Even if I didn't do it here, adding properties to the word
// would require a WordEntry, so I mark the scratch word with a morphCache value
// so that any words that WordEntry needs to make will be made in the same cache
// instead of as real words in dict.
        }
        if (authorFlag && traceFlag)
          trace("    recording results for subsense: " +
                (isKnownWord(senseWord.getWordString()) ? "known" : "unknown") +
                " word " + senseWord.getWordString() +
                " at depth " + atDepth);
        word.addSubsense(senseWord);
        recordCatSenses(senseWord, catSenses, dict, true);
      }
    }
    // then record info for the word (which may be the union of separate sense info)
    recordCatSenses(word, catSenses, dict, false);
//    catSenses.setSize(0); //tbd -- check if I should do this
    if (authorFlag && traceFlag)
      trace("    recorded results for: " +
            (isKnownWord(word.getWordString()) ? "known" : "unknown") +
            " word " + word.getWordString() +
            " at depth " + atDepth + ":\n    "
            + word + " > " + word.printEntryString());
    return;
  }

  protected void recordCatSenses(Word word, Vector catSenses, Lexicon dict,
                                            boolean senseFlag) {
    List penalties = (List)word.getdict(atom_penalties);
    for (int i = 0 ; i < catSenses.size(); i++) {
      CatSense thisCatSense = (CatSense)catSenses.elementAt(i);
      String thisNameString = thisCatSense.senseName;
      if (!senseFlag || thisNameString == null ||
                    thisNameString.equals(word.getWordString())) {
        //do nothing if this catSense is for a different sense
        Category thisCat = thisCatSense.category;
        Word thisRoot = thisCatSense.root;
        Value[] features = thisCatSense.features;
        List localPenalties = 
          (List)getpropFromPlistArray(atom_penalties, features);
        Word localPenalty = 
          (Word)getpropFromPlistArray(atom_penalty, features);
        int penalty = -1;
        int thisPenalty = -1;
        //select the right penalty from features
        if (localPenalty != null)
          penalty = localPenalty.numericalValue().intValue();
        else if ((localPenalties != null) &&
                 ((thisPenalty=getMaxPenalty(thisCat, localPenalties.contents)) > -1))
          // local penalties will override property list
          penalty = thisPenalty;
        else if ((penalty < 0) && (penalties != null) &&
                 ((thisPenalty=getMaxPenalty(thisCat, penalties.contents)) > -1))
        penalty = thisPenalty;
        if (penalty < 0)
          penalty = 0;
        if (!word.sensenamep()) {
          if (word!=thisRoot) word.addRoot(thisRoot);
          // tbd check if I need to add form root otherwise
          else word.markDict(atom_form, atom_root, true);
        }
        processCatSense(word, thisCat, thisRoot, features, penalty);
      }
    }
    return;
  }

  protected int getMaxPenalty (Category cat, Value[] penaltyArray) {
    int maxPenalty = -1;
    for (int i = 0; i < penaltyArray.length ; i++) {
      if (((Category)penaltyArray[i]).subsumesCategory(cat)) {
        maxPenalty =
          Math.max(maxPenalty,
                   ((Word)penaltyArray[i++]).numericalValue().intValue());
      }
    }
    return maxPenalty;
  }

  protected Object getpropFromPlistArray (Object prop, Object[] plistArray) {
    if (plistArray == null || prop == null) return null;
    for (int i = 0; i < plistArray.length ; i++) {
      if (prop == plistArray[i]) {
        return(plistArray[i+1]);
      }
      else i++;
    }
    return null;
  }

  /**
   * Method called by rule actions to mark rule as phase-one.  This is a
   * dummy function that doesn't do anything, since phase-one conditions
   * are now incorporated into features of the rule.
   */

  protected void doPhaseOne(MorphState state) {// consider this rule in phase one
    //don't do anything -- this is already accounted for with boolean flags on the rule
    return;
  }

  /**
   * Method called by rule actions to mark rule as phase-two.  This is a
   * dummy function that doesn't do anything, since phase-one conditions
   * are now incorporated into features of the rule.
   */

  protected void doPhaseTwo(MorphState state) {// reject this rule until phase two
   //don't do anything -- this is already accounted for with boolean flags on the rule
    return;
  }

  /**
   * Class for carrying information for a particular category sense of an
   * analyzed word.
   */

  protected class CatSense {
    protected Category category;
    protected Word root;
    protected Value[] features;
    protected String senseName;
    protected int probability;

    protected CatSense(Category cat, Word word, Value[] feats,
                                        String senseNameString, int prob) {
      category = cat;
      root = word;
      features = feats;
      senseName = senseNameString;
      probability = prob;
    }
  }

  /**
   * Class for carrying root-feature information for a particular category sense of an
   * analyzed word.
   */

  public class RootFeatureList {
    protected Word root;
    protected Value[] features;

    protected RootFeatureList(Word word, Value[] feats) {
      root = word;
      features = feats;
    }
  }

    /**
     * Method that determines if a word string matches the current rule
     */

    public Vector match(String wordString, MorphRule thisRule, MorphState state,
                           int depth, int skipnum) {
//        Vector catSenses = new Vector();
//        String wordString = word.getWordString();
//        MorphRule thisRule = state.rule; //capture the current rule before it can change
        String leftContext = "";
        String rightContext = "";
        int killRight = state.rule.killnum;
        int killLeft = state.rule.leftkillnum;
        int patternLength = state.rule.pattern.length;
        int matchCount = 0; //how many times this elt matched
        state.leftContextPtr = -1; //saved left context pointer
        state.rightContextPtr = -1; //saved right context pointer
        int started = 0; //flag indicating that right end is bound
        int [] altState = null;
        Vector alts = new Vector();
        HashMap doneStates = new HashMap();
        Long stateCode;
//        Integer stateCode;
//        String stateCode = null;
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
                state.leftContextPtr = altState[3]; //saved left context pointer
                state.rightContextPtr = altState[4]; //saved right context pointer
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
//              stateCode = new Integer(2 * (position + length * i) + matchCount);
//              stateCode = 2 * position + 100 * i + matchCount; //length < 50 usually
//              stateCode = "s:"+position+":"+i+":"+matchCount;
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

            //"<" set left context
            if (state.rule.pattern[i].equals("<")) {
                //we can assume we're started since < has to have a matching > to the right
                state.leftContextPtr = position+1;
                leftContext = wordString.substring(0,position+1);
                i--;
            }

            //">" set right context
            else if (state.rule.pattern[i].equals(">")) { //if not started, save alternative
                if (started < 1 && position > 0) //we're in an unanchored pattern
                  //generate an alt to try to start further left if this fails
                  makeAltState(position-1, i, 0, state.leftContextPtr, state.rightContextPtr,
                               killLeft, killRight, started, alts, doneStates, length);
                started = 1; //unconditional in case we are starting at position 0
                state.rightContextPtr = position+1;
                rightContext = wordString.substring(position+1);
                i--;
            }

            //check left end conditions
            else if (position < 0) {
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
                  if (position > 0)
                    makeAltState(position-1, i, 1, state.leftContextPtr, state.rightContextPtr,
                                 killLeft, killRight, started, alts, doneStates, length);
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
                  if (position > 0)
                    makeAltState(position-1, i, matchCount,
                                 state.leftContextPtr, state.rightContextPtr,
                                 killLeft, killRight, started, alts, doneStates, length);
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

            //"*" pattern can match any number of times (including zero)
            else if (state.rule.pattern[i].startsWith("*")) { 
                if (state.rule.pattern[i].indexOf(wordString.charAt(position), 1) >= 0) {
                  //it matches here, so save alts and go to next position
                  if (started < 1 && position > 0) //if we're in an unanchored pattern
                    //generate an alt to try to start further left if this fails
                    makeAltState(position-1, i, 0,
                                 state.leftContextPtr, state.rightContextPtr,
                                 killLeft, killRight, started, alts, doneStates, length);
                  //then generate an alt for the case of not counting this match
                  makeAltState(position, i-1, 0, state.leftContextPtr, state.rightContextPtr,
                               killLeft, killRight, started, alts, doneStates, length);
                  //then pursue alternative of looking for an additional match to the left
                  if (matchCount > 0) { //adjust killnums to compensate for additional matches
                    if (i >= patternLength-state.rule.killnum) killRight++;
                    else if (i < state.rule.leftkillnum) killLeft++;
                  }
                  started = 1;
                  matchCount = 1; //matchCount indicates now satisfied
                  position--;
                //  i--;
                }
                else if (state.rule.leftAnchor && (i == 0) && (position >= 0)) {
                  matched = false;
                //  break;
                }
                else if (matchCount > 0) {
                  matchCount = 0; //matchCount for next pattern element
                  i--;
                }
                else if (started < 1) {
                  if (i >= patternLength-state.rule.killnum) killRight++;
                  else if (i < state.rule.leftkillnum) killLeft++;
                  position--;
                }
                else { //go to next pattern element at this position (optionality clause)
                  //adjust killnums for failure to find a match
                  if (i >= patternLength-state.rule.killnum) killRight--;
                  else if (i < state.rule.leftkillnum) killLeft--;
                  matchCount = 0; //matchCount for next pattern element
                  i--; 
                }
            }

            //"+" pattern must match one or more times
            else if (state.rule.pattern[i].startsWith("+")) { 
                if (state.rule.pattern[i].indexOf(wordString.charAt(position), 1) >= 0) {
                  //it matches here, so save alts and go to next position
                  if (started < 1 && position > 0) //we're in an unanchored pattern
                    //generate an alt to try to start further left if this fails
                    makeAltState(position-1, i, 0,
                                 state.leftContextPtr, state.rightContextPtr,
                                 killLeft, killRight, started, alts, doneStates, length);
                  if (matchCount > 0) {//have found at least one matching element
                    //so generate an alt for the case of not counting this match
                    makeAltState(position, i-1, 0,
                                 state.leftContextPtr, state.rightContextPtr,
                                 killLeft, killRight, started, alts, doneStates, length);
                    //and adjust killnums:
                    if (i >= patternLength-state.rule.killnum) killRight++;
                    else if (i < state.rule.leftkillnum) killLeft++;
                  }
                  started = 1;
                  matchCount = 1; //matchCount indicates now satisfied
                  position--;
                //  i--;
                }
                else if (state.rule.leftAnchor && (i == 0) && (position >= 0)) {
                  matched = false;
                //  break;
                }
                else if (matchCount > 0) {
                  matchCount = 0; //matchCount for next pattern element
                  i--;
                }
                else if (started < 1) {
                  if (i >= patternLength-state.rule.killnum) killRight++;
                  else if (i < state.rule.leftkillnum) killLeft++;
                  position--;
                }
                else {
                  matched = false;
                //  break;
                } 
            }

            //"?" pattern is optional
            else if (state.rule.pattern[i].startsWith("?")) { 
                if (state.rule.pattern[i].indexOf(wordString.charAt(position), 1) >= 0) {
                  //it matches here, so save alts and go to next position
                  if (started < 1 && position > 0) //we're in an unanchored pattern
                    //generate an alt to try to start further left if this fails
                    makeAltState(position-1, i, 0,
                                 state.leftContextPtr, state.rightContextPtr,
                                 killLeft, killRight, started, alts, doneStates, length);
                  //then generate an alt for the case of not counting this match
                  makeAltState(position, i-1, 0, state.leftContextPtr, state.rightContextPtr,
                               killLeft, killRight, started, alts, doneStates, length);
                  started = 1;
                  position--;
                }
                else if (started < 1) {
                  if (i >= patternLength-state.rule.killnum) killRight++;
                  else if (i < state.rule.leftkillnum) killLeft++;
                  position--;
                }
                else if (state.rule.leftAnchor && (i == 0) && (position >= 0)) {
                  matched = false;
                //  break;
                }
                else {
                  if (i >= patternLength-state.rule.killnum) killRight--;
                  else if (i < state.rule.leftkillnum) killLeft--;
                } //otherwise, go to next pattern element at this position
                matchCount = 0; //matchCount for next pattern element
                i--; 
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
                  if (((started < 1) || (matchCount > 0)) && (position > 0))
                    //matchCount > 0 here means that we're working on an alt
                    //that came from a not-started altState
                    makeAltState(position-1, i, 1, state.leftContextPtr, state.rightContextPtr,
                                 killLeft, killRight, started, alts, doneStates, length);
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
                if (state.leftContextPtr > -1) //only needed for tracing //tbd not necessary
                  leftContext = wordString.substring(killLeft, state.leftContextPtr);
                if (state.rightContextPtr > -1) //only needed for tracing //tbd not necessary
                  rightContext = wordString.substring(state.rightContextPtr, length-killRight);
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
//          recordResults(word, state, state.catSenses, depth);
          if (authorFlag && traceFlag) {
            if ((leftContext.length() > 0) || (rightContext.length() > 0))
              trace("   finished rule: " + ruleName(thisRule, state.ruleNum) +
                    " at depth " + depth +
                    "\n        " + "     with leftContext = "+leftContext+
                    " and rightContext = "+rightContext+
                    "\n             with matched = "+matched+" and "+ 
                    state.catSenses.size() + " forms generated");
            else trace("   finished rule: " + ruleName(thisRule, state.ruleNum) +
                       " at depth " + depth +
                       "\n        " + "     with matched = "+matched+" and "+ 
                       state.catSenses.size() + " forms generated");
          }
          return state.catSenses;
        }
        else return null;
    }

  /**
   * Method for doing actions in a rule.
   */

  protected void doAction(int action, MorphState state) {
    if (authorFlag) {
      state.action = action; // for catPreTest & testPreTest to trace
      if (debugFlag)
        debug("   doing action: " + action);
    }
    doNumberedAction(action, state);
  }

  /**
   * Method for saving an alernative altState for matching the pattern of a rule.
   */

  protected void makeAltState(int position, int i, int matchCount,
                            int leftContextPtr, int rightContextPtr,
                            int killLeft, int killRight, int started,
                            Vector altStates, HashMap doneStates, int length) {
    Long stateCode = new Long(((long) position << 32) |
                              ((long) i << 16) |
                              matchCount);
//    Integer stateCode = new Integer(2 * (position + length * i) + matchCount);
//    int stateCode = 2 * position + 100 * i + matchCount;
//    String stateCode = "s:"+position+":"+i+":"+matchCount;
    if ((position > -1) && (i > -1) &&         //only need stateCodes for position and i >= 0
        (doneStates.get(stateCode) == null)) { //only add an altState if haven't already
      doneStates.put(stateCode, stateCode);
      int[]  altState = new int[8];
      altState[0] = position;
      altState[1] = i;
      altState[2] = matchCount;
      altState[3] = leftContextPtr;
      altState[4] = rightContextPtr;
      altState[5] = killLeft;
      altState[6] = killRight;
      altState[7] = started;
      if (authorFlag && debugFlag)
        debug("   saving alternative: " + (altState[1]+1) + ":" +
                  altState[2] + " at " + (altState[0]+1));
      altStates.addElement(altState);
    }
    return;
  }

  /**
   * Method for trying a specified set of morphological rules.
   */

  protected boolean tryRules(MorphRule[] rules, String blockName, MorphState state,
                             int atDepth, int skipnum) {
    state.depth = atDepth;
    Vector results = null;

    if (authorFlag && traceFlag)
      trace(" Analyzing " + state.lexString + " with rules " + blockName +
            " at depth " + state.depth);

    for (int n = 0; n < rules.length; n++) {
      initializeState(state);
      state.rule = rules[n]; // Set rule to use for this call to match.
      if (authorFlag && traceFlag) state.ruleNum = n; // only need this for tracing
      if (!(state.phaseOneFlag && !state.rule.definitePhaseOne &&
                                  !state.rule.conditionalPhaseOne)) {
        if (authorFlag && debugFlag)
          debug("   trying rule " + blockName + "-" + (1 + n) + ":" +
                "\n    " + state.rule + ", at depth " + state.depth);
        results = match(state.lexString, state.rule, state, state.depth, skipnum);
        if (results != null) { // rule n matched
          if (authorFlag) {
            if (traceFlag)
              trace("  " + (isKnownWord(state.lexString) ? "known" : "unknown") +
                    " word " + state.lexString + ": generated " +
                    state.catSenses.size() +
                    " results for rule\n " + blockName + "-" + (n+1) +
                    ": " + state.rule + ", at depth " + state.depth);
            state.lex.markDict(dict.derivationAtom,
                         // Need blockName.substring(1) to remove the colon
                         dict.makeAtom(blockName.substring(1) + "-" + (n+1)),
                         true);
          }
          break;
        }
      }
    }
    return ((results != null) || state.doneFlag); // doneFlag is set by doOnly dispatch
  }


// general utility methods for use during rule processing:

  /**
   * Method for testing if two words are in alphabetical order.
   * alphorder(x y) is a general ordering function for sorting
   * any mixture of numbers and words.  It orders things in the
   * way a dictionary might, rather than in their ascii order. 
   * Specifically, all numbers come first, ordered by numerical
   * value.  Character sequences are ordered so that strings 
   * with the same letter sequence but different case (or 
   * different accents) will be sorted together.
   * Characters are ordered by a function (compareChars)
   * in which special chars precede digits, which precede
   * alphabetics, and capitals only precede lowercase
   * characters when they are the same letter. A sample
   * of the ordering assigned is: (0.2 2 3.14 20 ! - [ ~
   * 2A #\\A #\\a A \\a \"A\" \"a\" #\\? \\? \"?\" \"?\" \"?\" \"?\" \"?\"
   * ALPHA \"Alpha\" \"alPha\" \"alpha\" #\\B).
   * @param word1 The putative earlier word in the order.
   * @param word2 The putative later word in the order.
   */

  public int alphorder(Word word1, Word word2) {
    if (word1 == null) {
      if (word2 == null) return 0;
      else return -1; //null comes before anything
    }
    else if (word2 == null) return 1;
    else if (word1 == word2) return 0;
    else if (word1.numericalValue() != null) {
      if (word2.numericalValue() != null) {
        double val1 = word1.numericalValue().doubleValue();
        double val2 = word2.numericalValue().doubleValue();
        if (val1 > val2) return 1;
        else if (val1 < val2) return -1;
        // integers come before equivalent doubles:
        else if (word1.numericalValue() instanceof Integer &&
                ! (word2.numericalValue() instanceof Integer)) return -1;
        else if (word2.numericalValue() instanceof Integer &&
                ! (word1.numericalValue() instanceof Integer)) return 1;
        // Now we have two integers or two doubles with same value but different
        // strings E.g., 1 and 001 or 1.2 and 1.200, so don't return here,
        // but fall through to the string comparison below
      }
      else return -1; 
    }
    else if (word2.numericalValue() != null) return 1; //numbers come before alphabetic
    String wordString1 = word1.getWordString(); //case doesn't matter for ordering words
    String wordString2 = word2.getWordString();
    if (wordString1 == null) {
      if (wordString2 == null) return 0;
      else return -1; //empty comes before anything
    }
    else if (wordString2 == null) return 1;
    return compareStrings(wordString1, wordString2);
  }

  /**
   * Method for testing if two strings are in alphabetical order.
   * alphorder(x y) is a general ordering function for sorting
   * any mixture of numbers and words.  It orders things in the
   * way a dictionary might, rather than in their ascii order. 
   * Specifically, all numbers come first, ordered by numerical
   * value.  Character sequences are ordered so that strings 
   * with the same letter sequence but different case (or 
   * different accents) will be sorted together.
   * Characters are ordered by a function (compareChars)
   * in which special chars precede digits, which precede
   * alphabetics, and capitals only precede lowercase
   * characters when they are the same letter. A sample
   * of the ordering assigned is: (0.2 2 3.14 20 ! - [ ~
   * 2A #\\A #\\a A \\a \"A\" \"a\" #\\? \\? \"?\" \"?\" \"?\" \"?\" \"?\"
   * ALPHA \"Alpha\" \"alPha\" \"alpha\" #\\B).
   * @param string1 The putative earlier string in the order.
   * @param string2 The putative later string in the order.
   */

  public int alphorder(String string1, String string2) {
    if (string1 == null) {
      if (string2 == null) return 0;
      else return -1; //null comes before anything
    }
    else if (string2 == null) return 1;
    else if (string1.equals(string2)) return 0;
    Double num1 = null;
    Double num2 = null;
    try {num1 = new Double(string1);
    } //see if this string has a numerical value
    catch (NumberFormatException e) {
    }
    if (Character.isLetter(string1.charAt(string1.length()-1))){
      //"27d" is seen as a double by Java reader....fix it
      num1 = null;
    }
    try {num2 = new Double(string2);
    } //see if this string has a numerical value
    catch (NumberFormatException e) {
    }
    if (Character.isLetter(string2.charAt(string2.length()-1))){
      //"27d" is seen as a double by Java reader....fix it
      num2 = null;
    }
    if (num1 != null) {
      if (num2 != null) {
        double val1 = num1.doubleValue();
        double val2 = num2.doubleValue();
        if (val1 > val2) return 1;
        else if (val1 < val2) return -1;
        // integers come before equivalent doubles:
        if (string1.indexOf(".") < 0) {
          if (string2.indexOf(".") >= 0) return -1;
          //otherwise they're both integers, so fall through to string test
        }
        else if (string2.indexOf(".") < 0) return 1;
        // otherwise they're both doubles, so fall through to string test
        // Now we have two integers or two doubles with same value but different
        // strings E.g., 1 and 001 or 1.2 and 1.200, so don't return here,
        // but fall through to the string comparison below
      }
      else return -1; 
    }
    else if (num2 != null) return 1; //numbers come before alphabetic
    String lowercaseString1 = string1.toLowerCase();
    String lowercaseString2 = string2.toLowerCase();
    int stringOrder = compareStrings(lowercaseString1, lowercaseString2);
    if (stringOrder != 0) return stringOrder; //case is used only to resolve ties
    else return compareStrings(string1, string2);
  }

  /**
   * Method for testing if two words are in alphabetical order,
   * considering two associated capitalization strings to resolve
   * ties.
   * alphorder(x y) is a general ordering function for sorting
   * any mixture of numbers and words.  It orders things in the
   * way a dictionary might, rather than in their ascii order. 
   * Specifically, all numbers come first, ordered by numerical
   * value.  Character sequences are ordered so that strings 
   * with the same letter sequence but different case (or 
   * different accents) will be sorted together.
   * Characters are ordered by a function (compareChars)
   * in which special chars precede digits, which precede
   * alphabetics, and capitals only precede lowercase
   * characters when they are the same letter. A sample
   * of the ordering assigned is: (0.2 2 3.14 20 ! - [ ~
   * 2A #\\A #\\a A \\a \"A\" \"a\" #\\? \\? \"?\" \"?\" \"?\" \"?\" \"?\"
   * ALPHA \"Alpha\" \"alPha\" \"alpha\" #\\B).
   */

  public int alphorder(Word word1, Word word2,
                                    String wordString1, String wordString2) {
    if (word1 == null) {
      if (word2 == null) return 0;
      else return -1; //null comes before anything
    }
    else if (word2 == null) return 1;
    else if (word1 == word2) return compareStrings(wordString1, wordString2);
    else if (word1.numericalValue() != null) {
      if (word2.numericalValue() != null) {
        double val1 = word1.numericalValue().doubleValue();
        double val2 = word2.numericalValue().doubleValue();
        if (val1 > val2) return 1;
        else if (val1 < val2) return -1;
        // integers come before equivalent doubles:
        else if (word1.numericalValue() instanceof Integer &&
                ! (word2.numericalValue() instanceof Integer)) return -1;
        else if (word2.numericalValue() instanceof Integer &&
                ! (word1.numericalValue() instanceof Integer)) return 1;
        // Now we have two integers or two doubles with same value but different
        // strings E.g., 1 and 001 or 1.2 and 1.200, so don't return here,
        // but fall through to the string comparison below
      }
      else return -1; 
    }
    else if (word2.numericalValue() != null) return 1; //numbers come before alphabetic
    if (wordString1 == null) wordString1 = word1.getWordString();
    if (wordString2 == null) wordString2 = word2.getWordString();
    if (wordString1 == null) {
      if (wordString2 == null) return 0;
      else return -1; //empty comes before anything
    }
    else if (wordString2 == null) return 1;
    else return compareStrings(wordString1, wordString2);
  }

  /**
   * Method for testing if two values are in alphabetical order.
   * alphorder(x y) is a general ordering function for
   * sorting arbitrary Values with a sort function.
   * This works for any mixture of symbols, numbers, lists,
   * characters, and strings. It orders things in the way
   * a dictionary might, rather than in their ascii order. 
   * Specifically, all numbers come first, ordered by
   * numerical value.  All lists come last, ordered
   * by the order of their first different corresponding
   * elements. Symbols, strings, and characters come after
   * numbers, ordered by the alphabetical order of their
   * character sequences and subordered by case and type
   * (so that, e.g., symbols and strings with the same
   * sequence of letters will occur together).  Character
   * sequences are ordered so that strings with the same 
   * letter sequence but different case (or different 
   * accents) will be sorted together.
   * Characters are ordered by a function (compareChars)
   * in which special chars precede digits, which precede
   * alphabetics, and capitals only precede lowercase
   * characters when they are the same letter. A sample
   * of the ordering assigned is: (0.2 2 3.14 20 ! - [ ~
   * 2A #\\A #\\a A \\a 'A' 'a' #\\? \\? '?' '?' '?' '?' '?'
   * ALPHA \"Alpha\" \"alPha\" \"alpha\" #\\B).
   */

  public int alphorder(Value value1, Value value2) {
    if (value1 == null) {
      if (value2 == null) return 0;
      else return -1; //null comes before anything
    }
    else if (value2 == null) return 1;
    else if (value1.listp()) {
      if (value2.listp()) {
        Value[] contents1 = ((List)value1).contents;
        Value[] contents2 = ((List)value2).contents;
        int length = contents1.length;
        if (length > contents2.length)
          length = contents2.length;
        for (int i = 0 ; i < length ; i++) {
          if(!(contents1[i].equals(contents2[i])))
            return alphorder(contents1[i], contents2[i]);
        }
        if (contents1.length < contents2.length) return -1;
        else if (contents1.length > contents2.length) return 1;
        else return 0;
      }
      else return 1; 
    }
    else if (value1.numericalValue() != null) {
      if (value2.numericalValue() != null) {
        double val1 = value1.numericalValue().doubleValue();
        double val2 = value2.numericalValue().doubleValue();
        if (val1 > val2) return 1;
        else if (val1 < val2) return -1;
        // integers come before equivalent doubles:
        if (value1.numericalValue() instanceof Integer) return -1; //integers come first
        else if (value2.numericalValue() instanceof Integer) return 1;
        else return 0;
      }
      else return -1; 
    }
    else if (value2.numericalValue() !=null) return 1; //numbers come before alphabetic
    String valueString1 = value1.printString(); //case doesn't matter for ordering atoms
    String valueString2 = value2.printString();
    if (valueString1 == null) {
      if (valueString2 == null) return 0;
      else return -1; //empty comes before anything
    }
    else if (valueString2 == null) return 1;
//    int stringOrder = valueString1.compareTo(valueString2);
    int stringOrder = compareStrings(valueString1, valueString2);
    return stringOrder;
  }

  /**
   * Method for comparing two strings for alphabetical order.
   */

  public int compareStrings (String string1, String string2) {
//    if (authorFlag && traceFlag) trace("  comparing " + string1 + " with " + string2);
    char char1, char2;
    int caseResult = 0;
    // Set length to the shortest of the two strings.
    int length = string1.length();
    if (length > string2.length())
      length = string2.length();
    for (int i = 0 ; i < length ; i++) {
      char1 = string1.charAt(i);
      char2 = string2.charAt(i);
      if (char1 == char2) { //keep searching
      }
      else if ((Character.toLowerCase(char1) == Character.toLowerCase(char2)) ||
              ("Aa??????????????".indexOf(char1) >= 0 &&
               "Aa??????????????".indexOf(char2) >= 0) ||
              ("Ee????????".indexOf(char1) >= 0 &&
               "Ee????????".indexOf(char2) >= 0) ||
              ("Ii????????".indexOf(char1) >= 0 &&
               "Ii????????".indexOf(char2) >= 0) ||
              ("Oo??????????????".indexOf(char1) >= 0 &&
               "Oo??????????????".indexOf(char2) >= 0) ||
              ("Uu????????".indexOf(char1) >= 0 &&
               "Uu????????".indexOf(char2) >= 0) ||
              ("Cc??".indexOf(char1) >= 0 &&
               "Cc??".indexOf(char2) >= 0) ||
              ("Nn??".indexOf(char1) >= 0 &&
               "Nn??".indexOf(char2) >= 0) ||
              ("Yy??".indexOf(char1) >= 0 &&
               "Yy??".indexOf(char2) >= 0) ) {
        if (caseResult == 0) { //this is first character with just a case or accent difference
            caseResult = compareChars(char1, char2);
        }
      }
      else {
        int charOrder = compareChars(string1.charAt(i), string2.charAt(i));
        if (charOrder != 0) return charOrder;
      }
    }
    if (caseResult != 0) return caseResult;
    else if (string1.length() < string2.length())
      return -1;
    else if (string1.length() == string2.length())
      return 0;
    else return 1;
  }

  /**
   * Method for comparing two chars for alphabetical order.
   * char-order (x y) is an ordering function for
   * characters used in alphorder.  It orders things
   * in the way a dictionary might, rather than in
   * ascii order --  specifically, all special chars
   * come first, followed by digits, followed by
   * alphabetics, and capitals only precede lowercase
   * characters when they are the same letter.  Also,
   * accented characters come after the character they
   * accent, with the following collating sequence:
   * A a ? ? ? ? ? ? ? ? ? ? ? ? ? ? B b C c ? ? D d
   * E e ? ? ? ? ? ? ? ? F f I i ? ? ? ? ? ? ? ? J j
   * N n ? ? O o ? ? ? ? ? ? ? ? ? ? ? ? ? ? P p
   * U u ? ? ? ? ? ? ? ? V v Y y ? ? Z z
   * (some of the upper case versions are nonprinting).
   */

  public int compareChars (char char1, char char2) {
//    if (authorFlag && traceFlag) trace("  comparing " + char1 + " with " + char2);
    String alphaChars =
     "Aa??????????????BbCc??DdEe????????FfGgHhIi????????JjKkLlMmNn??Oo??????????????PpQqRrSsTtUu????????VvWwXxYy??Zz";
    int pos1 = alphaChars.indexOf(char1);
    int pos2 = alphaChars.indexOf(char2);
    if (char1 == char2) return 0;
    else if (pos1 >= 0) { //char1 is alphabetic
      if (pos1 < pos2) return -1; //they're in order
      else return 1; //this includes the case of pos2 < 0 (and equal case is already done)
    }
    else if (pos2 >= 0) return -1; //char1 is not alpha and char2 is; so they're in order
    else if (Character.isLetter(char1) && !Character.isLetter(char2)) return 1;
    else if (Character.isLetter(char2) && !Character.isLetter(char1)) return -1;
    else if (Character.isDigit(char1) && !Character.isDigit(char2)) return -1;
    else if (Character.isDigit(char2) && !Character.isDigit(char1)) return 1;
    else if ((char1 == '_') && !(" \t\n\r".indexOf(char2) >= 0)) return -1; // _ precedes all
    else if ((char2 == '_') && !(" \t\n\r".indexOf(char1) >= 0)) return 1;  // but white space
    else if (char1 < char2) return -1;
    else if (char2 < char1) return 1;
    else return 0;
  }

  /**
   * Method for testing if a character is one of a string of characters.
   */

  public boolean charIsOneOf (char character, String testString) {
    if (testString.indexOf(character) < 0) return false;
    else return true;
  }

  /**
   * Method for searching for a position where a substring findString
   * occurs in a larger string.
   */

  public int search (String findString, String inString) {
    int searchLength = inString.length()-findString.length();
    if (searchLength < 0) return -1; 
    int result = -1;
    for (int n = 0; n <= searchLength; n++) {
      if (inString.startsWith(findString, n)) { // look for match at nth substring
        result = n;
        break;
      }
    }
    return result;
  }

  /**
   * Method for testing whether a word string ends with a specified pattern.
   */

  public boolean patternCheck (String testString, String[] patternArray) {
    int pos = testString.length()-patternArray.length;
    if (pos < 0) return false;
    String patternElement;
    for (int n = 0; n < patternArray.length; n++) {
      patternElement = patternArray[n];
      if (patternElement.indexOf(testString.charAt(pos)) < 0)
        return false;
      pos++;
    }
    return true;
  }

  /**
   * Method for testing whether a word string starts with a specified pattern.
   */

  public boolean patternCheckLeft (String testString, String[] patternArray) {
    if (testString.length()-patternArray.length < 0) return false;
    int pos = 0;
    String patternElement;
    for (int n = 0; n < patternArray.length; n++) {
      patternElement = patternArray[n];
      if (patternElement.indexOf(testString.charAt(pos)) < 0)
        return false;
      pos++;
    }
    return true;
  }

  /**
   * Method for testing whether a word ends with a specified pattern.
   */

  public boolean patternCheck (Word testWord, String[] patternArray) {
    return patternCheck(testWord.getWordString(), patternArray);
  }

  /**
   * Method for testing whether a word string starts with a specified pattern.
   */

  public boolean patternCheckLeft (Word testWord, String[] patternArray) {
    return patternCheckLeft(testWord.getWordString(), patternArray);
  }

  /**
   * Method for testing whether a word string ends with a specified pattern.
   */

  public boolean patternCheck (String testString, String pattern) {
    return patternCheck(testString, makePatternArray(pattern));
  }

  /**
   * Method for testing whether a word starts with a specified pattern.
   */

  public boolean patternCheckLeft (String testString, String pattern) {
    return patternCheckLeft(testString, makePatternArray(pattern));
  }

  /**
   * Method for making a pattern array by parsing a pattern string.
   */

  public String[] makePatternArray (String patternString) {//make array of pattern elts
    Vector patternBuffer = new Vector();
    StringTokenizer temp = new StringTokenizer(patternString, ", \t\n\r");
    while (temp.hasMoreTokens()) {
      patternBuffer.addElement(temp.nextToken());
    }
    String[] patternArray = new String[patternBuffer.size()];
      for(int i = 0; i < patternBuffer.size(); i++) {
        patternArray[i] = (String)patternBuffer.elementAt(i);
      }
    return patternArray;
  }

  /**
   * Method for getting the kth value from a list.
   */

  public Value kthValue (Value x, int k) {
    if (!(x.listp()) || k < 1 || k > ((List)x).length()) return null;
    Value val = ((List)x).elementAt(k-1);
    return val;
    }

  /**
   * Method for getting the kth Value from a list, cast as a Word, if it is a Word.
   */

  public Word kthWord (Value x, int k) {
    if (!(x.listp()) || k < 1 || k > ((List)x).length()) return null;
    Value val = ((List)x).elementAt(k-1);
    if ((val.wordp())) return (Word) val;
    else return null;
    }

  /**
   * Method for getting the kth Value from a list, cast as an Atom, if it is an Atom.
   */

  public Atom kthAtom (Value x, int k) {
    if (!(x.listp()) || k < 1 || k > ((List)x).length()) return null;
    Value val = ((List)x).elementAt(k-1);
    if (!(val.listp()) && !(val.wordp()))
      return (Atom) val;
    else return null;
    }

  /**
   * Method for getting the kth Value from a list, cast as a list, if it is a list.
   */

  public List kthList (Value x, int k) {
    if (!(x.listp()) || k < 1 || k > ((List)x).length()) return null;
    Value val = ((List)x).elementAt(k-1);
    if ((val.listp())) return (List) val;
    else return null;
    }

  protected static String className = "MorphEngine";


  /**
   * Method for debugging complex Boolean conditions by inserting a
   * trace printout inside a Boolean AND.
   */

    boolean truPrint(String ps, boolean flag){
      if (authorFlag && flag) System.out.print(ps);
      return true;
    }

//  protected static final boolean debugFlag = false; //uncomment for production version
//  protected static final boolean traceFlag = false; //uncomment for production version
  public static final boolean authorFlag = false;  //change to false for production version
  protected static boolean debugFlag = false; //can comment out for production version
  protected static boolean traceFlag = false; //can comment out for production version

  /**
   * For debugging the rule matcher by showing details of the match process.
   */

  public static void debugOn() {
  debugFlag = true;
  traceFlag = true;
  }

  /**
   * For debugging the rule matcher by showing details of the match process.
   */

  public static void debugOff() {
  debugFlag = false;
  traceFlag = false;
  }

  /**
   * For debugging the rule matcher by showing details of the match process.
   */

  protected void debug(String str) {
    if (debugFlag) {
       System.out.println(className + // ": " + state.lexString +
                          ": " + str);
    }
  }

  /**
   * For debugging rules by tracing the results of each rule attempt.
   */

  public static void traceOn() {
  traceFlag = true;
  }

  /**
   * For debugging rules by tracing the results of each rule attempt.
   */

  public static void traceOff() {
  traceFlag = false;
  }

  /**
   * For debugging rules by tracing the results of each rule attempt.
   */

  protected void trace(String str) {
    if (traceFlag) {
       System.out.println(className + // ": " + state.lexString +
                          ": " + str);
    }
  }
}
