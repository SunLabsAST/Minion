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
import com.sun.labs.util.NanoWatch;
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
     * A queue onto which partitions will be placed for marshalling.
     */
    protected BlockingQueue<MPHolder> toMarshall;

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
    protected boolean marshallerDone;

    /**
     * Are we done flushing?
     */
    protected boolean flushDone;

    /**
     * Threads to do the dumping.
     */
    private Thread[] marshallThreads;

    /**
     * The partition outputs that we'll use for marshalling data.
     */
    private RAMPartitionOutput[] partitionOutputs;

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
    public static String PROP_MARSHALL_QUEUE_LENGTH = "marshall_queue_length";

    /**
     * The size of the pool of partition outputs.
     */
    @ConfigInteger(defaultValue = 4)
    public static final String PARTITION_OUTPUT_PROP_POOL_SIZE = "partition_output_pool_size";

    @ConfigInteger(defaultValue = 2)
    public static final String PROP_MARSHALL_THREADS = "marshall_threads";

    /**
     * Default constructor used for configuration.
     */
    public Marshaller() {
    }

    public void setMemoryPartitionQueue(BlockingQueue<InvFileMemoryPartition> mpq) {
        this.mpPool = mpq;
    }
    
    public void setLongIndexingRun(boolean longIndexingRun) {
        for(PartitionOutput po : partitionOutputs) {
            ((AbstractPartitionOutput) po).setLongIndexingRun(longIndexingRun);
        }
    }

    public void marshall(InvFileMemoryPartition part) {
        try {
            toMarshall.put(new MPHolder(part, new Date()));
        } catch(InterruptedException ex) {
            logger.warning("Marshaller interrupted during put");
        }
    }

    /**
     * Tells the thread we're finished dumping and then waits for it to catch
     * up.
     */
    public void finish() {
        marshallerDone = true;
        for(int i = 0; i < marshallThreads.length; i++) {
            try {
                marshallThreads[i].join();
            } catch(InterruptedException ex) {
            }
        }

        flushDone = true;
        try {
            flushThread.join();
        } catch(InterruptedException ex) {
        }

        for(RAMPartitionOutput po : partitionOutputs) {
            try {
                po.close();
            } catch(IOException ex) {
                logger.log(Level.SEVERE, String.format("Error closing %s", po), ex);
            }
        }

    }

    @Override
    public void newProperties(PropertySheet ps)
            throws PropertyException {

        partitionManager = (PartitionManager) ps.getComponent(PROP_PARTITION_MANAGER);
        boolean longIndexingRun = ((SearchEngineImpl) partitionManager.getEngine()).isLongIndexingRun();

        int queueLength = ps.getInt(PROP_MARSHALL_QUEUE_LENGTH);
        int poPoolSize = ps.getInt(PARTITION_OUTPUT_PROP_POOL_SIZE);
        int ndt = ps.getInt(PROP_MARSHALL_THREADS);

        toMarshall = new ArrayBlockingQueue<MPHolder>(queueLength);
        poPool = new ArrayBlockingQueue<PartitionOutput>(poPoolSize);
        partitionOutputs = new RAMPartitionOutput[poPoolSize];
        for(int i = 0; i < partitionOutputs.length; i++) {
            try {
                partitionOutputs[i] = new RAMPartitionOutput(partitionManager);
                ((RAMPartitionOutput) partitionOutputs[i]).setName("PO-" + i);
                ((RAMPartitionOutput) partitionOutputs[i]).setLongIndexingRun(longIndexingRun);
                poPool.put(partitionOutputs[i]);
            } catch(Exception ex) {
                throw new PropertyException(ex, ps.getInstanceName(), PARTITION_OUTPUT_PROP_POOL_SIZE, "Error creating output pool");
            }
        }

        pollInterval = ps.getInt(PROP_POLL_INTERVAL);
        marshallThreads = new Thread[ndt];
        for(int i = 0; i < marshallThreads.length; i++) {
            marshallThreads[i] = new Thread(new MarshallThread());
            marshallThreads[i].setName("Marshall-" + i);
            marshallThreads[i].start();
        }

        toFlush = new ArrayBlockingQueue<PartitionOutput>(poPoolSize);
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

        @Override
        public void run() {
            while(!marshallerDone) {
                try {
                    //
                    // We'll poll for a defined interval so that we can catch when
                    // we're finished.
                    MPHolder mph = toMarshall.poll(pollInterval, TimeUnit.MILLISECONDS);
                    PartitionOutput partOut = null;
                    if(mph != null && mph.time.after(partitionManager.getLastPurgeTime())) {
                        try {

                            partOut = poPool.take();
                            marshall(mph, partOut);

                            Merger m = partitionManager.getMerger();
                            if(m != null) {
                                m.run();
                            }
                        } catch(Exception ex) {
                            logger.log(Level.SEVERE,
                                    "Error marshalling partition, continuing", ex);
                            partOut.cleanUp();
                        }
                    }
                } catch(InterruptedException ex) {
                    logger.log(Level.WARNING,
                            String.format("Dumper interrupted during poll, exiting with %d partitions waiting", toMarshall.size()));
                    return;
                }
            }

            //
            // Drain the list of partitions to dump and then dump them.
            List<MPHolder> l = new ArrayList<MPHolder>();
            toMarshall.drainTo(l);
            if(l.isEmpty()) {
                return;
            }

            try {
                for(MPHolder sh : l) {
                    if(sh.time.after(partitionManager.getLastPurgeTime())) {
                        try {
                            PartitionOutput partOut = poPool.take();
                            marshall(sh, partOut);
                        } catch(IOException ex) {
                            logger.log(Level.SEVERE, String.format(
                                    "Error marshalling partition"), ex);
                        }
                    }
                }
            } catch(InterruptedException ex) {
                logger.log(Level.SEVERE, String.format("Error flushing partitions at finish"), ex);
            }
        }

        private void marshall(MPHolder mph, PartitionOutput partOut) throws IOException, InterruptedException {
            if(partOut != null) {
                PartitionOutput ret = mph.part.marshall(partOut);
                mph.part.clear();
                mpPool.put(mph.part);
                if(ret != null) {
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.finest(String.format("Queuing %s for flush", partOut));
                    }
                    toFlush.put(partOut);
                } else {
                    //
                    // Nothing got dumped, but we still need to put the partition
                    // output back in the pool!
                    poPool.put(partOut);
                }
            }
        }
    }

    /**
     * A thread that will flush the marshalled data for partitions to the 
     * disk.
     */
    class FlushThread implements Runnable {
        
        private NanoWatch nw = new NanoWatch();

        @Override
        public void run() {
            while(!flushDone) {
                try {
                    PartitionOutput partOut = toFlush.poll(pollInterval, TimeUnit.MILLISECONDS);
                    if(partOut != null) {
                        try {
                            try {
                                nw.start();
                                partOut.flush();
                                nw.stop();
                            } catch(IllegalStateException ex) {
                                logger.log(Level.SEVERE, String.format("Illegal state for %s", partOut), ex);
                                throw(ex);
                            }
                            if(logger.isLoggable(Level.FINEST)) {
                                logger.finest(String.format("Flushed %s in %.2fms",
                                        partOut, nw.getLastTimeMillis()));
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
