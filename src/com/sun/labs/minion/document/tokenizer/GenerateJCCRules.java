/*
 * GenerateJCCRules.java
 *
 * Created on June 6, 2006, 11:18 AM
 */

package com.sun.labs.minion.document.tokenizer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A class that will generate a set of JavaCC lexer rules that follow the pattern
 * of the Universal Tokenizer.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class GenerateJCCRules {
    
    public void printRules() {
        
        List<Character> white = new ArrayList<Character>();
        List<Character> spacesep = new ArrayList<Character>();
        List<Character> nonspacesep = new ArrayList<Character>();
        List<Character> punct = new ArrayList<Character>();
        
        //
        // Divide up the unicode characters into different types.
        for(int i = 0; i < Character.MAX_VALUE; i++) {
            if(Character.isSpaceChar(i)) {
                white.add((char) i);
            } else if(UniversalTokenizer.isAsian((char) i)) {
                nonspacesep.add((char) i);
            } else if(Character.isLetterOrDigit(i)) {
                spacesep.add((char) i);
            } else {
                punct.add((char) i);
            }
        }
        
        //
        // Print the rules for these things.
        System.err.println("Whitespace: " + white.size());
        System.out.println("TOKEN : { < WHITESPACE : (<WHITECHAR>) (<WHITECHAR>)* > }");
        printRanges(white, "WHITECHAR", 10);
        System.err.println("Space separated: " + spacesep.size());
        System.out.println("TOKEN : { < SPACESEPTOKEN : (<SPACESEPCHAR> | <SPACESEPCHAR>)+ > }");
        printRanges(spacesep, "SPACESEPCHAR", 10);
        System.err.println("Non space separated: " + nonspacesep.size());
        System.out.println("TOKEN : { < NGRAMTOKEN : (<NONSPACESEPCHAR>) (<NONSPACESEPCHAR>)* > }");
        printRanges(nonspacesep, "NONSPACESEPCHAR", 10);
        System.err.println("Punctuation: " + punct.size());
        System.out.println("TOKEN : { < PUNCTUATION : ~[] > }");
//        printRanges(punct, "PUNCTCHAR", 10);
    }
    
    public void printRanges(List<Character> l, String basename, int groupSize) {
        
        //
        // If we only have a few characters, don't bother with ranges.
        if(l.size() < 30) {
            printRule(l, basename);
            return;
        }
        
        //
        // Bother with ranges.
        char start = (char) -1;
        char curr = (char) -1;
        List<Range> ranges = new ArrayList<Range>();
        List<Character> single = new ArrayList<Character>();
        for(Character c : l) {
            if(start == (char) -1) {
                start = c;
                curr = c;
                continue;
            }
            
            if(c == curr + 1) {
                curr = c;
                continue;
            }
            
            if(curr > start) {
                ranges.add(new Range(start, curr));
                start = c;
                curr = c;
            } else {
                single.add(start);
                start = c;
                curr = c;
            }
        }
        
        //
        // Handle the last one.
        if(curr > start) {
            ranges.add(new Range(start, curr));
        } else {
            single.add(start);
        }
        
        //
        // Figure out the number of ranges and print them in groups of the
        // given size.
        int nGroups = ranges.size() / groupSize;
        if(ranges.size() % groupSize != 0) {
            nGroups++;
        }
        
        //
        // Figure out how many rules we'll generate.
        int nRules = nGroups;
        if(single.size() > 0) {
            nRules++;
        }
        
        //
        // What if there's only one ranges rule?  We'll just use the base name
        // for it.
        if(nRules == 1 && single.size() == 0) {
            printRanges(ranges, 0, ranges.size(), basename);
            return;
        }
        
        //
        // Generate a rule with the base name that's an alternation of all the
        // other rules.
        System.out.println("TOKEN: { < " + basename + " :\n   (" );
        for(int i = 1; i <= nRules; i++) {
            System.out.print("    <" + basename + i + ">");
            if(i < nRules) {
                System.out.println(" | ");
            }
        }
        System.out.println("\n   )\n> }");
        
        //
        // Now we can actually generate the individual rules.
        //
        // Print the character ranges in groups of the given size.
        for(int i = 0; i < nGroups; i++) {
            int end = i + groupSize;
            if(end > ranges.size()) {
                end = ranges.size();
            }
            printRanges(ranges, i, end, basename + (i+1));
        }
        
        //
        // Print any single characters as a single alternation.
        if(single.size() > 0) {
            printRule(single, basename + (nGroups+1));
        }
        
    }
    
    public void printRule(List<Character> l, String name) {
        System.out.println("TOKEN : {\n< #" + name + " : ");
        for(Iterator<Character> i = l.iterator(); i.hasNext(); ) {
            Character c = i.next();
            System.out.print("   \"" + escape(c) + "\"");
            if(i.hasNext()) {
                System.out.println(" | ");
            }
        }
        System.out.println("\n> }");
    }
    
    public void printRanges(List<Range> l, int start, int end, String name) {
        System.out.println("TOKEN : {\n< #" + name +  " : " + "\n   [");
        for(int i = start; i < end; i++) {
            System.out.print("    \"" + escape(l.get(i).start) + "\"-\"" + escape(l.get(i).end) + "\"");
            if(i < end - 1) {
                System.out.println(",");
            }
        }
        System.out.println("\n   ]\n> }");
    }
    
    /**
     * Converts a string to a sequence of four hex digit unicode
     * descriptions.
     */
    public static String escape(Character c) {
        String u = Integer.toHexString((int) c.charValue());
        switch(u.length()) {
            case 1:
                return "\\u000" + u;
            case 2:
                return "\\u00" + u;
            case 3:
                return "\\u0" + u;
            default:
                return "\\u" + u;
        }
    }
    
    protected class Range {
        Character start;
        Character end;
        public Range(Character start, Character end) {
            this.start = start;
            this.end = end;
        }
    }
    
    public static void main(String[] args) {
        GenerateJCCRules ct = new GenerateJCCRules();
        ct.printRules();
    }
}
