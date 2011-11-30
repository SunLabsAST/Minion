package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.indexer.partition.DumpState;
import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.util.NanoWatch;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the dictionaries.
 */
public class DictionaryTest {

    static File tmpDir;

    static final Logger logger = Logger.getLogger(DictionaryTest.class.getName());

    static String[] otherData = new String[]{
        "words1.gz",};

    static TestData all;

    public DictionaryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        tmpDir = new File(System.getProperty("java.io.tmpdir"));
        all = new TestData("words.gz");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        all.close();
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
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
        sd.sort(MemoryDictionary.Renumber.RENUMBER, MemoryDictionary.IDMap.NONE);
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
        id.sort(MemoryDictionary.Renumber.RENUMBER, MemoryDictionary.IDMap.NONE);
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

        DumpState dumpState = new DumpState(tmpDir);
        dumpState.renumber = MemoryDictionary.Renumber.RENUMBER;
        dumpState.idMap = MemoryDictionary.IDMap.NONE;
        dumpState.encoder = new StringNameHandler();
        sd.dump(dumpState);
        logger.info(String.format("Dump completed"));
        File f = File.createTempFile("string", ".dict");
        f.deleteOnExit();
        RandomAccessFile dictFile = new RandomAccessFile(f, "rw");
        dumpState.fieldDictOut.flush(dictFile);
        dictFile.close();
    }

    @Test
    public void testAllWordsDump() throws Exception {
    }

    @Test
    public void testShuffleWordsDump() throws Exception {
        TestData td = new TestData(all.words);
        td.close();
    }

    @Test
    public void testDiskDictionarySize() throws Exception {
        ReadableBuffer rb = encodeDict(all.words);
        logger.info(String.format("Uncompressed: %d compressed: %d ratio: %.3f",
                                  rb.position(), all.dd.dh.namesSize,
                                  (double) all.dd.dh.namesSize / rb.position()));
    }

    @Test
    public void testDiskDictionaryGet() throws Exception {
        NanoWatch nw = new NanoWatch();
        nw.start();
        for(String w : all.words) {
            Entry e = all.dd.get(w);
            assertEquals(String.format("Requested %s got %s", w, e.getName()), e.
                    getName(), w);
        }
        nw.stop();
        logger.info(String.format("%d lookups took %.3fms, %.3fms/lookup", all.words.
                size(),
                                  nw.getTimeMillis(), nw.getTimeMillis() / all.words.
                size()));
    }

    @Test
    public void testDiskDictionaryGetFailure() throws Exception {
        List<String> shuffleList = new ArrayList<String>(all.words);
        Collections.shuffle(shuffleList);
        int nbad = Math.max((int) (shuffleList.size() * 0.25), 1000);
        TestData td =
                new TestData(shuffleList.subList(nbad, shuffleList.size()));
        NanoWatch nw = new NanoWatch();
        nw.start();
        for(String b : shuffleList.subList(0, nbad)) {
            Entry e = td.dd.get(b);
            assertNull(String.format("Requested %s, got %s", b, e), e);
        }
        nw.stop();
        logger.info(String.format("%d lookups took %.3fms, %.3fms/lookup", nbad,
                                  nw.getTimeMillis(), nw.getTimeMillis() / nbad));
        td.close();
    }

    @Test
    public void testDiskIteration() throws Exception {
        DictionaryIterator di = all.dd.iterator();
        NanoWatch nw = new NanoWatch();
        nw.start();
        for(String w : all.words) {
            QueryEntry qe = (QueryEntry) di.next();
            assertTrue(String.format("Expected %s, got %s", w, qe.getName()), w.
                    equals(qe.getName()));
        }
        nw.stop();
        logger.info(String.format(
                "Iterated through %d entries in %.3fms avg time: %.3fms", all.words.
                size(),
                nw.getTimeMillis(), nw.getTimeMillis() / all.words.size()));
    }

    @Test
    public void testRandomIteration() throws Exception {

        TestData td = new TestData((int) (all.words.size() * 0.001), all.words);
        DictionaryIterator di = td.dd.iterator();
        for(String w : td.uniq) {
            QueryEntry qe = (QueryEntry) di.next();
            if(!w.equals(qe.getName())) {
                assertTrue(String.format("Expected %s, got %s, words: %s", w, qe.
                        getName(), td.dump()), w.equals(qe.getName()));

            }
        }
    }

    @Test
    public void testOldIterations() throws Exception {
        for(String r : otherData) {
            TestData td = new TestData(r);
            DictionaryIterator di = td.dd.iterator();
            for(String w : td.uniq) {
                QueryEntry qe = (QueryEntry) di.next();
                if(!w.equals(qe.getName())) {
                    assertTrue(String.format("Expected %s, got %s, words: %s", w, qe.
                            getName(), td.dump()), w.equals(qe.getName()));

                }
            }
        }
    }

    private ReadableBuffer encodeDict(List<String> l) {
        ArrayBuffer b = new ArrayBuffer(l.size() * 4);
        for(String w : l) {
            b.encode(w);
        }
        return b;
    }
}
