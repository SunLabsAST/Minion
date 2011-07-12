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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.labs.minion.FieldInfo;

import com.sun.labs.minion.pipeline.StageAdapter;
import com.sun.labs.minion.pipeline.Token;

import java.util.logging.Logger;

/**
 * A pipeline stage that can be used to collect tokens from a defined set
 * of fields that can be used for passage and field highlighting.
 */
public class HighlightStage extends StageAdapter {

    /**
     * A map from all field names to the passages that are stored for each
     * field.  This map contains all defined passages in the document.
     */
    protected Map pass;

    /**
     * The ID of the document that we're highlighting.
     */
    protected int doc;

    /**
     * A map from field names that we're supposed to highlight to a list of
     * the passages that should be highlighted for that field.
     */
    protected Map<String,List<PassageImpl>> hPass;

    /**
     * The fields that we're currently working on.
     */
    protected Set<String> fields;

    /**
     * The set of fields that we're considering as body fields for the
     * purposes of highlighting.
     */
    protected Set<String> bodyFields;

    /**
     * Whether the body fields are to be sorted by score.
     */
    protected boolean sortBody;

    /**
     * The query terms used in the query.
     */
    protected String[] qt;

    private static final Logger logger = Logger.getLogger(HighlightStage.class.getName());

    public HighlightStage() {
        hPass = new HashMap();
        fields = new HashSet();
        bodyFields = new HashSet();
    } // HighlightStage constructor

    /**
     * Resets the stage so that we can use it for a different document.
     *
     * @param ag An array group containing the query results.
     * @param doc The document ID that we're processing.
     * @param qt The query terms.
     */
    public void reset(ArrayGroup ag, int doc, String[] qt) {
        pass = ag.getPassages(doc);
        this.doc = doc;
        this.qt = qt;
        hPass.clear();
        fields.clear();
        bodyFields.clear();
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
    public void addField(String fieldName, com.sun.labs.minion.Passage.Type type,
            int context, int maxSize, boolean doSort) {

        if(fieldName != null) {
            fieldName = fieldName.toLowerCase();
        }

        PassageStore ps = (PassageStore) pass.get(fieldName);

        //
        // If there are no passages defined for the given field, we'll only
        // handle it if there was a request for the whole field.
        if(ps == null) {
            if(context == -1) {
                List<PassageImpl> l = new ArrayList<PassageImpl>();
                l.add(new PassageImpl(null, 0, qt, context, maxSize));
                hPass.put(fieldName, l);
            }
            return;
        }

        //
        // We have some passages.  We'll treat them differently depending
        // on the type.
        if(type == com.sun.labs.minion.Passage.Type.JOIN) {
            List<PassageImpl> l = new ArrayList();
            l.add(new PassageImpl(ps.getAllPassages(doc),
                    ps.getPenalty(doc),
                    qt,
                    context,
                    maxSize));
            hPass.put(fieldName, l);
        } else if(type == com.sun.labs.minion.Passage.Type.UNIQUE) {
            int[][] p = ps.getUniquePassages(doc);
            float[] pen = ps.getPenalties(doc);
            List<PassageImpl> l = new ArrayList();
            for(int i = 0; i < p.length; i++) {
                l.add(new PassageImpl(p[i], pen[i],
                        qt, context, maxSize));
            }
            if(doSort) {
                Collections.sort(l);
            }
            hPass.put(fieldName, l);
        }
    }

    /**
     * Adds any passages for fields that were not explicitly added to the
     * <code>null</code> key.
     */
    public void addRemaining(int context, int maxSize, boolean doSort) {

        //
        // Loop through the defined passages, looking for ones that we
        // haven't added.
        for(Iterator i = pass.entrySet().iterator(); i.hasNext();) {
            Map.Entry me = (Map.Entry) i.next();
            String key = (String) me.getKey();
            if(hPass.get(key) != null) {
                continue;
            }

            //
            // Note that this is a body field.
            bodyFields.add(key);
            addField(key, com.sun.labs.minion.Passage.Type.UNIQUE,
                    context, maxSize, doSort);
        }
        sortBody = doSort;
    }

    public Map getPassages() {

        //
        // If we had fields we considered to be in the body, then move them
        // into the null key now.
        if(bodyFields.size() > 0) {
            List old = (List) hPass.remove(null);
            if(old == null) {
                old = new ArrayList();
            }
            for(Iterator i = bodyFields.iterator(); i.hasNext();) {
                String k = (String) i.next();
                List l = (List) hPass.remove(k);
                if(l != null) {
                    old.addAll(l);
                }
            }
            if(old.size() > 0) {
                if(sortBody) {
                    Collections.sort(old);
                }
                hPass.put(null, old);
            }
        }

        //
        // If there are any passages that don't have any tokens, then we
        // will remove them now.
        for(Iterator i = hPass.entrySet().iterator(); i.hasNext();) {
            Map.Entry me = (Map.Entry) i.next();
            List l = (List) me.getValue();
            for(int j = 0; j < l.size(); j++) {
                if(((PassageImpl) l.get(j)).size == 0) {
                    l.remove(j);
                }
            }
        }

        return hPass;
    }

    /**
     * Starts a field.  This simply puts it in the set of fields that we're
     * handling.
     *
     * @param fi The {@link com.sun.labs.minion.FieldInfo} object that describes
     * the field that is starting.
     */
    @Override
    public void startField(FieldInfo fi) {

        //
        // Add this to the set of fields we're currently processing.
        fields.add(fi.getName());
    }

    /**
     * Processes a token from further up the pipeline.
     *
     * @param t The token to process.
     */
    @Override
    public void token(Token t) {
        if(fields.isEmpty()) {
            addToAll(null, t);
            return;
        }

        //
        // Add this token to all the fields we're watching out for.
        for(String field : fields) {
            addToAll(field, t);
        }
    }

    /**
     * Adds a token to all of the things associated with a given field
     * name.
     */
    protected void addToAll(Object key, Token t) {
        List<PassageImpl> o = hPass.get((String) key);
        if(o == null) {
            return;
        }
        for(Iterator i = ((List) o).iterator(); i.hasNext();) {
            ((PassageImpl) i.next()).add(t);
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

    /**
     * Removes the field from our set of fields.
     *
     * @param fi The {@link com.sun.labs.minion.FieldInfo} object that describes
     * the field that is ending.
     */
    @Override
    public void endField(FieldInfo fi) {

        //
        // Tell the fields that we're watching that they're done.
        List added = new ArrayList();
        List l = (List) hPass.get(fi.getName());
        if(l != null) {

            //
            // Iterate through the list, telling the passages that they're
            // done, collecting any new ones offered.
            for(Iterator i = l.iterator(); i.hasNext();) {
                PassageImpl np =
                        ((PassageImpl) i.next()).endField();
                if(np != null) {
                    added.add(np);
                }
            }

            //
            // Put any newly added passages here.
            l.addAll(added);
        }

        fields.remove(fi.getName());
    }
} // HighlightStage
