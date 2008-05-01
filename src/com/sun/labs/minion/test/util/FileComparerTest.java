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

package com.sun.labs.minion.test.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.TestCase;

public class FileComparerTest extends TestCase {
    private File testFile1;
    private File testFile2;

    protected void setUp() throws Exception {
        super.setUp();
        setupFiles();
    }

    private void setupFiles() throws IOException {
        setUpTestFile1();
        setUpTestFile2();
        
    }

    private void setUpTestFile2() throws IOException {
        testFile2 = File.createTempFile("compareTestFile2", null);
        testFile2.deleteOnExit();
        FileWriter fileWriter = new FileWriter(testFile2);
        fileWriter.write("This is a temporary file\ncontaining not much text\nit's only here for testing purposes");
        fileWriter.close();
    }

    /**
     * @throws IOException
     */
    private void setUpTestFile1() throws IOException {
        testFile1 = File.createTempFile("compareTestFile1", null);
        testFile1.deleteOnExit();
        FileWriter fileWriter = new FileWriter(testFile1);
        File thisDirectory = new File(".");
        File[] someFiles = thisDirectory.listFiles();
        for (int i = 0; i < someFiles.length; i++) {
            fileWriter.write(someFiles[i].getAbsolutePath());
        }
        fileWriter.close();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        
    }

    public void testAreLinesEqual() throws IOException {
        FileComparer comparer;
        comparer = new FileComparer(testFile1.getAbsolutePath(), testFile2.getAbsolutePath());
        assertFalse(comparer.areLinesEqual());
        comparer = new FileComparer(testFile2.getAbsolutePath(), testFile1.getAbsolutePath());
        assertFalse(comparer.areLinesEqual());
        comparer = new FileComparer(testFile1.getAbsolutePath(), testFile1.getAbsolutePath());
        assertTrue(comparer.areLinesEqual());
        comparer = new FileComparer(testFile2.getAbsolutePath(), testFile2.getAbsolutePath());
        assertTrue(comparer.areLinesEqual());
    }

}
