package com.sun.labs.minion.indexer;


import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.dictionary.DateNameHandler;
import com.sun.labs.minion.indexer.dictionary.DoubleNameHandler;
import com.sun.labs.minion.indexer.dictionary.LongNameHandler;
import com.sun.labs.minion.indexer.dictionary.MemoryBiGramDictionary;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.NameEncoder;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.partition.DocumentVectorLengths;
import com.sun.labs.minion.indexer.postings.DocOccurrence;
import com.sun.labs.minion.indexer.postings.Occurrence;
import com.sun.labs.minion.indexer.postings.OccurrenceImpl;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.pipeline.Token;
import com.sun.labs.minion.util.CDateParser;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.File;
import java.io.RandomAccessFile;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * A bundle of dictionaries that encapsulates the variety of dictionaries that
 * can be stored in association with a given {@link MemoryField}.
 */
public class MemoryDictionaryBundle<N extends Comparable> {

    private static final Logger logger = Logger.getLogger(MemoryDictionaryBundle.class.getName());

    /**
     * The types of dictionaries that we can bundle.
     */
    public enum Type {

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
         * Lowercased saved values from the document.  Only used if this is a
         * string field.
         */
        UNCASED_SAVED,

        /**
         * A document vector with the raw tokens from the document.
         */
        RAW_VECTOR,

        /**
         * A document vector with the lowercased, stemmed tokens from the document.
         */
        STEMMED_VECTOR,
    }

    /**
     * The dictionaries making up this bundle, indexed by the ordinal of one of the
     * {@link Types}.
     */
    private MemoryDictionary[] dicts;

    /**
     * The field that this bundle is associated with.
     */
    private MemoryField field;

    private FieldInfo info;

    private int maxDocID;

    /**
     * An occurrence that we can use to add data to postings for the
     * document dictionary entries.
     */
    private DocOccurrence ddo;

    /**
     * The raw vector for the current document, if we're vectoring.
     */
    private IndexEntry rawVector;

    /**
     * The (possibly) stemmed and down-cased vector for the current document.
     */
    private IndexEntry stemmedVector;

    /**
     * An array of the sets of entries saved per document at indexing time.
     */
    private List[] dv = new List[128];

    private EntryFactory vectorEntryFactory = new EntryFactory(Postings.Type.ID_FREQ);

    private EntryFactory savedEntryFactory = new EntryFactory(Postings.Type.ID);

    protected CDateParser dateParser;

    public MemoryDictionaryBundle(MemoryField field, EntryFactory factory) {
        this.field = field;
        info = field.getInfo();
        dicts = new MemoryDictionary[Type.values().length];

        if(field.cased) {
            dicts[Type.CASED_TOKENS.ordinal()] = new MemoryDictionary<String>(factory);
        }

        if(field.uncased) {
            dicts[Type.UNCASED_TOKENS.ordinal()] = new MemoryDictionary<String>(factory);
        }

        if(field.stemmed) {
            dicts[Type.STEMMED_TOKENS.ordinal()] = new MemoryDictionary<String>(
                    factory);
        }

        if(field.vectored) {
            dicts[Type.RAW_VECTOR.ordinal()] = new MemoryDictionary<String>(
                    vectorEntryFactory);
            if(field.stemmed) {
                dicts[Type.STEMMED_VECTOR.ordinal()] = new MemoryDictionary<String>(
                        vectorEntryFactory);
            }
        }

        if(field.saved) {
            dicts[Type.RAW_SAVED.ordinal()] = new MemoryDictionary<N>(savedEntryFactory);
            if(field.uncased) {
                dicts[Type.UNCASED_SAVED.ordinal()] = new MemoryDictionary<N>(
                        savedEntryFactory);
            }
        }

        if(field.getInfo().getType() == FieldInfo.Type.DATE) {
            dateParser = new CDateParser();
        }
    }

