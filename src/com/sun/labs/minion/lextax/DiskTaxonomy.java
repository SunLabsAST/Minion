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

package com.sun.labs.minion.lextax;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.util.CharUtils;

public class DiskTaxonomy extends Taxonomy {

    /**
     * Instantiates a taxonomy from a file.
     * 
     * @param f
     *            The file to read the taxonomy from.
     * @param p
     *            The partition that this taxonomy is for
     */
    public DiskTaxonomy(File f, DiskPartition p) throws java.io.IOException {
        file = new RandomAccessFile(f, "r");
        dict = new DiskDictionary(ConceptEntry.class, new StringNameHandler(),
                file, new RandomAccessFile[0], p);
    }

    public void close() throws java.io.IOException {
        file.close();
    }

    /**
     * Gets the concept associated with a name.
     */
    public synchronized ConceptEntry getConcept(String name) {

        //
        // The taxonomy is case-insensitive.
        String low = CharUtils.toLowerCase(name);
        Entry e = dict.get(low);
        if (e == null) {
            return null;
        }
        return (ConceptEntry) e;
    }

    /**
     * Gets the given concept.
     * 
     * @param id
     *            The id of the concept we want.
     */
    public synchronized ConceptEntry getConcept(int id) {
        return (ConceptEntry) dict.get(id);
    }

    /**
     * Gets the morphological children for a given concept.
     * Or null if there are none
     */
    public Set getMorphChildren(int id) {
        ConceptEntry c = getConcept(id);
        if (c != null) {
            return c.getMorphChildren();
        }
        return null;
    }

    /**
     * Gets the kindof children for a given concept.
     * Or null of there are none
     */
    public Set getKindOfChildren(int id) {
        ConceptEntry c = getConcept(id);
        if (c != null) {
            return c.getIkoChildren();
        }
        return null;
    }

    /**
     * Gets the instanceof children for a given concept.
     * Or null if there are none
     */
    public Set getInstanceOfChildren(int id) {
        ConceptEntry c = getConcept(id);
        if (c != null) {
            return c.getInstanceOfChildren();
        }
        return null;
    }

    /**
     * Gets the children for a given name.
     * Or null if there are none
     */
    public Set getChildren(String name) {
        if (name.equals(TOP_NAME)) {
            return getChildren(TOP);
        } else {
            ConceptEntry e = (ConceptEntry) dict.get(CharUtils.toLowerCase(name));
            if (e == null) {
                return null;
            }
            return e.getChildren();
        }
    }

    /**
     * Gets all the children for a given concept.
     * Or null if there are none
     */
    public Set getChildren(int id) {
        ConceptEntry c = getConcept(id);
        if (c != null) {
            return c.getChildren();
        }
        return null;
    }

    /**
     * Gets the morphological parents for a given concept.
     * Or none if there are none
     */
    public Set getMorphParents(int id) {
        ConceptEntry c = getConcept(id);
        if (c != null) {
            return c.getMorphParents();
        }
        return null;
    }

    /**
     * Gets the kindof parents for a given concept.
     * Or null if there are none
     */
    public Set getKindOfParents(int id) {
        ConceptEntry c = getConcept(id);
        if (c != null) {
            return c.getIkoParents();
        }
        return null;
    }

    /**
     * Gets the instanceof parents for a given concept.
     * Or null if there are none
     */
    public Set getInstanceOfParents(int id) {
        ConceptEntry c = getConcept(id);
        if (c != null) {
            return c.getInstanceOfParents();
        }
        return null;
    }

    /**
     * Gets the parents for a given name.
     * Or null if there are none
     */
    public Set getParents(String name) {
        if (name.equals(TOP_NAME)) {
            return getParents(TOP);
        } else {
            ConceptEntry e = (ConceptEntry) dict.get(CharUtils.toLowerCase(name));
            if (e == null) {
                return null;
            }
            return e.getParents();
        }
    }

    /**
     * Gets all the parents for a given concept.
     * Or null if the concept doesn't exist
     */
    public Set getParents(int id) {
        ConceptEntry c = getConcept(id);
        if (c != null) {
            return c.getParents();
        }
        return null;
    }

