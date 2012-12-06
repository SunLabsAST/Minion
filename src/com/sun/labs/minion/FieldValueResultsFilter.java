package com.sun.labs.minion;

import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A results filter that will only select results where a given field contains 
 * any one of a number of values.
 */
public class FieldValueResultsFilter<V extends Comparable> implements
        ResultsFilter {

    private static final Logger logger = Logger.
            getLogger(FieldValueResultsFilter.class.getName());

    private String field;

    private V[] values;

    private int[] valueIDs;

    private InvFileDiskPartition part;

    int nTested = 0;

    int nPassed = 0;

    /**
     * Creates a results filter that will pass any document that has any one
     * of the provided field values.
     * @param field the field that we wish to filter on
     * @param values the values that we wish to restrict our results to having
     */
    public FieldValueResultsFilter(String field, V... values) {
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
                    }
                }
            }
        }
    }

    @Override
    public boolean filter(ResultAccessor ra) {
        nTested++;
        if(ra.contains(field, valueIDs)) {
            nPassed++;
            return true;
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
        return "filter " + field + " for " + Arrays.toString(values);
    }

}
