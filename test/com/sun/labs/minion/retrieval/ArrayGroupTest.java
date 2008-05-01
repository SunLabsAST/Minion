/*
 * Copyright 2007-2008 Sun Microsystems, Inc. All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER
 * 
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * only, as published by the Free Software Foundation.
 * 
 * This code is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License version 2 for more details (a copy is
 * included in the LICENSE file that accompanied this code).
 * 
 * You should have received a copy of the GNU General Public License
 * version 2 along with this work; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA
 * 
 * Please contact Sun Microsystems, Inc., 16 Network Circle, Menlo
 * Park, CA 94025 or visit www.sun.com if you need additional
 * information or have any questions.
 */

package com.sun.labs.minion.retrieval;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ArrayGroupTest {

    //
    // Some arrays to use for testing.
    int[][] docs = {
        {1}, 			// 0
        {0, 0, 0, 0, 0, 0, 0, 0},	// 1
        {1}, 			// 2
        {2}, 			// 3
        {1, 0, 0}, 		// 4
        {2, 0, 0}, 		// 5
        {1, 2, 0, 0, 0},	// 6
        {1, 2, 3, 4, 5, 0, 0, 0},	// 7
        {3, 4, 5, 0, 0, 0},	// 8
        {3, 4, 5, 6, 7, 8, 9, 0, 0},	// 9
        {6, 7, 8, 9, 0, 0, 0},	// 10
        {1, 2, 3, 4, 5, 6, 7, 8, 9},	// 11 
        {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 0}	// 12
    };

    float[][] scores = {{1},
                        {0, 0, 0, 0, 0, 0, 0, 0},
                        {1},
                        {2},
                        {1, 0, 0},
                        {2, 0, 0},
                        {1, 2, 0, 0, 0},
                        {1, 2, 3, 4, 5, 0, 0, 0},
                        {3, 4, 5, 0, 0, 0},
                        {3, 4, 5, 6, 7, 8, 9, 0, 0},
                        {6, 7, 8, 9, 0, 0, 0},
                        {1, 2, 3, 4, 5, 6, 7, 8, 9},
                        {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 0, 0}};
    
    int[] lens = {0, 0, 1, 1, 1, 1, 2, 5, 3, 7, 4, 9, 12};

    /**
     * Arrays for holding groups.
     */
    ArrayGroup[] strict;
    ScoredGroup[] scored;
    NegativeGroup[] negative;

    /** 
     * Creates a new <code>ArrayGroupTest</code> instance.
     *
     * @param name test name
     */
    public ArrayGroupTest() {
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
     * Checks an intersection of two groups for equality with a third.  The
     * intersection is done both ways, which should be the same for
     * non-negative sets.
     */
    public void checkIntersect(ArrayGroup g1, ArrayGroup g2, ArrayGroup g3) {
        assertEquals(g1.intersect(g2), g3);
        assertEquals(g2.intersect(g1), g3);
    }

    /**
     * Tests whether groups are equal to themselves.
     */
    @Test
    public void testSelfEquality() {
        for(int i = 0; i < strict.length; i++) {
            assertEquals(strict[i], strict[i]);
            assertEquals(scored[i], scored[i]);
            assertEquals(negative[i], negative[i]);
        }
    }

    /**
     * Tests equality of strict groups that are the same size, even if
     * their array lengths are different.
     */
    @Test
    public void testSameSizeEquality() {
        assertEquals(strict[2], strict[4]);
        assertEquals(strict[3], strict[5]);
        assertEquals(scored[2], scored[4]);
        assertEquals(scored[3], scored[5]);
        assertEquals(negative[2], negative[4]);
        assertEquals(negative[3], negative[5]);
    }

    /**
     * Tests self-intersection for strict groups.  The intersection of a
     * strict group and itself should always be the same group.
     */
    @Test
    public void testStrictSelfIntersect() {
        for(int i = 0; i < strict.length; i++) {
            assertEquals(strict[i].intersect(strict[i]),
                         strict[i]);
        }
    }

    /**
     * Tests intersection of strict groups with other strict groups.
     */
    @Test
    public void testStrictIntersect() {

        //
        // the first group is an initial subset of the second.
        checkIntersect(strict[6], strict[7], strict[6]);

        //
        // The second group trails the first.
        checkIntersect(strict[7], strict[8], strict[8]);

        //
        // Disjoint sets.
        checkIntersect(strict[7], strict[10], strict[0]);

        //
        // One set in the middle of another.
        checkIntersect(strict[10], strict[12], strict[10]);
    }
    
    /**
     * Tests self-intersection for scored groups.  The intersection of a
     * scored group and itself should always be the same group, but with
     * the scores doubled.
     */
    @Test
    public void testScoredSelfIntersect() {
        for(int i = 0; i < scored.length; i++) {
            assertEquals(scored[i].intersect(scored[i]),
                         scored[i].mult(2));
        }
    }

    /**
     * Tests intersection of scored groups with other scored groups.
     */
    @Test
    public void testScoredIntersect() {

        //
        // the first group is an initial subset of the second.
        checkIntersect(scored[6], scored[7], scored[6].mult(2));

        //
        // The second group trails the first.
        checkIntersect(scored[7], scored[8], scored[8].mult(2));

        //
        // Disjoint sets.
        checkIntersect(scored[7], scored[10], scored[0].mult(2));

        //
        // One set in the middle of another.
        checkIntersect(scored[10], scored[12], scored[10].mult(2));
    }
    
}// ArrayGroupTest
