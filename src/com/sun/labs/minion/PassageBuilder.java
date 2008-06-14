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

package com.sun.labs.minion;

import java.util.List;
import java.util.Map;

/**
 * An interface that will allow applications to build a set of passages to
 * display from a search result.
 *
 * @see Result#getPassageBuilder
 */
public interface PassageBuilder {
    
    /**
     * Registers a field for which we would like to get passages.  Any passages
     * from this field will be joined together into a single spanning passage, 
     * the entire field will be retained as context, and there will be no maximum
     * size imposed on the passages.
     * 
     * <p>
     * 
     * This is mostly a convenience for highlighting all of the passages in a 
     * field that is expected to be small (say, like the subject of an email 
     * message.)
     *
     * @param fieldName The name of the field that we want to collect
     * passages for.  If this name is <code>null</code>, the other
     * parameters specify the data for anything that is not in one of the
     * fields added using <code>addPassageField</code>.  If the name is
     * <code>NonField</code>, then the other parameters specify the data
     * for passages that do not occur in any field.
     */
    public void addPassageField(String fieldName);

    /**
     * Registers the parameters for a field for which we would like to get
     * passages.
     *
     * @param fieldName The name of the field that we want to collect
     * passages for.  If this name is <code>null</code>, the other
     * parameters specify the data for anything that is not in one of the
     * fields added using <code>addPassageField</code>.  If the name is
     * <code>NonField</code>, then the other parameters specify the data
     * for passages that do not occur in any field.
     * @param type The type of passage to build. If this is
     * <code>JOIN</code>, then all hits within the named field will
     * be joined into a single passage.  If this is
     * <code>UNIQUE</code>, then each hit will be a separate
     * passage.
     * @param context The size of the surrounding context to put in the
     * passage, in words.  -1 means take the entire containing field.
     * @param maxSize The maximum size of passage to return, in characters.
     * -1 means any size is OK.
     * @param doSort If <code>true</code>, then the passages for this field
     * will be sorted by score before being returned.
     */
    public void addPassageField(String fieldName, Passage.Type type,
                                int context, int maxSize,
                                boolean doSort);

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
    public Map<String,List<Passage>> getPassages(Map document);

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
    public Map<String,List<Passage>>  getPassages(Map document,
                           int context, int maxSize, boolean doSort);

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
    public List<Passage> getPassages(Map document,
                            int context, int maxSize);
    
}// PassageBuilder
