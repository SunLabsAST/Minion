package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.Facet;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.minion.util.Pair;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Logger;

/**
 * A single facet.
 */
public class FacetImpl<T extends Comparable> implements Facet<T> {

    private static final Logger logger = Logger.getLogger(FacetImpl.class.getName());
    
    /**
     * The result set that this facet was generated from.
     */
    private ResultSetImpl set;
    
    /**
     * The field that we are a facet of.
     */
    protected FieldInfo field;
    
    /**
     * The value of the facet.
     */
    protected T value;
    
    /**
     * The size of the facet.
     */
    protected int size;
    
    /**
     * The score of the facet.
     */
    protected float score;
    
    protected List<LocalFacet> localFacets = new ArrayList<LocalFacet>(2);
    
    /**
     * Creates a facet for the given field.
     * @param field 
     */
    public FacetImpl(FieldInfo field, T value, ResultSetImpl set) {
        this.field = field;
        this.value = value;
        this.set = set;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public float getScore() {
        return score;
    }

    @Override
    public List<Result> getResults(int n, SortSpec ss) {
        PriorityQueue<ResultImpl> sorter = new PriorityQueue<ResultImpl>(Math.max(n,size));
        Map<Partition,SortSpec> sortSpecs = new HashMap<Partition, SortSpec>();
        if(size < n) {
            n = size;
        }
        for(LocalFacet lf : localFacets) {
            SortSpec lss = null;
            if(ss != null) {
                lss = sortSpecs.get(lf.getPartition());
                if(lss == null) {
                    lss = new SortSpec(ss, lf.getPartition());
                    sortSpecs.put(lf.getPartition(), lss);
                }
            }
            for(Iterator<Pair<Integer, Float>> i = lf.iterator(); i.hasNext();) {
                Pair<Integer, Float> doc = i.next();
                ResultImpl curr = new ResultImpl(set, null, lss,
                                                 false,
                                                 doc.getA(), doc.getB());
                curr.setPartition(lf.getPartition());
                if(sorter.size() < n) {
                    sorter.offer(curr);
                } else {
                    ResultImpl top = sorter.peek();
                    if(curr.compareTo(top) > 0) {
                        sorter.poll();
                        sorter.offer(curr);
                    }
                }
            }
        }
        
        List<Result> ret = new ArrayList<Result>();
        while(!sorter.isEmpty()) {
            ret.add(sorter.poll());
        }
        Collections.reverse(ret);
        return ret;
    }

    
    public void addLocalFacet(LocalFacet localFacet) {
        localFacets.add(localFacet);
    }

    public void setValue(T value) {
        this.value = value;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public void addCount(int n) {
        size += n;
    }

    @Override
    public FieldInfo getField() {
        return field;
    }
    
    @Override
    public int compareTo(Facet<T> o) {
        return value.compareTo(o.getValue());
    }

    @Override
    public String toString() {
        return String.format("%s %.3f (%d)", value, score, size);
    }
}
