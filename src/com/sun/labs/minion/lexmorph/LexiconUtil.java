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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

import java.util.concurrent.ConcurrentHashMap;

import com.sun.labs.minion.util.BitBuffer;
import java.util.logging.Logger;

/**
 * This class <em>ONLY</em> contains static utility methods that have been moved
 * from the original Lexicon class.
 * 
 * @author bh37721
 *
 */
public class LexiconUtil {

    static Logger logger = Logger.getLogger(LexiconUtil.class.getName());

    public static Word[] valueWords(Value val) { // pmartin 23sep02
        if(val == null) {
            return null;
        } else if(val instanceof Word) {
            return new Word[]{(Word) val};
        } else if(val instanceof List) {
            return ((List) val).wordsIn();
        } else {
            return null;
        }
    }

    // static method for testing intersection of a value (maybe list) with array:
    //pmartin 12july01
    public static boolean valueArrayIntersectp(Value val1, Value[] list2) {
        if((val1 == null) || (list2 == null)) {
            return false;
        } else if(val1.listp()) {
            return intersectp(((List) val1).contents, list2);
        } else {
            return (memb(val1, list2) > 0);
        }
    }

    // static method to iterate toString over value arrays
    public static String toStringArray(Value[] array) { //pmartin 20sep99
        if(array == null) {
            return null;
        }
        String astr = "[";
        for(int i = 0; i < array.length; i++) {
            astr = astr + " " + array[i].toString();
        }
        return (astr + " ]");
    }

