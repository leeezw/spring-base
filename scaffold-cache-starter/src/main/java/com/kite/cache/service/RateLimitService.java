package com.kite.cache.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 限流服务
 */
@Service
@RequiredArgsConstructor
public class RateLimitService {
    
    private final StringRedisTemplate redisTemplate;
    
    /**
     * 滑动窗口限流
     */
    public boolean slidingWindowLimit(String key, int count, int period, TimeUnit timeUnit) {
        long now = System.currentTimeMillis();
        long windowStart = now - timeUnit.toMillis(period);
        
        // 移除窗口外的记录
        redisTemplate.opsForZSet().removeRangeByScore(key, 0, windowStart);
        
        // 获取当前窗口内的请求数
        Long currentCount = redisTemplate.opsForZSet().zCard(key);
        
        if (currentCount != null && currentCount >= count) {
            return false;
        }
        
        // 添加当前请求
        redisTemplate.opsForZSet().add(key, String.valueOf(now), now);
        redisTemplate.expire(key, period, timeUnit);
        
        return true;
    }
    
    /**
     * 固定窗口限流
     */
    public boolean fixedWindowLimit(String key, int count, int period, TimeUnit timeUnit) {
        Long currentCount = redisTemplate.opsForValue().increment(key);
        
        if (currentCount == null) {
            return false;
        }
        
        if (currentCount == 1) {
            redisTemplate.expire(key, period, timeUnit);
        }
        
        return currentCount <= count;
    }
    
    /**
     * 令牌桶限流（使用Lua脚本保证原子性）
     */
    public boolean tokenBucketLimit(String key, int count, int period, TimeUnit timeUnit) {
        String luaScript = 
            "local key = KEYS[1]\n" +
            "local capacity = tonumber(ARGV[1])\n" +
            "local rate = tonumber(ARGV[2])\n" +
            "local now = tonumber(ARGV[3])\n" +
            "local requested = 1\n" +
            "\n" +
            "local bucket = redis.call('hmget', key, 'tokens', 'last_time')\n" +
            "local tokens = tonumber(bucket[1])\n" +
            "local last_time = tonumber(bucket[2])\n" +
            "\n" +
            "if tokens == nil then\n" +
            "  tokens = capacity\n" +
            "  last_time = now\n" +
            "end\n" +
            "\n" +
            "local delta = math.max(0, now - last_time)\n" +
            "local new_tokens = math.min(capacity, tokens + delta * rate)\n" +
            "\n" +
            "if new_tokens >= requested then\n" +
            "  redis.call('hmset', key, 'tokens', new_tokens - requested, 'last_time', now)\n" +
            "  redis.call('expire', key, " + timeUnit.toSeconds(period) + ")\n" +
            "  return 1\n" +
            "else\n" +
            "  return 0\n" +
            "end";
        
        DefaultRedisScript<Long> script = new DefaultRedisScript<>(luaScript, Long.class);
        double rate = (double) count / timeUnit.toSeconds(period);
        Long result = redisTemplate.execute(
            script,
            Collections.singletonList(key),
            String.valueOf(count),
            String.valueOf(rate),
            String.valueOf(System.currentTimeMillis() / 1000)
        );
        
        return result != null && result == 1;
    }
}
