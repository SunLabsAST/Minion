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
import com.sun.labs.util.props.ConfigComponent;
import com.sun.labs.util.props.ConfigInteger;
import com.sun.labs.util.props.Configurable;
import com.sun.labs.util.props.PropertyException;
import com.sun.labs.util.props.PropertySheet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class that will be used to marshal the data from partitions in an orderly
 * fashion. This class will take partitions and marshal data into the final
 * form that will be written to disk.
 *
 */
public class Marshaler implements Configurable {

    private static final Logger logger = Logger.getLogger(Marshaler.class.
            getName());

    /**
     * Our configuration name.
     */
    protected String name;

    /**
     * A queue onto which partitions will be placed for marshaling.
     */
    protected BlockingQueue<MPHolder> toMarshal;

    /**
     * A queue onto which outputs will be placed for flushing.
     */
    protected BlockingQueue<PartitionOutput> toFlush;

    /**
     * A pool of partition output objects.
     */
    protected BlockingQueue<PartitionOutput> poPool;

    /**
     * The queue of partition whose data needs to be marshaled for output.
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
    protected boolean marshalerDone;

    /**
     * Are we done flushing?
     */
    protected boolean flushDone;

    /**
     * Threads to do the marshaling.
     */
    private Thread[] marshalThreads;
    
    private MarshalThread[] marshalers;
    
    /**
     * The partition outputs that we'll use for marshaling data.
     */
    private RAMPartitionOutput[] partitionOutputs;
    
    /**
     * A flusher.
     */
    private FlushThread flusher;

    /**
     * A thread to flush things to disk.
     */
    private Thread flushThread;

    @ConfigComponent(
    type = com.sun.labs.minion.indexer.partition.PartitionManager.class)
    public static final String PROP_PARTITION_MANAGER = "partition_manager";

    private PartitionManager partitionManager;

    /**
     * Default constructor used for configuration.
     */
    public Marshaler() {
    }

    public void setMemoryPartitionQueue(
            BlockingQueue<InvFileMemoryPartition> mpPool) {
        this.mpPool = mpPool;
    }

    public void setLongIndexingRun(boolean longIndexingRun) {
        for(PartitionOutput po : partitionOutputs) {
            ((AbstractPartitionOutput) po).setLongIndexingRun(longIndexingRun);
        }
    }

    public void marshal(InvFileMemoryPartition part, CountDownLatch completion) {
        try {
            toMarshal.put(new MPHolder(part, completion, new Date()));
        } catch(InterruptedException ex) {
            logger.warning("Marshaler interrupted during put");
        }
    }

    /**
     * Tells the thread we're finished dumping and then waits for it to catch
     * up.
     */
    public void finish() {
        

        NanoWatch fetchMPw = new NanoWatch();
        NanoWatch fetchPOw = new NanoWatch();
        NanoWatch marshalw = new NanoWatch();
        NanoWatch putMPw = new NanoWatch();
        NanoWatch putPOw = new NanoWatch();

        marshalerDone = true;
        for(int i = 0; i < marshalThreads.length; i++) {
            try {
                marshalThreads[i].join();
                fetchMPw.accumulate(marshalers[i].fetchMPw);
                putMPw.accumulate(marshalers[i].putMPw);
                marshalw.accumulate(marshalers[i].marshalw);
                fetchPOw.accumulate(marshalers[i].fetchPOw);
                putPOw.accumulate(marshalers[i].putPOw);
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
                logger.log(Level.SEVERE, String.format("Error closing %s", po),
                           ex);
            }
        }
        
        logger.info(String.format("Average (ms) fetch MP: %.2f marshal MP: %.3f put MP: %.2f fetch PO: %.2f put PO: %.2f", 
                                  fetchMPw.getAvgTimeMillis(), 
                                  marshalw.getAvgTimeMillis(),
                                  putMPw.getAvgTimeMillis(),
                                  fetchPOw.getAvgTimeMillis(), 
                                  putPOw.getAvgTimeMillis()));

    }
    
