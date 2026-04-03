package com.github.kafkaatomiccache.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

class AtomicCacheTest {

    private AtomicCache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new AtomicCache<>();
    }

    @Test
    @DisplayName("New cache is empty")
    void newCacheIsEmpty() {
        assertThat(cache.size()).isZero();
        assertThat(cache.getAll()).isEmpty();
        assertThat(cache.get("any")).isNull();
    }

    @Test
    @DisplayName("swap() replaces the entire cache contents")
    void swapReplacesCacheContents() {
        Map<String, String> data = Map.of("a", "1", "b", "2");
        cache.swap(data);

        assertThat(cache.size()).isEqualTo(2);
        assertThat(cache.get("a")).isEqualTo("1");
        assertThat(cache.get("b")).isEqualTo("2");
    }

    @Test
    @DisplayName("swap() with new data discards old entries")
    void swapDiscardsOldEntries() {
        cache.swap(Map.of("old", "value"));
        cache.swap(Map.of("new", "value"));

        assertThat(cache.get("old")).isNull();
        assertThat(cache.get("new")).isEqualTo("value");
        assertThat(cache.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("getAll() returns unmodifiable map")
    void getAllReturnsUnmodifiableMap() {
        cache.swap(Map.of("k", "v"));
        Map<String, String> all = cache.getAll();

        assertThatThrownBy(() -> all.put("new", "entry"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Concurrent reads never see an inconsistent state")
    void concurrentReadsAreConsistent() throws InterruptedException {
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // Pre-populate
        Map<String, String> v1 = Map.of("key", "version1");
        Map<String, String> v2 = Map.of("key", "version2");
        cache.swap(v1);

        for (int i = 0; i < threadCount; i++) {
            final boolean swapHalfWay = (i == threadCount / 2);
            executor.submit(() -> {
                try {
                    String value = cache.get("key");
                    // Value must always be one of the two versions, never null
                    assertThat(value).isIn("version1", "version2");
                    if (swapHalfWay) {
                        cache.swap(v2);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
    }
}
