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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import com.sun.labs.minion.document.tokenizer.UniversalTokenizer;
import com.sun.labs.minion.pipeline.Token;
import java.io.File;
import java.util.logging.Logger;

/**
 * A class holding various static utility functions.
 */
public class Util {

    /**
     * A log.
     */
    Logger logger = Logger.getLogger(getClass().getName());

    /**
     * A tag for our log entries.
     */
    protected static String logTag = "UTIL";

    /**
     * Expands an array of bytes, copying the existing values.
     *
     * @param a The array to expand.
     * @param ns The new size.
     * @return The expanded array.
     */
    public static final byte[] expandByte(byte[] a, int ns) {
        byte[] ret = new byte[ns];
        if(a == null) {
            return ret;
        }
        System.arraycopy(a, 0, ret, 0, a.length);
        return ret;
    }

    public static File getTempFile(String path, String prefix,
            String suffix) throws java.io.IOException {
        return getTempFile(new File(path), prefix, suffix);
    }

    public static File getTempFile(File path, String prefix,
            String suffix) throws java.io.IOException {
        return File.createTempFile(prefix, suffix, path);
    }

    /**
     * Expands an array of integers, copying the existing values.
     *
     * @param a The array to expand.
     * @param ns The new size.
     * @return The expanded array.
     */
    public static final int[] expandInt(int[] a, int ns) {
        int[] ret = new int[ns];
        if(a == null) {
            return ret;
        }
        System.arraycopy(a, 0, ret, 0, a.length);
        return ret;
    }

    public static final int[] addExpand(int[] a, int n, int x) {
        if(n >= a.length) {
            a = expandInt(a, a.length * 2);
        }
        a[n] = x;
        return a;
    }

    public static double[] expandDouble(double[] a, int ns) {
        double[] ret = new double[ns];
        if(a != null) {
            System.arraycopy(a, 0, ret, 0, a.length);
        }
        return ret;
    }

    /**
     * Expands an array of floats, copying the existing values.
     *
     * @param a The array to expand.
     * @param ns The new size.
     * @return The expanded array.
     */
    public static final float[] expandFloat(float[] a, int ns) {
        float[] ret = new float[ns];
        if(a == null) {
            return ret;
        }
        System.arraycopy(a, 0, ret, 0, a.length);
        return ret;
    }

    /**
     * Expands an array of char, copying the existing values.
     *
     * @param a The array to expand.
     * @param ns The new size.
     * @return The expanded array.
     */
    public static final char[] expandChar(char[] a, int ns) {
        char[] ret = new char[ns];
        if(a == null) {
            return ret;
        }
        System.arraycopy(a, 0, ret, 0, a.length);
        return ret;
    }

    /**
     * Expands any array, copying over the contents.  Slower (by about 30%)
     * than the type specific versions, but very easy to extend.
     *
     * @param a The array to expand.
     * @param ns The new capacity.
     * @return An expanded array of the same type, or null if the object is
     * not an array or we don't know how to do this type.
     * @see #expandInt
     * @see #expandFloat
     */
    public static final Object expand(Object a, int ns) {

        if(a instanceof int[]) {
            return expandInt((int[]) a, ns);
        } else if(a instanceof float[]) {
            return expandFloat((float[]) a, ns);
        } else if(a instanceof String[]) {
            String[] ret = new String[ns];
            System.arraycopy((String[]) a, 0, ret, 0, ((String[]) a).length);
            return ret;
        } else if(a instanceof Token[]) {
            Token[] ret = new Token[ns];
            System.arraycopy((Token[]) a, 0, ret, 0, ((Token[]) a).length);
            return ret;
        } else if(a instanceof int[][]) {
            int[][] ret = new int[ns][];
            System.arraycopy((int[][]) a, 0, ret, 0, ((int[][]) a).length);
            return ret;
        } else if(a instanceof float[][]) {
            float[][] ret = new float[ns][];
            System.arraycopy((float[][]) a, 0, ret, 0, ((float[][]) a).length);
            return ret;
        } else if(a instanceof String[][]) {
            String[][] ret = new String[ns][];
            System.arraycopy((String[][]) a, 0, ret, 0, ((String[][]) a).length);
            return ret;
        } else if(a instanceof int[][][]) {
            int[][][] ret = new int[ns][][];
            System.arraycopy((int[][][]) a, 0, ret, 0, ((int[][][]) a).length);
            return ret;
        } else if(a instanceof float[][][]) {
            float[][][] ret = new float[ns][][];
            System.arraycopy((float[][][]) a, 0, ret, 0,
                    ((float[][][]) a).length);
            return ret;
        } else if(a instanceof String[][][]) {
            String[][][] ret = new String[ns][][];
            System.arraycopy((String[][][]) a, 0, ret, 0,
                    ((String[][][]) a).length);
            return ret;
        } else {
            return null;
        }
    }

    /**
     * Gets exactly the provided number of elements from an array and
     * returns an array of that size.
     *
     * @param a The array to extract elements from.
     * @param ne The number of elements.
     * @return An array of the same type with exactly ne elements, or
     * <code>null</code>
     * if the object is not an array or we don't know how to do this type.
     * @see #expandInt
     * @see #expandFloat
     */
    public static final Object getExact(Object a, int ne) {
        if(a instanceof int[]) {
            int[] ret = new int[ne];
            System.arraycopy((int[]) a, 0, ret, 0, ne);
            return ret;
        } else if(a instanceof char[]) {
            char[] ret = new char[ne];
            System.arraycopy((char[]) a, 0, ret, 0, ne);
            return ret;
        } else if(a instanceof float[]) {
            float[] ret = new float[ne];
            System.arraycopy((float[]) a, 0, ret, 0, ne);
            return ret;
        } else if(a instanceof String[]) {
            String[] ret = new String[ne];
            System.arraycopy((String[]) a, 0, ret, 0, ne);
            return ret;
        } else if(a instanceof Token[]) {
            Token[] ret = new Token[ne];
            System.arraycopy((Token[]) a, 0, ret, 0, ne);
            return ret;
        } else if(a instanceof int[][]) {
            int[][] ret = new int[ne][];
            System.arraycopy((int[][]) a, 0, ret, 0, ne);
            return ret;
        } else if(a instanceof float[][]) {
            float[][] ret = new float[ne][];
            System.arraycopy((float[][]) a, 0, ret, 0, ne);
            return ret;
        } else if(a instanceof String[][]) {
            String[][] ret = new String[ne][];
            System.arraycopy((String[][]) a, 0, ret, 0, ne);
            return ret;
        } else {
            return null;
        }
    }

    /**
     * Converts a character to a unicode escape sequence.
     * @param c The character to convert.
     * @return The unicode escaped string containing this character.
     */
    public static final String escape(char c) {
        return String.format("\\u%04d", (int) c);
    }

