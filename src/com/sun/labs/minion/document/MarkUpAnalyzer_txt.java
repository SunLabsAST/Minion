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

package com.sun.labs.minion.document;

import java.io.Reader;
import com.sun.labs.minion.pipeline.Stage;

public class MarkUpAnalyzer_txt extends MarkUpAnalyzer {

    /**
     * Makes an analyzer for data from a file.
     */
    public MarkUpAnalyzer_txt(Reader r, int pos, String key) {
	super(r, pos, key);
    }

    /**
     * Makes an analyzer for data from a file.  This is not really useful,
     * but there you go.
     */
    public MarkUpAnalyzer_txt(String s, String key) {
	super(s, key);
    }

    /**
     * Analyzes the document.  Mostly reads in chunks and tokenizes them.
     */
    public void analyze(Stage stage) 
	throws java.io.IOException {

	char[] buffer = new char[8192];
	int fileBuffEnd;

	//
	// Process text until the end of file is encountered.
	while(true) {

	    //
	    // Fill the buffer.
	    fileBuffEnd = r.read(buffer, 0, buffer.length);

	    //
	    // If we hit EOF or didn't read anything, we're done.
	    if(fileBuffEnd <= 0) {
		
		//
		// Break out of the enclosing while.
		break;
	    }

	    stage.text(buffer, 0, fileBuffEnd);

	    pos += fileBuffEnd;
	}
    }

} // MarkUpAnalyzer_txt
