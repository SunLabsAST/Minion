package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.Facet;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.util.Pair;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A facet that is local to a single partition.  This is used while building
 * up facets from a number of partitions, so that optimized fetching operations
 * (e.g., fetching a number of dictionary entries) can be used within a single 
 * partition.
 * 
 * The default comparison compares partition facets based on the ID of the value
 * that the represent.
 */
public class LocalFacet<T extends Comparable> extends FacetImpl<T> implements Iterable<Pair<Integer,Float>> {

    InvFileDiskPartition partition;
    
    /**
     * The ID of the value in the given field in this partition for this facet.
     */
    int valueID;

    /**
     * The documents that contributed to this facet and their associated scored.
     */
    List<Pair<Integer,Float>> docs = new ArrayList<Pair<Integer,Float>>(3);
    
    public LocalFacet(InvFileDiskPartition part, FieldInfo field, int facetValueID) {
        super(field, null, null);
        this.partition = part;
        this.valueID = facetValueID;
    }
    
    public int getValueID() {
        return valueID;
    }
    
    public void add(int doc, float score) {
        docs.add(new Pair<Integer,Float>(doc, score));
    }

    public InvFileDiskPartition getPartition() {
        return partition;
    }

    @Override
    public Iterator<Pair<Integer, Float>> iterator() {
        return docs.iterator();
    }

    @Override
    public int compareTo(Facet<T> o) {
        if(o instanceof LocalFacet) {
            return valueID - ((LocalFacet) o).valueID;
        } else {
            return value.compareTo(o.getValue());
        }
    }
    
    public int compareTo(LocalFacet o) {
        return valueID - o.valueID;
    }


    @Override
    public String toString() {
        return "LocalFacet{" + "facetValueID=" + valueID + ", value=" +
                value + ", size=" + size + '}';
    }
}
