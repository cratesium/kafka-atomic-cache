package com.github.kafkaatomiccache;

import com.github.kafkaatomiccache.cache.CacheManager;
import com.github.kafkaatomiccache.event.CacheInvalidationEvent;
import com.github.kafkaatomiccache.event.CacheInvalidationProducer;
import com.github.kafkaatomiccache.demo.Product;
import com.github.kafkaatomiccache.demo.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end integration test that starts an embedded Kafka broker,
 * publishes an invalidation event, and asserts that the consumer
 * triggers a cache rebuild.
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = "cache-invalidation-events",
        brokerProperties = {
                "listeners=PLAINTEXT://localhost:0",
                "port=0"
        }
)
@DirtiesContext
@ActiveProfiles("test")
class CacheIntegrationTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CacheInvalidationProducer producer;

    @Test
    @DisplayName("Publishing invalidation event triggers cache rebuild with new data")
    void invalidationEventTriggersCacheRebuild() {
        // The cache should already be populated from data.sql on startup
        int initialSize = cacheManager.<Long, Product>getCache("products").size();

        // Insert a new product directly into the DB
        Product newProduct = new Product(null, "Test Widget", 19.99);
        productRepository.save(newProduct);

        // Publish an invalidation event
        producer.invalidate("products");

        // Wait for the consumer to process the event and rebuild the cache
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(cacheManager.<Long, Product>getCache("products").size())
                                .isGreaterThan(initialSize)
                );

        // Verify the new product is in the cache
        Product cached = cacheManager.<Long, Product>getCache("products")
                .get(newProduct.getId());
        assertThat(cached).isNotNull();
        assertThat(cached.getName()).isEqualTo("Test Widget");
    }
}
