# kafka-atomic-cache

> Event-driven, lock-free in-memory cache sync across Kubernetes pods
> using Kafka and atomic reference swapping in Spring Boot.

## The Problem

In a multi-pod Kubernetes deployment, a common pattern is to refresh
an in-memory cache on a schedule — clear it every N seconds and reload
from the database. This creates three compounding problems:

- **DB spikes** — N pods each hit the database independently on their
  own timers
- **Cache inconsistency** — pods are out of sync between refresh cycles
- **Race conditions** — requests arriving during the clear-and-reload
  window hit an empty cache and fall through to the DB

## The Solution

Replace the polling loop with an event-driven update. When data changes
in the database, a single Kafka event is published. Every pod consumes
that event, builds a completely new cache map in the background, and
atomically swaps the old reference for the new one — all without ever
clearing the old cache first.

Requests see a fully consistent cache at all times. No empty window.
No DB fallback. No inter-pod coordination.

## Architecture
```
DB change → Kafka event → all pods consume
                              ↓
                    each pod builds new cache
                              ↓
                    AtomicReference.set(newCache)
                              ↓
                    requests seamlessly use new cache
```

## Design Principles

**Atomic swap over clear-and-reload.** The old cache stays live and
serves traffic until the new one is fully constructed. Only then is the
reference replaced in a single atomic operation.

**Immutable cache objects.** Once a cache map is published via
`AtomicReference.set()`, it is never mutated. No `put`, no `remove`,
no in-place updates on the shared reference.

**Lock-free reads.** `AtomicReference.get()` needs no synchronization.
Reads never block, regardless of how many concurrent updates are
happening.

**Event-driven convergence.** All pods consume the same Kafka event
and independently rebuild to the same state. No leader election, no
distributed locking, no cross-pod communication required.

## Key Components

### CacheManager

Holds an `AtomicReference<Map<K, V>>`. Exposes a read method that calls
`get()` directly — no locking — and an update method that accepts a
fully-built map and calls `set()` to replace the reference atomically.

### KafkaConsumer

Annotated with `@KafkaListener`. On receiving a cache invalidation
event, it fetches the latest data from the DB (or derives it from the
event payload for delta updates), constructs a new `HashMap` or
`ImmutableMap`, and hands it to `CacheManager.update()`. The old cache
continues serving traffic throughout this process.

### Service layer

Reads from the cache via `CacheManager.get()`. No synchronization
primitives, no fallback logic, no awareness of update cycles.

## Thread Safety Contract

| Operation | Mechanism | Blocking? |
|---|---|---|
| Read | `AtomicReference.get()` | Never |
| Full rebuild | New map constructed off-thread | No |
| Swap | `AtomicReference.set()` | Single CAS |
| Mutation of live cache | **Prohibited** | — |

## Edge Cases Handled

- **Multiple events arriving rapidly** — last write wins; each swap is
  independent and idempotent
- **Slow DB fetch during rebuild** — old cache continues serving
  requests with no degradation
- **Pod restart** — cache initializes from DB on startup before the pod
  begins serving traffic
- **Duplicate Kafka events** — consumer is idempotent; rebuilding to
  the same data has no side effect

## What This Replaces

| Before | After |
|---|---|
| `@Scheduled` every 5s | `@KafkaListener` on change |
| `cache.clear()` then reload | Build new → `AtomicReference.set()` |
| N pods × N DB calls per cycle | 1 DB call per event total |
| Cache empty during refresh | Old cache live until swap |
| Reads synchronized or stale | Lock-free reads always |

## Tech Stack

- Java 17+
- Spring Boot 3.x
- Apache Kafka + Spring Kafka
- `java.util.concurrent.atomic.AtomicReference`
- `java.util.concurrent.ConcurrentHashMap`

## Getting Started
```bash
git clone https://github.com/your-org/kafka-atomic-cache
cd kafka-atomic-cache
./mvnw spring-boot:run
```

Requires a running Kafka broker. Configure the broker address and topic
name in `application.yml`.

## Configuration
```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: cache-consumer-group
      auto-offset-reset: earliest

cache:
  topic: cache-invalidation-events
  initial-load-on-startup: true
```

## License

MIT
