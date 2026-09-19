package com.sky.service;

/**
 * 平台营业状态
 */
public interface ShopService {

    /** 营业中 */
    Integer STATUS_OPEN = 1;

    /** 打烊中 */
    Integer STATUS_CLOSED = 0;

    /**
     * 查询当前营业状态
     */
    Integer getStatus();

    /**
     * 设置营业状态
     */
    void setStatus(Integer status);
}
