package com.sun.labs.minion.indexer;

import com.sun.labs.minion.indexer.dictionary.DiskDictionaryBundle;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.MemoryDictionaryBundle;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.DocumentVectorLengths;
import com.sun.labs.minion.indexer.partition.MergeState;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.retrieval.ArrayGroup;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.logging.Logger;

/**
 * A field on-disk
 */
public class DiskField extends Field {

    static final Logger logger = Logger.getLogger(DiskField.class.getName());

    private DiskDictionaryBundle bundle;

    public DiskField(DiskPartition partition,
            FieldInfo info,
            RandomAccessFile dictFile,
            RandomAccessFile vectorLengthsFile,
            RandomAccessFile[] postIn,
            EntryFactory entryFactory) throws java.io.IOException {
        super(partition, info, entryFactory);
        bundle = new DiskDictionaryBundle(this, dictFile, vectorLengthsFile,
                postIn);
    }
    
    public String[] getPostingsChannelNames() {
        return bundle.getPostingsChannelNames();
    }

    @Override
    public int getMaximumDocumentID() {
        return bundle.getHeader().maxDocID;
    }

    public DiskDictionary getTermDictionary(boolean cased) {
        return bundle.getTermDictionary(cased);
    }
    
    public DiskDictionary getDictionary(MemoryDictionaryBundle.Type type) {
        return bundle.getDictionary(type);
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
        return bundle.getTerm(name, caseSensitive);
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
        return bundle.getTerm(id, caseSensitive);
    }

    public List<QueryEntry> getWildcardMatches(String name, boolean caseSensitive,
            int maxEntries,
            long timeLimit) {
        return bundle.getWildcardMatches(name, caseSensitive, maxEntries,
                timeLimit);
    }

    public TermStatsImpl getTermStats(String name) {
        return partition.getPartitionManager().getTermStats(name, info);
    }

    public QueryEntry getStem(String stem) {
        return bundle.getStem(stem);
    }

    public QueryEntry getVector(String key) {
        return bundle.getVector(key);
    }

    /**
     * Gets the entry in the dictionary associated with a given value.
     * @param val the value to get, as a string
     * @param caseSensitive whether to do a case sensitive lookup
     * @return
     */
    public QueryEntry getSaved(String val, boolean caseSensitive) {
        return bundle.getSaved(val, caseSensitive);
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
        return bundle.getSavedValuesIterator(
                caseSensitive,
                lowerBound,
                includeLower,
                upperBound,
                includeUpper);
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
        return bundle.getMatchingIterator(
                val, caseSensitive,
                maxEntries,
                timeLimit);

    }

    public List<QueryEntry> getMatching(String pattern, boolean caseSensitive,
            int maxEntries, long timeLimit) {
        return bundle.getMatching(pattern,
                caseSensitive,
                maxEntries,
                timeLimit);

    }
    
    public DiskDictionary getSavedValuesDictionary() {
        return bundle.getSavedValuesDictionary();
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
        return bundle.getSubstringIterator(
                val,
                caseSensitive,
                starts,
                ends,
                maxEntries,
                timeLimit);
    }

    /**
     * Gets the values of a string field that are similar to the given field.
     *
     * @param value
     * @param caseSensitive
     * @return
     */
    public ArrayGroup getSimilar(String value, boolean caseSensitive) {
        return bundle.getSimilar(value, caseSensitive);
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
        return bundle.getUndefined(ag);
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
        return bundle.getDocumentVectorLength(docID);
    }

    public DocumentVectorLengths getDocumentVectorLengths() {
        return bundle.getDocumentVectorLengths();
    }

    /**
     * Normalizes the scores for a number of documents all at once, which will
     * be more efficient than doing them one-by-one
     * @param docs the IDs of the documents
     * @param scores the scores to normalize
     * @param n the number of actual documents and scores
     * @param qw any query weight to apply when normalizing
     */
    public void normalize(int[] docs, float[] scores, int n, float qw) {
        bundle.normalize(docs, scores, n, qw);
    }

    public static void merge(MergeState mergeState, DiskField[] fields)
            throws java.io.IOException {

        DiskDictionaryBundle[] bundles = new DiskDictionaryBundle[fields.length];
        for(int i = 0; i < fields.length; i++) {
            //
            // We can encounter a partition that has no instances of a field, 
            // but we need to account for that!
            if(fields[i] == null) {
                bundles[i] = null;
            } else {
                bundles[i] = fields[i].bundle;
            }
        }
        DiskDictionaryBundle.merge(mergeState, bundles);
    }

    public static void regenerateTermStats(DiskField[] fields,
            PartitionOutput partOut) {

        DiskDictionaryBundle[] bundles = new DiskDictionaryBundle[fields.length];
        boolean found = false;
        for(int i = 0; i < fields.length; i++) {
            //
            // We can encounter a partition that has no instances of a field, 
            // but we need to account for that!
            if(fields[i] == null) {
                bundles[i] = null;
            } else {
                bundles[i] = fields[i].bundle;
                found = true;
            }
        }
        if(found) {
            DiskDictionaryBundle.regenerateTermStats(bundles, partOut);
        }
    }

    public void calculateVectorLengths(PartitionOutput partOut) throws java.io.IOException {
        bundle.calculateVectorLengths(partOut);
    }

    /**
     * Gets a fetcher for field values.
     * 
     * @return a saved field value fetcher.
     */
    public DiskDictionaryBundle.Fetcher getFetcher() {
        return bundle.getFetcher();
    }
}
