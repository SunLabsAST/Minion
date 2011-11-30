package com.sun.labs.minion.indexer.postings;

import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import java.util.ArrayList;
import java.util.List;
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
public class PositionPostingsTest implements PostingsTest {

    private static final Logger logger = Logger.getLogger(PositionPostingsTest.class.getName());

    private RAMPostingsOutput[] postOut = new RAMPostingsOutput[2];

    private RAMPostingsInput[] postIn = new RAMPostingsInput[2];

    private long[] offsets = new long[2];

    private int[] sizes = new int[2];

    Random rand = new Random();

    Zipf zipf = new Zipf(1024, rand);

    private static String[] previousData = new String[]{};

    private static String[] previousAppends = new String[]{
        "/var/folders/zq/8xrjbcx915118brpx0pz9l34002wrf/T/randomappend81817644036518208.data",};

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
                testData = new TestData(rand, zipf, rand.nextInt(n) + 1);
                testData.paces(postOut, offsets, sizes, postIn, this, true, true);
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

    public Postings getPostings() {
        return new PositionPostings();
    }

    public Postings getPostings(PostingsInput[] postIn, long[] offsets, int[] sizes) throws java.io.IOException {
        return Postings.Type.getPostings(Postings.Type.ID_FREQ_POS, postIn, offsets, sizes);
    }

    /**
     * Tests the encoding of the IDs by decoding them ourselves.
     * @param p the postings we want to test
     * @param data the data that we're testing
     */
    public void checkPostingsEncoding(Postings p, TestData data, long[] offsets, int[] sizes) throws IOException {

        PositionPostings posnPostings = (PositionPostings) p;
        ReadableBuffer idBuff = postOut[0].asInput().read(offsets[0], sizes[0]);
        ReadableBuffer posnBuff = postOut[1].asInput().read(offsets[1], sizes[1]);

        if(logger.isLoggable(Level.FINE)) {
            long bsize = sizes[0] + sizes[1];
            long nb = 8 * data.ids.length + 4 * data.numPosns;

            logger.finer(String.format(" %d bytes for %d bytes of data,"
                    + " Compression: %.2f%%", bsize,
                    nb, 100 - ((double) bsize / nb) * 100));
        }

        int n = idBuff.byteDecode();
        assertTrue(String.format("Wrong number of IDs: %d, expected %d", n, data.ids.length),
                n == data.ids.length);
        n = idBuff.byteDecode();
        assertTrue(String.format("Wrong last ID: %d, expected %d", n, data.ids[data.ids.length - 1]),
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
        TestData d1 = new TestData(rand, zipf, rand.nextInt(size) + 20);
        TestData d2 = new TestData(rand, zipf, rand.nextInt(size) + 20);
        checkAppend(true, d1, d2);
    }

    private PositionPostings checkAppend(boolean dump, TestData... tds) throws java.io.IOException {

        PositionPostings[] ps = new PositionPostings[tds.length];
        long[][] tdOffsets = new long[tds.length][2];
        int[][] tdSizes = new int[tds.length][2];
        int[] starts = new int[tds.length];
        starts[0] = 1;
        for(int i = 0; i < tds.length; i++) {
            ps[i] = (PositionPostings) tds[i].addData(this);
            ps[i].write(postOut, tdOffsets[i], tdSizes[i]);
            if(i > 0) {
                starts[i] = starts[i - 1] + tds[i - 1].maxDocID;
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
            atd.iteration(append, true, true);
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

    /**
     * Tests simple addition of occurrences.
     */
//    @Test
    public void testSimpleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 4, 7, 10},
                new int[]{1, 1, 1, 1},
                new int[]{7, 3, 2, 4});
        simple.addData(this);
    }

    /**
     * Tests adding IDs multiple times.
     */
//    @Test
    public void testSimpleMultipleAdd() throws Exception {
        TestData simple = new TestData(
                new int[]{1, 4, 7, 8, 10, 11, 17},
                new int[]{4, 3, 2, 1, 1, 2, 1},
                new int[]{1, 2, 3, 4, 6, 10, 12, 1, 14, 7, 8, 10, 22, 36});
        PositionPostings p = (PositionPostings) simple.addData(this);
        simple.iteration(p, true, true);
    }

    /**
     * Tests adding IDs multiple times.
     */
//    @Test
    public void testSimpleClear() throws Exception {
        TestData simple = new TestData(
                new int[]{1, 4, 7, 8, 10, 11, 17},
                new int[]{4, 3, 2, 1, 1, 2, 1},
                new int[]{1, 2, 3, 4, 6, 10, 12, 1, 14, 7, 8, 10, 22, 36});
        PositionPostings p = (PositionPostings) simple.addData(this);
        simple.iteration(p, true, true);
        p.write(postOut, offsets, sizes);
        p.clear();
        simple = new TestData(new int[]{1, 4, 7, 10},
                new int[]{1, 1, 1, 1},
                new int[]{7, 3, 2, 4});

        simple.addData(p);
        simple.iteration(p, true, true);
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
            TestData[] testData = TestData.read(r);
            r.close();
            for(TestData td : testData) {
                td.paces(postOut, offsets, sizes, postIn, this, true, true);
            }
        }
    }

