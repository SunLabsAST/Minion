package com.sun.labs.minion;


import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.retrieval.ArrayGroup;
import java.util.logging.Logger;

/**
 * A results filter that will only select results where a given field contains
 * a given saved value.
 */
public class FieldValueResultsFilter<V extends Comparable> implements ResultsFilter {

    private static final Logger logger = Logger.getLogger(FieldValueResultsFilter.class.getName());
    
    private String field;
    
    private V value;
    
    private int[] valueID = new int[1];
    
    private InvFileDiskPartition part;
    
    int nTested = 0;
    
    int nPassed = 0;
    
    public FieldValueResultsFilter(String field, V value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public void setPartition(InvFileDiskPartition part) {
        this.part = part;
        valueID[0] = -1;
        DiskField df = part.getDF(field);
        if(df != null && df.getInfo().hasAttribute(FieldInfo.Attribute.SAVED)) {
            DiskDictionary dd = df.getSavedValuesDictionary();
            if(dd != null) {
                QueryEntry<V> entry = dd.get(value);
                if(entry != null) {
                    valueID[0] = entry.getID();
                }
            }
        }
    }

    @Override
    public boolean filter(ResultAccessor ra) {
        nTested++;
        if(ra.contains(field, valueID)) {
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
        return "filter " + field + " for " + value;
    }
}
