package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.Facet;
import com.sun.labs.minion.FieldInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * A single facet.
 */
public class FacetImpl<T extends Comparable> implements Facet<T> {

    private static final Logger logger = Logger.getLogger(FacetImpl.class.getName());

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
    public FacetImpl(FieldInfo field, T value) {
        this.field = field;
        this.value = value;
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
        return String.format("%s %.2f (%d)", value, score, size);
    }
}
