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

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;

import com.sun.labs.minion.knowledge.KnowledgeSource;
import com.sun.labs.minion.util.CharUtils;
import java.util.logging.Logger;

/**
 * This class will generate an array of morphological variants of a word
 * to use in search-and-retrieval applications where a user wants to find 
 * other words morphologically related to words in a query.  For example, 
 * a request such as "electrical fixtures" should also retrieve "electric
 * fixtures," "electrical fixture," etc.  Given a word of a query, these 
 * rules generate alternative variations of that word that should also be 
 * considered.  This generation of variant forms of a word fills a role 
 * similar to that often filled by the use of wild card characters or by 
 * stemming rules that produce truncated stems in traditional information
 * retrieval systems.  The approach of generating alternative variations
 * has advantages over a truncated stemming approach for many applications,
 * because it does not require stemming operations during the indexing
 * process, does not require extra indexing space for stems, nor does it
 * lose information by storing only stems.  Rather, the variation rules
 * are applied to the query to produce additional forms to check against
 * the index.
 * <p>
 * Compared to the use of wild card characters, this approach has two 
 * advantages: first, it does not require the user to think about where 
 * the wild cards should be placed, and secondly, it deals with irregular
 * variations for irregular verbs (e.g., "break," "broke," "broken"),
 * and with stem ending effects such as silent e's and doubling of final 
 * consonants (e.g., "dig," "digs," "digging").  Properly constructed 
 * morphological rules, together with a table of exceptions, can deal with 
 * a wide range of such effects, without requiring any more attention on 
 * the part of a user than to turn on the facility.
 * <p>
 * The rules treat almost all words as if they were multiply meaningful
 * as nouns, verbs, and adjectives.  Hence, the rules will often generate
 * spurious forms that should never occur in a text -- e.g., fix ->
 * fixest, happy -> happied.  Such rules are suitable for applications
 * such as searching text using inverted files, where a quick test
 * suffices to determine that a given candidate does not occur in the
 * corpus.  In such applications, it is preferable to overgenerate
 * candidates than to miss possible retrievals.
 * <p>
 * The program uses rules developed by W. A. Woods and Ellen Hays in 1992.
 * An original C program for using them was developed by Jacek Ambroziak
 * and was included in Sun's SearchIt product.
 *
 * For more information, see the documentation in LiteMorphRule.java.
 *
 * @author Roger D. Brinkley
 * @author W. A. Woods
 * @author Jacek Ambroziak
 * @version	1.2	07/23/2001
 * 
 * @see LiteMorphRule
 */
public abstract class LiteMorph implements KnowledgeSource {

    protected Hashtable rulesTable;

    protected Hashtable exceptions;

    protected static LiteMorph localizedMorph; //localized subclass instance

    public static int maxDepth = 10; // maximum depth for recursive calls

    public static PrintWriter logfile = null; //file to log record of used rules

    /**
     * The log.
     */
    static Logger logger = Logger.getLogger(LiteMorph.class.getName());

    /**
     * The tag for this module.
     */
    protected static String logTag = "LiteMorph";

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
     * is false so that the run-time class files will be smaller.  For a similar
     * reason, in the debug method below, the static final debugFlag is
     * temporarily commented out in favor of a public nonfinal settable version
     * for use when developing rules.  For a run-time version, the preceding
     * public nonfinal version should be commented out and the static final
     * version should be uncommented back in by removing the //temporary//.
     * Likewise, for a run-time version, the //temporary// comment in front
     * of the package statement at the beginning of this file and in the other
     * LiteMorphRule.java and LiteMorph_de.java, etc. files should be removed.  This
     * package statement is commented out during rule development so that the
     * LiteMorph capability can be run standalone under the control of the
     * LiteMorphTest class, which provides an environment for use by a rule
     * developer to test the operation of the rules.
     *
     * A typical development session looks like:
     *
     *  endor2 344 =>javac LiteMorphTest.java
     *  endor2 345 =>java LiteMorphTest
     * initialized
     * Enter word (enter :q or empty line to quit):
     * gesundheit
     * gesundheit > gesundheiten
     *  >
     * :q
     *
     * See the LiteMorphRule.java file for more information.
     */
    public static final boolean authorFlag = false; //false;

    public LiteMorph() {
        if(rulesTable == null) {
            intialize();
            if(authorFlag) //check definitions after rules are initialized
            {
                checkDefinitions();
            }
        }
    }

    public static LiteMorph getMorph() {
        return null;
    }

