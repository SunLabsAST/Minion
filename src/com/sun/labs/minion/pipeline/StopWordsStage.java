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

import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import com.sun.labs.minion.FieldInfo;
import com.sun.labs.util.props.ConfigComponent;
import java.util.logging.Logger;

/**
 * This stage provides the ability to remove stop words from  
 * the token stream.  User-tunable stop words are provided as
 * HashSet, presumably from a file.  Additionally, any token
 * that has a numeric character in it is discarded as a stop word.
 * Finally, words longer than 30 characters are dropped as well.
 *   
 * @author <a href="mailto:jeffrey.alexander@sun.com">Jeff Alexander</a>
 */
public class StopWordsStage extends StageAdapter implements
        com.sun.labs.util.props.Configurable {

    static Logger logger = Logger.getLogger(StopWordsStage.class.getName());

    protected static String logTag = "SWS";

    protected boolean inVectoredField = false;

    public StopWordsStage() {
    }

    /**
     * Processes the event that occurs at the start of a field.
     *
     * @param fi The {@link com.sun.labs.minion.FieldInfo} object that describes
     * the field that is starting.
     */
    public void startField(FieldInfo fi) {
        if(downstream == null) {
            return;
        }
        if(fi.isVectored()) {
            inVectoredField = true;
        }
        downstream.startField(fi);
    }

    /**
     * Processes a token from further up the pipeline.
     *
     * @param t The token to process.
     */
    public void token(Token t) {
        String val = t.getToken().toLowerCase();
        if(inVectoredField) {
            if(stopwords.isStop(val)) {
                return;
            }

            if(val.length() > 30) {
                return;
            }

            if(val.matches(".*\\d.*")) {
                return;
            }

        }

        if(downstream == null) {
            return;
        }
        downstream.token(t);
    }

    /**
     * Processes text passed in from the upstream stage.  The text is simply 
     * processed as a token.
     */
    public void text(char[] t, int b, int e) {
        token(new Token(new String(t, b, (e - b)), 1));
    }

    /**
     * Processes the event that occurs at the end of a field.
     *
     * @param fi The {@link com.sun.labs.minion.FieldInfo} object that describes
     * the field that is ending.
     */
    public void endField(FieldInfo fi) {
        if(downstream == null) {
            return;
        }
        inVectoredField = false;
        downstream.endField(fi);
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        stopwords = (StopWords) ps.getComponent(PROP_STOPWORDS);
    }
    @ConfigComponent(type = StopWords.class)
    public static final String PROP_STOPWORDS = "stopwords";

    private StopWords stopwords;

} // StopWordsStage