    public void startDocument(Entry docKey) {
        String key = docKey.getName().toString();
        maxDocID = Math.max(maxDocID, docKey.getID());
        if(field.vectored) {
            if(field.cased) {
                dicts[Type.RAW_VECTOR.ordinal()].remove(key);
                rawVector = dicts[Type.RAW_VECTOR.ordinal()].put(key);
            }
            if(field.uncased) {
                dicts[Type.STEMMED_VECTOR.ordinal()].remove(key);
                stemmedVector = dicts[Type.STEMMED_VECTOR.ordinal()].put(key);
            }
        } else {
            rawVector = null;
            stemmedVector = null;
        }
    }

    public int getMaxDocID() {
        return maxDocID;
    }

    public void token(Token t) {

        if(!field.tokenized) {
            throw new UnsupportedOperationException(String.format(
                    "Field: %s is not tokenized", info.getName()));
        }

        IndexEntry ce = null;
        IndexEntry uce = null;
        IndexEntry se = null;

        if(field.cased) {
            ce = dicts[Type.CASED_TOKENS.ordinal()].put(t.getToken());
            ce.add(t);
        }

        //
        // If we're storing uncased terms or stemming, then we need to downcase
        // the term, since stemming will likely break on a mixed case term.
        if(field.uncased || field.stemmed) {
            String uct = CharUtils.toLowerCase(t.getToken());

            if(field.uncased) {
                uce = dicts[Type.UNCASED_TOKENS.ordinal()].put(uct);
                uce.add(t);
            }

            if(field.stemmed) {
                String stok = field.stemmer.stem(uct);
                se = dicts[Type.STEMMED_TOKENS.ordinal()].put(stok);
                se.add(t);
            }
        }

        if(field.vectored) {
            if(field.uncased) {
                ddo.setEntry(uce);
                rawVector.add(ddo);
            }

            if(field.cased) {
                if(se != null) {
                    ddo.setEntry(se);
                } else {
                    ddo.setEntry(ce);
                }
                stemmedVector.add(ddo);
            }
        }
    }

    /**
     * Saves data in this field.  If this field is not saved, then an
     * exception is thrown.
     *
     * @param docID the document ID for the document containing the saved
     * data
     * @param data The actual field data.
     */
    public void save(int docID, Object data) {

        if(!field.saved) {
            throw new UnsupportedOperationException(String.format(
                    "Field: %s is not saved", info.getName()));
        }

        Comparable name = getEntryName(data);

        //
        // If we had a failure, then just return.
        if(name == null) {
            return;
        }

        IndexEntry savedEntry = null;

        if(info.getType() == FieldInfo.Type.STRING) {
            if((!field.uncased && !field.cased) || field.cased) {
                savedEntry = dicts[Type.RAW_SAVED.ordinal()].put(name);
            }

            if(field.uncased) {
                IndexEntry uce = dicts[Type.UNCASED_SAVED.ordinal()].put(CharUtils.
                        toLowerCase(
                        name.toString()));

                //
                // If there was no cased version saved, we'll keep the uncased version.
                if(savedEntry == null) {
                    savedEntry = uce;
                }
            }

        } else {
            savedEntry = dicts[Type.RAW_SAVED.ordinal()].put(name);
        }

        //
        // We'll just store the entries per-document, since we might be adding
        // entries in non-document ID order.  We'll build the actual postings
        // lists at dump time.
        if(docID >= dv.length) {
            dv = Arrays.copyOf(dv, (docID+1) * 2);
        }
        if(dv[docID] == null) {
            dv[docID] = new ArrayList<IndexEntry>();
        }
        dv[docID].add(savedEntry);
    }

