package com.github.kafkaatomiccache.loader;

import com.github.kafkaatomiccache.cache.AtomicCache;
import com.github.kafkaatomiccache.cache.CacheManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Component
public class CacheRefreshOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(CacheRefreshOrchestrator.class);

    private final CacheManager cacheManager;
    private final Map<String, CacheLoader<?, ?>> loadersByName = new ConcurrentHashMap<>();

    @Value("${cache.initial-load-on-startup:true}")
    private boolean initialLoadOnStartup;

    public CacheRefreshOrchestrator(CacheManager cacheManager,
                                    List<CacheLoader<?, ?>> loaders) {
        this.cacheManager = cacheManager;
        loaders.forEach(loader -> loadersByName.put(loader.cacheName(), loader));
    }

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

    public void refreshAll() {
        loadersByName.keySet().forEach(this::refresh);
    }
}
