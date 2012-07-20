package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.WeightedFeature;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 */
public class MultiDocumentVectorImpl extends AbstractDocumentVector {

    private static final Logger logger = Logger.getLogger(MultiDocumentVectorImpl.class.getName());
    
    /**
     * The individual document vectors that make up this multi-document vector.
     */
    private List<DocumentVector> components;
    
    /**
     * The fields used to generate the vectors that we're composed of.
     */
    private Set<FieldInfo> fields;
    
    private MultiDocumentVectorImpl() {
        
    }
    
    public MultiDocumentVectorImpl(SearchEngine e, List<DocumentVector> components) {
        this.e = e;
        this.wf = e.getQueryConfig().getWeightingFunction();
        this.wc = e.getQueryConfig().getWeightingComponents();
        this.components = components;
        this.v = combineFeatures(components);
        fields = new HashSet<FieldInfo>();
        
        //
        // Find the centroid of the components.
        for(DocumentVector component : components) {
            fields.addAll(component.getFields());
        }
    }

    @Override
    public Collection<FieldInfo> getFields() {
        return fields;
    }

    private WeightedFeature[] combineFeatures(List<DocumentVector> vectors) {

        //
        // Ye olde heap-based merge, fabled in song and IR systems.
        PriorityQueue<HE> heap = new PriorityQueue<HE>();
        for(DocumentVector dv : vectors) {
            if(dv instanceof AbstractDocumentVector) {
                HE el = new HE(((AbstractDocumentVector) dv).v);
                if(el.next()) {
                    heap.offer(el);
                }
            }
        }
        
        int nVectors = heap.size();

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
            ret.add(f);
        }

        //
        // Compute the centroid and normalize the length.
        double ss = 0;
        for(WeightedFeature f : ret) {
            float nw = f.getWeight() / nVectors;
            f.setWeight(nw);
            ss += nw * nw;
        }
        length = (float) Math.sqrt(ss);

        return ret.toArray(new WeightedFeature[0]);
    }

    @Override
    public WeightedFeature[] getFeatures() {
        return v;
    }

    @Override
    public DocumentVector copy() {
        MultiDocumentVectorImpl ret = new MultiDocumentVectorImpl();
        ret.components = new ArrayList<DocumentVector>(components);
        ret.fields = new HashSet<FieldInfo>(fields);
        ret.e = e;
        ret.key = key;
        ret.keyEntry = keyEntry;
        ret.wf = wf;
        ret.wc = wc;
        ret.ignoreWords = ignoreWords;
        ret.v = v != null ? v.clone() : null;
        return ret;
    }

    @Override
    public ResultSet findSimilar(String sortOrder, double skimPercent) {
        if(fields.size() == 1) {
            return SingleFieldDocumentVector.findSimilar(e, v, fields.iterator().next(), sortOrder, skimPercent, wf, wc);
        } else {
            return MultiFieldDocumentVector.findSimilar(e, v, fields.toArray(new FieldInfo[0]), sortOrder, skimPercent, wf, wc);
        }
    }
    
    //
    // A class for a heap element.
    protected class HE implements Comparable<HE> {

        int i = -1;

        public WeightedFeature[] fv;

        public WeightedFeature f;

        public HE(WeightedFeature[] fv) {
            this.fv = fv;
        }

        public boolean next() {
            if(i < fv.length - 1) {
                i++;
                f = fv[i];
                return true;
            }
            return false;
        }

        /**
         * Compares the heap elements by the name of the feature at the top of
         * the heap.
         */
        @Override
        public int compareTo(MultiDocumentVectorImpl.HE o) {
            return f.getName().compareTo(o.f.getName());
        }
    }
}
