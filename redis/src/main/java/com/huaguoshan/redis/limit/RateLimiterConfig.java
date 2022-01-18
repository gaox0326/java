package com.huaguoshan.redis.limit;

import java.time.Duration;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RateLimiterConfig {

    private String name;

    private int limitForPeriod;

    private Duration limitRefreshPeriod;

//    private Duration timeoutDuration;

}