    /**
     * Subclasses of this class (generally locale-specific) need
     * to set up exceptions and rules.  At a minium, implementations
     * need to define an initialize method to set the localizedMorph
     * variable to an instance of the localized Morph class, establish
     * the rules HashTable, and initialize the size and contents of
     * the exceptions HashTable.
     *
     * For languages with more complex morphology than English, such
     * as German, it may be necessary for the localization class to
     * define a specialization of the computeMorph method in order
     * to handle things that are difficult to do in the rule format.
     * If so, they must also instantiate a computeMorphArgs method
     * to return a list of legal values for the arg argument to
     * computeMorph.  See LiteMorph_de.java for an example.
     */
    protected abstract void intialize();

    /**
     * Method for initializing exceptions Hashtable using an array of
     * Strings. Each String is a list of variation groups.  The words in
     * the groups are space delimited. Any matching word in the exceptions
     * will cause all of the words in the group to be added to the variant
     * list.  This method supports fully specified variation groups, and
     * also "compressed" groups that replace some of the variations with
     * numbers indicating the repetition of a word that has already
     * occurred.
     */
    protected void initialize(Hashtable hashTable, String[] formsTable) {

        // Firewall
        if(hashTable == null || formsTable == null) {
            return;
        }
        String tempWord, tempVal;
        String compWord = null;
        //compword word is used as a base word for decompressing (starts null)
        for(int i = 0; i < formsTable.length; i++) {
            String entry = uncompress(formsTable[i], compWord);
            StringTokenizer tokens = new StringTokenizer(entry, " ");
            compWord = null; //reset to null for testing in the while loop
            while(tokens.hasMoreTokens()) {
                tempWord = tokens.nextToken();
                if(compWord == null) {
                    compWord = tempWord; //pick up first tempWord to use
                }                                       //for compWord of next entry
                tempVal = (String) hashTable.get(tempWord);
                if(tempVal == null) {
                    hashTable.put(tempWord, entry);
                } else {
                    hashTable.put(tempWord, tempVal + " " + entry);
//note: the same form can occur in several groups that must be appended
//That's the way you want it for the exceptions table.  It's not clear
//if that's what is wanted for e.g., the German strong verbs table.
                }
            }
        }
    }

    public String uncompress(String entry, String compWord) {
        debug("uncompressing " + entry + " with compWord = " + compWord);
        String tempWord;
        String result = null;
        boolean firstWord = true;
        int compNum = -1; // will be number of characters to copy from compWord
        StringTokenizer tokens = new StringTokenizer(entry, " ");
        while(tokens.hasMoreTokens()) {
            tempWord = tokens.nextToken();
            debug("uncompressing " + tempWord + " with compWord = " + compWord);
            if(compWord == null) {
                compWord = tempWord;
            } else if(tempWord.length() > 0) {
                compNum = "0123456789".indexOf(tempWord.charAt(0));
            }
            if(compNum > compWord.length()) {
                logger.severe("uncompressing " + tempWord +
                        " with too short compWord = " + compWord);
            } else if(compNum > -1) {
                tempWord = compWord.substring(0, compNum) +
                        tempWord.substring(1);
            }
            if(result == null) {
                result = tempWord;
            } else {
                result = result + " " + tempWord;
            }
            if(firstWord) {
                compWord = tempWord; //use this for compWord for rest of entry
                firstWord = false;
            }
        }
        return result;
    }

    /**
     * Get the variants of given word. This is locale specific.
     * Variants of a word are supplied by the locale-specific
     * implementation of this class.
     * 
     * @see com.sun.labs.minion.knowledge.KnowledgeSource#variantsOf(java.lang.String)
     */
    public Set<String> variantsOf(String word) {

        // Go get the morphological variants of the word.
        Set<String> variants = new HashSet<String>();

        //
        // We'll do morph in lower case, because the tests all assume lowercase.
        String lcWord = CharUtils.toLowerCase(word);

        // if a word is found among exceptions, don't try rules
        String exceptionList = (String) exceptions.get(lcWord);
        if(exceptionList == null) {
            exceptionList = "";
        }
        if(exceptionList.length() > 0) {
            StringTokenizer tokens = new StringTokenizer(exceptionList, " ");
            while(tokens.hasMoreTokens()) {
                variants.add(tokens.nextToken());
            }
            if(authorFlag) {
                debug("   " + word + ": found match in exceptions -- " +
                        exceptionList);
            }
        } else {
            morphWord(lcWord, 0, null, variants);
        }

        //
        // Rematch the case of the query term, if it was cased.
        if(!lcWord.equals(word)) {
            Set<String> casedVariants = new HashSet<String>();
            StringBuilder sb = new StringBuilder(word.length() * 2);
            for(String variant : variants) {
                sb.delete(0, sb.length());
                int i = 0;
                int j = 0;
                for(; i < word.length() && j < variant.length(); i++, j++) {
                    char c1 = word.charAt(i);
                    char c2 = lcWord.charAt(j);
                    if(c1 != c2 && CharUtils.toLowerCase(c1) == CharUtils.
                            toLowerCase(c2)) {
                        sb.append(c1);
                    } else {
                        sb.append(c2);
                    }
                }
                if(j < variant.length()) {
                    sb.append(variant.substring(j));
                }
                casedVariants.add(sb.toString());
            }
            variants = casedVariants;
        }
        variants.remove(word);

        return variants;
    }

