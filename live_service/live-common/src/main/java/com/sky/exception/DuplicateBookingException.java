package com.sky.exception;

/**
 * 重复预约同一时段
 * <p>
 * 由 Redis Lua 预扣脚本返回 -3 时抛出。
 * 有这道校验是因为用户可能连点提交按钮，或者换了设备重新下单。
 */
public class DuplicateBookingException extends BaseException {

    public DuplicateBookingException(String msg) {
        super(msg);
    }
}
