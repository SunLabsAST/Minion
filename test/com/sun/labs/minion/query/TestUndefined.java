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

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Result;
import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.SimpleIndexer;
import com.sun.labs.minion.indexer.TestUtil;
import java.io.File;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class TestUndefined {

    private static Logger logger = Logger.getLogger(
            TestUndefined.class.getName());

    private static SearchEngine se;

    private static File indexDir;

    private static Integer[] ints = new Integer[]{1, 2, 3, null, 5, 6, 7, null,
                                                  9, 10};

    private static String[] strings =
            new String[]{
        "Regarding the incorporation of Dtrace capabilities within Containers,",
        null,
        "I wanted to get a \"pulse\" on current and near-term support for this in",
        "current/planned Solaris 10 updates (unless only Nevada will handle this",
        null,
        "capabilty, which was not my prior understanding).  Any details / URL's",
        "explaining the capabilites would also be helpfull.",
        null,
        "Note that just catching |fork()| is not enougth since some shells may",
        "use newer ways to spawn children (like |posix_spawn()| (if available and"
    };

    public TestUndefined() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        indexDir =
                new File(System.getProperty("java.io.tmpdir"), "udtest.idx");
        se = SearchEngineFactory.getSearchEngine(indexDir.toString());
        se.defineField(new FieldInfo("num", EnumSet.of(
                FieldInfo.Attribute.SAVED), FieldInfo.Type.INTEGER));
        se.defineField(new FieldInfo("intf", EnumSet.of(
                FieldInfo.Attribute.SAVED), FieldInfo.Type.INTEGER));
        se.defineField(new FieldInfo("stringf",
                                     EnumSet.of(FieldInfo.Attribute.SAVED,
                                                FieldInfo.Attribute.INDEXED,
                                                FieldInfo.Attribute.TOKENIZED),
                                     FieldInfo.Type.STRING));
        SimpleIndexer si = se.getSimpleIndexer();
        for(int i = 0; i < ints.length; i++) {
            si.startDocument("key-" + (i + 1));
            si.addField("num", new Integer(i));
            if(ints[i] != null) {
                si.addField("intf", ints[i]);
            }
            if(strings[i] != null) {
                si.addField("stringf", strings[i]);
            }
            si.endDocument();
        }
        si.finish();
        se.flush();
        Thread.sleep(1000);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        try {
            se.close();
            TestUtil.deleteDirectory(indexDir);
        } catch(SearchEngineException ex) {
            logger.log(Level.SEVERE, "Error closing engine", ex);
        }
    }

    @Test
    public void testUndefinedInt() throws Exception {
        Undefined undef = new Undefined("intf");
        ResultSet rs = se.search(undef);
        int nundef = 0;
        for(int i = 0; i < ints.length; i++) {
            if(ints[i] == null) {
                nundef++;
            }
        }
        assertEquals("Wrong number of results!", nundef, rs.size());
        for(Result r : rs.getAllResults(false)) {
            int n = ((Long) r.getSingleFieldValue("num")).intValue();
            assertNull("Undefined for non-null value", ints[n]);
        }
    }

    @Test
    public void testDefinedInt() throws Exception {
        Element el = new Not(new Undefined("intf"));
        ResultSet rs = se.search(el);
        int ndef = 0;
        for(int i = 0; i < ints.length; i++) {
            if(ints[i] != null) {
                ndef++;
            }
        }
        assertEquals("Wrong number of results!", ndef, rs.size());
        for(Result r : rs.getAllResults(false)) {
            int n = ((Long) r.getSingleFieldValue("num")).intValue();
            assertNotNull("Not Undefined for null value", ints[n]);
        }
    }

    @Test
    public void testUndefinedString() throws Exception {

        Undefined undef = new Undefined("stringf");
        ResultSet rs = se.search(undef);
        int nundef = 0;
        for(int i = 0; i < strings.length; i++) {
            if(strings[i] == null) {
                nundef++;
            }
        }
        assertEquals("Wrong number of results!", nundef, rs.size());
        for(Result r : rs.getAllResults(false)) {
            int n = ((Long) r.getSingleFieldValue("num")).intValue();
            assertNull("Undefined for non-null value", strings[n]);
        }
    }
    @Test
    public void testDefinedString() throws Exception {

        Element el = new Not(new Undefined("stringf"));
        ResultSet rs = se.search(el);
        int ndef = 0;
        for(int i = 0; i < strings.length; i++) {
            if(strings[i] != null) {
                ndef++;
            }
        }
        assertEquals("Wrong number of results!", ndef, rs.size());
        for(Result r : rs.getAllResults(false)) {
            int n = ((Long) r.getSingleFieldValue("num")).intValue();
            assertNotNull("Undefined for non-null value", strings[n]);
        }
    }
}
