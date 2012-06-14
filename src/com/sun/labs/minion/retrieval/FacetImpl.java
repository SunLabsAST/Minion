package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.Facet;
import com.sun.labs.minion.FieldInfo;
import java.util.logging.Logger;

/**
 * A single facet.
 */
public class FacetImpl<T extends Comparable> implements Facet<T> {

    private static final Logger logger = Logger.getLogger(FacetImpl.class.getName());

    /**
     * The field that we are a facet of.
     */
    private FieldInfo field;
    
    /**
     * The value of the facet.
     */
    private T value;
    
    /**
     * The size of the facet.
     */
    private int size;
    
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
    
    protected void add(int n) {
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
        return value + " (" + size() + ')';
    }
}
