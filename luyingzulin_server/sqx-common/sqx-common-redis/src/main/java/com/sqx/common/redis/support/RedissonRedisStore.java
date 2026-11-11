package com.sqx.common.redis.support;

import org.redisson.api.RBatch;
import org.redisson.api.RBucket;
import org.redisson.api.RBucketAsync;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于 Redisson 的 Redis 存储实现。
 */
public class RedissonRedisStore implements RedisStore {

    private final RedissonClient client;

    public RedissonRedisStore(RedissonClient client) {
        this.client = client;
    }

    @Override
    public RedissonClient redissonClient() {
        return client;
    }

    @Override
    public <T> void setCacheObject(String key, T value) {
        client.getBucket(key).set(value);
    }

    @Override
    public <T> void setCacheObject(String key, T value, Duration duration) {
        RBatch batch = client.createBatch();
        RBucketAsync<T> bucket = batch.getBucket(key);
        bucket.setAsync(value);
        bucket.expireAsync(duration);
        batch.execute();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCacheObject(String key) {
        RBucket<T> bucket = client.getBucket(key);
        return bucket.get();
    }

    @Override
    public boolean hasKey(String key) {
        return client.getKeys().countExists(key) > 0;
    }

    @Override
    public boolean deleteObject(String key) {
        return client.getBucket(key).delete();
    }

    @Override
    public void deleteObject(Collection<?> keys) {
        RBatch batch = client.createBatch();
        keys.forEach(key -> batch.getBucket(String.valueOf(key)).deleteAsync());
        batch.execute();
    }

    @Override
    public void deleteKeys(String pattern) {
        client.getKeys().deleteByPattern(pattern);
    }

    public Collection<String> keys(String pattern) {
        Stream<String> stream = client.getKeys().getKeysStreamByPattern(pattern);
        return stream.collect(Collectors.toList());
    }
}
