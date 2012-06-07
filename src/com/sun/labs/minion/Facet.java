package com.sun.labs.minion;

import java.util.List;


/**
 * A single facet built from a set of documents.
 */
public interface Facet<T extends Comparable> extends Comparable<Facet<T>> {

    /**
     * Gets the field from which the facet was generated.
     * @return the field.
     */
    public FieldInfo getField();
    
    /**
     * Gets the value of the facet.
     * @return the value.
     */
    public T getValue();
    /**
     * Gets the size of the facet.
     * @return the number of documents in the facet.
     */
    public int size();
    
    /**
     * Gets the results that make up this facet.
     * @return the results in this facet.
     */ 
    public List<Result> getResults();
    
}
