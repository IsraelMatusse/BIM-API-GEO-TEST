package com.bim.api_test.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;


@Configuration
@EnableCaching
public class CacheConfig {

    public static final String GEO_API_CACHE = "geoApi";
    public static final String WEATHER_API_CACHE = "weatherApi";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCache geoApiCache = new CaffeineCache(GEO_API_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(24, TimeUnit.HOURS)
                        .maximumSize(2_000)
                        .recordStats()
                        .build());

        CaffeineCache weatherApiCache = new CaffeineCache(WEATHER_API_CACHE,
                Caffeine.newBuilder()
                        .expireAfterWrite(10, TimeUnit.MINUTES)
                        .maximumSize(2_000)
                        .recordStats()
                        .build());

        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(geoApiCache, weatherApiCache));
        return manager;
    }
}
