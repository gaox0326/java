package com.huaguoshan.redis;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import com.huaguoshan.redis.lock.ReentrantLock;

@SpringBootTest
class SpringDataRedisApplicationTests {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private HuaGuoShanRedis hgsReis;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static ExecutorService executorService = Executors.newFixedThreadPool(300);

    //@Test
    void test1() {
        String key = "test1";
        Boolean hasKey = redisTemplate.hasKey(key);
        logger.info("hasKey [" + key + "]: " + hasKey);
    }

//    @Test
    void testReentrantLock() {
        int count = 10;
        CountDownLatch countDownLatch = new CountDownLatch(count);
        String lockName = "ReentrantLock";
        for (int index = 0; index < count; index++) {
            int temp = index;
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    testReentrantLock(lockName, temp % 2 == 0);
                    countDownLatch.countDown();
                }
            });
        }
        try {
            countDownLatch.await(); // 等待所有线程执行完，才退出
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void testReentrantLock(String lockName, boolean tryLock) {
        String id = String.valueOf(Thread.currentThread().getId());
        logger.info(id + "准备获取 ReentrantLock " + (tryLock ? "tryLock" : "lock") + "，lockName = " + lockName);
        ReentrantLock lock = hgsReis.getReentrantLock(lockName);
        boolean lockSuccess = false;
        try {
            if (tryLock) {
                lockSuccess = lock.tryLock();
                if (!lockSuccess) {
                    logger.info(id + "获取 ReentrantLock tryLock 失败，lockName = " + lockName);
                    return;
                }
            } else {
                lock.lock();
                lockSuccess = true;
                logger.info(id + "获取 ReentrantLock 成功，lockName = " + lockName);
            }
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (lockSuccess) {
                lock.unlock();
                logger.info(id + "释放 ReentrantLock 成功，lockName = " + lockName);
            }
        }
    }

//    @Test
    void testReentrant() {
        String lockName = "ReentrantLock";
        String id = String.valueOf(Thread.currentThread().getId());
        ReentrantLock lock = hgsReis.getReentrantLock(lockName);
        logger.info(id + "准备获取 ReentrantLock，lockName = " + lockName);
        lock.lock();
        logger.info(id + "获取 ReentrantLock 成功，lockName = " + lockName);
        lock.lock();
        logger.info(id + "获取 ReentrantLock 成功，lockName = " + lockName);
        lock.unlock();
        logger.info(id + "释放 ReentrantLock 成功，lockName = " + lockName);
        lock.unlock();
        logger.info(id + "释放 ReentrantLock 成功，lockName = " + lockName);
    }

    @Test
    void testUnlock() {
        String lockName = "ReentrantLock";
        ReentrantLock lock = hgsReis.getReentrantLock(lockName);
        lock.unlock();
    }

}
