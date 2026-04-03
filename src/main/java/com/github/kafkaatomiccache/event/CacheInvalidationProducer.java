package com.github.kafkaatomiccache.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link CacheInvalidationEvent} messages to Kafka.
 *
 * <p>Services call {@link #invalidate(String)} after mutating data.
 * Every pod in the consumer group receives the event and rebuilds
 * the corresponding cache independently.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationProducer {

    private final KafkaTemplate<String, CacheInvalidationEvent> kafkaTemplate;

    @Value("${cache.topic:cache-invalidation-events}")
    private String topic;

    /**
     * Publish an invalidation event for the given cache name.
     */
    public void invalidate(String cacheName) {
        CacheInvalidationEvent event = CacheInvalidationEvent.of(cacheName);
        log.info("Publishing cache invalidation event for '{}'", cacheName);
        kafkaTemplate.send(topic, cacheName, event);
    }

    /**
     * Publish a pre-built event (e.g. with a custom payload).
     */
    public void publish(CacheInvalidationEvent event) {
        log.info("Publishing cache invalidation event for '{}'", event.getCacheName());
        kafkaTemplate.send(topic, event.getCacheName(), event);
    }
}
