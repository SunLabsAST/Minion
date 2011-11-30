/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.indexer.entry.Entry;
import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.postings.OccurrenceImpl;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.PostingsIterator;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 *
 * @author stgreen
 */
public class DictionaryAndPostingsTest {

    static File tmpDir;

    static Logger logger = Logger.getLogger(DictionaryTest.class.getName());

    static TestData all;

    public DictionaryAndPostingsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        all = new TestData("wordfreq.gz");
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

    @Test
    public void testSingleIDDictionary() {
        TestDictionary dt = new TestDictionary(all);
        dt.generateDictionary(Postings.Type.ID, 1);
    }

    @Test
    public void testSingleIDFreqDictionary() {
        TestDictionary dt = new TestDictionary(all);
        dt.generateDictionary(Postings.Type.ID_FREQ, 1);
    }

    @Test
    public void testSmallIDDictionary() {
        TestDictionary dt = new TestDictionary(all);
        dt.generateDictionary(Postings.Type.ID, 20);
    }

    @Test
    public void testSmallIDFreqDictionary() {
        TestDictionary dt = new TestDictionary(all);
        dt.generateDictionary(Postings.Type.ID_FREQ, 20);
    }

    @Test
    public void testIDDump() throws Exception {
        TestDictionary td = new TestDictionary(all);
        DiskDictionary dd = td.generateDiskDictionary(Postings.Type.ID, 50);
        td.close();
    }

    @Test
    public void testIDFreqDump() throws Exception {
        TestDictionary td = new TestDictionary(all);
        DiskDictionary dd = td.generateDiskDictionary(Postings.Type.ID_FREQ, 50);
        td.close();
    }

    @Test
    public void testIDDumpAndIterate() throws Exception {
        TestDictionary td = new TestDictionary(all);
        DiskDictionary<String> dd = td.generateDiskDictionary(Postings.Type.ID,
                                                              50);
        Map<String, List<DocPosting>> temp = new HashMap<String, List<DocPosting>>(
                td.m);
        for(Entry<String> e : dd) {
            List<DocPosting> l = temp.remove(e.getName());
            assertNotNull(String.format("Couldn't find %s", e.getName()));
            PostingsIterator pi = e.iterator(null);
            assertNotNull(String.format("Null iterator for %s", e.getName()), pi);
            for(DocPosting p : l) {
                if(pi.next()) {
                    assertTrue(String.format("Bad ID for %s expected %d got %d",
                                             e.getName(), p.id, pi.getID()),
                               p.id == pi.getID());
                } else {
                    fail(String.format("Ran out of IDs at %d", p.id));
                }
            }
        }
        td.close();
    }

    @Test
    public void testIDFreqDumpAndIterate() throws Exception {
        TestDictionary td = new TestDictionary(all);
        DiskDictionary<String> dd = td.generateDiskDictionary(Postings.Type.ID,
                                                              50);
        Map<String, List<DocPosting>> temp = new HashMap<String, List<DocPosting>>(
                td.m);
        for(Entry<String> e : dd) {
            List<DocPosting> l = temp.remove(e.getName());
            assertNotNull(String.format("Couldn't find %s", e.getName()));
            PostingsIterator pi = e.iterator(null);
            assertNotNull(String.format("Null iterator for %s", e.getName()), pi);
            for(DocPosting p : l) {
                if(pi.next()) {
                    assertTrue(String.format("Bad ID for %s expected %d got %d",
                                             e.getName(), p.id, pi.getID()),
                               p.id == pi.getID());
                } else {
                    fail(String.format("Ran out of IDs at %d", p.id));
                }
            }
        }
        td.close();
    }

    static protected class WordFreq implements Comparable<WordFreq> {

        public String word;

        public long freq;

        public WordFreq(String word, int freq) {
            this.word = word;
            this.freq = freq;
        }

        public int compareTo(WordFreq o) {
            return (int) (freq - o.freq);
        }
    }

    static protected class DocPosting {

        int freq;

        int id;

        public DocPosting(int id) {
            this.id = id;
        }
    }

    static protected class TestDictionary {

        private TestData td;

        private Map<String, List<DocPosting>> m =
                new HashMap<String, List<DocPosting>>();

        private MemoryDictionary<String> md;

        private DiskDictionary dd;

        public TestDictionary(TestData td) {
            this.td = td;
        }

