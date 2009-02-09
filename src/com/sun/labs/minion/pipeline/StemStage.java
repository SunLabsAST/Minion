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

import com.sun.labs.minion.util.PorterStemmer;
import java.util.logging.Logger;

public class StemStage extends StageAdapter {

    Logger logger = Logger.getLogger(getClass().getName());

    protected static String logTag = "SS";

    protected PorterStemmer stemmer = new PorterStemmer();

    public StemStage() {
    } // StemStage constructor

    public StemStage(Stage d) {
        downstream = d;
    } // StemStage constructor

    /**
     * Processes a token from further up the pipeline.
     *
     * @param t The token to process.
     */
    public void token(Token t) {
        if(downstream == null) {
            return;
        }
        String str = t.getToken();
        stemmer.add(str.toLowerCase().toCharArray(), str.length());
        stemmer.stem();
        t.setToken(stemmer.toString());
        downstream.token(t);
    }

    /**
     * Processes text passed in from the upstream stage.  The text is simply 
     * processed as a token.
     */
    public void text(char[] t, int b, int e) {
        token(new Token(new String(t, b, (e - b)), 1));
    }
} // StemStage
