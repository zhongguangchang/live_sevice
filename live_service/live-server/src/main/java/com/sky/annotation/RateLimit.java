package com.sky.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解
 * <p>
 * 加在下单、支付、评价这类「写接口」上，防止用户连点、
 * 或者脚本高频刷单刷评价。
 * <p>
 * 用法：
 * <pre>
 *   &#64;RateLimit(key = "order:submit", seconds = 5, limit = 3)
 *   意思是：同一用户在 5 秒内最多提交 3 次，超过直接拒绝。
 * </pre>
 * <p>
 * 为什么限流按「用户」而不是按「IP」：
 * 校园网/公司网出口 IP 是共用的，按 IP 限流会误伤同一栋楼的所有人；
 * 而限流的目的本来就是防单个账号连点，按登录态里的 userId 最准确。
 * 未登录场景（本项目写接口都要登录）退化为按 IP。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流维度标识，相同的 key 共用一套计数器。
     * 建议写成「业务:动作」，例如 order:submit
     */
    String key();

    /**
     * 统计窗口，单位秒
     */
    int seconds() default 1;

    /**
     * 窗口内允许的最大次数
     */
    int limit() default 5;
}
