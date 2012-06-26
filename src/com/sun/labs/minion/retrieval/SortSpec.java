/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */
package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.dictionary.DiskDictionaryBundle;
import com.sun.labs.minion.indexer.dictionary.DiskDictionaryBundle.Fetcher;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import java.util.Collections;
import java.util.Comparator;
import java.util.StringTokenizer;
import java.util.logging.Logger;

/**
 * A class to contain a sorting specification for a results set.  Results
 * can be sorted by any combination of saved fields and the score assigned
 * to the document.
 * <P>
 * This class implements a number of comparison methods that can be used to 
 * compare the field values (or the IDs of the field values) with respect to this
 * sorting specification.  The results of these comparison methods could be used
 * to sort the results into the correct order using a method like {@link Collections#sort(java.util.List)}
 * <P>
 * Unfortunately, when we are generating a list of search results, we generally
 * want to select only the top <em>n</em> results.  For this we want to use a 
 * min-heap of size n, where the top of the heap is the <em>n<sup>th</sup></em>.
 * This requires us to reverse the comparison that this class will produce.
 * <P> 
 * We've provided a comparator for this use case.
 */
public class SortSpec {
    
    /**
     * Directions for sorting field values.
     */
    public enum Direction {
        INCREASING,
        DECREASING
    };

    private static final Logger logger = Logger.getLogger(SortSpec.class.getName());

    /**
     * The original specification.
     */
    protected String spec;

    /**
     * The size of the specification, in terms of the number of fields.
     */
    protected int size;

    /**
     * The fields upon which to sort.
     */
    protected FieldInfo[] fields;

    /**
     * Fetchers for any saved fields in the sorting spec.
     */
    protected DiskDictionaryBundle.Fetcher[] fetchers;

    /**
     * The directions in which to sort each of the fields.
     * <code>true</code> indicates that the sort is in an increasing
     * direction, while <code>false</code> indicates that the sort is in a
     * decreasing direction.
     */
    protected Direction[] directions;
    
    protected boolean justScoreSort;
    
    /**
     * The number of decimal places of a score that we want to use in score
     * comparisons.  The default is 3.
     */
    private int scorePlaces = 3;
    
    /**
     * The factor that we'll multiply scores by to get an integer representation
     * of the score.
     */
    private float scoreFactor = 1000;

    /**
     * Creates a sorting specification from the given string description.
     *
     * @param spec The sorting specification.  This is of the form:
     * <tt>[+|-]fieldName{,[+|-]fieldName}{,[+|-]fieldName}...</tt>
     * <p>
     * A <tt>+</tt> indicates that the values from the named field should
     * be sorted in increasing order, and a <tt>-</tt> indicates that the
     * values from the named field should be sorted in decreasing order.
     * If the direction specifier is not given, <tt>+</tt> will be
     * assumed.  If a field given as part of the sorting specification does not
     * exist or is not a saved field, that field will be ignored for the purposes
     * of sorting.
     * @param manager a partition manager.  This is used to retrieve the field
     * information for each of the named fields.
     */
    public SortSpec(PartitionManager manager, String spec) {
        if(spec != null ) {
            this.spec = spec.trim();
            if(this.spec.isEmpty()) {
                this.spec = null;
            }
        } 
        
        if(this.spec == null)  {
            justScoreSort = true;
            return;
        }
        
        StringTokenizer tok = new StringTokenizer(spec, ",");
        size = tok.countTokens();
        fields = new FieldInfo[size];
        directions = new Direction[size];
        for(int i = 0; i < size; i++) {
            String fieldSpec = tok.nextToken().trim();
            String fn;
            char dc = fieldSpec.charAt(0);
            switch(dc) {
                case '+':
                    fn = fieldSpec.substring(1);
                    directions[i] = Direction.INCREASING;
                    break;
                case '-':
                    fn = fieldSpec.substring(1);
                    directions[i] = Direction.DECREASING;
                    break;
                default:
                    fn = fieldSpec;
                    directions[i] = Direction.INCREASING;
                    break;
            }
            if(fn.equalsIgnoreCase("score")) {
                fields[i] = null;
            } else {
                fields[i] = manager.getFieldInfo(fn);
                if(fields[i] == null) {
                    logger.warning(String.format("Unknown field in sort spec: %s", fn));
                } else if(!fields[i].hasAttribute(FieldInfo.Attribute.SAVED)) {
                    logger.warning(String.format("Can't sort on field %s that doesn't have SAVED attribute", fn));
                }
            }
        }
        
        //
        // Are we just sorting by score?
        justScoreSort = fields.length == 1 && fields[0] == null &&
                directions[0] == Direction.DECREASING;
    } // SortSpec constructor

    /**
     * Constructs a partition specific sorting specification that includes fetchers
     * for the saved fields appearing in the sorting specification.
     */
    public SortSpec(SortSpec ss, InvFileDiskPartition part) {
        spec = ss.spec;
        size = ss.size;
        justScoreSort = ss.justScoreSort;
        fields = ss.fields;
        directions = ss.directions;
        fetchers = new Fetcher[size];
        for(int i = 0; i < size; i++) {
            if(fields[i] != null) {
                fetchers[i] = part.getDF(fields[i]).getFetcher();
            }
        }
    }

    public boolean isJustScoreSort() {
        return justScoreSort;
    }

    public Direction getDirection(int i) {
        return directions[i];
    }
    
    public int getIntegerScore(float score) {
        return (int) (score * scoreFactor);
    }

    public void setScorePlaces(int scorePlaces) {
        this.scorePlaces = scorePlaces;
        scoreFactor = (float) Math.pow(10, scorePlaces);
    }
    
