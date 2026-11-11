package com.sqx.common.redis.support;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 无 Redis 时的本地内存缓存（仅覆盖 M00 联调所需 KV 能力）。
 */
@Slf4j
public class LocalMemoryRedisStore implements RedisStore {

    private static final class Entry {
        private final Object value;
        private final long expireAt;

        private Entry(Object value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public LocalMemoryRedisStore() {
        log.warn("sqx.redis.open=false，使用 LocalMemoryRedisStore（仅开发联调，勿用于生产）");
    }

    @Override
    public RedissonClient redissonClient() {
        throw new UnsupportedOperationException("本地内存模式不支持 RedissonClient 直调");
    }

    private Entry getEntry(String key) {
        Entry entry = cache.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.expireAt > 0 && entry.expireAt <= System.currentTimeMillis()) {
            cache.remove(key);
            return null;
        }
        return entry;
    }

    @Override
    public <T> void setCacheObject(String key, T value) {
        cache.put(key, new Entry(value, 0L));
    }

    @Override
    public <T> void setCacheObject(String key, T value, Duration duration) {
        long expireAt = duration == null ? 0L : System.currentTimeMillis() + duration.toMillis();
        cache.put(key, new Entry(value, expireAt));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCacheObject(String key) {
        Entry entry = getEntry(key);
        return entry == null ? null : (T) entry.value;
    }

    @Override
    public boolean hasKey(String key) {
        return getEntry(key) != null;
    }

    @Override
    public boolean deleteObject(String key) {
        return cache.remove(key) != null;
    }

    @Override
    public void deleteObject(Collection<?> keys) {
        if (keys == null) {
            return;
        }
        keys.forEach(key -> cache.remove(String.valueOf(key)));
    }

    @Override
    public void deleteKeys(String pattern) {
        String regex = pattern.replace("*", ".*");
        Pattern compiled = Pattern.compile("^" + regex + "$");
        cache.keySet().removeIf(key -> compiled.matcher(key).matches());
    }
}
