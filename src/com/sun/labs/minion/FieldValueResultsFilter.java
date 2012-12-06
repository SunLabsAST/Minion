package com.sun.labs.minion;

import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.retrieval.ArrayGroup;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A results filter that will only select results where a given field containsAny 
 * any one of a number of values.
 */
public class FieldValueResultsFilter<V extends Comparable> implements
        ResultsFilter {

    private static final Logger logger = Logger.
            getLogger(FieldValueResultsFilter.class.getName());
    
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

    /**
     * Creates a results filter that will pass documents that have any one
     * of the provided field values.
     * @param field the field that we wish to filter on
     * @param values the values that we wish to restrict our results to having
     */
    public FieldValueResultsFilter(String field, V... values) {
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
    public FieldValueResultsFilter(FilterType type, String field, V... values) {
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
        if(df != null && df.getInfo().hasAttribute(FieldInfo.Attribute.SAVED)) {
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
            }
        }
    }

    @Override
    public boolean filter(ResultAccessor ra) {
        nTested++;
        boolean debug = ra instanceof ArrayGroup.DocIterator && ((ArrayGroup.DocIterator) ra).getDoc() == 340;
        if(debug) {
            logger.info(String.format("type: %s doc: %d", type, ((ArrayGroup.DocIterator) ra).getDoc()));
        }
        switch(type) {
            case ALL:
                if(!ra.containsAll(field, valueIDs)) {
                    return false;
                }
                break;
            case ANY:
                if(ra.containsAny(field, valueIDs)) {
                    nPassed++;
                    return true;
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
