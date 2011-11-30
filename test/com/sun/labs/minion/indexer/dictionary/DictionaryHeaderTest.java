/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.indexer.dictionary;

import com.sun.labs.minion.util.buffer.ArrayBuffer;
import com.sun.labs.minion.util.buffer.Buffer.DecodeMode;
import com.sun.labs.minion.util.buffer.Buffer.Portion;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sg93990
 */
public class DictionaryHeaderTest {
    
    public DictionaryHeaderTest() {
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
    public void testEncodeDecode() {
        DictionaryHeader header = new DictionaryHeader(1);
        header.size = 2;
        header.maxEntryID = 2;
        header.idToPosnPos = 99;
        header.idToPosnSize = 12;
        header.nameOffsetsPos = 118;
        header.nameOffsetsSize = 4;
        header.namesPos = 111;
        header.namesSize = 7;
        header.entryInfoOffsetsPos = 130;
        header.entryInfoOffsetsSize = 8;
        header.entryInfoPos = 122;
        header.entryInfoSize = 8;
        header.postStart = new long[] {0};
        header.postEnd = new long[] {0};
        header.magic = DictionaryHeader.GOOD_MAGIC;
        ArrayBuffer b = new ArrayBuffer(256);
        header.write(b);
        System.out.format("b: %s\n", b.toString(Portion.BEGINNING_TO_POSITION, DecodeMode.INTEGER));
        b.position(0);
    }
}
