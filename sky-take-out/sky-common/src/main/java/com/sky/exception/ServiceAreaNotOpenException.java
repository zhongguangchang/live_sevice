package com.sky.exception;

/**
 * 服务区域未开通
 * <p>
 * 用户下单时地址所在区不在 service_area 表里，或者该区状态为未开通。
 * 这是生活服务网比外卖多出来的一道校验——外卖只要商家在配送范围内即可，
 * 生活服务得先保证平台确实有师傅能到。
 */
public class ServiceAreaNotOpenException extends BaseException {

    public ServiceAreaNotOpenException(String msg) {
        super(msg);
    }
}
