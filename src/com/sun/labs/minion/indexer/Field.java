package com.sun.labs.minion.indexer;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Stemmer;
import com.sun.labs.minion.StemmerFactory;
import com.sun.labs.minion.indexer.dictionary.Dictionary;
import com.sun.labs.minion.indexer.partition.Partition;
import com.sun.labs.minion.util.CDateParser;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An abstract base class for fields.
 */
public abstract class Field<N extends Comparable> {

    private static final Logger logger = Logger.getLogger(Field.class.getName());

    protected boolean cased;

    protected boolean saved;

    protected boolean stemmed;

    protected boolean uncased;

    protected boolean vectored;
    
    protected boolean indexed;

    protected CDateParser dateParser;

    protected FieldInfo info;

    protected Partition partition;
    
    /**
     * A stemmer for stemming.
     */
    protected Stemmer stemmer;
    
    public enum DictionaryType {

        /**
         * Tokens in the case found in the document.
         */
        CASED_TOKENS, 
        /**
         * Tokens transformed into lowercase.
         */
        UNCASED_TOKENS, 
        /**
         * Tokens that have been transformed into lowercase and stemmed.
         */
        STEMMED_TOKENS, 
        /**
         * Raw saved values from the document.
         */
        RAW_SAVED, 
        /**
         * Lowercased saved values from the document. Only used if this is a
         * string field.
         */
        UNCASED_SAVED, 
        /**
         * A document vector with the cased tokens from the document.
         */
        CASED_VECTOR, 
        /**
         * A document vector with the uncased tokens from the document.
         */
        UNCASED_VECTOR,
        /**
         * A document vector with the lowercased, stemmed tokens from the
         * document.  This will only be generated if a field has a STEMMED
         * dictionary.
         */
        STEMMED_VECTOR, 
        /**
         * A dictionary for the token bigrams. Bigrams are generated from the
         * uncased entries.
         */
        TOKEN_BIGRAMS, 
        /**
         * A dictionary for saved value bigrams. Bigrams are generated from the
         * uncased saved entries.
         */
        SAVED_VALUE_BIGRAMS
    }

    /**
     * The types of document vectors that we store.
     */
    public enum DocumentVectorType {
        CASED,
        UNCASED,
        STEMMED;
        
        public static TermStatsType getTermStatsType(DocumentVectorType type) {
            switch(type) {
                case CASED:
                    return TermStatsType.CASED;
                case UNCASED:
                    return TermStatsType.UNCASED;
                case STEMMED:
                    return TermStatsType.STEMMED;
            }
            throw new IllegalArgumentException("Unknown document vector type " + type);
        }
        
        public static DictionaryType getDictionaryType(DocumentVectorType type) {
            switch(type) {
                case CASED:
                    return DictionaryType.CASED_TOKENS;
                case UNCASED:
                    return DictionaryType.UNCASED_TOKENS;
                case STEMMED:
                    return DictionaryType.STEMMED_TOKENS;
            }
            throw new IllegalArgumentException("Unknown document vector type "
                    + type);
        }
    }
    
    /**
     * The types of term stats that we might want for a field.
     */
    public enum TermStatsType {
        CASED,
        UNCASED,
        STEMMED;
        
        public static DocumentVectorType getDocumentVectorType(TermStatsType type) {
            switch(type) {
                case CASED:
                    return DocumentVectorType.CASED;
                case UNCASED:
                    return DocumentVectorType.UNCASED;
                case STEMMED:
                    return DocumentVectorType.STEMMED;
            }
            throw new IllegalArgumentException("Unknown term stats type " + type);
        }
        
        public static DictionaryType getDictionaryType(
                TermStatsType type) {
            switch(type) {
                case CASED:
                    return DictionaryType.CASED_TOKENS;
                case UNCASED:
                    return DictionaryType.UNCASED_TOKENS;
                case STEMMED:
                    return DictionaryType.STEMMED_TOKENS;
            }
            throw new IllegalArgumentException("Unknown term stats type " + type);
        }
    }

    public Field(Partition partition, FieldInfo info) {
        this.partition = partition;
        this.info = info;
        indexed = info.hasAttribute(FieldInfo.Attribute.INDEXED);
        stemmed = info.hasAttribute(FieldInfo.Attribute.STEMMED);
        saved = info.hasAttribute(FieldInfo.Attribute.SAVED);
        cased = info.hasAttribute(FieldInfo.Attribute.CASED);
        uncased = info.hasAttribute(FieldInfo.Attribute.UNCASED);
        vectored = info.hasAttribute(FieldInfo.Attribute.VECTORED);

        if(info.getType() != FieldInfo.Type.STRING && info.getType() != FieldInfo.Type.NONE && uncased) {
            logger.warning(String.format("Field %s of type %s has UNCASED attribute, which "
                    + "doesn't make sense!", info.getName(), info.getType()));
        }
        
        //
        // Get the stemmer for this field.
        if(stemmed) {
            String stemmerFactoryName = info.getStemmerFactoryName();
            if(stemmerFactoryName != null) {
                StemmerFactory sf =
                        (StemmerFactory) partition
                        .getPartitionManager()
                        .getConfigurationManager()
                        .lookup(stemmerFactoryName);
                if(sf == null) {
                    logger.log(Level.SEVERE, String.
                            format(
                            "Unknown stemmer factory name %s",
                            stemmerFactoryName));
                    throw new IllegalArgumentException(String.format(
                            "Unknown stemmer factory name %s for field %s",
                            stemmerFactoryName,
                            info.getName()));
                }
                stemmer = sf.getStemmer();
            }
        }
    }
    
    public abstract Dictionary getDictionary(DictionaryType type);
    
    /**
     * Gets a term dictionary of the given type.
     * @return a dictionary of terms for the field, if one exists.
     */
    public Dictionary getTermDictionary(TermStatsType termStatsType) {
        switch(termStatsType) {
            case CASED:
                return getDictionary(DictionaryType.CASED_TOKENS);
            case UNCASED:
                return getDictionary(DictionaryType.UNCASED_TOKENS);
            case STEMMED:
                return getDictionary(DictionaryType.STEMMED_TOKENS);
            default:
                logger.log(Level.SEVERE, String.format(
                        "Unknown term stats type %s", termStatsType));
                return null;
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
    
    public boolean isIndexed() {
        return indexed;
    }
    
    public boolean isCased() {
        return cased;
    }

    public boolean isSaved() {
        return saved;
    }

    public boolean isStemmed() {
        return stemmed;
    }

    public boolean isUncased() {
        return uncased;
    }

    public boolean isVectored() {
        return vectored;
    }

    public abstract int getMaximumDocumentID();
}
