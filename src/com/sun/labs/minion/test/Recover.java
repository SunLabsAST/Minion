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

import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;

import java.util.HashSet;
import java.util.Set;

import java.util.logging.Handler;
import java.util.logging.Logger;
import com.sun.labs.minion.util.Getopt;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A class that can be used to recover an index, removing partitions that are
 * not in the active list and removing any remaining lock files.  It is meant
 * to be run on an index that <em>is not open</em>.  Running it on an index that
 * is open for indexing or query will likely result in errors.
 * 
 * <p>
 * 
 * Partitions that are removed from the index (i.e., that are not in the active list) 
 * will be moved into a recover directory where they can be inspected.  This 
 * recover directory will be an index like any other, with the proviso that it
 * is probably not a good idea to merge the partitions in it.
 * 
 */
public class Recover {

    protected static void move(File od, File nd, String fn) throws IOException {
        File oldFile = new File(od, fn);
        System.out.println("old file: " + oldFile);
        if(!oldFile.exists()) {
            return;
        }
        Files.move(oldFile.toPath(), new File(nd, fn).toPath());
    }

    public static void main(String[] args)
            throws NumberFormatException, IOException {
        String flags = "d:r:";

        //
        // Set up the logging for the search engine.  
        Logger logger = Logger.getLogger("");
        for(Handler h : logger.getHandlers()) {
            h.setFormatter(new SimpleLabsLogFormatter());
        }

        Thread.currentThread().setName("Recover");

        String indexDir = null;
        String recoverDir = null;
        Getopt gopt = new Getopt(args, flags);
        int c;

        while((c = gopt.getopt()) != -1) {
            switch(c) {

                case 'd':
                    indexDir = gopt.optArg;
                    break;

                case 'r':
                    recoverDir = gopt.optArg;
                    break;
            }
        }

        if(indexDir == null) {
            logger.severe("You must specify an index directory.");
            return;
        }

        if(recoverDir == null) {
            logger.severe("You must specify a recovery directory.");
            return;
        }

        //
        // Read the active file.
        File indexDirF = new File(indexDir);
        File idf = new File(indexDirF, "index");
        File rd = new File(recoverDir);
        File af = new File(idf, "AL.PM");
        DataInputStream in = new DataInputStream(new FileInputStream(af));
        int n = in.readInt();
        Set<Integer> s = new HashSet<Integer>(n);
        System.out.println(n + " partitions in active file: ");
        for(int i = 0; i < n; i++) {
            Integer p = new Integer(in.readInt());
            System.out.print(" " + p);
            s.add(p);
        }
        System.out.println("");
        in.close();

        //
        // List the files in the directory.
        File[] dir = idf.listFiles();

        if(dir == null) {
            logger.severe("Unable to list directory files.");
            return;
        }

        //
        // Walk through the files, removing locks and checking for orphaned
        // partitions.
        int partNum;
        for(int i = 0; i < dir.length; i++) {
            String name = dir[i].getName();
            if(name.charAt(0) == 'p') {

                //
                // Check if there's a partition number.
                int ind = name.indexOf('.');
                partNum = -1;
                if(ind > 0) {
                    try {
                        partNum = Integer.parseInt(name.substring(1, ind));
                    } catch(NumberFormatException nfe) {
                        partNum = -1;
                    }
                }
                if(partNum != -1 && !s.contains(new Integer(partNum))) {
                    move(idf, rd, dir[i].getName());
                }
            }
        }
    }
} // Recover

