package com.github.kafkaatomiccache.demo;

import com.github.kafkaatomiccache.loader.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loads all {@link Product} rows from the DB and keys them by
 * {@code id}. This is the single point of truth for rebuilding the
 * "products" cache.
 */
@Component
public class ProductCacheLoader implements CacheLoader<Long, Product> {

    private static final Logger log = LoggerFactory.getLogger(ProductCacheLoader.class);

    private final ProductRepository productRepository;

    public ProductCacheLoader(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

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