    /**
     * Gets all the concepts subsumed by a given concept.
     * 
     * @param name
     *            The name of the term we want subsumed terms for
     * @param maxTerms
     *            The maximum number of terms to return
     */
    public Set getSubsumed(String name, int maxTerms) {
        Entry e = dict.get(CharUtils.toLowerCase(name));

        return getSubsumed(e, maxTerms);
    }

    /**
     * Gets all the concepts subsumed by a given concept.
     * 
     * Modified Paul Martin 25nov02 to return only non-sensename concepts, so
     * that retrieval will find the base "name"
     * 
     * Further modified pmartin 5dec02 to follow all the other kinds of links
     * that are possibly useful for subsumption
     * 
     * @param entry the Entry for the term we want subsumed terms for
     * @param maxTerms the maximum number of terms to return
     * @return all the concepts subsumed by the given concept
     */
    public Set getSubsumed(Entry entry, int maxTerms) {

        Integer Integer0 = new Integer(0);
        Integer Integer1 = new Integer(1);
        List toDo = new ArrayList();
        toDo.add(entry);
        Set subsumed = new HashSet();
        Set instanced = new HashSet();
        Hashtable beenMorphed = new Hashtable();

        Set children, parents;

        //
        // Get all the allowed relations all the way down.
        while (toDo.size() > 0 && (maxTerms < 0 || subsumed.size() <= maxTerms)) {
            ConceptEntry c = (ConceptEntry) toDo.remove(0);
            Integer icmd = (Integer) beenMorphed.get(c);
            if (icmd == null) {
                icmd = Integer0;
            }
            int currMorphDepth = icmd.intValue();
            if (c != null) {
                // Are we still doing morph?
                if (morphDepthLimit < 0 || currMorphDepth <= morphDepthLimit) {
                    children = c.getMorphChildren();
                    if (!children.isEmpty()) {
                        Integer incmd = new Integer(currMorphDepth + 1);
                        for (Iterator iter = children.iterator(); iter
                                .hasNext();) {
                            Entry child = (Entry) iter.next();
                            if (!subsumed.contains(child)) {
                                subsumed.add(child);
                                beenMorphed.put(child, incmd);
                                toDo.add(child);
                            }
                        }
                    }

                }
                if (currMorphDepth == 0) { // do all the other kinds

                    // we always do iko children
                    children = c.getIkoChildren();
                    for (Iterator iter = children.iterator(); iter.hasNext();) {
                        Entry child = (Entry) iter.next();
                        if (!subsumed.contains(child)) {
                            subsumed.add(child);
                            toDo.add(child);
                        }
                    }

                    // we always do variant children
                    children = c.getVariantOfChildren();
                    for (Iterator iter = children.iterator(); iter.hasNext();) {
                        Entry child = (Entry) iter.next();
                        if (!subsumed.contains(child)) {
                            subsumed.add(child);
                            toDo.add(child);
                        }
                    }

                    // we always use variant parents
                    parents = c.getVariantOfParents();
                    for (Iterator iter = parents.iterator(); iter.hasNext();) {
                        Entry parent = (Entry) iter.next();
                        if (!subsumed.contains(parent)) {
                            subsumed.add(parent);
                            toDo.add(parent);
                        }
                    }

                    // we always do sense children
                    children = c.getSenseChildren();
                    for (Iterator iter = children.iterator(); iter.hasNext();) {
                        Entry child = (Entry) iter.next();
                        if (!subsumed.contains(child)) {
                            subsumed.add(child);
                            toDo.add(child);
                        }
                    }

                    // we do iio only once per path
                    if (!instanced.contains(c)) {
                        children = c.getIkoChildren();
                        if (!children.isEmpty()) {
                            instanced.add(c);
                            for (Iterator iter = children.iterator(); iter
                                    .hasNext();) {
                                Entry child = (Entry) iter.next();
                                if (!subsumed.contains(child)) {
                                    subsumed.add(child);
                                    instanced.add(child);
                                    toDo.add(child);
                                }
                            }

                        }
                    }

                    // Nicknames are one-shots (leaf only) like morphing

                    if (useNicknames) { // we do nickname children
                        children = c.getNicknameChildren();
                        for (Iterator iter = children.iterator(); iter
                                .hasNext();) {
                            Entry child = (Entry) iter.next();
                            if (!subsumed.contains(child)) {
                                subsumed.add(child);
                                beenMorphed.put(child, Integer1);
                                toDo.add(child);
                            }
                        }

                        // we use nickname parents
                        parents = c.getNicknameParents();
                        for (Iterator iter = parents.iterator(); iter.hasNext();) {
                            Entry parent = (Entry) iter.next();
                            if (!subsumed.contains(parent)) {
                                subsumed.add(parent);
                                beenMorphed.put(parent, Integer1);
                                toDo.add(parent);
                            }
                        }

                    }

                    if (useAbbreviations) {
                        // we do abbreviation children
                        children = c.getAbbrevChildren();
                        for (Iterator iter = children.iterator(); iter
                                .hasNext();) {
                            Entry child = (Entry) iter.next();
                            if (!subsumed.contains(child)) {
                                subsumed.add(child);
                                toDo.add(child);
                            }
                        }

                        // we use abbreviation parents
                        parents = c.getAbbrevParents();
                        for (Iterator iter = parents.iterator(); iter.hasNext();) {
                            Entry parent = (Entry) iter.next();
                            if (!subsumed.contains(parent)) {
                                subsumed.add(parent);
                                toDo.add(parent);
                            }
                        }

                    }

                    if (useMisspellings) { // we do misspelling children
                        children = c.getMisspellingChildren();
                        for (Iterator iter = children.iterator(); iter
                                .hasNext();) {
                            Entry child = (Entry) iter.next();
                            if (!subsumed.contains(child)) {
                                subsumed.add(child);
                                toDo.add(child);
                            }
                        }

                        // we use misspelling parents
                        parents = c.getMisspellingParents();
                        for (Iterator iter = parents.iterator(); iter.hasNext();) {
                            Entry parent = (Entry) iter.next();
                            if (!subsumed.contains(parent)) {
                                subsumed.add(parent);
                                toDo.add(parent);
                            }
                        }

                    }

                    if (useEntailments) { // we do only the children
                        children = c.getEntailsChildren();
                        for (Iterator iter = children.iterator(); iter
                                .hasNext();) {
                            Entry child = (Entry) iter.next();
                            if (!subsumed.contains(child)) {
                                subsumed.add(child);
                                toDo.add(child);
                            }
                        }

                    }
                }
            }

        }

        // substitute the "base" word for any sensename, removing
        // redundant entries and avoiding possible sensename parent loops
        Set subconcepts = new HashSet(subsumed.size());
        Set followedSubConcepts = new HashSet();
        List subConceptsToFollow = new ArrayList();
        Iterator subsumedIterator = subsumed.iterator();
        Set senseParents = new HashSet();

        while (subsumedIterator.hasNext()) {
            ConceptEntry ic = (ConceptEntry) subsumedIterator.next();
            subConceptsToFollow.add(ic);

            while (subConceptsToFollow.size() > 0) {
                ic = (ConceptEntry) subConceptsToFollow.remove(0);
                followedSubConcepts.add(ic);
                senseParents = ic.getSenseParents();
                if (!senseParents.isEmpty()) {
                    subconcepts.add(ic);
                } else {
                    for (Iterator iter = senseParents.iterator(); iter
                            .hasNext();) {
                        Entry isub = (Entry) iter.next();
                        if (!followedSubConcepts.contains(isub)) {
                            subConceptsToFollow.add(isub);
                        }
                    }

                }

            }
        }

        return subconcepts;
    }

