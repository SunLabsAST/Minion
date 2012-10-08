package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.DiskField;
import com.sun.labs.minion.indexer.Field;
import com.sun.labs.minion.indexer.FieldHeader;
import com.sun.labs.minion.indexer.Field.DictionaryType;
import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.entry.TermStatsIndexEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.DocumentVectorLengths;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.MergeState;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.ArrayGroup.DocIterator;
import com.sun.labs.minion.retrieval.LocalFacet;
import com.sun.labs.minion.retrieval.ResultImpl;
import com.sun.labs.minion.retrieval.ScoredGroup;
import com.sun.labs.minion.retrieval.ScoredQuickOr;
import com.sun.labs.minion.retrieval.SortSpec;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.util.CDateParser;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.Util;
import com.sun.labs.minion.util.buffer.FileWriteableBuffer;
import com.sun.labs.minion.util.buffer.NIOFileReadableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import com.sun.labs.util.NanoWatch;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A bundle of dictionaries to be used at query time.
 */
public class DiskDictionaryBundle<N extends Comparable> {

    private static final Logger logger =
            Logger.getLogger(DiskDictionaryBundle.class.getName());

    /**
     * Where the header was read from.
     */
    protected long headerPos;

    /**
     * The file our dictionaries were read from.
     */
    protected RandomAccessFile dictFile;

    /**
     * The header for this field.
     */
    protected FieldHeader header;

    /**
     * The dictionaries.
     */
    private DiskDictionary[] dicts;

    /**
     * The field we're associated with.
     */
    private DiskField field;

    /**
     * The field info, which we'll use a lot.
     */
    private FieldInfo info;

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
    private DocumentVectorLengths[] dvl;

    protected CDateParser dateParser;
    
