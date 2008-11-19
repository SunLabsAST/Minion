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

import com.sun.labs.minion.indexer.dictionary.UncachedTermStatsDictionary;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.entry.TermStatsEntry;
import com.sun.labs.minion.retrieval.TermStatsImpl;
import com.sun.labs.minion.util.Util;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        List<TermStatsImpl> actual = new ArrayList();
        for (QueryEntry qe : dict) {
            actual.add(((TermStatsEntry) qe).getTermStats());
        }
        List<TermStatsImpl> sorted = new ArrayList<TermStatsImpl>(actual);
        Collections.sort(sorted);
        if (!quiet) {
            output.format("Read %d entries\n", actual.size());
        }

        //
        // Make sure the order of the entries is right.
        for(int i = 0; i < actual.size(); i++) {
            if(actual.get(i) != sorted.get(i)) {
                output.format("Actual: %s Sorted: %s\n", actual.get(i), sorted.get(i));
                ret = false;
            }
        }

        for (TermStatsImpl tsi : actual) {
            TermStatsEntry ts = dict.getTermStats(tsi.getName());
            if (ts == null) {
                if (!quiet) {
                    output.format("Couldn't find: \"%s\"\n", 
                            Util.escape(tsi.getName(), true));
                }
                ret = false;
                continue;
            }

            if (!ts.getTermStats().equals(tsi)) {
                if (!quiet) {
                    output.format("Unequal: %s iter: %s get: %s\n", Util.escape(
                            tsi.getName(), true),
                            tsi,
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
        if (ret) {
            System.out.format("Dictionary is good\n");
        } else {
            System.out.format("Dictionary is bad\n");
        }
    }
}
