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
package com.sun.labs.minion.test.regression.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.PosPostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;

import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import java.util.logging.Logger;

/**
 * Selects entries in an index which will be used to create queries
 * for the regression test. The types of entries selected are:
 *
 * <ol>
 * <li>entrys with N number of postings</li>
 * <li>the first and last entry of each partition</li>
 * <li>the first entry of the first and last document</li>
 * <li>all the entries that appear in 5-10% of all documents</li>
 * <li>all the entries that appear in 45-55% of all documents</li>
 * <li>all the entries that appear in 95-100% of all documents</li>
 * </ol>
 */
public class TestEntrySelector {

    private SearchEngineImpl searchEngine;

    private PartitionManager partitionManager;

    private List<EntrySelector> selectors;

    /**
     * Creates a TestEntrySelector based on the given properties.
     * A SearchEngine has to be created before we can select the entrys,
     * so we need to specify the properties of the SearchEngine.
     *
     * @param indexDir the index directory
     */
    public TestEntrySelector(String indexDir, String cmFile)
            throws IOException, SearchEngineException {
        searchEngine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(
                cmFile, indexDir);
        partitionManager = searchEngine.getManager();
        selectors = new LinkedList<EntrySelector>();

        // selects entrys with N number of postings
        selectors.add(new NPostingsEntrySelector());

        // selects the first and last entry of each partition
        selectors.add(new PartitionEntrySelector());

        // selects the first entry of the first and last document
        selectors.add(new FirstLastDocEntrySelector());

        // creates a map from the name of entrys to the number of documents
        // the entry occurs in
        Map<String, EntryFreq> entryFreqMap = SelectorUtil.getEntryFreqMap(
                partitionManager);

        // selects all the entries that appear in 5-10% of all documents
        selectors.add(new FreqEntrySelector(0.05f, 0.1f, entryFreqMap));

        // selects all the entries that appear in 45-55% of all documents
        selectors.add(new FreqEntrySelector(0.45f, 0.55f, entryFreqMap));

        // selects all the entries that appear in 95-100% of all documents
        selectors.add(new FreqEntrySelector(0.95f, 1.0f, entryFreqMap));
    }

    /**
     * Returns the PartitionManager used by this TestEntrySelector.
     *
     * @return the PartitionManager used
     */
    public PartitionManager getPartitionManager() {
        return partitionManager;
    }

    /**
     * Selects entrys.
     *
     * @return a list of selected entrys
     */
    public List<Entry> select() {
        List<Entry> entrys = new LinkedList<Entry>();
        for(Iterator<EntrySelector> i = selectors.iterator(); i.hasNext();) {
            EntrySelector ts = i.next();
            List<Entry> list = ts.selectEntries(partitionManager);
            System.out.println(ts.toString());
            printEntries(list);
            System.out.println();
            entrys.addAll(list);
        }
        return entrys;
    }

    /**
     * Prints information about the underlying partitions.
     */
    private void printPartitionInfo() {
        System.out.println("Num. of Partitions: " +
                partitionManager.getNActive());
        Iterator i = partitionManager.getActivePartitions().iterator();
        int n = 0;
        while(i.hasNext()) {
            n++;
            DiskPartition p = (DiskPartition) i.next();
            System.out.println(" Partition " + n);
            System.out.println("  Dictionary: " +
                    p.getMainDictionaryIterator().getNEntries() +
                    " entries");
        }
    }

    /**
     * Prints a list of entries.
     *
     * @param entryList the list of entries to print
     */
    private void printEntries(List<Entry> entryList) {
        for(Iterator<Entry> i = entryList.iterator(); i.hasNext();) {
            Entry entry = i.next();
            System.out.println(SelectorUtil.getFirstToken(entry.toString()));
        }
    }

