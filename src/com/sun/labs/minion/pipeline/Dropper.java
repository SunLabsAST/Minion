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
import java.util.logging.Logger;

/**
 * A class that just drops things, but keeps track of the amount of data
 * processed.
 */
public class Dropper extends StageAdapter {

    protected static Object lock = new Object();

    /**
     * The time that we started indexing.
     */
    protected long startTime;

    /**
     * The number of documents that we've processed.
     */
    protected int docsProcessed;

    /**
     * The number of bytes that have been indexed.
     */
    protected float bytesProcessed;

    /**
     * A format object for formatting the output.
     */
    protected DecimalFormat form = new DecimalFormat("########0.00");

    /**
     * The number of files to index between progress reports.
     */
    protected static int PROGRESS_INTERVAL = 1000;

    /**
     * The log.
     */
    static Logger logger = Logger.getLogger(Dropper.class.getName());

    /**
     * The log tag.
     */
    protected static String logTag = "DROP";

    private int nTokens = 0;

    public Dropper() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Processes a token from further up the pipeline.
     *
     * @param t The token to process.
     */
    @Override
    public void token(Token t) {
        nTokens++;
    }

    /**
     * Processes the event that comes at the end of a document.
     *
     * @param size The size of the data that was processed for this file.
     */
    public void endDocument(long size) {
        docsProcessed++;
        bytesProcessed += size;

//        if(docsProcessed % 1000 == 0) {
//            reportProgress();
//        }
    }

    protected String toMB(long x) {
        return form.format((float) x / (float) (1024 * 1024));
    }

    /**
     * Reports on our progress.
     */
    public void reportProgress() {
        float secs = (float) (System.currentTimeMillis() -
                startTime) / 1000;
        float MB = bytesProcessed / (1024 * 1024);

        logger.info(docsProcessed + " documents, " + form.format(MB) + " MB, " +
                form.format(secs) + " s, " +
                form.format(MB / (secs / 3600)) + " MB/h " +
                toMB(Runtime.getRuntime().totalMemory()) + "MB");
    }
} // Dropper
