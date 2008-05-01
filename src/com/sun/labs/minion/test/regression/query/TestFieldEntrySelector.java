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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.dictionary.DictionaryIterator;
import com.sun.labs.minion.indexer.entry.DocKeyEntry;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.InvFileDiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.PosPostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;

import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;

/**
 * Selects entries in an index which will be used to create queries
 * for fields for the regression test. The types of entries selected are:
 *
 * <ol>
 * <li>the first last entry in each field of the last document</li>
 * <li>the first and last saved field in the first and last field of 
 *     the field store in each partition</li>
 * </ol>
 */
public class TestFieldEntrySelector {

    private SearchEngineImpl searchEngine;
    private PartitionManager partitionManager;
    private List<FieldEntrySelector> selectors;


    /**
     * Creates a TestFieldEntrySelector based on the given properties.
     * A SearchEngine has to be created before we can select the entrys,
     * so we need to specify the properties of the SearchEngine.
     *
     * @param cmFile the name of the confirguration file
     */
    public TestFieldEntrySelector(String cmFile, String indexDir) 
        throws IOException, SearchEngineException {
        searchEngine = (SearchEngineImpl) SearchEngineFactory.getSearchEngine(cmFile, indexDir);
        partitionManager = searchEngine.getManager();
        selectors = new LinkedList<FieldEntrySelector>();

        // selects the first last entry in each field of the last document
        selectors.add(new FieldFirstLastEntrySelector());

        // selects the first and last saved field in the first and last
        // field of the field store in each partition
        selectors.add(new FirstLastFieldEntrySelector());
    }


    /**
     * Returns the PartitionManager used by this TestFieldEntrySelector.
     *
     * @return the PartitionManager used
     */
    public PartitionManager getPartitionManager() {
        return partitionManager;
    }