    public DiskDictionaryBundle(DiskField field,
            RandomAccessFile dictFile,
            RandomAccessFile vectorLengthsFile,
            RandomAccessFile[] postIn) throws
            java.io.IOException {
        this.field = field;
        this.dictFile = dictFile;
        info = field.getInfo();

        headerPos = dictFile.getFilePointer();
        header = new FieldHeader(dictFile);
        dicts = new DiskDictionary[DictionaryType.values().length];
        
        for(DictionaryType type : DictionaryType.values()) {
            int ord = type.ordinal();
            NameDecoder decoder = null;
            EntryFactory entryFactory = null;
            if(header.dictionaryOffsets[ord] >= 0) {
                dictFile.seek(header.dictionaryOffsets[ord]);
            } else {
                continue;
            }

            switch(type) {
                case CASED_TOKENS:
                case UNCASED_TOKENS:
                case STEMMED_TOKENS:
                    decoder = new StringNameHandler();
                    if(info.hasAttribute(FieldInfo.Attribute.POSITIONS)) {
                        entryFactory = new EntryFactory(Postings.Type.ID_FREQ_POS);
                    } else {
                        entryFactory = new EntryFactory(Postings.Type.ID_FREQ);
                    }
                    break;

                case RAW_SAVED:
                case UNCASED_SAVED:
                    decoder = getDecoder();
                    entryFactory = new EntryFactory(Postings.Type.ID);
                    break;
                case RAW_VECTOR:
                case STEMMED_VECTOR:
                    decoder = new StringNameHandler();
                    entryFactory = new EntryFactory(Postings.Type.ID_FREQ);
                    break;
                case TOKEN_BIGRAMS:
                    DiskDictionary tokenDict =
                            dicts[DictionaryType.CASED_TOKENS.ordinal()] != null
                            ? dicts[DictionaryType.CASED_TOKENS.ordinal()]
                            : dicts[DictionaryType.UNCASED_TOKENS.ordinal()];
                    dicts[ord] = new DiskBiGramDictionary(dictFile, postIn[0],
                            DiskDictionary.PostingsInputType.FILE_PART_POST,
                            DiskDictionary.BufferType.NIOFILEBUFFER,
                            256, 2048, 2048, 2048, 2048,
                            null,
                            tokenDict);
                    dicts[ord].setPartition(field.getPartition());
                    continue;
                case SAVED_VALUE_BIGRAMS:
                    dicts[ord] =
                            new DiskBiGramDictionary(dictFile, postIn[0],
                            DiskDictionary.PostingsInputType.FILE_PART_POST,
                            DiskDictionary.BufferType.NIOFILEBUFFER,
                            256, 2048, 2048, 2048, 2048,
                            null,
                            dicts[DictionaryType.RAW_SAVED.ordinal()]);
                    dicts[ord].setPartition(field.getPartition());
                    continue;
                default:
            }

            try {
                dicts[ord] = new DiskDictionary(entryFactory, decoder, dictFile, postIn);
                dicts[ord].setPartition(field.getPartition());
            } catch(IOException ex) {
                logger.log(Level.SEVERE,
                        String.format("Error opening %s %s for %d", 
                        field.getInfo().getName(),
                        type, 
                        field.getPartition().getPartitionNumber()));
                throw (ex);
            }
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

        //
        // Load the document vector lengths.
        setVectorLengths(vectorLengthsFile);
    }
    
    public void setVectorLengths(RandomAccessFile vectorLengthsFile) throws IOException {
        dvl = new DocumentVectorLengths[header.vectorLengthOffsets.length];
        for(int i = 0; i < header.vectorLengthOffsets.length; i++) {
            if(header.vectorLengthOffsets[i] >= 0) {
                vectorLengthsFile.seek(header.vectorLengthOffsets[i]);
                dvl[i] = new DocumentVectorLengths(vectorLengthsFile, 8192);
            }
        }
    }

    public String[] getPostingsChannelNames() {
        String[] max = null;
        for(DiskDictionary dict : dicts) {
            if(dict != null) {
                String[] t = dict.getPostingsChannelNames();
                if(max == null || t.length > max.length) {
                    max = t;
                }
            }
        }
        return max;
    }

    public Postings.Type getTokenPostingsType() {
        if(info.hasAttribute(FieldInfo.Attribute.POSITIONS)) {
            return Postings.Type.ID_FREQ_POS;
        } else {
            return Postings.Type.ID_FREQ;
        }
    }

    public DiskDictionary getSavedValuesDictionary() {
        return dicts[DictionaryType.RAW_SAVED.ordinal()];
    }

    public FieldHeader getHeader() {
        return header;
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

    public DiskDictionary getTermDictionary() {
        if(dicts[DictionaryType.UNCASED_TOKENS.ordinal()] != null) {
            return dicts[DictionaryType.UNCASED_TOKENS.ordinal()];
        }
        return dicts[DictionaryType.CASED_TOKENS.ordinal()];
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
        DiskDictionary ct = dicts[DictionaryType.CASED_TOKENS.ordinal()];
        DiskDictionary ut = dicts[DictionaryType.UNCASED_TOKENS.ordinal()];
        if(caseSensitive) {
            if(field.isCased()) {
                if(ct != null) {
                    ret = ct.get(name);
                }
            } else {
                logger.warning(
                        String.format(
                        "Match case requested for term %s in %s, "
                        + "but this field only has case insensitive terms stored",
                        name, info.getName()));
            }
        } else {
            if(field.isUncased()) {
                if(ut != null) {
                    ret = ut.get(CharUtils.toLowerCase(name));
                }
            } else {
                logger.warning(
                        String.format(
                        "Case insenstive get requested for term %s in %s, "
                        + "but this field only has case sensitive terms stored",
                        name, info.getName()));
            }
        }

        if(ret != null) {
            ret.setField(info);
        }
        return ret;
    }

    public QueryEntry getDocumentTerm(int id) {
        if(dicts[DictionaryType.UNCASED_TOKENS.ordinal()] != null) {
            return dicts[DictionaryType.UNCASED_TOKENS.ordinal()].getByID(id);
        } else {
            return dicts[DictionaryType.CASED_TOKENS.ordinal()].getByID(id);
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
    public QueryEntry getTerm(int id, boolean caseSensitive) {
        QueryEntry ret = null;
        if(caseSensitive) {
            if(field.isCased()) {
                ret = dicts[DictionaryType.CASED_TOKENS.ordinal()].getByID(id);
            } else {
                logger.warning(
                        String.format(
                        "Match case requested for term %s in field %s, "
                        + "but this field only has case insensitive terms stored"));
            }
        }

        if(dicts[DictionaryType.UNCASED_TOKENS.ordinal()] != null) {
            ret = dicts[DictionaryType.UNCASED_TOKENS.ordinal()].getByID(id);
        }
        if(ret != null) {
            ret.setField(info);
        }
        return ret;
    }

    public List<QueryEntry> getWildcardMatches(String pat, boolean caseSensitive,
            int maxEntries, long timeLimit) {

        DiskDictionary tokenDict =
                dicts[DictionaryType.CASED_TOKENS.ordinal()] != null
                ? dicts[DictionaryType.CASED_TOKENS.ordinal()]
                : dicts[DictionaryType.UNCASED_TOKENS.ordinal()];

        if(tokenDict == null) {
            return Collections.EMPTY_LIST;
        }

        QueryEntry[] qes = tokenDict.getMatching(
                (DiskBiGramDictionary) dicts[DictionaryType.TOKEN_BIGRAMS.ordinal()],
                pat,
                caseSensitive,
                maxEntries,
                timeLimit);

        if(qes == null) {
            return Collections.EMPTY_LIST;
        }

        return Arrays.asList(qes);

    }

    public QueryEntry getStem(String stem) {
        QueryEntry ret = null;
        if(dicts[DictionaryType.STEMMED_TOKENS.ordinal()] != null) {
            ret = dicts[DictionaryType.STEMMED_TOKENS.ordinal()].get(stem);
        }

        if(ret != null) {
            ret.setField(info);
        }
        return ret;
    }

    public QueryEntry getVector(String key) {
        QueryEntry ret = null;
        if(!field.isVectored()) {
            logger.warning(
                    String.format("Requested vector for non-vectored field %s",
                    info.getName()));
            return null;
        }
        if(!field.isStemmed()) {
            ret = dicts[DictionaryType.RAW_VECTOR.ordinal()].get(key);
        } else {
            ret = dicts[DictionaryType.STEMMED_VECTOR.ordinal()].get(key);
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
    public QueryEntry getSaved(String stringVal, boolean caseSensitive) {
        if(!field.isSaved()) {
            logger.warning(String.format(
                    "Can't fetch saved value for non-SAVED field %s", info.getName()));
            return null;
        }

        DiskDictionary rs = dicts[DictionaryType.RAW_SAVED.ordinal()];
        DiskDictionary us = dicts[DictionaryType.UNCASED_SAVED.ordinal()];

        //
        // Handle string fields and their casedness separately.
        if(field.getInfo().getType() == FieldInfo.Type.STRING) {

            //
            // Case sensitive requests go to the raw dictionary.  We'll warn
            // on case sensitive requests on case insensitive fields.
            if(caseSensitive) {
                //
                // No raw dictionary means no result, but that might be OK if there
                // were no values in this partition.
                if(rs == null) {
                    if(!field.isCased()) {
                        logger.warning(
                                String.format(
                                "Case sensitive request for field value %s in field %s, "
                                + "but this field only has case insensitive values.",
                                stringVal, field.getInfo().getName()));
                    }
                    return null;
                }
                return rs.get(stringVal);
            } else {
                if(us == null) {
                    if(!field.isUncased()) {
                        logger.warning(
                                String.format(
                                "Case insensitive request for field value %s in field %s, "
                                + "but this field only has case sensitive values.",
                                stringVal, field.getInfo().getName()));
                    }
                    return null;
                }
                return us.get(stringVal);
            }
        }
        
        if(rs == null) {
            return null;
        }
        
        return rs.get(MemoryDictionaryBundle.getEntryName(stringVal, info, dateParser));
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

        if(!field.isSaved()) {
            return null;
        }

        if(info.getType() == FieldInfo.Type.STRING) {
            if(caseSensitive) {
                return dicts[DictionaryType.RAW_SAVED.ordinal()].iterator(lowerBound,
                        includeLower,
                        upperBound,
                        includeUpper);
            } else {
                if(dicts[DictionaryType.UNCASED_SAVED.ordinal()] == null) {
                    logger.warning(String.format("No case insensitive"
                            + " saved values for %s", info.getName()));
                    return null;
                }
                return dicts[DictionaryType.UNCASED_SAVED.ordinal()].iterator(lowerBound.toString(),
                        includeLower,
                        upperBound.toString(),
                        includeUpper);
            }
        } else {
            return dicts[DictionaryType.RAW_SAVED.ordinal()].iterator(lowerBound,
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
                    "Can't get matching values for non-string field %s", info.getName()));
            return null;
        }

        QueryEntry[] qes = dicts[DictionaryType.RAW_SAVED.ordinal()].getMatching(
                (DiskBiGramDictionary) dicts[DictionaryType.SAVED_VALUE_BIGRAMS.ordinal()],
                val,
                caseSensitive,
                maxEntries,
                timeLimit);
        return new ArrayDictionaryIterator(qes);
    }

    public List<QueryEntry> getMatching(String pattern, boolean caseSensitive,
            int maxEntries, long timeLimit) {

        List<QueryEntry> ret = new ArrayList<QueryEntry>();

        if(info.getType() != FieldInfo.Type.STRING) {
            logger.warning(String.format(
                    "Can't get matching values for non-string field %s", info.getName()));
            return ret;
        }

        //
        // Check for all asterisks, indicating that we should return everything.
        // We could be fancier here and catch question marks and only return things
        // of the appropriate length.
        if(pattern.matches("\\*+")) {
            for(DictionaryIterator di =
                    (DictionaryIterator) dicts[DictionaryType.RAW_SAVED.ordinal()].iterator(); di.hasNext();) {
                ret.add((QueryEntry) di.next());
            }
        } else {

            DictionaryIterator di = getMatchingIterator(pattern, caseSensitive,
                    maxEntries, timeLimit);
            if(di != null) {
                while(di.hasNext()) {
                    ret.add((QueryEntry) di.next());
                }
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
                    "Can't get matching values for non-string field %s", info.getName()));
            return null;
        }
        QueryEntry[] qes = dicts[DictionaryType.RAW_SAVED.ordinal()].getSubstring(
                (DiskBiGramDictionary) dicts[DictionaryType.SAVED_VALUE_BIGRAMS.ordinal()],
                val,
                caseSensitive,
                starts, ends,
                maxEntries,
                timeLimit);
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
                    "Can't get similar values for non-string field %s", info.getName()));
            return new ScoredGroup(0);
        }
        int[] var = ((DiskBiGramDictionary) dicts[DictionaryType.SAVED_VALUE_BIGRAMS.ordinal()]).getAllVariants(value, true);
        int maxd = Integer.MIN_VALUE;
        PriorityQueue<IntEntry> h = new PriorityQueue<IntEntry>();
        for(int i = 0; i < var.length; i++) {
            QueryEntry qe = (caseSensitive ? dicts[DictionaryType.RAW_SAVED.ordinal()]
                    : dicts[DictionaryType.UNCASED_SAVED.ordinal()]).getByID(
                    var[i]);
            String name = qe.getName().toString();
            if(!caseSensitive) {
                name = CharUtils.toLowerCase(name);
            }
            int d = Util.levenshteinDistance(value, name);
            maxd = Math.max(d, maxd);
            h.offer(new IntEntry(d, qe));
        }

        ScoredQuickOr qor =
                new ScoredQuickOr((DiskPartition) field.getPartition(), h.size());
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
        return getDocumentVectorLength(docID, Field.DocumentVectorType.RAW);
    }
    
    public float getDocumentVectorLength(int docID, Field.DocumentVectorType type) {
        return dvl[type.ordinal()].getVectorLength(docID);
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
        normalize(docs, scores, n, qw, Field.DocumentVectorType.RAW);
    }

    public void normalize(int[] docs, float[] scores, int n, float qw, Field.DocumentVectorType type) {
        dvl[type.ordinal()].normalize(docs, scores, n, qw);
    }
    
    public static void merge(MergeState mergeState, DiskDictionaryBundle[] bundles)
            throws java.io.IOException {

        DictionaryOutput fieldDictOut = mergeState.partOut.getPartitionDictionaryOutput();
        long headerPos = fieldDictOut.position();
        FieldHeader mergeHeader = new FieldHeader();
        mergeHeader.fieldID = mergeState.info.getID();
        mergeHeader.maxDocID = mergeState.partOut.getMaxDocID();
        mergeHeader.write(fieldDictOut);
        DiskDictionaryBundle exemplar = null;

        //
        // Find a non-null bundle for when we need an actual bundle.
        for(DiskDictionaryBundle bundle : bundles) {
            if(bundle != null) {
                exemplar = bundle;
                break;
            }
        }
        
        //
        // Whether we're merging document IDs in the dictionary entries, in which
        // case we need to use the entry mappers in the merge state.
        boolean mergingDocEntries;

        //
        // ID maps for the entries in the dictionaries.  We'll store them all, 
        // even though we'll only use a few.  Later merges will need maps from
        // earlier merges.
        int[][][] entryIDMaps = new int[DictionaryType.values().length][][];
        
        //
        // Merge the types of dictionaries.  This loop is a bit gross, but 
        // most of the merge is the same across the dictionaries.
        NanoWatch dw = new NanoWatch();

        for(DictionaryType type : DictionaryType.values()) {

            //
            // A place to hold the dictionaries for merging.
            int ord = type.ordinal();
            DiskDictionary[] mDicts = new DiskDictionary[bundles.length];
            DiskBiGramDictionary[] bgDicts = new DiskBiGramDictionary[bundles.length];
            boolean foundDict = false;
            mergingDocEntries = false;

            for(int i = 0; i < bundles.length; i++) {

                //
                // Handle bundles from empty fields.
                if(bundles[i] == null) {
                    mDicts[i] = null;
                } else {
                    mDicts[i] = bundles[i].dicts[ord];
                }
                if(mDicts[i] != null) {
                    foundDict = true;
                }
            }

            if(!foundDict) {
                mergeHeader.dictionaryOffsets[ord] = -1;
                logger.finer(String.format("No dicts for %s", type));
                continue;
            }

            NameEncoder encoder = null;
            mergeHeader.dictionaryOffsets[ord] = fieldDictOut.position();
            
            //
            // Starting IDs for the new merged dictionaries.
            int[] idStarts = mergeState.docIDStarts;

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
                    if(entryIDMaps[DictionaryType.UNCASED_TOKENS.ordinal()] != null) {
                        mergeState.postIDMaps = entryIDMaps[DictionaryType.UNCASED_TOKENS.ordinal()];
                    } else {
                        mergeState.postIDMaps = entryIDMaps[DictionaryType.CASED_TOKENS.ordinal()];
                    }
                    idStarts = mergeState.fakeStarts;
                    mergingDocEntries = true;
                    break;
                case STEMMED_VECTOR:
                    //
                    // The stemmed vector's names are strings, and we use the
                    // map for the stemmed tokens.
                    encoder = new StringNameHandler();
                    mergeState.postIDMaps = entryIDMaps[DictionaryType.STEMMED_TOKENS.ordinal()];
                    idStarts = mergeState.fakeStarts;
                    mergingDocEntries = true;
                    break;

                case TOKEN_BIGRAMS:
                    if(entryIDMaps[DictionaryType.CASED_TOKENS.ordinal()] != null) {
                        mergeState.entryIDMaps = entryIDMaps[DictionaryType.CASED_TOKENS.ordinal()];
                    } else {
                        mergeState.entryIDMaps = entryIDMaps[DictionaryType.UNCASED_TOKENS.ordinal()];
                    }
                    for(int i = 0; i < mDicts.length; i++) {
                        bgDicts[i] = (DiskBiGramDictionary) mDicts[i];
                    }
                    try {
                        DiskBiGramDictionary.merge(mergeState, bgDicts);
                    } catch(RuntimeException ex) {
                        logger.log(Level.SEVERE, String.format(
                                "Exception merging %s of field %s using bigrams from %s",
                                type, 
                                mergeState.info.getName(), 
                                entryIDMaps[DictionaryType.CASED_TOKENS.ordinal()] != null ? DictionaryType.CASED_TOKENS : DictionaryType.UNCASED_TOKENS));
                        throw (ex);

                    }
                    continue;

                case SAVED_VALUE_BIGRAMS:
                    mergeState.entryIDMaps = entryIDMaps[DictionaryType.RAW_SAVED.ordinal()];
                    for(int i = 0; i < mDicts.length; i++) {
                        bgDicts[i] = (DiskBiGramDictionary) mDicts[i];
                    }
                    try {
                        DiskBiGramDictionary.merge(mergeState, bgDicts);
                    } catch(RuntimeException ex) {
                        logger.log(Level.SEVERE, String.format("Exception merging %s of field %s",
                                type, mergeState.info.getName()));
                        throw (ex);

                    }
                    continue;
            }

            if(logger.isLoggable(Level.FINER)) {
                dw.start();
            }

            try {
                entryIDMaps[ord] = DiskDictionary.merge(mergeState.manager.getIndexDir(),
                        encoder,
                        mDicts,
                        mergingDocEntries ? mergeState.docIDMappers : null,
                        idStarts,
                        mergeState.postIDMaps,
                        fieldDictOut,
                        mergeState.partOut.getPostingsOutput(),
                        true);
            } catch(RuntimeException ex) {
                logger.log(Level.SEVERE, String.format("Exception merging %s of field %s",
                        type, mergeState.info.getName()));
                throw (ex);
            }
            
            if(logger.isLoggable(Level.FINER)) {
                dw.stop();
                logger.finer(String.format("Merging %s took %.2fms", type, dw.
                        getTimeMillis()));
                dw.reset();
            }

        }
        
        //
        // Merge the docs to values data, if we wrote and dictionaries!
        if(mergeState.info.hasAttribute(FieldInfo.Attribute.SAVED) && entryIDMaps[DictionaryType.RAW_SAVED.ordinal()] != null) {

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
                    dtvOffsetRAF, 1 << 16);

            if(logger.isLoggable(Level.FINER)) {
                logger.finer(String.format("Merging docs to values"));
                dw.start();
            }
            for(int i = 0; i < bundles.length; i++) {

                //
                // This partition didn't have this field, but we still want to
                // fill in values.
                if(bundles[i] == null || bundles[i].dtvData == null) {
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
                int[] valIDMap = entryIDMaps[DictionaryType.RAW_SAVED.ordinal()][i];

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
                            try {
                            mdtvBuff.byteEncode(mappedID);
                            } catch (ArithmeticException ex) {
                                logger.log(Level.SEVERE, 
                                        String.format("Error encoding data for field %s from %s orig ID %d",
                                        mergeState.info.getName(),
                                        bundles[i].dicts[DictionaryType.RAW_SAVED.ordinal()].getPartition(), 
                                        eid));
                                throw(ex);
                            }
                        }
                    }
                }
            }
            //
            // Transfer the temp buffers into the dictionary file.
            mergeHeader.dtvOffset = fieldDictOut.position();
            mdtvBuff.write(fieldDictOut);
            dtvRAF.close();
            dtvFile.delete();

            mergeHeader.dtvPosOffset = fieldDictOut.position();
            mdtvOffsetBuff.write(fieldDictOut);
            dtvOffsetRAF.close();
            dtvOffsetFile.delete();
            
            if(logger.isLoggable(Level.FINER)) {
                dw.stop();
                logger.fine(String.format("Merging d2v took %.2fms", dw.getTimeMillis()));
                dw.reset();
            }
        }


        //
        // Calculate document vector lengths.
        if(mergeState.info.hasAttribute(FieldInfo.Attribute.INDEXED)
                && !mergeState.partOut.isLongIndexingRun()) {

            //
            // Calculate document vector lengths.  We need an iterator for the 
            // main merged dictionary for this.
            long mdp = mergeHeader.dictionaryOffsets[DictionaryType.UNCASED_TOKENS.ordinal()];
            if(mdp < 0) {
                mdp = mergeHeader.dictionaryOffsets[DictionaryType.CASED_TOKENS.ordinal()];
            }
            if(mdp >= 0) {

                //
                // We need a disk dictionary for calculating the lengths, so we'll
                // open the one that we just wrote.  We'll start by remembering where 
                // we were in the file!
                long mdsp = fieldDictOut.position();

                File[] postOutFiles = mergeState.partOut.getPostingsFiles();
                RandomAccessFile[] mPostRAF = new RandomAccessFile[postOutFiles.length];
                for(int i = 0; i < postOutFiles.length; i++) {
                    mPostRAF[i] = new RandomAccessFile(postOutFiles[i], "rw");
                }
                try {
                    if(logger.isLoggable(Level.FINER)) {
                        dw.start();
                    }

                    //
                    // The raw document vectors.
                    WriteableBuffer vlb = mergeState.partOut.getVectorLengthsBuffer();
                    mergeHeader.vectorLengthOffsets[Field.DocumentVectorType.RAW.ordinal()] = vlb.position();
                    fieldDictOut.position(mdp);
                    DiskDictionary<String> newMainDict =
                            new DiskDictionary<String>(new EntryFactory(exemplar.getTokenPostingsType()),
                            new StringNameHandler(),
                            fieldDictOut,
                            mPostRAF);
                    DocumentVectorLengths.calculate(mergeState.info,
                            mergeState.partOut.getMaxDocID(),
                            mergeState.partOut.getMaxDocID(),
                            mergeState.manager,
                            (DictionaryIterator<String>) newMainDict.iterator(),
                            vlb,
                            mergeState.manager.getTermStatsDict(), 
                            Field.TermStatsType.RAW);
                    
                    //
                    // The stemmed document vectors.
                    if(mergeState.info.hasAttribute(
                            FieldInfo.Attribute.STEMMED)) {
                        mdp = mergeHeader.dictionaryOffsets[DictionaryType.STEMMED_TOKENS.ordinal()];
                        if(mdp > 0) {
                            mergeHeader.vectorLengthOffsets[Field.DocumentVectorType.STEMMED.ordinal()] = vlb.position();
                            fieldDictOut.position(mdp);
                            DiskDictionary<String> newStemDict =
                                        new DiskDictionary<String>(new EntryFactory(exemplar.getTokenPostingsType()),
                                                               new StringNameHandler(), fieldDictOut, mPostRAF);
                            DocumentVectorLengths.calculate(mergeState.info,
                                                            mergeState.partOut.getMaxDocID(),
                                                            mergeState.partOut.getMaxDocID(),
                                                            mergeState.manager,
                                                            (DictionaryIterator<String>) newStemDict.iterator(),
                                                            vlb,
                                                            mergeState.manager.getTermStatsDict(),
                                                            Field.TermStatsType.STEMMED);
                        }
                    }
                    for(RandomAccessFile mprf : mPostRAF) {
                        mprf.close();
                    }
                    fieldDictOut.position(mdsp);
                    if(logger.isLoggable(Level.FINER)) {
                        dw.stop();
                        logger.fine(String.format("Calculating doc vec lens took %.2fms", dw.getTimeMillis()));
                        dw.reset();
                    }

                } catch(RuntimeException ex) {
                    logger.log(Level.SEVERE, String.format(
                            "Exception computing vectors for merged partition on field %s using %s", 
                            mergeState.info.getName(), 
                            mergeHeader.dictionaryOffsets[DictionaryType.UNCASED_TOKENS.ordinal()] >= 0 ? 
                            DictionaryType.UNCASED_TOKENS : DictionaryType.CASED_TOKENS));
                    throw (ex);
                }

            } else {
                Arrays.fill(mergeHeader.vectorLengthOffsets, -1);
            }
        }

        //
        // Now zip back and write the header.
        long endPos = fieldDictOut.position();
        fieldDictOut.position(headerPos);
        mergeHeader.write(fieldDictOut);
        fieldDictOut.position(endPos);
    }
    
    /**
     * Generates term statistics for the main token dictionaries in the given
     * bundles.
     * @param bundles the bundles to use when generating term statistics.
     * @param partOut the partition output where the term statistics should be
     * written.
     * @return <code>true</code> if any term statistics were written, <code>false</code>
     * otherwise.
     */
    public static void calculateTermStats(DiskDictionaryBundle[] bundles,
                                            TermStatsHeader termStatsHeader,
                                            DictionaryOutput termStatsDictOut) {
        
        if(bundles.length == 1) {
            DiskDictionary dict = bundles[0].getTermDictionary();
            FieldInfo info = bundles[0].info;
            if(dict != null) {
                long offset = termStatsDictOut.position();
                int nWritten = calculateSingleDictionaryTermStats(info, dict,
                                                                  termStatsDictOut);
                if(nWritten > 0) {
                    termStatsHeader.addOffset(info.getID(),
                                              Field.TermStatsType.RAW, offset);
                }
            }
            if(info.hasAttribute(FieldInfo.Attribute.STEMMED)) {
                long offset = termStatsDictOut.position();
                dict = bundles[0].getDictionary(DictionaryType.STEMMED_TOKENS);
                if(dict != null) {
                    int nWritten = calculateSingleDictionaryTermStats(info, dict,
                                                                      termStatsDictOut);
                    if(nWritten > 0) {
                        termStatsHeader.addOffset(info.getID(),
                                                  Field.TermStatsType.STEMMED,
                                                  offset);
                    }
                }
            }
        } else {
            
            DiskDictionary dicts[] = new DiskDictionary[bundles.length];
            FieldInfo info = null;
            for(int i = 0; i < bundles.length; i++) {
                if(bundles[i] == null) {
                    continue;
                }
                info = bundles[i].info;
                dicts[i] = bundles[i].getTermDictionary();
            }
            long offset = termStatsDictOut.position();
            int nWritten = calculateMultipleDictionaryTermStats(info, dicts,
                                                               termStatsDictOut);
            if(nWritten > 0) {
                termStatsHeader.addOffset(info.getID(),
                                          Field.TermStatsType.RAW, offset);
            }
            if(info.hasAttribute(FieldInfo.Attribute.STEMMED)) {
                for(int i = 0; i < bundles.length; i++) {
                    dicts[i] = null;
                    if(bundles[i] == null) {
                        continue;
                    }
                    info = bundles[i].info;
                    dicts[i] = bundles[i].getDictionary(DictionaryType.STEMMED_TOKENS);
                }
                offset = termStatsDictOut.position();
                nWritten = calculateMultipleDictionaryTermStats(info, dicts,
                                                                   termStatsDictOut);
                if(nWritten > 0) {
                    termStatsHeader.addOffset(info.getID(),
                                              Field.TermStatsType.STEMMED, offset);
                }
                
            }
        }
    }
    
    private static int calculateSingleDictionaryTermStats(FieldInfo info,
                                                   DiskDictionary dict,
                                                   DictionaryOutput termStatsDictOut) {
        //
        // Special case of a generating term stats from a single partition, 
        // we just need to iterate through
        // the entries and copy out the data, so no need for messing with the heap.
        DictionaryIterator di = (DictionaryIterator) dict.iterator();
        if(di == null) {
            return 0;
        }
        int nMerged = 0;
        int nWritten = 0;
        int tsid = 1;
        termStatsDictOut.start(null, new StringNameHandler(),
                               MemoryDictionary.Renumber.RENUMBER, false, 0);
        while(di.hasNext()) {
            QueryEntry qe = (QueryEntry) di.next();
            //
            // If we only ended up with 1 document, then forget about this one.
            if(qe.getN() > 1) {
                TermStatsIndexEntry tse = new TermStatsIndexEntry((String) qe.
                        getName(), tsid++);
                tse.getTermStats().add(qe);
                termStatsDictOut.write(tse);
                nWritten++;
            }
            nMerged++;
            if(nMerged % 50000 == 0 && logger.isLoggable(Level.FINER)) {
                logger.finer(String.format("%s generated term stats for %d",
                                           info.getName(), nMerged));
            }
        }

        if(nMerged % 50000 != 0 && logger.isLoggable(Level.FINER)) {
            logger.finer(String.format("%s generated term stats for %d", info.
                    getName(), nMerged));
        }
        termStatsDictOut.finish();
        return nWritten;
    }
    
    private static int calculateMultipleDictionaryTermStats(FieldInfo info,
                                                        DiskDictionary[] dicts,
                                                        DictionaryOutput termStatsDictOut) {
        
        //
        // We need to handle data from a number of partitions, which
        // we'll do with a heap.
        class HE implements Comparable<HE> {

            DictionaryIterator di;

            QueryEntry curr;

            public HE(DictionaryIterator di) {
                this.di = di;
            }

            public boolean next() {
                if(di.hasNext()) {
                    curr = (QueryEntry) di.next();
                    return true;
                }
                return false;
            }

            @Override
            public int compareTo(HE o) {
                return ((Comparable) curr.getName()).compareTo(o.curr.getName());
            }
        }

        PriorityQueue<HE> h = new PriorityQueue<HE>();
        for(DiskDictionary dict : dicts) {
            if(dict == null) {
                continue;
            }
            HE el = new HE((DictionaryIterator) dict.iterator());
            if(el.next()) {
                h.offer(el);
            }
        }

        if(h.isEmpty()) {
            return 0;
        }

        int nMerged = 0;
        int nWritten = 0;
        int tsid = 1;
        termStatsDictOut.start(null, new StringNameHandler(),
                               MemoryDictionary.Renumber.RENUMBER, false,
                               0);
        while(h.size() > 0) {
            HE top = h.peek(); 
            TermStatsIndexEntry tse = new TermStatsIndexEntry((String) top.curr.
                    getName(), tsid++);
            TermStatsImpl ts = tse.getTermStats();
            while(top != null && top.curr.getName().equals(tse.getName())) {
                top = h.poll();
                ts.add(top.curr);
                if(top.next()) {
                    h.offer(top);
                }
                top = h.peek();
            }
            if(tse.getN() > 1) {
                termStatsDictOut.write(tse);
                nWritten++;
            }
            nMerged++;
            if(nMerged % 100000 == 0 && logger.isLoggable(Level.FINER)) {
                logger.finer(String.format("%s generated term stats for %d",
                                           info.getName(), nMerged));
            }
        }
        if(nMerged % 100000 != 0 && logger.isLoggable(Level.FINER)) {
            logger.finer(String.format("%s generated term stats for %d", info.
                    getName(), nMerged));
        }
        termStatsDictOut.finish();
        return nWritten;
    }
                                                

    public void calculateVectorLengths(PartitionOutput partOut) throws java.io.IOException {
        
        Arrays.fill(header.vectorLengthOffsets, -1);
        WriteableBuffer vectorLengthsBuffer = partOut.getVectorLengthsBuffer();
        DiskPartition p = (DiskPartition) field.getPartition();
        DiskDictionary<String> termDict = (DiskDictionary<String>) field.getTermDictionary(Field.TermStatsType.RAW);
        
        //
        // Vector lengths for the raw terms, preferrably the uncased ones.
        if(termDict != null) {
            header.vectorLengthOffsets[Field.DocumentVectorType.RAW.ordinal()] = vectorLengthsBuffer.position();
            DocumentVectorLengths.calculate(field.getInfo(),
                    p.getNDocs(),
                    p.getMaxDocumentID(),
                    p.getPartitionManager(),
                    (DictionaryIterator<String>) termDict.iterator(),
                    vectorLengthsBuffer,
                    p.getPartitionManager().getTermStatsDict(), 
                    Field.TermStatsType.RAW);
        }
        
        //
        // Vector lengths for the stemmed terms.      
        termDict = (DiskDictionary<String>) field.getDictionary(DictionaryType.STEMMED_TOKENS);
        if(termDict != null) {
            header.vectorLengthOffsets[Field.DocumentVectorType.STEMMED.ordinal()] = vectorLengthsBuffer.position();
            DocumentVectorLengths.calculate(field.getInfo(),
                                            p.getNDocs(),
                                            p.getMaxDocumentID(),
                                            p.getPartitionManager(),
                                            (DictionaryIterator<String>) termDict.iterator(),
                                            vectorLengthsBuffer,
                                            p.getPartitionManager().getTermStatsDict(),
                                            Field.TermStatsType.STEMMED);

        }

        //
        // Re-write our header.
        dictFile.seek(headerPos);
        header.write(dictFile);
    }

    /**
     * Gets a fetcher for field values.
     * 
     * @return a saved field value fetcher.
     */
    public Fetcher getFetcher() {

        if(!field.isSaved()) {
            return null;
        }
        return new Fetcher(dicts[DictionaryType.RAW_SAVED.ordinal()], dtvOffsets, dtvData);
    }

    public DocumentVectorLengths getDocumentVectorLengths() {
        return getDocumentVectorLengths(Field.DocumentVectorType.RAW);
    }
    
    public DocumentVectorLengths getDocumentVectorLengths(Field.DocumentVectorType type) {
        return dvl[type.ordinal()];
    }

    public DiskDictionary getDictionary(DictionaryType type) {
        return dicts[type.ordinal()];
    }
    
    /**
     * Gets a list of local facets for this field.
     *
     * @param ag the array group from which we'll generate the facets.
     * @param facetSortSpec a sorting specification that is being used at the
     * top level for facets.  We need this because we want to keep track of the
     * "top" field values while building the local facets, when we can rely on
     * internal IDs for faster sorting.
     * @param n if this value is greater than 0, then only facets from the top <code>n</code> hits
     * will be considered.
     * @param resultSortSpec a sorting specification that can be used to determine 
     * the top <code>n</code> hits from the given group of hits.
     * @return a list of the facets for this partition.
     */
    
    public List<LocalFacet<N>> getTopFacets(ArrayGroup ag, SortSpec facetSortSpec, int n, SortSpec resultSortSpec) {
        Map<Integer, LocalFacet<N>> m = new HashMap<Integer, LocalFacet<N>>();
        Fetcher fetcher = getFetcher();
        int[] valIDs = new int[16];
        if(n > 0) {
            for(ResultImpl ri : ag.getTopDocuments(resultSortSpec, n, null)) {
                valIDs = processFacets(fetcher, m, valIDs, ri.getDocID(),
                              ri.getScore(), facetSortSpec);
            }
        } else {
            for(ArrayGroup.DocIterator di = ag.iterator(); di.next(); ) {
                valIDs = processFacets(fetcher, m, valIDs, di.getDoc(),
                              di.getScore(), facetSortSpec);
            }
        }
        List<LocalFacet<N>> ret = new ArrayList<LocalFacet<N>>(m.values());
        if(!ret.isEmpty()) {
            //
            // Sort the facets by ID and set the name values all at once, which
            // will let us stream through the dictionary.
            Collections.sort(ret);
            DiskDictionary<N>.LightDiskDictionaryIterator lit =
                    (DiskDictionary.LightDiskDictionaryIterator) fetcher.
                    literator();
            for(LocalFacet<N> facet : ret) {
                lit.simpleAdvance(facet.getValueID());
                facet.setValue(lit.getName());
                facet.setArrayGroup(ag);
            }
        }
        return ret;
    }

    /**
     * Processes the facets for a single document and score.
     * @param fetcher a fetcher for facet values
     * @param m a map from value IDs to facets.
     * @param facetSortSpec a sorting specification for the facets.
     * @param valIDs a holder for the value IDs fetched
     * @param doc the doc that we're interested in fetching facet values for
     * @param score  the score associated with the document
     */
    private int[] processFacets(Fetcher fetcher, Map<Integer, LocalFacet<N>> m,
                               int[] valIDs, int doc, float score, 
                               SortSpec facetSortSpec) {
        int[] ret = fetcher.fetch(doc, valIDs);
        for(int j = 1; j <= valIDs[0]; j++) {
            int valID = valIDs[j];
            LocalFacet facet = m.get(valID);
            if(facet == null) {
                facet = new LocalFacet((InvFileDiskPartition) field.
                        getPartition(), info, valID, facetSortSpec);
                m.put(valID, facet);
            }
            facet.addCount(1);
            facet.add(doc, score);
        }
        return ret;
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
        
        DiskDictionary<N> rawSaved;

        DiskDictionary.LookupState<N> lus;

        public Fetcher(DiskDictionary<N> rawSaved,
                ReadableBuffer dtvOffsets,
                ReadableBuffer dtvData) {
            this.rawSaved = rawSaved;
            lus = rawSaved.getLookupState();
            ldtvo = dtvOffsets.duplicate();
            ldtv = dtvData.duplicate();
        }
        
        public N get(int valID) {
            QueryEntry<N> e = rawSaved.get(valID, lus);
            if(e == null) {
                return null;
            }
            return e.getName();
        }

        /**
         * Fetches one value for the given document.
         * @param docID the ID of the document from which we want to get a value.
         * @return the value for the given document.  If no value is stored for
         * that document, then <code>null</code> is returned.
         */
        public N fetchOne(int docID) {
            ldtv.position(ldtvo.byteDecode(4 * (docID - 1), 4));
            int n = ldtv.byteDecode();
            if(n == 0) {
                return null;
            }
            return rawSaved.get(ldtv.byteDecode(), lus).getName();
        }
        
        /**
         * Fetches the IDs stored for the document.
         */
        public int[] fetch(int docID, int[] vals) {
            ldtv.position(ldtvo.byteDecode(4 * (docID - 1), 4));
            int n = ldtv.byteDecode();
            if(vals == null || vals.length < n+1) {
                vals = new int[n+1];
            }
            vals[0] = n;
            for(int i = 1; i <= n; i++) {
                vals[i] = ldtv.byteDecode();
            }
            return vals;
        }
        
        /**
         * Fetches the lowest ID stored for the document.
         */
        public int fetchLowID(int docID) {
            ldtv.position(ldtvo.byteDecode(4 * (docID - 1), 4));
            int n = ldtv.byteDecode();
            if(n == 0) {
                return 0;
            }
            int lowID = Integer.MAX_VALUE;
            for(int i = 1; i <= n; i++) {
                lowID = Math.min(lowID, ldtv.byteDecode());
            }
            return lowID;
        }

        /**
         * Fetches all of the values stored in the field for the given document.
         *
         * @param docID
         * @return
         */
        public List<N> fetch(int docID) {
            return fetch(docID, new ArrayList());
        }

        public List<N> fetch(int docID, List l) {
            ldtv.position(ldtvo.byteDecode(4 * (docID - 1), 4));
            int n = ldtv.byteDecode();
            for(int i = 0; i < n; i++) {
                l.add(rawSaved.get(ldtv.byteDecode(), lus).getName());
            }
            return l;
        }
        
        public LightIterator<N> literator() {
            return rawSaved.literator(lus);
        }
    }
}
