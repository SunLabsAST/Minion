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
package com.sun.labs.minion.test;


import java.util.Iterator;

import com.sun.labs.minion.util.Getopt;
import com.sun.labs.minion.indexer.entry.CasedDFOEntry;
import com.sun.labs.minion.indexer.entry.QueryEntry;
import com.sun.labs.minion.indexer.dictionary.DiskDictionary;
import com.sun.labs.minion.indexer.dictionary.StringNameHandler;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

public class ListDict {

    public static void usage() {
        System.out.println(
                "Usage: java IndexTest\n" +
                " -d <dictionary file>   " +
                "Dictionary to list\n" +
                " -e                     " +
                "Escape term names.\n" +
                " -o <output file>       " +
                "The file to output the dictionary to");
    }

    public static void main(String[] args) throws java.io.IOException {

        if(args.length == 0) {
            usage();
            return;
        }

        //
        // Set up the log.
        Logger logger = Logger.getLogger(ListDict.class.getName());

        String flags = "d:o:es:q";
        Getopt gopt = new Getopt(args, flags);
        int c;
        String dictFile = null;
        PrintWriter output = null;
        boolean escape = false;
        boolean quiet = false;
        int skip = 16;

        //
        // Handle the options.
        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    dictFile = gopt.optArg;
                    break;

                case 'e':
                    escape = true;
                    break;

                case 's':
                    skip = Integer.parseInt(gopt.optArg);
                    break;

                case 'q':
                    quiet = true;
                    break;

                case 'o':
                    if(!quiet) {
                        output = new PrintWriter(new BufferedWriter(
                                new OutputStreamWriter(
                                new FileOutputStream(
                                gopt.optArg), "UTF-8")));

                        output.println("<HTML>");
                        output.println("<HEAD>");
                        output.println("<meta http-equiv=\"Content-Type\" " +
                                "content=\"text/html; charset=utf-8\">");
                        output.println("</HEAD>");
                        output.println("<BODY>");
                        output.println("<CODE>");
                    }
                    break;
            }
        }

        if(dictFile == null) {
            logger.severe("Dictionary file is required");
            usage();
            return;
        }

        RandomAccessFile raf = new RandomAccessFile(dictFile, "r");
        raf.seek(skip);
        DiskDictionary d = new DiskDictionary(CasedDFOEntry.class,
                new StringNameHandler(),
                raf,
                null);
        Iterator i = d.iterator();
        int n = 0;
        while(i.hasNext()) {
            QueryEntry e = (QueryEntry) i.next();
            n++;
            if(!quiet) {
                if(output != null) {
                    output.println(e.toString() + "<br>");
                } else {
                    System.out.println(e.toString());
                }
            }
        }

        if(quiet) {
            System.out.println("N: " + n);
        }

        raf.close();

        if(!quiet && output != null) {
            output.println("</code></body></html>");
            output.flush();
        }


    }
} // DictTest
