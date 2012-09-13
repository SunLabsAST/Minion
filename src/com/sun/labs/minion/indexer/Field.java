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

    protected boolean tokenized;

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
         * A document vector with the raw tokens from the document.
         */
        RAW_VECTOR, 
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
        RAW,
        STEMMED,
    }
    
    /**
     * The types of term stats that we might want for a field.
     */
    public enum TermStatsType {
        RAW,
        STEMMED
    }

    public Field(Partition partition, FieldInfo info) {
        this.partition = partition;
        this.info = info;
        indexed = info.hasAttribute(FieldInfo.Attribute.INDEXED);
        tokenized = info.hasAttribute(FieldInfo.Attribute.TOKENIZED);
        stemmed = info.hasAttribute(FieldInfo.Attribute.STEMMED);
        saved = info.hasAttribute(FieldInfo.Attribute.SAVED);
        cased = info.hasAttribute(FieldInfo.Attribute.CASED);
        uncased = info.hasAttribute(FieldInfo.Attribute.UNCASED);
        vectored = info.hasAttribute(FieldInfo.Attribute.VECTORED);

        if(info.getType() != FieldInfo.Type.STRING && info.getType()
                != FieldInfo.Type.NONE && uncased) {
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
     * Gets a term dictionary, preferring uncased over cased dictionaries, if
     * they exist.
     * @return a dictionary of terms for the field, if one exists.
     */
    public Dictionary getTermDictionary() {
        if(uncased) {
            return getDictionary(DictionaryType.UNCASED_TOKENS);
        } else {
            return getDictionary(DictionaryType.CASED_TOKENS);
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
    
    public boolean isCased() {
        return cased;
    }

    public boolean isSaved() {
        return saved;
    }

    public boolean isStemmed() {
        return stemmed;
    }

    public boolean isTokenized() {
        return tokenized;
    }

    public boolean isUncased() {
        return uncased;
    }

    public boolean isVectored() {
        return vectored;
    }

    public abstract int getMaximumDocumentID();
}