    /**
     * Converts a String to a unicode escaped String.
     * @param s The string to escape.
     * @return The string with unicode escapes
     */
    public static final String escape(String s) {
        return escape(s, false);
    }

    /**
     * Converts a String to a unicode escaped String.
     * @param s The string to escape.
     * @param leaveAscii if <code>true</code>, leave ASCII characters as-is.
     * @return The string with unicode escapes
     */
    public static final String escape(String s, boolean leaveAscii) {
        StringBuilder b = new StringBuilder();
        for(int i = 0; i < s.length(); ++i) {
            char c = s.charAt(i);
            if(leaveAscii && !UniversalTokenizer.isAsian(c)) {
                b.append(c);
            } else {
                b.append(escape(c));
            }
        }
        return b.toString();
    }

    /**
     * Converts a string to a sequence of four hex digit unicode
     * descriptions.
     */
    public static final String toHexDigits(String s) {
        StringBuilder b = new StringBuilder();
        for(int i = 0; i < s.length(); i++) {
            String u = Integer.toHexString((int) s.charAt(i));
            switch(u.length()) {
                case 1:
                    u = "000" + u;
                    break;
                case 2:
                    u = "00" + u;
                    break;
                case 3:
                    u = "0" + u;
                    break;
            }
            b.append("\\u");
            b.append(u);
        }
        return b.toString();
    }

    /**
     * Makes the case of two strings match, as closely as possible.
     *
     * @param x The original string.
     * @param y A modified string, such as a morphological variant of
     * <code>x</code>.
     * @return A version of <code>y</code> where the characters that
     * correspond with characters in <code>x</code> are in the same case.
     */
    public static final String matchCase(String x, String y) {
        int l = Math.min(x.length(), y.length());
        char[] ret = new char[y.length()];
        for(int i = 0; i < l; i++) {
            char c1 = x.charAt(i);
            char c2 = y.charAt(i);

            //
            // If the characters are equal in lowercase, take the character
            // from x.
            if(CharUtils.toLowerCase(c1) == CharUtils.toLowerCase(c2)) {
                ret[i] = c1;
            } else {
                ret[i] = c2;
            }
        }

        if(l < y.length()) {
            y.getChars(l, y.length(), ret, l);
        }

        return new String(ret);
    }

    /**
     * Generates a string containing the elements of the array.
     */
    public static final String arrayToString(int[] a) {
        if(a == null) {
            return "null";
        }
        return arrayToString(a, 0, a.length);
    }

