package com.sqx.common.redis.support;

import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Collection;

/**
 * Redis 存储抽象：sqx.redis.open=true 走 Redisson，false 走本地内存（开发联调无 Redis 场景）。
 */
public interface RedisStore {

    RedissonClient redissonClient();

    <T> void setCacheObject(String key, T value);

    <T> void setCacheObject(String key, T value, Duration duration);

    <T> T getCacheObject(String key);

    boolean hasKey(String key);

    boolean deleteObject(String key);

    void deleteObject(Collection<?> keys);

    void deleteKeys(String pattern);
}
