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

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultAccessor;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.ResultsFilter;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to do cross-index checking of document vector find similar scores.
 */
public class CrossIndexDV {

    private SearchEngine e1;
    private SearchEngine e2;

    public CrossIndexDV(SearchEngine e1, SearchEngine e2) {
        this.e1 = e1;
        this.e2 = e2;
    }

    public ResultSet findSim(String key, boolean findDoc) throws SearchEngineException {
        DocumentVector dv = e1.getDocumentVector(key);
        SearchEngine other = null;
        SearchEngine host = null;
        if (dv == null) {
            dv = e2.getDocumentVector(key);
            if (dv == null) {
                System.err.println("Can't find key: " + key);
                return null;
            }
            other = e1;
            host = e2;
        } else {
            other = e2;
            host = e1;
        }
        if (findDoc) {
            try {
                ResultSet ks = host.search(String.format("aura-key = \"%s\"", key));
                kr = ks.getResults(0, 10).get(0);
            } catch (SearchEngineException see) {
                System.err.println(String.format("Bad search for: %s", key));
                kr = null;
            }
        }
        dv.setEngine(other);
        return dv.findSimilar();
    }
    double sumDiff = 0;
    int nDiffs = 0;
    Result kr;
    ArtistFilter artistFilter = new ArtistFilter();

    public void checkKey(String key) throws SearchEngineException {
        ResultSet rs = findSim(key, true);
        System.out.println(String.format("Source key %s (%s)", key,
                kr == null ? null : kr.getSingleFieldValue("aura-name")));
        int pos = 0;
        for (Result r : rs.getResults(0, 10, artistFilter)) {
            if (r.getKey().equals(key)) {
                System.out.println(String.format(" Source at position %d score %.3f", pos, r.getScore()));
            } else {
                double score = r.getScore();
                double rscore = reflex(r, key);
                if (rscore < 0) {
                    System.out.println(String.format(" Target: %s (%s) source->target %.3f target->source not found",
                            r.getKey(),
                            r.getSingleFieldValue("aura-name"),
                            score));
                } else {
                    double diff = Math.abs(score - rscore);
                    sumDiff += diff;
                    nDiffs++;
                    System.out.println(String.format(" Target: %s (%s) source->target %.3f target->source %.3f (%5.3f%%)",
                            r.getKey(),
                            r.getSingleFieldValue("aura-name"),
                            score, rscore,
                            (diff / score) * 100));
                }
            }
            pos++;
        }

    }

    public double reflex(Result r, String key) throws SearchEngineException {
        ResultSet rset = findSim(r.getKey(), false);
        for (Result rr : rset.getAllResults(true)) {
            if (rr.getKey().equals(key)) {
                return rr.getScore();
            }
        }
        return -1;
    }

    public void close() throws SearchEngineException {
        e1.close();
        e2.close();
    }

    public static void main(String[] args) throws Exception {

        //
        // Use the labs format logging.
        Logger rl = Logger.getLogger("");
        for (Handler h : rl.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new SimpleLabsLogFormatter());
            try {
                h.setEncoding("utf-8");
            } catch (Exception ex) {
                rl.severe("Error setting output encoding");
            }
        }

        CrossIndexDV cid = new CrossIndexDV(
                SearchEngineFactory.getSearchEngine(args[0]),
                SearchEngineFactory.getSearchEngine(args[1]));

        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        String key;

        while ((key = r.readLine()) != null) {
            cid.checkKey(key);
        }
        cid.close();
        System.out.println(String.format("Average difference: %.3f", cid.sumDiff / cid.nDiffs));
    }

    public class ArtistFilter implements ResultsFilter {

        @Override
        public boolean filter(ResultAccessor ra) {
            String v = (String) ra.getSingleFieldValue("aura-type");
            return v != null && v.equalsIgnoreCase("artist");
        }

        @Override
        public int getTested() {
            return 0;
        }

        @Override
        public int getPassed() {
            return 0;
        }
        
    }
}