    public void getSortFieldValues(Object[] sortValues, int doc, float score) {
        for(int i = 0; i < sortValues.length; i++) {
            getSortFieldValue(i, sortValues, doc, score);
        }
    }
    
    /**
     * Gets a single field value, suitable for use in sorting results.
     *
     * @param field The index of the field in our sort specification whose value we
     * want to retrieve.
     * @param sortValues the values that we're setting
     * @param doc the document for which we want the field value
     * @param score the score associated with the result
     */
    public void getSortFieldValue(int field, Object[] sortValues, int doc, float score) {

        //
        // A field with a zero ID indicates that we're sorting by score.
        if(fields[field] == null) {
            sortValues[field] = new Float(score);
            return;
        }

        //
        // Get a single field value to use for the sort.
        if(fetchers[field] != null) {
            sortValues[field] = fetchers[field].fetchOne(doc);
        } else {
            sortValues[field] = null;
        }

        //
        // Get the default value for the field.
        if(sortValues[field] == null) {
            sortValues[field] = fields[field].getDefaultSavedValue();
        }
    }
    
    public void getSortFieldIDs(int[] sortIDs, int doc) {
        for(int i = 0; i < sortIDs.length; i++) {
            getSortFieldID(i, sortIDs, doc);
        }
    }

    /**
     * Gets the ID for a field value, suitable for sorting results locally to
     * a partition.
     * @param field the field whose value we want
     * @param sortIDs the ids that we're setting.
     * @param doc the document for which we want the id
     */
    public void getSortFieldID(int field, int[] sortIDs, int doc) {

        //
        // A field with a zero ID indicates that we're sorting by score.
        if(fields[field] == null) {
            sortIDs[field] = 0;
            return;
        }

        //
        // Get a single field value to use for the sort.
        if(fetchers[field] != null) {
            sortIDs[field] = fetchers[field].fetchLowID(doc);
        } else {
            sortIDs[field] = -1;
        }
    }
    
    /**
     * Compares two sets of IDs, which are meant to represent IDs taken from
     * a dictionary where the order of the values in the dictionary match the
     * order of the IDs.
     * @param ids1 the first set of IDs
     * @param ids2 the second set of IDs
     * @return less than zero, zero, or greater than zero when the first set of
     * IDs is respectively less than, equal to, or greater than the second
     * set of IDs.
     */
    public int compareIDs(int[] ids1, int[] ids2, float score1, float score2) {
        for(int i = 0; i < ids1.length; i++) {
            int cmp;
            if(fields[i] == null) {
                cmp = compareScore(score1, score2, directions[i]);
            } else {
                cmp = ids1[i] - ids2[i];
                if(directions[i] == Direction.DECREASING) {
                    cmp = -cmp;
                }
            }
            if(cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }
    
    /**
     * Compares two sets of field values, taken from two different documents
     * according to the sorting specification that the object embodies.
     * @param val1 the field values for the first document
     * @param val2 the field values for the second document
     * @param doc1 the id of the first document
     * @param score1 the score of the first document
     * @param doc2 the id of the second document
     * @param score2 the score of the second document
     * @return 
     */
    public int compareValues(Object[] val1, Object[] val2, int doc1, float score1, int doc2, float score2) {
        for(int i = 0; i < val1.length; i++) {

            //
            // Make sure we have this field value in both results.
            if(val1[i] == null) {
                getSortFieldValue(i, val1, doc1, score1);
            }

            if(val2[i] == null) {
                getSortFieldValue(i, val2, doc2, score2);
            }

            //
            // Compare the field values.
            int cmp = ((Comparable) val1[i]).compareTo(val2[i]);

            //
            // If this field is increasing, we can just use the comparison
            // that we just got.
            if(cmp != 0) {
                return directions[i] == Direction.INCREASING ? cmp : -cmp;
            }
        }
        return 0;
    }
    
    /**
     * Compare two scores in decreasing order.
     * @param score1 the first score
     * @param score2 the second score
     * @return -1 if score1 is greater than score2, 1 if score1 is less than score2
     * or 0 if the are equal.
     */
    public static int compareScore(float score1, float score2) {
        return compareScore(score1, score2, Direction.DECREASING);
    }
    
    public static int compareScore(float score1, float score2, Direction direction) {
        int cmp = 0;
        if(score1 < score2) {
            cmp = -1;
        }
        if(score1 > score2) {
            cmp = 1;
        }
        return direction == Direction.INCREASING ? cmp : -cmp;
    }
    
    public static final Comparator RESULT_COMPARATOR = new Comparator<ResultImpl>() {

        @Override
        public int compare(ResultImpl o1, ResultImpl o2) {
            return o1.compareTo(o2);
        }
    };
    
    /**
     * A comparator that reverses the result of the comparison that this sorting
     * specification generates.
     */
    public static final Comparator REVERSE_RESULT_COMPARATOR = new Comparator<ResultImpl>() {
        @Override
        public int compare(ResultImpl o1, ResultImpl o2) {
            return -o1.compareTo(o2);
        }
    };
    
    public static final Comparator FACET_COMPARATOR = new Comparator<FacetImpl>() {

        @Override
        public int compare(FacetImpl o1, FacetImpl o2) {
            return o1.compareTo(o2);
        }
    };

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < fields.length; i++) {
            if(i > 0) {
                sb.append(", ");
            }
            sb.append(directions[i]).append(' ');
            if(fields[i] == null) {
                sb.append("score");
            }
        }
        return sb.toString();
    }
} // SortSpec
