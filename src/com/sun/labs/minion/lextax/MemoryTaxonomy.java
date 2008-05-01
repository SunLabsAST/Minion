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

import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;

import com.sun.labs.minion.lexmorph.Lexicon;
import com.sun.labs.minion.lexmorph.Word;
import com.sun.labs.minion.lexmorph.MorphEngine;
import com.sun.labs.minion.lexmorph.Morph_en;
import com.sun.labs.minion.lexmorph.MorphState;
import com.sun.labs.minion.pipeline.Token;
        

public class MemoryTaxonomy extends Taxonomy {
    
    protected MorphEngine morph;
    protected MorphState state;
    private final static int IGNOREABLE_STRING_LENGTH = 50;
    
    public MemoryTaxonomy() {
        dict    = new MemoryDictionary(ConceptEntry.class);
        top     = new ConceptEntry(TOP_NAME);
        top.id=TOP;
        topID   = new Integer(TOP);
        dict.put(TOP_NAME, top);
    }
    
    
    public MemoryTaxonomy(Lexicon l) {
        this();
        setLexicon(l);
    }


    /**
     * @param l
     */
    public void setLexicon(Lexicon l) {
        lex 	= l;
        morph = new Morph_en();
        morph.initialize(lex);
        state = new MorphState();
        state.frame = morph;
    }
    
    public void classify(String t) {
        classify(morph.analyze(t, state), new ArrayList(), 0);
    }
    
