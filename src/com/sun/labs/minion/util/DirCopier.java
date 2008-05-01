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

import java.io.File;
import java.io.RandomAccessFile;

/**
 * A class that will copy data from a source directory to a target directory.
 */
public class DirCopier {
    
    private File source;
    
    private File target;
    public DirCopier(File source, File target) throws java.io.IOException {
        if(!source.isDirectory()) {
            throw new java.io.IOException(source + " is not a directory");
        }
        if(!target.exists()) {
            target.mkdirs();
        }
        if(!target.isDirectory()) {
            throw new java.io.IOException(target + " is not a directory");
        }
        this.source = source;
        this.target = target;
    }
    
    public void copy() throws java.io.IOException {
        copyDir(source, target);
    }
    
    private void copyDir(File sd, File td) throws java.io.IOException {
        File[] files = sd.listFiles();
        for(File f : files) {
            File nt = new File(td, f.getName());
            if(f.isDirectory()) {
                nt.mkdir();
                copyDir(f, nt);
            } else {
                copyFile(f, nt);
            }
        }
    }
    
    private void copyFile(File sf, File tf) throws java.io.IOException {
        RandomAccessFile sr = new RandomAccessFile(sf,"r");
        RandomAccessFile tr = new RandomAccessFile(tf, "rw");
        ChannelUtil.transferFully(sr.getChannel(), tr.getChannel());
        sr.close();
        tr.close();
    }

    public static void main(String[] args) throws java.io.IOException {
        if(args.length < 2) {
            System.err.println("Usage: DirCopier <srcdir> <targetdir>");
            return;
        }
        
        DirCopier dc = new DirCopier(new File(args[0]), new File(args[1]));
        dc.copy();
    }
}