    /**
     * Tests encoding data that has had problems before, ensuring that we
     * don't re-introduce old problems.
     */
//    @Test
    public void testPreviousAppends() throws Exception {
        for(String s : previousAppends) {

            BufferedReader r = getInputReader(s);
            if(r == null) {
                continue;
            }
            cleanUp();
            TestData[] tds = TestData.read(r);
            r.close();
            checkAppend(false, tds);
        }
    }

//    @Test
    public void testAppend() throws java.io.IOException {
        for(int i = 0; i < 256; i++) {
            logger.fine(String.format("Random append %d/%d", i + 1, 256));
            randomSingleAppend(8196);
        }
    }

//    @Test
    public void testStepUpAppend() throws java.io.IOException {
        for(int i = 1; i < 256; i += 2) {
            TestData td1 = new TestData(rand, zipf, i);
            for(int j = 1; j < 128; j++) {
                logger.fine(String.format("Step up %d/%d", i, j));
                TestData td2 = new TestData(rand, zipf, j);
                checkAppend(true, td1, td2);
            }
        }
    }

//    @Test
    public void testMultiAppend() throws java.io.IOException {
        for(int i = 0; i < 128; i++) {
            for(int j = 3; j < 20; j++) {
                logger.fine(String.format("multiAppend %d/%d iterations length %d", i + 1, 256, j));
                TestData[] tds = new TestData[j];
                for(int k = 0; k < tds.length; k++) {
                    tds[k] = new TestData(rand, zipf, 8196);
                }
                checkAppend(true, tds);
            }
        }
    }

//    @Test
    public void testTestDataAppend() {
        TestData[] mltd = new TestData[5];
        for(int i = 0; i < mltd.length; i++) {
            TestData[] tds = new TestData[3];
            for(int j = 0; j < tds.length; j++) {
                tds[j] = new TestData(rand, zipf, 8196);
            }
            mltd[i] = new TestData(tds);
        }
        TestData atd = new TestData(mltd);
    }

    @Test
    public void testMultiLevelAppend() throws java.io.IOException {
        int nIter = 128;
        for(int iter = 0; iter < nIter; iter++) {
            cleanUp();
            logger.fine(String.format("Iter %d/%d", iter + 1, nIter));
            PositionPostings[] pp = new PositionPostings[5];
            TestData[] mltd = new TestData[5];
            for(int i = 0; i < 5; i++) {
                TestData[] td = new TestData[5];
                for(int j = 0; j < 5; j++) {
                    td[j] = new TestData(rand, zipf, 1024);
                }
                pp[i] = checkAppend(true, td);
                mltd[i] = new TestData(td);
            }

            PositionPostings mlAppend = new PositionPostings();
            int start = 1;
            for(int i = 0; i < pp.length; i++) {
                mlAppend.append(pp[i], start);
                start += mltd[i].maxDocID;
            }
            long ao[] = new long[2];
            int as[] = new int[2];
            mlAppend.write(postOut, ao, as);
            mlAppend = (PositionPostings) Postings.Type.getPostings(Postings.Type.ID_FREQ_POS, postIn, ao, as);
            TestData atd = new TestData(mltd);
            checkPostingsEncoding(mlAppend, atd, ao, as);
            atd.iteration(mlAppend, true, true);
        }
    }

    //    @Test
    public void testDataDump() throws java.io.IOException {
        TestData d1 = new TestData(rand, zipf, rand.nextInt(8192));
        TestData d2 = new TestData(rand, zipf, rand.nextInt(8192));
        TestData ad = new TestData(d1, d2);
        File f = File.createTempFile("testdata", ".data");
        logger.info(String.format("Random data in %s", f));
        PrintWriter out = new PrintWriter(new FileWriter(f));
        TestData.write(out, d1, d2);
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
}
