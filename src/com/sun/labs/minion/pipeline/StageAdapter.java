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

import com.sun.labs.minion.IndexConfig;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;

import com.sun.labs.minion.FieldInfo;

/**
 * An adapter class for the stage interface, for those who don't want to
 * bother with implementing methods that they don't care about.  The
 * implementation of each method simply passes down the data to the
 * downstream stage.
 *
 * @see com.sun.labs.minion.pipeline.Token
 * 
 */
public class StageAdapter implements Stage {

    /**
     * Our configuration name.
     */
    protected String name;
    
    /**
     * Our downstream stage.
     */
    protected Stage downstream;

    /**
     * Void constructor just in case.
     */
    public StageAdapter() {
    }

    /**
     * Construct a Stage with a downstream stage to which it can pass
     * output.
     *
     * @param d The stage to which we will pass data.
     */
    public StageAdapter(Stage d) {
        downstream = d;
    }

    public void setDownstream(Stage s) {
        downstream = s;
    }

    public Stage getDownstream() {
        return downstream;
    }

    /**
     * Defines a field into which an application will index data.
     *
     * @param fi The {@link com.sun.labs.minion.FieldInfo} object that describes
     * the field we want defined.
     */
    public FieldInfo defineField(FieldInfo fi) {
        if(downstream == null) return fi;
        return downstream.defineField(fi);
    }

    /**
     * Process the event that occurs at the start of a document.
     *
     * @param key The document key for this document.
     */
    public void startDocument(String key) {
        if(downstream == null) return;
        downstream.startDocument(key);
    }

    /**
     * Processes the event that occurs at the start of a field.
     *
     * @param fi The {@link com.sun.labs.minion.FieldInfo} object that describes
     * the field that is starting.
     */
    public void startField(FieldInfo fi) {
        if(downstream == null) return;
        downstream.startField(fi);
    }

    /**
     * Processes a token from further up the pipeline.
     *
     * @param t The token to process.
     */
    public void token(Token t) {
        if(downstream == null) return;
        downstream.token(t);
    }

     /**
     * Processes some text from further up the pipeline.
     *
     * @param t The text to tokenize.
     * @param b The beginning position in the text buffer.
     * @param e The ending position in the text buffer.
     */
    public void text(char[] t, int b, int e) {
        if(downstream == null) {
            return;
        }
        downstream.text(t, b, e);
    }

   /**
     * Processes some punctuation from further up the pipeline.
     *
     * @param p The punctuation to process.
     */
    public void punctuation(Token p) {
        if(downstream == null) return;
        downstream.punctuation(p);
    }

    /**
     * Processes saved data from further up the pipeline.
     *
     * @param sd The data to save.
     */
    public void savedData(Object sd) {
        if(downstream == null) return;
        downstream.savedData(sd);
    }

    /**
     * Processes the event that occurs at the end of a field.
     *
     * @param fi The {@link com.sun.labs.minion.FieldInfo} object that describes
     * the field that is ending.
     */
    public void endField(FieldInfo fi) {
        if(downstream == null) return;
        downstream.endField(fi);
    }

    /**
     * Processes the event that comes at the end of a document.
     *
     * @param size The size of the data that was processed for this file.
     */
    public void endDocument(long size) {
        if(downstream == null) return;
        downstream.endDocument(size);
    }

    /**
     * Tells a stage that its data must be dumped to the index.
     *
     * @param iC The configuration for the index, which can be used to
     * retrieve things like the index directory.
     */
    public void dump(IndexConfig iC) {
        if(downstream == null) return;
        downstream.dump(iC);
    }

    /**
     * Tells a stage that it needs to shutdown, terminating any processing
     * that it is doing first.
     *
     * @param iC The configuration for the index, which can be used to
     * retrieve things like the index directory.
     */
    public void shutdown(IndexConfig iC) {
        if(downstream == null) return;
        downstream.shutdown(iC);
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        name = ps.getInstanceName();
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

} // StageAdapter
