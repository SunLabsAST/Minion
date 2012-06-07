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

import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.retrieval.ArrayGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A pipeline that can be used for highlighting documents.
 */
public class HLPipelineImpl extends PipelineImpl  {

    private HighlightStage hlStage;

    public HLPipelineImpl(List<Stage> pipeline) {
        super(pipeline);
        hlStage = (HighlightStage) pipeline.get(pipeline.size() - 1);

    } // HLPipeline constructor

    // Implementation of com.sun.labs.minion.PassageBuilder
    /**
     * Sets up for processing a new document.
     */
    public void reset(ArrayGroup ag, int doc, String[] qt) {
        hlStage.reset(ag, doc, qt);
    }

    public void addPassageField(String fieldName) {
        addPassageField(fieldName, com.sun.labs.minion.Passage.Type.JOIN,
                -1, -1, false);
    }

    public void addPassageField(String fieldName,
            com.sun.labs.minion.Passage.Type type,
            int context, int maxSize,
            boolean doSort) {
//        hlStage.addField(fieldName, type, context,
//                maxSize, doSort);
    }

    /**
     * Gets the highlighted passages that were specified using
     * <code>addPassageField</code>.
     *
     * @param document A map representing a list of field names and
     * values.
     * @return A <code>Map</code> that maps from field names to a
     * <code>List</code> of instances of <code>Passage</code> that are
     * associated with the field.  The key <code>null</code> maps to
     * passages that did not occur in any field.
     *
     * @see com.sun.labs.minion.SearchEngine#index
     * @see #addPassageField
     */
    public Map getPassages(Map document) {
        return getPassages(document, false, -1, -1, false);
    }

    /**
     * Gets the highlighted passages that were specified using
     * <code>addPassageField</code>.
     *
     * @param document A map representing a list of field names and
     * values.
     * @param context the amount of context that will be used around
     * passages in fields that were not explicitly added.
     * @param maxSize the maximum size of passage to return, in characters,
     * for fields that were not explicitly added.  -1 means any size is OK.
     * @param doSort If <code>true</code>, passages from any fields not
     * explictly added will be sorted by score before being returned.
     *
     * @return A <code>Map</code> that maps from field names to a
     * <code>List</code> of instances of <code>Passage</code> that are
     * associated with the field.  The key <code>null</code> maps to
     * passages that did not occur in any field and to passages from fields
     * that were not explicitly added.
     *
     * @see com.sun.labs.minion.SearchEngine#index
     * @see #addPassageField
     */
    public Map getPassages(Map document,
            int context, int maxSize, boolean doSort) {
        return getPassages(document, true, context, maxSize, doSort);
    }

    /**
     * Gets the highlighted passages that were specified using
     * <code>addPassageField</code>.
     *
     * @param document A map representing a list of field names and
     * values.
     * @param addRemaining If <code>true</code>, any passages that were not
     * explicitly added will be added under the <code>NonField</code> key.
     * @param context If <code>addRemaining</code> is true, this is the
     * amount of context that will be used around passages in fields that
     * were not explicitly added.
     * @param maxSize The maximum size of passage to return, in characters.  -1
     * means any size is OK.
     * 
     *
     * @param doSort If <code>true</code>, passages from remaining fields
     * will be sorted by score before being returned.
     *
     * @return <code>null</code> if there are no passages associated with
     * this hit or if we could not parse the document, or a
     * <code>Map</code> that maps from field names to a <code>List</code>
     * of the passages associated with that field, sorted by increasing
     * penalty score.
     *
     * @see com.sun.labs.minion.SearchEngine#index
     * @see #addPassageField
     */
    protected Map getPassages(Map document, boolean addRemaining,
            int context, int maxSize, boolean doSort) {
//        try {
//            if(addRemaining) {
//                hlStage.addRemaining(context, maxSize, doSort);
//            }
//            indexDoc("", document);
//            return hlStage.getPassages();
//        } catch(SearchEngineException se) {
//            logger.log(Level.SEVERE,
//                    "Error processing document for highlighting",
//                    se);
//            return new HashMap();
//        }
        return new HashMap();
    }

    /**
     * Gets all of the passages in the document as a list.
     *
     * @param document A map representing a list of field names and
     * values.
     * @param context The size of the surrounding context to put in the passage,
     * in words. -1 means take an entire field value as context.
     * @param maxSize The maximum size of passage to return, in characters.  -1
     * means any size is OK.
     *
     * @return a list of <code>Passage</code>s.
     */
    public List getPassages(Map document,
            int context, int maxSize) {
        hlStage.resetPassages();
        Map m = getPassages(document, true, context, maxSize, true);
        List ret = (List) m.get(null);
        if(ret == null) {
            return new ArrayList();
        }
        return ret;
    }
} // HLPipeline
