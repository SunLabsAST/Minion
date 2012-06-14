package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import java.util.Comparator;
import java.util.logging.Logger;

/**
 * A facet that is local to a single partition.  This is used while building
 * up facets from a number of partitions, so that optimized fetching operations
 * (e.g., fetching a number of dictionary entries) can be used within a single 
 * partition.
 * 
 * The default comparison compares partition facets based on the ID of the value
 * that the represent.
 */
public class LocalFacet<N> implements Comparable<LocalFacet> {

    private static final Logger logger = Logger.getLogger(LocalFacet.class.getName());
    
    InvFileDiskPartition part;
    
    /**
     * The field from which the facet has been drawn.
     */
    FieldInfo field;
    
    /**
     * The ID of the value in the given field for this facet.
     */
    int facetValueID;
    
    /**
     * The value of the facet.
     */
    N facetValue;

    /**
     * The size of the facet.
     */
    int size;
    
    /**
     * A comparator that will compare local facets by the name of the facet.
     */
    public static final Comparator<LocalFacet> NAME_COMPARATOR = new Comparator<LocalFacet>() {
        @Override
        public int compare(LocalFacet o1, LocalFacet o2) {
            return ((Comparable) o1.getFacetValue()).compareTo(o2.getFacetValue());
        }
    };
    
    public LocalFacet(InvFileDiskPartition part, FieldInfo field, int facetValueID) {
        this.part = part;
        this.field = field;
        this.facetValueID = facetValueID;
    }
    
    public void add(int size) {
        this.size += size;
    }
    
    public int size() {
        return size;
    }

    public N getFacetValue() {
        return facetValue;
    }

    public void setFacetValue(N facetValue) {
        this.facetValue = facetValue;
    }

    public int getFacetValueID() {
        return facetValueID;
    }

    public FieldInfo getField() {
        return field;
    }

    @Override
    public int compareTo(LocalFacet o) {
        return facetValueID - o.facetValueID;
    }

    @Override
    public String toString() {
        return "LocalFacet{" + "facetValueID=" + facetValueID + ", facetValue=" +
                facetValue + ", size=" + size + '}';
    }
}
