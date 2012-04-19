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

package com.sun.labs.minion.util;

/**
 * Some char utilities.
 */
public class CharUtils  {

    /**
     * Lower cases a string, using our lower case array.
     * @param s The string that we wish to lowercase.
     * @return The lowercase version of the string.
     */
    public static String toLowerCase(String s) {
        char[] ret = new char[s.length()];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = toLowerCase[s.charAt(i)];
        }
        return new String(ret);
    }

    /**
     * Lowercases a single character using our lower case array.
     * @param c The character to lowercase.
     * @return The lowercase version of the character.
     */
    public static char toLowerCase(char c) {
        return toLowerCase[c];
    }

    /**
     * Uppercases a string, using our upper case array.
     * @param s The string to uppercase.
     * @return The upper case version of the string.
     */
    public static String toUpperCase(String s) {
        char[] ret = new char[s.length()];
        for(int i = 0; i < ret.length; i++) {
            ret[i] = toUpperCase[s.charAt(i)];
        }
        return new String(ret);
    }

    /**
     * Upper cases a single character, using our upper case array.
     * @param c The character to upper case.
     * @return The uppercase character.
     */
    public static char toUpperCase(char c) {
        return toUpperCase[c];
    }

    /**
     * Tests whether a given character is upper case.
     * @param c The character to test.
     * @return <CODE>true</CODE> if the character is upper case, <CODE>false</CODE> otherwise
     */
    public static boolean isUpper(char c) {
        return c == toUpperCase[c];
    }

    /**
     * A method to check whether a given string has the same all upper and
     * all lower case forms.
     * @param s The string to test.
     * @return <CODE>true</CODE> if the given string has identical upper and lower case forms.
     */
    public static boolean isUncased(String s) {
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if(toLowerCase(c) != toUpperCase(c)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method that traslates unicode escape sequences
     * in an input string into appropriate unicode chars.
     * @param input The input string to be translated.
     * @return The string with unicode escape sequences replaced with actual unicode characters.
     */
    public static final String decodeUnicode (String input) {
        char[] in = input.toCharArray();
        int len = in.length;
        char[] out = new char[len];
        int i = 0;
        int j = 0;
        int val;
        while (i < len) {
            if (in[i] == '\\' && ((i+5) < len) &&
                (in[i+1] == 'u' || in[i+1] == 'U')) {
                String hex = new String(in, i+2, 4);
                try {
                    val = Integer.parseInt(hex, 16);
                    out[j++] = (char)val;
                    i += 6;
                } catch (Exception e) {
                    out[j++] = in[i++];
                }
            } else {
                out[j++] = in[i++];
            }  
        }
        return new String(out, 0, j);
    }

    /**
     * Converts a character to lower case and then converts the lower  case
     * to uppercase.
     *
     * @param c The character.
     * @return <code>true</code> if the character survives the roundtrip,
     * <code>false</code> otherwise.
     */
    public static boolean caseRoundTrip(char c) {
        return c == toUpperCase[toLowerCase[c]];
    }

    /**
     * Return the lower case of a char. Unlike Java's Character.toLowerCase(),
     * this version is free of loops and paths (!).
     */
    protected static final char toLowerCase[] = new char[Character.MAX_VALUE+1];

    /**
     * Return the upper case of a char. Unlike Java's Character.toUpperCase(),
     * this version is free of loops and paths (!).
     */
    protected static final char toUpperCase[] = new char[Character.MAX_VALUE+1];

    static {

        //
        // Initialize the lower case array with Java toLowerCase values.
        for (int c = 0; c <= Character.MAX_VALUE; ++c) {
            toLowerCase[c] = Character.toLowerCase((char)c);
        }

        //
        // Sanitize the array by removing paths.
        for (int c = 0; c <= Character.MAX_VALUE; ++c) {
            char lc = toLowerCase[(char)c];
            char llc = toLowerCase[lc];
            if (llc != lc) {
                //
                // The start of a path. Kill it...
                // This assume the first char (c) is upper case
                // and the second (lc) is lower case.
                //System.out.println("setting " + (int)lc);
                if (lc > c)
                    toLowerCase[lc] = lc;
                else
                    toLowerCase[c] = llc;
            }
        }

        //
        // Invert the lower case array to build the upper case array.
        for (int c = 0; c <= Character.MAX_VALUE; ++c)
            toUpperCase[c] = (char)c;
        for (int c = 0; c <= Character.MAX_VALUE; ++c) {
            char lc = toLowerCase[c];
            char uc = toUpperCase[lc];
            //
            // Favours the first upper case version when ambiguous.
            if (lc != c && uc == lc)
                toUpperCase[lc] = (char)c;
        }

        //
        // Patch the toLowerCase array for cross-VM compatibility.
        toLowerCase['\u01a6'] = '\u0280';
        toLowerCase['\u01f6'] = '\u0195';
        toLowerCase['\u01f7'] = '\u01bf';
        toLowerCase['\u01f8'] = '\u01f9';
        toLowerCase['\u0218'] = '\u0219';
        toLowerCase['\u021a'] = '\u021b';
        toLowerCase['\u021c'] = '\u021d';
        toLowerCase['\u021e'] = '\u021f';
        toLowerCase['\u0222'] = '\u0223';
        toLowerCase['\u0224'] = '\u0225';
        toLowerCase['\u0226'] = '\u0227';
        toLowerCase['\u0228'] = '\u0229';
        toLowerCase['\u022a'] = '\u022b';
        toLowerCase['\u022c'] = '\u022d';
        toLowerCase['\u022e'] = '\u022f';
        toLowerCase['\u0230'] = '\u0231';
        toLowerCase['\u0232'] = '\u0233';
        toLowerCase['\u0275'] = '\u0275';
        toLowerCase['\u03da'] = '\u03db';
        toLowerCase['\u03dc'] = '\u03dd';
        toLowerCase['\u03de'] = '\u03df';
        toLowerCase['\u03e0'] = '\u03e1';
        toLowerCase['\u0400'] = '\u0450';
        toLowerCase['\u040d'] = '\u045d';
        toLowerCase['\u048c'] = '\u048d';
        toLowerCase['\u048e'] = '\u048f';
        toLowerCase['\u04ec'] = '\u04ed';
        toLowerCase['\u10a0'] = '\u10a0';
        toLowerCase['\u10a1'] = '\u10a1';
        toLowerCase['\u10a2'] = '\u10a2';
        toLowerCase['\u10a3'] = '\u10a3';
        toLowerCase['\u10a4'] = '\u10a4';
        toLowerCase['\u10a5'] = '\u10a5';
        toLowerCase['\u10a6'] = '\u10a6';
        toLowerCase['\u10a7'] = '\u10a7';
        toLowerCase['\u10a8'] = '\u10a8';
        toLowerCase['\u10a9'] = '\u10a9';
        toLowerCase['\u10aa'] = '\u10aa';
        toLowerCase['\u10ab'] = '\u10ab';
        toLowerCase['\u10ac'] = '\u10ac';
        toLowerCase['\u10ad'] = '\u10ad';
        toLowerCase['\u10ae'] = '\u10ae';
        toLowerCase['\u10af'] = '\u10af';
        toLowerCase['\u10b0'] = '\u10b0';
        toLowerCase['\u10b1'] = '\u10b1';
        toLowerCase['\u10b2'] = '\u10b2';
        toLowerCase['\u10b3'] = '\u10b3';
        toLowerCase['\u10b4'] = '\u10b4';
        toLowerCase['\u10b5'] = '\u10b5';
        toLowerCase['\u10b6'] = '\u10b6';
        toLowerCase['\u10b7'] = '\u10b7';
        toLowerCase['\u10b8'] = '\u10b8';
        toLowerCase['\u10b9'] = '\u10b9';
        toLowerCase['\u10ba'] = '\u10ba';
        toLowerCase['\u10bb'] = '\u10bb';
        toLowerCase['\u10bc'] = '\u10bc';
        toLowerCase['\u10bd'] = '\u10bd';
        toLowerCase['\u10be'] = '\u10be';
        toLowerCase['\u10bf'] = '\u10bf';
        toLowerCase['\u10c0'] = '\u10c0';
        toLowerCase['\u10c1'] = '\u10c1';
        toLowerCase['\u10c2'] = '\u10c2';
        toLowerCase['\u10c3'] = '\u10c3';
        toLowerCase['\u10c4'] = '\u10c4';
        toLowerCase['\u10c5'] = '\u10c5';
        toLowerCase['\u2126'] = '\u03c9';
        toLowerCase['\u212a'] = '\u006b';
        toLowerCase['\u212b'] = '\u00e5';
	
        //
        // Patch the toUpperCase array for cross-vm compatibility.
        toUpperCase['\u00b5'] = '\u00b5';
        toUpperCase['\u0195'] = '\u01f6';
        toUpperCase['\u01bf'] = '\u01f7';
        toUpperCase['\u01f9'] = '\u01f8';
        toUpperCase['\u0219'] = '\u0218';
        toUpperCase['\u021b'] = '\u021a';
        toUpperCase['\u021d'] = '\u021c';
        toUpperCase['\u021f'] = '\u021e';
        toUpperCase['\u0223'] = '\u0222';
        toUpperCase['\u0225'] = '\u0224';
        toUpperCase['\u0227'] = '\u0226';
        toUpperCase['\u0229'] = '\u0228';
        toUpperCase['\u022b'] = '\u022a';
        toUpperCase['\u022d'] = '\u022c';
        toUpperCase['\u022f'] = '\u022e';
        toUpperCase['\u0231'] = '\u0230';
        toUpperCase['\u0233'] = '\u0232';
        toUpperCase['\u0275'] = '\u019f';
        toUpperCase['\u0280'] = '\u01a6';
        toUpperCase['\u0345'] = '\u0345';
        toUpperCase['\u03db'] = '\u03da';
        toUpperCase['\u03dd'] = '\u03dc';
        toUpperCase['\u03df'] = '\u03de';
        toUpperCase['\u03e1'] = '\u03e0';
        toUpperCase['\u03f2'] = '\u03f2';
        toUpperCase['\u0450'] = '\u0400';
        toUpperCase['\u045d'] = '\u040d';
        toUpperCase['\u048d'] = '\u048c';
        toUpperCase['\u048f'] = '\u048e';
        toUpperCase['\u04ed'] = '\u04ec';
        toUpperCase['\u10d0'] = '\u10d0';
        toUpperCase['\u10d1'] = '\u10d1';
        toUpperCase['\u10d2'] = '\u10d2';
        toUpperCase['\u10d3'] = '\u10d3';
        toUpperCase['\u10d4'] = '\u10d4';
        toUpperCase['\u10d5'] = '\u10d5';
        toUpperCase['\u10d6'] = '\u10d6';
        toUpperCase['\u10d7'] = '\u10d7';
        toUpperCase['\u10d8'] = '\u10d8';
        toUpperCase['\u10d9'] = '\u10d9';
        toUpperCase['\u10da'] = '\u10da';
        toUpperCase['\u10db'] = '\u10db';
        toUpperCase['\u10dc'] = '\u10dc';
        toUpperCase['\u10dd'] = '\u10dd';
        toUpperCase['\u10de'] = '\u10de';
        toUpperCase['\u10df'] = '\u10df';
        toUpperCase['\u10e0'] = '\u10e0';
        toUpperCase['\u10e1'] = '\u10e1';
        toUpperCase['\u10e2'] = '\u10e2';
        toUpperCase['\u10e3'] = '\u10e3';
        toUpperCase['\u10e4'] = '\u10e4';
        toUpperCase['\u10e5'] = '\u10e5';
        toUpperCase['\u10e6'] = '\u10e6';
        toUpperCase['\u10e7'] = '\u10e7';
        toUpperCase['\u10e8'] = '\u10e8';
        toUpperCase['\u10e9'] = '\u10e9';
        toUpperCase['\u10ea'] = '\u10ea';
        toUpperCase['\u10eb'] = '\u10eb';
        toUpperCase['\u10ec'] = '\u10ec';
        toUpperCase['\u10ed'] = '\u10ed';
        toUpperCase['\u10ee'] = '\u10ee';
        toUpperCase['\u10ef'] = '\u10ef';
        toUpperCase['\u10f0'] = '\u10f0';
        toUpperCase['\u10f1'] = '\u10f1';
        toUpperCase['\u10f2'] = '\u10f2';
        toUpperCase['\u10f3'] = '\u10f3';
        toUpperCase['\u10f4'] = '\u10f4';
        toUpperCase['\u10f5'] = '\u10f5';
        toUpperCase['\u1fbe'] = '\u1fbe';
    }


    /**
     * Tests the upper and lowercase
     * @param arg Arguments
     * @throws java.lang.Exception If anything goes wrong.
     */
    static public void main(String[] arg) throws Exception {

        //
        // Showing diffs b/w Minion toLower and Java toLower
        System.out.println("\nShowing diffs b/w Minion toLower and Java toLower");
        for (int i = 0; i < 65536; ++i) {
            char nlc = CharUtils.toLowerCase[i];
            char jlc = Character.toLowerCase((char)i);
            if (nlc != jlc)
                System.out.println("char " + Util.escape((char)i) + ": minion lc=" + Util.escape(nlc) + ", java lc=" + Util.escape(jlc));
        }

        //
        // Showing diffs b/w Minion toUpper and Java toUpper
        System.out.println("\nShowing diffs b/w Minion toUpper and Java toUpper");
        for (int i = 0; i < 65536; ++i) {
            char nuc = toUpperCase[i];
            char juc = Character.toUpperCase((char)i);
            if (nuc != juc)
                System.out.println("char " + Util.escape((char)i) + ": minion uc=" + Util.escape(nuc) + ", java uc=" + Util.escape(juc));
        }

        //
        // Test for loops/paths.
        System.out.println("\nTesting for loops and paths");
        for (int c = 0; c <= Character.MAX_VALUE; ++c) {
            char lc0 = CharUtils.toLowerCase[c];
            char lc1 = lc0;
            for (int i = 0; ; ++i) {
                char lc2 = CharUtils.toLowerCase[lc1];
                if (lc2 == lc1)
                    break;
                System.out.println("path - " + i +""+  (int)c + " -> " + (int)lc1 + " -> " + (int)lc2);
                if (lc2 == c) {
                    System.out.println("loop detected - " + c);
                    break;
                }
                break;
                //lc1 = lc2;
            }
        }

        //
        // Some known nasties.
        System.out.println("\nTesting some known chars");
        char[] t = new char[] { '\u019f', '\u0275', '\u01c4', '\u01c5', '\u01c6', '\u0130', '\u0131', '\u0069', '\u0049' };
        for (int i = 0; i < t.length; ++i) {
            System.out.println("" + Util.escape(t[i]) + " -> " + Util.escape(CharUtils.toLowerCase[t[i]]));
        }

        System.out.println("\nIdempotency");
        for (int i = 0; i < Character.MAX_VALUE; ++i) {
            char c = toLowerCase[i];
            if(toLowerCase[c] != c) {
                System.out.println("Weird tLC: " + Util.escape((char) i));
            }
            c = toUpperCase[i];
            if(toUpperCase[c] != c) {
                System.out.println("Weird tUC: " + Util.escape((char) i));
            }
        }

        //
        // Diff b/w Java and Minion toLowerCase.
        System.out.println("\nTesting diffs b/w Java and Minion toLowerCase()");
        for (int i = 0; i < 65536; ++i) {
            char jlc = Character.toLowerCase((char)i);
            char nlc = CharUtils.toLowerCase[i];
            if (jlc != nlc)
                System.out.println("char " + Util.escape((char)i) + ": java lc=" + Util.escape(jlc) + ", minion lc=" + Util.escape(nlc));
        }

        //
        // Checking toLower/toUpper round trip
        System.out.println("\nChecking toLower/toUpper round trip");
        for (int i = 0; i < 65536; ++i) {
            char lc = toLowerCase[i];
            char uc = toUpperCase[lc];
            if (i != lc && uc != i)
                System.out.println("char " + Util.escape((char)i) + ": lc=" + Util.escape(lc) + ", uc(lc)=" + Util.escape(uc));
        }

    }


}

