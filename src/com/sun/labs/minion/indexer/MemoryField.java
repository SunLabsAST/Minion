package com.sun.labs.minion.indexer;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Stemmer;
import com.sun.labs.minion.indexer.dictionary.DateNameHandler;
import com.sun.labs.minion.indexer.dictionary.DoubleNameHandler;
import com.sun.labs.minion.indexer.dictionary.LongNameHandler;
import com.sun.labs.minion.indexer.dictionary.MemoryBiGramDictionary;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionary;
import com.sun.labs.minion.indexer.dictionary.NameEncoder;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * A class that will hold all of the data for a field during indexing.
 */
public class MemoryField extends Field {

    static Logger logger = Logger.getLogger(MemoryField.class.getName());

    /**
     * A dictionary for cased tokens.
     */
    private MemoryDictionary<String> casedTokens;

    /**
     * A dictionary for uncased tokens.
     */
    private MemoryDictionary<String> uncasedTokens;

    /**
     * A dictionary for stemmed tokens.
     */
    private MemoryDictionary<String> stemmedTokens;

    /**
     * A dictionary for raw saved values.
     */
    private MemoryDictionary<Comparable> rawSaved;

    /**
     * A dictionary for uncased saved values.
     */
    private MemoryDictionary<String> uncasedSaved;

    /**
     * The document dictionary for this field, which maps from document keys
     * to the terms that occur in this document.  This is used for vectored fields.
     */
    private MemoryDictionary<String> vectors;

    /**
     * The vector for the current document, if we're vectoring.
     */
    private IndexEntry vector;

    /**
     * An occurrence that we can use to add data to postings for the
     * document dictionary entries.
     */
    protected DocOccurrence ddo;

    /**
     * A stemmer for stemming.
     */
    private Stemmer stemmer;

    /**
     * An array of the sets of entries saved per document at indexing time.
     */
    private List[] dv;

    /**
     * The maximum document ID for which we're storing data.
     */
    private int maximumDocumentID;

    public MemoryField(FieldInfo info, EntryFactory factory) {
        super(info);
        if(info.hasAttribute(FieldInfo.Attribute.TOKENIZED)) {
            if(cased) {
                casedTokens = new MemoryDictionary(factory);
            }
            if(uncased) {
                uncasedTokens = new MemoryDictionary(factory);
            }
        }

        if(info.hasAttribute(FieldInfo.Attribute.STEMMED)) {
            stemmedTokens = new MemoryDictionary(factory);
            stemmer = info.getStemmer();
        }

        if(info.hasAttribute(FieldInfo.Attribute.SAVED)) {
            if(cased) {
                rawSaved = new MemoryDictionary(factory);
            }
            if(uncased) {
                uncasedSaved = new MemoryDictionary(factory);
            }

            dv = new List[1024];
            if(info.getType() == FieldInfo.Type.DATE) {
                dateParser = new CDateParser();
            }
        }

        if(info.hasAttribute(FieldInfo.Attribute.VECTORED)) {
            vectored = true;
            vectors = new MemoryDictionary(factory);
            ddo = new DocOccurrence();
        }
    }

    public void startDocument(String key) {
        if(vectored) {
            vectors.remove(key);
            vector = vectors.put(key);
        }
    }

