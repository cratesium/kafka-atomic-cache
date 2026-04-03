package com.github.kafkaatomiccache.cache;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of named {@link AtomicCache} instances.
 *
 * <p>Each logical cache (e.g. "products", "users") is stored under a
 * unique name and can be looked up at runtime by consumers and
 * orchestrators.</p>
 */
@Component
public class CacheManager {

    private final ConcurrentHashMap<String, AtomicCache<?, ?>> caches =
            new ConcurrentHashMap<>();

    /**
     * Register (or replace) a named cache.
     */
    public <K, V> void register(String name, AtomicCache<K, V> cache) {
        caches.put(name, cache);
    }

    /**
     * Retrieve a previously-registered cache by name.
     *
     * @throws IllegalArgumentException if no cache is registered under {@code name}
     */
    @SuppressWarnings("unchecked")
    public <K, V> AtomicCache<K, V> getCache(String name) {
        AtomicCache<K, V> cache = (AtomicCache<K, V>) caches.get(name);
        if (cache == null) {
            throw new IllegalArgumentException("No cache registered with name: " + name);
        }
        return cache;
    }

    /**
     * Check whether a cache with the given name exists.
     */
    public boolean hasCache(String name) {
        return caches.containsKey(name);
    }
}
