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

package com.sun.labs.minion.test;

import com.sun.labs.minion.lexmorph.Lexicon;
import com.sun.labs.minion.lexmorph.MorphEngine;
import com.sun.labs.minion.lexmorph.MorphRule;
import com.sun.labs.minion.lexmorph.MorphState;
import com.sun.labs.minion.lexmorph.Morph_en;
import com.sun.labs.minion.lexmorph.Word;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import java.util.Vector;

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

/*
 * This class can be used to test the rules and behavior of a MorphEngine.
 * MorphEngine will generate a lexical entry for a word for use in natural
 * language processing applications.  This class can also be used for
 * dictionary maintenance, such as loading a compressed binary lexicon,
 * loading an additional taxonomy, and dumping the result as a compressed
 * binary lexicon (:restorepacked x, then :loadtax y, then :dump z).
 */

public class LexMorphTest {

  // subsumption types:

  public static final int IKO = 0;  // IKO = is kind of
  public static final int IIO = 1;  // IIO = is instance of
  public static final int ENTAILS = 2;   // implies (possibly virtual) presence
  public static final int VARIANTOF = 3; // is variant of

  public static MorphEngine me;
  public static Lexicon dict;

  public static int loadTaxonomy (String filename) {
    BufferedReader inBfRdr = null;
    FileReader fRdr = null;
    String input, concepts;
    StringTokenizer tokens;
    Word concept;
    Vector parents = null;
    Word[] parentWords = null;
    int subsumptionType = -1;
    boolean childrenFlag = false;
    int count = 0;
    try {
        fRdr = new FileReader(filename);
        inBfRdr = new BufferedReader(fRdr);
    }
    catch (FileNotFoundException e) {
        System.out.println("taxonomy file not found: " + filename);
        return count;
    }
    try {
      while (true) {
        input = inBfRdr.readLine(); //try using readUTF
        if (input != null) {
          if (input.length() > 0) {
            try {

              // First look for a line specifying a subsumption type and
              // a list of parents

              if (input.startsWith(":iko ")) {
                concepts = input.substring(5);
                parents = new Vector();
                subsumptionType = IKO;
                childrenFlag = false;
              }
              else if (input.startsWith(":iio ")) {
                concepts = input.substring(5);
                parents = new Vector();
                subsumptionType = IIO;
                childrenFlag = false;
              }
              else if (input.startsWith(":entails ")) {
                concepts = input.substring(9);
                parents = new Vector();
                subsumptionType = ENTAILS;
                childrenFlag = false;
              }
              else if (input.startsWith(":variantof ")) {
                concepts = input.substring(11);
                parents = new Vector();
                subsumptionType = VARIANTOF;
                childrenFlag = false;
              }
              else if (subsumptionType < 0) {
                // we don't have a governing subsumption type
                // assume that it's IKO and this line a the list of parents
                System.out.println("*** warning: no specified type for: " +
                                   input + "\t assuming they're IKO parents");
                concepts = input.substring(5);
                parents = new Vector();
                subsumptionType = IKO;
                childrenFlag = false;
              }
              else {

                // A subsumption-type line is followed by any number of
                // lines specifying children of the specified parents.

                concepts = input; // a line of children concepts
                parentWords = new Word[parents.size()];
                parents.toArray(parentWords);
                childrenFlag = true;
              }

              // same tokenizing loop is used for parents and children

              tokens = new StringTokenizer(concepts, ", \t");
              while (tokens.hasMoreTokens()) {
                String wordString = tokens.nextToken().toLowerCase();
                concept = dict.getWord(wordString);
                if (concept == null) {
                  // This will be a new term to be added to the lexicon.
                  concept = me.morph(wordString);
                  count++; // count new terms
                }

                if (childrenFlag) {

                  // we're tokenizing children for specified parents

//System.out.println("child: " + concept); // debugging printout

                  switch (subsumptionType) {

                    case IKO  : {
                      concept.addIkoParents(parentWords);
                    }
                    break;

                    case IIO  : {
                      concept.addIioParents(parentWords);
                    }
                    break;

                    case ENTAILS  : {
                      for (int i = 0 ; i < parentWords.length ; i++) {
                        Word parent = parentWords[i];
                        concept.addEntails(parent);
                      }
                    }
                    break;

                    case VARIANTOF  : {
                      concept.addVariantOf(parentWords);
                    }
                    break;
                  }
                }
                else {
                  // we're tokenizing parents, not children

//System.out.println("parent: " + concept); // debugging printout

                  parents.add(concept);
                }
              }
            }
            catch (OutOfMemoryError e) {
              System.out.println(" *** Out of memory error in loadTaxonomy: " +
                                 input);
              return count;
            }
          }
        }
        else break;
      }
    }

    catch (IOException e) {return count;}

    if (inBfRdr != null)
        try { inBfRdr.close();
        }catch (IOException e) {
            inBfRdr = null;
        }
    if (fRdr != null)
        try { fRdr.close();
        }catch (IOException e) {
            fRdr = null;
        }

    return count;
  }