    /**
     * Merges a number of taxonomies into a single one.
     */
    public void merge(DiskTaxonomy[] taxonomies, String indexDir, File f)
            throws java.io.IOException {
        

        PriorityQueue<HE> heap = new PriorityQueue<HE>(taxonomies.length);
        int[][] conceptIDMaps = new int[taxonomies.length][];
        for (int i = 0; i < taxonomies.length; i++) {
            conceptIDMaps[i] = new int[taxonomies[i].dict.getMaxID() + 2];
            HE heapEntry = new HE(i, taxonomies[i].dict.iterator());
            if (heapEntry.next()) {
                heap.offer(heapEntry);
            }
        }

        int conceptID = 1;
        List<ConceptEntry> conceptList = new ArrayList<ConceptEntry>();
        while (heap.size() > 0) {
            HE heapTop = heap.peek();
            Object currentName = heapTop.entry.getName();
            ConceptEntry conceptEntry = new ConceptEntry(currentName);
            conceptEntry.setID(conceptID);
            conceptList.add(conceptEntry);
            while (heapTop != null
                    && currentName.equals(heapTop.entry.getName())) {
                heap.poll();
                int i = heapTop.index;
                int j = heapTop.entry.getID();
                conceptIDMaps[i][j] = conceptID;
                if (heapTop.next()) {
                    heap.offer(heapTop);
                }
                heapTop = heap.peek();
            }
            conceptID++;
        }

        MemoryTaxonomy memoryTaxonomy = new MemoryTaxonomy();
        for (Iterator mergedEntryIterator = conceptList.iterator(); mergedEntryIterator.hasNext();) {
            ConceptEntry mergedEntry = (ConceptEntry) mergedEntryIterator.next();
            String mergedName = (String) mergedEntry.getName();

            for (int taxonomyIndex = 0; taxonomyIndex < taxonomies.length; taxonomyIndex++) {
                ConceptEntry conceptEntry = taxonomies[taxonomyIndex].getConcept(mergedName);
                if (conceptEntry != null) {
                    Set[] conceptLinks = conceptEntry.links;

                    for (int linkIndex = 0; linkIndex < conceptLinks.length; linkIndex++) {
                        Set links = conceptLinks[linkIndex];

                        for (Iterator si = links.iterator(); si.hasNext();) {
                            mergedEntry.links[linkIndex]
                                    .add(conceptList
                                            .get(conceptIDMaps[taxonomyIndex][((Integer) si
                                                    .next()).intValue()]));
                        }
                    }
                }
            }
            mergedEntry.setDictionary(memoryTaxonomy.dict);
            memoryTaxonomy.dict.put(mergedEntry.getName(), mergedEntry);
        }

        memoryTaxonomy.dump(indexDir, f);

        logger.info("Write merged taxonomy dictionary");
    }

