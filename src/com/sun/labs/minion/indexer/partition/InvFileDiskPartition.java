/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */
package com.sun.labs.minion.indexer.partition;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.Map;
import java.util.Set;
import com.sun.labs.minion.engine.DocumentImpl;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.dictionary.DiskBiGramDictionary;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.DictionaryFactory;
import com.sun.labs.minion.indexer.dictionary.DiskFieldStore;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
import com.sun.labs.minion.lextax.DiskTaxonomy;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A disk partition that holds data that is specific to the
 * implementation of an inverted file.  It extends the disk
 * partition to add bigrams and a field store to the main
 * and document dictionaries already present in the superclass.
 *
 */
public class InvFileDiskPartition extends DiskPartition {

    /**
     * A factory for the dictionaries that the field store will use for saved
     * field values.
     */
    protected DictionaryFactory fieldStoreDictFactory;

    /**
     * A factory for bigram dictionaries that will be used by the main dictioanry
     * and by the field store.
     */
    protected DictionaryFactory bigramDictFactory;

    /**
     * Bigrams from the main dictionary.
     */
    protected DiskBiGramDictionary bigramDict;

    /**
     * A disk taxonomy, if one exists.
     */
    protected DiskTaxonomy taxonomy;

    /**
     * The offset of the bigrams in the main dictionary.
     */
    protected long bigramDictOffset;

    /**
     * The field store.
     */
    protected DiskFieldStore fields;

    /**
     * The ngram dictionary.
     */
    protected DiskDictionary ngrams;

    /**
     * The stream for the bigram dictionaries.
     */
    protected RandomAccessFile bigramDictFile;

    /**
     * The stream for the bigram postings.
     */
    protected RandomAccessFile bigramPostFile;

    /**
     * The stream for the field store dictionaries.
     */
    protected RandomAccessFile fieldDictFile;

    /**
     * The stream for the field store postings.
     */
    protected RandomAccessFile fieldPostFile;

    protected static String logTag = "IFDP";

    /**
     * Opens a partition with a given number
     *
     * @param partNumber the number of this partition.
     * @param manager the manager for this partition.
     * @param mainDictFactory a factory that will be used to generate the main
     * dictionary for this partition
     * @param documentDictFactory a factory that will be used to generate the document
     * dictionary for this partition
     * @param fieldStoreDictFactory a factory that will be used to generate the
     * dictionaries in the field store
     * @param bigramDictFactory a factory that will be used to generate the
     * bigram dictionaries needed for this partition
     * @throws java.io.IOException If there is an error opening or reading
     * any of the files making up a partition.
     *
     * @see com.sun.labs.minion.indexer.partition.Partition
     * @see com.sun.labs.minion.indexer.dictionary.Dictionary
     *
     */
    public InvFileDiskPartition(int partNumber, PartitionManager manager,
            DictionaryFactory mainDictFactory,
            DictionaryFactory documentDictFactory,
            DictionaryFactory fieldStoreDictFactory,
            DictionaryFactory bigramDictFactory,
            boolean cacheVectorLengths,
            int termCacheSize) throws java.io.IOException {
        super(partNumber, manager, mainDictFactory, documentDictFactory,
                cacheVectorLengths, termCacheSize);
        this.fieldStoreDictFactory = fieldStoreDictFactory;
        this.bigramDictFactory = bigramDictFactory;
    }

    /**
     * Initializes everything all at once.
     * @throws java.io.IOException if there was an error reading the files
     */
    protected synchronized void initAll() throws java.io.IOException {
        super.initAll();
        initBigramDict();
        initFields();
    }

    /**
     * Initializes the bigram dictionary, if necessary.
     */
    protected synchronized void initBigramDict() {
        if(bigramDict != null) {
            return;
        }

        try {
            File[] files = getBigramFiles();
            bigramDictFile = new RandomAccessFile(files[0], "r");
            bigramPostFile = new RandomAccessFile(files[1], "r");

            logger.fine(partNumber + " Loading main bigram dictionary");
            bigramDict = bigramDictFactory.getBiGramDictionary(mainDict,
                    bigramDictFile, bigramPostFile, this);
        } catch(java.io.IOException ioe) {
            logger.severe("Error opening bigram dictionary for " + partNumber);
            bigramDict = null;
        }
    }

