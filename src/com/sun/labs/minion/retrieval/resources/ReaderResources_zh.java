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

package com.sun.labs.minion.retrieval.resources;

import com.sun.labs.minion.document.tokenizer.Tokenizer;
import com.sun.labs.minion.document.tokenizer.UniversalTokenizer;
import java.util.ListResourceBundle;

  /**
   * French locale resources for the Commpass LocalQueryReader.
   * This is a template to be copied and edited appropriately
   * for each locale.  The positions and order of the entries
   * in localOpCodeTable and their 2nd element (their semantic
   * interpretation by the parser) should not be changed, but
   * their names (the first element in the entry, in <>'s)
   * should be translated to the local language.  E.g., for French,
   * the entry "<and>, and" should be translated to "<et>, and"
   * (the second of the pair is unchanged).  In localErrorMessages,
   * the feedback comments should be translated, but the comments
   * should be left in English for reference.
   */

public class ReaderResources_zh extends ListResourceBundle {

    @Override
  public Object[][] getContents() {
    return contents;
  }

  static final Object[][] contents = {

    /**
     * Standard query operator names.  Translate the value strings
     * (i.e., the second element in each pair).
     */
    {"localAndString", "and"},
    {"localOrString", "or"},
    {"localNotString", "not"},
    {"localNearPrefix", "<near/"},

    /**
     * LiteMorph to use, if available.  If there's no LiteMorph for
     * this locale, specify an integer zero value, i.e.:
     * {"liteMorph",  new Integer(0)},
     */
    {"liteMorph", new Integer(0)},

    /**
     * Tokenizer class to use for this locale.  If none, use:
     * {"tokenizerClass", new Integer(0)}.
     *  We just store the class, so threads can get instances.
     */
    {"tokenizerClass", (Tokenizer) new UniversalTokenizer(null)},

    /**
     * Parameter values.  Do not translate, but set appropriately
     * for the given locale.  localAsianFlag should be "true" if the
     * local language does not have spaces separating words.
     */
    {"localAsianFlag", "true"},

    /**
     * The defined syntactic operator strings, in order of opCode.
     * These are the operators to be used in the QueryReader class
     * for the operations that it supports.  The first element in
     * each entry should be translated to the localized equivalent
     * for a localized version.  Only the first element of each entry
     * (the one in angle brackets) is to be translated.  The rest of
     * each entry should remain unchanged (this is important!).  E.g.,
     * for French, the entry "<and_fr>, and" should be translated to
     * "<et>, and" (the second of the pair is unchanged).  The second
     * element which is unchanged is the name of the Java query operator
     * that is to be executed.
     */
    {"localOpCodeTable", new String[] {
      "<exact>, exact",
      "<morph>, morph",
      "<expand>, expand",
      "<wild>, wild",
      "<and>, and",
      "<or>, or",
      "<not>, not",
      "<sand>, sand",
      "<sor>, sor",
      "<case>, case",
      "<cut>, cut",
      "<weight>, weight",
      "<within>, within",
      "<passage>, passage",
      "<pand>, pand",
      "<sequence>, sequence",
      "<not-in>, not-in",
      "<not-near>, not-near",
      "<tor>, tor",
      "<phrase>, phrase",
      "<hide>, hide",
      "<count>, count",
      "<if>, if",
      "<position>, position",
      "<field>, field",
      "<contains>, contains",
      "<is>, is",
      "<begins>, begins",
      "<finishes>, finishes",
      "<starts>, starts",
      "<ends>, ends",
      "<less>, less",
      "<equals>, equal",
      "<greater>, greater",
      "<leq>, leq",
      "<geq>, geq",
      "<matches>, matches",
      "<substring>, substring",
      "<word>, exact",
      "<near>, near",
      "<plus>, plus",
      "<minus>, minus",
      "<stem>, stem",
      "<not-equal>, not-equal",

      "<wildcard>, wild",
       } },

   /**
     * The user message strings to be used for reporting syntax errors
     * to the user.  Translate the String elements of the array, but
     * leave the comments unchanged for reference.
     */
    {"localErrorMessages", new String[] {

      // 0, "null query element"
      "null query element",

      // 1, "ill-formed query element"
      "ill-formed query element",

    } },

  };

} // ReaderResources_zh
