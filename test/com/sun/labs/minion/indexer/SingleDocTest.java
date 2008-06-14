/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.indexer;

import com.sun.labs.minion.ResultSet;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Handler;
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
public class SingleDocTest {

    static Logger log;

    SearchEngine engine;

    File indexDir;

    public SingleDocTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setFormatter(new SimpleLabsLogFormatter());
        }
        log = Logger.getLogger("com.sun.labs.minion.indexer.IndexTest");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws SearchEngineException {

        indexDir = new File(System.getProperty("java.io.tmpdir"),
                "indextest.idx");
        engine = SearchEngineFactory.getSearchEngine(indexDir.toString());
    }

    @After
    public void tearDown() throws SearchEngineException {
        engine.close();
        TestUtil.deleteDirectory(indexDir);
    }

    @Test
    public void indexOneDoc() throws SearchEngineException {
        HashMap<String, Object> m = new LinkedHashMap();
        m.put("name", "hello world");
        engine.index("SUN", m);
        engine.close();
        engine = SearchEngineFactory.getSearchEngine(indexDir.toString());
        ResultSet rs = engine.search("hello");
        assertTrue(rs.size() == 1);
    }
}