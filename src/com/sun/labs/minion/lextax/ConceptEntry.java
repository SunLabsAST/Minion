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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.labs.minion.indexer.dictionary.Dictionary;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.minion.indexer.postings.Occurrence;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;

/**
 * A dictionary entry that holds a concept in a taxonomy.  This kind of entry
 * does not have any associated postings.  Rather, the information about the
 * taxonomic structure is encoded directly into the entries in the dictionary.
 *
 * @author Stephen Green <stpehen.green@sun.com>
 */
public class ConceptEntry implements IndexEntry, QueryEntry {
    
    /**
     * The name of the concept.
     */
    protected String name;
    
    /**
     * The ID of the concept.
     */
    protected int id;
    
    /**
     * The dictionary containing this concept.
     */
    protected Dictionary dict;
    
    /**
     * Links from this concept to other concepts.
     */
    protected Set[] links;
       
    /** Creates a new instance of ConceptEntry */
    public ConceptEntry() {
        links = new Set[Taxonomy.NLINKS];
        for(int i = 0; i < links.length; i++) {
            links[i] = new HashSet();
        }
    }
    
    public ConceptEntry(Object name) {
        this();
        setName(name);     
    }
    
    public void setName(Object name) {
        this.name = name.toString();
    }
    
    public void setID(int id) {
        this.id = id;
    }
    
    public void add(Occurrence o) {
    }
    
    public boolean writePostings(PostingsOutput[] out, int[] idMap) throws IOException {
        return true;
    }
    
    public void append(QueryEntry qe, int start, int[] idMap) {

        //
        // There's nothing here because we handle the merging by hand in the
        // merge method.  Please go away.
    }
    
    public void encodePostingsInfo(WriteableBuffer b) {
        b.byteEncode(id);
        b.byteEncode(links.length);
        
        for(int i = 0; i < links.length; i++) {
            Set s = links[i];
            
            b.byteEncode(s.size());
            for(Iterator j = s.iterator(); j.hasNext(); ) {
                b.byteEncode(((ConceptEntry) j.next()).getID());
            }
        }
    }
    
    public Entry getEntry(Object name) {
        return new ConceptEntry(name);
    }
    
    public Entry getEntry() {
        ConceptEntry e = (ConceptEntry) getEntry(this.name);
        e.links = links;
        e.id = id;
        e.dict = dict;
        return e;
    }
    
    public Object getName() {
        return name;
    }
    
    public int getID() {
        return id;
    }
    
    public int getN() {
        return 0;
    }
    
    public int getTotalOccurrences() {
        return 0;
    }
    
    public int getMaxFDT() {
        return 0;
    }
    
    public void setDictionary(Dictionary dict) {
        this.dict = dict;
    }
    
    public Partition getPartition() {
        return dict.getPartition();
    }
    
    public int getNumChannels() {
        return 0;
    }
    
    public int compareTo(Object o) {
        return name.compareTo(((ConceptEntry) o).getName().toString());
    }
    
    public void setPostingsInput(PostingsInput[] postIn) {
    }
    
    /**
     * Decode a ConceptEntry from the buffer.
     * The links of the concept entry <em>do not</em> point to other concepts
     * but rather to their IDs (represented as Integers).
     * The links are fixed up  by {@link DiskTaxonomy}
     * @see com.sun.labs.minion.indexer.entry.QueryEntry#decodePostingsInfo(com.sun.labs.minion.util.buffer.ReadableBuffer, int)
     * @see com.sun.labs.minion.lextax.DiskTaxonomy#merge(DiskTaxonomy[], String, File)
     */
    public void decodePostingsInfo(ReadableBuffer b, int pos) {
        b.position(pos);
        id = b.byteDecode();
        int linksLength = b.byteDecode();
        for(int i = 0; i < linksLength; i++) {
            int setSize = b.byteDecode();
            for(int j = 0; j < setSize; j++) {
                int conceptID = b.byteDecode();
                links[i].add(new Integer(conceptID));
            }        
        }
        
    }
    
    /**
     * We don't have any postings!
     */
    public void readPostings() throws IOException {
    }
    
    public boolean hasPositionInformation() {
        return false;
    }

    public boolean hasFieldInformation() {
        return false;
    }
    
    public PostingsIterator iterator(PostingsIteratorFeatures features) {
        return null;
    }
    
    /**
     * Adds a link to this concept.
     *
     * @param linkType The type of link to add.
     * @param c The concept that the link is to.
     * @return the number of bits added to the representation
     */
    public int add(int linkType, ConceptEntry c) {
        
        Set s = links[linkType];
        
        if(s.contains(c)) {
            return 0;
        }
        
        s.add(c);
        return 0;
    }
    
    /**
     * Gets variant-of parents.
     */
    public Set getVariantOfParents() {
        return getLinks(Taxonomy.VARIANT_OF);
    }
    
