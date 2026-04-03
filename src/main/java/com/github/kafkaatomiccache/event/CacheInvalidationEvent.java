package com.github.kafkaatomiccache.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Payload published to the Kafka cache-invalidation topic.
 *
 * <p>{@code cacheName} identifies which cache should be rebuilt.
 * An optional {@code payload} field can carry delta information,
 * though the default behaviour is a full reload.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CacheInvalidationEvent {

    /** Which cache to invalidate (e.g. "products"). */
    private String cacheName;

    /** Epoch-millis timestamp of the event. */
    private long timestamp;

    /** Optional extra payload for delta-aware consumers. */
    private String payload;

    /**
     * Convenience factory for simple full-reload events.
     */
    public static CacheInvalidationEvent of(String cacheName) {
        return new CacheInvalidationEvent(cacheName, Instant.now().toEpochMilli(), null);
    }
}
