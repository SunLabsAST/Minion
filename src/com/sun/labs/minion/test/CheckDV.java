/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
