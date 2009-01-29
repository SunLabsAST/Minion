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

package com.sun.labs.minion;

import com.sun.labs.minion.pipeline.Stage;

/**
 * A class that encapsulates the machinery of a single indexing pipeline.
 * A pipeline can be used for indexing data or for performing highlighting
 * operations.
 *
 */
public interface Pipeline {
    
    /**
     * Gets the head of the pipeline.
     *
     * @return the stage at the head of the pipeline.
     */
    public Stage getHead();

    /**
     * Adds an indexable object to the pipeline for indexing.
     *
     * @param doc a document to index.
     * @throws SearchEngineException if there are any errors during indexing.
     */
    public void index(Indexable doc) throws SearchEngineException;
    
    /**
     * Dumps all of the data indexed by a given pipeline into the given dumper.
     * Data currently in the pipeline will most likely <em>not</em> be indexed 
     * before the dump is completed.
     * @throws com.sun.labs.minion.SearchEngineException if the dump fails
     */
    public void dump() throws SearchEngineException;

    /**
     * Purge the data currently in the pipeline.  The data is thrown out,
     * not getting written out to disk.  It is best not to be indexing while
     * a purge is in progress.
     *
     * @throws com.sun.labs.minion.SearchEngineException
     */
    public void purge();

    /**
     * Flushes all the data currently held in the queue.  If the pipeline
     * is asynchronous, then the flush to disk will occur asynchronously
     * when all of the data in the pipline has been indexed.
     */
    public void flush();

    /**
     * Shuts down the pipeline, flushing all data currently held in the queue.
     */
    public void shutdown();
}
