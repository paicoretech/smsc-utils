package com.paicbd.smsc.utils;

import com.paicbd.smsc.kafka.KafkaTopicsConstants;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

import static com.paicbd.smsc.utils.MpsWriteMode.EMIT_TO_KAFKA;
import static com.paicbd.smsc.utils.MpsWriteMode.WRITE_TO_FILE;

@Slf4j
public class RateLimiterUtil {
    private final LongAdder mpsCounter = new LongAdder();
    private final AtomicLong lastAccessTime = new AtomicLong(System.currentTimeMillis());

    private final ScheduledExecutorService scheduler;
    private final String networkId;
    private final int mps;
    private final int mpsWriteMode;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Bucket bucket;

    @Getter
    private final int currentMps;

    public RateLimiterUtil(String networkId, int mps,
                           KafkaTemplate<String, String> kafkaTemplate) {

        this.networkId = networkId;
        this.mps = mps;
        this.kafkaTemplate = kafkaTemplate;
        this.mpsWriteMode = Objects.isNull(kafkaTemplate) ? WRITE_TO_FILE : EMIT_TO_KAFKA;
        this.bucket = mps > 0 ? this.createBucketForRateLimiter(mps) : null;
        ThreadFactory factory = Thread.ofPlatform().name("CounterStatistics-" + this.networkId, 0).factory();
        this.scheduler = Executors.newScheduledThreadPool(1, factory);
        this.scheduler.scheduleAtFixedRate(this::emitTps, 0, 1, TimeUnit.SECONDS);
        this.currentMps = mps;
    }

    public boolean needsRefresh(int newMps) {
        return this.currentMps != newMps;
    }

    public boolean isInactiveFor(long inactivityThreshold) {
        return (System.currentTimeMillis() - lastAccessTime.get()) > inactivityThreshold;
    }

    public void shutdown() {
        this.flushThread();
    }

    public boolean tryConsume() {
        lastAccessTime.set(System.currentTimeMillis());
        try {
            if (this.mps == -1) {
                this.mpsCounter.increment();
                return true;
            }

            if (bucket.tryConsume(1)) {
                this.mpsCounter.increment();
                return true;
            }

            return false;

        } catch (Exception ex) {
            log.error(ex.getMessage());
            return false;
        }
    }

    public void flushThread() {
        this.scheduler.close();
    }

    private Bucket createBucketForRateLimiter(int mps) {
        Refill refill = Refill.intervally(mps, Duration.ofSeconds(1));
        Bandwidth limit = Bandwidth.classic(mps, refill);
        return Bucket.builder().addLimit(limit).build();
    }

    private void emitTps() {
        long currentCount = mpsCounter.sumThenReset();
        if (currentCount > 0) {
            MpsRecord message = new MpsRecord(networkId, System.currentTimeMillis(), currentCount);
            String json = Converter.valueAsString(message);
            switch (this.mpsWriteMode) {
                case WRITE_TO_FILE -> log.debug(json);
                case EMIT_TO_KAFKA -> kafkaTemplate.send(KafkaTopicsConstants.MPS_MESSAGES_COUNTER, json);
                default -> log.debug("Unrecognized MPS Writing mode");
            }
        }
    }

    private record MpsRecord(
            String networkId,
            long timestamp,
            long tps
    ) {
    }
}
