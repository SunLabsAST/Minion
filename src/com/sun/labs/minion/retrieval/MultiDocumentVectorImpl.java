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

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.SearchEngine;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import com.sun.labs.minion.classification.WeightedFeature;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.FieldedDocKeyEntry;

/**
 * An implementation of a document vector that combines the vectors for
 * a number of documents into a single document.
 */
public class MultiDocumentVectorImpl extends DocumentVectorImpl implements DocumentVector {
    
    List<DocKeyEntry> keys;
    
    public MultiDocumentVectorImpl(List<DocumentVector> dvs) {
        List<WeightedFeature[]> fvs = new ArrayList<WeightedFeature[]>(dvs.size());
        for(DocumentVector dv : dvs) {
            fvs.add(((DocumentVectorImpl) dv).getFeatures());
        }
        v = combineFeatures(fvs);
    }
    
    /**
     * Creates a document vector for a set of documents.  The resulting document
     * vector is the centroid of the vectors fro the individual documnts.
     *
     * @param e The search engine with which the docuemnt is associated.
     * @param key The entry from the document dictionary for the given
     * document.
     * @param field The name of the field for which we want the document vector.
     * If this value is <code>null</code> a vector for the whole document will
     * be returned.  If this value is the empty string, then a vector for the text
     * not in any defined field will be returned.  If the named field is not a
     * field that was indexed with the
     * vectored attribute set, the resulting document vector will be empty!
     */
    public MultiDocumentVectorImpl(SearchEngine e,
                               List<DocKeyEntry> keys, String field) {
        this(e, keys, field, e.getQueryConfig().getWeightingFunction(),
             e.getQueryConfig().getWeightingComponents());
    }

    public MultiDocumentVectorImpl(SearchEngine e,
                               List<DocKeyEntry> keys, String field,
                               WeightingFunction wf,
                               WeightingComponents wc) {

        this.e = e;
        this.keys = keys;
        setField(field);
        this.wf = wf;
        this.wc = wc;
        v = initFeatures();
    }
    
    public String getKey() {
        return "multidoc";
    }
    
    private WeightedFeature[] initFeatures() {
        List<WeightedFeature[]> fvs = new ArrayList<WeightedFeature[]>();
        for(DocKeyEntry dke : keys) {
            if(key instanceof FieldedDocKeyEntry) {
                fvs.add(((FieldedDocKeyEntry) key).getWeightedFeatures(fieldID, wf,
                        wc));
            } else {
                fvs.add(key.getWeightedFeatures(wf, wc));
            }
        }
        return combineFeatures(fvs);
    }
    
    private WeightedFeature[] combineFeatures(List<WeightedFeature[]> fvs) {
        
        //
        // Ye olde heap-based merge, fabled in song and IR systems.
        PriorityQueue<HE> heap = new PriorityQueue<HE>();
        for(WeightedFeature[] fv : fvs) {
            HE el = new HE(fv);
            if(el.next()) {
                heap.offer(el);
            }
        }
        
        List<WeightedFeature> ret = new ArrayList<WeightedFeature>();
        while(heap.size() > 0) {
            HE top = heap.peek();
            WeightedFeature f = new WeightedFeature(top.f.getName());
            while(top != null && top.f.getName().equals(f.getName())) {
                top = heap.poll();
                f.setWeight(f.getWeight() + top.f.getWeight());
                if(top.next()) {
                    heap.offer(top);
                }
                top = heap.peek();
            }
            f.setWeight(f.getWeight() / fvs.size());
            ret.add(f);
        }
        
        //
        // Compute the length.
        double ss = 0;
        for(WeightedFeature f : ret) {
            ss += f.getWeight() * f.getWeight();
        }
        length = (float) Math.sqrt(ss);
        
        return ret.toArray(new WeightedFeature[0]);
    }

    class HE implements Comparable<HE> {
        int i = -1;
        
        WeightedFeature[] fv;
        
        WeightedFeature f;
        
        public HE(WeightedFeature[] fv) {
            this.fv = fv;
        }
        
        public boolean next() {
            if(i < fv.length-1) {
                i++;
                f = fv[i];
                return true;
            }
            return false;
        }

        /**
         * Compares the heap elements by the name of the feature at the top
         * of the heap.
         */
        public int compareTo(MultiDocumentVectorImpl.HE o) {
            return f.getName().compareTo(o.f.getName());
        }
        
    }
    

}
