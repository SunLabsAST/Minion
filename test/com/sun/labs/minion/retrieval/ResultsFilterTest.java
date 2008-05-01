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

import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultAccessor;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.ResultsFilter;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.samples.MailIndexer;
import java.io.File;
import java.net.URL;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author stgreen
 */
public class ResultsFilterTest {

    static SearchEngine e;

    public static final String INDEX_DIR = "dft.idx";

    static File indexDir;

    public ResultsFilterTest() {
    }

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
        deleteDirectory(indexDir);
    }

    @Test
    public void removeAllFilter() throws SearchEngineException {
        ResultSet rs = e.search("subject <substring> performance");
        List<Result> l = rs.getAllResults(false, new NoPerformanceFilter());
        assertTrue(l.size() == 0);
    }

    /**
     * A filter that removes all results that have the string "performance"
     * in the subject.
     */
    public class NoPerformanceFilter implements ResultsFilter {
        protected int numCalls = 0;
        protected int numPassed = 0;
        
        public boolean filter(ResultAccessor ra) {
            numCalls++;
            boolean pass = false;
            if (!ra.getSingleFieldValue("subject").toString().toLowerCase().
                    contains("performance")) {
                pass = true;
                numPassed++;
            }
            return pass;
        }
        
        public int getTested() {
            return numCalls;
        }

        public int getPassed() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
        
    }
}
