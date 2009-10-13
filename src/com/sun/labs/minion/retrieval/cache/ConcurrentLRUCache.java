package com.sun.labs.minion.retrieval.cache;

import com.sun.labs.minion.util.NanoWatch;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A concurrent LRU cache that stores a map from keys to
 * time-stamped futures, allowing long-running computations to run in parallel.
 * 
 * @param <K> The key type
 * @param <V> The value type
 */
public class ConcurrentLRUCache<K, V> {

    private static Logger logger = Logger.getLogger(ConcurrentLRUCache.class.
            getName());

    private ConcurrentMap<K, Holder<V>> map;

    private int lowWater;

    private int highWater;

    private long time;

    private long hits;

    private long misses;

    private CacheValueComputer<K, V> c;

    private Cleaner cleaner;

    private Thread cleanerThread;

    private String name;

    public ConcurrentLRUCache(int size, CacheValueComputer<K, V> c) {
        this.c = c;
        map = new ConcurrentHashMap<K, Holder<V>>(size, 0.75f, 100);
        lowWater = size;
        highWater = (int) (size * 1.1);
        cleaner = new Cleaner();
        cleanerThread = new Thread(cleaner, "CacheCleaner");
        cleanerThread.setDaemon(true);
        cleanerThread.start();
    }

    public V get(final K key) throws InterruptedException {
        while(true) {
            Holder<V> h = map.get(key);
            if(h == null) {
                Callable<V> eval = new Callable<V>() {

                    @Override
                    public V call() throws Exception {
                        return c.compute(key);
                    }
                };

                Holder<V> hn = new Holder(key, new FutureTask<V>(eval), time++);
                h = map.putIfAbsent(key, hn);
                if(h == null) {
                    misses++;
                    h = hn;
                    hn.run();
                    if(map.size() >= highWater) {
                        cleaner.wake();
                    }
                }
            } else {
                hits++;
            }
            try {
                return h.get();
            } catch(CancellationException ex) {
                map.remove(key, h);
            } catch(ExecutionException ex) {
                throw new RuntimeException("Error executing task for " + key,
                                           ex);
            }
        }
    }

    public int size() {
        return map.size();
    }

    public double hitRatio() {
        return (double) hits / (hits + misses);
    }

    public long getHits() {
        return hits;
    }

    public long getMisses() {
        return misses;
    }

    public int getHighWater() {
        return highWater;
    }

    public void setHighWater(int highWater) {
        this.highWater = highWater;
    }

    public int getLowWater() {
        return lowWater;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void close() {
        cleaner.setFinished(true);
    }

    /**
     * A class to hold a future task and a timestamp.
     * @param <V>
     */
    private class Holder<V> {

        FutureTask<V> f;

        long ts;

        K key;

        public Holder(K key, FutureTask<V> f, long ts) {
            this.key = key;
            this.f = f;
            this.ts = ts;
        }

        public void run() {
            f.run();
        }

        public V get() throws InterruptedException, ExecutionException {
            return f.get();
        }

        public K getKey() {
            return key;
        }
    }

    /**
     * A runnable class that we'll use to keep the cache size down.
     */
    private class Cleaner implements Runnable {

        private Comparator<Holder<V>> comp = new Comparator<Holder<V>>() {

            @Override
            public int compare(Holder<V> o1,
                               Holder<V> o2) {
                if(o1.ts < o2.ts) {
                    return -1;
                } else if(o1.ts > o2.ts) {
                    return 1;
                }
                return 0;
            }
        };

        private PriorityQueue<Holder<V>> togo;

        private boolean finished;

        public Cleaner() {
            togo = new PriorityQueue<Holder<V>>(highWater - lowWater, comp);
        }

        public synchronized void wake() {
            this.notifyAll();
        }

        public void setFinished(boolean finished) {
            this.finished = finished;
        }


        @Override
        public void run() {
            while(!finished) {
                try {
                    synchronized(this) {
                        this.wait();
                    }

                    int s = map.size();
                    if(s < highWater) {
                        continue;
                    }
                    
                    NanoWatch nw = new NanoWatch();
                    nw.start();

                    //
                    // We'll use the priority queue to build a min-heap of the
                    // oldest entries, which we'll then remove.
                    int n = s - lowWater;
                    for(Holder<V> h : map.values()) {
                        if(togo.size() < n) {
                            togo.offer(h);
                        } else {
                            Holder<V> top = togo.peek();
                            if(comp.compare(h, top) < 0) {
                                togo.poll();
                                togo.offer(h);
                            }
                        }
                    }
                    while(togo.size() > 0) {
                        Holder<V> h = togo.poll();
                        map.remove(h.getKey(), h);
                    }
                    nw.stop();
                    if(logger.isLoggable(Level.FINER)) {
                        logger.finer(String.format(
                                "%s removed %d entries in %.3f",
                                                   name, n, nw.getTimeMillis()));
                    }
                } catch(InterruptedException ex) {
                }
            }
        }
    }
}