    /**
     * Dumps this dictionary bundle's dictionaries.
     *
     * @param path the path of the index
     * @param fieldDictFile the file where the data will be dumped
     * @param postOut where the postings will be written
     * @param termStatsDictFile a file containing the term statistics for this
     * field
     * @param vectorLengthsFile A place to store the document vector lengths
     * @param maxID the maximum ID for this
     * @throws java.io.IOException
     */
    public void dump(File path,
                     RandomAccessFile fieldDictFile,
                     PostingsOutput[] postOut,
                     RandomAccessFile termStatsDictFile,
                     RandomAccessFile vectorLengthsFile,
                     int maxID) throws
            java.io.IOException {

        long headerPos = fieldDictFile.getFilePointer();
        FieldHeader header = new FieldHeader();
        header.write(fieldDictFile);

        header.fieldID = info.getID();
        header.maxDocID = maxID;

        //
        // A place to save a few things that we'll need for dumping bigrams and
        // the document vectors.
        IndexEntry[] sortedTokens = null;
        int[] tokenIDMap = null;
        
        IndexEntry[] sortedSaved = null;
        

        //
        // Dump the dictionaries in this bundle.  This loop is a little gross, what
        // with the pre-dump and post-dump switches based on the type of dictionary
        // being dumped, but it makes things a lot more compact as most of the dups
        // are pretty much the same.
        for(Type type : Type.values()) {
            int ord = type.ordinal();
            if(dicts[ord] == null) {
                header.dictOffsets[ord] = -1;
                continue;
            }

            //
            // Figure out the encoder for the type of dictionary.
            NameEncoder encoder;
            MemoryDictionary.Renumber renumber = MemoryDictionary.Renumber.RENUMBER;
            MemoryDictionary.IDMap idmap = MemoryDictionary.IDMap.OLDTONEW;
            int[] currentTokenIDMap = null;
            switch(type) {
                case RAW_SAVED:
                case UNCASED_SAVED:
                    encoder = getEncoder(info);
                    break;
                case RAW_VECTOR:
                case STEMMED_VECTOR:
                    renumber = MemoryDictionary.Renumber.NONE;
                    idmap = MemoryDictionary.IDMap.NONE;
                    currentTokenIDMap = tokenIDMap;
                default:
                    encoder = new StringNameHandler();
            }

            header.dictOffsets[ord] = fieldDictFile.getFilePointer();
            IndexEntry[] sorted = dicts[ord].dump(path, encoder,
                                            fieldDictFile, postOut,
                                            renumber, idmap, currentTokenIDMap);

            //
            // Remember the sorted order and the id map for the dictionary of
            // tokens, which we'll need in a couple of iterations when we're
            // dumping the document vectors.  We'll remembe the sorted saved
            // values for string fields so that we can dump bigrams later on.
            switch(type) {
                case CASED_TOKENS:
                case UNCASED_TOKENS:
                    sortedTokens = sorted;
                    tokenIDMap = dicts[ord].getIdMap();
                case RAW_SAVED:
                case UNCASED_SAVED:
                    if(info.getType() == FieldInfo.Type.STRING) {
                        sortedSaved = sorted;
                    }
            }
        }

        //
        // If we have tokens or saved values, then output any bigrams that we
        // need for accelerating wildcards.
        if(sortedTokens != null) {
            MemoryBiGramDictionary tbg = new MemoryBiGramDictionary(
                    new EntryFactory(Postings.Type.ID_FREQ));
            for(IndexEntry e : sortedTokens) {
                tbg.add(e.getName().toString(), e.getID());
            }
            header.tokenBGOffset = fieldDictFile.getFilePointer();
            tbg.dump(path, new StringNameHandler(), fieldDictFile, postOut,
                     MemoryDictionary.Renumber.RENUMBER,
                     MemoryDictionary.IDMap.NONE,
                     null);
        }

        if(sortedSaved != null) {
            MemoryBiGramDictionary sbg = new MemoryBiGramDictionary(
                    new EntryFactory(Postings.Type.ID_FREQ));
            for(IndexEntry e : sortedSaved) {
                sbg.add(e.getName().toString(), e.getID());
            }
            header.savedBGOffset = fieldDictFile.getFilePointer();
            sbg.dump(path, new StringNameHandler(), fieldDictFile, postOut,
                     MemoryDictionary.Renumber.NONE, MemoryDictionary.IDMap.NONE,
                     null);
        } else {
            header.savedBGOffset = -1;
        }

        //
        // We didn't add any occurrence data while we were adding values, since
        // we might have added data in non-document ID order.  We can do that now
        // since we can process things in document ID order.
        Occurrence o = new OccurrenceImpl();
        for(int i = 0; i < dv.length; i++) {
            if(dv[i] != null) {
                o.setID(i);
                for(IndexEntry e : (List<IndexEntry>) dv[i]) {
                    e.add(o);
                }
            }
        }

        if(field.saved) {

            //
            // Dump the map from document IDs to the values saved for that document
            // ID, and collect the positions in the buffer where the data for each
            // document is recorded.
            WriteableBuffer dtv = new ArrayBuffer(maxID * 4);
            WriteableBuffer dtvPos = new ArrayBuffer(maxID * 4);
            for(int i = 1; i <= maxID; i++) {
                dtvPos.byteEncode(dtv.position(), 4);

                //
                // If there's no set here, or we're past the end of the array,
                // encode 0 for the count of items for this document ID.
                if(i >= dv.length || dv[i] == null) {
                    dtv.byteEncode(0);
                    continue;
                }
                List<IndexEntry> dvs = (List<IndexEntry>) dv[i];
                dtv.byteEncode(dvs.size());
                for(IndexEntry e : dvs) {
                    dtv.byteEncode(e.getID());
                }
            }

            //
            // Write the maps, recording the offset and size data in our
            // header.
            header.dtvOffset = fieldDictFile.getFilePointer();
            dtv.write(fieldDictFile);
            header.dtvPosOffset = fieldDictFile.getFilePointer();
            dtvPos.write(fieldDictFile);
        } else {
            header.dtvOffset = -1;
            header.dtvPosOffset = -1;
        }

        if(getTermDictionary(false) != null) {

            //
            // Write out our document vector lengths.
            header.vectorLengthOffset = vectorLengthsFile.getFilePointer();
            DocumentVectorLengths.calculate(field, termStatsDictFile,
                                            vectorLengthsFile,
                                            field.partition.getPartitionManager().
                    getTermStatsDict());
        } else {
            header.vectorLengthOffset = -1;
        }

        //
        // Now zip back and write the header.
        long end = fieldDictFile.getFilePointer();
        fieldDictFile.seek(headerPos);
        header.write(fieldDictFile);
        fieldDictFile.seek(end);

    }

