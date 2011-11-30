package com.sun.labs.minion.util.buffer;

import java.util.Random;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 */
public class ArrayBufferTest {

    private static Logger logger = Logger.getLogger(ArrayBufferTest.class.getName());

    public ArrayBufferTest() {
    }

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
     * Tests bytes that should be single byte encodings.
     */
    private void doByteEncode(int b, int e, int n) {
        logger.info(String.format("b: %d e: %d t: %d", b, e, e - b));
        for(int i = b; i < e; i++) {
            WriteableBuffer buff = new ArrayBuffer(16);
            buff.byteEncode(i);
            assertTrue(String.format("Weird position: %d for %d",
                                     buff.position(),
                                     i),
                       buff.position() == n);
        }
    }

    /**
     * Tests bytes that should be single byte encodings.
     */
    @Test
    public void testSingleByteEncode() {
        doByteEncode(0, 1 << 7, 1);
    }

    @Test
    public void testDoubleByteEncode() {
        doByteEncode(1 << 7, 1 << 14, 2);
    }

    @Test
    public void testTripleByteEncode() {
        doByteEncode(1 << 14, 1 << 21, 3);
    }

    @Test
    public void testQuadByteEncode() {
        doByteEncode(1 << 21, 1 << 28, 4);
    }
}