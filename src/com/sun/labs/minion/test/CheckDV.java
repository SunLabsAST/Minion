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
package com.sun.labs.minion.test;

import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.TermStats;
import com.sun.labs.minion.indexer.entry.FieldedDocKeyEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.partition.DiskPartition;
import com.sun.labs.minion.indexer.postings.FieldedPostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.PostingsIteratorFeatures;

/**
 *
 * @author stgreen
 */
public class CheckDV {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        String indexDir = args[0];
        String key = args[1];
        String field = args[2];

        SearchEngine e = SearchEngineFactory.getSearchEngine(indexDir);
        int fieldID = e.getFieldInfo(field).getID();

        FieldedDocKeyEntry dke = (FieldedDocKeyEntry) e.getManager().getDocumentTerm(key);
        int docID = dke.getID();
        DiskPartition part = (DiskPartition) dke.getPartition();
        System.out.format("part: %s docID: %d fieldID: %d\n", part, dke.getID(), fieldID);
        PostingsIteratorFeatures feat = new PostingsIteratorFeatures();
        feat.setFields(e.getManager().getMetaFile().getFieldArray(field));
        PostingsIterator pi = dke.iterator(feat);
        while(pi.next()) {
            int tid = pi.getID();
            int freq = pi.getFreq();
            QueryEntry term = part.getMainDictionary().get(tid);
            TermStats ts = e.getTermStats(term.getName().toString());
            System.out.format("stats: %s term freq: %d\n", ts, freq);
            PostingsIterator ti = term.iterator(feat);
            if(!ti.findID(docID)) {
                System.err.format("Can't find term: %s\n", term.getName());
            } else {
                int tfreq = ((FieldedPostingsIterator) ti).getFieldFreq()[fieldID];
                if(tfreq != freq) {
                    System.err.format("Mis-matched count: %s dv count: %d term count: %d\n", term.getName(), freq, tfreq);
                }
            }
        }

        e.close();
    }

}
