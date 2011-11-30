package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.FieldHeader;
import com.sun.labs.minion.indexer.MemoryField;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.partition.DocumentVectorLengths;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.indexer.postings.DocOccurrence;
import com.sun.labs.minion.indexer.postings.DocumentVectorPostings;
import com.sun.labs.minion.indexer.postings.Occurrence;
import com.sun.labs.minion.indexer.postings.OccurrenceImpl;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.pipeline.Token;
import com.sun.labs.minion.util.CDateParser;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
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
        /**
         * A dictionary for the token bigrams.
         */
        TOKEN_BIGRAMS,
        /**
         * A dictionary for saved value bigrams.
         */
        SAVED_VALUE_BIGRAMS

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
    private List[] dv;

    private List[] ucdv;

    private EntryFactory vectorEntryFactory = new EntryFactory(Postings.Type.DOC_VECTOR);

    private EntryFactory savedEntryFactory = new EntryFactory(Postings.Type.ID);

    protected CDateParser dateParser;

    public MemoryDictionaryBundle(MemoryField field, EntryFactory factory) {
        this.field = field;
        info = field.getInfo();
        dicts = new MemoryDictionary[Type.values().length];

        if(field.isCased()) {
            dicts[Type.CASED_TOKENS.ordinal()] = new MemoryDictionary<String>(factory);
        }

        if(field.isUncased()) {
            dicts[Type.UNCASED_TOKENS.ordinal()] = new MemoryDictionary<String>(factory);
        }

        if(field.isStemmed()) {
            dicts[Type.STEMMED_TOKENS.ordinal()] = new MemoryDictionary<String>(
                    factory);
        }

        if(field.isVectored()) {
            dicts[Type.RAW_VECTOR.ordinal()] = new MemoryDictionary<String>(
                    vectorEntryFactory);
            if(field.isStemmed()) {
                dicts[Type.STEMMED_VECTOR.ordinal()] = new MemoryDictionary<String>(
                        vectorEntryFactory);
            }
            ddo = new DocOccurrence();
        }

        if(field.isSaved()) {
            dv = new List[128];
            dicts[Type.RAW_SAVED.ordinal()] = new MemoryDictionary<N>(savedEntryFactory);
            if(field.isUncased()) {
                dicts[Type.UNCASED_SAVED.ordinal()] = new MemoryDictionary<N>(
                        savedEntryFactory);
                ucdv = new List[128];
            }
        }

        if(field.getInfo().getType() == FieldInfo.Type.DATE) {
            dateParser = new CDateParser();
        }
    }

    public void startDocument(Entry docKey) {
        String key = docKey.getName().toString();
        maxDocID = Math.max(maxDocID, docKey.getID());
        if(field.isVectored()) {
            if(field.isCased()) {
                dicts[Type.RAW_VECTOR.ordinal()].remove(key);
                rawVector = dicts[Type.RAW_VECTOR.ordinal()].put(key);
            }
            if(field.isStemmed()) {
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

        if(!field.isTokenized()) {
            throw new UnsupportedOperationException(String.format(
                    "Field: %s is not tokenized", info.getName()));
        }

        IndexEntry ce = null;
        IndexEntry uce = null;
        IndexEntry se = null;

        if(field.isCased()) {
            ce = dicts[Type.CASED_TOKENS.ordinal()].put(t.getToken());
            ce.add(t);
        }
        
        //
        // If we're storing uncased terms or stemming, then we need to downcase
        // the term, since stemming will likely break on a mixed case term.
        if(field.isUncased() || field.isStemmed()) {
            String uct = CharUtils.toLowerCase(t.getToken());

            if(field.isUncased()) {
                uce = dicts[Type.UNCASED_TOKENS.ordinal()].put(uct);
                uce.add(t);
            }

            if(field.isStemmed()) {
                String stok = field.getStemmer().stem(uct);
                se = dicts[Type.STEMMED_TOKENS.ordinal()].put(stok);
                se.add(t);
            }
        }

        if(field.isVectored()) {
            ddo.setEntry(uce);
            ddo.setCount(1);
            rawVector.add(ddo);

            if(field.isStemmed()) {
                ddo.setEntry(se);
                ddo.setCount(1);
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

        if(!field.isSaved()) {
            throw new UnsupportedOperationException(String.format(
                    "Field: %s is not saved", info.getName()));
        }

        Comparable name = getEntryName(data, field.getInfo(), dateParser);

        //
        // If we had a failure, then just return.
        if(name == null) {
            return;
        }

        //
        // We'll just store the saved entries per-document, since we might be adding
        // entries in non-document ID order (e.g., when doing classification.)
        // We'll build the actual postings lists at dump time.
        if(dicts[Type.RAW_SAVED.ordinal()] != null) {
            IndexEntry rawSavedEntry = dicts[Type.RAW_SAVED.ordinal()].put(name);
            if(docID >= dv.length) {
                dv = Arrays.copyOf(dv, (docID + 1) * 2);
            }

            if(dv[docID] == null) {
                dv[docID] = new ArrayList<IndexEntry>();
            }
            dv[docID].add(rawSavedEntry);
        }

        //
        // Handle the uncased values for string fields.
        if(dicts[Type.UNCASED_SAVED.ordinal()] != null) {
            IndexEntry uncasedSavedEntry = dicts[Type.UNCASED_SAVED.ordinal()].put(CharUtils.toLowerCase(
                    name.toString()));
            if(docID >= ucdv.length) {
                ucdv = Arrays.copyOf(ucdv, (docID + 1) * 2);
            }

            if(ucdv[docID] == null) {
                ucdv[docID] = new ArrayList<IndexEntry>();
            }
            ucdv[docID].add(uncasedSavedEntry);
        }

    }

    /**
     * Marshalls this dictionary bundle's dictionaries for eventual flushing
     * to disk.
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
    public MemoryField.MarshallResult marshall(PartitionOutput partOut) throws
            java.io.IOException {
        boolean debug = field.getInfo().getName().equals("timestamp");

        MemoryField.MarshallResult ret = MemoryField.MarshallResult.DICTS_DUMPED;
        DictionaryOutput partDictOut = partOut.getPartitionDictionaryOutput();

        long headerPos = partDictOut.position();
        FieldHeader header = new FieldHeader();
        header.write(partDictOut);

        header.fieldID = info.getID();
        header.maxDocID = partOut.getMaxDocID();


        //
        // The ID maps from each of the dictionaries.
        int[][] entryIDMaps = new int[Type.values().length][];

        //
        // Dump the dictionaries in this bundle.  This loop is a little gross, what
        // with the pre-dump and post-dump switches based on the type of dictionary
        // being dumped, but it makes things a lot more compact as most of the dups
        // are pretty much the same.
        for(Type type : Type.values()) {
            int ord = type.ordinal();
            if(dicts[ord] == null && type != Type.TOKEN_BIGRAMS && type != Type.SAVED_VALUE_BIGRAMS) {
                header.dictOffsets[ord] = -1;
                continue;
            }

            //
            // Figure out the encoder for the type of dictionary.
            partOut.setDictionaryRenumber(MemoryDictionary.Renumber.RENUMBER);
            partOut.setDictionaryIDMap(MemoryDictionary.IDMap.NONE);
            partOut.setPostingsIDMap(null);
            switch(type) {

                case CASED_TOKENS:
                    partOut.setDictionaryEncoder(new StringNameHandler());
                    partOut.setDictionaryIDMap(MemoryDictionary.IDMap.OLD_TO_NEW);
                    break;

                case UNCASED_TOKENS:
                    partOut.setDictionaryEncoder(new StringNameHandler());
                    partOut.setDictionaryIDMap(MemoryDictionary.IDMap.OLD_TO_NEW);
                    break;

                case RAW_SAVED:
                    partOut.setDictionaryEncoder(getEncoder(info));

                    //
                    // We didn't add any occurrence data to the dictionary
                    // while we were adding values, since
                    // we might have added data in non-document ID order.  
                    // We can do that now since we can process things 
                    // in document ID order.
                    Occurrence co = new OccurrenceImpl();
                    for(int i = 0; i < dv.length; i++) {
                        if(dv[i] != null && !dv[i].isEmpty()) {
                            co.setID(i);
                            for(IndexEntry e : (List<IndexEntry>) dv[i]) {
                                e.add(co);
                            }
                        }
                    }

                    break;

                case UNCASED_SAVED:
                    partOut.setDictionaryEncoder(getEncoder(info));

                    //
                    // We didn't add any occurrence data to the dictionary
                    // while we were adding values, since
                    // we might have added data in non-document ID order.  
                    // We can do that now since we can process things 
                    // in document ID order.
                    Occurrence uo = new OccurrenceImpl();
                    for(int i = 0; i < dv.length; i++) {
                        if(ucdv[i] != null && !dv[i].isEmpty()) {
                            uo.setID(i);
                            for(IndexEntry e : (List<IndexEntry>) ucdv[i]) {
                                e.add(uo);
                            }
                        }
                    }
                    break;
                case RAW_VECTOR:
                    partOut.setDictionaryEncoder(new StringNameHandler());
                    partOut.setDictionaryRenumber(MemoryDictionary.Renumber.NONE);
                    partOut.setDictionaryIDMap(MemoryDictionary.IDMap.NONE);
                    partOut.setPostingsIDMap(entryIDMaps[Type.CASED_TOKENS.ordinal()]);
                    break;

                case STEMMED_VECTOR:
                    partOut.setDictionaryEncoder(new StringNameHandler());
                    partOut.setDictionaryRenumber(MemoryDictionary.Renumber.NONE);
                    partOut.setDictionaryIDMap(MemoryDictionary.IDMap.NONE);
                    partOut.setPostingsIDMap(entryIDMaps[Type.STEMMED_TOKENS.ordinal()]);
                    break;

                case TOKEN_BIGRAMS:
                    MemoryDictionary tdict = null;
                    if(dicts[Type.CASED_TOKENS.ordinal()] != null) {
                        tdict = dicts[Type.CASED_TOKENS.ordinal()];
                    } else {
                        tdict = dicts[Type.UNCASED_TOKENS.ordinal()];
                    }

                    //
                    // We'll create this one on the fly.
                    if(tdict != null) {
                        if(dicts[ord] == null) {
                            dicts[ord] = new MemoryBiGramDictionary(new EntryFactory(Postings.Type.ID_FREQ));
                        }
                        MemoryBiGramDictionary tbg = (MemoryBiGramDictionary) dicts[ord];
                        for(Object o : tdict) {
                            IndexEntry e = (IndexEntry) o;
                            tbg.add(CharUtils.toLowerCase(e.getName().toString()), e.getID());
                        }
                        partOut.setDictionaryEncoder(new StringNameHandler());
                        partOut.setDictionaryRenumber(MemoryDictionary.Renumber.RENUMBER);
                        partOut.setDictionaryIDMap(MemoryDictionary.IDMap.NONE);
                        partOut.setPostingsIDMap(null);
                    } else {
                        header.dictOffsets[ord] = -1;
                        continue;
                    }
                    break;

                case SAVED_VALUE_BIGRAMS:
                    if(field.getInfo().getType() == FieldInfo.Type.STRING) {
                        if(dicts[ord] == null) {
                            dicts[ord] = new MemoryBiGramDictionary(new EntryFactory(Postings.Type.ID_FREQ));
                        }
                        MemoryBiGramDictionary sbg = (MemoryBiGramDictionary) dicts[ord];
                        for(Object o : dicts[Type.RAW_SAVED.ordinal()]) {
                            IndexEntry e = (IndexEntry) o;
                            sbg.add(CharUtils.toLowerCase(e.getName().toString()),
                                    e.getID());
                        }
                        partOut.setDictionaryEncoder(new StringNameHandler());
                        partOut.setDictionaryRenumber(MemoryDictionary.Renumber.RENUMBER);
                        partOut.setDictionaryIDMap(MemoryDictionary.IDMap.NONE);
                        partOut.setPostingsIDMap(null);
                    } else {
                        header.dictOffsets[ord] = -1;
                        continue;
                    }
                    break;
                default:
                    partOut.setDictionaryEncoder(new StringNameHandler());
            }

            header.dictOffsets[ord] = partDictOut.position();
            try {
            dicts[ord].marshall(partOut);
            } catch (RuntimeException ex) {
                //
                // It's nice to log what field and dictionary caused the problem
                logger.log(Level.SEVERE, String.format("Exception %s marshalling %s from %s", 
                        ex.getMessage(), type, field.getInfo().getName()));
                
                //
                // But we want the marshalling thread to ultimately catch the exception.
                throw(ex);
            }
        }

        if(field.isSaved()) {

            //
            // Dump the map from document IDs to the values saved for that document
            // ID, and collect the positions in the buffer where the data for each
            // document is recorded.
            WriteableBuffer dtv = new ArrayBuffer(partOut.getMaxDocID() * 4);
            WriteableBuffer dtvOffsets = new ArrayBuffer(partOut.getMaxDocID() * 4);
            for(int i = 1; i <= partOut.getMaxDocID(); i++) {

                dtvOffsets.byteEncode(dtv.position(), 4);

                //
                // If there's no set here, or we're past the end of the array,
                // encode 0 for the count of items for this document ID.
                if(i >= dv.length || dv[i] == null) {
                    dtv.byteEncode(0);
                    continue;
                }
                List<IndexEntry> dvs = (List<IndexEntry>) dv[i];

                if(debug) {
//                    logger.info(String.format("doc: %d n: %d", i, dvs.size()));
                }
                dtv.byteEncode(dvs.size());
                for(IndexEntry e : dvs) {
                    if(debug) {
//                        logger.info(String.format(" %s id: %d", ((Date) e.getName()).getTime(), e.getID()));
                    }
                    dtv.byteEncode(e.getID());
                }
            }

            //
            // Write the maps, recording the offset and size data in our
            // header.
            header.dtvOffset = partDictOut.position();
            dtv.write(partDictOut);
            header.dtvPosOffset = partDictOut.position();
            dtvOffsets.write(partDictOut);
        } else {
            header.dtvOffset = -1;
            header.dtvPosOffset = -1;
        }

        if(getTermDictionary(false) != null && !partOut.isLongIndexingRun()) {
            
            //
            // Write out our document vector lengths.
            WriteableBuffer vlb = partOut.getVectorLengthsBuffer();
            header.vectorLengthOffset = vlb.position();
            DocumentVectorLengths.calculate(field, partOut,
                    field.getPartition().getPartitionManager().
                    getTermStatsDict());
            ret = MemoryField.MarshallResult.EVERYTHING_DUMPED;
        } else {
            header.vectorLengthOffset = -1;
        }

        //
        // Now zip back and write the header.
        long endPos = partDictOut.position();
        partDictOut.position(headerPos);
        header.write(partDictOut);
        partDictOut.position(endPos);
        return ret;
    }

    public void clear() {
        for(MemoryDictionary md : dicts) {
            if(md != null) {
                md.clear();
            }
        }
        if(dv != null) {
            for(List l : dv) {
                if(l != null) {
                    l.clear();
                }
            }
        }
        if(ucdv != null) {
            for(List l : ucdv) {
                if(l != null) {
                    l.clear();
                }
            }
        }
        maxDocID = 0;
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
    public static Comparable getEntryName(Object val, FieldInfo info, CDateParser dateParser) {
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
