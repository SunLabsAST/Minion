package com.sun.labs.minion.indexer.postings;

import java.util.HashSet;
import java.util.Set;
import java.io.PrintWriter;
import com.sun.labs.minion.indexer.postings.io.PostingsInput;
import com.sun.labs.minion.indexer.postings.io.PostingsOutput;
import com.sun.labs.util.NanoWatch;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 *
 */
public class TestData {

    private static final Logger logger = Logger.getLogger(TestData.class.getName());

    protected int[] ids;

    protected int[] freqs;

    protected int[] posns;

    protected int numPosns;

    protected int maxDocID;

    Random rand;

    Zipf zipf;

    public TestData(Random rand, Zipf zipf, int n) {
        this.rand = rand;
        this.zipf = zipf;
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
        maxDocID = prev + rand.nextInt(256);
    }

    public TestData(int[] ids) {
        this(ids, null, null, 0, ids[ids.length - 1]);
    }

    public TestData(int[] ids, int[] freqs) {
        this(ids, freqs, null, 0, ids[ids.length - 1]);
    }

    public TestData(int[] ids, int[] freqs, int[] posns) {
        this(ids, freqs, posns, posns.length, ids[ids.length - 1]);
    }

    public TestData(int[] ids, int[] freqs, int[] posns, int numPosns, int maxDocID) {
        Set<Integer> unique = new HashSet<Integer>();
        for(int id : ids) {
            unique.add(id);
        }
        if(unique.size() != ids.length) {
            int prev = -1;
            int p = -1;
            ids = new int[unique.size()];
            freqs = new int[unique.size()];
            for(int id : ids) {
                if(id != prev) {
                    p++;
                    ids[p] = id;
                    freqs[p] = 1;
                } else {
                    freqs[p]++;
                }
                prev = id;
            }
        } else {
            this.ids = ids;
            this.freqs = freqs;
            this.posns = posns;
            this.numPosns = numPosns;
            this.maxDocID = maxDocID;
        }
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
        maxDocID = Integer.parseInt(r.readLine());
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
        int start = 0;
        for(int i = 0; i < tds.length; i++) {
            TestData td = tds[i];
            for(int j = 0; j < td.ids.length; j++, p++) {
                ids[p] = td.ids[j] + start;
                lastID = ids[p];
                freqs[p] = td.freqs[j];
            }
            System.arraycopy(td.posns, 0, posns, pp, td.numPosns);
            pp += td.numPosns;
            start += td.maxDocID;
        }
        maxDocID = start + 1;
        numPosns = posns.length;
    }

    /**
     * Puts the data through its paces.
     */
    public void paces(PostingsOutput[] postOut, long[] offsets, int[] sizes, PostingsInput[] postIn, PostingsTest test, boolean getFrequencies, boolean getPositions) throws java.io.IOException {
        paces(addData(test), postOut, offsets, sizes, postIn, test, getFrequencies, getPositions);
    }

    /**
     * Puts the data through its paces.
     */
    public void paces(Postings p, PostingsOutput[] postOut, long[] offsets, int[] sizes, PostingsInput[] postIn, PostingsTest test, boolean getPositions, boolean getFrequencies) throws java.io.IOException {
        NanoWatch nw = new NanoWatch();
        logger.fine(String.format("Paces for %d postings", ids.length));
        nw.start();
        iteration(p, getFrequencies, getPositions);
        nw.stop();
        logger.fine(String.format(" Uncompressed iteration %.3f", nw.getLastTimeMillis()));
        nw.start();
        p.write(postOut, offsets, sizes);
        nw.stop();
        logger.fine(String.format(" Encoding and writing %.3f", nw.getLastTimeMillis()));
        nw.start();
        p = test.getPostings(postIn, offsets, sizes);
        nw.stop();
        logger.fine(String.format(" Instantiation %.3f", nw.getLastTimeMillis()));
        nw.start();
        test.checkPostingsEncoding(p, this, offsets, sizes);
        nw.stop();
        logger.fine(String.format(" Encoding check %.3f", nw.getLastTimeMillis()));
        nw.start();
        iteration(p, getFrequencies, getPositions);
        nw.stop();
        logger.fine(String.format(" Compressed iteration %.3f", nw.getLastTimeMillis()));
    }

    public Postings addData(PostingsTest test) {
        return addData(test.getPostings());
    }

    public Postings addData(Postings p) {
        FieldOccurrenceImpl o = new FieldOccurrenceImpl();
        o.setCount(1);
        int pp = 0;
        if(freqs == null) {
            for(int i = 0; i < ids.length; i++) {
                o.setID(ids[i]);
                p.add(o);
            }
        } else {
            if(posns == null) {
                for(int i = 0; i < ids.length; i++) {
                    o.setID(ids[i]);
                    o.setCount(freqs[i]);
                    p.add(o);
                }
            } else {
                for(int i = 0; i < ids.length; i++) {
                    o.setID(ids[i]);
                    if(freqs != null) {
                        for(int j = 0; j < freqs[i]; j++, pp++) {
                            if(posns != null) {
                                o.setPos(posns[pp]);
                            }
                            p.add(o);
                        }
                    }
                }
            }
        }
        return p;
    }

    public void iteration(Postings p) {
        iteration(p, false, false);
    }

    public void iteration(Postings p, boolean getFreqs, boolean getPositions) {

        PostingsIteratorFeatures features = new PostingsIteratorFeatures();
        features.setPositions(getPositions);
        PostingsIterator pi = p.iterator(features);

        assertNotNull("Null postings iterator", pi);

        if(ids.length != pi.getN()) {
            fail(String.format("Expected %d ids got %d", ids.length, pi.getN()));
        }

        int i = 0;
        int pp = 0;
        while(pi.next()) {
            int expectedID = ids[i];
            if(expectedID != pi.getID()) {
                assertTrue(String.format(
                        "Couldn't match %d id, %d, got %d",
                        i,
                        expectedID,
                        pi.getID()),
                        expectedID == pi.getID());
            }
            if(getFreqs) {
                int expectedFreq = freqs[i];
                if(expectedFreq != pi.getFreq()) {
                    assertTrue(String.format(
                            "Incorrect %d freq %d, got %d",
                            i,
                            expectedFreq, pi.getFreq()), expectedFreq
                            == pi.getFreq());
                }
                if(getPositions) {
                    int[] piPosn = ((PostingsIteratorWithPositions) pi).getPositions();
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
                }
            }
            i++;
        }
    }

    protected void write(PrintWriter out) {
        for(int id : ids) {
            out.format("%d ", id);
        }
        out.println("");
        for(int freq : freqs) {
            out.format("%d ", freq);
        }
        out.println("");
        out.println(maxDocID);
        for(int i = 0; i < numPosns; i++) {
            out.format("%d ", posns[i]);
        }
        out.println("");
    }

    public static void write(PrintWriter out, TestData... tds) throws java.io.IOException {
        out.println(tds.length);
        for(TestData td : tds) {
            td.write(out);
        }
    }

    public static TestData[] read(BufferedReader r) throws java.io.IOException {
        String l = r.readLine();
        int n = 0;
        try {
            n = Integer.parseInt(l);
        } catch(NumberFormatException ex) {
            throw new java.io.IOException(String.format("Unable to parse number of test data: %s", l.substring(0, Math.min(l.length(), 20))), ex);
        }
        TestData[] tds = new TestData[n];
        for(int i = 0; i < tds.length; i++) {
            tds[i] = new TestData(r);
        }
        return tds;
    }
}