    /**
     * A test program that selects entries from an index.
     * <p>
     * <code>
     * <br>arg[0] : the index directory
     * </code>
     * </p>
     */
    public static void main(String[] argv) {
        String indexDirectory = argv[0];
        String cmFile = argv[1];

        try {
            TestEntrySelector selector = new TestEntrySelector(cmFile,
                    indexDirectory);
            selector.printPartitionInfo();
            System.out.println();
            selector.select();
        } catch(Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}

class SelectorUtil {

    /**
     * Returns the first and last entries of the given DictionaryIterator.
     *
     * @param di the DictionaryIterator
     *
     * @return a list containing the first and last entries
     */
    public static List<Entry> getFirstLastEntries(DictionaryIterator di) {
        List<Entry> entries = new LinkedList<Entry>();
        if(di.hasNext()) {
            entries.add((Entry) di.next()); // first entry
            if(di.hasNext()) {
                /* iterate to the last entry */
                Entry lastEntry = (Entry) di.next();
                while(di.hasNext()) {
                    lastEntry = (Entry) di.next();
                }
                entries.add(lastEntry);
            } else {
                System.out.println("Partition only has 1 entry.");
            }
        }
        return entries;
    }

    /**
     * Returns the first token of a string, which is all the characters
     * that appear before the first whitespace.
     *
     * @param entry the string to tokenize
     *
     * @return a string containing all the characters before the first
     *         whitespace
     */
    public static String getFirstToken(String entry) {
        int space = entry.indexOf(" ");
        if(space > -1) {
            return entry.substring(0, space);
        } else {
            return entry;
        }
    }

    /**
     * Returns a map that maps the name of entries to the total number of 
     * documents they occur in.
     *
     * @param manager the PartitionManager that contains the entries
     *                and their postings
     *
     * @return a map that maps the name of entries to the number of documents
     *         they occur in
     */
    public static Map<String, EntryFreq> getEntryFreqMap(
            PartitionManager manager) {
        Map<String, EntryFreq> map = new HashMap<String, EntryFreq>();
        Iterator i = manager.getActivePartitions().iterator();

        /* iterate through all partitions */
        while(i.hasNext()) {
            DiskPartition partition = (DiskPartition) i.next();
            DictionaryIterator di = partition.getMainDictionaryIterator();

            /* iterate through each entry in the partition */
            while(di.hasNext()) {
                Entry entry = (Entry) di.next();
                String key =
                        SelectorUtil.getFirstToken((String) entry.getName());
                EntryFreq tf = map.get(key);

                /* 
                 * map the string value of the entry to a EntryFreq object,
                 * which contains the entry itself and the number of documents
                 * the entry has occurred in so far
                 */
                if(tf == null) {
                    map.put(key, new EntryFreq(entry, new Integer(entry.getN())));
                } else {
                    tf.freq = new Integer(tf.freq.intValue() + entry.getN());
                }
            }
        }

        return map;
    }
}

class EntryFreq {

    Entry entry;

    Integer freq;

    EntryFreq(Entry t, Integer f) {
        entry = t;
        freq = f;
    }
}

/**
 * Selects the first and last entries of each partition.
 */
class PartitionEntrySelector implements EntrySelector {

    /**
     * Selects the first and last entries of each partition.
     *
     * @param manager the PartitionManager which contains the index
     *
     * @return a list containing the selected entries
     */
    public List<Entry> selectEntries(PartitionManager manager) {
        List<Entry> entries = new LinkedList<Entry>();
        Iterator i = manager.getActivePartitions().iterator();

        /* iterate through all the partitions */
        while(i.hasNext()) {
            DiskPartition partition = (DiskPartition) i.next();
            int numEntries = partition.getNEntries();
            if(numEntries > 0) {
                DictionaryIterator di = partition.getMainDictionaryIterator();
                entries.addAll(SelectorUtil.getFirstLastEntries(di));
            } else {
                System.out.println("Partition " + partition.getPartitionNumber() +
                        " has no entries.");
            }
        }
        return entries;
    }

    /**
     * Returns the name of this EntrySelector.
     *
     * @return the name of this EntrySelector
     */
    public String toString() {
        return "First and last entries of each partition";
    }
}

/**
 * Selects a set of entries that has a certain number of postings.
 * This implementation returns a set of entries with 1, 2, 9, 10, 11 and 20
 * postings.
 */
class NPostingsEntrySelector implements EntrySelector {

    /**
     * Selects entries that have exactly 1, 2, 9, 10, 11 and 20 IDPostings
     * from the index contained in the PartitionManager.
     *
     * @param manager the PartitionManager which contains the index
     *
     * @return a list of the selected entries
     */
    public List<Entry> selectEntries(PartitionManager manager) {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(selectEntry(1, manager));
        entries.add(selectEntry(2, manager));
        entries.add(selectEntry(9, manager));
        entries.add(selectEntry(10, manager));
        entries.add(selectEntry(11, manager));
        entries.add(selectEntry(20, manager));
        return entries;
    }

    /**
     * Returns the first entry in the index that contains the specified
     * number of IDPostings.
     *
     * @param n the number of IDPostings the return entry should have
     * @param manager the PartitionManager which contains the index
     *
     * @return the first entry with the specified number of IDPostings,
     *         or null if no entry has that number of IDPostings
     */
    public Entry selectEntry(int n, PartitionManager manager) {
        Iterator i = manager.getActivePartitions().iterator();

        /* iterate through all the partitions */
        while(i.hasNext()) {
            DiskPartition partition = (DiskPartition) i.next();
            DictionaryIterator di = partition.getMainDictionaryIterator();
            while(di.hasNext()) {
                Entry t = (Entry) di.next();
                if(t.getN() == n) {
                    return t;
                }
            }
        }
        return null;
    }

    /**
     * Returns the name of this EntrySelector.
     *
     * @return the name of this EntrySelector
     */
    public String toString() {
        return "Entries with N postings";
    }
}

/**
 * Selects the first entry in the first field of the first and last document
 * of each document dictionary.
 */
class FirstLastDocEntrySelector implements EntrySelector {

    Logger logger = Logger.getLogger(getClass().getName());

    protected static String logTag = "FLDES";

    /**
     * Selects the first entry in the first and last document of each
     * document dictionary.
     *
     * @param manager the PartitionManager which contains the index
     *
     * @return the first entry in the first and last document of each
     *         document dictionary
     */
    public List<Entry> selectEntries(PartitionManager manager) {
        List<Entry> entries = new LinkedList<Entry>();
        Iterator i = manager.getActivePartitions().iterator();

        /* iterate through all the partitions */
        while(i.hasNext()) {
            DiskPartition partition = (DiskPartition) i.next();
            Iterator di = partition.getDocumentIterator();
            if(di.hasNext()) {

                /* get the first document in the partition */
                DocKeyEntry firstDocEntry = (DocKeyEntry) di.next();

                /* get the first entry in the first document of the partition */
                entries.add(getFirstEntry(firstDocEntry, partition));

                /* get the last document in the partition */
                DocKeyEntry lastDocEntry = null;
                if(di.hasNext()) {
                    lastDocEntry = (DocKeyEntry) di.next();
                    while(di.hasNext()) {
                        lastDocEntry = (DocKeyEntry) di.next();
                    }
                }

                /* get the first entry in the last document of the partition */
                if(lastDocEntry != null) {
                    Entry e = getFirstEntry(lastDocEntry, partition);
                    if(e != null) {
                        entries.add(e);
                    }
                }
            }
        }
        return entries;
    }

    /**
     * Returns the first entry of the given document.
     *
     * @param docEntry the document
     *
     * @return the first entry of the given document
     */
    public Entry getFirstEntry(DocKeyEntry docEntry, DiskPartition partition) {
        PostingsIterator pi =
                docEntry.iterator(new PostingsIteratorFeatures());


        PostingsIteratorFeatures features = new PostingsIteratorFeatures();
        features.setPositions(true);
        features.setCaseSensitive(false);

        if(!pi.next()) {
            System.out.println("Postings iterator has nothing");
        }

        /* iterate through all the postings */
        while(pi.next()) {

            /* get the entry with the given ID */
            QueryEntry entry = partition.getTerm(pi.getID());
            PosPostingsIterator ppi =
                    (PosPostingsIterator) entry.iterator(features);

            //
            // We may hit an entry with no postings.  We'll just continue in such a case.
            if(ppi == null) {
                continue;
            }

            /* find the document ID in the list of postings */
            ppi.findID(docEntry.getID());

            int[][] positions = ppi.getPositions();

            for(int i = 0; i < positions.length; i++) {
                int[] field = positions[i];
                for(int j = 1; j < field.length; j++) {
                    if(field[j] == 1) {
                        // found the first entry in the document, return it
                        return entry;
                    }
                }
            }
        }

        // throw an error since we haven't found the first entry
        throw new Error("First entry not found: " + docEntry.getName());
    }

    /**
     * Returns a description of the selected entry
     *
     * @return a description of the selected entry
     */
    public String toString() {
        return "First entry of the first and last document";
    }
}

/**
 * Selects all the entries that appear in a certain frequency range 
 * of the all the documents.
 */
class FreqEntrySelector implements EntrySelector {

    private float lowerFreqBound;  // the lower bound for document frequency %

    private float upperFreqBound;  // the upper bound for document frequency %

    private Map<String, EntryFreq> entryFreqMap; // maps entry names to the # of docs they occur in

    /**
     * Constructs a FreqEntrySelector with the specified lower and upper
     * bound percentiles.
     *
     * @param lowerBound the lower bound for the percentage of documents
     *                   the entry appears in, a value between 0.0 and 1.0
     * @param upperBound the upper bound for the percentage of documents
     *                   the entry appears in, a value between 0.0 and 1.0
     */
    public FreqEntrySelector(float lowerBound, float upperBound,
            Map<String, EntryFreq> entryFreqMap) {
        this.lowerFreqBound = lowerBound;
        this.upperFreqBound = upperBound;
        this.entryFreqMap = entryFreqMap;
    }

    /**
     * Returns all the entries that appear the specified frequency range
     * of all the documents.
     *
     * @param manager the PartitionManager which contains the index
     *
     * @return a list containing the selected entries
     */
    public List<Entry> selectEntries(PartitionManager manager) {
        List<Entry> entries = new LinkedList<Entry>();

        int minDocs = (int) (manager.getNDocs() * lowerFreqBound);
        int maxDocs = (int) (manager.getNDocs() * upperFreqBound);

        Iterator<EntryFreq> i = entryFreqMap.values().iterator();

        while(i.hasNext()) {
            EntryFreq tf = i.next();
            if(minDocs <= tf.freq.intValue() &&
                    tf.freq.intValue() <= maxDocs) {
                entries.add(tf.entry);
            }
        }

        return entries;
    }

    /**
     * Returns a description of this EntrySelector
     *
     * @return a description of this EntrySelector
     */
    public String toString() {
        return "Entries that appear in " + ((int) (lowerFreqBound * 100)) + "-" +
                ((int) (upperFreqBound * 100)) + "% of all the documents";
    }
}

