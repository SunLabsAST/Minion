/*
 * Copyright 2007-2009 Sun Microsystems, Inc. All Rights Reserved.
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
package com.sun.labs.minion.query;

import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.indexer.TestUtil;
import com.sun.labs.minion.samples.MailIndexer;
import com.sun.labs.util.LabsLogFormatter;
import java.io.File;
import java.net.URL;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 */
public class TestBase {

    protected static File indexDir;

    protected static Logger logger;

    protected static MailIndexer mi;

    protected static String[] terms =
            new String[]{"java", "install", "graphical",
        "build", "latency", "syscall", "issue",
        "provider", "two", "thread"};

    protected static String[] csTerms =
            new String[]{"Java", "Install", "Graphical",
        "Build", "Latency", "Syscall", "Issue",
        "Provider", "Two", "Thread"};

    @BeforeClass
    public static void setUpClass() throws Exception {
        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setFormatter(new LabsLogFormatter());
        }
        logger = Logger.getLogger(TestTerms.class.getName());
        indexDir =
                new File(System.getProperty("java.io.tmpdir"), "indextest.idx");
        try {
            mi = new MailIndexer(indexDir.toString());
            URL u =
                    TestTerms.class.getResource(
                    "/com/sun/labs/minion/indexer/dtrace.txt");
            mi.indexMBox(u, "[dtrace-discuss]");
        } catch(SearchEngineException ex) {
            logger.log(Level.SEVERE, "Error opening engine", ex);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            mi.close();
            TestUtil.deleteDirectory(indexDir);
        } catch(SearchEngineException ex) {
            logger.log(Level.SEVERE, "Error closing engine", ex);
        }
    }

    public TestBase() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
}
