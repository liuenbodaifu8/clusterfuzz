package com.google.clusterfuzz.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive configuration properties for ClusterFuzz.
 * Manages 200+ configuration parameters across all components.
 */
@Data
@Component
@ConfigurationProperties(prefix = "clusterfuzz")
@Validated
public class ClusterFuzzConfigurationProperties {

    @Valid
    private Database database = new Database();

    @Valid
    private Security security = new Security();

    @Valid
    private Fuzzing fuzzing = new Fuzzing();

    @Valid
    private Bot bot = new Bot();

    @Valid
    private Storage storage = new Storage();

    @Valid
    private Monitoring monitoring = new Monitoring();

    @Valid
    private Integration integration = new Integration();

    @Valid
    private Performance performance = new Performance();

    @Data
    public static class Database {
        @NotBlank
        private String url = "jdbc:postgresql://localhost:5432/clusterfuzz";
        
        @NotBlank
        private String username = "clusterfuzz";
        
        @NotBlank
        private String password = "password";
        
        @Min(1)
        @Max(100)
        private int maxPoolSize = 20;
        
        @Min(0)
        @Max(50)
        private int minIdleSize = 5;
        
        @NotNull
        private Duration connectionTimeout = Duration.ofSeconds(30);
        
        @NotNull
        private Duration idleTimeout = Duration.ofMinutes(10);
        
        @NotNull
        private Duration maxLifetime = Duration.ofMinutes(30);
        
        private boolean enableQueryLogging = false;
        private boolean enableSlowQueryLogging = true;
        
        @Min(100)
        private long slowQueryThresholdMs = 1000;
        
        @Min(10)
        @Max(1000)
        private int batchSize = 50;
        
        private boolean enableSecondLevelCache = true;
        private boolean enableQueryCache = true;
        private String cacheRegionPrefix = "clusterfuzz";
    }

    @Data
    public static class Security {
        @Valid
        private Jwt jwt = new Jwt();
        
        @Valid
        private OAuth2 oauth2 = new OAuth2();
        
        @Valid
        private Cors cors = new Cors();
        
        private boolean enableCsrf = true;
        private boolean enableHttps = true;
        private boolean enableHsts = true;
        
        @Min(300)
        @Max(31536000)
        private long hstsMaxAge = 31536000; // 1 year
        
        private boolean enableContentSecurityPolicy = true;
        private String contentSecurityPolicy = "default-src 'self'; script-src 'self' 'unsafe-inline'";
        
        @Data
        public static class Jwt {
            @NotBlank
            private String secretKey = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
            
            @NotNull
            private Duration expiration = Duration.ofHours(24);
            
            @NotNull
            private Duration refreshExpiration = Duration.ofDays(7);
            
            @NotBlank
            private String issuer = "clusterfuzz";
            
            private boolean enableRefreshTokenRotation = true;
            private boolean enableTokenBlacklist = true;
        }
        
        @Data
        public static class OAuth2 {
            @Valid
            private Google google = new Google();
            
            @Valid
            private GitHub github = new GitHub();
            
            @Data
            public static class Google {
                private boolean enabled = false;
                private String clientId;
                private String clientSecret;
                private List<String> scope = List.of("openid", "profile", "email");
                private String redirectUri = "/oauth2/callback/google";
            }
            
            @Data
            public static class GitHub {
                private boolean enabled = false;
                private String clientId;
                private String clientSecret;
                private List<String> scope = List.of("user:email");
                private String redirectUri = "/oauth2/callback/github";
            }
        }
        
        @Data
        public static class Cors {
            private boolean enabled = true;
            private List<String> allowedOrigins = List.of("http://localhost:3000", "http://localhost:8080");
            private List<String> allowedMethods = List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");
            private List<String> allowedHeaders = List.of("*");
            private boolean allowCredentials = true;
            
            @Min(0)
            @Max(86400)
            private long maxAge = 3600;
        }
    }

    @Data
    public static class Fuzzing {
        @Valid
        private LibFuzzer libFuzzer = new LibFuzzer();
        
        @Valid
        private Afl afl = new Afl();
        
        @Valid
        private Corpus corpus = new Corpus();
        
        @Min(1)
        @Max(1000)
        private int maxConcurrentJobs = 100;
        
        @NotNull
        private Duration jobTimeout = Duration.ofHours(24);
        
        @NotNull
        private Duration heartbeatInterval = Duration.ofMinutes(5);
        
        @Min(1)
        @Max(100)
        private int maxRetries = 3;
        
        private boolean enableCrashDeduplication = true;
        private boolean enableMinimization = true;
        private boolean enableReproduction = true;
        
        @Data
        public static class LibFuzzer {
            private boolean enabled = true;
            
            @Min(1)
            @Max(3600)
            private int maxTotalTime = 3600;
            
            @Min(1)
            @Max(1000000)
            private int maxLen = 10000;
            
            @Min(1)
            @Max(100)
            private int workers = 8;
            
            private List<String> extraFlags = List.of("-print_final_stats=1");
            private String artifactPrefix = "/tmp/libfuzzer-";
        }
        
        @Data
        public static class Afl {
            private boolean enabled = true;
            
            @NotNull
            private Duration timeout = Duration.ofMinutes(60);
            
            @Min(1)
            @Max(1000000)
            private long memoryLimit = 200; // MB
            
            private boolean enableDeterministicMode = false;
            private boolean enableDictionaryMode = true;
            private String extraOptions = "-x /usr/share/afl/dictionaries/";
        }
        
        @Data
        public static class Corpus {
            @NotBlank
            private String basePath = "/data/corpus";
            
            @Min(1)
            @Max(1000000)
            private long maxSizeMb = 10000;
            
            @Min(1)
            @Max(1000000)
            private int maxFiles = 100000;
            
