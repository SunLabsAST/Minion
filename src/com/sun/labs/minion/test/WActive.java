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

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * A main program that will write an active file.  Note that this can be a 
 * dangerous thing to do on an index that is open!
 */
public class WActive {
    
    String activeFile;
    public WActive(String activeFile) {
        this.activeFile = activeFile;
    }
    
    public void writeActiveFile(String[] args) 
	throws NumberFormatException, java.io.IOException {

        Set<Integer> parts = new TreeSet<Integer>();
	for(int i = 0; i < args.length; i++) {
            String v = args[i];
            int p = v.indexOf('-');
            if(p < 0) {
                parts.add(Integer.parseInt(v));
            } else if(p == 0) {
                parts.remove(new Integer(v.substring(1)));
            } else {
                int start = Integer.parseInt(v.substring(0, p));
                int end = Integer.parseInt(v.substring(p+1));
                for(int j = start; j <= end; j++) {
                    parts.add(j);
                }
            }
	}
        writeActiveFile(parts);
    }
    
    public void writeActiveFile(List<Integer> parts)  throws java.io.IOException {
        writeActiveFile(new TreeSet<Integer>(parts));
    }
    
    public void writeActiveFile(Set<Integer> parts) throws java.io.IOException {
	DataOutputStream out = new DataOutputStream(
	    new FileOutputStream(activeFile));
	out.writeInt(parts.size());
        for(Integer p : parts) {
            out.writeInt(p);
        }
	out.close();
    }
    
    public static void main(String args[]) throws NumberFormatException, java.io.IOException {
        
        if(args.length <= 1) {
            System.err.println("Usage: WActive <activeFile> [partNum] [partNum]...");
        }
        String activeFile = args[0];
        String[] parts = new String[args.length-1];
        System.arraycopy(args, 1, parts, 0, args.length-1);
        WActive wa = new WActive(activeFile);
        wa.writeActiveFile(parts);
    }
} // WActive
