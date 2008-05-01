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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Hashtable;

/**
 * A Rule typically matches a pattern at the right end of a word, removes
 * some characters from the right end to produce a stem, and generates a
 * list of alternative forms of the word by adding each of a specified list
 * of alternative endings to the stem.  
 * <p>
 * Each rule specifies a pattern and a list of modifications to be made to
 * the input word to produce different variant forms of the word. The rule
 * is represented as a string of pattern elements, separated by spaces,
 * followed by a right arrow (" -> "), delimited by spaces, followed by
 * a sequence of modification patterns, separated by commas, as in:
 *
 *  ".aeiouy t + a -> as,ae,um,ums,on,ons,ic,ical",
 *            //         (e.g., chordata, data, errata, sonata, toccata)
 *
 * In this case, the pattern element ".aeiouy" will match a vowel anywhere
 * to the left of a "t" that is immediately to the left of a final "a",
 * and it will remove the "a" from the end to produce a stem and then add
 * each of the endings "as", "ae", "um", ... in turn to that stem to
 * produce different variant forms of the word.  The plus sign ("+") in
 * this rule indicates that the rule is a suffix rule that is anchored
 * to the right end of the word, with the "+" indicating the position
 * where the suffix is to be removed from the word to form the stem.
 * The dot (".") operator before the "aeiouy" indicates that one of these
 * characters must be found somewhere in the word to the left of the "t".
 *
 * Pattern elements are matched right-to-left starting with the pattern
 * element just to the left of the arrow (->) in the rule, with the matching 
 * usually starting with the rightmost letter of the word being analyzed.
 *
 * The different kinds of pattern elements that can occur are: 
 * <ul>
 * <li>
 * A letter group (e.g., aeiou) will match any of the letters in the group.
 * This can also be represented with vertical bars separating the letters
 * to express disjunction (e.g., a|e|i|o|u).  A letter group can also be
 * specified by a parameter name, indicated by an initial dollar sign ("$")
 * which is defined as part of the initialization of a LiteMorph localization
 * class (e.g. "$Vowel").  Such parameters are defined by expressions of
 * the form:
 *
 *         defVar("$Vowel", "aeiouy");
 *
 * <li> 
 * A letter group prefixed with a negation sign (~) will match any letter 
 * that is not in the group.
 * <li> 
 * A letter group prefixed with a period (.) must be matched somewhere 
 * preceding the match of its subsequent pattern element.  A letter group 
 * of this type is unanchored.  (A pattern element that is to be matched at
 * a specified position is "anchored," while one that can be matched at
 * any position to the left of some specified position is "unanchored.") 
 * <li> 
 * A letter group prefixed with a question mark (?) may be matched once, or 
 * skipped, immediately preceding the match of its subsequent pattern element. 
 * A letter group of this type is anchored if the pattern element to its
 * right is anchored. 
 * <li>
 * A letter group prefixed with an asterisk (*) may be matched zero or more 
 * times immediately preceding the match of its subsequent pattern element. 
 * A letter group of this type is anchored if the pattern element to its
 * right is anchored. 
 * <li>
 * A letter group prefixed with a plus sign (+) must be matched one or more 
 * times immediately preceding the match of its subsequent pattern element. 
 * A letter group of this type is anchored if the pattern element to its
 * right is anchored. 
 * <li>
 * An isolated plus sign (+) as a pattern element, marks a point in the 
 * pattern to the right of which the matching letters will be removed
 * to form the stem.  There should be no unanchored letter groups after
 * the plus sign, and there should be at most one plus sign in the pattern
 * (otherwise only the leftmost will count).  A plus sign also marks the
 * pattern as being anchored to the right end of a word, so that the
 * rightmost pattern element can only match the righmost character of
 * the input word.  In particular, a plus sign as the rightmost pattern
 * element marks the right end as the anchor point, although a # (see next)
 * is stylistically preferable in this case, and you may choose to put
 * an explicit # at the end of any suffix rule to emphasize the right anchor.
 * (A # sign is traditionally used by linguists to denote a word boundary.)
 * <li>
 * An isolated pound sign (#) as the first pattern element, marks the 
 * pattern as anchored at the left end of the word.  A pound sign as the
 * rightmost pattern element before the arrow, marks the pattern as anchored
 * to the right end of the word.  A pound sign as a pattern element anywhere
 * else in the pattern will be ignored.  There may be pound signs at both
 * ends of a pattern to mark the pattern as anchored at both ends. 
 * <li>
 * An ampersand (&) as a pattern element will match a letter that is the
 * same as its preceeding letter in the word.  
 * <li>
 * A left angle bracket (<) as a pattern element marks the left context
 * of a substitution rule that can replace a portion of the middle of a
 * word with each of the modifications in the right-hand part of the rule.
 * This should occur somewhere to the left of a right angle bracket that
 * marks the end of the portion to be replaced (see next).  
 * <li>
 * A right angle bracket (>) as a pattern element marks the right context
 * of a substitution rule that can replace a portion of the middle of a
 * word.  The portion of the word that matches the pattern elements between
 * a pair of angle brackets will be replaced by each of the modifications
 * in the right-hand part of the rule.  For example:
 *
 *    "< a e > +$Consonant e # -> <a>",
 *
 * will replace an "ae" by a single "a" when it occurs before a sequence
 * of one or more consonants preceding a final e.  The substitution operators
 * < and > cannot be used together with the + or - operators in the same rule.
 * </ul>
 * <p>
 * The modification entries in the right-hand side of a rule are typically
 * sequences of characters to be appended as a suffixes to the stem that
 * was determined by the pattern of the rule,  However, there are various
 * operators that can modify this behavior:
 * <ul>
 * <li>
 * An ampersand (&) as the first character of a character sequence in an
 * alternative modification indicates a repeat of the letter that ends the
 * stem. 
 * <li>
 * An under bar (_) as an alternative modification represents the empty
 * string, indicating that nothing is to be added to the stem for that
 * alternative.  
 * <li>
 * A modification beginning with an asterisk (*) indicates that the rules
 * are to be reapplied recursively to the form obtained from doing this
 * modification.
 * <li>
 * A modification beginning with a name in parentheses ((<name>)) indicates
 * that the rule set indicated by the <name> is to be applied to the form
 * obtained from doing this modification.  If the parentheses are preceeded
 * by the keyword TRY, then this rule will be considered to have failed if
 * nothing is found by the named rule set, and the next rule after this one
 * will be tried.  Otherwise, if this rule pattern matched, but the rule
 * found nothing, no further rules would be tried.  If the name in the
 * parentheses is prefixed with an exclamation point (!), then instead
 * of invoking a named rule set, the method computeMorph will be called
 * to process the modified stem, with the name following the exclamation
 * point as its argument.  This provides a universal escape mechanism that
 * can be used to provide features not otherwise available in the rules.
 * Specifically, a localization subclass of LiteMorph can redefine the 
 * method computeMorph to do whatever is necessary, and the argument 
 * provided by the name following the ! is effectively a subroutine name
 * within the computeMorph method.  For example, the computeMorph method
 * is used effectively in LiteMorph_de.java to deal with the separable and
 * inseparable prefixes in German.  If there is no name inside the parens,
 * then the unnamed rule set (:unnamed) is used -- this is equivalent to
 * the asterisk (*) modification operator.
 * <li>
 * A modification beginning with a left angle bracket (<) indicates that a
 * substitution is to be made corresponding to a < > pair in the left-hand-side
 * pattern.  The normal case is a simple <subst> pattern where the string between
 * the angle brackets is substituted for the portion of the input between
 * the < and > in the pattern.  A more complicated case allows the addition
 * of material at the ends of the input word in addition to making the
 * substitution.  The formats for such a modification are <subst>/rightadd
 * and <subst>/leftadd_rightadd, where the contents of the angle brackets are to
 * be substituted as before, and the material that follows the slash is to be
 * added to the ends, with _ (when present) indicating the position of the modified
 * input between the left and right add strings.  (If there is no _ specified, then
 * everything following the / is added to the right end.  In the first case, the
 * substitution is made and the string rightadd is added to the end of the result. 
 * In the second case, the substitution is made, leftadd is added to the beginning
 * of the result, and rightadd is added to the end of the result.
 *  E.g. (in a German morphology):
 *      "$AllConsonant < a e > u m + e ?n -> e,en,<&auml;>/_e,<&auml;>/_en"
 *          produces:
 * LiteMorph:      adding variation: Baeume
 * LiteMorph:      adding variation: Baeumen
 * LiteMorph:      adding variation: B&auml;ume
 * LiteMorph:      adding variation: B&auml;umen
 *          when applied to Baeume  
 * <li>
 * A modification beginning with a right angle bracket (>) indicates that a
 * pattern>subst substitution is to be applied in the right-hand-side of the rule,
 * after possibly adding something to the beginning or end of the input. 
 *  There are three formats for these substitution rules:
 *
 *              ">*foo>fie/_"    //substitute foo for fie after adding _
 *              "><foo>fie/_"    //in a mode determined by 2nd character:
 *              ">>foo>fie/_"    //* = every, < = leftmost, > = rightmost 
 *
 * The second character (after the initial >) is a mode character indicating
 * whether the substitution is to happen repeatedly as many times as
 * possible (*), just once at the leftmost position (<), and just once at the
 * rightmost position (>).  The material after this character, up to the >,
 * is the pattern to be searched for, the material from the > to the / is
 * the string to be substituted for it, and the material after the / indicates
 * any transformations to be applied to the input before substituting.  (If
 * this is simply _, then there are no such transformations.)  E.g.:
 *
 *      "$AllConsonant a e u m + e ?n -> e,en,>>ae>&auml;/_e,>>ae>&auml;/_en"
 *
 * produces the same result as example above for the input Baeume, but does
 * the pattern substitution action in the right-hand side, instead of using
 * < > in the left-hand side.  This type of substitution allows for repeated
 * substitutions and simple pattern>subst substitutions in the right-hand
 * side, while the previous substitution format, using < > operators in the
 * left-hand side provides more capabilities for determining exactly where a
 * substitution is to happen.
 * </ul>
 * <p>
 * Rules are grouped in blocks and labeled (often by a common final letter
 * sequence) and are ordered within each group so that after a matching
 * rule is found no further rules are to be tried (except when invoked
 * explicitly by a modification operator in an alternative modification in
 * the right-hand side of the rule).
 *
 * @author Roger D. Brinkley
 * @author William A. Woods
 * @author Jacek Ambroziak
 * @version	1.2	05/22/2001
 *
 * @see LiteMorph
 */

