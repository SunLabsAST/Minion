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

import com.sun.labs.minion.Document;
import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import com.sun.labs.minion.SimpleIndexer;
import java.util.List;
import com.sun.labs.minion.engine.DocumentImpl;
import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.minion.IndexableString;
import java.util.Collection;
import java.util.Date;
import com.sun.labs.minion.indexer.partition.Dumper;
import java.util.logging.Level;

/**
 *
 * @author Stephen Green <stephen.green@sun.com>
 */
public class SyncPipelineImpl extends PipelineImpl implements
        SimpleIndexer {

    /**
     * Whether we're currently in a document.
     */
    protected boolean inDoc;

    /**
     * Whether we're finished as a <code>SimpleIndexer</code>.
     */
    protected boolean simpleIndexingFinished;

    /**
     * The current active field for indexing.
     */
    private FieldInfo currField;

    /**
     * The current array of active fields.
     */
    private int[] fields = new int[32];

    /**
     * Creates a synchronous indexing pipeline
     * 
     * @param factory the factory that generated this pipeline
     * @param engine the engine into which this pipeline will index documents
     * @param pipeline the stages of the pipeline
     * @param dumper the dumper that will be used to dump partitions after
     * indexing
     */
    public SyncPipelineImpl(PipelineFactory factory,
            SearchEngine engine,
            List<Stage> pipeline,
            Dumper dumper) {
        super(factory, engine, pipeline, dumper);
    }

    public void index(Indexable doc) throws SearchEngineException {
        indexDoc(doc.getKey(), doc.getMap());
    }

    public void flush() {
        realDump();
    }

    public void dump() {
        realDump();
    }

    /**
     * Purge the contents of this pipeline / partition.  Since the pipeline
     * is synchronous, just call realPurge to dump the data that has already
     * been indexed in-memory.
     */
    public void purge() {
        realPurge();
    }

    public void shutdown() {
        realDump();
    }

    /**
     * Indexes a whole document at once.  You must not do this in the middle of
     * another document!
     *
     * @param doc a document to index.
     */
    public void indexDocument(Indexable doc) {
        stateCheck();
        if(inDoc) {
            throw new IllegalStateException("Cannot call indexDocument " +
                    "when alreading indexing a document!");
        }
        try {
            index(doc);
        } catch(SearchEngineException ex) {
            logger.severe("Error indexing: " + doc.getKey());
        }
    }

    /**
     * Indexes a whole document at once.  You must not do this in the middle of
     * another document!
     *
     * @param doc a document to index.
     */
    public void indexDocument(Document doc) throws SearchEngineException {
        stateCheck();
        if(inDoc) {
            throw new IllegalStateException("Cannot call indexDocument " +
                    "when alreading indexing a document!");
        }
        ((DocumentImpl) doc).index(this);
    }

    /**
     * Starts the indexing of a document.
     *
     * @param key the document key for the document.  If this is a
     * duplicate, then any old data associated with the document will be
     * removed.
     * @throws NullPointerException if the document key is null
     * @throws IllegalStateException if this method is called while we are
     * indexing another document.
     */
    public void startDocument(String key) {
        stateCheck();
        if(inDoc) {
            throw new IllegalStateException("Cannot call startDocument " +
                    "when alreading indexing a document!");
        }
        super.startDocument(key);
        inDoc = true;
    }

    /**
     * Adds a field value to the current document.  The named field should
     * have been defined by the index configuration used to configure the
     * search engine from which the simple indexer was created.  If the
     * field was not defined in the index configuration or if the field
     * name is <code>null</code>, then the value will be added to the body
     * of the document.
     *
     * @param name the name of the field to which we want to add a document
     * @param value the value of the field
     * @see com.sun.labs.minion.IndexConfig
     */
    public void addFieldInternal(String name, Object value) {
        stateCheck();
        inDocCheck();
        FieldInfo fi = null;

        //
        // If we have a field name, then get the info for the field, which may
        // require us to generate a new field info based on the defined defaults.
        if(name != null) {
            fi = engine.getManager().getMetaFile().getFieldInfo(name);
            if(fi == null) {

                //
                // We're creating a default field.
                fi = addImpliedField(name);
            }
        }

        try {
            handleField(fi, value);
        } catch(java.io.IOException ioe) {
            logger.log(Level.SEVERE, "Error simple indexing field: " + name, ioe);
        }
    }

    public void addField(String name, Collection<Object> value) {
        for(Object o : value) {
            addFieldInternal(name, o);
        }
    }

    public void addField(String name, Object[] value) {
        for(Object o : value) {
            addFieldInternal(name, o);
        }
    }

    public void addField(String name, Double value) {
        addFieldInternal(name, value);
    }

    public void addField(String name, Float value) {
        addFieldInternal(name, value);
    }

    public void addField(String name, Long value) {
        addFieldInternal(name, value);
    }

    public void addField(String name, Integer value) {
        addFieldInternal(name, value);
    }

    public void addField(String name, Date value) {
        addFieldInternal(name, value);
    }

    public void addField(String name, String value) {
        addFieldInternal(name, value);
    }

    public void addField(String name, IndexableString value) {
        addFieldInternal(name, value);
    }

    /**
     * Adds a term to the current document.
     *
     * @param term the term
     */
    public void addTerm(String term) {
        addTerm(null, term, 1);
    }

    /**
     * Adds a term to the current document.
     *
     * @param term the term
     * @param count the number of times that the term occurs in the
     * document
     */
    public void addTerm(String term, int count) {
        addTerm(null, term, count);
    }

    public void addTerm(String field, String term, int count) {
        stateCheck();
        inDocCheck();

        //
        // Check whether we changed fields and deal witht he change.
        //
        // If this field is null, then we're putting things in the no-field field.
        if(field == null) {

            if(currField != null) {
                fields[currField.getID()] = 0;
            }
            fields[0] = 1;
            currField = null;
        } else {

            //
            // Unset the current field, if there is one.
            if(currField != null) {
                fields[currField.getID()] = 0;
            }

            //
            // Get the info for the new field and deal with bad cases.
            currField = engine.getManager().getMetaFile().getFieldInfo(field);

            if(currField == null) {
                currField = engine.getIndexConfig().getDefaultFieldInfo(field);
            }

            //
            // If this field isn't indexed, then throw an exception.
            if(!currField.hasAttribute(FieldInfo.Attribute.INDEXED)) {
                throw new IllegalArgumentException("Trying to add terms to " +
                        field + " which is not an indexed field");
            }

            if(currField.getID() >= fields.length) {
                fields = new int[currField.getID() * 2 + 1];
            }
            fields[currField.getID()] = 1;
        }

        Token t = new Token(term, count);
        t.setFields(fields);
        head.token(t);
    }

    public void endDocument() {
        stateCheck();
        inDocCheck();
        super.endDocument();

        //
        // Check if we need to dump data based on our memory usage.
        if(((SearchEngineImpl) engine).checkLowMemory()) {
            flush();
        }
        inDoc = false;
    }

    /**
     * Finishes indexing.
     */
    public void finish() {
        stateCheck();
        if(inDoc) {
            endDocument();
        }
        flush();
        simpleIndexingFinished = true;
    }

    /**
     * Checks to see if we're in a document.  If we're not, then we'll
     * throw an exception.
     *
     * @throws IllegalStateException if we're not in a document.
     */
    protected void inDocCheck() throws IllegalStateException {
        if(!inDoc) {
            throw new IllegalStateException("startDocument has not been called, " +
                    "so no indexing " +
                    "activity is possible");
        }
    }

    /**
     * Checks to see if we're in a good state to index data.
     *
     * @throws IllegalStateException if we're not in a good state to be
     * doing indexing.
     */
    protected void stateCheck() throws IllegalStateException {
        if(simpleIndexingFinished) {
            throw new IllegalStateException("SimpleIndexer has had finish " +
                    "called.  No more indexing " +
                    "activity is possible");
        }
    }
}
