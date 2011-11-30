/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.indexer.postings;

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
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
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
public class IDFreqPostingsTest {

    private static final Logger logger = Logger.getLogger(
            IDFreqPostingsTest.class.getName());

    private RAMPostingsOutput[] postOut = new RAMPostingsOutput[1];

    private long[] offsets = new long[1];

    private int[] sizes = new int[1];

    Random rand = new Random();

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
    }

    @After
    public void tearDown() {
    }

    /**
     * Tests encoding our random data, dumping the data to a file if a failure occurs.
     * @throws Exception if there is an error
     */
    private void randomAddTest(int n, int nIter) throws Exception {
        for(int i = 0; i < nIter; i++) {
            TestData r = null;
            try {
                logger.info(String.format("Iteration %d", i));
                r = new TestData(rand.nextInt(n) + 1, 64000);
                IDFreqPostings p = r.encode();
                r.iteration(p);
                testPostingsEncoding(p, r);
            } catch(AssertionError ex) {
                File f = File.createTempFile("random", ".data");
                PrintWriter out = new PrintWriter(new FileWriter(f));
                r.dump(out);
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
        simple.encode();
    }

    /**
     * Tests adding IDs multiple times.
     */
    @Test
    public void testSimpleMultipleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 4, 7, 8, 10, 11, 17},
                new int[]{4, 3, 2, 1, 1, 2, 1});
        IDFreqPostings p = simple.encode();
        simple.iteration(p);
    }

    /**
     * Tests adding IDs multiple times.
     */
    @Test
    public void testSimpleClear() throws Exception {
        RAMPostingsOutput po = new RAMPostingsOutput();
        TestData simple = new TestData(new int[]{1, 4, 7, 8, 10, 11, 17},
                new int[]{4, 3, 2, 1, 1, 2, 1});
        IDFreqPostings p = simple.encode();
        simple.iteration(p);
        p.write(new PostingsOutput[] {po}, new long[1], new int[1]);
        p.clear();
        simple = new TestData(new int[]{1, 4, 7, 10},
                new int[]{1, 1, 1, 1});
        simple.encode(p);
        simple.iteration(p);
    }

    /**
     * Tests the encoding of the IDs by decoding them ourselves.
     * @param p the postings we want to test
     * @param data the data that we're testing
     */
    private void testPostingsEncoding(IDFreqPostings idp, TestData data) throws IOException {
        idp.write(postOut, offsets, sizes);

        RAMPostingsInput postIn = postOut[0].asInput();
        ReadableBuffer postBuff = postIn.read(offsets[0], sizes[0]);


        long bsize = postBuff.position();
        long nb = 4 * data.unique.size();

        logger.info(String.format("%d bytes in buffer for %d bytes of ids."
                + " Compression: %.2f%%", bsize,
                nb, 100 - ((double) bsize / nb) * 100));

        int n = postBuff.byteDecode();
        assertTrue(String.format("Wrong number of IDs: %d", n),
                n == data.unique.size());
        n = postBuff.byteDecode();
        assertTrue(String.format("Wrong last ID: %d", n),
                n == data.ids[data.ids.length - 1]);

        //
        // Decode the skip table.
        try {
            n = postBuff.byteDecode();
            for(int i = 0; i < n; i++) {
                postBuff.byteDecode();
                postBuff.byteDecode();
            }
        } catch(RuntimeException ex) {
            fail(String.format("Error decoding skip table nSkips might be %d: %s", n, ex));
        }

        int prev = 0;
        for(int i = 0; i < data.ids.length; i++) {
            int gap = postBuff.byteDecode();
            int freq = postBuff.byteDecode();
            int curr = prev + gap;
            assertTrue(String.format(
                    "Incorrect ID: %d should be %d, decoded: %d", curr,
                    data.ids[i], gap),
                       curr == data.ids[i]);
            assertTrue(String.format(
                    "Incorrect freq: %d should be %d", freq,
                    data.freqs[i]),
                       freq == data.freqs[i]);

            prev = curr;
        }
    }

    @Test
    public void smallRandomAddTest() throws Exception {
        randomAddTest(16, 128);
    }

    @Test
    public void mediumRandomAddTest() throws Exception {
        randomAddTest(256, 128);
    }

    @Test
    public void largeRandomAddTest() throws Exception {
        randomAddTest(8192, 64);
    }

    @Test
    public void extraLargeRandomAddTest() throws Exception {
        randomAddTest(1024 * 1024, 32);
    }

    /**
     * Tests encoding data that has had problems before, ensuring that we
     * don't re-introduce old problems.
     */
    @Test
    public void previousDataTest() throws Exception {
        for(String s : previousData) {

            InputStream pdis = getClass().getResourceAsStream(s);
            if(pdis == null) {
                logger.info(String.format("Couldn't find %s", s));
                continue;
            }
            logger.info(String.format("Testing data %s", s));
            GZIPInputStream gzis = new GZIPInputStream(pdis);
            TestData td = new TestData(gzis);
            gzis.close();
            IDFreqPostings p = td.encode();
            td.iteration(p);
            testPostingsEncoding(p, td);
        }
    }

    /**
     * Test writing then reading postings.
     */
    @Test
    public void testWriteAndRead() throws java.io.IOException {
        TestData td = new TestData(8192, 64000);
        IDFreqPostings p = td.encode();
        File of = File.createTempFile("single", ".post");
        of.deleteOnExit();
        p.write(postOut, offsets, sizes);
        IDFreqPostings p2 = (IDFreqPostings) Postings.Type.getPostings(Postings.Type.ID,
                new PostingsInput[] {postOut[0].asInput()}, offsets, sizes);
        td.iteration(p2);
    }

    /**
     * Test of merge method, of class IDFreqPostings.
     */
    @Test
    public void testAppend() throws java.io.IOException {
        testRandomAppend(8192);
    }
    
    private void testRandomAppend(int size) throws java.io.IOException {
        for(int i = 0; i < 128; i++) {
            TestData d1 = new TestData(rand.nextInt(size));
            TestData d2 = new TestData(rand.nextInt(size));
            testAppend(d1, d2);
        }
    }
    private void testAppend(TestData d1, TestData d2) throws java.io.IOException {


        IDFreqPostings p1 = d1.encode();
        IDFreqPostings p2 = d2.encode();
        int lastID = p1.getLastID();

        long o1[] = new long[1];
        int s1[] = new int[1];
        long o2[] = new long[1];
        int s2[] = new int[1];
        p1.write(postOut, o1, s1);
        p2.write(postOut, o2, s2);

        PostingsInput[] postIn = new PostingsInput[]{postOut[0].asInput()};
        TestData atd = new TestData(d1, d2, lastID + 1);

        try {
            IDFreqPostings append = new IDFreqPostings();
            p1 = (IDFreqPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ, postIn, o1, s1);
            d1.iteration(p1);
            append.append(p1, 1);
            p2 = (IDFreqPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ, postIn, o2, s2);
            d2.iteration(p2);
            append.append(p2, lastID + 1);
            long o3[] = new long[1];
            int s3[] = new int[1];
            append.write(postOut, o3, s3);
            append = (IDFreqPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ, postIn, o3, s3);
            atd.iteration(append);
        } catch(AssertionError er) {
            File f = File.createTempFile("randomappend", ".data");
            PrintWriter out = new PrintWriter(new FileWriter(f));
            d1.dump(out);
            d2.dump(out);
            atd.dump(out);
            out.close();
            logger.severe(String.format("Random data in %s", f));
            throw (er);
        }
    }

    private static class TestData {

        int[] ids;

        int[] freqs;

        Set<Integer> unique;

        public TestData(int n) {
            this(n, 64000);
        }

        public TestData(int n, int maxFreq) {
            ids = new int[n];
            freqs = new int[n];
            unique = new LinkedHashSet<Integer>();

            //
            // Generate some random data.  We need to account for gaps of zero, so
            // keep track of the unique numbers with some sets.
            Random r = new Random();
            Zipf z = new Zipf(maxFreq, r);
            int prev = 0;
            for(int i = 0; i < ids.length; i++) {

                //
                // We'll use random gaps to make sure we get appropriately increasing
                // postings.
                ids[i] = prev + r.nextInt(256) + 1;

                //
                // A zipf outcome for the frequency, which will skew towards low
                // numbers, as we would see in practice.
                freqs[i] = z.getOutcome();
                prev = ids[i];
                unique.add(prev);
            }
        }

        public TestData(int[] ids, int[] freqs) {
            this.ids = ids;
            this.freqs = freqs;
            unique = new LinkedHashSet<Integer>();
            for(int x : ids) {
                unique.add(x);
            }
        }

        public TestData(InputStream s) throws java.io.IOException {
            BufferedReader r = new BufferedReader(new InputStreamReader(s));
            String line = r.readLine();
            unique = new LinkedHashSet<Integer>();
            if(line == null) {
                ids = new int[0];
                return;
            }
            String[] nums = line.split("\\s");
            ids = new int[nums.length];
            for(int i = 0; i < ids.length; i++) {
                ids[i] = Integer.parseInt(nums[i]);
                unique.add(ids[i]);
            }
        }

        public TestData(TestData d1, TestData d2, int start) {
            ids = new int[d1.ids.length + d2.ids.length];
            freqs = new int[ids.length];
            unique = new LinkedHashSet<Integer>();
            int p = 0;
            for(int i = 0; i < d1.ids.length; i++) {
                ids[p] = d1.ids[i];
                freqs[p++] = d1.freqs[i];
                unique.add(d1.ids[i]);
            }
            for(int i = 0; i < d2.ids.length; i++) {
                int m = d2.ids[i] + start - 1;
                ids[p] = m;
                freqs[p++] = d2.freqs[i];
                unique.add(m);
            }
        }

        public IDFreqPostings encode() {
            IDFreqPostings p = new IDFreqPostings();
            return encode(p);
        }
        
        public IDFreqPostings encode(IDFreqPostings p) {
            OccurrenceImpl o = new OccurrenceImpl();
            o.setCount(1);
            logger.fine(String.format("Encoding %d ids (%d unique)",
                                      ids.length,
                                      unique.size()));
            for(int i = 0; i < ids.length; i++) {
                o.setID(ids[i]);
                for(int j = 0; j < freqs[i]; j++) {
                    p.add(o);
                }
            }
            return p;
        }

        public void iteration(IDFreqPostings p) {
            PostingsIterator pi = p.iterator(null);

            assertNotNull("Null postings iterator", pi);

            if(ids.length != pi.getN()) {
                fail(String.format("Expected %d ids got %d",
                                   ids.length, pi.getN()));
            }

            int i = 0;
            while(pi.next()) {
                int expectedID = ids[i];
                int expectedFreq = freqs[i];
                assertTrue(String.format(
                        "Couldn't match id %d, got %d",
                        expectedID, pi.getID()), expectedID == pi.getID());
                assertTrue(String.format(
                        "Incorrect freq %d, got %d",
                        expectedFreq, pi.getFreq()), expectedFreq
                        == pi.getFreq());
                i++;
            }
        }

        public void dump(PrintWriter out) throws java.io.IOException {
            for(int id : ids) {
                out.format("%d ", id);
            }
            out.println("");
            for(int freq : freqs) {
                out.format("%d ", freq);
            }
            out.println("");
        }
    }
}
