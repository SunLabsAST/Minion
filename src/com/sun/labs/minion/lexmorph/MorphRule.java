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
import java.util.StringTokenizer;
import java.util.Vector;
//import Lexicon;

/*
; copyright (c) by W.A. Woods.  All rights reserved.
; The following software was created in 1989, 1990, 1991, 1992, 1993, 1994,
; 1995, 1996, 1997, 1998, 1999, 2000, 2001, 2002, and 2004,
; but has not been published within the meaning of the copyright law.
; This file may be made available by the author to selected individuals
; for specific research projects, but may not to be copied, distributed, or
; used for other purposes without explicit permission from the author.

; This software is licensed "as is." The author expressly disclaims any
; warranties of any kind and accepts no liabilities resulting from its use.
*/

/**
 * This is a class of morphological rules used in MorphEngine.
 *
 * @author W. A. Woods
 * 
 * @version        1.0
 * 
 * The rules and code for this system were developed by W. A. Woods
 * 
 * @see MorphEngine
 */

  public class MorphRule {
    public String[] pattern;
    public int killnum=0;
    public int leftkillnum=0;
    public boolean leftAnchor=false;
    public boolean rightAnchor=false;
    public int[] actions;
    public String name = "unnamedRule";
    public boolean conditionalPhaseOne = false; // known root will entail phase one
    public boolean definitePhaseOne = false; // unconditionally phase one, regardless of root
    public boolean possibleSplitRule = false; // will need to create sense names and entries

    public MorphRule() {
    }

    /**
     * Create a Rule
     * @param expression A String representing a morphological rule.
     * @param blockName The name of the rule set containing this rule.
     */
    public MorphRule(String expression, String blockName, Hashtable varTable) {

        name = blockName+": "+expression;
        //set up pattern array:
        String actionString = null;
        String consonantChars = "bcdfghjklmnpqrstvwxyz???"; //should ? be a consonant?
        String vowelChars = "aeiou??????????????????????????";
        String anyChars = "abcdefghijklmnopqrstuvwxyz?????????????????????????????-'/.:,";
        if (expression.length() > 0) {
            boolean passedPlus = false;
            int count = 0;
            String charset = "";
            String lastCharset = "";
            if (expression.startsWith("#") && " \t\n\r".indexOf(expression.charAt(1)) >=0 ) {
                    leftkillnum = count;
                    leftAnchor=true;
            }
            Vector patternBuffer = new Vector(expression.length());
            StringTokenizer temp = new StringTokenizer(expression, " \t\n\r");
            while (temp.hasMoreTokens()) {
                lastCharset = charset;
                charset = temp.nextToken();
                if (charset.equals("->")) {
                    if (lastCharset.equals("#"))
                      rightAnchor=true;
                    else if (lastCharset.startsWith("+") && lastCharset.length() > 1 &&
                              !passedPlus)
                      System.out.println("*** WARNING *** " +
                                         "final element in an unanchored pattern starts with + in: " + 
                                         "\n    " + expression +
                                         "\n    " + "(suggestion: remove the + from this element; don't need to count repeats)");
                    else if (lastCharset.startsWith("*") && lastCharset.length() > 1 &&
                              !passedPlus)
                      System.out.println("*** WARNING *** " +
                                         "final element in an unanchored pattern starts with * in: " + 
                                         "\n    " + expression +
                                         "\n    " + "(suggestion: remove this unnecessary optional element from the pattern)");
                    else if (lastCharset.startsWith("?") && lastCharset.length() > 1 &&
                              !passedPlus)
                      System.out.println("*** WARNING *** " +
                                         "final element in an unanchored pattern starts with ? in: " + 
                                         "\n    " + expression +
                                         "\n    " + "(suggestion: remove this unnecessary optional element from the pattern)");
                    if (temp.hasMoreTokens())
                      actionString = temp.nextToken();
                    else actionString = "";
                    if (temp.hasMoreTokens())
                      System.out.println("*** ERROR *** " +
                                         "extra final element(s) in: " + 
                                         "\n    " + expression);
                    break;
                }
                else if (!passedPlus) {
                  //skip rest of block unless passedPlus is true
                }
                else if (charset.equals("#")) {
                  //don't count these for killnum
                }
                else if (charset.equals("+")) {
                  //don't count these for killnum
                }
                else if (charset.equals("-")) {
                  //don't count these for killnum
                }
                else if (charset.equals("<")) {
                  //don't count these for killnum
                }
                else if (charset.equals(">")) {
                  //don't count these for killnum
                }
                else if (charset.startsWith("+") || charset.startsWith("*")) {
                  if (passedPlus)
                    System.out.println("*** ERROR *** repeatable item to right of plus in: " + 
                                       name);
                }
                else if (passedPlus==true) {// count number of characters after +
                  killnum++;
                }

                if (charset.startsWith(".")) { //need to go on if . case to check for .$
                  if (passedPlus)
                    System.out.println("*** ERROR *** dot to right of plus in: " + 
                                       name);
                }

                if (charset.equals("+")) { //not else if, because of . case to get .$
                    passedPlus = true;
                    rightAnchor=true;
                }
                else if (charset.equals("#")) {
                  //ignore #'s here -- handled separately at beginning and end
                }
/*
                else if (charset.equals("-")) { 
                    passedMinus = true;
                    leftkillnum = count;
                    leftAnchor=true;
                    if (passedDot)
                      System.out.println("*** ERROR *** dot to left of minus in: "
                                       + name);
                    if (passedPlus)
                      System.out.println("*** ERROR *** plus to left of minus in: "
                                       + name);
                }
*/
                else if (charset.equals("consonant") || charset.equals("$consonant")) {
                  count++;
                  patternBuffer.addElement(consonantChars);
                }
                else if (charset.equals("vowel") || charset.equals("$vowel")) {
                  count++;
                  patternBuffer.addElement(vowelChars);
                }
                else if (charset.equals("any") || charset.equals("$any")) {
                  String val = anyChars;
                  count++;
                  patternBuffer.addElement(val);
                }
                else if (charset.startsWith("$")) {
                  String val = (String)(varTable.get(charset));
                  //note: varTable has to be set before rule definitions
                  //I planned to cancel the above because it can't be done language-independently
                  //but then consonantChars are also language dependent, so just need to make
                  //sure that the varTable is set before the rules are constructed.
                  //String val = null; //(String)varTable.get(charset);
                  if (val == null) {
                    System.out.println("*** ERROR *** no val for " + charset);
                    val = anyChars;
                  }
                  count++;
                  patternBuffer.addElement(val);
                }
                else if (charset.startsWith("$", 1)) {
                  String val = (String)(varTable.get(charset.substring(1)));
                  //String val = null; //(String)varTable.get(charset.substring(1));
                  if (val == null) {
                    System.out.println("*** ERROR *** no val for " + charset);
                    val = anyChars;
                  }
                  count++;
                  if (".?*+|".indexOf(charset.charAt(0))>=0)
                    patternBuffer.addElement(charset.substring(0,1)+val);
                  else {
                    System.out.println("*** ERROR *** illegal operator " +
                                       charset.charAt(0) + " in " + charset);
                    patternBuffer.addElement(""); //nothing will match
                  }
                }
                else {
                  count++;
                  patternBuffer.addElement(charset);
                }
            }
            pattern = new String[patternBuffer.size()];
            patternBuffer.copyInto(pattern);
        }
        else pattern =  new String[0];

        //set up actions array:
        actions = MorphEngine.makeActions(actionString);
    }

    @Override
    public synchronized String toString() {
      return name;
    }
  }