    /**
     * Initializes the field store, if necessary.
     */
    protected synchronized void initFields() {

        if(fields != null) {
            return;
        }

        try {
            //
            // Get channels for the saved field data.
            File[] files = getFieldFiles();
            fieldDictFile = new RandomAccessFile(files[0], "r");
            fieldPostFile = new RandomAccessFile(files[1], "r");

            logger.fine(partNumber + " Loading field store");
            fields =
                    new DiskFieldStore(this, fieldDictFile,
                    new RandomAccessFile[]{fieldPostFile}, fieldStoreDictFactory,
                    bigramDictFactory, manager.metaFile);
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error opening field store for " +
                    partNumber, ioe);
            fields = null;
        }
    }

    /**
     * Initialise the taxonomy, should one be necessary.
     * A taxonomy is initialised if the manager's indexConfig
     * responds that one is necessary.
     */
    protected synchronized void initTaxonomy() {
        if(manager.getIndexConfig().taxonomyEnabled()) {
            if(taxonomy != null) {
                return;
            }

            File taxFile = manager.makeTaxonomyFile(partNumber);
            if(!taxFile.exists()) {
                return;
            }

            initMainDict();
            try {
                taxonomy = new DiskTaxonomy(taxFile, this);
            } catch(IOException ioe) {
                logger.log(Level.SEVERE, "Error opening taxonomy for " +
                        partNumber, ioe);
                taxonomy = null;
            }
        }
    }

    /**
     * Gets some or all of the data saved in a given field, in a given
     * document.
     *
     * @param name The name of the field.
     * @param key The document key of the document for which we want data.
     * @param all If <code>true</code>, all field values will be returned
     * as a list.  If <code>false</code> only the first value will be returned.
     * @return A list of the values saved in the given field in the given
     * document, or <code>null</code> if the given key is not in this
     * partition.
     */
    public Object getSavedFieldData(String name, String key, boolean all) {
        Entry e = getDocumentTerm(key);
        if(e == null) {
            return null;
        }

        return getSavedFieldData(name, e.getID(), all);
    }

    /**
     * Gets some or all of the data saved in a given field.
     *
     * @param name The name of the field.
     * @param docID The document ID for which we want the saved data.
     * @param all If <code>true</code>, return all known values for the
     * field in the given document.  If <code>false</code> return only one
     * value.
     * @return If <code>all</code> is true, then return a <code>List</code>
     * of field values, otherwise, return a single field value of the
     * appropriate type.  If <code>all</code> is <code>false</code>, a single
     * value of the appropriate type will be returned.
     *
     * <P>
     *
     * If the given name is not the name of a saved field, or the document
     * ID is invalid, then if <code>all</code> is true, an empty list will
     * be returned.  If <code>all</code> is <code>false</code>,
     * <code>null</code> will be returned.
     */
    public Object getSavedFieldData(String name, int docID, boolean all) {
        initFields();
        return fields.getSavedFieldData(name, docID, all);
    }

    /**
     * Gets all of the data saved in a given field.
     *
     * @param name The name of the field.
     * @param docID The document ID for which we want the saved data.
     * @return a <code>List</code> of field values of the appropriate type.
     * If the given name is not the name of a saved field, or the document
     * ID is invalid, then an empty list is returned.
     */
    public List getSavedFieldData(String name, int docID) {
        return (List) getSavedFieldData(name, docID, true);
    }

    /**
     * Gets all of the the data saved in a given field, in a given
     * document.
     *
     * @param name The name of the field.
     * @param key The document key of the document for which we want data.
     * @return The field values for the given document, as a <code>List</code>
     *
     * <P>
     *
     * If the given name is not the name of a saved field, or the document
     * ID is invalid, then an empty list will be returned.
     */
    public List getSavedFieldData(String name, String key) {
        return (List) getSavedFieldData(name, key, true);
    }

    public Object getSavedFieldData(FieldInfo fi, int docID, boolean all) {
        initFields();
        return fields.getSavedFieldData(fi, docID, all);
    }

    /**
     * Gets an iterator for all the saved fields in a document.
     */
    public Map<String, List> getSavedFields(int docID) {
        initFields();
        return fields.getSavedFields(docID);
    }

    /**
     * Gets an iterator for all of the values in a field.
     *
     * @param name The name of the field we need an iterator for.
     * @return An iterator for the values in the field.
     */
    public DictionaryIterator getFieldIterator(String name) {
        return getFieldIterator(name, true, null, true, null, true);
    }

    /**
     * Gets an iterator for the values in a given range in a field.
     *
     * @param name The name of the field we need an iterator for.
     * @param caseSensitive If true, case should be taken into account when
     * iterating through the values.  This value will only be observed for
     * character fields!
     * @param lowerBound The lower bound on the iterator.  If
     * <code>null</code>, only the upper bound is considered and the
     * iteration will commence with the first term in the dictionary.
     * @param includeLower If <code>true</code>, then the lower bound will
     * be included in the entries returned by the iterator, if it occurs in
     * the dictionary.
     * @param upperBound The upper bound on the iterator.  If
     * <code>null</code>, only the lower bound is considered and the
     * iteration will end at the last term in the dictionary.
     * @param includeUpper If <code>true</code>, then the upper bound will
     * be included in the entries returned by the iterator, if it occurs in
     * the dictionary.
     * @return An iterator for the dictionary entries contained in the
     * range, or <code>null</code> if there is no such range or the named
     * field is not a saved field.
     */
    public DictionaryIterator getFieldIterator(String name,
            boolean caseSensitive, Object lowerBound, boolean includeLower,
            Object upperBound, boolean includeUpper) {
        initFields();
        return fields.getFieldIterator(name, caseSensitive, lowerBound,
                includeLower, upperBound, includeUpper);
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
     */
    public DictionaryIterator getMatchingIterator(String name, String val,
            boolean caseSensitive) {
        initFields();
        return fields.getMatchingIterator(name, val, caseSensitive, -1, -1);
    }

    /**
     * Gets an iterator for the character saved field values that contain a
     * given substring.
     *
     * @param name The name of the field whose values we wish to match
     * against.
     * @param val The wildcard value against which we will match.
     * @param caseSensitive If <code>true</code>, then case will be taken
     * into account during the match.
     */
    public DictionaryIterator getSubstringIterator(String name, String val,
            boolean caseSensitive, boolean starts, boolean ends) {
        initFields();
        return fields.getSubstringIterator(name, val, caseSensitive, starts,
                ends, -1, -1);
    }

    /**
     * Gets the postings associated with a particular field value.
     *
     * @param name The name of the field for which we want postings.
     * @param value The value from the field for which we want postings.
     * @param caseSensitive If true, case should be taken into account when
     * iterating through the values.  This value will only be observed for
     * character fields!
     * @return The postings associated with that value, or
     * <code>null</code> if there is no such value in the field.
     */
    public PostingsIterator getFieldPostings(String name, Object value,
            boolean caseSensitive) {
        initFields();
        return fields.getFieldPostings(name, value, caseSensitive);
    }

    /**
     * Gets the number of defined fields.
     */
    public int getFieldCount() {
        return manager.metaFile.size();
    }

    /**
     * Gets the field store associated with this partition.
     */
    public DiskFieldStore getFieldStore() {
        initFields();
        return fields;
    }

    public int getFieldSize(String name) {
        initFields();
        com.sun.labs.minion.indexer.dictionary.SavedField sf = fields.
                getSavedField(name);
        if(sf != null) {
            return sf.size();
        }
        return 0;
    }

    /**
     * Gets the entries subsumed by a given name.
     *
     * @param name the name for which we want subsumed entries.
     * @return the subsumed entries, or <code>null</code> if this name is not
     * in the main dictionary.
     */
    public Set getSubsumed(String name) {
        initMainDict();
        if(getMainDictionary().get(name) == null) {
            return null;
        }
        initTaxonomy();
        if(taxonomy == null) {
            return null;
        }

        return taxonomy.getSubsumed(name, 500);
    }

    /**
     * Computes the euclidean distance between the given document and all
     * documents.  The distance is based on the features stored in the saved field
     * with the given name.
     */
    public double[] euclideanDistance(double[] vec, String field) {
        initFields();
        return fields.euclideanDistance(vec, field);
    }

    protected void mergeCustom(int newPartNumber, DiskPartition[] sortedParts,
            int[][] idMaps, int newMaxDocID, int[] docIDStart, int[] nUndel,
            int[][] docIDMaps) throws Exception {

        //
        // Merge the bigram dictionaries for the main dictionary.
        logger.fine("Merging main bigram dictionaries");
        DiskBiGramDictionary[] bgds =
                new DiskBiGramDictionary[sortedParts.length];
        for(int i = 0; i < sortedParts.length; i++) {
            InvFileDiskPartition ifdp = (InvFileDiskPartition) sortedParts[i];
            ifdp.initBigramDict();
            bgds[i] = ifdp.bigramDict;
        }
        //
        // Get the files that are used for the bigram dict.
        File[] files = InvFilePartitionUtils.getBigramFiles(manager,
                newPartNumber);

        //
        // Get a channel for the bigram dictionaries.
        RandomAccessFile mDictFile = new RandomAccessFile(files[0], "rw");

        //
        // Get channels for the postings.
        OutputStream mPostStream = new BufferedOutputStream(
                new FileOutputStream(files[1]), 8192);
        PostingsOutput mPostOut = new StreamPostingsOutput(mPostStream);

        bgds[0].merge(bgds, docIDStart, idMaps, mDictFile, mPostOut);

        mDictFile.close();
        mPostStream.close();

        //
        // Merge the field store
        //
        // Get channels for the saved field data.
        files = InvFilePartitionUtils.getFieldFiles(manager, newPartNumber);
        RandomAccessFile fieldDictFile = new RandomAccessFile(files[0], "rw");
        BufferedOutputStream fieldPostStream = new BufferedOutputStream(
                new FileOutputStream(files[1]));
        DiskFieldStore[] stores = new DiskFieldStore[sortedParts.length];
        for(int i = 0; i < stores.length; i++) {
            ((InvFileDiskPartition) sortedParts[i]).initFields();
            stores[i] = ((InvFileDiskPartition) sortedParts[i]).fields;
        }

        //
        // Perform the merge
        stores[0].merge(stores, newMaxDocID, docIDStart, nUndel, docIDMaps,
                fieldDictFile, new StreamPostingsOutput(fieldPostStream));

        fieldDictFile.close();
        fieldPostStream.close();

        //
        // Merge the taxonomies, if they exist.
        initTaxonomy();
        if(taxonomy != null) {
            logger.fine("Merge taxonomies");
            DiskTaxonomy[] taxes = new DiskTaxonomy[sortedParts.length];
            for(int i = 0; i < taxes.length; i++) {
                ((InvFileDiskPartition) sortedParts[i]).initTaxonomy();
                taxes[i] = ((InvFileDiskPartition) sortedParts[i]).taxonomy;
            }

            taxes[0].merge(taxes, manager.indexDir, manager.makeTaxonomyFile(
                    newPartNumber));
        }
    }

    /**
     * Close the files associated with this partition.
     */
    public synchronized boolean close(long currTime) {
        if(!super.close(currTime)) {
            return false;
        }
        try {
            if(fields != null) {
                fieldDictFile.close();
                fieldPostFile.close();
            }

            if(bigramDict != null) {
                bigramDictFile.close();
                bigramPostFile.close();
            }

            if(taxonomy != null) {
                taxonomy.close();
            }
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error closing partition", ioe);
        }
        return true;
    }

    /**
     * Gets the entries matching the given pattern
     *
     * @param pat The pattern to match entries against.
     * @param caseSensitive If <code>true</code>, then do the lookup in a
     * case sensitive fashion.
     * @param maxEntries The maximum number of entries to return.  If zero or
     * negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     * @return An array of entries containing the matching entries, or null
     * if there are not such entries, or an array of length zero if the
     * operation timed out before any entries could be matched
     */
    public QueryEntry[] getMatching(String pat, boolean caseSensitive,
            int maxEntries, long timeLimit) {
        initMainDict();
        initBigramDict();
        return getMainDictionary().getMatching(bigramDict, pat, caseSensitive,
                maxEntries, timeLimit);
    }

    /**
     * Gets the spelling variants of a term
     *
     * @param pat The pattern to match entries against.
     * @param caseSensitive If <code>true</code>, then do the lookup in a
     * case sensitive fashion.
     * @param maxEntries The maximum number of entries to return.  If zero or
     * negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     * @return An array of entries containing the spelling variants, or null
     * if there are not such entries, or an array of length zero if the
     * operation timed out before any entries could be matched
     */
    public QueryEntry[] getSpellingVariants(String pat, boolean caseSensitive,
            int maxEntries, long timeLimit) {
        initMainDict();
        initBigramDict();
        return getMainDictionary().getSpellingVariants(bigramDict, pat,
                caseSensitive, maxEntries, timeLimit);
    }

    /**
     * Gets the entries containing the given substring.
     *
     * @param pat The pattern to match entries against.
     * @param caseSensitive If <code>true</code>, then do the lookup in a
     * case sensitive fashion.
     * @param maxEntries The maximum number of entries to return.  If zero or
     * negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     * @return An array of <code>Term</code> objects containing the
     * matching entries, or null if there are not such entries, or an
     * array of length zero if the operation timed out before any
     * entries could be matched
     */
    public QueryEntry[] getSubstring(String pat, boolean caseSensitive,
            int maxEntries, long timeLimit) {
        initMainDict();
        initBigramDict();
        return getMainDictionary().getSubstring(bigramDict, pat, caseSensitive,
                false, false, maxEntries, timeLimit);
    }

    /**
     * Gets the entries that match the stem of the given term.  Uses the
     * default minimum length and match cutoff values.
     *
     * @param term The term we want to get variants of.
     * @param caseSensitive If <code>true</code>, then do the lookup in a
     * case sensitive fashion.
     * @param maxEntries The maximum number of entries to return.  If zero or
     * negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     * @return An array of <code>Term</code> objects containing the
     * matching entries, or null if there are not such entries.
     */
    public QueryEntry[] getStemMatches(String term, boolean caseSensitive,
            int maxEntries, long timeLimit) {
        initMainDict();
        initBigramDict();
        return getStemMatches(term, caseSensitive, MIN_LEN, MATCH_CUT_OFF,
                maxEntries, -1);
    }

    /**
     * Gets the entries that match the stem of the given term.
     *
     * @param term The term we want to get variants of.
     * @param caseSensitive If <code>true</code>, then do the lookup in a
     * case sensitive fashion.
     * @param minLen The minimum term length for stemming.
     * @param matchCutOff The cutoff score for matching variants and the
     * original term.
     * @param maxEntries The maximum number of entries to return.  If zero or
     * negative, return all possible entries.
     * @param timeLimit The maximum amount of time (in milliseconds) to
     * spend trying to find matches.  If zero or negative, no time limit is
     * imposed.
     * @return An array of <code>Term</code> objects containing the
     * matching entries, or null if there are not such entries.
     */
    public QueryEntry[] getStemMatches(String term, boolean caseSensitive,
            int minLen, float matchCutOff, int maxEntries, long timeLimit) {

        return getMainDictionary().getStemMatches(bigramDict, term,
                caseSensitive, minLen, matchCutOff, maxEntries, timeLimit);
    }

    /**
     * Gets the files associated with the field store for a partition.
     *
     * @return an array of files.  The first is for the dictionary, and the
     * remaining are for the postings files.
     */
    protected File[] getFieldFiles() {
        return InvFilePartitionUtils.getFieldFiles(manager, partNumber);
    }

    /**
     * Gets all the files associated with a partition, including those specific to
     * the inverted file.
     *
     * @return an array of files
     */
    protected File[] getAllFiles() {
        return InvFilePartitionUtils.getAllFiles(manager, partNumber);
    }

    /**
     * Gets all the files associated with a partition, including those specific to
     * the inverted file.
     *
     * @return an array of files
     */
    protected static File[] getAllFiles(PartitionManager manager, int partNumber) {
        return InvFilePartitionUtils.getAllFiles(manager, partNumber);
    }

    /**
     * Reaps the given partition.  If the postings file cannot be removed,
     * then we return control immediately.
     *
     * @param m The manager associated with the partition.
     * @param n The partition number to reap.
     */
    protected static void reap(PartitionManager m, int n) {
        //
        // Remove the data files.
        Logger sl = Logger.getLogger(InvFileDiskPartition.class.getName());
        File[] files = getAllFiles(m, n);
        for(int i = 0; i < files.length; i++) {
            if(!files[i].delete() && (files[i].exists())) {
                sl.warning("Failed to delete: " + files[i]);
            }
        }

        //
        // Remove the deletion bitmap and the removed partition files.
        if(m.makeDeletedDocsFile(n).exists() &&
                !m.makeDeletedDocsFile(n).delete()) {
            sl.severe("Failed to reap partition " + n + " deleted docs");
        }
        if(!m.makeRemovedPartitionFile(n).delete()) {
            sl.severe("Failed to reap partition " + n + " rem file");
        }
    }

    /**
     * Gets the files associated with the bigram postings for a partition.
     *
     * @return an array of files.  The first is for the dictionary, and the
     * remaining are for the postings files.
     */
    protected File[] getBigramFiles() {
        return InvFilePartitionUtils.getBigramFiles(manager, partNumber);
    }

    /**
     * @return Returns the taxonomy.
     */
    public DiskTaxonomy getTaxonomy() {
        initTaxonomy();
        return taxonomy;
    }

    /**
     * Exports the data in this partition to an XML file format.
     *
     * @param o the writer to which the data will be output.
     */
    public void export(PrintWriter o) {
        initDocDict();
        initFields();
        for(DictionaryIterator di = docDict.iterator(); di.hasNext();) {
            DocKeyEntry dke = (DocKeyEntry) di.next();
            if(isDeleted(dke.getID())) {
                continue;
            }

            DocumentImpl d = new DocumentImpl(manager.engine, this, dke.getID());
            d.export(o);
        }
    }
}
