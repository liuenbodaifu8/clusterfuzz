package com.google.clusterfuzz.core.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Cache configuration for ClusterFuzz entities and operations.
 * Provides both in-memory and distributed caching strategies.
 */
@Configuration
@EnableCaching
public class CacheConfiguration {

    public static final String TESTCASE_CACHE = "testcases";
    public static final String ISSUE_CACHE = "issues";
    public static final String JOB_CACHE = "jobs";
    public static final String BOT_CACHE = "bots";
    public static final String COVERAGE_CACHE = "coverage";
    public static final String CRASH_ANALYSIS_CACHE = "crash_analysis";
    public static final String FUZZING_ENGINE_CACHE = "fuzzing_engines";

    /**
     * Default cache manager for development and testing.
     */
    @Bean
    @Profile({"default", "test"})
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(java.util.List.of(
            TESTCASE_CACHE,
            ISSUE_CACHE,
            JOB_CACHE,
            BOT_CACHE,
            COVERAGE_CACHE,
            CRASH_ANALYSIS_CACHE,
            FUZZING_ENGINE_CACHE
        ));
        return cacheManager;
    }

    /**
     * Redis cache manager for production environments.
     */
    @Bean
    @Profile("production")
    public CacheManager redisCacheManager() {
        // Redis cache configuration would go here
        // For now, return the concurrent map cache manager
        return cacheManager();
    }

    /**
     * Cache configuration for testcases.
     */
    @Bean
    public MutableConfiguration<Long, Object> testcaseCacheConfiguration() {
        return new MutableConfiguration<Long, Object>()
            .setTypes(Long.class, Object.class)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_HOUR))
            .setStatisticsEnabled(true);
    }

    /**
     * Cache configuration for crash analysis results.
     */
    @Bean
    public MutableConfiguration<String, Object> crashAnalysisCacheConfiguration() {
        return new MutableConfiguration<String, Object>()
            .setTypes(String.class, Object.class)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.THIRTY_MINUTES))
            .setStatisticsEnabled(true);
    }

    /**
     * Cache configuration for fuzzing engine status.
     */
    @Bean
    public MutableConfiguration<String, Object> fuzzingEngineCacheConfiguration() {
        return new MutableConfiguration<String, Object>()
            .setTypes(String.class, Object.class)
            .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.MINUTES, 5)))
            .setStatisticsEnabled(true);
    }
}