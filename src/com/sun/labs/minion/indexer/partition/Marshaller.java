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

import com.sun.labs.minion.engine.SearchEngineImpl;
import com.sun.labs.minion.indexer.partition.PartitionManager.Merger;
import com.sun.labs.minion.indexer.partition.io.AbstractPartitionOutput;
import com.sun.labs.minion.indexer.partition.io.PartitionOutput;
import com.sun.labs.minion.indexer.partition.io.RAMPartitionOutput;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.Configurable;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that will be used to marshall the data from  partitions in an orderly
 * fashion.  This class will take partitions and marshall data into the final form
 * that will be written to disk.
 * 
 */
public class Marshaller implements Configurable {

    private static final Logger logger = Logger.getLogger(Marshaller.class.getName());

    /**
     * Our configuration name.
     */
    protected String name;

    /**
     * A queue onto which partitions will be placed for dumping.
     */
    protected BlockingQueue<MPHolder> toDump;

    /**
     * A queue onto which outputs will be placed for flushing.
     */
    protected BlockingQueue<PartitionOutput> toFlush;

    /**
     * A pool of partition output objects.
     */
    protected BlockingQueue<PartitionOutput> poPool;
    
    /**
     * The queue of partition whose data needs to be marshalled for output.
     */
    private BlockingQueue<InvFileMemoryPartition> mpPool;

    /**
     * The poll interval for the queue, in milliseconds.
     */
    @ConfigInteger(defaultValue = 100)
    public static String PROP_POLL_INTERVAL = "poll_interval";

    /**
     * The interval for polling the partition queue.
     */
    protected int pollInterval;

    /**
     * Whether we are done dumping.
     */
    protected boolean dumperDone;

    /**
     * Are we done flushing?
     */
    protected boolean flushDone;

    /**
     * Threads to do the dumping.
     */
    private Thread[] dumpThreads;

    /**
     * The partition outputs that we'll use for marshalling data.
     */
    private RAMPartitionOutput[] partOuts;

    /**
     * A thread to flush things to disk.
     */
    private Thread flushThread;

    @ConfigComponent(type = com.sun.labs.minion.indexer.partition.PartitionManager.class)
    public static final String PROP_PARTITION_MANAGER = "partition_manager";

    private PartitionManager partitionManager;

    /**
     * The queue length property for configuration.
     */
    @ConfigInteger(defaultValue = 1)
    public static String PROP_DUMP_QUEUE_LENGTH = "dump_queue_length";

    /**
     * The size of the pool of partition outputs.
     */
    @ConfigInteger(defaultValue = 4)
    public static final String PROP_POOL_SIZE = "pool_size";

    @ConfigInteger(defaultValue = 2)
    public static final String PROP_DUMP_THREADS = "dump_threads";

    /**
     * Default constructor used for configuration.
     */
    public Marshaller() {
    }

    public void setMemoryPartitionQueue(BlockingQueue<InvFileMemoryPartition> mpq) {
        this.mpPool = mpq;
    }
    
    public void setLongIndexingRun(boolean longIndexingRun) {
        for(PartitionOutput po : partOuts) {
            ((AbstractPartitionOutput) po).setLongIndexingRun(longIndexingRun);
        }
    }

    public void dump(InvFileMemoryPartition part) {
        try {
            toDump.put(new MPHolder(part, new Date()));
        } catch(InterruptedException ex) {
            logger.warning("Dumper interrupted during put");
        }
    }

