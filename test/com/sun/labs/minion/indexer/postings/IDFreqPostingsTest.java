/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.indexer.postings;

import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsInput;
import com.sun.labs.minion.indexer.postings.io.StreamPostingsOutput;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
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
public class IDFreqPostingsTest {

    private static final Logger logger = Logger.getLogger(
            IDFreqPostingsTest.class.getName());

    public IDFreqPostingsTest() {
    }
    private static String[] previousData = new String[]{};

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
        Random rand = new Random();
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
        simple.encode();
    }

    /**
     * Tests the encoding of the IDs by decoding them ourselves.
     * @param p the postings we want to test
     * @param data the data that we're testing
     */
    private void testPostingsEncoding(IDFreqPostings idp, TestData data) {
        WriteableBuffer[] buffs = idp.getBuffers();
        assertTrue(String.format("Wrong number of buffers: %d", buffs.length),
                   buffs.length == 2);

        long bsize = buffs[0].position() + buffs[1].position();
        long nb = 8 * data.unique.size();

        logger.info(String.format("%d bytes in buffers (%d + %d) for %d bytes of ids."
                + " Compression: %.2f%%", bsize,
                                  buffs[0].position(), buffs[1].position(),
                                  nb, 100 - ((double) bsize / nb) * 100));

        ReadableBuffer rb = buffs[0].getReadableBuffer();

        int n = rb.byteDecode();
        assertTrue(String.format("Wrong number of IDs: %d", n), n == data.unique.
                size());
        n = rb.byteDecode();
        assertTrue(String.format("Wrong last ID: %d", n),
                   n == data.ids[data.ids.length - 1]);
        rb = buffs[1].getReadableBuffer();

        int prev = 0;
        for(int i = 0; i < data.ids.length; i++) {
            int gap = rb.byteDecode();
            int freq = rb.byteDecode();
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
        randomAddTest(16);
    }

    @Test
    public void mediumRandomAddTest() throws Exception {
        randomAddTest(256);
    }

    @Test
    public void largeRandomAddTest() throws Exception {
        randomAddTest(8192);
    }

    @Test
    public void extraLargeRandomAddTest() throws Exception {
        randomAddTest(1024 * 1024);
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

    private long writePostings(File of, IDFreqPostings p) throws
            java.io.IOException {
        OutputStream os = new FileOutputStream(of);
        PostingsOutput postOut = new StreamPostingsOutput(os);
        long ret = postOut.write(p);
        postOut.flush();
        os.close();
        return ret;
    }

    private IDFreqPostings readPostings(File f, long size) throws
            java.io.IOException {
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        PostingsInput postIn = new StreamPostingsInput(raf, 8192);
        return (IDFreqPostings) postIn.read(Postings.Type.ID_FREQ, 0L,
                                            (int) size);
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
        long size = writePostings(of, p);
        IDFreqPostings p2 = readPostings(of, size);
        td.iteration(p2);
    }

    /**
     * Test of merge method, of class IDFreqPostings.
     */
    @Test
    public void testAppend() throws java.io.IOException {
        testAppend(8192);
    }
    
    private void testAppend(int size) throws java.io.IOException {
        Random rand = new Random();
        for(int i = 0; i < 128; i++) {

            TestData d1 = new TestData(rand.nextInt(size));
            IDFreqPostings idp1 = d1.encode();
            TestData d2 = new TestData(rand.nextInt(size));
            IDFreqPostings idp2 = d2.encode();

            int lastID = idp1.getLastID();
            
            //
            // Write out the data so it can be read in.
            File of = File.createTempFile("single", ".post");
            of.deleteOnExit();
            StreamPostingsOutput po = new StreamPostingsOutput(new FileOutputStream(
                    of));
            long s1 = po.write(idp1);
            long s2 = po.write(idp2);
            po.close();

            //
            // Read the data and make sure that it's OK.
            RandomAccessFile raf = new RandomAccessFile(of, "r");
            StreamPostingsInput pi = new StreamPostingsInput(raf, 8192);
            idp1 = (IDFreqPostings) pi.read(Postings.Type.ID_FREQ, 0, (int) s1);
            d1.iteration(idp1);
            idp2 = (IDFreqPostings) pi.read(Postings.Type.ID_FREQ, s1, (int) s2);
            d2.iteration(idp2);
            raf.close();
            
            //
            // Now append the data.
            IDFreqPostings append = new IDFreqPostings();
            append.append(idp1, 1);
            append.append(idp2, lastID + 1);
            logger.info(String.format("Writing appended postings"));
            po = new StreamPostingsOutput(new FileOutputStream(of));
            s1 = po.write(append);
            po.close();
            logger.info(String.format("Postings written: %d", s1));
            raf = new RandomAccessFile(of, "r");
            pi = new StreamPostingsInput(raf, 8192);
            append = (IDFreqPostings) pi.read(Postings.Type.ID_FREQ, 0, (int) s1);
            raf.close();
            TestData atd = new TestData(d1, d2, lastID + 1);
            atd.iteration(append);
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