    /**
     * Generates a string containing the elements of the array.
     */
    public static final String arrayToString(int[] a, int s, int e) {
        if(a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        for(int i = s; i < e; i++) {
            if(i > s) {
                b.append(", ");
            }
            b.append(a[i]);
        }
        b.append(']');
        return b.toString();
    }

    public static final String arrayToString(boolean[] a) {
        if(a == null) {
            return "null";
        }
        return arrayToString(a, 0, a.length);
    }

    /**
     * Generates a string containing the elements of the array.
     */
    public static final String arrayToString(boolean[] a, int s, int e) {
        if(a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        for(int i = s; i < e; i++) {
            if(i > s) {
                b.append(", ");
            }
            b.append(a[i]);
        }
        b.append(']');
        return b.toString();
    }

    /**
     * Generates a string containing the elements of the array.
     */
    public static final String arrayToString(float[] a) {
        if(a == null) {
            return "null";
        }
        return arrayToString(a, 0, a.length);
    }

    /**
     * Generates a string containing the elements of the array.
     */
    public static final String arrayToString(float[] a, int s, int e) {
        if(a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        for(int i = s; i < e; i++) {
            if(i > s) {
                b.append(", ");
            }
            b.append(a[i]);
        }
        b.append(']');
        return b.toString();
    }

    public static String arrayToString(String[] a) {
        if(a == null) {
            return null;
        }
        return arrayToString(a, 0, a.length);
    }

    public static String arrayToString(String[] a, int s, int e) {
        if(a == null) {
            return null;
        }

        StringBuilder b = new StringBuilder();
        b.append('[');
        for(int i = s; i < e; i++) {
            if(i > s) {
                b.append(", ");
            }
            b.append("\"" + a[i] + "\"");
        }
        b.append(']');
        return b.toString();
    }

    public static String arrayToString(Object[] a) {
        if(a == null) {
            return null;
        }
        return arrayToString(a, 0, a.length);
    }

    public static String arrayToString(Object[] a, int s, int e) {
        if(a == null) {
            return null;
        }

        StringBuilder b = new StringBuilder();
        b.append('[');
        for(int i = s; i < e; i++) {
            if(i > s) {
                b.append(", ");
            }
            b.append("\"" + a[i] + "\"");
        }
        b.append(']');
        return b.toString();
    }

    public static String arrayToString(double[] a) {
        return arrayToString(a, 0, a.length);
    }

    /**
     * Generates a string containing the elements of the array.
     */
    public static final String arrayToString(double[] a, int s, int e) {
        if(a == null) {
            return "null";
        }
        StringBuilder b = new StringBuilder();
        b.append('[');
        for(int i = s; i < e; i++) {
            if(i > s) {
                b.append(", ");
            }
            b.append(String.format("%.3f", a[i]));
        }
        b.append(']');
        return b.toString();
    }

    /**
     * Generates a string containing the elements of the array.
     */
    public static final String arrayToString(long[] a, int s, int e) {
        StringBuilder b = new StringBuilder();
        b.append('[');
        for(int i = s; i < e; i++) {
            if(i > s) {
                b.append(", ");
            }
            b.append(a[i]);
        }
        b.append(']');
        return b.toString();
    }

    /**
     * Gets the extension from the given filename.
     *
     * @param file The file we want the extension from.
     * @return The piece of the filename after the last ., or the empty
     * string if there is no such character.
     */
    public static String getExtension(String file) {
        if(file == null) {
            return "";
        }

        int pos = file.lastIndexOf('.');
        if(pos >= 0) {
            return file.substring(pos + 1);
        } else {
            return "";
        }
    }

    /**
     * Intersects two arrays of integers, returning another array.
     * Elements less than 0 are skipped.
     */
    public static int[] intersect(int[] a1, int[] a2) {
        int i1 = 0;
        int i2 = 0;
        int[] ret = new int[Math.min(a1.length, a2.length)];
        int rp = 0;

        while(i1 < a1.length && i2 < a2.length) {
            int d1 = a1[i1];
            if(d1 < 0) {
                i1++;
                continue;
            }
            int d2 = a2[i2];
            if(d2 < 0) {
                i2++;
                continue;
            }

            if(d1 < d2) {
                i1++;
                continue;
            }

            if(d1 > d2) {
                i2++;
                continue;
            }

            ret[rp++] = d1;
            i1++;
            i2++;
        }
        return ret;
    }

    /**
     * Unions two arrays of integers, returning another array.  Elements
     * less than 0 are skipped.
     */
    public static int[] union(int[] a1, int[] a2) {
        int i1 = 0;
        int i2 = 0;
        int[] ret = new int[a1.length + a2.length];
        int rp = 0;

        while(i1 < a1.length && i2 < a2.length) {
            int d1 = a1[i1];
            if(d1 < 0) {
                i1++;
                continue;
            }
            int d2 = a2[i2];
            if(d2 < 0) {
                i2++;
                continue;
            }

            if(d1 < d2) {
                ret[rp++] = d1;
                i1++;
                continue;
            }

            if(d1 > d2) {
                ret[rp++] = d2;
                i2++;
                continue;
            }

            ret[rp++] = d1;
            i1++;
            i2++;
        }

        if(i1 < a1.length) {
            System.arraycopy(a1, i1, ret, rp, a1.length - i1);
        }

        if(i2 < a2.length) {
            System.arraycopy(a2, i2, ret, rp, a2.length - i2);
        }

        return ret;
    }

    /**
     * Maps the asian characters in a string into unicode escapes.  Returns
     * the mapped string, or null if there were no such characters.
     */
    public static String mapAsian(String s) {
        StringBuilder b = new StringBuilder();
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if(UniversalTokenizer.isAsian(c)) {
                b.append("\\u");
                String u = Integer.toHexString(c);
                switch(u.length()) {
                    case 1:
                        b.append("000");
                        break;
                    case 2:
                        b.append("00");
                        break;
                    case 3:
                        b.append("0");
                        break;
                }
                b.append(u);
            } else {
                b.append(c);
            }
        }

        return b.toString();
    }

    /**
     * Encodes <>"'& characters
     * @param s string to be html encoded
     * @param sb string buffer to hold results
     */
    public static StringBuffer htmlEncode(String s, StringBuffer sb) {
        for(int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch(c) {
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '&':
                    sb.append("&amp;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        return sb;
    }

    /**
     * Changes the characters in an array to lower case,
     * destructively.
     *
     * @param c The array we wish to transform
     */
    public static final char[] toLowerCase(char[] c) {
        return toLowerCase(c, 0, c.length);
    }

    /**
     * Changes the characters in a region of an array to lower case,
     * destructively.
     *
     * @param c The array we wish to transform
     * @param b The beginning offset
     * @param e The ending offset
     */
    public static final char[] toLowerCase(char[] c, int b, int e) {
        for(int i = b; i < e; i++) {
            c[i] = CharUtils.toLowerCase[c[i]];
        }
        return c;
    }

    /**
     * Changes the characters in an array to upper case, destructively.
     *
     * @param c The array we wish to transform
     */
    public static final char[] toUpperCase(char[] c) {
        return toUpperCase(c, 0, c.length);
    }

    /**
     * Changes the characters in a region of an array to upper case,
     * destructively.
     *
     * @param c The array we wish to transform
     * @param b The beginning offset
     * @param e The ending offset
     */
    public static final char[] toUpperCase(char[] c, int b, int e) {
        for(int i = b; i < e; i++) {
            c[i] = CharUtils.toUpperCase[c[i]];
        }
        return c;
    }

    /**
     * Gets a string that does not share a common initial substring with
     * another string.
     * @param s1 The first string
     * @param s2 The second string
     * @return The trailing portion of s2 that does not match s1
     */
    public static final String compressInitial(String s1, String s2) {
        int i;
        int l1 = s1.length();
        int l2 = s2.length();
        for(i = 0; i < l1 && i < l2; i++) {
            if(s1.charAt(i) != s2.charAt(i)) {
                break;
            }
        }
        return s2.substring(i);
    }

    /**
     * Finds the common prefix length between two arrays of characters.
     * Returns the number of characters that the two arrays share.
     *
     * @param c1 The first array
     * @param c2 The second array
     * @return The number of initial characters that the two arrays share.
     */
    public static final int findInitial(char[] c1, char[] c2) {
        int end = Math.min(c1.length, c2.length);
        for(int i = 0; i < end; i++) {
            if(c1[i] != c2[i]) {
                return i;
            }
        }
        return end;
    }

    /**
     * finds the common prefix length between two sections of two arrays of
     * characters.  Returns the number of characters in the initial shared
     * prefix.  c1 and c2 may point to the same array.
     *
     * @param c1 The first array of characters.
     * @param b1 The starting index of the first section.
     * @param e1 The exclusive index of the end of the first section (i.e.,
     * we check up to, but not including this character.)
     * @param c2 The second array of characters.
     * @param b2 The starting index of the second section.
     * @param e2 The exclusive index of the end of the second section.
     */
    public static final int findInitial(char[] c1, int b1, int e1,
            char[] c2, int b2, int e2) {
        int end = Math.min(e1 - b1, e2 - b2);
        for(int i = 0, i1 = b1, i2 = b2; i < end; i++, i1++, i2++) {
            if(c1[i1] != c2[i2]) {
                return i;
            }
        }
        return end;
    }

    /**
     * Compares two arrays of characters, as in the <code>String</code>
     * compareTo function.
     *
     * @param c1 The first array of characters.
     * @param c2 The second array of characters.
     */
    public static final int compareArray(char[] c1, char[] c2) {
        return compareArray(c1, 0, c1.length, c2, 0, c2.length);
    }

    /**
     * Compares two arrays of characters, as in the <code>String</code>
     * compareTo function.
     *
     * @param c1 The first array of characters.
     * @param b1 The beginning offset for comparison
     * @param e1 The ending offset for comparison
     * @param c2 The second array of characters.
     * @param b2 The beginning offset for comparison
     * @param e2 The ending offset for comparison
     * @return 0 if the arrays are equal, less than 0 if c1 is
     * lexicographically smaller than c2, or greater than 0 if c1 is
     * lexicographically greater than c2.
     */
    public static final int compareArray(char[] c1, int b1, int e1,
            char[] c2, int b2, int e2) {

        int n = Math.min(e1 - b1, e2 - b2);
        for(int i = 0, i1 = b1, i2 = b2; i < n; i++, i1++, i2++) {
            char a = c1[i1];
            char b = c2[i2];
            if(a != b) {
                return a - b;
            }
        }
        return e1 - b1 - (e2 - b2);
    }

    /**
     * Compares two arrays of characters in a case-insensitive manner.
     *
     * @param c1 The first array of characters.
     * @param b1 The beginning offset for comparison
     * @param e1 The ending offset for comparison
     * @param c2 The second array of characters.
     * @param b2 The beginning offset for comparison
     * @param e2 The ending offset for comparison
     * @return 0 if the arrays are equal, less than 0 if c1 is
     * lexicographically smaller than c2, or greater than 0 if c1 is
     * lexicographically greater than c2.
     */
    public static final int compareArrayCI(char[] c1, int b1, int e1,
            char[] c2, int b2, int e2) {
        int n = Math.min(e1 - b1, e2 - b2);
        for(int i = 0, i1 = b1, i2 = b2; i < n; i++, i1++, i2++) {
            char a = CharUtils.toLowerCase[c1[i1]];
            char b = CharUtils.toLowerCase[c2[i2]];
            if(a != b) {
                return a - b;
            }
        }
        return e1 - b1 - (e2 - b2);
    }

    /**
     * Tests whether a given array of characters starts with another.
     * We're checking if <code>c1</code> starts with <code>c2</code>.
     *
     * @param c1 The first array of characters.
     * @param b1 The beginning offset for comparison
     * @param e1 The ending offset for comparison
     * @param c2 The second array of characters.
     * @param b2 The beginning offset for comparison
     * @param e2 The ending offset for comparison
     * @param ci Whether the comparison should be case insensitive.
     * @return true if <code>c1</code> starts with the characters in
     * <code>c2</code>.
     */
    public static final boolean startsWith(char[] c1, int b1, int e1,
            char[] c2, int b2, int e2,
            boolean ci) {

        int l1 = e1 - b1;
        int l2 = e2 - b2;

        //
        // If c2 is longer than c1, we're done.
        if(l2 > l1) {
            return false;
        }

        char a, b;

        //
        // Now walk the two arrays.
        for(int i = 0, i1 = b1, i2 = b2; i < l2; i++, i1++, i2++) {
            a = ci ? CharUtils.toLowerCase[c1[i1]] : c1[i1];
            b = ci ? CharUtils.toLowerCase[c2[i2]] : c2[i2];
            if(a != b) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether a given array of characters ends with another.  We're
     * checking if <code>c1</code> ends with <code>c2</code>.
     *
     * @param c1 The first array of characters.
     * @param b1 The beginning offset for comparison
     * @param e1 The ending offset for comparison
     * @param c2 The second array of characters.
     * @param b2 The beginning offset for comparison
     * @param e2 The ending offset for comparison
     * @param ci Whether the comparison should be case insensitive.
     * @return true if <code>c1</code> ends with the characters in
     * <code>c2</code>.
     */
    public static final boolean endsWith(char[] c1, int b1, int e1,
            char[] c2, int b2, int e2,
            boolean ci) {

        int l1 = e1 - b1;
        int l2 = e2 - b2;

        //
        // If c2 is longer than c1, we're done.
        if(l2 > l1) {
            return false;
        }

        char a, b;

        //
        // Now walk the two arrays.
        for(int i = l2, i1 = e1 - 1, i2 = e2 - 1; i > 0; i--, i1--, i2--) {
            a = ci ? CharUtils.toLowerCase[c1[i1]] : c1[i1];
            b = ci ? CharUtils.toLowerCase[c2[i2]] : c2[i2];
            if(a != b) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether a given array of characters is contained in another.
     * We're checking if <code>c1</code> contains <code>c2</code>.
     *
     * @param c1 The first array of characters.
     * @param b1 The beginning offset for comparison
     * @param e1 The ending offset for comparison
     * @param c2 The second array of characters.
     * @param b2 The beginning offset for comparison
     * @param e2 The ending offset for comparison
     * @param ci Whether the comparison should be case insensitive.
     * @return The offset from b1 where <code>c2</code> is found in
     * <code>c1</code>, or -1 if it is not found.
     */
    public static final int indexOf(char[] c1, int b1, int e1,
            char[] c2, int b2, int e2,
            boolean ci) {

        int l1 = e1 - b1;
        int l2 = e2 - b2;

        //
        // If c2 is longer than c1, we're done.
        if(l2 > l1) {
            return -1;
        }

        //
        // We need the first character.
        char first = ci ? CharUtils.toLowerCase[c2[b2]] : c2[b2];
        char c;

        //
        // Loop through c1 looking for the first character.  This is a bit
        // inefficient, but shouldn't be a lot worse than String's indexOf
        // method, and we don't incur the String creation overhead.
        for(int i = b1; i < e1; i++) {
            c = ci ? CharUtils.toLowerCase[c1[i]] : c1[i];

            if(c == first) {
                if(equalToN(c1, i, e1, c2, b2, e2, l2, ci)) {
                    return i - b1;
                }
            }
        }
        return -1;
    }

    /**
     * Tests whether <em>n</em> characters of  two arrays are equal.
     *
     * @param c1 The first array of characters
     * @param b1 The beginning offset for comparison
     * @param e1 The ending offset for comparison
     * @param c2 The second array of characters
     * @param b2 The beginning offset for comparison
     * @param e2 The ending offset for comparison
     * @param n The number of characters to compare
     * @param ci Whether the comparison should be case insensitive.
     * @return true if <code>c1</code> is equal to <code>c2</code>.
     */
    public static final boolean equalToN(char[] c1, int b1, int e1,
            char[] c2, int b2, int e2,
            int n, boolean ci) {

        int l1 = e1 - b1;
        int l2 = e2 - b2;

        //
        // If they're different lengths, we're done.
        if(n > l1 || n > l2) {
            return false;
        }

        char a, b;

        //
        // Now walk the two arrays.
        for(int i = 0, i1 = b1, i2 = b2; i < n; i++, i1++, i2++) {
            a = ci ? CharUtils.toLowerCase[c1[i1]] : c1[i1];
            b = ci ? CharUtils.toLowerCase[c2[i2]] : c2[i2];
            if(a != b) {
                return false;
            }
        }
        return true;
    }

    /**
     * Tests whether a two arrays of characters are equal.
     *
     * @param c1 The first array of characters.
     * @param b1 The beginning offset for comparison
     * @param e1 The ending offset for comparison
     * @param c2 The second array of characters.
     * @param b2 The beginning offset for comparison
     * @param e2 The ending offset for comparison
     * @param ci Whether the comparison should be case insensitive.
     * @return true if <code>c1</code> is equal to <code>c2</code>.
     */
    public static final boolean equalTo(char[] c1, int b1, int e1,
            char[] c2, int b2, int e2,
            boolean ci) {

        int l1 = e1 - b1;
        int l2 = e2 - b2;

        //
        // If they're different lengths, we're done.
        if(l2 != l1) {
            return false;
        }

        char a, b;

        //
        // Now walk the two arrays.
        for(int i = 0, i1 = b1, i2 = b2; i < l2; i++, i1++, i2++) {
            a = ci ? CharUtils.toLowerCase[c1[i1]] : c1[i1];
            b = ci ? CharUtils.toLowerCase[c2[i2]] : c2[i2];
            if(a != b) {
                return false;
            }
        }
        return true;
    }

    /**
     * Compares two arrays of characters so that any cased variants of a
     * string come directly after the lowercase version of a string.  This
     * is almost the same function as the <code>ConceptInfo</code>
     * <code>compareTo</code> method.
     *
     * @param a1 The first array of characters.
     * @param b1 The beginning offset for comparison
     * @param e1 The ending offset for comparison
     * @param a2 The second array of characters.
     * @param b2 The beginning offset for comparison
     * @param e2 The ending offset for comparison
     * @return 0 if the arrays are equal, less than 0 if c1 is less than
     * c2, or greater than 0 if c1 is greater than c2.
     */
    public static final int caseCompareArray(char[] a1, int b1, int e1,
            char[] a2, int b2, int e2) {
        return caseCompareArray(a1, b1, e1, a2, b2, e2, false);
    }

    /**
     * Compares two arrays of characters so that any cased variants of a
     * string come directly after the lowercase version of a string.  This
     * is almost the same function as the <code>ConceptInfo</code>
     * <code>compareTo</code> method.
     *
     * @param a1 The first array of characters.
     * @param b1 The beginning offset for comparison
     * @param e1 The ending offset for comparison
     * @param a2 The second array of characters.
     * @param b2 The beginning offset for comparison
     * @param e2 The ending offset for comparison
     * @param ignoreLength Whether we should ignore the length of the
     * second string when deciding if the strings are equal or not.  This
     * is needed for the stem finder.
     * @return 0 if the arrays are equal, less than 0 if c1 is less than
     * c2, or greater than 0 if c1 is greater than c2.
     */
    public static final int caseCompareArray(char[] a1, int b1, int e1,
            char[] a2, int b2, int e2,
            boolean ignoreLength) {

        //
        // Now compare the strings case-insensitively.
        int caseCmp = 0;
        char c1, c2, lc1, lc2;
        int l1 = e1 - b1;
        int l2 = e2 - b2;
        int e = Math.min(l1, l2);
        int i, i1, i2;

        //
        // Loop to the end of the shortest string.
        for(i = 0, i1 = b1, i2 = b2; i < e; i++, i1++, i2++) {

            c1 = a1[i1];
            c2 = a2[i2];

            //
            // Catch a character difference...
            if(c1 != c2) {

                //
                // ... which we ignore for now if it's only a case
                // difference, and only if it's the first case difference
                // that we've found.
                lc1 = CharUtils.toLowerCase[c1];
                lc2 = CharUtils.toLowerCase[c2];

                //
                // The lower case versions are the same, and we haven't
                // found a case difference so far.
                if(lc1 == lc2) {
                    if(caseCmp == 0) {
                        if(c1 == lc1) {
                            caseCmp = -1;
                        } else if(c2 == lc2) {
                            caseCmp = 1;
                        } else {
                            caseCmp = c1 - c2;
                        }
                    }
                } else {

                    //
                    // This really a different character, so return the
                    // difference.
                    return lc1 - lc2;
                }
            }
        }

        //
        // If the lengths are the same, then we return the result due to
        // the first case difference that we found.
        if(l1 == l2 || (ignoreLength && i == e && e == l1)) {
            return caseCmp;
        }

        //
        // The shortest is the earliest.
        return l1 - l2;
    }

    /**
     * Sorts an array of <CODE>Object</CODE> using heap sort.
     * @param x The array to sort.
     */
    public static final <T> T[] sort(T x[]) {
        return sort(x, 0, x.length);
    }

    /**
     * Sorts the specified sub-array of <code>Object</code>s into ascending
     * order.  We're using a heap sort to avoing stack overflow for very large
     * arrays.
     * @param x The array containing the subarray that we wish to sort.
     * @param off The offset of the subarray that we wish to sort.
     * @param len The length of the subarray that we wish to sort.
     */
    public static final <T> T[] sort(T x[], int off, int len) {
        PriorityQueue<T> pq = new PriorityQueue<T>(len - off + 1);
        for(int i = off; i < len; i++) {
            pq.offer(x[i]);
        }
        for(int i = off; i < len; i++) {
            x[i] = pq.poll();
        }
        return x;
    }

    public static final <T> T[] sort(T x[], Comparator<T> comp) {
        return sort(x, 0, x.length, comp);
    }

    /**
     * Sorts the specified sub-array of <code>Object</code>s into ascending
     * order.  This is the tuned quicksort from
     * <code>java.util.Arrays</code>.  We're using it in favour of the
     * <code>Arrays.sort</code> method because that wants to clone the
     * array being sorted and our arrays are very big.
     */
    public static final <T> T[] sort(T x[], int off, int len,
            Comparator<T> cm) {
        PriorityQueue<T> pq = new PriorityQueue<T>(len - off + 1, cm);
        for(int i = off; i < len; i++) {
            pq.offer(x[i]);
        }
        for(int i = off; i < len; i++) {
            x[i] = pq.poll();
        }
        return x;
    }

    /**
     * Matches the provided wildcard pattern against the given term.
     * Wildcard patterns are made up of characters and the following
     * special symbols:
     *
     * <dl>
     * <dt>*</dt><dd>Matches zero or more characters</dd>
     * <dt>?</dt><dd>Matches a single character</dd>
     * </dl>
     *
     * @param pattern The wildcard pattern.
     * @param term The term to match against.
     * @return true if the term matches the pattern, false otherwise.
     */
    public static final boolean match(String pattern, String term) {
        return match(pattern.toCharArray(), term.toCharArray(),
                term.length());
    }

    /**
     * Matches the provided pattern against the given term.
     * @param pattern The wildcard pattern.
     * @param term The term to match against.
     * @param caseSensitive parameter that causes case-sensitive match
     *        only when true (the default when this parameter is not
     *        supplied will be case-sensitive matching).
     * @return true if the term matches the pattern, false otherwise.
     */
    public static final boolean match(String pattern, String term,
            boolean caseSensitive) {
        if(caseSensitive) {
            return match(pattern.toCharArray(), term.toCharArray(),
                    term.length());
        } else {
            char[] patternArray = pattern.toCharArray();
            char[] termArray = term.toCharArray();
            toLowerCase(patternArray);
            toLowerCase(termArray);
            return match(patternArray, termArray,
                    term.length());
        }
    }

    /**
     * Matches the provided pattern against the given term.
     * @param pattern The wildcard pattern.  If caseSensitive is false,
     * this <em>must</em> be lowercased before it is passed in!
     * @param term The term to match against.
     * @param caseSensitive parameter that causes case-sensitive match
     *        only when true (the default when this parameter is not
     *        supplied will be case-sensitive matching).
     * @return true if the term matches the pattern, false otherwise.
     */
    public static final boolean match(char[] pattern, String term,
            boolean caseSensitive) {
        if(caseSensitive) {
            return match(pattern,
                    term.toCharArray(),
                    term.length());
        } else {
            char[] termArray = toLowerCase(term.toCharArray());
            return match(pattern, termArray, term.length());
        }
    }

    /**
     * Matches the provided wildcard pattern against the given term.
     * Wildcard patterns are made up of characters and the following
     * special symbols:
     *
     * <dl>
     * <dt>*</dt><dd>Matches zero or more characters</dd>
     * <dt>?</dt><dd>Matches a single character</dd>
     * </dl>
     *
     * @param pattern The wildcard pattern.
     * @param term The term to match against.
     * @return true if the term matches the pattern, false otherwise.
     */
    public static final boolean match(char[] pattern, char[] term) {
        return match(pattern, term, term.length);
    }

    /**
     * Matches the provided pattern against the given term.
     * @param pattern The wildcard pattern.
     * @param term The term to match against.
     * @param caseSensitive parameter that causes case-sensitive match
     *        only when true (the default when this parameter is not
     *        supplied will be case-sensitive matching).
     * @return true if the term matches the pattern, false otherwise.
     */
    public static final boolean match(char[] pattern,
            char[] term,
            boolean caseSensitive) {
        if(caseSensitive) {
            return match(pattern, term, term.length);
        } else {
            char[] patternArray = pattern.clone();
            char[] termArray = term.clone();
            toLowerCase(patternArray);
            toLowerCase(termArray);
            return match(patternArray, termArray,
                    term.length);
        }
    }

    /**
     * Matches the provided pattern against the given term.
     * @param pattern The wildcard pattern.
     * @param term The term to match against.
     * @param caseSensitive parameter that causes case-sensitive match
     *        only when true (the default when this parameter is not
     *        supplied will be case-sensitive matching).
     * @param length The length of the term.
     * @return true if the term matches the pattern, false otherwise.
     */
    public static final boolean match(char[] pattern,
            char[] term,
            boolean caseSensitive,
            int length) {
        if(caseSensitive) {
            return match(pattern, term, length);
        } else {
            char[] patternArray = pattern.clone();
            char[] termArray = term.clone();
            toLowerCase(patternArray);
            toLowerCase(termArray);
            return match(patternArray, termArray,
                    length);
        }
    }

    /**
     * Matches the provided pattern against the given term.
     * @param pattern The wildcard pattern.
     * @param term The term to match against.
     * @param caseSensitive parameter that causes case-sensitive match
     *        only when true (the default when this parameter is not
     *        supplied will be case-sensitive matching).
     * @param begin The starting offset to begin to match in the term array.
     * @param end The offset beyond the end of the region of term to match.
     * @return true if the term matches the pattern, false otherwise.
     */
    public static final boolean match(char[] pattern,
            char[] term,
            boolean caseSensitive,
            int begin,
            int end) {
        int length = end - begin;
        char[] termArray = new char[length];
        System.arraycopy(term, begin, termArray, 0, length);
        if(caseSensitive) {
            return match(pattern, termArray, length);
        } else {
            char[] patternArray = pattern.clone();
            toLowerCase(patternArray);
            toLowerCase(termArray);
            return match(patternArray, termArray, length);
        }
    }

    /**
     * Matches the provided pattern against the given term.
     * @param pattern The wildcard pattern.
     * @param term The term to match against.
     * @param length The length of the term.
     * @return true if the term matches the pattern, false otherwise.
     */
    public static final boolean match(char[] pattern,
            char[] term,
            int length) {
        int[] state = null;
        ArrayList<int[]> alts = new ArrayList<int[]>();
        HashMap<Long, Long> doneStates = new HashMap<Long, Long>();
        Long stateCode;
        int position = length - 1;
        int i = pattern.length - 1;
        boolean matched = true;

        while((matched && ((i > -1)) ||
                ((position > -1) && alts.size() > 0)) ||
                (!matched && alts.size() > 0)) {
            if((i < 0) || !matched || (position < 0)) {
                if((position < 0) && matched && (i > -1) &&
                        (pattern[i] == '*')) {
                    //go on and try these pattern cases
                } else if(alts.size() > 0) {
                    int last = alts.size() - 1;
                    state = alts.get(last);
                    alts.remove(last);
                    position = state[0];
                    //
                    // this is >=0 because altState only saves when i > -1
                    i = state[1];
                    matched = true;
                } else {
                    matched = false;
                    break;
                }
            }

            //
            // stateCodes are only constructed for position and i >= 0
            if(position > -1) {
                stateCode = new Long(((long) position << 32) |
                        ((long) i));
                if(doneStates.get(stateCode) == null) {
                    //
                    // block alts for states already seen
                    doneStates.put(stateCode, stateCode);
                }
            }

            //try to match pattern element:

            //check left end conditions
            if(position < 0) {
                if(pattern[i] == '*') {
                    i--;
                } else {
                    matched = false;
                }
            } else if(pattern[i] == '*') {
                //
                //"*" pattern can match any number of times (including zero)

                if(i == 0) {
                    break;
                } else {
                    char prev = pattern[i - 1];
                    if(prev == '?') { //reorder this and match the ?
                        pattern[i] = prev;
                        pattern[i - 1] = '*';
                        position--;
                        i--;
                    } else if(prev == '*') {
                        //
                        // Shouldn't get this, but deal with it
                        i--;
                    } else if(prev == term[position]) {
                        //save alt to skip anyway
                        altState(position - 1, i, alts, doneStates);
                        position--;
                        //we have already matched pattern[i-1]
                        i = i - 2;
                    } else {
                        //move left in word and keep looking
                        position--;
                    }
                }
            } else if(pattern[i] == '?') {

                //"?" pattern matches any single character
                position--;
                i--;
            } else if(pattern[i] != term[position]) {
                //match pattern characters against input character
                // doesn't match here
                matched = false;
            } else if((i == 0) && (pattern[0] != '*') &&
                    (position > 0)) {
                // otherwise, character matches pattern -- decide what to
                // do next
                matched = false;
            } else if((i == 0) && matched &&
                    ((pattern[0] == '*') || (position <= 0))) {
                break; //match is true
            } else {
                // Character matches pattern
                i--;
                position--;
            }
        }
        if(matched && (pattern[0] != '*') &&
                ((position > 0) || (position > i))) {
            matched = false;
        }
        return matched;
    }

    private static void altState(int position, int i,
            ArrayList<int[]> altStates,
            HashMap<Long, Long> doneStates) {
        Long stateCode = new Long(((long) position << 32) |
                ((long) i));
        //only need stateCodes for position and i >= 0
        //only add an altState if haven't already done
        //or already visited that state
        if((position > -1) && (i > -1) &&
                (doneStates.get(stateCode) == null)) {
            doneStates.put(stateCode, stateCode);
            int[] state = new int[2];
            state[0] = position;
            state[1] = i;
            altStates.add(state);
        }
    }

    /**
     * Matches a given word against a target stem and returns a stem match
     * score.  This score indicates the likelihood that the given word has
     * the target as a stem.
     *
     * @param wordString The word to check.
     * @param target The stem to match against.
     * @return The stem match score, as a double between 0 and 1.  The
     * score is the proportion of the length of the initial substring that
     * the word and the target share to the maximum length.
     */
    public static final double matchStem(String wordString, String target) {
        return matchStem(wordString.toCharArray(),
                0, wordString.length(),
                target.toCharArray(),
                0, target.length());
    }

    /**
     * Matches a given word against a target stem and returns a stem match
     * score.  This score indicates the likelihood that the given word has
     * the target as a stem.
     *
     * @param wordString The word to check.
     * @param target The stem to match against.
     * @return The stem match score, as a double between 0 and 1.  The
     * score is the proportion of the length of the initial substring that
     * the word and the target share to the maximum length.
     */
    public static final double matchStem(char[] wordString, int b1, int e1,
            char[] target, int b2, int e2) {

        int initial = findInitial(wordString, b1, e1, target, b2, e2);
        int maxLength = Math.max(e1 - b1, e2 - b2);
        return (double) initial / (double) maxLength;
    }

    /**
     * Calculate the Levenshtein edit distance between two strings.
     *
     * @param str1 the base string
     * @param str2 the string to compare to
     * @return the number of edits required to change str1 into str2
     */
    public static int levenshteinDistance(String str1, String str2) {
        int d[][] = new int[str1.length() + 1][str2.length() + 1];

        for(int i = 0; i <= str1.length(); i++) {
            d[i][0] = i;
        }
        for(int j = 0; j <= str2.length(); j++) {
            d[0][j] = j;
        }

        int cost;
        for(int i = 1; i <= str1.length(); i++) {
            for(int j = 1; j <= str2.length(); j++) {
                if(str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    cost = 0;
                } else {
                    cost = 1;
                }

                int a1 = d[i - 1][j] + 1;
                int a2 = d[i][j - 1] + 1;
                int a3 = d[i - 1][j - 1] + cost;

                d[i][j] = java.lang.Math.min(a1, java.lang.Math.min(a2, a3));
            }
        }

        return d[str1.length()][str2.length()];
    }

    /**
     * Makes a string containing the list.
     */
    public static final String listToString(List l) {
        StringBuilder b = new StringBuilder();
        b.append("[");
        synchronized(l) {
            boolean first = true;
            Iterator i = l.iterator();
            while(i.hasNext()) {
                if(!first) {
                    b.append(", ");
                }
                b.append(i.next());
                first = false;
            }
        }
        b.append("]");
        return b.toString();
    }

    /**
     * Do a binary search on a region of an array.  A curious omission
     * from java.util.Arrays.
     *
     * @param a The array to be searched
     * @param low The low end of the region to be searched
     * @param high The high end of the region to be searched
     * @param key The value that we're searching for
     * @return index of the search key, if it is contained in the list;
     *	       otherwise, <tt>(-(<i>insertion point</i>) - 1)</tt>.  The
     *	       <i>insertion point</i> is defined as the point at which the
     *	       key would be inserted into the list: the index of the first
     *	       element greater than the key, or <tt>list.size()</tt>, if all
     *	       elements in the list are less than the specified key.  Note
     *	       that this guarantees that the return value will be &gt;= 0 if
     *	       and only if the key is found.
     */
    public static int binarySearch(int[] a, int low, int high, int key) {
        while(low <= high) {
            int mid = (low + high) / 2;
            int midVal = a[mid];
            if(midVal < key) {
                low = mid + 1;
            } else if(midVal > key) {
                high = mid - 1;
            } else {
                return mid; // key found
            } // key found
        }
        return -(low + 1);  // key not found.
    }

    /**
     * Does a tandem sort of two arrays:  one of <code>int</code> one of
     * <code>float</code>.
     */
    public static void sort(int x[], float y[], int off, int len) {
        // Insertion sort on smallest arrays
        if(len < 7) {
            for(int i = off; i < len + off; i++) {
                for(int j = i; j > off && x[j - 1] > x[j];
                        j--) {
                    swap(x, y, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + len / 2;       // Small arrays, middle element
        if(len > 7) {
            int l = off;
            int n = off + len - 1;
            if(len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        int v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while(true) {
            while(b <= c && x[b] <= v) {
                if(x[b] == v) {
                    swap(x, y, a++, b);
                }
                b++;
            }
            while(c >= b && x[c] >= v) {
                if(x[c] == v) {
                    swap(x, y, c, d--);
                }
                c--;
            }
            if(b > c) {
                break;
            }
            swap(x, y, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, y, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, y, b, n - s, s);

        // Recursively sort non-partition-elements
        if((s = b - a) > 1) {
            sort(x, y, off, s);
        }
        if((s = d - c) > 1) {
            sort(x, y, n - s, s);
        }
    }

    /**
     * Swaps x[a] with x[b] and y[a] with y[b].
     */
    private static void swap(int x[], float[] y, int a, int b) {
        int t = x[a];
        x[a] = x[b];
        x[b] = t;

        float tf = y[a];
        y[a] = y[b];
        y[b] = tf;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(int x[], float[] y, int a, int b, int n) {
        int t;
        float tf;
        for(int i = 0; i < n; i++, a++, b++) {
            t = x[a];
            x[a] = x[b];
            x[b] = t;

            tf = y[a];
            y[a] = y[b];
            y[b] = tf;
        }
    }

    /**
     * Returns the index of the median of the three indexed integers.
     */
    public static int med3(int x[], int a, int b, int c) {
        return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] >
                x[c] ? b : x[a] > x[c] ? c : a));
    }

    /**
     * Does a tandem sort of two arrays:  one of <code>int</code> one of
     * <code>float</code>.
     */
    public static void sort(int x[], int y[], int off, int len) {
        // Insertion sort on smallest arrays
        if(len < 7) {
            for(int i = off; i < len + off; i++) {
                for(int j = i; j > off && x[j - 1] > x[j];
                        j--) {
                    swap(x, y, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + len / 2;       // Small arrays, middle element
        if(len > 7) {
            int l = off;
            int n = off + len - 1;
            if(len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        int v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while(true) {
            while(b <= c && x[b] <= v) {
                if(x[b] == v) {
                    swap(x, y, a++, b);
                }
                b++;
            }
            while(c >= b && x[c] >= v) {
                if(x[c] == v) {
                    swap(x, y, c, d--);
                }
                c--;
            }
            if(b > c) {
                break;
            }
            swap(x, y, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, y, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, y, b, n - s, s);

        // Recursively sort non-partition-elements
        if((s = b - a) > 1) {
            sort(x, y, off, s);
        }
        if((s = d - c) > 1) {
            sort(x, y, n - s, s);
        }
    }

    /**
     * Swaps x[a] with x[b] and y[a] with y[b].
     */
    private static void swap(int x[], int[] y, int a, int b) {
        int t = x[a];
        x[a] = x[b];
        x[b] = t;

        t = y[a];
        y[a] = y[b];
        y[b] = t;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(int x[], int[] y, int a, int b, int n) {
        int t;
        for(int i = 0; i < n; i++, a++, b++) {
            t = x[a];
            x[a] = x[b];
            x[b] = t;

            t = y[a];
            y[a] = y[b];
            y[b] = t;
        }
    }

    /**
     * Does a tandem sort of three arrays: one of <code>int</code> two of
     * <code>float</code>.
     */
    public static void sort(int x[], float y[], float z[], int off, int len) {
        // Insertion sort on smallest arrays
        if(len < 7) {
            for(int i = off; i < len + off; i++) {
                for(int j = i; j > off && x[j - 1] > x[j];
                        j--) {
                    swap(x, y, z, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + len / 2;       // Small arrays, middle element
        if(len > 7) {
            int l = off;
            int n = off + len - 1;
            if(len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        int v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while(true) {
            while(b <= c && x[b] <= v) {
                if(x[b] == v) {
                    swap(x, y, z, a++, b);
                }
                b++;
            }
            while(c >= b && x[c] >= v) {
                if(x[c] == v) {
                    swap(x, y, z, c, d--);
                }
                c--;
            }
            if(b > c) {
                break;
            }
            swap(x, y, z, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, y, z, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, y, z, b, n - s, s);

        // Recursively sort non-partition-elements
        if((s = b - a) > 1) {
            sort(x, y, z, off, s);
        }
        if((s = d - c) > 1) {
            sort(x, y, z, n - s, s);
        }
    }

    /**
     * Swaps x[a] with x[b] and y[a] with y[b].
     */
    private static void swap(int x[], float[] y, float[] z, int a, int b) {
        int t = x[a];
        x[a] = x[b];
        x[b] = t;

        float tf = y[a];
        y[a] = y[b];
        y[b] = tf;

        tf = z[a];
        z[a] = z[b];
        z[b] = tf;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(int x[], float[] y, float[] z,
            int a, int b, int n) {
        int t;
        float tf;
        for(int i = 0; i < n; i++, a++, b++) {
            t = x[a];
            x[a] = x[b];
            x[b] = t;

            tf = y[a];
            y[a] = y[b];
            y[b] = tf;

            tf = z[a];
            z[a] = z[b];
            z[b] = tf;
        }
    }

    /**
     * Does a tandem sort of two arrays:  one of <code>float</code> one of
     * <code>int</code>.  Comparison is made by considering the floats
     * only: the ints are along for the ride.
     */
    public static void sort(float x[], int y[], int off, int len) {
        // Insertion sort on smallest arrays
        if(len < 7) {
            for(int i = off; i < len + off; i++) {
                for(int j = i; j > off && x[j - 1] > x[j];
                        j--) {
                    swap(x, y, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + len / 2;       // Small arrays, middle element
        if(len > 7) {
            int l = off;
            int n = off + len - 1;
            if(len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        float v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while(true) {
            while(b <= c && x[b] <= v) {
                if(x[b] == v) {
                    swap(x, y, a++, b);
                }
                b++;
            }
            while(c >= b && x[c] >= v) {
                if(x[c] == v) {
                    swap(x, y, c, d--);
                }
                c--;
            }
            if(b > c) {
                break;
            }
            swap(x, y, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, y, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, y, b, n - s, s);

        // Recursively sort non-partition-elements
        if((s = b - a) > 1) {
            sort(x, y, off, s);
        }
        if((s = d - c) > 1) {
            sort(x, y, n - s, s);
        }
    }

    /**
     * Swaps x[a] with x[b] and y[a] with y[b].
     */
    private static void swap(float x[], int[] y, int a, int b) {
        float t = x[a];
        x[a] = x[b];
        x[b] = t;

        int tf = y[a];
        y[a] = y[b];
        y[b] = tf;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(float x[], int y[], int a, int b, int n) {
        for(int i = 0; i < n; i++, a++, b++) {
            float t = x[a];
            x[a] = x[b];
            x[b] = t;

            int tf = y[a];
            y[a] = y[b];
            y[b] = tf;
        }
    }

    /**
     * Returns the index of the median of the three indexed integers.
     */
    public static int med3(float x[], int a, int b, int c) {
        return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] >
                x[c] ? b : x[a] > x[c] ? c : a));
    }

    /**
     * Does a tandem sort of two arrays:  one of <code>double</code> one of
     * <code>int</code>.  Comparison is made by considering the floats
     * only: the ints are along for the ride.
     */
    public static void sort(double x[], int y[], int off, int len) {
        // Insertion sort on smallest arrays
        if(len < 7) {
            for(int i = off; i < len + off; i++) {
                for(int j = i; j > off && x[j - 1] > x[j];
                        j--) {
                    swap(x, y, j, j - 1);
                }
            }
            return;
        }

        // Choose a partition element, v
        int m = off + len / 2;       // Small arrays, middle element
        if(len > 7) {
            int l = off;
            int n = off + len - 1;
            if(len > 40) {        // Big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // Mid-size, med of 3
        }
        double v = x[m];

        // Establish Invariant: v* (<v)* (>v)* v*
        int a = off, b = a, c = off + len - 1, d = c;
        while(true) {
            while(b <= c && x[b] <= v) {
                if(x[b] == v) {
                    swap(x, y, a++, b);
                }
                b++;
            }
            while(c >= b && x[c] >= v) {
                if(x[c] == v) {
                    swap(x, y, c, d--);
                }
                c--;
            }
            if(b > c) {
                break;
            }
            swap(x, y, b++, c--);
        }

        // Swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, y, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, y, b, n - s, s);

        // Recursively sort non-partition-elements
        if((s = b - a) > 1) {
            sort(x, y, off, s);
        }
        if((s = d - c) > 1) {
            sort(x, y, n - s, s);
        }
    }

    /**
     * Swaps x[a] with x[b] and y[a] with y[b].
     */
    private static void swap(double x[], int[] y, int a, int b) {
        double t = x[a];
        x[a] = x[b];
        x[b] = t;

        int tf = y[a];
        y[a] = y[b];
        y[b] = tf;
    }

    /**
     * Swaps x[a .. (a+n-1)] with x[b .. (b+n-1)].
     */
    private static void vecswap(double x[], int y[], int a, int b, int n) {
        for(int i = 0; i < n; i++, a++, b++) {
            double t = x[a];
            x[a] = x[b];
            x[b] = t;

            int tf = y[a];
            y[a] = y[b];
            y[b] = tf;
        }
    }

    /**
     * Returns the index of the median of the three indexed integers.
     */
    public static int med3(double x[], int a, int b, int c) {
        return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a) : (x[b] >
                x[c] ? b : x[a] > x[c] ? c : a));
    }
} // Util
