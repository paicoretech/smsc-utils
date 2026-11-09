package com.paicbd.smsc.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.clients.jedis.UnifiedJedis;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisManagerTest {

    @Mock
    UnifiedJedis unifiedJedis;

    RedisManager redisManager;

    @BeforeEach
    void setUp() {
        redisManager = new RedisManager(unifiedJedis);
    }

    @Test
    void hget_delegatesToUnifiedJedis() {
        when(unifiedJedis.hget("hash", "field")).thenReturn("value");
        assertEquals("value", redisManager.hget("hash", "field"));
        verify(unifiedJedis).hget("hash", "field");
    }

    @Test
    void hgetAll_delegatesToUnifiedJedis() {
        Map<String, String> expected = Map.of("f1", "v1", "f2", "v2");
        when(unifiedJedis.hgetAll("hash")).thenReturn(expected);
        assertEquals(expected, redisManager.hgetAll("hash"));
        verify(unifiedJedis).hgetAll("hash");
    }

    @Test
    void hset_delegatesToUnifiedJedis() {
        when(unifiedJedis.hset("hash", "field", "value")).thenReturn(1L);
        assertEquals(1L, redisManager.hset("hash", "field", "value"));
        verify(unifiedJedis).hset("hash", "field", "value");
    }

    @Test
    void hdel_delegatesToUnifiedJedis() {
        when(unifiedJedis.hdel("hash", "f1", "f2")).thenReturn(2L);
        assertEquals(2L, redisManager.hdel("hash", "f1", "f2"));
        verify(unifiedJedis).hdel("hash", "f1", "f2");
    }

    @Test
    void get_delegatesToUnifiedJedis() {
        when(unifiedJedis.get("key")).thenReturn("value");
        assertEquals("value", redisManager.get("key"));
        verify(unifiedJedis).get("key");
    }

    @Test
    void setex_delegatesToUnifiedJedis() {
        when(unifiedJedis.setex("key", 60L, "value")).thenReturn("OK");
        assertEquals("OK", redisManager.setex("key", 60L, "value"));
        verify(unifiedJedis).setex("key", 60L, "value");
    }

    // UnifiedJedis overloads del(String) and del(String...); RedisManager forwards its varargs
    // array, so the stub has to target the varargs overload explicitly.
    @Test
    void del_delegatesToUnifiedJedis() {
        when(unifiedJedis.del(new String[]{"key"})).thenReturn(1L);
        assertEquals(1L, redisManager.del("key"));
        verify(unifiedJedis).del(new String[]{"key"});
    }

    @Test
    void del_forwardsEveryKey() {
        when(unifiedJedis.del(new String[]{"a", "b"})).thenReturn(2L);
        assertEquals(2L, redisManager.del("a", "b"));
        verify(unifiedJedis).del(new String[]{"a", "b"});
    }

    @Test
    void exists_delegatesToUnifiedJedis() {
        when(unifiedJedis.exists("key")).thenReturn(true);
        assertTrue(redisManager.exists("key"));

        when(unifiedJedis.exists("missing")).thenReturn(false);
        assertFalse(redisManager.exists("missing"));
    }

    @Test
    void close_delegatesToUnifiedJedis() {
        redisManager.close();
        verify(unifiedJedis).close();
    }
}
