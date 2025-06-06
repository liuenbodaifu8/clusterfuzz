package com.google.clusterfuzz.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClusterFuzz configuration properties.
 */
@SpringBootTest
@ActiveProfiles("test")
class ClusterFuzzConfigurationPropertiesTest {

    @Autowired
    private ClusterFuzzConfigurationProperties properties;

    @Test
    @DisplayName("Should load default database configuration")
    void testDefaultDatabaseConfiguration() {
        var database = properties.getDatabase();
        
        assertNotNull(database);
        assertEquals("jdbc:postgresql://localhost:5432/clusterfuzz", database.getUrl());
        assertEquals("clusterfuzz", database.getUsername());
        assertEquals("password", database.getPassword());
        assertEquals(20, database.getMaxPoolSize());
        assertEquals(5, database.getMinIdleSize());
        assertEquals(Duration.ofSeconds(30), database.getConnectionTimeout());
        assertTrue(database.isEnableSecondLevelCache());
        assertEquals(50, database.getBatchSize());
    }

    @Test
    @DisplayName("Should load default security configuration")
    void testDefaultSecurityConfiguration() {
        var security = properties.getSecurity();
        
        assertNotNull(security);
        assertTrue(security.isEnableCsrf());
        assertTrue(security.isEnableHttps());
        assertTrue(security.isEnableHsts());
        assertEquals(31536000L, security.getHstsMaxAge());
        
        // JWT configuration
        var jwt = security.getJwt();
        assertNotNull(jwt);
        assertEquals(Duration.ofHours(24), jwt.getExpiration());
        assertEquals(Duration.ofDays(7), jwt.getRefreshExpiration());
        assertEquals("clusterfuzz", jwt.getIssuer());
        assertTrue(jwt.isEnableRefreshTokenRotation());
        
        // CORS configuration
        var cors = security.getCors();
        assertNotNull(cors);
        assertTrue(cors.isEnabled());
        assertTrue(cors.isAllowCredentials());
        assertEquals(3600L, cors.getMaxAge());
    }

    @Test
    @DisplayName("Should load default fuzzing configuration")
    void testDefaultFuzzingConfiguration() {
        var fuzzing = properties.getFuzzing();
        
        assertNotNull(fuzzing);
        assertEquals(100, fuzzing.getMaxConcurrentJobs());
        assertEquals(Duration.ofHours(24), fuzzing.getJobTimeout());
        assertEquals(Duration.ofMinutes(5), fuzzing.getHeartbeatInterval());
        assertEquals(3, fuzzing.getMaxRetries());
        assertTrue(fuzzing.isEnableCrashDeduplication());
        assertTrue(fuzzing.isEnableMinimization());
        
        // LibFuzzer configuration
        var libFuzzer = fuzzing.getLibFuzzer();
        assertNotNull(libFuzzer);
        assertTrue(libFuzzer.isEnabled());
        assertEquals(3600, libFuzzer.getMaxTotalTime());
        assertEquals(10000, libFuzzer.getMaxLen());
        assertEquals(8, libFuzzer.getWorkers());
        
        // AFL configuration
        var afl = fuzzing.getAfl();
        assertNotNull(afl);
        assertTrue(afl.isEnabled());
        assertEquals(Duration.ofMinutes(60), afl.getTimeout());
        assertEquals(200L, afl.getMemoryLimit());
        
        // Corpus configuration
        var corpus = fuzzing.getCorpus();
        assertNotNull(corpus);
        assertEquals("/data/corpus", corpus.getBasePath());
        assertEquals(10000L, corpus.getMaxSizeMb());
        assertEquals(100000, corpus.getMaxFiles());
        assertTrue(corpus.isEnableCompression());
    }

