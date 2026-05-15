package com.paicbd.smsc.utils;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RateLimiterUtilManager {
    private final Map<String, RateLimiterUtil> rateLimiterCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cacheCleaner;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public RateLimiterUtilManager(KafkaTemplate<String, String> kafkaTemplate) {
        ThreadFactory factory = Thread.ofPlatform().name("Cache-Cleaner", 0).factory();
        this.cacheCleaner = Executors.newScheduledThreadPool(1, factory);
        this.kafkaTemplate = kafkaTemplate;
        this.cacheCleaner.scheduleAtFixedRate(this::cleanupUnusedLimiters, 1, 1, TimeUnit.HOURS);
    }

    public boolean tryConsume(String networkId, int mps) {
        RateLimiterUtil rateLimiter = getRateLimiter(networkId, mps);
        return rateLimiter.tryConsume();
    }

    private RateLimiterUtil getRateLimiter(String networkId, int mps) {
        return rateLimiterCache.compute(networkId, (id, existingLimiter) ->
                Optional.ofNullable(existingLimiter)
                        .filter(limiter -> !limiter.needsRefresh(mps))
                        .orElseGet(() -> recreateLimiter(id, existingLimiter, mps)));
    }

    private RateLimiterUtil recreateLimiter(String networkId, RateLimiterUtil existingLimiter, int mps) {
        if (existingLimiter != null) {
            log.info("Recreating rate limiter for networkId: {}, old MPS: {}, new MPS: {}",
                    networkId, existingLimiter.getCurrentMps(), mps);
            existingLimiter.shutdown();
        }
        return new RateLimiterUtil(networkId, mps, this.kafkaTemplate);
    }

    private void cleanupUnusedLimiters() {
        rateLimiterCache.entrySet().removeIf(entry -> {
            boolean shouldRemove = entry.getValue().isInactiveFor(3_600_000L);
            if (shouldRemove) {
                entry.getValue().shutdown();
                log.info("Cleaned up unused rate limiter for networkId: {}", entry.getKey());
            }
            return shouldRemove;
        });
    }

    @PreDestroy
    public void shutdown() {
        cacheCleaner.shutdown();
        rateLimiterCache.values().forEach(RateLimiterUtil::shutdown);
        rateLimiterCache.clear();
        log.info("RateLimiterManager shutdown completed");
    }
}
