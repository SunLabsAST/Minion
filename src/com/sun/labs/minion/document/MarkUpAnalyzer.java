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

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import com.sun.labs.minion.pipeline.Stage;

import com.sun.labs.minion.util.Util;

/**
 * An abstract class intended to be the superclass of all mark-up
 * analyzers.
 *
 */
public abstract class MarkUpAnalyzer {

    /**
     * A reader that can be used to read the data from the file.
     */
    protected Reader r;

    /**
     * The current character position in the input.
     */
    int pos;

    /**
     * The key for the document we're analyzing, so that we can report
     * errors usefully.
     */
    String key;

    /**
     * Makes a markup analyzer that will read from an input stream.
     * 
     * @param r 
     * @param pos The current position in the input stream, so that we can
     * keep accurate counts of where things are.
     * @param key The key for the document we're analyzing, so that we can
     * report errors usefully.
     */
    public MarkUpAnalyzer(Reader r, int pos, String key) {
	this.r = r;
	this.pos = pos;
	this.key = key;
    }

    /**
     * Makes a markup analyzer that will read from a string.
     *
     * @param s The string to read data from.
     * @param key The key for the document we're analyzing, so that we can
     * report errors usefully.
     * 
     */
    public MarkUpAnalyzer(String s, String key) {
	this.r = new StringReader(s);
	this.key = key;
    }

    /**
     * Gets a markup analyzer that's appropriate for the given file.  This
     * currently works by looking at the extension on the file.
     *
     * @param f The file that we're going to read from.
     * @param r The reader that we can read data from.
     */
    public static MarkUpAnalyzer getMarkUpAnalyzer(File f,
						   Reader r,
						   String key) {

	String ext = Util.getExtension(f.getName()).toLowerCase();
	
	//
	// Get the extension off the location.
	if(ext.equals("html") || ext.equals("htm")) {
	    return new MarkUpAnalyzer_html(r, 0, key);
	} else if(ext.equals("txt")) {
	    return new MarkUpAnalyzer_txt(r, 0, key);
	} else if(ext.equals("xml")) {
	    return new MarkUpAnalyzer_xml(r, 0, key);
	} else {
	    return new MarkUpAnalyzer_txt(r, 0, key);
	}
    }

    /**
     * Analyzes the current document.  This will pass the markup and text
     * events through to the tokenizer.
     *
     * @param stage the head of the pipeline that will process the text of the document
     * @throws java.io.IOException If there is any error reading.
     */
    public abstract void analyze(Stage stage) throws java.io.IOException;

} // MarkUpAnalyzer
