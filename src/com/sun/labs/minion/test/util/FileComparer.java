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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Utility class to compare two files, for example to perform a diff.
 * @author Bernard Horan
 *
 */
public class FileComparer {
    private BufferedReader firstReader;
    private BufferedReader secondReader;

    /**
     * Example execution.
     * @param args pass in the two files to be compared
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        FileComparer comparer = new FileComparer(args[0], args[1]);
        System.out.println(comparer.areLinesEqual());
    }
    
    /**
     * Create an instance of FileComparer with the pathnames of two files.
     * @param fileName1 pathname of file1
     * @param fileName2 pathname of file2
     * @throws IOException 
     */
    public FileComparer(String fileName1, String fileName2) throws IOException {
        File firstFile = new File(fileName1);
        check(firstFile);
        firstReader = new BufferedReader(new FileReader(firstFile));
        File secondFile = new File(fileName2);
        check(secondFile);
        secondReader = new BufferedReader(new FileReader(secondFile));
    }
    
    
    
    
    

    /**
     * Ensure that the file exists and is readable
     * @param aFile a File to be checked
     * @throws IOException
     */
    private void check(File aFile) throws IOException {
        if (!aFile.exists()) {
            throw new FileNotFoundException("No such file " + aFile.getAbsolutePath());
        }
        if (!aFile.canRead()) {
            throw new IOException("Can't read " + aFile.getAbsolutePath());
        }
    }
    
    /**
     * Perform a line-by-line comparison from the first file to the second
     * @return true if the files are equal
     * @throws IOException
     */
    public boolean areLinesEqual() throws IOException {
        String line1, line2;
        while (null != (line1 = firstReader.readLine())) {
            line1 = line1.trim();
            if (line1.length() > 0) {
                line2 = secondReader.readLine();
                line2 = line2.trim();
                if (!line1.equals(line2)) {
                    return false;
                }
            }
        }
        return true;
    }
}
