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

import java.io.BufferedReader;
import java.io.Reader;
import java.util.EnumSet;

import java.util.Map;
import java.util.HashMap;

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.pipeline.Stage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of a SAX XML handler that we can use to parse one or
 * more XML documents.
 */
public class XMLAnalyzer extends DefaultHandler {

    /**
     * The markup analyzer that's using us.
     */
    protected MarkUpAnalyzer mua;

    /**
     * The reader to use to parse the document.
     */
    protected Reader r;

    /**
     * The tokenizer that we will pass our events and characters to.
     */
    protected Stage stage;

    /**
     * The XML reader that we're using for parsing.
     */
    protected XMLReader xr;

    /**
     * The position in the current field.
     */
    protected int pos;

    /**
     * A map from tag names to field information objects.
     */
    protected Map fields = new HashMap();

    Logger logger = Logger.getLogger(getClass().getName());

    protected static String logTag = "XMLA";

    /**
     * Instantiates a handler for the given document that will use the
     * given tokenizer to tokenize the document.
     *
     * @param r The reader from which we'll read the document.
     * @param stage the head of the pipeline that we'll use for processing text
     *
     * @throws org.xml.sax.SAXException If there is a problem making the
     * <code>XMLReader</code>.
     */
    public XMLAnalyzer(Reader r, Stage stage, MarkUpAnalyzer mua)
            throws org.xml.sax.SAXException {
        super();

        this.r = new BufferedReader(r);
        this.mua = mua;
        this.stage = stage;

        //
        // Create the reader, then hand it ourself as the handler.
        xr = XMLReaderFactory.createXMLReader();

        xr.setContentHandler(this);
        xr.setErrorHandler(this);
        xr.setFeature(
                "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                false);
    }

    /**
     * Actually analyze the document.
     *
     */
    public void analyze() {
        try {
            xr.parse(new InputSource(r));
        } catch(org.xml.sax.SAXParseException se) {
            logger.warning("Error parsing XML " + mua.key + " " +
                    se.getLineNumber() + " " + se.getMessage());
            endDocument();
        } catch(org.xml.sax.SAXException se) {
            logger.warning("Error parsing XML " + mua.key + " " +
                    "line: " + se.getMessage());
            endDocument();
        } catch(java.io.IOException ioe) {
            logger.log(Level.WARNING, "Error reading during XML " + mua.key,
                    ioe);
            endDocument();
        }
    }

    /**
     * Begin tag.
     */
    public void startElement(String namespaceURI, String localName,
            String qName, Attributes atts) {

        //
        // We'll keep character positions from the start of each element.
        pos = 0;

        //
        // See if we have a field info object for this field.
        String hk = qName.toLowerCase();
        FieldInfo fi = (FieldInfo) fields.get(hk);

        if(fi == null) {
            fi = new FieldInfo(hk, EnumSet.of(FieldInfo.Attribute.INDEXED,
                    FieldInfo.Attribute.TOKENIZED));
            fields.put(hk, fi);
        }

        stage.startField(fi);
    }

    /**
     * Character data.
     */
    public void characters(char[] ch, int start, int length) {
        stage.text(ch, start, start + length);
        pos += length;
    }

    /**
     * End tag.
     */
    public void endElement(String namespaceURI, String localName,
            String qName) {
        stage.endField((FieldInfo) null);
    }

    /**
     * Document end.
     */
    public void endDocument() {
        stage.endDocument(0);
    }
} // XMLAnalyzer