            @NotNull
            private Duration cleanupInterval = Duration.ofHours(24);
            
            private boolean enableCompression = true;
            private boolean enableDeduplication = true;
        }
    }

    @Data
    public static class Bot {
        @Min(1)
        @Max(10000)
        private int maxBots = 1000;
        
        @NotNull
        private Duration heartbeatTimeout = Duration.ofMinutes(10);
        
        @NotNull
        private Duration taskTimeout = Duration.ofHours(6);
        
        @Min(1)
        @Max(100)
        private int maxTasksPerBot = 5;
        
        private boolean enableAutoScaling = true;
        
        @Min(1)
        @Max(100)
        private int minIdleBots = 5;
        
        @Min(1)
        @Max(1000)
        private int maxIdleBots = 50;
        
        @Valid
        private Map<String, PlatformConfig> platforms = Map.of(
            "linux", new PlatformConfig("ubuntu-20.04", 4, 8192),
            "windows", new PlatformConfig("windows-2019", 4, 8192),
            "macos", new PlatformConfig("macos-11", 4, 8192)
        );
        
        @Data
        public static class PlatformConfig {
            @NotBlank
            private String image;
            
            @Min(1)
            @Max(64)
            private int cpuCores;
            
            @Min(1024)
            @Max(131072)
            private int memoryMb;
            
            public PlatformConfig() {}
            
            public PlatformConfig(String image, int cpuCores, int memoryMb) {
                this.image = image;
                this.cpuCores = cpuCores;
                this.memoryMb = memoryMb;
            }
        }
    }

    @Data
    public static class Storage {
        @Valid
        private GoogleCloud googleCloud = new GoogleCloud();
        
        @Valid
        private Local local = new Local();
        
        @NotBlank
        private String provider = "local"; // local, gcs
        
        @Data
        public static class GoogleCloud {
            @NotBlank
            private String projectId = "clusterfuzz-project";
            
            @NotBlank
            private String bucketName = "clusterfuzz-storage";
            
            @NotBlank
            private String credentialsPath = "/etc/gcp/credentials.json";
            
            private boolean enableEncryption = true;
            private String kmsKeyName;
            
            @Min(1)
            @Max(100)
            private int uploadThreads = 10;
            
            @Min(1)
            @Max(100)
            private int downloadThreads = 10;
        }
        
        @Data
        public static class Local {
            @NotBlank
            private String basePath = "/data/storage";
            
            @Min(1)
            @Max(1000000)
            private long maxSizeGb = 1000;
            
            private boolean enableCompression = true;
            private boolean enableBackup = false;
            private String backupPath = "/backup/storage";
        }
    }

    @Data
    public static class Monitoring {
        @Valid
        private Metrics metrics = new Metrics();
        
        @Valid
        private Logging logging = new Logging();
        
        @Valid
        private Alerting alerting = new Alerting();
        
        @Data
        public static class Metrics {
            private boolean enabled = true;
            
            @NotNull
            private Duration collectionInterval = Duration.ofSeconds(30);
            
            private boolean enableJvmMetrics = true;
            private boolean enableSystemMetrics = true;
            private boolean enableCustomMetrics = true;
            
            @NotBlank
            private String endpoint = "/actuator/prometheus";
        }
        
        @Data
        public static class Logging {
            @NotBlank
            private String level = "INFO";
            
            private boolean enableJsonFormat = true;
            private boolean enableAsyncLogging = true;
            
            @Min(1)
            @Max(10000)
            private int asyncQueueSize = 1000;
            
            @NotBlank
            private String pattern = "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n";
        }
        
        @Data
        public static class Alerting {
            private boolean enabled = false;
            private String webhookUrl;
            private String slackChannel;
            private String emailRecipients;
            
            @Min(1)
            @Max(3600)
            private int errorThreshold = 10;
            
            @NotNull
            private Duration alertInterval = Duration.ofMinutes(15);
        }
    }

    @Data
    public static class Integration {
        @Valid
        private Jira jira = new Jira();
        
        @Valid
        private Bugzilla bugzilla = new Bugzilla();
        
        @Valid
        private Webhook webhook = new Webhook();
        
        @Data
        public static class Jira {
            private boolean enabled = false;
            private String url;
            private String username;
            private String apiToken;
            private String projectKey;
            private String issueType = "Bug";
        }
        
        @Data
        public static class Bugzilla {
            private boolean enabled = false;
            private String url;
            private String username;
            private String password;
            private String product;
            private String component;
        }
        
        @Data
        public static class Webhook {
            private boolean enabled = false;
            private List<String> urls = List.of();
            
            @NotNull
            private Duration timeout = Duration.ofSeconds(30);
            
            @Min(0)
            @Max(10)
            private int retries = 3;
        }
    }

    @Data
    public static class Performance {
        @Valid
        private Cache cache = new Cache();
        
        @Valid
        private ThreadPool threadPool = new ThreadPool();
        
        @Data
        public static class Cache {
            private boolean enabled = true;
            
            @Min(1)
            @Max(10000)
            private int maxSize = 1000;
            
            @NotNull
            private Duration expireAfterWrite = Duration.ofHours(1);
            
            @NotNull
            private Duration expireAfterAccess = Duration.ofMinutes(30);
            
            private boolean enableStatistics = true;
        }
        
        @Data
        public static class ThreadPool {
            @Min(1)
            @Max(1000)
            private int corePoolSize = 10;
            
            @Min(1)
            @Max(1000)
            private int maxPoolSize = 50;
            
            @Min(0)
            @Max(3600)
            private int keepAliveSeconds = 60;
            
            @Min(1)
            @Max(10000)
            private int queueCapacity = 1000;
            
            @NotBlank
            private String threadNamePrefix = "clusterfuzz-";
        }
    }
}