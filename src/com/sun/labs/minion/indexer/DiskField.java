package com.sun.labs.minion.indexer;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.dictionary.DateNameHandler;
import com.sun.labs.minion.indexer.dictionary.DiskBiGramDictionary;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.DoubleNameHandler;
import com.sun.labs.minion.indexer.dictionary.LongNameHandler;
import com.sun.labs.minion.indexer.dictionary.NameDecoder;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DocumentVectorLengths;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.util.CharUtils;
import com.sun.labs.minion.util.buffer.FileWriteableBuffer;
import com.sun.labs.minion.util.buffer.NIOFileReadableBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private DiskDictionary rawSaved;

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
     * @param matchCase whether we should lookup the term using the provided case.
     * If <code>true</code>, then an entry will only be returned if there is a
     * match for this term with the provided case.  If <code>false</code> then an
     * entry will be returned if there is a match for this term in lower case.
     * @return
     */
    public QueryEntry getTerm(String name, boolean matchCase) {
        if(matchCase) {
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

    public void normalize(int[] docs, float[] scores, int p, float qw) {
        dvl.normalize(docs, scores, p, qw);
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
