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
 * An ElementHandler to handle <i>Diffing</i> XML Elements
 * @author Bernard Horan
 *
 */
class DiffingElementHandler extends ElementHandler {

    /**
     * * Constructor to set the name of the element handler to "Diffing"
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#ElementHandler(MergeTestReplayer)
     */
    public DiffingElementHandler(MergeTestReplayer replayer) {
        super(replayer);
        name = "Diffing";
    }
    /**
     * Report that we're starting the diff
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#startElement(java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement (String localName, Attributes atts) {
        super.startElement(localName, atts);
        System.out.println("Starting Diff");
    }

    
    /**
     * Report that we're ending the diff
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#endElement(java.lang.String)
     */
    public void endElement(String localName) {
        super.endElement(localName);
        System.out.println("Ending diff");
    }
}
