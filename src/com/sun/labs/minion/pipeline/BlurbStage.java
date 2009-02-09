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

import java.util.HashSet;

import com.sun.labs.minion.FieldInfo;

import java.util.logging.Logger;

/**
 * This stages removes certain stop-like words from the review portion
 * of a book document.
 *
 * @author <a href="mailto:jeffrey.alexander@sun.com">Jeff Alexander</a>
 */
public class BlurbStage extends StageAdapter {

    Stage downstream;

    protected HashSet stopWords;

    Logger logger = Logger.getLogger(getClass().getName());

    protected static String logTag = "BS";

    protected static boolean inReview = false;

    public BlurbStage(Stage d, HashSet stopWords) {
        downstream = d;
        this.stopWords = stopWords;
    } // BlurbStage constructor

    public void setDownstream(Stage s) {
        downstream = s;
    }

    public Stage getDownstream() {
        return downstream;
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
        downstream.startField(fi);
        if(fi.getName().equals("review")) {
            inReview = true;
        }
    }

    /**
     * Processes a token from further up the pipeline.
     *
     * @param t The token to process.
     */
    public void token(Token t) {
        if(inReview == true) {
            String val = t.getToken().toLowerCase();
            if(stopWords.contains(val)) {
                return;
            }
        }
        if(downstream == null) {
            return;
        }
        downstream.token(t);
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
        downstream.endField(fi);
        inReview = false;
    }
} // BlurbStage
