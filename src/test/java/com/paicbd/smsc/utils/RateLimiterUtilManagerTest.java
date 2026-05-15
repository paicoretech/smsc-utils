package com.paicbd.smsc.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterUtilManagerTest {
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    private static final String NETWORK_ID = "1";

    @Test
    @DisplayName("tryConsume when rate limiter exists and allows consumption then return true")
    void tryConsumeWhenLimiterExistsThenReturnTrue() {
        RateLimiterUtilManager manager = new RateLimiterUtilManager(kafkaTemplate);
        RateLimiterUtil limiterMock = mock(RateLimiterUtil.class);
        when(limiterMock.tryConsume()).thenReturn(true);

        setLimiter(manager, limiterMock);

        boolean result = manager.tryConsume(NETWORK_ID, 10);

        assertTrue(result);
        verify(limiterMock).tryConsume();
        manager.shutdown();
    }

    @Test
    @DisplayName("tryConsume when new limiter must be created then returns limiter result")
    void tryConsumeWhenNewLimiterCreatedThenReturnResult() {
        RateLimiterUtilManager manager = new RateLimiterUtilManager(kafkaTemplate);

        boolean result = manager.tryConsume(NETWORK_ID, 5);
        assertTrue(result);

        manager.shutdown();
    }

    @Test
    @DisplayName("recreateLimiter should shutdown old limiter and create new one with new MPS")
    void recreateLimiterShouldShutdownOldAndCreateNew() throws Exception {
        RateLimiterUtilManager manager = new RateLimiterUtilManager(kafkaTemplate);

        RateLimiterUtil oldLimiter = mock(RateLimiterUtil.class);
        when(oldLimiter.getCurrentMps()).thenReturn(5);

        Method recreate = RateLimiterUtilManager.class.getDeclaredMethod(
                "recreateLimiter", String.class, RateLimiterUtil.class, int.class);
        recreate.setAccessible(true);

        RateLimiterUtil newLimiter = (RateLimiterUtil) recreate.invoke(manager, NETWORK_ID, oldLimiter, 10);

        assertNotNull(newLimiter);
        verify(oldLimiter, atLeastOnce()).shutdown();

        manager.shutdown();
    }

    @Test
    @DisplayName("cleanupUnusedLimiters should remove inactive limiters and call shutdown")
    void cleanupUnusedLimitersShouldRemoveInactive() throws Exception {
        RateLimiterUtilManager manager = new RateLimiterUtilManager(kafkaTemplate);

        RateLimiterUtil inactiveLimiter = mock(RateLimiterUtil.class);
        when(inactiveLimiter.isInactiveFor(anyLong())).thenReturn(true);

        setLimiter(manager, inactiveLimiter);

        Method cleanup = RateLimiterUtilManager.class.getDeclaredMethod("cleanupUnusedLimiters");
        cleanup.setAccessible(true);
        cleanup.invoke(manager);

        Map<String, RateLimiterUtil> cache = getLimiterCache(manager);
        assertFalse(cache.containsKey(NETWORK_ID));
        verify(inactiveLimiter).shutdown();

        manager.shutdown();
    }

    @Test
    @DisplayName("cleanupUnusedLimiters should keep active limiters")
    void cleanupUnusedLimitersShouldKeepActive() throws Exception {
        RateLimiterUtilManager manager = new RateLimiterUtilManager(kafkaTemplate);

        RateLimiterUtil activeLimiter = mock(RateLimiterUtil.class);
        when(activeLimiter.isInactiveFor(anyLong())).thenReturn(false);

        setLimiter(manager, activeLimiter);

        Method cleanup = RateLimiterUtilManager.class.getDeclaredMethod("cleanupUnusedLimiters");
        cleanup.setAccessible(true);
        cleanup.invoke(manager);

        Map<String, RateLimiterUtil> cache = getLimiterCache(manager);
        assertTrue(cache.containsKey(NETWORK_ID));
        verify(activeLimiter, never()).shutdown();

        manager.shutdown();
    }

    @Test
    @DisplayName("shutdown should stop cleaner thread and clear all limiters")
    void shutdownShouldStopAndClearAll() {
        RateLimiterUtilManager manager = new RateLimiterUtilManager(kafkaTemplate);

        RateLimiterUtil limiterMock = mock(RateLimiterUtil.class);
        setLimiter(manager, limiterMock);

        manager.shutdown();

        Map<String, RateLimiterUtil> cache = getLimiterCache(manager);
        assertTrue(cache.isEmpty());
        verify(limiterMock).shutdown();
    }

    private void setLimiter(RateLimiterUtilManager manager, RateLimiterUtil limiter) {
        Map<String, RateLimiterUtil> cache = getLimiterCache(manager);
        cache.put(RateLimiterUtilManagerTest.NETWORK_ID, limiter);
    }

    @SuppressWarnings("unchecked")
    private Map<String, RateLimiterUtil> getLimiterCache(RateLimiterUtilManager manager) {
        try {
            var field = RateLimiterUtilManager.class.getDeclaredField("rateLimiterCache");
            field.setAccessible(true);
            return (ConcurrentHashMap<String, RateLimiterUtil>) field.get(manager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("getRateLimiter should reuse existing limiter when needsRefresh is false")
    void getRateLimiterShouldReuseExistingLimiterWhenNoRefreshNeeded() {
        RateLimiterUtilManager manager = new RateLimiterUtilManager(kafkaTemplate);

        RateLimiterUtil existingLimiter = mock(RateLimiterUtil.class);
        when(existingLimiter.needsRefresh(10)).thenReturn(false);
        when(existingLimiter.tryConsume()).thenReturn(true);

        setLimiter(manager, existingLimiter);

        boolean result = manager.tryConsume(NETWORK_ID, 10);

        assertTrue(result);
        verify(existingLimiter).tryConsume();

        Map<String, RateLimiterUtil> cache = getLimiterCache(manager);
        assertSame(existingLimiter, cache.get(NETWORK_ID));

        manager.shutdown();
    }

    @Test
    @DisplayName("getRateLimiter should recreate limiter when needsRefresh is true")
    void getRateLimiterShouldRecreateLimiterWhenNeedsRefreshTrue() {
        RateLimiterUtilManager manager = new RateLimiterUtilManager(kafkaTemplate);

        RateLimiterUtil oldLimiter = mock(RateLimiterUtil.class);
        when(oldLimiter.needsRefresh(10)).thenReturn(true);
        when(oldLimiter.getCurrentMps()).thenReturn(5);
        doNothing().when(oldLimiter).shutdown();

        setLimiter(manager, oldLimiter);

        boolean result = manager.tryConsume(NETWORK_ID, 10);

        assertTrue(result);

        Map<String, RateLimiterUtil> cache = getLimiterCache(manager);
        RateLimiterUtil newLimiter = cache.get(NETWORK_ID);
        assertNotSame(oldLimiter, newLimiter);

        verify(oldLimiter).shutdown();

        manager.shutdown();
    }
}
