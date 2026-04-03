package com.github.kafkaatomiccache.event;

import java.time.Instant;

/**
 * Payload published to the Kafka cache-invalidation topic.
 *
 * <p>{@code cacheName} identifies which cache should be rebuilt.
 * An optional {@code payload} field can carry delta information,
 * though the default behaviour is a full reload.</p>
 */
public class CacheInvalidationEvent {

    private String cacheName;
    private long timestamp;
    private String payload;

    public CacheInvalidationEvent() {
    }

    public CacheInvalidationEvent(String cacheName, long timestamp, String payload) {
        this.cacheName = cacheName;
        this.timestamp = timestamp;
        this.payload = payload;
    }

    public static CacheInvalidationEvent of(String cacheName) {
        return new CacheInvalidationEvent(cacheName, Instant.now().toEpochMilli(), null);
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}