public class LiteMorphRule {
    private String[] rulePattern;
    private int killnum=0;
    private int leftkillnum=0;
    private boolean leftAnchor=false;
    private boolean rightAnchor=false;
    private String[] expansions;
    private LiteMorph morph;
    private String name = "unnamedRule";
    private boolean matched = true;

    /**
     * The following static final boolean variable authorFlag is a flag for
     * use by localization authors when developing morphological rules.
     * This flag will be set to false for a delivered run-time version, but
     * can be set true when a morphological rule set is being developed.
     * This flag is used to enable format checking and tracing that is important
     * during rule development, but which is unnecessary in the run-time
     * rule system, after the rule developer has used this facility to insure
     * that the rules are well-formed.  It is a static final variable so
     * that the compiler will optimize the extra code away when the variable
     * is false so that the run-time class files will be smaller.  When
     * authorFlag is false, all of the code associated with the tracing
     * mechanism will automatically be eliminated by the compiler.
     */
    public static final boolean authorFlag = false;

    public static boolean debugFlag = false;
    
    /**
     * Create a Rule
     * @param expression A String representing the ending patern described previously.
     * @param ruleName the name of the rule
     * @param morph the LiteMorph to make morphological variants
     */
    public LiteMorphRule(String expression, String ruleName, LiteMorph morph) {
        this.morph = morph;
        name = ruleName+": "+expression;
        //set up rulePattern array:
        String expansionString = null;
        if (expression.length() > 0) {
            boolean passedDot = false;
            boolean passedPlus = false;
            boolean passedMinus = false;
            boolean passedLeft = false;
            boolean passedRight = false;
            int count = 0;
            String charset = "";
            String lastCharset = "";
            //check for left anchor
            if (expression.startsWith("#") && expression.length() >= 2 &&
                " \t\n\r".indexOf(expression.charAt(1))>=0) { //first charset is #
                    leftkillnum = count;
                    leftAnchor=true;
            }
            Vector patternBuffer = new Vector(expression.length());
            StringTokenizer temp = new StringTokenizer(expression, " \t\n\r");
            while (temp.hasMoreTokens()) {
                lastCharset = charset;
                charset = temp.nextToken();
                if (charset.equals("->")) {
                    if (authorFlag && passedLeft && !passedRight)
                    System.out.println("*** ERROR *** < without > to right in:" +
                                      "\n    " +  name);
                    if (lastCharset.equals("#"))
                      rightAnchor=true;
                    else if (authorFlag && lastCharset.startsWith("+") &&
                             lastCharset.length() > 1 && !passedPlus)
                      System.out.println("*** WARNING *** " +
                        "final element in an unanchored pattern starts with + in:" + 
                        "\n    " + name +  "\n    " +
                        "(suggestion: remove the + from this element;" +
                        " don't need to count repeats)");
                    else if (authorFlag && lastCharset.startsWith("*") &&
                             lastCharset.length() > 1 && !passedPlus)
                      System.out.println("*** WARNING *** " +
                        "final element in an unanchored pattern starts with * in:" + 
                        "\n    " + name + "\n    " +
                        "(suggestion: remove this unnecessary optional element" +
                        " from the pattern)");
                    else if (authorFlag && lastCharset.startsWith("?") &&
                             lastCharset.length() > 1 && !passedPlus)
                      System.out.println("*** WARNING *** " +
                        "final element in an unanchored pattern starts with ? in:" + 
                        "\n    " + name + "\n    " +
                        "(suggestion: remove this unnecessary optional element" +
                        " from the pattern)");
                    if (temp.hasMoreTokens())
                      expansionString = temp.nextToken();
                    else {
                      expansionString = "";
                      if (authorFlag)
                        System.out.println("*** ERROR *** " +
                                           "empty right-hand side in: " + 
                                           "\n    " + name);
                    }
                    if (authorFlag && temp.hasMoreTokens())
                      System.out.println("*** ERROR *** " +
                                         "extra final element(s) in: " + 
                                         "\n    " + name);
                    break;
                }
                else if (charset.equals("<")) {
                  //don't count these for killnum
                   passedLeft = true;
                  if (authorFlag && passedRight)
                    System.out.println("*** ERROR *** > to left of < in: " + name);
                  if (authorFlag && passedPlus)
                    System.out.println("*** ERROR *** + to left of < in: " + name);
                  // if (authorFlag && passedMinus)
                  //   System.out.println("*** ERROR *** - used with < in: " + name);
                }
                else if (charset.equals(">")) {
                  //don't count these for killnum
                   passedRight = true;
                  if (authorFlag && !passedLeft)
                    System.out.println("*** ERROR *** > without < to left in: " + name);
                  if (authorFlag && passedPlus)
                    System.out.println("*** ERROR *** + to left of > in: " + name);
                  // if (authorFlag && passedMinus)
                  //   System.out.println("*** ERROR *** - used with > in: " + name);
                }
                else if (charset.equals("+")) {
                  //don't count these for killnum
                  // if (authorFlag && passedLeft)
                  //   System.out.println("*** ERROR *** + used with < in: " + name);
                  if (authorFlag && passedLeft && !passedRight)
                    System.out.println("*** ERROR *** + used after < in: " + name);
                  // if (authorFlag && passedRight)
                  //   System.out.println("*** ERROR *** + used with > in: " + name);
                  if (authorFlag && passedPlus)
                    System.out.println("*** ERROR *** more than one + in: " + name);
                }
                else if (charset.equals("-")) {
                  //don't count these for killnum
                  if (authorFlag && passedLeft)
                    System.out.println("*** ERROR *** - used after < in: " + name);
                  if (authorFlag && passedRight)
                    System.out.println("*** ERROR *** - used after > in: " + name);
                  if (authorFlag && passedMinus)
                    System.out.println("*** ERROR *** more than one - in: " + name);
                }
                else if (!passedPlus) {
                  //skip rest of block unless passedPlus is true
                }
                else if (charset.equals("#")) {
                  //don't count these for killnum
                }
                else if (charset.startsWith("*")) { // || charset.startsWith("+")) {
                  if (authorFlag && passedPlus)
                    System.out.println("*** ERROR ***" +
                                       " repeatable item to right of plus in: " + 
                                       name);
                }
                else if (passedPlus==true) {// count number of characters after +
                  killnum++;
                }

                if (charset.startsWith(".")) { //need to go on to check for possible .$
                  passedDot = true;
                  if (authorFlag && passedPlus)
                    System.out.println("*** ERROR *** dot to right of plus in: " + 
                                       name);
                }

                if (charset.equals("+")) { //not else if, because need to test .$ below
                    passedPlus = true;
                    rightAnchor=true;
                }
                else if (charset.equals("#")) {
                  //ignore #'s here -- handled separately at beginning and end
                }
                else if (charset.equals("-")) { 
                    passedMinus = true;
                    leftkillnum = count;
                    leftAnchor=true;
                    if (authorFlag && passedDot)
                      System.out.println("*** ERROR *** dot to left of minus in: "
                                       + name);
                    if (authorFlag && passedPlus)
                      System.out.println("*** ERROR *** plus to left of minus in: "
                                       + name);
                }
                else if (charset.startsWith("$")) {
                  String val = (String)morph.rulesTable.get(charset);
                  if (val == null) {
                    if (authorFlag)
                      System.out.println("*** ERROR *** no val for " + charset +
                                         " in " + name);
                    val = "abcdefghijklmnopqrstuvwxyz";
                  }
                  count++;
                  patternBuffer.addElement(val);
                }
                else if (charset.startsWith("$", 1)) { //i.e., after . or + or * or ?
                  String val = 
                         (String)morph.rulesTable.get(charset.substring(1));
                  if (authorFlag && ".+*?|~".indexOf(charset.charAt(0)) < 0)
                    System.out.println("*** ERROR *** illegal operator " +
                                       charset.charAt(0) + " in " + 
                                       charset + " in:\n    " + name);
                  if (val == null) {
                    if (authorFlag)
                      System.out.println("*** ERROR *** no val for " +
                                         charset.substring(1) + " in:" +
                                         "\n    " + name);
                    val = "abcdefghijklmnopqrstuvwxyz";
                  }
                  count++;
                  if (".+*?|~".indexOf(charset.charAt(0)) >= 0) //legal operators
                    patternBuffer.addElement(charset.substring(0,1)+val);
                  else  patternBuffer.addElement(""); //don't match anything
                }
                else {
                  count++;
                  if (charset.length()>1 && "?+.~".indexOf(charset.charAt(0))<0 &&
                      charset.indexOf("|") < 0)
                    System.out.println("*** Caution *** no |s in alternative" +
                                       " char set " + charset + " in:" +
                                       "\n    " + name);
                  patternBuffer.addElement(charset);
                }
            }
            rulePattern = new String[patternBuffer.size()];
            patternBuffer.copyInto(rulePattern);
        }
        else rulePattern =  new String[0];

        //set up expansions array:
        if (expansionString==null) {
          expansionString = "";
          if (authorFlag)
            System.out.println("*** ERROR *** " +
                               "no recognized right arrow in: " + 
                               "\n    " + name);
        }
        if (expansionString.length() > 0) {
            Vector expansionsBuffer = new Vector(expansionString.length());
            StringTokenizer temp = new StringTokenizer(expansionString,
                                                       ", \t\n\r");
            while (temp.hasMoreTokens()) {
              String expansion = temp.nextToken();
              if (authorFlag) {
                int lpos, rpos;
                if ((lpos = expansion.indexOf("(")) > 0 &&
                     ! expansion.startsWith("TRY("))
                  System.out.println("*** ERROR *** something other than TRY" +
                                     " before left paren in " + expansion + 
                                     " in:\n    " + name);
                else if (((rpos = expansion.indexOf(")")) >= 0 &&
                          (lpos < 0 || lpos > rpos)) ||
                         (rpos < 0 && lpos >= 0))
                  System.out.println("*** ERROR *** " +
                                     "unmatched paren in " + expansion + 
                                     " in:\n    " + name);
                else if ((rpos >= 0 && expansion.substring(rpos+1).indexOf(")") >=0) ||
                         (lpos >= 0 && expansion.substring(lpos+1).indexOf("(") >=0))
                  System.out.println("*** ERROR *** " +
                                     "too many parens in " + expansion + 
                                     " in:\n    " + name);
                else if (rpos >= 0 && expansion.endsWith(")"))
                  System.out.println("*** WARNING *** " +
                                     "nothing after right paren in " + expansion + 
                                     " in:\n    " + name);
                else if ((lpos = (expansion.indexOf("&"))) >= 0 &&
                         (expansion.substring(lpos+1).indexOf("&") >=0 || //more than one
                          expansion.substring(lpos+1).indexOf("<") >=0 || //left of <
                          (lpos > 0 && ")_<".indexOf(expansion.charAt(lpos-1)) < 0) //not 1st
                           ))
                  System.out.println("*** ERROR *** " +
                                     "& at improper position in " + expansion + 
                                     " in:\n    " + name);
                if ((lpos = expansion.indexOf("<")) >= 0 &&
                    (expansion.substring(lpos+1).indexOf(">") < 0 ||
                     expansion.substring(lpos+1).indexOf("<") >= 0))
                  System.out.println("*** ERROR *** " +
                                     "unmatched or extra angle bracket (< or >) in " +
                                     expansion + " in:\n    " + name);
                else if ((rpos = expansion.indexOf(">")) == 0 &&
                          expansion.length() > 3 &&
                          "><*".indexOf(expansion.charAt(1)) >= 0 &&
                          expansion.substring(rpos+2).indexOf(">") >= 0 &&
                          expansion.substring(rpos+2).indexOf("/") >
                            expansion.substring(rpos+2).indexOf(">")) {
                  //legal format for substitution operator -- i.e.:
                  // >>something>substitution/context (or same with >< or >* prefix)
                  if (expansion.indexOf("/")+1 == expansion.length())
                    System.out.println("*** WARNING *** " +
                                       "nothing to right of / in " + expansion +
                                       " in:\n    " + name);
                }
                else if (lpos > 0 && expansion.charAt(lpos-1) != ')') {
                  System.out.println("*** ERROR *** " +
                                     "something to left of < will be ignored in " +
                                     expansion + " in:\n    " + name);
                  expansion = expansion.substring(lpos);
                  if (rpos >= 0 && rpos+1 < expansion.length() &&
                       !expansion.startsWith("/", rpos+1)) {
                    System.out.println("*** ERROR *** " +
                                       "something to right of > will be ignored in " +
                                       expansion + " in:\n    " + name);
                    expansion = expansion.substring(0, rpos+1);
                  }
                }
                else if (rpos >= 0 && rpos+1 < expansion.length() &&
                         !expansion.startsWith("/", rpos+1)) {
                  System.out.println("*** ERROR *** " +
                                       "something to right of > will be ignored in " +
                                     expansion + " in:\n    " + name);
                  expansion = expansion.substring(0, rpos+1);
                }
                else if (rpos >= 0 &&
                         (lpos > rpos || lpos < 0 ||
                          expansion.substring(rpos+1).indexOf(">") >= 0 ||
                          expansion.substring(rpos+1).indexOf("<") >= 0)) {
                         //not ballanced <>'s and not (following) format for subst:
                  System.out.println("*** ERROR *** " +
                                     "unmatched or extra angle bracket (< or >) in " +
                                     expansion + " in:\n    " + name);
                }
              }
              expansionsBuffer.addElement(expansion);
            }
            expansions = new String[expansionsBuffer.size()];
            expansionsBuffer.copyInto(expansions);
        }
        else {
            expansions = new String[0];
        }
    }

