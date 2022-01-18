package com.huaguoshan.redis.lock;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * 基于 redis 实现的可重入分布式锁
 * 
 * @author gaox
 */
public class ReentrantLock implements Lock {

    private Logger logger = LoggerFactory.getLogger(getClass());

    // 锁名称
    private String lockName;

    private RedisTemplate<String, Object> redisTempalte;

    // 获取锁默认过期时间
    private final static long DEFAULT_EXPIRE = 30 * 1000L;

    // 获取锁 lua 脚本
    // KEYS[1] 锁名称，ARGV[1] 锁过期时间（秒），ARGV[2] 获取锁客户端标识
    // return nil if lock success, expire seconds otherwise
    private final static String LOCK = "if (redis.call('exists', KEYS[1]) == 0) then "
            + "    redis.call('hset', KEYS[1], ARGV[2], 1); "
            + "    redis.call('pexpire', KEYS[1], ARGV[1]); "
            + "    return nil; "
            + "end; "
            + "if (redis.call('hexists', KEYS[1], ARGV[2]) == 1) then "
            + "    redis.call('hincrby', KEYS[1], ARGV[2], 1); "
            + "    redis.call('pexpire', KEYS[1], ARGV[1]); "
            + "    return nil; "
            + "end; "
            + "return redis.call('pttl', KEYS[1]);";

    // 解锁 lua 脚本
    // KEYS[1] 锁名称，ARGV[1] 获取锁客户端标识
    // return 1 if unlock success, 0 otherwise
    private final static String UNLOCK = "if (redis.call('exists', KEYS[1]) == 0) then "
            + "    return 1; "
            + "end; "
            + "if (redis.call('hexists', KEYS[1], ARGV[1]) == 1) then "
            + "    local count = redis.call('hincrby', KEYS[1], ARGV[1], -1); "
            + "    if (count == 0) then "
            + "          redis.call('del', KEYS[1]); "
            + "    end; "
            + "    return 1; "
            + "end; "
            + "return 0;";

    private ReentrantLock(String lockName, RedisTemplate<String, Object> redisTemplate) {
        this.lockName = lockName;
        this.redisTempalte = redisTemplate;
    }

    /**
     * 获取可重入锁实例
     * @param lockName 锁名称
     * @param redisTemplate redisTemplate
     * @return
     */
    public static ReentrantLock instance(String lockName, RedisTemplate<String, Object> redisTemplate) {
        ReentrantLock lock = new ReentrantLock(lockName, redisTemplate);
        return lock;
    }

    @Override
    public void lock() {
        while (!tryAcquire()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                ex.printStackTrace(); // TODO
            }
        }
    }

    private boolean tryAcquire() {
        List<String> keys = new ArrayList<>();
        keys.add(lockName);
        Long expireMilli = DEFAULT_EXPIRE;
        String clientId = getClientId();
        try {
            RedisScript<Long> luaScript = new DefaultRedisScript<>(LOCK, Long.class);
            Long result = redisTempalte.execute(luaScript, keys, expireMilli, clientId);
            return result == null;
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return false;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean tryLock() {
        return tryAcquire();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void unlock() {
        List<String> keys = new ArrayList<>();
        keys.add(lockName);
        String clientId = getClientId();
        Long result;
        try {
            RedisScript<Long> luaScript = new DefaultRedisScript<>(UNLOCK, Long.class);
            result = redisTempalte.execute(luaScript, keys, clientId);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            throw ex;
        }
        if (result == 0) {
            throw new IllegalMonitorStateException();
        }
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    private String getClientId() {
        return String.valueOf(Thread.currentThread().getId());
    }

}
