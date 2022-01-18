package com.huaguoshan.redis.limit;

/**
 * 频率限流器，限流算法类型
 * 
 * @author gaox
 *
 */
public enum RateLimiterType {

    /** 固定窗口 */
    FIXWINDOW,

    /** 滑动窗口 */
    SLIDINGWINDOW

}
