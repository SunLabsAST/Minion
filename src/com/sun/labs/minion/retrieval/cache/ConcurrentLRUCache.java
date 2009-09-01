package com.sun.labs.minion.retrieval.cache;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

/**
 * A concurrent LRU cache that stores a map from keys to
 * time-stamped futures, allowing long-running computations to run in parallel.
 * 
 * @param <K> The key type
 * @param <V> The value type
 */
public class ConcurrentLRUCache<K,V> {

    private ConcurrentMap<K,Holder<V>> map;

    private long time;

    private long hits;

    private long misses;
    
    private CacheValueComputer<K,V> c;

    public ConcurrentLRUCache(int size, CacheValueComputer<K,V> c) {
        this.c = c;
        map = new ConcurrentHashMap<K, Holder<V>>(size, 0.75f, 100);
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
                
                Holder<V> hn = new Holder(new FutureTask<V>(eval), time++);
                h = map.putIfAbsent(key, hn);
                if(h == null) {
                    misses++;
                    h = hn;
                    hn.run();
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
        return (double) hits / (hits+misses);
    }

    public long getHits() {
        return hits;
    }

    public long getMisses() {
        return misses;
    }

    /**
     * A class to hold a future task and a timestamp.
     * @param <V>
     */
    private static class Holder<V> {

        public Holder(FutureTask<V> f, long ts) {
            this.f = f;
            this.ts = ts;
        }

        public void run() {
            f.run();
        }

        public V get() throws InterruptedException, ExecutionException {
            return f.get();
        }

        FutureTask<V> f;
        long ts;
    }
}