    public String[] getExpansions() {
      return expansions;
    }
    
    /**
     * Determines if a word matches the rule
     */
    public Vector match(String word, int depth, int skipnum) {
        Vector words = new Vector();
        String prefix = "";
        String suffix = "";
        String leftContext = "";
        String rightContext = "";
        int killRight = killnum;
        int killLeft = leftkillnum;
        int patternLength = rulePattern.length;
        int matchCount = 0; //how many times this elt matched
        int leftContextPtr = -1; //saved left context pointer
        int rightContextPtr = -1; //saved right context pointer
        int started = 0; //flag indicating that right end is bound
        int [] state = null;
        Vector alts = new Vector();
        Hashtable doneStates = new Hashtable();
        String stateCode = null;
        
        // skipnum positions at the right end have already been
        // tested by the dispatch method
        int length = word.length();
        int position = length-1; //position in word
        int i = patternLength-1; //position in rulePattern
        //  int position = length-1-skipnum; //position in word
        //  int i = patternLength-1-skipnum; //position in rulePattern
        // problem above for patterns with > and # at the right end -- don't skip them
        // so currently turned off skipnum effect and do those matches anyway
        if (rightAnchor || (skipnum > 0)) started = 1; //adjacency constraint is on
        // the following won't work for patterns like + and * that can match several positions
        //  if (leftAnchor && !rightAnchor && (position > i))
        //    position = i; //start match at rightmost position possible
        matched = true;
        while ((matched && (i >= 0)) || (!matched && alts.size() > 0)) {
            if ((i < 0) || !matched || (position < 0)) {
              if ((position < 0) && matched && (i >= 0) &&
                  (rulePattern[i].length() > 0) &&
                  ("?*<>".indexOf(rulePattern[i].charAt(0)) >= 0)) {
                //go on and try these pattern cases, which can match in empty string
              }
              else if (alts.size() > 0) { //try an alternative match path
                state = (int[])alts.lastElement();
                alts.removeElement(state);
                if (authorFlag)
                  traceMatch("   resuming alternative: pattern at " +
                             (state[1]+1) + " (matchcount " + state[2] +
                             ") against word at " + (state[0]+1));
                position = state[0];
                i = state[1];
                matchCount = state[2]; //how many times this elt matched
                leftContextPtr = state[3]; //saved left context pointer
                rightContextPtr = state[4]; //saved right context pointer
                killLeft = state[5]; //leftkillnum
                killRight = state[6]; //(right) killnum
                started = state[7];
                matched = true;
              }
              else {
                matched = false;
                if (authorFlag) traceMatch("    no more match possibilities");
                break;
              }
            }
            if (position >= 0) {
              stateCode = "s:"+position+":"+i+":"+matchCount;
              if (doneStates.get(stateCode) == null)
                doneStates.put(stateCode, stateCode); // blocks repeating this case
              if (authorFlag)
                traceMatch("   testing pattern "+rulePattern[i]+" at "+ (i+1) +
                           " against "+ word.charAt(position) +
                           " at "+ (position+1) + " in "+word);
            }
            else
              if (authorFlag)
                traceMatch("   testing pattern "+rulePattern[i]+" at "+(i+1)+
                           " against null at left end in "+word);

            //try to match pattern element:

            //"<" set left context
            if (rulePattern[i].equals("<")) { 
                //we can assume that we're started since < has to have a matching >
                leftContextPtr = position+1;
                leftContext = word.substring(0,position+1);
                i--; //move to next pattern element
            }

            //">" set right context
            else if (rulePattern[i].equals(">")) { 
                if (started < 1 && position > 0) //we're in an unanchored pattern
                  // generate an alt to try to start further left if this position fails
                  altState(position-1, i, 0, leftContextPtr, rightContextPtr,
                           killLeft, killRight, started, alts, doneStates);
                started = 1; //unconditional in case we are starting at position 0
                rightContextPtr = position+1;
                rightContext = word.substring(position+1);
                i--; //move to next pattern element
            }

            //check left end conditions
            else if (position < 0) {
                if ((rulePattern[i].startsWith("?")) ||
                    (rulePattern[i].startsWith("*"))) {
                  i--;
                }
                else {
                  matched = false;
                 // break;
                }
            }

            //"&" match duplicate of previous letter
            else if (rulePattern[i].equals("&")) {
                if ((position > 0) &&
                    (word.charAt(position) == word.charAt(position-1))) {
                  started = 1;
                  i--; // move to next pattern element
                }
                else if (!((position > 1) && (started < 1))) {
                  matched = false;
                 // break;
                }
                position--; // and move to next word position
            }

            //"." pattern can match anywhere
            else if (rulePattern[i].startsWith(".")) { 
                if (matchCount == 0 &&
                    rulePattern[i].indexOf(word.charAt(position), 1) >= 0) {
                  // it matches here for first time, so go to next pattern element
                  started = 1; //do this first here so it gets saved in the altState
                  if (position > 0)
                    altState(position-1, i, 1, leftContextPtr, rightContextPtr,
                             killLeft, killRight, started, alts, doneStates);
                  matchCount=0; //matchCount for next pattern element
                  i--;
                  position--;
                }
                else if (i == 0 && position == 0 && matchCount == 0) {
                  matched = false;
                //  break;
                }
                else if (matchCount > 0) {
                  //have already matched, now skip any number of anything
                  if (position > 0)
                    altState(position-1, i, matchCount, leftContextPtr, rightContextPtr,
                             killLeft, killRight, started, alts, doneStates);
                  //started = 1; //should already be 1 if matchCount is > 0
                  if (i >= patternLength-killnum) killRight--;
                  else if (i < leftkillnum) killLeft--;
                  matchCount=0; //matchCount for next pattern element
                  i--;
                  position--;
                }
                else { //still looking for a match, move left one position
                  if (i >= patternLength-killnum) killRight--;
                  else if (i < leftkillnum) killLeft--;
                  position--;
                }
            }

            //"*" pattern can match any number of times (including zero)
            else if (rulePattern[i].startsWith("*")) { 
                if (rulePattern[i].indexOf(word.charAt(position), 1) >= 0) {
                  //it matches here, so save alts and go to next position
                  if (started < 1 && position > 0) //if we're in an unanchored pattern
                    //generate an alt to try to start further left if this fails
                    altState(position-1, i, 0, leftContextPtr, rightContextPtr,
                             killLeft, killRight, started, alts, doneStates);
                  //then generate an alt for the case of not counting this match
                  altState(position, i-1, 0, leftContextPtr, rightContextPtr,
                           killLeft, killRight, started, alts, doneStates);
                  //then pursue alternative looking for an additional match to the left
                  if (matchCount > 0) {
                    //adjust killnums to compensate for additional matches
                    if (i >= patternLength-killnum) killRight++;
                    else if (i < leftkillnum) killLeft++;
                  }
                  started = 1;
                  matchCount = 1; //matchCount indicates now satisfied
                  position--; // move to next position in word
                 //  i--; // and try to match this same pattern element again
                }
                else if (leftAnchor && (i == 0) && (position >= 0)) {
                  matched = false; //no chance to complete match to left end
                //  break;
                }
                else if (matchCount > 0) {
                  matchCount = 0; //matchCount for next pattern element
                  i--; //move to the next pattern element at this position in word
                }
                else if (started < 1) {
                  if (i >= patternLength-killnum) killRight++;
                  else if (i < leftkillnum) killLeft++;
                  position--;
                }
                else {
                  //go to next pattern element at this position (optionality clause)
                  //adjust killnums for failure to find a match
                  if (i >= patternLength-killnum) killRight--;
                  else if (i < leftkillnum) killLeft--;
                  matchCount = 0; //matchCount for next pattern element
                  i--; 
                }
            }

            //"+" pattern must match one or more times
            else if (rulePattern[i].startsWith("+")) { 
                if (rulePattern[i].indexOf(word.charAt(position), 1) >= 0) {
                  //it matches here, so save alts and go to next position
                  int AdjustedKillLeft = killLeft; //adjust kill nums for the alternative
                  int AdjustedKillRight = killRight;
                  if (matchCount < 1 && i >= patternLength-killnum) AdjustedKillRight--;
                  else if (matchCount < 1 && i < leftkillnum) AdjustedKillLeft--;
                  if (started < 1 && position > 0) //we're in an unanchored pattern
                    //generate an alt to try to start further to the left instead of here
                    altState(position-1, i, 0, leftContextPtr, rightContextPtr,
                             AdjustedKillLeft, AdjustedKillRight, started, alts, doneStates);
                  if (matchCount > 0) {//have found at least one matching element already
                    //so generate an alt for the case of not counting this match
                    altState(position, i-1, 0, leftContextPtr, rightContextPtr,
                             killLeft, killRight, started, alts, doneStates);
                    //and adjust killnums to account for this extra match:
                    if (i >= patternLength-killnum) killRight++;
                    else if (i < leftkillnum) killLeft++;
                  }
                  started = 1;
                  matchCount = 1; //matchCount indicates now satisfied
                  position--;
                //  i--;
                }
                else if (leftAnchor && (i == 0) && (position >= 0)) {
                  matched = false;
                //  break;
                }
                else if (matchCount > 0) { //match has been satisfied already
                  matchCount = 0; //matchCount for next pattern element
                  i--;
                }
                else if (started < 1) {
                  if (i >= patternLength-killnum) killRight++;
                  else if (i < leftkillnum) killLeft++;
                  position--;
                }
                else {
                  matched = false;
                //  break;
                } 
            }

            //"?" pattern is optional
            else if (rulePattern[i].startsWith("?")) { 
                if (rulePattern[i].indexOf(word.charAt(position), 1) >= 0) {
                  //it matches here, so save alts and go to next position
                  if (started < 1 && position > 0) //we're in an unanchored pattern
                    //generate an alt to try to start further left if this fails
                    altState(position-1, i, 0, leftContextPtr, rightContextPtr,
                             killLeft, killRight, started, alts, doneStates);
                  //then generate an alt for the case of not counting this match
                  int AdjustedKillLeft = killLeft; //adjust kill nums for the alternative
                  int AdjustedKillRight = killRight;
                  if (i >= patternLength-killnum) AdjustedKillRight--;
                  else if (i < leftkillnum) AdjustedKillLeft--;
                  altState(position, i-1, 0, leftContextPtr, rightContextPtr,
                           AdjustedKillLeft, AdjustedKillRight, started, alts, doneStates);
                  started = 1;
                  position--;
                }
                else if (started < 1) { //increment kill num for skipped elements
                  if (i >= patternLength-killnum) killRight++;
                  else if (i < leftkillnum) killLeft++;
                  position--;
                }
                else if (leftAnchor && (i == 0) && (position >= 0)) {
                  matched = false;
                //  break;
                }
                else { //increment kill num for skipped elements
                  if (i >= patternLength-killnum) killRight--;
                  else if (i < leftkillnum) killLeft--;
                } //otherwise, go to next pattern element at this position
                matchCount = 0; //matchCount for next pattern element
                i--; 
            }

            //reject match of pattern characters against input character
            else if (rulePattern[i].startsWith("~") &&
                     rulePattern[i].indexOf(word.charAt(position)) > 0) {
                // negative pattern doesn't match here
                if ((started < 1) && (position > 0)) //unanchored match
                  position--; //move position and keep looking
                else {
                  if (authorFlag) traceMatch("    match failed");
                  matched = false;
                 // break;
                }
            }
            //match pattern characters against input character
            else if (!rulePattern[i].startsWith("~") &&
                     rulePattern[i].indexOf(word.charAt(position)) < 0) {
                // positive pattern doesn't match here
                if ((started < 1) && (position > 0)) //unanchored match
                  position--; //move position and keep looking
                else {
                  if (authorFlag) traceMatch("    match failed");
                  matched = false;
                 // break;
                }
            }

            //otherwise, character matches pattern -- decide what to do next

            else if ((i == 0) && (leftAnchor == true) &&
                     (position > 0) && (started > 0)) {
                matched = false;
                if (authorFlag) traceMatch("    left anchored match failed");
               // break;
            }
            else if ((i == 0) && matched &&
                     ((leftAnchor == false) || (position <= 0))) {
                break; //match is true
            }

            else { //character matches pattern
                  //treat rightmost unanchored match as +
                  if (((started < 1) || (matchCount > 0)) && (position > 0))
                    //matchCount > 0 here means that we're working on an alt
                    //that came from a not-started state
                    altState(position-1, i, 1, leftContextPtr, rightContextPtr,
                             killLeft, killRight, started, alts, doneStates);
                  started = 1;
                  i--;
                  position--;
            }
        }
        if  (matched && position > 0 && leftAnchor) { //finished pattern but not at left
          matched = false;
          if (authorFlag) traceMatch("    left anchored match failed");
        }
        // All done with the comparing. If we've got a match then
        // build the list of words from the expansion list
        if (matched) {
                if (leftContextPtr >= 0)
                  leftContext = word.substring(killLeft, leftContextPtr);
                if (rightContextPtr >= 0)
                  rightContext = word.substring(rightContextPtr, length-killRight);
                prefix = word.substring(0, killLeft);
                suffix = word.substring(length-killRight);
                String stem = word.substring(killLeft, length-killRight);
                for (i = 0; i < expansions.length; i++) {
                  makeForm(stem, expansions[i], depth, prefix, suffix,
                           leftContext, rightContext, words);
                }
            }
        if (authorFlag) {
          if ((leftContext.length() > 0) || (rightContext.length() > 0))
            traceMatch("   finished rule: " + name + " at depth " + depth +
                       "\n        " + "     with leftContext = "+leftContext+
                       " and rightContext = "+rightContext+
                       "\n             with matched = "+matched+" and "+ 
                       words.size() + " forms generated");
          else traceMatch("   finished rule: " + name + " at depth " + depth +
                          "\n        " + "     with matched = "+matched+" and "+ 
                          words.size() + " forms generated");
        }
        if (matched)
          return words;
        else return null;
    }

