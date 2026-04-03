package com.github.kafkaatomiccache.loader;

import com.github.kafkaatomiccache.cache.AtomicCache;
import com.github.kafkaatomiccache.cache.CacheManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers all {@link CacheLoader} beans, registers their caches,
 * performs the initial load on startup, and exposes a {@link #refresh}
 * method for event-driven rebuilds.
 */
@Slf4j
@Component
public class CacheRefreshOrchestrator {

    private final CacheManager cacheManager;
    private final Map<String, CacheLoader<?, ?>> loadersByName = new ConcurrentHashMap<>();

    @Value("${cache.initial-load-on-startup:true}")
    private boolean initialLoadOnStartup;

    public CacheRefreshOrchestrator(CacheManager cacheManager,
                                    List<CacheLoader<?, ?>> loaders) {
        this.cacheManager = cacheManager;
        loaders.forEach(loader -> loadersByName.put(loader.cacheName(), loader));
    }

    /**
     * Register caches and perform the initial data load for every
     * discovered {@link CacheLoader}.
     */
    @PostConstruct
    public void initialize() {
        loadersByName.forEach((name, loader) -> {
            cacheManager.register(name, new AtomicCache<>());
            log.info("Registered cache: {}", name);
        });

        if (initialLoadOnStartup) {
            log.info("Performing initial cache load for {} cache(s)…", loadersByName.size());
            loadersByName.keySet().forEach(this::refresh);
        }
    }

    /**
     * Rebuild a single named cache by calling its loader and atomically
     * swapping the result into the live reference.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void refresh(String cacheName) {
        CacheLoader loader = loadersByName.get(cacheName);
        if (loader == null) {
            log.warn("No CacheLoader registered for cache '{}' — skipping refresh", cacheName);
            return;
        }

        long start = System.currentTimeMillis();
        Map data = loader.loadAll();
        AtomicCache cache = cacheManager.getCache(cacheName);
        cache.swap(data);
        long elapsed = System.currentTimeMillis() - start;

        log.info("Cache '{}' refreshed — {} entries in {} ms", cacheName, data.size(), elapsed);
    }

    /**
     * Refresh <em>all</em> registered caches.
     */
    public void refreshAll() {
        loadersByName.keySet().forEach(this::refresh);
    }
}
