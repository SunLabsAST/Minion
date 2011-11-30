/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.Iterator;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the memory dictionary.
 */
public class MemoryDictionaryTest {

    static File tmpDir;

    static Logger logger =
            Logger.getLogger(MemoryDictionaryTest.class.getName());

    public MemoryDictionaryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        tmpDir = new File(System.getProperty("java.io.tmpdir"));
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    public File getFile(String prefix) {
        try {
            return File.createTempFile(prefix, ".dict");
        } catch(java.io.IOException ex) {

            return null;
        }
    }

    /**
     * Test simple puts.  Make sure they're there.
     */
    @Test
    public void testSimplePuts() {
        MemoryDictionary<String> sd = new MemoryDictionary<String>(new EntryFactory(
                Postings.Type.NONE));
        sd.put("a");
        sd.put("b");
        sd.put("c");
        sd.put("d");
        assertNotNull(sd.get("a"));
        assertNotNull(sd.get("b"));
        assertNotNull(sd.get("c"));
        assertNotNull(sd.get("d"));
        assertNull(sd.get("e"));

        MemoryDictionary<Long> id = new MemoryDictionary<Long>(new EntryFactory(
                Postings.Type.NONE));
        id.put(1l);
        id.put(2l);
        id.put(4l);
        id.put(5l);
        id.put(7l);

        assertNotNull(id.get(1l));
        assertNotNull(id.get(2l));
        assertNull(id.get(3l));
        assertNotNull(id.get(4l));
        assertNotNull(id.get(5l));
        assertNull(id.get(6l));
        assertNotNull(id.get(7l));
    }

    /**
     * Test of remove method, of class MemoryDictionary.
     */
    @Test
    public void testRemove() {
        MemoryDictionary<String> sd = new MemoryDictionary<String>(new EntryFactory(
                Postings.Type.NONE));
        sd.put("a");
        sd.put("b");
        sd.put("c");
        sd.put("d");
        assertNotNull(sd.remove("a"));
        assertNull(sd.remove("a"));
        assertNull(sd.get("a"));
    }

    /**
     * Simple test of the iterator, ensuring that entries come out in name order.
     */
    @Test
    public void testIterator() {
        MemoryDictionary<String> sd = new MemoryDictionary<String>(new EntryFactory(
                Postings.Type.NONE));
        sd.put("b");
        sd.put("d");
        sd.put("c");
        sd.put("a");
        Iterator<Entry> i = sd.iterator();
        assertEquals("a", i.next().getName());
        assertEquals("b", i.next().getName());
        assertEquals("c", i.next().getName());
        assertEquals("d", i.next().getName());
        assertFalse(i.hasNext());

        MemoryDictionary<Long> id = new MemoryDictionary<Long>(new EntryFactory(
                Postings.Type.NONE));
        id.put(7l);
        id.put(5l);
        id.put(2l);
        id.put(1l);
        id.put(4l);
        i = id.iterator();
        assertEquals(1L, i.next().getName());
        assertEquals(2L, i.next().getName());
        assertEquals(4L, i.next().getName());
        assertEquals(5L, i.next().getName());
        assertEquals(7L, i.next().getName());
        assertFalse(i.hasNext());
    }

    /**
     * Test of clear method, of class MemoryDictionary.
     */
    @Test
    public void testClear() {
        MemoryDictionary<String> sd = new MemoryDictionary<String>(new EntryFactory(
                Postings.Type.NONE));
        sd.put("b");
        sd.put("d");
        sd.put("c");
        sd.put("a");
        sd.clear();
        assertNull(sd.get("a"));
        assertNull(sd.get("b"));
        assertNull(sd.get("c"));
        assertNull(sd.get("d"));
    }

    /**
     * Test of dump method, of class MemoryDictionary.
     */
    @Test
    public void testDump() throws Exception {
        MemoryDictionary<String> sd = new MemoryDictionary<String>(new EntryFactory(
                Postings.Type.NONE));
        sd.put("b");
        sd.put("d");
        sd.put("c");
        sd.put("a");

        File f = getFile("string");
        RandomAccessFile dictFile = new RandomAccessFile(f, "rw");
        sd.dump(tmpDir, new StringNameHandler(), dictFile,
                new PostingsOutput[0], MemoryDictionary.Renumber.NONE,
                MemoryDictionary.IDMap.NONE, null);
        dictFile.close();
        f.delete();
    }
}