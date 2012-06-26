package com.sun.labs.minion;

import com.sun.labs.minion.retrieval.SortSpec;
import java.util.Comparator;
import java.util.List;

/**
 * A single facet built from a set of documents.
 */
public interface Facet<T extends Comparable> extends Comparable<Facet<T>> {
    
    /**
     * Gets the field from which the facet was generated.
     *
     * @return the field.
     */
    public FieldInfo getField();

    /**
     * Gets the value of the facet.
     *
     * @return the value.
     */
    public T getValue();

    /**
     * Gets the size of the facet.
     *
     * @return the number of documents in the facet.
     */
    public int size();

    /**
     * Gets the score associated with the facet.
     *
     * @return the score associated with this facet.
     */
    public float getScore();
    
    /**
     * Gets the top results in this facet, sorted by the given sorting specification.
     * @param n the number of results to get.
     * @param sortSpec a sorting specification to use for the results.
     */
    public List<Result> getResults(int n, SortSpec sortSpec);
    
    /**
     * A comparator that will return facets in increasing order of size.
     *
     * @see ResultSet#getTopFacets(java.lang.String, java.util.Comparator,
     * com.sun.labs.minion.ScoreCombiner, int)
     */
    public static final Comparator<Facet> FACET_SIZE_COMPARATOR = new Comparator<Facet>() {
        @Override
        public int compare(Facet o1, Facet o2) {
            return o1.size() - o2.size();
        }
    };

    /**
     * A comparator that will return facets in the natural order of score (i.e.,
     * lowest score first.)
     *
     *
     * @see ResultSet#getTopFacets(java.lang.String, java.util.Comparator,
     * com.sun.labs.minion.ScoreCombiner, int)
     */
    public static final Comparator<Facet> FACET_SCORE_COMPARATOR = new Comparator<Facet>() {
        @Override
        public int compare(Facet o1, Facet o2) {
            float diff = o1.getScore() - o2.getScore();
            if(diff < 0) {
                return -1;
            } else if(diff > 0) {
                return 1;
            }
            return 0;
        }
    };

    /**
     * A comparator that will compare facets into increasing order by the name
     * of the facet.
     *
     *
     * @see ResultSet#getTopFacets(java.lang.String, java.util.Comparator,
     * com.sun.labs.minion.ScoreCombiner, int)
     */
    public static final Comparator<Facet> FACET_NAME_COMPARATOR = new Comparator<Facet>() {
        @Override
        public int compare(Facet o1, Facet o2) {
            return ((Comparable) o1.getValue()).compareTo(o2.getValue());
        }
    };

}
