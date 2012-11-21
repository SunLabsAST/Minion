/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.sun.labs.minion.query;

import java.util.EnumSet;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 *
 * @author stgreen
 */
public class ElementTest {
    
    public ElementTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of equals method, of class Element.
     */
    @Test
    public void testParsedEquals() {
        Element e1 = new ParsedElement("machine learning");
        Element e2 = new ParsedElement("machine learning");
        assertEquals(e1, e2);
    }

    @Test
    public void testSimpleTermEquals() {
        Element e1 = new Term("machine");
        Element e2 = new Term("machine");
        assertEquals(e1, e2);
    }
    
    @Test
    public void testTermEquals() {
        Element e1 = new Term("machine", EnumSet.of(Term.Modifier.CASE));
        Element e2 = new Term("machine");
        Element e3 = new Term("machine", EnumSet.of(Term.Modifier.CASE));
        assertThat(e1, is(e3));
        assertThat(e1, is(not(e2)));
    }
}
