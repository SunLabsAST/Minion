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
import org.xml.sax.SAXException;

/**
 *  An ElementHandler to handle <i>entry</i> XML Elements.<br>
 *  An &lt;entry&gt; element is used within several other elements to represent
 *  key-value pairs. This class deconstructs the content of the &lt;entry&gt;
 *  elements into two fields: key and value, according to the content of the element.
 * @author Bernard Horan
 *
 */
class EntryElementHandler extends ElementHandler {
    /**
     * A String representing the value of the key attribute of the element
     */
    private String key = null;

    /**
     * A String representing the content of the element
     */
    private String value = null;

    /**
     * Constructor to set the name of the element handler to "entry"
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#ElementHandler(MergeTestReplayer)
     */
    public EntryElementHandler(MergeTestReplayer replayer) {
        super(replayer);
        name = "entry";
    }

    /**
     * Gets the value of the <code>key</code> attribute.
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#startElement(java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String localName, Attributes atts) {
        super.startElement(localName, atts);
        key = atts.getValue("key");
    }

    /**
     * Gets the content of the element, and assigns it to the <code>value</code> field.
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#characters(char[], int, int)
     */
    public void characters(char[] text, int start, int length)
            throws SAXException {
        value = new String(text);
        value = value.substring(start, start + length);
    }

    /**
     * Depending on which element encloses the &lt;entry&gt; element, cause the replayer to:
     * <ul>
     * <li>compare the memory against that in the log</li>
     * <li>compare a system property against that in the log</li>
     * <li>compare a Java environment value against that in the log</li>
     * <li>add an command line argument to the replayer</li>
     * </ul>
     * @see com.sun.labs.minion.test.regression.merge.ElementHandler#endElement(java.lang.String)
     */
    public void endElement(String localName) {
        super.endElement(localName);
        if (peekElementStack().equals("Memory")) {
            replayer.checkMemory(key, value);
        }
        if (peekElementStack().equals("Properties")) {
            replayer.checkProperty(key, value);
        }
        if (peekElementStack().equals("Environment")) {
            replayer.checkEnvironment(key, value);
        }
        if (peekElementStack().equals("Arguments")) {
            replayer.addArgument(key, value);
        }
        key = null;
        value = null;
    }

    private Object peekElementStack() {
        return replayer.peekHandlerStack();
    }

}