    private void altState(int position, int i, int matchCount,
                           int leftContextPtr, int rightContextPtr,
                           int killLeft, int killRight, int started,
                           Vector altStates, Hashtable doneStates) {
        String stateCode = "s:"+position+":"+i+":"+matchCount;
        if ((position > -1) && (i > -1) &&
            (doneStates.get(stateCode) == null)) {
          doneStates.put(stateCode, stateCode);
          int[]  state = new int[8];
          state[0] = position;
          state[1] = i;
          state[2] = matchCount;
          state[3] = leftContextPtr;
          state[4] = rightContextPtr;
          state[5] = killLeft;
          state[6] = killRight;
          state[7] = started;
          if (authorFlag)
            traceMatch("   saving alternative: pattern at " + (state[1]+1) +
                       " (matchcount " + state[2] + ") against word at " +
                       (state[0]+1));
          altStates.addElement(state);
        }
        return;
    }

    private void makeForm(String stem, String action, int depth,
                          String prefix, String suffix,
                          String leftContext, String rightContext,
                          Vector variants) {
        LiteMorphRule[] rules = null;
        String form = stem;
        String expansion = action;
        String nextRules = null;
        String substPattern = null;
        String subst = null;
        char modeChar =  ' ';
        int pos1 = 0;
        boolean redoFlag = false;
        if (expansion.length() < 1) return;
        if (expansion.charAt(0) == '!') {
          if (authorFlag)
            trace("   calling computeMorph on " + form + ", " +
                  expansion.substring(1) + " (" + prefix + "_" + suffix + ")" +
                  " at depth " + (1 + depth));
          if (morph == null) {
            if (authorFlag) trace("no morph to use for " + form);
            return;
          }
          String[] computedVariations =
            morph.computeMorph(form, expansion.substring(1), depth,
                               prefix, suffix);
          if (computedVariations == null) //computeMorph returned null
            matched=false;
          else for (int i=0; i<computedVariations.length; i++) {
            variants.addElement(computedVariations[i]);
          }
          return;
        }
        if (expansion.startsWith("TRY")) {
          matched=false; //rule will only succeed if it generates something
          expansion = expansion.substring(3);
        }
        // the following can apply to the result of the previous
        if (expansion.startsWith("(")) { //this test can apply to result of previous
          int pos = expansion.indexOf(')');
          if (pos > 0) {
            if (authorFlag)
              traceMatch("   will analyze " + form + " with " + expansion +
                         " at depth " + (1 + depth));
            nextRules = expansion.substring(1, pos);
            expansion = expansion.substring(pos+1);
          }
        }
       else if (expansion.startsWith("*")) {
          if (authorFlag)
            traceMatch("   will redo " + form + "+" + expansion +
                       " at depth " + (1 + depth));
          nextRules = null;
          expansion = expansion.substring(1);
          redoFlag = true;
        }
        //the following can apply to the result of the previous
        if (expansion.startsWith(">") && (expansion.length() > 1)) {
            // record substPattern, subst, and substitution mode
            // will apply substitution to the resulting expanded form
            //  substitution formats:
            //  ">*foo>fie/_"    //substitute foo for fie after adding _
            //  "><foo>fie/_"    //in mode determined by 2nd character:
            //  ">>foo>fie/_"    //* = every, < = leftmost, > = rightmost 
          modeChar = expansion.charAt(1);
          pos1 = expansion.indexOf('>',2);
          int pos2 = 0;
          if (pos1 > 0) {
            substPattern = expansion.substring(2,pos1);
            pos2 = expansion.indexOf('/',pos1+1);
          }
          else pos1 = 0;
          if (pos2 > 0) {
            subst = expansion.substring(pos1+1, pos2);
            expansion = expansion.substring(pos2+1);
          }
        }
        else if (expansion.startsWith("<")) {
          expansion = expansion.substring(1);
          if (expansion.startsWith("&")) { //get last letter of left context
            expansion = expansion.substring(1);
            if (leftContext.length() > 0)
              expansion = leftContext.substring(leftContext.length()-1) +
                expansion;
            else if (((pos1 = expansion.indexOf(">/")) >= 0) &&
                     (expansion.substring(pos1+2).indexOf("_") > 0) &&
                     ((pos1 = expansion.indexOf("_", pos1+2)) > 0))
              expansion = expansion.charAt(pos1-1) + expansion;
            else if (authorFlag)
              System.out.println("*** WARNING *** ignored & because of " +
                                  "empty left context in makeForm for <&" +
                                  expansion);
          }
          if (expansion.endsWith(">")) {
            form = expansion.substring(0,expansion.length()-1);
            expansion = "";
          }
          else if ((pos1 = expansion.indexOf(">/")) >= 0) {
            form = expansion.substring(0,pos1);
            expansion = expansion.substring(pos1+2);
          }
          else { //only the above two patterns can use the context variables 
            // leftContext = "";
            // rightContext = "";
          }
          form = leftContext + form + rightContext;
        }
        int leftNum = expansion.indexOf('_');
        if (leftNum >= 0) {                           //deal with underscore in rhs
          if (leftNum > 0)                            //do any left side addition
            form = expansion.substring(0,leftNum) + form;
          expansion = expansion.substring(leftNum+1); //and remove the underscore (_)
        }
        if (expansion.startsWith("&")) {
          // double last letter of form (or left context), e.g. $Vowel $Consonant # -> &ing
          if (form.length() > 0)
            form = form + form.substring(form.length() - 1);
          else if (leftContext.length() > 0)
            form = leftContext.substring(leftContext.length() - 1);
          else if (authorFlag)
            System.out.println("*** WARNING *** ignored & because of " +
                               "empty left context in makeForm for " +
                               expansion);
          form = form + expansion.substring(1); //add right side addition
        }
        else form = form + expansion;
        if ((subst != null) && !(form.length() < substPattern.length())) {
            int length = form.length();
            int pos = 0;
            if (authorFlag)
              traceMatch(" applying substitution " + substPattern + " > " +
                         subst +  " in mode " + modeChar + " to: " + form +
                         // "+" + expansion +
                         " at depth " + depth);
            switch (modeChar) {
              case '>': //substitute at first match found right-to-left
              pos = form.lastIndexOf(substPattern);
              if (pos >= 0)
                form = form.substring(0,pos)+subst+
                       form.substring(pos+substPattern.length(),length);
              break;

              case '<': //substitute at first match found left-to-right
              pos = form.indexOf(substPattern);
              if (pos >= 0)
                form = form.substring(0,pos)+subst+
                       form.substring(pos+substPattern.length(),length);
              break;

              case '*': //repeat as many times as found left-to-right
              int i = 0;
              length = substPattern.length();
              while (i <= (form.length()-length)) {
                if (form.regionMatches(i,substPattern,0,length)) {
                  form = form.substring(0,i) + subst +
                         form.substring(i+length);
                  i = i+subst.length();
                }
                else i++;
              }
              break;
            }
        }
        //  if (leftContext.length() > 0 || rightContext.length() > 0)
        //    form = leftContext + expansion + rightContext;
        if (nextRules != null || redoFlag) {
          Set subset = new HashSet();
          morph.morphWord(form, depth+1, nextRules, subset);
          for (Iterator iter = subset.iterator(); iter.hasNext();) {
            variants.addElement(iter.next());
          }
          if (subset.size() > 0) matched = true; //for successful TRY cases
        }
        else {
          if (authorFlag)
            trace ("     generating variation: " + form);
          variants.addElement(form);
        }
    }

    public synchronized String toString() {
      return name;
    }

    /**
     * For tracing behavior of rule matching.
     */
    public static boolean traceMatchFlag = false; //so tester can set it

    /**
     * For tracing the testing of LiteMorph rules.
     */
    public static boolean traceFlag = false; //so tester can set it

    private static void traceMatch(String str) {
        if( authorFlag && traceMatchFlag ) {
            System.out.println("Rule: " + str);
        }
    }

    private static void trace(String str) {
        if( authorFlag && traceFlag ) {
            System.out.println("Rule: " + str);
        }
    }
}
  
