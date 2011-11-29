package com.sun.labs.minion.util;

import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class StringUtilTest {
    
    public StringUtilTest() {
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
     * Test of escapeQuotes method, of class StringUtil.
     */
    @Test
    public void testSimpleQuoteEscapes() {
        String s = "Internal \"quotes\" in this string";
        String expResult = "Internal \\\"quotes\\\" in this string";
        String result = StringUtil.escapeQuotes(s);
        assertEquals(expResult, result);
        s = "Multiple \"internal quotes\" in \"this here\" string";
        expResult = "Multiple \\\"internal quotes\\\" in \\\"this here\\\" string";
        result = StringUtil.escapeQuotes(s);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testInitialQuoteEscape() {
        String s = "\"Initial quotes\" in this string";
        String expResult = "\\\"Initial quotes\\\" in this string";
        String result = StringUtil.escapeQuotes(s);
        assertEquals(expResult, result);
        s = "\"Initial quotes\" in \"this string\" right here";
        expResult = "\\\"Initial quotes\\\" in \\\"this string\\\" right here";
        result = StringUtil.escapeQuotes(s);
        assertEquals(expResult, result);
        
    }
}
