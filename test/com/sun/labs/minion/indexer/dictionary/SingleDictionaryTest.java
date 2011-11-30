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
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
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
public class SingleDictionaryTest {

    static File tmpDir;

    static Logger logger = Logger.getLogger(SingleDictionaryTest.class.getName());

    static String[] otherData = new String[]{
        "words1.gz",};

    static TestData all;

    public SingleDictionaryTest() {
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

//    @Test
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
    public void testOldIterations() throws Exception {
        for(String r : otherData) {
            TestData td = new TestData(r);
            DictionaryIterator di = td.dd.iterator();
            for(String w : td.words) {
                QueryEntry qe = (QueryEntry) di.next();
                logger.info(String.format("w: %s e: %s", w, qe.getName()));
                if(!w.equals(qe.getName())) {
                    assertTrue(String.format("Expected %s, got %s, words: %s", w, qe.
                            getName(), td.dump()), w.equals(qe.getName()));

                }
            }
        }
    }

//    @Test
    public void testRandomIteration() throws Exception {

        TestData td = new TestData();
        DictionaryIterator di = td.dd.iterator();
        for(String w : td.words) {
            QueryEntry qe = (QueryEntry) di.next();
            if(!w.equals(qe.getName())) {
                assertTrue(String.format("Expected %s, got %s, words: %s", w, qe.
                        getName(), td.dump()), w.equals(qe.getName()));

            }
        }
    }

    private static class TestData {

        private List<String> words;

        private MemoryDictionary md;

        private DiskDictionary dd;

        private File dictFile;

        private RandomAccessFile raf;

        public TestData() throws Exception {
            this(Math.max((int) (all.words.size() * 0.5),
                          1000));
        }

        public TestData(int n) throws Exception {
            words = new ArrayList<String>(n);
            Random r = new Random();
            for(int i = 0; i < n; i++) {
                words.add(all.words.get(r.nextInt(all.words.size())));
            }
            Collections.sort(words);
            init();
        }

        public TestData(List<String> words) throws Exception {
            this.words = words;
            init();
        }

        public TestData(String resourceName) throws Exception {
            InputStream is = SingleDictionaryTest.class.getResourceAsStream(
                    resourceName);
            if(is == null) {
                is = SingleDictionaryTest.class.getResourceAsStream(
                        "/com/sun/labs/minion/indexer/dictionary/resource/"
                        + resourceName);
            }
            if(is == null) {
                throw new java.io.IOException(String.format(
                        "Couldn't find resource %s", resourceName));
            }

            BufferedReader r;

            if(resourceName.endsWith(".gz")) {
                r = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                        is), "utf-8"));
            } else {
                r = new BufferedReader(new InputStreamReader(is, "utf-8"));
            }
            words = new ArrayList<String>();
            String w;
            while((w = r.readLine()) != null) {
                words.add(w);
            }
            init();
        }

        private void init() throws Exception {
            md = new MemoryDictionary<String>(new EntryFactory(
                    Postings.Type.NONE));
            for(String s : words) {
                md.put(s);
            }
            dictFile = File.createTempFile("all", ".dict");
            dictFile.deleteOnExit();
            raf = new RandomAccessFile(dictFile, "rw");
            md.dump(tmpDir, new StringNameHandler(), raf,
                    new PostingsOutput[0], MemoryDictionary.Renumber.NONE,
                    MemoryDictionary.IDMap.NONE, null);
            raf.close();
            raf = new RandomAccessFile(dictFile, "r");
            dd = new DiskDictionary<String>(new EntryFactory<String>(
                    Postings.Type.NONE), new StringNameHandler(), raf,
                                            new RandomAccessFile[0]);
        }

        public File dump() throws Exception {
            File f = File.createTempFile("words", "");
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
                    f), "utf-8"));
            for(String w : words) {
                pw.println(w);
            }
            pw.close();
            return f;
        }

        private void close() throws Exception {
            raf.close();
        }
    }
}
