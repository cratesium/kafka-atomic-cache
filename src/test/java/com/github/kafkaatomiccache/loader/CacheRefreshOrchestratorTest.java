package com.github.kafkaatomiccache.loader;

import com.github.kafkaatomiccache.cache.CacheManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class CacheRefreshOrchestratorTest {

    private CacheManager cacheManager;
    private CacheRefreshOrchestrator orchestrator;

    /** Simple in-memory loader for tests. */
    private static class StubLoader implements CacheLoader<String, Integer> {
        private Map<String, Integer> data;

        StubLoader(Map<String, Integer> initialData) {
            this.data = initialData;
        }

        void setData(Map<String, Integer> newData) {
            this.data = newData;
        }

        @Override
        public String cacheName() {
            return "stub";
        }

        @Override
        public Map<String, Integer> loadAll() {
            return data;
        }
    }

    @BeforeEach
    void setUp() {
        cacheManager = new CacheManager();
    }

    /** Set the @Value field that Spring would normally inject. */
    private void enableInitialLoad(CacheRefreshOrchestrator orch) throws Exception {
        Field field = CacheRefreshOrchestrator.class.getDeclaredField("initialLoadOnStartup");
        field.setAccessible(true);
        field.set(orch, true);
    }

    @Test
    @DisplayName("initialize() registers caches and loads data")
    void initializeLoadsCaches() throws Exception {
        StubLoader loader = new StubLoader(Map.of("a", 1, "b", 2));
        orchestrator = new CacheRefreshOrchestrator(cacheManager, List.of(loader));
        enableInitialLoad(orchestrator);
        orchestrator.initialize();

        assertThat(cacheManager.hasCache("stub")).isTrue();
        assertThat(cacheManager.<String, Integer>getCache("stub").size()).isEqualTo(2);
        assertThat(cacheManager.<String, Integer>getCache("stub").get("a")).isEqualTo(1);
    }

    @Test
    @DisplayName("refresh() swaps cache data to latest")
    void refreshSwapsData() throws Exception {
        StubLoader loader = new StubLoader(Map.of("x", 10));
        orchestrator = new CacheRefreshOrchestrator(cacheManager, List.of(loader));
        enableInitialLoad(orchestrator);
        orchestrator.initialize();

        // Mutate the loader's backing data
        loader.setData(Map.of("x", 20, "y", 30));
        orchestrator.refresh("stub");

        assertThat(cacheManager.<String, Integer>getCache("stub").get("x")).isEqualTo(20);
        assertThat(cacheManager.<String, Integer>getCache("stub").get("y")).isEqualTo(30);
        assertThat(cacheManager.<String, Integer>getCache("stub").size()).isEqualTo(2);
    }

    @Test
    @DisplayName("refresh() with unknown name logs warning and does not throw")
    void refreshUnknownCacheNameDoesNotThrow() {
        orchestrator = new CacheRefreshOrchestrator(cacheManager, List.of());
        orchestrator.initialize();

        assertThatCode(() -> orchestrator.refresh("nonexistent"))
                .doesNotThrowAnyException();
    }
}

