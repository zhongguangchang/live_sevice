package com.sky.service.impl;

import com.sky.service.ShopService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 平台营业状态实现
 * <p>
 * 状态存在 Redis 里（key：SHOP_STATUS），因为读的频率极高而变更极少，
 * 没必要每次都查数据库。
 * <p>
 * <b>为什么要有这个 Service，而不是像原来那样在 Controller 里直接读 Redis：</b>
 * <ol>
 *   <li>原来的 {@code status == 1} 是 Integer 和 int 比较，会触发自动拆箱。
 *       Redis 里没有这个 key 时（比如换了库、Redis 重启、第一次部署）
 *       status 是 null，拆箱直接抛 NullPointerException，接口 500</li>
 *   <li>营业状态现在不只是给前端展示用的，<b>下单时要校验</b>：
 *       打烊后用户端必须下不了单。校验逻辑放在下单流程里，
 *       意味着除了 Controller 之外的代码也要能拿到这个状态</li>
 * </ol>
 */
@Service
@Slf4j
public class ShopServiceImpl implements ShopService {

    /** 和管理端前端约定好的 key，不要随意改，改了两端要对齐 */
    public static final String KEY = "SHOP_STATUS";

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public Integer getStatus() {
        Object value = redisTemplate.opsForValue().get(KEY);
        if (value == null) {
            // 从没设置过时按「营业中」处理。
            // 反过来默认打烊的话，Redis 一重启用户就全下不了单，
            // 这种故障比「多收一个订单」严重得多
            return STATUS_OPEN;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.valueOf(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Redis 里的营业状态格式异常，按营业中处理：value={}", value);
            return STATUS_OPEN;
        }
    }

    @Override
    public void setStatus(Integer status) {
        redisTemplate.opsForValue().set(KEY, status);
    }
}
