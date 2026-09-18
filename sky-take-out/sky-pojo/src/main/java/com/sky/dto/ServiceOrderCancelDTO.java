package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 取消订单入参
 */
@Data
public class ServiceOrderCancelDTO implements Serializable {

    //订单id
    private Long id;

    //取消原因
    private String cancelReason;
}