    /**
     * Tells the marshaler to pauseIndexing until the given latch counts down to zero.
     * We can use this in situations where the indexer is feeding data quickly
     * enough that we need to hold off on making new partitions until we've
     * completed (for example) a long merge.
     */
    public void pause(CountDownLatch pauseLatch) {
        for(MarshalThread mt : marshalers) {
            mt.pause(pauseLatch);
        }
    }

    /**
     * Performs a synchronous flush of the marshaling queue, returning only
     * when the partitions in the queue have been flushed to disk and are open
     * for querying.
     */
    public void flush() {
    }

    @Override
    public void newProperties(PropertySheet ps)
            throws PropertyException {

        partitionManager = (PartitionManager) ps.getComponent(
                PROP_PARTITION_MANAGER);
        boolean longIndexingRun = ((SearchEngineImpl) partitionManager.
                getEngine()).isLongIndexingRun();

        //
        // We'll derive our various queue lengths and numbers of threads from
        // the number of indexing threads that are configured in the engine.
        int numIndexingThreads = ((SearchEngineImpl) partitionManager.getEngine()).getNumIndexingThreads();

        int queueLength = numIndexingThreads * 2;
        int poPoolSize = queueLength;
        int numMarshalThreads = Math.max(queueLength-1, 2);

        toMarshal = new ArrayBlockingQueue<MPHolder>(queueLength);
        poPool = new ArrayBlockingQueue<PartitionOutput>(poPoolSize);
        partitionOutputs = new RAMPartitionOutput[poPoolSize];
        for(int i = 0; i < partitionOutputs.length; i++) {
            try {
                partitionOutputs[i] = new RAMPartitionOutput(partitionManager);
                ((RAMPartitionOutput) partitionOutputs[i]).setName("PO-" + i);
                ((RAMPartitionOutput) partitionOutputs[i]).setLongIndexingRun(
                        longIndexingRun);
                poPool.put(partitionOutputs[i]);
            } catch(Exception ex) {
                throw new PropertyException(ex, ps.getInstanceName(),
                                            "PARTITION_OUTPUT_POOL_SIZE",
                                            "Error creating output pool");
            }
        }

        pollInterval = ps.getInt(PROP_POLL_INTERVAL);
        marshalThreads = new Thread[numMarshalThreads];
        marshalers = new MarshalThread[numMarshalThreads];
        for(int i = 0; i < marshalThreads.length; i++) {
            marshalers[i] = new MarshalThread();
            marshalThreads[i] = new Thread(marshalers[i]);
            marshalThreads[i].setName("Marshal-" + i);
            marshalThreads[i].start();
        }

        toFlush = new ArrayBlockingQueue<PartitionOutput>(poPoolSize);
        flusher = new FlushThread();
        flushThread = new Thread(flusher);
        flushThread.setName("Flusher");
        flushThread.start();
    }

    public String getName() {
        return name;
    }

    class MPHolder {

        public InvFileMemoryPartition part;

        public CountDownLatch completion;

        public Date time;

        public MPHolder(CountDownLatch completion) {
            this.completion = completion;
        }

        public MPHolder(InvFileMemoryPartition part, CountDownLatch completion,
                        Date time) {
            this.part = part;
            this.completion = completion;
            this.time = time;
        }
    }

    /**
     * A thread that will run, selecting partitions from the queue and
     * marshaling their data.
     */
    class MarshalThread implements Runnable {

        private CountDownLatch pauseRequested;
        
        private CountDownLatch flushRequested;
        
        private CountDownLatch readyToFlush;

        private CountDownLatch flushCompleted;
        
        protected NanoWatch fetchMPw = new NanoWatch();
        
        protected NanoWatch fetchPOw = new NanoWatch();
        
        protected NanoWatch marshalw = new NanoWatch();
        
        protected NanoWatch putMPw = new NanoWatch();
        
