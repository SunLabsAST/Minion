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

import org.xml.sax.Attributes;

/**
 * An ElementHandler to handle <i>Index</i> XML Elements
 * @author Bernard Horan
 *
 */
class IndexElementHandler extends ElementHandler {
    private String file = null;
    private boolean success = false;

    /**
     * Constructor to set the name of the element handler to "Index"
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#ElementHandler(MergeTestReplayer)
     */
    public IndexElementHandler(MergeTestReplayer replayer) {
        super(replayer);
        name = "Index";
    }
    /**
     * Gets the vallue of the <code>file</code> attribute 
     * and the value of the <code>success</code> attribute.
     * 
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#startElement(java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement (String localName, Attributes atts) {
        super.startElement(localName, atts);
        file = atts.getValue("file");
        success = new Boolean(atts.getValue("success"));
    }
    
    /**
     * Add the name of the file and the success with which it was indexed to the filetable
     * managed by the IndexingElementHandler.
     * Then reset the fields to null (to stop any potential side-effects)
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#endElement(java.lang.String)
     */
    public void endElement(String localName) {
        super.endElement(localName);
        if(replayer.peekHandlerStack().equals("Indexing")) {
            IndexingElementHandler handler = (IndexingElementHandler) replayer.getElementHandler("Indexing");
            handler.addFile(file, success);
        }
        file = null;
        success = false;
    }
    

}
