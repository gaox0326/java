package com.huaguoshan.redis.limit;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;

/**
 * 频率限流器配置信息
 * <p>
 * 指定期间 {@code limitRefreshPeriod} 内限制总通过次数 {@code limitForPeriod}
 * 
 * @author gaox
 *
 */
@Getter
@Setter
public class RateLimiterConfig {

    // 限流器名称
    private String name;

    // 限流容量
    private int limitForPeriod;

    // 限流刷新期间，毫秒
    private Duration limitRefreshPeriod;

    // 限流算法类型
    private RateLimiterType type = RateLimiterType.FIXWINDOW;

//    private Duration timeoutDuration;

}