        public MemoryDictionary<String> generateDictionary(Postings.Type type,
                                                           int nDocs) {
            md = new MemoryDictionary<String>(new EntryFactory(type));
            OccurrenceImpl o = new OccurrenceImpl();
            Map<String, DocPosting> doc = new HashMap<String, DocPosting>();
            for(int i = 0; i < nDocs; i++) {
                int size = td.rand.nextInt(1024) + 1;
                o.setID(i + 1);
                for(int j = 0; j < size; j++) {
                    String w = td.getNextWord();
                    IndexEntry e = md.put(w);
                    DocPosting p = doc.get(w);
                    if(p == null) {
                        p = new DocPosting(i + 1);
                        doc.put(w, p);
                    }
                    p.freq++;
                    e.add(o);
                }
                for(Map.Entry<String, DocPosting> e : doc.entrySet()) {
                    List<DocPosting> l = m.get(e.getKey());
                    if(l == null) {
                        l = new ArrayList<DocPosting>();
                        m.put(e.getKey(), l);
                    }
                    l.add(e.getValue());
                }
            }
            return md;
        }

        public DiskDictionary<String> generateDiskDictionary(Postings.Type type,
                                                             int nDocs) throws
                Exception {
            generateDictionary(type, nDocs);
            File dictFile = File.createTempFile("all", ".dict");
            dictFile.deleteOnExit();
            RandomAccessFile raf = new RandomAccessFile(dictFile, "rw");
            File postFile = File.createTempFile("all", ".post");
            postFile.deleteOnExit();
            OutputStream os = new BufferedOutputStream(
                    new FileOutputStream(postFile));
            md.dump(DictionaryTest.tmpDir,
                    new StringNameHandler(), raf,
                    new PostingsOutput[]{new StreamPostingsOutput(os)},
                    MemoryDictionary.Renumber.NONE,
                    MemoryDictionary.IDMap.NONE, null);

            raf.close();
            os.close();

            raf = new RandomAccessFile(dictFile, "r");
            RandomAccessFile praf = new RandomAccessFile(postFile, "r");
            dd = new DiskDictionary<String>(new EntryFactory<String>(type),
                                            new StringNameHandler(), raf,
                                            new RandomAccessFile[]{praf});
            return dd;
        }

        public void close() throws java.io.IOException {
            if(dd != null) {
                dd.dictFile.close();
                if(dd.postFiles != null && dd.postFiles.length > 0) {
                    for(RandomAccessFile raf : dd.postFiles) {
                        raf.close();
                    }
                }
            }
        }
    }

    static protected class TestData {

        List<WordFreq> words = new ArrayList<WordFreq>();

        double[] model;

        Random rand = new Random();

        long totalFreq;

        public TestData(String resourceName) throws Exception {
            InputStream is = DictionaryTest.class.getResourceAsStream(
                    resourceName);
            if(is == null) {
                is = TestData.class.getResourceAsStream("/com/sun/labs/minion/indexer/dictionary/resource/"
                        + resourceName);
            }
            if(is == null) {
                throw new java.io.IOException(String.format(
                        "Couldn\'t find resource %s",
                        resourceName));
            }
            BufferedReader r;
            if(resourceName.endsWith(".gz")) {
                r = new BufferedReader(new InputStreamReader(new GZIPInputStream(
                        is), "utf-8"));
            } else {
                r = new BufferedReader(new InputStreamReader(is, "utf-8"));
            }
            String w;
            int n = 0;
            while((w = r.readLine()) != null) {
                n++;
                String[] f = w.split("\\s+");
                if(f.length != 2) {
                    logger.warning(String.format("Bad line %d: %s", n, w));
                    continue;
                }

                try {
                    WordFreq wf = new WordFreq(f[0], Integer.parseInt(f[1]));
                    words.add(wf);
                    totalFreq += wf.freq;
                } catch(NumberFormatException ex) {
                    logger.warning(String.format("Bad number at %d: %s", n, w));
                }
            }
            r.close();
            Collections.sort(words);
            model = new double[words.size()];
            //
            // Normalize the frequency.
            for(int i = 0; i < words.size(); i++) {
                model[i] = words.get(i).freq / (double) totalFreq;
            }
        }

        /**
         * Gets the next word using a zipfian distribution.
         * @return
         */
        public String getNextWord() {
            double p = rand.nextDouble();
            for(int i = 0; i < model.length; i++) {
                p -= model[i];
                if(p <= 0) {
                    return words.get(i).word;
                }
            }
            return words.get(words.size() - 1).word;
        }
    }
}
