/*
 * Copyright 2007-2009 Sun Microsystems, Inc. All Rights Reserved.
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
package com.sun.labs.minion.query;

import com.sun.labs.minion.retrieval.DictTerm;
import com.sun.labs.minion.retrieval.QueryElement;
import com.sun.labs.minion.util.CharUtils;
import java.util.EnumSet;

/**
 * A class for a search term.  Various modifiers can be applied to a term to
 * affect how the term should be treated during the search.
 */
public class Term extends Element {

    /**
     * The modifiers that affect which terms will be pulled from the index
     * for the given query term.
     */
    public enum Modifier {
        /**
         * Indicates that only terms with the exact case provided should be
         * returned.
         */
        CASE,

        /**
         * Indicates that terms that are morphological variations may be returned.
         */
        MORPH,

        /**
         * Indicates that semantic or other term variations may be returned.
         */
        EXPAND,

        /**
         * Indicates that stem matches may be returned.
         */
        STEM,

        /**
         * Indicates that wildcard matches may be returned.
         */
        WILDCARD,
    }

    private String term;

    private EnumSet<Modifier> modifiers;

    public Term(String term) {
        this(term, EnumSet.of(Modifier.MORPH));
    }

    public Term(String term, EnumSet<Modifier> mods) {
        this.term = term;
        if(mods != null) {
            this.modifiers = EnumSet.copyOf(mods);
        } else {
            this.modifiers = EnumSet.noneOf(Modifier.class);
        }
    }

    public void addModifier(Modifier modifier) {
        modifiers.add(modifier);
    }

    public String getTerm() {
        return term;
    }

    public EnumSet<Modifier> getModifiers() {
        return modifiers;
    }

    public QueryElement getQueryElement() {
        //
        // If we don't want to match case, then downcase the string, so that 
        // dict term will do the right thing by default.
        if(!modifiers.contains(Modifier.CASE)) {
            term = CharUtils.toLowerCase(term);
        }
        DictTerm ret = new DictTerm(term);
        ret.setMatchCase(modifiers.contains(Modifier.CASE));
        ret.setDoExpand(modifiers.contains(Modifier.EXPAND));
        ret.setDoMorph(modifiers.contains(Modifier.MORPH));
        ret.setDoStem(modifiers.contains(Modifier.STEM));
        ret.setDoWild(modifiers.contains(Modifier.WILDCARD));
        return ret;
    }

}
