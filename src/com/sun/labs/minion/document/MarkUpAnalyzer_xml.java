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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A wrapper around an XML analyzer that we can use in our typical context.
 */
public class MarkUpAnalyzer_xml extends MarkUpAnalyzer {

    protected XMLAnalyzer xa;

    static Logger logger = Logger.getLogger(MarkUpAnalyzer_xml.class.getName());

    protected static String logTag = "MUX";

    public MarkUpAnalyzer_xml(Reader r, int pos, String key) {
        super(r, 0, key);
    }

    public MarkUpAnalyzer_xml(String s, String key) {
        super(s, key);
    }

    /**
     * Analyze the document.
     */
    public void analyze(Stage stage) {

        try {
            xa = new XMLAnalyzer(r, stage, this);
        } catch(org.xml.sax.SAXException se) {
            logger.log(Level.WARNING, "Error creating XML analyzer", se);
            xa = null;
        }

        //
        // If there were problems in initialization, we're done.
        if(xa == null) {
            return;
        }
        xa.analyze();
    }
} // MarkUpAnalyzer_xml
