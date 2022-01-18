package com.huaguoshan.redis.limit;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

public class RateLimiter {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private RateLimiterConfig config;

    private RedisTemplate<String, Object> redisTempalte;

    // 获取权限 lua 脚本
    // KEYS[1] 限流名称，ARGV[1] 限流容量，ARGV[2] 限流区间，毫秒
    // 通过限流控制返回当前次数，否则返回 0
    private final static String GET_PERMISSION = "local key = KEYS[1]; "
            + "local limitForPeriod = tonumber(ARGV[1]); "
            + "local limitRefreshPeriod = tonumber(ARGV[2]); "
            + "local current = tonumber(redis.call('get', key) or '0'); "
            + "if (current >= limitForPeriod) then "
            + "    return 0; "
            + "else "
            + "    redis.call('incrby', key, 1); "
            + "    if (current == 0) then "
            + "        redis.call('pexpire', key, limitRefreshPeriod); "
            + "    end "
            + "    return current + 1; "
            + "end;";

    private RateLimiter(RateLimiterConfig config, RedisTemplate<String, Object> redisTempalte) {
        this.config = config;
        this.redisTempalte = redisTempalte;
    }

    public static RateLimiter instance(RateLimiterConfig config, RedisTemplate<String, Object> redisTempalte) {
        Assert.notNull(config, "config 不能为空");
        Assert.notNull(redisTempalte, "redisTempalte 不能为空");
        RateLimiter rateLimiter = new RateLimiter(config, redisTempalte);
        return rateLimiter;
    }

    public boolean getPermission() {
        List<String> keys = new ArrayList<>();
        keys.add(config.getName());
        try {
            RedisScript<Long> luaScript = new DefaultRedisScript<>(GET_PERMISSION, Long.class);
            Long result = redisTempalte.execute(luaScript, keys, config.getLimitForPeriod(), config.getLimitRefreshPeriod().toMillis());
            return result != 0;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return false;
    }

}
