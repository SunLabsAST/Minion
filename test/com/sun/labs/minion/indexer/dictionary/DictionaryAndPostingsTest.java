/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.indexer.dictionary.io.DictionaryOutput;
import com.sun.labs.minion.indexer.dictionary.io.DiskDictionaryOutput;
import java.io.OutputStreamWriter;
import com.sun.labs.util.LabsLogFormatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.io.PrintWriter;
import com.sun.labs.minion.indexer.postings.Zipf;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.StringWriter;
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
 */
public class DictionaryAndPostingsTest {

    private static final Logger logger = Logger.getLogger(DictionaryTest.class.getName());

    static TestData all;

    public DictionaryAndPostingsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        all = new TestData("wordfreq.gz", 50000);
        logger.info(String.format("class set up."));
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
        testDictionaries(Postings.Type.ID, 1, 1, 20);
    }

    @Test
    public void testSingleIDFreqDictionary() {
        testDictionaries(Postings.Type.ID_FREQ, 1, 1, 20);
    }

    @Test
    public void testSmallIDDictionaries() {
        testDictionaries(Postings.Type.ID, 1, 30, 3);
    }

    @Test
    public void testSmallIDFreqDictionaries() {
        testDictionaries(Postings.Type.ID_FREQ, 1, 30, 3);
    }

    @Test
    public void testMediumIDDictionaries() {
        logger.info(String.format("medum ID"));
        testDictionaries(Postings.Type.ID, 30, 200, 10, 100);
    }

    @Test
    public void testMediumIDFreqDictionaries() {
        testDictionaries(Postings.Type.ID_FREQ, 30, 200, 10, 100);
    }

    private void testDictionaries(Postings.Type type, int startDocs, int endDocs, int nIter) {
        testDictionaries(type, startDocs, endDocs, 1, nIter);
    }
    private void testDictionaries(Postings.Type type, int startDocs, int endDocs, int stride, int nIter) {
        logger.info(String.format("Generating dictionaries with %s postings", type));
        for(int i = startDocs; i <= endDocs; i += stride) {
            for(int j = 0; j < nIter; j++) {
                logger.info(String.format("Generating dictionary with %s postings, %d docs, iteration %d", type, i, j));
                TestDictionary td = new TestDictionary(all, type, i);
                td.checkConsistency();
                td.close();
            }
        }

    }

    public static void main(String[] args) {
        Logger l = Logger.getLogger("");
        for(Handler h : l.getHandlers()) {
            h.setLevel(Level.ALL);
            h.setFormatter(new LabsLogFormatter());
        }

        org.junit.runner.JUnitCore.runClasses(com.sun.labs.minion.indexer.dictionary.DictionaryAndPostingsTest.class);
    }

    static protected class WordFreq implements Comparable<WordFreq> {

        public String word;

        public long freq;

        public WordFreq(String word, int freq) {
            this.word = word;
            this.freq = freq;
        }

        @Override
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

        private Postings.Type type;

        private int nDocs;

        private Map<String, List<DocPosting>> m =
                new HashMap<String, List<DocPosting>>();

        private MemoryDictionary<String> md;

        private DiskDictionary<String> dd;

        public TestDictionary(TestData td, Postings.Type type, int nDocs) {
            this.td = td;
            this.type = type;
            this.nDocs = nDocs;
        }

        public TestDictionary(File f) {
        }

        public MemoryDictionary<String> generateMemoryDictionary() {
            md = new MemoryDictionary<String>(new EntryFactory(type));
            OccurrenceImpl o = new OccurrenceImpl();
            for(int i = 0; i < nDocs; i++) {
                Map<String, DocPosting> doc = new HashMap<String, DocPosting>();

                //
                // Generate the number of words in this document.  We'll generate
                // documents with at least 256 words.
                int numWords = td.rand.nextInt(1024) + 256;
                o.setID(i + 1);
                for(int j = 0; j < numWords; j++) {
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

        public DiskDictionary<String> generateDiskDictionary() throws
                Exception {
            if(md == null) {
                generateMemoryDictionary();
            }
//            File dictFile = File.createTempFile("all", ".dict");
//            dictFile.deleteOnExit();
//            File postFile = File.createTempFile("all", ".post");
//            postFile.deleteOnExit();
//            DiskDictionaryOutput dout = new DiskDictionaryOutput(DictionaryTest.tmpDir);
//            dout.postStream = new OutputStream[] {new BufferedOutputStream(
//                    new FileOutputStream(postFile))};
//            dout.postOut = new PostingsOutput[] {new StreamPostingsOutput(dout.postStream[0])};
//            dout.renumber = MemoryDictionary.Renumber.RENUMBER;
//            dout.idMap = MemoryDictionary.IDMap.NONE;
//            dout.encoder = new StringNameHandler();
//            md.dump(dout);
//            dout.close();
//            raf.close();
//
//            raf = new RandomAccessFile(dictFile, "r");
//            RandomAccessFile praf = new RandomAccessFile(postFile, "r");
//            dd = new DiskDictionary<String>(new EntryFactory<String>(type),
//                    new StringNameHandler(), raf,
//                    new RandomAccessFile[]{praf});
            return dd;
        }

        /**
         * Checks whether the map in-memory and the loaded dictionary are 
         * consistent.
         * 
         * @return <code>true</code> if the dictionaries are consistent, 
         * <code>false</code> otherwise.
         */
        public boolean checkConsistency() {

            if(dd == null) {
                try {
                    generateDiskDictionary();
                } catch(Exception ex) {
                    dump();
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    ex.printStackTrace(pw);
                    pw.close();
                    fail(String.format("Failed with exception: %s\n%s", ex, sw.toString()));
                }
            }

            //
            // A map that we can clobber
            Map<String, List<DocPosting>> temp =
                    new HashMap<String, List<DocPosting>>(m);
            for(Entry<String> e : dd) {
                List<DocPosting> l = temp.remove(e.getName());
                if(l == null) {
                    dump();
                }
                assertNotNull(String.format("Couldn't find %s", e.getName()), l);
                PostingsIterator pi = e.iterator(null);
                if(pi == null) {
                    dump();
                }
                assertNotNull(String.format("Null iterator for %s", e.getName()), pi);
                for(DocPosting p : l) {
                    if(pi.next()) {
                        if(p.id != pi.getID()) {
                            dump();
                        }
                        assertTrue(String.format("Bad ID for %s expected %d got %d",
                                e.getName(),
                                p.id, pi.getID()),
                                p.id == pi.getID());
                    } else {
                        File dumpFile = dump();
                        fail(String.format("Ran out of IDs at %d for %s, dict data dumped in %s", p.id, e.getName(), dumpFile));
                    }
                }
            }

            if(!temp.isEmpty()) {
                File dumpFile = dump();
                fail(String.format("Temp still has %d entries after iteration, dict data in %s", temp.size(), dumpFile));
            }

            return true;
        }

        public File dump() {
            File f = null;
            try {
                f = File.createTempFile("words", "");
                PrintWriter pw =
                        new PrintWriter(new OutputStreamWriter(new FileOutputStream(f),
                        "utf-8"));
                pw.println(type.toString());
                pw.println(nDocs);
                for(Map.Entry<String, List<DocPosting>> e : m.entrySet()) {
                    pw.format("%s %d ", e.getKey(), e.getValue().size());
                    for(DocPosting dp : e.getValue()) {
                        pw.format("%d %d", dp.id, dp.freq);
                    }
                    pw.print('\n');
                }
                pw.close();
                logger.info(String.format("Created dump file %s", f));
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format("Error creating dump file %s", f), ex);
            }
            return f;
        }

        public void close() {
            if(dd != null) {
                try {
                    dd.dictFile.close();
                } catch(IOException ex) {
                    logger.log(Level.SEVERE, String.format("Error closing dictionary file"), ex);
                }
                if(dd.postFiles != null && dd.postFiles.length > 0) {
                    for(RandomAccessFile raf : dd.postFiles) {
                        try {
                            raf.close();
                        } catch(IOException ex) {
                            logger.log(Level.SEVERE, String.format("Error closing postings file"), ex);
                        }
                    }
                }
            }
        }
    }

    static protected class TestData {

        List<WordFreq> words = new ArrayList<WordFreq>();

        Zipf model;

        Random rand = new Random();

        long totalFreq;

        public TestData(String resourceName) throws Exception {
            this(resourceName, Integer.MAX_VALUE);
        }

        public TestData(String resourceName, int maxTerms) throws Exception {
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
                if(n >= maxTerms) {
                    break;
                }
            }
            r.close();
            Collections.sort(words);

            model = new Zipf(words.size());
        }

        /**
         * Gets the next word to put in the dictionary.
         * @return
         */
        public String getNextWord() {
            return words.get(model.getOutcome()).word;
        }
    }
}
