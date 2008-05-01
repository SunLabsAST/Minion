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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * @author bh37721
 *
 */
public class MergeReportHandler extends DefaultHandler {
    /**
     * The replayer that's using this handler to parse XML
     */
    private MergeTestReplayer replayer;
    /**
     * A stack to maintain which element is currently being parsed
     */
    Stack<String> handlerStack = new Stack<String>();
    /**
     * a table of <code>ElementHandler<code> subclasses that are responsible for dealing 
     * with each XML element
     */
    private Map<String, ElementHandler> elementHandlers;
    
    

    /**
     * Create a new instance of me and set my replayer object
     * @param replayer the MergeTestReplayer that is parsing some XML stream
     */
    public MergeReportHandler(MergeTestReplayer replayer) {
        super();
        this.replayer = replayer;
        initializeElementHandlers();
    }
    
    /**
     * INitialse the table of element handlers.<br>
     * Each handler has a name and is responsible for dealing with the start, end and characters 
     * in an XML element.
     * @see ElementHandler
     */
    private void initializeElementHandlers() {
        elementHandlers = new HashMap<String, ElementHandler>();
        
        ElementHandler entryHandler = new EntryElementHandler(replayer);
        elementHandlers.put(entryHandler.getName(), entryHandler);
        
        ElementHandler configurationHandler = new ConfigurationElementHandler(replayer);
        elementHandlers.put(configurationHandler.getName(), configurationHandler);
        
        ElementHandler diffingHandler = new DiffingElementHandler(replayer);
        elementHandlers.put(diffingHandler.getName(), diffingHandler);
        
        ElementHandler invertingHandler = new InvertingElementHandler(replayer);
        elementHandlers.put(invertingHandler.getName(), invertingHandler);
        
        ElementHandler optimizingHandler = new OptimizingElementHandler(replayer);
        elementHandlers.put(optimizingHandler.getName(), optimizingHandler);
        
        ElementHandler indexHandler = new IndexElementHandler(replayer);
        elementHandlers.put(indexHandler.getName(), indexHandler);
        
        ElementHandler diffHandler = new DiffElementHandler(replayer);
        elementHandlers.put(diffHandler.getName(), diffHandler);
        
        ElementHandler indexingHandler = new IndexingElementHandler(replayer);
        elementHandlers.put(indexingHandler.getName(), indexingHandler);
    }
        

    /**
     * Find the elementhandler that's responsible for this element and hand off to it.<br>
     * Push the name of this element onto the handlerStack.
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String namespaceURI,  String localName, String qualifiedName, Attributes atts) {
        ElementHandler handler = getElementHandler(localName);
        if (handler == null) {
            reportMissingHandler(localName);
        } else {
            handler.startElement(localName, atts);
        }
        handlerStack.push(localName);
    }

    /**
     * For debuggng purposed
     * @param elementName the name of the element for which there is no handler
     */
    private void reportMissingHandler(String elementName) {
        if (replayer.DEBUG) {
            System.err.println("Handler missing for: " + elementName);
        }
        
    }

    /**
     * Check that the name of the element matches the name on the top of the stack.<br>
     * Find the elementhandler that's responsible for this element and hand off to it.
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String namespaceURI, String localName, String qualifiedName) {
        if (!localName.equals(handlerStack.pop())) {
            throw new RuntimeException("Stack buggered");
        }
        ElementHandler handler = getElementHandler(localName);
        if (handler == null) {
            reportMissingHandler(localName);
            return;
        }
        handler.endElement(localName);
    }
           
    

    /**
     * Get the name of the element that's currently being parsed (the top of the stack) and hand off to it.
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] text, int start, int length) throws SAXException {
        ElementHandler handler = getElementHandler(peekHandlerStack());
        if (handler == null) {
            reportMissingHandler(peekHandlerStack());
            return;
        }
        handler.characters(text, start, length);
    }

    /**
     * @return the element at the top of the handlerStack (but don't pop it)
     */
    public String peekHandlerStack() {
        return handlerStack.peek();
    }
    
    ElementHandler getElementHandler(String handlerName) {
        return elementHandlers.get(handlerName);
    }

    

}
