package com.linglevel.api.common.ratelimit.bucket4j;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.redisson.Bucket4jRedisson;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Bucket4j configuration for rate limiting with Redis backend.
 */
@Configuration
public class Bucket4jConfig {

    @Bean
    public ProxyManager<String> proxyManager(RedissonClient redissonClient) {
        Redisson redisson = (Redisson) redissonClient;
        return Bucket4jRedisson.casBasedBuilder(redisson.getCommandExecutor())
                .build();
    }
}
