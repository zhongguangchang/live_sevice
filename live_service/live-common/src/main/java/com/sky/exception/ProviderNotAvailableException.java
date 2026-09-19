package com.sky.exception;

/**
 * 服务人员当前不可接单
 * <p>
 * 派单或师傅接单时，师傅处于休息、忙碌或离职状态则抛出。
 */
public class ProviderNotAvailableException extends BaseException {

    public ProviderNotAvailableException(String msg) {
        super(msg);
    }
}
