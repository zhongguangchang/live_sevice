package com.sky.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 订单分页查询入参
 */
@Data
public class ServiceOrderPageQueryDTO implements Serializable {

    //页码
    private int page;

    //每页记录数
    private int pageSize;

    //订单号（精确匹配）
    private String number;

    //联系电话
    private String phone;

    //订单状态 1待付款 2待接单 3已接单 4服务中 5待评价 6已完成 7已取消
    private Integer status;

    //服务人员id
    private Long providerId;

    //下单用户id
    private Long userId;

    //下单开始时间
    private LocalDateTime beginTime;

    //下单结束时间
    private LocalDateTime endTime;
}
