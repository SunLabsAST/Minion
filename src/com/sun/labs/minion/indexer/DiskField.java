package com.sun.labs.minion.indexer;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.dictionary.ArrayDictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DateNameHandler;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DiskBiGramDictionary;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.DoubleNameHandler;
import com.sun.labs.minion.indexer.dictionary.IntEntry;
import com.sun.labs.minion.indexer.dictionary.LongNameHandler;
import com.sun.labs.minion.indexer.dictionary.NameDecoder;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.DocumentVectorLengths;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.ArrayGroup.DocIterator;
import com.sun.labs.minion.retrieval.ScoredGroup;
import com.sun.labs.minion.retrieval.ScoredQuickOr;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.FileWriteableBuffer;
import com.sun.labs.minion.util.buffer.NIOFileReadableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.PriorityQueue;
import java.util.logging.Logger;

/**
 * A field on-disk
 */
public class DiskField extends Field {

    static Logger logger = Logger.getLogger(DiskField.class.getName());

    /**
     * The header for this field.
     */
    private FieldHeader header;

    /**
     * A dictionary for cased tokens.
     */
    private DiskDictionary<String> casedTokens;

    /**
     * A dictionary for uncased tokens.
     */
    private DiskDictionary<String> uncasedTokens;

    /**
     * A dictionary for token bigrams.
     */
    private DiskBiGramDictionary tokenBigrams;

    /**
     * A dictionary for stemmed tokens.
     */
    private DiskDictionary<String> stemmedTokens;

    /**
     * A dictionary for raw saved values.
     */
    private DiskDictionary<Comparable> rawSaved;

    /**
     * A dictionary for uncased saved values.
     */
    private DiskDictionary<String> uncasedSaved;

    /**
     * A dictionary for saved value bigrams.
     */
    private DiskBiGramDictionary savedBigrams;

    /**
     * The document dictionary for this field, which maps from document keys
     * to the terms that occur in this document.  This is used for vectored fields.
     */
    private DiskDictionary<String> vectors;

    /**
     * A buffer containing the dtv offsets at query time.
     */
    protected ReadableBuffer dtvOffsets;

    /**
     * A buffer containing the actual dtv data at query time.
     */
    protected ReadableBuffer dtvData;

    private DocumentVectorLengths dvl;