    public MemoryDictionary getTermDictionary(boolean cased) {
        if(cased) {
            return dicts[Type.CASED_TOKENS.ordinal()];
        } else {
            return dicts[Type.UNCASED_TOKENS.ordinal()];
        }
    }


    /**
     * Gets a name for a given saved value, parsing from strings as necessary.
     *
     * @param val The value that we were passed.
     * @return A name appropriate for the given value and field type.
     */
    private Comparable getEntryName(Object val) {
        if(val == null) {
            return null;
        }

        switch(info.getType()) {
            case INTEGER:
                try {
                    if(val instanceof Integer) {
                        return new Long(((Integer) val).intValue());
                    } else if(val instanceof Long) {
                        return (Comparable) val;
                    } else {
                        return new Long(val.toString());
                    }
                } catch(NumberFormatException nfe) {
                    logger.warning(String.format("Non integer value: %s " 
                            + " for integer saved field %s "
                            + ", ignoring %s", val,
                            info.getName(),
                            val.getClass()));
                    return null;
                }
            case FLOAT:
                try {
                    if(val instanceof Double) {
                        return (Comparable) val;
                    } else if(val instanceof Float) {
                        return new Double(((Float) val).floatValue());
                    } else {
                        return new Double(val.toString());
                    }
                } catch(NumberFormatException nfe) {
                    logger.warning(String.format("Non float value: %s "
                            + " for float saved field %s "
                            + ", ignoring %s", val,
                                                 info.getName(),
                                                 val.getClass()));
                    return null;
                }
            case DATE:
                try {
                    if(val instanceof Date) {
                        return (Comparable) val;
                    } else if(val instanceof Long) {
                        return new Date(((Long) val).longValue());
                    } else if(val instanceof Integer) {
                        return new Date(((long) ((Integer) val).intValue())
                                * 1000);
                    } else {
                        return dateParser.parse(val.toString());
                    }
                } catch(java.text.ParseException pe) {
                    logger.warning(String.format("Non-parseable date: %s "
                            + " for date saved field %s "
                            + ", ignoring", val, info.getName()));
                    return null;
                }
            case STRING:
                return val.toString();
            default:
                logger.warning(String.format("Field: %s has unknown type %s, using STRING",
                        info.getName(), info.getType()));
                return val.toString();
        }
    }

    protected static NameEncoder getEncoder(FieldInfo info) {
        switch(info.getType()) {
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

}