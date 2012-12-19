package com.sun.labs.minion;

import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A results filter that will only select results where a given field containsAny 
 * any one of a number of values.
 */
public class DistinctValuesFilter<V extends Comparable> implements
        ResultsFilter {

    private static final Logger logger = Logger.
            getLogger(DistinctValuesFilter.class.getName());
    
    /**
     * The type of filter that should be applied to the values. 
     */
    public enum FilterType {
        /**
         * Documents that pass the filter must have all of the field values.
         */
        ALL,
        /**
         * Documents that pass the filter may have any of the field values.
         */
        ANY
    }

    private String field;

    private V[] values;
    
    private FilterType type;

    private int[] valueIDs;

    private InvFileDiskPartition part;

    int nTested = 0;

    int nPassed = 0;
    
    boolean passNone;
    
    /**
     * Creates a results filter that will pass documents that have any one
     * of the provided field values.
     * @param field the field that we wish to filter on
     * @param values the values that we wish to restrict our results to having
     */
    public DistinctValuesFilter(String field, V... values) {
        this(FilterType.ANY, field, values);
    }
    
    /**
     * Creates a results filter that will pass documents that have any or all
     * of the provided field values, depending on the type of filter specified.
     *
     * @param type the type of filter to apply.
     * @param field the field that we wish to filter on
     * @param values the values that we wish to restrict our results to having
     */
    public DistinctValuesFilter(FilterType type, String field, V... values) {
        this.type = type;
        this.field = field;
        this.values = values;
        valueIDs = new int[values.length];
        
    }

    @Override
    public void setPartition(InvFileDiskPartition part) {
        this.part = part;
        valueIDs[0] = -1;
        DiskField df = part.getDF(field);
        passNone = df == null || !df.getInfo().hasAttribute(FieldInfo.Attribute.SAVED);
        if(!passNone) {
            DiskDictionary dd = df.getSavedValuesDictionary();
            if(dd != null) {
                for(int i = 0; i < values.length; i++) {
                    QueryEntry<V> entry = dd.get(values[i]);
                    if(entry != null) {
                        valueIDs[i] = entry.getID();
                    } else {
                        valueIDs[i] = -1;
                    }
                }
            } else {
                passNone = true;
            }
        }
    }

    @Override
    public boolean filter(ResultAccessor ra) {
        nTested++;
        if(passNone) {
            return false;
        }
        int[] fieldValueIDs = ra.getFieldValueIDs(field);
        switch(type) {
            case ALL:
                for(int id : valueIDs) {
                    if(!contains(fieldValueIDs, id)) {
                        return false;
                    }
                }
                break;
            case ANY:
                for(int id : valueIDs) {
                    if(contains(fieldValueIDs, id)) {
                        nPassed++;
                        return true;
                    }
                }
                break;
        }
        switch(type) {
            case ALL:
                nPassed++;
                return true;
            case ANY:
                return false;
            default:
                return false;
        }
    }
    
    private boolean contains(int[] fieldValueIDs, int id) {
        for(int i = 1; i < fieldValueIDs[0]+1; i++) {
            if(id == fieldValueIDs[i]) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int getTested() {
        return nTested;
    }

    @Override
    public int getPassed() {
        return nPassed;
    }

    @Override
    public String toString() {
        return "filter " + field + " for " + type + " " + Arrays.toString(values);
    }

}
