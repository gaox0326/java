package com.huaguoshan.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.huaguoshan.redis.limit.RateLimiter;
import com.huaguoshan.redis.limit.RateLimiterConfig;
import com.huaguoshan.redis.lock.ReentrantLock;

@Component
public class HuaGuoShanRedis {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public ReentrantLock getReentrantLock(String lockName) {
        return ReentrantLock.instance(lockName, redisTemplate);
    }

    public RateLimiter getRateLimiter(RateLimiterConfig config) {
        return RateLimiter.instance(config, redisTemplate);
    }

}
