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

import java.util.Hashtable;
import java.util.Vector;
//import MorphEngine;
//import Lexicon;

/*
; copyright (c) by W.A. Woods.  All rights reserved.
; The following software was created in 1989, 1990, 1991, 1992, 1993, 1994,
; 1995, 1996, 1997, 1998, 1999, 2000, 2001, 2002, 2004, and 2005,
; but has not been published within the meaning of the copyright law.
; This file may be made available by the author to selected individuals
; for specific research projects, but may not to be copied, distributed, or
; used for other purposes without explicit permission from the author.

; This software is licensed "as is." The author expressly disclaims any
; warranties of any kind and accepts no liabilities resulting from its use.
*/

/**
 * This is a class of objects used to carry the state of a morphological analysis.
 *
 * MorphState was designed to support multithreading by allowing each morph thread
 * to instantiate a different MorphState instance to keep the state of morphological
 * analyses on that thread.  During morphological analysis, additional instances
 * of MorphState may be created for managing the morphological analyses of
 * potential roots of a word being analyzed.
 *
 * @author W. A. Woods
 * 
 * @version	1.0
 * 
 */

public class MorphState {

  protected Hashtable morphCache = null;
  public MorphEngine frame = null;

  /**
   * Create a MorphState
   */
  public MorphState() {
    depth = 0;
    morphCache = new Hashtable();
  }

  /**
   * Create a MorphState
   * @param startDepth The depth of analysis this state is used for.
   * @param cache The morphCache to use at this level.
   */
  public MorphState(int startDepth, Hashtable cache) {
    depth = startDepth;
    if (cache == null) morphCache = new Hashtable();
    else morphCache = cache;
  }

  /**
   * Create a MorphState for testing a MorphRule
   * @param startDepth The depth of analysis this state is used for.
   * @param cache The morphCache to use for this test.
   * @param morph The MorphEngine to use.
   * @param testrule A Rule to test.
   * @param teststring A string to test the rule against.
   */
  public MorphState(int startDepth, Hashtable cache, MorphEngine morph,
                    MorphRule testrule, String teststring) {
    depth = startDepth;
    if (cache == null) morphCache = new Hashtable();
    else morphCache = cache;
    frame = morph;
    rule = testrule;
    catSenses = new Vector();
    morphCache = new Hashtable();
    lexString = teststring;
    lex = morph.getMorphDict().getWord(teststring);
  }



// state variables for rule matching:

  protected Word lex;
  protected String lexString;

  protected boolean cModeKnownFlag = false; //morph lex in cmode = known mode
  protected MorphRule rule; //current rule, so match & catPreTest & testPreTest can see it
  protected int ruleNum; //current rule number, so catPreTest & testPreTest can trace it
  protected int action; //current rule action, so catPreTest & testPreTest can trace it
  protected boolean matched = true; //working variable for match method
  protected Word root = null;
  protected String rootString = null;
  protected Word apparentRoot = null;
  protected Value capcode = null;

  protected String addLeftString = "";
  protected String addRightString = "";
  protected String substitutionString = "";
  protected int killLeftNum = 0;
  protected int killRightNum = 0;
  protected int leftContextPtr = -1;
  protected int rightContextPtr = -1;
  protected boolean testval = true;
  protected int likelihood = -1;
  protected boolean phaseOneFlag = false;
  protected boolean phaseTwoFlag = false;
  protected boolean prefixPhaseFlag = false;
  protected boolean knownRoot = false;
  protected Vector catSenses = null; //new Vector();
  protected String prefixString = "";
  protected String suffixString = "";
  protected Word prefix = null;
  protected Word suffix = null;
  int depth = 0; // keeps track of depth of recursion

  protected boolean doneFlag = false; //set by doOnly and doTry

// variables used by compound rules from MorphEngFrame:

  protected MorphCompoundRule compoundRule; //current rule for catPreTest & testPreTest to see
  protected Word word1 = null;
  protected Word word2 = null;
  protected String connector = null;

// variables used to govern behavior:

  protected boolean keepMorphCacheFlag = false;

  /**
   * Method to initialize a state for reuse.
   */

  public void initialize() {
    // Called at top level before a word is analyzed
    // This is different from initializeState in MorphEngine, which is called
    // before each rule application
    if (!keepMorphCacheFlag) morphCache = new Hashtable();
    depth = 0;
    rule = null;
    ruleNum = -1;
    phaseOneFlag = false; //this needs to persist and be managed by rules
    phaseTwoFlag = false; //this needs to persist and be managed by rules
    prefixPhaseFlag = false; //this needs to persist and be managed by rules
    doneFlag = false;
    // Other state variables will be initialized by tryRules and
    // morphCompoundTest (in English) before each rule match.
  }

  protected static String className = "MorphState";

} // end of MorphState class
