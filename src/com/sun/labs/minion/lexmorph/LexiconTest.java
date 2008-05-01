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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.Map;
import java.util.TreeSet;

import com.sun.labs.minion.util.MinionLog;

/**
 * This class <em>ONLY</em> contains a <code>main()</code> method.
 * It was moved from the Lexicon class.
 * 
 * @author bh37721
 *
 */
public class LexiconTest {

	public static void main(String[] args){
	    Locale loc = new Locale("en", "us");
	    Locale.setDefault(loc);
	     //log.debug(logTag, 10, "Set default locale to " + loc);
	    BufferedReader in =
	      // make it a single-character buffer to force
	      // immediate reading of input characters
	      new BufferedReader(new InputStreamReader(System.in), 1);
	    String argument = null; // variables for argument of command line input
	    //Lexicon.Category testcategory;
	    Word testword;
	    Lexicon testlex = new Lexicon (100000, 1000);
	    int idx;
	    String blHistory = "";
	    /* list of categories to be optimized for fast testing: */
	
	
	    testlex.setTestCategories(Lexicon.defaultTestCategoriesString);
	
	    RandomAccessFile binLexRaf = null;
	    int icount;
	
	            //
	        // Set up the log.
	         MinionLog log = MinionLog.getLog();
	         log.setStream(System.err);
	         log.setStream(MinionLog.ERROR, System.err);
	         log.setLevel(10);
	
	         Thread.currentThread().setName("Lexicon");
	
	
	    System.out.println("Enter command (add, assign, " +
	       "catbits, clear, crushlex, disklex, droots, dump, " +
	       "findequivs, fixvars, forgetmorph, id, iroots, isformofcat, " +
	       "load, loglevel, nrparents, preserve, printatoms, purge, " +
	       "reload, reloadid, remove, restore, restoreindex, restorepacked, " +
	       "save, savemorph, show, showcat, showsense, shrinklex, " +
	       "stubs, wtparents); (enter empty line to quit):");
	    try {
	      while(true) {
	        String input = in.readLine();
	        if (input==null || input.equals(""))
	          break;
	        else {
	          long start = System.currentTimeMillis();
	          if (input.startsWith(":"))
	              input = input.substring(1);
	          if (input.startsWith("assign")) {
	            System.out.println("Assigning index numbers");
	            testlex.assignIndexNumbers();
	            System.out.println("hiCats= " + testlex.highCatIndex +
	                               "; hiAtoms = " + testlex.highAtomIndex +
	                               "; hiWords = " + testlex.highWordIndex);
	          } else  if (input.startsWith("fixvars")) {
	            argument = (input.substring(7)).trim();
	            testword = testlex.getWord(argument);
	            if (testword == null)
	                System.out.println("can't find word " + argument);
	            else{
	            System.out.println("making variant sense words for " +
	                               testword.printEntryString());
	            if (testword != null)
	                testword.makeVariantLinks();
	            }
	          } else  if (input.startsWith("catbits")) {
	              System.out.println("assigning category bits");
	              testlex.setCategoryBits();
	          }else if (input.startsWith("id ")) {
	            if (testlex.highCatIndex < 3)
	                System.out.println("Need to assign index nums first...");
	            else {
	                idx = Integer.parseInt(input.substring(3));
	                Value val = testlex.valueIndexTable[idx];
	                if (val == null)
	                    System.out.println("index " + idx + " null");
	                else System.out.println("index " + idx + " = " +
	                                        val.printString());
	            }
	          }
	          else if (input.startsWith("iroots ")) {
	              StringTokenizer tokens = new StringTokenizer(input.substring(6), " ");
	              String wordString, catString, c1Str;
	              Word w1 = null;
	              Category c1 = null;
	              if (tokens.hasMoreTokens() &&
	                  ((wordString = tokens.nextToken()) != null) )
	                  w1 = testlex.makeWord(wordString);
	              if (tokens.hasMoreTokens() &&
	                    ((catString = tokens.nextToken()) != null))
	                  c1 = testlex.makeCategory(catString);
	              if (c1 != null) c1Str = c1.printString();
	              else c1Str = "null";
	              if (w1 != null)
	                  System.out.println(w1.printString() + ".inflectionRoots(" +
	                                     c1Str + ") = " +
	                                     LexiconUtil.arrayToString(w1.getInflectionRoots(c1)));
	              else System.out.println("need a word to get its inflectionRoots");
	          }
	           else if (input.startsWith("isformofcat ")) {
	              StringTokenizer tokens = new StringTokenizer(input.substring(12), " ");
	              String wordString;
	              String catString = "<null string>";
	              Word w1 = null;
	              Category c1 = null;
	              if (tokens.hasMoreTokens() &&
	                  ((wordString = tokens.nextToken()) != null) )
	                  w1 = testlex.makeWord(wordString);
	              if (tokens.hasMoreTokens()){
	                  catString = tokens.nextToken();
	                  c1 = testlex.getCategory(catString);
	              }
	              if (c1 == null){
	                  System.out.println("couldn't get category from string "+
	                                     catString);
	              } else {
	                  if (w1 != null)
	                      System.out.println(w1.printString() +
	                                         ".isFormOfCat(" +
	                                     c1.printString() + ") = " +
	                                     (w1.isFormOfCat(c1)));
	                  else
	                      System.out.println("need a word to do isFormOfCat");
	              }
	           }
	            else if (input.startsWith("nrparents ")) {
	              StringTokenizer tokens = new StringTokenizer(input.substring(10), " ");
	              String wordString, boolString;
	              Word w1 = null;
	              boolean nonames = false;
	              if (tokens.hasMoreTokens() &&
	                  ((wordString = tokens.nextToken()) != null) )
	                  w1 = testlex.makeWord(wordString);
	              if (tokens.hasMoreTokens() &&
	                  ((boolString = tokens.nextToken()) != null) &&
	                  (boolString.startsWith("t") || boolString.startsWith("T")))
	                  nonames = true;
	              if (w1 != null)
	                  System.out.println(w1.printString() + ".nonRootParents(" +
	                      nonames + ") = " +
	                      LexiconUtil.arrayToString(w1.nonRootParents(nonames)));
	              else System.out.println("need a word to get its nonRootParents");
	          }
	          else if (input.startsWith("crushlex")) {
	              icount = testlex.crushLex();
	              System.out.println("crush lex crushed " + icount +
	                                 " word entries");
	               blHistory += ":crushedLex of  " + icount + " objects";
	          }
	          else if (input.startsWith("disklex ")) {
	              argument = input.substring(8);
	              System.out.println("opening bin lex file " + argument +".blex");
	              binLexRaf = testlex.openBinLexRAF(argument);
	          }
	          else if (input.startsWith("droots ")) {
	              StringTokenizer tokens = new StringTokenizer(input.substring(6), " ");
	              String wordString;
	              Word w1 = null;
	              if (tokens.hasMoreTokens() &&
	                  ((wordString = tokens.nextToken()) != null) )
	                  w1 = testlex.makeWord(wordString);
	              if (w1 != null)
	                  System.out.println(w1.printString() + ".directRoots() = " +
	                                     LexiconUtil.arrayToString(w1.getDirectRoots()));
	              else System.out.println("need a word to get its directRoots");
	          }
	          else if (input.startsWith("dump ")) {
	              argument = input.substring(5);
	              System.out.println("dumping bin lex in " + argument);
	              icount = testlex.dumpLex(argument);
	              System.out.println("Wrote " + icount + " objects");
	              blHistory += ":dumped " + icount + " objects";
	              System.out.println("dumping bin lex Index for " + argument);
	              icount = testlex.dumpLexIndex(argument);
	              if (icount > 0){
	                  System.out.println("Wrote index of " + icount + " objects");
	                  blHistory += " with index\n";
	              }else{
	                  System.out.println("writing index failed");
	                  blHistory += " but index failed\n";
	              }
	              testlex.dumpHistory(blHistory, argument);
	          }
	          else if (input.startsWith("findequivs")) {
	              System.out.println("searching loaded lexicon for iko/iio equivs");
	              HashMap eh = testlex.findEquivs();
	              TreeMap fe = new TreeMap(eh);
	
	              System.out.println("found " + fe.size() + " equivocations");
	              Iterator eqit = fe.entrySet().iterator();
	              while (eqit.hasNext()){
	                  Map.Entry mpent = (Map.Entry)eqit.next();
	                  System.out.print(((Word)(mpent.getValue())).wordstring + " ");
	              }
	              System.out.println("\nfound " + fe.size() + " equivocations");
	          }
	          else if (input.startsWith("forgetmorph")) {
	              System.out.println("restoring index numbs");
	              //////testlex.restoreIndexNumbers();
	              System.out.println("forgetting morphed words");
	              icount = testlex.forgetMorphWords(testlex.highWordIndex);
	              System.out.println("forgot " + icount + " morphed words");
	          }
	          else if (input.startsWith("load ")) {
	            argument = input.substring(5);
	            System.out.println("Loading file: " + argument);
	            if (testlex.loadFile(argument))
	                blHistory+= ":loaded " + argument + "\n";
	            System.out.println("done");
	          }
	          else if (input.startsWith("loaddef")) {
	            System.out.println("Reloading any deffered lexical items ");
	            testlex.reloadDeferredEntries();
	            blHistory+= ":reloaded deferred lexical items \n";
	            System.out.println("done reloading deferred");
	          }
	          else if (input.startsWith("loglevel ")) {
	            argument = (input.substring(8)).trim();
	            int ll = Integer.parseInt(argument);
	            log.setLevel(ll);
	            System.out.println("logLevel reset to " + ll);
	          }
	          else if (input.startsWith("preserveparents")) {
	            System.out.println("Marking parents of preserved words for preservation");
	            icount = testlex.fastPreserveParents();
	            blHistory += ":preserved " + icount + " parent words\n";
	            System.out.println("preserveparents preserved " + icount +
	                               " parent words");
	          }
	          else if (input.startsWith("preserve")) {
	            System.out.println("Marking all words for preservation");
	            icount = testlex.preserveWords();
	            blHistory += ":preserved preserved " + icount + " words\n";
	            System.out.println("preserved " + icount + " words");
	          }
	          else if (input.startsWith("printatoms")) {
	              testlex.printAtoms();
	          }
	          else if (input.startsWith("purge")) {
	            System.out.println("Purging words");
	            icount = testlex.purgeWords();
	            blHistory += ":purged " + icount + "words\n";
	            System.out.println("purged " + icount + "words");
	          }
	          else if (input.startsWith("shrinklex")) {
	              System.out.println("assigning any needed new index numbers");
	              icount = testlex.assignAdditionalIndexNumbers();
	              System.out.println("Assigned " + icount + " new ones");
	              System.out.println("Shrinking lex");
	              icount = testlex.crushLex();
	              System.out.println("crushed " + icount + " word entries");
	              icount = testlex.forgetMorphWords(testlex.highWordIndex);
	              System.out.println("forgot " + icount + " morphed words");
	          }
	          else if (input.startsWith("stubs")) {
	              System.out.println("Searching loaded lexicon for stub words");
	              TreeSet stubs = testlex.findStubs();
	              int found = stubs.size();
	              if (found > 0) {
	                  System.out.println("Found " + found + " stub words");
	
	                  Iterator it = stubs.iterator();
	                  while (it.hasNext())
	                      System.out.print((String)it.next() + " ");
	                  System.out.print("\n");
	              }
	          }
	          else if (input.startsWith("trim")) {
	            System.out.println("Trimming all indexed words");
	            icount = testlex.trimWords();
	            blHistory += ":trimmed " + icount + "words\n";
	            System.out.println("trimmed " + icount + " words");
	          }
	          else if (input.startsWith("show ")) {
	            argument = (input.substring(5)).trim();
	            testword = testlex.getWord(argument);
	            if (testword != null){
	                System.out.println(argument + ".printEntryString() = " +
	                                   testword.printEntryString());
	                System.out.print("index = ");
	                String dtags = " ";
	                if ((testword.index & Lexicon.NODISKINDEX) != 0)
	                    dtags += "NODISKINDEX & ";
	                if ((testword.index & Lexicon.DISKOUTDATED) != 0)
	                    dtags += "DISKOUTDATED & ";
	                if ((testword.index & Lexicon.NOWORDENTRY) != 0)
	                    dtags += "NOWORDENTRY & ";
	                System.out.println(dtags + (Lexicon.WORDINDEXMASK & testword.index));
	            }else
	                System.out.println(argument + " not found.");
	          }
	          else if (input.startsWith("showcat ")) {
	            argument = (input.substring(8)).trim();
	            Category testcat = testlex.getCategory(argument);
	            if (testcat == null)
	                System.out.println(argument + " Not a known category");
	            else {
	                System.out.print("category " + argument + " = ");
	                testcat.showCategory();
	            }
	          }else if (input.startsWith("showsense ")) {
	            argument = (input.substring(10).trim());
	            testword = testlex.getWord(argument);
	            if (testword != null){
	                System.out.print(testword.printString());
	                if (testword.sensenamep())
	                    System.out.println("  sensecat=" + testword.getSenseCat() +
	                                       "; senseword=" + testword.getSenseWord() +
	                                       "; senseTail=" + testword.getSenseTailString());
	                else System.out.println(" no sensename entry ");
	            }else
	                System.out.println(argument + " not found.");
	          }
	          else if (input.startsWith("add ")) {
	            argument = input.substring(4);
	            System.out.println("Adding entry: " + argument);
	             blHistory += ":added " + argument + "\n";
	            testlex.makeEntry(argument);
	            System.out.println("done");
	          }
	          else if (input.startsWith("reload")) {
	            if (testlex.highCatIndex < 3)
	                System.out.println("Need to assign index nums first...");
	            else if (binLexRaf == null)
	                System.out.println("Can't reload until DiskLex is opened");
	            else if (input.startsWith("reloadid ")){
	                idx = Integer.parseInt(input.substring(9));
	                System.out.println("reloading disk obj " + idx);
	                Value val = testlex.reloadBinObj(idx);
	                if (val == null)
	                    System.out.println("index " + idx + " null");
	                else if (val instanceof Word)
	                    System.out.println("index " + idx + " reloaded word " +
	                                       ((Word)val).printEntryString());
	                else System.out.println("index " + idx + " = " +
	                                        val.printString());
	            }else if (input.startsWith("reload ")){
	                argument = input.substring(7);
	                Word w = testlex.getWord(argument);
	                if (w == null)
	                    System.out.println("can't find a word =>" + argument + "<=");
	                else {
	                    if ((w.index & Lexicon.MARKEDINDEX) != 0)
	                        System.out.println("marked not use disk index for word " +
	                                           w.printString());
	                    else {
	                        System.out.println("reloaded disk word " +
	                                           w.printEntryString());
	                    }
	                }
	            }
	          }
	          else if (input.startsWith("clear ")) {
	              StringTokenizer tokens = new StringTokenizer(input.substring(6), " ");
	              String wordString;
	              while (tokens.hasMoreTokens() &&
	                     ((wordString = tokens.nextToken()) != null) ){
	                  Word temp = testlex.getWord(wordString);
	                  if (temp != null) {
	                      System.out.println("Clearing: " + wordString);
	                      temp.clearWord(true);
	                      blHistory += ":cleared " + temp.printString() + "\n";
	                  }
	                  else System.out.println(wordString + " not in lexicon");
	              }
	          }
	          else if (input.startsWith("remove ")) {
	            argument = input.substring(7);
	            Word temp = testlex.getWord(argument);
	            if (temp != null) {
	              System.out.println("Removing: " + argument);
	              temp.removeWord();
	              blHistory += ":removed " + temp.printString() + "\n";
	            }
	            else System.out.println(argument + " not in lexicon");
	          }
	          else if (input.startsWith("restore ")) {
	              argument = input.substring(8);
	              System.out.println("restoring bin lex Index for " + argument +
	                                 ".bidx");
	              icount = testlex.loadLexIndex(argument);
	              if (icount > 0){
	                  System.out.println("restored index of " + icount + " objects");
	                  blHistory += ":restored index " + argument + ".bidx\n";
	              }else{
	                  System.out.println("restoring index failed");
	                  blHistory += ":restore " + argument + ".bidx failed\n";
	              }
	              System.out.println("restoring bin lex in " + argument);
	              icount = testlex.loadLex(argument);
	              System.out.println("restored " + icount + " objects");
	              blHistory += ":restored full bin lex " + argument + ".blex\n";
	          }
	          else if (input.startsWith("restoreindex ")) {
	              argument = input.substring(13);
	              System.out.println("restoring bin lex Index for " + argument +
	                                 ".bidx");
	              icount = testlex.loadLexIndex(argument);
	              if (icount > 0){
	                  System.out.println("restored index of " + icount + " objects");
	                  blHistory += ":restored index " + argument + ".bidx\n";
	              }else{
	                  System.out.println("restoring index failed");
	                  blHistory += ":restore " + argument + ".bidx failed\n";
	              }
	              System.out.println("reloading core words");
	              int oldLoadedWE = testlex.reloadedWordEntries;
	              testlex.loadCoreLex(argument);
	              icount = testlex.reloadedWordEntries - oldLoadedWE;
	              System.out.println("reloaded " + icount + " core word entries");
	              blHistory += ":restored " + icount + " core words from " +
	                  argument + "\n";
	              System.out.println("opening bin lex random access file " + argument);
	              binLexRaf = testlex.openBinLexRAF(argument);
	          }
	          else if (input.startsWith("restorepacked ")) {
	              argument = input.substring(14);
	              System.out.println("restoring bin lex Index for " + argument +
	                                 ".bidx");
	              blHistory += testlex.restoreHistory(argument);
	              icount = testlex.loadLexIndex(argument);
	              if (icount > 0){
	                  System.out.println("restored index of " + icount + " objects");
	                  blHistory += ":restored index " + argument + ".bidx\n";
	              }else{
	                  System.out.println("restoring index failed");
	                  blHistory += ":restore " + argument + ".bidx failed\n";
	              }
	              System.out.println("reloading packed forms of all words");
	              int oldLoadedWE = testlex.reloadedWordEntries;
	              int oldLoadedBB = testlex.reloadedBitBuffers;
	              testlex.loadPackedLex(argument);
	              int bbcount = testlex.reloadedBitBuffers - oldLoadedBB;
	              icount = testlex.reloadedWordEntries - oldLoadedWE;
	              System.out.println("reloaded " + bbcount +
	                                 " binary packed word entries and unpacked " +
	                  icount);
	              blHistory += ":restored " + icount + " core words from " +
	                  argument + "\n";
	          }
	          else if (input.startsWith("savemorph ")) {
	            argument = input.substring(10).trim();
	            System.out.println("Saving morphed words to: " + argument);
	            testlex.saveMorphWords(0, argument);
	            System.out.println("done");
	          }
	          else if (input.startsWith("save ")) {
	            argument = input.substring(5).trim();
	            System.out.println("Saving entries to: " + argument);
	            testlex.saveFile(argument);
	            System.out.println("done");
	          }
	          else if (input.equals("clear")) {
	            System.out.println("Clearing lexicon");
	            blHistory = "";
	            testlex.clear();
	          }
	          else if (input.startsWith("size ")) {
	            argument = input.substring(5);
	            System.out.println("Setting lexicon size to: " + argument);
	            try {
	              testlex.setSize(new Integer(argument).intValue());
	            }
	            catch (Exception e) {
	              System.out.println("Setting lexicon size failed");
	            }
	          }
	          else if (input.startsWith("wtparents ")) {
	            argument = (input.substring(10).trim());
	            WordToken testwt = new WordToken(testlex,argument);
	            if (testwt == null)
	                System.out.println("null WordToken");
	            else {
	                System.out.print(testwt.toString() + " ");
	                Word[] wtroots = testwt.getRoots();
	                Word[] wtparents = testwt.nonRootParents();
	                System.out.println("roots = " + LexiconUtil.arrayToString(wtroots) +
	                           "; nrParents = " + LexiconUtil.arrayToString(wtparents));
	            }
	          }
	          else if (input.startsWith("debug ")) {
	            argument = input.substring(6);
	            if (argument.equals("on")) {
	              System.out.println("Debugging: on");
	              testlex.startDebug();
	            }
	            else {
	              System.out.println("Debugging: off");
	              testlex.stopDebug();
	            }
	          }
	          else System.out.println("Unrecognized input: " + input);
	
	          long end = System.currentTimeMillis();
	
	          if (args.length < 1 || !args[0].equals("-t"))
	            System.out.println((end-start) + " msec");
	        }
	        System.out.println(" >");
	      }
	    }
	    catch (IOException e) {;}
	  }

}
