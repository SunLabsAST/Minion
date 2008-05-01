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
 * Class ElementHandler provides a simple base class for subclasses to 
 * implement the behaviour associated with the SAX parser methods:
 * <ul>
 * <li>{@link org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)}</li>
 * <li>{@link org.xml.sax.ContentHandler#characters(char[], int, int)}</li>
 * <li>{@link org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)}</li>
 * </ul>
 * Each subclass should deal with a particular XML element that is encountered during parsing
 * 
 * @author Bernard Horan
 *
 */
class ElementHandler {
    /**
     * A reference to the MergeTestReplayer that is the originator of the parsing of some XML
     */
    protected MergeTestReplayer replayer;

    /**
     * A name to identify this handler that corresponds to the element for which it is responsible
     */
    protected String name = null;

    /**
     * Constructor to set the reference to the originator of the XML parsing
     * @param replayer
     */
    public ElementHandler(MergeTestReplayer replayer) {
        this.replayer = replayer;
    }

    /**
     * Called by a handler to handle the start of an element.
     * Subclasses <em>should</em> call super.startElement() to ensure that its name is checked.<br>
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String localName, Attributes atts) {
        checkName(localName);
    }

    /**
     * Check the parameter against the name of the element that this handler handles
     * @param localName the name of an XML element to be compared with the name of this handler
     */
    private void checkName(String localName) {
        if (!localName.equals(name)) {
            throw new RuntimeException("Names don't match");
        }
    }

    /**
     * Called by a handler to handle the end of an element.
     * Subclasses <em>should</em> call super.endElement() to ensure that its name is checked.<br>
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String localName) {
        checkName(localName);
    }

    /**
     * Called by a handler to handle the character content of an element.
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     
     */
    public void characters(char[] text, int start, int length)
            throws SAXException {
    }

    /**
     * Gets the name of this element handler
     * @return a String corresponding to the name of the element handled by this handler
     */
    public String getName() {
        return name;
    };
}
