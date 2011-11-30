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
import com.sun.labs.minion.indexer.dictionary.ArrayDictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DateNameHandler;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DoubleNameHandler;
import com.sun.labs.minion.indexer.dictionary.IntEntry;
import com.sun.labs.minion.indexer.dictionary.LongNameHandler;
import com.sun.labs.minion.indexer.dictionary.NameDecoder;
import com.sun.labs.minion.indexer.dictionary.NameEncoder;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.MergeState;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.ArrayGroup.DocIterator;
import com.sun.labs.minion.retrieval.ScoredGroup;
import com.sun.labs.minion.retrieval.ScoredQuickOr;
import com.sun.labs.minion.util.CDateParser;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.FileWriteableBuffer;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
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
    protected FieldHeader header;

    /**
     * The dictionaries.
     */
    private DiskDictionary[] dicts;
    
    /**
     * An entry factory for the token dictionaries.
     */
    private EntryFactory entryFactory;

    /**
     * The field we're associated with.
     */
    private DiskField field;

    /**
     * The field info, which we'll use a lot.
     */
    private FieldInfo info;

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

    protected CDateParser dateParser;

    public DiskDictionaryBundle(DiskField field,
                                RandomAccessFile dictFile,
                                RandomAccessFile vectorLengthsFile,
                                RandomAccessFile[] postIn,
                                EntryFactory entryFactory) throws java.io.IOException {
        this.field = field;
        this.entryFactory = entryFactory;
        info = field.getInfo();
        
        header = new FieldHeader(dictFile);
        dicts = new DiskDictionary[MemoryDictionaryBundle.Type.values().length];
        
        for(Type type : Type.values()) {
            int ord = type.ordinal();
            NameDecoder decoder;
            EntryFactory fact;
            switch(type) {
                case RAW_SAVED:
                case UNCASED_SAVED:
                    decoder = getDecoder();
                    fact = new EntryFactory(Postings.Type.ID);
                    break;
                case RAW_VECTOR:
                case STEMMED_VECTOR:
                    decoder = new StringNameHandler();
                    fact = new EntryFactory(Postings.Type.ID_FREQ);
                    break;
                default:
                    decoder = new StringNameHandler();
                    fact = entryFactory;
            }

            if(header.dictOffsets[ord] >= 0) {
                dictFile.seek(header.dictOffsets[ord]);
                dicts[ord] = new DiskDictionary<String>(fact,
                                                        decoder,
                                                        dictFile, postIn);
                dicts[ord].setPartition(field.partition);
            }
        }


        if(header.tokenBGOffset > 0) {
            dictFile.seek(header.tokenBGOffset);
            tokenBigrams = new DiskBiGramDictionary(dictFile, postIn[0],
                                                    DiskDictionary.PostingsInputType.FILE_PART_POST,
                                                    DiskDictionary.BufferType.FILEBUFFER,
                                                    256, 2048, 2048, 2048, 2048,
                                                    null,
                                                    dicts[Type.UNCASED_TOKENS.
                    ordinal()]);
            tokenBigrams.setPartition(field.partition);
        }

        if(header.savedBGOffset > 0) {
            dictFile.seek(header.savedBGOffset);
            savedBigrams = new DiskBiGramDictionary(dictFile, postIn[0],
                                                    DiskDictionary.PostingsInputType.FILE_PART_POST,
                                                    DiskDictionary.BufferType.FILEBUFFER,
                                                    256, 2048, 2048, 2048, 2048,
                                                    null,
                                                    dicts[Type.UNCASED_SAVED.
                    ordinal()]);
            savedBigrams.setPartition(field.partition);
        }

        if(header.dtvOffset >= 0) {

            //
            // Load the docs to vals data, using a file backed buffer.
            dtvData = new NIOFileReadableBuffer(dictFile, header.dtvOffset,
                                                8192);

            //
            // Load the docs to vals offset data, using a file backed buffer.
            dtvOffsets = new NIOFileReadableBuffer(dictFile,
                                                   header.dtvPosOffset, 8192);
        }

        if(header.vectorLengthOffset >= 0) {
            vectorLengthsFile.seek(header.vectorLengthOffset);
            dvl = new DocumentVectorLengths(vectorLengthsFile, 8192);
        }

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

    /**
     * Gets a term from the "main" dictionary for this partition.
     * @param name the name of the term to lookup
     * @param caseSensitive whether we should lookup the term using the provided case.
     * If <code>true</code>, then an entry will only be returned if there is a
     * match for this term with the provided case.  If <code>false</code> then an
     * entry will be returned if there is a match for this term in lower case.
     * @return
     */
    public QueryEntry getTerm(String name, boolean caseSensitive) {
        QueryEntry ret = null;
        if(caseSensitive) {
            if(field.cased) {
                ret = dicts[Type.CASED_TOKENS.ordinal()].get(name);
            } else {
                logger.warning(
                        String.format(
                        "Match case requested for term %s in field %s, "
                        + "but this field only has case insensitive terms stored",
                        name, info.getName()));
            }
        } else if(dicts[Type.UNCASED_TOKENS.ordinal()] != null) {
            ret = dicts[Type.UNCASED_TOKENS.ordinal()].get(CharUtils.
                    toLowerCase(name));
        }
        
        if(ret != null) {
            ret.setField(info);
        }
        return ret;
    }

    /**
     * Gets a term from the "main" dictionary for this partition.
     * @param name the name of the term to lookup
     * @param caseSensitive whether we should lookup the term using the provided case.
     * If <code>true</code>, then an entry will only be returned if there is a
     * match for this term with the provided case.  If <code>false</code> then an
     * entry will be returned if there is a match for this term in lower case.
     * @return
     */
    public QueryEntry getTerm(int id, boolean caseSensitive) {
        QueryEntry ret = null;
        if(caseSensitive) {
            if(field.cased) {
                ret = dicts[Type.CASED_TOKENS.ordinal()].getByID(id);
            } else {
                logger.warning(
                        String.format(
                        "Match case requested for term %s in field %s, "
                        + "but this field only has case insensitive terms stored"));
            }
        }

        if(dicts[Type.UNCASED_TOKENS.ordinal()] != null) {
            ret = dicts[Type.UNCASED_TOKENS.ordinal()].getByID(id);
        }
        if(ret != null) {
            ret.setField(info);
        }
        return ret;
    }

    public QueryEntry getStem(String stem) {
        QueryEntry ret = null;
        if(dicts[Type.STEMMED_TOKENS.ordinal()] != null) {
            ret = dicts[Type.STEMMED_TOKENS.ordinal()].get(stem);
        }
        
        if(ret != null) {
            ret.setField(info);
        }
        return ret;
    }

    public QueryEntry getVector(String key, boolean caseSensitive) {
        QueryEntry ret = null;
        if(!field.vectored) {
            logger.warning(
                    String.format("Requested vector for non-vectored field %s", info.
                    getName()));
            return null;
        }
        if(caseSensitive) {
            ret = dicts[Type.RAW_VECTOR.ordinal()].get(key);
        } else {
            ret = dicts[Type.STEMMED_VECTOR.ordinal()].get(key);
        }
        
        if(ret != null) {
            ret.setField(info);
        }
        return ret;

    }

    /**
     * Gets the entry in the dictionary associated with a given value.
     * @param val the value to get
     * @param caseSensitive whether to do a case sensitive lookup
     * @return
     */
    public QueryEntry getSaved(Comparable val, boolean caseSensitive) {
        if(!field.saved) {
            logger.warning(String.format(
                    "Can't fetch saved value for non-SAVED field %s", info.
                    getName()));
            return null;
        }
        if(caseSensitive) {
            if(field.cased) {
                return dicts[Type.RAW_SAVED.ordinal()].get(val);
            } else {
                logger.warning(
                        String.format(
                        "Case sensitive request for field value %s in field %s, "
                        + "but this field only has case insensitive values."));
                return dicts[Type.RAW_SAVED.ordinal()].get(val);
            }
        }

        return null;
    }

    /**
     * Gets an iterator for the saved values in a field.
     * @param caseSensitive for a string saved field, if this is <code>true</code>,
     * then case sensitive values will be returned.  Otherwise, case insensitive
     * values will be returned
     * @param lowerBound the lower bound for the values to return
     * @param includeLower whether to include the lower bound in the results of
     * the iterator
     * @param upperBound the upper bound of the values to return. If this is <code>null</code>
     * then no upper bound will be imposed.
     * @param includeUpper whether to include the upper bound in the results of
     * the iterator
     * @return an iterator for the given range, or <code>null</code> if this
     * field is not saved
     */
    public DictionaryIterator getSavedValuesIterator(boolean caseSensitive,
                                                     Comparable lowerBound,
                                                     boolean includeLower,
                                                     Comparable upperBound,
                                                     boolean includeUpper) {

        if(!field.saved) {
            return null;
        }

        if(info.getType() == FieldInfo.Type.STRING) {
            if(caseSensitive) {
                return dicts[Type.RAW_SAVED.ordinal()].iterator(lowerBound,
                                                                includeLower,
                                                                upperBound,
                                                                includeUpper);
            } else {
                if(dicts[Type.UNCASED_SAVED.ordinal()] == null) {
                    logger.warning(String.format("No case insensitive"
                            + " saved values for %s", info.getName()));
                    return null;
                }
                return dicts[Type.UNCASED_SAVED.ordinal()].iterator(lowerBound.
                        toString(),
                                                                    includeLower, upperBound.
                        toString(),
                                                                    includeUpper);
            }
        } else {
            return dicts[Type.RAW_SAVED.ordinal()].iterator(lowerBound,
                                                            includeLower,
                                                            upperBound,
                                                            includeUpper);
        }
    }

    /**
     * Gets an iterator for the character saved field values that match a
     * given wildcard pattern.
     *
     * @param name The name of the field whose values we wish to match
     * against.
     * @param val The wildcard value against which we will match.
     * @param caseSensitive If <code>true</code>, then case will be taken
     * into account during the match.
     * @param maxEntries The maximum number of entries to return.  If zero or
     * negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     */
    public DictionaryIterator getMatchingIterator(String val,
                                                  boolean caseSensitive,
                                                  int maxEntries,
                                                  long timeLimit) {

        if(info.getType() != FieldInfo.Type.STRING) {
            logger.warning(String.format(
                    "Can't get matching values for non-string field %s", info.
                    getName()));
            return null;
        }
        QueryEntry[] qes;
        if(caseSensitive) {
            qes = dicts[Type.RAW_SAVED.ordinal()].getMatching(savedBigrams, val,
                                                              true,
                                                              maxEntries,
                                                              timeLimit);
        } else {
            if(dicts[Type.UNCASED_SAVED.ordinal()] == null) {
                logger.warning(String.format(
                        "Can't get uncased matches for string field %s",
                        info.getName()));
                return null;
            }
            qes = dicts[Type.UNCASED_SAVED.ordinal()].getMatching(savedBigrams, val.
                    toLowerCase(),
                                                                  false,
                                                                  maxEntries,
                                                                  timeLimit);
        }
        return new ArrayDictionaryIterator(qes);
    }

    public List<QueryEntry> getMatching(String pattern, boolean caseSensitive,
                                        int maxEntries, long timeLimit) {

        List<QueryEntry> ret = new ArrayList<QueryEntry>();

        if(info.getType() != FieldInfo.Type.STRING) {
            logger.warning(String.format(
                    "Can't get matching values for non-string field %s", info.
                    getName()));
            return ret;
        }

        //
        // Check for all asterisks, indicating that we should return everything.
        // We could be fancier here and catch question marks and only return things
        // of the appropriate length.
        if(pattern.matches("\\*+")) {
            for(DictionaryIterator di =
                    dicts[Type.RAW_SAVED.ordinal()].iterator(); di.hasNext();) {
                ret.add((QueryEntry) di.next());
            }
        } else {

            DictionaryIterator di = getMatchingIterator(pattern, caseSensitive,
                                                        maxEntries, timeLimit);
            while(di.hasNext()) {
                ret.add((QueryEntry) di.next());
            }
        }
        return ret;
    }

    /**
     * Gets an iterator for the character saved field values that contain a
     * given substring.
     *
     * @param val The substring that we are looking for.
     * @param caseSensitive If <code>true</code>, then case will be taken
     * into account during the match.
     * @param starts If <code>true</code>, returned values must start with the
     * given substring.
     * @param ends If <code>true</code>, returned values must end with the given
     * substring.
     * @param maxEntries The maximum number of entries to return.  If zero or
     * negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     * @return an iterator for the saved values that have the provided value
     * as a substring, or <code>null</code> if there are no such values.
     */
    public DictionaryIterator getSubstringIterator(String val,
                                                   boolean caseSensitive,
                                                   boolean starts,
                                                   boolean ends,
                                                   int maxEntries,
                                                   long timeLimit) {
        if(info.getType() != FieldInfo.Type.STRING) {
            logger.warning(String.format(
                    "Can't get matching values for non-string field %s", info.
                    getName()));
            return null;
        }
        QueryEntry[] qes;
        if(caseSensitive) {
            qes = dicts[Type.RAW_SAVED.ordinal()].getSubstring(savedBigrams, val,
                                                               caseSensitive,
                                                               starts, ends,
                                                               maxEntries,
                                                               timeLimit);
        } else {
            if(dicts[Type.UNCASED_SAVED.ordinal()] == null) {
                logger.warning(String.format(
                        "Can't get uncased matches for string field %s",
                        info.getName()));
                return null;
            }
            qes = dicts[Type.UNCASED_SAVED.ordinal()].getSubstring(savedBigrams,
                                                                   val.
                    toLowerCase(),
                                                                   false, starts,
                                                                   ends,
                                                                   maxEntries,
                                                                   timeLimit);
        }
        return new ArrayDictionaryIterator(qes);
    }

    /**
     * Gets the values of a string field that are similar to the given field.
     *
     * @param ag an array group 
     * @param value
     * @param caseSensitive
     * @return
     */
    public ArrayGroup getSimilar(String value, boolean caseSensitive) {
        if(info.getType() != FieldInfo.Type.STRING) {
            logger.warning(String.format(
                    "Can't get similar values for non-string field %s", info.
                    getName()));
            return new ScoredGroup(0);
        }
        int[] var = savedBigrams.getAllVariants(value, true);
        int maxd = Integer.MIN_VALUE;
        PriorityQueue<IntEntry> h = new PriorityQueue<IntEntry>();
        for(int i = 0; i < var.length; i++) {
            QueryEntry qe = (caseSensitive ? dicts[Type.RAW_SAVED.ordinal()] : dicts[Type.UNCASED_SAVED.
                    ordinal()]).getByID(
                    var[i]);
            String name = qe.getName().toString();
            if(!caseSensitive) {
                name = CharUtils.toLowerCase(name);
            }
            int d = Util.levenshteinDistance(value, name);
            maxd = Math.max(d, maxd);
            h.offer(new IntEntry(d, qe));
        }

        ScoredQuickOr qor = new ScoredQuickOr((DiskPartition) field.partition,
                                              h.size());
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
        while(h.size() > 0) {
            IntEntry e = h.poll();
            PostingsIterator pi = e.e.iterator(feat);
            if(pi == null) {
                continue;
            }
            qor.add(pi, (maxd - e.i) / (float) maxd, 1);
        }
        ScoredGroup sg = (ScoredGroup) qor.getGroup();
        sg.setNormalized();
        return sg;
    }

    /**
     * Gets a group of all the documents that do not have any values saved for
     * this field.
     *
     * @param ag a set of documents to which we should restrict the search for
     * documents with undefined field values.  If this is <code>null</code> then
     * there is no such restriction.
     * @return a set of documents that have no defined values for this field.
     * This set may be restricted to documents occurring in the group that was
     * passed in.
     */
    public ArrayGroup getUndefined(ArrayGroup ag) {

        ArrayGroup ret = new ArrayGroup(ag == null ? 2048 : ag.getSize());

        //
        // Get local copies of the buffers.
        ReadableBuffer ldtvo = dtvOffsets.duplicate();
        ReadableBuffer ldtv = dtvData.duplicate();

        if(ag == null) {

            //
            // If there's no restriction set, then do all documents.
            ldtvo.position(0);
            for(int i = 0; i < header.maxDocID; i++) {

                //
                // Jump to each offset and decode the number of saved values.  If
                // that's 0, then we have a document that has no defined values, so
                // we put it in the return set.
                ldtv.position(ldtvo.byteDecode(4));
                if(ldtv.byteDecode() == 0) {
                    ret.addDoc(i + 1);
                }
            }
        } else {

            //
            // Just check for the documents in the set.
            for(DocIterator i = ag.iterator(); i.next();) {
                ldtvo.position((i.getDoc() - 1) * 4);
                ldtv.position(ldtvo.byteDecode(4));
                if(ldtv.byteDecode() == 0) {
                    ret.addDoc(i.getDoc());
                }
            }
        }

        return ret;
    }

    /**
     * Gets the length of a document vector for a given document.  Note
     * that this may cause all of the document vector lengths for this
     * partition to be calculated!
     *
     * @param docID the ID of the document for whose vector we want the length
     * @return the length of the document.  If there are any errors getting
     * the length, a value of 1 is returned.
     */
    public float getDocumentVectorLength(int docID) {
        return dvl.getVectorLength(docID);
    }

    /**
     * Normalizes the scores for a number of documents all at once, which will
     * be more efficient than doing them one-by-one
     * @param docs the IDs of the documents
     * @param scores the scores to normalize
     * @param n the number of actual docs and scores
     * @param qw any query weight to apply when normalizing
     */
    public void normalize(int[] docs, float[] scores, int n, float qw) {
        dvl.normalize(docs, scores, n, qw);
    }

    public static void merge(MergeState mergeState, 
                      DiskDictionaryBundle[] bundles) 
            throws java.io.IOException {
        
        long headerPos = mergeState.dictRAF.getFilePointer();
        FieldHeader mergeHeader = new FieldHeader();
        mergeHeader.write(mergeState.dictRAF);
        
        //
        // ID maps for the entries in the dictionaries.  We'll store them all, 
        // even though we'll only use a few.  Later merges will need maps from
        // earlier merges.
        int[][][] entryIDMaps = new int[Type.values().length][][];

        //
        // Merge the types of dictionaries.  This loop is a bit gross, but 
        // most of the merge is the same across the dictionaries.
        for(Type type : Type.values()) {

            //
            // A place to hold the dictionaries for merging.
            int ord = type.ordinal();
            DiskDictionary[] mDicts = new DiskDictionary[bundles.length];
            boolean foundDict = false;

            for(int i = 0; i < bundles.length; i++) {
                mDicts[i] = bundles[i].dicts[ord];
                if(mDicts[i] != null) {
                    foundDict = true;
                }
            }

            NameEncoder encoder = null;

            switch(type) {
                case CASED_TOKENS:
                case UNCASED_TOKENS:
                case STEMMED_TOKENS:
                    //
                    // Tokens are always encoded as strings, and their postings
                    // are mapped using the document ID maps that were passed in
                    // in the merge state.
                    encoder = new StringNameHandler();
                    mergeState.postIDMaps = mergeState.docIDMaps;
                    break;
                case RAW_SAVED:
                case UNCASED_SAVED:
                    //
                    // Saved values are encoded according to their type, and
                    // we use the document ID maps that were passed in to map
                    // their postings.
                    encoder = MemoryDictionaryBundle.getEncoder(mergeState.info);
                    mergeState.postIDMaps = mergeState.docIDMaps;
                    break;
                case RAW_VECTOR:
                    //
                    // The raw vector's names are strings, and the postings are
                    // mapped using the map generated by merging the appropriate
                    // tokens dictionary.
                    encoder = new StringNameHandler();
                    if(entryIDMaps[Type.UNCASED_TOKENS.ordinal()] == null) {
                        mergeState.postIDMaps = entryIDMaps[Type.CASED_TOKENS.
                                ordinal()];
                    } else {
                        mergeState.postIDMaps = entryIDMaps[Type.UNCASED_TOKENS.
                                ordinal()];
                    }
                    break;
                case STEMMED_VECTOR:
                    //
                    // The stemmed vector's names are strings, and we use the
                    // map for the stemmed tokens.
                    encoder = new StringNameHandler();
                    mergeState.postIDMaps = entryIDMaps[Type.STEMMED_TOKENS.
                            ordinal()];
                    break;
            }
            
            if(foundDict) {
                mergeHeader.dictOffsets[ord] = mergeState.dictRAF.getFilePointer();
                entryIDMaps[ord] = DiskDictionary.merge(mergeState.manager.getIndexDir(),
                                                  encoder, 
                                                  mDicts,
                                                  null,
                                                  mergeState.docIDStart,
                                                  mergeState.postIDMaps,
                                                  mergeState.dictRAF, 
                                                  mergeState.postOut, 
                                                  true);
            } else {
                mergeHeader.dictOffsets[ord] = -1;
            }
        }

        DiskBiGramDictionary[] bgDicts =
                new DiskBiGramDictionary[bundles.length];
        DiskBiGramDictionary bgMerger = null;
        for(int i = 0; i < bundles.length; i++) {
            bgDicts[i] = bundles[i].tokenBigrams;
            if(bgDicts[i] != null) {
                bgMerger = bgDicts[i];
            }
        }

        if(bgMerger != null) {
            int[][] tokenIDMap = entryIDMaps[Type.UNCASED_TOKENS.ordinal()] != null ?
                    entryIDMaps[Type.UNCASED_TOKENS.ordinal()] :
                    entryIDMaps[Type.CASED_TOKENS.ordinal()];
            mergeHeader.tokenBGOffset = mergeState.dictRAF.getFilePointer();
            bgMerger.merge(mergeState.manager.getIndexDir(), 
                           bgDicts, mergeState.docIDStart, tokenIDMap, 
                           mergeState.dictRAF,
                           mergeState.postOut[0]);
        }

        Arrays.fill(bgDicts, null);
        bgMerger = null;

        for(int i = 0; i < bundles.length; i++) {
            bgDicts[i] = bundles[i].savedBigrams;
            if(bgDicts[i] != null) {
                bgMerger = bgDicts[i];
            }
        }

        int[][] savedValueIDMap = entryIDMaps[Type.RAW_SAVED.ordinal()]
                != null ? entryIDMaps[Type.RAW_SAVED.ordinal()] : entryIDMaps[Type.UNCASED_SAVED.
                ordinal()];
        if(bgMerger != null) {
            mergeHeader.savedBGOffset = mergeState.dictRAF.getFilePointer();
            bgMerger.merge(mergeState.manager.getIndexDir(), 
                           bgDicts, mergeState.docIDStart, 
                           savedValueIDMap, mergeState.dictRAF,
                           mergeState.postOut[0]);
        }

        long mdsp = mergeState.dictRAF.getFilePointer();

        //
        // Merge the docs to values data.
        if(mergeState.info.hasAttribute(FieldInfo.Attribute.SAVED)) {
            
            File id = mergeState.manager.getIndexDir();

            //
            // File backed buffers for the offsets and encoded values
            File dtvFile = File.createTempFile("dtv", "buff", id);
            RandomAccessFile dtvRAF = new RandomAccessFile(dtvFile, "rw");
            FileWriteableBuffer mdtvBuff = new FileWriteableBuffer(dtvRAF,
                                                                   16384);
            File dtvOffsetFile = File.createTempFile("dtvOff", "buff", id);
            RandomAccessFile dtvOffsetRAF = new RandomAccessFile(dtvOffsetFile,
                                                                 "rw");
            FileWriteableBuffer mdtvOffsetBuff = new FileWriteableBuffer(
                    dtvOffsetRAF, 16384);
            
            for(int i = 0; i < bundles.length; i++) {
                //
                // This partition didn't have this field, but we still want to
                // fill in values.
                if(bundles[i] == null) {
                    for(int j = 0; j < mergeState.nUndel[i]; j++) {
                        mdtvOffsetBuff.byteEncode(mdtvBuff.position(), 4);
                        mdtvBuff.byteEncode(0);
                    }
                    continue;
                }
                
                //
                // Copy the values from this field.
                ReadableBuffer dtvDup = bundles[i].dtvData.duplicate();
                int[] docIDMap = mergeState.docIDMaps[i];
                int[] valIDMap = savedValueIDMap[i];
                
                for(int j = 0; j < bundles[i].header.maxDocID; j++) {
                    int n = dtvDup.byteDecode();
                    
                    if(docIDMap != null && docIDMap[j + 1] < 0) {
                        //
                        // Skip this document's data.
                        for(int k = 0; k < n; k++) {
                            dtvDup.byteDecode();
                        }
                    } else {

                        //
                        // Where the data can be found in the merged partition.
                        mdtvOffsetBuff.byteEncode(mdtvBuff.position(), 4);
                        
                        //
                        // Re-encode the data, keeping in mind that the IDs for
                        // the values were remapped by the values merge above.
                        mdtvBuff.byteEncode(n);
                        for(int k = 0; k < n; k++) {
                            int eid = dtvDup.byteDecode();
                            int mappedID = valIDMap[eid];
                            mdtvBuff.byteEncode(mappedID);
                        }
                    }
                }
            }

            //
            // Transfer the temp buffers.
            mergeHeader.dtvOffset = mergeState.dictRAF.getFilePointer();
            mdtvBuff.write(dtvRAF.getChannel());
            dtvRAF.close();
            dtvFile.delete();

            mergeHeader.dtvPosOffset = mergeState.dictRAF.getFilePointer();
            mdtvOffsetBuff.write(dtvOffsetRAF.getChannel());
            dtvOffsetRAF.close();
            dtvOffsetFile.delete();
        }
        
        //
        // Calculate document vectors.
        if(mergeState.info.hasAttribute(FieldInfo.Attribute.VECTORED)) {

            //
            // Calculate document vector lengths.  We need an iterator for the 
            // main merged dictionary for this.
            long mdp = mergeHeader.dictOffsets[Type.UNCASED_TOKENS.ordinal()];
            if(mdp < 0) {
                mdp = mergeHeader.dictOffsets[Type.UNCASED_TOKENS.ordinal()];
            }
            mdsp = mergeState.dictRAF.getFilePointer();

            if(mdp >= 0) {
                
                //
                // We need a disk dictionary for calculating the lengths, so we'll
                // open the one that we just wrote.
                RandomAccessFile[] mPostRAF = new RandomAccessFile[mergeState.postFiles.length];
                for(int i = 0; i < mergeState.postFiles.length; i++) {
                    mPostRAF[i] = new RandomAccessFile(mergeState.postFiles[i], "rw");
                }
                mergeHeader.vectorLengthOffset = mergeState.vectorLengthRAF.getFilePointer();
                mergeState.dictRAF.seek(mdp);
                DiskDictionary newMainDict =
                        new DiskDictionary(mergeState.entryFactory,
                        new StringNameHandler(),
                        mergeState.dictRAF,
                        mPostRAF);
                DocumentVectorLengths.calculate(mergeState.info, 
                                                mergeState.maxDocID,
                                                mergeState.maxDocID,  
                                                mergeState.manager,
                                                newMainDict.iterator(),
                                                null,  // no term stats calculation during merges.
                                                mergeState.vectorLengthRAF,
                                                mergeState.manager.getTermStatsDict());
                for(RandomAccessFile mprf : mPostRAF) {
                    mprf.close();
                }
                
            } else {
                mergeHeader.vectorLengthOffset = -1;
            }
        }
        
        //
        // Now zip back and write the header.
        mergeState.dictRAF.seek(headerPos);
        mergeHeader.write(mergeState.dictRAF);
        mergeState.dictRAF.seek(mdsp);
    }

    /**
     * Gets a fetcher for field values.
     * 
     * @return a saved field value fetcher.
     */
    public Fetcher getFetcher() {

        if(!field.saved) {
            return null;
        }
        return new Fetcher(dicts[Type.RAW_SAVED.ordinal()], dtvOffsets, dtvData);
    }

    DocumentVectorLengths getDocumentVectorLengths() {
        return dvl;
    }

    /**
     * A class that can be used when you want to get a lot of field values for
     * a particular field, for example, when sorting or clustering results
     * by a particular field.
     */
    public static class Fetcher {

        /**
         * A local copy of the docs-to-vals offset buffer.
         */
        private ReadableBuffer ldtvo;

        /**
         * A local copy of the docs-to-vals buffer.
         */
        private ReadableBuffer ldtv;

        DiskDictionary rawSaved;

        public Fetcher(DiskDictionary rawSaved, ReadableBuffer dtvOffsets,
                       ReadableBuffer dtvData) {
            this.rawSaved = rawSaved;
            ldtvo = dtvOffsets.duplicate();
            ldtv = dtvData.duplicate();
        }

        /**
         * Fetches one value for the given document.
         * @param docID the ID of the document from which we want to get a value.
         * @return the value for the given document.  If no value is stored for
         * that document, then <code>null</code> is returned.
         */
        public Object fetchOne(int docID) {
            ldtv.position(ldtvo.byteDecode(4 * (docID - 1), 4));
            int n = ldtv.byteDecode();
            if(n == 0) {
                return null;
            }
            return rawSaved.getByID(ldtv.byteDecode()).getName();
        }

        /**
         * Fetches all of the values stored in the field for the given document.
         *
         * @param docID
         * @return
         */
        public List<Object> fetch(int docID) {
            return fetch(docID, new ArrayList());
        }

        public List<Object> fetch(int docID, List l) {
            ldtv.position(ldtvo.byteDecode(4 * (docID - 1), 4));
            int n = ldtv.byteDecode();
            for(int i = 0; i < n; i++) {
                l.add(rawSaved.getByID(ldtv.byteDecode()).getName());
            }
            return l;
        }
    }
}
