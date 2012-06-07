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
package com.sun.labs.minion.pipeline;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Passage;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.PassageImpl;
import com.sun.labs.minion.retrieval.PassageStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * A pipeline stage that can be used to collect tokens from a field 
 * when highlighting search results.
 */
public class HighlightStage extends StageAdapter {

    /**
     * The ID of the document that we're highlighting.
     */
    protected int doc;
    
    /**
     * The field that we're highlighting for.
     */
    private FieldInfo field;
    
    /**
     * The passage store for this field.
     */
    private PassageStore store;

    /**
     * The passages that should be highlighted.
     */
    protected List<Passage> hPass;

    /**
     * Whether the passages should be sorted by score, so that better passages
     * come first.
     */
    protected boolean sortFields;

    /**
     * The query terms used in the query.
     */
    protected String[] queryTerms;

    private static final Logger logger = Logger.getLogger(HighlightStage.class.getName());

    public HighlightStage() {
        hPass = new ArrayList<com.sun.labs.minion.Passage>();
    }
    
    public void setField(FieldInfo field) {
        this.field = field;
    }
    
    /**
     * Resets the stage so that we can use it for a different document.
     *
     * @param ag An array group containing the query results.
     * @param doc The document ID that we're processing.
     * @param queryTerms The query terms.
     */
    public void reset(ArrayGroup ag, int doc, String[] queryTerms) {
        this.doc = doc;
        this.queryTerms = queryTerms;
        store = ag.getPassages(doc, field);
        hPass.clear();
    }

    /**
     * Resets the passages for this document.
     */
    public void resetPassages() {
        hPass.clear();
    }

    /**
     * Adds a field to the map of fields that we're supposed to look for.
     *
     * @param fieldName The name of the field that we want to collect
     * passages for.  If the name is <code>NonField</code>, then the other
     * parameters specify the data for passages that do not occur in any
     * field.
     * @param type The type of passage to build. If this is
     * <code>com.sun.labs.minion.JOIN_PASSAGES</code>, then all hits within
     * the named field will be joined into a single passage.  If this is
     * <code>com.sun.labs.minion.UNIQUE_PASSAGES</code>, then each hit will
     * be a separate passage.
     * @param context The size of the surrounding context to put in the
     * passage, in words.  -1 means take the entire containing field.
     * @param maxSize The maximum size of passage to return, in characters.
     * -1 means any size is OK.
     * @param doSort If <code>true</code>, then the passages for this field
     * will be sorted by score before being returned.
     */
    public void addPassage(com.sun.labs.minion.Passage.Type type,
            int context, int maxSize, boolean doSort) {

        //
        // If there are no passages defined for the given field, we'll only
        // handle it if there was a request for the whole field.
        if(store == null) {
            if(context == -1) {
                hPass.add(new PassageImpl(null, 0, queryTerms, context, maxSize));
            }
            return;
        }

        //
        // We have some passages.  We'll treat them differently depending
        // on the type.
        if(type == com.sun.labs.minion.Passage.Type.JOIN) {
            hPass.add(new PassageImpl(store.getAllPassages(doc),
                    store.getPenalty(doc),
                    queryTerms,
                    context,
                    maxSize));
        } else if(type == com.sun.labs.minion.Passage.Type.UNIQUE) {
            int[][] p = store.getUniquePassages(doc);
            float[] pen = store.getPenalties(doc);
            for(int i = 0; i < p.length; i++) {
                hPass.add(new PassageImpl(p[i], pen[i],
                        queryTerms, context, maxSize));
            }
            if(doSort) {
                Collections.sort(hPass);
            }
        }
    }

    public List<Passage> getPassages() {
        return hPass;
    }

    /**
     * Processes a token from further up the pipeline.
     *
     * @param t The token to process.
     */
    @Override
    public void token(Token t) {
        //
        // We'll only get tokens for the fields that we're interested 
        // in, so put them here.
        for(Passage passage : hPass) {
            ((PassageImpl) passage).add(t);
        }
    }

    /**
     * Processes some punctuation from further up the pipeline.
     *
     * @param p The punctuation to process.
     */
    @Override
    public void punctuation(Token p) {
        token(p);
    }

} // HighlightStage
