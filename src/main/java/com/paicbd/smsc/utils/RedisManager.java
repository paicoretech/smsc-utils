package com.paicbd.smsc.utils;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.UnifiedJedis;

import java.util.Map;

@Slf4j
public class RedisManager {
    private final UnifiedJedis unifiedJedis;

    public RedisManager(UnifiedJedis unifiedJedis) {
        this.unifiedJedis = unifiedJedis;
    }

    public String hget(String key, String field) {
        return unifiedJedis.hget(key, field);
    }

    public Map<String, String> hgetAll(String key) {
        return unifiedJedis.hgetAll(key);
    }

    public long hset(String key, String field, String value) {
        return unifiedJedis.hset(key, field, value);
    }

    public long hdel(String key, String... fields) {
        return unifiedJedis.hdel(key, fields);
    }

    public String get(String key) {
        return unifiedJedis.get(key);
    }

    public String setex(String key, long seconds, String value) {
        return unifiedJedis.setex(key, seconds, value);
    }

    public long del(String... keys) {
        return unifiedJedis.del(keys);
    }

    public boolean exists(String key) {
        return unifiedJedis.exists(key);
    }

    public void close() {
        unifiedJedis.close();
    }
}
