/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.labs.minion.test;

import com.sun.labs.minion.indexer.dictionary.UncachedTermStatsDictionary;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.entry.TermStatsEntry;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.util.Util;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 *
 * @author stgreen
 */
public class CheckTermStatsDict {

    private UncachedTermStatsDictionary dict;

    public CheckTermStatsDict(String f) throws IOException {
        dict = new UncachedTermStatsDictionary(new File(f));
    }

    public CheckTermStatsDict(UncachedTermStatsDictionary dict) {
        this.dict = dict;
    }

    public boolean check(PrintStream output, boolean quiet) {
        boolean ret = true;
        Map<String, TermStatsImpl> m = new LinkedHashMap();
        for (QueryEntry qe : dict) {
            m.put(qe.getName().toString(), ((TermStatsEntry) qe).getTermStats());
        }
        if(!quiet) {
        output.format("Read %d entries\n", m.size());
        }

        for (Map.Entry<String, TermStatsImpl> e : m.entrySet()) {
            TermStatsEntry ts = dict.getTermStats(e.getKey());
            if (ts == null) {
                if(!quiet) {
                output.format("Couldn't find: \"%s\"\n", Util.escape(e.getValue().
                        getName().toString(), true));
                }
                ret = false;
                continue;
            }

            if (!ts.getTermStats().equals(e.getValue())) {
                if(!quiet) {
                output.format("Unequal: %s iter: %s get: %s\n", Util.escape(e.getKey(), true),
                        e.getValue(),
                        ts.getTermStats());
                }
                ret = false;
                continue;
            }
        }
        return ret;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        CheckTermStatsDict check = new CheckTermStatsDict(args[0]);
        boolean ret = check.check(System.out, false);
        if(ret) {
            System.out.format("Dictionary is good\n");
        } else {
            System.out.format("Dictionary is bad\n");
        }
    }

}
