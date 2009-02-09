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

import com.sun.labs.minion.Indexable;
import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.SearchEngineException;
import java.util.concurrent.BlockingQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.sun.labs.minion.indexer.partition.Dumper;
import java.util.logging.Level;

/**
 * A class that encapsulates the machinery of a single indexing pipeline.
 * A pipeline can be used for indexing data or for performing highlighting
 * operations.
 *
 */
public class AsyncPipelineImpl extends AbstractPipelineImpl implements Runnable {

    /**
     * The queue from which we will take documents to index.
     */
    protected BlockingQueue<Indexable> indexingQueue;

    /**
     * Whether we're currently in a document.
     */
    protected boolean inDoc;

    /**
     * Whether we're finished as a <code>SimpleIndexer</code>.
     */
    protected boolean simpleIndexingFinished;

    /**
     * Whether we've finished and must quit.
     */
    protected boolean finished;

    /**
     * Whether we should flush all of the data currently in the
     * pipeline.
     */
    protected boolean doFlush;

    /**
     * Whether we need to dump our partition.
     */
    protected boolean doDump;

    /**
     * Whether we need to purge the data in this partition/pipeline.
     */
    protected boolean doPurge;

    /**
     * Instantiates a pipeline.
     *
     * @param factory the factory from which to make this pipeline
     * @param engine The search engine for which this pipeline will be
     * processing documents.
     * @param pipeline the stages in the pipeline
     * @param dumper the dumper that will dump partitions after indexing
     * @param indexingQueue the queue of items to index
     */
    public AsyncPipelineImpl(PipelineFactory factory,
            SearchEngine engine,
            List<Stage> pipeline,
            Dumper dumper,
            BlockingQueue<Indexable> indexingQueue) {
        super(factory, engine, pipeline, dumper);
        this.indexingQueue = indexingQueue;

    }

    /**
     * Removes documents from the queue and indexes them.
     */
    public void run() {

        while(!finished) {

            try {

                //
                // Try to get something from the queue, waiting a few
                // seconds before checking to see if something else is going on
                // (e.g., we were asked for a flush.
                Indexable doc = indexingQueue.poll(1, TimeUnit.SECONDS);

                //
                // If we got asked to do a purge, drain the queue and purge
                // our collected in-memory data
                if(doPurge) {
                    List<Indexable> l = new ArrayList<Indexable>();
                    indexingQueue.drainTo(l);
                    realPurge();
                    doPurge = false;
                }

                if(doc != null) {

                    //
                    // Index a document if we got one.
                    indexDoc(doc.getKey(), doc.getMap());
                }

                //
                // If we got asked to do a dump, do that now, then replace our
                // indexing stage with something new.
                if(doDump) {
                    realDump();
                    doDump = false;
                }

                //
                // If we were asked to to a flush, then do that now.
                if(doFlush) {
                    drain();
                    doFlush = false;
                }
            } catch(SearchEngineException se) {
                logger.log(Level.SEVERE, "Error during asynchronous indexing",
                        se);
            } catch(InterruptedException ex) {
                logger.log(Level.WARNING, "Interrupted during take",
                        ex);
            }
        }

        //
        // Drain whatever's left in the queue.
        drain();
        head.shutdown(engine.getIndexConfig());
    }

    /**
     * Drains the indexing queue, indexing each of the elements.
     */
    protected void drain() {
        List<Indexable> l = new ArrayList<Indexable>();
        indexingQueue.drainTo(l);
        if(l.size() > 0) {
            for(Indexable i : l) {
                try {
                    indexDoc(i.getKey(), i.getMap());
                } catch(SearchEngineException se) {
                    logger.log(Level.SEVERE, "Error indexing: " + i.getKey(), se);
                }
            }
        }
        realDump();
    }

    /**
     * Flushes all the data currently held in the queue.  If the pipeline
     * is asynchronous, then the flush to disk will occur asynchronously
     * when all of the data in the pipline has been indexed.
     */
    public void flush() {
        doFlush = true;
    }

    /**
     * Dumps the current indexing data to disk.  This will not empty out the
     * indexing pipeline before the data are written.
     */
    public void dump() {
        doDump = true;
    }

    /**
     * Purge the data currently in the pipeline.  The data is thrown out,
     * not getting written out to disk.  It is best not to be indexing while
     * a purge is in progress.
     *
     * @throws com.sun.labs.minion.SearchEngineException
     */
    public void purge() {
        doPurge = true;
    }

    /**
     * Shuts down this pipeline, making sure that any documents in the
     * queue have been finished.
     */
    public void shutdown() {
        finished = true;
    }

    public void index(Indexable doc) {
        indexingQueue.offer(doc);
    }
}
