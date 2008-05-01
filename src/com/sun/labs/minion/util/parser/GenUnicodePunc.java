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

package com.sun.labs.minion.util.parser;

import java.io.PrintStream;
import java.util.ArrayList;

/**
 * 
 * @author Jeff Alexander
 */

public class GenUnicodePunc
{

    public GenUnicodePunc() {

        }

    public static class Range {
        public char start;
        public char end;
    }
    
    public static void main(String[] args) throws Exception {
        Range currRange = null;
        char prevPunc = Character.MIN_VALUE;
        ArrayList<Range> list = new ArrayList();
        ArrayList<Range> singles = new ArrayList();
        
        //PrintStream o = new PrintStream(System.out, true, "UTF-8");
        for (char curr = Character.MIN_VALUE; curr < Character.MAX_VALUE; curr++) {
            if (Character.getType(curr) == Character.DASH_PUNCTUATION
                || Character.getType(curr) == Character.CONNECTOR_PUNCTUATION
                || Character.getType(curr) == Character.END_PUNCTUATION
                || Character.getType(curr) == Character.FINAL_QUOTE_PUNCTUATION
                || Character.getType(curr) == Character.INITIAL_QUOTE_PUNCTUATION
                || Character.getType(curr) == Character.OTHER_PUNCTUATION
                || Character.getType(curr) == Character.START_PUNCTUATION
                )
            {
                if (curr == prevPunc + 1) {
                    prevPunc = curr;
                } else {
                    if (currRange != null) {
                        currRange.end = prevPunc;
                        if (currRange.start == currRange.end) {
                            singles.add(currRange);
                        } else {
                            list.add(currRange);
                        }
                    }
                    currRange = new Range();
                    currRange.start = curr;
                    prevPunc = curr;
                }

            }
            /**
               
            if (Character.getType(curr) == Character.CONNECTOR_PUNCTUATION) {
                o.format("%c is connector (\\u%04x)\n", curr,  (int)curr);
            }
            if (Character.getType(curr) == Character.END_PUNCTUATION) {
                o.format("%c is end (\\u%04x)\n", curr,  (int)curr);
            }
            if (Character.getType(curr) == Character.FINAL_QUOTE_PUNCTUATION) {
                o.format("%c is final_quote (\\u%04x)\n", curr,  (int)curr);
            }
            if (Character.getType(curr) == Character.INITIAL_QUOTE_PUNCTUATION) {
                o.format("%c is initial_quote (\\u%04x)\n", curr,  (int)curr);
            }
            if (Character.getType(curr) == Character.OTHER_PUNCTUATION) {
                o.format("%c is other (\\u%04x)\n", curr,  (int)curr);
            }
            if (Character.getType(curr) == Character.START_PUNCTUATION) {
                o.format("%c is start (\\u%04x)\n", curr,  (int)curr);
            }
            */
        }
        currRange.end = prevPunc;
        list.add(currRange);

        //
        // Now list out all the ranges
        for (Range r : list) {
            System.out.format("\"\\u%04x\"-\"\\u%04x\",\n", (int)r.start, (int)r.end);
        }

        //
        // And the singleton values
        for (Range r : singles) {
            System.out.format("\"\\u%04x\" |\n", (int)r.start);
        }
    }
}
