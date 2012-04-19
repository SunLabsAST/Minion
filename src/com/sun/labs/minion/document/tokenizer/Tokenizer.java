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

import com.sun.labs.minion.pipeline.Stage;
import com.sun.labs.minion.pipeline.StageAdapter;
import com.sun.labs.util.props.ConfigBoolean;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.logging.Logger;

public abstract class Tokenizer extends StageAdapter implements
        com.sun.labs.util.props.Configurable {

    protected static final Logger logger = Logger.getLogger(Tokenizer.class.getName());

    @ConfigBoolean(defaultValue = false)
    public static final String PROP_SEND_PUNCT = "send_punct";

    @ConfigBoolean(defaultValue = false)
    public static final String PROP_SEND_WHITE = "send_white";

    protected boolean sendWhite;

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

    @Override
    public void process(String text) {
        text(text);
    }

    @Override
    public int getLastWordPosition() {
        return wordNum;
    }

    @Override
    public void setNextWordPosition(int wordPosition) {
        this.wordNum = wordPosition;
    }
    
    /**
     * Tokenize the given character sequence.
     * @param s the sequence to tokenize.
     */
    public abstract void text(CharSequence s);

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
     * Reset state of tokenizer to clean slate.
     */
    @Override
    public void reset() {
        this.reset(sendPunct);
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

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        sendPunct = ps.getBoolean(PROP_SEND_PUNCT);
        sendWhite = ps.getBoolean(PROP_SEND_WHITE);
    }
} // Tokenizer
