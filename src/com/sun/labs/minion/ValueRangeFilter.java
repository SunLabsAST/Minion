package com.sun.labs.minion;

import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import java.util.logging.Logger;

/**
 * A results filter that will only select results where a given field has a
 * value in a defined range.
 */
public class ValueRangeFilter<V extends Comparable> implements
        ResultsFilter {

    private static final Logger logger = Logger.
            getLogger(ValueRangeFilter.class.getName());

    private String field;

    private V highValue;

    private int highValueID;

    private V lowValue;

    private int lowValueID;

    private InvFileDiskPartition part;

    private boolean passAny;
    
    private boolean passNone;

    int nTested = 0;

    int nPassed = 0;

    /**
     * Creates a results filter that will pass documents that contain a
     * particular field whose value is in a given range. The two values provided
     * will be construed as a range no matter the order they are given in.
     *
     * @param field the field that we wish to filter on
     * @param v1 the first value for the field. If <code>null</code>, then this
     * will be considered a range from the first value in the field to <code>v2</code>.
     * @param v2 the second value for the field. If <code>null</code> then this
     * range will be considered a range from <code>v1</code> to the end of the stored
     * values.
     */
    public ValueRangeFilter(String field, V v1, V v2) {
        this.field = field;
        
        //
        // Sort out the range. null, null means any value, null, foo means 
        // from the beginning of the values to foo.  foo,null means 
        // from foo to the end of the values.  foo,bar means from foo to bar or 
        // bar to foo (inclusive), whichever makes sense.
        if(v1 == null) {
            if(v2 == null) {
                passAny = true;
            } else {
                highValue = v2;
            }
        } else {
            if(v2 == null) {
                lowValue = v1;
            } else {
                if(v1.compareTo(v2) < 0) {
                    lowValue = v1;
                    highValue = v2;
                } else {
                    lowValue = v2;
                    highValue = v1;
                }
            }
        }
    }

    @Override
    public void setPartition(InvFileDiskPartition part) {
        this.part = part;
        //
        // All shall pass.
        if(passAny) {
            return;
        }
        passNone = false;
        DiskField df = part.getDF(field);
        
        //
        // No data in this field.  None shall pass.
        if(df == null) {
            passNone = true;
            return;
        }
        
        if(df != null && df.getInfo().hasAttribute(FieldInfo.Attribute.SAVED)) {
            DiskDictionary dd = df.getSavedValuesDictionary();
            if(dd == null) {
                passNone = true;
                return;
            }
            if(lowValue != null) {
                QueryEntry<V> entry = dd.getClosest(lowValue);
                if(entry != null) {
                    lowValueID = entry.getID();
                } else {
                    lowValueID = Integer.MAX_VALUE;
                }
            } 
            
            if(highValue != null) {
                QueryEntry<V> entry = dd.getClosest(highValue);
                if(entry != null) {
                    highValueID = entry.getID();
                } else {
                    lowValueID = -1;
                }
            }
        }
    }

    @Override
    public boolean filter(ResultAccessor ra) {
        nTested++;
        if(passNone) {
            return false;
        }
        if(passAny) {
            return true;
        }
        int[] fieldValueIDs = ra.getFieldValueIDs(field);
        for(int i = 1; i < fieldValueIDs[0]+1; i++) {
            int id = fieldValueIDs[i];
            if((lowValue == null || id >= lowValueID) &&
                    (highValue == null || id <= highValueID)) {
                nPassed++;
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
        return "filter " + field + " between " + lowValue + " and " + highValue;
    }

}