    public ConceptEntry classify(Word w, ArrayList seen, int d) {
        /** tweaked pmartin 19Sep02 and 8oct02 to reflect special
         * treatment for variantOf links (including abbreviation
         * and misspellings) and subsense links (asymetric IKO).
         *
         * Also removed the calls for cloning the seen set --
         * I believe it is both faster and better reusing the same set
         * recursively.  (But look here for the crash.....)
         */
        
        String ws = w.getWordString();
        ConceptEntry entry = (ConceptEntry) dict.get(ws);
        
        //
        // If the entry that we get from the dictionary is non-null, we can just
        // return it, since it's already been classified.
        if(entry != null) {
            return entry;
        }
        
        entry = (ConceptEntry) dict.newEntry(ws);
        dict.put(ws, entry);
        
        
        Integer id     = new Integer(entry.getID());
        
        //
        // If we've already seen this concept, then we're done!
        if(seen.contains(id)) {
            // System.out.println("Classifier already saw " + ws);
            return entry;
        }
        seen.add(id);
        
        
        // debugging printout to find looping variant concepts
        // if (ws.length() > 40){
// 	    String varls = "   ";
// 	    if (!w.ownsWordEntry()) varls = " _ ";
// 	    else if (w.variantsLinked()) varls = " V ";
// 	    System.out.println("Classifying" + varls + ws);
// 	}
        
        /***
         * Make sure the variant Links are expanded into
         * the ikos, iio, and sense links we need on the word we are
         * doing.  Some are recorded only one-way from the jlf.
         */
        w.makeVariantLinks();
        //
        // make sure that any growing of the senses here has been
        // done by classifying the words that have inverse variant ptrs
        //
        Word[] inverseVariants = w.getInverseVariantLinks();
        if (inverseVariants != null)
            for (int i=0; i<inverseVariants.length; i++)
                classify(inverseVariants[i], seen, d+1);
        //
        // Record the variant links for retrieval
        //
        Word[] variants = w.variantParents();
        if (variants != null)
            for (int i=0; i<variants.length; i++){
            ConceptEntry vp = classify(variants[i], seen, d+1);
            entry.add(VARIANT_OF, vp);
            vp.add(INV_VARIANT_OF, entry);
            }
        //
        // Record abbreviation links for retrieval
        //
        variants = w.getAbbrevOf();
        if (variants != null)
            for (int i=0; i<variants.length; i++){
            ConceptEntry vp = classify(variants[i], seen, d+1);
            entry.add(ABBREVIATION_OF, vp);
            vp.add(INV_ABBREVIATION_OF, entry);
            }
        //
        // Record the nickname links for retrieval
        //
        variants = w.getNicknameOf();
        if (variants != null)
            for (int i=0; i<variants.length; i++){
            ConceptEntry vp = classify(variants[i], seen, d+1);
            entry.add(NICKNAME_OF, vp);
            vp.add(INV_NICKNAME_OF, entry);
            }
        //
        // Record the misspelling links for retrieval
        //
        variants = w.getMisspellingOf();
        if (variants != null)
            for (int i=0; i<variants.length; i++){
            ConceptEntry vp = classify(variants[i], seen, d+1);
            entry.add(MISSPELLING_OF, vp);
            vp.add(INV_MISSPELLING_OF, entry);
            }
        //
        // Record any entailment links for retrieval
        //
        Word[] entailParents = w.getEntails();
        if (entailParents != null)
            for (int i=0; i<entailParents.length; i++){
            ConceptEntry ep = classify(entailParents[i], seen, d+1);
            entry.add(ENTAILS, ep);
            ep.add(INV_ENTAILS, entry);
            }
        //
        // Add morphological root parents to taxonomy
        Word[] directRoots = w.getDirectRoots();
        if(directRoots != null)
            for(int i=0; i < directRoots.length; i++) {
            // ArrayList newSeen = (ArrayList) seen.clone();
            //Concept parent = classify(directRoots[i], newSeen,
            //d+1);
            ConceptEntry parent = classify(directRoots[i], seen, d+1);
            entry.add(MORPH_OF, parent);
            parent.add(INV_MORPH_OF, entry);
            }
        //
        // Add conceptual (iko) parents to taxonomy
        Word[] ikoParents = w.getIkoParents(false);
        
        if(ikoParents != null)
            for(int i=0; i < ikoParents.length; i++) {
            // ArrayList newSeen = (ArrayList) seen.clone();
            // Concept parent = classify(ikoParents[i], newSeen, d+1);
            ConceptEntry parent = classify(ikoParents[i], seen, d+1);
            entry.add(KIND_OF, parent);
            parent.add(INV_KIND_OF, entry);
            }
        //
        // Add instance-of (iio) parents to taxonomy
        Word[] iioParents = w.getIioParents();
        
        if(iioParents != null) {
            for(int i=0; i < iioParents.length; i++) {
                ConceptEntry parent = classify(iioParents[i], seen, d+1);
                entry.add(INSTANCE_OF, parent);
                parent.add(INV_INSTANCE_OF, entry);
            }
        }
        
        Word[] subsenses = w.getSubsenses();
        Word senseParent = null;
        
        //
        // Add this word's subsenses to taxonomy
        if(subsenses != null) {
            for(int i=0; i <subsenses.length; i++) {
                ConceptEntry sub = classify(subsenses[i], seen, d+1);
                entry.add(INV_SENSE_OF, sub);
                sub.add(SENSE_OF, entry);
                // classifying the subsense has added its IKOs
            }
        }
        
        //
        // If this word is a senseword, connect to its parent
        if (w.sensenamep()){
            senseParent = w.getSenseWord();
            ConceptEntry sp = classify(senseParent, seen, d+1);
            entry.add(SENSE_OF, sp);
            sp.add(INV_SENSE_OF, entry);
        }
        
        //
        // If no parents, put under TOP.
        if(directRoots == null &&
                ikoParents == null &&
                iioParents == null &&
                senseParent == null){
            entry.add(KIND_OF, top);
            top.add(INV_KIND_OF, entry);
        }
        
        
        return entry;
    }
    
    protected Lexicon lex;
    
    protected MemoryDictionary dict;
        
    protected Integer topID;
    
    protected ConceptEntry top;
    
    protected static String logTag = "MLT";


    /**
     * Simple test for the taxonomy to decided if it should ignore
     * the token for classification purposes.
     * @param token an instnce of Token that has been created when parsing a document
     * @return a boolean indicating whether or not to ignore the token for classification purposes
     */
    public boolean shouldIgnore(Token token) {
        //
        //quickest test first, does it contain digits?
        if (token.containsDigits()) {
            return true;
        }
        //
        //Is is too long?
        return token.getToken().length() > IGNOREABLE_STRING_LENGTH;
        
    }

    /**
     * Dumps the taxonomy to the given file.
     */
    public void dump(String indexDir, File file) 
     throws java.io.IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        dict.dump(indexDir,
                new StringNameHandler(),
                null,
                raf,
                new PostingsOutput[0],
                MemoryDictionary.Renumber.RENUMBER,
                MemoryDictionary.IDMap.NONE,
                null
                );
    }
} // MemoryTaxonomy
