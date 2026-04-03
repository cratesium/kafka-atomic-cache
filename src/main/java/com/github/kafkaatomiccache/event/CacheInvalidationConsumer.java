package com.github.kafkaatomiccache.event;

import com.github.kafkaatomiccache.loader.CacheRefreshOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer that listens for {@link CacheInvalidationEvent}
 * messages and triggers a cache rebuild via the
 * {@link CacheRefreshOrchestrator}.
 *
 * <p>Because every pod in the Kafka consumer group is configured with
 * a <em>unique</em> group-id (or — in broadcast mode — a shared group
 * whose partitions are spread across all pods), each pod receives every
 * event and independently rebuilds its local cache.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationConsumer {

    private final CacheRefreshOrchestrator orchestrator;

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