        protected NanoWatch putPOw = new NanoWatch();

        @Override
        public void run() {
            while(!marshalerDone) {
                try {
                    marshalOne();
                } catch(InterruptedException ex) {
                    logger.warning(String.
                            format(
                            "Marshaler interrupted during poll with %d waiting",
                            toMarshal.size()));
                    return;
                }
                
                if(pauseRequested != null) {
                    try {
                        logger.fine(String.format("%s pausing", Thread.currentThread().getName()));
                        pauseRequested.await();
                        logger.fine(String.format("%s resuming", Thread.currentThread().getName()));
                    } catch (InterruptedException ex) {
                        logger.warning(String.format("Interrupted while pausing"));
                        return;
                    }
                    pauseRequested = null;
                }

                //
                // See if we're supposed to flush.  If we are, wait until 
                // it's done.
                if(flushRequested != null) {
                    //
                    // Let the thread requesting the flush know that we're ready to go.      
                    readyToFlush.countDown();
                    try {
                        //
                        // Wait until the flush is done.
                        flushCompleted.await();
                    } catch(InterruptedException ex) {
                        logger.warning(String.format(
                                "Interrupted waiting for flush"));
                        return;
                    }
                }

            }

            //
            // Drain the list of partitions to dump and then dump them.
            List<MPHolder> l = new ArrayList<MPHolder>();
            toMarshal.drainTo(l);
            if(l.isEmpty()) {
                return;
            }

            try {
                for(MPHolder sh : l) {
                    if(sh.time.after(partitionManager.getLastPurgeTime())) {
                        try {
                            PartitionOutput partOut = poPool.take();
                            marshal(sh, partOut);
                        } catch(IOException ex) {
                            logger.log(Level.SEVERE, String.format(
                                    "Error marshaling partition"), ex);
                        }
                    }
                }
            } catch(InterruptedException ex) {
                logger.log(Level.SEVERE, String.format(
                        "Error flushing partitions at finish"), ex);
            }
        }

        private void marshalOne() throws InterruptedException {
            //
            // We'll poll for a defined interval so that we can catch when
            // we're finished.
            fetchMPw.start();
            MPHolder mph = toMarshal.poll(pollInterval,
                                           TimeUnit.MILLISECONDS);
            fetchMPw.stop();
            PartitionOutput partOut = null;
            if(mph != null) {
                //
                // If this partition was added after the time of the last
                // purge, then marshal it.
                if(mph.time.after(partitionManager.getLastPurgeTime())) {
                    try {

                        fetchPOw.start();
                        partOut = poPool.take();
                        fetchPOw.stop();
                        marshal(mph, partOut);
                        Merger m = partitionManager.getMerger();
                        if(m != null) {
                            m.run();
                        }
                    } catch(Exception ex) {
                        logger.log(Level.SEVERE,
                                   "Error marshaling partition, continuing",
                                   ex);
                        partOut.cleanUp();
                    }
                } else {
                    //
                    // This one was queued before the last purge, so
                    // clear it out and countdown anyone waiting on it.
                    mph.part.clear();
                    if(mph.completion != null) {
                        mph.completion.countDown();
                    }
                }
            }
        }
        
        public void pause(CountDownLatch pauseRequested) {
            this.pauseRequested = pauseRequested;
        }

