package com.sky.exception;

/**
 * 时段不可用
 * <p>
 * 时段不存在、已关闭、或者还没预热到 Redis 时抛出。
 */
public class SlotNotAvailableException extends BaseException {

    public SlotNotAvailableException(String msg) {
        super(msg);
    }
}
