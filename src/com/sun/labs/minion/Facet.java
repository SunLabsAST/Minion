package com.sun.labs.minion;

import java.util.Comparator;

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
     * A comparator that will return facets in reverse order of score (i.e., 
     * highest score first.)
     */
    public static final Comparator<Facet> FACET_REVERSE_SCORE_COMPARATOR = new Comparator<Facet>() {
        @Override
        public int compare(Facet o1, Facet o2) {
            float diff = o1.getScore() - o2.getScore();
            if(diff < 0) {
                return 1;
            } else if(diff > 0) {
                return -1;
            }
            return 0;
        }
    };

    /**
     * A comparator that will return facets in reverse order of size (i.e.,
     * largest facets first.)
     */
    public static final Comparator<Facet> FACET_REVERSE_SIZE_COMPARATOR = new Comparator<Facet>() {
        @Override
        public int compare(Facet o1, Facet o2) {
            return o1.size() - o2.size();
        }
    };

    /**
     * A comparator that will compare facets into increasing order by the name
     * of the facet.
     */
    public static final Comparator<Facet> NAME_COMPARATOR = new Comparator<Facet>() {
        @Override
        public int compare(Facet o1, Facet o2) {
            return ((Comparable) o1.getValue()).compareTo(o2.getValue());
        }
    };

}
