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

import com.sun.labs.minion.IndexableFile;
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
        //
        // If this is an indexable file, check the file type.
	if (f instanceof IndexableFile) {
            IndexableFile idxf = (IndexableFile)f;
            switch (idxf.getMarkupType()) {
                case TEXT:
                    return new MarkUpAnalyzer_txt(r, 0, key);
                case XML:
                    return new MarkUpAnalyzer_xml(r, 0, key);
                case HTML:
                    return new MarkUpAnalyzer_html(r, 0, key);
                //
                // By default, fall through and use the logic below
                default:
                    break;
            }
        }
        
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
     * Gets a markup analyzer that is appropriate for the given MIME type.
     * As there are many variations in how types are specified, this currently
     * looks only at the subtype for certain key words (xml and html).  If
     * a specific analyzer can't be chosen, a default text analyzer that just
     * reads the file as text is used.
     * 
     * @param mimeType the RFC 2046 mime type
     * @param r a reader with the contents of the data
     * @param key the document key
     * @return an appropriate markup analyzer
     */
    public static MarkUpAnalyzer getMarkUpAnalyzer(String mimeType,
                                                   Reader r,
                                                   String key) {
        //
        // If we can't get a subtype, use a text analyzer
        if (mimeType.indexOf('/') < 0) {
            return new MarkUpAnalyzer_txt(r, 0, key);
        }
        
        String subType = mimeType.substring(mimeType.indexOf('/') + 1);
        if (subType.contains("xml")) {
            return new MarkUpAnalyzer_xml(r, 0, key);
        } else if (subType.contains("html")) {
            return new MarkUpAnalyzer_html(r, 0, key);
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