    protected class HE implements Comparable<HE> {
        int index;

        DictionaryIterator di;

        ConceptEntry entry;

        public HE(int index, DictionaryIterator di) {
            this.index = index;
            this.di = di;
        }

        public boolean next() {
            if (di.hasNext()) {
                entry = (ConceptEntry) di.next();
                return true;
            }
            return false;
        }

        public int compareTo(HE o) {
            return entry.compareTo(o.entry);
        }
    }

    /**
     * The file containing the taxonomy.
     */
    protected RandomAccessFile file;

    /**
     * The dictionary for this partition.
     */
    protected DiskDictionary dict;

    /**
     * The offsets of the concepts.
     */
    protected long[] offsets;

    /**
     * The tag for log messages.
     */
    protected static String logTag = "DLT";

    /**
     * Parameters for controlling subbsumption search.
     */

    /**
     * Subsumption search parameter: Limits number of morphological steps total
     * along the path to a possible child node to explore.
     * 
     * Zero blocks morphology, -1 means no limit.
     */
    protected int morphDepthLimit = 4;

    /**
     * Subsumption search parameter: If true abbreviations find their target and
     * full names also find the abbreviated version.
     */
    protected boolean useAbbreviations = true;

    /**
     * Subsumption search parameter: If true nicknames find the full name and
     * full name queries also return marked nicknames, such as "chuck" for
     * "Charles"
     */
    protected boolean useNicknames = true;

    /**
     * Subsumption search parameter: If true, marked known possible misspellings
     * are used in subsumption in both directions. The misspelling will find the
     * true one and a query spelled correctly will also find the misspelled
     * data.
     */
    protected boolean useMisspellings = true;

    /**
     * Subsumption search parameter: If true the inverse entailments of the
     * taxonomy will be followed when doing subsumption. So if the taxonomy said
     * that "harmony" entails "good", a search on "good" would include harmony
     * as subsumption children.
     */
    protected boolean useEntailments = true;

    /**
     * @return Returns the dict.
     */
    public DiskDictionary getDict() {
        return dict;
    }

} // DiskTaxonomy
