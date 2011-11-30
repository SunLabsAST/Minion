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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
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
    private static int[][] randomData;

    private static Set[] sets;

    @BeforeClass
    public static void setUpClass() throws Exception {

        //
        // Generate some random data.  We need to account for gaps of zero, so
        // keep track of the unique numbers.
        randomData = new int[10][];
        sets = new Set[randomData.length];
        Random r = new Random();
        for(int i = 0; i < randomData.length; i++) {
            int s = r.nextInt(10240) + 1;
            int[] a = new int[s];
            LinkedHashSet<Integer> uniq = new LinkedHashSet<Integer>(a.length);
            int prev = 0;

            //
            // We'll use random gaps to make sure we get appropriately increasing
            // postings.
            for(int j = 0; j < a.length; j++) {
                a[j] = prev + r.nextInt(256);
                prev = a[j];
            }
            randomData[i] = a;
            sets[i] = uniq;
        }
    }

    /**
     * Dumps the random data so that we can add it as a new test case when things
     * go south.
     *
     * @return the name of the file where the data was dumped.
     * @throws Exception if anything goes wrong
     */
    private static String dumpRandomData() throws Exception {
        File f = File.createTempFile("random", ".data");
        PrintWriter out = new PrintWriter(new FileWriter(f));
        out.println(randomData.length);
        for(int i = 0; i < randomData.length; i++) {
            for(int j = 0; j < randomData[i].length; j++) {
                out.format("%d ", randomData[i][j]);
            }
            out.println("");
        }
        out.close();
        return f.toString();
    }

    /**
     * Reads test data from an input stream so that we can collect and re-run
     * bad random tests
     * @param s the stream to read the data from
     * @return the data
     * @throws Exception
     */
    private static int[][] readRandomData(InputStream s) throws Exception {
        BufferedReader r = new BufferedReader(new InputStreamReader(s));
        int size = Integer.parseInt(r.readLine());
        int[][] ret = new int[size][];
        for(int i = 0; i < size; i++) {
            String[] nums = r.readLine().split("\\s");
            int[] curr = new int[nums.length];
            for(int j = 0; j < curr.length; j++) {
                curr[j] = Integer.parseInt(nums[j]);
            }
            ret[i] = curr;
        }
        return ret;
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
    public void testSimpleAdd() {
        IDPostings idp = new IDPostings();
        OccurrenceImpl o = new OccurrenceImpl();
        int[] ids = new int[]{1, 4, 7, 10};
        for(int i : ids) {
            o.setID(i);
            idp.add(o);
        }

        PostingsIterator pi = idp.iterator(null);
        assertNotNull("Null postings iterator", pi);
        int i = 0;
        while(pi.next()) {
            assertTrue(String.format("Couldn't match id %d, got %d", ids[i], pi.
                    getID()),
                       pi.getID() == ids[i]);
            i++;
        }
    }

    /**
     * Tests adding IDs multiple times.
     */
    @Test
    public void testSimpleMultipleAdd() {
        IDPostings idp = new IDPostings();
        OccurrenceImpl o = new OccurrenceImpl();
        int[] multIDs = new int[]{1, 1, 1, 1, 4, 4, 4, 7, 7, 8, 10, 11, 11, 17};
        int[] ids = new int[]{1, 4, 7, 8, 10, 11, 17};
        for(int i : multIDs) {
            o.setID(i);
            idp.add(o);
        }

        PostingsIterator pi = idp.iterator(null);
        assertNotNull("Null postings iterator", pi);
        int i = 0;
        while(pi.next()) {
            assertTrue(String.format("Couldn't match id %d, got %d", ids[i], pi.
                    getID()), pi.getID() == ids[i]);
            i++;
        }
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

    /**
     * Tests encoding a lot of data, dumping the data to a file if a failure occurs.
     * @throws Exception if there is an error
     */
    @Test
    public void randomAddTest() throws Exception {
        for(int i = 0; i < randomData.length; i++) {
            IDPostings idp = new IDPostings();
            OccurrenceImpl o = new OccurrenceImpl();
            int[] curr = randomData[i];
            logger.info(String.format("Encoding %d ids", curr.length));
            for(int j = 0; j < curr.length; j++) {
                try {
                    o.setID(curr[j]);
                    idp.add(o);
                } catch(Exception ex) {
                    fail(String.format("Failed to encode random data. See: %s",
                                       dumpRandomData()));
                }
            }
            
            if(sets[i].size() != idp.getN()) {
                fail(String.format("Expected %d ids got %d, see: %s", sets[i].
                        size(), idp.getN(), dumpRandomData()));
            }

            PostingsIterator pi = idp.iterator(null);
            Iterator<Integer> ui = sets[i].iterator();
            assertNotNull("Null postings iterator", pi);
            while(pi.next()) {
                int expected = ui.next();
                if(pi.getID() != expected) {
                    fail(String.format("Couldn't match id %d, got %d, see %s",
                                       expected, pi.getID(), dumpRandomData()));
                }
            }
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
}
