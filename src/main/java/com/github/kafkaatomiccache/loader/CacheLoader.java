package com.github.kafkaatomiccache.loader;

import java.util.Map;

/**
 * Strategy interface that users implement to supply data for a named cache.
 *
 * <p>Spring will discover all {@code CacheLoader} beans on startup and
 * hand them to the {@link CacheRefreshOrchestrator}, which calls
 * {@link #loadAll()} to populate (or rebuild) the corresponding
 * {@link com.github.kafkaatomiccache.cache.AtomicCache}.</p>
 *
 * @param <K> cache key type
 * @param <V> cache value type
 */
public interface CacheLoader<K, V> {

    /**
     * Logical name of the cache this loader populates
     * (e.g. {@code "products"}).
     */
    String cacheName();

    /**
     * Load the complete data set for this cache.
     * The returned map is handed directly to
     * {@link com.github.kafkaatomiccache.cache.AtomicCache#swap(Map)}.
     */
    Map<K, V> loadAll();
}
