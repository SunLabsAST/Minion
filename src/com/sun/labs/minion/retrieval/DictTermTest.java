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

import com.sun.labs.minion.*;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import com.sun.labs.minion.engine.SearchEngineImpl;

public class DictTermTest extends TestCase {

    /**
     * An index configuration.
     */
    protected IndexConfig ic;

    /**
     * A query configuration.
     */
    protected QueryConfig qc;

    /**
     * A search engine that we can use.
     */
    protected SearchEngine se;

    /** 
     * Creates a new <code>DictTermTest</code> instance.
     *
     * @param name test name
     */
    public DictTermTest(String name) {
        super(name);
    }

    /**
     * Set up by building an index with some terms in it so that we can
     * test the retrieval capabilities.
     */
    public void setUp() throws SearchEngineException {

        //
        // Get an index configuration for our tests.
        se = SearchEngineFactory.getSearchEngine("/tmp/dtt.idx");
    }

    public void tearDown() {
    }

    /**
     * @return a <code>TestSuite</code>
     */
    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
	
        return suite;
    }

    /** 
     * Entry point 
     */ 
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}// DictTermTest