    @Test
    @DisplayName("Should load default bot configuration")
    void testDefaultBotConfiguration() {
        var bot = properties.getBot();
        
        assertNotNull(bot);
        assertEquals(1000, bot.getMaxBots());
        assertEquals(Duration.ofMinutes(10), bot.getHeartbeatTimeout());
        assertEquals(Duration.ofHours(6), bot.getTaskTimeout());
        assertEquals(5, bot.getMaxTasksPerBot());
        assertTrue(bot.isEnableAutoScaling());
        assertEquals(5, bot.getMinIdleBots());
        assertEquals(50, bot.getMaxIdleBots());
        
        // Platform configurations
        var platforms = bot.getPlatforms();
        assertNotNull(platforms);
        assertTrue(platforms.containsKey("linux"));
        assertTrue(platforms.containsKey("windows"));
        assertTrue(platforms.containsKey("macos"));
        
        var linuxConfig = platforms.get("linux");
        assertEquals("ubuntu-20.04", linuxConfig.getImage());
        assertEquals(4, linuxConfig.getCpuCores());
        assertEquals(8192, linuxConfig.getMemoryMb());
    }

    @Test
    @DisplayName("Should load default storage configuration")
    void testDefaultStorageConfiguration() {
        var storage = properties.getStorage();
        
        assertNotNull(storage);
        assertEquals("local", storage.getProvider());
        
        // Google Cloud configuration
        var gcs = storage.getGoogleCloud();
        assertNotNull(gcs);
        assertEquals("clusterfuzz-project", gcs.getProjectId());
        assertEquals("clusterfuzz-storage", gcs.getBucketName());
        assertTrue(gcs.isEnableEncryption());
        assertEquals(10, gcs.getUploadThreads());
        
        // Local storage configuration
        var local = storage.getLocal();
        assertNotNull(local);
        assertEquals("/data/storage", local.getBasePath());
        assertEquals(1000L, local.getMaxSizeGb());
        assertTrue(local.isEnableCompression());
    }

    @Test
    @DisplayName("Should load default monitoring configuration")
    void testDefaultMonitoringConfiguration() {
        var monitoring = properties.getMonitoring();
        
        assertNotNull(monitoring);
        
        // Metrics configuration
        var metrics = monitoring.getMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.isEnabled());
        assertEquals(Duration.ofSeconds(30), metrics.getCollectionInterval());
        assertTrue(metrics.isEnableJvmMetrics());
        assertEquals("/actuator/prometheus", metrics.getEndpoint());
        
        // Logging configuration
        var logging = monitoring.getLogging();
        assertNotNull(logging);
        assertEquals("INFO", logging.getLevel());
        assertTrue(logging.isEnableJsonFormat());
        assertTrue(logging.isEnableAsyncLogging());
        assertEquals(1000, logging.getAsyncQueueSize());
        
        // Alerting configuration
        var alerting = monitoring.getAlerting();
        assertNotNull(alerting);
        assertFalse(alerting.isEnabled()); // Disabled by default
        assertEquals(10, alerting.getErrorThreshold());
        assertEquals(Duration.ofMinutes(15), alerting.getAlertInterval());
    }

    @Test
    @DisplayName("Should load default performance configuration")
    void testDefaultPerformanceConfiguration() {
        var performance = properties.getPerformance();
        
        assertNotNull(performance);
        
        // Cache configuration
        var cache = performance.getCache();
        assertNotNull(cache);
        assertTrue(cache.isEnabled());
        assertEquals(1000, cache.getMaxSize());
        assertEquals(Duration.ofHours(1), cache.getExpireAfterWrite());
        assertEquals(Duration.ofMinutes(30), cache.getExpireAfterAccess());
        assertTrue(cache.isEnableStatistics());
        
        // Thread pool configuration
        var threadPool = performance.getThreadPool();
        assertNotNull(threadPool);
        assertEquals(10, threadPool.getCorePoolSize());
        assertEquals(50, threadPool.getMaxPoolSize());
        assertEquals(60, threadPool.getKeepAliveSeconds());
        assertEquals(1000, threadPool.getQueueCapacity());
        assertEquals("clusterfuzz-", threadPool.getThreadNamePrefix());
    }
}