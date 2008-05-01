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

import java.io.DataInputStream;
import java.io.FileInputStream;

/**
 * A main program that will read and display the partitions currently in the 
 * active file.  This program does not lock the active file, which may result
 * in odd results for an index that is in use.
 */
public class RActive {
    public static void main(String[] args)
	throws NumberFormatException, java.io.IOException {
        
        if(args.length == 0) {
            System.err.println("Usage: RActive <activeFile>");
            return;
        }

	DataInputStream in = new DataInputStream(
	    new FileInputStream(args[0]));
	int n = in.readInt();
	System.out.println(n + " partitions in active file: " );
	for(int i = 0; i < n; i++) {
	    int p = in.readInt();
	    System.out.print(" " + p);
	}
	System.out.println("");
	in.close();
    }
} // WActive
