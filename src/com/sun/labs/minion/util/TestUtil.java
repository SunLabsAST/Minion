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

package com.sun.labs.minion.util;

import java.util.Random;
import java.util.HashMap;
import java.io.*;

/**
 * This class defines a set of utilities that will be used
 * by the unit tests for AST.
 * 
 * @author Jeff Alexander
 * @version $Revision: 1.1.2.3 $
 */

public class TestUtil
{

    private static final String ARRAY_FILE_NAME = "ast.testdata";
    private static HashMap storedArrays = new HashMap();

    private static File dataFile;
    
    static {
        // If stored data file exists, read it in to be our hashmap
        dataFile = new File(ARRAY_FILE_NAME);
        if (dataFile.exists()) {
            if (dataFile.canRead()) {
                // read into storedArrays:
                try {
                    FileInputStream fileIn = new FileInputStream(dataFile);
                    ObjectInputStream objIn = new ObjectInputStream(fileIn);
                    storedArrays = (HashMap) objIn.readObject();
                    objIn.close();
                    fileIn.close();
                } catch (Exception ex) {
                    // I don't really care what went wrong here... if we got
                    // any exceptions, just fail
                    System.out.println("Unable to read stored data file, will not be able to repeat tests with prior data.");
                }
                if (!dataFile.canWrite()) {
                    System.out.println("Unable to write to stored data file, will not be able to store test data");
                }
            } else {
                System.out.println("Unable to read stored data file, will not be able to repeat tests with prior data.");
            }
        }
    }


    public static String stackTrace(Throwable th) {
        String trace = "\n" + th.toString();
        StackTraceElement ste[] = th.getStackTrace();
        for (int i = 0; i < ste.length; i++) {
            trace = trace + "\n at " + ste[i];
        }
        return trace;
    }

    /** 
     * Generates an array of random long values in no particular order
     * 
     * @param numIDs the number of elements to put in the array
     * @return an array of random long values
     */
    public static long[] generateIDs(int numIDs) {
        long[] ids = new long[numIDs];
        Random chaos = new Random();
        for (int i = 0; i < numIDs; i++) {
            ids[i] = getRandomLong(chaos);
        }
        return ids;
    }

    /** 
     * Generates an array of random int values in increasing order
     * 
     * @param numIDs the number of elements to put in the array
     * @return an array of random int values in increasing order
     */
    public static int[] generateOrderedIntIDs(int numIDs) {
        int[] ids = new int[numIDs];
        Random chaos = new Random();
        int prev = 0;
        for (int i = 0; i < numIDs; i++) {
            int next = getRandomInt(chaos);
            // handle wrap-around:
            if ((prev + next) < 0) {
                ids[i] = prev;
            } else {
                ids[i] = prev + next;
                prev = ids[i];
            }
        }
        return ids;
    }

    /** 
     * Generate a random number that will occupy no more than a certain
     * number of bytes.
     * 
     * @param chaos a random number generator
     * @param maxBytes the max number of bytes - valid values are 1-8
     * @return a random number less than or equal to maxBytes in length
     */
    public static long getRandom(Random chaos, int maxBytes) {
        long next = java.lang.Math.abs(chaos.nextLong());
        if (maxBytes < 8) {
            next = next % ((1L << (8 * maxBytes)) - 1);
        }
        return next;
    }

    /** 
     * Generate a random long.  This method will return numbers that
     * are random in a way that meets the needs of these tests.  Specifically,
     * the number of bytes that are required to store each value will be
     * highly variable from one call to the next.
     * 
     * @param chaos a random number generator
     * @return a random number
     */
    public static long getRandomLong(Random chaos) {
        int maxBytes = (chaos.nextInt(8)) + 1;
        long next = java.lang.Math.abs(chaos.nextLong());
        if (maxBytes < 8) {
            next = next % ((1L << (8 * maxBytes)) - 1);
        }
        return next;
    }


    /** 
     * Generate a random int.  This method will return numbers that
     * are random in a way that meets the needs of these tests.  Specifically,
     * the number of bytes that are required to store each value will be
     * highly variable from one call to the next.
     * 
     * @param chaos a random number generator
     * @return a random number
     */
    public static int getRandomInt(Random chaos) {
        int maxBytes = (chaos.nextInt(4)) + 1;
        int next = java.lang.Math.abs(chaos.nextInt());
        if (maxBytes < 4) {
            next = next % ((1 << (8 * maxBytes)) - 1);
        }
        return next;
    }

    

    public static void writeArray(String name, long[] values) {
        // We'll always keep an up to date in-memory copy of
        // our stored arrays.  This method will act a little
        // like a write-through cache in that it will update
        // the in-memory copy, then write the data out to a
        // file.
        storedArrays.put(name, values);
        try {
            FileOutputStream fileOut = new FileOutputStream(dataFile, false);
            ObjectOutputStream objOut = new ObjectOutputStream(fileOut);
            objOut.writeObject(storedArrays);
        } catch (Exception ex) {
            // Again, I don't really care what went wrong... I just
            // can't write the data
            System.out.println("Unable to write to stored data file, will not be able to store test data");
        }
        
    }

    public static long[] readArray(String name) {
        return (long[])storedArrays.get(name);
    }
    
}
