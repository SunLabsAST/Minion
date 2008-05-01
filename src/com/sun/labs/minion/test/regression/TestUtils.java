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

package com.sun.labs.minion.test.regression;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;

import com.sun.labs.minion.indexer.entry.Entry;

/**
 * Utility methods for the regression test package.
 */
public class TestUtils {

    /**
     * Writes to the given file all possible permutations of k entrys
     * from the given list of entrys. For example, if k is 3,
     * then a list containing all the possible permutations of 3 entrys
     * will be returned.
     *
     * @param k          the size of the resulting permutation
     * @param entryList  the list of entrys to choose from
     * @param file       the file to write to
     */
    public static void permutations(int k, List entryList, String file)
        throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        System.out.println("# of entries: " + entryList.size());
        Entry[] e = new Entry[k];
        visit(0, e, entryList, writer);
        writer.close();
    }

    /**
     * Generates a list of array of entries. The list represents all the
     * permutations of the given entries.
     *
     * @param k the position of the resulting entries array to start
     * @param e the array to place the temporarily generated entries array
     * @param entryList the source of entries
     * @param resultList the list to put the generated entries array
     */
    private static void visit(int k, Entry[] e,
                              List entryList, Writer writer)
        throws IOException {

        boolean isLast = (k == (e.length - 1));

        for (int i = 0; i < entryList.size(); i++) {
            e[k] = (Entry) entryList.get(i);
            boolean sameAsPrevious = false;
            for (int j = 0; j < k; j++) {
                if (e[j] == e[k]) {
                    sameAsPrevious = true;
                    break;
                }
            }
            if (!sameAsPrevious) {
                if (isLast) {
                    writeEntries(e, writer);
                } else {
                    visit((k + 1), e, entryList, writer);
                }
            }
        }
    }

    /**
     * Duplicates the given array of entries.
     *
     * @param entries the array entries to duplicate
     *
     * @return the duplicated array of entries
     */
    private static Entry[] duplicateEntries(Entry[] entries) {
        Entry[] duplicated = new Entry[entries.length];
        for (int i = 0; i < entries.length; i++) {
            duplicated[i] = entries[i];
        }
        return duplicated;
    }

    /**
     * Prints the given array of entries.
     *
     * @param entryArray the array of entries to print
     */
    public static void writeEntries(Entry[] entryArray, Writer writer)
        throws IOException {
        String result = "(";
        for (int i = 0; i < entryArray.length; i++) {
            result += entryArray[i].toString();
            if (i < (entryArray.length - 1)) {
                result += (", ");
            }
        }
        result += ")\n";
        writer.write(result);
    }
}
