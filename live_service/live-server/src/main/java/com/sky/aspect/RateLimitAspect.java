package com.sky.aspect;

import com.sky.annotation.RateLimit;
import com.sky.constant.MessageConstant;
import com.sky.constant.RedisKeyConstant;
import com.sky.context.BaseContext;
import com.sky.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流切面
 * <p>
 * <b>为什么用 Redis 计数器而不是 Guava RateLimiter：</b>
 * 单机限流器只在单实例下有效，部署两台机器时用户可以拿到两倍的额度；
 * Redis 计数器是全局共享的，多实例下依然准确。
 * <p>
 * <b>为什么用 INCR + EXPIRE 而不是「先 GET 再 INCR」：</b>
 * 两个命令分开写会有并发窗口 —— 同一毫秒内的多个请求都读到 0，
 * 然后一起自增，限流形同虚设。Redis 的 INCR 本身是原子的，
 * 配合「第一次自增时设置过期时间」就足够了。
 * <p>
 * 注意：切面只做「计数」，不参与事务。所以即使后面业务抛异常、事务回滚，
 * 计数也是算数的 —— 这正是限流想要的语义（防止高频重试）。
 */
@Aspect
@Component
@Slf4j
public class RateLimitAspect {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Pointcut("@annotation(com.sky.annotation.RateLimit)")
    public void rateLimitPointCut() {
    }

    @Around("rateLimitPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RateLimit rateLimit = signature.getMethod().getAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return joinPoint.proceed();
        }

        String identity = currentIdentity();
        String redisKey = RedisKeyConstant.RATE_LIMIT_PREFIX
                + rateLimit.key() + ":" + identity;

        Long count = stringRedisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1L) {
            // 只有第一次自增时才设过期时间：
            // 每次都设的话，用户一直点就会一直续期，窗口永远不过期
            stringRedisTemplate.expire(redisKey, Duration.ofSeconds(rateLimit.seconds()));
        }

        if (count != null && count > rateLimit.limit()) {
            Long ttl = stringRedisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            log.warn("触发限流：key={}, identity={}, 窗口内第 {} 次，窗口剩余 {} 秒",
                    rateLimit.key(), identity, count, ttl);
            throw new RateLimitException(MessageConstant.RATE_LIMIT_EXCEEDED);
        }

        return joinPoint.proceed();
    }

    /**
     * 限流维度：优先用登录态里的用户/员工 id，没登录时退化为客户端 IP
     */
    private String currentIdentity() {
        Long currentId = BaseContext.getCurrentId();
        if (currentId != null) {
            return String.valueOf(currentId);
        }
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "anonymous";
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
