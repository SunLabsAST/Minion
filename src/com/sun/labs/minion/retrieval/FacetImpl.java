package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.Facet;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.util.Pair;
import com.sun.labs.util.NanoWatch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
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
    
    /**
     * A specification for sorting this facet against other facets when
     * building a set of facets to return.
     */
    protected SortSpec facetSortSpec;
    
    /**
     * The field values that we should use for sorting facets.
     */
    protected Object[] sortFieldValues;
    
    /**
     * The partition local facets that make up this facet.
     */
    protected List<LocalFacet> localFacets = new ArrayList<LocalFacet>(2);
    
    /**
     * The array group that generated this facet.  We'll need this if we want to
     * highlight results that we get from facets later on.
     */
    ArrayGroup arrayGroup;
    
    /**
     * Creates a facet for the given field.
     * @param field 
     */
    public FacetImpl(FieldInfo field, T value, ResultSetImpl set, SortSpec facetSortSpec) {
        this.field = field;
        this.value = value;
        this.set = set;
        this.facetSortSpec = facetSortSpec;
    }
    
    public final void reset(T value) {
        this.value = value;
        localFacets.clear();
        score = 0;
        size = 0;
        if(sortFieldValues != null) {
            Arrays.fill(sortFieldValues, null);
        }
    }

    public void setArrayGroup(ArrayGroup ag) {
        this.arrayGroup = ag;
    }

    public ArrayGroup getArrayGroup() {
        return arrayGroup;
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
    public List<Result> getTopResults(int n, SortSpec sortSpec) {
        NanoWatch nw = new NanoWatch();
        nw.start();
        if(size < n) {
            n = size;
        }
        PriorityQueue<ResultImpl> sorter = new PriorityQueue<ResultImpl>(n, ResultImpl.REVERSE_COMPARATOR);
        ResultImpl curr = new ResultImpl();
        for(LocalFacet lf : localFacets) {
            SortSpec localRSS = null;
            if(sortSpec != null) {
                localRSS = new SortSpec(sortSpec, lf.getPartition());
            }
            for(Iterator<Pair<Integer, Float>> i = lf.iterator(); i.hasNext();) {
                Pair<Integer, Float> doc = i.next();
                curr.init(set, null, localRSS, false, doc.getA(), doc.getB());
                curr.setArrayGroup(lf.arrayGroup);
                curr.setPartition(lf.getPartition());
                curr.setSortFieldValues();
                if(sorter.size() < n) {
                    sorter.offer(curr);
                    curr = new ResultImpl();
                } else {
                    ResultImpl top = sorter.peek();
                    if(sorter.comparator().compare(curr, top) > 0) {
                        top = sorter.poll();
                        sorter.offer(curr);
                        curr = top;
                    }
                }
            }
        }
        
        List<Result> ret = new ArrayList<Result>();
        while(!sorter.isEmpty()) {
            ret.add(sorter.poll());
        }
        Collections.reverse(ret);
        nw.stop();
        return ret;
    }

    public void addLocalFacet(LocalFacet localFacet) {
        if(localFacets.isEmpty()) {
            facetSortSpec = localFacet.facetSortSpec;
        }
        localFacets.add(localFacet);
        size += localFacet.size;
        if(facetSortSpec == null || facetSortSpec.isJustScoreSort()) {
            score = Math.max(score, localFacet.exemplarScore);
        } else {
            
            //
            // Remember the field values that were highest according to the 
            // sorting specification, because we'll use those to (eventually) 
            // sort the facets.
            localFacet.setSortFieldValues();
            if(sortFieldValues == null || 
                    facetSortSpec.compareValues(sortFieldValues, 
                                                localFacet.sortFieldValues,
                                                0, score, 0, 
                                                localFacet.exemplarScore, 
                                                this, localFacet) < 0) {
                sortFieldValues = localFacet.sortFieldValues;
                score = localFacet.exemplarScore;
            }
        }
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
        FacetImpl ofi = (FacetImpl) o;
        int cmp;
        if(facetSortSpec == null || facetSortSpec.isJustScoreSort()) {
            cmp = SortSpec.compareScore(score, ofi.score, SortSpec.Direction.DECREASING);
        } else {
            cmp = facetSortSpec.compareValues(sortFieldValues, ofi.sortFieldValues, size, score, ofi.size, ofi.score, this, ofi);
        }
        
        //
        // Sort by size if all else fails.
        return cmp == 0 ? ofi.size - size : cmp;
    }
    
    /**
     * A comparator that reverses the result of the comparison for a pair of
     * facets. This can be used when we want to generate a min-heap of results
     * during results sorting.
     */
    public static final Comparator REVERSE_COMPARATOR =
            new Comparator<FacetImpl>() {
                @Override
                public int compare(FacetImpl o1,
                                   FacetImpl o2) {
                    return -o1.compareTo(o2);
                }
            };


    @Override
    public String toString() {
        return String.format("%s %.3f (%d)", value, score, size);
    }

}
