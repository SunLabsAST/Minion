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

package com.sun.labs.minion.test.regression.merge;

import java.util.LinkedHashMap;
import java.util.Map;

import org.xml.sax.Attributes;


/**
 * An ElementHandler to handle <i>Indexing</i> XML Elements
 * @author Bernard Horan
 *
 */
class IndexingElementHandler extends ElementHandler {
    /**
     * Table to keep the files to be indexed. 
     * The keys of the table are the names of the files, and the keys represent
     * the success of the indexing operation as read from the log
     */
    private Map<String, Boolean> fileTable;

    /**
     * Constructor to set the name of the element handler to "Indexing".
     * Create the table to manager the files.
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#ElementHandler(MergeTestReplayer)
     */
    public IndexingElementHandler(MergeTestReplayer replayer) {
        super(replayer);
        name = "Indexing";
        fileTable = new LinkedHashMap<String, Boolean>();
    }
    
    /**
     * Start of indexing, clear the table that manages the files.
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#startElement(java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String localName, Attributes atts) {
        super.startElement(localName, atts);
        fileTable.clear();
    } 
    
    
    
    
    /**
     * Causes the replayer to index the files in the filetable
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#endElement(java.lang.String)
     */
    public void endElement (String localName) {
        super.endElement(localName);
        replayer.index(fileTable);
    }
    /**
     * Add an association between the name of a file that has been indexed 
     * and the success with which it was indexed
     * @param file the name of the file that was indexed
     * @param success the success with which it was indexed
     */
    public void addFile(String file, boolean success) {
        fileTable.put(file, success);
        
    }

}
