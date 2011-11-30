package com.sun.labs.minion.indexer;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.minion.util.CDateParser;
import java.util.logging.Logger;

/**
 * An abstract base class for fields.
 */
public abstract class Field {
    private static Logger logger = Logger.getLogger(Field.class.getName());
    
    public static int CASED_TOKENS = 0;

    public static int UNCASED_TOKENS = 1;

    public static int STEMMED_TOKENS = 2;

    public static int RAW_SAVED = 3;

    public static int UNCASED_SAVED = 4;

    public static int VECTORS = 5;

    public static int NUM_DICTS = 6;

    protected boolean cased;

    protected boolean saved;

    protected boolean stemmed;

    protected boolean tokenized;

    protected boolean uncased;

    protected boolean vectored;

    protected CDateParser dateParser;

    protected FieldInfo info;

    protected Partition partition;

    public Field(FieldInfo info) {
        this.info = info;
        tokenized = info.hasAttribute(FieldInfo.Attribute.TOKENIZED);
        stemmed = info.hasAttribute(FieldInfo.Attribute.STEMMED);
        saved = info.hasAttribute(FieldInfo.Attribute.SAVED);
        cased = info.hasAttribute(FieldInfo.Attribute.CASED);
        uncased = info.hasAttribute(FieldInfo.Attribute.UNCASED);

        if(info.getType() != FieldInfo.Type.STRING && uncased) {
            logger.warning(String.format("Field %s of type %s has UNCASED attribute, which "
                    + "doesn't make sense!", info.getName(), info.getType()));
        }
    }

    public Partition getPartition() {
        return partition;
    }

    public abstract int getMaximumDocumentID();

}
