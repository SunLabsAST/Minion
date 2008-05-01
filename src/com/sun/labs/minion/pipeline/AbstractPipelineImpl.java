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

package com.sun.labs.minion.pipeline;

import com.sun.labs.minion.CustomAnalyzer;
import com.sun.labs.minion.IndexableFile;
import com.sun.labs.minion.IndexableString;
import com.sun.labs.minion.Log;
import com.sun.labs.minion.Pipeline;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import com.sun.labs.minion.document.MarkUpAnalyzer;
import com.sun.labs.minion.document.MarkUpAnalyzer_html;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.Posting;
import com.sun.labs.minion.indexer.partition.Dumper;
import com.sun.labs.minion.indexer.partition.InvFileMemoryPartition;

/**
 * An abstract implementation of pipeline.
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public abstract class AbstractPipelineImpl implements Pipeline {

    /**
     * The list of stages making up the pipeline.
     */
    protected List<Stage> pipeline;

    /**
     * The stage at the head of the pipeline.
     */
    protected Stage head;

    /**
     * A place where we can dump our data.
     */
    protected Dumper dumper;

    /**
     * A date format for dates including milliseconds.
     */
    protected SimpleDateFormat d64;

    /**
     * The engine that is using this pipeline.
     */
    protected SearchEngine engine;

    /**
     * The log.
     */
    protected static Log log = Log.getLog();

    /**
     * Our log tag.
     */
    protected static String logTag = "PI";

    /**
     * Character data taken from a field.
     */
    protected char[] text;

    /**
     * The factory that created this pipeline.
     */
    private PipelineFactory factory;

    /**
     * Creates a AbstractPipelineImpl
     */
    public AbstractPipelineImpl(PipelineFactory factory,
                                SearchEngine engine,
                                List<Stage> pipeline,
                                Dumper dumper) {
        this.factory = factory;
        this.engine = engine;
        this.pipeline = pipeline;
        this.dumper = dumper;
        head = pipeline.get(0);
        text = new char[1024];
    }

    public SearchEngine getEngine() {
        return engine;
    }

    public Stage getHead() {
        return head;
    }


    /**
     * Gets the indexer stage for this pipline.  This can be used by a
     * simple indexer.
     */
    public Stage getIndexer() {
        return pipeline.get(pipeline.size() - 1);
    }

    /**
     * Sets the indexing stage for this pipeline.  This allows us to put in
     * a new indexing stage while dumps are happening asynchronously.
     */
    public void setIndexer(Stage s) {

        //
        // If there is more than one stage in the pipeline, we need to
        // splice in this new stage.
        if(pipeline.size() > 1) {
            Stage prev = pipeline.get(pipeline.size() - 2);
            prev.setDownstream(s);
        } else {
            head = s;
        }
        pipeline.set(pipeline.size() - 1, s);
    }

    /**
     * Handles a single field value, which may have several instances.
     */
    protected void handleField(FieldInfo fi, Object val)
            throws IOException {
        // We'll have cases here for things that need to be handled
        // in a specific way. Everything else falls through to the
        // catch all.
        if(val instanceof String[]) {

            String[] s = (String[]) val;
            for(int j = 0; j < s.length; j++) {
                if(s[j] != null) {
                    docSize +=
                            handleField(fi, s[j],
                                        IndexableString.Type.PLAIN);
                }
            }
        } else if(val instanceof Object[]) {
            for(Object o : (Object[]) val) {
                handleField(fi, o);
            }
        } else if(val instanceof IndexableString) {
            docSize +=
                    handleField(fi,
                                ((IndexableString) val).getValue(),
                                ((IndexableString) val).getMarkupType());
        } else if(val instanceof IndexableString[]) {
            IndexableString[] s = (IndexableString[]) val;
            for(int j = 0; j < s.length;
                    j++) {
                if(s[j] != null) {
                    docSize +=
                            handleField(fi, s[j].getValue(),
                                        s[j].getMarkupType());
                }
            }
        } else if(val instanceof File) {
            //
            // Get an input stream for the file, and then make a
            // markup analyzer for it.
            File file = (File) val;
            if(file.isFile()) {
                FileInputStream fis = new FileInputStream(file);
                InputStreamReader isr;
                if(val instanceof IndexableFile) {
                    isr =   new InputStreamReader(fis,
                                                  ((IndexableFile) val).getEncoding());
                } else {
                    isr = new InputStreamReader(fis);
                }
                try {
                    MarkUpAnalyzer mua =
                            MarkUpAnalyzer.getMarkUpAnalyzer(file, isr, currKey);

                    mua.analyze(head);
                } finally {
                    isr.close();
                }
                docSize += file.length();
            } else {
                log.warn(logTag, 3, file + " is not a file");
            }
        } else if(val instanceof Collection) {

            //
            // We'll just recurse for each member of the collection.
            for(Object o : (Collection) val) {
                handleField(fi, o);
            }
        } else {
            docSize += handleField(fi, val, IndexableString.Type.PLAIN);
        }
    }


    /**
     * Handle the characters for one instance of a given field.
     *
     * @param fi
     *            The field information for the field we're adding to.
     * @param val
     *            The data as a string.
     * @param type
     *            The type of markup in the value. This is take from the
     *            {@link com.sun.labs.minion.IndexableString.Type} enum.
     * @return the number of characters indexed
     *
     * @see IndexableString
     * @see IndexableFile
     */
    protected long handleField(FieldInfo fi, Object val,
                               IndexableString.Type type) {

        if(val == null) {
            return 0;
        }

        boolean indexed = true;
        boolean tokenized = true;
        boolean saved = false;
        if(fi != null) {
            indexed = fi.isIndexed();
            tokenized = fi.isTokenized();
            saved = fi.isSaved();
        }
        
        //
        // Handle a posting here.
        if(val instanceof Posting) {
            Posting p = (Posting) val;
            if(p.getTerm() == null) {
                log.warn(logTag, 3, "Null term in posting", new Exception("Here"));
                return 0;
            }
            if(fi != null) {
                head.startField(fi);
            }
            head.token(new Token(p.getTerm(),
                    p.getFreq()));
            if(fi != null) {
                if(fi.isSaved()) {
                    head.savedData(p.getTerm());
                }
                head.endField(fi);
            }
            return p.getTerm().length();
        }

        //
        // If a field is to be indexed or tokenized, we'll need to turn it
        // into a string.  The tokenizer will have to worry about saving
        // the data.
        if(indexed || tokenized) {

            if(fi != null) {
                head.startField(fi);
            }

            String sval;
            if(val instanceof Date) {
                //
                // We need to format dates with milliseconds!  Note that
                // com.sun.labs.minion.util.NDateParser needs to know about this format!
                if(d64 == null) {
                    d64 =   new SimpleDateFormat("E, d MMM y h:m:s.S a z");
                }
                sval = d64.format((Date) val);
            } else if (val instanceof IndexableString) {
                sval = ((IndexableString)val).getValue();
            } else {
                sval = val.toString();
            }

            //
            // Get the characters from the value.
            int len = sval.length();
            if(text.length <= len) {
                text = new char[len];
            }
            sval.getChars(0, len, text, 0);

            //
            // Analyze the characters, using the given type.
            switch(type) {
                case PLAIN:

                    head.text(text, 0, len);
                    break;
                case HTML:

                    MarkUpAnalyzer hmu =
                            new MarkUpAnalyzer_html(sval, currKey);

                    try {
                        hmu.analyze(head);
                    } catch(IOException ioe) {
                        log.warn(logTag, 2,
                                 "Error reading from HTML string for: " + currKey);
                    }

                    break;
                case CUSTOM:
                    CustomAnalyzer ca = ((IndexableString)val).getCustomAnalyzer();
                    try {
                        ca.analyze(((IndexableString)val).getValue(), head);
                    } catch (IOException ioe) {
                        log.warn(logTag, 2, "Error reading text from custom analyzer for " + currKey + ": " + ioe.getMessage());
                    }
                    break;
            }
            if(fi != null) {
                head.endField(fi);
            }

            return len;
        } else if(saved) {

            if(val instanceof Date &&
               fi.getType() != FieldInfo.Type.DATE) {
                log.warn(logTag, 4,
                         "Date added to non-date saved field: " + fi.getName());
            }

            //
            // If the data is only to be saved, then send it down in its
            // original format.
            head.startField(fi);
            if (val instanceof IndexableString) {
                head.savedData(((IndexableString)val).getValue());
            } else {
                head.savedData(val);
            }
            head.endField(fi);
            return 0;
        }

        return 0;
    }


    /**
     * Does the actual work of indexing a document.
     */
    protected void indexDoc(String key, Map m)
            throws SearchEngineException {

        boolean ind = false;

        //
        // We may get a null map from Indexable.getMap(), so we should skip
        // this doc.
        if(m == null) {
            return;
        }

        try {
            //
            // Start the document.
            startDocument(key);
            ind = true;
            for(Iterator i = m.entrySet().iterator(); i.hasNext();) {
                Entry e = (Map.Entry) i.next();
                String fn = (String) e.getKey();
                FieldInfo fi = null;
                if(fn != null) {
                    fi = engine.getManager().getMetaFile().getFieldInfo(fn);
                    if(fi == null) {
                        //
                        // Make an implied field
                        fi = addImpliedField(fn);
                    }
                }
                handleField(fi, e.getValue());
            }

            //
            // End the document.
            endDocument();
        } catch(Exception e) {

            //
            // If we were in a document when things went south, then
            // end the document, so that we're in a good state.
            if(ind) {

                //
                // Try to end the document, but we may have gotten an
                // exception caused by a dump of the partition that we
                // caught to get here, so we need to make sure we handle
                // that and report the original cause!
                try {
                    endDocument();
                } catch(Exception e2) {
                }
            }

            //
            // Propagate the original exception.
            throw new SearchEngineException("Error indexing document: " + key, e);
        }
    }

    public synchronized FieldInfo addImpliedField(String name) {

        log.log(logTag, 4, "Defining undefined field: " + name);
        
        //
        // Create the field using the defaults defined in the index config.
        FieldInfo fi = engine.getIndexConfig().getDefaultFieldInfo(name);

        //
        // Since this is the first time we've seen this one, inform
        // the pipeline that there's a new one.
        fi = head.defineField(fi);
        return fi;
    }

    /**
     * Indicates whether a document has been indexed or not.  A document
     * has been indexed if its document key is in the index and the
     * document has not been deleted.
     *
     * @param key the document key for the document that we wish to check.
     * @return <code>true</code> if the document is in the index,
     * <code>false</code> otherwise.  A document has been indexed if its
     * document key is in the index and the document has not been deleted.
     */
    public boolean isIndexed(String key) {
        return engine.isIndexed(key);
    }


    /**
     * Ends the current document.
     *
     * @throws SearchEngineException if there is any error ending the document.
     */
    public void endDocument() {
        head.endDocument(docSize);
    }


    /**
     * Starts the indexing of a document.
     *
     * @param key the document key for the document.  If this is a
     * duplicate, then any old data associated with the document will be
     * removed.
     * @throws NullPointerException if the document key is null
     */
    public void startDocument(String key) {
        if(key == null) {
            throw new NullPointerException("Null document key");
        }
        head.startDocument(key);
        currKey = key;
        docSize = 0;
    }

    /**
     * Does actual dumping of the indexed data and resetting of the indexing stage
     * so that dumps may proceed asynchronously, if required.
     */
    protected void realDump() {
        dumper.dump(getIndexer());
        setIndexer(factory.getIndexingStage());
    }

    /**
     * The current key that we're working on.
     */
    protected String currKey;


    /**
     * The size of the current document, in characters.
     */
    protected long docSize;
}
