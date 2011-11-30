package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.util.NanoWatch;
import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
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

    static Logger logger =
            Logger.getLogger(DictionaryTest.class.getName());

    static List<String> wordList;

    static List<String> shuffleList;

    public DictionaryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        tmpDir = new File(System.getProperty("java.io.tmpdir"));
        wordList = new ArrayList<String>();
        InputStream pdis = DictionaryTest.class.getResourceAsStream(
                "/com/sun/labs/minion/indexer/dictionary/resource/words.gz");
        if (pdis == null) {
            logger.severe(String.format("Couldn't find test data!"));
            return;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                pdis)));
        String w;
        while ((w = r.readLine()) != null) {
            wordList.add(w);
        }
        Collections.sort(wordList);

        shuffleList = new ArrayList<String>(wordList);
        Collections.shuffle(shuffleList);
        logger.info(String.format("list: %d shuffle: %d", wordList.size(), shuffleList.
                size()));
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

        File f = File.createTempFile("string", ".dict");
        f.deleteOnExit();
        RandomAccessFile dictFile = new RandomAccessFile(f, "rw");
        sd.dump(tmpDir, new StringNameHandler(), dictFile,
                new PostingsOutput[0], MemoryDictionary.Renumber.NONE,
                MemoryDictionary.IDMap.NONE, null);
        dictFile.close();
    }

    private File makeAndDump(List<String> l) throws Exception {
        MemoryDictionary<String> sd = new MemoryDictionary<String>(new EntryFactory(
                Postings.Type.NONE));
        for (String s : l) {
            sd.put(s);
        }
        File f = File.createTempFile("all", ".dict");
        f.deleteOnExit();
        RandomAccessFile dictFile = new RandomAccessFile(f, "rw");
        sd.dump(tmpDir, new StringNameHandler(), dictFile,
                new PostingsOutput[0], MemoryDictionary.Renumber.NONE,
                MemoryDictionary.IDMap.NONE, null);
        dictFile.close();
        return f;
    }

    private DiskDictionary makeAndGet(List<String> l) throws Exception {
        File f = makeAndDump(l);
        RandomAccessFile dictFile = new RandomAccessFile(f, "r");
        DiskDictionary<String> dd =
                new DiskDictionary<String>(new EntryFactory<String>(
                Postings.Type.NONE),
                new StringNameHandler(),
                dictFile,
                new RandomAccessFile[0]);
        return dd;
    }

    @Test
    public void testAllWordsDump() throws Exception {
        makeAndDump(wordList);
    }

    @Test
    public void testShuffleWordsDump() throws Exception {
        makeAndDump(shuffleList);
    }

    @Test
    public void testDiskDictionarySize() throws Exception {
        DiskDictionary dd = makeAndGet(shuffleList);
        ReadableBuffer rb = encodeDict(wordList);
        logger.info(String.format("Uncompressed: %d compressed: %d ratio: %.3f",
                rb.position(), dd.dh.namesSize, (double) dd.dh.namesSize / rb.
                position()));
    }

    @Test
    public void testDiskDictionaryGet() throws Exception {
        DiskDictionary dd = makeAndGet(shuffleList);
        NanoWatch nw = new NanoWatch();
        nw.start();
        for (String w : wordList) {
            Entry e = dd.get(w);
            assertEquals(String.format("Requested %s got %s", w, e.getName()), e.
                    getName(), w);
        }
        nw.stop();
        logger.info(String.format("%d lookups took %.3fms, %.3fms/lookup", wordList.
                size(),
                nw.getTimeMillis(), nw.getTimeMillis() / wordList.size()));
        dd.dictFile.close();
    }

    @Test
    public void testDiskDictionaryGetFailure() throws Exception {
        int nbad = Math.max((int) (shuffleList.size() * 0.25),
                1000);
        DiskDictionary dd = makeAndGet(shuffleList.subList(nbad, shuffleList.
                size()));
        NanoWatch nw = new NanoWatch();
        nw.start();
        for (String b : shuffleList.subList(0, nbad)) {
            Entry e = dd.get(b);
            assertNull(String.format("Requested %s, got %s", b, e), e);
        }
        nw.stop();
        logger.info(String.format("%d lookups took %.3fms, %.3fms/lookup", nbad,
                nw.getTimeMillis(), nw.getTimeMillis() / nbad));
        dd.dictFile.close();
    }

    @Test
    public void testDiskIteration() throws Exception {
        DiskDictionary dd = makeAndGet(shuffleList);
        DictionaryIterator di = dd.iterator();
        NanoWatch nw = new NanoWatch();
        nw.start();
        for (String w : wordList) {
            QueryEntry qe = (QueryEntry) di.next();
            assertTrue(String.format("Expected %s, got %s", w, qe.getName()), w.
                    equals(qe.getName()));
        }
        nw.stop();
        logger.info(String.format(
                "Iterated through %d entries in %.3fms avg time: %.3fms", wordList.
                size(),
                nw.getTimeMillis(), nw.getTimeMillis() / wordList.size()));
    }

    @Test
    public void testMerge() throws Exception {

        List<String> d1w = new ArrayList<String>();
        List<String> d2w = new ArrayList<String>();
        int nw = Math.max((int) (shuffleList.size() * 0.5),
                1000);
        Random r = new Random();
        for (int i = 0; i < nw; i++) {
            d1w.add(wordList.get(r.nextInt(wordList.size())));
            d2w.add(wordList.get(r.nextInt(wordList.size())));
        }
        DiskDictionary dd1 = makeAndGet(d1w);
        DiskDictionary dd2 = makeAndGet(d2w);

        File f = File.createTempFile("merge", ".dict");
        f.deleteOnExit();

        RandomAccessFile mf = new RandomAccessFile(f, "rw");

        DiskDictionary.merge(tmpDir,
                new StringNameHandler(),
                new DiskDictionary[]{dd1, dd2},
                null, new int[2], new int[2][], mf,
                new PostingsOutput[0], true);

        mf.close();
        dd1.dictFile.close();
        dd2.dictFile.close();
    }

    private ReadableBuffer encodeDict(List<String> l) {
        ArrayBuffer b = new ArrayBuffer(l.size() * 4);
        for (String w : l) {
            b.encode(w);
        }
        return b;
    }
}