        /**
         * Performs a synchronous flush of the marshaling queue, returning only when the
         * partitions have been flushed to disk and are available to search.
         */
        public void flush(CountDownLatch flushRequested) {
            readyToFlush = new CountDownLatch(1);
            flushCompleted = new CountDownLatch(1);
            this.flushRequested = flushRequested;

            try {
                //
                // Wait until the thread has stopped.
                readyToFlush.await();

                //
                // Marshal until the queue is empty, since nothing will be
                // added to the queue once we start.
                while(!toMarshal.isEmpty()) {
                    try {
                        marshalOne();
                    } catch(InterruptedException ex) {
                        logger.
                                warning(String.
                                format("Interrupted during flush"));
                        break;
                    }
                }
                CountDownLatch queueDrained = flusher.drainQueue();
                try {
                    queueDrained.await();
                } catch (InterruptedException ex) {
                    logger.warning(String.format("Interrupted waiting for flush queue drain"));
                }
                
            } catch(InterruptedException ex) {
                logger.warning(String.format(
                        "Interrupted waiting to start flush"));
            }

            //
            // Let the requesting thread know that we're done, and clobber the
            // local reference so that our thread won't try to flush again
            // immediately.
            this.flushRequested.countDown();
            this.flushRequested = null;
            readyToFlush = null;

            //
            // Let the thread continue.
            flushCompleted.countDown();
        }

        private void marshal(MPHolder mph, PartitionOutput partOut) throws
                IOException, InterruptedException {
            if(partOut != null) {
                //
                // Let the partition output know if there's a latch to signal 
                // when the marshal and flush has finished.
                partOut.setCompletion(mph.completion);
                marshalw.start();
                PartitionOutput ret = mph.part.marshal(partOut);
                marshalw.stop();
                mph.part.clear();
                putMPw.start();
                mpPool.put(mph.part);
                putMPw.stop();
                if(ret != null) {
                    if(logger.isLoggable(Level.FINEST)) {
                        logger.finest(String.format("Queuing %s for flush",
                                                    partOut));
                    }
                    toFlush.put(partOut);
                } else {
                    //
                    // Nothing got dumped, but we still need to put the partition
                    // output back in the pool!
                    putPOw.start();
                    poPool.put(partOut);
                    putPOw.stop();
                    
                    //
                    // We also need to countdown the completion latch that we
                    // were given.
                    if(mph.completion != null) {
                        mph.completion.countDown();
                    }
                }
            }
        }
    }

    /**
     * A thread that will flush the marshaled data for partitions to the disk.
     */
    class FlushThread implements Runnable {

        private NanoWatch flushw = new NanoWatch();
        
        private NanoWatch fetchPOw = new NanoWatch();
        
        private NanoWatch putPOw = new NanoWatch();
        
        private CountDownLatch queueDrained;
        
        /**
         * Tells the flush thread to let us know when the queue has drained.
         * @param queueDrained 
         */
        public synchronized CountDownLatch drainQueue() {
            if(queueDrained == null) {
                queueDrained = new CountDownLatch(1);
            }
            return queueDrained;
        }

        @Override
        public void run() {
            while(!flushDone) {
                try {
                    fetchPOw.start();
                    PartitionOutput partOut = toFlush.poll(pollInterval,
                                                           TimeUnit.MILLISECONDS);
                    fetchPOw.stop();
                    if(partOut != null) {
                        try {
                            try {
                                flushw.start();
                                partOut.flush();
                                flushw.stop();
                            } catch(IllegalStateException ex) {
                                logger.log(Level.SEVERE, String.format(
                                        "Illegal state for %s", partOut), ex);
                                throw (ex);
                            }
                            if(logger.isLoggable(Level.FINEST)) {
                                logger.finest(String.format(
                                        "Flushed %s in %.2fms",
                                                            partOut, flushw.
                                        getLastTimeMillis()));
                            } 
                            putPOw.start();
                            poPool.put(partOut);
                            putPOw.stop();
                            if(queueDrained != null && toFlush.size() == 0) {
                                queueDrained.countDown();
                                queueDrained = null;
                            }
                        } catch(IOException ex) {
                            logger.log(Level.SEVERE, String.format(
                                    "Error writing %d to disk", partOut.
                                    getPartitionNumber()), ex);
                        }
                    }
                } catch(InterruptedException ex) {
                    logger.log(Level.SEVERE, String.format(
                            "Interrupted getting partition output"), ex);
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
                    logger.log(Level.SEVERE, String.format(
                            "Error writing %d to disk", partOut.
                            getPartitionNumber()), ex);
                }
            }
        }
    }
}
