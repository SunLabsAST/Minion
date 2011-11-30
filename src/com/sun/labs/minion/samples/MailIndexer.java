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
package com.sun.labs.minion.samples;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SearchEngineFactory;
import com.sun.labs.minion.SimpleIndexer;
import com.sun.labs.util.SimpleLabsLogFormatter;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.EnumSet;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A main class that indexes a simplified RFC-822ish mail format (mostly
 * fewer headers and no MIME).  The aim is to be able to do regression testing
 * on changes to the field store to make sure that it's working correctly.
 *
 */
public class MailIndexer {

    /**
     * The engine that we'll use for searching.
     */
    protected SearchEngine engine;

    /**
     * A regex pattern for rfc822 headers.
     */
    protected Pattern header;

    private Logger logger;

    /**
     * Creates a mail indexer that will index into the given directory.
     * @param indexDir the name of the directory where the index data will be
     * stored
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * opening the engine.
     */
    public MailIndexer(String indexDir)
            throws SearchEngineException {

        //
        // Open the search engine for this directory.  Note that we're only 
        // providing the index directory here, because we're happy to use the 
        // default configuration for the engine.
        URL cu = getClass().getResource("mailIndexerConfig.xml");
        engine = SearchEngineFactory.getSearchEngine(indexDir, cu);
        logger = Logger.getLogger("com.sun.labs.minion.samples.MailIndexer");

        //
        // Get a couple of sets of attributes for the fields that we want to
        // define.
        EnumSet<FieldInfo.Attribute> ia =
                FieldInfo.getIndexedAttributes();
        EnumSet<FieldInfo.Attribute> sa = ia.clone();
        sa.add(FieldInfo.Attribute.SAVED);

        //
        // Define the fields. We could have done this in the config file, but 
        // since this is the only configuration change that we actually need to make,
        // it's simpler to put the field definitions here.  Note that it's perfectly
        // OK to define the fields every time that we open the search engine, as long
        // as we don't redefine any of the attributes.
        // 
        // The header fields are all saved, so that we can display them at query time.
        // The body is indexed but not saved.
        engine.defineField(new FieldInfo("from", sa,
                FieldInfo.Type.STRING));
        engine.defineField(new FieldInfo("subject", sa,
                FieldInfo.Type.STRING));
        engine.defineField(new FieldInfo("date", sa,
                FieldInfo.Type.DATE));
        engine.defineField(new FieldInfo("reference", sa,
                FieldInfo.Type.STRING));
        engine.defineField(new FieldInfo("sequence", sa,
                FieldInfo.Type.INTEGER));
        engine.defineField(new FieldInfo("body", ia));

        //
        // A pattern to match headers.
        header = Pattern.compile("^([^:]*):\\s*(.*)$");
    }

    public SearchEngine getSearchEngine() {
        return engine;
    }

    /**
     * Closes the search engine.  You need to do this, otherwise the maintenance threads that
     * the search engine starts will never be terminated.
     */
    public void close()
            throws SearchEngineException {
        engine.close();
    }

    public void indexMBox(String mbox)
            throws java.io.IOException, SearchEngineException {
        indexMBox(mbox, null);
    }

    /**
     * Indexes a mailbox at a given path
     * @param mbox the path to the mbox file we want to index
     * @param strip a string to strip from the starts of subjects.
     */
    public void indexMBox(String mbox, String strip)
            throws java.io.IOException, SearchEngineException {
        URL mbu = getClass().getResource(mbox);
        if(mbu == null) {
            mbu = (new File(mbox)).toURI().toURL();
        }
        indexMBox(mbu, strip);
    }

    public void indexMBox(URL mbox) throws java.io.IOException, SearchEngineException {
        indexMBox(mbox, null);
    }

    /**
     * Indexes a mailbox at a given location
     * @param mbox the path to the mbox file we want to index
     */
    public void indexMBox(URL mbox, String strip) throws java.io.IOException, SearchEngineException {
        BufferedReader r =
                new BufferedReader(new InputStreamReader(mbox.openStream()));
        logger.info("Indexing: " + mbox);


        //
        // We'll use a simple indexer to do the indexing, since that will allow
        // us to dump data into the index as we go, rather than storing it up and
        // then indexing it.  
        SimpleIndexer si = engine.getSimpleIndexer();
        String l = null;
        String key = null;
        StringBuilder body = new StringBuilder();
        boolean inHeader = false;
        int sequence = 1;
        while((l = r.readLine()) != null) {

            //
            // Check whether we've encountered the start of a message.  If so,
            // then we need to see whether we need to end the indexing of the last
            // message.
            if(l.startsWith("From ")) {
                if(key != null) {
                    //
                    // Finish off the last document.
                    si.addField("body", body.toString());
                    body.delete(0, body.length());
                    si.endDocument();
                    if(sequence % 100 == 0) {
                        logger.info("Indexed: " + sequence);
                    }
                }

                //
                // Start the new document.  Note that we generate a unique key
                // for the document that consists of the path to the mbox and 
                // the sequence number of the message in the mbox.
                key = mbox + "-" + sequence++;
                si.startDocument(key);
                si.addField("from", l.substring(5));
                si.addField("sequence", sequence);
                inHeader = true;
                continue;
            }

            //
            // Parse a header if we're in the header.
            if(inHeader) {

                //
                // A blank line is the end of the header.
                if(l.length() == 0) {
                    inHeader = false;
                    continue;
                }

                //
                // Match against the header, and save the two parts.
                Matcher m = header.matcher(l);
                if(!m.matches()) {
                    logger.severe("Bad header: \"" + l + "\" in " + key);
                } else {
                    String h = m.group(1);
                    String val = m.group(2);

                    if(strip != null && h.equalsIgnoreCase("subject")) {
                        if(val.startsWith(strip)) {
                            val = val.substring(strip.length()).trim();
                        }
                    }

                    //
                    // There might be multiple references, so we better deal 
                    // with that!
                    if(h.equalsIgnoreCase("references")) {
                        String[] refs = val.split("\\s+");
                        for(String ref : refs) {
                            si.addField("reference", ref);
                        }
                    } else {
                        si.addField(h, val);
                    }
                }
            } else {
                //
                // Collect the body.
                body.append(l);
            }
        }
        si.finish();
        logger.info("Indexed: " + (sequence - 1));
    }

    /**
     * A main program to index a number of mbox files given on the command 
     * line.
     * @param args the command line arguments
     * @throws java.io.IOException if there is any exception reading the mbox
     * files
     * @throws com.sun.labs.minion.SearchEngineException if there is any error
     * indexing the data
     */
    public static void main(String[] args)
            throws java.io.IOException, SearchEngineException {

        Logger l = Logger.getLogger("");
        l.setLevel(Level.INFO);
        for(Handler h : l.getHandlers()) {
            h.setFormatter(new SimpleLabsLogFormatter());
        }
        MailIndexer mi = new MailIndexer(args[0]);
        for(int i = 1; i < args.length; i++) {
            mi.indexMBox(args[i]);
        }
        mi.close();
    }
}
