package com.sun.labs.minion.indexer.postings;

import com.sun.labs.minion.indexer.postings.io.RAMPostingsInput;
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
 * Tests for position postings.
 */
public class PositionPostingsTest {

    private static final Logger logger = Logger.getLogger(PositionPostingsTest.class.getName());

    private RAMPostingsOutput[] postOut = new RAMPostingsOutput[2];

    private RAMPostingsInput[] postIn = new RAMPostingsInput[2];

    private long[] offsets = new long[2];

    private int[] sizes = new int[2];

    Random rand = new Random();

    Zipf zipf = new Zipf(1024, rand);

    private static String[] previousData = new String[]{
        "/com/sun/labs/minion/indexer/postings/resource/position/positionpostings.1.gz",
        "/var/folders/zq/8xrjbcx915118brpx0pz9l34002wrf/T/random6926995377196554799.data"};

    private static String[] previousAppends = new String[]{
        "/var/folders/zq/8xrjbcx915118brpx0pz9l34002wrf/T/randomappend2869275918609327210.data",
    };
    
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
    private void randomAdd(int n, int nIter) throws Exception {
        for(int i = 0; i < nIter; i++) {
            cleanUp();
            NanoWatch nw = new NanoWatch();
            TestData testData = null;
            try {
                logger.fine(String.format("randomAdd iteration %d/%d max %d", i + 1, nIter, n));
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
    private void checkPostingsEncoding(PositionPostings posnPostings, TestData data, long[] offsets, int[] sizes) throws IOException {

        ReadableBuffer idBuff = postOut[0].asInput().read(offsets[0], sizes[0]);
        ReadableBuffer posnBuff = postOut[1].asInput().read(offsets[1], sizes[1]);

        if(logger.isLoggable(Level.FINE)) {
            long bsize = sizes[0] + sizes[1];
            long nb = 8 * data.ids.length+ 4 * data.numPosns;

            logger.finer(String.format(" %d bytes for %d bytes of data,"
                    + " Compression: %.2f%%", bsize,
                    nb, 100 - ((double) bsize / nb) * 100));
        }

        int n = idBuff.byteDecode();
        assertTrue(String.format("Wrong number of IDs: %d", n),
                n == data.ids.length);
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

    private void randomSingleAppend(int size) throws java.io.IOException {
        TestData d1 = new TestData(rand.nextInt(size)+20);
        TestData d2 = new TestData(rand.nextInt(size)+20);
        checkAppend(true, d1, d2);
    }

    private void checkAppend(boolean dump, TestData... tds) throws java.io.IOException {


        cleanUp();
        PositionPostings[] ps = new PositionPostings[tds.length];
        long[][] tdOffsets = new long[tds.length][2];
        int[][] tdSizes = new int[tds.length][2];
        int[] starts = new int[tds.length];
        starts[0] = 1;
        for(int i = 0; i < tds.length; i++) {
            ps[i] = tds[i].addData();
            ps[i].write(postOut, tdOffsets[i], tdSizes[i]);
            if(i > 0) {
                starts[i] = starts[i-1] + tds[i-1].ids[tds[i-1].ids.length-1];
            }
        }
        

        try {
        PositionPostings append = new PositionPostings();
        for(int i = 0; i < ps.length; i++) {
            ps[i] = (PositionPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ_POS, postIn, tdOffsets[i], tdSizes[i]);
            append.append(ps[i], starts[i]);
        }

        TestData atd = new TestData(tds);

            long ao[] = new long[2];
            int as[] = new int[2];
            append.write(postOut, ao, as);
            append = (PositionPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ_POS, postIn, ao, as);
            checkPostingsEncoding(append, atd, ao, as);
            atd.iteration(append);  
        } catch(AssertionError er) {
            if(dump) {
                File f = File.createTempFile("randomappend", ".data");
                logger.severe(String.format("Random data in %s", f));
                PrintWriter out = new PrintWriter(new FileWriter(f));
                out.println(tdSizes.length);
                for(TestData td : tds) {
                    td.dump(out);
                }
                out.close();
            }
            throw (er);
        } catch(RuntimeException ex) {
            if(dump) {
                File f = File.createTempFile("randomappend", ".data");
                logger.severe(String.format("Random data in %s", f));
                PrintWriter out = new PrintWriter(new FileWriter(f));
                out.println(tdSizes.length);
                for(TestData td : tds) {
                    td.dump(out);
                }
                out.close();
            }
            throw (ex);
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

//    @Test
    public void testSmallRandomAdds() throws Exception {
        randomAdd(16, 256);
    }

//    @Test
    public void testMediumRandomAdds() throws Exception {
        randomAdd(256, 256);
    }

//    @Test
    public void testLargeRandomAdds() throws Exception {
        randomAdd(8192, 256);
    }

//    @Test
    public void testExtraLargeRandomAdds() throws Exception {
        randomAdd(1024 * 1024, 128);
    }
    
    /**
     * Tests encoding data that has had problems before, ensuring that we
     * don't re-introduce old problems.
     */
//    @Test
    public void testPreviousData() throws Exception {
        for(String s : previousData) {
            BufferedReader r = getInputReader(s);
            if(r == null) {
                continue;
            }
            cleanUp();
            TestData testData = new TestData(r);
            r.close();
            testData.paces();
        }
    }

    /**
     * Tests encoding data that has had problems before, ensuring that we
     * don't re-introduce old problems.
     */
    @Test
    public void testPreviousAppends() throws Exception {
        for(String s : previousAppends) {

            BufferedReader r = getInputReader(s);
            if(r == null) {
                continue;
            }
            cleanUp();
            TestData d1 = new TestData(r);
            TestData d2 = new TestData(r);
            r.close();
            checkAppend(false, d1, d2);
        }
    }
    
    @Test
    public void testAppend() throws java.io.IOException {
        for(int i = 0; i < 256; i++) {
            logger.fine(String.format("Random append %d/%d", i+1, 256));
            randomSingleAppend(8196);
        }
    }
    
//    @Test
    public void testMultiAppend() throws java.io.IOException {
        for(int i = 0; i < 256; i++) {
            for(int j = 3; j < 10; j++) {
                TestData[] tds = new TestData[j];
                for(int k = 0; k < tds.length; k++) {
                    tds[k] = new TestData(8196);
                }
            }
        }
    }
    
//    @Test
    public void testDataDump() throws java.io.IOException {
        TestData d1 = new TestData(rand.nextInt(8192));
        TestData d2 = new TestData(rand.nextInt(8192));
        TestData ad = new TestData(d1, d2);
        File f = File.createTempFile("testdata", ".data");
        logger.info(String.format("Random data in %s", f));
        PrintWriter out = new PrintWriter(new FileWriter(f));
        d1.dump(out);
        d2.dump(out);
        out.close();
        BufferedReader r = getInputReader(f.getAbsolutePath());
        try {
            TestData nd1 = new TestData(r);
        } catch(IOException ex) {
            logger.log(Level.SEVERE, String.format("Error getting d1 back"), ex);
        }
        try {
            TestData nd2 = new TestData(r);
        } catch(IOException ex) {
            logger.log(Level.SEVERE, String.format("Error getting d2 back"), ex);
        }
    }

    private class TestData {

        int[] ids;

        int[] freqs;

        int[] posns;
        
        int numPosns;

        public TestData(int n) {
            ids = new int[n];
            freqs = new int[n];
            posns = new int[n];

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
                prev = ids[i];
            }
        }

        public TestData(int[] ids, int[] freqs, int[] posns) {
            this.ids = ids;
            this.freqs = freqs;
            this.posns = posns;
        }

        public TestData(BufferedReader r) throws java.io.IOException {
            String line = r.readLine();
            if(line == null) {
                ids = new int[0];
                return;
            }
            String[] nums = line.split("\\s");
            ids = new int[nums.length];
            for(int i = 0; i < ids.length; i++) {
                ids[i] = Integer.parseInt(nums[i]);
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
            int tf = 0;
            for(int f : freqs) {
                tf += f;
            }
            if(tf != posns.length) {
                throw new IOException(String.format("Error in input data: expected %d positions, got %d", tf, posns.length));
            }
            numPosns = posns.length;
            logger.fine(String.format("Read %d ids", ids.length));
        }

        public TestData(TestData... tds) {
            int tids = 0;
            int tnp = 0;
            for(TestData td : tds) {
                tids += td.ids.length;
                tnp += td.numPosns;
            }
            ids = new int[tids];
            freqs = new int[tids];
            posns = new int[tnp];
            int lastID = 0;
            int p = 0;
            int pp = 0;
            for(TestData td : tds) {
                for(int i = 0; i < td.ids.length; i++, p++) {
                    int m = td.ids[i] + lastID;
                    ids[p] = m;
                    freqs[p] = td.freqs[i];
                }
                lastID = ids[p-1];
                System.arraycopy(td.posns, 0, posns, pp, td.numPosns);
                pp += td.numPosns;
            }
            numPosns = posns.length;
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
            checkPostingsEncoding(p, this, offsets, sizes);
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
                                "Incorrect position for id %d at %d, freq %d, freq # %d, expected %d, got %d",
                                expectedID,
                                i,
                                expectedFreq,
                                j,
                                posns[pp], piPosn[j]),
                                posns[pp] == piPosn[j]);
                    }

                }
                i++;
            }
        }

        public void dump(PrintWriter out) throws java.io.IOException {
            logger.fine(String.format("Dumping %d ids", ids.length));
            for(int id : ids) {
                out.format("%d ", id);
            }
            out.println("");
            for(int freq : freqs) {
                out.format("%d ", freq);
            }
            out.println("");
            for(int i = 0; i < numPosns; i++) {
                out.format("%d ", posns[i]);
            }
            out.println("");
        }
    }
}
