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

import java.text.DecimalFormat;

import com.sun.labs.minion.IndexConfig;

public class StatStage extends StageAdapter {

    public StatStage() {
        start = System.currentTimeMillis();
    }

    /**
     * Processes the event that comes at the end of a document.
     *
     * @param size The size of the data that was processed for this file.
     */
    public void endDocument(long size) {
        bytes += size;
        nDocs++;
        if(nDocs % 1000 == 0) {
            report();
        }
    }

    /**
     * Tells a stage that it needs to shutdown, terminating any processing
     * that it is doing first.
     *
     * @param iC The configuration for the index, which can be used to
     * retrieve things like the index directory.
     */
    public void shutdown(IndexConfig iC) {
        report();
    }

    public void report() {
        float secs = (float) (System.currentTimeMillis() -
                start) / 1000;
        float MB = (bytes / (1024 * 1024));

    }
    /**
     * A format object for formatting the output.
     */
    protected static DecimalFormat form = new DecimalFormat("########0.00");

    long start;

    int nDocs = 0;

    long bytes = 0;

} // StatStage