    /**
     * Adds data to this field, taking whatever actions are appropriate based on
     * the attributes of the field.
     * @param docID the ID of the document for which the data is being added.
     * @param data the data to add
     */
    public void addData(int docID, Object data) {
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

        if(!saved) {
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

        if(info.getType() != FieldInfo.Type.STRING || cased) {
            savedEntry = rawSaved.put(name);
        }

        if(info.getType() == FieldInfo.Type.STRING && uncased) {
            IndexEntry uce = uncasedSaved.put(CharUtils.toLowerCase(
                    name.toString()));

            //
            // If there was no cased version saved, we'll keep the uncased version.
            if(savedEntry == null) {
                savedEntry = uce;
            }
        }

        //
        // We'll just store the entries per-document, since we might be adding
        // entries in non-document ID order.  We'll build the actual postings
        // lists at dumpt time.
        if(docID >= dv.length) {
            List[] temp = new List[(docID + 1) * 2];
            System.arraycopy(dv, 0, temp, 0, dv.length);
            dv = temp;
        }
        if(dv[docID] == null) {
            dv[docID] = new ArrayList<IndexEntry>();
        }
        dv[docID].add(savedEntry);
    }

    public void token(Token t) {

        if(!tokenized) {
            throw new UnsupportedOperationException(String.format(
                    "Field: %s is not tokenized", info.getName()));
        }

        IndexEntry ce = null;
        IndexEntry uce = null;

        if(cased) {
            ce = casedTokens.put(t.getToken());
            ce.add(t);
        }

        //
        // If we're storing uncased terms or stemming, then we need to downcase
        // the term, since stemming will likely break on a mixed case term.
        if(uncased || stemmed) {
            String uct = CharUtils.toLowerCase(t.getToken());

            if(uncased) {
                uce = uncasedTokens.put(uct);
                uce.add(t);
            }

            if(stemmed) {
                String stok = stemmer.stem(uct);
                IndexEntry e = stemmedTokens.put(stok);
                e.add(t);
            }
        }

        if(vectored) {
            if(uncased && uce != null) {
                ddo.setEntry(uce);
            } else if(cased && ce != null) {
                ddo.setEntry(ce);
            }
            vector.add(ddo);
        }
    }

    public int getMaximumDocumentID() {
        return maximumDocumentID;
    }

    /**
     * Writes the field to the provided files.
     *
     * @param path The path of the index directory.
     * @param fieldDictFile The file where the dictionaries will be written.
     * @param termStatsDictFile a file where a new term statistics dictionary
     * will be written
     * @param vectorLengthsFile a file where vector lengths for the documents
     * in this field will be written
     * @param postOut A place to write the postings associated with the
     * dictionaries.
     * @param maxID The maximum document ID for this partition.
     * @throws java.io.IOException if there is an error during the
     * writing.
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

        logger.info(String.format("Dump field %s", info.getName()));

        header.maxDocID = maxID;

        //
        // Dump the token and saved dictionaries.
        IndexEntry[] sortedTokens = null;
        int[] tokenIDMap = null;
        if(casedTokens != null) {
            header.dictOffsets[CASED_TOKENS] = fieldDictFile.getFilePointer();
            sortedTokens = casedTokens.dump(path, new StringNameHandler(),
                                            fieldDictFile, postOut,
                                            MemoryDictionary.Renumber.RENUMBER,
                                            MemoryDictionary.IDMap.OLDTONEW,
                                            null);
            tokenIDMap = casedTokens.getIdMap();
        } else {
            header.dictOffsets[CASED_TOKENS] = -1;
        }

        if(uncasedTokens != null) {
            header.dictOffsets[UNCASED_TOKENS] = fieldDictFile.getFilePointer();
            sortedTokens = uncasedTokens.dump(path, new StringNameHandler(),
                                              fieldDictFile, postOut,
                                              MemoryDictionary.Renumber.RENUMBER,
                                              MemoryDictionary.IDMap.OLDTONEW,
                                              null);
            tokenIDMap = uncasedTokens.getIdMap();
        } else {
            header.dictOffsets[UNCASED_TOKENS] = -1;
        }

        if(stemmedTokens != null) {
            header.dictOffsets[STEMMED_TOKENS] = fieldDictFile.getFilePointer();
            stemmedTokens.dump(path, new StringNameHandler(),
                               fieldDictFile, postOut,
                               MemoryDictionary.Renumber.RENUMBER,
                               MemoryDictionary.IDMap.OLDTONEW,
                               null);
            stemmedTokens.getIdMap();
        } else {
            header.dictOffsets[STEMMED_TOKENS] = -1;
        }

        IndexEntry[] sortedSaved = null;
        if(rawSaved != null) {
            header.dictOffsets[RAW_SAVED] = fieldDictFile.getFilePointer();
            sortedSaved = rawSaved.dump(path, getEncoder(info),
                                        fieldDictFile, postOut,
                                        MemoryDictionary.Renumber.RENUMBER,
                                        MemoryDictionary.IDMap.OLDTONEW,
                                        null);
        } else {
            header.dictOffsets[RAW_SAVED] = -1;
        }

        if(uncasedSaved != null) {
            header.dictOffsets[UNCASED_SAVED] = fieldDictFile.getFilePointer();
            sortedSaved = uncasedSaved.dump(path, getEncoder(info),
                                            fieldDictFile, postOut,
                                            MemoryDictionary.Renumber.RENUMBER,
                                            MemoryDictionary.IDMap.OLDTONEW,
                                            null);
        } else {
            header.dictOffsets[UNCASED_SAVED] = -1;
        }

        if(vectors != null) {

            //
            // Dump the document vectors.
            header.dictOffsets[VECTORS] = fieldDictFile.getFilePointer();
            vectors.dump(path, new StringNameHandler(), fieldDictFile, postOut,
                         MemoryDictionary.Renumber.NONE,
                         MemoryDictionary.IDMap.NONE,
                         tokenIDMap);
        } else {
            header.dictOffsets[VECTORS] = -1;
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

        //
        // Write out our document vector lengths.
        header.vectorLengthOffset = vectorLengthsFile.getFilePointer();
        DocumentVectorLengths.calculate(this, termStatsDictFile,
                                        vectorLengthsFile,
                                        partition.getPartitionManager().
                getTermStatsDict());
        
        //
        // Now zip back and write the header.
        long end = fieldDictFile.getFilePointer();
        fieldDictFile.seek(headerPos);
        header.write(fieldDictFile);
        fieldDictFile.seek(end);

    }

    public MemoryDictionary getTermDictionary(boolean cased) {
        if(cased && casedTokens != null) {
            return casedTokens;
        }

        if(!cased && uncasedTokens != null) {
            return uncasedTokens;
        }

        return null;
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
                    logger.warning("Non integer value: " + val
                            + " for integer saved field " + info.getName()
                            + ", ignoring " + val.getClass());
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
                    logger.warning("Non float value: " + val
                            + " for float saved field " + info.getName()
                            + ", ignoring");
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
                    logger.warning("Non-parseable date: " + val
                            + " for date saved field " + info.getName()
                            + ", ignoring");
                    return null;
                }
            case STRING:
                return val.toString();
            default:
                logger.warning("Field: " + info.getName() + " "
                        + "has unknown SAVED type: " + info.getType()
                        + ", using VARCHAR.");
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
