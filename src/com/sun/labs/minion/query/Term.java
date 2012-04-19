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

import com.sun.labs.minion.QueryPipeline;
import com.sun.labs.minion.retrieval.DictTerm;
import com.sun.labs.minion.retrieval.Phrase;
import com.sun.labs.minion.retrieval.QueryElement;
import com.sun.labs.minion.util.CharUtils;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumSet;

/**
 * A class for a search term.  Various modifiers can be applied to a term to
 * affect how the term should be treated during the search.
 */
public class Term extends Element implements Serializable {

    /**
     * The modifiers that affect which terms will be pulled from the index
     * for the given query term.
     */
    public enum Modifier {
        /**
         * Indicates that only terms with the exact case provided should be
         * returned.
         */
        CASE("<case>"),

        /**
         * Indicates that terms that are morphological variations may be returned.
         */
        MORPH("<morph>"),

        /**
         * Indicates that semantic or other term variations may be returned.
         */
        EXPAND("<expand>"),

        /**
         * Indicates that stem matches may be returned.
         */
        STEM("<stem>"),

        /**
         * Indicates that wildcard matches may be returned.
         */
        WILDCARD("<wild>");
        
        private String rep;
        
        Modifier() {
        }
        
        Modifier(String rep) {
            this.rep = rep;
        }
        
        public String getRep() {
            return rep;
        }
    }

    private String term;

    private EnumSet<Modifier> modifiers;
    
    private float termWeight;

    public Term(String term) {
        this(term, EnumSet.of(Modifier.MORPH), 0);
    }

    public Term(String term, EnumSet<Modifier> mods) {
        this(term, mods, 0);
    }
    
    /**
     * Creates a term with a given set of modifiers and a given term weight.
     * @param term the name of the term
     * @param mods modifiers for how the term should be handled at query time
     * @param termWeight a weight to be used for the term when computing similarity
     */
    public Term(String term, EnumSet<Modifier> mods, float termWeight) {
        this.term = term;
        if(mods != null) {
            this.modifiers = EnumSet.copyOf(mods);
        } else {
            this.modifiers = EnumSet.noneOf(Modifier.class);
        }
        this.termWeight = termWeight;
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

    @Override
    public QueryElement getQueryElement(QueryPipeline pipeline) {
        //
        // If we don't want to match case, then downcase the string, so that 
        // dict term will do the right thing by default.
        if(!modifiers.contains(Modifier.CASE)) {
            term = CharUtils.toLowerCase(term);
        }
        
        //
        // Start by throwing the term into a dummy document that we'll
        // feed through a pipeline to process the term.  This will apply
        // any further changes (such as stemming) that might be defined
        // in the query configuration.
        pipeline.process(term);
        String[] tokens = pipeline.getTokens();

        //
        // See what we got out.  If we broke into multiple tokens, make a
        // phrase out of what was entered.
        QueryElement ret = null;
        if (tokens.length > 1) {
            ArrayList<QueryElement> qes = new ArrayList<QueryElement>();
            for (String t : tokens) {
                DictTerm dt = new DictTerm(t);
                setModifiers(dt);
                dt.setTermWeight(termWeight);
                qes.add(dt);
            }
            ret = new Phrase(qes);
        } else if (tokens.length == 1) {
            DictTerm dt = new DictTerm(tokens[0]);
            setModifiers(dt);
            ret = dt;
        } else {
            // The whole thing was tokenized away
            DictTerm dt = new DictTerm("");
            setModifiers(dt);
            ret = dt;
        }
        return ret;
    }
    
    protected void setModifiers(DictTerm term) {
        term.setMatchCase(modifiers.contains(Modifier.CASE));
        term.setDoExpand(modifiers.contains(Modifier.EXPAND));
        term.setDoMorph(modifiers.contains(Modifier.MORPH));
        term.setDoStem(modifiers.contains(Modifier.STEM));
        term.setDoWild(modifiers.contains(Modifier.WILDCARD));
        term.setSearchFields(fields);
        term.strictEval = strict;
    }

    @Override
    public String toQueryString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for(Modifier modifier : modifiers) {
            if(!first) {
                sb.append(' ');
            }
            sb.append(modifier.getRep());
            first = false;
        }
        sb.append(term);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Term: " + term + " mods: " + modifiers;
    }

}
