/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.sun.labs.minion.util.buffer;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author stgreen
 */
public class LongBufferTest {

    public LongBufferTest() {
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

    @Test
    public void testEncodeDecodeLong() {
        ArrayBuffer b = new ArrayBuffer(10);
        long l = ((long) Integer.MAX_VALUE) + 10;
        b.byteEncode(l);
        b.position(0);
        long d = b.byteDecodeLong();
        assertTrue("Not equal: " + l + " " + d, d == l);
    }

}