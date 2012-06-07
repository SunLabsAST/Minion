package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.Facet;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Result;
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
    private FieldInfo field;
    
    /**
     * The value of the facet.
     */
    private T value;
    
    /**
     * The results in this facet.
     */
    private List<Result> results;

    /**
     * Creates a facet for the given field.
     * @param field 
     */
    public FacetImpl(FieldInfo field, T value) {
        this.field = field;
        this.value = value;
        results = new ArrayList<Result>();
    }
    
    public void add(Result r) {
        results.add(r);
    }

    @Override
    public FieldInfo getField() {
        return field;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public int size() {
        return results.size();
    }
    
    @Override
    public List<Result> getResults() {
        return results;
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