    /**
     * Tells the thread we're finished dumping and then waits for it to catch
     * up.
     */
    public void finish() {
        dumperDone = true;
        for(int i = 0; i < dumpThreads.length; i++) {
            try {
                dumpThreads[i].join();
            } catch(InterruptedException ex) {
            }
        }

        flushDone = true;
        try {
            flushThread.join();
        } catch(InterruptedException ex) {
        }

        for(RAMPartitionOutput po : partOuts) {
            try {
                po.close();
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format("Error closing %s", po), ex);
            }
        }

    }

    public void newProperties(PropertySheet ps)
            throws PropertyException {

        partitionManager = (PartitionManager) ps.getComponent(PROP_PARTITION_MANAGER);
        boolean longIndexingRun = ((SearchEngineImpl) partitionManager.getEngine()).isLongIndexingRun();

        int queueLength = ps.getInt(PROP_DUMP_QUEUE_LENGTH);
        int poolSize = ps.getInt(PROP_POOL_SIZE);
        int ndt = ps.getInt(PROP_DUMP_THREADS);

        toDump = new ArrayBlockingQueue<MPHolder>(queueLength);
        poPool = new ArrayBlockingQueue<PartitionOutput>(poolSize);
        partOuts = new RAMPartitionOutput[poolSize];
        for(int i = 0; i < partOuts.length; i++) {
            try {
                partOuts[i] = new RAMPartitionOutput(partitionManager);
                ((RAMPartitionOutput) partOuts[i]).setName("PO-" + i);
                ((RAMPartitionOutput) partOuts[i]).setLongIndexingRun(longIndexingRun);
                poPool.put(partOuts[i]);
            } catch(Exception ex) {
                throw new PropertyException(ex, ps.getInstanceName(), PROP_POOL_SIZE, "Error creating output pool");
            }
        }

        pollInterval = ps.getInt(PROP_POLL_INTERVAL);
        dumpThreads = new Thread[ndt];
        for(int i = 0; i < dumpThreads.length; i++) {
            dumpThreads[i] = new Thread(new MarshallThread());
            dumpThreads[i].setName("Dump-" + i);
            dumpThreads[i].start();
        }

        toFlush = new ArrayBlockingQueue<PartitionOutput>(poolSize);
        flushThread = new Thread(new FlushThread());
        flushThread.setName("Flusher");
        flushThread.start();

    }

    public String getName() {
        return name;
    }

    class MPHolder {

        public InvFileMemoryPartition part;

        public Date time;

        public MPHolder(InvFileMemoryPartition part, Date t) {
            this.part = part;
            time = t;
        }
    }

    /**
     * A thread that will run, selecting partitions from the queue and 
     * marshalling their data.
     */
    class MarshallThread implements Runnable {

        public void run() {
            while(!dumperDone) {
                try {
                    //
                    // We'll poll for a defined interval so that we can catch when
                    // we're finished.
                    MPHolder mph = toDump.poll(pollInterval, TimeUnit.MILLISECONDS);
                    if(mph != null && mph.time.after(partitionManager.getLastPurgeTime())) {
                        try {

                            PartitionOutput partOut = poPool.take();
                            dump(mph, partOut);

                            //
                            // Merges will happen synchronously in the thread
                            // running the dumper, which will help regulate
                            // partition dumping when merging is going on.
                            Merger m = partitionManager.getMerger();
                            if(m != null) {
                                m.run();
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
            }

            //
            // Drain the list of partitions to dump and then dump them.
            List<MPHolder> l = new ArrayList<MPHolder>();
            toDump.drainTo(l);
            if(l.isEmpty()) {
                return;
            }

            try {
                for(MPHolder sh : l) {
                    if(sh.time.after(partitionManager.getLastPurgeTime())) {
                        try {
                            PartitionOutput partOut = poPool.take();
                            dump(sh, partOut);
                        } catch(IOException ex) {
                            logger.log(Level.SEVERE, String.format(
                                    "Error dumping partition"), ex);
                        }
                    }
                }
            } catch(InterruptedException ex) {
                logger.log(Level.SEVERE, String.format("Error flushing partitions at finish"), ex);
            }
        }

        private void dump(MPHolder mph, PartitionOutput partOut) throws IOException, InterruptedException {
            if(partOut != null) {
                PartitionOutput ret = mph.part.marshall(partOut);
                if(ret != null) {
                    toFlush.put(partOut);
                }
                mph.part.clear();
                mpPool.put(mph.part);
            }
        }
    }

    /**
     * A thread that will flush the marshalled data for partitions to the 
     * disk.
     */
    class FlushThread implements Runnable {

        public void run() {
            while(!flushDone) {
                try {
                    PartitionOutput partOut = toFlush.poll(pollInterval, TimeUnit.MILLISECONDS);
                    if(partOut != null) {
                        try {
                            try {
                                partOut.flush();
                            } catch(IllegalStateException ex) {
                                logger.log(Level.SEVERE, String.format("Illegal state for %s", partOut), ex);
                                throw(ex);
                            }
                            poPool.put(partOut);
                        } catch(IOException ex) {
                            logger.log(Level.SEVERE, String.format("Error writing %d to disk", partOut.getPartitionNumber()), ex);
                        }
                    }
                } catch(InterruptedException ex) {
                    logger.log(Level.SEVERE, String.format("Interrupted getting partition output"), ex);
                }
            }

            List<PartitionOutput> l = new ArrayList<PartitionOutput>();
            toFlush.drainTo(l);
            if(l.isEmpty()) {
                return;
            }
            for(PartitionOutput partOut : l) {
                try {
                    partOut.flush();
                } catch(IOException ex) {
                    logger.log(Level.SEVERE, String.format("Error writing %d to disk", partOut.getPartitionNumber()), ex);
                }
            }
        }
    }
}