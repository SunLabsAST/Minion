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

import java.util.StringTokenizer;
import java.util.Vector;

/*
; copyright (c) by W.A. Woods.  All rights reserved.
; The following software was created in 1989, 1990, 1991, 1992, 1993, 1994,
; 1995, 1996, 1997, 1998, 1999, 2000, 2001, 2002, and 2003,
; but has not been published within the meaning of the copyright law.
; This file may be made available by the author to selected individuals
; for specific research projects, but may not to be copied, distributed, or
; used for other purposes without explicit permission from the author.

; This software is licensed "as is." The author expressly disclaims any
; warranties of any kind and accepts no liabilities resulting from its use.
*/

/**
 * This is a class of compound rules used in MorphEngine.
 *
 * @author W. A. Woods
 * 
 * @version        1.0
 * 
 * The rules and code for this system were developed by W. A. Woods
 * 
 * @see MorphEngine
 */

  public class MorphCompoundRule extends MorphRule {
    public Category[] categoryPattern;
    public int[] actions;
    public String name = "unnamedRule";

    public MorphCompoundRule() {
    }

    /**
     * Create a MorphCompoundRule
     * @param expression A string specifying a MorphCompoundRule.
     * @param blockName A String specifying the name of the block for tracing.
     * @param dict A Lexicon to use for making the categories to match.
     */
    public MorphCompoundRule(String expression, String blockName, Lexicon dict) {
        name = blockName+": "+expression;
        //set up categoryPattern array:
        String actionString = null;
        if (expression.length() > 0) {
            Vector patternBuffer = new Vector(expression.length());
            StringTokenizer temp = new StringTokenizer(expression, " \t\n\r");
            String charset;
            while (temp.hasMoreTokens()) {
                charset = temp.nextToken();
                if (charset.equals("->")) {
                    if (temp.hasMoreTokens())
                      actionString = temp.nextToken();
                    else actionString = "";
                    if (temp.hasMoreTokens())
                      System.out.println("*** ERROR *** " +
                                         "extra final element(s) in: " + 
                                         "\n    " + expression);
                    break;
                }
                else {
                  patternBuffer.addElement(dict.makeCategory(charset));
                }
            }
            categoryPattern = new Category[patternBuffer.size()];
            patternBuffer.copyInto(categoryPattern);
        }
        else categoryPattern =  new Category[0];

        //set up actions array:
        actions = MorphEngine.makeActions(actionString);
    }

    /**
     * Create a MorphCompoundRule from a temporary MorphRule
     * @param oldRule A MorphRule whose pattern specifies two categories as strings.
     * @param newName A String specifying the name of the converted rule for tracing.
     * @param dict A Lexicon to use for making the categories to match.
     */
    public MorphCompoundRule(MorphRule oldRule, String newName, Lexicon dict) {
      String[] oldPattern = oldRule.pattern;
      Category[] newPattern = new Category[oldPattern.length];
      for (int i = 0 ; i < oldPattern.length ; i++) {
        newPattern[i] = dict.makeCategory(oldPattern[i]);
      }
      categoryPattern = newPattern;
      actions = oldRule.actions;
      name = newName;
    }

    public synchronized String toString() {
      return name;
    }

  }

