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
package com.sun.labs.minion.indexer.partition;

import com.sun.labs.minion.SearchEngine;
import com.sun.labs.minion.indexer.partition.PartitionManager.Merger;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.util.props.ConfigBoolean;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import com.sun.labs.minion.util.NanoWatch;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that will be used to dump partitions in an orderly fashion.  This
 * class dumps partitions asynchronously from the threads that did the indexing,
 * so a thread that dumps a partition may continue indexing new data before the
 * old data has been dumped to disk.
 *
 * <p>
 *
 * We use a bounded queue for dump requests in order to throttle the indexers
 * when they attempt to dump if the dumper cannot keep up.  Note that the longer
 * the queue the more memory will be required to store the indexed but undumped
 * data.
 *
 */
public class AsyncDumper implements Runnable, Dumper {

    private PartitionManager pm;

    /**
     * Our configuration name.
     */
    protected String name;

    /**
     * A queue onto which partitions will be placed for dumping.
     */
    protected BlockingQueue<MPHolder> toDump;

    /**
     * The interval for polling the partition queue.
     */
    protected int pollInterval;

    /**
     * Whether we are done.
     */
    protected boolean done;

    /**
     * The thread that is running our dumping.
     */
    protected Thread t;

    private int nDumps;

    /**
     * The queue length property for configuration.
     */
    @ConfigInteger(defaultValue = 1)
    public static String PROP_QUEUE_LENGTH = "queue_length";

    private int queueLength;

    /**
     * The poll interval for the queue, in seconds.
     */
    @ConfigInteger(defaultValue = 3)
    public static String PROP_POLL_INTERVAL = "poll_interval";

    @ConfigBoolean(defaultValue = true)
    public static final String PROP_DO_GC = "do_gc";

    private boolean doGC;

    private NanoWatch nw;

    static final Logger logger = Logger.getLogger(AsyncDumper.class.getName());

    protected static String logTag = "ADMP";

    /**
     * Default constructor used for configuration.
     */
    public AsyncDumper() {
    }

    public void setSearchEngine(SearchEngine e) {
        pm = e.getManager();
    }

    public int getQueueLength() {
        return queueLength;
    }

    public void dump(MemoryPartition part) {
        try {
            nw.start();
            toDump.put(new MPHolder(part, new Date()));
            nw.stop();
        } catch(InterruptedException ex) {
            logger.warning("Dumper interrupted during put");
        }
    }

    public void run() {
        while(!done) {
            try {
                //
                // We'll poll for a defined interval so that we can catch when
                // we're finished.
                MPHolder mph = toDump.poll(pollInterval, TimeUnit.SECONDS);
                if(mph != null && mph.time.after(pm.getLastPurgeTime())) {
                    try {
                        
                        PartitionOutput partOut = mph.part.dump();
                        if(partOut != null) {
                            partOut.flush(mph.part.getDocumentDictionary().getKeys());
                            partOut.close();
                        }
                        
                        //
                        // Merges will happen synchronously in the thread
                        // running the dumper, which will help regulate
                        // partition dumping when merging is going on.
                        Merger m = pm.getMerger();
                        if(m != null) {
                            m.run();
                        }
                        nDumps++;

                        //
                        // Why 2 extra?  Those are for the one that we just 
                        // dumped and the one held by the indexing thread.
                        if(doGC && nDumps % (queueLength + 2) == 0) {
                            System.gc();
                            nDumps = 0;
                        }
                    } catch(Exception ex) {
                        logger.log(Level.SEVERE,
                                   "Error dumping partition, continuing", ex);
                    }
                }
            } catch(InterruptedException ex) {
                logger.log(Level.WARNING,
                           String.format("Dumper interrupted during poll, exiting with %d partitions waiting", toDump.size()));
                return;
            }
            if(done) {
                break;
            }
        }

        //
        // Drain the list of partitions to dump and then dump them.
        List<MPHolder> l = new ArrayList<MPHolder>();
        toDump.drainTo(l);
        for(MPHolder sh : l) {
            if(sh.time.after(pm.getLastPurgeTime())) {
                try {
                    PartitionOutput partOut = sh.part.dump();
                    if(partOut != null) {
                        partOut.flush(sh.part.getDocumentDictionary().getKeys());
                        partOut.close();
                    }
                } catch(IOException ex) {

                    logger.log(Level.SEVERE, String.format(
                            "Error dumping partition"), ex);
                }
            }
        }
    }

    /**
     * Tells the thread we're finished dumping and then waits for it to catch
     * up.
     */
    public void finish() {
        done = true;
        try {
            t.join();
        } catch(InterruptedException ex) {
            //
            // We can't really do anything here...
        }
    }

    public void newProperties(PropertySheet ps)
            throws PropertyException {
        queueLength = ps.getInt(PROP_QUEUE_LENGTH);
        toDump = new ArrayBlockingQueue<MPHolder>(queueLength);
        pollInterval = ps.getInt(PROP_POLL_INTERVAL);
        doGC = ps.getBoolean(PROP_DO_GC);
        nw = new NanoWatch();

        //
        // Create our thread and start ourselves running.
        t = new Thread(this);
        t.setName("AsyncDumper");
        t.start();
    }

    public String getName() {
        return name;
    }

    class MPHolder {

        public MemoryPartition part;

        public Date time;

        public MPHolder(MemoryPartition part, Date t) {
            this.part = part;
            time = t;
        }
    }
}
