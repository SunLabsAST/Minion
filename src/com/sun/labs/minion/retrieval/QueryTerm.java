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

package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.QueryConfig;

/**
 * A class that holds a single term during querying.  Generally speaking,
 * terms are things that are backed by a postings list. Such a list may
 * come from the postings file for a partition or from the field store.
 */
public abstract class QueryTerm extends QueryElement {

    /**
     * The value given in the query.
     */
    protected String val;

    /**
     * If <code>true</code>, then the terms we pull from the dictionary
     * must match the case of the query term. This includes any
     * morphological variations. This flag will be set by the &lt;case&gt;
     * operator and reset by the &lt;case&gt; operator.
     */
    protected boolean matchCase;

    /**
     * If <code>true</code>, then position data must be loaded for
     * querying.
     */
    protected boolean loadPositions;

    /**
     * If <code>true</code>, then we must find morphological variants for
     * the query term using the appropriate lightweight morphology. If no
     * such morphology exists, we will use the stemmer to find
     * expansions. This flag will be set by the &lt;morph&gt; operator, and
     * reset by the &lt;exact&gt; operator. This flag will have no effect
     * on FieldTerms
     */
    protected boolean doMorph;

    /**
     * If <code>true</code>, then we must find words whose stems match the
     * current word.  This flag will be set by the &lt;stem&gt; operator,
     * and reset by the &lt;exact&gt; operator. This flag will have no
     * effect on FieldTerms.
     */
    protected boolean doStem;

    /**
     * If <code>true</code>, then we must expand this term by finding
     * matching terms from the dictionary. This flag will be set by the
     * &lt;wild&gt; operator. This flag will have no effect on FieldTerms.
     */
    protected boolean doWild;

    /**
     * If <code>true</code>, then we must perform a semantic expansion on
     * the term using the appropriate resource. If no such resource exists,
     * then no expansion will be performed. This flag will be set by the
     * &lt;expand&gt; operator and reset by the &lt;exact&gt;
     * operator. This flag will have no effect on FieldTerms
     */
    protected boolean doExpand;

    /**
     * Sets the query configuration for this term.  At this point we can
     * figure out about case matching.
     */
    @Override
    public void setQueryConfig(QueryConfig qc) {
        super.setQueryConfig(qc);

        //
        // If matchCase is false, then we need to look at the config and
        // the term itself to decide what to do.
        if(!matchCase) {
            matchCase = qc.caseSensitive(val);
        }
    }

    /**
     * Evaluates the term in the current partition.
     *
     * @param ag An array group that we can use to limit the evaluation of
     * the term.  If this group is <code>null</code> a new group will be
     * returned.  If this group is non-<code>null</code>, then the elements
     * in the group will be used to restrict the documents that we return
     * from the term.
     * @return A new <code>ArrayGroup</code> containing the results of
     * evaluating the term against the given group.  The static type of the
     * returned group depends on the query status parameter.
     */
    @Override
    public abstract ArrayGroup eval(ArrayGroup ag);


    public void setMatchCase(boolean matchCase) {
        this.matchCase = matchCase;
    }

    public void setLoadPositions(boolean loadPositions) {
        this.loadPositions = loadPositions;
    }
    
    public void setDoMorph(boolean doMorph) {
        this.doMorph = doMorph;
    }

    public void setDoStem(boolean doStem) {
        this.doStem = doStem;
    }

    public void setDoWild(boolean doWild) {
        this.doWild = doWild;
    }

    public void setDoExpand(boolean doExpand) {
        this.doExpand = doExpand;
    }

    @Override
    public String toString(String prefix) {
        String mods = "";
        if (matchCase) { mods = mods + "MATCH "; }
        if (doMorph) { mods = mods + "MORPH "; }
        if (doStem) { mods = mods + "STEM "; }
        if (doWild) { mods = mods + "WILD "; }
        if (doExpand) {mods = mods + "EXPAND "; }
        return super.toString(prefix) + " " + mods;
    }
} // QueryTerm
