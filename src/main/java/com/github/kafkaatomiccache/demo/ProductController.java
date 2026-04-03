package com.github.kafkaatomiccache.demo;

import com.github.kafkaatomiccache.cache.AtomicCache;
import com.github.kafkaatomiccache.cache.CacheManager;
import com.github.kafkaatomiccache.event.CacheInvalidationProducer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * Demo REST controller that reads exclusively from the atomic cache
 * and publishes Kafka invalidation events after DB writes.
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final CacheManager cacheManager;
    private final ProductRepository productRepository;
    private final CacheInvalidationProducer invalidationProducer;

    private static final String CACHE_NAME = "products";

    public ProductController(CacheManager cacheManager,
                             ProductRepository productRepository,
                             CacheInvalidationProducer invalidationProducer) {
        this.cacheManager = cacheManager;
        this.productRepository = productRepository;
        this.invalidationProducer = invalidationProducer;
    }

    // ── Reads (lock-free, straight from cache) ───────────────────────

    @GetMapping
    public Collection<Product> getAll() {
        AtomicCache<Long, Product> cache = cacheManager.getCache(CACHE_NAME);
        return cache.getAll().values();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable Long id) {
        AtomicCache<Long, Product> cache = cacheManager.getCache(CACHE_NAME);
        Product product = cache.get(id);
        return product != null
                ? ResponseEntity.ok(product)
                : ResponseEntity.notFound().build();
    }

    // ── Writes (DB mutation → Kafka event) ───────────────────────────

    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        Product saved = productRepository.save(product);
        invalidationProducer.invalidate(CACHE_NAME);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id,
                                          @RequestBody Product product) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        product.setId(id);
        Product saved = productRepository.save(product);
        invalidationProducer.invalidate(CACHE_NAME);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!productRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        productRepository.deleteById(id);
        invalidationProducer.invalidate(CACHE_NAME);
        return ResponseEntity.noContent().build();
    }

    // ── Manual invalidation endpoint ─────────────────────────────────

    @PostMapping("/invalidate")
    public ResponseEntity<String> invalidate() {
        invalidationProducer.invalidate(CACHE_NAME);
        return ResponseEntity.ok("Cache invalidation event published for '" + CACHE_NAME + "'");
    }

    // ── Cache stats ──────────────────────────────────────────────────

    @GetMapping("/cache/stats")
    public Map<String, Object> cacheStats() {
        AtomicCache<Long, Product> cache = cacheManager.getCache(CACHE_NAME);
        return Map.of(
                "cacheName", CACHE_NAME,
                "size", cache.size()
        );
    }
}
