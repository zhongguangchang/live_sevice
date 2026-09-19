package com.sky.exception;

/**
 * 时段名额已抢完
 * <p>
 * 由 Redis Lua 预扣脚本返回 -2 时抛出。
 */
public class SlotSoldOutException extends BaseException {

    public SlotSoldOutException(String msg) {
        super(msg);
    }
}
