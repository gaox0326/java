package com.huaguoshan.redis.limit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.util.Assert;

/**
 * 频率限流器实现
 * 
 * @author gaox
 *
 */
public class RateLimiter {

    /** slf4j logger */
    private Logger logger = LoggerFactory.getLogger(getClass());

    /** 频率限流器配置信息 */
    private RateLimiterConfig config;

    /** redisTemplate */
    private RedisTemplate<String, Object> redisTemplate;

    // 限流验证 lua 脚本，固定窗口算法实现
    // KEYS[1] 限流名称，ARGV[1] 限流容量，ARGV[2] 限流区间，毫秒
    // 通过限流控制返回当前次数，不通过返回 0
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

    // 限流验证 lua 脚本，滑动窗口算法实现
    // KEYS[1] 限流名称，ARGV[1] 限流容量，ARGV[2] 限流区间（毫秒），ARGV[3] 当前时间戳（毫秒），ARGV[4] 唯一标识
    // 通过限流控制返回当前次数，不通过返回 0
    private final static String SLIDING_WINDOW_GET_PERMISSION = "local key = KEYS[1]; "
            + "local limitForPeriod = tonumber(ARGV[1]); "
            + "local limitRefreshPeriod = tonumber(ARGV[2]); "
            + "local now = tonumber(ARGV[3]); "
            + "local expired = now - limitRefreshPeriod; "
            + "redis.call('zremrangebyscore', key, 0, expired); "
            + "local current = tonumber(redis.call('zcard', key)); "
            + "if (current >= limitForPeriod) then "
            + "    return 0; "
            + "else "
            + "    redis.call('zadd', key, now, ARGV[4]); "
            + "    redis.call('pexpire', key, limitRefreshPeriod + 1000000); "
            + "    return current + 1; "
            + "end;";

    private RateLimiter(RateLimiterConfig config, RedisTemplate<String, Object> redisTemplate) {
        this.config = config;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取频率限流器实例
     * @param config 频率限流器配置信息
     * @param redisTemplate redisTemplate
     * @return 频率限流器实例
     */
    public static RateLimiter instance(RateLimiterConfig config, RedisTemplate<String, Object> redisTemplate) {
        Assert.notNull(config, "config 不能为空");
        Assert.notNull(redisTemplate, "redisTemplate 不能为空");
        RateLimiter rateLimiter = new RateLimiter(config, redisTemplate);
        return rateLimiter;
    }

    /**
     * 限流控制判断
     * @return 是否通过限流控制
     */
    public boolean getPermission() {
        List<String> keys = new ArrayList<>();
        keys.add(config.getName());
        try {
            Long result;
            RateLimiterType type = config.getType();
            if (RateLimiterType.SLIDINGWINDOW.equals(type)) {
                RedisScript<Long> luaScript = new DefaultRedisScript<>(SLIDING_WINDOW_GET_PERMISSION, Long.class);
                result = redisTemplate.execute(luaScript, keys, config.getLimitForPeriod(), config.getLimitRefreshPeriod().toMillis(), System.currentTimeMillis(), UUID.randomUUID().toString());
            } else {
                RedisScript<Long> luaScript = new DefaultRedisScript<>(GET_PERMISSION, Long.class);
                result = redisTemplate.execute(luaScript, keys, config.getLimitForPeriod(), config.getLimitRefreshPeriod().toMillis());
            }
            return result != 0;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return false;
    }

}
