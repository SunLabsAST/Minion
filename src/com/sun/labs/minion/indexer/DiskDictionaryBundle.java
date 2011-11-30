package com.sun.labs.minion.indexer;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.dictionary.DiskBiGramDictionary;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.partition.DocumentVectorLengths;
import com.sun.labs.minion.util.buffer.NIOFileReadableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.indexer.MemoryDictionaryBundle.Type;
import com.sun.labs.minion.indexer.dictionary.DateNameHandler;
import com.sun.labs.minion.indexer.dictionary.DoubleNameHandler;
import com.sun.labs.minion.indexer.dictionary.LongNameHandler;
import com.sun.labs.minion.indexer.dictionary.NameDecoder;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.util.CDateParser;
import java.io.RandomAccessFile;
import java.util.logging.Logger;


/**
 * A bundle of dictionaries to be used at query time.
 */
public class DiskDictionaryBundle<N extends Comparable> {

    public static final Logger logger = Logger.getLogger(DiskDictionaryBundle.class.
            getName());

    /**
     * The header for this field.
     */
    private FieldHeader header;

    /**
     * The dictionaries.
     */
    private DiskDictionary[] dicts;

    /**
     * The field we're associated with.
     */
    private DiskField field;

    /**
     * A dictionary for saved value bigrams.
     */
    private DiskBiGramDictionary savedBigrams;

    /**
     * A dictionary for token bigrams.
     */
    private DiskBiGramDictionary tokenBigrams;

    /**
     * A buffer containing the dtv offsets at query time.
     */
    private ReadableBuffer dtvOffsets;

    /**
     * A buffer containing the actual dtv data at query time.
     */
    private ReadableBuffer dtvData;

    /**
     * The length of the document vectors for this field.
     */
    private DocumentVectorLengths dvl;

    private EntryFactory vectorEntryFactory = new EntryFactory(
            Postings.Type.ID_FREQ);

    protected CDateParser dateParser;
    
    public DiskDictionaryBundle(DiskField field,
                                RandomAccessFile dictFile,
                                RandomAccessFile vectorLengthsFile,
                                RandomAccessFile[] postIn,
                                EntryFactory factory) throws java.io.IOException {
        this.field = field;
        header = new FieldHeader(dictFile);
        
        for(Type type : Type.values()) {
            int ord = type.ordinal();
            NameDecoder decoder;
            EntryFactory fact;
            switch(type) {
                case RAW_SAVED:
                case UNCASED_SAVED:
                    decoder = getDecoder();
                    fact = new EntryFactory(Postings.Type.ID);
                case RAW_VECTOR:
                case STEMMED_VECTOR:
                    decoder = new StringNameHandler();
                    fact = new EntryFactory(Postings.Type.ID_FREQ);
                default:
                    decoder = new StringNameHandler();
                    fact = factory;
                    
            }
            if(header.dictOffsets[ord] > 0) {
                dictFile.seek(header.dictOffsets[ord]);
                dicts[ord] = new DiskDictionary<String>(fact,
                                                        decoder,
                                                        dictFile, postIn);
            }
        }


        if(header.tokenBGOffset > 0) {
            dictFile.seek(header.tokenBGOffset);
            tokenBigrams = new DiskBiGramDictionary(dictFile, postIn[0],
                                                    DiskDictionary.PostingsInputType.FILE_PART_POST,
                                                    DiskDictionary.BufferType.FILEBUFFER,
                                                    256, 2048, 2048, 2048, 2048, null,
                                                    dicts[Type.UNCASED_TOKENS.ordinal()]);
        }

        if(header.savedBGOffset > 0) {
            dictFile.seek(header.savedBGOffset);
            tokenBigrams = new DiskBiGramDictionary(dictFile, postIn[0],
                                                    DiskDictionary.PostingsInputType.FILE_PART_POST,
                                                    DiskDictionary.BufferType.FILEBUFFER,
                                                    256, 2048, 2048, 2048, 2048, null,
                                                    dicts[Type.UNCASED_SAVED.ordinal()]);
        }

        //
        // Load the docs to vals data, using a file backed buffer.
        dtvData = new NIOFileReadableBuffer(postIn[0], header.dtvOffset, 8192);

        //
        // Load the docs to vals offset data, using a file backed buffer.
        dtvOffsets = new NIOFileReadableBuffer(postIn[0],
                                               header.dtvPosOffset, 8192);

        vectorLengthsFile.seek(header.vectorLengthOffset);
        dvl = new DocumentVectorLengths(vectorLengthsFile, 8192);

    }
    
    public int getMaximumDocumentID() {
        return header.maxDocID;
    }

    private NameDecoder getDecoder() {
        switch(field.getInfo().getType()) {
            case STRING:
                return new StringNameHandler();
            case DATE:
                return new DateNameHandler();
            case INTEGER:
                return new LongNameHandler();
            case FLOAT:
                return new DoubleNameHandler();
            default:
                return new StringNameHandler();
        }
    }

    public DiskDictionary getTermDictionary(boolean cased) {
        if(cased) {
            return dicts[Type.CASED_TOKENS.ordinal()];
        } else {
            return dicts[Type.UNCASED_TOKENS.ordinal()];
        }
    }
}
