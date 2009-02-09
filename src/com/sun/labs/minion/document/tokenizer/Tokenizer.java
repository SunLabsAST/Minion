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
package com.sun.labs.minion.document.tokenizer;

import com.sun.labs.minion.IndexConfig;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;

import com.sun.labs.minion.pipeline.Stage;

import com.sun.labs.minion.FieldInfo;
import com.sun.labs.util.props.ConfigBoolean;
import com.sun.labs.minion.pipeline.StageAdapter;

import com.sun.labs.minion.util.Util;
import java.util.logging.Logger;

public abstract class Tokenizer extends StageAdapter implements
        com.sun.labs.util.props.Configurable {

    /**
     * The character position in the file we're tokenizing.
     */
    protected int pos;

    /**
     * The ordinal word number for the token we're building.
     */
    protected int wordNum;

    /**
     * An array to save text in when processing saved fields.
     */
    protected char[] savedData;

    /**
     * The length of the saved data.
     */
    protected int savedLen;

    /**
     * Whether we should make tokens.
     */
    protected boolean makeTokens;

    /**
     * Whether tokens should be indexed.
     */
    protected boolean indexed;

    /**
     * Whether we should pass along punctuation.
     */
    protected boolean sendPunct;

    /**
     * Whether we should save data.
     */
    protected boolean saveData;

    /**
     * Whether we have saved data.
     */
    protected boolean dataSaved;

    /**
     * Whether the saved data needs to be trimmed of spaces before being
     * passed on.
     */
    protected boolean trimSpaces;

    /**
     * The length of the longest token that we will generate.
     */
    protected int maxTokLen = 256;

    protected Logger logger = Logger.getLogger(getClass().getName());

    protected static String logTag = "TOK";

    public Tokenizer() {
    }

    /**
     * Create a tokenizer that will send it's output to the given
     * <code>Stage</code>
     */
    public Tokenizer(Stage s) {
        this(s, false);
    }

    /**
     * Creates a tokenizer that will send tokens and possibly punctuation
     * to the given stage.
     */
    public Tokenizer(Stage s, boolean sendPunct) {
        super(s);
        savedData = new char[128];
        this.sendPunct = sendPunct;
        indexed = true;
    }

    /**
     * Gets a tokenizer that we can use in the query parser.
     */
    public abstract Tokenizer getTokenizer(Stage s, boolean sp);

    /**
     * Handles a begin document event.
     */
    public void startDocument(String key) {
        wordNum = 1;
        downstream.startDocument(key);
    }

    /**
     * Handles the start of a field event.
     */
    public void startField(FieldInfo fi) {
        flush();
        makeTokens = fi.isTokenized();
        indexed = fi.isIndexed();
        saveData = fi.isSaved();
        trimSpaces = fi.isTrimmed();
        downstream.startField(fi);
    }

    /**
     * Tokenize the given text.  Output tokens will be placed on the output
     * pipe.
     *
     * @param text The text to tokenize.
     * @param b The beginning position in the text buffer.
     * @param e The ending position in the text buffer.
     */
    public abstract void text(char[] text, int b, int e);

    /**
     * Handles a character that takes up more than one character in a
     * file.  For example, a character entity in an HTML file.
     *
     * @param c The character
     * @param b The beginning position of the character in the document.
     * @param l The length of the character in the document.
     */
    public abstract void handleLongChar(char c, int b, int l);

    /**
     * Saves the given range of the array, if we're supposed to be saving
     * field data.  Saves field text exactly as it occurs in the document.
     */
    protected void handleFieldData(char[] text, int b, int e) {
        if(saveData) {

            int len = e - b;

            if(savedLen + len >= savedData.length) {
                savedData = Util.expandChar(savedData, (savedLen + len) * 2);
            }

            System.arraycopy(text, b, savedData, savedLen, len);
            savedLen += len;
            dataSaved = true;
        }
    }

    /**
     * Handles the end of a field event.  Checks whether saved data needs
     * to be sent down the pipeline.
     *
     * @param fi The information for the field that is ending.
     */
    public void endField(FieldInfo fi) {
        flush();
        if(saveData && dataSaved) {

            String saveString = new String(savedData,
                    0, savedLen);
            downstream.savedData(trimSpaces ? saveString.trim() : saveString);
            savedLen = 0;
        }
        makeTokens = true;
        indexed = true;
        saveData = false;
        dataSaved = false;
        trimSpaces = false;
        downstream.endField(fi);
    }

    /**
     * Handles the end of document event.
     */
    public void endDocument(long size) {
        flush();
        downstream.endDocument(size);
    }

    /**
     * Tells the downstream stage that its data must be dumped to the
     * index.
     *
     * @param iC The configuration for the index, which can be used to
     * retrieve things like the index directory.
     */
    public void dump(IndexConfig iC) {
        flush();
        if(downstream == null) {
            return;
        }
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
        flush();
        if(downstream == null) {
            return;
        }
        downstream.shutdown(iC);
    }

    /**
     * Flushes any collected tokens.
     */
    public abstract void flush();

    /**
     * Reset state of tokenizer to clean slate.
     */
    public void reset() {
        this.reset(false);
    }

    /**
     * Reset state of tokenizer to clean slate.
     */
    public void reset(boolean sendPunct) {
        makeTokens = true;
        indexed = true;
        savedLen = 0;
        saveData = false;
        trimSpaces = false;
        this.sendPunct = sendPunct;
    }

    /**
     * Get the character position in the file we're tokenizing.
     */
    public int getPos() {
        return pos;
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        sendPunct = ps.getBoolean(PROP_SEND_PUNCT);
        sendWhite = ps.getBoolean(PROP_SEND_WHITE);
    }
    @ConfigBoolean(defaultValue = false)
    public static final String PROP_SEND_PUNCT = "send_punct";

    @ConfigBoolean(defaultValue = false)
    public static final String PROP_SEND_WHITE = "send_white";

    protected boolean sendWhite;

} // Tokenizer