    /** variant-of children.
     */
    public Set getVariantOfChildren() {
        return getLinks(Taxonomy.INV_VARIANT_OF);
    }
    
    /**
     * Gets sense-of parents.
     */
    public Set getSenseParents() {
        return getLinks(Taxonomy.SENSE_OF);
    }
    
    /**
     * Gets subsenses of this concept (sense children).
     */
    public Set getSenseChildren() {
        return getLinks(Taxonomy.INV_SENSE_OF);
    }
    
    /**
     * Gets morph, kindof, senseof, and instanceof parents.
     */
    public Set getParents() {
        
        Set r = new HashSet();
        r.addAll(getMorphParents());
        r.addAll(getIkoParents());
        r.addAll(getSenseParents());
        r.addAll(getInstanceOfParents());
        return r;
    }
    
   public Set getNicknameParents(){
        return getLinks(Taxonomy.NICKNAME_OF);
    }
    
    public Set getNicknameChildren(){
        return getLinks(Taxonomy.INV_NICKNAME_OF);
    }
    
    /**
     * Gets morphological parents.
     */
    public Set getMorphParents() {
        return getLinks(Taxonomy.MORPH_OF);
    }
    
    /**
     * Gets morphological children.
     */
    public Set getMorphChildren() {
        return getLinks(Taxonomy.INV_MORPH_OF);
    }
    
    public Set getMisspellingParents(){
        return getLinks(Taxonomy.MISSPELLING_OF);
    }
    
    public Set getMisspellingChildren(){
        return getLinks(Taxonomy.INV_MISSPELLING_OF);
    }
    
    /**
     * Gets links of a given type.
     */
    protected Set getLinks(int linkType) {
        if (linkType < Taxonomy.NLINKS && linkType < links.length){
            Set linkSet = links[linkType];
            Set conceptSet = new HashSet();
            for (Iterator iter = linkSet.iterator(); iter.hasNext();) {
                Integer conceptId = (Integer) iter.next();
                conceptSet.add(((DiskDictionary)dict).get(conceptId.intValue()));
            }
            return conceptSet;
        }
        return null;
    }
    
    /**
     * Gets instance-of parents.
     */
    public Set getInstanceOfParents() {
        return getLinks(Taxonomy.INSTANCE_OF);
    }
    
    /** instance-of children.
     */
    public Set getInstanceOfChildren() {
        return getLinks(Taxonomy.INV_INSTANCE_OF);
    }
    
    /**
     * Gets all varieties of kind-of(iko) parents.
     */
    public Set getIkoParents() {
        return getLinks(Taxonomy.KIND_OF);
    }
    
    /**
     * Gets kind-of children.
     */
    public Set getIkoChildren() {
        return getLinks(Taxonomy.INV_KIND_OF);
    }
    
    /**
     * Gets Entails parents.
     */
    public Set getEntailsParents() {
        return getLinks(Taxonomy.ENTAILS);
    }
    
    /**
     * Gets entailment children.
     */
    public Set getEntailsChildren() {
        return getLinks(Taxonomy.INV_ENTAILS);
    }
    
    /**
     * Gets morph and instanceof and kindof children.
     */
    public Set getChildren() {
        Set r = new HashSet();
        r.addAll(getMorphChildren());
        r.addAll(getIkoChildren());
        r.addAll(getSenseChildren());
        r.addAll(getInstanceOfChildren());
        return r;
    }
    
    public Set getAbbrevParents(){
        return getLinks(Taxonomy.ABBREVIATION_OF);
    }
    
    public Set getAbbrevChildren(){
        return getLinks(Taxonomy.INV_ABBREVIATION_OF);
    }
    
    public String toString() {
        return "ConceptEntry <" + id + "> '" + name + "' dictionary: " + dict;
    }

    /**
     * Gets links in Parent direction
     */
    public Set[][] getParentLinkSet() {
    return getLinkSet(Taxonomy.PARENTS);
    }

    /**
     * Gets the set of links of a given type. linkSet is 0 or 1.
     * Some of the extra tests are to deal with link sets of varying size
     */
    protected Set[][] getLinkSet(int linkSet) {
    Set [][] ans = new Set[Taxonomy.NLINKTYPES][];
    for (int i=0; i<Taxonomy.NLINKTYPES; i++){
        ans[i] = null;
        int ii = linkSet+i*2;
        if (ii < links.length){
    	//int[] l = links[ii];
    //	if (l != null)  ans[i] = (int[]) l.clone();
        }
    }
    return ans;
    }

    /**
     * Gets links in Children direction
     */
    public Set[][] getChildrenLinkSet() {
    return getLinkSet(Taxonomy.CHILDREN);
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return id;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object conceptEntry) {
        return id == ((ConceptEntry)conceptEntry).id;
    }
}
