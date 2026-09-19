package com.sky.utils;

import com.alibaba.fastjson.JSON;
import com.sky.constant.RedisKeyConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * 缓存工具（缓存三兄弟的落地处）
 * <p>
 * <b>缓存穿透</b>——查一个不存在的数据，缓存永远不命中，请求全都打到数据库。
 * 做法：查不到也缓存（存一个空值占位，TTL 短一些），
 * 这样同一个不存在的 id 再来就直接在缓存层被挡住。
 * <p>
 * <b>缓存击穿</b>——某个热点 key 刚好过期，瞬间大量并发请求同时去查数据库。
 * 做法：重建缓存时用 SETNX 抢一把互斥锁，只放一个请求去查库，
 * 其余请求短暂等待后重试读缓存。抢不到锁又等不到数据的，直接放行去查库，
 * 避免死等造成请求堆积。
 * <p>
 * <b>缓存雪崩</b>——大批 key 设了相同的过期时间，同时失效，数据库瞬时被打爆。
 * 做法：过期时间加上随机偏移（见 {@link #randomTtl}）。
 * <p>
 * 为什么用 StringRedisTemplate + fastjson 存 JSON 字符串，
 * 而不是用带 JSON 序列化器的 RedisTemplate 存对象：
 * 后者会在 JSON 里写入 {@code @class} 类型信息，虽然能自动还原成对象，
 * 但一旦实体类改名/挪包，历史缓存就反序列化失败；
 * 用字符串 + 显式指定目标类型，缓存内容和代码解耦，出问题也好排查。
 */
@Component
@Slf4j
public class CacheHelper {

    /** 空值占位符：缓存「查不到」这个事实，用于防止缓存穿透 */
    private static final String NULL_HOLDER = RedisKeyConstant.CACHE_NULL_VALUE;

    /** 空值缓存的有效期（秒）。短一些，避免数据真的被创建后长时间读不到 */
    private static final int NULL_TTL_SECONDS = 60;

    /** 重建缓存时互斥锁的持有时长（秒） */
    private static final int REBUILD_LOCK_SECONDS = 10;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 读缓存，没有就加载并回写
     *
     * @param key        缓存 key
     * @param type       目标类型（fastjson 的 Type，支持 List&lt;VO&gt; 这类泛型）
     * @param loader     回源方法（通常是查数据库）
     * @param ttlSeconds 基础过期时间，实际会加随机偏移防雪崩
     */
    public <T> T getOrLoad(String key, Type type, Supplier<T> loader, long ttlSeconds) {
        String cached = stringRedisTemplate.opsForValue().get(key);
        if (cached != null) {
            if (NULL_HOLDER.equals(cached)) {
                // 命中空值占位：说明之前查过、确实没有，直接返回 null，
                // 不再回源，穿透就被挡住了
                return null;
            }
            try {
                return JSON.parseObject(cached, type);
            } catch (Exception e) {
                // 缓存内容解析失败（结构变更、被人手工改过）时不能直接报错，
                // 当作未命中重新回源即可
                log.warn("缓存解析失败，按未命中处理：key={}, 原因={}", key, e.getMessage());
            }
        }
        return loadAndCache(key, type, loader, ttlSeconds);
    }

    /**
     * 回源并写缓存，带互斥锁防止缓存击穿
     */
    private <T> T loadAndCache(String key, Type type, Supplier<T> loader, long ttlSeconds) {
        String lockKey = key + ":rebuild";
        boolean locked = tryLock(lockKey);
        if (!locked) {
            // 没抢到锁：说明有别的线程正在重建。
            // 等一小会儿再看一眼缓存，命中就返回；
            // 还不行就直接回源，宁可多查一次库也不让请求卡住
            sleepQuietly(50);
            String cached = stringRedisTemplate.opsForValue().get(key);
            if (cached != null && !NULL_HOLDER.equals(cached)) {
                try {
                    return JSON.parseObject(cached, type);
                } catch (Exception ignored) {
                    // 落到下面直接回源
                }
            }
        }

        try {
            T value = loader.get();
            if (value == null || (value instanceof java.util.Collection
                    && ((java.util.Collection<?>) value).isEmpty())) {
                // 空结果也缓存，防止同一个不存在的条件反复打库
                stringRedisTemplate.opsForValue()
                        .set(key, NULL_HOLDER, Duration.ofSeconds(NULL_TTL_SECONDS));
                return value;
            }
            stringRedisTemplate.opsForValue()
                    .set(key, JSON.toJSONString(value), Duration.ofSeconds(randomTtl(ttlSeconds)));
            return value;
        } finally {
            if (locked) {
                stringRedisTemplate.delete(lockKey);
            }
        }
    }

    /**
     * 删除单个缓存
     */
    public void evict(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            // 删缓存失败不能影响主流程：缓存最多多存活一个 TTL，
            // 业务上会短暂读到旧数据，但不会出错
            log.warn("清除缓存失败：key={}, 原因={}", key, e.getMessage());
        }
    }

    /**
     * 按前缀批量删除（例如某个分类下的服务列表都失效）
     */
    public void evictByPrefix(String prefix) {
        try {
            java.util.Set<String> keys = stringRedisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
                log.info("按前缀清除缓存：prefix={}, 共 {} 个", prefix, keys.size());
            }
        } catch (Exception e) {
            log.warn("按前缀清除缓存失败：prefix={}, 原因={}", prefix, e.getMessage());
        }
    }

    /**
     * 随机过期时间：基础值 + 0~20% 的随机偏移
     * <p>
     * 这是防缓存雪崩最关键的一步。如果所有 key 都是 30 分钟过期，
     * 它们会在同一秒集体失效，数据库瞬间接到全量请求。
     */
    private long randomTtl(long baseSeconds) {
        long offset = ThreadLocalRandom.current().nextLong(Math.max(1, baseSeconds / 5));
        return baseSeconds + offset;
    }

    private boolean tryLock(String lockKey) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, "1", Duration.ofSeconds(REBUILD_LOCK_SECONDS));
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            // Redis 异常时不阻塞业务，直接放行回源
            return false;
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
