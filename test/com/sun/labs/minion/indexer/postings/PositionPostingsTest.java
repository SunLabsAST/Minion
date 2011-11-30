package com.sun.labs.minion.indexer.postings;

import com.sun.labs.minion.indexer.postings.io.RAMPostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.indexer.postings.io.RAMPostingsOutput;
import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.util.NanoWatch;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Arrays;
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
 * Tests for position postings.
 */
public class PositionPostingsTest {

    private static final Logger logger = Logger.getLogger(PositionPostingsTest.class.getName());

    private RAMPostingsOutput[] postOut = new RAMPostingsOutput[2];

    private RAMPostingsInput[] postIn = new RAMPostingsInput[2];

    private long[] offsets = new long[2];

    private int[] sizes = new int[2];

    Random rand = new Random();

    Zipf zipf = new Zipf(32768, rand);

    private static String[] previousData = new String[]{
        "/com/sun/labs/minion/indexer/postings/resource/position/positionpostings.1.gz",
        "/var/folders/zq/8xrjbcx915118brpx0pz9l34002wrf/T/random6926995377196554799.data"};

    public PositionPostingsTest() {
        for(int i = 0; i < postOut.length; i++) {
            postOut[i] = new RAMPostingsOutput(2048);
            postIn[i] = postOut[i].asInput();
        }
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        cleanUp();
    }

    @After
    public void tearDown() {
    }

    private void cleanUp() {
        for(RAMPostingsOutput po : postOut) {
            po.cleanUp();
        }
    }

    /**
     * Tests encoding our random data, dumping the data to a file if a failure occurs.
     * @throws Exception if there is an error
     */
    private void randomAdd(int n, int nIter) throws Exception {
        for(int i = 0; i < nIter; i++) {
            cleanUp();
            NanoWatch nw = new NanoWatch();
            TestData testData = null;
            try {
                logger.info(String.format("randomAdd iteration %d/%d max %d", i + 1, nIter, n));
                testData = new TestData(rand.nextInt(n) + 1);
                testData.paces();
            } catch(AssertionError ex) {
                File f = File.createTempFile("random", ".data");
                PrintWriter out = new PrintWriter(new FileWriter(f));
                testData.dump(out);
                out.close();
                logger.severe(String.format("Random data in %s", f));
                throw (ex);
            }
        }
    }

    /**
     * Tests the encoding of the IDs by decoding them ourselves.
     * @param p the postings we want to test
     * @param data the data that we're testing
     */
    private void postingsEncodingCheck(PositionPostings posnPostings, TestData data) throws IOException {

        ReadableBuffer idBuff = postOut[0].asInput().read(offsets[0], sizes[0]);
        ReadableBuffer posnBuff = postOut[1].asInput().read(offsets[1], sizes[1]);

        if(logger.isLoggable(Level.FINE)) {
            long bsize = sizes[0] + sizes[1];
            long nb = 8 * data.unique.size() + 4 * data.numPosns;

            logger.fine(String.format(" %d bytes for %d bytes of data,"
                    + " Compression: %.2f%%", bsize,
                    nb, 100 - ((double) bsize / nb) * 100));
        }

        int n = idBuff.byteDecode();
        assertTrue(String.format("Wrong number of IDs: %d", n),
                n == data.unique.size());
        n = idBuff.byteDecode();
        assertTrue(String.format("Wrong last ID: %d", n),
                n == data.ids[data.ids.length - 1]);
        n = idBuff.byteDecode();
        assertTrue(String.format("Wrong last position offset: %d", n),
                n == posnPostings.lastPosnOffset);

        //
        // Decode the skip table.
        try {
            n = idBuff.byteDecode();
            for(int i = 0; i < n; i++) {
                idBuff.byteDecode();
                idBuff.byteDecode();
                idBuff.byteDecode();
            }
        } catch(RuntimeException ex) {
            fail(String.format("Error decoding skip table nSkips might be %d: %s", n, ex));
        }

        int prevID = 0;
        int pp = 0;
        for(int i = 0; i < data.ids.length; i++) {
            int gap = idBuff.byteDecode();
            int freq = idBuff.byteDecode();
            int offsetGap = idBuff.byteDecode();
            int curr = prevID + gap;
            if(curr != data.ids[i]) {
                assertTrue(String.format(
                        "Incorrect ID: %d should be %d, decoded: %d", curr,
                        data.ids[i], gap),
                        curr == data.ids[i]);
            }
            if(freq != data.freqs[i]) {
                assertTrue(String.format(
                        "Incorrect freq: %d should be %d", freq,
                        data.freqs[i]),
                        freq == data.freqs[i]);
            }
            int prevPosn = 0;
            for(int j = 0; j < data.freqs[i]; j++) {
                int posnGap = posnBuff.byteDecode();
                int currPosn = prevPosn + posnGap;
                if(currPosn != data.posns[pp]) {
                    assertTrue(String.format(
                            "Incorrect position for id %d, freq %d: %d should be %d, decoded gap %d",
                            data.ids[i], data.freqs[i],
                            currPosn, data.posns[pp], posnGap),
                            currPosn == data.posns[pp]);
                }
                prevPosn = currPosn;
                pp++;
            }

            prevID = curr;
        }
    }

