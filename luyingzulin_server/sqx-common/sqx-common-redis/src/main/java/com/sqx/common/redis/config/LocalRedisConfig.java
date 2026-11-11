package com.sqx.common.redis.config;

import com.sqx.common.redis.config.properties.RedissonProperties;
import com.sqx.common.redis.support.LocalMemoryRedisStore;
import com.sqx.common.redis.support.RedisStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * sqx.redis.open=false 时使用本地内存缓存，便于无 Redis 环境联调。
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RedissonProperties.class)
@ConditionalOnProperty(prefix = "sqx.redis", name = "open", havingValue = "false")
public class LocalRedisConfig {

    @Bean
    public RedisStore redisStore() {
        log.info("sqx.redis.open=false，启用 LocalMemoryRedisStore");
        return new LocalMemoryRedisStore();
    }
}
