package com.github.kafkaatomiccache.event;

import com.github.kafkaatomiccache.loader.CacheRefreshOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for {@link CacheInvalidationEvent}
 * messages and triggers a cache rebuild via the
 * {@link CacheRefreshOrchestrator}.
 */
@Component
public class CacheInvalidationConsumer {

    private static final Logger log = LoggerFactory.getLogger(CacheInvalidationConsumer.class);

    private final CacheRefreshOrchestrator orchestrator;

    public CacheInvalidationConsumer(CacheRefreshOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @KafkaListener(
            topics = "${cache.topic:cache-invalidation-events}",
            groupId = "${spring.kafka.consumer.group-id:cache-consumer-group}"
    )
    public void onCacheInvalidation(CacheInvalidationEvent event) {
        log.info("Received cache invalidation event for '{}' (ts={})",
                event.getCacheName(), event.getTimestamp());
        orchestrator.refresh(event.getCacheName());
    }
}
