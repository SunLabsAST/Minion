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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Iterator;
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
public class IDPostingsTest {

    private static final Logger logger = Logger.getLogger(
            IDPostingsTest.class.getName());

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
    }

    @After
    public void tearDown() {
    }

    /**
     * Tests encoding our random data, dumping the data to a file if a failure occurs.
     * @throws Exception if there is an error
     */
    private void randomAddTest(int n) throws Exception {
        Random rand = new Random();
        for(int i = 0; i < 128; i++) {
            TestData r = null;
            try {
                r = new TestData(rand.nextInt(n) + 1);
                IDPostings p = r.encode();
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
        TestData simple = new TestData(new int[]{1, 4, 7, 10});
        simple.encode();
    }

    /**
     * Tests adding IDs multiple times.
     */
    @Test
    public void testSimpleMultipleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 1, 1,
                                                 1, 4, 4, 4, 7, 7, 8,
                                                 10, 11, 11, 17});
        simple.encode();
    }

    /**
     * Tests the encoding of the IDs by decoding them ourselves.
     * @param idp the postings we want to test
     * @param data the data that we're testing
     */
    private void testPostingsEncoding(IDPostings idp, TestData data) {
        WriteableBuffer[] buffs = idp.getBuffers();
        assertTrue(String.format("Wrong number of buffers: %d", buffs.length),
                   buffs.length == 2);

        long bsize = buffs[0].position() + buffs[1].position();
        long nb = 4 * data.unique.size();

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
                   n == data.rawData[data.rawData.length - 1]);
        rb = buffs[1].getReadableBuffer();

        int prev = 0;
        for(Integer x : data.unique) {
            int gap = rb.byteDecode();
            int curr = prev + gap;
            assertTrue(String.format(
                    "Incorrect ID: %d should be %d, decoded: %d", curr,
                    x, gap),
                       curr == x);
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
            IDPostings p = td.encode();
            td.iteration(p);
            testPostingsEncoding(p, td);
        }
    }

    private long writePostings(File of, IDPostings p) throws java.io.IOException {
        OutputStream os = new FileOutputStream(of);
        PostingsOutput postOut = new StreamPostingsOutput(os);
        long ret = postOut.write(p);
        postOut.flush();
        os.close();
        return ret;
    }

    private IDPostings readPostings(File f, long size) throws
            java.io.IOException {
        RandomAccessFile raf = new RandomAccessFile(f, "r");
        PostingsInput postIn = new StreamPostingsInput(raf, 8192);
        return (IDPostings) postIn.read(Postings.Type.ID, 0L, (int) size);
    }

    /**
     * Test writing then reading postings.
     */
    @Test
    public void testWriteAndRead() throws java.io.IOException {
        TestData td = new TestData(8192);
        IDPostings p = td.encode();
        File of = File.createTempFile("single", ".post");
        of.deleteOnExit();
        long size = writePostings(of, p);
        IDPostings idp2 = readPostings(of, size);
        td.iteration(p);
    }

    /**
     * Test of merge method, of class IDPostings.
     */
    @Test
    public void testRandomAppends() throws java.io.IOException {
        Random rand = new Random();
        for(int i = 0; i < 128; i++) {

            TestData d1 = new TestData(rand.nextInt(1024 * 16));
            TestData d2 = new TestData(rand.nextInt(1024 * 16));
            testAppend(d1, d2);
        }
    }

    @Test
    public void previousAppendTest() throws Exception {
        for(String s : previousAppends) {

            InputStream pdis = getClass().getResourceAsStream(s);
            if(pdis == null) {
                logger.info(String.format("Couldn't find %s", s));
                continue;
            }
            logger.info(String.format("Testing data %s", s));
            GZIPInputStream gzis = new GZIPInputStream(pdis);
            TestData td1 = new TestData(gzis);
            TestData td2 = new TestData(gzis);
            TestData atd = new TestData(gzis);
            testAppend(td1, td2);
            gzis.close();
        }
    }

    private void testAppend(TestData d1, TestData d2) throws java.io.IOException {


        IDPostings p1 = d1.encode();
        IDPostings p2 = d2.encode();
        int lastID = p1.getLastID();

        File of = File.createTempFile("single", ".post");
        of.deleteOnExit();
        StreamPostingsOutput po = new StreamPostingsOutput(new FileOutputStream(
                of));
        long s1 = po.write(p1);
        long s2 = po.write(p2);
        po.close();

        TestData atd = new TestData(d1, d2, lastID + 1);

        try {
            RandomAccessFile raf = new RandomAccessFile(of, "r");
            StreamPostingsInput pi = new StreamPostingsInput(raf, 8192);
            IDPostings append = new IDPostings();
            p1 = (IDPostings) pi.read(Postings.Type.ID, 0, (int) s1);
            d1.iteration(p1);
            append.append(p1, 1);
            p2 = (IDPostings) pi.read(Postings.Type.ID, s1, (int) s2);
            d2.iteration(p2);
            append.append(p2, lastID + 1);
            atd.iteration(append);
            raf.close();
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

        int[] rawData;

        Set<Integer> unique;

        public TestData(int n) {
            rawData = new int[n];
            unique = new LinkedHashSet<Integer>();

            //
            // Generate some random data.  We need to account for gaps of zero, so
            // keep track of the unique numbers with some sets.
            Random r = new Random();
            int prev = 0;
            for(int i = 0; i < rawData.length; i++) {

                //
                // We'll use random gaps to make sure we get appropriately increasing
                // postings.
                rawData[i] = prev + r.nextInt(256);
                if(i == 0 && rawData[i] == 0) {
                    rawData[i] = 1;
                }
                prev = rawData[i];
                unique.add(prev);
            }
        }

        public TestData(int[] d) {
            rawData = d;
            unique = new LinkedHashSet<Integer>();
            for(int x : d) {
                unique.add(x);
            }
        }

        public TestData(InputStream s) throws java.io.IOException {
            BufferedReader r = new BufferedReader(new InputStreamReader(s));
            String line = r.readLine();
            unique = new LinkedHashSet<Integer>();
            if(line == null) {
                rawData = new int[0];
                return;
            }
            String[] nums = line.split("\\s");
            rawData = new int[nums.length];
            for(int i = 0; i < rawData.length; i++) {
                rawData[i] = Integer.parseInt(nums[i]);
                unique.add(rawData[i]);
            }
        }

        public TestData(TestData d1, TestData d2, int start) {
            rawData = new int[d1.rawData.length + d2.rawData.length];
            unique = new LinkedHashSet<Integer>();
            int p = 0;
            for(int x : d1.rawData) {
                rawData[p++] = x;
                unique.add(x);
            }
            for(int x : d2.rawData) {
                int m = x + start - 1;
                rawData[p++] = m;
                unique.add(m);
            }
            logger.info(String.format("p: %d unique: %d", p, unique.size()));
        }

        public IDPostings encode() {
            IDPostings idp = new IDPostings();
            OccurrenceImpl o = new OccurrenceImpl();
            logger.info(String.format("Encoding %d ids (%d unique)",
                                      rawData.length,
                                      unique.size()));
            for(int i = 0; i < rawData.length; i++) {
                o.setID(rawData[i]);
                idp.add(o);
            }
            return idp;
        }

        public void iteration(IDPostings p) {
            PostingsIterator pi = p.iterator(null);
            Iterator<Integer> ui = unique.iterator();

            assertNotNull("Null postings iterator", pi);

            assertTrue(String.format("Expected %d ids got %d",
                                   unique.size(), pi.getN()),
                                   unique.size() == pi.getN());
            while(pi.next()) {
                int expected = ui.next();
                assertTrue(String.format(
                            "Couldn't match id %d, got %d",
                            expected, pi.getID()),
                            pi.getID() == expected);
            }
            
        }

        public void dump(PrintWriter out) throws java.io.IOException {
            for(int i = 0; i < rawData.length; i++) {
                out.format("%d ", rawData[i]);
            }
            out.println("");
        }
    }
}
