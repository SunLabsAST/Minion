package com.sun.labs.minion.retrieval.cache;

/**
 * An interface for things that will compute the value that should be stored
 * in the cache for a given key.
 */
public interface CacheValueComputer<K,V> {

    public V compute(K key);

}
