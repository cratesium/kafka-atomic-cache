package com.github.kafkaatomiccache.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@Component
public class CacheInvalidationProducer {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationProducer.class);

    private final KafkaTemplate<String, CacheInvalidationEvent> kafkaTemplate;

    @Value("${cache.topic:cache-invalidation-events}")
    private String topic;

    public CacheInvalidationProducer(KafkaTemplate<String, CacheInvalidationEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void invalidate(String cacheName) {
        CacheInvalidationEvent event = CacheInvalidationEvent.of(cacheName);
        log.info("Publishing cache invalidation event for '{}'", cacheName);
        kafkaTemplate.send(topic, cacheName, event);
    }

    public void publish(CacheInvalidationEvent event) {
        log.info("Publishing cache invalidation event for '{}'", event.getCacheName());
        kafkaTemplate.send(topic, event.getCacheName(), event);
    }
}
