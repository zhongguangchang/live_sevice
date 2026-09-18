package com.sky.constant;

/**
 * Redis key 常量类
 * <p>
 * 统一管理所有 Redis key 前缀，避免魔法字符串散落在各个 Service 里。
 * 命名约定：{业务域}:{数据类型}:{业务主键}
 */
public class RedisKeyConstant {

    /**
     * 店铺营业状态
     * 类型 String，值 1营业中 0打烊中
     */
    public static final String SHOP_STATUS = "shop:status";

    /**
     * 时段名额库存（Redis 预扣层）
     * 类型 String（数字），key 形如 slot:stock:1001
     * 由 Lua 脚本原子扣减，是防超卖的第一道闸门
     */
    public static final String SLOT_STOCK_PREFIX = "slot:stock:";

    /**
     * 时段已预约用户集合
     * 类型 Set，key 形如 slot:users:1001
     * 用于实现「同一用户同一时段只能约一次」，取消订单时要 SREM 移除
     */
    public static final String SLOT_USERS_PREFIX = "slot:users:";

    /**
     * 分布式锁 key 前缀
     * 用于防重复下单、支付回调幂等
     */
    public static final String LOCK_ORDER_SUBMIT_PREFIX = "lock:order:submit:";
    public static final String LOCK_ORDER_PAY_PREFIX = "lock:order:pay:";
    public static final String LOCK_DISPATCH_PREFIX = "lock:order:dispatch:";

    /**
     * 缓存 key 前缀
     */
    public static final String CACHE_SERVICE_ITEM_PREFIX = "cache:service:item:";
    public static final String CACHE_SERVICE_LIST_PREFIX = "cache:service:list:";
    public static final String CACHE_CATEGORY_LIST = "cache:category:list";
    public static final String CACHE_SLOT_LIST_PREFIX = "cache:slot:list:";

    /**
     * 空值缓存（解决缓存穿透）
     * 查不到数据时写入这个占位值，避免请求反复打到数据库
     */
    public static final String CACHE_NULL_VALUE = "";

    /**
     * 接口限流 key 前缀
     */
    public static final String RATE_LIMIT_PREFIX = "rate:limit:";

    /**
     * 拼装时段库存 key
     */
    public static String slotStockKey(Long slotId) {
        return SLOT_STOCK_PREFIX + slotId;
    }

    /**
     * 拼装时段已预约用户集合 key
     */
    public static String slotUsersKey(Long slotId) {
        return SLOT_USERS_PREFIX + slotId;
    }
}
