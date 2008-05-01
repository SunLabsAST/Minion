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
 * An ElementHandler to handle <i>Diff</i> XML Elements
 * @author Bernard Horan
 *
 */
class DiffElementHandler extends ElementHandler {

    /**
     * Constructor to set the name of the element handler to "Diff"
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#ElementHandler(MergeTestReplayer)
     */
    public DiffElementHandler(MergeTestReplayer replayer) {
        super(replayer);
        name = "Diff";
    }
    /**
     * Gets the value of the <code>exitValue</code> attribute of the &lt;Diff&gt; element
     * and compares it with the 
     * value of the exitValue field in the replayer. Prints out an error if they differ.
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#startElement(java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement (String localName, Attributes atts) {
        super.startElement(localName, atts);
        String filename = atts.getValue("file");
        boolean success = Boolean.parseBoolean(atts.getValue("success"));
        replayer.diff(filename, success);
    }

}
