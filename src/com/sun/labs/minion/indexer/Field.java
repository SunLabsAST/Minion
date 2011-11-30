package com.sun.labs.minion.indexer;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Stemmer;
import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.minion.util.CDateParser;
import java.util.logging.Logger;

/**
 * An abstract base class for fields.
 */
public abstract class Field {
    private static final Logger logger = Logger.getLogger(Field.class.getName());
    
    protected boolean cased;

    protected boolean saved;

    protected boolean stemmed;

    protected boolean tokenized;

    protected boolean uncased;

    protected boolean vectored;

    protected CDateParser dateParser;

    protected FieldInfo info;

    protected Partition partition;
    
    /**
     * A stemmer for stemming.
     */
    protected Stemmer stemmer;

    public Field(Partition partition, FieldInfo info) {
        this.partition = partition;
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

    public FieldInfo getInfo() {
        return info;
    }

    public Partition getPartition() {
        return partition;
    }

    public Stemmer getStemmer() {
        return stemmer;
    }

    public abstract int getMaximumDocumentID();

}
