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

import com.sun.labs.minion.lexmorph.LiteMorph;
import com.sun.labs.minion.lexmorph.LiteMorphRule;
import com.sun.labs.minion.lexmorph.LiteMorph_de;
import com.sun.labs.minion.lexmorph.LiteMorph_en;
import java.io.*;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

/**
 ** This class can be used to test the rules and behavior of LiteMorph.
 ** LiteMorph will generate an array of morphological variants of a word
 ** to use in search-and-retrieval applications where a user wants to find 
 ** other words morphologically related to words in a query.  For example, 
 ** a request such as "electrical fixtures" should also retrieve "electric
 ** fixtures," "electrical fixture," etc.  Given a word of a query, these 
 ** rules generate alternative variations of that word that should also be 
 ** considered.  This generation of variant forms of a word fills a role 
 ** similar to that often filled by the use of wild card characters or by 
 ** stemming rules that produce truncated stems in traditional information
 ** retrieval systems.  The approach of generating alternative variations
 ** has advantages over a truncated stemming approach for many applications,
 ** because it does not require stemming operations during the indexing
 ** process, does not require extra indexing space for stems, nor does it
 ** lose information by storing only stems.  Rather, the variation rules
 ** are applied to the query to produce additional forms to check against
 ** the index.

 ** Compared to the use of wild card characters, this approach has two 
 ** advantages: first, it does not require the user to think about where 
 ** the wild cards should be placed, and secondly, it deals with irregular
 ** variations such as irregular verbs (e.g., "break," "broke," "broken"),
 ** and with stem ending effects such as silent e's and doubling of final 
 ** consonants (e.g., "dig," "digs," "digging").  The rules presented here, 
 ** together with a table of exceptions, provided at the end, deal with 
 ** a wide range of such effects, without requiring any more attention on 
 ** the part of the user than to turn on the facility.

 ** These rules generate regular morphological variants of words using the 
 ** suffixes s, er, ers, est, ed, ing, ly, ful, less, ness, and ment.  Rules 
 ** are included for dealing with some words ending in -ize, -ise, -ic and 
 ** -ical, and for some words requiring irregular forms, such as -leaf and
 ** -man compounds (flyleaf, footman), and Latin words ending in -um and -a, 
 ** such as datum.  The rules are supplemented by a list of exceptions for
 ** words that do not inflect regularly.  They are not intended to apply to
 ** function words or to proper names.  When expanding queries, you may not
 ** want to apply them to capitalized words or to hyphenated words like
 ** day-to-day and low-level.

 ** The rules treat almost all words as if they were multiply meaningful
 ** as nouns, verbs, and adjectives.  Hence, the rules will often generate
 ** spurious forms that should never occur in a text -- e.g., fix ->
 ** fixest, happy -> happied.  The rules are suitable for applications
 ** such as searching text using inverted files, where a quick test
 ** suffices to determine that a given candidate does not occur in the
 ** corpus.  In such applications, it is preferable to overgenerate
 ** candidates than to miss possible retrievals.

 **/
 

public class LiteMorphTest {

    public static void main(String[] args)
    {
	try {
	    // BufferedReader is needed for readLine() method
	    BufferedReader in =
		// but we'll make it a degenerate buffer of 1
		// to workaround a problem with some PC implementations
		// which wouldn't start to read before a lot of chars
		// were typed
		new BufferedReader(new InputStreamReader(System.in), 1);
	    LiteMorph morph_en = LiteMorph_en.getMorph(); 
	    LiteMorph morph_de = LiteMorph_de.getMorph(); 
	    LiteMorph morph = morph_de; 
	    Set<String> test = morph.variantsOf("-"); // warm it up
	    test = null; //don't keep this
	    System.out.println("initialized");
	    System.out.println("Enter word (enter :q or empty line to quit):");
            boolean showTime = true;
	    LiteMorphRule testrule = null;
	    while(true) {
	      String input = in.readLine();
	      if (input==null || input.equals(""))
		break;
	      else if (input.equals("?")) // show commands
	        System.out.println("commands are :th, :tr, :nt, :dr, :nd, " +
				   ":logfile, :nl, " +
				   ":en, :de, :ti, :to, :q");
	      else if (input.equals(":th")) {// trace rule history
		morph.traceFlag = true; //just show rules that applied
	      }
	      else if (input.equals(":tr")) {// trace rules
		morph.traceFlag = true;
	        morph.debugFlag = true;
	      }
	      else if (input.equals(":dr")) { // debug rules
		LiteMorph.traceFlag = true;
	        LiteMorphRule.debugFlag = true; //show all rule attempts
	        LiteMorph.debugFlag = true; //show details of match attempts
              }
	      else if (input.equals(":nt")) { // turn off trace
		LiteMorph.traceFlag = false;
	        LiteMorph.debugFlag = false;
	        LiteMorphRule.debugFlag = false;
              }
	      else if (input.equals(":nd")) { // turn off debug
		LiteMorph.traceFlag = false;
	        LiteMorph.debugFlag = false;
	        LiteMorphRule.debugFlag = false;
              }
              else if (input.startsWith(":logfile ")) {
	        String argument = input.substring(9);
                System.out.println("Will log entries to: " + argument);
		morph.logfile = new PrintWriter(new BufferedWriter
						(new FileWriter(argument)),
						true);
              }
	      else if (input.equals(":nl")) { // turn off logging
	        morph.logfile = null;
                System.out.println("turned off logging");
              }
              else if (input.startsWith(":testrule ")) {
                String argument = input.substring(10);
	        System.out.println("make testrule from: "+ argument);
                testrule = new LiteMorphRule(argument, ":testrule", morph);
                System.out.println("testrule = "+ testrule);
              }
	      else if (input.startsWith(":match ")) {
                String argument = input.substring(7);
                if (testrule != null) {
                  Vector results = testrule.match(argument, 1, 0);
                    if (results != null)
	              System.out.println("for testrule, match(" +
					 argument + ", 1, 0,) = " +
					 results);
                    else System.out.println(argument + " no results.");
                }
                else System.out.println("error: first define a testrule, " +
					"using :testrule, " +
                                        "before calling :match");
              }

	      else if (input.equals(":en")) { // do English
	        morph = morph_en;
		LiteMorph.setLocalizedMorph(morph);
	        System.out.println("switching to English morphology");
	      }
	      else if (input.equals(":de")) {// do German
	        morph = morph_de;
		LiteMorph.setLocalizedMorph(morph);
	        System.out.println("switching to German morphology");
	      }
	      else if (input.equals(":current")) {// what's current morphology
	        System.out.println("current morphology is: "+
				   LiteMorph.getLocalizedMorph());
	      }
	      else if (input.equals(":ti")) // time
	        showTime = true;
	      else if (input.equals(":to")) // time off
                showTime = false;

	      else if (input.equals(":q")) // quit
	        break;
	      else if (input.startsWith(":")) // unrecognized command
		System.out.println("unrecognized command: " + input);
	      else {
		  input = input.trim();
		long start = System.currentTimeMillis();
		Set<String> output = morph.variantsOf(input);
		long end = System.currentTimeMillis();
		if (showTime && (args.length < 1 || !args[0].equals("-t")))
		  System.out.println((end-start) + " msec");
		if (args.length < 1 || !args[0].equals("-v")) {
		  StringBuffer result = new StringBuffer();
          for (Iterator iter = output.iterator(); iter.hasNext();) {
            result.append(' ');
		    result.append(iter.next());
		  }
		  System.out.println(input + " >" + result.toString());
		}
	      }
	      System.out.println(" >");
	    }
	}
	catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
