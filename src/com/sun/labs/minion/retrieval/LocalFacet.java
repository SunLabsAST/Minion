package com.sun.labs.minion.retrieval;


import com.sun.labs.minion.Facet;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

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

    private static final Logger logger = Logger.getLogger(LocalFacet.class.getName());

    /**
     * The partition from which this facet was generated.
     */
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
    
     /**
     * Creates a facet that is local to a single partition.
     * @param part the partition that this facet is local to
     * @param field the field that we're a facet of
     * @param facetValueID the ID of the value of the field that we're a facet
     * of.
     * @param facetSortSpec the sorting specification that will be used for facets.
     * We use this to keep track of the "top" field values for the documents 
     * in this facet.  These top values will be used to sort facets at the top
     * level, even though local facets will always be compared by names only.
     */
    public LocalFacet(InvFileDiskPartition part, FieldInfo field,
                      int facetValueID, SortSpec facetSortSpec) {
        super(field, null, null, null);
        this.partition = part;
        this.valueID = facetValueID;
        this.facetSortSpec = facetSortSpec;
    }
    
    public int getValueID() {
        return valueID;
    }
    
    public void add(int doc, float score) {
        docs.add(new Pair<Integer,Float>(doc, score));
        
        //
        // Keep track of the "maximum" document in the facet, using the 
        // provided sorting criterion or just the score if that's all we're 
        // doing. This will be needed later when we're sorting the facets to 
        // determine which facets to return to the user.
        //
        // We keep track of this here because at this point we can do the
        // sorting by considering IDs, rather than values.  See FacetImpl.addLocalFacet
        // for the point where we combine exemplar values across partitions.
        if(facetSortSpec == null || facetSortSpec.isJustScoreSort()) {
            if(score > exemplarScore) {
                exemplar = doc;
                exemplarScore = score;
            }
        } else {
            if(tempSortFieldIDs == null) {
                tempSortFieldIDs = new int[facetSortSpec.size];
            }
            facetSortSpec.getSortFieldIDs(tempSortFieldIDs, doc);
            if(exemplarSortFieldIDs == null) {
                exemplar = doc; 
                exemplarSortFieldIDs = tempSortFieldIDs;
                exemplarScore = score;
            } else if(facetSortSpec.compareIDs(tempSortFieldIDs, exemplarSortFieldIDs, score, exemplarScore) > 0) {
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
        if(value != null) {
            return value.compareTo(o.getValue());
        } else if(o instanceof LocalFacet) {
            return valueID - ((LocalFacet) o).valueID;
        } else {
            throw new IllegalStateException(String.format("Not sure what to compare: %s %s", value, o.getClass()));
        }
    }
    
    protected void setSortFieldValues() {
        if(facetSortSpec != null) {
            sortFieldValues = new Object[facetSortSpec.size];
            facetSortSpec.getSortFieldValues(sortFieldValues, exemplar,
                                             exemplarScore, this);
        }
    }

    @Override
    public String toString() {
        return String.format("value: %s id: %d sortVals: %s", value, valueID, 
                                                                     sortFieldValues == null ? "null":
                Arrays.toString(
                sortFieldValues));
    }
}