    /**
     * Morph the word into other words, if possible.
     */
    protected void morphWord(String word, int depth, String ruleSetName,
            Set<String> variants) {
        LiteMorphRule[] rules = null;
        if(ruleSetName != null) {
            if(ruleSetName.startsWith("!")) {
                if(authorFlag) {
                    debug(" Trying computeMorph (" + ruleSetName + ") on " +
                            word +
                            " at depth " + depth);
                }
                String[] computedVariations =
                        localizedMorph.computeMorph(word, ruleSetName.substring(
                        1),
                        depth, "", "");
                if(computedVariations != null) {
                    for(int i = 0; i < computedVariations.length; i++) {
                        variants.add(computedVariations[i]);
                    }
                }
                return;
            }
            rules = (LiteMorphRule[]) rulesTable.get(ruleSetName);
            if(rules == null) {
                logger.severe("Can't find " + ruleSetName);
                return;
            }
        } else {
            ruleSetName = ":unnamed";
        }

        if(authorFlag) {
            debug(" Analyzing " + word + " with rules " + ruleSetName +
                    " at depth " + depth);
        }

        if(depth > maxDepth) {
            return;
        }

        /* ;this is now moved to variantsOf so that it is only done once
        // if a word is found among exceptions, don't try rules

        String exceptionList = (String)exceptions.get(word.toLowerCase());
        if (exceptionList == null) {
        exceptionList = "";
        }
        if (exceptionList.length() > 0) {
        StringTokenizer tokens = new StringTokenizer(exceptionList, " ");
        while (tokens.hasMoreTokens())
        variants.addElement(tokens.nextToken());
        if (authorFlag)
        debug("   "+word+": found match in exceptions -- "+
        exceptionList+", at depth "+depth);
        return;
        }
         */
        if(word.indexOf("-") >= 0) {
            return;
        }
        if(word.indexOf("|") >= 0) {
            return;
        }
        //don't apply rules to words with internal hyphens or |'s
        // (but could get matches from exceptions table if there are any)

        int skipnum = 0;

        if(rules == null) {
            // See if the word ends with one of the keys in the rulesTable
            Enumeration keys = rulesTable.keys();
            int pos;
            while(keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                if(word.endsWith(key) && !key.equals(":unnamed")) {
                    rules = (LiteMorphRule[]) rulesTable.get(key);
                    skipnum = key.length();
                    if(rules.length > 0 && (pos = rules[0].toString().indexOf(
                            "-")) >= 0) {
                        ruleSetName = rules[0].toString().substring(0, pos);
                    }
                    break;
                }
            }
        }
        if(rules == null) {
            // no match; try to get the unnamed "default" rules.
            rules = (LiteMorphRule[]) rulesTable.get(":unnamed");
            skipnum = 0;
            int pos;
            if(rules.length > 0 && (pos = rules[0].toString().indexOf("-")) >= 0) {
                ruleSetName = rules[0].toString().substring(0, pos);
            }
        }

        for(int n = 0; n < rules.length; n++) {
            if(authorFlag) {
                debug("  " + word + ": trying rule " + ruleSetName + "-" + (1 +
                        n) +
                        ":\n    " + rules[n] + ", at depth " + depth);
            }
            Vector results = rules[n].match(word, depth, skipnum);
            if(results != null) { // rule n matched
                if(authorFlag) {
                    trace("  " + word + ": generated " + results.size() +
                            " results for rule " + ruleSetName + "-" + (n + 1) +
                            ", at depth " + depth + ":\n    " + rules[n]);
                    if(logfile != null) {
                        //logfile.println(rules[n]); //prints name and entire rule
                        logfile.println(ruleSetName + "-" + (1 + n)); //prints rule name
                    }
                }
                for(int j = 0; j < results.size(); j++) {
                    String variant = (String) results.elementAt(j);
                    if(authorFlag) {
                        debug("     adding variation: " + variant);
                    }
                    variants.add(variant);
                }
                break;
            }
        }
        return;
    }

