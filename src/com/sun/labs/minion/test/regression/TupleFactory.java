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

package com.sun.labs.minion.test.regression;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.partition.PartitionManager;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.QueryEntry;

/**
 * Creates tuple objects with documents that contain a certain entry.
 */
public class TupleFactory {

    private static Map tupleMap = new HashMap();

    /**
     * Creates a Tuple with all the documents that contain the given Entry.
     *
     * @param entry the entry the documents of the created Tuple should have
     *
     * @return a Tuple with all the documents that contain the given Entry
     */
    public static Tuple createTuple(Entry entry, PartitionManager manager) {

        String entryName = (String) entry.getName();
        Tuple tuple = (Tuple) tupleMap.get(entry);
        
        if (tuple == null) {
            Iterator i = manager.getActivePartitions().iterator();
            List documentList = new LinkedList();
                        
            /* iterate through all the partitions */
            while (i.hasNext()) {
                
                /* get the entry from the partition */
                DiskPartition partition = (DiskPartition) i.next();
                QueryEntry partEntry = partition.getTerm(entryName);
                
                if (partEntry != null) {
                    /* add the documents of the entry to the document list */
                    PostingsIterator pi = partEntry.iterator(null);
                    while (pi.next()) {
                        /*
                         * since a document can only appear in one partition,
                         * document IDs won't be repeated, so its okay to just
                         * add it to the document list
                         */
                        int docID = pi.getID();
                        String docName = 
                            (String) partition.getDocumentTerm(docID).getName();
                        documentList.add(docName);
                    }
                }
            }
            tuple = new Tuple(documentList, new TupleRelation(entryName));
        }
        return tuple;
    }
}
