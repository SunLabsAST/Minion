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
 * Local facets are always compared by the name (well, the dictionary ID) of the
 * values, since we need to combine the local facets from different partitions by
 * name.  We will sort the documents that make up the facet according to an 
 * external sorting specification and keep an exemplar document that is the 
 * maximum document according to that sorting specification.
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

    /**
     * The document that is an exemplar for this facet.  This is the document
     * that has the maximum field values according to the sorting specification
     * for these facets.
     */
    protected int exemplar;

    /**
     * The score associated with the exemplar document.
     */
    protected float exemplarScore;
    
    /**
     * The IDs for the field values that we'll use to sort this exemplar.
     */
    protected int[] exemplarSortFieldIDs;
    
    /**
     * A holder for a set of field value IDs that we want to check against the
     * exemplar.
     */
    private int[] tempSortFieldIDs;
    
    private SortSpec resultSortSpec;
    
    public LocalFacet(InvFileDiskPartition part, FieldInfo field,
                      int facetValueID, SortSpec resultSortSpec) {
        super(field, null, null, null);
        this.partition = part;
        this.valueID = facetValueID;
        this.resultSortSpec = resultSortSpec;
    }
    
    public int getValueID() {
        return valueID;
    }
    
    public void add(int doc, float score) {
        docs.add(new Pair<Integer,Float>(doc, score));
        
        //
        // Keep track of the "maximum" document in the facet, using the 
        // provided sorting criterion or just the score if that's all we're 
        // doing.
        if(resultSortSpec == null || resultSortSpec.isJustScoreSort()) {
            if(score > exemplarScore) {
                exemplar = doc;
                exemplarScore = score;
            }
        } else {
            if(tempSortFieldIDs == null) {
                tempSortFieldIDs = new int[resultSortSpec.size];
            }
            resultSortSpec.getSortFieldIDs(tempSortFieldIDs, doc);
            if(exemplarSortFieldIDs == null) {
                exemplar = doc; 
                exemplarSortFieldIDs = tempSortFieldIDs;
                exemplarScore = score;
            } else if(resultSortSpec.compareIDs(tempSortFieldIDs, exemplarSortFieldIDs, score, exemplarScore) > 0) {
                exemplar = doc;
                int[] tmp = exemplarSortFieldIDs;
                exemplarSortFieldIDs = tempSortFieldIDs;
                tempSortFieldIDs = tmp;
                exemplarScore = score;
            }
        }
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
        
        //
        // Local facets are always compared by name.
        if(o instanceof LocalFacet) {
            return valueID - ((LocalFacet) o).valueID;
        } else {
            return value.compareTo(o.getValue());
        }
    }
    
    protected void setSortFieldValues() {
        sortFieldValues = new Object[facetSortSpec.size];
        resultSortSpec.getSortFieldValues(sortFieldValues, exemplar, exemplarScore, this);
    }

    @Override
    public String toString() {
        return String.format("LocalFacet{facetValueID=%d value=%s size=%d exemplarScore=%f\n\t\tdocs=%s",
                             valueID, value, size, exemplarScore, docs);
    }
}
