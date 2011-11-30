/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.indexer.postings;

import com.sun.labs.minion.util.buffer.ReadableBuffer;
import com.sun.labs.minion.util.buffer.WriteableBuffer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
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

    private static Logger logger = Logger.getLogger(
            IDPostingsTest.class.getName());

    public IDPostingsTest() {
    }
    private static String[] previousData = new String[]{
        "/com/sun/labs/minion/indexer/postings/resource/2.data.gz",
        "/com/sun/labs/minion/indexer/postings/resource/1.data.gz",};

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
     * Tests simple addition of occurrences.
     */
    @Test
    public void testSimpleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 4, 7, 10});
        encodeData(simple);
    }

    /**
     * Tests adding IDs multiple times.
     */
    @Test
    public void testSimpleMultipleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 1, 1,
                                                 1, 4, 4, 4, 7, 7, 8,
                                                 10, 11, 11, 17});
        encodeData(simple);
    }

    /**
     * Tests the encoding of the IDs by decoding them ourselves.
     */
    @Test
    public void testSimpleEncoding() {
        IDPostings idp = new IDPostings();
        OccurrenceImpl o = new OccurrenceImpl();
        int[] ids = new int[]{1, 4, 7, 10};
        for(int i : ids) {
            o.setID(i);
            idp.add(o);
        }

        WriteableBuffer[] buffs = idp.getBuffers();
        assertTrue(String.format("Wrong number of buffers: %d", buffs.length),
                   buffs.length == 2);
        ReadableBuffer rb = buffs[0].getReadableBuffer();

        int n = rb.byteDecode();
        assertTrue(String.format("Wrong number of IDs: %d", n), n == ids.length);
        n = rb.byteDecode();
        assertTrue(String.format("Wrong last ID: %d", n), n == ids[ids.length
                                                                   - 1]);
        rb = buffs[1].getReadableBuffer();

        int prev = 0;
        for(int i = 0; i < ids.length; i++) {
            int gap = rb.byteDecode();
            int curr = prev + gap;
            assertTrue(String.format(
                    "Incorrect ID at %d: %d should be %d, decoded: %d", i, curr,
                    ids[i], gap),
                       curr == ids[i]);
        }
    }

    private IDPostings encodeData(TestData data) {
        IDPostings idp = new IDPostings();
        OccurrenceImpl o = new OccurrenceImpl();
        logger.info(String.format("Encoding %d ids (%d unique)",
                                  data.rawData.length,
                                  data.unique.size()));
        for(int i = 0; i < data.rawData.length; i++) {
            o.setID(data.rawData[i]);
            idp.add(o);
        }
        return idp;
    }

    private void testIteration(IDPostings idp, TestData data) {

        PostingsIterator pi = idp.iterator(null);
        Iterator<Integer> ui = data.unique.iterator();

        assertNotNull("Null postings iterator", pi);

        if(data.unique.size() != pi.getN()) {
            fail(String.format("Expected %d ids got %d",
                               data.unique.size(), pi.getN()));
        }

        while(pi.next()) {
            int expected = ui.next();
            if(pi.getID() != expected) {
                fail(String.format(
                        "Couldn't match id %d, got %d",
                        expected, pi.getID()));
            }
        }
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
                IDPostings idp = encodeData(r);
                testIteration(idp, r);
            } catch(AssertionError ex) {
                PrintWriter out = getTempWriter();
                r.dump(out);
                out.close();
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

    @Test
    public void extraLargeRandomAddTest() throws Exception {
        randomAddTest(1024*1024);
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
                continue;
            }
            logger.info(String.format("Testing data %s", s));
            GZIPInputStream gzis = new GZIPInputStream(pdis);
            TestData td = new TestData(gzis);
            gzis.close();
            IDPostings idp = encodeData(td);
            testIteration(idp, td);
        }
    }

    /**
     * Test of merge method, of class IDPostings.
     */
    @Test
    public void testMerge() {
    }

    /**
     * Test of iterator method, of class IDPostings.
     */
    @Test
    public void testIterator() {
    }

    private PrintWriter getTempWriter() throws java.io.IOException {
        File f = File.createTempFile("random", ".data");
        return new PrintWriter(new FileWriter(f));
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
                rawData[i] = prev + r.nextInt(256) + 1;
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

        public void dump(PrintWriter out) throws java.io.IOException {
            for(int i = 0; i < rawData.length; i++) {
                out.format("%d ", rawData[i]);
            }
            out.println("");
        }
    }
}
