/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.indexer.entry.EntryFactory;
import com.sun.labs.minion.indexer.entry.IndexEntry;
import com.sun.labs.minion.indexer.postings.OccurrenceImpl;
import com.sun.labs.minion.indexer.postings.Postings;
import com.sun.labs.minion.indexer.postings.Postings.Type;
import com.sun.labs.minion.indexer.postings.io.ChannelPostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsInput;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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

    private MemoryDictionary<String> generateDictionary(Postings.Type type,
                                                        int nDocs) {
        MemoryDictionary<String> md =
                new MemoryDictionary<String>(new EntryFactory(type));
        OccurrenceImpl o = new OccurrenceImpl();
        for(int i = 0; i < nDocs; i++) {
            int size = all.rand.nextInt(1024) + 1;
            o.setID(i + 1);
            for(int j = 0; j < size; j++) {
                String w = all.getNextWord();
                IndexEntry e = md.put(w);
                e.add(o);
            }
        }
        return md;
    }

    private DiskDictionary<String> generateDiskDictionary(Postings.Type type,
                                                          int nDocs) throws
            Exception {
        MemoryDictionary<String> md = generateDictionary(type, nDocs);
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

        return new DiskDictionary<String>(new EntryFactory<String>(
                Postings.Type.NONE), new StringNameHandler(), raf,
                                          new RandomAccessFile[]{praf});
    }

    @Test
    public void testSingleIDDictionary() {
        generateDictionary(Postings.Type.ID, 1);
    }

    @Test
    public void testSingleIDFreqDictionary() {
        generateDictionary(Postings.Type.ID_FREQ, 1);
    }

    @Test
    public void testSmallIDDictionary() {
        generateDictionary(Postings.Type.ID, 20);
    }

    @Test
    public void testSmallIDFreqDictionary() {
        generateDictionary(Postings.Type.ID_FREQ, 20);
    }

    @Test
    public void testIDDump() throws Exception {
        DiskDictionary dd = generateDiskDictionary(Postings.Type.ID, 50);
        dd.dictFile.close();
        dd.postFiles[0].close();
    }

    @Test
    public void testIDFreqDump() throws Exception {
        DiskDictionary dd = generateDiskDictionary(Postings.Type.ID_FREQ, 50);
        dd.dictFile.close();
        dd.postFiles[0].close();
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
            logger.info(String.format("sorting %d", words.size()));
            Collections.sort(words);
            logger.info(String.format("sorted %d", words.size()));
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