    private void randomAppend(int size) throws java.io.IOException {
        for(int i = 0; i < 128; i++) {
            TestData d1 = new TestData(rand.nextInt(size));
            TestData d2 = new TestData(rand.nextInt(size));
            checkAppend(d1, d2);
        }
    }

    private void checkAppend(TestData d1, TestData d2) throws java.io.IOException {


        PositionPostings p1 = d1.addData();
        PositionPostings p2 = d2.addData();
        int lastID = p1.getLastID();

        long o1[] = new long[2];
        int s1[] = new int[2];
        long o2[] = new long[2];
        int s2[] = new int[2];
        p1.write(postOut, o1, s1);
        p2.write(postOut, o2, s2);

        PostingsInput[] postIn = new PostingsInput[]{postOut[0].asInput()};
        TestData atd = new TestData(d1, d2, lastID + 1);

        try {
            PositionPostings append = new PositionPostings();
            p1 = (PositionPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ, postIn, o1, s1);
            d1.iteration(p1);
            append.append(p1, 1);
            p2 = (PositionPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ, postIn, o2, s2);
            d2.iteration(p2);
            append.append(p2, lastID + 1);
            long o3[] = new long[2];
            int s3[] = new int[2];
            append.write(postOut, o3, s3);
            append = (PositionPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ, postIn, o3, s3);
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

    /**
     * Tests simple addition of occurrences.
     */
    @Test
    public void testSimpleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 4, 7, 10},
                new int[]{1, 1, 1, 1},
                new int[]{7, 3, 2, 4});
        simple.addData();
    }

    /**
     * Tests adding IDs multiple times.
     */
    @Test
    public void testSimpleMultipleAdd() throws Exception {
        TestData simple = new TestData(
                new int[]{1, 4, 7, 8, 10, 11, 17},
                new int[]{4, 3, 2, 1, 1, 2, 1},
                new int[]{1, 2, 3, 4, 6, 10, 12, 1, 14, 7, 8, 10, 22, 36});
        PositionPostings p = simple.addData();
        simple.iteration(p);
    }

    /**
     * Tests adding IDs multiple times.
     */
    @Test
    public void testSimpleClear() throws Exception {
        TestData simple = new TestData(
                new int[]{1, 4, 7, 8, 10, 11, 17},
                new int[]{4, 3, 2, 1, 1, 2, 1},
                new int[]{1, 2, 3, 4, 6, 10, 12, 1, 14, 7, 8, 10, 22, 36});
        PositionPostings p = simple.addData();
        simple.iteration(p);
        p.write(postOut, offsets, sizes);
        p.clear();
        simple = new TestData(new int[]{1, 4, 7, 10},
                new int[]{1, 1, 1, 1},
                new int[]{7, 3, 2, 4});

        simple.addData(p);
        simple.iteration(p);
    }

    @Test
    public void smallRandomAddTest() throws Exception {
        randomAdd(16, 256);
    }

    @Test
    public void mediumRandomAddTest() throws Exception {
        randomAdd(256, 256);
    }

    @Test
    public void largeRandomAddTest() throws Exception {
        randomAdd(8192, 256);
    }

    @Test
    public void extraLargeRandomAddTest() throws Exception {
        randomAdd(1024 * 1024, 128);
    }

    /**
     * Tests encoding data that has had problems before, ensuring that we
     * don't re-introduce old problems.
     */
