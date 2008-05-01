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

package com.sun.labs.minion.retrieval;

import com.sun.labs.minion.DocumentVector;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.WeightedField;
import java.io.File;
import java.net.URL;
import com.sun.labs.minion.samples.MailIndexer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author stgreen
 */
public class CompositeDocumentVectorTest {

    public CompositeDocumentVectorTest() {
    }
    static SearchEngine e;

    public static final String INDEX_DIR = "cdvt.idx";

    static File indexDir;

    private static void deleteDirectory(File indexDir) {
        File[] fs = indexDir.listFiles();
        for(File f : fs) {
            if(f.isDirectory()) {
                deleteDirectory(f);
            } else {
                assertTrue(f.delete());
            }
        }
        assertTrue(indexDir.delete());
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        //
        // Make sure we have a fresh index.
        indexDir = new File(new File(System.getProperty("java.io.tmpdir")), INDEX_DIR);
        if(indexDir.exists()) {
            deleteDirectory(indexDir);
        }
        MailIndexer mi = new MailIndexer(indexDir.toString());
        URL u = ResultsFilterTest.class.getResource("/com/sun/labs/minion/indexer/dtrace.txt");
        mi.indexMBox(u);
        u = ResultsFilterTest.class.getResource("/com/sun/labs/minion/indexer/zfs.txt");
        mi.indexMBox(u);
        e = mi.getSearchEngine();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if(e != null) {
            e.close();
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testComposite() throws SearchEngineException {
        doCompositeQuery("subject <substring> \"install dtrace  java api on solaris\"",
                0.5f, 0.5f);
    }

    @Test
    public void testComposite2() throws SearchEngineException {
        doCompositeQuery("subject <substring> \"install dtrace  java api on solaris\"", 0.75f, 0.25f);
    }

    private void doCompositeQuery(String query, float w1, float w2) throws SearchEngineException {
        ResultSet qr = e.search(query);
        doCompositeKey(qr.getResults(0, 1).get(0).getKey(), w1, w2);
    }
    
    private void doCompositeKey(String key, float w1, float w2) throws SearchEngineException {
        DocumentVector sv = e.getDocumentVector(key, "subject");
        DocumentVector bv = e.getDocumentVector(key, "body");
        DocumentVector sbv = e.getDocumentVector(key, new WeightedField[]{
            new WeightedField("subject", w1),
            new WeightedField("body", w2)
        });

        Map<String, Float> ss = new HashMap<String, Float>();
        Map<String, Float> bs = new HashMap<String, Float>();

        //
        // Get the scores for the documents in either set.
        ResultSet srs = sv.findSimilar("-score");
        List<Result> l = srs.getAllResults(false);
        for(Result r : l) {
            ss.put(r.getKey(), r.getScore());
        }
        ResultSet brs = bv.findSimilar("-score");
        l = brs.getAllResults(false);
        for(Result r : l) {
            bs.put(r.getKey(), r.getScore());
        }

        //
        // Make sure the score in the composite the composed scores from the two
        // other sets.
        ResultSet sbrs = sbv.findSimilar("-score");
        l = sbrs.getAllResults(false);
        for(Result r : l) {
            String k = r.getKey();
            Float f = ss.get(k);
            float s1 = f == null ? 0 : f.floatValue();
            f = bs.get(k);
            float s2 = f == null ? 0 : f.floatValue();
            assertTrue("Error for key: " + k + " " + s1 + " " + s2,
                    w1 * s1 + w2 * s2 == r.getScore());
        }
    }
}
