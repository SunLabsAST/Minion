package com.sun.labs.minion.util.buffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;
import junit.framework.Assert;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class FileWriteableBufferTest {

    public FileWriteableBufferTest() {
    }
    private static RandomAccessFile raf;

    private static File file;

    private static Random rand = new Random();

    private static final Logger logger = Logger.getLogger(FileWriteableBufferTest.class.getName());
    
    private static final String[] prevData = new String[] {
        "/var/folders/zq/8xrjbcx915118brpx0pz9l34002wrf/T/bits8725031421649639788buff",
    };

    @BeforeClass
    public static void setUpClass() throws Exception {
        file = File.createTempFile("fwb", "buff");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws IOException {
        raf = new RandomAccessFile(file, "rw");
    }

    @After
    public void tearDown() throws IOException {
        raf.close();
        file.delete();
    }

    private void setBits(int n, int max) throws IOException {
        Set<Integer> bits = new LinkedHashSet<Integer>();
        for(int i = 0; i < n; i++) {
            int bit = rand.nextInt(max);
            bits.add(bit);
        }
        setBits(bits, true);
    }
    
    private void setBits(Set<Integer> bits, boolean writeOnFailure) throws IOException {
        if(bits.isEmpty()) {
            return;
        }
        try {
            WriteableBuffer b = new FileWriteableBuffer(raf);
            for(Integer bit : bits) {
                b.set(bit);
            }
            long count = b.countBits();
            ReadableBuffer rb = b.getReadableBuffer();
            long rcount = rb.countBits();
            for(Integer bit : bits) {
                Assert.assertTrue(String.format("Couldn't get bit %d", bit),
                        rb.test(bit));
            }
            Assert.assertEquals(bits.size(), count);
        } catch(AssertionError err) {
            if(writeOnFailure) {
                File f = File.createTempFile("bits", "buff");
                PrintWriter pw = new PrintWriter(f);
                for(Integer bit : bits) {
                    pw.println(bit);
                }
                pw.close();
                logger.info(String.format("Wrote file %s", f));
            }
            throw(err);
        }
    }
    
    private Set<Integer> readBits(String s) throws IOException {
        InputStream is = getClass().getResourceAsStream(s);
        if(is == null) {
            is = new FileInputStream(s);
        }
        if(is == null) {
            logger.info(String.format("Can't find resource %s", is));
            return Collections.EMPTY_SET;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        String num;
        Set<Integer> bits = new LinkedHashSet<Integer>();
        while((num = r.readLine()) != null) {
            bits.add(Integer.parseInt(num));
        }
        r.close();
        return bits;
    }

    @Test
    public void testSet() throws java.io.IOException {
        setBits(10, 20000);
    }
    
    @Test
    public void testBigSet() throws java.io.IOException {
        setBits(1000, 1000000);
    }
    
    @Test 
    public void testLots() throws java.io.IOException {
        tearDown();
        int nIter = 2000;
        for(int i = 0; i < nIter; i++) {
            setUp();
            int n = (int) (rand.nextDouble() * 10000);
            int size = (int) (rand.nextDouble() * 100000000);
            logger.info(String.format("Iteration %d/%d n: %d size: %d", (i + 1), nIter, n, size));
            setBits(n, size);
            tearDown();
        }
    }
    
    @Test
    public void testPreviousData() throws java.io.IOException {
        tearDown();
        for(String prev : prevData) {
            setUp();
            setBits(readBits(prev), false);
            tearDown();
        }
    }
}
