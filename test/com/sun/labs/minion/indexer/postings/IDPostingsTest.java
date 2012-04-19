package com.sun.labs.minion.indexer.postings;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.indexer.postings.io.RAMPostingsInput;
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
public class IDPostingsTest implements PostingsTest {

    private static final Logger logger = Logger.getLogger(
            IDPostingsTest.class.getName());
    
    private RAMPostingsOutput[] postOut = new RAMPostingsOutput[] {
        new RAMPostingsOutput(2048),
    };
    
    private RAMPostingsInput[] postIn = new RAMPostingsInput[] {
        postOut[0].asInput(),
    };
    
    private long[] offsets = new long[1];
    
    private int[] sizes = new int[1];

    Random rand = new Random();
    
    Zipf zipf = new Zipf(1024, rand);
    
    public IDPostingsTest() {
    }
    
    private static String[] previousData = new String[]{
        "/com/sun/labs/minion/indexer/postings/resource/2.data.gz",
        "/com/sun/labs/minion/indexer/postings/resource/1.data.gz",};

    private static String[] previousAppends = new String[]{
        "/com/sun/labs/minion/indexer/postings/resource/1.append.gz"
    };

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

    @Override
    public Postings getPostings() {
        return new IDPostings();
    }

    @Override
    public Postings getPostings(PostingsInput[] postIn, long[] offsets, int[] sizes) throws IOException {
        return new IDPostings(postIn, offsets, sizes);
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
    private void randomAddTest(int n) throws Exception {
        for(int i = 0; i < 128; i++) {
            TestData testData = null;
            try {
                testData = new TestData(rand, zipf, rand.nextInt(n) + 1);
                IDPostings p = (IDPostings) testData.addData(this);
                testData.paces(postOut, offsets, sizes, postIn, this, false, false);
            } catch(RuntimeException ex) {
                File f = File.createTempFile("random", ".data");
                PrintWriter out = new PrintWriter(new FileWriter(f));
                TestData.write(out, testData);
                out.close();
                logger.severe(String.format("Random data in %s", f));
                throw (ex);
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
//    @Test
    public void testSimpleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 4, 7, 10});
        simple.addData(this);
    }

    /**
     * Tests adding IDs multiple times.
     */
//    @Test
    public void testSimpleMultipleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 1, 1,
                                                 1, 4, 4, 4, 7, 7, 8,
                                                 10, 11, 11, 17});
        simple.addData(this);
    }

    @Override
    public void checkPostingsEncoding(Postings p, TestData testData, long[] offsets, int[] sizes) throws IOException {
        
        IDPostings idp = (IDPostings) p;
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
        for(Integer x : testData.ids) {
            int gap = idBuff.byteDecode();
            int curr = prev + gap;
            if(curr != x) {
                assertTrue(String.format(
                        "Incorrect ID: %d should be %d, decoded: %d", curr, x, gap),
                        curr == x);
            }
            prev = curr;
        }
    }

    /**
     * Tests simple addition of occurrences.
     */
//    @Test
    public void testSimpleClear() throws Exception {
        TestData simple = new TestData(new int[]{1, 4, 7, 10});
        IDPostings p = (IDPostings) simple.addData(this);
        simple.iteration(p);
        p.clear();
        simple = new TestData(new int[] {3,6,9,12});
        simple.addData(p);
        simple.iteration(p);
    }
    
//    @Test
    public void testRandomClear() throws Exception {
        for(int i = 0; i < 128; i++) {
            TestData testData = null;
            try {
                testData = new TestData(rand, zipf, rand.nextInt(2000) + 1);
                IDPostings p = (IDPostings) testData.addData(this);
                testData.iteration(p);
                p.clear();
                testData = new TestData(rand, zipf, rand.nextInt(2000) + 1);
                p = (IDPostings) testData.addData(p);
                testData.iteration(p);        
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

//    @Test
//    public void extraLargeRandomAddTest() throws Exception {
//        randomAddTest(1024 * 1024);
//    }

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
                td.paces(postOut, offsets, sizes, postIn, this, false, false);
            }
        }
    }

    /**
     * Test writing then reading postings.
     */
    @Test
    public void testWriteAndRead() throws java.io.IOException {
        TestData testData = new TestData(rand, zipf, 8192);
        IDPostings p = (IDPostings) testData.addData(this);
        p.write(postOut, offsets, sizes);
        IDPostings idp2 = (IDPostings) Postings.Type.getPostings(
                Postings.Type.ID, postIn, 
                offsets, sizes);
        testData.iteration(idp2);
    }

    /**
     * Test of merge method, of class IDPostings.
     */
    @Test
    public void testRandomAppends() throws java.io.IOException {
        for(int i = 0; i < 256; i++) {

            TestData d1 = new TestData(rand, zipf, rand.nextInt(1024 * 16));
            TestData d2 = new TestData(rand, zipf, rand.nextInt(1024 * 16));
            checkAppend(true, d1, d2);
        }
    }

    @Test
    public void previousAppendTest() throws Exception {
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

    private Postings checkAppend(boolean dump, TestData... tds) throws java.io.IOException {

        cleanUp();
        IDPostings[] ps = new IDPostings[tds.length];
        long[][] tdOffsets = new long[tds.length][1];
        int[][] tdSizes = new int[tds.length][1];
        int[] starts = new int[tds.length];
        starts[0] = 1;
        for(int i = 0; i < tds.length; i++) {
            ps[i] = (IDPostings) tds[i].addData(this);
            ps[i].write(postOut, tdOffsets[i], tdSizes[i]);
            if(i > 0) {
                starts[i] = starts[i - 1] + tds[i - 1].maxDocID;
            }
        }

        try {
            IDPostings append = new IDPostings();
            for(int i = 0; i < ps.length; i++) {
                ps[i] = (IDPostings) Postings.Type.getPostings(Postings.Type.ID, postIn, tdOffsets[i], tdSizes[i]);
                append.append(ps[i], starts[i]);
            }

            TestData atd = new TestData(tds);

            long ao[] = new long[2];
            int as[] = new int[2];
            append.write(postOut, ao, as);
            append = (IDPostings) Postings.Type.getPostings(Postings.Type.ID, postIn, ao, as);
            checkPostingsEncoding(append, atd, ao, as);
            atd.iteration(append, false, false);
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
}
