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
import com.sun.labs.minion.util.CharUtils;
import java.util.HashSet;
import java.util.Iterator;

/**
 * A class encapsulating all of our knowledge about a given token.  We use
 * this to avoid having to lower case strings more than once.
 */
public class WordToken  {
    
    public WordToken(String t) {
        token = t;
        word  = null;
        equal = true;

        //
        // Figure out a capcode for this word.  As we're going, we'll
        // lowercase the token.
        int     len        = token.length();
        char[]  l          = new char[len];
        boolean allUpper   = true;
        boolean firstUpper = true;
        boolean mixedUpper = false;
        char    c, d;
        for(int i = 0; i < l.length; i++) {
            c = token.charAt(i);

            if(Character.isDigit(c)) {
                ignore = true;
            }

            d = CharUtils.toLowerCase(c);
            if(c == d) {
                if(i == 0) {
                    firstUpper = false;
                }
                allUpper = false;
            } else {
                if(i > 0) {
                    mixedUpper = true;
                }
                equal = false;
            }
            l[i] = d;
        }
        lcToken = new String(l);

        //
        // Set the capitalization code.
        if(allUpper) {
            capCode = UC;
        } else if(mixedUpper) {
            capCode = MC;
        } else if(firstUpper) {
            capCode = IC;
        } else if(equal) {
            capCode = LC;
        }
    }
    public WordToken(Lexicon lex, String rawString) {
        this(rawString);
        word = lex.getWord(this);
    }
    
    protected void makeWord(Lexicon lex){
        if (word == null)
            word = lex.makeWord(this);
    }

    protected void morphWord(MorphEngine me){
        Word w = me.morph(lcToken);
        word = w;
    }

    protected void analyzeWord(MorphState ms){
        Word w = ms.frame.analyze(lcToken, ms);
        word = w;
    }

    protected void analyzeWord(MorphEngine me, MorphState ms){
        Word w = me.analyze(lcToken, ms);
        word = w;
    }

    public String getToken() {
        return token;
    }

    public String getLCToken() {
        return lcToken;
    }

    public boolean isEqual() {
        return equal;
    }

    public boolean isIgnoreable() {
        return ignore;
    }

    public Word getWord() {
        return word;
    }

    @Override
    public String toString() {
        return token + " " + lcToken + " " + capCode + " " + equal;
    }
    public Word[] getRoots(){
        if (word == null) return null;
        else return word.getRoots();
    }

    public Word[] nonRootParents(){
        if (word == null) return null;
        WordEntry we = word.getWordEntry();
        if (we == null) return null;
        Word[] iios = we.iioParents;
        Word[] ikos = we.ikoParents;
        Word[] sups = we.getSenseParents();
        Word[] subs = we.subsenses;
        Word[] vars = we.variantOf;
        int max = 0;
        if (iios != null) max += iios.length;
        if (ikos != null) max += ikos.length;
        if (sups != null) max += sups.length;
        if (subs != null) max += subs.length;
        if (vars != null) max += vars.length;

        if (max == 0) return null;
        HashSet notpr = new HashSet(max);
        HashSet pr = null;
        boolean nprOK = capCode != LC; // don't allow proper for lower case
        if (!nprOK) pr = new HashSet(max);

        if (iios != null)
            for (int i=0; i<iios.length; i++)
                if (nprOK || iios[i].hasCommonCat())
                    notpr.add(iios[i]);
                else pr.add(iios[i]);

        if (ikos != null)
            for (int i=0; i<ikos.length; i++)
                if (nprOK || ikos[i].hasCommonCat())
                    notpr.add(ikos[i]);
                else pr.add(ikos[i]);

        if (sups != null)
            for (int i=0; i<sups.length; i++)
                if (nprOK || sups[i].hasCommonCat())
                    notpr.add(sups[i]);
                else pr.add(sups[i]);

        if (subs != null)
            for (int i=0; i<subs.length; i++)
                if (nprOK || subs[i].hasCommonCat())
                    notpr.add(subs[i]);
                else pr.add(subs[i]);

        if (vars != null)
            for (int i=0; i<vars.length; i++)
                if (nprOK || vars[i].hasCommonCat())
                    notpr.add(vars[i]);
                else pr.add(vars[i]);

        Iterator it = null;
        int psize;
        if ((psize = notpr.size()) > 0)
            it = notpr.iterator();
        else if ((psize = pr.size()) > 0)
            it = pr.iterator();
        else return null;

        Word[] parents = new Word[psize];
        int i = 0;
        while (it.hasNext())
            parents[i++] = (Word)it.next();
        return parents;
    }


    /**
     * The string for a token.
     */
    protected String token;

    /**
     * The lowercase version of the token.
     */
    protected String lcToken;

    /**
     * The lexical item associated with this token.
     */
    protected Word word;

    /**
     * The capitalization code for this word.
     */
    protected int capCode;
     
    /**
     * Whether the lower and upper case are the same.
     */
    protected boolean equal;

    /**
     * Whether we should ignore this word.
     */
    protected boolean ignore;

    /**
     * The allowable cap-codes.
     */
    protected static final int LC = 0;
    protected static final int IC = 1;
    protected static final int UC = 2;
    protected static final int MC = 3;

} // WordToken