    public DiskField(FieldInfo info,
                     RandomAccessFile dictFile,
                     RandomAccessFile vectorLengthsFile,
                     RandomAccessFile[] postIn,
                     EntryFactory factory) throws java.io.IOException {
        super(info);

        header = new FieldHeader(dictFile);
        this.info = info;

        if(header.dictOffsets[CASED_TOKENS] > 0) {
            dictFile.seek(header.dictOffsets[CASED_TOKENS]);
            casedTokens = new DiskDictionary<String>(factory,
                                                     new StringNameHandler(),
                                                     dictFile, postIn);
        }
        if(header.dictOffsets[UNCASED_TOKENS] > 0) {
            dictFile.seek(header.dictOffsets[CASED_TOKENS]);
            uncasedTokens = new DiskDictionary<String>(factory,
                                                       new StringNameHandler(),
                                                       dictFile, postIn);
        }
        if(header.dictOffsets[STEMMED_TOKENS] > 0) {
            dictFile.seek(header.dictOffsets[CASED_TOKENS]);
            stemmedTokens = new DiskDictionary<String>(factory,
                                                       new StringNameHandler(),
                                                       dictFile, postIn);
        }
        if(header.dictOffsets[RAW_SAVED] > 0) {
            dictFile.seek(header.dictOffsets[CASED_TOKENS]);
            rawSaved = new DiskDictionary(factory, getDecoder(),
                                          dictFile, postIn);
        }
        if(header.dictOffsets[UNCASED_SAVED] > 0) {
            dictFile.seek(header.dictOffsets[CASED_TOKENS]);
            uncasedSaved = new DiskDictionary<String>(factory,
                                                      new StringNameHandler(),
                                                      dictFile, postIn);
        }

        if(header.dictOffsets[VECTORS] > 0) {
            dictFile.seek(header.dictOffsets[CASED_TOKENS]);
            vectors = new DiskDictionary<String>(factory,
                                                 new StringNameHandler(),
                                                 dictFile, postIn);
        }

        if(header.tokenBGOffset > 0) {
            dictFile.seek(header.tokenBGOffset);
            tokenBigrams = new DiskBiGramDictionary(dictFile, postIn[0],
                                                    DiskDictionary.PostingsInputType.FILE_PART_POST,
                                                    DiskDictionary.BufferType.FILEBUFFER,
                                                    RAW_SAVED, VECTORS,
                                                    CASED_TOKENS,
                                                    VECTORS, VECTORS, null,
                                                    rawSaved);
        }

        if(header.savedBGOffset > 0) {
            dictFile.seek(header.savedBGOffset);
            tokenBigrams = new DiskBiGramDictionary(dictFile, postIn[0],
                                                    DiskDictionary.PostingsInputType.FILE_PART_POST,
                                                    DiskDictionary.BufferType.FILEBUFFER,
                                                    RAW_SAVED, VECTORS,
                                                    CASED_TOKENS,
                                                    VECTORS, VECTORS, null,
                                                    rawSaved);
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

        if(info.hasAttribute(FieldInfo.Attribute.STEMMED)) {
            stemmer = info.getStemmer();
        }
    }

    @Override
    public int getMaximumDocumentID() {
        return header.maxDocID;
    }

    private NameDecoder getDecoder() {
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

    public DiskDictionary getTermDictionary(boolean cased) {
        if(cased && casedTokens != null) {
            return casedTokens;
        }

        if(!cased && uncasedTokens != null) {
            return uncasedTokens;
        }

        return null;
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
        if(caseSensitive) {
            if(cased) {
                return casedTokens.get(name);
            } else {
                logger.warning(
                        String.format(
                        "Match case requested for term %s in field %s, "
                        + "but this field only has case insensitive terms stored"));
            }
        }

        return uncasedTokens.get(CharUtils.toLowerCase(name));
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
        if(caseSensitive) {
            if(cased) {
                return casedTokens.getByID(id);
            } else {
                logger.warning(
                        String.format(
                        "Match case requested for term %s in field %s, "
                        + "but this field only has case insensitive terms stored"));
            }
        }

        return uncasedTokens.getByID(id);
    }

    public TermStatsImpl getTermStats(String name) {
        return partition.getPartitionManager().getTermStats(name, info);
    }

    public QueryEntry getStem(String stem) {
        return stemmedTokens.get(stem);
    }

    public QueryEntry getVector(String key) {
        if(!vectored) {
            logger.warning(String.format("Requested vector for non-vectored field %s", info.getName()));
            return null;
        }
        return vectors.get(key);
    }

    /**
     * Gets the entry in the dictionary associated with a given value.
     * @param val the value to get
     * @param caseSensitive whether to do a case sensitive lookup
     * @return
     */
    public QueryEntry getSaved(Comparable val, boolean caseSensitive) {
        if(caseSensitive) {
            if(cased) {
                return rawSaved.get(val);
            } else {
                logger.warning(
                        String.format(
                        "Case sensitive request for field value %s in field %s, "
                        + "but this field only has case insensitive values."));
                return rawSaved.get(val);
            }
        }

        return uncasedSaved.get(CharUtils.toLowerCase((val.toString())));
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

        if(!saved) {
            return null;
        }

        if(info.getType() == FieldInfo.Type.STRING) {
            if(caseSensitive) {
                return rawSaved.iterator(lowerBound, includeLower, upperBound,
                                         includeUpper);
            } else {
                if(uncasedSaved == null) {
                    logger.warning(String.format("No case insensitive"
                            + " saved values for %s", info.getName()));
                    return null;
                }
                return uncasedSaved.iterator(lowerBound.toString(),
                                             includeLower, upperBound.toString(),
                                             includeUpper);
            }
        } else {
            return rawSaved.iterator(lowerBound, includeLower, upperBound,
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
            qes = rawSaved.getMatching(savedBigrams, val, true,
                                       maxEntries, timeLimit);
        } else {
            if(uncasedSaved == null) {
                logger.warning(String.format(
                        "Can't get uncased matches for string field %s",
                        info.getName()));
                return null;
            }
            qes = uncasedSaved.getMatching(savedBigrams, val.toLowerCase(),
                                           false,
                                           maxEntries, timeLimit);
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
            for(Entry e : rawSaved) {
                ret.add((QueryEntry) e);
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
            qes = rawSaved.getSubstring(savedBigrams, val, caseSensitive,
                                        starts, ends, maxEntries, timeLimit);
        } else {
            if(uncasedSaved == null) {
                logger.warning(String.format(
                        "Can't get uncased matches for string field %s",
                        info.getName()));
                return null;
            }
            qes = uncasedSaved.getSubstring(savedBigrams,
                                            val.toLowerCase(),
                                            false, starts, ends,
                                            maxEntries, timeLimit);
        }
        return new ArrayDictionaryIterator(qes);
    }

    /**
     * Gets the values of a string field that are similar to the given field.
     *
     * @param ag an array group agains
     * @param value
     * @param caseSensitive
     * @return
     */
    public ArrayGroup getSimilar(String value, boolean caseSensitive) {
        if(info.getType() != FieldInfo.Type.STRING) {
            logger.warning(String.format("Can't get similar values for non-string field %s", info.getName()));
            return new ScoredGroup(0);
        }
        int[] var = savedBigrams.getAllVariants(value, true);
        int maxd = Integer.MIN_VALUE;
        PriorityQueue<IntEntry> h = new PriorityQueue<IntEntry>();
        for(int i = 0; i < var.length; i++) {
            QueryEntry qe = (caseSensitive ? rawSaved : uncasedSaved).getByID(var[i]);
            String name = qe.getName().toString();
            if(!caseSensitive) {
                name = CharUtils.toLowerCase(name);
            }
            int d = Util.levenshteinDistance(value, name);
            maxd = Math.max(d, maxd);
            h.offer(new IntEntry(d, qe));
        }

        ScoredQuickOr qor = new ScoredQuickOr((DiskPartition) partition, h.size());
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

    public void merge(DiskField[] fields,
                      int[] starts,
                      int[][] docIDMaps,
                      int[] nUndel,
                      RandomAccessFile mDict,
                      PostingsOutput[] mPostOut,
                      RandomAccessFile mVectorLengths)
            throws java.io.IOException {

        logger.info(String.format("Merge %s", info.getName()));

        long headerPos = mDict.getFilePointer();
        FieldHeader mergeHeader = new FieldHeader();
        mergeHeader.write(mDict);

        DiskDictionary[] dicts = new DiskDictionary[fields.length];
        DiskDictionary merger = null;

        //
        // ID maps from the main dictionary, that we'll use to merge the
        // document vectors.
        int[][] tokenIDMap = null;
        int[][] savedIDMap = null;

        for(int i = 0; i < fields.length; i++) {
            dicts[i] = fields[i].casedTokens;
            if(dicts[i] != null) {
                merger = dicts[i];
            }
        }

        if(merger != null) {
            mergeHeader.dictOffsets[CASED_TOKENS] = mDict.getFilePointer();
            tokenIDMap = merger.merge(new StringNameHandler(), dicts, null,
                                      starts,
                                      docIDMaps, mDict, mPostOut, true);
        } else {
            mergeHeader.dictOffsets[CASED_TOKENS] = -1;
        }

        Arrays.fill(dicts, null);
        merger = null;

        for(int i = 0; i < fields.length; i++) {
            dicts[i] = fields[i].uncasedTokens;
            if(dicts[i] != null) {
                merger = dicts[i];
            }
        }

        if(merger != null) {
            mergeHeader.dictOffsets[UNCASED_TOKENS] = mDict.getFilePointer();
            tokenIDMap = merger.merge(new StringNameHandler(), dicts, null,
                                      starts,
                                      docIDMaps, mDict, mPostOut, true);
        } else {
            mergeHeader.dictOffsets[UNCASED_TOKENS] = -1;
        }

        Arrays.fill(dicts, null);
        merger = null;

        for(int i = 0; i < fields.length; i++) {
            dicts[i] = fields[i].stemmedTokens;
            if(dicts[i] != null) {
                merger = dicts[i];
            }
        }

        if(merger != null) {
            mergeHeader.dictOffsets[STEMMED_TOKENS] = mDict.getFilePointer();
            merger.merge(new StringNameHandler(), dicts, null, starts,
                         docIDMaps, mDict, mPostOut, true);
        } else {
            mergeHeader.dictOffsets[STEMMED_TOKENS] = -1;
        }

        Arrays.fill(dicts, null);
        merger = null;

        for(int i = 0; i < fields.length; i++) {
            dicts[i] = fields[i].rawSaved;
            if(dicts[i] != null) {
                merger = dicts[i];
            }
        }

        if(merger != null) {
            mergeHeader.dictOffsets[RAW_SAVED] = mDict.getFilePointer();
            savedIDMap = merger.merge(MemoryField.getEncoder(info), dicts, null,
                                      starts,
                                      docIDMaps, mDict, mPostOut, true);
        } else {
            mergeHeader.dictOffsets[RAW_SAVED] = -1;
        }

        Arrays.fill(dicts, null);
        merger = null;

        for(int i = 0; i < fields.length; i++) {
            dicts[i] = fields[i].uncasedSaved;
            if(dicts[i] != null) {
                merger = dicts[i];
            }
        }

        if(merger != null) {
            mergeHeader.dictOffsets[UNCASED_SAVED] = mDict.getFilePointer();
            savedIDMap = merger.merge(MemoryField.getEncoder(info), dicts, null,
                                      starts,
                                      docIDMaps, mDict, mPostOut, true);
        } else {
            mergeHeader.dictOffsets[UNCASED_SAVED] = -1;
        }

        Arrays.fill(dicts, null);
        merger = null;

        for(int i = 0; i < fields.length; i++) {
            dicts[i] = fields[i].vectors;
            if(dicts[i] != null) {
                merger = dicts[i];
            }
        }

        if(merger != null) {
            mergeHeader.dictOffsets[VECTORS] = mDict.getFilePointer();
            merger.merge(new StringNameHandler(), dicts, null, null,
                         tokenIDMap, mDict, mPostOut, true);
        } else {
            mergeHeader.dictOffsets[VECTORS] = -1;
        }

        DiskBiGramDictionary[] bgDicts = new DiskBiGramDictionary[fields.length];
        DiskBiGramDictionary bgMerger = null;
        for(int i = 0; i < fields.length; i++) {
            bgDicts[i] = fields[i].tokenBigrams;
            if(bgDicts[i] != null) {
                bgMerger = bgDicts[i];
            }
        }

        if(bgMerger != null) {
            mergeHeader.tokenBGOffset = mDict.getFilePointer();
            bgMerger.merge(bgDicts, starts, tokenIDMap, mDict, mPostOut[0]);
        }

        Arrays.fill(bgDicts, null);
        bgMerger = null;

        for(int i = 0; i < fields.length; i++) {
            bgDicts[i] = fields[i].savedBigrams;
            if(bgDicts[i] != null) {
                bgMerger = bgDicts[i];
            }
        }

        if(bgMerger != null) {
            mergeHeader.savedBGOffset = mDict.getFilePointer();
            bgMerger.merge(bgDicts, starts, savedIDMap, mDict, mPostOut[0]);
        }

        //
        // Merge the docs to values data.
        if(saved) {
            File id = partition.getPartitionManager().getIndexDir();

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
            for(int i = 0; i < fields.length; i++) {
                //
                // This partition didn't have this field, but we still want to
                // fill in values.
                if(fields[i] == null) {
                    for(int j = 0; j < nUndel[i]; j++) {
                        mdtvOffsetBuff.byteEncode(mdtvBuff.position(), 4);
                        mdtvBuff.byteEncode(0);
                    }
                    continue;
                }

                //
                // Copy the values from this field.
                ReadableBuffer dtvDup = fields[i].dtvData.duplicate();
                int[] docIDMap = docIDMaps[i];
                int[] valIDMap = savedIDMap[i];

                for(int j = 0; j < fields[i].header.maxDocID; j++) {
                    int n = dtvDup.byteDecode();

                    if(docIDMap != null && docIDMap[j + 1] < 0) {
                        //
                        // Skip this document's data.
                        for(int k = 0; k < n; k++) {
                            dtvDup.byteDecode();
                        }
                    } else {

                        //
                        // Where the data can be found.
                        mdtvOffsetBuff.byteEncode(mdtvBuff.position(), 4);
                        //
                        // Re-encode the data, keeping in mind that the IDs for
                        // the values were remapped by the values merge above.
                        mdtvBuff.byteEncode(n);
                        for(int k = 0; k < n; k++) {
                            int mappedID = valIDMap[dtvDup.byteDecode()];
                            mdtvBuff.byteEncode(mappedID);
                        }
                    }
                }
            }

            //
            // Transfer the temp buffers.
            mergeHeader.dtvOffset = mDict.getFilePointer();
            mdtvBuff.write(dtvRAF.getChannel());
            dtvRAF.close();
            dtvFile.delete();

            mergeHeader.dtvPosOffset = mDict.getFilePointer();
            mdtvOffsetBuff.write(dtvOffsetRAF.getChannel());
            dtvOffsetRAF.close();
            dtvOffsetFile.delete();

            //
            // Calculate document vector lengths.
            mergeHeader.vectorLengthOffset = mVectorLengths.getFilePointer();
            DocumentVectorLengths.calculate(this, null, mVectorLengths,
                                            partition.getPartitionManager().
                    getTermStatsDict());

            //
            // Now zip back and write the header.
            long end = mDict.getFilePointer();
            mDict.seek(headerPos);
            mergeHeader.write(mDict);
            mDict.seek(end);

        }
    }

    /**
     * Gets a fetcher for field values.
     * 
     * @return a saved field value fetcher.
     */
    public Fetcher getFetcher() {

        if(!saved) {
            return null;
        }
        return new Fetcher();
    }

    /**
     * A class that can be used when you want to get a lot of field values for
     * a particular field, for example, when sorting or clustering results
     * by a particular field.
     */
    public class Fetcher {

        /**
         * A local copy of the docs-to-vals offset buffer.
         */
        private ReadableBuffer ldtvo;

        /**
         * A local copy of the docs-to-vals buffer.
         */
        private ReadableBuffer ldtv;

        public Fetcher() {
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
