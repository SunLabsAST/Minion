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

import com.sun.labs.util.props.ConfigBoolean;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.logging.Logger;

public class PrintStage extends StageAdapter {

    static final Logger logger = Logger.getLogger(PrintStage.class.getName());

    @ConfigBoolean(defaultValue = false)
    public static final String PROP_PRINT_TOKENS = "print_tokens";

    private boolean printTokens;

    public PrintStage() {
    }

    public PrintStage(boolean printTokens) {
        this.printTokens = printTokens;
    }

    public PrintStage(Stage d, boolean printTokens) {
        super(d);
        this.printTokens = printTokens;
    } // PrintStage constructor

    @Override
    public void token(Token t) {
        if(printTokens) {
            logger.info(String.format("T: %s", t));
        }
        if(downstream == null) {
            return;
        }
        downstream.token(t);
    }

    @Override
    public void punctuation(Token p) {
        if(printTokens) {
            logger.info(String.format("P: %s", p));
        }
        if(downstream == null) {
            return;
        }
        downstream.punctuation(p);
    }

    @Override
    public void process(String text) {
        logger.info(String.format("TEXT: %s", text));
        if(downstream == null) {
            return;
        }
        downstream.process(text);
    }

    @Override
    public void newProperties(PropertySheet ps) throws PropertyException {
        super.newProperties(ps);
        printTokens = ps.getBoolean(PROP_PRINT_TOKENS);
    }
} // PrintStage