    public static void main(String[] args) {
        try {
            // BufferedReader is needed for readLine() method
            BufferedReader sysin =
                // but we'll make it a degenerate buffer of 1
                // to workaround a problem with some PC implementations
                // which wouldn't start to read before a lot of chars
                // were typed
                new BufferedReader(new InputStreamReader(System.in), 1);
            BufferedReader in = sysin;
            String argument = null; // variables for argument of command line input
            Lexicon testlex = new Lexicon (350000, 150000);
            testlex.setTestCategories("adj adj-post adv adv-qualifier city "+
              " det firstname lastname month n name nameprefix namesuffix "+
              " nc nco nm nn npl npr nsg nsp number postdet prep prespart "+
              " pro statecode title v vi vt weekday "+
              " 1sg/2sg/3sg/comparative/npl/past/pastpart/prespart/superlative/not13sg "+
              " adj/adv/aux/conj/det/interj/n/number/prep/v  adj/adv/det/n "+
              " adj/adv/det/nm/npl/number/ord/pro  adj/adv/n  adj/adv/n/v "+
              " adj/adv/ord  adj/n  adj/n/v  adj/number/ord  adj/ord  adv/v "+
              " city/firstname/lastname  country  det/n/number "+
              " det/nm/npl/number/pro  det/number  det/number/prep "+
              " det/number/prep/pro  det/number/v  name/nm/v  nc/unit "+
              " npl/unit  predet/pro  statecode/statename");

            Morph_en testMorph_en = new Morph_en();
            testMorph_en.initialize(testlex);
            MorphEngine morph = testMorph_en;
            me = morph;
            dict = testlex;
//            Word test = morph.analyze("-"); // warm it up
//            test = null; //don't keep this
            System.out.println("initialized");
            MorphRule testrule = null; //place to hold a rule for testing
            System.out.println("Enter word (enter empty line to quit):");
            boolean showTime = false;
            boolean freshMorphFlag = true; // forces morph on known words
	    int icount;
            int oldHighWordIndex = testlex.getHighWordIndex();
            while(true) {
              String input = in.readLine();
              if (input==null || input.equals("")) {
		  if (in != sysin) in = sysin; // switch back from file input
                else break;
              }
              else if (input.equals("?")) {// show commands
                System.out.println(
          "commands are: :tr, :dr, :nd, :nt, :en, :de, :ti, :to," +
          " :add, :analyze, :binlex," +
          " :catbits, :cats, :clear, :compare ,:dump, :file," +
          " :load, :loadcore, :loadtax, :match, :morph," +
          " :reload, :remove, :restorepacked, :save, :savemorph, :show," +
          " :size, :testrule, :q \nbare input is freshMorphed");
              }
              else if (input.equals(":tr")) {// trace rules
                System.out.println("Rule tracing: on");
                morph.traceOn();
              }
              else if (input.equals(":dr")) { // debug rules
                System.out.println("Rule debugging: on");
                morph.debugOn();
                testlex.startDebug();
              }
              else if (input.equals(":nd")) { // turn off debug
                System.out.println("Rule debugging: off");
                morph.debugOff();
                testlex.stopDebug();
              }
              else if (input.equals(":nt")) { // turn off trace
                System.out.println("Rule tracing: off");
                morph.traceOff();
                testlex.stopDebug();
              }
              else if (input.equals(":en")) { // do English
                morph = testMorph_en;
                System.out.println("switching to English morphology");
              }
              else if (input.startsWith(":binlex ")) { // load binlex index
                argument = input.substring(8);
                System.out.println("restoring bin lex Index for " + argument +
                                   ".bidx");
                icount = testlex.loadLexIndex(argument);
                if (icount > 0)
                    System.out.println("restored index of " + icount +
                                       " objects");
                else
                  System.out.println("restoring index failed");
                // argument = argument + ".blex";
                System.out.println("opening bin lex random access file " +
                                   argument);
                testlex.openBinLexRAF(argument);
              }
              else  if (input.startsWith(":catbits")) {
                System.out.println("assigning category bits");
                testlex.setCategoryBits();
              }
              else if (input.startsWith(":restorepacked ")) {
                argument = input.substring(15);
                System.out.println("restoring bin lex Index for " + argument +
                                   ".bidx");
                icount = testlex.loadLexIndex(argument);
                if (icount > 0){
                    System.out.println("restored index of " +
                                       icount + " objects");
                    oldHighWordIndex = testlex.getHighWordIndex();
                }
                else{
                    System.out.println("restoring index failed"); 
                }
                System.out.println("reloading packed forms of all words");
                int oldLoadedWE = testlex.reloadedWordEntries;
                int oldLoadedBB = testlex.reloadedBitBuffers;
                testlex.loadPackedLex(argument);
                int bbcount = testlex.reloadedBitBuffers - oldLoadedBB;
                icount = testlex.reloadedWordEntries - oldLoadedWE;
                System.out.println(
                    "reloaded " + bbcount +
                    " binary packed word entries and unpacked " +
                    icount);
                oldHighWordIndex = testlex.getHighWordIndex();
              }
              else if (input.startsWith(":loadcore ")) { // load core binlex
                argument = input.substring(10);
                System.out.println("reloading core words");
                int oldLoadedWE = testlex.reloadedWordEntries;
                testlex.loadCoreLex(argument);
                icount = testlex.reloadedWordEntries - oldLoadedWE;
                System.out.println(
                    "reloaded " + icount + " core word entries");
                oldHighWordIndex = testlex.getHighWordIndex();
              }
              else if (input.startsWith(":load ")) {
                argument = input.substring(6);
                System.out.println("Loading file: " + argument);
                testlex.loadFile(argument);
                System.out.println("done");
              }
              else if (input.startsWith(":loadtax ")) { // load taxonomy
                argument = input.substring(9);
                System.out.println("loading taxonomy " + input);
                icount = loadTaxonomy(argument);
                System.out.println(
                    "loading produced " + icount + " new word entries");
              }
              else if (input.equals(":cats")) { // debug categories
                // prints all categories
                testlex.showCategories();
              }
              else if (input.equals(":current")) {// what's current morphology
                System.out.println("current morphology is: " + morph);
              }
              else if (input.startsWith(":dump ")) {
                argument = input.substring(6);
                System.out.println("dumping bin lex in " + argument);
                icount = testlex.dumpLex(argument);
                System.out.println("Wrote " + icount + " objects");
                System.out.println("dumping bin lex Index for " + argument);
                icount = testlex.dumpLexIndex(argument);
                if (icount > 0)
                  System.out.println("Wrote index of " + icount + " objects");
                else
                  System.out.println("writing index failed");
              }
              else if (input.startsWith(":file ")){
                // selecting new input source
                argument = input.substring(6);
                System.out.println("reading from file " + argument);
                FileReader fRdr = null;
                try {
                    fRdr = new FileReader(argument);
                    in = new BufferedReader(fRdr);
                }
                catch (java.io.FileNotFoundException e) {
                    System.out.println("input file not found: " + argument);
                }
              }
              else if (input.startsWith(":show ")) {
                argument = input.substring(6);
                Word testword = testlex.getWord(argument);
                if (testword != null)
                  System.out.println(argument + ".printEntryString() = " +
                                     testword.printEntryString());
                else System.out.println(argument + " not found.");
              }
              else if (input.startsWith(":add ")) {
                argument = input.substring(5);
                System.out.println("Adding entry: " + argument);
                testlex.makeEntry(argument);
                System.out.println("done");
              }
              else if (input.startsWith(":printatoms")) {
                testlex.printAtoms();
              }
              else if (input.startsWith(":reload")) {
                System.out.println("reloading deferred entries");
                testlex.reloadDeferredEntries();
              }
              else if (input.startsWith(":remove ")) {
                argument = input.substring(8);
                Word temp = testlex.getWord(argument);
                if (temp != null) {
                  System.out.println("Removing: " + argument);
                  temp.removeWord();
                  System.out.println("done");
                }
                else System.out.println(argument + " not in lexicon");
              }
              else if (input.startsWith(":save ")) {
                argument = input.substring(6);
                System.out.println("Saving entries to: " + argument);
                testlex.saveFile(argument);
                System.out.println("done");
              }
              else if (input.startsWith(":savemorph ")) {
                argument = input.substring(11).trim();
                System.out.println("Saving morphed entries (index > " +
                                   oldHighWordIndex + " to: " + argument);
                testlex.saveMorphWords(oldHighWordIndex, argument);
                System.out.println("done");
              }
              else if (input.equals(":clear")) {
                System.out.println("Clearing lexicon");
                testlex.clear();
              }
              else if (input.startsWith(":size ")) {
                argument = input.substring(6);
                System.out.println("Setting lexicon size to: " + argument);
                try {
                  testlex.setSize((int)new Integer(argument).intValue());
                }
                catch (Exception e) {
                  System.out.println("Setting lexicon size failed");
                }
              }
              else if (input.startsWith(":compare ")) {
                argument = input.substring(9);
                StringTokenizer tokens = new StringTokenizer(argument, " ");
                String wordString1, wordString2;
                if (tokens.hasMoreTokens() &&
                    ((wordString1=tokens.nextToken()) != null) &&
                    tokens.hasMoreTokens() &&
                    ((wordString2=tokens.nextToken()) != null)) {
//                   WordToken wt1 = testlex.makeWordToken(wordString1);
//                   WordToken wt2 = testlex.makeWordToken(wordString2);
//                   if (testlex.compare(wt1,wt2) < 1)
//                   //if (morph.alphorder(wordString1,wordString2) < 1)
//                     System.out.println(argument + " are in alphorder");
//                   else System.out.println(argument + " are out of order");
                  }
                else System.out.println(argument +
                                        " should be two word strings.");
                }
              else if (input.startsWith(":comparew ")) {
                argument = input.substring(10);
                StringTokenizer tokens = new StringTokenizer(argument, " ");
                Word word1, word2;
                String wordString1, wordString2;
                if (tokens.hasMoreTokens() &&
                    ((wordString1=tokens.nextToken()) != null) &&
                    ((word1=testlex.makeWord(wordString1)) != null) &&
                    tokens.hasMoreTokens() &&
                    ((wordString2=tokens.nextToken()) != null) &&
                    ((word2=testlex.makeWord(wordString2)) != null)) {
                  if (morph.alphorder(word1,word2) < 1)
                    System.out.println(argument + " are in alphorder");
                  else System.out.println(argument + " are out of order");
                  }
                else System.out.println(argument +
                                        " should be two word strings.");
                }
              else if (input.startsWith(":debug ")) {
                argument = input.substring(7);
                if (argument.equals("on")) {
                  System.out.println("Lexicon debugging: on");
                  testlex.startDebug();
                }
                else {
                  System.out.println("Lexicon debugging: off");
                  testlex.stopDebug();
                }
              }
              else if (input.startsWith(":testrule ")) {
                argument = input.substring(10);
                testrule = testMorph_en.r(argument, ":testrule");
                System.out.println("testrule = "+ testrule);
              }
              else if (input.startsWith(":match ")) {
                argument = input.substring(7);
                if (testrule != null) {
                  MorphState newState =
                             new MorphState(0, null, morph, testrule, argument);
/*
// The above MorphState constructor replaces these variable assignments
                  // newState.initializeState();
                  newState.frame = morph;
                  newState.rule = testrule;
                  newState.catSenses = new Vector();
                  newState.morphCache = new Hashtable();
                  newState.lexString = argument;
                  newState.lex = morph.getMorphDict().getWord(argument);
*/
                  Vector results =
                    morph.match(argument, testrule, newState, 1, 0);
                  if (results != null) {
                    System.out.println("for testrule, match(" + argument +
                                       ", 1, 0,) = " + results);
                  }
                  else {
                    System.out.println(argument + " no results.");
                  }
                }
                else {
                  System.out.println("error: first define a testrule, using" +
                                     " :testrule, before calling :match");
                }
              }
              else if (input.startsWith(":morph ")) {
                argument = input.substring(7);
                long start = System.currentTimeMillis();
                Word result = morph.morph(argument);
                long end = System.currentTimeMillis();
                if (showTime && (args.length < 1 || !args[0].equals("-t")))
                  System.out.println((end-start) + " msec");
                if (result!=null) {
                  System.out.println(argument + " > " + result.toString());
                  System.out.println("    " + argument + 
                                     ".printEntryString() = " +
                                     result.printEntryString());
                }
                else System.out.println(argument + " > no result");
              }
              else if (input.startsWith(":analyze ")) {
                argument = input.substring(9);
                long start = System.currentTimeMillis();
                Word result = morph.analyze(argument);
                long end = System.currentTimeMillis();
                if (showTime && (args.length < 1 || !args[0].equals("-t")))
                  System.out.println((end-start) + " msec");
                if (result!=null) {
                  System.out.println(argument + " > " + result.toString());
                  System.out.println("    " + argument + 
                                     ".printEntryString() = " +
                                     result.printEntryString());
                }
                else System.out.println(argument + " > no result");
              }
              else if (input.equals(":ti")) {// time
                showTime = true;
              }
              else if (input.equals(":to")) {// time off
                showTime = false;
              }
              else if (input.equals(":q")) {// quit
                break;
              }
              else if (input.startsWith(":")) {// unrecognized command
                System.out.println("unrecognized command: " + input);
              }
              else {
                  if (freshMorphFlag){
		      Word temp = testlex.getWord(input);
		      if (temp != null)  temp.removeWord();
		  }

                long start = System.currentTimeMillis();
                Word result = morph.morph(input);
                long end = System.currentTimeMillis();
                if (showTime && (args.length < 1 || !args[0].equals("-t")))
                  System.out.println((end-start) + " msec");
                if (result!=null) {
                  System.out.println(input + " > " + result.toString());
                  System.out.println("    " + input +
                                     ".printEntryString() = " +
                                     result.printEntryString());
                }
                else System.out.println(input + " > no result");
              }
              System.out.println(" >");
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
