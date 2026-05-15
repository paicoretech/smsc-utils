package com.paicbd.smsc.utils;

import com.paicbd.smsc.kafka.KafkaTopicsConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterUtilTest {
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    private static final String NETWORK_ID = "5";


    @Test
    @DisplayName("tryConsume when mps is -1 then always return true")
    void tryConsumeWhenMpsIsUnlimitedThenAlwaysTrue() {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, -1, null);
        assertTrue(rateLimiter.tryConsume());
        assertTrue(rateLimiter.tryConsume());
        rateLimiter.flushThread();
    }

    @Test
    @DisplayName("tryConsume when tokens are available then return true")
    void tryConsumeWhenTokensAvailableThenReturnTrue() {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 5, null);
        assertTrue(rateLimiter.tryConsume());
        rateLimiter.flushThread();
    }

    @Test
    @DisplayName("tryConsume when tokens exhausted then return false until refill")
    void tryConsumeWhenTokensExhaustedThenFalseUntilRefill() {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 1, null);

        assertTrue(rateLimiter.tryConsume());
        assertFalse(rateLimiter.tryConsume());

        await()
                .atMost(Duration.ofSeconds(2))
                .until(rateLimiter::tryConsume);

        rateLimiter.flushThread();
    }

    @Test
    @DisplayName("emitTps when mode is EMIT_TO_KAFKA then send message via kafkaTemplate")
    void emitTpsWhenEmitToKafkaThenSendToKafka() throws Exception {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 5, kafkaTemplate);

        rateLimiter.tryConsume();

        var method = RateLimiterUtil.class.getDeclaredMethod("emitTps");
        method.setAccessible(true);
        method.invoke(rateLimiter);

        verify(kafkaTemplate, atLeastOnce()).send(eq(KafkaTopicsConstants.MPS_MESSAGES_COUNTER), anyString());

        rateLimiter.flushThread();
    }

    @Test
    @DisplayName("tryConsume when bucket throws exception then catch block returns false")
    void tryConsumeWhenBucketThrowsExceptionThenReturnFalse() throws Exception {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 5, null);

        var bucketMock = mock(io.github.bucket4j.Bucket.class);
        when(bucketMock.tryConsume(anyLong())).thenThrow(new RuntimeException("Bucket failure"));

        var field = RateLimiterUtil.class.getDeclaredField("bucket");
        field.setAccessible(true);
        field.set(rateLimiter, bucketMock);

        assertFalse(rateLimiter.tryConsume());

        rateLimiter.flushThread();
    }

    @Test
    @DisplayName("emitTps when mode is unknown then log unrecognized mode (default branch)")
    void emitTpsWhenModeUnknownThenDefaultExecuted() throws Exception {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 5, null);

        var method = RateLimiterUtil.class.getDeclaredMethod("emitTps");
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(rateLimiter));

        rateLimiter.flushThread();
    }

    @Test
    @DisplayName("flushThread when called then shutdown scheduler safely")
    void flushThreadWhenCalledThenSchedulerClosed() {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 5, null);
        assertDoesNotThrow(rateLimiter::flushThread);
    }

    @Test
    @DisplayName("needsRefresh should return true when newMps is different")
    void needsRefreshWhenDifferentMpsThenTrue() {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 5, null);
        assertTrue(rateLimiter.needsRefresh(10));
        rateLimiter.flushThread();
    }

    @Test
    @DisplayName("needsRefresh should return false when newMps is equal")
    void needsRefreshWhenSameMpsThenFalse() {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 5, null);
        assertFalse(rateLimiter.needsRefresh(5));
        rateLimiter.flushThread();
    }

    @Test
    @DisplayName("isInactiveFor should return true when lastAccessTime is older than threshold")
    void isInactiveForShouldReturnTrueWhenExceedsThreshold() throws Exception {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 5, null);

        var lastAccessField = RateLimiterUtil.class.getDeclaredField("lastAccessTime");
        lastAccessField.setAccessible(true);
        var atomicLong = (java.util.concurrent.atomic.AtomicLong) lastAccessField.get(rateLimiter);
        atomicLong.set(System.currentTimeMillis() - 5000);

        assertTrue(rateLimiter.isInactiveFor(1000));
        rateLimiter.flushThread();
    }

    @Test
    @DisplayName("isInactiveFor should return false when recent activity within threshold")
    void isInactiveForShouldReturnFalseWhenWithinThreshold() throws Exception {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 5, null);

        var lastAccessField = RateLimiterUtil.class.getDeclaredField("lastAccessTime");
        lastAccessField.setAccessible(true);
        var atomicLong = (java.util.concurrent.atomic.AtomicLong) lastAccessField.get(rateLimiter);
        atomicLong.set(System.currentTimeMillis());

        assertFalse(rateLimiter.isInactiveFor(5000));
        rateLimiter.flushThread();
    }

    @Test
    @DisplayName("shutdown should invoke flushThread safely without exceptions")
    void shutdownShouldInvokeFlushThread() {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 5, null);
        assertDoesNotThrow(rateLimiter::shutdown);
    }

    @Test
    @DisplayName("emitTps default branch executes when mpsWriteMode is unknown")
    void emitTpsDefaultBranchForceExecution() throws Exception {
        RateLimiterUtil rateLimiter = new RateLimiterUtil(NETWORK_ID, 5, null);

        var counterField = RateLimiterUtil.class.getDeclaredField("mpsCounter");
        counterField.setAccessible(true);
        var counter = (java.util.concurrent.atomic.LongAdder) counterField.get(rateLimiter);
        counter.add(5L);

        var modeField = RateLimiterUtil.class.getDeclaredField("mpsWriteMode");
        modeField.setAccessible(true);
        modeField.set(rateLimiter, 999);

        var method = RateLimiterUtil.class.getDeclaredMethod("emitTps");
        method.setAccessible(true);

        assertDoesNotThrow(() -> method.invoke(rateLimiter));
        rateLimiter.flushThread();
    }
}
