package com.sun.labs.minion.indexer.postings;

import java.util.logging.Logger;

/**
 *
 */
public class FieldOccurrenceImpl extends OccurrenceImpl implements FieldOccurrence {
    
    private static final Logger logger = Logger.getLogger(FieldOccurrenceImpl.class.getName());
    
    private int pos;
    
    private int[] fields;

    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int[] getFields() {
        return fields;
    }

    public void setFields(int[] fields) {
        this.fields = fields;
    }

}
