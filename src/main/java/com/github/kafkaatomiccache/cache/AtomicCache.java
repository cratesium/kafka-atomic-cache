package com.github.kafkaatomiccache.cache;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-free, atomic-swap cache backed by {@link AtomicReference}.
 *
 * <p>The live map is never mutated in place. On every refresh the entire
 * map is rebuilt off-thread and then swapped in with a single
 * {@code AtomicReference.set()} call. Reads via {@link #get(Object)}
 * never block.</p>
 *
 * @param <K> cache key type
 * @param <V> cache value type
 */
public class AtomicCache<K, V> {

    private final AtomicReference<Map<K, V>> cacheRef =
            new AtomicReference<>(Collections.emptyMap());

    /**
     * Return the value mapped to {@code key}, or {@code null}.
     * This is a plain volatile read — no locking.
     */
    public V get(K key) {
        return cacheRef.get().get(key);
    }

    /**
     * Return an <em>unmodifiable</em> snapshot of the full cache.
     */
    public Map<K, V> getAll() {
        return Collections.unmodifiableMap(cacheRef.get());
    }

    /**
     * Atomically replace the entire cache with {@code newData}.
     * The old map remains visible to any in-flight readers until the
     * swap completes.
     */
    public void swap(Map<K, V> newData) {
        cacheRef.set(Collections.unmodifiableMap(newData));
    }

    /**
     * Return the number of entries in the current cache snapshot.
     */
    public int size() {
        return cacheRef.get().size();
    }
}
