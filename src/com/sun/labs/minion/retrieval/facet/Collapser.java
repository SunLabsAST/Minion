package com.sun.labs.minion.retrieval.facet;


/**
 * An interface that can be used to collapse multiple facet values into a 
 * single one based on some application-defined criteria.  For example, a collapser 
 * can be used to collapse full date fields into "Last 24 hours, Last week, 
 * Last 30 days" facets.
 */
public interface Collapser<N,R> {

    /**
     * Collapses a given facet name into another, which may be of a different
     * type.
     * @param name the name we want to collapse
     * @return the new name of the facet, which may be a different type.
     */
    R collapse(N name);
}
