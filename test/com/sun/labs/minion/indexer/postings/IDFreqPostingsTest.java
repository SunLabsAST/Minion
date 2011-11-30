/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.indexer.postings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import com.sun.labs.minion.indexer.postings.io.RAMPostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.RAMPostingsOutput;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for the ID-only postings.
 */
public class IDFreqPostingsTest implements PostingsTest {

    private static final Logger logger = Logger.getLogger(
            IDFreqPostingsTest.class.getName());

    private RAMPostingsOutput[] postOut = new RAMPostingsOutput[] {
        new RAMPostingsOutput(2048),
    };
    
    private RAMPostingsInput[] postIn = new RAMPostingsInput[] {
        postOut[0].asInput(),
    };

    private long[] offsets = new long[1];

    private int[] sizes = new int[1];

    Random rand = new Random();

    Zipf zipf = new Zipf(1024);

    private static String[] previousData = new String[]{};

    public IDFreqPostingsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        postOut[0].cleanUp();
    }

    @After
    public void tearDown() {
    }

    private void cleanUp() {
        for(RAMPostingsOutput po : postOut) {
            po.cleanUp();
        }
    }

    public Postings getPostings() {
        return new IDFreqPostings();
    }

    public Postings getPostings(PostingsInput[] postIn, long[] offsets, int[] sizes) throws IOException {
        return new IDFreqPostings(postIn, offsets, sizes);
    }

    private BufferedReader getInputReader(String resourceName) throws IOException {
        InputStream pdis = getClass().getResourceAsStream(resourceName);
        if(pdis == null) {
            try {
                pdis = new FileInputStream(resourceName);
            } catch(FileNotFoundException ex) {
                logger.info(String.format("Couldn't find %s", resourceName));
                return null;
            }
        }
        logger.info(String.format("Opening test data %s", resourceName));
        InputStream is;
        if(resourceName.endsWith(".gz")) {
            is = new GZIPInputStream(pdis);
        } else {
            is = pdis;
        }
        return new BufferedReader(new InputStreamReader(is));
    }

    /**
     * Tests encoding our random data, dumping the data to a file if a failure occurs.
     * @throws Exception if there is an error
     */
    private void randomAddTest(int n, int nIter) throws Exception {
        for(int i = 0; i < nIter; i++) {
            TestData testData = null;
            try {
                testData = new TestData(rand, zipf, rand.nextInt(n) + 1);
                testData.paces(postOut, offsets, sizes, postIn, this, true, false);
            } catch(AssertionError ex) {
                File f = File.createTempFile("random", ".data");
                PrintWriter out = new PrintWriter(new FileWriter(f));
                TestData.write(out, testData);
                out.close();
                logger.severe(String.format("Random data in %s", f));
                throw (ex);
            }
        }
    }

    /**
     * Tests simple addition of occurrences.
     */
    @Test
    public void testSimpleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 4, 7, 10},
                                       new int[]{1, 1, 1, 1});
        simple.addData(this);
    }

    /**
     * Tests adding IDs multiple times.
     */
    @Test
    public void testSimpleMultipleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 4, 7, 8, 10, 11, 17},
                new int[]{4, 3, 2, 1, 1, 2, 1});
        IDFreqPostings p = (IDFreqPostings) simple.addData(this);
        simple.iteration(p, true, false);
    }

    /**
     * Tests adding IDs multiple times.
     */
    @Test
    public void testSimpleClear() throws Exception {
        RAMPostingsOutput po = new RAMPostingsOutput();
        TestData simple = new TestData(new int[]{1, 4, 7, 8, 10, 11, 17},
                new int[]{4, 3, 2, 1, 1, 2, 1});
        IDFreqPostings p = (IDFreqPostings) simple.addData(this);
        simple.iteration(p, true, false);
        p.write(new PostingsOutput[] {po}, new long[1], new int[1]);
        p.clear();
        simple = new TestData(new int[]{1, 4, 7, 10},
                new int[]{1, 1, 1, 1});
        simple.addData(p);
        simple.iteration(p, true, false);
    }

    public void checkPostingsEncoding(Postings p, TestData testData, long[] offsets, int[] sizes) throws IOException {

        IDFreqPostings idp = (IDFreqPostings) p;
        ReadableBuffer idBuff = postIn[0].read(offsets[0], sizes[0]);


        long bsize = sizes[0];
        long nb = 4 * testData.ids.length;

        if(logger.isLoggable(Level.FINE)) {
            logger.fine(String.format("%d bytes in buffer for %d bytes of ids."
                    + " Compression: %.2f%%", bsize,
                    nb, 100 - ((double) bsize / nb) * 100));
        }

        int n = idBuff.byteDecode();
        assertTrue(String.format("Wrong number of IDs: %d", n),
                n == testData.ids.length);
        n = idBuff.byteDecode();
        assertTrue(String.format("Wrong last ID: %d", n),
                n == testData.ids[testData.ids.length - 1]);

        //
        // Decode the skip table.
        try {
            n = idBuff.byteDecode();
            for(int i = 0; i < n; i++) {
                idBuff.byteDecode();
                idBuff.byteDecode();
            }
        } catch(RuntimeException ex) {
            fail(String.format("Error decoding skip table nSkips might be %d: %s", n, ex));
        }

        int prev = 0;
        for(int i = 0; i < testData.ids.length; i++) {
            int gap = idBuff.byteDecode();
            int freq = idBuff.byteDecode();
            int curr = prev + gap;
            if(curr != testData.ids[i]) {
                assertTrue(String.format(
                        "Incorrect ID: %d should be %d, decoded: %d", curr,
                        testData.ids[i], gap),
                        curr == testData.ids[i]);
            }
            if(freq != testData.freqs[i]) {
                assertTrue(String.format(
                        "Incorrect freq: %d should be %d", freq,
                        testData.freqs[i]),
                        freq == testData.freqs[i]);
            }

            prev = curr;
        }
    }
    
    private Postings checkAppend(boolean dump, TestData... tds) throws java.io.IOException {

        cleanUp();
        IDFreqPostings[] ps = new IDFreqPostings[tds.length];
        long[][] tdOffsets = new long[tds.length][1];
        int[][] tdSizes = new int[tds.length][1];
        int[] starts = new int[tds.length];
        starts[0] = 1;
        for(int i = 0; i < tds.length; i++) {
            ps[i] = (IDFreqPostings) tds[i].addData(this);
            ps[i].write(postOut, tdOffsets[i], tdSizes[i]);
            if(i > 0) {
                starts[i] = starts[i - 1] + tds[i - 1].maxDocID;
            }
        }

        try {
            IDFreqPostings append = new IDFreqPostings();
            for(int i = 0; i < ps.length; i++) {
                ps[i] = (IDFreqPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ, postIn, tdOffsets[i], tdSizes[i]);
                append.append(ps[i], starts[i]);
            }

            TestData atd = new TestData(tds);

            long ao[] = new long[2];
            int as[] = new int[2];
            append.write(postOut, ao, as);
            append = (IDFreqPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ, postIn, ao, as);
            checkPostingsEncoding(append, atd, ao, as);
            atd.iteration(append, true, false);
            return append;
        } catch(AssertionError er) {
            if(dump) {
                File f = File.createTempFile("randomappend", ".data");
                logger.severe(String.format("Random data in %s", f));
                PrintWriter out = new PrintWriter(new FileWriter(f));
                TestData.write(out, tds);
                out.close();
            }
            throw (er);
        } catch(RuntimeException ex) {
            if(dump) {
                File f = File.createTempFile("randomappend", ".data");
                logger.severe(String.format("Random data in %s", f));
                PrintWriter out = new PrintWriter(new FileWriter(f));
                out.println(tdSizes.length);
                TestData.write(out, tds);
                out.close();
            }
            throw (ex);
        }
    }

    @Test
    public void testSmallRandomAdd() throws Exception {
        randomAddTest(16, 128);
    }

    @Test
    public void testMediumRandomAdd() throws Exception {
        randomAddTest(256, 128);
    }

    @Test
    public void testLargeRandomAdd() throws Exception {
        randomAddTest(8192, 64);
    }

