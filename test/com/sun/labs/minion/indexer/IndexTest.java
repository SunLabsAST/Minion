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

package com.sun.labs.minion.indexer;

import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.samples.MailIndexer;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;

/**
 *
 * @author stgreen
 */
public class IndexTest {

    private static Logger log;

    private static MailIndexer mi;

    private static File indexDir;

    public IndexTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setFormatter(new SimpleLabsLogFormatter());
        }
        log = Logger.getLogger("com.sun.labs.minion.indexer.IndexTest");
        indexDir = new File(System.getProperty("java.io.tmpdir"), "indextest.idx");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        try {
            mi = new MailIndexer(indexDir.toString());
        } catch(SearchEngineException ex) {
            log.log(Level.SEVERE, "Error opening engine", ex);
        }
    }

    @After
    public void tearDown() {
        try {
            mi.close();
            TestUtil.deleteDirectory(indexDir);
        } catch(SearchEngineException ex) {
            log.log(Level.SEVERE, "Error closing engine", ex);
        }
    }

    @Test
    public void testPurge() throws IOException, SearchEngineException {
        URL u = getClass().getResource("dtrace-short.txt");
        mi.indexMBox(u);
        SearchEngine e = mi.getSearchEngine();
        int n = e.getNDocs();
        assertTrue(n > 0);
        e.purge();
        n = e.getNDocs();
        assertTrue(n == 0);
    }

    @Test
    public void reindex() throws SearchEngineException, IOException {
        URL u = getClass().getResource("dtrace.txt");
        mi.indexMBox(u);
        SearchEngine e = mi.getSearchEngine();
        int n = e.getNDocs();
        mi.indexMBox(u);
        assertTrue(n == e.getNDocs());
    }
}
