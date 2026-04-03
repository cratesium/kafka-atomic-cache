package com.github.kafkaatomiccache.demo;

import com.github.kafkaatomiccache.loader.CacheLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loads all {@link Product} rows from the DB and keys them by
 * {@code id}. This is the single point of truth for rebuilding the
 * "products" cache.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheLoader implements CacheLoader<Long, Product> {

    private final ProductRepository productRepository;

    @Override
    public String cacheName() {
        return "products";
    }

    @Override
    public Map<Long, Product> loadAll() {
        log.info("Loading all products from database…");
        return productRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }
}