    /**
     * Selects entries.
     *
     * @return a list of selected entrys
     */
    public List<FieldEntry> select() {
        List<FieldEntry> entrys = new LinkedList<FieldEntry>();
        for (Iterator i = selectors.iterator(); i.hasNext();) {
            FieldEntrySelector ts = (FieldEntrySelector) i.next();
            List<FieldEntry> list = ts.selectEntries(partitionManager);
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
        while (i.hasNext()) {
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
    private void printEntries(List entryList) {
        for (Iterator i = entryList.iterator(); i.hasNext();) {
            FieldEntry fieldEntry = (FieldEntry) i.next();
            System.out.println
                (SelectorUtil.getFirstToken(fieldEntry.entry.toString()));
        }
    }


    /**
     * A test program that selects field entries from an index.
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
            TestFieldEntrySelector selector 
                = new TestFieldEntrySelector(cmFile, indexDirectory);
            selector.printPartitionInfo();
            System.out.println();
            selector.select();
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.exit(0);
    }
}


/**
 * Selects the first and last entry in each field of the first and last 
 * document in each partition.
 */
class FieldFirstLastEntrySelector implements FieldEntrySelector {

    /**
     * Returns the first and last entry in each field of the last document
     * in each partition.
     *
     * @param manager the PartitionManager which contains the index
     *
     * @return a list of selected entries
     */
    public List<FieldEntry> selectEntries(PartitionManager manager) {
        List<FieldEntry> entries = new LinkedList<FieldEntry>();
        Iterator i = manager.getActivePartitions().iterator();
        
        /* iterate through each partition */
        while (i.hasNext()) {
            DiskPartition partition = (DiskPartition) i.next();
            Iterator di = partition.getDocumentIterator();

            /*
             * get the first and last entry of each field of the first document
             */
            if (di.hasNext()) {
                DocKeyEntry firstDoc = (DocKeyEntry) di.next();
                entries.addAll(getFirstLastEntries(firstDoc, partition));
            }

            /* get the last document */
            DocKeyEntry lastDoc = null;
            while (di.hasNext()) {
                lastDoc = (DocKeyEntry) di.next();
            }

            /* get the first and last entries of the last document */
            if (lastDoc != null) {
                entries.addAll(getFirstLastEntries(lastDoc, partition));
            }
        }
        return entries;
    }


    /**
     * Returns the first and last entries of each field of the given document.
     *
     * @param docEntry the document
     *
     * @return the first and last entries of each field of the given document
     */
    public List<FieldEntry> getFirstLastEntries(DocKeyEntry docEntry,
                                    DiskPartition partition) {
        List<FieldEntry> entries = new LinkedList<FieldEntry>();

        int[] fieldMaxPosition = null;
        Entry[] lastEntry = null;

        /* get the postings of this document */
        PostingsIterator pi =
            docEntry.getPostings().iterator(new PostingsIteratorFeatures());

        PostingsIteratorFeatures features = new PostingsIteratorFeatures();
        features.setPositions(true);

        while (pi.next()) {

            /* get the entry with the given ID */
            QueryEntry entry = partition.getTerm(pi.getID());

            /* get the position positings for the current entry */
            PosPostingsIterator ppi = 
                (PosPostingsIterator) entry.iterator(features);
            
            ppi.findID(docEntry.getID());
            int[][] positions = ppi.getPositions();
            
            /* initialize the fieldMaxPosition and lastEntry arrays correctly */
            if (fieldMaxPosition == null) {
                fieldMaxPosition = new int[positions.length];
                lastEntry = new Entry[positions.length];
            } else {
                if (fieldMaxPosition.length < positions.length) {
                    int[] temp = fieldMaxPosition;
                    fieldMaxPosition = new int[positions.length];
                    System.arraycopy(temp, 0, fieldMaxPosition, 0, temp.length);
                    Entry[] tempEntry = lastEntry;
                    lastEntry = new Entry[positions.length];
                    System.arraycopy(tempEntry, 0, lastEntry, 0, tempEntry.length);
                }
            }

            /* find the first and last entries in the field */
            for (int i = 0; i < positions.length; i++) {
                int[] field = positions[i];
                for (int j = 1; j < field.length; j++) {
                    if (field[j] == 1) {
                        // found the first entry in the field, add it
                        String fieldName = ((InvFileDiskPartition) partition).getFieldStore().getFieldName(i);
                        entries.add(new FieldEntry(entry, fieldName));
                    }

                    // set the current last entry in the field
                    if (field[j] > fieldMaxPosition[i]) {
                        fieldMaxPosition[i] = field[j];
                        lastEntry[i] = entry;
                    }
                }
            }
        }

        // add the last entries of the fields to the list
        if (fieldMaxPosition != null) {
            for (int i = 0; i < fieldMaxPosition.length; i++) {
                if (fieldMaxPosition[i] > 1) {
                    String fieldName = ((InvFileDiskPartition) partition).getFieldStore().getFieldName(i);
                    entries.add(new FieldEntry(lastEntry[i], fieldName));
                }
            }
        }

        return entries;
    }


    /**
     * Returns the name of this selector.
     *
     * @return a description of this selector
     */
    public String toString() {
        return "First and last entries of each field in the last document";
    }
}

/**
 * For the first and last field in the field store of each partition,
 * pick the first and last entry of the field.
 */
class FirstLastFieldEntrySelector implements FieldEntrySelector {

    /* (non-Javadoc)
     * @see com.sun.labs.minion.test.regression.query.FieldEntrySelector#selectEntries(com.sun.labs.minion.indexer.partition.PartitionManager)
     */
    public List<FieldEntry> selectEntries(PartitionManager manager) {
        List<FieldEntry> entries = new LinkedList<FieldEntry>();
        Iterator i = manager.getActivePartitions().iterator();

        while (i.hasNext()) {
            InvFileDiskPartition partition = (InvFileDiskPartition) i.next();

            /* find the first and last entries of the first field */
            String firstFieldName = partition.getFieldStore().getFieldName(1);
            DictionaryIterator firstField =
                partition.getFieldIterator(firstFieldName);
            if (firstField == null) {
                System.err.println("FirstLastFieldEntrySelector.selectEntries() --> First field null from name: " + firstFieldName);
            } else {
                List e = SelectorUtil.getFirstLastEntries(firstField);
                
                /* create a FieldEntry object for each entry */
                for (Iterator it = e.iterator(); it.hasNext(); ) {
                    Entry entry = (Entry) it.next();
                    entries.add(new FieldEntry(entry, firstFieldName));
                }
            }
            
            /* find the first and last entries of the last field */
            String lastFieldName
                = partition.getFieldStore().getFieldName(partition.getManager().getMetaFile().size() - 1);
            DictionaryIterator lastField =
                partition.getFieldIterator(lastFieldName);
            List e = SelectorUtil.getFirstLastEntries(lastField);
            
            /* create a FieldEntry object for each entry */
            for (Iterator it = e.iterator(); it.hasNext(); ) {
                Entry entry = (Entry) it.next();
                entries.add(new FieldEntry(entry, lastFieldName));
            }
        }
        
        return entries;
    }


    /* (non-Javadoc)
     * @see com.sun.labs.minion.test.regression.query.FieldEntrySelector#toString()
     */
    public String toString() {
        return "First and last entry of the first and last field store";
    }
}