    public static boolean stringInArrayP(String str, String[] array) {
        if((str != null) && (str.length() != 0) && (array != null)) {
            for(int i = 0; i < array.length; i++) {
                if(array[i].equals(str)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     *  quick way to decide a string MIGHT be a number
     */
    public static final boolean startsLikeNumber(String str) {
        if(str == null) {
            return false;
        }
        int len = str.length();
        if(len < 1) {
            return false;
        }
        char ch = str.charAt(0);
        if(isDigit(ch)) {
            return true;
        } else {
            return ((ch == '.') &&
                    (len > 1) && (isDigit(str.charAt(1))));
        }
    }

    public static String senseNameWordString(String str) { // pmartin 23sep02
        String nstr = "";
        int i, j;
        if((str.length() > 3) &&
                (str.charAt(0) == '!') &&
                ((i = str.indexOf("/")) > 1)) {
            j = str.indexOf("/", i + 1);
            if(j < 0) {
                j = str.length();
            }
            nstr = str.substring(i + 1, j);
        }
        return nstr;
    }

    public static boolean senseNameStringp(String str) { // pmartin 7sep01
        return ((str != null) &&
                (str.length() > 3 &&
                str.charAt(0) == '!') &&
                (str.indexOf("/") >= 1));
    }

    //pmartin 2aug01
    public static Category[] selectNonRootCategories(Category[] cats) {
        if(cats == null) {
            return null;
        }
        ArrayList nrc = new ArrayList(Lexicon.defaultCatListSize);
        for(int i = 0; i < cats.length; i++) {
            if(!(cats[i].isRootCategory())) {
                nrc.add(cats[i]);
            }
        }
        if(nrc.size() == 0) {
            return null;
        } else {
            Category[] nrca = new Category[nrc.size()];
            return (Category[]) (nrc.toArray(nrca));
        }
    }

    public static String SAtoString(String[] sa) {
        if(sa == null) {
            return "null";
        }
        StringBuffer sb = new StringBuffer();
        if(sa.length > 0) {
            sb.append(sa[0]);
        }
        for(int i = 1; i < sa.length; i++) {
            sb.append(", ").append(sa[i]);
        }
        return "{" + sb.toString() + "}";
    }

    public static synchronized String safePrintString(Value[] array) {
        if(array == null) {
            return "null";
        }
        int i = 0;
        String val = "";
        while(i < array.length) {
            if(i > 0) {
                val = val + " ";
            }
            if(array[i] == null) {
                val = val + "null";
            } else {
                val = val + array[i].safePrintString();
            }
            i++;
        }
        return val;
    }

    // static method for removing a value from an array:
    // pmartin 27Aug99
    // tweaked 25jul02 pmartin for multiples of remove element
    public static Value[] removeArrayElement(Value elt, Value[] array) {
        if(isMembOfArray(elt, array)) {
            int j = 0;
            int k = 0;
            int len = array.length;
            for(int ii = 0; ii < len; ii++) {
                if(array[ii] != elt) {
                    k++;
                }
            }
            Value[] newArray = new Value[k];
            for(int ii = 0; ii < len; ii++) {
                if(array[ii] != elt) {
                    newArray[j++] = array[ii];
                }
            }
            return newArray;
        } else {
            return array;
        }
    }

    // static method for removing a value from a Category array:
    // pmartin 1 aug 01 (basic Value method fails cast!)
    // tweaked 25jul02 pmartin for multiples of remove element
    public static Category[] removeArrayElement(Category elt, Category[] array) {
        if(isMembOfArray(elt, array)) {
            int j = 0;
            int k = 0;
            int len = array.length;
            for(int ii = 0; ii < len; ii++) {
                if(array[ii] != elt) {
                    k++;
                }
            }
            Category[] newArray = new Category[k];
            for(int ii = 0; ii < len; ii++) {
                if(array[ii] != elt) {
                    newArray[j++] = array[ii];
                }
            }
            return newArray;
        } else {
            return array;
        }
    }

    public static String printStringArray(Value[] array) {
        //pmartin 20apr00 pretty version of toStringArray
        if(array == null) {
            return null;
        }
        String astr = "[";
        for(int i = 0; i < array.length; i++) {
            String eleStr = "<null>";
            if(array[i] != null) {
                eleStr = array[i].printString();
            }
            astr = astr + " " + eleStr;
        }
        return (astr + " ]");
    }

    // static method for printing contents of arrays of values:
    public static synchronized String printString(Value[] array) {
        if(array == null) {
            return "null";
        }
        int i = 0;
        String val = "";
        while(i < array.length) {
            if(i > 0) {
                val = val + " ";
            }
            if(array[i] == null) {
                val = val + "null";
            } else {
                val = val + array[i].printString();
            }
            i++;
        }
        return val;
    }

    public static Category[] mergeExcluding(HashSet exclude,
            Category[] ca1,
            Category[] ca2) {
        HashSet good = new HashSet();
        if(ca1 != null) {
            for(int i = 0; i < ca1.length; i++) {
                if(!exclude.contains(ca1[i])) {
                    good.add(ca1[i]);
                }
            }
        }
        if(ca2 != null) {
            for(int i = 0; i < ca2.length; i++) {
                if(!exclude.contains(ca2[i])) {
                    good.add(ca2[i]);
                }
            }
        }
        Category[] mc = new Category[good.size()];
        return (Category[]) good.toArray(mc);
    }

    // tweaked 16oct01 to use a Word[] to accumulate instead of vector
    public static Word[] mergeArrays(Word[] wa1, Word[] wa2) {
        if((wa1 == null) || (wa1.length == 0)) {
            return wa2;
        } else if((wa2 == null) || (wa2.length == 0)) {
            return wa1;
        }

        Word[] newWa2 = new Word[wa2.length];
        int newIdx = 0;
        for(int i = 0; i < wa2.length; i++) {
            if(!isMembOfArray(wa2[i], wa1)) {
                newWa2[newIdx++] = wa2[i];
            }
        }

        if(newIdx == 0) {
            return wa1; // all were duplicates
        }
        int oldLen = wa1.length;
        Word[] wa3 = new Word[oldLen + newIdx];
        for(int i = 0; i < oldLen; i++) {
            wa3[i] = wa1[i];
        }
        for(int i = 0; i < newIdx; i++) {
            wa3[oldLen + i] = newWa2[i];
        }
        return wa3;
    }

    public static Value[] mergeArrays(Value[] v1, Value[] v2) {
        if((v1 == null) || (v1.length == 0)) {
            return v2;
        }
        if((v2 == null) || (v2.length == 0)) {
            return v1;
        }
        HashSet newV = new HashSet();
        for(int i = 0; i < v1.length; i++) {
            newV.add(v1[i]);
        }
        for(int i = 0; i < v2.length; i++) {
            newV.add(v2[i]);
        }
        Value[] mv = new Value[newV.size()];
        return (Value[]) newV.toArray(mv);
    }

    public static Category[] mergeArrays(Category[] ca1, Category[] ca2) {
        if((ca1 == null) || (ca1.length == 0)) {
            return ca2;
        } else if((ca2 == null) || (ca2.length == 0)) {
            return ca1;
        }

        HashSet newS = new HashSet();
        for(int i = 0; i < ca2.length; i++) {
            newS.add(ca2[i]);
        }
        for(int i = 0; i < ca1.length; i++) {
            newS.add(ca1[i]);
        }
        int newC = newS.size();
        if(newC == 0) {
            return ca1;
        }
        Category[] mc = new Category[newC];
        return (Category[]) (newS.toArray(mc));
    }

    public static Atom[] mergeArrays(Atom[] v1, Atom[] v2) {
        if((v1 == null) || (v1.length == 0)) {
            return v2;
        }
        if((v2 == null) || (v2.length == 0)) {
            return v1;
        }
        HashSet newV = new HashSet();
        for(int i = 0; i < v1.length; i++) {
            newV.add(v1[i]);
        }
        for(int i = 0; i < v2.length; i++) {
            newV.add(v2[i]);
        }
        Value[] mv = new Value[newV.size()];
        return (Atom[]) newV.toArray(mv);
    }

    public static int membr(Value elt, Value[] array) {
        //returns the index of the membership of the element
        if((elt != null) && (array != null)) {
            int i = 0;
            while(i < array.length) {
                if(array[i].equal(elt)) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    public static int membr(Value elt, List testList) {
        return membr(elt, testList.contents);
    }

    public static int memb(Value elt, List testList) {
        return memb(elt, testList.contents);
    }

    public static int memb(Object elt, Object[] array) {
        //returns the index of the membership of the element
        if((elt != null) && (array != null)) {
            int i = 0;
            while(i < array.length) {
                if(array[i] == elt) {
                    return i;
                }
                i++;
            }
        }
        return -1;
    }

    public static boolean isMembOfArray(Object elt, Object[] array) {
        if((elt != null) && (array != null)) {  //pmartin null test added 14sep99
            int i = 0;
            while(i < array.length) {
                if(array[i] == elt) {
                    return true;
                }
                i++;
            }
        }
        return false;
    }

    // odd signature to handle migration to HashSets pm 06apr04
    public static boolean isMembOfArray(Object elt, HashSet hs) {
        if((elt == null) || (hs == null)) {
            return false;
        } else {
            return hs.contains(elt);
        }
    }

    public static boolean isMemberOfArray(Value elt, Value[] array) {
        if((elt != null) && (array != null)) {  //pmartin null test added 14sep99
            int i = 0;
            while(i < array.length) {
                if(array[i].equal(elt)) {
                    return true;
                }
                i++;
            }
        }
        return false;
    }

    // odd signature to handle migration to HashSets pm 06apr04
    public static boolean isMemberOfArray(Object elt, HashSet hs) {
        if((elt == null) || (hs == null)) {
            return false;
        } else {
            return hs.contains(elt);
        }
    }

    //tests if this node is in a list
    public static boolean isInList(Value val, List list) {
        return ((list != null) && isMembOfArray(val, list.contents));
    }

    /**
     * A quick check for whether a character is a digit.
     * Borrowed from SmartTokenizer_en 15Nov01
     * @param c The character to check
     */
    public static final boolean isDigit(char c) {
        if((c <= 57 && c >= 48) //the most frequent case, ASCII numbers 0...9
                ) {
            return true;
        } else if(c <= 255) {
            return false;
        } else {
            return Character.isDigit(c);
        }
    }

    // static method for testing intersection of arrays of values:
    public static boolean intersectp(Value[] list1, Value[] list2) {
        if((list1 == null) || (list2 == null)) {
            return false; //pmartin 25may01
        }
        int i, j;
        i = 0;
        while(i < list1.length) {
            j = 0;
            while(j < list2.length) {
                if(list1[i] == list2[j]) {
                    return true;
                }
                j++;
            }
            i++;
        }
        return false;
    }

    public static HashSet hashArray(HashSet hs, Object[] va) {
        if(hs == null) {
            hs = new HashSet();
        }
        if((va != null) && (va.length > 0)) {
            for(int i = 0; i < va.length; i++) {
                hs.add(va[i]);
            }
        }
        return hs;
    }

    public static String getWordStringArray(Value[] array) {
        //pmartin 15nov00 concatenated wordStrings
        if((array == null) || (array.length < 1)) {
            return "";
        }
        StringBuffer astr = new StringBuffer();
        astr.append(array[0].getWordString());
        for(int i = 1; i < array.length; i++) {
            astr.append("_").append(array[i].getWordString());
        }
        return astr.toString();
    }

    public static String fringeString(Value[] vals) {//pmartin 14may01
        if((vals == null) || (vals.length == 0)) {
            return "";
        }
        StringBuffer fringeBuf = new StringBuffer();
        String frg;
        Value val;
        for(int i = 0; i < vals.length; i++) {
            val = vals[i];
            if(val.listp()) {
                frg = fringeString(((List) val).contents);
            } else {
                frg = val.printString(); // covers Atom, Categories, Words
            }
            if(fringeBuf.length() != 0) {
                fringeBuf.append("_");
            }
            fringeBuf.append(frg);
        }
        return fringeBuf.toString();
    }

    public static String fringeString(List lval) {
        if(lval == null) {
            return "";
        } else {
            return fringeString(lval.contents);
        }
    }

    public static Word firstVal(Word[] wa) { //pmartin 02aug01
        if((wa != null) && (wa.length > 0)) {
            return wa[0];
        } else {
            return null;
        }
    }

    public static Value firstVal(Value[] va) { //pmartin 01aug01
        if((va != null) && (va.length > 0)) {
            return va[0];
        } else {
            return null;
        }
    }

    public static Value firstVal(Value fv) { //pmartin 01aug01
        if((fv != null) && (fv.listp()) && (((List) fv).contents.length > 0)) {
            return ((List) fv).contents[0];
        } else {
            return null;
        }
    }

    // public static boolean wordTrace = true;
    public static Word[] concatArrays(Word[] wa1, Word[] wa2) {
        // pmartin 11mar04
        int w1 = 0;
        if(wa1 != null) {
            w1 = wa1.length;
        }
        int w2 = 0;
        if(wa2 != null) {
            w2 = wa2.length;
        }
        int w3 = w1 + w2;
        if(w3 == 0) {
            return null;
        }
        Word[] newwa = new Word[w3];
        for(int i = 0; i < w1; i++) {
            newwa[i] = wa1[i];
        }
        for(int i = 0; i < w2; i++) {
            newwa[w1 + i] = wa2[i];
        }
        return newwa;
    }

    public static Value[] concatArrays(Value[] va1, Value[] va2) {
        // pmartin 11mar04
        int v1 = 0;
        if(va1 != null) {
            v1 = va1.length;
        }
        int v2 = 0;
        if(va2 != null) {
            v2 = va2.length;
        }
        int v3 = v1 + v2;
        if(v3 == 0) {
            return null;
        }
        Value[] newva = new Value[v3];
        for(int i = 0; i < v1; i++) {
            newva[i] = va1[i];
        }
        for(int i = 0; i < v2; i++) {
            newva[v1 + i] = va2[i];
        }
        return newva;
    }

    public static boolean compoundStringp(String str) { // pmartin 18oct01
        if(str.length() > 2) {
            int i = str.indexOf("_");
            if(i > 0) {
                return true;
            }
            i = str.indexOf("-");
            if(i > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean charInArrayP(char chr, char[] array) {
        if(array != null) {
            for(int i = 0; i < array.length; i++) {
                if(array[i] == chr) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String arrayToString(Object[] oa) {
        if(oa == null) {
            return "null";
        }
        StringBuffer sb = new StringBuffer();
        String s;
        if(oa.length > 0) {
            if(oa[0] instanceof String) {
                s = (String) oa[0];
            } else {
                s = oa[0].toString();
            }
            sb.append(s);
        }
        for(int i = 1; i < oa.length; i++) {
            if(oa[0] instanceof String) {
                s = (String) oa[i];
            } else {
                s = oa[i].toString();
            }
            sb.append(", ").append(s);
        }
        return "{" + sb.toString() + "}";
    }

    public static Value[] addValueToArray(Value[] va, Value newV) {
        Value[] newVa;
        if((newV == null) || (isMemberOfArray(newV, va))) {
            return va;
        } else {
            int len = 0;
            if(va != null) {
                len = va.length;
            }
            newVa = new Value[len + 1];
            newVa[len] = newV;
            for(int i = 0; i < len; i++) {
                newVa[i] = va[i];
            }
        }
        return newVa;
    }

    // static methods  to add element to array or merge two arrays
    // pmartin 17july01
    public static Word[] addToArray(Word[] wa, Word newW) {
        Word[] newWa;
        if((newW == null) || (isMembOfArray(newW, wa))) {
            return wa;
        } else {
            int len = 0;
            if(wa != null) {
                len = wa.length;
            }
            newWa = new Word[len + 1];
            newWa[len] = newW;
            for(int i = 0; i < len; i++) {
                newWa[i] = wa[i];
            }
        }
        return newWa;
    }

    // static method for inserting a word at the front of an array:
    // pmartin 29Oct99
    public static Word[] addArrayElement(Word elt, Word[] array) {
        int oldLen = 0;
        if(array != null) {
            oldLen = array.length;
        }
        Word[] newArray = new Word[oldLen + 1];
        newArray[0] = elt;
        for(int i = 0; i < oldLen; i++) {
            newArray[i + 1] = array[i];
        }
        return newArray;
    }

    // static method for inserting a value at the front of an array:
    // pmartin 27Aug99
    public static Value[] addArrayElement(Value elt, Value[] array) {
        int oldLen = 0;
        if(array != null) {
            oldLen = array.length;
        }
        Value[] newArray = new Value[oldLen + 1];
        newArray[0] = elt;
        for(int i = 0; i < oldLen; i++) {
            newArray[i + 1] = array[i];
        }
        return newArray;
    }

    // static method for inserting a category at the front of an array:
    // pmartin 29Oct99
    public static Category[] addArrayElement(Category elt, Category[] array) {
        int oldLen = 0;
        if(array != null) {
            oldLen = array.length;
        }
        Category[] newArray = new Category[oldLen + 1];
        newArray[0] = elt;
        for(int i = 0; i < oldLen; i++) {
            newArray[i + 1] = array[i];
        }
        return newArray;
    }

    public static int encodeWordVal(BitBuffer bb, Word w) {
        int nBits = bb.gammaEncode(Lexicon.WORDVALS);
        nBits += encodeWordP(bb, w);
        return nBits;
    }

    public static int encodeWords(BitBuffer bb, Word[] wds) {
        int n;
        int wi;
        if((wds == null) || ((n = wds.length) == 0)) {
            bb.push(true);
            return 1;
        } else {
            bb.push(false);
        }
        if(n == 1) {
            bb.push(true);
            return 2 + encodeWordP(bb, wds[0]);
        } else {
            bb.push(false);
            int[] ints = new int[n];
            for(int i = 0; i < n; i++) {
                wi = (wds[i].index) & Lexicon.WORDINDEXMASK;
                if(wi <= 0) {
                    wi = wds[i].assignWordIndex();
                }
                ints[i] = wi;
            }
            return 2 + bb.differenceEncodeArray(n, ints);
        }
    }

    public static int encodeWordP(BitBuffer bb, Word w) {
        int windex = w.index & Lexicon.WORDINDEXMASK;
        if(windex < 1) {
            windex = w.assignWordIndex();
        }
        return bb.gammaEncode(windex);
    }

    public static int encodeValues(BitBuffer bb, Value[] vals) {
        int n;
        if((vals == null) || ((n = vals.length) == 0)) {
            bb.push(true);
            return 1;
        } else {
            bb.push(false);
            n = vals.length;
            int nBits = bb.gammaEncode(n + 1);
            for(int i = 0; i < n; i++) {
                nBits += encodeValue(bb, vals[i]);
            }
            return nBits + 1;
        }
    }

    public static int encodeValueHash(BitBuffer bb, ConcurrentHashMap vh) {
        int nBits = 1;
        int vhSize;
        if((vh == null) || ((vhSize = vh.size()) == 0)) {
            bb.push(true);
        } else {
            bb.push(false);
            nBits += bb.gammaEncode(vhSize);
            Enumeration temp = vh.keys();
            while(temp.hasMoreElements()) {
                Atom key = (Atom) temp.nextElement();
                nBits += encodeAtomP(bb, key);
                Value propVal = (Value) vh.get(key);
                nBits += encodeValue(bb, propVal);
            }
        }
        return nBits;
    }

    public static int encodeValue(BitBuffer bb, Value v) {
        int nBits = 0;
        if((v == null) || (v.listp())) {
            nBits = encodeList(bb, (List) v);
        } else if(v.wordp()) {
            nBits = encodeWordVal(bb, (Word) v);
        } else if(v.categoryp()) {
            nBits = encodeCategoryVal(bb, (Category) v);
        } else if(v.phrasep()) {
            logger.finest("encodeValue can't do phrases!");
        } else {
            nBits = encodeAtomVal(bb, (Atom) v);
        }
        return nBits;
    }

    public static int encodeNumber(BitBuffer bb, Number num) {
        long lnum;
        Long someLong = null;
        if(num == null) {
            bb.push(true);
            return 1;
        }
        bb.push(false);
        if(num instanceof Integer) {
            bb.push(true);
            return (2 + bb.encodeInt(num.intValue()));
        } else {
            bb.push(false);
            if(num instanceof Double) {
                bb.push(true);
                lnum = Double.doubleToLongBits(num.doubleValue());
                someLong = new Long(lnum);
            } else {
                bb.push(false);
                someLong = (Long) num;
            }
            return (3 + bb.encodeLong(someLong.longValue()));
        }
    }

    public static int encodeList(BitBuffer bb, List lst) {
        int nBits = bb.gammaEncode(Lexicon.LISTVALS);
        Value[] lvals = null;
        if(lst != null) {
            lvals = lst.contents;
        }
        nBits += encodeValues(bb, lvals);
        return nBits;
    }

    public static int encodeCategoryVal(BitBuffer bb, Category c) {
        int nBits = bb.gammaEncode(Lexicon.CATVALS);
        nBits += encodeCategoryP(bb, c);
        return nBits;
    }

    public static int encodeCategoryP(BitBuffer bb, Category c) {
        int cindex = c.index;
        if(cindex < 1) {
            cindex = c.assignCategoryIndex();
        }
        return bb.gammaEncode(cindex);
    }

    public static int encodeCategories(BitBuffer bb, Category[] cats) {
        return encodeAtoms(bb, cats);
    }

    public static int encodeAtomVal(BitBuffer bb, Atom a) {
        int nBits = bb.gammaEncode(Lexicon.ATOMVALS);
        nBits += encodeAtomP(bb, a);
        return nBits;
    }

    /* methods to interface Lexicon objects to the BitBuffer */
    public static int encodeAtoms(BitBuffer bb, Atom[] atms) {
        int n;
        if((atms == null) || ((n = atms.length) == 0)) {
            bb.push(true);
            return 1;
        } else {
            bb.push(false);
        }
        if(n == 1) {
            bb.push(true);
            return 2 + encodeAtomP(bb, atms[0]);
        } else {
            bb.push(false);
            int[] ints = new int[n];
            for(int i = 0; i < n; i++) {
                if(atms[i] == null) {
                    logger.finest("null atom in element " + i +
                            " of array " + LexiconUtil.printStringArray(atms));
                }
                ints[i] = (atms[i].index) & Lexicon.WORDINDEXMASK;
            }
            return 2 + bb.differenceEncodeArray(n, ints);
        }
    }

    public static int encodeAtomP(BitBuffer bb, Atom a) {
        int aindex = a.index;
        if(aindex < 1) {
            aindex = a.assignAtomIndex();
        }
        return bb.gammaEncode(aindex);
    }

    public static boolean startsLikeTime(String str) { //pmartin 2aug00
        int colon1 = str.indexOf(':');
        return (((colon1 == 1) || (colon1 == 2)) &&
                (str.length() > colon1 + 2) &&
                (Character.isDigit(str.charAt(0))) &&
                ((Character.isDigit(str.charAt(1)) && (colon1 == 2)) ||
                (colon1 == 1)) &&
                (Character.isDigit(str.charAt(colon1 + 1))) &&
                (Character.isDigit(str.charAt(colon1 + 2))));
    }
}