//    @Test
    public void testPreviousData() throws Exception {
        for(String s : previousData) {

            InputStream pdis = getClass().getResourceAsStream(s);
            if(pdis == null) {
                try {
                    pdis = new FileInputStream(s);
                } catch(FileNotFoundException ex) {
                    logger.info(String.format("Couldn't find %s", s));
                    continue;
                }
            }
            logger.info(String.format("Testing data %s", s));
            InputStream is;
            if(s.endsWith(".gz")) {
                is = new GZIPInputStream(pdis);
            } else {
                is = pdis;
            }
            cleanUp();
            TestData testData = new TestData(is);
            is.close();
            testData.paces();
        }
    }

    @Test
    public void testAppend() throws java.io.IOException {
        randomAppend(8192);
    }

    private class TestData {

        int[] ids;

        int[] freqs;

        int[] posns;
        
        int numPosns;

        Set<Integer> unique;

        public TestData(int n) {
            ids = new int[n];
            freqs = new int[n];
            posns = new int[n];
            unique = new LinkedHashSet<Integer>();

            //
            // Generate some random data.  We need to account for gaps of zero, so
            // keep track of the unique numbers with some sets.
            int prev = 0;
            for(int i = 0; i < ids.length; i++) {

                //
                // We'll use random gaps to make sure we get appropriately increasing
                // postings.
                ids[i] = prev + rand.nextInt(256) + 1;

                //
                // A zipf outcome for the frequency, which will skew towards low
                // numbers, as we would see in practice.
                int freq = zipf.getOutcome();
                freqs[i] = freq;

                //
                // Position data, which is distributed amongst a pretend 4K document.
                if(numPosns + freq >= posns.length) {
                    posns = Arrays.copyOf(posns, (numPosns + freq) * 2);
                }
                int prevPos = 0;
                int limit = freq > 4096 ? freq : 4096 / freq;
                for(int j = 0; j < freq; j++) {
                    int pos = prevPos + rand.nextInt(limit) + 1;
                    posns[numPosns++] = pos;
                    prevPos = pos;
                }
                unique.add(ids[i]);
                prev = ids[i];
            }
        }

        public TestData(int[] ids, int[] freqs, int[] posns) {
            this.ids = ids;
            this.freqs = freqs;
            this.posns = posns;
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
            line = r.readLine();
            nums = line.split("\\s");
            if(nums.length != ids.length) {
                throw new IOException(String.format("Mismatched input data: ids: %d freqs: %d", ids.length, nums.length));
            }
            freqs = new int[nums.length];
            for(int i = 0; i < ids.length; i++) {
                freqs[i] = Integer.parseInt(nums[i]);
            }

            line = r.readLine();
            nums = line.split("\\s");
            posns = new int[nums.length];
            for(int i = 0; i < nums.length; i++) {
                posns[i] = Integer.parseInt(nums[i]);
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
        
        /**
         * Puts the data through its paces.
         */
        public void paces() throws java.io.IOException {
            logger.fine(String.format("Paces for %d postings", ids.length));
            NanoWatch nw = new NanoWatch();
            nw.start();
            PositionPostings p = addData();
            nw.stop();
            logger.fine(String.format(" Adding data %.3f", nw.getLastTimeMillis()));
            nw.start();
            iteration(p);
            nw.stop();
            logger.fine(String.format(" Uncompressed iteration %.3f", nw.getLastTimeMillis()));
            nw.start();
            p.write(postOut, offsets, sizes);
            nw.stop();
            logger.fine(String.format(" Encoding and writing %.3f", nw.getLastTimeMillis()));
            nw.start();
            postingsEncodingCheck(p, this);
            nw.stop();
            logger.fine(String.format(" Encoding check %.3f", nw.getLastTimeMillis()));
            nw.start();
            p = (PositionPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ_POS, postIn, offsets, sizes);
            nw.stop();
            logger.fine(String.format(" Instantiation %.3f", nw.getLastTimeMillis()));
            nw.start();
            iteration(p);
            nw.stop();
            logger.fine(String.format(" Compressed iteration %.3f", nw.getLastTimeMillis()));
        }

        public PositionPostings addData() {
            PositionPostings p = new PositionPostings();
            return addData(p);
        }

        public PositionPostings addData(PositionPostings p) {
            FieldOccurrenceImpl o = new FieldOccurrenceImpl();
            o.setCount(1);
            int pp = 0;
            for(int i = 0; i < ids.length; i++) {
                o.setID(ids[i]);
                for(int j = 0; j < freqs[i]; j++, pp++) {
                    o.setPos(posns[pp]);
                    p.add(o);
                }
            }
            return p;
        }

        public void iteration(PositionPostings p) {

            PostingsIteratorFeatures features = new PostingsIteratorFeatures();
            features.setPositions(true);
            PostingsIteratorWithPositions pi = (PostingsIteratorWithPositions) p.iterator(features);

            assertNotNull("Null postings iterator", pi);

            if(ids.length != pi.getN()) {
                fail(String.format("Expected %d ids got %d", ids.length, pi.getN()));
            }

            int i = 0;
            int pp = 0;
            while(pi.next()) {
                int expectedID = ids[i];
                int expectedFreq = freqs[i];
                if(expectedID != pi.getID()) {
                    assertTrue(String.format(
                            "Couldn't match %d id, %d, got %d",
                            i, 
                            expectedID, 
                            pi.getID()), 
                            expectedID == pi.getID());
                }
                if(expectedFreq != pi.getFreq()) {
                    assertTrue(String.format(
                            "Incorrect %d freq %d, got %d",
                            i,
                            expectedFreq, pi.getFreq()), expectedFreq
                            == pi.getFreq());
                }
                int[] piPosn = pi.getPositions();
                for(int j = 0; j < expectedFreq; j++, pp++) {
                    if(posns[pp] != piPosn[j]) {
                        assertTrue(String.format(
                                "Incorrect position, expected %d, got %d",
                                posns[pp], piPosn[j]),
                                posns[pp] == piPosn[j]);
                    }

                }
                i++;
            }
        }

        public void dump(PrintWriter out) throws java.io.IOException {
            logger.info(String.format("Dumping %d ids", ids.length));
            for(int id : ids) {
                out.format("%d ", id);
            }
            out.println("");
            int tf = 0;
            for(int freq : freqs) {
                out.format("%d ", freq);
                tf += freq;
            }
            out.println("");
            for(int i = 0; i < tf; i++) {
                out.format("%d ", posns[i]);
            }
            out.println("");
        }
    }
}