    /**
     * Dummy function for computing morphological variants -- can be
     * redefined in subclasses of LiteMorph for locales that need to
     * compute something that cannot be done easily in the rule format.
     */
    protected String[] computeMorph(String stem, String arg, int depth,
            String prefix, String suffix) {
        return null;
    }

    /**
     * Dummy function for specifying legal arguments to computeMorph -- can be
     * redefined in subclasses of LiteMorph for locales that need to
     * compute something that cannot be done easily in the rule format.
     */
    protected String[] computeMorphArgs() {
        return null;
    }

    // Useful functions for use in a defined version of computeMorph:
    public boolean charIsOneOf(char character, String testString) {
        if(testString.indexOf(character) < 0) {
            return false;
        } else {
            return true;
        }
    }

    public int search(String findString, String inString) {
        int searchLength = inString.length() - findString.length();
        if(searchLength < 0) {
            return -1;
        }
        int result = -1;
        for(int n = 0; n <= searchLength; n++) {
            if(inString.startsWith(findString, n)) { // look for match at nth substring
                result = n;
                break;
            }
        }
        return result;
    }

    /**
     * Method for creating Rules
     */
    protected void defRules(String name, String[] ruleStrings) {
        LiteMorphRule[] rules = new LiteMorphRule[ruleStrings.length];
//        LiteMorph morph = getMorph();
        LiteMorph morph = this;
        if(morph == null) {
            logger.severe("No localizedMorph for " + name);
        }
        for(int i = 0; i < ruleStrings.length; i++) {
            rules[i] = new LiteMorphRule(ruleStrings[i], name + "-" + (i + 1),
                    morph);
        }
        rulesTable.put(name, rules);
    }

    /**
     * Method for creating defined variables
     */
    protected void defVar(String name, String valString) {
        rulesTable.put(name, valString);
    }

    /**
     * Method for checking calls to defined rulesets
     */
    protected void checkDefinitions() {
        if(authorFlag) { //check that all called rulesets are defined
            Enumeration keys = rulesTable.keys();
            while(keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                if(key.startsWith("$") == false) {
                    // assume key is a ruleset key if it's not a variable
                    LiteMorphRule[] rules =
                            (LiteMorphRule[]) rulesTable.get(key);
                    for(int i = 0; i < rules.length; i++) {
                        LiteMorphRule rule = rules[i];
                        String[] expansions = rule.getExpansions();
                        for(int j = 0; j < expansions.length; j++) {
                            String expansion = expansions[j];
                            int lpos, rpos;
                            String arg; //name of ruleset to be called recursively
                            if((lpos = expansion.indexOf("(")) >= 0 &&
                                    (rpos = expansion.indexOf(")")) >= 0 &&
                                    rpos > lpos &&
                                    rulesTable.get(arg = expansion.substring(
                                    lpos + 1, rpos)) == null) {
                                if(arg.startsWith("!")) {
                                    boolean foundDef = false;
                                    String testArg = arg.substring(1);
                                    String[] args = localizedMorph.
                                            computeMorphArgs();
                                    if(args != null) {
                                        for(int k = 0; k < args.length; k++) {
                                            if(testArg.equals(args[k])) {
                                                foundDef = true;
                                                break;
                                            }
                                        }
                                    }
                                    if(foundDef == false) {
                                        logger.severe("No definition for " +
                                                testArg + " in " +
                                                rule.toString());
                                    }
                                } else if(arg.startsWith(":")) {
                                    logger.severe("No definition for " + arg +
                                            " in " + rule.toString());
                                } else {
                                    logger.severe("No specified operator " +
                                            "(! or :) before " +
                                            arg + " in " + rule.toString());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void setLocalizedMorph(LiteMorph m) {
        localizedMorph = m;
    }

    public static LiteMorph getLocalizedMorph() {
        return localizedMorph;
    }
    /**
     * For printf debugging.
     */
    public static boolean debugFlag = false; //so tester can set it
//temporary//    protected static final boolean debugFlag = false;

    private static void debug(String str) {
        if(debugFlag) {
            logger.info(str);
        }
    }
    public static boolean traceFlag = false; //so tester can set it
//temporary//    protected static final boolean debugFlag = false;

    private static void trace(String str) {
        if(traceFlag) {
            logger.info(str);
        }
    }
}
