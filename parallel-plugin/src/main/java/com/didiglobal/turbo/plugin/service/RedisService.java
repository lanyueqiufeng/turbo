package com.didiglobal.turbo.plugin.service;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
public class RedisService {

    @Resource
    private RedissonClient redissonClient;

    // redis key 过期时间：40 分钟（秒）
    public static final int REAL_EXPIRE_TIME = 40 * 60;
    public static final String PREFIX = "turboPlugin:";

    /**
     * 先存 key-value，再设置过期时间
     */
    public void save(String key, String value) {
        String redisKey = PREFIX + key;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        bucket.set(value);
        bucket.expire(REAL_EXPIRE_TIME, TimeUnit.SECONDS);
    }
    /**
     * 获取 key 对应的 value
     */
    public String get(String key) {
        String redisKey = PREFIX + key;
        RBucket<String> bucket = redissonClient.getBucket(redisKey);
        return bucket.get();
    }

}
