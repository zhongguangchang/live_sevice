package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 订单支付入参
 */
@Data
public class ServiceOrderPaymentDTO implements Serializable {

    //订单号
    private String orderNumber;

    //支付方式 1微信 2支付宝
    private Integer payMethod;
}