//    @Test
    public void testExtraLargeRandomAdd() throws Exception {
        randomAddTest(1024 * 1024, 32);
    }

    /**
     * Tests encoding data that has had problems before, ensuring that we
     * don't re-introduce old problems.
     */
    @Test
    public void previousDataTest() throws Exception {
        for(String s : previousData) {
            BufferedReader r = getInputReader(s);
            if(r == null) {
                continue;
            }
            cleanUp();
            TestData[] testData = TestData.read(r);
            r.close();
            for(TestData td : testData) {
                td.paces(postOut, offsets, sizes, postIn, this, true, false);
            }
        }
    }

    /**
     * Test writing then reading postings.
     */
    @Test
    public void testWriteAndRead() throws java.io.IOException {
        TestData testData = new TestData(rand, zipf, 8192);
        IDFreqPostings p = (IDFreqPostings) testData.addData(this);
        p.write(postOut, offsets, sizes);
        IDFreqPostings idp2 = (IDFreqPostings) Postings.Type.getPostings(
                Postings.Type.ID_FREQ, postIn,
                offsets, sizes);
        testData.iteration(idp2);
    }

    /**
     * Test of merge method, of class IDFreqPostings.
     */
    @Test
    public void testAppend() throws java.io.IOException {
        testRandomAppend(8192);
    }
    
    private void testRandomAppend(int size) throws java.io.IOException {
        for(int i = 0; i < 256; i++) {
            TestData d1 = new TestData(rand, zipf, rand.nextInt(1024 * 16));
            TestData d2 = new TestData(rand, zipf, rand.nextInt(1024 * 16));
            checkAppend(true, d1, d2);
        }
    }

}
