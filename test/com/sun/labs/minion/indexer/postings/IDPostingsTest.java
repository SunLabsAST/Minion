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
    private static TestData randomData = new TestData(32);

    private static String[] previousData = new String[]{
        "/com/sun/labs/minion/indexer/postings/resource/2.data.gz",
        "/com/sun/labs/minion/indexer/postings/resource/1.data.gz",
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
     * Tests simple addition of occurrences.
     */
    @Test
    public void testSimpleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 4, 7, 10});
        encodeData(simple, false);
    }

    /**
     * Tests adding IDs multiple times.
     */
    @Test
    public void testSimpleMultipleAdd() throws Exception {
        TestData simple = new TestData(new int[]{1, 1, 1,
                    1, 4, 4, 4, 7, 7, 8,
                    10, 11, 11, 17});
        encodeData(simple, false);
    }

    /**
     * Tests the encoding of the IDs by decoding them ourselves.
     */
    @Test
    public void testSimpleEncoding() {
        IDPostings idp = new IDPostings();
        OccurrenceImpl o = new OccurrenceImpl();
        int[] ids = new int[]{1, 4, 7, 10};
        for (int i : ids) {
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
        assertTrue(String.format("Wrong last ID: %d", n), n == ids[ids.length -
                1]);
        rb = buffs[1].getReadableBuffer();

        int prev = 0;
        for (int i = 0; i < ids.length; i++) {
            int gap = rb.byteDecode();
            int curr = prev + gap;
            assertTrue(String.format(
                    "Incorrect ID at %d: %d should be %d, decoded: %d", i, curr,
                    ids[i], gap),
                    curr == ids[i]);
        }
    }

    private void encodeData(TestData data, boolean dumpOnFailure) throws
            Exception {
        for (int i = 0; i < data.rawData.length; i++) {
            IDPostings idp = new IDPostings();
            OccurrenceImpl o = new OccurrenceImpl();
            int[] curr = data.rawData[i];
            logger.info(String.format("Encoding %d ids (%d unique)", curr.length,
                    data.sets[i].size()));
            for (int j = 0; j < curr.length; j++) {
                try {
                    o.setID(curr[j]);
                    idp.add(o);
                } catch (Exception ex) {
                    if (dumpOnFailure) {
                        fail(String.format(
                                "Failed to encode random data. Data row %d, item %d See: %s",
                                i, j,
                                randomData.dump()));
                    } else {
                        fail(String.format(
                                "Failed to encode random data. Data row %d, item %d",
                                i, j));
                    }
                }
            }

            PostingsIterator pi = idp.iterator(null);
            Iterator<Integer> ui = data.sets[i].iterator();

            assertNotNull("Null postings iterator", pi);

            if (data.sets[i].size() != pi.getN()) {
                if (dumpOnFailure) {
                    fail(String.format(
                            "Expected %d ids got %d data row: %d, see: %s",
                            data.sets[i].size(), pi.getN(), i,
                            randomData.dump()));
                } else {
                    fail(String.format("Expected %d ids got %d data row: %d",
                            data.sets[i].size(), pi.getN(), i));

                }
            }

            while (pi.next()) {
                int expected = ui.next();
                if (pi.getID() != expected) {
                    if (dumpOnFailure) {
                        fail(String.format(
                                "Couldn't match id %d, got %d, data row %d see %s",
                                expected, pi.getID(), i, randomData.dump()));
                    } else {
                        fail(String.format(
                                "Couldn't match id %d, got %d, data row %d",
                                expected, pi.getID(), i));

                    }
                }
            }
        }
    }

    /**
     * Tests encoding our random data, dumping the data to a file if a failure occurs.
     * @throws Exception if there is an error
     */
    @Test
    public void randomAddTest() throws Exception {
        encodeData(randomData, true);
    }
    /**
     * Tests encoding data that has had problems before, ensuring that we
     * don't re-introduce old problems.
     */
    @Test
    public void previousDataTest() throws Exception {
        for (String s : previousData) {

            InputStream pdis = getClass().getResourceAsStream(s);
            if (pdis == null) {
                logger.info(String.format("Can't find resource: %s", s));
                continue;
            }
            logger.info(String.format("Testing data %s", s));
            GZIPInputStream gzis = new GZIPInputStream(pdis);
            TestData td = new TestData(gzis);
            gzis.close();
            encodeData(td, false);
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

    private static class TestData {

        int[][] rawData;

        Set[] sets;

        public TestData(int n) {
            rawData = new int[n][];
            sets = new Set[n];

            //
            // Generate some random data.  We need to account for gaps of zero, so
            // keep track of the unique numbers with some sets.
            Random r = new Random();
            for (int i = 0; i < rawData.length; i++) {
                int s = r.nextInt(1024 * 8) + 1;
                int[] a = new int[s];
                LinkedHashSet<Integer> uniq = new LinkedHashSet<Integer>();
                int prev = 0;

                //
                // We'll use random gaps to make sure we get appropriately increasing
                // postings.
                for (int j = 0; j < a.length; j++) {
                    a[j] = prev + r.nextInt(256) + 1;
                    prev = a[j];
                    uniq.add(prev);
                }
                rawData[i] = a;
                sets[i] = uniq;
            }
        }

        public TestData(int[] d) {
            rawData = new int[1][];
            rawData[0] = d;
            sets = new Set[1];
            sets[0] = new LinkedHashSet<Integer>();
            for (int x : d) {
                sets[0].add(x);
            }
        }

        public TestData(InputStream s) throws java.io.IOException {
            BufferedReader r = new BufferedReader(new InputStreamReader(s));
            List<int[]> l = new ArrayList<int[]>();
            String line;
            while ((line = r.readLine()) != null) {
                String[] nums = line.split("\\s");
                int[] curr = new int[nums.length];
                for (int j = 0; j < curr.length; j++) {
                    curr[j] = Integer.parseInt(nums[j]);
                }
                l.add(curr);
            }
            rawData = new int[l.size()][];
            sets = new Set[rawData.length];
            for (int i = 0; i < l.size(); i++) {
                rawData[i] = l.get(i);
                sets[i] = new LinkedHashSet<Integer>();
                for (int x : rawData[i]) {
                    sets[i].add(x);
                }
            }
        }

        public String dump() throws java.io.IOException {
            File f = File.createTempFile("random", ".data");
            PrintWriter out = new PrintWriter(new FileWriter(f));
            out.println(rawData.length);
            for (int i = 0; i < rawData.length; i++) {
                for (int j = 0; j < rawData[i].length; j++) {
                    out.format("%d ", rawData[i][j]);
                }
                out.println("");
            }
            out.close();
            return f.toString();
        }
    }
}
