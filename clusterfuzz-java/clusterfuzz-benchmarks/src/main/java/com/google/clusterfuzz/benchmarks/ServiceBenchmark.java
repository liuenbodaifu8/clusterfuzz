package com.google.clusterfuzz.benchmarks;

import com.google.clusterfuzz.core.entity.Bot;
import com.google.clusterfuzz.core.service.BotService;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Performance benchmarks for service layer operations.
 * Tests business logic performance under various loads.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
public class ServiceBenchmark {

    private ConfigurableApplicationContext context;
    private BotService botService;
    
    @Setup(Level.Trial)
    public void setup() {
        // Initialize Spring context with H2 database
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:servicebench;DB_CLOSE_DELAY=-1");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "create-drop");
        System.setProperty("spring.jpa.show-sql", "false");
        
        context = SpringApplication.run(BenchmarkApplication.class);
        botService = context.getBean(BotService.class);
        
        // Seed test data
        seedBotData();
    }
    
    @TearDown(Level.Trial)
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }
    
    private void seedBotData() {
        // Create 100 bots for benchmarking
        for (int i = 0; i < 100; i++) {
            Bot bot = Bot.builder()
                .name("bot_" + i)
                .platform(i % 2 == 0 ? "linux" : "windows")
                .version("1.0." + i)
                .ipAddress("192.168.1." + (i % 254 + 1))
                .port(8080 + i)
                .cpuArchitecture("x86_64")
                .cpuCores(4 + (i % 8))
                .memoryMb(8192 + (i % 4) * 1024)
                .diskSpaceMb(100000 + i * 1000)
                .status(i % 3 == 0 ? Bot.BotStatus.IDLE : Bot.BotStatus.WORKING)
                .lastHeartbeat(LocalDateTime.now().minusMinutes(i % 60))
                .build();
            botService.registerBot(bot);
        }
    }

    @Benchmark
    public void benchmarkRegisterBot(Blackhole bh) {
        Bot bot = Bot.builder()
            .name("benchmark_bot_" + System.nanoTime())
            .platform("linux")
            .version("1.0.0")
            .ipAddress("10.0.0.1")
            .port(8080)
            .cpuArchitecture("x86_64")
            .cpuCores(4)
            .memoryMb(8192)
            .diskSpaceMb(100000)
            .status(Bot.BotStatus.IDLE)
            .lastHeartbeat(LocalDateTime.now())
            .build();
        
        Bot registered = botService.registerBot(bot);
        bh.consume(registered);
    }

    @Benchmark
    public void benchmarkGetAvailableBots(Blackhole bh) {
        List<Bot> bots = botService.getAvailableBots();
        bh.consume(bots);
    }

    @Benchmark
    public void benchmarkGetBotsByPlatform(Blackhole bh) {
        List<Bot> bots = botService.getBotsByPlatform("linux");
        bh.consume(bots);
    }

    @Benchmark
    public void benchmarkUpdateBotHeartbeat(Blackhole bh) {
        // Find first available bot
        List<Bot> bots = botService.getAvailableBots();
        if (!bots.isEmpty()) {
            Bot bot = bots.get(0);
            Bot updated = botService.updateHeartbeat(bot.getName());
            bh.consume(updated);
        }
    }

    @Benchmark
    public void benchmarkCleanupStaleConnections(Blackhole bh) {
        int cleaned = botService.cleanupStaleConnections();
        bh.consume(cleaned);
    }

    @Benchmark
    public void benchmarkGetBotStatistics(Blackhole bh) {
        var stats = botService.getBotStatistics();
        bh.consume(stats);
    }

    @SpringBootApplication
    public static class BenchmarkApplication {
        // Minimal Spring Boot application for benchmarking
    }
}